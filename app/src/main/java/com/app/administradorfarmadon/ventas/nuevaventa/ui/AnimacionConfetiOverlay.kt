package com.app.administradorfarmadon.ventas.nuevaventa.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import java.util.Locale

/**
 * Diálogo Enterprise de confirmación y comprobante de venta exitosa (Farmadon POS).
 *
 * Máxima velocidad y sobriedad:
 * - Cero partículas ni animaciones infinitas que congelen o gasten GPU.
 * - Desglose financiero limpio con comprobante oficial.
 * - Banner destacado de vuelto en letras grandes de alto contraste.
 * - Teclas rápidas: Enter o Espacio para pasar de inmediato a la siguiente venta; P o I para imprimir.
 */
@Composable
fun DialogoVentaExitosa(
    venta: Venta,
    simboloMoneda: String,
    onImprimir: () -> Unit,
    onNuevaVenta: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    val clienteNombre = if (venta.cliente.nombre.isNotBlank()) venta.cliente.nombre else "Consumidor Final"
    val docCliente = if (venta.cliente.numeroDocumento.isNotBlank()) " (${venta.cliente.tipoDocumento}: ${venta.cliente.numeroDocumento})" else ""
    val totalStr = "$simboloMoneda " + String.format(Locale.US, "%.2f", venta.total)
    val vueltoStr = "$simboloMoneda " + String.format(Locale.US, "%.2f", venta.vuelto)

    Dialog(
        onDismissRequest = onNuevaVenta,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.Escape -> {
                                onNuevaVenta()
                                true
                            }
                            Key.P, Key.I -> {
                                onImprimir()
                                true
                            }
                            else -> false
                        }
                    } else false
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = FDShapes.Large,
                color = FDColors.Surface,
                border = BorderStroke(1.5.dp, FDColors.Success.copy(alpha = 0.5f)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .width(460.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Badge de éxito limpio
                    Surface(
                        color = FDColors.Success.copy(alpha = 0.12f),
                        shape = CircleShape,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = FDColors.Success
                            )
                        }
                    }

                    // Título y Comprobante
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            "¡Cobro Exitoso!",
                            style = FDType.Heading2.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            ),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            "${venta.tipoComprobante}: ${venta.numeroCompleto}",
                            style = FDType.Body.copy(
                                fontWeight = FontWeight.Black,
                                color = FDColors.Primary,
                                fontSize = 14.sp
                            )
                        )
                    }

                    // Banner Destacado de Vuelto (si corresponde)
                    if (venta.vuelto > 0.0) {
                        Surface(
                            color = FDColors.Success.copy(alpha = 0.10f),
                            shape = FDShapes.Small,
                            border = BorderStroke(1.5.dp, FDColors.Success.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    "VUELTO A ENTREGAR AL CLIENTE",
                                    style = FDType.Caption.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = FDColors.Success
                                    )
                                )
                                Text(
                                    vueltoStr,
                                    style = FDType.Heading1.copy(
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        color = FDColors.Success
                                    )
                                )
                            }
                        }
                    }

                    // Resumen financiero de la transacción
                    Surface(
                        color = FDColors.InputBackground.copy(alpha = 0.6f),
                        shape = FDShapes.Small,
                        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Cobrado:", style = FDType.BodySmall, color = FDColors.TextSecondary)
                                Text(
                                    totalStr,
                                    style = FDType.Heading3.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
                                    color = FDColors.TextPrimary
                                )
                            }

                            if (venta.pagos.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Método(s) de Pago:", style = FDType.BodySmall, color = FDColors.TextSecondary)
                                    val resumenPagos = venta.pagos.joinToString(" + ") { p ->
                                        "${p.nombreMetodo} ($simboloMoneda ${String.format(Locale.US, "%.2f", p.monto)})"
                                    }
                                    Text(
                                        resumenPagos,
                                        style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                        color = FDColors.TextPrimary
                                    )
                                }
                            }

                            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cliente:", style = FDType.BodySmall, color = FDColors.TextSecondary)
                                Text(
                                    "$clienteNombre$docCliente",
                                    style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                    color = FDColors.TextPrimary
                                )
                            }
                        }
                    }

                    // Botones de acción rápida
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onImprimir,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = FDShapes.Small,
                            border = BorderStroke(1.dp, FDColors.Primary)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = FDColors.Primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Imprimir (P)",
                                style = FDType.Label.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FDColors.Primary,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Button(
                            onClick = onNuevaVenta,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp),
                            shape = FDShapes.Small,
                            colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)
                        ) {
                            Text(
                                "NUEVA VENTA (Enter)",
                                style = FDType.Label.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compatibilidad hacia atrás con llamadas existentes a AnimacionConfetiOverlay.
 * Redirige de forma directa al modal enterprise sin sobrecarga de partículas ni loops.
 */
@Composable
fun AnimacionConfetiOverlay(
    venta: Venta,
    simboloMoneda: String,
    onImprimir: () -> Unit,
    onNuevaVenta: () -> Unit
) {
    DialogoVentaExitosa(
        venta = venta,
        simboloMoneda = simboloMoneda,
        onImprimir = onImprimir,
        onNuevaVenta = onNuevaVenta
    )
}
