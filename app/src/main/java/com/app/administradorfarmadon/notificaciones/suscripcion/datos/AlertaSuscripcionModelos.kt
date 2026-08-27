package com.app.administradorfarmadon.notificaciones.suscripcion.datos

import androidx.compose.runtime.Immutable

enum class TipoAlertaSuscripcion {
    BIENVENIDA_INICIAL,
    PAGO_EXITOSO,
    CORTESIA_OTORGADA,
    FIN_PRUEBA,
    CAMBIO_PLAN,
    AVISO_PREVENTIVO_48H,
    AVISO_PREVENTIVO_7D,
    COMPROBANTE_OBSERVADO,
    COMPROBANTE_EN_REVISION
}


@Immutable
data class AlertaSuscripcionItem(
    val id: String = "",
    val tipo: TipoAlertaSuscripcion = TipoAlertaSuscripcion.PAGO_EXITOSO,
    val titulo: String = "",
    val mensaje: String = "",
    val fechaReferencia: String = "",
    val esPersistente: Boolean = false,
    val autoHideSegundos: Int = 6,
    val esSoloAdminODueno: Boolean = false
)
