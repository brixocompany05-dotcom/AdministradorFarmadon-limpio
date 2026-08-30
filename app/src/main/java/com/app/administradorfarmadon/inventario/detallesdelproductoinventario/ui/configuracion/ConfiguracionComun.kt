package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType

/**
 * Secciones de configuración operativa en Detalle del Producto.
 */
enum class SeccionConfiguracion(
    val titulo: String,
    val subtitulo: String,
    val icon: ImageVector
) {
    UBICACION("Ubicación y Almacén", "Estante, vitrina o pasillo asignado", Icons.Outlined.Place),
    STOCK_MINIMO("Alerta de Stock Mínimo", "Umbral crítico de reabastecimiento", Icons.Outlined.NotificationsActive),
    ALERTA_VENCIMIENTO("Alerta Preventiva de Vencimiento", "Anticipación para canjes y devoluciones", Icons.Outlined.HourglassBottom),
    CONSUMO_FEFO("Consumo de Lotes (FEFO)", "Orden automático por vencimiento o elección manual", Icons.Outlined.DateRange),
    ESTADO_OPERATIVO("Estado de Venta en Mostrador", "Habilitado o pausado para facturación", Icons.Outlined.ToggleOn),
    CODIGO_BARRAS("Código de Barras y Etiquetas", "Generador, escáner e impresión de góndola", Icons.Outlined.QrCode)
}

@Composable
fun ConfigSectionHeader(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = FDType.Heading3.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = FDColors.TextPrimary,
                letterSpacing = 0.5.sp
            )
        )
        Text(
            text = description,
            style = FDType.Caption.copy(
                fontSize = 11.5.sp,
                color = FDColors.TextSecondary
            )
        )
    }
}

@Composable
fun ConfiguracionTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = FDType.Label.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isFocused) FDColors.Primary else FDColors.TextSecondary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFocused) FDColors.SurfaceElevated else FDColors.Surface)
                .border(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    color = if (isFocused) FDColors.Primary else FDColors.Border,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (isFocused) FDColors.Primary else FDColors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    interactionSource = interactionSource,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle = FDType.Body.copy(
                        color = FDColors.TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(FDColors.Primary),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = FDType.Body.copy(
                                    color = FDColors.TextTertiary,
                                    fontSize = 12.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

/**
 * Estado del auto-guardado en tiempo real.
 */
enum class EstadoAutoGuardado {
    REPOSO,
    GUARDANDO,
    GUARDADO,
    ERROR
}

@Composable
fun AutoSaveBadge(
    estado: EstadoAutoGuardado,
    mensajeError: String? = null,
    onReintentar: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = estado != EstadoAutoGuardado.REPOSO,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val (bg, border, text, label) = when (estado) {
            EstadoAutoGuardado.GUARDANDO -> listOf(
                FDColors.Primary.copy(alpha = 0.1f),
                FDColors.Primary.copy(alpha = 0.3f),
                FDColors.Primary,
                "Guardando en tiempo real..."
            )
            EstadoAutoGuardado.GUARDADO -> listOf(
                FDColors.Success.copy(alpha = 0.12f),
                FDColors.Success.copy(alpha = 0.4f),
                FDColors.Success,
                "──œ“ Guardado automáticamente"
            )
            EstadoAutoGuardado.ERROR -> listOf(
                FDColors.Error.copy(alpha = 0.12f),
                FDColors.Error.copy(alpha = 0.4f),
                FDColors.Error,
                mensajeError ?: "Error al sincronizar"
            )
            EstadoAutoGuardado.REPOSO -> listOf(
                FDColors.Surface,
                FDColors.Border,
                FDColors.TextSecondary,
                ""
            )
        }

        Surface(
            color = bg as androidx.compose.ui.graphics.Color,
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, border as androidx.compose.ui.graphics.Color),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (estado == EstadoAutoGuardado.GUARDANDO) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = FDColors.Primary
                    )
                }
                Text(
                    text = label as String,
                    style = FDType.Caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = text as androidx.compose.ui.graphics.Color
                    )
                )
                if (estado == EstadoAutoGuardado.ERROR && onReintentar != null) {
                    Text(
                        text = "· Reintentar",
                        style = FDType.Caption.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FDColors.Primary
                        ),
                        modifier = Modifier.clickable { onReintentar() }
                    )
                }
            }
        }
    }
}
