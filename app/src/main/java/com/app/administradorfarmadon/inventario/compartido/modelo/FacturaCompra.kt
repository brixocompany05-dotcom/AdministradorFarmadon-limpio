package com.app.administradorfarmadon.inventario.compartido.modelo

/**
 * Representa un abono parcial o pago registrado contra una factura comercial de compra.
 */
data class AbonoFactura(
    val id: String = "",
    val fechaLegible: String = "",
    val fechaMs: Long = 0L,
    val monto: Double = 0.0,
    val metodoPago: String = "Transferencia", // "Transferencia", "Efectivo", "Yape/Plin", "Cheque"
    val numeroOperacion: String = "",
    val usuarioNombre: String = "",
    val usuarioEmail: String = "",
    val notas: String = ""
)

/**
 * Modelo Contable de Factura General de Compra Multi-Producto (Cuentas por Pagar).
 * Representa el documento comercial de compra emitido por una droguería o distribuidor.
 */
data class FacturaCompra(
    val id: String = "",
    val numeroFactura: String = "",
    val proveedorId: String = "",
    val proveedorNombre: String = "",
    val rucProveedor: String = "",
    val condicionPago: String = "Contado",
    val fechaVencimientoPago: String = "",
    val estadoPago: String = "PENDIENTE", // "PENDIENTE" | "ABONADO_PARCIAL" | "PAGADA" | "PAGADO" | "ANULADA"
    val montoTotal: Double = 0.0,
    val montoAcumulado: Double = 0.0,
    val montoPagado: Double = 0.0,
    val abonos: List<AbonoFactura> = emptyList(),
    val items: List<ItemFacturaCompra> = emptyList(),
    val usuarioRegistroEmail: String = "",
    val notas: String = "",
    val fechaRegistro: String = ""
) {
    val esContado: Boolean
        get() = condicionPago.contains("Contado", ignoreCase = true)

    val totalEfectivo: Double
        get() = if (items.isNotEmpty()) items.sumOf { it.costoTotal } else if (montoTotal > 0) montoTotal else montoAcumulado

    val totalAbonadoReal: Double
        get() = if (abonos.isNotEmpty()) abonos.sumOf { it.monto } else montoPagado

    val saldoPendienteReal: Double
        get() = if (esContado) 0.0 else (totalEfectivo - totalAbonadoReal).coerceAtLeast(0.0)

    val esTotalmentePagada: Boolean
        get() = esContado || saldoPendienteReal <= 0.01 || estadoPago.equals("PAGADA", ignoreCase = true) || estadoPago.equals("PAGADO", ignoreCase = true)

    val saldoRestantePorAsignar: Double
        get() = (montoTotal - (if (montoAcumulado > 0) montoAcumulado else items.sumOf { it.costoTotal })).coerceAtLeast(0.0)

    val porcentajeAsignado: Float
        get() = if (montoTotal > 0) {
            val acum = if (montoAcumulado > 0) montoAcumulado else items.sumOf { it.costoTotal }
            (acum / montoTotal).toFloat().coerceIn(0f, 1f)
        } else 0f
}

data class ItemFacturaCompra(
    val productoId: String = "",
    val productoNombre: String = "",
    val empaque: String = "Caja",
    val loteNumero: String = "",
    val vencimiento: String = "",
    val cantidadTotal: Double = 0.0,
    val cantidadComprada: Double = 0.0,
    val bonificacionGratis: Double = 0.0,
    val costoTotal: Double = 0.0,
    val costoUnitario: Double = 0.0
)
