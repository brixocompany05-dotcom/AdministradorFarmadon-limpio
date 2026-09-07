package com.app.administradorfarmadon.inventario.compartido.modelo

/**
 * Representa un abono parcial o pago registrado contra una factura comercial de compra.
 */
data class AbonoFactura(
    val id: String = "",
    val fechaLegible: String = "",
    val fechaMs: Long = 0L,
    val monto: Double = 0.0,
    val metodoPago: String = "", // Método REAL con el que se pagó; vacío = aún no se pagó (jamás un valor decorativo)
    val numeroOperacion: String = "",
    // PAGO MIXTO: un abono puede dividirse en varios métodos reales.
    // Ej: S/400 en Efectivo + S/400 por Yape. La suma de pagos SIEMPRE == monto.
    val pagos: List<com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle> = emptyList(),
    val usuarioNombre: String = "",
    val usuarioEmail: String = "",
    val notas: String = "",
    // ANULACIÓN CONSERVADORA: un abono jamás se borra; si se anula queda marcado
    // con quién, cuándo y motivo, y deja de contar para el saldo. El rastro permanece.
    val anulado: Boolean = false,
    val anuladoPorNombre: String = "",
    val anuladoPorEmail: String = "",
    val anuladoElLegible: String = "",
    val motivoAnulacion: String = ""
)

/**
 * Respuesta del dueño sobre la plata ya pagada de una factura anulada (R1/R3: la plata
 * jamás queda a medias). Se escribe SIEMPRE en la factura anulada cuando hubo pagos.
 */
data class RespuestaPlataAnulacion(
    val decision: String = "", // "SALDO_A_FAVOR" | "DEVOLUCION_RECIBIDA" | "PERDIDA"
    val monto: Double = 0.0,
    val metodoDevolucion: String = "",
    val referenciaDevolucion: String = "",
    val fechaLegible: String = "",
    val fechaMs: Long = 0L,
    val usuarioNombre: String = "",
    val usuarioEmail: String = ""
)

/**
 * Nota de crédito / ajuste del total de una factura (mercadería facturada que nunca llegó,
 * o que llegó incompleta). Historial append-only: se reduce el total del papel SOLO con
 * un documento real del proveedor (número, motivo, quién, cuándo). Jamás en silencio.
 */
data class AjusteFactura(
    val id: String = "",
    val tipo: String = "NOTA_CREDITO",
    val numeroDocumento: String = "",
    val monto: Double = 0.0,
    val motivo: String = "",
    val fechaLegible: String = "",
    val fechaMs: Long = 0L,
    val usuarioNombre: String = "",
    val usuarioEmail: String = ""
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
    val fechaRegistro: String = "",
    // ── ESTRUCTURA DE COMPROBANTE DE COMPRA (SUNAT / CONTABILIDAD DE PAPEL) ──
    val tipoDoc: String = "FACTURA", // "FACTURA" | "BOLETA" | "GUIA" | "S/C"
    val serie: String = "",
    val correlativo: String = "",
    val montoBase: Double = 0.0,
    val montoIgv: Double = 0.0,
    val fechaEmision: String = "",
    // ── AJUSTES POR NOTA DE CRÉDITO: el papel se reduce con documento, jamás en silencio ──
    val ajustesFactura: List<AjusteFactura> = emptyList(),
    // ── ANULACIÓN (el papel jamás se borra: se anula con quién, cuándo y por qué) ──
    val motivoAnulacion: String = "",
    val anuladoPorEmail: String = "",
    val anuladoPorNombre: String = "",
    val anuladoElLegible: String = "",
    val anulacionPlata: RespuestaPlataAnulacion? = null,
    val sucursalId: String = "",
    val farmaciaId: String = "",
    val pedidoId: String = "",
    val pedidoNumeroOrden: String = ""
) {
    val esContado: Boolean
        get() = condicionPago.contains("Contado", ignoreCase = true)

    /** Una sola verdad: anulada se consulta aquí, jamás comparando strings sueltos. */
    val esAnulada: Boolean
        get() = estadoPago.equals("ANULADA", ignoreCase = true)

    /** El total que dice el PAPEL (inmutable por sí solo; solo baja con nota de crédito documentada). */
    val totalPapel: Double
        get() = if (montoTotal > 0) montoTotal else if (montoAcumulado > 0) montoAcumulado else items.sumOf { it.costoTotal }

    /** Suma de notas de crédito registradas contra esta factura. */
    val totalAjustes: Double
        get() = ajustesFactura.sumOf { it.monto.coerceAtLeast(0.0) }

    /** Lo que realmente se debe: total del papel menos notas de crédito. */
    val totalEfectivo: Double
        get() = (totalPapel - totalAjustes).coerceAtLeast(0.0)

    val totalAbonadoReal: Double
        get() {
            val vigentes = abonos.filterNot { it.anulado }.sumOf { it.monto }
            return if (abonos.isNotEmpty()) vigentes else montoPagado
        }

    val saldoPendienteReal: Double
        get() = if (esAnulada) 0.0 else (totalEfectivo - totalAbonadoReal).coerceAtLeast(0.0)

    val esTotalmentePagada: Boolean
        get() = !esAnulada && totalEfectivo > 0.01 && saldoPendienteReal <= 0.01

    val montoBaseCalculado: Double
        get() = if (montoBase > 0) montoBase else Math.round((totalEfectivo / 1.18) * 100.0) / 100.0

    val montoIgvCalculado: Double
        get() = if (montoIgv > 0) montoIgv else Math.round((totalEfectivo - montoBaseCalculado) * 100.0) / 100.0

    /** Plata real ya pagada al proveedor por esta factura (contado invertido o abonos de crédito). */
    val plataPagadaEnFactura: Double
        get() = totalAbonadoReal.coerceAtLeast(0.0)

    val saldoRestantePorAsignar: Double
        get() = (totalPapel - (if (montoAcumulado > 0) montoAcumulado else items.sumOf { it.costoTotal })).coerceAtLeast(0.0)

    val porcentajeAsignado: Float
        get() = if (totalPapel > 0) {
            val acum = if (montoAcumulado > 0) montoAcumulado else items.sumOf { it.costoTotal }
            (acum / totalPapel).toFloat().coerceIn(0f, 1f)
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
