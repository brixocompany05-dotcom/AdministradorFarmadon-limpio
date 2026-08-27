package com.app.administradorfarmadon.autenticacion.registro

import android.app.Application
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.app.administradorfarmadon.autenticacion.login.datos.ListaNegraRepository
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RegistroDraftManager
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RegistroIncidenteAccion
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RegistroIncidenteTipo
import com.app.administradorfarmadon.autenticacion.registro.contenedor.logica.RegistroFarmaciaViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.logica.EnviarCorreccionUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TestRule

@OptIn(ExperimentalCoroutinesApi::class)
class RegistroFarmaciaViewModelTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: RegistroFarmaciaViewModel
    private val application = mockk<Application>(relaxed = true)

    private val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    private val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)
    private val mockUser = mockk<FirebaseUser>(relaxed = true)
    private val mockEnviarCorreccionUseCase = mockk<EnviarCorreccionUseCase>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock de Firebase estáticos
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0

        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)
        
        every { FirebaseAuth.getInstance() } returns mockAuth
        every { FirebaseFirestore.getInstance() } returns mockFirestore
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.delete() } returns Tasks.forResult(null)

        // Mock para evitar errores en escucharPlanes()
        val mockCollection = mockk<CollectionReference>(relaxed = true)
        val mockQuery = mockk<Query>(relaxed = true)
        val mockEcosistema = mockk<DocumentReference>(relaxed = true)

        every { mockFirestore.collection("compartido") } returns mockCollection
        every { mockCollection.document("ecosistema") } returns mockEcosistema
        every { mockEcosistema.collection("planes") } returns mockCollection

        every { mockCollection.whereEqualTo(any<String>(), any()) } returns mockQuery
        every { mockQuery.whereEqualTo(any<String>(), any()) } returns mockQuery
        every { mockQuery.orderBy(any<String>(), any()) } returns mockQuery
        every { mockQuery.addSnapshotListener(any()) } returns mockk(relaxed = true)
        
        // Mock de DraftManager
        mockkObject(RegistroDraftManager)
        every { RegistroDraftManager.saveDraft(any(), any()) } just Runs

        // Mock de ListaNegraRepository
        mockkObject(ListaNegraRepository)
        coEvery { ListaNegraRepository.consultar(any(), any(), any()) } returns null
        
        coEvery { mockEnviarCorreccionUseCase.ejecutar(any(), any(), any(), any(), any()) } returns Unit
        
        viewModel = RegistroFarmaciaViewModel(application, mockk(relaxed = true), mockEnviarCorreccionUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `enviarCorreccion bloquea si no hay cambios sustantivos`() = runTest {
        val mockDoc = mockk<DocumentSnapshot>(relaxed = true)
        every { mockDoc.exists() } returns true
        every { mockDoc.getBoolean("correccionSolicitada") } returns true
        every { mockDoc.getString("nombreFarmacia") } returns "Farmacia"
        every { mockDoc.getString("dueno") } returns "Dueño"
        every { mockDoc.getString("email") } returns "test@test.com"
        every { mockDoc.getString("telefono") } returns "999888777"
        every { mockDoc.getString("ruc") } returns "20123456789"
        every { mockDoc.getString("direccion") } returns "Direccion"
        every { mockDoc.getGeoPoint("ubicacionGeo") } returns null
        every { mockDoc.getString("planId") } returns "p1"
        every { mockDoc.getLong("version") } returns 1L

        val mockSolCol = mockk<CollectionReference>(relaxed = true)
        val mockDocRef = mockk<DocumentReference>(relaxed = true)
        every { mockFirestore.collection("farmaciapp").document("app").collection("solicitudes") } returns mockSolCol
        every { mockSolCol.document("test-uid") } returns mockDocRef
        every { mockDocRef.get() } returns Tasks.forResult(mockDoc)

        viewModel.precargarSolicitudCorreccion("test-uid")
        advanceUntilIdle()

        viewModel.enviarCorreccion()
        advanceUntilIdle()

        assertEquals(RegistroIncidenteTipo.DATOS_IDENTICOS, viewModel.state.value.incidenteActual?.tipo)
    }

    @Test
    fun `ejecutarAccionIncidente RecargarSolicitud dispara precarga`() = runTest {
        val mockDoc = mockk<DocumentSnapshot>(relaxed = true)
        every { mockDoc.exists() } returns true
        every { mockDoc.getBoolean("correccionSolicitada") } returns true
        every { mockDoc.getLong("version") } returns 1L

        val mockDocRef = mockk<DocumentReference>(relaxed = true)
        every { mockFirestore.collection("farmaciapp").document("app").collection("solicitudes").document("test-uid") } returns mockDocRef
        every { mockDocRef.get() } returns Tasks.forResult(mockDoc)

        viewModel.precargarSolicitudCorreccion("test-uid")
        advanceUntilIdle()

        viewModel.ejecutarAccionIncidente(RegistroIncidenteAccion.RecargarSolicitud, {}, {})
        advanceUntilIdle()

        verify(exactly = 2) { mockDocRef.get() }
    }
}
