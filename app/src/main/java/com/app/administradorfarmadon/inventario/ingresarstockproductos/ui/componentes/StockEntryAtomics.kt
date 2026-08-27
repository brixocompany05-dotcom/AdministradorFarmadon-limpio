package com.app.administradorfarmadon.inventario.ingresarstockproductos.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDSizes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import java.util.Calendar
import java.util.Locale

@Composable
fun DetailItemBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = value,
            style = FDType.BodySmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextPrimary),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FormSectionHeader(title: String) {
    Text(
        text = title,
        style = FDType.Label.copy(fontSize = 11.5.sp, color = FDColors.TextTertiary, letterSpacing = 1.sp)
    )
}

@Composable
fun CondicionPagoChip(
    titulo: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (seleccionado) FDColors.Primary else FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, if (seleccionado) FDColors.Primary else FDColors.BorderStrong)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = titulo,
                style = FDType.Caption.copy(
                    fontSize = 12.sp,
                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                    color = if (seleccionado) FDColors.PrimaryText else FDColors.TextSecondary
                )
            )
        }
    }
}

@Composable
fun EnterpriseTextField(
    label: String,
    placeholder: String,
    value: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    isError: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) FDColors.TextSecondary else FDColors.TextTertiary)
            )
            if (!enabled) {
                Icon(Icons.Default.Lock, null, tint = FDColors.TextTertiary, modifier = Modifier.size(12.dp))
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            placeholder = { Text(placeholder, style = FDType.Body.copy(color = FDColors.TextTertiary)) },
            textStyle = FDType.Body.copy(color = if (enabled) FDColors.TextPrimary else FDColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp),
            leadingIcon = leadingIcon?.let {
                { Icon(it, null, tint = if (!enabled) FDColors.TextTertiary else if (value.isNotBlank()) FDColors.Primary else FDColors.TextTertiary, modifier = Modifier.size(18.dp)) }
            },
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FDColors.InputBackground,
                unfocusedContainerColor = FDColors.InputBackground,
                disabledContainerColor = FDColors.InputBackground.copy(alpha = 0.5f),
                focusedBorderColor = FDColors.BorderFocus,
                unfocusedBorderColor = FDColors.InputBorder,
                disabledBorderColor = FDColors.InputBorder.copy(alpha = 0.5f),
                focusedTextColor = FDColors.TextPrimary,
                unfocusedTextColor = FDColors.TextPrimary,
                disabledTextColor = FDColors.TextSecondary,
                focusedPlaceholderColor = FDColors.InputPlaceholder,
                unfocusedPlaceholderColor = FDColors.InputPlaceholder
            ),
            modifier = Modifier.fillMaxWidth().height(FDSizes.inputHeight)
        )
    }
}

/**
 * Selector Unificado de Mes y Año de Caducidad (MM/YYYY).
 * Altura estándar de 52dp con diálogo rápido de 1 solo toque.
 */
@Composable
fun MonthYearPickerEnterprise(
    selectedMonth: Int,
    selectedYear: Int,
    enabled: Boolean = true,
    onMonthYearSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var currentYearViewing by remember(selectedYear, showDialog) { mutableIntStateOf(if (selectedYear >= 2020) selectedYear else Calendar.getInstance().get(Calendar.YEAR) + 1) }

    val months = listOf(
        1 to "Ene", 2 to "Feb", 3 to "Mar", 4 to "Abr",
        5 to "May", 6 to "Jun", 7 to "Jul", 8 to "Ago",
        9 to "Sep", 10 to "Oct", 11 to "Nov", 12 to "Dic"
    )

    val fullMonthNames = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    val esVacio = selectedMonth !in 1..12 || selectedYear < 2020
    val displayMonthName = if (esVacio) "" else fullMonthNames.getOrElse(selectedMonth - 1) { "Mes $selectedMonth" }
    val displayFormatted = if (esVacio) "Seleccionar mes/año" else String.format(Locale.US, "%s %d  (%02d/%d)", displayMonthName, selectedYear, selectedMonth, selectedYear)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vence *",
                style = FDType.Label.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) FDColors.TextSecondary else FDColors.TextTertiary
                )
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable(enabled = enabled) { showDialog = true },
            shape = RoundedCornerShape(8.dp),
            color = if (enabled) FDColors.SurfaceElevated else FDColors.Background.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, if (enabled) FDColors.BorderStrong else FDColors.Border.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = "Calendario de Caducidad",
                        tint = if (enabled) FDColors.Primary else FDColors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = displayFormatted,
                        style = FDType.Body.copy(
                            color = if (esVacio) FDColors.TextTertiary else if (enabled) FDColors.TextPrimary else FDColors.TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
                if (enabled) {
                    Icon(Icons.Default.ArrowDropDown, null, tint = FDColors.TextSecondary)
                } else {
                    Icon(Icons.Default.Lock, null, tint = FDColors.TextTertiary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }

    if (showDialog && enabled) {
        val nowCal = Calendar.getInstance()
        val actualYear = nowCal.get(Calendar.YEAR)
        val actualMonth = nowCal.get(Calendar.MONTH) + 1

        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                modifier = Modifier
                    .width(360.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(14.dp),
                color = FDColors.SurfaceElevated,
                border = BorderStroke(1.dp, FDColors.Border)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cabecera del selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECCIONAR CADUCIDAD",
                            style = FDType.Caption.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FDColors.TextSecondary,
                                letterSpacing = 1.sp
                            )
                        )
                        IconButton(onClick = { showDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = FDColors.TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)

                    // Selector de Año interactivo (< Año 2027 >)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(FDColors.Background, RoundedCornerShape(8.dp))
                            .border(1.dp, FDColors.Border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { currentYearViewing-- },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, "Año anterior", tint = FDColors.TextPrimary)
                        }

                        Text(
                            text = "Año $currentYearViewing",
                            style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FDColors.TextPrimary)
                        )

                        IconButton(
                            onClick = { currentYearViewing++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, "Año siguiente", tint = FDColors.TextPrimary)
                        }
                    }

                    // Grilla de 12 Meses (4 filas x 3 columnas)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        months.chunked(3).forEach { rowMonths ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowMonths.forEach { (mNum, mShort) ->
                                    val isSelected = (currentYearViewing == selectedYear && mNum == selectedMonth)
                                    // Mes vencido O el mes actual: no seleccionable (error humano prevenido)
                                    val isPastMonth = (currentYearViewing < actualYear) || (currentYearViewing == actualYear && mNum <= actualMonth)

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable(enabled = !isPastMonth) {
                                                onMonthYearSelected(mNum, currentYearViewing)
                                                showDialog = false
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) FDColors.Primary else if (isPastMonth) FDColors.Background.copy(alpha = 0.4f) else FDColors.Background,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) FDColors.Primary else if (isPastMonth) FDColors.Border.copy(alpha = 0.4f) else FDColors.Border
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$mShort (${String.format(Locale.US, "%02d", mNum)})",
                                                style = FDType.Caption.copy(
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) FDColors.PrimaryText else if (isPastMonth) FDColors.TextTertiary else FDColors.TextPrimary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
