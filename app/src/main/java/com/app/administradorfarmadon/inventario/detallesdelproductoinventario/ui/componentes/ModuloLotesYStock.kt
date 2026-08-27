package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
 * Lotes — RE-DISEÑO 10/10 QUIET (2026)
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
    onCambiarBloqueoLote: (lote: LoteProducto, ponerEnCuarentena: Boolean, cantidad: Double, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onRegistrarDevolucion: (lote: LoteProducto, cantidad: Double, guiaRetiro: String, notaCredito: String, motivo: String, modalidad: String, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onRegistrarCanje: (lote: LoteProducto, cantidad: Double, nuevoLote: String, nuevoVencimiento: String, guiaCanje: String, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onAnularIngreso: (lote: LoteProducto, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onRegistrarMerma: (lote: LoteProducto, cantidad: Double, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onDefinirPrioridadLote: (loteId: String?) -> Unit = {},
    movements: List<MovimientoInventario> = emptyList(),
    onVerKardexDelLote: (loteNumero: String) -> Unit = {},
    onAdjustStock: (LoteProducto?) -> Unit
) {
    var loteFicha by remember { mutableStateOf<LoteProducto?>(null) }
    var loteMerma by remember { mutableStateOf<LoteProducto?>(null) }
    var loteAnular by remember { mutableStateOf<LoteProducto?>(null) }

    loteFicha?.let { lote ->
        val esPri = p.lotePrioritarioId.isNotBlank() && (p.lotePrioritarioId.equals(lote.loteId, true) || p.lotePrioritarioId.equals(lote.numero, true))
        LoteDetailDrawer(
            product = p, lote = lote, esEsteElLotePrioritario = esPri,
            onDefinirPrioridad = { marcar -> onDefinirPrioridadLote(if (marcar) lote.loteId.ifBlank { lote.numero } else null) },
            isPrivileged = isPrivileged, onDismiss = { loteFicha = null },
            onIngresarMasStock = { l -> loteFicha = null; onAdjustStock(l) },
            onRegistrarDevolucion = onRegistrarDevolucion, onRegistrarCanje = onRegistrarCanje,
            onCambiarBloqueo = onCambiarBloqueoLote, onAnularLote = onAnularIngreso, onRegistrarMerma = onRegistrarMerma
        )
    }
    loteMerma?.let { lote ->
        DialogoRegistroMermaDrawer(product = p, lote = lote, onDismiss = { loteMerma = null }, onConfirm = { c, m, cb -> onRegistrarMerma(lote, c, m) { r -> cb(r); if (r.isSuccess) loteMerma = null } })
    }
    loteAnular?.let { lote ->
        DialogoAnulacionLegal(product = p, lote = lote, onDismiss = { loteAnular = null }, onConfirm = { motivo, cb -> onAnularIngreso(lote, motivo) { r -> cb(r); if (r.isSuccess) loteAnular = null } })
    }

    val prioridadId = p.lotePrioritarioId.trim()
    val lotesVendibles = remember(p.lotes) { p.lotes.values.filter { it.cantidad > 0 }.sortedBy { ProductDetailMapper.diasHastaVencer(it.vencimiento) ?: Int.MAX_VALUE } }
    val loteElegido = if (prioridadId.isNotBlank()) lotesVendibles.firstOrNull { it.loteId.equals(prioridadId, true) || it.numero.equals(prioridadId, true) } else null
    val loteEnUso = loteElegido ?: lotesVendibles.firstOrNull()
    val fuente = when { loteEnUso == null -> "Sin dispensación"; loteElegido != null -> "Prioridad del dueño"; else -> "FEFO automático" }
    val sorted = remember(p.lotes) { p.lotes.values.sortedBy { ProductDetailMapper.diasHastaVencer(it.vencimiento) ?: Int.MAX_VALUE } }
    val maxStock = remember(sorted) { sorted.maxOfOrNull { it.cantidad } ?: 1.0 }

    Column(
        modifier = Modifier.fillMaxSize().background(FDColors.Background).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Métrica quiet — una línea, no 3 cards
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(FDColors.Surface).border(0.5.dp, FDColors.Border, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${Math.round(totalStock).toInt()} ${p.empaque.ifBlank { "Unid" }}", style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = FDColors.TextPrimary)
            Text("·", color = FDColors.TextTertiary)
            Text(MonedaHelper.formatearSimple(valorTotalInventario), style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterPremium), color = FDColors.TextSecondary)
            Text("·", color = FDColors.TextTertiary)
            val vencTxt = when {
                nearestExpiryLote == null -> "Sin venc."
                diasVencimiento == null -> nearestExpiryLote.vencimiento
                diasVencimiento < 0 -> "Vencido ${-diasVencimiento}d"
                diasVencimiento <= 30 -> "Vence ${diasVencimiento}d"
                else -> "Vigente ${diasVencimiento}d"
            }
            Text(vencTxt, style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium, fontFamily = InterPremium), color = when { diasVencimiento != null && diasVencimiento < 0 -> FDColors.Error; diasVencimiento != null && diasVencimiento <= 30 -> FDColors.Warning; else -> FDColors.TextSecondary })
        }

        // Hero lote en uso — quiet centrado
        if (loteEnUso != null) {
            val dias = ProductDetailMapper.diasHastaVencer(loteEnUso.vencimiento)
            val col = when { dias != null && dias < 0 -> FDColors.Error; dias != null && dias <= 30 -> FDColors.Warning; else -> FDColors.Success }
            val prog = (loteEnUso.cantidad / maxStock.coerceAtLeast(1.0)).toFloat().coerceIn(0.08f, 1f)
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FDColors.SurfaceElevated).border(0.6.dp, FDColors.Border.copy(alpha = 0.8f), RoundedCornerShape(16.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(col))
                    Text(fuente.uppercase(), style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp, fontFamily = InterPremium), color = col)
                    if (loteElegido != null) {
                        Text("· PRIORIDAD", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = FDColors.Primary)
                    }
                }
                Text(loteEnUso.numero.ifBlank { "S/N" }.uppercase(), style = FDType.Heading2.copy(fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterPremium), color = FDColors.TextPrimary)
                Text("${Math.round(loteEnUso.cantidad).toInt()} ${p.empaque} · Vence ${loteEnUso.vencimiento.ifBlank { "—" }} · ${p.ubicacion.ifBlank { "General" }}", style = FDType.Body.copy(fontSize = 12.5.sp, fontFamily = InterPremium), color = FDColors.TextSecondary)
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(FDColors.Background).border(0.5.dp, FDColors.Border, RoundedCornerShape(3.dp))) {
                    Box(Modifier.fillMaxWidth(prog).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(col))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onVerKardexDelLote(loteEnUso.numero) }, modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(0.6.dp, FDColors.Border)) {
                        Text("Ver Kardex", style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterPremium), color = FDColors.TextSecondary)
                    }
                    Button(onClick = { loteFicha = loteEnUso }, modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText)) {
                        Text("Gestionar", style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium))
                    }
                }
                OutlinedButton(onClick = { onAdjustStock(null) }, modifier = Modifier.fillMaxWidth().height(38.dp), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(0.6.dp, FDColors.Border)) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp), tint = FDColors.TextSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text("Ingresar nuevo lote", style = FDType.Label.copy(fontSize = 12.sp, fontFamily = InterPremium), color = FDColors.TextSecondary)
                }
            }
            Text("El lote de arriba se dispensa primero · toca cualquier fila y márcalo como prioritario si necesitas otro", style = FDType.Caption.copy(fontSize = 11.sp, fontFamily = InterPremium), color = FDColors.TextTertiary, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FDColors.Surface).border(0.6.dp, FDColors.Border, RoundedCornerShape(16.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.Inventory2, null, tint = FDColors.TextTertiary.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                Text("Sin lote en dispensación", style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = FDColors.TextPrimary)
                Text("Registra tu primer lote para activar FEFO", style = FDType.Caption.copy(fontSize = 11.5.sp, fontFamily = InterPremium), color = FDColors.TextSecondary)
                Button(onClick = { onAdjustStock(null) }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary)) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nuevo lote", style = FDType.Label.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium))
                }
            }
        }

        // Lista
        if (p.lotes.isEmpty()) {
            // vacío ya cubierto arriba
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TRAZABILIDAD FEFO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                Text("${sorted.count { it.cantidad > 0 }} con stock · ${sorted.count { it.cantidad <= 0 }} agotados", style = FDType.Caption.copy(fontSize = 11.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
            }

            // Tabla quiet
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FDColors.Surface).border(0.6.dp, FDColors.Border, RoundedCornerShape(12.dp))
            ) {
                // header tabla
                Row(
                    modifier = Modifier.fillMaxWidth().background(FDColors.SurfaceElevated).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LOTE", modifier = Modifier.weight(1.2f), style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                    Text("CANT.", modifier = Modifier.weight(0.7f), style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                    Text("VENCE", modifier = Modifier.weight(0.9f), style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                    Text("ESTADO", modifier = Modifier.weight(0.8f), style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                    Spacer(Modifier.width(72.dp))
                }
                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.6f), thickness = 0.5.dp)

                val conStock = sorted.filter { it.cantidad > 0 }
                val sinStock = sorted.filter { it.cantidad <= 0 }

                conStock.forEach { lote ->
                    val isAct = lote == loteEnUso
                    val dias = ProductDetailMapper.diasHastaVencer(lote.vencimiento)
                    val col = when { dias != null && dias < 0 -> FDColors.Error; dias != null && dias <= 30 -> FDColors.Warning; else -> FDColors.Success }
                    val esPri = p.lotePrioritarioId.equals(lote.loteId, true) || p.lotePrioritarioId.equals(lote.numero, true)
                    Row(
                        modifier = Modifier.fillMaxWidth().background(if (isAct) FDColors.Primary.copy(alpha = 0.04f) else Color.Transparent)
                            .clickable { loteFicha = lote }.padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(col))
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(lote.numero.ifBlank { "S/N" }.uppercase(), style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = FDColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (esPri) Text("★ Prioridad", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = FDColors.Primary)
                            }
                        }
                        Text("${Math.round(lote.cantidad).toInt()}", modifier = Modifier.weight(0.7f), style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterPremium), color = FDColors.TextPrimary)
                        Text(lote.vencimiento.ifBlank { "—" }, modifier = Modifier.weight(0.9f), style = FDType.Body.copy(fontSize = 12.sp, fontFamily = InterPremium), color = FDColors.TextSecondary, maxLines = 1)
                        Text(
                            when { dias == null -> "—"; dias < 0 -> "Vencido"; dias <= 30 -> "${dias}d"; else -> "Vigente" },
                            modifier = Modifier.weight(0.8f),
                            style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterPremium), color = col
                        )
                        Text(
                            text = "Gestionar",
                            style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterPremium),
                            color = if (isAct) FDColors.Primary else FDColors.TextSecondary,
                            modifier = Modifier.width(72.dp).clip(RoundedCornerShape(8.dp)).background(if (isAct) FDColors.Primary.copy(alpha = 0.10f) else FDColors.SurfaceElevated).border(0.5.dp, if (isAct) FDColors.Primary.copy(alpha = 0.2f) else FDColors.Border, RoundedCornerShape(8.dp)).clickable { loteFicha = lote }.padding(horizontal = 8.dp, vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.25f), thickness = 0.5.dp)
                }

                if (sinStock.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().background(FDColors.Background.copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("AGOTADOS · ${sinStock.size}", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                    }
                    sinStock.take(6).forEach { lote ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(FDColors.Background.copy(alpha = 0.25f)).clickable { loteFicha = lote }.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lote.numero.ifBlank { "S/N" }.uppercase(), modifier = Modifier.weight(1.2f), style = FDType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterPremium), color = FDColors.TextTertiary)
                            Text("${Math.round(lote.cantidad).toInt()}", modifier = Modifier.weight(0.7f), style = FDType.Body.copy(fontSize = 12.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                            Text(lote.vencimiento.ifBlank { "—" }, modifier = Modifier.weight(0.9f), style = FDType.Caption.copy(fontSize = 11.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                            Text("Agotado", modifier = Modifier.weight(0.8f), style = FDType.Caption.copy(fontSize = 11.sp, fontFamily = InterPremium), color = FDColors.TextTertiary)
                            Spacer(Modifier.width(72.dp))
                        }
                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.18f), thickness = 0.5.dp)
                    }
                    if (sinStock.size > 6) {
                        Text("+${sinStock.size - 6} más agotados", style = FDType.Caption.copy(fontSize = 11.sp, fontFamily = InterPremium), color = FDColors.TextTertiary, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
    }
}
