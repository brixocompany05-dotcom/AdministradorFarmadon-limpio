package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.inventario.compartido.logica.CostoRealLote

@Composable
internal fun EnterpriseSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    enabled: Boolean = true,
    activeColor: Color = FDColors.Primary
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = FDColors.Surface,
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) activeColor else Color.Transparent)
                        .clickable(enabled = enabled && !isSelected) { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = FDType.Label.copy(
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) FDColors.PrimaryText else FDColors.TextSecondary
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun EnterpriseInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().then(if (singleLine) Modifier.height(56.dp) else Modifier),
        singleLine = singleLine,
        minLines = minLines,
        textStyle = FDType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextPrimary),
        cursorBrush = SolidColor(FDColors.Primary),
        interactionSource = interactionSource
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = enabled,
            singleLine = singleLine,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            label = {
                Text(
                    label.uppercase(),
                    style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                )
            },
            placeholder = {
                Text(placeholder, style = FDType.Body.copy(fontSize = 13.5.sp, color = FDColors.TextTertiary))
            },
            supportingText = if (isError && errorMessage != null) {
                { Text(errorMessage, color = FDColors.Error, style = FDType.Caption.copy(fontSize = 10.5.sp)) }
            } else null,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FDColors.Primary,
                unfocusedBorderColor = FDColors.BorderStrong,
                errorBorderColor = FDColors.Error,
                focusedLabelColor = FDColors.Primary,
                unfocusedLabelColor = FDColors.TextSecondary,
                errorLabelColor = FDColors.Error,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            container = {
                OutlinedTextFieldDefaults.ContainerBox(
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = FDColors.Primary,
                        unfocusedBorderColor = FDColors.BorderStrong,
                        errorBorderColor = FDColors.Error
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        )
    }
}

@Composable
internal fun EnterpriseReasonList(
    reasons: List<String>,
    selectedReason: String,
    onSelectReason: (String) -> Unit,
    enabled: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = FDColors.Surface,
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            reasons.forEachIndexed { index, reason ->
                val isSelected = selectedReason == reason
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(enabled = enabled) { onSelectReason(reason) }
                        .background(if (isSelected) FDColors.Primary.copy(alpha = 0.08f) else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = reason,
                        style = FDType.BodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) FDColors.TextPrimary else FDColors.TextSecondary
                        )
                    )
                }
                if (index < reasons.lastIndex) {
                    HorizontalDivider(
                        color = FDColors.Border.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun EnterpriseInfoRow(
    label: String,
    value: String,
    valueColor: Color = FDColors.TextPrimary,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = FDType.Caption.copy(fontSize = 11.5.sp, color = FDColors.TextTertiary))
        Text(
            text = value,
            style = FDType.BodySmall.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
            )
        )
    }
}

@Composable
internal fun EnterpriseModalShell(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onDismiss: () -> Unit,
    isProcesando: Boolean,
    mensajeError: String?,
    minWidth: androidx.compose.ui.unit.Dp = 680.dp,
    maxWidth: androidx.compose.ui.unit.Dp = 860.dp,
    leftContent: @Composable ColumnScope.() -> Unit,
    rightContent: @Composable ColumnScope.() -> Unit,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!isProcesando) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FDColors.Scrim.copy(alpha = 0.7f))
                .clickable(onClick = { if (!isProcesando) onDismiss() })
                .padding(horizontal = 40.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = minWidth, max = maxWidth)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(14.dp),
                color = FDColors.SurfaceElevated,
                border = BorderStroke(1.dp, FDColors.Border),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cabecera Enterprise
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
                            Text(
                                text = title,
                                style = FDType.Heading3.copy(fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            enabled = !isProcesando,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Outlined.Close, null, tint = FDColors.TextTertiary)
                        }
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Banner de Error Resiliente
                    if (mensajeError != null) {
                        Surface(
                            color = FDColors.Error.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Outlined.ErrorOutline, null, tint = FDColors.Error, modifier = Modifier.size(20.dp))
                                Text(mensajeError, style = FDType.Caption.copy(color = FDColors.Error, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    // Layout 2 Columnas Tablet (Patrón 60/40)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Columna Izquierda: Formulario Continuo (60%)
                        Column(
                            modifier = Modifier
                                .weight(1.5f)
                                .padding(end = 24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            content = leftContent
                        )

                        // Divisor Vertical
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(FDColors.Border.copy(alpha = 0.5f))
                        )

                        Column(
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxHeight()
                                .background(FDColors.Glass)
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            content = rightContent
                        )
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Barra de Acciones Inferior
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dismissButton()
                        if (confirmButton != null) {
                            Spacer(Modifier.width(12.dp))
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

//

// ESTADO DEL FORMULARIO DE RESOLUCIÓN
// Agrupa todo el estado mutable de forma explícita y con nombres claros.


internal fun esFechaVencimientoValida(input: String): Boolean {
    val sep = '/'
    val parts = input.trim().split(sep)
    if (parts.size != 2) return false
    val mes = parts[0].toIntOrNull() ?: return false
    val anio = parts[1].toIntOrNull() ?: return false
    if (mes < 1 || mes > 12) return false
    val ahora = java.util.Calendar.getInstance()
    val anioActual = ahora.get(java.util.Calendar.YEAR)
    val mesActual = ahora.get(java.util.Calendar.MONTH) + 1
    return anio > anioActual || (anio == anioActual && mes >= mesActual)
}

internal data class DevolucionFormState(
    // Modalidad: 0 = Canje físico, 1 = Nota de crédito
    val modalidadIdx: Int = 0,
    // Cantidad: 0 = Todo el lote, 1 = Parcial
    val modoCantidadIdx: Int = 0,
    val cantidadStr: String = "",
    // Lote de reposición (solo canje): 0 = Mismo lote, 1 = Lote nuevo
    val tipoLoteIdx: Int = 0,
    val nuevoLoteNumero: String = "",
    val nuevoVencimiento: String = "",
    // Documentos
    val guiaCanjeStr: String = "",
    val guiaRetiroStr: String = "",
    val notaCreditoStr: String = "",
    // Motivo seleccionado
    val motivoSeleccionado: String = ""
) {
    val esCanjesFisico get() = modalidadIdx == 0
    val tipoAccion get() = if (esCanjesFisico) "CANJE_FISICO" else "NOTA_CREDITO"
    val esLoteNuevo get() = tipoLoteIdx == 1
    val esCantidadParcial get() = modoCantidadIdx == 1
}

// LÓGICA DE NEGOCIO —” FUNCIONES PURAS
// Sin estado, sin UI. Solo cálculos y reglas del formulario.


internal fun calcularCostoUnitario(lote: LoteProducto): Double = CostoRealLote.costoUnitario(lote)

internal fun motivosPorModalidad(esCanjesFisico: Boolean): List<String> =
    if (esCanjesFisico) listOf(
        "Mercadería dañada o rota",
        "Próximo a Vencer (Canje por Vencimiento)",
        "Defecto de Calidad / Falla de Fábrica"
    ) else listOf(
        "Mercadería No Solicitada / Error de Despacho",
        "Devolución Definitiva por Sobrestock",
        "Producto Defectuoso Sin Reposición Física",
        "Retiro Sanitario / Alerta de Laboratorio"
    )


// COMPOSABLE PRINCIPAL —” SOLO ORQUESTA ESTADO Y SECCIONES


@Composable
internal fun SeccionLabel(texto: String, color: Color = FDColors.TextSecondary) {
    Text(
        text = texto,
        style = FDType.Label.copy(
            fontSize = 10.5.sp,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    )
}

@Composable

internal fun EnterpriseOptionSelector(
    options: List<Pair<String, String>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    enabled: Boolean = true,
    accentColor: Color = FDColors.Primary
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = FDColors.Surface,
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            options.forEachIndexed { index, (titulo, descripcion) ->
                val isSelected = selectedIndex == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { onSelect(index) }
                        .background(if (isSelected) accentColor.copy(alpha = 0.07f) else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = titulo,
                            style = FDType.BodySmall.copy(
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) FDColors.TextPrimary else FDColors.TextSecondary
                            )
                        )
                        Text(
                            text = descripcion,
                            style = FDType.Caption.copy(
                                fontSize = 11.5.sp,
                                color = FDColors.TextTertiary,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
                if (index < options.lastIndex) {
                    HorizontalDivider(
                        color = FDColors.Border.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
