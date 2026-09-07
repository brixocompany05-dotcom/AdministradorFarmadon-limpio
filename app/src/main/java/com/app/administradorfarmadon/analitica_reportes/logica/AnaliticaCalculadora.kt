package com.app.administradorfarmadon.analitica_reportes.logica


import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.facturacion.envio.datos.FacturacionPayloadBuilder
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.stockDisponibleFisico
import com.app.administradorfarmadon.inventario.compartido.modelo.stockFisicoTotalUnidades
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.MovimientoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.analitica_reportes.modelo.ConciliacionCaja
import com.app.administradorfarmadon.analitica_reportes.modelo.ConciliacionFiscal
import com.app.administradorfarmadon.analitica_reportes.modelo.MetricasVentas
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Calculadora pura y determinista para Analítica (R4 - Arquitectura Mínima y Testeable).
 *
 * Contrato: "Analítica debe poder explicar de dónde salió cada importe y reproducir
 * el mismo resultado aunque los eventos se repitan, la pantalla se reconstruya,
 * el usuario reintente la operación o los costos actuales cambien."
 */
object AnaliticaCalculadora {

    val TIMEZONE_LIMA: TimeZone = TimeZone.getTimeZone("America/Lima")

    fun redondear2(valor: Double): Double = kotlin.math.round(valor * 100.0) / 100.0

    /**
     * Motor central de cálculo de métricas financieras.
     * Reutilizado por todos los módulos para garantizar que NUNCA exista discrepancia
     * entre Resumen, Ventas, Rentabilidad o Sucursales.
     */
    fun calcular(
        ventas: Collection<Venta>,
        devoluciones: Collection<DevolucionVenta>
    ): MetricasVentas {
        // Deduplicación estricta por ID
        val ventasById = ventas.associateBy { it.id }
        val devolucionesById = devoluciones.associateBy { it.id }

        // Regla R12: Vacío absoluto sin inventar datos
        if (ventasById.isEmpty() && devolucionesById.isEmpty()) {
            return MetricasVentas()
        }

        // Filtramos ventas ANULADAS (impacto 0 en dinero y en costos)
        val noAnuladas = ventasById.values.filter { it.estado != Venta.ESTADO_ANULADA }

        // Brutas: suma de totales de ventas no anuladas
        val brutas = redondear2(noAnuladas.sumOf { it.total })

        // Devoluciones: suma de montos devueltos según su fecha de devolución
        val devTotal = redondear2(devolucionesById.values.sumOf { it.montoReembolso })

        // Netas: brutas - devTotal (sin piso artificial a 0 para auditar períodos de solo reembolsos)
        val netas = redondear2(brutas - devTotal)

        // Costo bruto: costo histórico real guardado al vender
        val costoBruto = redondear2(noAnuladas.sumOf { it.costoTotalReal })

        // Costo devuelto: costo histórico guardado en cada devolución
        val costoDev = redondear2(devolucionesById.values.sumOf { it.costoTotalDevuelto })

        // Costo neto: costoBruto - costoDev
        val costoNeto = redondear2(costoBruto - costoDev)

        // Utilidad y Margen
        val util = redondear2(netas - costoNeto)
        val margen = if (netas > 0.0) redondear2((util / netas) * 100.0) else 0.0

        // IGV = sum desglose(total) de no anuladas - sum desglose de devoluciones
        val igvBruto = noAnuladas.sumOf { FacturacionPayloadBuilder.calcularDesgloseIgv(it.total).igv }
        val igvDevuelto = devolucionesById.values.sumOf { FacturacionPayloadBuilder.calcularDesgloseIgv(it.montoReembolso).igv }
        val igvTotal = redondear2((igvBruto - igvDevuelto).coerceAtLeast(0.0))

        // Cantidad de transacciones
        val cantidadTx = noAnuladas.count { it.estado != Venta.ESTADO_DEVOLUCION_TOTAL }
        val ticketPromedio = if (cantidadTx > 0 && netas > 0.0) redondear2(netas / cantidadTx) else 0.0

        // Auditoría de costos guardados: porcentaje exacto sin costo congelado
        val ventasConMonto = noAnuladas.filter { it.total > 0.0 }
        val ventasSinCosto = ventasConMonto.filter { it.costoTotalReal <= 0.0 }
        val tieneCostosEstimados = ventasSinCosto.isNotEmpty()
        val pctSinCosto = if (ventasConMonto.isNotEmpty()) {
            redondear2((ventasSinCosto.size.toDouble() / ventasConMonto.size) * 100.0)
        } else 0.0

        // Desglose de cobros NETOS por método (igual que Caja: el vuelto solo nace del efectivo y se descuenta ahí).
        val metodosMap = mutableMapOf<String, Double>()
        val ticketsMetodoMap = mutableMapOf<String, Int>()
        for (v in noAnuladas) {
            var vueltoRestante = redondear2(v.vuelto.coerceAtLeast(0.0))
            if (v.pagos.isEmpty() && v.total > 0.0) {
                val metodo = "EFECTIVO"
                metodosMap[metodo] = redondear2((metodosMap[metodo] ?: 0.0) + v.total)
                ticketsMetodoMap[metodo] = (ticketsMetodoMap[metodo] ?: 0) + 1
            } else {
                for (p in v.pagos) {
                    val metodo = p.tipoId.trim().ifBlank { p.nombreMetodo.trim().ifBlank { "SIN_ESPECIFICAR" } }
                    val netoLinea = if (p.tipoId == "EFECTIVO" && vueltoRestante > 0.0) {
                        val asignado = minOf(vueltoRestante, p.monto)
                        vueltoRestante = redondear2(vueltoRestante - asignado)
                        redondear2(p.monto - asignado)
                    } else redondear2(p.monto)
                    metodosMap[metodo] = redondear2((metodosMap[metodo] ?: 0.0) + netoLinea)
                    ticketsMetodoMap[metodo] = (ticketsMetodoMap[metodo] ?: 0) + 1
                }
            }
        }
        for (d in devolucionesById.values) {
            val metodo = d.metodoReembolso.trim().ifBlank { "SIN_ESPECIFICAR" }
            metodosMap[metodo] = redondear2((metodosMap[metodo] ?: 0.0) - d.montoReembolso)
        }
        val metodosNetos = metodosMap.mapValues { redondear2(it.value) }

        return MetricasVentas(
            ventasBrutas = brutas,
            devoluciones = devTotal,
            ventasNetas = netas,
            costoBruto = costoBruto,
            costoDevuelto = costoDev,
            costoNeto = costoNeto,
            utilidadBruta = util,
            margenBruto = margen,
            igvTotal = igvTotal,
            cantidadTransacciones = cantidadTx,
            ticketPromedio = ticketPromedio,
            tieneCostosEstimados = tieneCostosEstimados,
            porcentajeSinCostoCongelado = pctSinCosto,
            cantidadVentasSinCosto = ventasSinCosto.size,
            ventasPorMetodo = metodosNetos,
            ticketsPorMetodo = ticketsMetodoMap
        )
    }

    // ── 1. MÓDULO RESUMEN ──────────────────────────────────────────────────────
    fun calcularResumen(ventas: Collection<Venta>, devoluciones: Collection<DevolucionVenta>): ResumenAnalytics {
        val m = calcular(ventas, devoluciones)
        return ResumenAnalytics(
            ventasNetas = m.ventasNetas,
            ventasBrutas = m.ventasBrutas,
            totalDevoluciones = m.devoluciones,
            costoNeto = m.costoNeto,
            utilidadBruta = m.utilidadBruta,
            margenBruto = m.margenBruto,
            igvTotal = m.igvTotal,
            cantidadTransacciones = m.cantidadTransacciones,
            ticketPromedio = m.ticketPromedio,
            tieneCostosEstimados = m.tieneCostosEstimados
        )
    }

    // ── 2. MÓDULO VENTAS ───────────────────────────────────────────────────────
    fun calcularVentas(ventas: Collection<Venta>, devoluciones: Collection<DevolucionVenta>): VentasAnalytics {
        val m = calcular(ventas, devoluciones)
        val noAnuladas = ventas.filter { it.estado != Venta.ESTADO_ANULADA }

        val ventasPorHora = mutableMapOf<Int, Double>()
        val ticketsPorHora = mutableMapOf<Int, Int>()
        val ventasPorVendedor = mutableMapOf<String, Double>()
        val ticketsPorVendedor = mutableMapOf<String, Int>()
        val cal = Calendar.getInstance(TIMEZONE_LIMA)

        for (v in noAnuladas) {
            if (v.fechaHoraMs > 0) {
                cal.timeInMillis = v.fechaHoraMs
                val hora = cal.get(Calendar.HOUR_OF_DAY)
                ventasPorHora[hora] = redondear2((ventasPorHora[hora] ?: 0.0) + v.total)
                ticketsPorHora[hora] = (ticketsPorHora[hora] ?: 0) + 1
            }
            val vendedor = v.cajeroNombre.trim().ifBlank { "SIN_ESPECIFICAR" }
            ventasPorVendedor[vendedor] = redondear2((ventasPorVendedor[vendedor] ?: 0.0) + v.total)
            ticketsPorVendedor[vendedor] = (ticketsPorVendedor[vendedor] ?: 0) + 1
        }

        val totalEfectivos = noAnuladas.count { it.estado != Venta.ESTADO_DEVOLUCION_TOTAL }

        return VentasAnalytics(
            totalDocumentos = ventas.size,
            totalDocumentosEfectivos = totalEfectivos,
            ventasNetas = m.ventasNetas,
            ventasBrutas = m.ventasBrutas,
            devoluciones = m.devoluciones,
            ticketPromedio = m.ticketPromedio,
            ventasPorHoraTurno = ventasPorHora,
            ticketsPorHora = ticketsPorHora,
            ventasPorVendedor = ventasPorVendedor,
            ticketsPorVendedor = ticketsPorVendedor
        )
    }

    // ── 3. MÓDULO RENTABILIDAD (COSTO CONGELADO, NETO CON DESCUENTO Y DEVOLUCIONES) ──
    fun calcularRentabilidad(ventas: Collection<Venta>, devoluciones: Collection<DevolucionVenta>): RentabilidadAnalytics {
        val m = calcular(ventas, devoluciones)
        val noAnuladas = ventas.filter { it.estado != Venta.ESTADO_ANULADA }

        val prodVentas = mutableMapOf<String, Double>()
        val prodCosto = mutableMapOf<String, Double>()
        val prodNombres = mutableMapOf<String, String>()
        val prodPresentaciones = mutableMapOf<String, String>()
        val prodUnidadesVendidas = mutableMapOf<String, Int>()
        val prodUnidadesDevueltas = mutableMapOf<String, Int>()

        for (v in noAnuladas) {
            val factorDesc = if (v.subtotal > 0.0) (v.total / v.subtotal).coerceIn(0.0, 1.0) else 1.0
            for (it in v.items) {
                val pid = it.productoId.trim().ifBlank { "SIN_ID_${it.nombreProducto.trim().ifBlank { "SIN_NOMBRE" }}" }
                val pres = it.presentacionNombre.trim()
                val key = if (pres.isNotBlank()) "${pid}__${pres}" else pid
                val nombreConPres = if (pres.isNotBlank()) "${it.nombreProducto.trim()} ($pres)" else it.nombreProducto.trim()
                if (prodNombres[key].isNullOrBlank()) prodNombres[key] = nombreConPres.ifBlank { "Producto sin nombre" }
                if (prodPresentaciones[key].isNullOrBlank()) prodPresentaciones[key] = pres
                prodVentas[key] = redondear2((prodVentas[key] ?: 0.0) + it.subtotal * factorDesc)
                prodCosto[key] = redondear2((prodCosto[key] ?: 0.0) + it.costoTotalReal)
                prodUnidadesVendidas[key] = (prodUnidadesVendidas[key] ?: 0) + it.cantidad
            }
        }
        // Restar devoluciones del período por producto y presentación para que Σ productos == neto central (sin pérdida).
        for (d in devoluciones) {
            for (it in d.items) {
                val pid = it.productoId.trim().ifBlank { "SIN_ID_${it.nombreProducto.trim().ifBlank { "SIN_NOMBRE" }}" }
                val pres = it.presentacionNombre.trim()
                val key = if (pres.isNotBlank()) "${pid}__${pres}" else pid
                val nombreConPres = if (pres.isNotBlank()) "${it.nombreProducto.trim()} ($pres)" else it.nombreProducto.trim()
                if (prodNombres[key].isNullOrBlank()) prodNombres[key] = nombreConPres.ifBlank { "Producto sin nombre" }
                if (prodPresentaciones[key].isNullOrBlank()) prodPresentaciones[key] = pres
                prodVentas[key] = redondear2((prodVentas[key] ?: 0.0) - it.monto)
                prodCosto[key] = redondear2((prodCosto[key] ?: 0.0) - it.montoCosto)
                prodUnidadesDevueltas[key] = (prodUnidadesDevueltas[key] ?: 0) + it.cantidad
            }
        }

        val listaProductos = prodVentas.keys.map { key ->
            val vNet = redondear2(prodVentas[key] ?: 0.0)
            val cNet = redondear2(prodCosto[key] ?: 0.0)
            val util = redondear2(vNet - cNet)
            val margen = if (vNet > 0.0) redondear2((util / vNet) * 100.0) else 0.0
            MargenProductoItem(
                productoId = key,
                nombre = prodNombres[key] ?: key,
                presentacion = prodPresentaciones[key] ?: "",
                unidadesVendidas = prodUnidadesVendidas[key] ?: 0,
                unidadesDevueltas = prodUnidadesDevueltas[key] ?: 0,
                ventasNetas = vNet,
                costoNeto = cNet,
                utilidad = util,
                margenPorcentaje = margen,
                esEstimado = cNet <= 0.0
            )
        }

        val candidatosConCosto = listaProductos.filter { !it.esEstimado && it.ventasNetas > 0 }

        // 1. Mayor Margen (% descendente)
        val topMargen = candidatosConCosto.sortedByDescending { it.margenPorcentaje }.take(5)
        val idsEnTop = topMargen.map { it.productoId }.toSet()

        // 2. Mayor Ganancia en Dinero (+S/ descendente)
        val topDinero = candidatosConCosto.sortedByDescending { it.utilidad }.take(5)

        // 3. Menor Margen / Ajustado (sin solapar el top de margen, con margen positivo)
        val bajoMargen = candidatosConCosto
            .filter { it.productoId !in idsEnTop && it.utilidad >= 0.0 }
            .sortedBy { it.margenPorcentaje }
            .take(5)

        // 4. Productos con Pérdida (utilidad negativa: venta por debajo de costo o devoluciones)
        val conPerdida = listaProductos.filter { it.esPerdida }.sortedBy { it.utilidad }

        // 5. Todos los productos vendidos en el período ordenados por utilidad
        val todosValidos = listaProductos.filter { it.ventasNetas > 0 || it.esPerdida }.sortedByDescending { it.utilidad }

        return RentabilidadAnalytics(
            utilidadBruta = m.utilidadBruta,
            margenGeneral = m.margenBruto,
            costoVentasCogs = m.costoNeto,
            tieneCostosEstimados = m.tieneCostosEstimados,
            porcentajeSinCostoCongelado = m.porcentajeSinCostoCongelado,
            cantidadVentasSinCosto = m.cantidadVentasSinCosto,
            productosMayorMargen = topMargen,
            productosMayorGananciaDinero = topDinero,
            productosMenorMargen = bajoMargen,
            productosConPerdida = conPerdida,
            todosLosProductos = todosValidos
        )
    }

    data class CostoReferenciaLote(
        val costoUnitario: Double,
        val origen: String,
        val proveedor: String
    )

    private fun parseFechaMs(fechaStr: String): Long {
        if (fechaStr.isBlank()) return 0L
        val formatos = listOf("dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm", "dd/MM/yyyy", "yyyy-MM-dd")
        for (f in formatos) {
            try {
                val sdf = SimpleDateFormat(f, Locale.US)
                sdf.isLenient = false
                val d = sdf.parse(fechaStr)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return fechaStr.toLongOrNull() ?: 0L
    }

    private fun obtenerCostoReferenciaLote(p: MoldeProductos): CostoReferenciaLote {
        val lotesConCosto = p.lotes.values.filter {
            (it.costoCompraUnitario > 0.0 || it.costoUltimoIngresoUnitario > 0.0) && !it.noValorizado
        }
        if (lotesConCosto.isNotEmpty()) {
            val loteMasReciente = lotesConCosto.maxByOrNull {
                val msCreated = parseFechaMs(it.createdAt)
                if (msCreated > 0L) msCreated else parseFechaMs(it.fecha)
            } ?: lotesConCosto.last()

            val costo = if (loteMasReciente.costoCompraUnitario > 0.0) {
                loteMasReciente.costoCompraUnitario
            } else {
                loteMasReciente.costoUltimoIngresoUnitario
            }

            val origen = when {
                loteMasReciente.nroFactura.isNotBlank() -> "Fact. ${loteMasReciente.nroFactura}"
                loteMasReciente.numero.isNotBlank() -> "Lote ${loteMasReciente.numero}"
                else -> "Lote reciente"
            }
            val prov = loteMasReciente.proveedorNombre.ifBlank { p.proveedorBaseNombre }
            return CostoReferenciaLote(redondear2(costo), origen, prov)
        }

        val costoBase = p.precioCompra.coerceAtLeast(0.0)
        val origen = if (costoBase > 0.0) "Ficha de catálogo" else "Sin costo registrado"
        return CostoReferenciaLote(redondear2(costoBase), origen, p.proveedorBaseNombre)
    }

    fun obtenerRepresentacionUnidad(p: MoldeProductos, plural: Boolean = false): String {
        val pres = p.presentaciones.firstOrNull()
        val singular = p.inventarioPerfilUnidadSingular.trim()
            .ifBlank { p.empaque.trim() }
            .ifBlank { p.unidadBase.trim() }
            .ifBlank { pres?.empaque?.trim().orEmpty() }
            .ifBlank { pres?.unidadMedida?.trim().orEmpty() }
            .ifBlank { "Unidad" }

        if (!plural) return singular

        val pl = p.inventarioPerfilUnidadPlural.trim()
        if (pl.isNotBlank()) return pl

        val lower = singular.lowercase()
        return when {
            lower.endsWith("a") || lower.endsWith("e") || lower.endsWith("i") || lower.endsWith("o") || lower.endsWith("u") -> "${singular}s"
            lower.endsWith("l") || lower.endsWith("r") || lower.endsWith("n") || lower.endsWith("d") || lower.endsWith("j") -> "${singular}es"
            lower.endsWith("z") -> "${singular.dropLast(1)}ces"
            else -> "${singular}s"
        }
    }

    // ── 4. MÓDULO INVENTARIO ───────────────────────────────────────────────────
    fun calcularInventario(
        productos: List<MoldeProductos>,
        ventasPeriodo: Collection<Venta>,
        ahoraMs: Long = 0L,
        inicioPeriodoMs: Long = 0L,
        finPeriodoMs: Long = 0L
    ): InventarioAnalytics {
        var valorTotal = 0.0
        var totalUnidadesVendibles = 0.0
        val quiebresNombres = mutableListOf<String>()
        val detalleQuiebres = mutableListOf<ProductoQuiebreKpi>()
        var capitalRiesgoQuiebre = 0.0
        var capitalPedidoMinimoCompleto = 0.0
        var porVencerCount = 0
        var productosSinCostoCount = 0

        val productosVendidosIds = ventasPeriodo
            .filter { it.estado != Venta.ESTADO_ANULADA }
            .flatMap { it.items }
            .map { it.productoId.trim() }
            .filter { it.isNotBlank() }
            .toSet()

        val inmovilizadosNombres = mutableListOf<String>()
        val detalleInmovilizados = mutableListOf<ProductoInmovilizadoKpi>()
        var valorInmovilizado = 0.0

        for (p in productos) {
            val esEliminado = p.categoriaPrincipal.contains("ELIMINADO", ignoreCase = true) ||
                p.nombre.startsWith("[ELIMINADO]", ignoreCase = true) ||
                p.codigo.startsWith("DEL_", ignoreCase = true) ||
                p.indice.isBlank()
            val esAptoParaVenta = p.activo && !esEliminado

            val stockFisico = p.stockFisicoTotalUnidades
            val stockVendible = p.stockDisponibleFisico
            if (esAptoParaVenta) {
                totalUnidadesVendibles += stockVendible
            }

            // Costo de compra de referencia: último costo de adquisición real (lote más reciente con factura/proveedor)
            val costoRefInfo = obtenerCostoReferenciaLote(p)
            val costoUnitarioRef = costoRefInfo.costoUnitario

            // Auditoría de verdad (R3/R12): detectar existencias físicas sin costo de compra asignado
            if (stockFisico > 0.0 && costoUnitarioRef <= 0.0) {
                productosSinCostoCount++
            }

            // 1. Valorización real del producto lote por lote:
            // Solo computa existencias físicas reales. Suma lote por lote para precisión contable absoluta.
            var valorProd = 0.0
            var stockFisicoReal = 0.0
            var lotesConStockCount = 0

            if (p.lotes.isNotEmpty()) {
                for (lote in p.lotes.values) {
                    val cantLote = (lote.cantidad + lote.cantidadBloqueada).coerceAtLeast(0.0)
                    if (cantLote > 0.0) {
                        stockFisicoReal += cantLote
                        lotesConStockCount++
                        if (!lote.noValorizado) {
                            val cLote = when {
                                lote.costoCompraUnitario > 0.0 -> lote.costoCompraUnitario
                                lote.costoUltimoIngresoUnitario > 0.0 -> lote.costoUltimoIngresoUnitario
                                else -> costoUnitarioRef
                            }
                            valorProd += cantLote * cLote
                        }
                    }
                }
            } else if (stockFisico > 0.0) {
                stockFisicoReal = stockFisico
                valorProd = stockFisico * costoUnitarioRef
                lotesConStockCount = 1
            }
            valorTotal += valorProd

            val pId = p.indice.ifBlank { p.codigo }.trim()
            val pNombre = p.nombre.ifBlank { pId }
            val empaque = p.empaque.ifBlank { p.unidadBase.ifBlank { "Caja" } }
            val unidadSingular = obtenerRepresentacionUnidad(p, plural = false)
            val unidadPlural = obtenerRepresentacionUnidad(p, plural = true)

            // 2. Quiebre contable real de stock:
            // Regla de Negocio y Verdad (R12/R3):
            // Solo aplica a productos APTOS PARA VENTA (p.activo == true, no eliminados, no merma total).
            // Un producto inactivo o descontinuado por la farmacia NUNCA se sugiere para reposición.
            val tieneMinimoConfigurado = p.stockMinimoBase > 0.0
            val tieneOperacionReal = costoUnitarioRef > 0.0 || p.lotes.isNotEmpty()
            if (esAptoParaVenta && tieneMinimoConfigurado && tieneOperacionReal && stockVendible <= p.stockMinimoBase) {
                val unidadesFaltantes = (p.stockMinimoBase - stockVendible).coerceAtLeast(0.0)
                if (unidadesFaltantes > 0.0) {
                    val costoRepoFaltante = redondear2(unidadesFaltantes * costoUnitarioRef)
                    val costoPedidoMinimo = redondear2(p.stockMinimoBase * costoUnitarioRef)

                    capitalRiesgoQuiebre += costoRepoFaltante
                    capitalPedidoMinimoCompleto += costoPedidoMinimo
                    quiebresNombres.add(pNombre)

                    detalleQuiebres.add(
                        ProductoQuiebreKpi(
                            nombre = pNombre,
                            empaque = empaque,
                            unidadMedida = unidadSingular,
                            unidadMedidaPlural = unidadPlural,
                            stockActual = stockVendible,
                            stockMinimo = p.stockMinimoBase,
                            costoUnitarioCompra = costoUnitarioRef,
                            unidadesFaltantes = unidadesFaltantes,
                            costoReposicionFaltante = costoRepoFaltante,
                            costoPedidoMinimoCompleto = costoPedidoMinimo,
                            origenCosto = costoRefInfo.origen,
                            proveedorNombre = costoRefInfo.proveedor
                        )
                    )
                }
            }

            // Lotes por vencer (mantenido en memoria para compatibilidad)
            for (lote in p.lotes.values) {
                if (lote.cantidad > 0 && lote.vencimiento.isNotBlank()) {
                    val dias = com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper.diasHastaVencer(lote.vencimiento)
                    if (dias != null && dias in 0..30) {
                        porVencerCount++
                    }
                }
            }

            // 3. Mercadería inmóvil (Capital inmovilizado real):
            // Requiere que el producto tenga existencias físicas (stockFisicoReal > 0) y capital valorizado (valorProd > 0).
            // Si el producto no tiene stock, no tiene dinero estancado.
            // Tampoco si fue creado después del fin del período consultado.
            val fueCreadoDespuesDelPeriodo = finPeriodoMs > 0L && p.creadoEnMillis > 0L && p.creadoEnMillis > finPeriodoMs
            val esInmovilizado = stockFisicoReal > 0.0 && valorProd > 0.0 && !fueCreadoDespuesDelPeriodo &&
                pId !in productosVendidosIds && (p.codigo.isBlank() || p.codigo !in productosVendidosIds)

            if (esInmovilizado) {
                inmovilizadosNombres.add(pNombre)
                valorInmovilizado += valorProd

                val costoPromedioPonderado = if (stockFisicoReal > 0.0) valorProd / stockFisicoReal else costoUnitarioRef
                val esPromedio = lotesConStockCount > 1

                detalleInmovilizados.add(
                    ProductoInmovilizadoKpi(
                        nombre = pNombre,
                        empaque = empaque,
                        unidadMedida = unidadSingular,
                        unidadMedidaPlural = unidadPlural,
                        stockActual = stockFisicoReal,
                        costoUnitarioCompra = redondear2(costoPromedioPonderado),
                        valorEstancado = redondear2(valorProd),
                        cantidadLotes = lotesConStockCount,
                        esCostoPromedio = esPromedio
                    )
                )
            }
        }

        val valorTotalRedondeado = redondear2(valorTotal)
        val valorInmovilizadoRedondeado = redondear2(valorInmovilizado)
        val capitalEnRotacion = redondear2((valorTotalRedondeado - valorInmovilizadoRedondeado).coerceAtLeast(0.0))
        val tasaRotacion = if (valorTotalRedondeado > 0.0) {
            redondear2((capitalEnRotacion / valorTotalRedondeado) * 100.0)
        } else 0.0

        return InventarioAnalytics(
            valorInventarioTotal = valorTotalRedondeado,
            capitalEnRotacion = capitalEnRotacion,
            capitalInmovilizado = valorInmovilizadoRedondeado,
            tasaRotacionPorcentaje = tasaRotacion,
            capitalRiesgoQuiebre = redondear2(capitalRiesgoQuiebre),
            capitalPedidoMinimoCompleto = redondear2(capitalPedidoMinimoCompleto),
            cantidadQuiebresStock = detalleQuiebres.size,
            productosSinCostoDefinido = productosSinCostoCount,
            detalleQuiebres = detalleQuiebres.sortedByDescending { it.costoReposicionFaltante },
            detalleInmovilizados = detalleInmovilizados.sortedByDescending { it.valorEstancado },
            // Compatibilidad legada
            totalUnidadesDisponibles = redondear2(totalUnidadesVendibles),
            productosEnQuiebre = quiebresNombres,
            cantidadPorVencer30Dias = porVencerCount,
            valorInmovilizado90Dias = valorInmovilizadoRedondeado,
            productosInmovilizados = inmovilizadosNombres
        )
    }

    // ── 5. MÓDULO COMPRAS (excluye ANULADA y CANCELADA/CANCELADO: no suman ni cuentan) ──
    fun calcularCompras(facturasCompra: List<FacturaCompra>): ComprasAnalytics {
        val validas = facturasCompra.filter { f ->
            if (f.esAnulada) return@filter false
            val est = f.estadoPago.trim().uppercase()
            est != "CANCELADA" && est != "CANCELADO"
        }
        val comprasNetas = redondear2(validas.sumOf { it.totalEfectivo })
        val comprasContado = redondear2(validas.filter { it.esContado }.sumOf { it.totalEfectivo })
        val comprasCredito = redondear2(validas.filterNot { it.esContado }.sumOf { it.totalEfectivo })
        val totalPagado = redondear2(validas.sumOf { it.totalAbonadoReal })
        val deudaPendiente = redondear2(validas.sumOf { it.saldoPendienteReal })

        val porProveedor = mutableMapOf<String, Double>()
        for (f in validas) {
            val prov = f.proveedorNombre.trim().ifBlank { "SIN ESPECIFICAR" }
            porProveedor[prov] = redondear2((porProveedor[prov] ?: 0.0) + f.totalEfectivo)
        }

        // Ordenamiento cronológico real (yyyyMMdd numérico para evitar errores de orden alfabético en dd/MM/yyyy)
        val facturasOrdenadas = validas.sortedByDescending { f ->
            val fechaStr = f.fechaEmision.ifBlank { f.fechaRegistro }
            if (fechaStr.isBlank()) 0L
            else {
                try {
                    val partes = fechaStr.trim().split("/", "-")
                    if (partes.size == 3) {
                        if (fechaStr.contains("-") && partes[0].length == 4) {
                            partes[0].toLong() * 10000L + partes[1].toLong() * 100L + partes[2].toLong()
                        } else {
                            partes[2].toLong() * 10000L + partes[1].toLong() * 100L + partes[0].toLong()
                        }
                    } else 0L
                } catch (_: Exception) {
                    0L
                }
            }
        }

        return ComprasAnalytics(
            comprasNetas = comprasNetas,
            comprasContado = comprasContado,
            comprasCredito = comprasCredito,
            totalPagadoProveedores = totalPagado,
            deudaPendienteProveedores = deudaPendiente,
            cantidadFacturasCompra = validas.size,
            comprasPorProveedor = porProveedor,
            facturas = facturasOrdenadas
        )
    }

    // ── 6. MÓDULO CLIENTES ─────────────────────────────────────────────────────
    fun calcularClientes(ventas: Collection<Venta>, devoluciones: Collection<DevolucionVenta>): ClientesAnalytics {
        val noAnuladas = ventas.filter { it.estado != Venta.ESTADO_ANULADA }
        val devsByVentaId = devoluciones.groupBy { it.ventaId }

        val clientesMap = mutableMapOf<String, ResumenClienteKpi>()
        var cantIdentificados = 0
        var cantAnonimos = 0

        for (v in noAnuladas) {
            val doc = v.cliente.numeroDocumento.trim()
            val nom = v.cliente.nombre.trim()
            // Verdad operativa: si hay documento o nombre real, es identificado (aunque el tipo legacy sea NINGUNO).
            val esAnonimo = doc.isBlank() && (nom.isBlank() || nom.equals("Consumidor Final", ignoreCase = true))

            if (esAnonimo) {
                cantAnonimos++
            } else {
                cantIdentificados++
            }

            val cid = when {
                v.cliente.clienteId.isNotBlank() && v.cliente.clienteId != "CONSUMIDOR_FINAL" -> v.cliente.clienteId.trim()
                doc.isNotBlank() -> doc
                nom.isNotBlank() && !nom.equals("Consumidor Final", ignoreCase = true) -> nom.uppercase(Locale.ROOT)
                else -> "CONSUMIDOR_FINAL"
            }
            val cNom = when {
                nom.isNotBlank() && !nom.equals("Consumidor Final", ignoreCase = true) -> nom
                doc.isNotBlank() -> "Cliente $doc"
                else -> "Consumidor Final"
            }

            val montoDev = devsByVentaId[v.id]?.sumOf { it.montoReembolso } ?: 0.0
            val netoVenta = (v.total - montoDev).coerceAtLeast(0.0)

            val previo = clientesMap[cid]
            if (previo == null) {
                clientesMap[cid] = ResumenClienteKpi(
                    clienteId = cid,
                    documento = doc,
                    nombre = cNom,
                    totalCompradoNeto = redondear2(netoVenta),
                    cantidadCompras = if (netoVenta > 0.0) 1 else 0,
                    ultimaCompraMs = v.fechaHoraMs
                )
            } else {
                val mejorNombre = when {
                    nom.isNotBlank() && !nom.equals("Consumidor Final", ignoreCase = true) && !nom.startsWith("Cliente ") -> nom
                    previo.nombre.isNotBlank() && !previo.nombre.equals("Consumidor Final", ignoreCase = true) && !previo.nombre.startsWith("Cliente ") -> previo.nombre
                    cNom.isNotBlank() -> cNom
                    else -> previo.nombre
                }
                val mejorDoc = if (doc.isNotBlank()) doc else previo.documento
                clientesMap[cid] = previo.copy(
                    documento = mejorDoc,
                    nombre = mejorNombre,
                    totalCompradoNeto = redondear2(previo.totalCompradoNeto + netoVenta),
                    cantidadCompras = previo.cantidadCompras + (if (netoVenta > 0.0) 1 else 0),
                    ultimaCompraMs = maxOf(previo.ultimaCompraMs, v.fechaHoraMs)
                )
            }
        }

        val clientesReales = clientesMap.values.filter { it.clienteId != "CONSUMIDOR_FINAL" }
        val totalClientes = clientesReales.size + cantAnonimos
        val nuevos = clientesReales.count { it.cantidadCompras == 1 }
        val recurrentes = clientesReales.count { it.cantidadCompras > 1 }
        // Ticket con la misma verdad central: netas del período / transacciones efectivas (excluye ANULADA y DEV_TOTAL).
        val central = calcular(ventas, devoluciones)
        val ticketPromedio = central.ticketPromedio

        val rankingIdentificados = clientesReales
            .filter { (it.documento.isNotBlank() || !it.nombre.equals("Consumidor Final", ignoreCase = true)) && it.totalCompradoNeto > 0.0 }
            .sortedByDescending { it.totalCompradoNeto }
            .take(10)

        return ClientesAnalytics(
            totalClientesAtendidos = totalClientes,
            clientesIdentificados = cantIdentificados,
            clientesAnonimos = cantAnonimos,
            clientesNuevos = nuevos,
            clientesRecurrentes = recurrentes,
            ticketPromedioCliente = ticketPromedio,
            rankingClientes = rankingIdentificados
        )
    }

    // ── 7. MÓDULO CAJA ─────────────────────────────────────────────────────────
    fun calcularCaja(
        estadoCaja: EstadoCaja,
        movimientos: List<MovimientoCaja>,
        metricasVentas: MetricasVentas
    ): CajaAnalytics {
        val cobrosNetos = metricasVentas.ventasPorMetodo
        val ventasEfecPos = cobrosNetos["EFECTIVO"] ?: 0.0
        val difCuadre = redondear2(ventasEfecPos - estadoCaja.ventasEfectivo)

        return CajaAnalytics(
            estadoTurno = estadoCaja.estado,
            sesionId = estadoCaja.sesionId,
            abiertoPorNombre = estadoCaja.abiertoPorNombre,
            fondoInicial = estadoCaja.fondoInicial,
            ventasEfectivoNetas = estadoCaja.ventasEfectivo,
            ingresosManuales = estadoCaja.ingresos,
            retirosManuales = estadoCaja.retiros,
            efectivoEsperadoEnCajon = estadoCaja.efectivoEsperado,
            cobrosPorMetodo = cobrosNetos,
            diferenciaCuadre = difCuadre,
            hayDescuadre = kotlin.math.abs(difCuadre) > 0.01
        )
    }



    // ── 10. ESTADO REAL DEL NEGOCIO: RESULTADO ECONÓMICO ───────────────────────
    fun calcularEstadoResultadoNegocio(
        metricas: MetricasVentas,
        gastosOperativos: Double,
        diferenciaCaja: Double = 0.0
    ): EstadoResultadoNegocio {
        val faltante = if (diferenciaCaja < -0.01) kotlin.math.abs(diferenciaCaja) else 0.0
        val utilidadNeta = redondear2(metricas.utilidadBruta - gastosOperativos - faltante)
        val margenNeto = if (metricas.ventasNetas > 0.0) {
            redondear2((utilidadNeta / metricas.ventasNetas) * 100.0)
        } else 0.0

        return EstadoResultadoNegocio(
            ventasNetas = metricas.ventasNetas,
            costoVentasCogs = metricas.costoNeto,
            utilidadBruta = metricas.utilidadBruta,
            margenBruto = metricas.margenBruto,
            gastosOperativos = redondear2(gastosOperativos),
            faltanteCaja = redondear2(faltante),
            utilidadNeta = utilidadNeta,
            margenNeto = margenNeto,
            tieneCostosEstimados = metricas.tieneCostosEstimados
        )
    }

    // ── 11. DINERO Y CAJA (FLUJO REAL Y CONTROL FÍSICO) ────────────────────────
    fun calcularDineroYCaja(
        sesiones: List<CajaSesion>,
        movimientos: List<MovimientoCaja>,
        estadoCajaActual: EstadoCaja,
        metricasVentas: MetricasVentas,
        esPeriodoHoy: Boolean
    ): DineroYCajaAnalytics {
        val sesionesValidas = sesiones.filter { it.id != "actual" }
        val sesionesCerradas = sesionesValidas.filter { it.estado == CajaSesion.ESTADO_CERRADA }

        // Aperturas: suma de fondos iniciales de sesiones cerradas (+ turno vivo si hoy está abierto)
        var aperturas = sesionesCerradas.sumOf { it.fondoInicial }
        if (esPeriodoHoy && estadoCajaActual.estado == CajaSesion.ESTADO_ABIERTA) {
            aperturas += estadoCajaActual.fondoInicial
        }

        // Cobros: total de ventas netas por todos los métodos en el período
        val cobros = metricasVentas.ventasNetas

        // Otros ingresos: ingresos manuales a caja (desde movimientos o sesiones)
        val ingresosMovs = movimientos.filter { it.tipo == MovimientoCaja.TIPO_INGRESO }.sumOf { kotlin.math.abs(it.monto) }
        val ingresosCerradas = sesionesCerradas.sumOf { it.ingresos }
        val ingresosAbierta = if (esPeriodoHoy && estadoCajaActual.estado == CajaSesion.ESTADO_ABIERTA) estadoCajaActual.ingresos else 0.0
        val otrosIngresos = if (ingresosMovs > 0.0) redondear2(ingresosMovs) else redondear2(ingresosCerradas + ingresosAbierta)

        // Egresos: retiros manuales / gastos en efectivo de caja
        val retirosMovs = movimientos.filter { it.tipo == MovimientoCaja.TIPO_RETIRO }.sumOf { kotlin.math.abs(it.monto) }
        val retirosCerradas = sesionesCerradas.sumOf { it.retiros }
        val retirosAbierta = if (esPeriodoHoy && estadoCajaActual.estado == CajaSesion.ESTADO_ABIERTA) estadoCajaActual.retiros else 0.0
        val egresos = if (retirosMovs > 0.0) redondear2(retirosMovs) else redondear2(retirosCerradas + retirosAbierta)

        // Flujo neto: (Cobros + Otros Ingresos) - Egresos
        val flujoNeto = redondear2((cobros + otrosIngresos) - egresos)

        // Control físico de caja:
        var esperado = sesionesCerradas.sumOf { it.efectivoEsperado }
        val contado = sesionesCerradas.sumOf { it.efectivoContado }
        if (esPeriodoHoy && estadoCajaActual.estado == CajaSesion.ESTADO_ABIERTA) {
            // Turno abierto: solo suma al ESPERADO. El conteo físico ocurre en el arqueo formal;
            // copiar el esperado como "contado" falsearía la lectura real de gaveta (R3/R12).
            esperado += estadoCajaActual.efectivoEsperado
        }

        val diferenciaTotal = redondear2(sesionesCerradas.sumOf { it.diferenciaEfectivo })

        val cantFaltantes = sesionesCerradas.count { it.diferenciaEfectivo < -0.01 }
        val cantSobrantes = sesionesCerradas.count { it.diferenciaEfectivo > 0.01 }

        val esAnteriorAbierta = estadoCajaActual.esDeJornadaAnterior(HoraServidor.ahoraMs())

        return DineroYCajaAnalytics(
            aperturasTotal = redondear2(aperturas),
            cobrosTotales = redondear2(cobros),
            otrosIngresos = redondear2(otrosIngresos),
            egresosTotales = redondear2(egresos),
            flujoNetoDinero = flujoNeto,
            cajaEsperadaTotal = redondear2(esperado),
            cajaContadaTotal = redondear2(contado),
            diferenciaCajaTotal = diferenciaTotal,
            cantidadTurnosCerrados = sesionesCerradas.size,
            cantidadCajasConFaltante = cantFaltantes,
            cantidadCajasConSobrante = cantSobrantes,
            turnosPendientesDeCierre = if (esAnteriorAbierta) 1 else 0,
            fechaCajaPendiente = if (esAnteriorAbierta) estadoCajaActual.fechaAperturaLegible() else ""
        )
    }

    // ── 12. ALERTAS DE LA VERDAD DEL NEGOCIO (INCLUYENDO COSAS MALAS) ───────────
    fun generarAlertasVerdad(
        dineroYCaja: DineroYCajaAnalytics,
        rentabilidad: RentabilidadAnalytics,
        sesiones: List<CajaSesion>,
        estadoCajaActual: EstadoCaja
    ): List<AlertaVerdadNegocio> {
        val alertas = mutableListOf<AlertaVerdadNegocio>()

        // 1. Alerta de caja de jornada anterior abierta (hora corregida por servidor, no reloj local)
        if (estadoCajaActual.esDeJornadaAnterior(HoraServidor.ahoraMs())) {
            alertas.add(
                AlertaVerdadNegocio(
                    clave = "CAJA_ANTERIOR_ABIERTA",
                    titulo = "Caja pendiente de cierre del ${estadoCajaActual.fechaAperturaLegible()}",
                    descripcion = "Existe una caja abierta de una fecha anterior sin arqueo formal (abierta por ${estadoCajaActual.abiertoPorNombre.ifBlank { "usuario" }}). Debe cerrarse para no mezclar dinero.",
                    severidad = "ERROR"
                )
            )
        }

        // 2. Alertas de faltantes de caja en el período
        val sesionesConFaltante = sesiones.filter { it.diferenciaEfectivo < -0.01 }
        for (s in sesionesConFaltante) {
            val fechaTurno = if (s.cierreLegible.isNotBlank()) s.cierreLegible else s.aperturaLegible
            val responsable = s.cerradoPorNombre.ifBlank { s.abiertoPorNombre.ifBlank { "Cajero" } }
            alertas.add(
                AlertaVerdadNegocio(
                    clave = "FALTANTE_${s.id}",
                    titulo = "Faltante de caja: 🔴 -S/ %.2f".format(Locale.US, kotlin.math.abs(s.diferenciaEfectivo)),
                    descripcion = "Turno cerrado el $fechaTurno por $responsable. Esperado: S/ %.2f, Contado físico: S/ %.2f.".format(
                        Locale.US, s.efectivoEsperado, s.efectivoContado
                    ),
                    severidad = "ERROR",
                    monto = s.diferenciaEfectivo
                )
            )
        }

        // 3. Alertas de sobrantes de caja
        val sesionesConSobrante = sesiones.filter { it.diferenciaEfectivo > 0.01 }
        for (s in sesionesConSobrante) {
            val fechaTurno = if (s.cierreLegible.isNotBlank()) s.cierreLegible else s.aperturaLegible
            alertas.add(
                AlertaVerdadNegocio(
                    clave = "SOBRANTE_${s.id}",
                    titulo = "Sobrante de caja: +S/ %.2f".format(Locale.US, s.diferenciaEfectivo),
                    descripcion = "Turno cerrado el $fechaTurno. Se contó más dinero que el esperado teórico.",
                    severidad = "INFO",
                    monto = s.diferenciaEfectivo
                )
            )
        }

        // 4. Alerta de gastos operativos registrados
        if (dineroYCaja.egresosTotales > 0.0) {
            alertas.add(
                AlertaVerdadNegocio(
                    clave = "EGRESOS_REGISTRADOS",
                    titulo = "Egresos / Retiros de caja: S/ %.2f".format(Locale.US, dineroYCaja.egresosTotales),
                    descripcion = "Salidas de efectivo registradas durante el período que reducen la utilidad neta.",
                    severidad = "WARNING",
                    monto = dineroYCaja.egresosTotales
                )
            )
        }

        // 5. Alerta de margen bajo
        for (p in rentabilidad.productosMenorMargen.take(3)) {
            if (!p.esEstimado && p.margenPorcentaje < 15.0 && p.ventasNetas > 0.0) {
                alertas.add(
                    AlertaVerdadNegocio(
                        clave = "MARGEN_BAJO_${p.productoId}",
                        titulo = "Margen bajo en '${p.nombre}': %.1f%%".format(Locale.US, p.margenPorcentaje),
                        descripcion = "Costo congelado S/ %.2f sobre venta S/ %.2f.".format(Locale.US, p.costoNeto, p.ventasNetas),
                        severidad = "WARNING"
                    )
                )
            }
        }

        return alertas
    }

    // ── 13. CONCILIACIÓN FISCAL POS ↔ SUNAT (para que Reportes no muestre ceros falsos) ──
    fun construirConciliacionFiscal(
        ventas: Collection<Venta>,
        devoluciones: Collection<DevolucionVenta>,
        documentos: Collection<com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento>
    ): ConciliacionFiscal {
        val totalNoAnuladas = ventas.count { it.estado != Venta.ESTADO_ANULADA }
        val totalAnuladas = ventas.count { it.estado == Venta.ESTADO_ANULADA }
        val aceptados = documentos.count { it.estadoEnvio == com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento.ESTADO_ACEPTADO }
        val pendientes = documentos.count { it.estadoEnvio == com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento.ESTADO_PENDIENTE && !it.numeroQuemado }
        val enviados = documentos.count { it.estadoEnvio == com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento.ESTADO_ENVIADO && !it.numeroQuemado }
        val rechazados = documentos.count { it.numeroQuemado || it.estadoEnvio == com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento.ESTADO_RECHAZADO }
        val anulados = documentos.count { it.estadoEnvio == com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento.ESTADO_ANULADO }
        // Cada venta genera 1 doc, cada devolución 1 NC extra, cada anulación 1 doc extra (baja/NC).
        val posEmitidos = ventas.size + devoluciones.size + totalAnuladas
        val diferencia = posEmitidos - documentos.size
        return ConciliacionFiscal(
            totalVentasNoAnuladas = totalNoAnuladas,
            totalDocumentosFiscales = documentos.size,
            aceptados = aceptados,
            pendientes = pendientes,
            rechazados = rechazados,
            enviados = enviados,
            anulados = anulados,
            diferenciaEmitidosVsFiscal = diferencia
        )
    }

    // ── 14. CONCILIACIÓN CAJA POS ↔ MOVIMIENTOS (para que Reportes cuadre con Cierre) ──
    fun construirConciliacionCaja(
        metricas: MetricasVentas,
        movimientos: Collection<MovimientoCaja>,
        ventasSegunPuntero: Map<String, Double> = emptyMap()
    ): ConciliacionCaja {
        // Solo cobros: VENTA (+) , DEVOLUCION (−) , ANULACION (−). Ingresos/retiros manuales no son ventas.
        val cobrosCaja = mutableMapOf<String, Double>()
        for (m in movimientos) {
            if (m.tipo != MovimientoCaja.TIPO_VENTA && m.tipo != MovimientoCaja.TIPO_DEVOLUCION && m.tipo != MovimientoCaja.TIPO_ANULACION) continue
            val clave = m.metodoTipo.trim().ifBlank { "SIN_ESPECIFICAR" }
            cobrosCaja[clave] = redondear2((cobrosCaja[clave] ?: 0.0) + m.monto)
        }
        val pos = metricas.ventasPorMetodo.mapValues { redondear2(it.value) }
        val todasClaves = (pos.keys + cobrosCaja.keys).distinct()
        val difs = mutableMapOf<String, Double>()
        for (k in todasClaves) {
            difs[k] = redondear2((pos[k] ?: 0.0) - (cobrosCaja[k] ?: 0.0))
        }
        val difTotal = redondear2(difs.values.sum())
        val hayDif = difs.values.any { kotlin.math.abs(it) > 0.01 }
        val haySinMetodo = todasClaves.any { it == "SIN_ESPECIFICAR" || it == "SIN_METODO" || it.isBlank() } &&
                todasClaves.filter { it == "SIN_ESPECIFICAR" || it == "SIN_METODO" || it.isBlank() }.sumOf { kotlin.math.abs(pos[it] ?: 0.0) + kotlin.math.abs(cobrosCaja[it] ?: 0.0) } > 0.01
        return ConciliacionCaja(
            hayDiferencia = hayDif,
            diferenciaTotal = difTotal,
            hayMovimientosSinMetodo = haySinMetodo,
            sumCajaMovimientos = cobrosCaja.mapValues { redondear2(it.value) },
            ventasSegunPOS = pos,
            ventasSegunPunteroCaja = ventasSegunPuntero.mapValues { redondear2(it.value) },
            diferenciasPorMetodo = difs
        )
    }

    /** Suma los ventasPorMetodo de varias sesiones cerradas + turno vivo para el puntero del período. */
    fun sumarVentasPorMetodoSesiones(
        sesiones: Collection<CajaSesion>,
        estadoActual: EstadoCaja? = null,
        incluirAbiertaHoy: Boolean = false
    ): Map<String, Double> {
        val acc = mutableMapOf<String, Double>()
        for (s in sesiones) {
            if (s.id == "actual") continue
            for ((k, v) in s.ventasPorMetodo) {
                acc[k] = redondear2((acc[k] ?: 0.0) + v)
            }
        }
        if (incluirAbiertaHoy && estadoActual != null && estadoActual.estado == CajaSesion.ESTADO_ABIERTA) {
            for ((k, v) in estadoActual.ventasPorMetodo) {
                acc[k] = redondear2((acc[k] ?: 0.0) + v)
            }
        }
        return acc
    }

    /**
     * Combina los punteros de turno VIGENTES (uno por cajero) en un único EstadoCaja agregado
     * de la sede (R1/R3/R13). Sin turnos abiertos devuelve caja cerrada vacía (R12: cero inventos).
     */
    fun combinarEstadosCaja(estados: Collection<EstadoCaja>): EstadoCaja {
        val abiertas = estados.filter { it.estado == CajaSesion.ESTADO_ABIERTA && it.aperturaMs > 0L }
        if (abiertas.isEmpty()) return EstadoCaja()

        val metodos = mutableMapOf<String, Double>()
        for (e in abiertas) {
            for ((k, v) in e.ventasPorMetodo) {
                metodos[k] = redondear2((metodos[k] ?: 0.0) + v)
            }
        }
        val nombres = abiertas.map { it.abiertoPorNombre.trim() }.filter { it.isNotBlank() }.distinct()

        return EstadoCaja(
            estado = CajaSesion.ESTADO_ABIERTA,
            sesionId = abiertas.firstOrNull { it.sesionId.isNotBlank() }?.sesionId ?: "",
            fondoInicial = redondear2(abiertas.sumOf { it.fondoInicial }),
            aperturaMs = abiertas.minOf { it.aperturaMs },
            abiertoPorNombre = when (nombres.size) {
                1 -> nombres.first()
                else -> if (nombres.isEmpty()) "" else "${nombres.size} cajeros con turno abierto"
            },
            ventasPorMetodo = metodos,
            ingresos = redondear2(abiertas.sumOf { it.ingresos }),
            retiros = redondear2(abiertas.sumOf { it.retiros }),
            devolucionesEfectivo = redondear2(abiertas.sumOf { it.devolucionesEfectivo }),
            cantidadVentas = abiertas.sumOf { it.cantidadVentas },
            cantidadDevoluciones = abiertas.sumOf { it.cantidadDevoluciones }
        )
    }

    // ── 15. MULTISEDE: misma fórmula central por sede, total cadena == Σ sedes exacto ──
    fun calcularSucursales(
        ventasPorSede: Map<String, List<Venta>>,
        devsPorSede: Map<String, List<DevolucionVenta>>,
        nombresSedes: Map<String, String>
    ): SucursalesAnalytics {
        val items = mutableListOf<ItemSucursalAnalytics>()
        var totalCadena = 0.0
        var utilidadCadena = 0.0
        val todasSedesIds = (ventasPorSede.keys + devsPorSede.keys + nombresSedes.keys).distinct().sorted()
        for (sId in todasSedesIds) {
            val vs = ventasPorSede[sId] ?: emptyList()
            val ds = devsPorSede[sId] ?: emptyList()
            if (vs.isEmpty() && ds.isEmpty() && !nombresSedes.containsKey(sId)) continue
            val m = calcular(vs, ds)
            totalCadena = redondear2(totalCadena + m.ventasNetas)
            utilidadCadena = redondear2(utilidadCadena + m.utilidadBruta)
            items.add(
                ItemSucursalAnalytics(
                    sucursalId = sId,
                    nombreSede = nombresSedes[sId] ?: sId.ifBlank { "Sin sede" },
                    ventasNetas = m.ventasNetas,
                    utilidad = m.utilidadBruta,
                    cantidadTransacciones = m.cantidadTransacciones
                )
            )
        }
        return SucursalesAnalytics(
            ventasTotalesCadena = totalCadena,
            utilidadCadena = utilidadCadena,
            ventasPorSede = items.sortedByDescending { it.ventasNetas }
        )
    }

    // ── 16. INSIGHTS SOLO CON EVIDENCIA: cero si todo cuadra ──
    fun calcularInsights(
        inventario: InventarioAnalytics,
        rentabilidad: RentabilidadAnalytics,
        conciliacionFiscal: ConciliacionFiscal,
        conciliacionCaja: ConciliacionCaja
    ): InsightsAnalytics {
        val stockMin = inventario.productosEnQuiebre.take(5)
        val porVencer = if (inventario.cantidadPorVencer30Dias > 0) listOf("${inventario.cantidadPorVencer30Dias} lotes por vencer en 30 días") else emptyList()
        val inmov = inventario.productosInmovilizados.take(5)
        val margenBajo = rentabilidad.productosMenorMargen.filter { !it.esEstimado && it.ventasNetas > 0 && it.margenPorcentaje < 15.0 }.take(3).map { "${it.nombre}: ${String.format(Locale.US, "%.1f%%", it.margenPorcentaje)}" }
        val descuadres = mutableListOf<String>()
        if (conciliacionFiscal.rechazados > 0) descuadres.add("${conciliacionFiscal.rechazados} comprobantes rechazados por SUNAT")
        if (conciliacionCaja.hayDiferencia) descuadres.add("Caja descuadrada por S/ ${String.format(Locale.US, "%.2f", conciliacionCaja.diferenciaTotal)}")
        return InsightsAnalytics(
            alertasStockMinimo = stockMin,
            alertasPorVencer = porVencer,
            alertasInmovilizados = inmov,
            alertasMargenBajoCostoCongelado = margenBajo,
            alertasDescuadreFiscalCaja = descuadres
        )
    }
}
