package com.app.administradorfarmadon.analitica_reportes.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium

// ═════════════════════════════════════════════════════════════════════════════
// 1. SECCIÓN RESUMEN (ESTRUCTURADA EN TABLAS Y KPIS AMPLIOS)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AnaliticaResumenSection(
    periodo: String,
    s: MedidaAdaptativa
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Grid 4 Tarjetas KPI Amplias
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricKpiSaaS("Ventas Totales", "S/ 84,250.00", "+12.4%", true, Icons.Default.TrendingUp, s, Modifier.weight(1f))
            MetricKpiSaaS("Utilidad Bruta", "S/ 31,172.50", "+14.2%", true, Icons.Default.AttachMoney, s, Modifier.weight(1f))
            MetricKpiSaaS("Margen Bruto", "37.0%", "+0.6%", true, Icons.Default.PieChart, s, Modifier.weight(1f))
            MetricKpiSaaS("Clientes Atendidos", "1,420 personas", "+8.5%", true, Icons.Default.Groups, s, Modifier.weight(1f))
        }

        // TABLA ESTRUCTURADA DE RENTABILIDAD POR CATEGORÍA
        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RESUMEN FINANCIERO Y MARGEN POR CATEGORÍA",
                        style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = FDColors.TextTertiary
                    )
                    Text(
                        text = "Período: $periodo",
                        style = FDType.Caption.copy(fontSize = 11.5.sp),
                        color = FDColors.TextSecondary
                    )
                }

                // Table Header
                Surface(
                    color = FDColors.SurfaceElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CATEGORÍA DE PRODUCTOS", style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, modifier = Modifier.weight(2.5f))
                        Text("VENTAS (S/)", style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.TextSecondary, modifier = Modifier.weight(1.5f))
                        Text("COSTO (COGS)", style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.TextSecondary, modifier = Modifier.weight(1.5f))
                        Text("UTILIDAD BRUTA", style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.TextSecondary, modifier = Modifier.weight(1.5f))
                        Text("MARGEN %", style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.TextSecondary, modifier = Modifier.weight(1.2f))
                    }
                }

                // Table Rows
                val categoriasResumen = listOf(
                    ResumenCategoriaFila("Medicamentos OTC", "S/ 38,450.00", "S/ 23,800.00", "S/ 14,650.00", "38.1%"),
                    ResumenCategoriaFila("Medicamentos Éticos / Receta", "S/ 26,800.00", "S/ 17,500.00", "S/ 9,300.00", "34.7%"),
                    ResumenCategoriaFila("Cuidado Personal & Higiene", "S/ 9,420.00", "S/ 6,100.00", "S/ 3,320.00", "35.2%"),
                    ResumenCategoriaFila("Nutrición & Bebidas", "S/ 5,800.00", "S/ 3,900.00", "S/ 1,900.00", "32.8%"),
                    ResumenCategoriaFila("Material Médico & Suplementos", "S/ 3,780.00", "S/ 1,777.50", "S/ 2,002.50", "53.0%")
                )

                categoriasResumen.forEach { cat ->
                    Surface(
                        color = FDColors.Surface,
                        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.categoria, style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = FDColors.TextPrimary, modifier = Modifier.weight(2.5f))
                            Text(cat.ventas, style = FDType.Body.copy(fontSize = 13.sp, textAlign = TextAlign.End), color = FDColors.TextPrimary, modifier = Modifier.weight(1.5f))
                            Text(cat.costo, style = FDType.BodySmall.copy(fontSize = 12.5.sp, textAlign = TextAlign.End), color = FDColors.TextSecondary, modifier = Modifier.weight(1.5f))
                            Text(cat.utilidad, style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.Success, modifier = Modifier.weight(1.5f))
                            Text(cat.margen, style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.Primary, modifier = Modifier.weight(1.2f))
                        }
                    }
                }

                // Row Total
                Surface(
                    color = FDColors.PrimarySubtle,
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL GENERAL PERÍODO", style = FDType.Label.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Black), color = FDColors.Primary, modifier = Modifier.weight(2.5f))
                        Text("S/ 84,250.00", style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.End), color = FDColors.Primary, modifier = Modifier.weight(1.5f))
                        Text("S/ 53,077.50", style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.TextSecondary, modifier = Modifier.weight(1.5f))
                        Text("S/ 31,172.50", style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.End), color = FDColors.Success, modifier = Modifier.weight(1.5f))
                        Text("37.0%", style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.End), color = FDColors.Primary, modifier = Modifier.weight(1.2f))
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. SECCIÓN VENTAS
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AnaliticaVentasSection(periodo: String, s: MedidaAdaptativa) {
    var descomposicionSeleccionada by remember { mutableStateOf("CATEGORIA") }

    val dimensiones = listOf(
        "CATEGORIA" to "Por Categoría",
        "PRODUCTO" to "Por Producto",
        "HORA" to "Por Hora / Turno",
        "VENDEDOR" to "Por Vendedor",
        "METODO_PAGO" to "Método de Pago",
        "SUCURSAL" to "Por Sucursal"
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dimensiones.forEach { (key, label) ->
                val esSel = descomposicionSeleccionada == key
                FilterChip(
                    selected = esSel,
                    onClick = { descomposicionSeleccionada = key },
                    label = { Text(label, style = FDType.Caption.copy(fontSize = 12.sp, fontWeight = if (esSel) FontWeight.Bold else FontWeight.Medium)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FDColors.PrimarySubtle,
                        selectedLabelColor = FDColors.Primary,
                        containerColor = FDColors.SurfaceElevated,
                        labelColor = FDColors.TextSecondary
                    )
                )
            }
        }

        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DESGLOSE DE VENTAS ($descomposicionSeleccionada)", style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)

                val filasMuestra = listOf(
                    Triple("Medicamentos OTC", "842 tx", "S/ 38,450.00 (45.6%)"),
                    Triple("Medicamentos Éticos / Receta", "520 tx", "S/ 26,800.00 (31.8%)"),
                    Triple("Cuidado Personal & Higiene", "210 tx", "S/ 9,420.00 (11.2%)"),
                    Triple("Nutrición & Bebidas", "115 tx", "S/ 5,800.00 (6.9%)"),
                    Triple("Material Médico & Suplementos", "50 tx", "S/ 3,780.00 (4.5%)")
                )

                filasMuestra.forEach { (nombre, tx, total) ->
                    Surface(color = FDColors.SurfaceElevated, shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(nombre, style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold), color = FDColors.TextPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(tx, style = FDType.Caption.copy(fontSize = 12.sp), color = FDColors.TextSecondary)
                                Text(total, style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold), color = FDColors.Primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. SECCIÓN RENTABILIDAD
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AnaliticaRentabilidadSection(periodo: String, s: MedidaAdaptativa) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("UTILIDAD BRUTA TOTAL", style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                    Text("S/ 31,172.50", style = FDType.Heading1.copy(fontSize = 22.sp, fontWeight = FontWeight.Black), color = FDColors.Success)
                    Text("Margen promedio: 37.0% sobre S/ 84,250.00 en ventas", style = FDType.Caption.copy(fontSize = 11.5.sp), color = FDColors.TextSecondary)
                }

                Surface(color = FDColors.SuccessSubtle, shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), horizontalAlignment = Alignment.End) {
                        Text("Costo de Venta (COGS)", style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.Success)
                        Text("S/ 53,077.50", style = FDType.Body.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold), color = FDColors.Success)
                    }
                }
            }
        }

        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MARGEN DE GANANCIA POR PRODUCTO CLAVE", style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)

                val productosMargen = listOf(
                    FilaRentabilidadEnterprise("Vitamina C 1000mg Tab eferv", "S/ 8,500.00", "S/ 5,200.00", "S/ 3,300.00", "38.8%", "ALTO MARGEN"),
                    FilaRentabilidadEnterprise("Paracetamol 500mg Genérico", "S/ 6,200.00", "S/ 5,500.00", "S/ 700.00", "11.3%", "BAJO MARGEN"),
                    FilaRentabilidadEnterprise("Amoxicilina 500mg Cap", "S/ 12,400.00", "S/ 8,100.00", "S/ 4,300.00", "34.7%", "ALTO VOLUMEN")
                )

                productosMargen.forEach { p ->
                    Surface(color = FDColors.SurfaceElevated, shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(p.nombre, style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                                Text("Vendido: ${p.ventas} • Costo: ${p.costo}", style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextTertiary)
                            }
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                Text("Utilidad: ${p.utilidad}", style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold), color = FDColors.Success)
                                Text("Margen: ${p.margen}", style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = FDColors.Primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. SECCIÓN INVENTARIO
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AnaliticaInventarioSection(s: MedidaAdaptativa) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = FDColors.WarningSubtle,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Warning, null, tint = FDColors.Warning, modifier = Modifier.size(24.dp))
                    Column {
                        Text("INVENTARIO INMOVILIZADO DETECTADO", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = FDColors.Warning)
                        Text("S/ 18,420.00 en productos sin venta durante más de 90 días.", style = FDType.Heading2.copy(fontSize = 14.sp, fontFamily = InterPremium, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                    }
                }

                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Warning, contentColor = FDColors.PrimaryText),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Ver Productos", style = FDType.Caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricKpiSaaS("Valor Valorizado", "S/ 142,850.00", "Actual", true, Icons.Default.Inventory2, s, Modifier.weight(1f))
            MetricKpiSaaS("Stock Disponible", "18,450 unid.", "Disponibles", true, Icons.Default.Category, s, Modifier.weight(1f))
            MetricKpiSaaS("Rotación Promedio", "4.2x / año", "Saludable", true, Icons.Default.Autorenew, s, Modifier.weight(1f))
            MetricKpiSaaS("Quiebres de Stock", "8 productos", "Urgente", false, Icons.Default.ErrorOutline, s, Modifier.weight(1f))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. SECCIONES RESTANTES
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AnaliticaComprasSection(periodo: String, s: MedidaAdaptativa) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MetricKpiSaaS("Compras Totales", "S/ 53,077.50", "-3.1%", false, Icons.Default.LocalShipping, s, Modifier.fillMaxWidth())
        Surface(color = FDColors.Surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, FDColors.Border), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("VARIACIÓN DE PRECIOS DETECTADA", style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold), color = FDColors.Warning)
                Text("El costo del producto 'Amoxicilina 500mg' aumentó 18% respecto a la última compra.", style = FDType.Body.copy(fontSize = 12.5.sp), color = FDColors.TextPrimary)
            }
        }
    }
}

@Composable
fun AnaliticaClientesSection(periodo: String, s: MedidaAdaptativa) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        MetricKpiSaaS("Clientes Nuevos", "142 nuevos", "+14%", true, Icons.Default.PersonAdd, s, Modifier.weight(1f))
        MetricKpiSaaS("Clientes Recurrentes", "1,278 activos", "+8%", true, Icons.Default.Groups, s, Modifier.weight(1f))
        MetricKpiSaaS("Frecuencia Compra", "2.1x / mes", "Estable", true, Icons.Default.Repeat, s, Modifier.weight(1f))
    }
}

@Composable
fun AnaliticaCajaSection(periodo: String, s: MedidaAdaptativa) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        MetricKpiSaaS("Efectivo", "S/ 32,500.00", "38.5%", true, Icons.Default.Payments, s, Modifier.weight(1f))
        MetricKpiSaaS("Yape / Plin", "S/ 28,400.00", "33.7%", true, Icons.Default.QrCode, s, Modifier.weight(1f))
        MetricKpiSaaS("Tarjetas POS", "S/ 18,350.00", "21.8%", true, Icons.Default.CreditCard, s, Modifier.weight(1f))
        MetricKpiSaaS("Transferencias", "S/ 5,000.00", "6.0%", true, Icons.Default.AccountBalance, s, Modifier.weight(1f))
    }
}

@Composable
fun AnaliticaSucursalesSection(s: MedidaAdaptativa) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MetricKpiSaaS("Sede Principal", "S/ 54,200.00", "64.3%", true, Icons.Default.Storefront, s, Modifier.fillMaxWidth())
        MetricKpiSaaS("Sucursal Norte", "S/ 30,050.00", "35.7%", true, Icons.Default.Storefront, s, Modifier.fillMaxWidth())
    }
}

@Composable
fun AnaliticaInsightsSection(s: MedidaAdaptativa) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InsightCardEnterprise("Ventas aumentaron 16% esta semana", "El crecimiento viene principalmente de medicamentos OTC e higiene.", FDColors.Primary, s)
        InsightCardEnterprise("Riesgo de quiebre de stock", "8 productos clave podrían quedarse sin stock durante los próximos 7 días.", FDColors.Error, s)
        InsightCardEnterprise("Oportunidad de margen", "4 productos representan el 22% de tus ventas pero tienen margen inferior al 8%.", FDColors.Warning, s)
        InsightCardEnterprise("Inventario lento", "S/ 7,240.00 están en productos sin movimiento durante más de 60 días.", FDColors.TextSecondary, s)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// COMPONENTES AUXILIARES COMPACTOS
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun MetricKpiSaaS(
    titulo: String,
    valor: String,
    tag: String,
    esPositivo: Boolean,
    icono: ImageVector,
    s: MedidaAdaptativa,
    modifier: Modifier = Modifier
) {
    Surface(
        color = FDColors.Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icono, null, tint = FDColors.Primary, modifier = Modifier.size(18.dp))
                Surface(
                    color = if (esPositivo) FDColors.SuccessSubtle else FDColors.ErrorSubtle,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = tag,
                        style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                        color = if (esPositivo) FDColors.Success else FDColors.Error,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column {
                Text(valor, style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterPremium), color = FDColors.TextPrimary)
                Text(titulo, style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun InsightCardEnterprise(titulo: String, descripcion: String, colorAccento: Color, s: MedidaAdaptativa) {
    Surface(
        color = FDColors.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(6.dp).background(colorAccento, RoundedCornerShape(50)))
            Column {
                Text(titulo, style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                Text(descripcion, style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextSecondary)
            }
        }
    }
}

private data class ResumenCategoriaFila(
    val categoria: String,
    val ventas: String,
    val costo: String,
    val utilidad: String,
    val margen: String
)

private data class FilaRentabilidadEnterprise(
    val nombre: String,
    val ventas: String,
    val costo: String,
    val utilidad: String,
    val margen: String,
    val tag: String
)
