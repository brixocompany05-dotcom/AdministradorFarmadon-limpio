package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium

/**
 * Ubicación — 10/10 QUIET
 * Una sola verdad + una acción primaria, sin cajas anidadas gruesas, colores adaptativos, geometría s.
 */
@Composable
fun PanelUbicacionAlmacen(
    ubicacionSeleccionada: String,
    onUbicacionChange: (String) -> Unit,
    ubicacionesDisponibles: List<String>,
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    var showPicker by remember { mutableStateOf(false) }
    val totalUbicaciones =
        remember(ubicacionesDisponibles) { ubicacionesDisponibles.distinct().size }
    val isAsignada = ubicacionSeleccionada.isNotBlank()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(s.radiusCard))
                .background(FDColors.SurfaceElevated)
                .border(s.borderWidth * 0.6f, FDColors.Border.copy(alpha = 0.75f), RoundedCornerShape(s.radiusCard))
                .padding(horizontal = s.padCardLarge, vertical = s.padCardLarge),
            verticalArrangement = Arrangement.spacedBy(s.gapSmall),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(s.iconLarge + 16.dp)
                    .clip(RoundedCornerShape(s.radiusChip + 2.dp))
                    .background(
                        if (isAsignada) FDColors.Success.copy(alpha = 0.10f) else FDColors.Warning.copy(alpha = 0.11f)
                    )
                    .border(
                        s.borderWidth * 0.5f,
                        if (isAsignada) FDColors.Success.copy(alpha = 0.16f) else FDColors.Warning.copy(alpha = 0.18f),
                        RoundedCornerShape(s.radiusChip + 2.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Place, null,
                    tint = if (isAsignada) FDColors.Success else FDColors.Warning,
                    modifier = Modifier.size(s.iconSmall + 2.dp)
                )
            }

            Text(
                text = ubicacionSeleccionada.ifBlank { "Sin ubicación asignada" },
                style = FDType.Heading2.copy(
                    fontSize = s.textSubtitle.value.sp * 1.1f,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterPremium,
                    lineHeight = s.textSubtitle.value.sp * 1.35f,
                    textAlign = TextAlign.Center
                ),
                color = if (isAsignada) FDColors.TextPrimary else FDColors.TextTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(s.xs * 0.6f)
            ) {
                Box(
                    Modifier
                        .size(s.xs * 0.6f)
                        .clip(CircleShape)
                        .background(if (isAsignada) FDColors.Success else FDColors.Warning)
                )
                Text(
                    text = if (isAsignada) "Asignada · visible en inventario, etiquetas y búsqueda"
                    else "Por asignar · elige una ubicación para agilizar el mostrador",
                    style = FDType.Caption.copy(
                        fontSize = s.textLabel.value.sp,
                        fontFamily = InterPremium,
                        textAlign = TextAlign.Center
                    ),
                    color = FDColors.TextSecondary
                )
            }
        }

        Button(
            onClick = { showPicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(s.btnMediumH),
            shape = RoundedCornerShape(s.radiusButton),
            colors = ButtonDefaults.buttonColors(
                containerColor = FDColors.Primary,
                contentColor = FDColors.PrimaryText
            )
        ) {
            Text(
                text = if (isAsignada) "Cambiar ubicación" else "Asignar ubicación",
                style = FDType.Label.copy(
                    fontSize = s.textBody.value.sp * 0.96f,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterPremium
                )
            )
        }

        Text(
            text = when {
                totalUbicaciones == 0 -> "Aún no hay ubicaciones en esta farmacia · se crearán al asignar"
                totalUbicaciones == 1 -> "1 ubicación registrada en tu farmacia"
                else -> "$totalUbicaciones ubicaciones registradas · administradas para toda la farmacia"
            },
            style = FDType.Caption.copy(
                fontSize = s.textLabel.value.sp * 0.92f,
                fontFamily = InterPremium,
                textAlign = TextAlign.Center
            ),
            color = FDColors.TextTertiary,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showPicker) {
        OverlaySelectorUbicacionDialog(
            ubicacionActual = ubicacionSeleccionada,
            ubicacionesDisponibles = ubicacionesDisponibles,
            onUbicacionSelected = { nueva ->
                if (nueva.trim().isNotBlank()) onUbicacionChange(nueva.trim())
            },
            onDismiss = { showPicker = false }
        )
    }
}
