package com.app.administradorfarmadon.analitica_reportes.rentabilidad.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.analitica_reportes.comunes.ui.EstadoVacioSaaS
import com.app.administradorfarmadon.analitica_reportes.comunes.ui.MetricKpiSaaS
import com.app.administradorfarmadon.analitica_reportes.logica.EstadoResultadoNegocio
import com.app.administradorfarmadon.analitica_reportes.logica.MargenProductoItem
import com.app.administradorfarmadon.analitica_reportes.logica.RentabilidadAnalytics
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// SUBMÓDULO: RENTABILIDAD (AUDITORÍA CONTABLE Y VERDAD ECONÓMICA DE PRODUCTO)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AnaliticaRentabilidadSection(
    rentabilidad: RentabilidadAnalytics,
    estadoResultado: EstadoResultadoNegocio = EstadoResultadoNegocio(),
    tieneCostosEstimados: Boolean,
    periodo: String,
    sedeNombre: String = "Todas las sedes",
    s: MedidaAdaptativa
) {
    val listaMaestra = remember(rentabilidad) {
        if (rentabilidad.todosLosProductos.isNotEmpty()) {
            rentabilidad.todosLosProductos
        } else {
            (rentabilidad.productosMayorGananciaDinero + rentabilidad.productosMayorMargen + rentabilidad.productosMenorMargen).distinctBy { it.productoId }
        }
    }

    val esVacio = listaMaestra.isEmpty()

    if (esVacio) {
        EstadoVacioSaaS(
            titulo = "Sin movimientos en este período",
            subtitulo = "No se registraron ventas con costo calculado para determinar márgenes en $periodo."
        )
        return
    }

    // Filtros de tabla
    var tabSeleccionado by remember { mutableStateOf("TODOS") } // TODOS, MAYOR_GANANCIA, MAYOR_MARGEN, MENOR_MARGEN, PERDIDAS
    var filtroBusqueda by remember { mutableStateOf("") }

    val listaPorTab = remember(tabSeleccionado, listaMaestra, rentabilidad) {
        when (tabSeleccionado) {
            "MAYOR_GANANCIA" -> rentabilidad.productosMayorGananciaDinero
            "MAYOR_MARGEN" -> rentabilidad.productosMayorMargen
            "MENOR_MARGEN" -> rentabilidad.productosMenorMargen
            "PERDIDAS" -> rentabilidad.productosConPerdida
            else -> listaMaestra
        }
    }

    val productosFiltrados = remember(listaPorTab, filtroBusqueda) {
        if (filtroBusqueda.isBlank()) listaPorTab
        else listaPorTab.filter {
            it.nombre.contains(filtroBusqueda.trim(), ignoreCase = true) ||
            it.presentacion.contains(filtroBusqueda.trim(), ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner honesto si hay ventas con costo estimado (🟡 X% sin costo congelado)
        if (tieneCostosEstimados || rentabilidad.tieneCostosEstimados) {
            Surface(
                color = FDColors.WarningSubtle,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.WarningAmber, null, tint = FDColors.Warning, modifier = Modifier.size(20.dp))
                    val textoEstimado = if (rentabilidad.porcentajeSinCostoCongelado > 0.0) {
                        "Margen estimado: el %.1f%% de las ventas (%d ventas) se registró sin costo congelado al vender. El margen real puede variar.".format(
                            Locale.US,
                            rentabilidad.porcentajeSinCostoCongelado,
                            rentabilidad.cantidadVentasSinCosto
                        )
                    } else {
                        "Margen estimado: se detectaron ventas sin costo asignado al momento de la venta. El margen real puede variar."
                    }
                    Text(
                        text = textoEstimado,
                        style = FDType.BodySmall.copy(fontWeight = FontWeight.Medium),
                        color = FDColors.TextPrimary
                    )
                }
            }
        }

        // ── 2. ALERTA CRÍTICA: PRODUCTOS VENDIDOS CON PÉRDIDA (-S/) ──
        if (rentabilidad.productosConPerdida.isNotEmpty()) {
            Surface(
                color = FDColors.ErrorSubtle,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.TrendingDown, null, tint = FDColors.Error, modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "ALERTA FINANCIERA: ${rentabilidad.productosConPerdida.size} producto(s) con margen negativo (pérdida)",
                            style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold),
                            color = FDColors.Error
                        )
                        Text(
                            text = "Se vendieron por debajo de su costo de adquisición o las devoluciones superaron la facturación del período. Revise precios de venta o notas de crédito.",
                            style = FDType.Caption.copy(fontSize = 10.5.sp),
                            color = FDColors.TextPrimary
                        )
                    }
                    OutlinedButton(
                        onClick = { tabSeleccionado = "PERDIDAS" },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, FDColors.Error),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.Error),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Ver Pérdidas [ ${rentabilidad.productosConPerdida.size} ]", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // ── 3. KPIS CENTRALES DE RENTABILIDAD ──
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricKpiSaaS(
                titulo = "UTILIDAD BRUTA",
                valor = "S/ %.2f".format(Locale.US, rentabilidad.utilidadBruta),
                tag = "Total Ganancia Neta",
                colorValor = if (rentabilidad.utilidadBruta >= 0.0) FDColors.Primary else FDColors.Error,
                modifier = Modifier.weight(1f)
            )
            MetricKpiSaaS(
                titulo = "MARGEN GENERAL",
                valor = "%.1f%%".format(Locale.US, rentabilidad.margenGeneral),
                tag = "Retorno Neto sobre Ventas",
                colorValor = if (rentabilidad.margenGeneral >= 25.0) FDColors.Success else FDColors.Warning,
                modifier = Modifier.weight(1f)
            )
            MetricKpiSaaS(
                titulo = "COSTO DE LO VENDIDO (COGS)",
                valor = "S/ %.2f".format(Locale.US, rentabilidad.costoVentasCogs),
                tag = "Costo Real al Vender",
                modifier = Modifier.weight(1f)
            )
        }

        // ── 4. TABLERO COMPARATIVO EJECUTIVO (3 PANELES RIGUROSOS) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Panel 1: Mayor Ganancia en Dinero (+S/)
            PanelRankingRentabilidad(
                titulo = "Mayor Ganancia en Dinero",
                subtitulo = "Productos que más soles aportan a la farmacia",
                tagConteo = "[ ${rentabilidad.productosMayorGananciaDinero.size} ]",
                colorAcento = FDColors.Success,
                items = rentabilidad.productosMayorGananciaDinero,
                criterioEsDinero = true,
                modifier = Modifier.weight(1f)
            )

            // Panel 2: Mayor Margen Porcentual (%)
            PanelRankingRentabilidad(
                titulo = "Mayor Margen (%)",
                subtitulo = "Mayor rendimiento por cada sol invertido",
                tagConteo = "[ ${rentabilidad.productosMayorMargen.size} ]",
                colorAcento = FDColors.Primary,
                items = rentabilidad.productosMayorMargen,
                criterioEsDinero = false,
                modifier = Modifier.weight(1f)
            )

            // Panel 3: Menor Margen / Ajustado (%)
            PanelRankingRentabilidad(
                titulo = "Menor Margen / Ajustado",
                subtitulo = "Margen comercial positivo bajo",
                tagConteo = "[ ${rentabilidad.productosMenorMargen.size} ]",
                colorAcento = FDColors.Warning,
                items = rentabilidad.productosMenorMargen,
                criterioEsDinero = false,
                colorMargenBajo = true,
                modifier = Modifier.weight(1f)
            )
        }

        // ── 5. TABLA COMPLETA DE TODOS LOS PRODUCTOS VENDIDOS (CERO OMISIÓN R3/R12) ──
        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cabecera superior con buscador
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "AUDITORÍA DE COSTOS Y RENTABILIDAD POR PRODUCTO",
                            style = FDType.Heading2.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            text = "Desglose independiente por presentación física y lote (${listaMaestra.size} ítems vendidos en $periodo)",
                            style = FDType.Caption.copy(fontSize = 10.5.sp),
                            color = FDColors.TextSecondary
                        )
                    }

                    // Buscador directo sin recargar
                    OutlinedTextField(
                        value = filtroBusqueda,
                        onValueChange = { filtroBusqueda = it },
                        placeholder = { Text("Buscar producto o presentación...", style = FDType.Caption.copy(fontSize = 11.sp)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp), tint = FDColors.TextTertiary) },
                        trailingIcon = {
                            if (filtroBusqueda.isNotBlank()) {
                                IconButton(onClick = { filtroBusqueda = "" }) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = FDColors.TextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.width(280.dp).height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FDColors.Primary,
                            unfocusedBorderColor = FDColors.Border
                        )
                    )
                }

                // Selector de pestañas / filtros de tabla
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabFiltroRentabilidad(
                        label = "Todos",
                        conteo = listaMaestra.size,
                        seleccionado = tabSeleccionado == "TODOS",
                        onClick = { tabSeleccionado = "TODOS" }
                    )
                    TabFiltroRentabilidad(
                        label = "Mayor Ganancia (+S/)",
                        conteo = rentabilidad.productosMayorGananciaDinero.size,
                        seleccionado = tabSeleccionado == "MAYOR_GANANCIA",
                        colorActivo = FDColors.Success,
                        onClick = { tabSeleccionado = "MAYOR_GANANCIA" }
                    )
                    TabFiltroRentabilidad(
                        label = "Mayor Margen (%)",
                        conteo = rentabilidad.productosMayorMargen.size,
                        seleccionado = tabSeleccionado == "MAYOR_MARGEN",
                        colorActivo = FDColors.Primary,
                        onClick = { tabSeleccionado = "MAYOR_MARGEN" }
                    )
                    TabFiltroRentabilidad(
                        label = "Menor Margen (%)",
                        conteo = rentabilidad.productosMenorMargen.size,
                        seleccionado = tabSeleccionado == "MENOR_MARGEN",
                        colorActivo = FDColors.Warning,
                        onClick = { tabSeleccionado = "MENOR_MARGEN" }
                    )
                    if (rentabilidad.productosConPerdida.isNotEmpty()) {
                        TabFiltroRentabilidad(
                            label = "Con Pérdida (-S/)",
                            conteo = rentabilidad.productosConPerdida.size,
                            seleccionado = tabSeleccionado == "PERDIDAS",
                            colorActivo = FDColors.Error,
                            onClick = { tabSeleccionado = "PERDIDAS" }
                        )
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.35f))

                // Encabezados de tabla
                Surface(
                    color = FDColors.SurfaceElevated,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PRODUCTO Y PRESENTACIÓN", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, modifier = Modifier.weight(2.4f))
                        Text("UNIDADES", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.TextSecondary, modifier = Modifier.weight(0.9f))
                        Text("VENTAS NETAS", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.TextSecondary, modifier = Modifier.weight(1.1f))
                        Text("COSTO REAL", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.TextSecondary, modifier = Modifier.weight(1.1f))
                        Text("GANANCIA (S/)", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.TextSecondary, modifier = Modifier.weight(1.1f))
                        Text("MARGEN %", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.TextSecondary, modifier = Modifier.weight(0.9f))
                    }
                }

                // Filas de productos
                if (productosFiltrados.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (filtroBusqueda.isNotBlank()) "No se encontraron productos con '$filtroBusqueda'" else "Sin productos en esta vista",
                            style = FDType.Caption,
                            color = FDColors.TextTertiary
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        productosFiltrados.forEachIndexed { idx, p ->
                            Surface(
                                color = if (idx % 2 == 1) FDColors.Background else FDColors.Surface,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Nombre y Presentación
                                    Column(modifier = Modifier.weight(2.4f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = p.nombre,
                                                style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                                                color = FDColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (p.presentacion.isNotBlank() && !p.nombre.contains(p.presentacion, ignoreCase = true)) {
                                                Surface(
                                                    color = FDColors.InputBackground,
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = BorderStroke(0.5.dp, FDColors.Border)
                                                ) {
                                                    Text(
                                                        text = p.presentacion,
                                                        style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                                                        color = FDColors.TextSecondary,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (p.esEstimado) {
                                            Text("Costo histórico no registrado (Venta anterior)", style = FDType.Caption.copy(fontSize = 8.5.sp), color = FDColors.Warning)
                                        }
                                    }

                                    // 2. Unidades
                                    Column(modifier = Modifier.weight(0.9f), horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${p.unidadesEfectivas} uds",
                                            style = FDType.BodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                            color = FDColors.TextPrimary
                                        )
                                        if (p.unidadesDevueltas > 0) {
                                            Text(
                                                text = "-${p.unidadesDevueltas} dev.",
                                                style = FDType.Caption.copy(fontSize = 8.5.sp),
                                                color = FDColors.Error
                                            )
                                        }
                                    }

                                    // 3. Ventas Netas
                                    Text(
                                        text = "S/ %.2f".format(Locale.US, p.ventasNetas),
                                        style = FDType.BodySmall.copy(fontSize = 11.sp, textAlign = TextAlign.End),
                                        color = FDColors.TextPrimary,
                                        modifier = Modifier.weight(1.1f)
                                    )

                                    // 4. Costo Real
                                    Text(
                                        text = "S/ %.2f".format(Locale.US, p.costoNeto),
                                        style = FDType.BodySmall.copy(fontSize = 11.sp, textAlign = TextAlign.End),
                                        color = FDColors.TextSecondary,
                                        modifier = Modifier.weight(1.1f)
                                    )

                                    // 5. Ganancia (S/)
                                    Text(
                                        text = (if (p.utilidad >= 0.0) "+S/ %.2f" else "S/ %.2f").format(Locale.US, p.utilidad),
                                        style = FDType.BodySmall.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.End
                                        ),
                                        color = if (p.utilidad >= 0.0) FDColors.Success else FDColors.Error,
                                        modifier = Modifier.weight(1.1f)
                                    )

                                    // 6. Margen %
                                    Text(
                                        text = "%.1f%%".format(Locale.US, p.margenPorcentaje),
                                        style = FDType.BodySmall.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.End
                                        ),
                                        color = when {
                                            p.esPerdida -> FDColors.Error
                                            p.margenPorcentaje >= 35.0 -> FDColors.Success
                                            p.margenPorcentaje >= 15.0 -> FDColors.TextPrimary
                                            else -> FDColors.Warning
                                        },
                                        modifier = Modifier.weight(0.9f)
                                    )
                                }
                            }
                        }
                    }

                    // ── BARRA TOTALIZADORA (CONCILIACIÓN EN VIVO) ──
                    val totalVentasFiltradas = productosFiltrados.sumOf { it.ventasNetas }
                    val totalCostoFiltrado = productosFiltrados.sumOf { it.costoNeto }
                    val totalUtilidadFiltrada = productosFiltrados.sumOf { it.utilidad }
                    val margenPonderadoFiltrado = if (totalVentasFiltradas > 0.0) (totalUtilidadFiltrada / totalVentasFiltradas) * 100.0 else 0.0

                    Surface(
                        color = FDColors.SurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, FDColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTALES DE ESTA VISTA (${productosFiltrados.size} ÍTEMS)",
                                style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextSecondary,
                                modifier = Modifier.weight(3.3f)
                            )
                            Text(
                                text = "S/ %.2f".format(Locale.US, totalVentasFiltradas),
                                style = FDType.BodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                                color = FDColors.TextPrimary,
                                modifier = Modifier.weight(1.1f)
                            )
                            Text(
                                text = "S/ %.2f".format(Locale.US, totalCostoFiltrado),
                                style = FDType.BodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                                color = FDColors.TextSecondary,
                                modifier = Modifier.weight(1.1f)
                            )
                            Text(
                                text = (if (totalUtilidadFiltrada >= 0.0) "+S/ %.2f" else "S/ %.2f").format(Locale.US, totalUtilidadFiltrada),
                                style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                                color = if (totalUtilidadFiltrada >= 0.0) FDColors.Success else FDColors.Error,
                                modifier = Modifier.weight(1.1f)
                            )
                            Text(
                                text = "%.1f%%".format(Locale.US, margenPonderadoFiltrado),
                                style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
                                color = if (margenPonderadoFiltrado >= 20.0) FDColors.Success else FDColors.Warning,
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// COMPONENTES PRIVADOS DE SOPORTE PARA RENTABILIDAD
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PanelRankingRentabilidad(
    titulo: String,
    subtitulo: String,
    tagConteo: String,
    colorAcento: androidx.compose.ui.graphics.Color,
    items: List<MargenProductoItem>,
    criterioEsDinero: Boolean,
    colorMargenBajo: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = FDColors.Surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titulo,
                    style = FDType.Heading2.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = tagConteo,
                    style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    color = colorAcento
                )
            }
            Text(
                text = subtitulo,
                style = FDType.Caption.copy(fontSize = 9.5.sp),
                color = FDColors.TextSecondary
            )
            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Sin datos para este ranking", style = FDType.Caption, color = FDColors.TextTertiary)
                }
            } else {
                items.forEach { p ->
                    Surface(
                        color = FDColors.InputBackground.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = p.nombre,
                                    style = FDType.BodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                    color = FDColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${p.unidadesEfectivas} uds · Ventas: S/ %.2f · Costo: S/ %.2f".format(Locale.US, p.ventasNetas, p.costoNeto),
                                    style = FDType.Caption.copy(fontSize = 9.sp),
                                    color = FDColors.TextSecondary
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                if (criterioEsDinero) {
                                    Text(
                                        text = "+S/ %.2f".format(Locale.US, p.utilidad),
                                        style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                        color = FDColors.Success
                                    )
                                    Text(
                                        text = "%.1f%% margen".format(Locale.US, p.margenPorcentaje),
                                        style = FDType.Caption.copy(fontSize = 8.5.sp),
                                        color = FDColors.TextTertiary
                                    )
                                } else {
                                    Text(
                                        text = "%.1f%%".format(Locale.US, p.margenPorcentaje),
                                        style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                        color = if (colorMargenBajo) FDColors.Warning else FDColors.Success
                                    )
                                    Text(
                                        text = "+S/ %.2f".format(Locale.US, p.utilidad),
                                        style = FDType.Caption.copy(fontSize = 8.5.sp),
                                        color = FDColors.TextTertiary
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

@Composable
private fun TabFiltroRentabilidad(
    label: String,
    conteo: Int,
    seleccionado: Boolean,
    colorActivo: androidx.compose.ui.graphics.Color = FDColors.Primary,
    onClick: () -> Unit
) {
    Surface(
        color = if (seleccionado) colorActivo.copy(alpha = 0.12f) else FDColors.InputBackground.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (seleccionado) colorActivo else FDColors.Border.copy(alpha = 0.5f)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium),
                color = if (seleccionado) colorActivo else FDColors.TextPrimary
            )
            Surface(
                color = if (seleccionado) colorActivo else FDColors.Border,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = conteo.toString(),
                    style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = if (seleccionado) FDColors.PrimaryText else FDColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
