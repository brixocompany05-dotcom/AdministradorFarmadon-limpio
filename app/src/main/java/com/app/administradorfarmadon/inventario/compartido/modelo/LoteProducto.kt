package com.app.administradorfarmadon.inventario.compartido.modelo

data class LoteProducto(
    val numero: String = "",
    val vencimiento: String = "",
    val cantidad: Double = 0.0,
    @Deprecated("Usar flujo de bloqueo de lote futuro")
    val cantidadBloqueada: Double = 0.0,
    val fecha: String = "",
    val costoCompraUnitario: Double = 0.0,
    val costoUltimoIngreso: Double = 0.0,
    val costoUltimoIngresoUnitario: Double = 0.0,
    val observaciones: String = "",
    val motivoBloqueo: String = "",
    val timestampUltimoBloqueo: Long = 0L,
    val nroFactura: String = "",
    val condicionPago: String = "CONTADO",
    val fechaVencimientoPago: String = "",
    val estadoPago: String = "PAGADO",
    val tipoDocumento: String = "",
    val iva: Double = 0.0,
    val descuentos: Double = 0.0,
    val retenciones: Map<String, Double> = emptyMap(),
    val createdByUserId: String = "",
    val createdByUserName: String = "",
    val createdAt: String = "",
    val proveedorId: String = "",
    val proveedorNombre: String = "",
    val loteId: String = ""
)
