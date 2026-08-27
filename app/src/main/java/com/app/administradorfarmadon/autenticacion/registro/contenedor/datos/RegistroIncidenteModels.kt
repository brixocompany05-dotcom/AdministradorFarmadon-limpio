package com.app.administradorfarmadon.autenticacion.registro.contenedor.datos

import androidx.compose.ui.graphics.vector.ImageVector

class PlanNoDisponibleException : Exception("Plan no disponible")
class RucDuplicadoException : Exception("RUC duplicado")
class ConflictoVersionException(msg: String = "La solicitud cambió mientras editabas") : Exception(msg)
class RucDuplicadoEnColaException : Exception("RUC duplicado en cola de aprobación")

/**
 * Tipos de incidentes que pueden ocurrir durante el registro.
 */
enum class RegistroIncidenteTipo {
    CORREO_EXISTENTE,
    RUC_EXISTENTE,
    SIN_INTERNET,
    PLAN_NO_DISPONIBLE,
    ERROR_BASE_DATOS,
    ERROR_DESCONOCIDO,
    FALLO_INTEGRIDAD,
    USUARIO_BANEADO,
    DATOS_IDENTICOS,
    CORRECCION_CONFLICTO,
    RUC_DUPLICADO_EN_COLA
}

/**
 * Acciones posibles que el usuario puede tomar ante un incidente.
 */
sealed class RegistroIncidenteAccion {
    data object ContactarSoporte : RegistroIncidenteAccion()
    data object IrALogin : RegistroIncidenteAccion()
    data object Reintentar : RegistroIncidenteAccion()
    data object ElegirOtroPlan : RegistroIncidenteAccion()
    data object VolverYCorregir : RegistroIncidenteAccion()
    data object Descartar : RegistroIncidenteAccion()
    data object RecargarSolicitud : RegistroIncidenteAccion()
}

/**
 * Modelo completo de un incidente de registro para ser consumido por la UI.
 */
data class RegistroIncidenteUi(
    val tipo: RegistroIncidenteTipo,
    val titulo: String,
    val mensaje: String,
    val icono: ImageVector,
    val accionPrincipal: RegistroIncidenteAccion,
    val accionSecundaria: RegistroIncidenteAccion? = RegistroIncidenteAccion.Descartar
)
