package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium

/**
 * Menú lateral quiet 10/10 —” lista silenciosa tipo Ajustes iPad, una sola jerarquía,
 * selección fondo tenue + borde izquierdo, geometría s, WindowInsets respetados, imePadding seguro.
 */
@Composable
fun ConfiguracionMenuLateral(
    seccionSeleccionada: SeccionConfiguracion,
    onSeleccionarSeccion: (SeccionConfiguracion) -> Unit,
    ubicacionActual: String,
    stockMinimoActual: Double,
    diasVencimientoActual: Int,
    fefoAutomaticoActual: Boolean,
    isActivo: Boolean,
    codigoActual: String = "",
    unidadStock: String = "Unidad",
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    val secciones = SeccionConfiguracion.values().toList()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(s.radiusCard))
            .background(FDColors.Surface)
            .padding(s.sm)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(s.xs * 0.35f)
    ) {
        Text(
            text = "SECCIONES",
            style = FDType.Label.copy(
                fontSize = s.textLabel.value.sp * 0.95f,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.9.sp,
                fontFamily = InterPremium
            ),
            color = FDColors.TextTertiary,
            modifier = Modifier.padding(horizontal = s.xs, vertical = s.xs * 0.6f)
        )

        secciones.forEach { sec ->
            val isSelected = seccionSeleccionada == sec
            val statusColor = when (sec) {
                SeccionConfiguracion.UBICACION -> if (ubicacionActual.isBlank()) FDColors.Error else FDColors.Success
                SeccionConfiguracion.STOCK_MINIMO -> if (stockMinimoActual <= 0) FDColors.Warning else FDColors.Success
                SeccionConfiguracion.ALERTA_VENCIMIENTO -> when {
                    diasVencimientoActual < 30 -> FDColors.Error
                    diasVencimientoActual in 30..60 -> FDColors.Warning
                    else -> FDColors.Success
                }
                SeccionConfiguracion.CONSUMO_FEFO -> if (fefoAutomaticoActual) FDColors.Success else FDColors.Warning
                SeccionConfiguracion.ESTADO_OPERATIVO -> if (isActivo) FDColors.Success else FDColors.Error
                SeccionConfiguracion.CODIGO_BARRAS -> if (codigoActual.isBlank()) FDColors.Warning else FDColors.Success
            }
            val resumen = when (sec) {
                SeccionConfiguracion.UBICACION -> ubicacionActual.ifBlank { "Sin asignar · toca para elegir" }
                SeccionConfiguracion.STOCK_MINIMO -> {
                    val sing = unidadStock.trim().ifBlank { "Unidad" }
                    val plural = when {
                        sing.equals("Unidad", true) -> "Unidades"
                        sing.endsWith("s", true) -> sing
                        else -> "${sing}s"
                    }
                    val n = stockMinimoActual.toInt()
                    if (n <= 0) "Sin umbral configurado" else "$n ${if (n == 1) sing else plural} al mínimo"
                }
                SeccionConfiguracion.ALERTA_VENCIMIENTO -> "$diasVencimientoActual días antes de vencer"
                SeccionConfiguracion.CONSUMO_FEFO -> if (fefoAutomaticoActual) "FEFO automático activo" else "Selección manual de lote"
                SeccionConfiguracion.ESTADO_OPERATIVO -> if (isActivo) "Se puede vender en mostrador" else "Pausado · oculto para venta"
                SeccionConfiguracion.CODIGO_BARRAS -> if (codigoActual.isNotBlank()) codigoActual.uppercase() else "Pendiente de asignar"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(s.radiusButton))
                    .background(if (isSelected) FDColors.Primary.copy(alpha = 0.06f) else FDColors.Surface)
                    .clickable { onSeleccionarSeccion(sec) }
                    .padding(horizontal = s.xs + 2.dp, vertical = s.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(s.xs)
            ) {
                Box(
                    modifier = Modifier
                        .width(s.borderWidth * 2f)
                        .height(s.iconMedium + 8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) FDColors.Primary else FDColors.Surface)
                )
                Box(
                    modifier = Modifier
                        .size(s.iconMedium + 10.dp)
                        .clip(RoundedCornerShape(s.radiusChip))
                        .background(if (isSelected) FDColors.Primary.copy(alpha = 0.10f) else FDColors.Background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = sec.icon,
                        contentDescription = null,
                        tint = if (isSelected) FDColors.Primary else FDColors.TextTertiary,
                        modifier = Modifier.size(s.iconSmall - 2.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = sec.titulo,
                        style = FDType.Body.copy(
                            fontSize = s.textBody.value.sp * 0.92f,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontFamily = InterPremium
                        ),
                        color = FDColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = resumen,
                        style = FDType.Caption.copy(fontSize = s.textLabel.value.sp * 0.92f, fontFamily = InterPremium),
                        color = if (isSelected) FDColors.TextSecondary else FDColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(s.xs * 0.8f)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = if (isSelected) FDColors.TextSecondary else FDColors.TextTertiary.copy(alpha = 0.45f),
                    modifier = Modifier.size(s.iconTiny)
                )
            }
        }

        Spacer(modifier = Modifier.height(s.gapSmall))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.xs * 0.6f),
            modifier = Modifier.padding(horizontal = s.xs)
        ) {
            Box(Modifier.size(s.xs * 0.5f).clip(CircleShape).background(FDColors.Success.copy(alpha = 0.9f)))
            Text(
                text = "Los cambios se guardan al instante",
                style = FDType.Caption.copy(fontSize = s.textLabel.value.sp * 0.9f, fontFamily = InterPremium),
                color = FDColors.TextTertiary
            )
        }
    }
}
