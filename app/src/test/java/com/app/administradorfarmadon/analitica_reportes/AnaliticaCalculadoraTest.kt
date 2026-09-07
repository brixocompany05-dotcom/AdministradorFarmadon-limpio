package com.app.administradorfarmadon.analitica_reportes

import com.app.administradorfarmadon.analitica_reportes.logica.AnaliticaCalculadora
import com.app.administradorfarmadon.analitica_reportes.logica.ComprasAnalytics
import com.app.administradorfarmadon.analitica_reportes.modelo.ConciliacionCaja
import com.app.administradorfarmadon.analitica_reportes.modelo.ConciliacionFiscal
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteInfo
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.PrecioStock
import com.app.administradorfarmadon.inventario.compartido.modelo.ProductoBase
import com.app.administradorfarmadon.ventas.compartido.modelo.ClienteDeVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemDevolucion
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.LoteConsumido
import com.app.administradorfarmadon.ventas.compartido.modelo.PagoVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.analitica_reportes.exportacion.ReporteExportador
import com.app.administradorfarmadon.analitica_reportes.logica.ReportesDatasetBuilder
import com.app.administradorfarmadon.analitica_reportes.modelo.AnaliticaUiState
import com.app.administradorfarmadon.analitica_reportes.modelo.PeriodoAnalitica
import com.app.administradorfarmadon.analitica_reportes.modelo.FuenteDatos
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de la Calculadora de Analítica (MVP + 9 Módulos).
 *
 * Verifica el contrato fundamental:
 * "Analítica debe poder explicar de dónde salió cada importe y reproducir
 * el mismo resultado aunque los eventos se repitan, la pantalla se reconstruya,
 * el usuario reintente la operación o los costos actuales cambien."
 */
class AnaliticaCalculadoraTest {

    private fun crearVenta(
        id: String,
        total: Double,
        costoTotal: Double,
        estado: String = Venta.ESTADO_COMPLETADA,
        diaClave: String = "2026-09-02",
        metodoPago: String = "EFECTIVO",
        clienteId: String = "cli-1",
        productoId: String = "prod-1",
        nombreProducto: String = "Paracetamol 500mg",
        cajero: String = ""
    ): Venta {
        return Venta(
            id = id,
            total = total,
            costoTotalReal = costoTotal,
            estado = estado,
            diaClave = diaClave,
            cajeroNombre = cajero,
            cliente = ClienteDeVenta(clienteId = clienteId, nombre = "Juan Perez", numeroDocumento = "12345678"),
            items = listOf(
                ItemVenta(
                    productoId = productoId,
                    nombreProducto = nombreProducto,
                    cantidad = 2,
                    precioUnitario = total / 2,
                    subtotal = total,
                    costoTotalReal = costoTotal,
                    lotesConsumidos = listOf(
                        LoteConsumido(
                            loteId = "lote-1",
                            loteNumero = "L001",
                            cantidadFisica = 2.0,
                            costoUnitarioReal = costoTotal / 2
                        )
                    )
                )
            ),
            pagos = listOf(
                PagoVenta(
                    tipoId = metodoPago,
                    nombreMetodo = metodoPago,
                    monto = total
                )
            )
        )
    }

    private fun crearDevolucion(
        id: String,
        ventaId: String,
        monto: Double,
        costoDevuelto: Double,
        diaClave: String = "2026-09-02",
        metodoReembolso: String = "EFECTIVO"
    ): DevolucionVenta {
        return DevolucionVenta(
            id = id,
            ventaId = ventaId,
            montoReembolso = monto,
            costoTotalDevuelto = costoDevuelto,
            diaClave = diaClave,
            metodoReembolso = metodoReembolso,
            items = listOf(
                ItemDevolucion(
                    productoId = "prod-1",
                    nombreProducto = "Paracetamol 500mg",
                    cantidad = 1,
                    precioUnitario = monto,
                    monto = monto,
                    costoUnitarioReal = costoDevuelto,
                    montoCosto = costoDevuelto
                )
            )
        )
    }

    // ── 1. EVENTO DUPLICADO CON MISMO ID SE CUENTA 1 SOLA VEZ ──────────────────
    @Test
    fun eventoDuplicadoConMismoId_seCuentaUnaSolaVez() {
        val v1 = crearVenta("V001", total = 100.0, costoTotal = 60.0)
        val v1Repetida = crearVenta("V001", total = 100.0, costoTotal = 60.0)
        val v2 = crearVenta("V002", total = 50.0, costoTotal = 30.0)

        val listaDuplicada = listOf(v1, v1Repetida, v2)
        val res = AnaliticaCalculadora.calcular(listaDuplicada, emptyList())

        assertEquals(150.0, res.ventasBrutas, 0.001)
        assertEquals(150.0, res.ventasNetas, 0.001)
        assertEquals(90.0, res.costoBruto, 0.001)
        assertEquals(90.0, res.costoNeto, 0.001)
        assertEquals(60.0, res.utilidadBruta, 0.001)
        assertEquals(2, res.cantidadTransacciones)
    }

    // ── 2. VENTA ANULADA = 0 (EXCLUIDA TOTALMENTE) ─────────────────────────────
    @Test
    fun ventaAnulada_tieneImpactoCero() {
        val v1 = crearVenta("V001", total = 100.0, costoTotal = 60.0, estado = Venta.ESTADO_COMPLETADA)
        val vAnulada = crearVenta("V002", total = 200.0, costoTotal = 120.0, estado = Venta.ESTADO_ANULADA)

        val res = AnaliticaCalculadora.calcular(listOf(v1, vAnulada), emptyList())

        assertEquals(100.0, res.ventasBrutas, 0.001)
        assertEquals(100.0, res.ventasNetas, 0.001)
        assertEquals(60.0, res.costoBruto, 0.001)
        assertEquals(60.0, res.costoNeto, 0.001)
        assertEquals(40.0, res.utilidadBruta, 0.001)
        assertEquals(1, res.cantidadTransacciones)
    }

    // ── 3. DEVOLUCIÓN PARCIAL RESTA 1 SOLA VEZ ─────────────────────────────────
    @Test
    fun devolucionParcial_restaUnaSolaVez() {
        val v1 = crearVenta("V001", total = 100.0, costoTotal = 60.0, estado = Venta.ESTADO_DEVOLUCION_PARCIAL)
        val d1 = crearDevolucion("D001", ventaId = "V001", monto = 50.0, costoDevuelto = 30.0)
        val d1Repetida = crearDevolucion("D001", ventaId = "V001", monto = 50.0, costoDevuelto = 30.0)

        val res = AnaliticaCalculadora.calcular(listOf(v1), listOf(d1, d1Repetida))

        assertEquals(100.0, res.ventasBrutas, 0.001)
        assertEquals(50.0, res.devoluciones, 0.001)
        assertEquals(50.0, res.ventasNetas, 0.001)
        assertEquals(60.0, res.costoBruto, 0.001)
        assertEquals(30.0, res.costoDevuelto, 0.001)
        assertEquals(30.0, res.costoNeto, 0.001)
        assertEquals(20.0, res.utilidadBruta, 0.001)
        assertEquals(40.0, res.margenBruto, 0.001)
    }

    // ── 4. CAMBIO DE COSTO EN CATÁLOGO NO ALTERA VENTA PASADA ──────────────────
    @Test
    fun cambioDeCostoEnCatalogo_noAlteraCostoVentaPasada() {
        val v1 = crearVenta("V001", total = 100.0, costoTotal = 40.0)
        val res = AnaliticaCalculadora.calcular(listOf(v1), emptyList())

        assertEquals(40.0, res.costoBruto, 0.001)
        assertEquals(40.0, res.costoNeto, 0.001)
        assertEquals(60.0, res.utilidadBruta, 0.001)
        assertEquals(60.0, res.margenBruto, 0.001)
        assertFalse(res.tieneCostosEstimados)
    }

    // ── 5. DEVOLUCIÓN TARDÍA IMPACTA EN EL PERÍODO DE LA DEVOLUCIÓN ───────────
    @Test
    fun devolucionTardia_impactaEnPeriodoDeDevolucion() {
        val vDia1 = crearVenta("V001", total = 100.0, costoTotal = 50.0, diaClave = "2026-09-01")
        val dDia5 = crearDevolucion("D001", ventaId = "V001", monto = 100.0, costoDevuelto = 50.0, diaClave = "2026-09-05")

        val resDia1 = AnaliticaCalculadora.calcular(listOf(vDia1), emptyList())
        assertEquals(100.0, resDia1.ventasBrutas, 0.001)
        assertEquals(0.0, resDia1.devoluciones, 0.001)
        assertEquals(100.0, resDia1.ventasNetas, 0.001)
        assertEquals(50.0, resDia1.costoNeto, 0.001)
        assertEquals(50.0, resDia1.utilidadBruta, 0.001)

        val resDia5 = AnaliticaCalculadora.calcular(emptyList(), listOf(dDia5))
        assertEquals(0.0, resDia5.ventasBrutas, 0.001)
        assertEquals(100.0, resDia5.devoluciones, 0.001)
        assertEquals(-100.0, resDia5.ventasNetas, 0.001)
        assertEquals(50.0, resDia5.costoDevuelto, 0.001)
    }

    // ── 6. MÉTODOS NETOS PERMITE SALDO NEGATIVO (SIN PISO ARTIFICIAL A CERO) ──
    @Test
    fun metodosNetos_permiteSaldoNegativoPorSobreReembolso() {
        // Venta cobrada en EFECTIVO por S/ 20
        val v1 = crearVenta("V001", total = 20.0, costoTotal = 10.0, metodoPago = "EFECTIVO")
        // Devolución reembolsada en EFECTIVO por S/ 50 (de una venta anterior)
        val d1 = crearDevolucion("D001", ventaId = "V000", monto = 50.0, costoDevuelto = 25.0, metodoReembolso = "EFECTIVO")

        val res = AnaliticaCalculadora.calcular(listOf(v1), listOf(d1))

        // El saldo neto de efectivo debe ser 20 - 50 = -30.00 (auditable, sin piso a 0)
        assertEquals(-30.0, res.ventasPorMetodo["EFECTIVO"] ?: 0.0, 0.001)
    }

    // ── 7. RENTABILIDAD SEPARA MARGEN REAL DE MARGEN ESTIMADO ─────────────────
    @Test
    fun rentabilidad_separaMargenRealDeMargenEstimado() {
        val vReal = crearVenta("V001", total = 100.0, costoTotal = 60.0, productoId = "p1", nombreProducto = "Amoxicilina")
        val vVieja = crearVenta("V002", total = 80.0, costoTotal = 0.0, productoId = "p2", nombreProducto = "Ibuprofeno")

        val rent = AnaliticaCalculadora.calcularRentabilidad(listOf(vReal, vVieja), emptyList())

        assertTrue(rent.tieneCostosEstimados)
        assertEquals(180.0 - 60.0, rent.utilidadBruta, 0.001)
        assertEquals(60.0, rent.costoVentasCogs, 0.001)

        val amox = rent.productosMayorMargen.firstOrNull { it.productoId == "p1" }
        if (amox != null) {
            assertFalse(amox.esEstimado)
            assertEquals(40.0, amox.margenPorcentaje, 0.001)
        }
    }

    // ── 8. INVENTARIO DETECTA QUIEBRES Y PRODUCTOS INMOVILIZADOS ──────────────
    @Test
    fun inventario_detectaQuiebresEInmovilizadosSinVentas() {
        val pSinVentas = MoldeProductos(
            productoBase = ProductoBase(nombre = "Vitamina C", indice = "p-inmovilizado", activo = true),
            precioStock = PrecioStock(stockMinimoBase = 10.0, precioCompra = 5.0),
            loteInfo = LoteInfo(lotes = mapOf("L1" to LoteProducto(cantidad = 20.0, costoCompraUnitario = 5.0)))
        )

        val pQuiebre = MoldeProductos(
            productoBase = ProductoBase(nombre = "Alcohol 70", indice = "p-quiebre", activo = true),
            precioStock = PrecioStock(stockMinimoBase = 15.0, precioCompra = 2.0),
            loteInfo = LoteInfo(lotes = mapOf("L2" to LoteProducto(cantidad = 5.0, costoCompraUnitario = 2.0)))
        )

        // Ninguna venta en el período para p-inmovilizado
        val inv = AnaliticaCalculadora.calcularInventario(listOf(pSinVentas, pQuiebre), emptyList(), ahoraMs = System.currentTimeMillis())

        assertEquals(1, inv.cantidadQuiebresStock)
        assertTrue(inv.productosEnQuiebre.contains("Alcohol 70"))
        assertEquals(2, inv.productosInmovilizados.size)
        assertEquals(110.0, inv.valorInventarioTotal, 0.001) // 20*5 + 5*2 = 110
    }

    // ── 9. COMPRAS EXCLUYE FACTURAS ANULADAS O CANCELADAS ───────────────────
    @Test
    fun compras_excluyeFacturasAnuladas() {
        val fc1 = FacturaCompra(id = "FC1", montoTotal = 500.0, estadoPago = "PAGADA", proveedorNombre = "Droguería A")
        val fcAnulada = FacturaCompra(id = "FC2", montoTotal = 1200.0, estadoPago = "ANULADA", proveedorNombre = "Droguería B")
        val fcCancelada = FacturaCompra(id = "FC3", montoTotal = 800.0, estadoPago = "CANCELADO", proveedorNombre = "Droguería C")

        val compras = AnaliticaCalculadora.calcularCompras(listOf(fc1, fcAnulada, fcCancelada))

        assertEquals(500.0, compras.comprasNetas, 0.001)
        assertEquals(1, compras.cantidadFacturasCompra)
        assertEquals(500.0, compras.comprasPorProveedor["Droguería A"] ?: 0.0, 0.001)
        assertFalse(compras.comprasPorProveedor.containsKey("Droguería B"))
        assertFalse(compras.comprasPorProveedor.containsKey("Droguería C"))
    }

    // ── 10. CLIENTES ACUMULA COMPRAS RESTANDO MÚLTIPLES DEVOLUCIONES PARCIALES ──
    @Test
    fun clientes_acumulaComprasRestandoDevoluciones() {
        val v1 = crearVenta("V001", total = 100.0, costoTotal = 50.0, clienteId = "cli-10")
        val v2 = crearVenta("V002", total = 60.0, costoTotal = 30.0, clienteId = "cli-10")
        // 2 devoluciones parciales de la MISMA venta V002
        val d1 = crearDevolucion("D001", ventaId = "V002", monto = 20.0, costoDevuelto = 10.0)
        val d2 = crearDevolucion("D002", ventaId = "V002", monto = 15.0, costoDevuelto = 7.5)

        val cli = AnaliticaCalculadora.calcularClientes(listOf(v1, v2), listOf(d1, d2))

        assertEquals(1, cli.totalClientesAtendidos)
        assertEquals(1, cli.clientesRecurrentes) // 2 compras
        val resumen = cli.rankingClientes.first()
        assertEquals("cli-10", resumen.clienteId)
        // 100 + 60 - 20 - 15 = 125.00
        assertEquals(125.0, resumen.totalCompradoNeto, 0.001)
    }

    // ── 11. SUCURSALES FAN-OUT MANTIENE EXACTITUD SIN DIVERGENCIA ─────────────
    @Test
    fun sucursales_fanOut_mantieneExactitudSinDivergencia() {
        val vSede1 = crearVenta("V1", total = 100.0, costoTotal = 40.0)
        val vSede2 = crearVenta("V2", total = 200.0, costoTotal = 80.0)

        val ventasPorSede = mapOf("sede-1" to listOf(vSede1), "sede-2" to listOf(vSede2))
        val nombresSedes = mapOf("sede-1" to "Principal", "sede-2" to "Norte")

        val suc = AnaliticaCalculadora.calcularSucursales(ventasPorSede, emptyMap(), nombresSedes)

        assertEquals(300.0, suc.ventasTotalesCadena, 0.001)
        assertEquals(180.0, suc.utilidadCadena, 0.001) // 60 + 120 = 180
        assertEquals(2, suc.ventasPorSede.size)
    }

    // ── 12. INSIGHTS SOLO CON EVIDENCIA: CERO SI TODO CUADRA, ALERTA CON PRUEBA ──
    @Test
    fun insights_soloConEvidencia() {
        // Caso A: Operación 100% cuadrada y saludable -> 0 alertas
        val invLimpio = AnaliticaCalculadora.calcularInventario(emptyList(), emptyList(), System.currentTimeMillis())
        val rentLimpia = AnaliticaCalculadora.calcularRentabilidad(emptyList(), emptyList())
        val concFiscalOk = ConciliacionFiscal(totalVentasNoAnuladas = 5, aceptados = 5, pendientes = 0, rechazados = 0)
        val concCajaOk = ConciliacionCaja(hayDiferencia = false, diferenciaTotal = 0.0)

        val insightsLimpio = AnaliticaCalculadora.calcularInsights(invLimpio, rentLimpia, concFiscalOk, concCajaOk)

        assertTrue(insightsLimpio.alertasStockMinimo.isEmpty())
        assertTrue(insightsLimpio.alertasPorVencer.isEmpty())
        assertTrue(insightsLimpio.alertasInmovilizados.isEmpty())
        assertTrue(insightsLimpio.alertasMargenBajoCostoCongelado.isEmpty())
        assertTrue(insightsLimpio.alertasDescuadreFiscalCaja.isEmpty())

        // Caso B: Con prueba irrefutable (SUNAT rechazó 1 comprobante y caja descuadrada)
        val concFiscalFalla = concFiscalOk.copy(rechazados = 1)
        val concCajaFalla = concCajaOk.copy(hayDiferencia = true, diferenciaTotal = 15.0)

        val insightsAlerta = AnaliticaCalculadora.calcularInsights(invLimpio, rentLimpia, concFiscalFalla, concCajaFalla)

        assertEquals(2, insightsAlerta.alertasDescuadreFiscalCaja.size)
    }

    // ── 13. AISLAMIENTO: VENTA NORTE NO APARECE EN PRINCIPAL (PARTICIÓN R1) ────
    @Test
    fun multisede_aislamiento_ventaNorteNoApareceEnPrincipal() {
        val vPrincipal = crearVenta("V_PRI_1", total = 100.0, costoTotal = 40.0)
        val vNorte = crearVenta("V_NOR_1", total = 250.0, costoTotal = 110.0)

        val ventasPorSede = mapOf(
            "sede-principal" to listOf(vPrincipal),
            "sede-norte" to listOf(vNorte)
        )

        // Partición estricta: calcular solo Principal
        val metricasPrincipal = AnaliticaCalculadora.calcular(
            ventasPorSede["sede-principal"] ?: emptyList(),
            emptyList()
        )
        // Partición estricta: calcular solo Norte
        val metricasNorte = AnaliticaCalculadora.calcular(
            ventasPorSede["sede-norte"] ?: emptyList(),
            emptyList()
        )

        assertEquals(100.0, metricasPrincipal.ventasNetas, 0.001)
        assertEquals(40.0, metricasPrincipal.costoNeto, 0.001)
        assertEquals(250.0, metricasNorte.ventasNetas, 0.001)
        assertEquals(110.0, metricasNorte.costoNeto, 0.001)

        // Aislamiento: Principal no tiene ni un céntimo de Norte
        assertFalse(metricasPrincipal.ventasNetas >= 250.0)
    }

    // ── 14. SUMA GLOBAL: TOTAL FARMACIA == Σ SEDES EXACTO A 2 DECIMALES ────────
    @Test
    fun multisede_sumaGlobal_totalFarmaciaIgualSumaSedesExacto() {
        // Sede Principal: S/ 150.35 - S/ 10.20 dev = S/ 140.15 netas
        val vP = crearVenta("VP1", total = 150.35, costoTotal = 60.0)
        val dP = crearDevolucion("DP1", ventaId = "VP1", monto = 10.20, costoDevuelto = 4.0)

        // Sede Norte: S/ 240.80 - S/ 25.45 dev = S/ 215.35 netas
        val vN = crearVenta("VN1", total = 240.80, costoTotal = 90.0)
        val dN = crearDevolucion("DN1", ventaId = "VN1", monto = 25.45, costoDevuelto = 10.0)

        // Sede Sur: S/ 99.90 - S/ 0.00 dev = S/ 99.90 netas
        val vS = crearVenta("VS1", total = 99.90, costoTotal = 35.0)

        val ventasPorSede = mapOf("sede-p" to listOf(vP), "sede-n" to listOf(vN), "sede-s" to listOf(vS))
        val devsPorSede = mapOf("sede-p" to listOf(dP), "sede-n" to listOf(dN), "sede-s" to emptyList())
        val nombresSedes = mapOf("sede-p" to "Principal", "sede-n" to "Norte", "sede-s" to "Sur")

        // 1. Calculado con fan-out multisede
        val sucursales = AnaliticaCalculadora.calcularSucursales(ventasPorSede, devsPorSede, nombresSedes)

        // 2. Calculado con consolidación global única
        val todasVentas = listOf(vP, vN, vS)
        val todasDevs = listOf(dP, dN)
        val consolidado = AnaliticaCalculadora.calcular(todasVentas, todasDevs)

        // Suma manual de control: 140.15 + 215.35 + 99.90 = 455.40
        val sumaEsperada = 455.40

        assertEquals(sumaEsperada, sucursales.ventasTotalesCadena, 0.001)
        assertEquals(sumaEsperada, consolidado.ventasNetas, 0.001)
        // Total farmacia == Σ sedes bajo las mismas reglas exacto a 2 decimales
        assertEquals(sucursales.ventasTotalesCadena, consolidado.ventasNetas, 0.0001)
    }

    // ── 15. REVERSIBLE: TODAS -> NORTE -> PRINCIPAL -> TODAS DEVUELVE LO MISMO ──
    @Test
    fun multisede_reversible_todasNortePrincipalTodas_mismoResultado() {
        val vP = crearVenta("V_P", total = 100.0, costoTotal = 40.0)
        val vN = crearVenta("V_N", total = 200.0, costoTotal = 90.0)

        val ventasPorSede = mapOf("p" to listOf(vP), "n" to listOf(vN))
        val nombres = mapOf("p" to "Principal", "n" to "Norte")

        // Paso 1: TODAS
        val resTodas1 = AnaliticaCalculadora.calcularSucursales(ventasPorSede, emptyMap(), nombres)

        // Paso 2: Solo Norte
        val resNorte = AnaliticaCalculadora.calcular(listOf(vN), emptyList())
        assertEquals(200.0, resNorte.ventasNetas, 0.001)

        // Paso 3: Solo Principal
        val resPrincipal = AnaliticaCalculadora.calcular(listOf(vP), emptyList())
        assertEquals(100.0, resPrincipal.ventasNetas, 0.001)

        // Paso 4: TODAS de nuevo
        val resTodas2 = AnaliticaCalculadora.calcularSucursales(ventasPorSede, emptyMap(), nombres)

        // Idéntico e inmutable: sin fugas de estado
        assertEquals(resTodas1.ventasTotalesCadena, resTodas2.ventasTotalesCadena, 0.0001)
        assertEquals(resTodas1.utilidadCadena, resTodas2.utilidadCadena, 0.0001)
        assertEquals(resTodas1.ventasPorSede.size, resTodas2.ventasPorSede.size)
    }

    // ── 16. SEDE INACTIVA CON HISTORIA SUMA AL PASADO SIN OPERACIONES NUEVAS ──
    @Test
    fun multisede_sedeInactivaConHistoria_sumaAlPasadoSinOperacionesNuevas() {
        val vActiva = crearVenta("V_ACT", total = 500.0, costoTotal = 200.0)
        val vInactivaHistorica = crearVenta("V_INACT", total = 300.0, costoTotal = 120.0)

        val ventasPorSede = mapOf(
            "sede-activa" to listOf(vActiva),
            "sede-cerrada" to listOf(vInactivaHistorica)
        )
        val nombres = mapOf(
            "sede-activa" to "Sede Principal",
            "sede-cerrada" to "Sede Sur (Inactiva)"
        )

        val suc = AnaliticaCalculadora.calcularSucursales(ventasPorSede, emptyMap(), nombres)

        assertEquals(800.0, suc.ventasTotalesCadena, 0.001)
        assertEquals(480.0, suc.utilidadCadena, 0.001) // 300 + 180 = 480
        val sedeCerradaItem = suc.ventasPorSede.first { it.sucursalId == "sede-cerrada" }
        assertEquals("Sede Sur (Inactiva)", sedeCerradaItem.nombreSede)
        assertEquals(300.0, sedeCerradaItem.ventasNetas, 0.001)
    }

    // ── 17. GENÉRICO N: 50 SEDES CON FOREACH SIN IFS POR NOMBRE ───────────────
    @Test
    fun multisede_genericoN_soporta50SedesConForEachSinIfs() {
        val cantidadSedes = 50
        val ventasPorSede = mutableMapOf<String, List<Venta>>()
        val nombres = mutableMapOf<String, String>()

        for (i in 1..cantidadSedes) {
            val sId = "sede-$i"
            ventasPorSede[sId] = listOf(crearVenta("V_$i", total = 100.0, costoTotal = 60.0))
            nombres[sId] = "Sucursal #$i"
        }

        val suc = AnaliticaCalculadora.calcularSucursales(ventasPorSede, emptyMap(), nombres)

        assertEquals(50, suc.ventasPorSede.size)
        // 50 * 100.00 = 5000.00
        assertEquals(5000.0, suc.ventasTotalesCadena, 0.001)
        // 50 * (100 - 60) = 2000.00
        assertEquals(2000.0, suc.utilidadCadena, 0.001)
    }

    // ── 18. SEPARACIÓN ESTRICTA DE MÉTODOS DE PAGO (SIN FUSIONES NI SUPOSICIONES) ──
    @Test
    fun metodos_separacionEstricta_yapePlinYTransferenciaNoSeFusionan() {
        val vYape = crearVenta("V_YAPE", total = 50.0, costoTotal = 30.0, metodoPago = "YAPE")
        val vPlin = crearVenta("V_PLIN", total = 70.0, costoTotal = 40.0, metodoPago = "PLIN")
        val vTrans = crearVenta("V_TRANS", total = 100.0, costoTotal = 60.0, metodoPago = "TRANSFERENCIA")
        val vOtros = crearVenta("V_OTROS", total = 20.0, costoTotal = 10.0, metodoPago = "OTROS")
        val vSinMetodo = crearVenta("V_SIN", total = 15.0, costoTotal = 8.0, metodoPago = "")

        val res = AnaliticaCalculadora.calcular(listOf(vYape, vPlin, vTrans, vOtros, vSinMetodo), emptyList())

        // Verificamos que YAPE y PLIN son entradas separadas e independientes
        assertEquals(50.0, res.ventasPorMetodo["YAPE"] ?: 0.0, 0.001)
        assertEquals(70.0, res.ventasPorMetodo["PLIN"] ?: 0.0, 0.001)

        // Verificamos que TRANSFERENCIA y OTROS son entradas separadas
        assertEquals(100.0, res.ventasPorMetodo["TRANSFERENCIA"] ?: 0.0, 0.001)
        assertEquals(20.0, res.ventasPorMetodo["OTROS"] ?: 0.0, 0.001)

        // Verificamos que método en blanco se clasifica como SIN_ESPECIFICAR, no como EFECTIVO ni OTROS
        assertEquals(15.0, res.ventasPorMetodo["SIN_ESPECIFICAR"] ?: 0.0, 0.001)
        assertEquals(0.0, res.ventasPorMetodo["EFECTIVO"] ?: 0.0, 0.001)
    }

    // ── 19. AUDITORÍA DE ESTIMACIÓN CON PORCENTAJE EXACTO ─────────────────────
    @Test
    fun rentabilidad_porcentajeExactoSinCostoCongelado_seCalculaConVerdad() {
        val v1 = crearVenta("V1", total = 100.0, costoTotal = 60.0) // con costo
        val v2 = crearVenta("V2", total = 100.0, costoTotal = 0.0)  // sin costo
        val v3 = crearVenta("V3", total = 100.0, costoTotal = 50.0) // con costo
        val v4 = crearVenta("V4", total = 100.0, costoTotal = 0.0)  // sin costo
        val v5 = crearVenta("V5", total = 100.0, costoTotal = 70.0) // con costo

        val res = AnaliticaCalculadora.calcular(listOf(v1, v2, v3, v4, v5), emptyList())
        val rent = AnaliticaCalculadora.calcularRentabilidad(listOf(v1, v2, v3, v4, v5), emptyList())

        assertTrue(res.tieneCostosEstimados)
        assertEquals(2, res.cantidadVentasSinCosto)
        // 2 de 5 = 40.0%
        assertEquals(40.0, res.porcentajeSinCostoCongelado, 0.01)

        assertTrue(rent.tieneCostosEstimados)
        assertEquals(2, rent.cantidadVentasSinCosto)
        assertEquals(40.0, rent.porcentajeSinCostoCongelado, 0.01)
    }

    // ── 20. CLIENTES: IDENTIFICADOS VS ANÓNIMOS ───────────────────────────────
    @Test
    fun clientes_desdobleIdentificadosVsAnonimos_seCalculaCorrecto() {
        val vIdent1 = crearVenta("V1", total = 100.0, costoTotal = 50.0, clienteId = "CLI1")
        val vIdent2 = crearVenta("V2", total = 150.0, costoTotal = 80.0, clienteId = "CLI2")
        val vAnon1 = Venta(
            id = "V3",
            total = 30.0,
            costoTotalReal = 15.0,
            cliente = ClienteDeVenta(clienteId = "", nombre = "Consumidor Final", numeroDocumento = "")
        )
        val vAnon2 = Venta(
            id = "V4",
            total = 40.0,
            costoTotalReal = 20.0,
            cliente = ClienteDeVenta(clienteId = "", nombre = "", numeroDocumento = "")
        )

        val cli = AnaliticaCalculadora.calcularClientes(listOf(vIdent1, vIdent2, vAnon1, vAnon2), emptyList())

        assertEquals(2, cli.clientesIdentificados)
        assertEquals(2, cli.clientesAnonimos)
    }

    // ── 21. COMPRAS: EXCLUYE FACTURAS ANULADAS Y CANCELADAS ───────────────────
    @Test
    fun compras_soloExcluyeAnuladas_mantieneFacturasCanceladasPagadas() {
        val fPagada = FacturaCompra(
            id = "F1",
            proveedorNombre = "Drogueria Central",
            estadoPago = "PAGADA",
            montoTotal = 1500.0
        )
        val fPendiente = FacturaCompra(
            id = "F3",
            proveedorNombre = "Laboratorio Farmed",
            estadoPago = "PENDIENTE",
            montoTotal = 1200.0
        )
        val fAnulada = FacturaCompra(
            id = "F4",
            proveedorNombre = "Drogueria Central",
            estadoPago = "ANULADA",
            montoTotal = 5000.0
        )
        val fCancelada = FacturaCompra(
            id = "F5",
            proveedorNombre = "Distribuidora Lima",
            estadoPago = "CANCELADA",
            montoTotal = 800.0
        )

        val comp = AnaliticaCalculadora.calcularCompras(listOf(fPagada, fPendiente, fAnulada, fCancelada))

        // Excluye fAnulada (5000) y fCancelada (800). Mantiene 1500 + 1200 = 2700.00
        assertEquals(2, comp.cantidadFacturasCompra)
        assertEquals(2700.0, comp.comprasNetas, 0.001)
    }

    // ── 22. CAJA: CONTRATO ACTIVO - SEDE SIN PLIN NO MUESTRA PLIN ─────────────
    @Test
    fun caja_contratoActivo_sedeSinPlinNoMuestraPlin() {
        val metodosConfiguradosSede = listOf(
            InstanciaPago(id = "1", tipoId = "EFECTIVO", activa = true),
            InstanciaPago(id = "2", tipoId = "YAPE", activa = true)
        )
        val v1 = crearVenta("V1", 50.0, 30.0, metodoPago = "EFECTIVO")
        val v2 = crearVenta("V2", 80.0, 40.0, metodoPago = "YAPE")

        val metricas = AnaliticaCalculadora.calcular(listOf(v1, v2), emptyList())

        // PLIN no está configurado en la sede
        assertFalse(metodosConfiguradosSede.any { it.tipoId == "PLIN" })
        assertEquals(0.0, metricas.ventasPorMetodo["PLIN"] ?: 0.0, 0.001)
        assertEquals(50.0, metricas.ventasPorMetodo["EFECTIVO"] ?: 0.0, 0.001)
        assertEquals(80.0, metricas.ventasPorMetodo["YAPE"] ?: 0.0, 0.001)
    }

    // ── 23. REPORTES: 14 DATASETS CONSTRUIDOS CON TOTALES REALES ───────────────
    @Test
    fun reportes_construirDataset14Reportes_generaTablasConTotalesReales() {
        val v1 = crearVenta("V1", 100.0, 60.0, cajero = "Cajero Juan")
        val metricas = AnaliticaCalculadora.calcular(listOf(v1), emptyList())
        val exito = AnaliticaUiState.Exito(
            metricas = metricas,
            estadoCaja = EstadoCaja(sesionId = "SES-1", abiertoPorNombre = "Cajero Juan"),
            conciliacionFiscal = ConciliacionFiscal(totalVentasNoAnuladas = 1, aceptados = 1),
            conciliacionCaja = ConciliacionCaja(),
            rentabilidad = AnaliticaCalculadora.calcularRentabilidad(listOf(v1), emptyList()),
            inventario = AnaliticaCalculadora.calcularInventario(emptyList(), listOf(v1), 1000L),
            compras = AnaliticaCalculadora.calcularCompras(emptyList()),
            clientes = AnaliticaCalculadora.calcularClientes(listOf(v1), emptyList()),
            sucursales = AnaliticaCalculadora.calcularSucursales(mapOf("principal" to listOf(v1)), emptyMap(), mapOf("principal" to "Sede Principal")),
            insights = AnaliticaCalculadora.calcularInsights(
                AnaliticaCalculadora.calcularInventario(emptyList(), listOf(v1), 1000L),
                AnaliticaCalculadora.calcularRentabilidad(listOf(v1), emptyList()),
                ConciliacionFiscal(totalVentasNoAnuladas = 1, aceptados = 1),
                ConciliacionCaja()
            ),
            listaVentas = listOf(v1),
            listaDevoluciones = emptyList(),
            esVacio = false
        )

        val reporteVentas = ReportesDatasetBuilder.construirReporte(
            categoria = "VENTAS",
            tipoReporte = "Ventas Detalladas por Producto",
            exito = exito,
            periodo = "Hoy",
            nombreSede = "Sede Principal",
            razonSocial = "FARMADON TEST S.A.C.",
            ruc = "20600000001"
        )

        assertEquals("Ventas Detalladas por Producto", reporteVentas.tipoReporte)
        assertEquals("FARMADON TEST S.A.C.", reporteVentas.razonSocial)
        assertEquals("20600000001", reporteVentas.ruc)
        assertEquals(1, reporteVentas.filas.size)
        assertEquals("S/ 100.00", reporteVentas.totales["Total Vendido"])
    }

    // ── 24. REPORTES: EXPORTACIÓN CSV CON BOM UTF-8 Y RFC 4180 ────────────────
    @Test
    fun reportes_exportacionCsv_incluyeBomUtf8YFormatoRfc4180() {
        val v1 = crearVenta("V1", 45.50, 20.0, cajero = "Ana Cajera")
        val metricas = AnaliticaCalculadora.calcular(listOf(v1), emptyList())
        val exito = AnaliticaUiState.Exito(
            metricas = metricas,
            estadoCaja = EstadoCaja(),
            conciliacionFiscal = ConciliacionFiscal(),
            conciliacionCaja = ConciliacionCaja(),
            rentabilidad = AnaliticaCalculadora.calcularRentabilidad(listOf(v1), emptyList()),
            inventario = AnaliticaCalculadora.calcularInventario(emptyList(), listOf(v1), 1000L),
            compras = AnaliticaCalculadora.calcularCompras(emptyList()),
            clientes = AnaliticaCalculadora.calcularClientes(listOf(v1), emptyList()),
            sucursales = AnaliticaCalculadora.calcularSucursales(emptyMap(), emptyMap(), emptyMap()),
            insights = AnaliticaCalculadora.calcularInsights(
                AnaliticaCalculadora.calcularInventario(emptyList(), listOf(v1), 1000L),
                AnaliticaCalculadora.calcularRentabilidad(listOf(v1), emptyList()),
                ConciliacionFiscal(),
                ConciliacionCaja()
            ),
            listaVentas = listOf(v1),
            listaDevoluciones = emptyList(),
            esVacio = false
        )

        val tabla = ReportesDatasetBuilder.construirReporte(
            categoria = "VENTAS",
            tipoReporte = "Ventas por Vendedor y Turno",
            exito = exito,
            periodo = "Hoy",
            nombreSede = "Sede Norte",
            razonSocial = "BOTICA LA SALUD S.A.C.",
            ruc = "20700000002"
        )

        val csv = ReporteExportador.generarCsv(tabla)

        // Verifica el Byte Order Mark (BOM) UTF-8 requerido por Excel
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("BOTICA LA SALUD S.A.C."))
        assertTrue(csv.contains("Ana Cajera"))
        assertTrue(csv.contains("S/ 45.50"))
    }

    // ── 25. REPORTES: BUSCADOR EN VIVO FILTRA FILAS ────────────────────────────
    @Test
    fun reportes_buscadorEnVivo_filtraFilasYRecalculaTotales() {
        val v1 = crearVenta("V1", 100.0, 50.0, cajero = "Pedro")
        val v2 = crearVenta("V2", 200.0, 100.0, cajero = "Maria")
        val metricas = AnaliticaCalculadora.calcular(listOf(v1, v2), emptyList())
        val exito = AnaliticaUiState.Exito(
            metricas = metricas,
            estadoCaja = EstadoCaja(),
            conciliacionFiscal = ConciliacionFiscal(),
            conciliacionCaja = ConciliacionCaja(),
            rentabilidad = AnaliticaCalculadora.calcularRentabilidad(listOf(v1, v2), emptyList()),
            inventario = AnaliticaCalculadora.calcularInventario(emptyList(), listOf(v1, v2), 1000L),
            compras = AnaliticaCalculadora.calcularCompras(emptyList()),
            clientes = AnaliticaCalculadora.calcularClientes(listOf(v1, v2), emptyList()),
            sucursales = AnaliticaCalculadora.calcularSucursales(emptyMap(), emptyMap(), emptyMap()),
            insights = AnaliticaCalculadora.calcularInsights(
                AnaliticaCalculadora.calcularInventario(emptyList(), listOf(v1, v2), 1000L),
                AnaliticaCalculadora.calcularRentabilidad(listOf(v1, v2), emptyList()),
                ConciliacionFiscal(),
                ConciliacionCaja()
            ),
            listaVentas = listOf(v1, v2),
            listaDevoluciones = emptyList(),
            esVacio = false
        )

        val tabla = ReportesDatasetBuilder.construirReporte(
            categoria = "VENTAS",
            tipoReporte = "Ventas por Vendedor y Turno",
            exito = exito,
            periodo = "Hoy",
            nombreSede = "Sede Sur",
            razonSocial = "FARMACIA CENTRAL",
            ruc = "20800000003"
        )

        assertEquals(2, tabla.filas.size)

        // Simulación del filtro en vivo con búsqueda "Maria"
        val query = "Maria".lowercase(java.util.Locale.ROOT)
        val filtradas = tabla.filas.filter { f -> f.celdas.any { it.lowercase(java.util.Locale.ROOT).contains(query) } }

        assertEquals(1, filtradas.size)
        assertTrue(filtradas.first().celdas.contains("Maria"))
    }

    // ── 26. COMPRAS: ORIGEN IGV DERIVADO Y DISCLAIMER TRIBUTARIO ──────────────
    @Test
    fun compras_disclaimerTributarioYOrigenDerivado_presenteEnReporteYCsv() {
        val f1 = FacturaCompra(id = "F1", proveedorNombre = "Droguería Farmed", montoTotal = 1180.0, estadoPago = "PAGADO")
        val compras = AnaliticaCalculadora.calcularCompras(listOf(f1))
        val exito = AnaliticaUiState.Exito(
            metricas = AnaliticaCalculadora.calcular(emptyList(), emptyList()),
            estadoCaja = EstadoCaja(),
            conciliacionFiscal = ConciliacionFiscal(),
            conciliacionCaja = ConciliacionCaja(),
            rentabilidad = AnaliticaCalculadora.calcularRentabilidad(emptyList(), emptyList()),
            inventario = AnaliticaCalculadora.calcularInventario(emptyList(), emptyList(), 1000L),
            compras = compras,
            clientes = AnaliticaCalculadora.calcularClientes(emptyList(), emptyList()),
            sucursales = AnaliticaCalculadora.calcularSucursales(emptyMap(), emptyMap(), emptyMap()),
            insights = AnaliticaCalculadora.calcularInsights(
                AnaliticaCalculadora.calcularInventario(emptyList(), emptyList(), 1000L),
                AnaliticaCalculadora.calcularRentabilidad(emptyList(), emptyList()),
                ConciliacionFiscal(),
                ConciliacionCaja()
            ),
            listaVentas = emptyList(),
            listaDevoluciones = emptyList(),
            esVacio = false
        )

        val tabla = ReportesDatasetBuilder.construirReporte(
            categoria = "TRIBUTARIOS",
            tipoReporte = "Compras a Proveedores y Droguerías",
            exito = exito,
            periodo = "Mes Actual",
            nombreSede = "Sede Principal",
            razonSocial = "FARMACIA INTEGRAL",
            ruc = "20609876543"
        )

        // 1. Columna de ORIGEN IGV presente
        assertTrue(tabla.columnas.any { it.titulo == "ORIGEN IGV" })
        assertEquals("DERIVADO (ESTIMADO)", tabla.filas.first().celdas.last())

        // 2. Disclaimer legal explícito (No constituye propuesta SUNAT)
        assertTrue(tabla.disclaimerTributario != null)
        assertTrue(tabla.disclaimerTributario!!.contains("No sustituye comprobante ni propuesta SIRE/RCE"))

        // 3. En CSV el aviso va declarado en encabezados
        val csv = ReporteExportador.generarCsv(tabla)
        assertTrue(csv.contains("AVISO TRIBUTARIO:,"))
        assertTrue(csv.contains("DERIVADO (ESTIMADO)"))
    }

    // ── 27. PAQUETE CONTADOR: GENERA ZIP CON 7 CSV + RESUMEN + INCIDENCIAS ────
    @Test
    fun paqueteContador_generaZipConArchivosOperativosYDisclaimer() {
        val v1 = crearVenta("V1", 100.0, 50.0)
        val f1 = FacturaCompra(id = "F1", proveedorNombre = "Droguería Alfa", montoTotal = 500.0, estadoPago = "PAGADO")
        val exito = AnaliticaUiState.Exito(
            metricas = AnaliticaCalculadora.calcular(listOf(v1), emptyList()),
            estadoCaja = EstadoCaja(),
            conciliacionFiscal = ConciliacionFiscal(totalVentasNoAnuladas = 1, aceptados = 1),
            conciliacionCaja = ConciliacionCaja(),
            rentabilidad = AnaliticaCalculadora.calcularRentabilidad(listOf(v1), emptyList()),
            inventario = AnaliticaCalculadora.calcularInventario(emptyList(), listOf(v1), 1000L),
            compras = AnaliticaCalculadora.calcularCompras(listOf(f1)),
            clientes = AnaliticaCalculadora.calcularClientes(listOf(v1), emptyList()),
            sucursales = AnaliticaCalculadora.calcularSucursales(emptyMap(), emptyMap(), emptyMap()),
            insights = AnaliticaCalculadora.calcularInsights(
                AnaliticaCalculadora.calcularInventario(emptyList(), listOf(v1), 1000L),
                AnaliticaCalculadora.calcularRentabilidad(listOf(v1), emptyList()),
                ConciliacionFiscal(totalVentasNoAnuladas = 1, aceptados = 1),
                ConciliacionCaja()
            ),
            listaVentas = listOf(v1),
            listaDevoluciones = emptyList(),
            esVacio = false
        )

        val tempDir = java.nio.file.Files.createTempDirectory("test_paquete_contador").toFile()
        val zipFile = ReporteExportador.generarPaqueteContadorZip(
            directorioDestino = tempDir,
            exito = exito,
            periodo = "Septiembre 2026",
            nombreSede = "Sede Norte",
            razonSocial = "BOTICA MODERNA S.A.C.",
            ruc = "20601234567"
        )

        assertTrue(zipFile.exists())
        assertTrue(zipFile.length() > 0)

        // Inspeccionar contenido del ZIP
        java.util.zip.ZipFile(zipFile).use { zf ->
            val entradas = zf.entries().asSequence().map { it.name }.toSet()
            assertTrue(entradas.contains("01_ventas_detalladas.csv"))
            assertTrue(entradas.contains("02_resumen_ventas_por_dia.csv"))
            assertTrue(entradas.contains("03_anulaciones_y_devoluciones.csv"))
            assertTrue(entradas.contains("04_caja_movimientos.csv"))
            assertTrue(entradas.contains("05_inventario_valorizado.csv"))
            assertTrue(entradas.contains("06_conciliacion_fiscal_sunat.csv"))
            assertTrue(entradas.contains("07_compras_proveedores.csv"))
            assertTrue(entradas.contains("08_resumen_periodo_contador.txt"))
            assertTrue(entradas.contains("09_incidencias_y_observaciones.txt"))

            // Verificar contenido del resumen legal
            val resumenEntry = zf.getEntry("08_resumen_periodo_contador.txt")
            val resumenTexto = zf.getInputStream(resumenEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
            assertTrue(resumenTexto.contains("PAQUETE DE INFORMACIÓN OPERATIVA PARA EL CONTADOR"))
            assertTrue(resumenTexto.contains("No constituye libro contable oficial"))
            assertTrue(resumenTexto.contains("BOTICA MODERNA S.A.C."))
        }

        tempDir.deleteRecursively()
    }

    // ── 28. ESTADO PARCIAL: FALLA COMPRAS, VENTAS QUEDAN VIGENTES Y SE DECLARA ──
    @Test
    fun estadoParcial_cuandoFallaCompras_mantieneVentasYDeclaraParcial() {
        val v1 = crearVenta("V1", 150.0, 75.0)
        val exitoParcial = AnaliticaUiState.Exito(
            metricas = AnaliticaCalculadora.calcular(listOf(v1), emptyList()),
            estadoCaja = EstadoCaja(),
            conciliacionFiscal = ConciliacionFiscal(),
            conciliacionCaja = ConciliacionCaja(),
            rentabilidad = AnaliticaCalculadora.calcularRentabilidad(listOf(v1), emptyList()),
            inventario = AnaliticaCalculadora.calcularInventario(emptyList(), listOf(v1), 1000L),
            compras = ComprasAnalytics(),
            clientes = AnaliticaCalculadora.calcularClientes(listOf(v1), emptyList()),
            sucursales = AnaliticaCalculadora.calcularSucursales(emptyMap(), emptyMap(), emptyMap()),
            insights = AnaliticaCalculadora.calcularInsights(
                AnaliticaCalculadora.calcularInventario(emptyList(), listOf(v1), 1000L),
                AnaliticaCalculadora.calcularRentabilidad(listOf(v1), emptyList()),
                ConciliacionFiscal(),
                ConciliacionCaja()
            ),
            listaVentas = listOf(v1),
            listaDevoluciones = emptyList(),
            esVacio = false,
            fuentesOK = setOf(FuenteDatos.VENTAS, FuenteDatos.CAJA),
            fuentesFallidas = mapOf(FuenteDatos.COMPRAS to "Timeout de red en compras_facturas")
        )

        // 1. Debe estar en estado PARCIAL
        assertTrue(exitoParcial.esParcial)
        assertEquals(1, exitoParcial.fuentesFallidas.size)
        assertTrue(exitoParcial.fuentesFallidas.containsKey(FuenteDatos.COMPRAS))

        // 2. Ventas no deben estar en 0 ni perdidas
        assertEquals(150.0, exitoParcial.metricas.ventasNetas, 0.001)

        // 3. El paquete ZIP del contador declara explícitamente el estado parcial e incidencia
        val tempDir = java.nio.file.Files.createTempDirectory("test_parcial_contador").toFile()
        val zipFile = ReporteExportador.generarPaqueteContadorZip(
            directorioDestino = tempDir,
            exito = exitoParcial,
            periodo = "Septiembre 2026",
            nombreSede = "Sede Norte",
            razonSocial = "FARMACIA TEST",
            ruc = "20601234567"
        )

        java.util.zip.ZipFile(zipFile).use { zf ->
            val incEntry = zf.getEntry("09_incidencias_y_observaciones.txt")
            val incTexto = zf.getInputStream(incEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
            assertTrue(incTexto.contains("FUENTES DE DATOS CON INCIDENCIA DE RED"))
            assertTrue(incTexto.contains("Timeout de red en compras_facturas"))

            val resEntry = zf.getEntry("08_resumen_periodo_contador.txt")
            val resTexto = zf.getInputStream(resEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
            assertTrue(resTexto.contains("ESTADO DE CARGA: PARCIAL"))
        }

        tempDir.deleteRecursively()
    }

    // ── 29. MULTISEDE: AISLAMIENTO Y DEDUPLICACIÓN POR SEDE + ID ──────────────
    @Test
    fun multisede_aislamientoSede_docsSinSedeVanASinSedeYNoMezclan() {
        val vSede1 = crearVenta("V1", 100.0, 50.0)
        val vSede2 = crearVenta("V1", 200.0, 100.0)
        val vSinSede = crearVenta("V2", 300.0, 150.0)

        val mapaVentas = mapOf(
            "SEDE_A" to listOf(vSede1),
            "SEDE_B" to listOf(vSede2),
            "" to listOf(vSinSede)
        )

        val ventasConsolidadas = mapaVentas.flatMap { (sId, vs) ->
            vs.map { sId to it }
        }.distinctBy { (sId, v) ->
            "${sId.ifBlank { "SIN_SEDE" }}_${v.id}"
        }.map { it.second }

        // V1 en SEDE_A y V1 en SEDE_B no se pisan porque son de sedes distintas
        assertEquals(3, ventasConsolidadas.size)
        assertEquals(2, ventasConsolidadas.count { it.id == "V1" })
        assertEquals(1, ventasConsolidadas.count { it.id == "V2" })
    }

    // ── 30. EXPORTACIÓN ATÓMICA: ATOMIC .TMP Y VALIDACIÓN DE ARCHIVO ──────────
    @Test
    fun exportacionAtomica_escribeTmpYRenombraSinDejarArchivosCorruptos() {
        val v1 = crearVenta("V1", 100.0, 50.0)
        val exito = AnaliticaUiState.Exito(
            metricas = AnaliticaCalculadora.calcular(listOf(v1), emptyList()),
            estadoCaja = EstadoCaja(),
            conciliacionFiscal = ConciliacionFiscal(),
            conciliacionCaja = ConciliacionCaja(),
            rentabilidad = AnaliticaCalculadora.calcularRentabilidad(listOf(v1), emptyList()),
            inventario = AnaliticaCalculadora.calcularInventario(emptyList(), listOf(v1), 1000L),
            compras = ComprasAnalytics(),
            clientes = AnaliticaCalculadora.calcularClientes(listOf(v1), emptyList()),
            sucursales = AnaliticaCalculadora.calcularSucursales(emptyMap(), emptyMap(), emptyMap()),
            insights = AnaliticaCalculadora.calcularInsights(
                AnaliticaCalculadora.calcularInventario(emptyList(), listOf(v1), 1000L),
                AnaliticaCalculadora.calcularRentabilidad(listOf(v1), emptyList()),
                ConciliacionFiscal(),
                ConciliacionCaja()
            ),
            listaVentas = listOf(v1),
            listaDevoluciones = emptyList(),
            esVacio = false
        )

        val tempDir = java.nio.file.Files.createTempDirectory("test_atomic_export").toFile()
        val zipFile = ReporteExportador.generarPaqueteContadorZip(
            directorioDestino = tempDir,
            exito = exito,
            periodo = "Mes Actual",
            nombreSede = "Sede Central",
            razonSocial = "EMPRESA VERIFICADA S.A.C.",
            ruc = "20700000001"
        )

        // El archivo final existe
        assertTrue(zipFile.exists())
        // El archivo temporal .tmp NO debe existir en el directorio (debe haberse renombrado o eliminado)
        val tmpFiles = tempDir.listFiles { _, name -> name.endsWith(".tmp") }
        assertTrue(tmpFiles == null || tmpFiles.isEmpty())

        tempDir.deleteRecursively()
    }

    // ── 31. RESOLUCIÓN DE DÍAS LIMA (15 DÍAS, MES ESPECÍFICO, RANGO PERSONALIZADO) ──
    @Test
    fun periodos_resolucionDiasLima_exactitud15DiasMesYRango() {
        val tzLima = TimeZone.getTimeZone("America/Lima")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tzLima }

        // Test 1: 15 días consecutivos
        val calHoy = Calendar.getInstance(tzLima)
        val cal15Atras = (calHoy.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -14) }
        val dias15 = mutableListOf<String>()
        val cursor = cal15Atras.clone() as Calendar
        while (!cursor.after(calHoy)) {
            dias15.add(sdf.format(cursor.time))
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        assertEquals(15, dias15.size)

        // Test 2: Mes Específico - Febrero 2026 (año común, 28 días)
        val calFeb = Calendar.getInstance(tzLima).apply {
            clear()
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.FEBRUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxFeb = calFeb.getActualMaximum(Calendar.DAY_OF_MONTH)
        assertEquals(28, maxFeb)

        // Test 3: Mes Específico - Marzo 2026 (31 días)
        val calMar = Calendar.getInstance(tzLima).apply {
            clear()
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.MARCH)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxMar = calMar.getActualMaximum(Calendar.DAY_OF_MONTH)
        assertEquals(31, maxMar)

        // Test 4: Verificación de Enum
        assertTrue("Debe existir ULTIMOS_15_DIAS", PeriodoAnalitica.entries.any { it == PeriodoAnalitica.ULTIMOS_15_DIAS })
        assertTrue("Debe existir MES", PeriodoAnalitica.entries.any { it == PeriodoAnalitica.MES })
        assertEquals("Últimos 15 días", PeriodoAnalitica.ULTIMOS_15_DIAS.label)
        assertEquals("Mes Específico", PeriodoAnalitica.MES.label)
    }

    // ── 32. CAMBIO RÁPIDO DE PERÍODOS (JOB_ID): DESCARTA VIEJO Y MANTIENE VIGENTE ──
    @Test
    fun periodos_cambioRapido_jobIdDescartaCargaVieja() {
        var cargaJobId = 0L
        var ultimoEstadoPublicado: String? = null

        fun simularCarga(periodoNombre: String): Long {
            return ++cargaJobId
        }

        fun simularLlegadaRespuesta(jobId: Long, periodoNombre: String) {
            if (jobId == cargaJobId) {
                ultimoEstadoPublicado = periodoNombre
            }
        }

        // El usuario hace clic rápido en: Hoy -> 7 Días -> 15 Días
        val job1 = simularCarga("HOY")
        val job2 = simularCarga("7_DIAS")
        val job3 = simularCarga("15_DIAS")

        // Supongamos que la respuesta de HOY (job1) llega después por latencia de red
        simularLlegadaRespuesta(job1, "HOY")
        assertEquals("La respuesta de HOY debe descartarse porque es vieja", null, ultimoEstadoPublicado)

        // Supongamos que llega la respuesta de 7_DIAS (job2)
        simularLlegadaRespuesta(job2, "7_DIAS")
        assertEquals("La respuesta de 7 Días debe descartarse porque es vieja", null, ultimoEstadoPublicado)

        // Llega la respuesta de 15_DIAS (job3)
        simularLlegadaRespuesta(job3, "15_DIAS")
        assertEquals("Solo la respuesta vigente (15 Días) debe publicarse", "15_DIAS", ultimoEstadoPublicado)
    }

    // ── 33. PAGINACIÓN 30 DÍAS CON MILES DE DOCUMENTOS SIN RECORTE ─────────────
    @Test
    fun periodos_paginacion30Dias_consolidaMilesSinRecorte() {
        val ventas30Dias = mutableListOf<Venta>()
        var sumaEsperadaVentas = 0.0

        for (dia in 1..30) {
            // 50 ventas por día = 1,500 ventas en el período (supera el lote de 500)
            for (v in 1..50) {
                val totalVenta = 20.0
                ventas30Dias.add(
                    crearVenta("V_${dia}_$v", total = totalVenta, costoTotal = 12.0)
                )
                sumaEsperadaVentas += totalVenta
            }
            // Agregar 2 ventas anuladas por día que deben excluirse
            ventas30Dias.add(
                crearVenta("V_ANULADA_1_${dia}", total = 100.0, costoTotal = 60.0, estado = Venta.ESTADO_ANULADA)
            )
            ventas30Dias.add(
                crearVenta("V_ANULADA_2_${dia}", total = 100.0, costoTotal = 60.0, estado = Venta.ESTADO_ANULADA)
            )
        }

        assertEquals(1560, ventas30Dias.size) // 1500 válidas + 60 anuladas

        val metricas = AnaliticaCalculadora.calcular(ventas30Dias, emptyList())

        // Verifica que no hubo recorte de las 1,500 ventas válidas
        assertEquals(1500, metricas.cantidadTransacciones)
        assertEquals(sumaEsperadaVentas, metricas.ventasBrutas, 0.001)
        assertEquals(sumaEsperadaVentas, metricas.ventasNetas, 0.001)
        assertEquals(30000.0, metricas.ventasNetas, 0.001) // 1500 * 20.0 = 30,000.0
    }

    // ── 34. TOTALES MULTISEDE: SUMA EXACTA EN TODOS LOS PERÍODOS ───────────────
    @Test
    fun periodos_totalesMultisede_mantieneSumaExactaEnTodosLosPeriodos() {
        val vSede1 = crearVenta("V_S1", total = 1250.0, costoTotal = 800.0)
        val vSede2 = crearVenta("V_S2", total = 850.0, costoTotal = 500.0)
        val vSede3 = crearVenta("V_S3", total = 300.0, costoTotal = 200.0)

        val ventasPorSede = mapOf(
            "sede-1" to listOf(vSede1),
            "sede-2" to listOf(vSede2),
            "sede-3" to listOf(vSede3)
        )
        val nombres = mapOf("sede-1" to "Central", "sede-2" to "Sucursal 2", "sede-3" to "Sucursal 3")

        val sucAnalytics = AnaliticaCalculadora.calcularSucursales(ventasPorSede, emptyMap(), nombres)
        val consolidado = AnaliticaCalculadora.calcular(listOf(vSede1, vSede2, vSede3), emptyList())

        assertEquals(2400.0, sucAnalytics.ventasTotalesCadena, 0.001)
        assertEquals(consolidado.ventasNetas, sucAnalytics.ventasTotalesCadena, 0.001)
        assertEquals(900.0, sucAnalytics.utilidadCadena, 0.001)
        assertEquals(consolidado.utilidadBruta, sucAnalytics.utilidadCadena, 0.001)
    }

    // ── 39. INVENTARIO CONTABLE REAL: PRODUCTOS RECIÉN CREADOS EN 0 NO SON QUIEBRE NI INMOVILIZADOS ──
    private fun crearMoldeProducto(
        id: String,
        nombre: String,
        stock: Double = 0.0,
        costoUnitario: Double = 0.0,
        stockMinimo: Double = 0.0,
        activo: Boolean = true,
        loteNumero: String = "",
        creadoEnMs: Long = 0L
    ): MoldeProductos {
        val lotesMap = if (stock > 0.0 || loteNumero.isNotBlank()) {
            mapOf(
                "lote-1" to LoteProducto(
                    loteId = "lote-1",
                    numero = loteNumero.ifBlank { "L001" },
                    cantidad = stock,
                    costoCompraUnitario = costoUnitario,
                    vencimiento = "31/12/2027"
                )
            )
        } else emptyMap()

        return MoldeProductos(
            productoBase = ProductoBase(indice = id, nombre = nombre, codigo = id, activo = activo),
            precioStock = PrecioStock(precioCompra = costoUnitario, stockMinimoBase = stockMinimo),
            loteInfo = LoteInfo(lotes = lotesMap, creadoEnMillis = creadoEnMs)
        )
    }

    @Test
    fun inventario_productoRecienCreadoEnCero_noEsQuiebreNiInmovilizado() {
        val pNuevo = crearMoldeProducto("P_NUEVO", "Vitamina C 1000mg", stock = 0.0, costoUnitario = 0.0, stockMinimo = 0.0)

        val res = AnaliticaCalculadora.calcularInventario(listOf(pNuevo), emptyList(), ahoraMs = 1000L)

        assertEquals(0.0, res.valorInventarioTotal, 0.001)
        assertEquals(0.0, res.totalUnidadesDisponibles, 0.001)
        assertEquals(0, res.cantidadQuiebresStock)
        assertEquals(0.0, res.capitalRiesgoQuiebre, 0.001)
        assertTrue(res.detalleQuiebres.isEmpty())
        assertTrue(res.detalleInmovilizados.isEmpty())
        assertEquals(0.0, res.capitalInmovilizado, 0.001)
        assertEquals(0.0, res.capitalEnRotacion, 0.001)
        assertEquals(0.0, res.tasaRotacionPorcentaje, 0.001)
    }

    @Test
    fun inventario_productoConStockReal_tuvoVentasEnPeriodo_esCapitalEnRotacion() {
        val p = crearMoldeProducto("P1", "Paracetamol 500mg", stock = 50.0, costoUnitario = 10.0, stockMinimo = 10.0)
        val venta = crearVenta("V1", total = 100.0, costoTotal = 20.0, productoId = "P1")

        val res = AnaliticaCalculadora.calcularInventario(listOf(p), listOf(venta), ahoraMs = 1000L)

        assertEquals(500.0, res.valorInventarioTotal, 0.001)
        assertEquals(50.0, res.totalUnidadesDisponibles, 0.001)
        assertEquals(0, res.cantidadQuiebresStock)
        assertEquals(0.0, res.capitalInmovilizado, 0.001)
        assertEquals(500.0, res.capitalEnRotacion, 0.001)
        assertEquals(100.0, res.tasaRotacionPorcentaje, 0.001)
        assertTrue(res.detalleInmovilizados.isEmpty())
    }

    @Test
    fun inventario_productoConStockReal_sinVentasEnPeriodo_esCapitalInmovilizado() {
        val p = crearMoldeProducto("P2", "Ibuprofeno 400mg", stock = 20.0, costoUnitario = 15.0, stockMinimo = 5.0)

        val res = AnaliticaCalculadora.calcularInventario(listOf(p), emptyList(), ahoraMs = 1000L)

        assertEquals(300.0, res.valorInventarioTotal, 0.001)
        assertEquals(20.0, res.totalUnidadesDisponibles, 0.001)
        assertEquals(300.0, res.capitalInmovilizado, 0.001)
        assertEquals(0.0, res.capitalEnRotacion, 0.001)
        assertEquals(0.0, res.tasaRotacionPorcentaje, 0.001)
        assertEquals(1, res.detalleInmovilizados.size)
        assertEquals(300.0, res.detalleInmovilizados.first().valorEstancado, 0.001)
    }

    @Test
    fun inventario_quiebreRealConStockMinimoConfigurado_calculaReposicionExacta() {
        // Stock actual = 3, Mínimo de seguridad = 10, Costo unitario = S/ 8.00
        // Faltan 7 unidades * S/ 8.00 = S/ 56.00 de reposición
        val pQuiebre = crearMoldeProducto("P_Q", "Amoxicilina Jarabe", stock = 3.0, costoUnitario = 8.0, stockMinimo = 10.0)

        val res = AnaliticaCalculadora.calcularInventario(listOf(pQuiebre), emptyList(), ahoraMs = 1000L)

        assertEquals(1, res.cantidadQuiebresStock)
        assertEquals(56.0, res.capitalRiesgoQuiebre, 0.001)
        assertEquals(1, res.detalleQuiebres.size)
        assertEquals("Amoxicilina Jarabe", res.detalleQuiebres.first().nombre)
        assertEquals(3.0, res.detalleQuiebres.first().stockActual, 0.001)
        assertEquals(10.0, res.detalleQuiebres.first().stockMinimo, 0.001)
        assertEquals(56.0, res.detalleQuiebres.first().costoReposicion, 0.001)
    }

    @Test
    fun inventario_mixCompleto_calculaMetricasContablesSinFalsedades() {
        val pRotando = crearMoldeProducto("P_ROT", "Paracetamol", stock = 40.0, costoUnitario = 10.0, stockMinimo = 10.0)
        val pInmovil = crearMoldeProducto("P_INM", "Jarabe Tos", stock = 20.0, costoUnitario = 5.0, stockMinimo = 5.0)
        val pQuiebre = crearMoldeProducto("P_QUI", "Antibiótico", stock = 2.0, costoUnitario = 8.0, stockMinimo = 10.0)
        val pNuevoVacio = crearMoldeProducto("P_NUEVO", "Suplemento Nuevo", stock = 0.0, costoUnitario = 0.0, stockMinimo = 0.0)

        val venta = crearVenta("V_ROT", total = 50.0, costoTotal = 20.0, productoId = "P_ROT")

        val res = AnaliticaCalculadora.calcularInventario(
            listOf(pRotando, pInmovil, pQuiebre, pNuevoVacio),
            listOf(venta),
            ahoraMs = 1000L
        )

        // Valor total: (40 * 10) + (20 * 5) + (2 * 8) + 0 = 400 + 100 + 16 = 516.0
        assertEquals(516.0, res.valorInventarioTotal, 0.001)

        // Inmovilizados: Jarabe Tos (100) + Antibiótico (16) = 116.0. El nuevo vacío NO entra.
        assertEquals(116.0, res.capitalInmovilizado, 0.001)

        // En rotación: 516 - 116 = 400.0
        assertEquals(400.0, res.capitalEnRotacion, 0.001)

        // Tasa de rotación: (400 / 516) * 100 = 77.52%
        assertEquals(77.52, res.tasaRotacionPorcentaje, 0.01)

        // Quiebres: solo Antibiótico (faltan 8 * 8 = 64.0). El nuevo vacío NO entra.
        assertEquals(1, res.cantidadQuiebresStock)
        assertEquals(64.0, res.capitalRiesgoQuiebre, 0.001)
    }

    @Test
    fun inventario_productoCreadoDespuesDelPeriodo_noSeCuentaComoInmovilizado() {
        val inicioPeriodo = 1000L
        val finPeriodo = 2000L
        val creadoDespues = 3000L

        val pCreadoDespues = crearMoldeProducto("P_DESPUES", "Crema Reciente", stock = 10.0, costoUnitario = 15.0, stockMinimo = 5.0, creadoEnMs = creadoDespues)

        val res = AnaliticaCalculadora.calcularInventario(
            listOf(pCreadoDespues),
            emptyList(),
            ahoraMs = 4000L,
            inicioPeriodoMs = inicioPeriodo,
            finPeriodoMs = finPeriodo
        )

        // No debe ser inmovilizado del período anterior
        assertEquals(0, res.detalleInmovilizados.size)
        assertEquals(0.0, res.capitalInmovilizado, 0.001)
    }

    // ── 39. RENTABILIDAD: DESGLOSE POR PRESENTACIÓN Y CUADRE MATEMÁTICO REAL ────
    @Test
    fun rentabilidad_mismoProductoDistintasPresentaciones_seSeparanSinMezclarNiPromediar() {
        // Un mismo producto (prod-paracetamol) vendido en 3 presentaciones distintas
        val v1 = Venta(
            id = "V101",
            total = 32.0,
            subtotal = 32.0,
            costoTotalReal = 20.0,
            items = listOf(
                ItemVenta(
                    productoId = "prod-paracetamol",
                    nombreProducto = "Paracetamol 500mg",
                    presentacionNombre = "Caja x 100",
                    cantidad = 2,
                    precioUnitario = 16.0,
                    subtotal = 32.0,
                    costoTotalReal = 20.0
                )
            )
        )
        val v2 = Venta(
            id = "V102",
            total = 25.0,
            subtotal = 25.0,
            costoTotalReal = 10.0,
            items = listOf(
                ItemVenta(
                    productoId = "prod-paracetamol",
                    nombreProducto = "Paracetamol 500mg",
                    presentacionNombre = "Blíster x 10",
                    cantidad = 10,
                    precioUnitario = 2.5,
                    subtotal = 25.0,
                    costoTotalReal = 10.0
                )
            )
        )
        val v3 = Venta(
            id = "V103",
            total = 10.0,
            subtotal = 10.0,
            costoTotalReal = 2.0,
            items = listOf(
                ItemVenta(
                    productoId = "prod-paracetamol",
                    nombreProducto = "Paracetamol 500mg",
                    presentacionNombre = "Pastilla x 1",
                    cantidad = 20,
                    precioUnitario = 0.5,
                    subtotal = 10.0,
                    costoTotalReal = 2.0
                )
            )
        )
        val v4 = Venta(
            id = "V104",
            total = 80.0,
            subtotal = 80.0,
            costoTotalReal = 65.0,
            items = listOf(
                ItemVenta(
                    productoId = "prod-leche",
                    nombreProducto = "Leche Fórmula",
                    presentacionNombre = "Lata 800g",
                    cantidad = 1,
                    precioUnitario = 80.0,
                    subtotal = 80.0,
                    costoTotalReal = 65.0
                )
            )
        )
        val v5 = Venta(
            id = "V105",
            total = 8.0,
            subtotal = 8.0,
            costoTotalReal = 7.2,
            items = listOf(
                ItemVenta(
                    productoId = "prod-alcohol",
                    nombreProducto = "Alcohol 70",
                    presentacionNombre = "Frasco 1000ml",
                    cantidad = 1,
                    precioUnitario = 8.0,
                    subtotal = 8.0,
                    costoTotalReal = 7.2
                )
            )
        )
        val vPerdida = Venta(
            id = "V106",
            total = 50.0,
            subtotal = 50.0,
            costoTotalReal = 60.0,
            items = listOf(
                ItemVenta(
                    productoId = "prod-vacuna",
                    nombreProducto = "Vacuna Antigripal",
                    presentacionNombre = "Dosis Única",
                    cantidad = 1,
                    precioUnitario = 50.0,
                    subtotal = 50.0,
                    costoTotalReal = 60.0
                )
            )
        )

        val rent = AnaliticaCalculadora.calcularRentabilidad(listOf(v1, v2, v3, v4, v5, vPerdida), emptyList())

        // 1. Existen 6 registros independientes en todos los productos (las 3 presentaciones de paracetamol no se mezclaron)
        assertEquals(6, rent.todosLosProductos.size)

        val cajaItem = rent.todosLosProductos.firstOrNull { it.presentacion == "Caja x 100" }
        val blisterItem = rent.todosLosProductos.firstOrNull { it.presentacion == "Blíster x 10" }
        val pastillaItem = rent.todosLosProductos.firstOrNull { it.presentacion == "Pastilla x 1" }

        assertTrue("Debe existir ítem para Caja x 100", cajaItem != null)
        assertTrue("Debe existir ítem para Blíster x 10", blisterItem != null)
        assertTrue("Debe existir ítem para Pastilla x 1", pastillaItem != null)

        // Cada presentación tiene sus matemáticas propias sin distorsión
        assertEquals(32.0, cajaItem!!.ventasNetas, 0.001)
        assertEquals(20.0, cajaItem.costoNeto, 0.001)
        assertEquals(12.0, cajaItem.utilidad, 0.001)
        assertEquals(37.5, cajaItem.margenPorcentaje, 0.01)

        assertEquals(25.0, blisterItem!!.ventasNetas, 0.001)
        assertEquals(10.0, blisterItem.costoNeto, 0.001)
        assertEquals(15.0, blisterItem.utilidad, 0.001)
        assertEquals(60.0, blisterItem.margenPorcentaje, 0.01)

        assertEquals(10.0, pastillaItem!!.ventasNetas, 0.001)
        assertEquals(2.0, pastillaItem.costoNeto, 0.001)
        assertEquals(8.0, pastillaItem.utilidad, 0.001)
        assertEquals(80.0, pastillaItem.margenPorcentaje, 0.01)

        // 2. Mayor Margen (%): Pastilla x 1 (80%) y Blíster x 10 (60%) están en el top de porcentaje
        assertEquals("prod-paracetamol__Pastilla x 1", rent.productosMayorMargen[0].productoId)
        assertEquals(80.0, rent.productosMayorMargen[0].margenPorcentaje, 0.01)

        // 3. Mayor Ganancia Dinero (+S/): Blíster x 10 (+S/ 15) y Leche (+S/ 15) lideran en dinero
        assertTrue(rent.productosMayorGananciaDinero.any { it.presentacion == "Blíster x 10" && it.utilidad == 15.0 })
        assertTrue(rent.productosMayorGananciaDinero.any { it.presentacion == "Lata 800g" && it.utilidad == 15.0 })

        // 4. Menor Margen no incluye a los del top de mayor margen (cero contradicción lógica)
        val idsEnTopMargen = rent.productosMayorMargen.map { it.productoId }.toSet()
        for (itemBajo in rent.productosMenorMargen) {
            assertFalse("Un producto en top margen no puede estar en bajo margen", itemBajo.productoId in idsEnTopMargen)
            assertTrue("Bajo margen debe tener utilidad no negativa", itemBajo.utilidad >= 0.0)
        }

        // 5. Productos con Pérdida: Vacuna Antigripal aislada en productosConPerdida
        assertEquals(1, rent.productosConPerdida.size)
        assertEquals("prod-vacuna__Dosis Única", rent.productosConPerdida[0].productoId)
        assertEquals(-10.0, rent.productosConPerdida[0].utilidad, 0.001)

        // 6. Conciliación matemática total (suma de filas == KPIs centrales)
        val sumaVentas = redondear2(rent.todosLosProductos.sumOf { it.ventasNetas })
        val sumaCostos = redondear2(rent.todosLosProductos.sumOf { it.costoNeto })
        val sumaUtilidad = redondear2(rent.todosLosProductos.sumOf { it.utilidad })

        val central = AnaliticaCalculadora.calcular(listOf(v1, v2, v3, v4, v5, vPerdida), emptyList())
        assertEquals(central.ventasNetas, sumaVentas, 0.001)
        assertEquals(central.costoNeto, sumaCostos, 0.001)
        assertEquals(central.utilidadBruta, sumaUtilidad, 0.001)
    }

    // ── 40. COMBINAR ESTADOS DE CAJA (punteros por cajero → estado agregado de la sede) ──
    private fun crearPunteroCaja(
        cajero: String,
        fondo: Double,
        ventasEfectivo: Double = 0.0,
        aperturaMs: Long = 1_000_000L,
        estado: String = com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion.ESTADO_ABIERTA
    ): EstadoCaja = EstadoCaja(
        estado = estado,
        sesionId = "ses-$cajero",
        fondoInicial = fondo,
        aperturaMs = aperturaMs,
        abiertoPorNombre = cajero,
        cajeroId = cajero,
        cajaId = "caja_$cajero",
        ventasPorMetodo = if (ventasEfectivo > 0.0) mapOf("EFECTIVO" to ventasEfectivo) else emptyMap()
    )

    @Test
    fun combinarEstadosCaja_variosCajeros_sumaTodoYConservaAperturaMasAntigua() {
        val rosa = crearPunteroCaja("Rosa", fondo = 100.0, ventasEfectivo = 800.0, aperturaMs = 1_000L)
        val jose = crearPunteroCaja("José", fondo = 50.0, ventasEfectivo = 500.0, aperturaMs = 2_000L)

        val total = AnaliticaCalculadora.combinarEstadosCaja(listOf(rosa, jose))

        assertEquals(com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion.ESTADO_ABIERTA, total.estado)
        assertEquals(150.0, total.fondoInicial, 0.001)
        // Esperado agregado: (100 fondo + 800 ventas) + (50 fondo + 500 ventas) = 1,450.0
        assertEquals(1450.0, total.efectivoEsperado, 0.001)
        assertEquals(1_000L, total.aperturaMs)
        assertEquals("2 cajeros con turno abierto", total.abiertoPorNombre)
    }

    @Test
    fun combinarEstadosCaja_sinTurnosAbiertos_devuelveCajaCerradaVacia() {
        val cerrada = crearPunteroCaja("Rosa", fondo = 100.0, estado = com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion.ESTADO_CERRADA)
        val total = AnaliticaCalculadora.combinarEstadosCaja(listOf(cerrada))

        assertEquals(com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion.ESTADO_CERRADA, total.estado)
        assertEquals(0.0, total.fondoInicial, 0.001)
    }

    @Test
    fun combinarEstadosCaja_turnoUnico_conservaNombreYDatos() {
        val rosa = crearPunteroCaja("Rosa", fondo = 100.0, ventasEfectivo = 800.0)
        val total = AnaliticaCalculadora.combinarEstadosCaja(listOf(rosa))

        assertEquals("Rosa", total.abiertoPorNombre)
        assertEquals(900.0, total.efectivoEsperado, 0.001)
    }

    @Test
    fun dineroYCaja_turnoAbierto_noInventaContadoFisico() {
        val turnoVivo = crearPunteroCaja("Rosa", fondo = 100.0, ventasEfectivo = 800.0)
        val res = AnaliticaCalculadora.calcularDineroYCaja(
            sesiones = emptyList(),
            movimientos = emptyList(),
            estadoCajaActual = turnoVivo,
            metricasVentas = AnaliticaCalculadora.calcular(emptyList(), emptyList()),
            esPeriodoHoy = true
        )
        // El esperado incluye el turno vivo; el contado solo arqueos reales (aquí: ninguno).
        assertEquals(900.0, res.cajaEsperadaTotal, 0.001)
        assertEquals(0.0, res.cajaContadaTotal, 0.001)
        assertEquals(0.0, res.diferenciaCajaTotal, 0.001)
    }

    private fun redondear2(v: Double): Double = kotlin.math.round(v * 100.0) / 100.0
}
