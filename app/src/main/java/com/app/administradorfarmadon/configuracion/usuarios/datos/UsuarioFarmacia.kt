package com.app.administradorfarmadon.configuracion.usuarios.datos

/**
 * Modelo de Colaborador / Usuario de una Farmacia.
 */
data class UsuarioFarmacia(
    val id: String = "",
    val clienteId: String = "",
    val nombre: String = "",
    val dni: String = "",
    val telefono: String = "",
    val email: String = "",
    val password: String = "",
    val rolId: String = "",
    val rolNombre: String = "",
    val sucursalId: String = "",
    val sucursalNombre: String = "",
    val acceso: Boolean = true,
    val estado: String = "ACTIVO", // "ACTIVO", "SUSPENDIDO", "DADO_DE_BAJA"
    val dadoDeBaja: Boolean = false,
    val fechaCreacion: Any? = null,
    val actualizadoEn: Any? = null,
    // RAíZ: permisos por usuario y por módulo. Si el mapa está vacío, hereda del rol.
    // Si tiene entrada { "inventario": true, "compras": false }, esa es la verdad final.
    // Solo se muestran/se guardan módulos que están en el plan contratado (ModulosResueltos).
    val permisosModulos: Map<String, Boolean> = emptyMap()
)
