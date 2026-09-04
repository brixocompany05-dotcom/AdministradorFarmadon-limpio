package com.app.administradorfarmadon.analitica_reportes.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val TIMEZONE_LIMA: TimeZone = TimeZone.getTimeZone("America/Lima")

private val NOMBRES_MESES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

/**
 * Diálogo Enterprise SaaS para seleccionar Año y Mes de forma limpia y directa.
 */
@Composable
fun DialogoSelectorMes(
    anioInicial: Int,
    mesInicial: Int, // 1..12
    onDismiss: () -> Unit,
    onConfirmar: (anio: Int, mes: Int) -> Unit
) {
    var anioSeleccionado by remember { mutableIntStateOf(if (anioInicial > 0) anioInicial else Calendar.getInstance(TIMEZONE_LIMA).get(Calendar.YEAR)) }
    var mesSeleccionado by remember { mutableIntStateOf(if (mesInicial in 1..12) mesInicial else Calendar.getInstance(TIMEZONE_LIMA).get(Calendar.MONTH) + 1) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = FDColors.SurfaceElevated,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.width(380.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = FDColors.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Seleccionar Mes",
                            style = FDType.Heading2.copy(
                                fontSize = 16.sp,
                                fontFamily = InterPremium,
                                fontWeight = FontWeight.Bold
                            ),
                            color = FDColors.TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = FDColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Selector de Año
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FDColors.Background, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { anioSeleccionado-- },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Año anterior",
                            tint = FDColors.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "$anioSeleccionado",
                        style = FDType.Body.copy(
                            fontSize = 15.sp,
                            fontFamily = InterPremium,
                            fontWeight = FontWeight.Bold
                        ),
                        color = FDColors.TextPrimary
                    )
                    IconButton(
                        onClick = { anioSeleccionado++ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Año siguiente",
                            tint = FDColors.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Grilla de Meses (4 filas x 3 columnas)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(180.dp)
                ) {
                    itemsIndexed(NOMBRES_MESES) { index, nombre ->
                        val mesNum = index + 1
                        val esSeleccionado = mesNum == mesSeleccionado

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (esSeleccionado) FDColors.Primary else FDColors.Background,
                            border = BorderStroke(
                                1.dp,
                                if (esSeleccionado) FDColors.Primary else FDColors.Border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { mesSeleccionado = mesNum }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = nombre.take(3),
                                    style = FDType.Label.copy(
                                        fontSize = 12.sp,
                                        fontFamily = InterPremium,
                                        fontWeight = if (esSeleccionado) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (esSeleccionado) Color.White else FDColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, FDColors.Border)
                    ) {
                        Text(
                            text = "Cancelar",
                            style = FDType.Label.copy(fontSize = 12.5.sp),
                            color = FDColors.TextSecondary
                        )
                    }
                    Button(
                        onClick = { onConfirmar(anioSeleccionado, mesSeleccionado) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                    ) {
                        Text(
                            text = "Aplicar",
                            style = FDType.Label.copy(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Diálogo con selector de rango de fechas (Inicio y Fin) para Analítica.
 * Convierte las fechas a milisegundos de inicio (00:00:00) y fin (23:59:59.999) en zona horaria Lima.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoSelectorRangoPersonalizado(
    inicioMsInicial: Long,
    finMsInicial: Long,
    onDismiss: () -> Unit,
    onConfirmar: (inicioMs: Long, finMs: Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = if (inicioMsInicial > 0) inicioMsInicial else null,
        initialSelectedEndDateMillis = if (finMsInicial > 0) finMsInicial else null
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val startUtc = dateRangePickerState.selectedStartDateMillis
                    val endUtc = dateRangePickerState.selectedEndDateMillis
                    if (startUtc != null) {
                        val endEfectivoUtc = endUtc ?: startUtc

                        val sdfUtc = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        val strInicio = sdfUtc.format(Date(startUtc))
                        val strFin = sdfUtc.format(Date(endEfectivoUtc))

                        val sdfLima = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TIMEZONE_LIMA
                        }
                        val dateIniLima = sdfLima.parse(strInicio) ?: Date(startUtc)
                        val dateFinLima = sdfLima.parse(strFin) ?: Date(endEfectivoUtc)

                        val calIni = Calendar.getInstance(TIMEZONE_LIMA).apply {
                            time = dateIniLima
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val calFin = Calendar.getInstance(TIMEZONE_LIMA).apply {
                            time = dateFinLima
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }

                        onConfirmar(calIni.timeInMillis, calFin.timeInMillis)
                    }
                }
            ) {
                Text(
                    text = "APLICAR",
                    style = FDType.Label.copy(fontWeight = FontWeight.Bold),
                    color = FDColors.Primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCELAR",
                    style = FDType.Label,
                    color = FDColors.TextSecondary
                )
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "Seleccionar Rango de Fechas",
                    style = FDType.Heading2.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    color = FDColors.TextPrimary
                )
            },
            headline = {
                val start = dateRangePickerState.selectedStartDateMillis
                val end = dateRangePickerState.selectedEndDateMillis
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                val txt = when {
                    start != null && end != null -> "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
                    start != null -> "${sdf.format(Date(start))} - ..."
                    else -> "Elige fecha de inicio y fin"
                }
                Text(
                    text = txt,
                    style = FDType.BodySmall.copy(fontSize = 13.sp),
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                    color = FDColors.TextSecondary
                )
            },
            showModeToggle = false
        )
    }
}
