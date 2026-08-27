package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.animacion

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.tabla.InventoryColumns

/**
 * Gestión de Layout Enterprise SaaS 2026 — INSTANT RESPONSE.
 * 5 Columnas esenciales con espacio amplio y legible: Producto, Presentación, Categoría, Código y Stock.
 */
@Composable
fun rememberInventarioLayoutState(
    isDetailOpen: Boolean,
    isFilterPanelOpen: Boolean,
    screenWidth: Dp
): InventarioLayoutState {
    
    // ── ANCHOS DE PANELES LATERALES (INSTANTÁNEOS · SIMÉTRICOS Y RESPIRADOS) ──
    val detailWidth = if (isDetailOpen) (screenWidth * 0.35f).coerceIn(360.dp, 520.dp) else 0.dp
    // Filtro: ancho fijo adaptativo 320-380dp (no 25% que aplasta en pantallas pequeñas)
    // Garantiza 320dp mínimo en tablet 600dp y 380dp máximo en 1360dp, simétrico siempre
    val filterWidth = if (isFilterPanelOpen) (screenWidth * 0.30f).coerceIn(320.dp, 400.dp) else 0.dp

    // Progreso binario (0 o 1)
    val detailProgress = if (isDetailOpen) 1f else 0f

    // ── 4 COLUMNAS PILARES ENTERPRISE SAAS ──
    val categoryWidth     = if (isDetailOpen) 140.dp else 200.dp
    val stockWidth        = if (isDetailOpen) 100.dp else 130.dp
    val minWidth          = if (isDetailOpen) 100.dp else 130.dp

    val cols = InventoryColumns(
        category = categoryWidth,
        stock = stockWidth,
        min = minWidth
    )
    
    return InventarioLayoutState(
        detailProgress = detailProgress,
        detailWidth = detailWidth,
        filterWidth = filterWidth,
        cols = cols
    )
}

data class InventarioLayoutState(
    val detailProgress: Float,
    val detailWidth: Dp,
    val filterWidth: Dp,
    val cols: InventoryColumns
)
