package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.tabla

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dimensiones y anchos para la tabla de inventario Enterprise SaaS Farmadon.
 * PRODUCTO (weight 1) | PRESENTACIÓN | CATEGORÍA | CÓDIGO | STOCK | MÍNIMO | VENCIMIENTO | VALOR | ESTADO
 */
data class InventoryColumns(
    val category: Dp,
    val stock: Dp,
    val min: Dp,
    val expiry: Dp = 120.dp,
    val value: Dp = 120.dp,
    val status: Dp = 110.dp,
    val presentation: Dp = 110.dp,
    val code: Dp = 100.dp
)
