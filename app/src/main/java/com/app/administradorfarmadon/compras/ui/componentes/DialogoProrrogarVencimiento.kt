package com.app.administradorfarmadon.compras.ui.componentes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DialogoProrrogarVencimiento(
    factura: FacturaCompra,
    onDismiss: () -> Unit,
    onConfirmarProrroga: (nuevaFecha: String) -> Unit
) {
    val s = recordarMedidaAdaptativa()
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0
    BackHandler(enabled = true) {
        when {
            isKeyboardVisible -> { keyboardController?.hide(); focusManager.clearFocus(force = true) }
            else -> onDismiss()
        }
    }

    fun hoyServidor(): Date = Date(com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs())

    var fechaTexto by remember { mutableStateOf(factura.fechaVencimientoPago.ifBlank { sdf.format(hoyServidor()) }) }
    var errorFecha by remember { mutableStateOf<String?>(null) }

    fun sumarDias(dias: Int) {
        val baseDate = try {
            if (factura.fechaVencimientoPago.isNotBlank()) sdf.parse(factura.fechaVencimientoPago) ?: hoyServidor()
            else hoyServidor()
        } catch (e: Exception) {
            hoyServidor()
        }
        val cal = Calendar.getInstance().apply {
            time = baseDate
            add(Calendar.DAY_OF_YEAR, dias)
        }
        fechaTexto = sdf.format(cal.time)
        errorFecha = null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(FDColors.Background.copy(alpha = 0.6f))
                .windowInsetsPadding(WindowInsets.systemBars).imePadding()
                .padding(s.padCard),
            contentAlignment = Alignment.Center
        ) {
        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(s.radiusCard),
            border = BorderStroke(s.borderWidth, FDColors.Border),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(s.padCardLarge),
                verticalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                // Header quiet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.xs)
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, tint = FDColors.Primary, modifier = Modifier.size(s.iconSmall))
                        Column {
                            Text(
                                text = "PRORROGAR VENCIMIENTO",
                                style = FDType.Label.copy(fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp * 0.92f),
                                color = FDColors.TextTertiary
                            )
                            Text(
                                text = "Factura: ${factura.numeroFactura}",
                                style = FDType.Heading3.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextPrimary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = FDColors.TextTertiary)
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = s.separatorH)

                Text(
                    text = "Vencimiento actual: ${factura.fechaVencimientoPago.ifBlank { "Sin fecha fijada" }}",
                    style = FDType.BodySmall.copy(fontSize = s.textBody.value.sp * 0.92f),
                    color = FDColors.TextSecondary
                )

                // Botones Rápidos de Días — pills quiet
                Column(verticalArrangement = Arrangement.spacedBy(s.xs * 0.6f)) {
                    Text("AGREGAR PLAZO RÁPIDO", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.82f, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                    Row(horizontalArrangement = Arrangement.spacedBy(s.xs)) {
                        listOf(7, 15, 30, 45).forEach { dias ->
                            Surface(
                                color = FDColors.TextPrimary.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(s.radiusChip),
                                border = BorderStroke(s.borderWidth * 0.5f, FDColors.Border),
                                modifier = Modifier
                                    .clickable { sumarDias(dias) }
                            ) {
                                Text(
                                    text = "+$dias días",
                                    style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.92f, fontWeight = FontWeight.Bold),
                                    color = FDColors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = s.sm, vertical = s.xs * 0.6f)
                                )
                            }
                        }
                    }
                }

                // Campo Nueva Fecha
                OutlinedTextField(
                    value = fechaTexto,
                    onValueChange = {
                        fechaTexto = it.filter { c -> c.isDigit() || c == '/' || c == '-' }
                        errorFecha = null
                    },
                    label = { Text("Nueva Fecha (DD/MM/AAAA)", fontSize = s.textLabel.value.sp * 0.92f) },
                    singleLine = true,
                    shape = RoundedCornerShape(s.radiusInput),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = FDColors.SurfaceElevated, unfocusedContainerColor = FDColors.SurfaceElevated, focusedBorderColor = FDColors.BorderFocus, unfocusedBorderColor = FDColors.Border),
                    modifier = Modifier.fillMaxWidth().height(s.inputMinH)
                )

                if (errorFecha != null) {
                    Text(
                        text = errorFecha ?: "",
                        style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.Error
                    )
                }

                // Botones — un primario quiet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(s.radiusButton),
                        border = BorderStroke(s.borderWidth, FDColors.Border),
                        modifier = Modifier.height(s.btnMediumH)
                    ) {
                        Text("CANCELAR", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.92f, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary)
                    }
                    Spacer(Modifier.width(s.gapSmall))
                    Button(
                        onClick = {
                            val fTrim = fechaTexto.trim()
                            try {
                                val parsed = sdf.parse(fTrim)
                                if (parsed == null) {
                                    errorFecha = "Formato no válido. Usa DD/MM/AAAA."
                                    return@Button
                                }
                                keyboardController?.hide(); focusManager.clearFocus()
                                onConfirmarProrroga(sdf.format(parsed))
                            } catch (e: Exception) {
                                errorFecha = "Fecha no válida."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Primary,
                            contentColor = FDColors.PrimaryText
                        ),
                        shape = RoundedCornerShape(s.radiusButton),
                        modifier = Modifier.height(s.btnMediumH)
                    ) {
                        Text("APLICAR PRÓRROGA", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.92f, fontWeight = FontWeight.Black))
                    }
                }
            }
        }
        }
    }
}
