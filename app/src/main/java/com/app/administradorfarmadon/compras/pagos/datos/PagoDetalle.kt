package com.app.administradorfarmadon.compras.pagos.datos

/**
 * Una porción de un abono pagada con un método concreto (pago mixto).
 * Cada porción es real: método + monto + (opcional) número de operación.
 * La suma de porciones de un abono SIEMPRE debe cuadrar con su monto total.
 */
data class PagoDetalle(
    val metodoPago: String = "",
    val monto: Double = 0.0,
    val numeroOperacion: String = ""
)
