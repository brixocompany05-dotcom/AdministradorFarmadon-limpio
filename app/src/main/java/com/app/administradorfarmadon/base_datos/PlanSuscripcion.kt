package com.app.administradorfarmadon.base_datos

data class PlanSuscripcion(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val precioMensual: Double = 0.0,
    val periodo: String = "mensual",
    val descuentoAnual: Int = 0,
    val maxSucursales: Int = 1,
    val diasPrueba: Int = 0,
    val esGratuito: Boolean = false,
    // Duracion del plan GRATIS en dias (BrixoPanel: un plan gratis SIEMPRE es temporal).
    val diasGratis: Int = 0,
    val features: List<String> = emptyList(),
    val activo: Boolean = true,
    val eliminado: Boolean = false,
    val orden: Int = 0,
    val paisIso: String = "",
    val monedaCodigo: String = "PEN",
    val monedaSimbolo: String = "S/"
)
