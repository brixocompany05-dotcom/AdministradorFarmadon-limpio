package com.app.administradorfarmadon.ventas.nuevaventa.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class ConfetiParticula(
    var x: Float,
    var y: Float,
    val radioW: Float,
    val radioH: Float,
    val velocidadY: Float,
    val velocidadX: Float,
    val color: Color,
    var angulo: Float,
    val velocidadRotacion: Float,
    val oscilacionFrecuencia: Float
)

/**
 * Overlay de celebración de cobro exitoso (Enterprise SaaS 2026).
 *
 * Muestra lluvia de confeti en Canvas a pantalla completa sobre un fondo
 * difuminado semitransparente (blur overlay). Mientras la animación celebra el éxito,
 * la pantalla inferior ya ha quedado reseteada y limpia para la próxima venta.
 */
@Composable
fun AnimacionConfetiOverlay(
    venta: Venta,
    simboloMoneda: String,
    onImprimir: () -> Unit,
    onNuevaVenta: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    val paletaColores = remember {
        listOf(
            Color(0xFF10B981), // Verde Esmeralda
            Color(0xFF06B6D4), // Cyan brillante
            Color(0xFFF59E0B), // Dorado / Ámbar
            Color(0xFF8B5CF6), // Púrpura Enterprise
            Color(0xFFEC4899), // Magenta festivo
            Color(0xFF3B82F6), // Azul Farmadon
            Color(0xFF14B8A6)  // Menta
        )
    }

    val particulas = remember {
        List(85) {
            ConfetiParticula(
                x = Random.nextFloat(),
                y = -Random.nextFloat() * 0.8f,
                radioW = Random.nextFloat() * 10f + 8f,
                radioH = Random.nextFloat() * 6f + 5f,
                velocidadY = Random.nextFloat() * 0.005f + 0.0035f,
                velocidadX = (Random.nextFloat() - 0.5f) * 0.002f,
                color = paletaColores.random(),
                angulo = Random.nextFloat() * 360f,
                velocidadRotacion = (Random.nextFloat() - 0.5f) * 8f,
                oscilacionFrecuencia = Random.nextFloat() * 0.05f + 0.02f
            )
        }
    }

    var tiempoAnimacion by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
        while (true) {
            withFrameNanos { _ ->
                tiempoAnimacion += 0.016f
                for (p in particulas) {
                    p.y += p.velocidadY
                    p.x += p.velocidadX + sin(tiempoAnimacion * p.oscilacionFrecuencia * 20f) * 0.001f
                    p.angulo += p.velocidadRotacion
                    if (p.y > 1.15f) {
                        p.y = -0.1f
                        p.x = Random.nextFloat()
                    }
                }
            }
        }
    }

    val clienteNombre = if (venta.cliente.nombre.isNotBlank()) venta.cliente.nombre else "Consumidor Final"
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
                .background(Color.Black.copy(alpha = 0.55f))
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown &&
                        (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter || keyEvent.key == Key.Spacebar)
                    ) {
                        onNuevaVenta()
                        true
                    } else false
                },
            contentAlignment = Alignment.Center
        ) {
            // Capa 1: Lluvia continua de confeti sobre el fondo
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                for (p in particulas) {
                    val px = p.x * w
                    val py = p.y * h
                    val flipScale = cos(Math.toRadians(p.angulo.toDouble())).toFloat()

                    rotate(degrees = p.angulo, pivot = Offset(px, py)) {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(px - (p.radioW * flipScale) / 2f, py - p.radioH / 2f),
                            size = Size((p.radioW * flipScale).coerceAtLeast(1f), p.radioH)
                        )
                    }
                }
            }

            // Capa 2: Tarjeta ejecutiva de comprobante de venta exitosa
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(250)) + scaleIn(tween(350, easing = LinearEasing), initialScale = 0.88f)
            ) {
                Surface(
                    shape = FDShapes.Large,
                    color = FDColors.Surface,
                    border = BorderStroke(1.5.dp, FDColors.Success.copy(alpha = 0.4f)),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .width(450.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Badge con animación de éxito
                        Surface(
                            color = FDColors.Success.copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = FDColors.Success
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "¡Cobro Exitoso!",
                                style = FDType.Heading2.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp
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

                        // Resumen financiero liquidado
                        Surface(
                            color = FDColors.InputBackground.copy(alpha = 0.6f),
                            shape = FDShapes.Small,
                            border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.4f)),
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
                                if (venta.vuelto > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Vuelto Entregado:", style = FDType.BodySmall, color = FDColors.TextSecondary)
                                        Text(
                                            vueltoStr,
                                            style = FDType.Heading3.copy(
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = FDColors.Success
                                            )
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Cliente:", style = FDType.BodySmall, color = FDColors.TextSecondary)
                                    Text(
                                        clienteNombre,
                                        style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = FDColors.TextPrimary
                                    )
                                }
                            }
                        }

                        // Botones de acción
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
                                    "Imprimir Ticket",
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
                                    .weight(1.2f)
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
}
