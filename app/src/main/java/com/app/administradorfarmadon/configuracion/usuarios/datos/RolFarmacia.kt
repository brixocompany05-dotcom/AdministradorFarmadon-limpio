package com.app.administradorfarmadon.configuracion.usuarios.datos

/**
 * Rol oficial del ecosistema Farmaciapp (leído desde catálogo compartido).
 */
data class RolFarmacia(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val esSistema: Boolean = false,
    val orden: Int = 0
)
