package com.app.administradorfarmadon.compras.ui.componentes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import java.util.Locale

@Composable
fun DialogoRegistrarAbono(
    factura: FacturaCompra,
    procesando: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmarAbono: (
        monto: Double,
        metodoPago: String,
        numeroOperacion: String,
        notas: String
    ) -> Unit
) {
    val s = recordarMedidaAdaptativa()
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0
    BackHandler(enabled = true) {
        when {
            isKeyboardVisible -> { keyboardController?.hide(); focusManager.clearFocus(force = true) }
            !procesando -> onDismiss()
            else -> {}
        }
    }

    val saldoPendiente = factura.saldoPendienteReal
    val totalFactura = factura.totalEfectivo
    val totalAbonadoPrevio = factura.totalAbonadoReal

    var montoTexto by remember { mutableStateOf(String.format(Locale.US, "%.2f", saldoPendiente)) }
    var metodoPago by remember { mutableStateOf("Transferencia") }
    var numeroOperacion by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    var errorMonto by remember { mutableStateOf<String?>(null) }

    val montoNumerico = montoTexto.toDoubleOrNull() ?: 0.0
    val montoValido = montoNumerico > 0.0 && montoNumerico <= (saldoPendiente + 0.01)

    Dialog(
        onDismissRequest = { if (!procesando) onDismiss() },
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
                .widthIn(max = 580.dp)
                .fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(s.padCardLarge),
                verticalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                // ── HEADER quiet — una verdad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(s.iconLarge + s.xs)
                                .clip(RoundedCornerShape(s.radiusChip))
                                .background(FDColors.Primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                tint = FDColors.Primary,
                                modifier = Modifier.size(s.iconSmall)
                            )
                        }
                        Column {
                            Text(
                                text = "REGISTRAR ABONO",
                                style = FDType.Label.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = s.textLabel.value.sp * 0.92f,
                                    letterSpacing = 0.8.sp
                                ),
                                color = FDColors.TextTertiary
                            )
                            Text(
                                text = "Factura: ${factura.numeroFactura}",
                                style = FDType.Heading3.copy(fontSize = s.textSubtitle.value.sp),
                                color = FDColors.TextPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (!procesando) onDismiss() },
                        enabled = !procesando
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = FDColors.TextTertiary)
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = s.separatorH)

                // ── ESTADO FINANCIERO — tabla quiet horizontal
                Surface(
                    color = FDColors.SurfaceElevated,
                    shape = RoundedCornerShape(s.radiusInput),
                    border = BorderStroke(s.borderWidth, FDColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(s.padCard * 0.75f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOTAL FACTURA", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.82f, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                            Text("$simboloMoneda " + String.format(Locale.US, "%,.2f", totalFactura), style = FDType.Body.copy(fontSize = s.textBody.value.sp * 0.95f, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                        }
                        Box(modifier = Modifier.height(s.iconMedium).width(s.separatorH).background(FDColors.Border))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("YA ABONADO", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.82f, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                            Text("$simboloMoneda " + String.format(Locale.US, "%,.2f", totalAbonadoPrevio), style = FDType.Body.copy(fontSize = s.textBody.value.sp * 0.95f, fontWeight = FontWeight.Bold), color = FDColors.Success)
                        }
                        Box(modifier = Modifier.height(s.iconMedium).width(s.separatorH).background(FDColors.Border))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SALDO PENDIENTE", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.82f, fontWeight = FontWeight.Black), color = FDColors.Warning)
                            Text("$simboloMoneda " + String.format(Locale.US, "%,.2f", saldoPendiente), style = FDType.Heading3.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Black), color = FDColors.Warning)
                        }
                    }
                }

                // ── CAMPO DE MONTO Y BOTONES RÁPIDOS — un control quiet
                Column(verticalArrangement = Arrangement.spacedBy(s.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MONTO A ABONAR ($simboloMoneda) *",
                            style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.92f, fontWeight = FontWeight.Bold),
                            color = FDColors.TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(s.xs * 0.7f)) {
                            Surface(
                                color = FDColors.Primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(s.radiusChip),
                                modifier = Modifier.clickable {
                                    montoTexto = String.format(Locale.US, "%.2f", saldoPendiente / 2.0)
                                    errorMonto = null
                                }
                            ) {
                                Text("50% (" + String.format(Locale.US, "%.2f", saldoPendiente / 2.0) + ")", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.82f, fontWeight = FontWeight.Bold), color = FDColors.Primary, modifier = Modifier.padding(horizontal = s.xs, vertical = s.xs * 0.4f))
                            }
                            Surface(
                                color = FDColors.Success.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(s.radiusChip),
                                modifier = Modifier.clickable {
                                    montoTexto = String.format(Locale.US, "%.2f", saldoPendiente)
                                    errorMonto = null
                                }
                            ) {
                                Text("Saldar Total", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.82f, fontWeight = FontWeight.Bold), color = FDColors.Success, modifier = Modifier.padding(horizontal = s.xs, vertical = s.xs * 0.4f))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = montoTexto,
                        onValueChange = {
                            montoTexto = it
                            val m = it.toDoubleOrNull()
                            errorMonto = when {
                                m == null || m <= 0 -> "Ingresa un monto válido mayor a 0."
                                m > (saldoPendiente + 0.01) -> "El monto supera el saldo pendiente de $simboloMoneda " + String.format(Locale.US, "%.2f", saldoPendiente)
                                else -> null
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(s.radiusInput),
                        modifier = Modifier.fillMaxWidth().height(s.inputMinH),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FDColors.Primary,
                            unfocusedBorderColor = if (errorMonto != null) FDColors.Error else FDColors.Border,
                            focusedContainerColor = FDColors.SurfaceElevated,
                            unfocusedContainerColor = FDColors.SurfaceElevated,
                            focusedTextColor = FDColors.TextPrimary,
                            unfocusedTextColor = FDColors.TextPrimary
                        )
                    )

                    if (errorMonto != null) {
                        Text(
                            text = errorMonto ?: "",
                            style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.Error
                        )
                    }
                }

                // ── MÉTODO DE PAGO — pills quiet
                Column(verticalArrangement = Arrangement.spacedBy(s.xs * 0.6f)) {
                    Text(
                        text = "MÉTODO DE PAGO",
                        style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.92f, fontWeight = FontWeight.Bold),
                        color = FDColors.TextTertiary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(s.xs)
                    ) {
                        listOf("Transferencia", "Efectivo", "Yape/Plin", "Cheque").forEach { metodo ->
                            FilterChip(
                                selected = metodoPago == metodo,
                                onClick = { metodoPago = metodo },
                                label = { Text(metodo, fontSize = s.textLabel.value.sp * 0.92f, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FDColors.Primary, selectedLabelColor = FDColors.PrimaryText),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ── N° OPERACIÓN
                OutlinedTextField(
                    value = numeroOperacion,
                    onValueChange = { numeroOperacion = it.uppercase() },
                    label = { Text("N° Operación Bancaria", fontSize = s.textLabel.value.sp * 0.92f) },
                    placeholder = { Text("Número de operación bancaria", fontSize = s.textBody.value.sp * 0.88f, color = FDColors.TextTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(s.radiusInput),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = FDColors.SurfaceElevated, unfocusedContainerColor = FDColors.SurfaceElevated, focusedBorderColor = FDColors.BorderFocus, unfocusedBorderColor = FDColors.Border),
                    modifier = Modifier.fillMaxWidth().height(s.inputMinH)
                )

                // ── NOTAS
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas (Opcional)", fontSize = s.textLabel.value.sp * 0.92f) },
                    placeholder = { Text("Notas u observaciones", fontSize = s.textBody.value.sp * 0.88f, color = FDColors.TextTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(s.radiusInput),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = FDColors.SurfaceElevated, unfocusedContainerColor = FDColors.SurfaceElevated, focusedBorderColor = FDColors.BorderFocus, unfocusedBorderColor = FDColors.Border),
                    modifier = Modifier.fillMaxWidth().height(s.inputMinH)
                )

                // ── BOTONES DE ACCIÓN — un primario quiet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { if (!procesando) onDismiss() },
                        enabled = !procesando,
                        shape = RoundedCornerShape(s.radiusButton),
                        border = BorderStroke(s.borderWidth, FDColors.Border),
                        modifier = Modifier.height(s.btnMediumH)
                    ) {
                        Text("CANCELAR", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.92f, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary)
                    }

                    Spacer(Modifier.width(s.gapSmall))

                    Button(
                        onClick = {
                            if (!montoValido) {
                                errorMonto = "Verifica el monto ingresado."
                                return@Button
                            }
                            keyboardController?.hide(); focusManager.clearFocus()
                            onConfirmarAbono(
                                montoNumerico,
                                metodoPago,
                                numeroOperacion,
                                notas
                            )
                        },
                        enabled = !procesando && montoValido,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Primary,
                            contentColor = FDColors.PrimaryText
                        ),
                        shape = RoundedCornerShape(s.radiusButton),
                        modifier = Modifier.height(s.btnMediumH)
                    ) {
                        if (procesando) {
                            CircularProgressIndicator(modifier = Modifier.size(s.iconSmall), color = FDColors.PrimaryText, strokeWidth = 2.dp)
                            Spacer(Modifier.width(s.xs))
                            Text("PROCESANDO...", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.92f, fontWeight = FontWeight.Black))
                        } else {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(s.iconSmall))
                            Spacer(Modifier.width(s.xs * 0.7f))
                            Text("CONFIRMAR ABONO", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.92f, fontWeight = FontWeight.Black))
                        }
                    }
                }
            }
        }
        }
    }
}
