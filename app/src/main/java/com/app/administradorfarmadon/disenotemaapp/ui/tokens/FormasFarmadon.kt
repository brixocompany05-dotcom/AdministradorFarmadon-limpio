package com.app.administradorfarmadon.disenotemaapp.ui.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Formas Estándar de Farmadon.
 */
@Immutable
data class FormasFarmadon(
    val extraPequena: RoundedCornerShape = RoundedCornerShape(6.dp),
    val pequena: RoundedCornerShape = RoundedCornerShape(8.dp),
    val mediana: RoundedCornerShape = RoundedCornerShape(12.dp),
    val grande: RoundedCornerShape = RoundedCornerShape(16.dp),
    val extraGrande: RoundedCornerShape = RoundedCornerShape(20.dp),
    val completa: RoundedCornerShape = RoundedCornerShape(50)
)

val LocalFormasFarmadon = staticCompositionLocalOf { FormasFarmadon() }
