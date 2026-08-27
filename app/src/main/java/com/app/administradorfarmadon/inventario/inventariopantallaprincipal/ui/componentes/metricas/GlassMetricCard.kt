package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.metricas

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

/**
 * Tarjeta de métrica con elevación limpia y contraste natural.
 * Se despega suavemente del fondo base mediante FDColors.SurfaceElevated y borde sutil.
 * Si onClick != null la tarjeta es interactiva (filtro en vivo) y muestra estado seleccionado.
 */
@Composable
fun GlassMetricCard(
    number: String,
    label: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    // Diseño Unificado SaaS Enterprise (Sin diferenciación de selección)
    val cardColor = FDColors.SurfaceElevated
    val borderColor = FDColors.Border
    val borderWidth = 0.8.dp
    val textPrimary = FDColors.TextPrimary
    val textSecondary = FDColors.TextSecondary

    val clickableMod = if (onClick != null && !isLoading) Modifier.clickable(onClick = onClick) else Modifier

    val infiniteTransition = rememberInfiniteTransition(label = "metric_skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        color = cardColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = if (!FDColors.isDark && !isLoading) 6.dp else 0.dp,
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(clickableMod)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label.uppercase(),
                color = textSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TokensFarmadon.colores.textoTerciario.copy(alpha = alpha))
                )
            } else {
                Text(
                    text = number,
                    color = textPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )
            }
        }
    }
}
