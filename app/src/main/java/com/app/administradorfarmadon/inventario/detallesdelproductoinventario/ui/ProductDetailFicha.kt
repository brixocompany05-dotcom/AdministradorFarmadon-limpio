package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.base_datos.MonedaHelper
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.stockDisponibleUnidades
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario

/**
 * Pestaña Maestra: FICHA DEL PRODUCTO.
 * Consolida stock, costos de lotes y precios de presentaciones en una sola vista continua.
 */
@Composable
internal fun ModuloFichaInformativaGeneral(p: MoldeProductos) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── AVISO DE PRECIO: sin precio de venta no se puede vender (se desbloquea solo) ──
        if (!p.tienePrecioVenta) {
            Surface(
                color = FDColors.WarningSubtle,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Warning, null, tint = FDColors.Warning, modifier = Modifier.size(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "PRODUCTO SIN PRECIO DE VENTA",
                            style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp),
                            color = FDColors.Warning
                        )
                        Text(
                            "Fija el precio en la pestaña de Precios para poder venderlo. Se desbloquea solo al guardarlo.",
                            style = FDType.BodySmall.copy(fontSize = 11.sp),
                            color = FDColors.TextSecondary
                        )
                    }
                }
            }
        }

        // ── 1. DASHBOARD DE STOCK Y VALORIZACIÓN ──
        Surface(
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.5.dp, FDColors.Border)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Inventory, null, tint = FDColors.Primary, modifier = Modifier.size(20.dp))
                    Text(
                        "RESUMEN OPERATIVO Y STOCK",
                        style = FDType.Label.copy(fontSize = 11.sp, color = FDColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val stockTotal = p.stockDisponibleUnidades
                    FichaMetricCard("STOCK TOTAL", "${stockTotal.toInt()} ${p.empaque.ifBlank { "Unidad" }}", Modifier.weight(1f))
                    FichaMetricCard("STOCK MÍNIMO", "${p.stockMinimoBase.toInt()} ${p.empaque.ifBlank { "Unidad" }}", Modifier.weight(1f))
                    
                    val valorTotal = p.lotes.values.sumOf { (it.cantidad + it.cantidadBloqueada) * (if (it.costoCompraUnitario > 0) it.costoCompraUnitario else p.precioCompra) }
                    FichaMetricCard("VALOR TOTAL INV.", MonedaHelper.formatear(valorTotal), Modifier.weight(1.2f), highlight = true)
                }
            }
        }

        // ── 2. DESGLOSE DE COSTOS POR LOTE ──
        Surface(
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.5.dp, FDColors.Border)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Layers, null, tint = FDColors.Primary, modifier = Modifier.size(20.dp))
                    Text(
                        "DESGLOSE DE LOTES Y COSTOS DE COMPRA",
                        style = FDType.Label.copy(fontSize = 11.sp, color = FDColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                if (p.lotes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("No hay lotes registrados", style = FDType.Body.copy(color = FDColors.TextTertiary))
                    }
                } else {
                    // Cabecera de tabla de lotes
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("LOTE / VENC.", modifier = Modifier.weight(1.5f), style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                        Text("ESTADO", modifier = Modifier.weight(1f), style = FDType.Caption.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                        Text("CANT.", modifier = Modifier.weight(1f), style = FDType.Caption.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                        Text("COSTO COMPRA", modifier = Modifier.weight(1.2f), style = FDType.Caption.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)
                    
                    p.lotes.values.sortedBy { it.vencimiento }.forEach { lote ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(lote.numero.uppercase(), style = FDType.Body.copy(fontWeight = FontWeight.Bold))
                                Text("Vence: ${lote.vencimiento}", style = FDType.Caption.copy(color = FDColors.TextSecondary))
                            }
                            
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                val dias = ProductDetailMapper.diasHastaVencer(lote.vencimiento) ?: 999
                                when {
                                    lote.cantidadBloqueada > 0 -> EnterpriseStatusPill("BLOQUEADO", FDColors.Error)
                                    dias <= 30 -> EnterpriseStatusPill("POR VENCER", FDColors.Warning)
                                    else -> EnterpriseStatusPill("DISPONIBLE", FDColors.Success)
                                }
                            }
                            
                            Text(
                                text = "${(lote.cantidad + lote.cantidadBloqueada).toInt()} ${p.empaque.ifBlank { "Unidad" }}", 
                                modifier = Modifier.weight(1f), 
                                style = FDType.Body, 
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = MonedaHelper.formatear(if (lote.costoCompraUnitario > 0) lote.costoCompraUnitario else p.precioCompra), 
                                modifier = Modifier.weight(1.2f), 
                                style = FDType.Body.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary), 
                                textAlign = TextAlign.End
                            )
                        }
                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), thickness = 0.5.dp)
                    }
                }
            }
        }

        // ── 3. ESQUEMA DE PRECIOS Y PRESENTACIONES ──
        Surface(
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.5.dp, FDColors.Border)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.LocalOffer, null, tint = FDColors.Primary, modifier = Modifier.size(20.dp))
                    Text(
                        "PRESENTACIONES Y PRECIOS DE VENTA",
                        style = FDType.Label.copy(fontSize = 11.sp, color = FDColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                p.presentaciones.forEach { pres ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp)
                            .background(FDColors.Background.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, FDColors.Border, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(pres.nombre.uppercase(), style = FDType.Body.copy(fontWeight = FontWeight.Bold))
                            Text("Contiene: ${pres.cantidad} ${pres.unidadMedida.ifBlank { p.empaque.ifBlank { "Unidad" } }}", style = FDType.Caption.copy(color = FDColors.TextSecondary))
                        }
                        
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(MonedaHelper.formatear(pres.precioventa), style = FDType.Heading3.copy(color = FDColors.Primary, fontWeight = FontWeight.Black))
                            Text("Precio de Venta", style = FDType.Caption.copy(fontSize = 9.sp, color = FDColors.TextTertiary))
                        }
                    }
                }
            }
        }

        // ── 4. DATOS TÉCNICOS ADICIONALES ──
        ModuloFichaTecnicaSanitaria(p)
    }
}

@Composable
private fun FichaMetricCard(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Surface(
        modifier = modifier,
        color = if (highlight) FDColors.Primary.copy(alpha = 0.05f) else FDColors.Background.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, if (highlight) FDColors.Primary.copy(alpha = 0.2f) else FDColors.Border)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = FDType.Label.copy(fontSize = 9.sp, color = if (highlight) FDColors.Primary else FDColors.TextTertiary))
            Text(value, style = if (highlight) FDType.Numeric.copy(color = FDColors.Primary) else FDType.Numeric)
        }
    }
}

// ════════════════════════════════════════════════════════════════
// MÓDULO: ESPECIFICACIONES TÉCNICAS (COMPACTO)
// ════════════════════════════════════════════════════════════════
@Composable
internal fun ModuloFichaTecnicaSanitaria(p: MoldeProductos) {
    Surface(
        color = FDColors.SurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "ESPECIFICACIONES TÉCNICAS Y LOGÍSTICA",
                style = FDType.Label.copy(fontSize = 11.sp, color = FDColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FichaFieldEnterprise("PRINCIPIO ACTIVO", p.principioActivo.ifBlank { "Sin principio activo" }, Modifier.weight(1f))
                FichaFieldEnterprise("LABORATORIO", p.proveedorBaseNombre.ifBlank { "Sin laboratorio" }, Modifier.weight(1f))
                FichaFieldEnterprise("UBICACIÓN", p.ubicacion.ifBlank { "Sin asignar" }, Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FichaFieldEnterprise("CÓDIGO DE BARRAS", p.codigo.ifBlank { "SIN CÓDIGO" }, Modifier.weight(1f))
                FichaFieldEnterprise("RECETA MÉDICA", if (p.requiereReceta) "SÍ" else "NO", Modifier.weight(1f))
                FichaFieldEnterprise("CONSERVACIÓN", if (p.temperaturaAlmacenamiento.contains("refrig", ignoreCase = true)) "REFRIGERADO" else "T. AMBIENTE", Modifier.weight(1f))
            }
        }
    }
}
