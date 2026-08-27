package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors

// ── Colores SaaS Dinámicos y Armónicos (Light & Dark Enterprise) ────────
val SaaSPrimary: Color get() = FDColors.Primary
val SaaSSuccess: Color get() = FDColors.Success
val SaaSWarning: Color get() = FDColors.Warning
val SaaSError: Color get() = FDColors.Error
val SaaSBackground: Color get() = FDColors.Background
val SaaSSurface: Color get() = FDColors.Surface
val SaaSTextPrimary: Color get() = FDColors.TextPrimary
val SaaSTextSecondary: Color get() = FDColors.TextSecondary
val SaaSGlass: Color get() = FDColors.Glass
val SaaSBorder: Color get() = FDColors.Border

// ─── MODIFIER GLASSMORPHISM HELPER ───────────────────────────────────
fun Modifier.glassmorphic(
    borderColor: Color = Color.Transparent,
    backgroundColor: Color = FDColors.Surface,
    cornerRadius: Dp = 16.dp
): Modifier = this
    .background(backgroundColor, RoundedCornerShape(cornerRadius))
    .border(
        width = 1.dp,
        brush = if (borderColor == Color.Transparent) {
            Brush.linearGradient(
                colors = listOf(
                    FDColors.BorderStrong,
                    FDColors.Border
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.15f)
                )
            )
        },
        shape = RoundedCornerShape(cornerRadius)
    )
    .clip(RoundedCornerShape(cornerRadius))
