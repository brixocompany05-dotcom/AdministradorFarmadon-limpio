package com.app.administradorfarmadon.analitica_reportes.logica

import com.app.administradorfarmadon.analitica_reportes.modelo.AnaliticaUiState
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ReporteColumna(
    val titulo: String,
    val peso: Float = 1f,
    val esMonto: Boolean = false
)

data class ReporteFila(
    val celdas: List<String>
)

data class ReporteTabla(
    val categoria: String,
    val tipoReporte: String,
    val periodo: String,
    val nombreSede: String,
    val razonSocial: String,
    val ruc: String,
    val columnas: List<ReporteColumna>,
    val filas: List<ReporteFila>,
    val totales: Map<String, String> = emptyMap(),
    val totalRegistros: Int = filas.size,
    val fechaGeneracionLima: String = "",
    val disclaimerTributario: String? = null
)

/**
 * Constructor determinista y puro de datasets tabulares para los 14 reportes oficiales (R1/R3/R12).
 * Sin datos ficticios, sin quemados, alimentado 100% de la verdad operativa de AnaliticaUiState.Exito.
 */
object ReportesDatasetBuilder {

    private val TIMEZONE_LIMA: TimeZone = TimeZone.getTimeZone("America/Lima")

    private fun formatearFechaLima(ms: Long): String {
        if (ms <= 0L) return "—"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
        sdf.timeZone = TIMEZONE_LIMA
        return sdf.format(Date(ms))
    }

    private fun formatearDiaLima(ms: Long): String {
        if (ms <= 0L) return "—"
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        sdf.timeZone = TIMEZONE_LIMA
        return sdf.format(Date(ms))
    }

    fun construirReporte(
        categoria: String,
        tipoReporte: String,
        exito: AnaliticaUiState.Exito,
        periodo: String,
        nombreSede: String,
        razonSocial: String,
        ruc: String
    ): ReporteTabla {
        val sdfAhora = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)
        sdfAhora.timeZone = TIMEZONE_LIMA
        val ahoraLima = sdfAhora.format(Date())

        val resultado = when {
            // ── CATEGORÍA: VENTAS ──────────────────────────────────────────────
            tipoReporte.contains("Ventas Detalladas", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("FECHA / HORA", 1.4f),
                    ReporteColumna("COMPROBANTE", 1.3f),
                    ReporteColumna("PRODUCTO", 2.8f),
                    ReporteColumna("CANTIDAD", 0.9f),
                    ReporteColumna("PRECIO UNIT", 1.1f, esMonto = true),
                    ReporteColumna("TOTAL NETO", 1.2f, esMonto = true)
                )
                val filas = mutableListOf<ReporteFila>()
                var sumCant = 0.0
                var sumTotal = 0.0

                val ventasValidas = exito.listaVentas.filter { it.estado != Venta.ESTADO_ANULADA }
                for (v in ventasValidas) {
                    val comp = v.numeroCompleto.ifBlank { "${v.tipoComprobante} #${v.id.takeLast(6)}" }
                    val fStr = formatearFechaLima(v.fechaHoraMs)
                    // Prorrateo del descuento global de la venta para que el total coincida con Ventas Brutas (sum total).
                    val factorDesc = if (v.subtotal > 0.0) (v.total / v.subtotal).coerceIn(0.0, 1.0) else 1.0
                    for (item in v.items) {
                        val cant = item.cantidad.toDouble()
                        val tot = kotlin.math.round(item.subtotal * factorDesc * 100.0) / 100.0
                        val unit = if (cant > 0) kotlin.math.round((tot / cant) * 100.0) / 100.0 else item.precioUnitario
                        sumCant += cant
                        sumTotal += tot

                        filas.add(
                            ReporteFila(
                                listOf(
                                    fStr,
                                    comp,
                                    item.nombreProducto.ifBlank { "Producto sin nombre" },
                                    String.format(Locale.US, "%.0f", cant),
                                    String.format(Locale.US, "S/ %.2f", unit),
                                    String.format(Locale.US, "S/ %.2f", tot)
                                )
                            )
                        )
                    }
                }
                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Ítems Facturados" to String.format(Locale.US, "%.0f unids", sumCant),
                        "Total Vendido" to String.format(Locale.US, "S/ %.2f", sumTotal)
                    ),
                    fechaGeneracionLima = ahoraLima,
                    disclaimerTributario = "Detalle bruto por producto (con descuento prorrateado, antes de devoluciones). El neto del período descuenta Notas de Crédito (ver “Reporte de Anulaciones y Devoluciones”)."
                )
            }

            tipoReporte.contains("Resumen de Ventas por Día", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("DÍA (LIMA)", 1.5f),
                    ReporteColumna("COMPROBANTES", 1.2f),
                    ReporteColumna("VENTAS BRUTAS", 1.5f, esMonto = true),
                    ReporteColumna("DEVOLUCIONES", 1.5f, esMonto = true),
                    ReporteColumna("VENTAS NETAS", 1.5f, esMonto = true)
                )
                val ventasPorDia = exito.listaVentas.filter { it.estado != Venta.ESTADO_ANULADA }.groupBy { it.diaClave }
                val devsPorDia = exito.listaDevoluciones.groupBy { it.diaClave }
                val todosDias = (ventasPorDia.keys + devsPorDia.keys).filter { it.isNotBlank() }.sortedDescending()

                val filas = mutableListOf<ReporteFila>()
                var sumBrutas = 0.0
                var sumDevs = 0.0
                var sumNetas = 0.0
                var sumTickets = 0

                for (dia in todosDias) {
                    val vs = ventasPorDia[dia] ?: emptyList()
                    val ds = devsPorDia[dia] ?: emptyList()
                    val brutas = vs.sumOf { it.total }
                    val dev = ds.sumOf { it.montoReembolso }
                    val net = brutas - dev
                    sumBrutas += brutas
                    sumDevs += dev
                    sumNetas += net
                    sumTickets += vs.size

                    filas.add(
                        ReporteFila(
                            listOf(
                                dia,
                                vs.size.toString(),
                                String.format(Locale.US, "S/ %.2f", brutas),
                                String.format(Locale.US, "S/ %.2f", dev),
                                String.format(Locale.US, "S/ %.2f", net)
                            )
                        )
                    )
                }

                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Total Tickets" to sumTickets.toString(),
                        "Total Bruto" to String.format(Locale.US, "S/ %.2f", sumBrutas),
                        "Total Devoluciones" to String.format(Locale.US, "S/ %.2f", sumDevs),
                        "Total Neto" to String.format(Locale.US, "S/ %.2f", sumNetas)
                    ),
                    fechaGeneracionLima = ahoraLima
                )
            }

            tipoReporte.contains("Ventas por Vendedor", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("VENDEDOR / CAJERO", 2.2f),
                    ReporteColumna("COMPROBANTES", 1.2f),
                    ReporteColumna("TICKET PROMEDIO NETO", 1.5f, esMonto = true),
                    ReporteColumna("VENTAS NETAS", 1.6f, esMonto = true)
                )
                // Neto por vendedor con la misma verdad central: solo transacciones efectivas (excluye ANULADA y DEV_TOTAL).
                val devsPorVentaId = exito.listaDevoluciones.groupBy { it.ventaId }
                val porVendedor = exito.listaVentas.filter { it.estado != Venta.ESTADO_ANULADA && it.estado != Venta.ESTADO_DEVOLUCION_TOTAL }
                    .groupBy { it.cajeroNombre.trim().ifBlank { "SIN ESPECIFICAR" } }

                val filas = mutableListOf<ReporteFila>()
                var sumTotal = 0.0
                var sumTickets = 0

                for ((vendedor, vs) in porVendedor.entries.sortedByDescending { e -> e.value.sumOf { v -> v.total } - e.value.sumOf { v -> devsPorVentaId[v.id]?.sumOf { it.montoReembolso } ?: 0.0 } }) {
                    val bruto = vs.sumOf { it.total }
                    val dev = vs.sumOf { v -> devsPorVentaId[v.id]?.sumOf { it.montoReembolso } ?: 0.0 }
                    val tot = kotlin.math.round((bruto - dev) * 100.0) / 100.0
                    val tkts = vs.size
                    val prom = if (tkts > 0 && tot > 0.0) kotlin.math.round((tot / tkts) * 100.0) / 100.0 else 0.0
                    sumTotal += tot
                    sumTickets += tkts

                    filas.add(
                        ReporteFila(
                            listOf(
                                vendedor,
                                tkts.toString(),
                                String.format(Locale.US, "S/ %.2f", prom),
                                String.format(Locale.US, "S/ %.2f", tot)
                            )
                        )
                    )
                }

                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Total Tickets" to sumTickets.toString(),
                        "Total Neto" to String.format(Locale.US, "S/ %.2f", sumTotal)
                    ),
                    fechaGeneracionLima = ahoraLima
                )
            }

            tipoReporte.contains("Anulaciones y Devoluciones", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("FECHA / HORA", 1.4f),
                    ReporteColumna("TIPO DE ACCIÓN", 1.3f),
                    ReporteColumna("DOC ORIGEN / REF", 1.5f),
                    ReporteColumna("MOTIVO / DETALLE", 2.2f),
                    ReporteColumna("MONTO AFECTADO", 1.3f, esMonto = true)
                )
                val filas = mutableListOf<ReporteFila>()
                var sumMonto = 0.0

                for (d in exito.listaDevoluciones) {
                    sumMonto += d.montoReembolso
                    filas.add(
                        ReporteFila(
                            listOf(
                                formatearFechaLima(d.fechaMs),
                                "Devolución (" + d.metodoReembolso + ")",
                                d.numeroVenta.ifBlank { d.ventaId.takeLast(10) },
                                d.motivo.ifBlank { "Reembolso en caja" },
                                String.format(Locale.US, "S/ %.2f", d.montoReembolso)
                            )
                        )
                    )
                }

                val anuladas = exito.listaVentas.filter { it.estado == Venta.ESTADO_ANULADA }
                for (a in anuladas) {
                    sumMonto += a.total
                    filas.add(
                        ReporteFila(
                            listOf(
                                formatearFechaLima(a.fechaHoraMs),
                                "Venta Anulada",
                                a.numeroCompleto.ifBlank { a.id.takeLast(10) },
                                a.anulacionMotivo.ifBlank { "Comprobante anulado" },
                                String.format(Locale.US, "S/ %.2f", a.total)
                            )
                        )
                    )
                }

                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Total Operaciones" to (exito.listaDevoluciones.size + anuladas.size).toString(),
                        "Monto Devuelto / Anulado" to String.format(Locale.US, "S/ %.2f", sumMonto)
                    ),
                    fechaGeneracionLima = ahoraLima
                )
            }

            // ── CATEGORÍA: INVENTARIO ──────────────────────────────────────────
            tipoReporte.contains("Stock Actual Valorizado", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("MÉTRICA INVENTARIO", 3.0f),
                    ReporteColumna("CANTIDAD", 1.2f),
                    ReporteColumna("VALOR ESTIMADO", 1.8f, esMonto = true)
                )
                val filas = mutableListOf<ReporteFila>()
                filas.add(ReporteFila(listOf("Unidades Físicas Disponibles", String.format(Locale.US, "%.0f", exito.inventario.totalUnidadesDisponibles), String.format(Locale.US, "S/ %.2f", exito.inventario.valorInventarioTotal))))
                filas.add(ReporteFila(listOf("Productos con Quiebre de Stock (0 unid)", exito.inventario.cantidadQuiebresStock.toString(), "—")))
                filas.add(ReporteFila(listOf("Lotes Próximos a Vencer (30d)", exito.inventario.cantidadPorVencer30Dias.toString(), "—")))
                filas.add(ReporteFila(listOf("Mercadería Inmovilizada (90d)", exito.inventario.productosInmovilizados.size.toString(), String.format(Locale.US, "S/ %.2f", exito.inventario.valorInmovilizado90Dias))))

                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Valorización Total" to String.format(Locale.US, "S/ %.2f", exito.inventario.valorInventarioTotal)
                    ),
                    fechaGeneracionLima = ahoraLima
                )
            }

            tipoReporte.contains("Kardex Físico y Valorizado", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("PRODUCTO", 2.6f),
                    ReporteColumna("VENTAS NETAS", 1.4f, esMonto = true),
                    ReporteColumna("COSTO NETO", 1.4f, esMonto = true),
                    ReporteColumna("UTILIDAD", 1.4f, esMonto = true),
                    ReporteColumna("MARGEN", 1.0f)
                )
                val filas = mutableListOf<ReporteFila>()
                val todosProds = (exito.rentabilidad.productosMayorMargen + exito.rentabilidad.productosMenorMargen).distinctBy { it.nombre }

                var sumVentas = 0.0
                var sumCosto = 0.0
                var sumUtil = 0.0

                for (p in todosProds) {
                    sumVentas += p.ventasNetas
                    sumCosto += p.costoNeto
                    sumUtil += p.utilidad
                    filas.add(
                        ReporteFila(
                            listOf(
                                p.nombre,
                                String.format(Locale.US, "S/ %.2f", p.ventasNetas),
                                String.format(Locale.US, "S/ %.2f", p.costoNeto),
                                String.format(Locale.US, "S/ %.2f", p.utilidad),
                                String.format(Locale.US, "%.1f%%", p.margenPorcentaje)
                            )
                        )
                    )
                }

                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Ventas Netas Totales" to String.format(Locale.US, "S/ %.2f", exito.metricas.ventasNetas),
                        "Costo Neto Total" to String.format(Locale.US, "S/ %.2f", exito.metricas.costoNeto),
                        "Utilidad Bruta Total" to String.format(Locale.US, "S/ %.2f", exito.metricas.utilidadBruta)
                    ),
                    fechaGeneracionLima = ahoraLima
                )
            }

            tipoReporte.contains("Lotes y Próximos a Vencer", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("MÉTRICA DE CONTROL", 3.0f),
                    ReporteColumna("CANTIDAD", 1.2f),
                    ReporteColumna("ESTADO OPERATIVO", 2.0f)
                )
                val filas = listOf(
                    ReporteFila(listOf("Lotes por vencer (próximos 30 días)", exito.inventario.cantidadPorVencer30Dias.toString(), if (exito.inventario.cantidadPorVencer30Dias > 0) "Atención requerida" else "Control óptimo")),
                    ReporteFila(listOf("Productos con quiebre de stock (0 unid)", exito.inventario.cantidadQuiebresStock.toString(), if (exito.inventario.cantidadQuiebresStock > 0) "Requiere reposición" else "Stock abastecido")),
                    ReporteFila(listOf("Mercadería inmovilizada en período", exito.inventario.productosInmovilizados.size.toString(), "Revisar rotación"))
                )
                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Lotes en Riesgo" to exito.inventario.cantidadPorVencer30Dias.toString(),
                        "Quiebres" to exito.inventario.cantidadQuiebresStock.toString()
                    ),
                    fechaGeneracionLima = ahoraLima
                )
            }

            tipoReporte.contains("Sin Movimiento", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("PRODUCTO SIN ROTACIÓN", 3.5f),
                    ReporteColumna("ESTADO EN EL PERÍODO", 2.5f)
                )
                val filas = exito.inventario.productosInmovilizados.map { prod ->
                    ReporteFila(listOf(prod, "Cero ventas en el período evaluado"))
                }
                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf("Total Productos Inmovilizados" to filas.size.toString()),
                    fechaGeneracionLima = ahoraLima
                )
            }

            // ── CATEGORÍA: CAJA ────────────────────────────────────────────────
            tipoReporte.contains("Cierres de Caja", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("FECHA APERTURA", 1.4f),
                    ReporteColumna("FECHA CIERRE", 1.4f),
                    ReporteColumna("CAJERO RESPONSABLE", 1.8f),
                    ReporteColumna("FONDO", 1.1f, esMonto = true),
                    ReporteColumna("VENTAS NETAS", 1.2f, esMonto = true),
                    ReporteColumna("FÍSICO EN CAJA", 1.2f, esMonto = true),
                    ReporteColumna("DIFERENCIA", 1.1f, esMonto = true),
                    ReporteColumna("ESTADO", 1.0f)
                )
                val filas = mutableListOf<ReporteFila>()
                val sesiones = exito.historialSesionesCaja
                var sumVentas = 0.0
                var sumFisico = 0.0

                if (sesiones.isNotEmpty()) {
                    for (s in sesiones) {
                        val dif = s.diferenciaEfectivo
                        val difStr = when {
                            dif > 0.009 -> "+S/ %.2f".format(Locale.US, dif)
                            dif < -0.009 -> "-S/ %.2f".format(Locale.US, kotlin.math.abs(dif))
                            else -> "S/ 0.00"
                        }
                        sumVentas += s.totalVentas
                        sumFisico += s.efectivoContado

                        filas.add(
                            ReporteFila(
                                listOf(
                                    formatearFechaLima(s.aperturaMs),
                                    if (s.cierreMs > 0) formatearFechaLima(s.cierreMs) else "En Curso",
                                    s.abiertoPorNombre.ifBlank { "—" },
                                    String.format(Locale.US, "S/ %.2f", s.fondoInicial),
                                    String.format(Locale.US, "S/ %.2f", s.totalVentas),
                                    String.format(Locale.US, "S/ %.2f", s.efectivoContado),
                                    difStr,
                                    s.estado
                                )
                            )
                        )
                    }
                } else {
                    val c = exito.estadoCaja
                    filas.add(
                        ReporteFila(
                            listOf(
                                if (c.aperturaMs > 0) formatearFechaLima(c.aperturaMs) else "—",
                                "Turno Actual",
                                c.abiertoPorNombre.ifBlank { "—" },
                                String.format(Locale.US, "S/ %.2f", c.fondoInicial),
                                String.format(Locale.US, "S/ %.2f", c.ventasEfectivo),
                                String.format(Locale.US, "S/ %.2f", c.efectivoEsperado),
                                "S/ 0.00",
                                c.estado
                            )
                        )
                    )
                }

                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Total Turnos" to filas.size.toString(),
                        "Total Ventas Turnos" to String.format(Locale.US, "S/ %.2f", sumVentas),
                        "Total Físico Arqueado" to String.format(Locale.US, "S/ %.2f", sumFisico)
                    ),
                    fechaGeneracionLima = ahoraLima
                )
            }

            tipoReporte.contains("Movimientos de Ingresos y Egresos", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("CONCEPTO / FLUJO DE GAVETA", 2.8f),
                    ReporteColumna("TIPO", 1.2f),
                    ReporteColumna("INGRESOS (+)", 1.4f, esMonto = true),
                    ReporteColumna("EGRESOS (−)", 1.4f, esMonto = true),
                    ReporteColumna("SALDO / TOTAL", 1.4f, esMonto = true)
                )
                val filas = mutableListOf<ReporteFila>()
                val d = exito.dineroYCaja

                filas.add(
                    ReporteFila(
                        listOf(
                            "Fondo Inicial (Aperturas de Turno)",
                            "Apertura",
                            String.format(Locale.US, "S/ %.2f", d.aperturasTotal),
                            "—",
                            String.format(Locale.US, "S/ %.2f", d.aperturasTotal)
                        )
                    )
                )

                val cobrosEfectivo = exito.metricas.ventasPorMetodo["EFECTIVO"] ?: 0.0
                filas.add(
                    ReporteFila(
                        listOf(
                            "Cobros de Ventas en Efectivo",
                            "Venta POS",
                            String.format(Locale.US, "S/ %.2f", cobrosEfectivo),
                            "—",
                            String.format(Locale.US, "S/ %.2f", cobrosEfectivo)
                        )
                    )
                )

                if (d.otrosIngresos > 0.0) {
                    filas.add(
                        ReporteFila(
                            listOf(
                                "Otros Ingresos Manuales a Caja",
                                "Ingreso",
                                String.format(Locale.US, "S/ %.2f", d.otrosIngresos),
                                "—",
                                String.format(Locale.US, "S/ %.2f", d.otrosIngresos)
                            )
                        )
                    )
                }

                if (d.egresosTotales > 0.0) {
                    filas.add(
                        ReporteFila(
                            listOf(
                                "Retiros y Gastos Operativos de Gaveta",
                                "Egreso / Retiro",
                                "—",
                                String.format(Locale.US, "S/ %.2f", d.egresosTotales),
                                String.format(Locale.US, "-S/ %.2f", d.egresosTotales)
                            )
                        )
                    )
                }

                filas.add(
                    ReporteFila(
                        listOf(
                            "Efectivo Total Esperado en Gaveta",
                            "Cierre Teórico",
                            "—",
                            "—",
                            String.format(Locale.US, "S/ %.2f", d.cajaEsperadaTotal)
                        )
                    )
                )

                if (d.cantidadTurnosCerrados > 0) {
                    filas.add(
                        ReporteFila(
                            listOf(
                                "Efectivo Contado Físicamente en Arqueos",
                                "Conteo Físico",
                                "—",
                                "—",
                                String.format(Locale.US, "S/ %.2f", d.cajaContadaTotal)
                            )
                        )
                    )

                    val dif = d.diferenciaCajaTotal
                    val difStr = when {
                        dif > 0.009 -> "+S/ %.2f".format(Locale.US, dif)
                        dif < -0.009 -> "-S/ %.2f".format(Locale.US, kotlin.math.abs(dif))
                        else -> "S/ 0.00"
                    }
                    filas.add(
                        ReporteFila(
                            listOf(
                                "Diferencia Acumulada de Arqueo",
                                if (dif < -0.009) "Faltante 🔴" else if (dif > 0.009) "Sobrante 🟡" else "Cuadrado 🟢",
                                "—",
                                "—",
                                difStr
                            )
                        )
                    )
                }

                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Fondos Apertura" to String.format(Locale.US, "S/ %.2f", d.aperturasTotal),
                        "Otros Ingresos" to String.format(Locale.US, "S/ %.2f", d.otrosIngresos),
                        "Egresos Gaveta" to String.format(Locale.US, "S/ %.2f", d.egresosTotales),
                        "Efectivo Esperado" to String.format(Locale.US, "S/ %.2f", d.cajaEsperadaTotal),
                        "Diferencia Arqueo" to String.format(Locale.US, "S/ %.2f", d.diferenciaCajaTotal)
                    ),
                    fechaGeneracionLima = ahoraLima,
                    disclaimerTributario = "Flujo de dinero físico en gaveta según aperturas, cobros en efectivo, ingresos y egresos registrados en el sistema."
                )
            }

            tipoReporte.contains("Desglose por Métodos de Pago", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("MÉTODO DE PAGO", 2.2f),
                    ReporteColumna("COBROS POS", 1.6f, esMonto = true),
                    ReporteColumna("MOVIMIENTOS CAJA", 1.6f, esMonto = true),
                    ReporteColumna("DIFERENCIA", 1.6f, esMonto = true),
                    ReporteColumna("ESTADO CONTRATO", 1.5f)
                )
                val filas = mutableListOf<ReporteFila>()
                val todosMetodos = (exito.metricas.ventasPorMetodo.keys + exito.conciliacionCaja.sumCajaMovimientos.keys).distinct()
                val activosIds = exito.metodosConfigurados.filter { it.activa }.map { it.tipoId }.toSet()

                for (m in todosMetodos.sorted()) {
                    val pos = exito.metricas.ventasPorMetodo[m] ?: 0.0
                    val caja = exito.conciliacionCaja.sumCajaMovimientos[m] ?: 0.0
                    val dif = pos - caja
                    val estado = when {
                        m in activosIds -> "Activo en Contrato"
                        m == "SIN_ESPECIFICAR" || m == "SIN_METODO" -> "⚠️ Sin Método"
                        else -> "Histórico / Inactivo"
                    }
                    filas.add(
                        ReporteFila(
                            listOf(
                                m,
                                String.format(Locale.US, "S/ %.2f", pos),
                                String.format(Locale.US, "S/ %.2f", caja),
                                String.format(Locale.US, "S/ %.2f", dif),
                                estado
                            )
                        )
                    )
                }
                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Total POS" to String.format(Locale.US, "S/ %.2f", exito.metricas.ventasNetas),
                        "Diferencia" to String.format(Locale.US, "S/ %.2f", exito.conciliacionCaja.diferenciaTotal)
                    ),
                    fechaGeneracionLima = ahoraLima
                )
            }

            // ── CATEGORÍA: TRIBUTARIOS / FISCAL ────────────────────────────────
            tipoReporte.contains("Registro de Ventas", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("FECHA", 1.1f),
                    ReporteColumna("COMPROBANTE", 1.4f),
                    ReporteColumna("CLIENTE", 2.2f),
                    ReporteColumna("BASE IMP.", 1.2f, esMonto = true),
                    ReporteColumna("IGV (18%)", 1.1f, esMonto = true),
                    ReporteColumna("TOTAL S/", 1.2f, esMonto = true)
                )
                val filas = mutableListOf<ReporteFila>()
                var sumBase = 0.0
                var sumIgv = 0.0
                var sumTot = 0.0

                val validas = exito.listaVentas.filter { it.estado != Venta.ESTADO_ANULADA }
                for (v in validas) {
                    val tot = v.total
                    val desglose = com.app.administradorfarmadon.facturacion.envio.datos.FacturacionPayloadBuilder.calcularDesgloseIgv(tot)
                    val base = desglose.baseGravada
                    val igv = desglose.igv
                    sumBase += base
                    sumIgv += igv
                    sumTot += tot

                    val comp = v.numeroCompleto.ifBlank { "${v.tipoComprobante} #${v.id.takeLast(6)}" }
                    val cli = if (v.cliente.nombre.isNotBlank() && !v.cliente.esConsumidorFinal) {
                        "${v.cliente.nombre} (${v.cliente.numeroDocumento})"
                    } else {
                        "Consumidor Final"
                    }

                    filas.add(
                        ReporteFila(
                            listOf(
                                formatearDiaLima(v.fechaHoraMs),
                                comp,
                                cli,
                                String.format(Locale.US, "S/ %.2f", base),
                                String.format(Locale.US, "S/ %.2f", igv),
                                String.format(Locale.US, "S/ %.2f", tot)
                            )
                        )
                    )
                }

                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Base Imponible" to String.format(Locale.US, "S/ %.2f", sumBase),
                        "IGV Total (18%)" to String.format(Locale.US, "S/ %.2f", sumIgv),
                        "Total Facturado" to String.format(Locale.US, "S/ %.2f", sumTot)
                    ),
                    fechaGeneracionLima = ahoraLima,
                    disclaimerTributario = "Registro de ventas brutas del período (no descuenta Notas de Crédito). Para el neto real ver “Resumen de Ventas por Día” y conciliación SUNAT en “Resumen de Comprobantes Electrónicos”."
                )
            }

            tipoReporte.contains("Resumen de Comprobantes", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("ESTADO SUNAT (CDR)", 3.0f),
                    ReporteColumna("CANTIDAD COMPROBANTES", 2.0f)
                )
                val f = exito.conciliacionFiscal
                val filas = listOf(
                    ReporteFila(listOf("Comprobantes Aceptados por SUNAT", f.aceptados.toString())),
                    ReporteFila(listOf("Comprobantes Pendientes de Envío", f.pendientes.toString())),
                    ReporteFila(listOf("Comprobantes En Trámite", f.enviados.toString())),
                    ReporteFila(listOf("Comprobantes Rechazados", f.rechazados.toString())),
                    ReporteFila(listOf("Comprobantes Anulados", f.anulados.toString())),
                    ReporteFila(listOf("Total Documentos Fiscales Registrados", f.totalDocumentosFiscales.toString())),
                    ReporteFila(listOf("Total Ventas Efectivas en POS", f.totalVentasNoAnuladas.toString()))
                )
                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Aceptados SUNAT" to f.aceptados.toString(),
                        "Diferencia POS vs Fiscal" to f.diferenciaEmitidosVsFiscal.toString()
                    ),
                    fechaGeneracionLima = ahoraLima
                )
            }

            tipoReporte.contains("Compras a Proveedores", ignoreCase = true) || tipoReporte.contains("Libro de Medicamentos", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("DROGUERÍA / DISTRIBUIDOR", 2.2f),
                    ReporteColumna("TOTAL FACTURADO", 1.5f, esMonto = true),
                    ReporteColumna("BASE DERIVADA (EST.)", 1.5f, esMonto = true),
                    ReporteColumna("IGV DERIVADO (EST.)", 1.4f, esMonto = true),
                    ReporteColumna("ORIGEN IGV", 1.4f)
                )
                val filas = mutableListOf<ReporteFila>()
                val totCompras = exito.compras.comprasNetas
                var sumBase = 0.0
                var sumIgv = 0.0

                for ((prov, monto) in exito.compras.comprasPorProveedor.entries.sortedByDescending { it.value }) {
                    val desglose = com.app.administradorfarmadon.facturacion.envio.datos.FacturacionPayloadBuilder.calcularDesgloseIgv(monto)
                    val baseEstimada = desglose.baseGravada
                    val igvEstimado = desglose.igv
                    sumBase += baseEstimada
                    sumIgv += igvEstimado
                    filas.add(
                        ReporteFila(
                            listOf(
                                prov,
                                String.format(Locale.US, "S/ %.2f", monto),
                                String.format(Locale.US, "S/ %.2f", baseEstimada),
                                String.format(Locale.US, "S/ %.2f", igvEstimado),
                                "DERIVADO (ESTIMADO)"
                            )
                        )
                    )
                }

                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = "Compras a Proveedores y Droguerías",
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Total Facturas Compra" to exito.compras.cantidadFacturasCompra.toString(),
                        "Compras Netas" to String.format(Locale.US, "S/ %.2f", totCompras),
                        "Base Derivada (Est.)" to String.format(Locale.US, "S/ %.2f", sumBase),
                        "IGV Derivado (Est.)" to String.format(Locale.US, "S/ %.2f", sumIgv)
                    ),
                    fechaGeneracionLima = ahoraLima,
                    disclaimerTributario = "Aviso tributario: Las compras registradas muestran base e IGV estimados derivados del monto total papel. No sustituye comprobante ni propuesta SIRE/RCE de SUNAT. El contador valida el crédito fiscal en su software contable (CONCAR/SIRE/PLE)."
                )
            }

            // ── CATEGORÍA: RENTABILIDAD & MÁRGENES ──────────────────────────────
            tipoReporte.contains("Rentabilidad", ignoreCase = true) || tipoReporte.contains("Margen", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("PRODUCTO Y PRESENTACIÓN", 2.6f),
                    ReporteColumna("UNIDADES", 1.0f),
                    ReporteColumna("VENTAS NETAS", 1.3f, esMonto = true),
                    ReporteColumna("COSTO VENTAS", 1.3f, esMonto = true),
                    ReporteColumna("UTILIDAD BRUTA", 1.3f, esMonto = true),
                    ReporteColumna("MARGEN %", 0.9f)
                )
                val filas = mutableListOf<ReporteFila>()
                val todos = if (exito.rentabilidad.todosLosProductos.isNotEmpty()) {
                    exito.rentabilidad.todosLosProductos
                } else {
                    (exito.rentabilidad.productosMayorMargen + exito.rentabilidad.productosMenorMargen).distinctBy { it.productoId }
                }

                for (p in todos) {
                    val unidStr = if (p.unidadesDevueltas > 0) "${p.unidadesEfectivas} (${p.unidadesVendidas} vend - ${p.unidadesDevueltas} dev)" else "${p.unidadesVendidas}"
                    filas.add(
                        ReporteFila(
                            listOf(
                                p.nombre,
                                unidStr,
                                String.format(Locale.US, "S/ %.2f", p.ventasNetas),
                                String.format(Locale.US, "S/ %.2f", p.costoNeto),
                                String.format(Locale.US, "S/ %.2f", p.utilidad),
                                String.format(Locale.US, "%.1f%%", p.margenPorcentaje)
                            )
                        )
                    )
                }

                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Ventas Netas Totales" to String.format(Locale.US, "S/ %.2f", exito.metricas.ventasNetas),
                        "Costo Ventas Total" to String.format(Locale.US, "S/ %.2f", exito.metricas.costoNeto),
                        "Utilidad Bruta Total" to String.format(Locale.US, "S/ %.2f", exito.metricas.utilidadBruta),
                        "Margen General" to String.format(Locale.US, "%.1f%%", exito.rentabilidad.margenGeneral)
                    ),
                    fechaGeneracionLima = ahoraLima
                )
            }

            // ── CATEGORÍA: CLIENTES & FIDELIZACIÓN ─────────────────────────────
            tipoReporte.contains("Ranking de Mejores Clientes", ignoreCase = true) || tipoReporte.contains("Mejores Clientes", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("CLIENTE / RAZÓN SOCIAL", 2.6f),
                    ReporteColumna("DNI / RUC", 1.4f),
                    ReporteColumna("COMPRAS", 1.0f),
                    ReporteColumna("TOTAL CONSUMIDO", 1.5f, esMonto = true),
                    ReporteColumna("ÚLTIMA COMPRA", 1.5f)
                )
                val filas = mutableListOf<ReporteFila>()
                var sumTotal = 0.0
                var sumCompras = 0

                for (c in exito.clientes.rankingClientes) {
                    sumTotal += c.totalCompradoNeto
                    sumCompras += c.cantidadCompras
                    filas.add(
                        ReporteFila(
                            listOf(
                                c.nombre,
                                c.documento.ifBlank { "—" },
                                c.cantidadCompras.toString(),
                                String.format(Locale.US, "S/ %.2f", c.totalCompradoNeto),
                                formatearFechaLima(c.ultimaCompraMs)
                            )
                        )
                    )
                }

                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Clientes Evaluados" to filas.size.toString(),
                        "Total Transacciones" to sumCompras.toString(),
                        "Consumo Acumulado" to String.format(Locale.US, "S/ %.2f", sumTotal)
                    ),
                    fechaGeneracionLima = ahoraLima
                )
            }

            tipoReporte.contains("Comportamiento de Clientes", ignoreCase = true) || tipoReporte.contains("Recurrencia", ignoreCase = true) -> {
                val cols = listOf(
                    ReporteColumna("MÉTRICA DE CLIENTES", 3.0f),
                    ReporteColumna("VALOR REGISTRADO", 2.0f)
                )
                val cl = exito.clientes
                val filas = listOf(
                    ReporteFila(listOf("Total Clientes Atendidos en Período", cl.totalClientesAtendidos.toString())),
                    ReporteFila(listOf("Clientes Identificados con DNI / RUC", cl.clientesIdentificados.toString())),
                    ReporteFila(listOf("Consumidor Final (Ventas Anónimas)", cl.clientesAnonimos.toString())),
                    ReporteFila(listOf("Clientes Nuevos (Primera Compra)", cl.clientesNuevos.toString())),
                    ReporteFila(listOf("Clientes Recurrentes (Fidelizados)", cl.clientesRecurrentes.toString())),
                    ReporteFila(listOf("Ticket Promedio por Cliente", String.format(Locale.US, "S/ %.2f", cl.ticketPromedioCliente)))
                )
                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    totales = mapOf(
                        "Total Atendidos" to cl.totalClientesAtendidos.toString(),
                        "Ticket Promedio" to String.format(Locale.US, "S/ %.2f", cl.ticketPromedioCliente)
                    ),
                    fechaGeneracionLima = ahoraLima
                )
            }

            else -> {
                val cols = listOf(
                    ReporteColumna("FECHA", 1.5f),
                    ReporteColumna("DESCRIPCIÓN", 3.5f),
                    ReporteColumna("TOTAL S/", 1.5f, esMonto = true)
                )
                val filas = exito.listaVentas.map {
                    ReporteFila(listOf(formatearFechaLima(it.fechaHoraMs), "Venta #" + it.id.takeLast(6), String.format(Locale.US, "S/ %.2f", it.total)))
                }
                ReporteTabla(
                    categoria = categoria,
                    tipoReporte = tipoReporte,
                    periodo = periodo,
                    nombreSede = nombreSede,
                    razonSocial = razonSocial,
                    ruc = ruc,
                    columnas = cols,
                    filas = filas,
                    fechaGeneracionLima = ahoraLima
                )
            }
        }
        return resultado
    }
}

