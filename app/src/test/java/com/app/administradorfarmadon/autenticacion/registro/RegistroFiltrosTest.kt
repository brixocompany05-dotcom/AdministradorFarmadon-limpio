package com.app.administradorfarmadon.autenticacion.registro

import com.app.administradorfarmadon.autenticacion.login.datos.ListaNegraRepository
import com.app.administradorfarmadon.autenticacion.registro.contenedor.logica.RegistroFiltros
import com.app.administradorfarmadon.autenticacion.registro.contenedor.logica.FiltroRegistroResultado
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RegistroIncidenteTipo
import com.app.administradorfarmadon.base_datos.PlanSuscripcion
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueba el PIPELINE de 4 filtros del registro:
 * 1) ya es cliente, 2) ya envió solicitud, 3) lista negra, 4) plan activo.
 * Verifica el ORDEN y el corto-circuito (el siguiente filtro no corre si el
 * anterior falló).
 */
class RegistroFiltrosTest {

    private val db = mockk<FirebaseFirestore>()
    private val farmaciaDocRef = mockk<DocumentReference>()     // farmacias/{RUC}
    private val solicitudDocRef = mockk<DocumentReference>()
    private val planDocRef = mockk<DocumentReference>()
    private val restRucRef = mockk<DocumentReference>()
    private val restEmailRef = mockk<DocumentReference>()

    private val rootFarmacia = mockk<CollectionReference>()        // farmaciapp
    private val appDoc = mockk<DocumentReference>()                 // farmaciapp/app
    private val solicitudesCol = mockk<CollectionReference>()
    private val farmaciasCol = mockk<CollectionReference>()

    private val rootBrixo = mockk<CollectionReference>()            // brixo
    private val existenciaDoc = mockk<DocumentReference>()          // brixo/existencia
    private val existeRestringidosCol = mockk<CollectionReference>()

    private val rootCompartido = mockk<CollectionReference>()       // compartido
    private val ecosistemaDoc = mockk<DocumentReference>()          // compartido/ecosistema
    private val planesCol = mockk<CollectionReference>()

    private val plan = PlanSuscripcion(id = "p1", nombre = "Plan")

    private fun setup(
        esCliente: Boolean = false,
        restringido: Boolean = false,
        haySolicitudPendiente: Boolean = false,
        estadoSolicitud: String = "pendiente",
        planActivo: Boolean = true
    ) {
        // farmaciapp/app/farmacias/{RUC} — el documento REAL "¿ya es cliente?"
        every { db.collection("farmaciapp") } returns rootFarmacia
        every { rootFarmacia.document("app") } returns appDoc
        every { appDoc.collection("solicitudes") } returns solicitudesCol
        every { solicitudesCol.document(any()) } returns solicitudDocRef
        val solSnap = mockk<DocumentSnapshot>()
        every { solSnap.exists() } returns haySolicitudPendiente
        every { solSnap.getString("estado") } returns if (haySolicitudPendiente) estadoSolicitud else ""
        every { solicitudDocRef.get(Source.SERVER) } returns Tasks.forResult(solSnap)

        every { appDoc.collection("farmacias") } returns farmaciasCol
        every { farmaciasCol.document(any()) } returns farmaciaDocRef
        val farmSnap = mockk<DocumentSnapshot>()
        every { farmSnap.exists() } returns esCliente
        every { farmaciaDocRef.get(Source.SERVER) } returns Tasks.forResult(farmSnap)

        // brixo/existencia/restringidos/{RUC} y /{hash} — tarjeta "¿está restringido?"
        every { db.collection("brixo") } returns rootBrixo
        every { rootBrixo.document("existencia") } returns existenciaDoc
        every { existenciaDoc.collection("restringidos") } returns existeRestringidosCol
        every { existeRestringidosCol.document(any()) } returns restRucRef
        every { existeRestringidosCol.document("20123456789") } returns restRucRef
        val restSnapRuc = mockk<DocumentSnapshot>()
        every { restSnapRuc.exists() } returns restringido
        every { restRucRef.get(Source.SERVER) } returns Tasks.forResult(restSnapRuc)

        // compartido/ecosistema/planes/{id} — "¿plan activo?"
        every { db.collection("compartido") } returns rootCompartido
        every { rootCompartido.document("ecosistema") } returns ecosistemaDoc
        every { ecosistemaDoc.collection("planes") } returns planesCol
        every { planesCol.document("p1") } returns planDocRef
        val planSnap = mockk<DocumentSnapshot>()
        every { planSnap.exists() } returns true
        every { planSnap.getBoolean("activo") } returns planActivo
        every { planSnap.getBoolean("eliminado") } returns false
        every { planDocRef.get(Source.SERVER) } returns Tasks.forResult(planSnap)

        // Lista negra (solo para el motivo opcional; la decisión la da la tarjeta)
        mockkObject(ListaNegraRepository)
        coEvery { ListaNegraRepository.consultar(any(), any(), any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkObject(ListaNegraRepository)
    }

    @Test
    fun `filtro 1 - falla si el RUC ya es cliente y detiene el resto`() = runTest {
        setup(esCliente = true)
        val r = RegistroFiltros.verificar(db, "20123456789", "a@b.com", plan)
        assertTrue(r is FiltroRegistroResultado.FALLA)
        assertTrue((r as FiltroRegistroResultado.FALLA).tipo == RegistroIncidenteTipo.RUC_EXISTENTE)
        verify(exactly = 0) { solicitudesCol.document(any()) }
    }

    @Test
    fun `filtro 2 - falla si ya hay una solicitud pendiente`() = runTest {
        setup(esCliente = false, haySolicitudPendiente = true)
        val r = RegistroFiltros.verificar(db, "20123456789", "a@b.com", plan)
        assertTrue(r is FiltroRegistroResultado.FALLA)
        assertTrue((r as FiltroRegistroResultado.FALLA).tipo == RegistroIncidenteTipo.RUC_DUPLICADO_EN_COLA)
    }

    @Test
    fun `filtro 2 - bloquea reenvio durante revision u observacion del agente`() = runTest {
        for (estado in listOf("en_revision", "observada")) {
            setup(haySolicitudPendiente = true, estadoSolicitud = estado)
            val r = RegistroFiltros.verificar(db, "20123456789", "a@b.com", plan)
            assertTrue("Estado $estado debió bloquear", r is FiltroRegistroResultado.FALLA)
            assertTrue(
                "Estado $estado debió ser RUC_DUPLICADO_EN_COLA",
                (r as FiltroRegistroResultado.FALLA).tipo == RegistroIncidenteTipo.RUC_DUPLICADO_EN_COLA
            )
        }
    }

    @Test
    fun `filtro 3 - falla si el RUC esta en lista negra`() = runTest {
        setup(restringido = true)
        val banDoc = mockk<DocumentSnapshot>()
        every { banDoc.getString("motivo") } returns "Bloqueado por fraude"
        coEvery { ListaNegraRepository.consultar(any(), any(), any()) } returns banDoc

        val r = RegistroFiltros.verificar(db, "20123456789", "a@b.com", plan)
        assertTrue(r is FiltroRegistroResultado.FALLA)
        assertTrue((r as FiltroRegistroResultado.FALLA).tipo == RegistroIncidenteTipo.USUARIO_BANEADO)
        verify(exactly = 0) { planesCol.document(any()) }
    }

    @Test
    fun `filtro 4 - falla si el plan no esta activo`() = runTest {
        setup(esCliente = false, haySolicitudPendiente = false, planActivo = false)
        val r = RegistroFiltros.verificar(db, "20123456789", "a@b.com", plan)
        assertTrue(r is FiltroRegistroResultado.FALLA)
        assertTrue((r as FiltroRegistroResultado.FALLA).tipo == RegistroIncidenteTipo.PLAN_NO_DISPONIBLE)
    }

    @Test
    fun `pasa si los 4 filtros salen positivos`() = runTest {
        setup()
        val r = RegistroFiltros.verificar(db, "20123456789", "a@b.com", plan)
        assertTrue(r is FiltroRegistroResultado.PASA)
    }
}