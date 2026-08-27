package com.app.administradorfarmadon.inventario.ingresarstockproductos.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.inventario.ingresarstockproductos.logica.StockEntryState

/**
 * Panel Ejecutivo Derecho: Resumen Financiero, Conciliación de Factura y Confirmación.
 * Limpio, legible y estructurado sin sobreingeniería (1+1=2).
 */
@Composable
fun PanelLiquidacionFactura(
    state: StockEntryState,
    onConfirmarIngreso: () -> Unit,
    onCambiarStockMinimo: () -> Unit = {},
    onStockMinimoChanged: (String) -> Unit = {},
    onConfirmarStockMinimo: () -> Unit = {},
    onCancelarStockMinimo: () -> Unit = {},
    modifier: Modifier = Modifier,
    sinMarco: Boolean = false,
    sinBoton: Boolean = false
) {
    val contenido: @Composable () -> Unit = {
        Column(
            modifier = if (sinBoton) Modifier.fillMaxWidth().padding(if (sinMarco) 0.dp else 20.dp)
            else Modifier.fillMaxSize().padding(if (sinMarco) 0.dp else 20.dp),
            verticalArrangement = if (sinBoton) Arrangement.spacedBy(16.dp) else Arrangement.SpaceBetween
        ) {
            Column(
                modifier = if (sinBoton) Modifier.fillMaxWidth() else Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. TÍTULO DEL PANEL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Resumen",
                        style = FDType.Heading3.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                    )
                    Icon(Icons.Outlined.Receipt, null, tint = FDColors.Primary, modifier = Modifier.size(18.dp))
                }

                HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)

                // 2. TARJETA STOCK — barra 6dp nítida + pill
                Surface(
                    color = FDColors.Background,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, FDColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Stock", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp))
                        // Barra visual hermosa 6dp
                        Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(100.dp)).background(FDColors.Border)) {
                            val actual = state.stockActual.coerceAtLeast(0.0)
                            val nuevo = state.nuevoStockProyectado.coerceAtLeast(actual)
                            val total = nuevo.coerceAtLeast(1.0)
                            val fracActual = (actual / total).toFloat().coerceIn(0f, 1f)
                            when {
                                fracActual <= 0f -> Box(modifier = Modifier.fillMaxSize().background(FDColors.Success.copy(alpha = 0.35f)))
                                fracActual >= 1f -> Box(modifier = Modifier.fillMaxSize().background(FDColors.TextTertiary.copy(alpha = 0.5f)))
                                else -> {
                                    Box(modifier = Modifier.fillMaxHeight().weight(fracActual).background(FDColors.TextTertiary.copy(alpha = 0.5f)))
                                    Box(modifier = Modifier.fillMaxHeight().weight(1f - fracActual).background(FDColors.Success))
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Ahora", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextSecondary))
                                Text("${Math.round(state.stockActual).toInt()} ${state.nombreEmpaquePlural}", style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.TextTertiary))
                            }
                            Surface(color = FDColors.Success.copy(alpha = 0.12f), shape = RoundedCornerShape(100.dp), border = BorderStroke(0.5.dp, FDColors.Success.copy(alpha = 0.32f))) {
                                Text("+${state.cantidadTotalIngresada.toInt()}", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FDColors.Success), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.End) {
                                Text("Quedará", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextSecondary))
                                Text("${Math.round(state.nuevoStockProyectado).toInt()} ${state.nombreEmpaquePlural}", style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.Success))
                            }
                        }
                    }
                }

                // 3. DETALLE — lista plana nítida 0.5dp
                Surface(
                    color = FDColors.Background,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, FDColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text("Detalle", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp), modifier = Modifier.padding(bottom = 8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lote", style = FDType.BodySmall.copy(color = FDColors.TextSecondary))
                            Text(
                                text = state.numeroLote.ifBlank { "---" },
                                style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = FDColors.TextPrimary)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Vence", style = FDType.BodySmall.copy(color = FDColors.TextSecondary))
                            Text(
                                text = state.fechaVencimiento,
                                style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = FDColors.TextPrimary)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cantidad", style = FDType.BodySmall.copy(color = FDColors.TextSecondary))
                            val bonifTxt = if (state.bonificacionUnidadesTotales > 0) " (+${state.bonificacionUnidadesTotales.toInt()} gratis)" else ""
                            val eqContenido = if (state.contenidoPorUnidad > 1) " (= ${(state.cantidadTotalIngresada * state.contenidoPorUnidad).toInt()} ${state.unidadContenido.ifBlank { "Und" }})" else ""
                            Text(
                                text = "+${state.cantidadTotalIngresada.toInt()} ${state.nombreEmpaquePlural}$bonifTxt$eqContenido",
                                style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.Primary, fontSize = 11.sp)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pagaste", style = FDType.BodySmall.copy(color = FDColors.TextSecondary))
                            Text(
                                text = "$ ${String.format("%.2f", state.costoTotalIngreso)}",
                                style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.Primary)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Por ${state.empaque.ifBlank { "unidad" }}", style = FDType.BodySmall.copy(color = FDColors.TextSecondary))
                            Text(
                                text = if (state.costoUnitarioCalculado > 0) "$ ${String.format(java.util.Locale.US, "%.2f", state.costoUnitarioCalculado)} / ${state.empaque.ifBlank { "und" }}" else "---",
                                style = FDType.BodySmall.copy(fontWeight = FontWeight.SemiBold, color = if (state.costoUnitarioCalculado > 0) FDColors.TextPrimary else FDColors.TextTertiary)
                            )
                        }

                        if (state.margenGananciaEstimado != null) {
                            val margen = state.margenGananciaEstimado!!
                            val margenColor = when {
                                margen >= 30.0 -> FDColors.Success
                                margen >= 15.0 -> FDColors.Primary
                                margen > 0.0 -> FDColors.Warning
                                else -> FDColors.Error
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Margen est.", style = FDType.BodySmall.copy(color = FDColors.TextSecondary))
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f", margen)}% (Venta: $ ${String.format(java.util.Locale.US, "%.2f", state.precioVentaActual)})",
                                    style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = margenColor, fontSize = 11.sp)
                                )
                            }
                        }

                        if (state.diferenciaCostoPorcentual != null && kotlin.math.abs(state.diferenciaCostoPorcentual!!) >= 0.5) {
                            val diff = state.diferenciaCostoPorcentual!!
                            val diffColor = if (diff > 0) FDColors.Warning else FDColors.Success
                            val sign = if (diff > 0) "+" else ""
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Variación costo", style = FDType.BodySmall.copy(color = FDColors.TextSecondary))
                                Text(
                                    text = "$sign${String.format(java.util.Locale.US, "%.1f", diff)}% vs anterior ($ ${String.format(java.util.Locale.US, "%.2f", state.ultimoPrecioCompraRegistrado)})",
                                    style = FDType.BodySmall.copy(fontWeight = FontWeight.SemiBold, color = diffColor, fontSize = 10.5.sp)
                                )
                            }
                        }

                        if (state.numeroFactura.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Factura", style = FDType.BodySmall.copy(color = FDColors.TextSecondary))
                                Text(
                                    text = state.numeroFactura,
                                    style = FDType.BodySmall.copy(fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, color = FDColors.TextPrimary)
                                )
                            }
                        }

                        if (state.proveedorNombre.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Proveedor", style = FDType.BodySmall.copy(color = FDColors.TextSecondary))
                                Text(
                                    text = state.proveedorNombre,
                                    style = FDType.BodySmall.copy(fontWeight = FontWeight.SemiBold, color = FDColors.TextPrimary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (state.numeroFactura.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pago", style = FDType.BodySmall.copy(color = FDColors.TextSecondary))
                                Text(
                                    text = state.condicionPagoResumen,
                                    style = FDType.BodySmall.copy(fontSize = 11.sp, color = FDColors.TextTertiary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 4. CONCILIACIÓN DE FACTURA (SI TIENE TOPE EN PAPEL)
                val totalFactura = state.montoTotalFacturaDouble
                val acumulado = state.montoAcumuladoProyectadoFactura
                if (totalFactura > 0 && state.numeroFactura.isNotBlank()) {
                    Surface(
                        color = FDColors.Background,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, if (state.excedeMontoFactura) FDColors.Error else FDColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total factura:", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary))
                                Text("$ ${String.format("%.2f", totalFactura)}", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ya cargado:", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary))
                                Text(
                                    text = "$ ${String.format("%.2f", acumulado)}",
                                    style = FDType.Caption.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.excedeMontoFactura) FDColors.Error else if (acumulado == totalFactura) FDColors.Success else FDColors.Primary
                                    )
                                )
                            }

                            LinearProgressIndicator(
                                progress = { state.porcentajeFacturaProgreso },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp),
                                color = if (state.excedeMontoFactura) FDColors.Error else if (acumulado == totalFactura) FDColors.Success else FDColors.Primary,
                                trackColor = FDColors.Border
                            )

                            if (state.facturaDetectadaInfo != null && state.facturaDetectadaInfo.items.isNotEmpty()) {
                                HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Productos en esta factura:", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary))
                                        Text("${state.facturaDetectadaInfo.items.size} producto(s)", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextPrimary))
                                    }
                                    // Lista simple sin complejidad: hasta 3 filas, resto colapsado
                                    state.facturaDetectadaInfo.items.take(3).forEach { item ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                text = "${item.productoNombre} • ${item.loteNumero}",
                                                style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextSecondary),
                                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${item.cantidadTotal.toInt()} und • $${String.format("%.2f", item.costoTotal)}",
                                                style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextPrimary),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    if (state.facturaDetectadaInfo.items.size > 3) {
                                        Text("+ ${state.facturaDetectadaInfo.items.size - 3} más", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary))
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. AVISO DE REPOSICIÓN — propuesta justa y editable; jamás deja el mínimo en 0.
                // Cero campos obligatorios: se propone, se explica y se cambia con un toque.
                if (state.mostrarPropuestaStockMinimo) {
                    Surface(
                        color = FDColors.Background,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, FDColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "AVISO DE REPOSICIÓN",
                                style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.Bold)
                            )
                            if (!state.stockMinimoEditando) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Te avisaremos cuando queden ${state.stockMinimoAplicar} ${state.nombreEmpaquePlural.lowercase()}",
                                            style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                                        )
                                        Text(
                                            text = "Regla inicial: 1 de cada 5 que compras. Así pides a tiempo.",
                                            style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary)
                                        )
                                    }
                                    TextButton(onClick = onCambiarStockMinimo, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                        Text("Cambiar", style = FDType.Label.copy(fontSize = 11.5.sp, color = FDColors.Primary))
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = state.stockMinimoPersonalizado,
                                        onValueChange = onStockMinimoChanged,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = onConfirmarStockMinimo) {
                                        Icon(Icons.Default.CheckCircle, "Confirmar", tint = FDColors.Success, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = onCancelarStockMinimo) {
                                        Icon(Icons.Default.Close, "Cancelar", tint = FDColors.TextTertiary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text(
                                    text = "Mínimo sugerido: ${state.stockMinimoPropuesto}. Nunca menor a 1.",
                                    style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary)
                                )
                            }
                        }
                    }
                }

                // 6. ALERTA VISUAL SI EXCEDE EL TOPE DE LA FACTURA
                if (state.excedeMontoFactura) {
                    Surface(
                        color = FDColors.Error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, FDColors.Error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = FDColors.Error, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Ya supera lo que dice la factura ($$totalFactura). Corrige el costo antes de guardar.",
                                style = FDType.Caption.copy(color = FDColors.Error, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // 6. BOTÓN GUARDAR — fijo abajo, sin texto helper
            if (!sinBoton) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onConfirmarIngreso,
                        enabled = state.esFormularioValido && !state.estaGuardando,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Primary,
                            contentColor = FDColors.PrimaryText,
                            disabledContainerColor = FDColors.Primary.copy(alpha = 0.35f),
                            disabledContentColor = FDColors.PrimaryText.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(50.dp).bounceClick()
                    ) {
                        if (state.estaGuardando) {
                            CircularProgressIndicator(color = FDColors.PrimaryText, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Guardando...", style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        } else {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Guardar ingreso", style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
    if (sinMarco) {
        Box(modifier = modifier) { contenido() }
    } else {
        Surface(
            modifier = modifier.fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            color = if (FDColors.isDark) FDColors.SurfaceElevated else FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border)
        ) {
            contenido()
        }
    }
}
