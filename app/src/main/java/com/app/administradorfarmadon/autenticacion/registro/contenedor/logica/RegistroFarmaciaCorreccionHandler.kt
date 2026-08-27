package com.app.administradorfarmadon.autenticacion.registro.contenedor.logica
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.datos.AuthPaths
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.ConflictoVersionException
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.PlanNoDisponibleException
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RegistroIncidenteTipo
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RucDuplicadoEnColaException
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RucDuplicadoException
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.datos.Paso1UiState
import com.app.administradorfarmadon.autenticacion.registro.paso2_plan.datos.Paso2UiState
import com.app.administradorfarmadon.base_datos.PlanSuscripcion
import com.app.administradorfarmadon.organizacion.datos.CatalogoPaises
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

internal fun RegistroFarmaciaViewModel.precargarSolicitudCorreccionImpl(uid: String) {
    _state.update {
        it.copy(
            precargandoCorreccion = true,
            cargando = false,
            esCorreccion = true,
            uidCorreccion = uid,
            paso1 = it.paso1.copy(erroresCampos = emptyMap()),
            incidenteActual = null
        )
    }
    viewModelScope.launch {
        try {
            val db = FarmadonFirestore.db
            // El documento de solicitud usa el RUC como id, pero lleva el campo "uid"
            // (auth uid). El camino de CORRECCIÓN puede recibir el RUC (desde "consultar
            // estado") o el auth uid (desde el login). Se busca por id del documento y,
            // si no existe, por el campo "uid", para cubrir ambos casos sin perder al usuario.
            var document = AuthPaths.solicitudes(db).document(uid).get().await()
            if (document == null || !document.exists()) {
                document = AuthPaths.solicitudes(db)
                    .whereEqualTo("uid", uid)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
            }
            if (document == null || !document.exists()) {
                _state.update {
                    it.copy(
                        precargandoCorreccion = false,
                        cargando = false,
                        incidenteActual = mapearIncidente(RegistroIncidenteTipo.ERROR_DESCONOCIDO)
                    )
                }
                return@launch
            }
            // Observación VIVA = pedida y aún sin respuesta del cliente.
            // camposACorregir NO se borra al enviarse la corrección (BrixoPanel
            // lo necesita para la tabla antes→después), así que solo cuenta como
            // pendiente mientras correccionRecibidaAt sea null. Sin este sello,
            // el cliente podía reentrar al flujo de corrección sin nada pendiente.
            val correccionYaRespondida = document.get("correccionRecibidaAt") != null
            val tieneObservacion = (document.getBoolean("correccionSolicitada") == true ||
                    document.getString("estado") == "observada" ||
                    (document.get("camposACorregir") as? List<*>)?.isNotEmpty() == true) &&
                    !correccionYaRespondida
            if (!tieneObservacion) {
                // Mensaje VERAZ según el motivo real del bloqueo. Reutilizar
                // ERROR_DESCONOCIDO sin contexto diría "error, reintenta" a
                // alguien que simplemente ya envió su corrección (mentira + acción inútil).
                val mensajeBloqueo = if (correccionYaRespondida) {
                    "Ya enviamos tu corrección a la central de BRIXO. Está en cola de revisión; te avisaremos cuando haya una respuesta."
                } else {
                    "No encontramos una corrección pendiente para tu solicitud."
                }
                _state.update {
                    it.copy(
                        precargandoCorreccion = false,
                        cargando = false,
                        incidenteActual = mapearIncidente(
                            RegistroIncidenteTipo.ERROR_DESCONOCIDO,
                            customMensaje = mensajeBloqueo
                        )
                    )
                }
                return@launch
            }
            val nombreFarmacia = document.getString("nombreFarmacia") ?: ""
            val dueno = document.getString("dueno") ?: ""
            val email = document.getString("email") ?: ""
            val telefono = document.getString("telefono") ?: ""
            val ruc = document.getString("ruc") ?: ""
            val direccion = document.getString("direccion") ?: ""
            val geo = document.getGeoPoint("ubicacionGeo")
            val latitud = geo?.latitude
            val longitud = geo?.longitude
            val planId = document.getString("planId") ?: ""
            val planNombre = document.getString("planNombre") ?: ""
            // País y moneda originales del expediente: la corrección debe operar
            // con el catálogo del MISMO país con el que el cliente postuló.
            val paisIso = (document.getString("pais") ?: "").trim().uppercase()
            val monedaIso = document.getString("monedaOperativa") ?: ""
            val monedaSimbolo = document.getString("simboloMoneda") ?: ""
            val camposACorregir = (document.get("camposACorregir") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

            val planesRechazados = (document.get("planesRechazados") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val sugerirCambioPlan = document.getBoolean("sugerirCambioPlan") ?: false
            val accionSugerida = document.getString("accionSugerida")
            val motivoRechazo = document.getString("mensajeBrixo")
                ?: document.getString("motivoObservacion")
                ?: document.getString("motivoRechazo")
            val version = document.getLong("version") ?: 1L

            val snapshotOriginal = mapOf(
                "nombreFarmacia" to nombreFarmacia,
                "dueno" to dueno,
                "email" to email,
                "telefono" to telefono,
                "ruc" to ruc,
                "direccion" to direccion,
                "latitud" to latitud,
                "longitud" to longitud,
                "planId" to planId,
                // El país también es contenido del expediente: corregirlo solo
                // a él ES un cambio real (no un envío "idéntico").
                "pais" to paisIso
            )
            _state.update { currentState ->
                val p1 = Paso1UiState(
                    nombreFarmacia = nombreFarmacia,
                    dueno = dueno,
                    email = email,
                    ruc = ruc,
                    direccion = direccion,
                    latitud = latitud,
                    longitud = longitud,
                    // País del expediente tal cual (legados sin país → vacío:
                    // el formulario queda bloqueado hasta que elija). Si el
                    // país existe pero faltan monedas, se derivan del catálogo.
                    paisIso = paisIso,
                    monedaIso = monedaIso.ifBlank { CatalogoPaises.monedaDe(paisIso).first },
                    monedaSimbolo = monedaSimbolo.ifBlank { CatalogoPaises.monedaDe(paisIso).second },
                    // Legados guardaban "+51…" pegado: el prefijo ahora es visual.
                    telefono = CatalogoPaises.telefonoSinPrefijo(paisIso, telefono)
                )
                val p2 = Paso2UiState(
                    planSeleccionado = PlanSuscripcion(id = planId, nombre = planNombre)
                )
                val updatedState = currentState.copy(
                    pasoActual = 1,
                    paso1 = p1,
                    paso2 = p2,
                    esCorreccion = true,
                    uidCorreccion = uid,
                    camposACorregir = camposACorregir,
                    planesRechazados = planesRechazados,
                    sugerirCambioPlan = sugerirCambioPlan,
                    accionSugerida = accionSugerida,
                    motivoRechazo = motivoRechazo,
                    originalSnapshot = snapshotOriginal,
                    versionOriginal = version,
                    cargando = false,
                    precargandoCorreccion = false,
                    requestIdCorreccion = UUID.randomUUID().toString()
                )
                actualizarPlanesDisponibles(updatedState)
            }
        } catch (error: Exception) {
            Log.e("FARMADON_REGISTRO", "Error al precargar solicitud rechazada: ${error.message}", error)
            _state.update {
                it.copy(
                    cargando = false,
                    precargandoCorreccion = false,
                    incidenteActual = mapearIncidente(RegistroIncidenteTipo.ERROR_DESCONOCIDO)
                )
            }
        }
    }
}

internal fun RegistroFarmaciaViewModel.enviarCorreccionImpl() {
    val s = _state.value
    val uid = s.uidCorreccion ?: return
    if (enviando || s.cargando) return
    val snap = s.originalSnapshot
    val planOriginal = snap["planId"] as? String ?: ""
    val planActual = s.paso2.planSeleccionado?.id ?: ""
    val isIdentical = s.paso1.nombreFarmacia.trim() == (snap["nombreFarmacia"] as? String ?: "") &&

            s.paso1.dueno.trim() == (snap["dueno"] as? String ?: "") &&
            s.paso1.telefono.trim() == (snap["telefono"] as? String ?: "") &&
            s.paso1.ruc.trim() == (snap["ruc"] as? String ?: "") &&
            s.paso1.direccion.trim() == (snap["direccion"] as? String ?: "") &&
            s.paso1.latitud == (snap["latitud"] as? Double) &&
            s.paso1.longitud == (snap["longitud"] as? Double) &&
            // El plan también cuenta como cambio real: bloquear el envío cuando
            // lo único que corrigió fue el plan sería una mentira ("no cambiaste
            // nada") para alguien que sí cambió algo visible.
            (planOriginal.isBlank() || planActual == planOriginal || planActual.isBlank()) &&
            ((snap["pais"] as? String ?: "").trim().uppercase() == s.paso1.paisIso.trim().uppercase()) &&
            s.paso1.notaAclaratoria.trim().isEmpty()
    if (isIdentical) {
        _state.update { it.copy(incidenteActual = mapearIncidente(RegistroIncidenteTipo.DATOS_IDENTICOS)) }
        return
    }

    val originalRuc = snap["ruc"] as? String ?: ""
    if (originalRuc.isNotBlank() && s.paso1.ruc.trim() != originalRuc) {
        _state.update {
            it.copy(
                incidenteActual = mapearIncidente(
                    RegistroIncidenteTipo.FALLO_INTEGRIDAD,
                    customMensaje = "El RUC del expediente no puede modificarse en corrección. Para rectificar el documento de identidad, contacta a soporte de BRIXO."
                )
            )
        }
        return
    }

    _state.update {
        it.copy(
            cargando = true,
            progresoEnvio = 0.15f,
            mensajeProgreso = "Preparando corrección...",
            incidenteActual = null
        )
    }
    enviando = true
    val requestId = s.requestIdCorreccion ?: UUID.randomUUID().toString()
    viewModelScope.launch {
        try {
            _state.update { it.copy(progresoEnvio = 0.50f, mensajeProgreso = "Subiendo correcciones a BrixoPanel...") }
            enviarCorreccionUseCase.ejecutar(
                uid = uid,
                state = s.paso1,
                requestId = requestId,
                versionOriginal = s.versionOriginal,
                originalRuc = snap["ruc"] as? String ?: "",
                nuevoPlanId = s.paso2.planSeleccionado?.id ?: "",
                nuevoPlanNombre = s.paso2.planSeleccionado?.nombre ?: ""
            )
            _state.update { it.copy(progresoEnvio = 0.85f, mensajeProgreso = "Finalizando sesión del expediente...") }
            FirebaseAuth.getInstance().signOut()
            borrarBorrador()
            _state.update { it.copy(cargando = false, progresoEnvio = 1.0f, mensajeProgreso = "¡Corrección enviada!", exitoso = true) }
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.toString()
            Log.e("VIORA_CORRECCION", "Error al enviar corrección: $errorMsg", e)
            val incidente = when (e) {
                is ConflictoVersionException -> RegistroIncidenteTipo.CORRECCION_CONFLICTO
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
