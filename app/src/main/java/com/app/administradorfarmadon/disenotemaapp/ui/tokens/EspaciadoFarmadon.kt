package com.app.administradorfarmadon.disenotemaapp.ui.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Escala de Espaciado Canónica (4dp base).
 */
@Immutable
data class EspaciadoFarmadon(
    val nulo: Dp = 0.dp,
    val muyPequeno: Dp = 4.dp,
    val pequeno: Dp = 8.dp,
    val mediano: Dp = 16.dp,
    val grande: Dp = 24.dp,
    val muyGrande: Dp = 32.dp,
    val extraGrande: Dp = 48.dp,
    val superGrande: Dp = 64.dp
)

val LocalEspaciadoFarmadon = staticCompositionLocalOf { EspaciadoFarmadon() }
