package com.app.administradorfarmadon.inventario.crearproductogeneral.datos

data class ProductoInventario(
    val id: String = "",
    val clienteId: String = "",
    val nombre: String = "",
    val principioActivo: String = "",
    val codigoBarras: String = "",
    val tipoProducto: String = "MEDICAMENTO",
    val categoriaId: String = "",
    val categoriaNombre: String = "",
    val laboratorio: String = "",
    val empaque: String = "Caja",
    val contenido: String = "",
    val contenidoUnidad: String = "",
    val medidaConcentracion: String = "",
    val requiereReceta: Boolean = false,
    val esRefrigerado: Boolean = false,
    val permiteFraccionar: Boolean = false,
    val clasificacionControl: String = "VENTA_LIBRE", // "PSICOTROPICO" | "CONTROLADO" | "ANTIBIOTICO" | "VENTA_LIBRE"
    val estado: String = "ACTIVO",
    val creadoEn: Any? = null,
    val creadoPor: String = "",
    val actualizadoEn: Any? = null
)
