package com.app.administradorfarmadon.autenticacion.registro.contenedor.logica

import com.app.administradorfarmadon.autenticacion.datos.AuthPaths
import com.app.administradorfarmadon.autenticacion.login.datos.ListaNegraRepository
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RegistroIncidenteTipo
import com.app.administradorfarmadon.base_datos.PlanSuscripcion
import com.app.administradorfarmadon.compartido.datos.EcosistemaPaths
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.compartido.sha256
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

sealed class FiltroRegistroResultado {
    data object PASA : FiltroRegistroResultado()
    data class FALLA(
        val mensaje: String,
        val tipo: RegistroIncidenteTipo? = null,
        val esTemporal: Boolean = false
    ) : FiltroRegistroResultado()
}

/** Estados con solicitud VIVA en cola: un reenvío los pisaría. íšnica fuente (filtro + transacción). */
internal val ESTADOS_SOLICITUD_ACTIVA = setOf("pendiente", "en_revision", "observada")

object RegistroFiltros {

    suspend fun verificar(
        db: FirebaseFirestore,
        ruc: String,
        email: String,
        plan: PlanSuscripcion?
    ): FiltroRegistroResultado {
        val rucLimpio = ruc.trim()
        val emailLimpio = email.trim().lowercase()
        if (rucLimpio.isBlank()) {
            return FiltroRegistroResultado.FALLA(
                "Ingresa un RUC/DNI válido para continuar.",
                tipo = RegistroIncidenteTipo.ERROR_BASE_DATOS
            )
        }

        // FILTRO 1 · ¿Ya es cliente?
        // La verdad real: el documento de negocio farmacias/{RUC} que BrixoPanel
        // crea al aprobar. Cero tarjetas intermedias: un dato, una ruta.
        val farmaciaDoc = FarmadonPaths.farmacia(db, rucLimpio).get(Source.SERVER).await()
        if (farmaciaDoc.exists()) {
            return FiltroRegistroResultado.FALLA(
                "Este RUC ya está registrado en el sistema.",
                tipo = RegistroIncidenteTipo.RUC_EXISTENTE
            )
        }

        // FILTRO 2 · ¿Ya envió una solicitud y sigue EN PROCESO?
        // Bloquea los tres estados vivos: un reenvío durante la revisión
        // borraría la custodia del agente. "rechazada" SÍ permite reintento.
        val solDoc = AuthPaths.solicitudes(db).document(rucLimpio).get(Source.SERVER).await()
        if (solDoc.exists() && solDoc.getString("estado") in ESTADOS_SOLICITUD_ACTIVA) {
            return FiltroRegistroResultado.FALLA(
                "Este RUC ya tiene una solicitud en proceso. Espera la revisión de BRIXO.",
                tipo = RegistroIncidenteTipo.RUC_DUPLICADO_EN_COLA
            )
        }

        // FILTRO 3 · ¿Está en la lista negra?
        val restRuc = AuthPaths.existeRestringido(db).document(rucLimpio).get(Source.SERVER).await()
        val restEmail = AuthPaths.existeRestringido(db).document(emailLimpio.sha256()).get(Source.SERVER).await()
        if (restRuc.exists() || restEmail.exists()) {
        	val motivo = try {
        		ListaNegraRepository.consultar(db, rucLimpio, emailLimpio)?.getString("motivo")
        	} catch (e: Exception) {
        		return FiltroRegistroResultado.FALLA(
        			"No se pudo verificar la lista negra: ${e.message ?: e.toString()}",
        			tipo = RegistroIncidenteTipo.ERROR_BASE_DATOS
        		)
        	}

        	return FiltroRegistroResultado.FALLA(
        		"Este RUC/correo está restringido. ${motivo ?: "Sin motivo registrado."}",
        		tipo = RegistroIncidenteTipo.USUARIO_BANEADO
        	)
        }

        // FILTRO 4 · ¿El plan elegido sigue activo?
        if (plan != null && plan.id.isNotBlank()) {
            val planDoc = EcosistemaPaths.planes(db).document(plan.id).get(Source.SERVER).await()
            if (!planDoc.exists() || planDoc.getBoolean("activo") != true || planDoc.getBoolean("eliminado") == true) {
                return FiltroRegistroResultado.FALLA(
                    "El plan seleccionado ya no está disponible. Elige otro.",
                    tipo = RegistroIncidenteTipo.PLAN_NO_DISPONIBLE
                )
            }
        }

        return FiltroRegistroResultado.PASA
    }
}
