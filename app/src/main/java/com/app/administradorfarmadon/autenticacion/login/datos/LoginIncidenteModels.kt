package com.app.administradorfarmadon.autenticacion.login.datos

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Tipos de incidentes que pueden ocurrir durante el login.
 */
enum class LoginIncidenteTipo {
    CAMPOS_VACIOS,
    CREDENCIALES_INCORRECTAS,
    USUARIO_NO_REGISTRADO,
    SOLICITUD_PENDIENTE,
    SOLICITUD_EN_REVISION,
    SOLICITUD_RECHAZADA,
    USUARIO_BANEADO,
    ACCESO_BLOQUEADO,
    SIN_INTERNET,
    ERROR_FIREBASE_AUTH,
    ERROR_BASE_DATOS,
    ERROR_VALIDACION_SESION,
    SERVICIO_NO_DISPONIBLE,
    DEMASIADOS_INTENTOS,
    ERROR_DESCONOCIDO
}

/**
 * Acciones posibles ante un incidente de login.
 */
sealed class LoginIncidenteAccion {
    data object Reintentar : LoginIncidenteAccion()
    data object IrARegistro : LoginIncidenteAccion()
    data object ContactarSoporte : LoginIncidenteAccion()
    data object LimpiarCampos : LoginIncidenteAccion()
    data object Descartar : LoginIncidenteAccion()
    data object CorregirSolicitud : LoginIncidenteAccion()
}

/**
 * Modelo para representar el incidente en la UI.
 */
data class LoginIncidenteUi(
    val tipo: LoginIncidenteTipo,
    val titulo: String,
    val mensaje: String,
    val icono: ImageVector,
    val accionPrincipal: LoginIncidenteAccion,
    val accionSecundaria: LoginIncidenteAccion? = LoginIncidenteAccion.Descartar
)

/**
 * Constantes de mensajes en español.
 */
object LoginMensajes {
    const val CAMPOS_VACIOS_TITULO = "Faltan datos"
    const val CAMPOS_VACIOS_MENSAJE = "Por favor ingresa tu usuario y contraseña para continuar."
    
    const val CREDENCIALES_INCORRECTAS_TITULO = "Credenciales incorrectas"
    const val CREDENCIALES_INCORRECTAS_MENSAJE = "El usuario o contraseña que ingresaste no son correctos. Verifica e intenta nuevamente."
    
    const val USUARIO_NO_REGISTRADO_TITULO = "Usuario no registrado"
    const val USUARIO_NO_REGISTRADO_MENSAJE = "Este usuario no existe en el sistema. Si deseas solicitar acceso, ve a la sección de registro."
    
    const val SOLICITUD_PENDIENTE_TITULO = "Solicitud enviada"
    const val SOLICITUD_PENDIENTE_FALLBACK = "Tu solicitud fue recibida. La central Brixo la revisará pronto."
    
    const val SOLICITUD_EN_REVISION_TITULO = "Solicitud en revisión"
    const val SOLICITUD_EN_REVISION_FALLBACK = "Tu solicitud está siendo revisada por la central Brixo."
    
    const val SOLICITUD_RECHAZADA_TITULO = "Solicitud rechazada"
    const val SOLICITUD_RECHAZADA_FALLBACK = "Tu solicitud no fue aprobada. Contacta a soporte."

    const val USUARIO_BANEADO_TITULO = "Acceso denegado"
    const val USUARIO_BANEADO_FALLBACK = "Tu acceso está restringido. Contacta a soporte para más información."
    
    const val ACCESO_BLOQUEADO_TITULO = "Acceso restringido"
    const val ACCESO_BLOQUEADO_MENSAJE = "Tu acceso fue desactivado por un administrador. Contacta a soporte para más información."
    
    const val SIN_INTERNET_TITULO = "Sin conexión"
    const val SIN_INTERNET_MENSAJE = "No pudimos conectarnos al servidor. Revisa tu internet e intenta de nuevo."
    
    const val ERROR_FIREBASE_AUTH_TITULO = "Error de autenticación"
    const val ERROR_FIREBASE_AUTH_MENSAJE = "Ocurrió un error al verificar tus credenciales. Por favor, intenta nuevamente."
    
    const val ERROR_BASE_DATOS_TITULO = "Error de base de datos"
    const val ERROR_BASE_DATOS_MENSAJE = "Hubo un problema al consultar tus datos de acceso. Intenta de nuevo en unos minutos."
    
    const val SERVICIO_NO_DISPONIBLE_TITULO = "Servicio no disponible"
    const val SERVICIO_NO_DISPONIBLE_MENSAJE = "El servicio está temporalmente no disponible. Intenta nuevamente en unos segundos."
    
    const val DEMASIADOS_INTENTOS_TITULO = "Demasiados intentos"
    const val DEMASIADOS_INTENTOS_MENSAJE = "Hiciste demasiados intentos fallidos. Espera un momento e intenta de nuevo."
    
    const val ERROR_DESCONOCIDO_TITULO = "Error inesperado"
    const val ERROR_DESCONOCIDO_MENSAJE = "Ocurrió un problema inesperado durante el login. Si el problema persiste, contacta a soporte con el código de error."
}
