package com.app.administradorfarmadon.autenticacion.registro.contenedor.logica
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.datos.AuthPaths
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.*
import com.app.administradorfarmadon.compartido.datos.EcosistemaPaths
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.compartido.sha256
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

internal fun RegistroFarmaciaViewModel.ejecutarRegistroConFirebaseImpl() {
    _state.update {
        it.copy(
            cargando = true,
            progresoEnvio = 0.10f,
            mensajeProgreso = "Estableciendo conexión segura...",
            incidenteActual = null,
            paso1 = it.paso1.copy(erroresCampos = emptyMap())
        )
    }
    viewModelScope.launch {
        try {
            val email = _state.value.paso1.email.trim().lowercase()
            val ruc = _state.value.paso1.ruc.trim()
            val planSeleccionado = _state.value.paso2.planSeleccionado
            val db = FarmadonFirestore.db

            _state.update { it.copy(progresoEnvio = 0.35f, mensajeProgreso = "Comprobando duplicados en la central...") }

            val resultadoFiltros = try {
                RegistroFiltros.verificar(db, ruc, email, planSeleccionado)
            } catch (e: Exception) {
                android.util.Log.w("VIORA_REGISTRO", "Filtros sin red o error de base de datos", e)
                val tipo = when (e) {
                    is FirebaseNetworkException -> RegistroIncidenteTipo.SIN_INTERNET
                    is FirebaseFirestoreException -> if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) RegistroIncidenteTipo.SIN_INTERNET else RegistroIncidenteTipo.ERROR_BASE_DATOS
                    else -> RegistroIncidenteTipo.ERROR_BASE_DATOS
                }
                _state.update { it.copy(cargando = false, progresoEnvio = 0f, mensajeProgreso = "", incidenteActual = mapearIncidente(tipo, customMensaje = e.message)) }
                enviando = false
                return@launch
            }

            when (resultadoFiltros) {
                is FiltroRegistroResultado.FALLA -> {
                    _state.update {
                        it.copy(
                            cargando = false,
                            incidenteActual = mapearIncidente(
                                resultadoFiltros.tipo ?: RegistroIncidenteTipo.ERROR_BASE_DATOS,
                                if (resultadoFiltros.tipo == RegistroIncidenteTipo.USUARIO_BANEADO) resultadoFiltros.mensaje else null
                            )
                        )
                    }
                    return@launch
                }
                FiltroRegistroResultado.PASA -> Unit
            }

            _state.update { it.copy(progresoEnvio = 0.60f, mensajeProgreso = "Creando credenciales de acceso...") }
            val contrasena = _state.value.paso1.contrasena
            val authResult = FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, contrasena)
                .await()
            val authUid = authResult.user?.uid
                ?: throw Exception("No se pudo crear la cuenta de usuario")

            _state.update { it.copy(progresoEnvio = 0.85f, mensajeProgreso = "Guardando expediente en la central...") }
            try {
                guardarEnFirestoreConTransaccion(authUid)
            } catch (firestoreError: Exception) {
                // Rollback atómico: Si falla el guardado del expediente, eliminar la cuenta Auth
                // recién creada para no dejar un usuario huérfano ni atrapar al postulante (Regla R3)
                try {
                    authResult.user?.delete()?.await()
                } catch (delEx: Exception) {
                    android.util.Log.e("VIORA_REGISTRO", "Fallo al revertir cuenta Auth huérfana", delEx)
                }
                throw firestoreError
            }
            FirebaseAuth.getInstance().signOut()

            borrarBorrador()
            _state.update { it.copy(cargando = false, progresoEnvio = 1.0f, mensajeProgreso = "¡Completado exitosamente!", exitoso = true) }
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.toString()
            android.util.Log.e("VIORA_REGISTRO", "Error durante el registro: $errorMsg", e)
            val incidente = when (e) {
                is FirebaseAuthUserCollisionException -> RegistroIncidenteTipo.CORREO_EXISTENTE
                is RucDuplicadoException -> RegistroIncidenteTipo.RUC_EXISTENTE
                is RucDuplicadoEnColaException -> RegistroIncidenteTipo.RUC_DUPLICADO_EN_COLA
                is PlanNoDisponibleException -> RegistroIncidenteTipo.PLAN_NO_DISPONIBLE
                is FirebaseNetworkException -> RegistroIncidenteTipo.SIN_INTERNET
                is FirebaseFirestoreException -> {
                    if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) RegistroIncidenteTipo.SIN_INTERNET
                    else RegistroIncidenteTipo.ERROR_DESCONOCIDO
                }
                else -> RegistroIncidenteTipo.ERROR_DESCONOCIDO
            }
            _state.update {
                it.copy(
                    cargando = false,
                    incidenteActual = mapearIncidente(incidente, customMensaje = errorMsg)
                )
            }
        } finally {
            enviando = false
        }
    }
}

internal suspend fun RegistroFarmaciaViewModel.guardarEnFirestoreConTransaccion(authUid: String) {
    val currentState = _state.value
    val db = FarmadonFirestore.db
    val rucLimpio = currentState.paso1.ruc.trim()
    val emailLimpio = currentState.paso1.email.trim().lowercase()
    val emailHash = emailLimpio.sha256()
    db.runTransaction { tx ->
        val farmaciaExistenteRef = FarmadonPaths.farmacias(db).document(rucLimpio)
        val farmaciaDoc = tx.get(farmaciaExistenteRef)
        if (farmaciaDoc.exists()) throw RucDuplicadoException()

        val solRef = AuthPaths.solicitudes(db).document(rucLimpio)
        val plan = currentState.paso2.planSeleccionado ?: throw PlanNoDisponibleException()
        val planRef = EcosistemaPaths.planes(db).document(plan.id)
        val solDoc = tx.get(solRef)
        // Misma guarda que el FILTRO 2 (fuente única ESTADOS_SOLICITUD_ACTIVA):
        // los tres estados vivos bloquean el reenvío — un pisón borraría la
        // custodia del agente. "rechazada" pasa: reintento legítimo que
        // conserva la línea de tiempo de abajo.
        if (solDoc.exists() && solDoc.getString("estado") in ESTADOS_SOLICITUD_ACTIVA) {
            throw RucDuplicadoEnColaException()
        }
        // Reintento tras rechazo: el doc se rehace limpio, pero la línea de
        // tiempo ANTERIOR se conserva y se sella el reintento. BrixoPanel así
        // ve el contexto completo (por qué fue rechazado antes) al revisarlo.
        val lineaTiempoPrevia: List<*> = if (solDoc.exists()) {
            (solDoc.get("lineaTiempo") as? List<*>) ?: emptyList<Any>()
        } else emptyList<Any>()
        val planDoc = tx.get(planRef)
        if (!planDoc.exists() || planDoc.getBoolean("activo") == false || planDoc.getBoolean("eliminado") == true) {
            throw PlanNoDisponibleException()
        }
        val solicitudData = mapOf(
            "id" to rucLimpio,
            "nombreFarmacia" to currentState.paso1.nombreFarmacia.trim(),
            "dueno" to currentState.paso1.dueno.trim(),
            "usuario" to emailLimpio,
            "email" to emailLimpio,
            "ruc" to rucLimpio,
            "uid" to authUid,
            "telefono" to currentState.paso1.telefono.trim(),
            "direccion" to currentState.paso1.direccion.trim(),
            "ubicacionGeo" to if (currentState.paso1.latitud != null && currentState.paso1.longitud != null) {
                GeoPoint(currentState.paso1.latitud, currentState.paso1.longitud)
            } else null,
            "planId" to plan.id,
            "planNombre" to plan.nombre,
            "precioOfrecido" to plan.precioMensual,
            "pais" to currentState.paso1.paisIso,
            "monedaOperativa" to currentState.paso1.monedaIso,
            "simboloMoneda" to currentState.paso1.monedaSimbolo,
            "orgId" to com.app.administradorfarmadon.organizacion.datos.OrgId.desdeIdEmpresarial(rucLimpio),
            "estado" to "pendiente",
            "asignadoA" to "",
            "abierto" to false,
            "correccionDe" to "",
            "correccionSolicitada" to false,
            "registroCompleto" to true,
            "emailHash" to emailHash,
            "fechaSolicitud" to FieldValue.serverTimestamp(),
            "fechaCreacion" to FieldValue.serverTimestamp(),
            "version" to 1
        )
        // Historial previo conservado + sello del reintento (si venía de un rechazo).
        if (lineaTiempoPrevia.isNotEmpty()) {
            val lineaTiempoFinal = lineaTiempoPrevia + mapOf(
                "tipo" to "REINTENTO_DESPUES_RECHAZO",
                "agente" to authUid,
                "ts" to com.google.firebase.Timestamp(Date()),
                "detalle" to "El postulante volvió a enviar la solicitud tras un rechazo previo."
            )
            tx.set(solRef, solicitudData + mapOf("lineaTiempo" to lineaTiempoFinal))
        } else {
            tx.set(solRef, solicitudData)
        }
        val historialRef = AuthPaths.historialTiempoSolicitudes(db).document()
        val historialData = mapOf(
            "entidadId" to rucLimpio,
            "accion" to "SOLICITUD_CREADA",
            "actor" to "HUMAN_CLIENTE",
            "actorEmail" to emailLimpio,
            "actorRol" to "HUMAN_CLIENTE",
            "timestamp" to FieldValue.serverTimestamp(),
            "motivo" to "",
            "antes" to null,
            "despues" to null
        )
        tx.set(historialRef, historialData)
        tx.set(AuthPaths.usuariosFarmacia(db).document(authUid), mapOf(
            "uid" to authUid,
            "email" to emailLimpio,
            "nombre" to currentState.paso1.nombreFarmacia.trim(),
            "rol" to "Administrador",
            "acceso" to false,
            "clienteId" to "",
            "clienteIds" to listOf<String>(),
            "creadoEn" to FieldValue.serverTimestamp()
        ))
    }.await()
}
