package com.app.administradorfarmadon.autenticacion.login.logica

import com.app.administradorfarmadon.appconexioninternet.NetworkHealthMonitor
import com.app.administradorfarmadon.appconexioninternet.NetworkStatus
import com.app.administradorfarmadon.autenticacion.login.datos.LoginIncidenteUi

enum class LoginScreenState {
    LOGIN,
    ACCESO_RESTRINGIDO
}

/**
 * Estado REAL de la solicitud del usuario, leído en vivo del servidor.
 */
data class SolicitudEstadoUi(
    val estado: String = "pendiente",
    val mensajeBrixo: String = "",
    val uid: String = "",
    val correccionSolicitada: Boolean = false,
    val fechaCorreccion: String = ""
) {
    val etiqueta: String
        get() = when {
            estado == "aprobada" -> "APROBADA"
            estado == "en_revision" -> "EN REVISIÓN"
            correccionSolicitada && fechaCorreccion.isBlank() -> "CORRECCIÓN SOLICITADA"
            correccionSolicitada && fechaCorreccion.isNotBlank() -> "CORRECCIÓN ENVIADA"
            estado == "rechazada" -> "RECHAZADA"
            else -> "PENDIENTE"
        }
}

data class LoginUiState(
    val estadoPantalla: LoginScreenState = LoginScreenState.LOGIN,
    val sesionInicialResuelta: Boolean = false,
    val cargando: Boolean = false,
    val usuario: String = "",
    val contrasena: String = "",
    val mensajeRestringido: String? = null,
    val mensajeDialogo: Pair<String, String>? = null,
    val loginExitoso: Boolean = false,
    val usuarioAutenticadoEmail: String? = null,
    val loginExitosoReciente: Boolean = false,
    val resolucionSesionFallida: Boolean = false,
    val errorResolucionSesion: String? = null,
    val estadoRed: NetworkStatus = NetworkHealthMonitor.status.value,
    val incidente: LoginIncidenteUi? = null,
    val estadoSolicitud: SolicitudEstadoUi? = null
)
