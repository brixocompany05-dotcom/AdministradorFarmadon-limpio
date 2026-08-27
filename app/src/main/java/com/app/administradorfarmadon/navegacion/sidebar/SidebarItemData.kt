package com.app.administradorfarmadon.navegacion.sidebar

/**
 * Modelo de datos para un item del Sidebar.
 */
data class SidebarItemData(
    val nombre: String = "",
    val modulo: String = "",
    val icono: String = "",
    val categoria: String = "",
    val orden: Int = 0,
    val badge: Int? = null
)
