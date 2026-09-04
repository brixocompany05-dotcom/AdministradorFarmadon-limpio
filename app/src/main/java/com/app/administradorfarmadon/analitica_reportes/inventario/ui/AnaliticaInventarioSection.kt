package com.app.administradorfarmadon.analitica_reportes.inventario.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.analitica_reportes.comunes.ui.MetricKpiSaaS
import com.app.administradorfarmadon.analitica_reportes.modelo.InventarioAnalytics
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// SUBMÓDULO: INVENTARIO (VERDAD CONTABLE DE CAPITAL Y ROTACIÓN)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AnaliticaInventarioSection(
    inventario: InventarioAnalytics,
    periodo: String = "Período actual",
    sedeNombre: String = "Todas las sedes",
    s: MedidaAdaptativa
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // Alerta de Auditoría de Verdad si existen productos con stock físico pero costo S/ 0.00
        if (inventario.productosSinCostoDefinido > 0) {
            Surface(
                color = FDColors.Warning.copy(alpha = 0.08f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = FDColors.Warning,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Auditoría de Verdad: ${inventario.productosSinCostoDefinido} producto(s) tienen existencia física pero costo de compra S/ 0.00. No sumarán al valorizado hasta registrar su factura de compra o costo base.",
                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = FDColors.TextPrimary
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricKpiSaaS(
                titulo = "VALORIZADO TOTAL",
                valor = "S/ %.2f".format(Locale.US, inventario.valorInventarioTotal),
                tag = "Costo Activo Total",
                modifier = Modifier.weight(1.1f)
            )
            MetricKpiSaaS(
                titulo = "EN ROTACIÓN",
                valor = "S/ %.2f".format(Locale.US, inventario.capitalEnRotacion),
                tag = "Mercadería con Ventas",
                colorValor = FDColors.Success,
                modifier = Modifier.weight(1f)
            )
            MetricKpiSaaS(
                titulo = "INMOVILIZADO",
                valor = "S/ %.2f".format(Locale.US, inventario.capitalInmovilizado),
                tag = "Dinero Sin Rotación",
                colorValor = if (inventario.capitalInmovilizado > 0) FDColors.Warning else FDColors.Success,
                modifier = Modifier.weight(1f)
            )
            MetricKpiSaaS(
                titulo = "TASA ROTACIÓN",
                valor = "%.1f%%".format(Locale.US, inventario.tasaRotacionPorcentaje),
                tag = "Velocidad de Capital",
                colorValor = if (inventario.tasaRotacionPorcentaje >= 50.0) FDColors.Success else FDColors.Warning,
                modifier = Modifier.weight(0.9f)
            )
        }

        // Panel dividido Enterprise SaaS (50% Quiebres / 50% Capital Inmovilizado)
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Panel Izquierdo: Quiebres de Stock y Desglose Financiero
            Surface(
                color = FDColors.Surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (inventario.cantidadQuiebresStock > 0) FDColors.Error.copy(alpha = 0.4f) else FDColors.Border.copy(alpha = 0.5f)),
                modifier = Modifier.weight(0.5f).fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (inventario.cantidadQuiebresStock > 0) FDColors.Error else FDColors.Success,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Riesgo de Quiebre de Stock",
                                style = FDType.Heading2.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextPrimary
                            )
                        }
                        Text(
                            "[ ${inventario.detalleQuiebres.size} ]",
                            style = FDType.Caption.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (inventario.cantidadQuiebresStock > 0) FDColors.Error else FDColors.Success
                            )
                        )
                    }

                    // Resumen contable de reposición: Reponer lo que falta vs Mínimo completo
                    Surface(
                        color = FDColors.InputBackground.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Para reponer lo que falta:",
                                    style = FDType.Caption.copy(fontSize = 9.sp),
                                    color = FDColors.TextSecondary
                                )
                                Text(
                                    "S/ %.2f".format(Locale.US, inventario.capitalRiesgoQuiebre),
                                    style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                    color = if (inventario.capitalRiesgoQuiebre > 0.0) FDColors.Error else FDColors.TextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Si pides el mínimo completo:",
                                    style = FDType.Caption.copy(fontSize = 9.sp),
                                    color = FDColors.TextSecondary
                                )
                                Text(
                                    "S/ %.2f".format(Locale.US, inventario.capitalPedidoMinimoCompleto),
                                    style = FDType.BodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                    color = FDColors.TextPrimary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))

                    if (inventario.detalleQuiebres.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Inventario saludable: sin quiebres de stock", style = FDType.Caption, color = FDColors.Success)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            inventario.detalleQuiebres.forEach { item ->
                                Surface(
                                    color = FDColors.Error.copy(alpha = 0.03f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, FDColors.Error.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        // Fila 1: Nombre y Empaque
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                item.nombre,
                                                style = FDType.BodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                                color = FDColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (item.empaque.isNotBlank()) {
                                                Surface(
                                                    color = FDColors.Surface,
                                                    shape = RoundedCornerShape(3.dp),
                                                    border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.3f))
                                                ) {
                                                    Text(
                                                        item.empaque,
                                                        style = FDType.Caption.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Medium),
                                                        color = FDColors.TextSecondary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Fila 2: Stock, Mínimo y Costo Unitario de Compra con Unidad Física Real
                                        val unidadPluralStr = item.unidadMedidaPlural.lowercase(Locale.US)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Stock: %.0f · Mín: %.0f".format(Locale.US, item.stockActual, item.stockMinimo),
                                                    style = FDType.Caption.copy(fontSize = 9.5.sp),
                                                    color = FDColors.TextSecondary
                                                )
                                                Text(
                                                    "➜ Faltan: %.0f %s".format(Locale.US, item.unidadesFaltantes, unidadPluralStr),
                                                    style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                    color = FDColors.Error
                                                )
                                            }
                                            Text(
                                                "Costo: S/ %.2f c/u".format(Locale.US, item.costoUnitarioCompra),
                                                style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                                color = FDColors.TextSecondary
                                            )
                                        }

                                        // Fila 3: Origen y trazabilidad del costo (Factura, Lote o Catálogo + Proveedor)
                                        if (item.origenCosto.isNotBlank() || item.proveedorNombre.isNotBlank()) {
                                            val textoOrigen = buildString {
                                                if (item.origenCosto.isNotBlank()) append(item.origenCosto)
                                                if (item.proveedorNombre.isNotBlank()) {
                                                    if (isNotEmpty()) append(" · ")
                                                    append(item.proveedorNombre)
                                                }
                                            }
                                            Text(
                                                text = "Ref. compra: $textoOrigen",
                                                style = FDType.Caption.copy(fontSize = 8.5.sp),
                                                color = FDColors.TextTertiary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Fila 4: Comparativa clara Nivelar vs Lote Completo
                                        Surface(
                                            color = FDColors.Surface,
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.25f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "Comprar faltante (%.0f %s):".format(Locale.US, item.unidadesFaltantes, unidadPluralStr),
                                                        style = FDType.Caption.copy(fontSize = 9.sp),
                                                        color = FDColors.TextSecondary
                                                    )
                                                    Text(
                                                        "S/ %.2f".format(Locale.US, item.costoReposicionFaltante),
                                                        style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                        color = FDColors.Error
                                                    )
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "Mínimo completo (%.0f %s):".format(Locale.US, item.stockMinimo, unidadPluralStr),
                                                        style = FDType.Caption.copy(fontSize = 8.5.sp),
                                                        color = FDColors.TextTertiary
                                                    )
                                                    Text(
                                                        "S/ %.2f".format(Locale.US, item.costoPedidoMinimoCompleto),
                                                        style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                                        color = FDColors.TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Panel Derecho: Productos Sin Rotación (Capital Inmovilizado)
            Surface(
                color = FDColors.Surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                modifier = Modifier.weight(0.5f).fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Capital Inmovilizado (Sin Rotación)",
                            style = FDType.Heading2.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            "[ ${inventario.detalleInmovilizados.size} ]",
                            style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.Primary
                        )
                    }
                    Text(
                        "Dinero dormido en anaquel: S/ %.2f".format(Locale.US, inventario.capitalInmovilizado),
                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextSecondary
                    )
                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))
                    if (inventario.detalleInmovilizados.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Excelente rotación: todos los productos tuvieron ventas", style = FDType.Caption, color = FDColors.Success)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            inventario.detalleInmovilizados.forEach { item ->
                                Surface(
                                    color = FDColors.InputBackground.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                item.nombre,
                                                style = FDType.BodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                                color = FDColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (item.empaque.isNotBlank()) {
                                                Surface(
                                                    color = FDColors.Surface,
                                                    shape = RoundedCornerShape(3.dp),
                                                    border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.3f))
                                                ) {
                                                    Text(
                                                        item.empaque,
                                                        style = FDType.Caption.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Medium),
                                                        color = FDColors.TextSecondary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        val unidadPluralStr = item.unidadMedidaPlural.lowercase(Locale.US)
                                        val detalleLotesStr = if (item.cantidadLotes > 1) "${item.cantidadLotes} lotes" else "1 lote"
                                        val etiquetaCosto = if (item.esCostoPromedio) "Costo prom." else "Costo"

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "%.0f %s en stock (%s) · %s: S/ %.2f c/u".format(
                                                    Locale.US,
                                                    item.stockActual,
                                                    unidadPluralStr,
                                                    detalleLotesStr,
                                                    etiquetaCosto,
                                                    item.costoUnitarioCompra
                                                ),
                                                style = FDType.Caption.copy(fontSize = 9.sp),
                                                color = FDColors.TextSecondary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Estancado:",
                                                    style = FDType.Caption.copy(fontSize = 9.sp),
                                                    color = FDColors.TextTertiary
                                                )
                                                Text(
                                                    "S/ %.2f".format(Locale.US, item.valorEstancado),
                                                    style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                                    color = FDColors.Warning
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
