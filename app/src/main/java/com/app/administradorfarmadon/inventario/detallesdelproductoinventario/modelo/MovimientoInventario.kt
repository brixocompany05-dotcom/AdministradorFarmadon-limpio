package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo

data class MovimientoInventario(
    val id: String = "",
    val productoId: String = "",
    val tipo: String = "",
    val cantidad: Double = 0.0,
    val stockAnterior: Double = 0.0,
    val stockResultante: Double = 0.0,
    val referencia: String = "",
    val loteNumero: String = "",
    val fecha: String = "",
    val usuarioNombre: String = "",
    val unidadVisual: String = "",
    val costoTotal: Double = 0.0,
    val fechaEmisionFactura: String = "",
    val diferenciaConciliacion: Double = 0.0,
    val pagoTipo: String = "",
    val pagoDetalle: String = ""
)
