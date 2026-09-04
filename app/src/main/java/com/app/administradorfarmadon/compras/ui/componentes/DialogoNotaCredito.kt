package com.app.administradorfarmadon.compras.ui.componentes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.configuracion.preferencias_sistema.teclado.ui.AplicarBloqueoTecladoVentana
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import java.util.Locale

/**
 * Nota de crédito / ajuste del total de una factura (mercadería facturada que nunca llegó
 * o llegó incompleta). Solo dueño/administración, siempre con documento del proveedor.
 */
@Composable
fun DialogoNotaCredito(
    factura: FacturaCompra,
    procesando: Boolean = false,
    estadoFactura: String? = null,
    autorizadoPlata: Boolean,
    onDismiss: () -> Unit,
    onConfirmarNota: (numeroDocumento: String, monto: Double, motivo: String) -> Unit
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

    val esAnulada = estadoFactura.equals("ANULADA", ignoreCase = true)
    val totalPapel = factura.totalPapel
    val totalAjustes = factura.totalAjustes
    val maximoAjustable = (totalPapel - totalAjustes).coerceAtLeast(0.0)
    val totalAbonado = factura.totalAbonadoReal
    val saldoPendiente = factura.saldoPendienteReal

    var numeroDocumento by remember { mutableStateOf("") }
    var montoTexto by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var errorMonto by remember { mutableStateOf<String?>(null) }
    var errorDoc by remember { mutableStateOf<String?>(null) }
    var errorMotivo by remember { mutableStateOf<String?>(null) }

    // Comparación en céntimos exactos: ni un céntimo más del papel se descuenta.
    val montoNumerico = montoTexto.replace(',', '.').toDoubleOrNull()?.let { Math.round(it * 100.0) / 100.0 } ?: 0.0
    val maximoAjustableRedondeado = Math.round(maximoAjustable * 100.0) / 100.0
    val montoValido = montoNumerico > 0.0 && montoNumerico <= maximoAjustableRedondeado
    val documentoValido = numeroDocumento.trim().isNotBlank()
    val motivoValido = motivo.trim().length >= 5
    val puedeConfirmar = autorizadoPlata && !esAnulada && documentoValido && montoValido && motivoValido && !procesando

    val totalDespues = (maximoAjustable - montoNumerico).coerceAtLeast(0.0)
    val saldoDespues = (totalDespues - totalAbonado).coerceAtLeast(0.0)
    val quedaraSaldada = saldoDespues <= 0.01

    Dialog(
        onDismissRequest = { if (!procesando) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Regla "Bloquear Teclado": este diálogo tampoco abre el teclado si está activada.
        AplicarBloqueoTecladoVentana()
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
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(0.94f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(s.padCardLarge),
                    verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                ) {
                    // HEADER
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
                                    .background(if (esAnulada) FDColors.Error.copy(alpha = 0.12f) else FDColors.Primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = if (esAnulada) FDColors.Error else FDColors.Primary,
                                    modifier = Modifier.size(s.iconSmall)
                                )
                            }
                            Column {
                                Text(
                                    text = if (esAnulada) "FACTURA ANULADA" else "NOTA DE CRÉDITO",
                                    style = FDType.Label.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = s.textLabel.value.sp * 0.92f,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = if (esAnulada) FDColors.Error else FDColors.TextTertiary
                                )
                                Text(
                                    text = "Factura: ${factura.numeroFactura} · ${factura.proveedorNombre}",
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

                    if (esAnulada) {
                        Surface(
                            color = FDColors.Error.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(s.radiusInput),
                            border = BorderStroke(s.borderWidth, FDColors.Error.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(s.padCard),
                                verticalArrangement = Arrangement.spacedBy(s.xs * 0.7f)
                            ) {
                                Text("Esta factura fue anulada.", style = FDType.Body.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Bold), color = FDColors.Error)
                                Text("No se puede registrar una nota de crédito.", style = FDType.BodySmall.copy(fontSize = s.textBody.value.sp * 0.92f), color = FDColors.TextSecondary)
                            }
                        }
                    } else {
                        // VERDAD ACTUAL
                        Surface(
                            color = FDColors.SurfaceElevated,
                            shape = RoundedCornerShape(s.radiusInput),
                            border = BorderStroke(s.borderWidth * 0.7f, FDColors.Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(s.padCard),
                                verticalArrangement = Arrangement.spacedBy(s.xs * 0.7f)
                            ) {
                                FilaNotaCredito("Total del papel", "$simboloMoneda " + String.format(Locale.US, "%.2f", totalPapel))
                                if (totalAjustes > 0.01) {
                                    FilaNotaCredito("Notas ya registradas", "− $simboloMoneda " + String.format(Locale.US, "%.2f", totalAjustes), color = FDColors.Warning)
                                }
                                FilaNotaCredito("Abonado", "$simboloMoneda " + String.format(Locale.US, "%.2f", totalAbonado), color = FDColors.Success)
                                FilaNotaCredito("Saldo pendiente", "$simboloMoneda " + String.format(Locale.US, "%.2f", saldoPendiente), color = if (saldoPendiente > 0.01) FDColors.Error else FDColors.Success)
                                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = s.separatorH)
                                FilaNotaCredito("Máximo ajustable", "$simboloMoneda " + String.format(Locale.US, "%.2f", maximoAjustable), color = FDColors.Primary)
                            }
                        }

                        // CAMPO NÚMERO DE NOTA
                        OutlinedTextField(
                            value = numeroDocumento,
                            onValueChange = {
                                numeroDocumento = it.uppercase()
                                errorDoc = null
                            },
                            label = { Text("N° DE NOTA DE CRÉDITO DEL PROVEEDOR *", fontSize = s.textLabel.value.sp * 0.92f) },
                            placeholder = { Text("Número impreso en el documento del proveedor", fontSize = s.textBody.value.sp * 0.88f, color = FDColors.TextTertiary) },
                            isError = errorDoc != null,
                            singleLine = true,
                            enabled = !procesando && autorizadoPlata,
                            shape = RoundedCornerShape(s.radiusInput),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FDColors.SurfaceElevated,
                                unfocusedContainerColor = FDColors.SurfaceElevated,
                                focusedBorderColor = FDColors.BorderFocus,
                                unfocusedBorderColor = FDColors.Border,
                                focusedTextColor = FDColors.TextPrimary,
                                unfocusedTextColor = FDColors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().height(s.inputMinH)
                        )
                        if (errorDoc != null) {
                            Text(errorDoc ?: "", style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold), color = FDColors.Error)
                        }

                        // CAMPO MONTO
                        OutlinedTextField(
                            value = montoTexto,
                            onValueChange = {
                                montoTexto = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                                val mParse = montoTexto.replace(',', '.').toDoubleOrNull()
                                val m = mParse?.let { v -> Math.round(v * 100.0) / 100.0 } ?: 0.0
                                errorMonto = when {
                                    montoTexto.isBlank() -> null
                                    mParse == null -> "Ese monto no es un número válido (ej. 150.50)."
                                    m <= 0.0 -> "Ingresa un monto mayor a 0."
                                    m > maximoAjustableRedondeado -> "El monto supera el máximo ajustable de $simboloMoneda " + String.format(Locale.US, "%.2f", maximoAjustableRedondeado)
                                    else -> null
                                }
                            },
                            label = { Text("MONTO DE LA NOTA ($simboloMoneda) *", fontSize = s.textLabel.value.sp * 0.92f) },
                            isError = errorMonto != null,
                            singleLine = true,
                            enabled = !procesando && autorizadoPlata,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(s.radiusInput),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FDColors.SurfaceElevated,
                                unfocusedContainerColor = FDColors.SurfaceElevated,
                                focusedBorderColor = if (errorMonto != null) FDColors.Error else FDColors.BorderFocus,
                                unfocusedBorderColor = if (errorMonto != null) FDColors.Error else FDColors.Border,
                                focusedTextColor = FDColors.TextPrimary,
                                unfocusedTextColor = FDColors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().height(s.inputMinH)
                        )
                        if (errorMonto != null) {
                            Text(errorMonto ?: "", style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold), color = FDColors.Error)
                        }

                        // CAMPO MOTIVO
                        OutlinedTextField(
                            value = motivo,
                            onValueChange = {
                                motivo = it
                                errorMotivo = null
                            },
                            label = { Text("MOTIVO (POR QUÉ EL PROVEEDOR NO ENTREGA) *", fontSize = s.textLabel.value.sp * 0.92f) },
                            placeholder = { Text("Describe el motivo real del papel", fontSize = s.textBody.value.sp * 0.88f, color = FDColors.TextTertiary) },
                            isError = errorMotivo != null,
                            singleLine = true,
                            enabled = !procesando && autorizadoPlata,
                            shape = RoundedCornerShape(s.radiusInput),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FDColors.SurfaceElevated,
                                unfocusedContainerColor = FDColors.SurfaceElevated,
                                focusedBorderColor = FDColors.BorderFocus,
                                unfocusedBorderColor = FDColors.Border,
                                focusedTextColor = FDColors.TextPrimary,
                                unfocusedTextColor = FDColors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().height(s.inputMinH)
                        )
                        if (errorMotivo != null) {
                            Text(errorMotivo ?: "", style = FDType.Label.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Bold), color = FDColors.Error)
                        }

                        if (!autorizadoPlata) {
                            Surface(
                                color = FDColors.WarningSubtle,
                                shape = RoundedCornerShape(s.radiusInput),
                                border = BorderStroke(s.borderWidth * 0.8f, FDColors.Warning.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(s.padCard * 0.7f),
                                    horizontalArrangement = Arrangement.spacedBy(s.gapSmall),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.Lock, null, tint = FDColors.Warning, modifier = Modifier.size(16.dp))
                                    Text(
                                        "Solo el dueño o administración puede reducir el total de una factura. Pídele ayuda a un usuario de administración.",
                                        style = FDType.BodySmall.copy(fontSize = 11.5.sp),
                                        color = FDColors.TextPrimary
                                    )
                                }
                            }
                        }

                        if (montoValido && documentoValido) {
                            Surface(
                                color = FDColors.Primary.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(s.radiusInput),
                                border = BorderStroke(s.borderWidth * 0.7f, FDColors.Primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(s.padCard)) {
                                    Text(
                                        "QUEDARÁ ESCRITO",
                                        style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp),
                                        color = FDColors.TextTertiary
                                    )
                                    Text(
                                        "Total del papel ${String.format(Locale.US, "%.2f", totalPapel)} → total ajustado ${String.format(Locale.US, "%.2f", totalDespues)}. " +
                                            if (quedaraSaldada) {
                                                val excesoVista = (totalAbonado - totalDespues).coerceAtLeast(0.0)
                                                if (excesoVista > 0.01)
                                                    "La factura quedará SALDADA y los ${String.format(Locale.US, "%.2f", excesoVista)} que ya pagaste de más nacerán como SALDO A FAVOR del proveedor (aparecerá para cobrarlo o usarlo en otra compra)."
                                                else "La factura quedará SALDADA."
                                            } else "Quedará un saldo de ${String.format(Locale.US, "%.2f", saldoDespues)}.",
                                        style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                                        color = FDColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // BOTONES
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
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
                                if (esAnulada) return@Button
                                if (!documentoValido) {
                                    errorDoc = "El número de la nota de crédito es obligatorio."
                                    return@Button
                                }
                                if (!montoValido) {
                                    errorMonto = "Verifica el monto ingresado."
                                    return@Button
                                }
                                if (!motivoValido) {
                                    errorMotivo = "El motivo debe tener al menos 5 caracteres."
                                    return@Button
                                }
                                keyboardController?.hide(); focusManager.clearFocus()
                                onConfirmarNota(numeroDocumento.trim().uppercase(), montoNumerico, motivo.trim())
                            },
                            enabled = puedeConfirmar,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FDColors.Primary,
                                contentColor = FDColors.PrimaryText,
                                disabledContainerColor = FDColors.Primary.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(s.radiusButton),
                            modifier = Modifier.height(s.btnMediumH)
                        ) {
                            if (procesando) {
                                CircularProgressIndicator(modifier = Modifier.size(s.iconSmall), color = FDColors.PrimaryText, strokeWidth = 2.dp)
                                Spacer(Modifier.width(s.xs))
                                Text("REGISTRANDO...", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.92f, fontWeight = FontWeight.Black))
                            } else {
                                Text("CONFIRMAR NOTA DE CRÉDITO", style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.92f, fontWeight = FontWeight.Black))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaNotaCredito(etiqueta: String, valor: String, color: androidx.compose.ui.graphics.Color = FDColors.TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = FDType.BodySmall.copy(fontSize = 11.5.sp), color = FDColors.TextTertiary)
        Text(valor, style = FDType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = color)
    }
}
