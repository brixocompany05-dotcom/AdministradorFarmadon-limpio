package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.tabla

import androidx.compose.ui.unit.Dp

/**
 * Dimensiones y anchos para la tabla de inventario Enterprise SaaS Farmadon.
 * Organizado por: PRODUCTO | CÓDIGO | STOCK ACTUAL | STOCK MÍNIMO | VENCIMIENTO
 */
data class InventoryColumns(
    val category: Dp,
    val code: Dp,
    val stock: Dp,
    val min: Dp,
    val expiry: Dp
)
