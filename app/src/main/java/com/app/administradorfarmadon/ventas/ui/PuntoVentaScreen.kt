package com.app.administradorfarmadon.ventas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import java.util.*

@Composable
fun PuntoVentaScreen() {
    val s = recordarMedidaAdaptativa()
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }
    
    var busquedaProducto by remember { mutableStateOf("") }
    var metodoPagoSeleccionado by remember { mutableStateOf("EFECTIVO") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = s.padScreenH, vertical = s.padScreenV),
        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
    ) {
        // ── CABECERA ESTRATÉGICA (KPIs del Turno) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = s.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "VENTAS DEL TURNO",
                        style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                        color = FDColors.TextTertiary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$simboloMoneda 1,250.00",
                        style = FDType.Numeric.copy(fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
                        color = FDColors.TextPrimary
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "ARTÍCULOS",
                        style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                        color = FDColors.TextTertiary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "42",
                        style = FDType.Numeric.copy(fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
                        color = FDColors.TextPrimary
                    )
                }
            }

            // Reloj o Info del Cajero
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "CAJERO: ${SessionManager.nombreUsuario.uppercase()}",
                    style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.Primary
                )
                Text(
                    text = "TURNO MAÑANA · SEDE PRINCIPAL",
                    style = FDType.Caption,
                    color = FDColors.TextTertiary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
        ) {
            // ══════════════════════════════════════════════════════════════════════════
            // PANEL IZQUIERDO: CARRITO DE COMPRA (60%)
            // ══════════════════════════════════════════════════════════════════════════
            Surface(
                color = FDColors.Surface,
                shape = FDShapes.Medium,
                border = BorderStroke(s.borderWidth, FDColors.Border),
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ── BUSCADOR DOMINANTE ──
                    OutlinedTextField(
                        value = busquedaProducto,
                        onValueChange = { busquedaProducto = it },
                        placeholder = { Text("Escanee código de barras o busque producto...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, null, tint = FDColors.Primary) },
                        trailingIcon = { 
                            if (busquedaProducto.isNotEmpty()) {
                                IconButton(onClick = { busquedaProducto = "" }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = FDShapes.Small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FDColors.InputBackground,
                            unfocusedContainerColor = FDColors.InputBackground,
                            focusedBorderColor = FDColors.Primary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .height(60.dp)
                    )

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // ── LISTA DE ALTA DENSIDAD ──
                    val itemsDemo = listOf(
                        demoItem("PARACETAMOL 500mg X 10 TAB", 2, 5.50),
                        demoItem("AMOXICILINA 1g - TABLETAS", 1, 18.20),
                        demoItem("ALCOHOL EN GEL 250ml", 1, 12.00),
                        demoItem("GASAS ESTÉRILES 10x10 (SOBRE)", 5, 1.50),
                        demoItem("JERINGA 5ml CON AGUJA X 10", 1, 8.50)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(itemsDemo) { item ->
                            ItemVentaRow(item, simboloMoneda)
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = FDColors.Border.copy(alpha = 0.3f),
                                thickness = 0.5.dp
                            )
                        }
                    }

                    // INFO SUTIL AL PIE DEL CARRITO
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Info, null, tint = FDColors.TextTertiary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Pistola de scanner lista para entrada directa",
                            style = FDType.Caption,
                            color = FDColors.TextTertiary
                        )
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════════════════
            // PANEL DERECHO: LIQUIDACIÓN (40%)
            // ══════════════════════════════════════════════════════════════════════════
            Surface(
                color = FDColors.Surface,
                shape = FDShapes.Medium,
                border = BorderStroke(s.borderWidth, FDColors.Border),
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // PARTE SUPERIOR: TOTAL Y PAGO
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // KPI TOTAL GIGANTE
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "TOTAL A COBRAR",
                                style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                color = FDColors.TextTertiary
                            )
                            Text(
                                text = "$simboloMoneda 41.20",
                                style = FDType.Numeric.copy(fontSize = 48.sp, fontWeight = FontWeight.Black),
                                color = FDColors.TextPrimary
                            )
                        }

                        // SELECTOR DE PAGO (APPLE STYLE)
                        Text(
                            text = "MÉTODO DE PAGO",
                            style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextTertiary
                        )
                        
                        Surface(
                            color = FDColors.InputBackground.copy(alpha = 0.5f),
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("EFECTIVO", "TARJETA", "YAPE/PLIN").forEach { metodo ->
                                    val isSel = metodoPagoSeleccionado == metodo
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isSel) FDColors.SurfaceElevated else Color.Transparent)
                                            .clickable { metodoPagoSeleccionado = metodo },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = metodo,
                                            style = FDType.Label.copy(fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium),
                                            color = if (isSel) FDColors.Primary else FDColors.TextTertiary
                                        )
                                    }
                                }
                            }
                        }

                        // CLIENTE (OPCIONAL)
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            label = { Text("DNI / RUC del Cliente (Opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = FDShapes.Small,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = FDColors.Border.copy(alpha = 0.5f)
                            )
                        )
                    }

                    // PARTE INFERIOR: BOTÓN MAESTRO
                    Column(
                        modifier = Modifier
                            .background(FDColors.SurfaceElevated)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", style = FDType.Body, color = FDColors.TextSecondary)
                            Text("$simboloMoneda 34.92", style = FDType.Numeric, color = FDColors.TextSecondary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("IGV (18%)", style = FDType.Body, color = FDColors.TextSecondary)
                            Text("$simboloMoneda 6.28", style = FDType.Numeric, color = FDColors.TextSecondary)
                        }
                        
                        FDBotonPrimario(
                            texto = "COBRAR E IMPRIMIR",
                            onClick = { /* Solo diseño */ },
                            icono = Icons.Default.Payments,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemVentaRow(item: DemoItem, simbolo: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // CANTIDAD
        Surface(
            color = FDColors.Primary.copy(alpha = 0.08f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "${item.cantidad}",
                    style = FDType.Numeric.copy(fontSize = 18.sp, fontWeight = FontWeight.Black),
                    color = FDColors.Primary
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // DESCRIPCIÓN
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.nombre,
                style = FDType.Body.copy(fontWeight = FontWeight.Bold),
                color = FDColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Lote: L2304 · Vence: 12/2026",
                style = FDType.Caption,
                color = FDColors.TextTertiary
            )
        }

        // PRECIO Y SUBTOLTAL
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$simbolo " + String.format(Locale.US, "%.2f", item.precio * item.cantidad),
                style = FDType.Numeric.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
                color = FDColors.TextPrimary
            )
            Text(
                text = "Unit: $simbolo ${item.precio}",
                style = FDType.Caption,
                color = FDColors.TextTertiary
            )
        }
    }
}

private data class DemoItem(val nombre: String, val cantidad: Int, val precio: Double)
private fun demoItem(n: String, c: Int, p: Double) = DemoItem(n, c, p)
