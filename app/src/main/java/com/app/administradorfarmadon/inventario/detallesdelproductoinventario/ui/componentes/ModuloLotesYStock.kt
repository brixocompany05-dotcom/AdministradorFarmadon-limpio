package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.base_datos.MonedaHelper
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario

/**
 * Lotes —” RE-DISEÑO 10/10 QUIET (2026)
 * Una sola métrica arriba, un hero quiet al centro, tabla comparación abajo. Sin atrium 3 cards, sin gauge circular, sin bento 2 col compitiendo.
 */
@Composable
fun ModuloLotesYStock(
    p: MoldeProductos,
    totalStock: Double,
    stockMinimo: Double,
    valorTotalInventario: Double,
    nearestExpiryLote: LoteProducto?,
    diasVencimiento: Int?,
    isPrivileged: Boolean,
    movements: List<MovimientoInventario> = emptyList(),
    onVerKardexDelLote: (loteNumero: String) -> Unit = {},
    onAdjustStock: (LoteProducto?) -> Unit
) {
    val prioridadId = if (p.fefoAutomatico) "" else p.lotePrioritarioId.trim()
    val lotesVendibles = remember(p.lotes) { p.lotes.values.filter { it.cantidad > 0 }.sortedBy { ProductDetailMapper.diasHastaVencer(it.vencimiento) ?: Int.MAX_VALUE } }
    val loteElegido = if (prioridadId.isNotBlank()) lotesVendibles.firstOrNull { it.loteId.equals(prioridadId, true) || it.numero.equals(prioridadId, true) } else null
    val loteEnUso = loteElegido ?: lotesVendibles.firstOrNull()
    val fuente = when {
        loteEnUso == null -> "Sin dispensación"
        loteElegido != null -> "Lote principal de consumo"
        p.fefoAutomatico -> "FEFO automático"
        else -> "Sin lote principal · FEFO de respaldo"
    }
    val sorted = remember(p.lotes) { p.lotes.values.sortedBy { ProductDetailMapper.diasHastaVencer(it.vencimiento) ?: Int.MAX_VALUE } }
    val ordenados = remember(p.lotes, prioridadId) {
        ProductDetailMapper.ordenarLotesParaConsumo(p.lotes.values, prioridadId)
    }
    val maxStock = remember(sorted) { sorted.maxOfOrNull { it.cantidad } ?: 1.0 }

    if (p.lotes.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Outlined.Inventory2, null, tint = FDColors.TextTertiary.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                Text("No hay lotes registrados", style = FDType.Heading3, color = FDColors.TextPrimary)
                Text("El inventario está vacío para este producto", style = FDType.Body, color = FDColors.TextSecondary)
                Button(onClick = { onAdjustStock(null) }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Registrar primera entrada", style = FDType.Label.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── 1. CONTEXTUAL TOOLBAR (SaaS Pattern) ──
            Column {
                Text(
                    text = "GESTIÓN DE LOTES Y STOCK",
                    style = FDType.Label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        fontFamily = InterPremium
                    ),
                    color = FDColors.TextTertiary
                )
                Text(
                    text = "${sorted.count { it.cantidad > 0 }} lotes con stock disponible",
                    style = FDType.Caption.copy(fontSize = 12.sp, color = FDColors.TextSecondary)
                )
            }

            // ── 2. PANELES DE TRABAJO (Split View) ──
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ── PANE IZQUIERDO: TRAZABILIDAD FEFO (60%) ──
                Column(
                    modifier = Modifier.weight(0.6f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = FDColors.Surface,
                        border = BorderStroke(1.dp, FDColors.BorderStrong.copy(alpha = if (FDColors.isDark) 1f else 0.4f)),
                        tonalElevation = if (FDColors.isDark) 0.dp else 2.dp
                    ) {
                        Column {
                            // Header Tabla con fondo diferenciado y borde inferior marcado
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FDColors.SurfaceElevated)
                                    .border(BorderStroke(0.dp, Color.Transparent))
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("LOTE", modifier = Modifier.weight(1.2f), style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                                Text("CANTIDAD", modifier = Modifier.weight(0.8f), style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                                Text("VENCIMIENTO", modifier = Modifier.weight(1f), style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                                Text("ESTADO", modifier = Modifier.weight(0.9f), style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                                Spacer(Modifier.width(80.dp))
                            }
                            HorizontalDivider(color = FDColors.BorderStrong.copy(alpha = 0.6f), thickness = 1.dp)

                            // Lista con scroll interno y reorden animado
                            val conStock = ordenados.filter { it.cantidad > 0 }
                            val sinStock = ordenados.filter { it.cantidad <= 0 }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                items(conStock, key = { "lote|${it.loteId}|${it.numero}|${it.vencimiento}" }) { lote ->
                                    val isAct = lote == loteEnUso
                                    val dias = ProductDetailMapper.diasHastaVencer(lote.vencimiento)
                                    val col = when { dias != null && dias < 0 -> FDColors.Error; dias != null && dias <= 30 -> FDColors.Warning; else -> FDColors.Success }
                                    val esPri = ProductDetailMapper.esLotePrioritario(lote, p.lotePrioritarioId)

                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .animateItem()
                                            .background(
                                                if (isAct) FDColors.Primary.copy(alpha = if (FDColors.isDark) 0.04f else 0.08f) 
                                                else Color.Transparent
                                            )
 .clickable { onAdjustStock(lote) }.padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(Modifier.size(8.dp).clip(CircleShape).background(col))
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(lote.numero.ifBlank { "S/N" }.uppercase(), style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = FDColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                if (esPri) Text("Lote principal de consumo", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = FDColors.Primary)
                                            }
                                        }
                                        Text("${Math.round(lote.cantidad).toInt()}", modifier = Modifier.weight(0.8f), style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterPremium), color = FDColors.TextPrimary)
                                        Text(lote.vencimiento.ifBlank { "N/A" }, modifier = Modifier.weight(1f), style = FDType.Body.copy(fontSize = 13.sp, fontFamily = InterPremium), color = FDColors.TextSecondary)
                                        Text(
                                            when { dias == null -> "N/A"; dias < 0 -> "Vencido"; dias <= 30 -> "En ${dias}d"; else -> "Vigente" },
                                            modifier = Modifier.weight(0.9f),
                                            style = FDType.Caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterPremium), color = col
                                        )
                                        Box(
 modifier = Modifier.width(80.dp).clip(RoundedCornerShape(8.dp)).background(if (isAct) FDColors.Primary.copy(alpha = 0.12f) else FDColors.SurfaceElevated).border(0.5.dp, if (isAct) FDColors.Primary.copy(alpha = 0.3f) else FDColors.BorderStrong, RoundedCornerShape(8.dp)).clickable { onAdjustStock(lote) }.padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Gestionar",
                                                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                                                color = if (isAct) FDColors.Primary else FDColors.TextSecondary
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), thickness = 0.5.dp)
                                }

                                if (sinStock.isNotEmpty()) {
                                    item(key = "agotados_cabecera") {
                                        Row(Modifier.fillMaxWidth().background(FDColors.Background.copy(alpha = 0.6f)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("HISTORIAL AGOTADOS (${sinStock.size})", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                                        }
                                    }
                                    items(sinStock, key = { "agotado|${it.loteId}|${it.numero}|${it.vencimiento}" }) { lote ->
                                        Row(
 modifier = Modifier.fillMaxWidth().background(FDColors.Background.copy(alpha = 0.3f)).clickable { onAdjustStock(lote) }.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(lote.numero.ifBlank { "S/N" }.uppercase(), modifier = Modifier.weight(1.2f), style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium, fontFamily = InterPremium), color = FDColors.TextTertiary)
                                            Text("${Math.round(lote.cantidad).toInt()}", modifier = Modifier.weight(0.8f), style = FDType.Body.copy(fontSize = 12.5.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                                            Text(lote.vencimiento.ifBlank { "N/A" }, modifier = Modifier.weight(1f), style = FDType.Caption.copy(fontSize = 12.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                                            Text("Agotado", modifier = Modifier.weight(0.9f), style = FDType.Caption.copy(fontSize = 11.5.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                                            Spacer(Modifier.width(80.dp))
                                        }
                                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.2f), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = "El lote principal de consumo aparece primero; el resto se ordena por vencimiento (FEFO).",
                        style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextTertiary),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // ── PANE DERECHO: INSPECTOR Y MÉTRICAS (40%) ──
                Column(
                    modifier = Modifier.weight(0.4f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Métrica Enterprise Estructurada
                    Surface(
                        color = FDColors.Surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, FDColors.BorderStrong.copy(alpha = if (FDColors.isDark) 1f else 0.4f)),
                        tonalElevation = if (FDColors.isDark) 0.dp else 3.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("RESUMEN DE INVENTARIO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                            
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("STOCK TOTAL", style = FDType.Caption.copy(fontSize = 9.sp, color = FDColors.TextTertiary))
                                    Text("${Math.round(totalStock).toInt()} ${p.empaque.ifBlank { "Und" }}", style = FDType.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = FDColors.TextPrimary)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("VALOR COSTO", style = FDType.Caption.copy(fontSize = 9.sp, color = FDColors.TextTertiary))
                                    Text(MonedaHelper.formatearSimple(valorTotalInventario), style = FDType.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = FDColors.TextPrimary)
                                }
                            }

                            HorizontalDivider(color = FDColors.BorderStrong.copy(alpha = 0.4f), thickness = 0.5.dp)

                            Column {
                                Text("PRÓXIMO VENCIMIENTO", style = FDType.Caption.copy(fontSize = 9.sp, color = FDColors.TextTertiary))
                                val (vencText, vencColor) = when {
                                    nearestExpiryLote == null -> "Sin lotes registrados" to FDColors.TextTertiary
                                    diasVencimiento == null -> nearestExpiryLote.vencimiento to FDColors.TextSecondary
                                    diasVencimiento < 0 -> "Vencido hace ${-diasVencimiento}d" to FDColors.Error
                                    diasVencimiento <= 30 -> "Vence en ${diasVencimiento}d" to FDColors.Warning
                                    else -> "Vigente por ${diasVencimiento}d" to FDColors.Success
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (nearestExpiryLote != null) Box(Modifier.size(6.dp).clip(CircleShape).background(vencColor))
                                    Text(vencText, style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = vencColor)
                                }
                            }
                        }
                    }

                    // Hero Lote en Uso
                    if (loteEnUso != null) {
                        val dias = ProductDetailMapper.diasHastaVencer(loteEnUso.vencimiento)
                        val col = when { dias != null && dias < 0 -> FDColors.Error; dias != null && dias <= 30 -> FDColors.Warning; else -> FDColors.Success }
                        val prog = (loteEnUso.cantidad / maxStock.coerceAtLeast(1.0)).toFloat().coerceIn(0.08f, 1f)
                        
                        Surface(
                            color = FDColors.SurfaceElevated,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.2.dp, if (FDColors.isDark) FDColors.BorderStrong else FDColors.BorderStrong.copy(alpha = 0.5f)),
                            tonalElevation = if (FDColors.isDark) 0.dp else 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(col))
                                    Text(fuente.uppercase(), style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterPremium), color = col)
                                    if (loteElegido != null) {
                                        Surface(color = FDColors.Primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                            Text("PRINCIPAL", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = FDColors.Primary)
                                        }
                                    }
                                }
                                
                                Text(loteEnUso.numero.ifBlank { "S/N" }.uppercase(), style = FDType.Heading2.copy(fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = FDColors.TextPrimary)
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${Math.round(loteEnUso.cantidad).toInt()} ${p.empaque} en stock", style = FDType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterPremium), color = FDColors.TextPrimary)
                                    Text("Vencimiento: ${loteEnUso.vencimiento.ifBlank { "N/A" }}", style = FDType.Caption.copy(fontSize = 12.sp, fontFamily = InterPremium), color = FDColors.TextSecondary)
                                }

                                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(FDColors.Background).border(0.5.dp, FDColors.BorderStrong.copy(alpha = 0.4f), RoundedCornerShape(4.dp))) {
                                    Box(Modifier.fillMaxWidth(prog).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(col))
                                }

                            // Acción de Gestión del Lote - Estilo Secundario Premium (Sin competencia visual)
                            OutlinedButton(
                                onClick = { onAdjustStock(loteEnUso) },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, FDColors.BorderStrong),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = FDColors.SurfaceElevated,
                                    contentColor = FDColors.TextPrimary
                                )
                            ) {
                                Text(
                                    text = "LOTES Y MOVIMIENTOS",
                                    style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                )
                            }
                            }
                        }
                    }

                    Text(
                        text = "Este panel muestra el lote que se dispensa actualmente y las métricas financieras del inventario.",
                        style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextTertiary, lineHeight = 16.sp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

