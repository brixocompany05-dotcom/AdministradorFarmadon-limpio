package com.app.administradorfarmadon.analitica_reportes.ventas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.analitica_reportes.comunes.ui.BadgeEstadoVenta
import com.app.administradorfarmadon.analitica_reportes.comunes.ui.EstadoVacioSaaS
import com.app.administradorfarmadon.analitica_reportes.comunes.ui.MetricKpiSaaS
import com.app.administradorfarmadon.analitica_reportes.logica.AnaliticaCalculadora
import com.app.administradorfarmadon.analitica_reportes.logica.MetricasVentas
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.ventas.compartido.modelo.ClienteDeVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.PagoVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ═════════════════════════════════════════════════════════════════════════════
// SUBMÓDULO: VENTAS (VOLUMEN COMERCIAL, COMPORTAMIENTO Y AUDITORÍA DE COMPROBANTES)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AnaliticaVentasSection(
    ventas: List<Venta>,
    devoluciones: List<DevolucionVenta>,
    metricas: MetricasVentas,
    periodo: String,
    sedeNombre: String = "Todas las sedes",
    s: MedidaAdaptativa,
    onVerDetalleVenta: (Venta) -> Unit
) {
    if (ventas.isEmpty() && devoluciones.isEmpty()) {
        EstadoVacioSaaS(
            titulo = "Sin ventas en este período",
            subtitulo = "No se registran comprobantes cobrados para las fechas seleccionadas en $periodo."
        )
        return
    }

    // Motor de analítica de ventas para horarios y vendedores (100% tiempo real desde Firestore)
    val ventasAnalytics = remember(ventas, devoluciones) {
        AnaliticaCalculadora.calcularVentas(ventas, devoluciones)
    }

    // Métricas de tipos de comprobante de todo el período (R13)
    val totalEmitidos = ventas.size
    val boletas = remember(ventas) {
        ventas.filter { it.tipoComprobante.contains("BOLETA", ignoreCase = true) || it.serie.startsWith("B", ignoreCase = true) }
    }
    val facturas = remember(ventas) {
        ventas.filter { it.tipoComprobante.contains("FACTURA", ignoreCase = true) || it.serie.startsWith("F", ignoreCase = true) }
    }
    val conDevolucion = remember(ventas) {
        ventas.filter {
            it.estado == Venta.ESTADO_DEVOLUCION_PARCIAL ||
            it.estado == Venta.ESTADO_DEVOLUCION_TOTAL ||
            it.totalDevueltoProrrateado > 0.0 ||
            it.totalDevuelto > 0.0
        }
    }
    val anulados = remember(ventas) {
        ventas.filter { it.estado == Venta.ESTADO_ANULADA }
    }

    // Filtro por pestaña y buscador
    var tabSeleccionado by remember { mutableStateOf("TODOS") } // TODOS, BOLETAS, FACTURAS, CON_DEVOLUCION, ANULADOS
    var filtroBusqueda by remember { mutableStateOf("") }
    var paginaActual by remember { mutableIntStateOf(1) }
    val itemsPorPagina = 25

    val ventasFiltradasPorTab = remember(tabSeleccionado, ventas, boletas, facturas, conDevolucion, anulados) {
        when (tabSeleccionado) {
            "BOLETAS" -> boletas
            "FACTURAS" -> facturas
            "CON_DEVOLUCION" -> conDevolucion
            "ANULADOS" -> anulados
            else -> ventas
        }
    }

    val ventasFiltradas = remember(ventasFiltradasPorTab, filtroBusqueda) {
        if (filtroBusqueda.isBlank()) ventasFiltradasPorTab
        else {
            val q = filtroBusqueda.trim()
            ventasFiltradasPorTab.filter {
                it.numeroCompleto.contains(q, ignoreCase = true) ||
                it.cliente.nombre.contains(q, ignoreCase = true) ||
                it.cliente.numeroDocumento.contains(q, ignoreCase = true) ||
                it.cajeroNombre.contains(q, ignoreCase = true) ||
                it.anulacionMotivo.contains(q, ignoreCase = true) ||
                it.numeroNotaCredito.contains(q, ignoreCase = true) ||
                it.devolucionMotivo.contains(q, ignoreCase = true) ||
                it.devolucionNumeroNotaCredito.contains(q, ignoreCase = true)
            }
        }
    }

    // Reseteo de página al cambiar filtros
    LaunchedEffect(tabSeleccionado, filtroBusqueda) {
        paginaActual = 1
    }

    val totalPaginas = remember(ventasFiltradas.size) {
        maxOf(1, (ventasFiltradas.size + itemsPorPagina - 1) / itemsPorPagina)
    }

    val ventasPaginadas = remember(ventasFiltradas, paginaActual) {
        val start = (paginaActual - 1) * itemsPorPagina
        ventasFiltradas.drop(start).take(itemsPorPagina)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. KPIS DEL RITMO COMERCIAL (4 TARJETAS GRANDES) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricKpiSaaS(
                titulo = "VENTAS NETAS",
                valor = "S/ %.2f".format(Locale.US, metricas.ventasNetas),
                tag = "Total Cobrado Real",
                colorValor = FDColors.Primary,
                modifier = Modifier.weight(1f)
            )
            MetricKpiSaaS(
                titulo = "VENTAS BRUTAS",
                valor = "S/ %.2f".format(Locale.US, metricas.ventasBrutas),
                tag = if (metricas.devoluciones > 0.0) "-S/ %.2f dev.".format(Locale.US, metricas.devoluciones) else "Sin devoluciones",
                colorValor = FDColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricKpiSaaS(
                titulo = "TICKET PROMEDIO",
                valor = "S/ %.2f".format(Locale.US, metricas.ticketPromedio),
                tag = "Gasto Promedio / Cliente",
                colorValor = FDColors.Success,
                modifier = Modifier.weight(1f)
            )
            MetricKpiSaaS(
                titulo = "COMPROBANTES COBRADOS",
                valor = "${metricas.cantidadTransacciones} ops",
                tag = "${boletas.size} Boletas · ${facturas.size} Facturas",
                modifier = Modifier.weight(1f)
            )
        }

        // ── 2. PANELES DE COMPORTAMIENTO COMERCIAL (GRID 3 COLUMNAS NIVELADAS CON SCROLL INTERNO) ──
        Row(
            modifier = Modifier.fillMaxWidth().height(215.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Panel 1: Medios de Cobro (Canales Reales con Contabilidad Rigurosa)
            PanelMediosDePago(
                ventasPorMetodo = metricas.ventasPorMetodo,
                ticketsPorMetodo = metricas.ticketsPorMetodo,
                totalNeto = metricas.ventasNetas,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            // Panel 2: Horarios Punta (Cálculo Real desde fechaHoraMs en UTC-5)
            PanelHorariosPunta(
                ventasPorHora = ventasAnalytics.ventasPorHoraTurno,
                ticketsPorHora = ventasAnalytics.ticketsPorHora,
                totalNeto = metricas.ventasNetas,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            // Panel 3: Ventas por Cajero / Vendedor (Scroll Interno con >10 cajeros)
            PanelVentasPorVendedor(
                ventasPorVendedor = ventasAnalytics.ventasPorVendedor,
                ticketsPorVendedor = ventasAnalytics.ticketsPorVendedor,
                totalNeto = metricas.ventasNetas,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // ── 2B. CONTROL DEL EQUIPO: DESCUENTOS, RECETAS Y DEVOLUCIONES CON RESPONSABLE ──
        PanelControlEquipo(
            ventas = ventas,
            devoluciones = devoluciones,
            periodo = periodo
        )

        // ── 3. AUDITORÍA DE COMPROBANTES: EL LIBRO DIARIO DE VENTAS ──
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
                // Cabecera superior con título y buscador
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "LIBRO DIARIO DE COMPROBANTES EMITIDOS",
                            style = FDType.Heading2.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            text = "Historial auditable de boletas y facturas ($totalEmitidos comprobantes en $periodo)",
                            style = FDType.Caption.copy(fontSize = 10.5.sp),
                            color = FDColors.TextSecondary
                        )
                    }

                    // Buscador con centrado vertical ergonómico (cero recorte de texto)
                    Surface(
                        color = FDColors.InputBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (filtroBusqueda.isNotBlank()) FDColors.Primary else FDColors.Border
                        ),
                        modifier = Modifier
                            .width(300.dp)
                            .height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = if (filtroBusqueda.isNotBlank()) FDColors.Primary else FDColors.TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (filtroBusqueda.isEmpty()) {
                                    Text(
                                        text = "Buscar serie, cliente, motivo...",
                                        style = FDType.Caption.copy(fontSize = 11.5.sp),
                                        color = FDColors.TextTertiary
                                    )
                                }
                                BasicTextField(
                                    value = filtroBusqueda,
                                    onValueChange = { filtroBusqueda = it },
                                    singleLine = true,
                                    textStyle = FDType.BodySmall.copy(
                                        fontSize = 12.sp,
                                        color = FDColors.TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(FDColors.Primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (filtroBusqueda.isNotBlank()) {
                                IconButton(
                                    onClick = { filtroBusqueda = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpiar",
                                        tint = FDColors.TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Pestañas de filtrado rápido con cantidades reales de todo el período (R13)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabFiltroVenta("Todos", totalEmitidos, tabSeleccionado == "TODOS") { tabSeleccionado = "TODOS" }
                    TabFiltroVenta("Boletas", boletas.size, tabSeleccionado == "BOLETAS", FDColors.Primary) { tabSeleccionado = "BOLETAS" }
                    TabFiltroVenta("Facturas", facturas.size, tabSeleccionado == "FACTURAS", FDColors.Success) { tabSeleccionado = "FACTURAS" }
                    TabFiltroVenta("Con Devolución", conDevolucion.size, tabSeleccionado == "CON_DEVOLUCION", FDColors.Warning) { tabSeleccionado = "CON_DEVOLUCION" }
                    TabFiltroVenta("Anulados", anulados.size, tabSeleccionado == "ANULADOS", FDColors.Error) { tabSeleccionado = "ANULADOS" }
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
                        Text("FECHA Y HORA", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, modifier = Modifier.weight(1.2f))
                        Text("COMPROBANTE", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, modifier = Modifier.weight(1.5f))
                        Text("CLIENTE", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, modifier = Modifier.weight(2.0f))
                        Text("MEDIO PAGO", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, modifier = Modifier.weight(1.3f))
                        Text("CAJERO", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary, modifier = Modifier.weight(1.3f))
                        Text("ESTADO", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), color = FDColors.TextSecondary, modifier = Modifier.weight(1.1f))
                        Text("TOTAL (S/)", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), color = FDColors.TextSecondary, modifier = Modifier.weight(1.1f))
                    }
                }

                // Filas de comprobantes
                if (ventasPaginadas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (filtroBusqueda.isNotBlank()) "No se encontraron comprobantes para '$filtroBusqueda'" else "Sin comprobantes en esta categoría",
                            style = FDType.Caption,
                            color = FDColors.TextTertiary
                        )
                    }
                } else {
                    val formatoHora = remember { SimpleDateFormat("dd/MM HH:mm", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("America/Lima") } }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ventasPaginadas.forEachIndexed { idx, v ->
                            val fechaStr = if (v.fechaHoraMs > 0) formatoHora.format(Date(v.fechaHoraMs)) else v.diaClave
                            val primerMetodo = v.pagos.firstOrNull()?.nombreMetodo?.ifBlank { v.pagos.firstOrNull()?.tipoId } ?: "Sin registro"
                            val metodoResumen = if (v.pagos.size > 1) "$primerMetodo (+${v.pagos.size - 1})" else primerMetodo

                            Surface(
                                color = if (idx % 2 == 1) FDColors.Background else FDColors.Surface,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onVerDetalleVenta(v) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Fecha y hora
                                    Text(
                                        text = fechaStr,
                                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                                        color = FDColors.TextSecondary,
                                        modifier = Modifier.weight(1.2f)
                                    )

                                    // 2. Comprobante
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            text = v.numeroCompleto.ifBlank { "S/N" },
                                            style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                            color = FDColors.TextPrimary
                                        )
                                        Text(
                                            text = v.tipoComprobante,
                                            style = FDType.Caption.copy(fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold),
                                            color = if (v.tipoComprobante == "FACTURA") FDColors.Success else FDColors.Primary
                                        )
                                    }

                                    // 3. Cliente
                                    Column(modifier = Modifier.weight(2.0f)) {
                                        Text(
                                            text = v.cliente.nombre.ifBlank { "Consumidor Final" },
                                            style = FDType.BodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                            color = FDColors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (v.cliente.numeroDocumento.isNotBlank()) {
                                            Text(
                                                text = "${v.cliente.tipoDocumento}: ${v.cliente.numeroDocumento}",
                                                style = FDType.Caption.copy(fontSize = 8.5.sp),
                                                color = FDColors.TextTertiary
                                            )
                                        }
                                    }

                                    // 4. Medio de pago
                                    Text(
                                        text = metodoResumen,
                                        style = FDType.Caption.copy(fontSize = 10.sp),
                                        color = FDColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1.3f)
                                    )

                                    // 5. Cajero
                                    Text(
                                        text = v.cajeroNombre.ifBlank { "Cajero" },
                                        style = FDType.Caption.copy(fontSize = 10.sp),
                                        color = FDColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1.3f)
                                    )

                                    // 6. Estado
                                    Column(
                                        modifier = Modifier.weight(1.1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        BadgeEstadoVenta(v.estado)
                                        if (v.estado == Venta.ESTADO_ANULADA && v.anulacionMotivo.isNotBlank()) {
                                            Text(
                                                text = v.anulacionMotivo,
                                                style = FDType.Caption.copy(fontSize = 8.sp, fontWeight = FontWeight.Medium),
                                                color = FDColors.Error,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else if (v.totalDevueltoProrrateado > 0.0 || v.devolucionMotivo.isNotBlank()) {
                                            val devTexto = if (v.devolucionMotivo.isNotBlank()) {
                                                "Dev: ${v.devolucionMotivo}"
                                            } else {
                                                "-S/ %.2f dev.".format(Locale.US, v.totalDevueltoProrrateado)
                                            }
                                            Text(
                                                text = devTexto,
                                                style = FDType.Caption.copy(fontSize = 8.sp, fontWeight = FontWeight.Medium),
                                                color = FDColors.Warning,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // 7. Total cobrado
                                    Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                                        if (v.estado == Venta.ESTADO_ANULADA) {
                                            Text(
                                                text = "S/ 0.00",
                                                style = FDType.BodySmall.copy(
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.End
                                                ),
                                                color = FDColors.Error
                                            )
                                            Text(
                                                text = "Orig: S/ %.2f".format(Locale.US, v.total),
                                                style = FDType.Caption.copy(fontSize = 8.5.sp),
                                                color = FDColors.TextTertiary
                                            )
                                        } else {
                                            val netoFila = (v.total - v.totalDevueltoProrrateado).coerceAtLeast(0.0)
                                            Text(
                                                text = "S/ %.2f".format(Locale.US, netoFila),
                                                style = FDType.BodySmall.copy(
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.End
                                                ),
                                                color = FDColors.Primary
                                            )
                                            if (v.totalDevueltoProrrateado > 0.0) {
                                                Text(
                                                    text = "Emitido: S/ %.2f".format(Locale.US, v.total),
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

                    // ── PAGINADOR ERGONÓMICO (CERO SCROLL INFINITO) ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val startIdx = (paginaActual - 1) * itemsPorPagina + 1
                        val endIdx = minOf(paginaActual * itemsPorPagina, ventasFiltradas.size)

                        Text(
                            text = "Mostrando $startIdx - $endIdx de ${ventasFiltradas.size} comprobantes",
                            style = FDType.Caption.copy(fontSize = 10.5.sp),
                            color = FDColors.TextSecondary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (paginaActual > 1) paginaActual-- },
                                enabled = paginaActual > 1,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(16.dp))
                                Text("Anterior", style = FDType.Caption.copy(fontSize = 10.sp))
                            }

                            Surface(
                                color = FDColors.InputBackground,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, FDColors.Border)
                            ) {
                                Text(
                                    text = "$paginaActual / $totalPaginas",
                                    style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                    color = FDColors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = { if (paginaActual < totalPaginas) paginaActual++ },
                                enabled = paginaActual < totalPaginas,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Siguiente", style = FDType.Caption.copy(fontSize = 10.sp))
                                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// COMPONENTES PRIVADOS DE SOPORTE PARA VENTAS
// ═════════════════════════════════════════════════════════════════════════════

private fun formatearNombreMetodoPago(raw: String): String {
    val trimmed = raw.trim()
    return when (trimmed.uppercase(Locale.US)) {
        "EFECTIVO" -> "Efectivo"
        "YAPE" -> "Yape"
        "PLIN" -> "Plin"
        "TARJETA", "TARJETA_POS" -> "Tarjeta POS"
        "TARJETA_VISA", "VISA" -> "Tarjeta Visa"
        "TARJETA_MASTERCARD", "MASTERCARD" -> "Tarjeta Mastercard"
        "TRANSFERENCIA", "TRANSFERENCIA_BANCARIA" -> "Transferencia"
        "CHEQUE" -> "Cheque"
        "SIN_ESPECIFICAR" -> "Sin especificar"
        else -> trimmed.replace("_", " ").lowercase(Locale.US).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    }
}

/**
 * Bloquea la propagación del scroll hacia el contenedor padre (R3/UX).
 * Evita que al deslizar dentro de tarjetas con scroll interno (cajeros, horas, medios de pago)
 * la pantalla principal salte o se mueva.
 */
@Composable
private fun rememberScrollPadreBloqueado(): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Consume todo el remanente para que el scroll padre no se mueva al llegar al tope o fin
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Consume el impulso remanente para que la pantalla padre no pegue saltos
                return available
            }
        }
    }
}

@Composable
private fun PanelMediosDePago(
    ventasPorMetodo: Map<String, Double>,
    ticketsPorMetodo: Map<String, Int> = emptyMap(),
    totalNeto: Double,
    modifier: Modifier = Modifier
) {
    val scrollBlocker = rememberScrollPadreBloqueado()

    Surface(
        color = FDColors.Surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
        modifier = modifier
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
                    Icon(Icons.Default.Payments, null, tint = FDColors.Primary, modifier = Modifier.size(16.dp))
                    Text("Métodos de Cobro", style = FDType.Heading2.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                }
                Text("[ ${ventasPorMetodo.size} canales ]", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.Primary)
            }
            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))

            if (ventasPorMetodo.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin datos de métodos de pago", style = FDType.Caption, color = FDColors.TextTertiary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(scrollBlocker)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ventasPorMetodo.entries.sortedByDescending { it.value }.forEachIndexed { idx, (metodo, monto) ->
                        val nombreHumano = formatearNombreMetodoPago(metodo)
                        val ops = ticketsPorMetodo[metodo] ?: 0
                        val pct = if (totalNeto > 0.0) ((monto / totalNeto) * 100.0).coerceIn(0.0, 100.0) else 0.0
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Surface(
                                        color = if (idx == 0) FDColors.Primary.copy(alpha = 0.15f) else FDColors.Border.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            text = "#${idx + 1}",
                                            style = FDType.Caption.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                                            color = if (idx == 0) FDColors.Primary else FDColors.TextSecondary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = nombreHumano,
                                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                                        color = FDColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = if (ops > 0) "S/ %.2f (%d ops · %.0f%%)".format(Locale.US, monto, ops, pct)
                                           else "S/ %.2f (%.0f%%)".format(Locale.US, monto, pct),
                                    style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                    color = FDColors.TextSecondary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = if (idx == 0) FDColors.Primary else FDColors.Primary.copy(alpha = 0.65f),
                                trackColor = FDColors.Border.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelHorariosPunta(
    ventasPorHora: Map<Int, Double>,
    ticketsPorHora: Map<Int, Int>,
    totalNeto: Double,
    modifier: Modifier = Modifier
) {
    val scrollBlocker = rememberScrollPadreBloqueado()
    val horasActivas = remember(ventasPorHora) {
        ventasPorHora.filter { it.value > 0.0 }.entries.sortedByDescending { it.value }
    }
    val horaPicoEntry = horasActivas.firstOrNull()
    val maxMontoHora = horaPicoEntry?.value ?: 1.0

    val horaPicoTag = if (horaPicoEntry != null) {
        val h = horaPicoEntry.key
        "🔥 %02d:00 - %02d:00 (%d ops)".format(Locale.US, h, (h + 1) % 24, ticketsPorHora[h] ?: 0)
    } else "Sin concentración"

    Surface(
        color = FDColors.Surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
        modifier = modifier
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
                    Icon(Icons.Default.Schedule, null, tint = FDColors.Success, modifier = Modifier.size(16.dp))
                    Text("Horas de Mayor Demanda", style = FDType.Heading2.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                }
                Text(
                    text = horaPicoTag,
                    style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.Success,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))

            if (horasActivas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin horas con actividad", style = FDType.Caption, color = FDColors.TextTertiary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(scrollBlocker)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    horasActivas.forEachIndexed { idx, entry ->
                        val h = entry.key
                        val monto = entry.value
                        val tickets = ticketsPorHora[h] ?: 0
                        val pct = if (totalNeto > 0.0) ((monto / totalNeto) * 100.0).coerceIn(0.0, 100.0) else 0.0
                        val ratioPico = (monto / maxMontoHora).coerceIn(0.0, 1.0).toFloat()
                        val franjaStr = "%02d:00 - %02d:00".format(Locale.US, h, (h + 1) % 24)

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        color = if (idx == 0) FDColors.Success.copy(alpha = 0.15f) else FDColors.Border.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            text = "#${idx + 1}",
                                            style = FDType.Caption.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                                            color = if (idx == 0) FDColors.Success else FDColors.TextSecondary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = franjaStr,
                                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                                        color = FDColors.TextPrimary
                                    )
                                }
                                Text(
                                    text = "S/ %.2f (%d ops · %.0f%%)".format(Locale.US, monto, tickets, pct),
                                    style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                    color = FDColors.TextSecondary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratioPico },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = if (idx == 0) FDColors.Success else FDColors.Success.copy(alpha = 0.65f),
                                trackColor = FDColors.Border.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelVentasPorVendedor(
    ventasPorVendedor: Map<String, Double>,
    ticketsPorVendedor: Map<String, Int>,
    totalNeto: Double,
    modifier: Modifier = Modifier
) {
    val scrollBlocker = rememberScrollPadreBloqueado()
    val vendedoresOrdenados = remember(ventasPorVendedor) {
        ventasPorVendedor.entries.sortedByDescending { it.value }
    }
    val maxVendedorMonto = remember(vendedoresOrdenados) {
        vendedoresOrdenados.firstOrNull()?.value ?: 1.0
    }

    Surface(
        color = FDColors.Surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
        modifier = modifier
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
                    Icon(Icons.Default.Person, null, tint = FDColors.Primary, modifier = Modifier.size(16.dp))
                    Text("Cajeros / Vendedores", style = FDType.Heading2.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                }
                Text(
                    text = "[ ${ventasPorVendedor.size} activos ]",
                    style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.Primary
                )
            }
            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))

            if (vendedoresOrdenados.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin datos de vendedores", style = FDType.Caption, color = FDColors.TextTertiary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(scrollBlocker)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    vendedoresOrdenados.forEachIndexed { idx, entry ->
                        val vendedor = entry.key
                        val monto = entry.value
                        val tickets = ticketsPorVendedor[vendedor] ?: 0
                        val pct = if (totalNeto > 0.0) ((monto / totalNeto) * 100.0).coerceIn(0.0, 100.0) else 0.0
                        val ratioMax = (monto / maxVendedorMonto).coerceIn(0.0, 1.0).toFloat()

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Surface(
                                        color = if (idx == 0) FDColors.Primary.copy(alpha = 0.15f) else FDColors.Border.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            text = "#${idx + 1}",
                                            style = FDType.Caption.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                                            color = if (idx == 0) FDColors.Primary else FDColors.TextSecondary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = vendedor,
                                        style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                        color = FDColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "S/ %.2f (%d ops · %.0f%%)".format(Locale.US, monto, tickets, pct),
                                    style = FDType.Caption.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                    color = FDColors.TextSecondary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratioMax },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = if (idx == 0) FDColors.Primary else FDColors.Primary.copy(alpha = 0.65f),
                                trackColor = FDColors.Border.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelControlEquipo(
    ventas: List<Venta>,
    devoluciones: List<DevolucionVenta>,
    periodo: String
) {
    val ventasVigentes = remember(ventas) { ventas.filter { it.estado != Venta.ESTADO_ANULADA } }

    val conDescuento = remember(ventasVigentes) {
        ventasVigentes.filter { it.descuento > 0.0 }.sortedByDescending { it.descuento }
    }
    val totalDescuentos = remember(conDescuento) {
        kotlin.math.round(conDescuento.sumOf { it.descuento } * 100.0) / 100.0
    }

    val conReceta = remember(ventasVigentes) {
        ventasVigentes.filter { v ->
            v.recetaVerificada || v.items.any { it.requiereReceta }
        }.sortedByDescending { it.fechaHoraMs }
    }

    val devsOrdenadas = remember(devoluciones) { devoluciones.sortedByDescending { it.fechaMs } }
    val totalReembolsos = remember(devsOrdenadas) {
        kotlin.math.round(devsOrdenadas.sumOf { it.montoReembolso } * 100.0) / 100.0
    }

    if (conDescuento.isEmpty() && conReceta.isEmpty() && devsOrdenadas.isEmpty()) return

    val fmtHora = remember {
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("America/Lima")
        }
    }

    Surface(
        color = FDColors.Surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "CONTROL DEL EQUIPO · QUIÉN HIZO QUÉ",
                    style = FDType.Heading2.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = "Descuentos, recetas y devoluciones del período con responsable ($periodo)",
                    style = FDType.Caption.copy(fontSize = 10.5.sp),
                    color = FDColors.TextSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Columna 1: Descuentos con responsable
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DESCUENTOS · S/ ${"%.2f".format(Locale.US, totalDescuentos)} (${conDescuento.size})",
                        style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextSecondary
                    )
                    if (conDescuento.isEmpty()) {
                        Text("Sin descuentos.", style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextTertiary)
                    } else {
                        conDescuento.take(5).forEach { v ->
                            val pct = if (v.subtotal > 0.0) (v.descuento / v.subtotal) * 100.0 else 0.0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        v.numeroCompleto.ifBlank { v.id },
                                        style = FDType.Body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                        color = FDColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        v.cajeroNombre.ifBlank { "Sin cajero" },
                                        style = FDType.Caption.copy(fontSize = 10.sp),
                                        color = FDColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    "-S/ ${"%.2f".format(Locale.US, v.descuento)} (${"%.0f".format(Locale.US, pct)}%)",
                                    style = FDType.Numeric.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                    color = FDColors.Warning
                                )
                            }
                        }
                        if (conDescuento.size > 5) {
                            Text("+${conDescuento.size - 5} más en el libro diario.", style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.TextTertiary)
                        }
                    }
                }

                // Columna 2: Recetas verificadas con responsable
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "RECETAS · ${conReceta.size} comprobante${if (conReceta.size == 1) "" else "s"}",
                        style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextSecondary
                    )
                    if (conReceta.isEmpty()) {
                        Text("Sin recetas en el período.", style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextTertiary)
                    } else {
                        conReceta.take(5).forEach { v ->
                            val itemsReceta = v.items.count { it.requiereReceta }
                            val verificador = v.recetaVerificadaPor.ifBlank { v.cajeroNombre.ifBlank { "Sin registro" } }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        v.numeroCompleto.ifBlank { v.id },
                                        style = FDType.Body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                        color = FDColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (v.recetaVerificada) "Verificó: $verificador" else "Sin rastro (venta antigua)",
                                        style = FDType.Caption.copy(fontSize = 10.sp),
                                        color = if (v.recetaVerificada) FDColors.Success else FDColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    "$itemsReceta ítem${if (itemsReceta == 1) "" else "s"}",
                                    style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                    color = FDColors.TextSecondary
                                )
                            }
                        }
                        if (conReceta.size > 5) {
                            Text("+${conReceta.size - 5} más en el libro diario.", style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.TextTertiary)
                        }
                    }
                }

                // Columna 3: Devoluciones con motivo y origen
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DEVOLUCIONES · S/ ${"%.2f".format(Locale.US, totalReembolsos)} (${devsOrdenadas.size})",
                        style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextSecondary
                    )
                    if (devsOrdenadas.isEmpty()) {
                        Text("Sin devoluciones.", style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextTertiary)
                    } else {
                        devsOrdenadas.take(5).forEach { d ->
                            val otroTurno = d.ventaCajeroId.isNotBlank() && d.usuarioId.isNotBlank() && d.ventaCajeroId != d.usuarioId
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        d.numeroCompleto.ifBlank { d.id },
                                        style = FDType.Body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                        color = FDColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "→ ${d.numeroVenta.ifBlank { "venta" }} · ${d.usuarioNombre.ifBlank { "Sin cajero" }}${if (otroTurno) " · OTRO TURNO (${d.ventaCajeroNombre.ifBlank { "origen" }})" else ""}",
                                        style = FDType.Caption.copy(fontSize = 10.sp),
                                        color = if (otroTurno) FDColors.Warning else FDColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (d.motivo.isNotBlank()) {
                                        Text(
                                            d.motivo,
                                            style = FDType.Caption.copy(fontSize = 10.sp),
                                            color = FDColors.TextTertiary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "-S/ ${"%.2f".format(Locale.US, d.montoReembolso)}",
                                    style = FDType.Numeric.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                    color = FDColors.Error
                                )
                            }
                        }
                        if (devsOrdenadas.size > 5) {
                            Text("+${devsOrdenadas.size - 5} más en el libro diario.", style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.TextTertiary)
                        }
                    }
                }
            }

            if (conReceta.any { !it.recetaVerificada }) {
                Text(
                    "“Sin rastro” = ventas con receta de antes de activar la huella. Desde hoy toda receta queda con quién y cuándo.",
                    style = FDType.Caption.copy(fontSize = 10.sp),
                    color = FDColors.TextTertiary
                )
            }
        }
    }
}

@Composable
private fun TabFiltroVenta(
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
