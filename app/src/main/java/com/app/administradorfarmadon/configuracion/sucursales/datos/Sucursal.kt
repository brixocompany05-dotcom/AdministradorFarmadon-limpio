package com.app.administradorfarmadon.configuracion.sucursales.datos

/**
 * Modelo de Sucursal / Sede de una Farmacia.
 */
data class Sucursal(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val esPrincipal: Boolean = false,
    val activa: Boolean = true,
    val responsable: String = "",
    val codigoInterno: String = "",
    val fechaCreacion: Any? = null
)
