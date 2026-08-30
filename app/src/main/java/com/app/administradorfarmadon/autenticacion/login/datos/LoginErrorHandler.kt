package com.app.administradorfarmadon.autenticacion.login.datos

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException

object LoginErrorHandler {

    fun mapearError(e: Exception?): LoginIncidenteTipo {
        return when (e) {
            is FirebaseNetworkException -> LoginIncidenteTipo.SIN_INTERNET
            is FirebaseAuthInvalidUserException -> LoginIncidenteTipo.CREDENCIALES_INCORRECTAS
            is FirebaseAuthInvalidCredentialsException -> LoginIncidenteTipo.CREDENCIALES_INCORRECTAS
            is ErrorVerificacionListaNegra -> LoginIncidenteTipo.ERROR_BASE_DATOS
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.UNAVAILABLE, 
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> LoginIncidenteTipo.ERROR_BASE_DATOS
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> LoginIncidenteTipo.ACCESO_BLOQUEADO
                    FirebaseFirestoreException.Code.NOT_FOUND -> LoginIncidenteTipo.USUARIO_NO_REGISTRADO
                    else -> LoginIncidenteTipo.ERROR_BASE_DATOS
                }
            }
            is FirebaseAuthException -> {
                when (e.errorCode) {
                    "USER_DISABLED" -> LoginIncidenteTipo.ACCESO_BLOQUEADO
                    "TOO_MANY_ATTEMPTS_TRY_LATER" -> LoginIncidenteTipo.DEMASIADOS_INTENTOS
                    "INVALID_EMAIL" -> LoginIncidenteTipo.CREDENCIALES_INCORRECTAS
                    "OPERATION_NOT_ALLOWED" -> LoginIncidenteTipo.ERROR_FIREBASE_AUTH
                    "WEAK_PASSWORD" -> LoginIncidenteTipo.CREDENCIALES_INCORRECTAS
                    "CREDENTIAL_TOO_OLD_LOGIN_AGAIN" -> LoginIncidenteTipo.ERROR_FIREBASE_AUTH
                    "EMAIL_ALREADY_IN_USE" -> LoginIncidenteTipo.CREDENCIALES_INCORRECTAS
                    "UNAVAILABLE" -> LoginIncidenteTipo.ERROR_FIREBASE_AUTH
                    "INTERNAL" -> LoginIncidenteTipo.ERROR_FIREBASE_AUTH
                    else -> LoginIncidenteTipo.ERROR_DESCONOCIDO
                }
            }
            else -> {
                val msg = e?.message?.lowercase() ?: ""
                when {
                    msg.contains("network") || msg.contains("connection") -> LoginIncidenteTipo.SIN_INTERNET
                    msg.contains("permission_denied") -> LoginIncidenteTipo.ACCESO_BLOQUEADO
                    msg.contains("timeout") || msg.contains("deadline") -> LoginIncidenteTipo.ERROR_BASE_DATOS
                    msg.contains("unavailable") || msg.contains("service") -> LoginIncidenteTipo.ERROR_FIREBASE_AUTH
                    else -> LoginIncidenteTipo.ERROR_DESCONOCIDO
                }
            }
        }
    }

    fun crearIncidente(
        tipo: LoginIncidenteTipo,
        customMensaje: String? = null,
        errorOriginal: Exception? = null
    ): LoginIncidenteUi {
        return when (tipo) {
            LoginIncidenteTipo.CAMPOS_VACIOS -> LoginIncidenteUi(
                tipo = tipo,
                titulo = LoginMensajes.CAMPOS_VACIOS_TITULO,
                mensaje = LoginMensajes.CAMPOS_VACIOS_MENSAJE,
                icono = Icons.Default.Info,
                accionPrincipal = LoginIncidenteAccion.Descartar
            )
            LoginIncidenteTipo.CREDENCIALES_INCORRECTAS -> LoginIncidenteUi(
                tipo = tipo,
                titulo = LoginMensajes.CREDENCIALES_INCORRECTAS_TITULO,
                mensaje = LoginMensajes.CREDENCIALES_INCORRECTAS_MENSAJE,
                icono = Icons.Default.Lock,
                accionPrincipal = LoginIncidenteAccion.Reintentar,
                accionSecundaria = LoginIncidenteAccion.LimpiarCampos
            )
            LoginIncidenteTipo.USUARIO_NO_REGISTRADO -> LoginIncidenteUi(
                tipo = tipo,
                titulo = LoginMensajes.USUARIO_NO_REGISTRADO_TITULO,
                mensaje = LoginMensajes.USUARIO_NO_REGISTRADO_MENSAJE,
                icono = Icons.Default.PersonSearch,
                accionPrincipal = LoginIncidenteAccion.IrARegistro
            )
            LoginIncidenteTipo.SOLICITUD_PENDIENTE -> LoginIncidenteUi(
                tipo = tipo,
                titulo = LoginMensajes.SOLICITUD_PENDIENTE_TITULO,
                mensaje = customMensaje ?: LoginMensajes.SOLICITUD_PENDIENTE_FALLBACK,
                icono = Icons.Default.HourglassEmpty,
                accionPrincipal = LoginIncidenteAccion.ContactarSoporte
            )
            LoginIncidenteTipo.SOLICITUD_EN_REVISION -> LoginIncidenteUi(
                tipo = tipo,
                titulo = LoginMensajes.SOLICITUD_EN_REVISION_TITULO,
                mensaje = customMensaje ?: LoginMensajes.SOLICITUD_EN_REVISION_FALLBACK,
                icono = Icons.Default.FactCheck,
                accionPrincipal = LoginIncidenteAccion.ContactarSoporte
            )
            LoginIncidenteTipo.SOLICITUD_RECHAZADA -> LoginIncidenteUi(
                tipo = tipo,
                titulo = LoginMensajes.SOLICITUD_RECHAZADA_TITULO,
                mensaje = customMensaje ?: LoginMensajes.SOLICITUD_RECHAZADA_FALLBACK,
                icono = Icons.Default.AssignmentReturn,
                accionPrincipal = LoginIncidenteAccion.CorregirSolicitud,
                accionSecundaria = LoginIncidenteAccion.ContactarSoporte
            )
            LoginIncidenteTipo.USUARIO_BANEADO -> LoginIncidenteUi(
                tipo = tipo,
                titulo = LoginMensajes.USUARIO_BANEADO_TITULO,
                mensaje = customMensaje ?: LoginMensajes.USUARIO_BANEADO_FALLBACK,
                icono = Icons.Default.GppBad,
                accionPrincipal = LoginIncidenteAccion.ContactarSoporte,
                accionSecundaria = null
            )
            LoginIncidenteTipo.ACCESO_BLOQUEADO -> LoginIncidenteUi(
                tipo = tipo,
                titulo = LoginMensajes.ACCESO_BLOQUEADO_TITULO,
                mensaje = LoginMensajes.ACCESO_BLOQUEADO_MENSAJE,
                icono = Icons.Default.Block,
                accionPrincipal = LoginIncidenteAccion.ContactarSoporte
            )
            LoginIncidenteTipo.ERROR_BASE_DATOS -> LoginIncidenteUi(
                tipo = tipo,
                titulo = LoginMensajes.ERROR_BASE_DATOS_TITULO,
                mensaje = LoginMensajes.ERROR_BASE_DATOS_MENSAJE,
                icono = Icons.Default.Storage,
                accionPrincipal = LoginIncidenteAccion.Reintentar
            )
            LoginIncidenteTipo.SIN_INTERNET -> LoginIncidenteUi(
                tipo = tipo,
                titulo = LoginMensajes.SIN_INTERNET_TITULO,
                mensaje = LoginMensajes.SIN_INTERNET_MENSAJE,
                icono = Icons.Default.WifiOff,
                accionPrincipal = LoginIncidenteAccion.Reintentar
            )
            LoginIncidenteTipo.SERVICIO_NO_DISPONIBLE -> LoginIncidenteUi(
                tipo = tipo,
                titulo = LoginMensajes.SERVICIO_NO_DISPONIBLE_TITULO,
                mensaje = LoginMensajes.SERVICIO_NO_DISPONIBLE_MENSAJE,
                icono = Icons.Default.CloudOff,
                accionPrincipal = LoginIncidenteAccion.Reintentar
            )
            LoginIncidenteTipo.DEMASIADOS_INTENTOS -> LoginIncidenteUi(
                tipo = tipo,
                titulo = LoginMensajes.DEMASIADOS_INTENTOS_TITULO,
                mensaje = LoginMensajes.DEMASIADOS_INTENTOS_MENSAJE,
                icono = Icons.Default.Timer,
                accionPrincipal = LoginIncidenteAccion.Reintentar
            )
            LoginIncidenteTipo.ERROR_DESCONOCIDO -> {
                val codigo = errorOriginal?.let { err ->
                    when (err) {
                        is FirebaseAuthException -> "FirebaseAuth #${err.errorCode}"
                        else -> "${err::class.simpleName}: ${err.message?.take(100)}"
                    }
                } ?: "Código no disponible"

                LoginIncidenteUi(
                    tipo = tipo,
                    titulo = LoginMensajes.ERROR_DESCONOCIDO_TITULO,
                    mensaje = "Ocurrió un problema inesperado.\n\n" +
                            "Código: $codigo\n\n" +
                            "Si el problema persiste, contacta a soporte con este código.",
                    icono = Icons.Default.Error,
                    accionPrincipal = LoginIncidenteAccion.Reintentar,
                    accionSecundaria = LoginIncidenteAccion.ContactarSoporte
                )
            }
            else -> LoginIncidenteUi(
                tipo = tipo,
                titulo = LoginMensajes.ERROR_DESCONOCIDO_TITULO,
                mensaje = LoginMensajes.ERROR_DESCONOCIDO_MENSAJE,
                icono = Icons.Default.Error,
                accionPrincipal = LoginIncidenteAccion.Reintentar
            )
        }
    }
}
