package com.app.administradorfarmadon.analitica_reportes.logica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.analitica_reportes.datos.AnaliticaRepository
import com.app.administradorfarmadon.analitica_reportes.modelo.*
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.FacturacionDocumento
import com.app.administradorfarmadon.ventas.compartido.modelo.MovimientoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AnaliticaViewModel(
    private val repository: AnaliticaRepository = AnaliticaRepository()
) : ViewModel() {

    companion object {
        val TIMEZONE_LIMA: TimeZone = TimeZone.getTimeZone("America/Lima")
        fun formatoDia(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TIMEZONE_LIMA }
    }

    private val _uiState = MutableStateFlow<AnaliticaUiState>(AnaliticaUiState.Loading)
    val uiState: StateFlow<AnaliticaUiState> = _uiState.asStateFlow()

    private val _periodoSeleccionado = MutableStateFlow(PeriodoAnalitica.HOY)
    val periodoSeleccionado: StateFlow<PeriodoAnalitica> = _periodoSeleccionado.asStateFlow()

    // Fechas personalizadas en ms
    private var customInicioMs: Long = 0L
    private var customFinMs: Long = 0L
    private val _rangoPersonalizado = MutableStateFlow<Pair<Long, Long>?>(null)
    val rangoPersonalizado: StateFlow<Pair<Long, Long>?> = _rangoPersonalizado.asStateFlow()

    // Mes específico seleccionado (año, mes 1..12)
    private var customAnio: Int = 0
    private var customMes: Int = 0
    private val _mesSeleccionado = MutableStateFlow<Pair<Int, Int>?>(null)
    val mesSeleccionado: StateFlow<Pair<Int, Int>?> = _mesSeleccionado.asStateFlow()

    // Drill-down de venta
    private val _ventaSeleccionada = MutableStateFlow<Venta?>(null)
    val ventaSeleccionada: StateFlow<Venta?> = _ventaSeleccionada.asStateFlow()

    private val _devolucionSeleccionada = MutableStateFlow<DevolucionVenta?>(null)
    val devolucionSeleccionada: StateFlow<DevolucionVenta?> = _devolucionSeleccionada.asStateFlow()

    private val _mensajeFeedback = MutableStateFlow<String?>(null)
    val mensajeFeedback: StateFlow<String?> = _mensajeFeedback.asStateFlow()

    private val _sucursalesDisponibles = MutableStateFlow<List<SucursalInfo>>(emptyList())
    val sucursalesDisponibles: StateFlow<List<SucursalInfo>> = _sucursalesDisponibles.asStateFlow()

    private val _sedeSeleccionada = MutableStateFlow("TODAS")
    val sedeSeleccionada: StateFlow<String> = _sedeSeleccionada.asStateFlow()

    private var activeJob: Job? = null
    private var cargaJobId: Long = 0L
    private var sucursalObserverJob: Job? = null
    private var comprasObserverJob: Job? = null

    init {
        observarCambiosDeSucursal()
        cargarPeriodo(PeriodoAnalitica.HOY)
    }

    private fun observarCambiosDeSucursal() {
        sucursalObserverJob?.cancel()
        sucursalObserverJob = viewModelScope.launch {
            var ultimaSucursal: String? = null
            SessionManager.sucursalFlow.collect { sucursal ->
                if (sucursal != ultimaSucursal) {
                    ultimaSucursal = sucursal
                    _sucursalesDisponibles.value = emptyList()
                    cargarPeriodo(_periodoSeleccionado.value)
                }
            }
        }
    }

    fun seleccionarPeriodo(periodo: PeriodoAnalitica) {
        _periodoSeleccionado.value = periodo
        cargarPeriodo(periodo)
    }

    fun seleccionarMesEspecifico(anio: Int, mes: Int) {
        customAnio = anio
        customMes = mes
        _mesSeleccionado.value = Pair(anio, mes)
        _periodoSeleccionado.value = PeriodoAnalitica.MES
        cargarPeriodo(PeriodoAnalitica.MES)
    }

    fun seleccionarRangoPersonalizado(inicioMs: Long, finMs: Long) {
        customInicioMs = inicioMs
        customFinMs = finMs
        _rangoPersonalizado.value = Pair(inicioMs, finMs)
        _periodoSeleccionado.value = PeriodoAnalitica.PERSONALIZADO
        cargarPeriodo(PeriodoAnalitica.PERSONALIZADO)
    }

    fun limpiarFeedback() {
        _mensajeFeedback.value = null
    }

    fun recargar() {
        cargarPeriodo(_periodoSeleccionado.value)
    }

    fun reintentarFuente(fuente: FuenteDatos) {
        recargar()
    }

    fun seleccionarSede(sedeId: String) {
        _sedeSeleccionada.value = sedeId
        cargarPeriodo(_periodoSeleccionado.value)
    }

    private fun cargarPeriodo(periodo: PeriodoAnalitica) {
        activeJob?.cancel()
        comprasObserverJob?.cancel()
        val miJobId = ++cargaJobId

        val currentExito = _uiState.value as? AnaliticaUiState.Exito
        if (currentExito != null) {
            _uiState.value = currentExito.copy(estaActualizando = true)
        } else {
            _uiState.value = AnaliticaUiState.Loading
        }

        val farmaciaId = SessionManager.clienteIdGarantizado
        if (farmaciaId.isBlank()) {
            _uiState.value = AnaliticaUiState.Error("Sesión no válida: farmacia no identificada (R1).")
            return
        }

        activeJob = viewModelScope.launch {
            try {
                if (_sucursalesDisponibles.value.isEmpty()) {
                    _sucursalesDisponibles.value = repository.obtenerSucursales(farmaciaId)
                }
                if (miJobId != cargaJobId) return@launch

                val sucursales = _sucursalesDisponibles.value
                val sedeSel = _sedeSeleccionada.value
                val hoyClave = formatoDia().format(Date(HoraServidor.ahoraMs()))

                val sedesIdsCompras = if (sedeSel == "TODAS") {
                    sucursales.map { it.id }.ifEmpty { listOf(SessionManager.sucursalIdEfectiva) }
                } else {
                    listOf(sedeSel)
                }

                val (inicioRangoMs, finRangoMs) = if (periodo == PeriodoAnalitica.HOY) {
                    val (i, _) = calcularInicioFinDia(Date(HoraServidor.ahoraMs()))
                    Pair(i, HoraServidor.ahoraMs())
                } else {
                    val (_, i, f) = resolverRango(periodo)
                    Pair(i, f)
                }

                iniciarObservadorCompras(farmaciaId, sedesIdsCompras, inicioRangoMs, finRangoMs, miJobId)

                if (periodo == PeriodoAnalitica.HOY) {
                    val sucursalEfectiva = if (sedeSel != "TODAS") sedeSel else SessionManager.sucursalIdEfectiva
                    val inicioHoyMs = calcularInicioFinDia(Date(HoraServidor.ahoraMs())).first

                    combine(
                        repository.observarVentasHoy(farmaciaId, sucursalEfectiva, hoyClave),
                        repository.observarDevolucionesHoy(farmaciaId, sucursalEfectiva, hoyClave),
                        repository.observarEstadoCaja(farmaciaId, sucursalEfectiva)
                    ) { ventas, devs, estadoCaja ->
                        Triple(ventas, devs, estadoCaja)
                    }.collect { (ventas, devs, estadoCaja) ->
                        if (miJobId != cargaJobId) return@collect

                        if (sedeSel == "TODAS" && sucursales.size > 1) {
                            val sedesRestantes = sucursales.map { it.id }.filter { it != sucursalEfectiva }
                            val ventasResto = repository.obtenerVentasMultisede(farmaciaId, sedesRestantes, listOf(hoyClave))
                            val devsResto = repository.obtenerDevolucionesMultisede(farmaciaId, sedesRestantes, listOf(hoyClave), inicioHoyMs, HoraServidor.ahoraMs())

                            val todasVentasPorSede = (ventasResto + (sucursalEfectiva to ventas)).toMutableMap()
                            val todasDevsPorSede = (devsResto + (sucursalEfectiva to devs)).toMutableMap()

                            procesarDatosMultisede(
                                farmaciaId = farmaciaId,
                                sucursales = sucursales,
                                ventasPorSede = todasVentasPorSede,
                                devsPorSede = todasDevsPorSede,
                                estadoCaja = estadoCaja,
                                inicioMs = inicioHoyMs,
                                finMs = HoraServidor.ahoraMs(),
                                jobId = miJobId
                            )
                        } else {
                            procesarDatos(
                                farmaciaId = farmaciaId,
                                sucursalId = sucursalEfectiva,
                                ventas = ventas,
                                devoluciones = devs,
                                estadoCaja = estadoCaja,
                                inicioMs = inicioHoyMs,
                                finMs = HoraServidor.ahoraMs(),
                                jobId = miJobId
                            )
                        }
                    }
                } else {
                    val (dias, inicioMs, finMs) = resolverRango(periodo)

                    if (sedeSel == "TODAS") {
                        val sedesIds = sucursales.map { it.id }.ifEmpty { listOf(SessionManager.sucursalIdEfectiva) }
                        val ventasPorSede = repository.obtenerVentasMultisede(farmaciaId, sedesIds, dias)
                        val devsPorSede = repository.obtenerDevolucionesMultisede(farmaciaId, sedesIds, dias, inicioMs, finMs)
                        if (miJobId != cargaJobId) return@launch

                        procesarDatosMultisede(
                            farmaciaId = farmaciaId,
                            sucursales = sucursales,
                            ventasPorSede = ventasPorSede,
                            devsPorSede = devsPorSede,
                            estadoCaja = EstadoCaja(),
                            inicioMs = inicioMs,
                            finMs = finMs,
                            jobId = miJobId
                        )
                    } else {
                        val ventas = repository.obtenerVentasPeriodo(farmaciaId, sedeSel, dias)
                        val devoluciones = repository.obtenerDevolucionesPeriodo(farmaciaId, sedeSel, dias, inicioMs, finMs)
                        if (miJobId != cargaJobId) return@launch

                        procesarDatos(
                            farmaciaId = farmaciaId,
                            sucursalId = sedeSel,
                            ventas = ventas,
                            devoluciones = devoluciones,
                            estadoCaja = EstadoCaja(),
                            inicioMs = inicioMs,
                            finMs = finMs,
                            jobId = miJobId
                        )
                    }
                }
            } catch (e: Exception) {
                if (miJobId == cargaJobId) {
                    _uiState.value = AnaliticaUiState.Error(e.message ?: "Error al cargar analítica del período")
                }
            }
        }
    }

    private suspend fun procesarDatos(
        farmaciaId: String,
        sucursalId: String,
        ventas: List<Venta>,
        devoluciones: List<DevolucionVenta>,
        estadoCaja: EstadoCaja,
        inicioMs: Long,
        finMs: Long,
        jobId: Long = 0L
    ) {
        val fuentesOK = mutableSetOf(FuenteDatos.VENTAS, FuenteDatos.CAJA)
        val fuentesFallidas = mutableMapOf<FuenteDatos, String>()

        // 1. Cálculo central determinista
        val metricas = AnaliticaCalculadora.calcular(ventas, devoluciones)

        // 2. Módulo Rentabilidad
        val rentabilidad = AnaliticaCalculadora.calcularRentabilidad(ventas, devoluciones)

        // 3. Módulo Clientes
        val clientes = AnaliticaCalculadora.calcularClientes(ventas, devoluciones)

        // 4. Módulo Inventario (sede efectiva)
        val inventario = try {
            val productosSede = repository.obtenerInventarioSede(farmaciaId, sucursalId)
            fuentesOK.add(FuenteDatos.INVENTARIO)
            AnaliticaCalculadora.calcularInventario(productosSede, ventas, HoraServidor.ahoraMs(), inicioMs, finMs)
        } catch (e: Exception) {
            fuentesFallidas[FuenteDatos.INVENTARIO] = e.message ?: "No se pudo sincronizar inventario"
            InventarioAnalytics()
        }

        // 5. Módulo Compras (sede efectiva)
        val compras = try {
            val facturasCompra = repository.obtenerComprasPeriodo(farmaciaId, sucursalId, inicioMs, finMs)
            fuentesOK.add(FuenteDatos.COMPRAS)
            AnaliticaCalculadora.calcularCompras(facturasCompra)
        } catch (e: Exception) {
            fuentesFallidas[FuenteDatos.COMPRAS] = e.message ?: "No se pudo sincronizar facturas de proveedores"
            ComprasAnalytics()
        }



        val metodosConfig = try {
            repository.obtenerMetodosPagoConfigurados(farmaciaId, sucursalId)
        } catch (_: Exception) {
            emptyList()
        }

        val sesionesPeriodo = try {
            repository.obtenerSesionesCajaPeriodo(farmaciaId, sucursalId, inicioMs, finMs)
        } catch (_: Exception) {
            emptyList()
        }

        val movsCajaPeriodo = try {
            repository.obtenerMovimientosCajaPeriodo(farmaciaId, sucursalId, inicioMs, finMs)
        } catch (_: Exception) {
            emptyList()
        }

        val estadoCajaPuntual = if (_periodoSeleccionado.value == PeriodoAnalitica.HOY) {
            estadoCaja
        } else {
            try { repository.obtenerEstadoCajaPuntual(farmaciaId, sucursalId) } catch (_: Exception) { estadoCaja }
        }

        val esPeriodoHoy = _periodoSeleccionado.value == PeriodoAnalitica.HOY

        // Dimensión 2 y 3: Flujo de Dinero y Arqueos de Caja
        val dineroYCaja = AnaliticaCalculadora.calcularDineroYCaja(
            sesiones = sesionesPeriodo,
            movimientos = movsCajaPeriodo,
            estadoCajaActual = estadoCajaPuntual,
            metricasVentas = metricas,
            esPeriodoHoy = esPeriodoHoy
        )

        val estadoResultado = AnaliticaCalculadora.calcularEstadoResultadoNegocio(
            metricas = metricas,
            gastosOperativos = dineroYCaja.egresosTotales,
            diferenciaCaja = dineroYCaja.diferenciaCajaTotal
        )

        // Dimensión 4: Alertas de la Verdad (Faltantes, Cajas Viejas Abiertas, Margen)
        val alertasVerdad = AnaliticaCalculadora.generarAlertasVerdad(
            dineroYCaja = dineroYCaja,
            rentabilidad = rentabilidad,
            sesiones = sesionesPeriodo,
            estadoCajaActual = estadoCajaPuntual
        )

        // Verdad Vigente y Coherencia de Filtros: los turnos mostrados corresponden estrictamente al período elegido (R8/R12)
        val historialSesiones = sesionesPeriodo

        val hayActividadCaja = sesionesPeriodo.isNotEmpty() || movsCajaPeriodo.isNotEmpty() || (esPeriodoHoy && estadoCajaPuntual.estado == CajaSesion.ESTADO_ABIERTA)
        val esVacio = ventas.isEmpty() && devoluciones.isEmpty() && !hayActividadCaja

        // Conciliación fiscal POS ↔ SUNAT (documentos reales del período, no ceros).
        val docsFiscales = try {
            repository.obtenerFacturacionDocumentos(farmaciaId, sucursalId, inicioMs, finMs)
        } catch (_: Exception) { emptyList() }
        val conciliacionFiscal = AnaliticaCalculadora.construirConciliacionFiscal(ventas, devoluciones, docsFiscales)

        // Conciliación caja POS ↔ movimientos (cobros netos por método, cuadra con Cierre).
        val punteroPeriodo = AnaliticaCalculadora.sumarVentasPorMetodoSesiones(sesionesPeriodo, estadoCajaPuntual, esPeriodoHoy)
        val conciliacionCaja = AnaliticaCalculadora.construirConciliacionCaja(metricas, movsCajaPeriodo, punteroPeriodo)
        val sedeNombreUnica = resolverEtiquetaSede(sucursalId, _sucursalesDisponibles.value)
        val sucursalesUi = AnaliticaCalculadora.calcularSucursales(
            mapOf(sucursalId to ventas),
            mapOf(sucursalId to devoluciones),
            mapOf(sucursalId to sedeNombreUnica)
        )
        val insightsUi = AnaliticaCalculadora.calcularInsights(inventario, rentabilidad, conciliacionFiscal, conciliacionCaja)

        if (jobId != 0L && jobId != cargaJobId) return

        _uiState.value = AnaliticaUiState.Exito(
            metricas = metricas,
            estadoCaja = estadoCajaPuntual,
            rentabilidad = rentabilidad,
            inventario = inventario,
            compras = compras,
            clientes = clientes,
            listaVentas = ventas,
            listaDevoluciones = devoluciones,
            listaMovimientosCaja = movsCajaPeriodo,
            metodosConfigurados = metodosConfig,
            esVacio = esVacio,
            estaActualizando = false,
            leidoEnMs = HoraServidor.ahoraMs(),
            fuentesOK = fuentesOK,
            fuentesFallidas = fuentesFallidas,
            periodoEtiqueta = resolverEtiquetaPeriodo(_periodoSeleccionado.value),
            sedeEtiqueta = sedeNombreUnica,
            historialSesionesCaja = historialSesiones,
            estadoResultado = estadoResultado,
            dineroYCaja = dineroYCaja,
            alertasVerdad = alertasVerdad,
            conciliacionFiscal = conciliacionFiscal,
            conciliacionCaja = conciliacionCaja,
            sucursales = sucursalesUi,
            insights = insightsUi
        )
    }

    private suspend fun procesarDatosMultisede(
        farmaciaId: String,
        sucursales: List<SucursalInfo>,
        ventasPorSede: Map<String, List<Venta>>,
        devsPorSede: Map<String, List<DevolucionVenta>>,
        estadoCaja: EstadoCaja,
        inicioMs: Long,
        finMs: Long,
        jobId: Long = 0L
    ) {
        val fuentesOK = mutableSetOf(FuenteDatos.VENTAS, FuenteDatos.CAJA)
        val fuentesFallidas = mutableMapOf<FuenteDatos, String>()

        val sedesIds = sucursales.map { it.id }.ifEmpty { ventasPorSede.keys.toList() }
        val nombresSedes = sucursales.associate {
            it.id to (if (it.activa) it.nombre else "${it.nombre} (Inactiva)")
        }

        // Unión exacta y deduplicada por sede + ID para toda la farmacia (R1/R3)
        val ventasConsolidadas = ventasPorSede.flatMap { (sId, vs) ->
            vs.map { sId to it }
        }.distinctBy { (sId, v) ->
            "${sId.ifBlank { "SIN_SEDE" }}_${v.id}"
        }.map { it.second }

        val devsConsolidadas = devsPorSede.flatMap { (sId, ds) ->
            ds.map { sId to it }
        }.distinctBy { (sId, d) ->
            "${sId.ifBlank { "SIN_SEDE" }}_${d.id}"
        }.map { it.second }

        // 1. Cálculo determinista central con la misma fórmula
        val metricas = AnaliticaCalculadora.calcular(ventasConsolidadas, devsConsolidadas)
        val rentabilidad = AnaliticaCalculadora.calcularRentabilidad(ventasConsolidadas, devsConsolidadas)
        val clientes = AnaliticaCalculadora.calcularClientes(ventasConsolidadas, devsConsolidadas)

        // Inventario y compras multisede con try-catch independiente
        val inventario = try {
            val invPorSede = repository.obtenerInventarioMultisede(farmaciaId, sedesIds)
            val invConsolidado = invPorSede.values.flatten()
            fuentesOK.add(FuenteDatos.INVENTARIO)
            AnaliticaCalculadora.calcularInventario(invConsolidado, ventasConsolidadas, HoraServidor.ahoraMs(), inicioMs, finMs)
        } catch (e: Exception) {
            fuentesFallidas[FuenteDatos.INVENTARIO] = e.message ?: "No se pudo sincronizar inventario multisede"
            InventarioAnalytics()
        }

        val compras = try {
            val comprasPorSede = repository.obtenerComprasMultisede(farmaciaId, sedesIds, inicioMs, finMs)
            val comprasConsolidadas = comprasPorSede.values.flatten().distinctBy { "${it.id}" }
            fuentesOK.add(FuenteDatos.COMPRAS)
            AnaliticaCalculadora.calcularCompras(comprasConsolidadas)
        } catch (e: Exception) {
            fuentesFallidas[FuenteDatos.COMPRAS] = e.message ?: "No se pudo sincronizar compras multisede"
            ComprasAnalytics()
        }

        val todasSesionesMulti = mutableListOf<CajaSesion>()
        val todosMovsMulti = mutableListOf<MovimientoCaja>()
        for (sId in sedesIds) {
            try {
                todasSesionesMulti.addAll(repository.obtenerSesionesCajaPeriodo(farmaciaId, sId, inicioMs, finMs))
                todosMovsMulti.addAll(repository.obtenerMovimientosCajaPeriodo(farmaciaId, sId, inicioMs, finMs))
            } catch (_: Exception) {}
        }

        val esPeriodoHoyMulti = _periodoSeleccionado.value == PeriodoAnalitica.HOY
        val dineroYCajaMulti = AnaliticaCalculadora.calcularDineroYCaja(
            sesiones = todasSesionesMulti,
            movimientos = todosMovsMulti,
            estadoCajaActual = estadoCaja,
            metricasVentas = metricas,
            esPeriodoHoy = esPeriodoHoyMulti
        )

        val estadoResultadoMulti = AnaliticaCalculadora.calcularEstadoResultadoNegocio(
            metricas = metricas,
            gastosOperativos = dineroYCajaMulti.egresosTotales,
            diferenciaCaja = dineroYCajaMulti.diferenciaCajaTotal
        )

        val alertasVerdadMulti = AnaliticaCalculadora.generarAlertasVerdad(
            dineroYCaja = dineroYCajaMulti,
            rentabilidad = rentabilidad,
            sesiones = todasSesionesMulti,
            estadoCajaActual = estadoCaja
        )

        val metodosConfigMultisede = try {
            repository.obtenerMetodosPagoMultisede(farmaciaId, sedesIds)
        } catch (_: Exception) {
            emptyList()
        }
        // Verdad Vigente y Coherencia de Filtros: turnos consolidados de todas las sedes del período
        val historialSesionesMulti = todasSesionesMulti

        val hayActividadCajaMulti = todasSesionesMulti.isNotEmpty() || todosMovsMulti.isNotEmpty() || (esPeriodoHoyMulti && estadoCaja.estado == CajaSesion.ESTADO_ABIERTA)
        val esVacioMulti = ventasConsolidadas.isEmpty() && devsConsolidadas.isEmpty() && !hayActividadCajaMulti

        val docsFiscalesMulti = try {
            repository.obtenerFacturacionDocumentosMultisede(farmaciaId, sedesIds, inicioMs, finMs)
        } catch (_: Exception) { emptyList() }
        val conciliacionFiscalMulti = AnaliticaCalculadora.construirConciliacionFiscal(ventasConsolidadas, devsConsolidadas, docsFiscalesMulti)
        val punteroMulti = AnaliticaCalculadora.sumarVentasPorMetodoSesiones(todasSesionesMulti, estadoCaja, esPeriodoHoyMulti)
        val conciliacionCajaMulti = AnaliticaCalculadora.construirConciliacionCaja(metricas, todosMovsMulti, punteroMulti)
        val nombresMulti = sucursales.associate { it.id to (if (it.activa) it.nombre else "${it.nombre} (Inactiva)") }
            .ifEmpty { ventasPorSede.keys.associateWith { it } }
        val sucursalesMulti = AnaliticaCalculadora.calcularSucursales(ventasPorSede, devsPorSede, nombresMulti)
        val insightsMulti = AnaliticaCalculadora.calcularInsights(inventario, rentabilidad, conciliacionFiscalMulti, conciliacionCajaMulti)

        if (jobId != 0L && jobId != cargaJobId) return

        _uiState.value = AnaliticaUiState.Exito(
            metricas = metricas,
            estadoCaja = estadoCaja,
            rentabilidad = rentabilidad,
            inventario = inventario,
            compras = compras,
            clientes = clientes,
            listaVentas = ventasConsolidadas,
            listaDevoluciones = devsConsolidadas,
            listaMovimientosCaja = todosMovsMulti,
            metodosConfigurados = metodosConfigMultisede,
            esVacio = esVacioMulti,
            estaActualizando = false,
            leidoEnMs = HoraServidor.ahoraMs(),
            fuentesOK = fuentesOK,
            fuentesFallidas = fuentesFallidas,
            periodoEtiqueta = resolverEtiquetaPeriodo(_periodoSeleccionado.value),
            sedeEtiqueta = resolverEtiquetaSede("TODAS", sucursales),
            historialSesionesCaja = historialSesionesMulti,
            estadoResultado = estadoResultadoMulti,
            dineroYCaja = dineroYCajaMulti,
            alertasVerdad = alertasVerdadMulti,
            conciliacionFiscal = conciliacionFiscalMulti,
            conciliacionCaja = conciliacionCajaMulti,
            sucursales = sucursalesMulti,
            insights = insightsMulti
        )
    }

    // ── DRILL DOWN ─────────────────────────────────────────────────────────────

    fun seleccionarVenta(venta: Venta) {
        _ventaSeleccionada.value = venta
    }

    fun cerrarDetalleVenta() {
        _ventaSeleccionada.value = null
    }

    fun buscarVentaParaDrillDown(numeroCompleto: String) {
        viewModelScope.launch {
            val res = repository.buscarVentaPorNumero(numeroCompleto)
            res.onSuccess { list ->
                val encontrada = list.firstOrNull { it.numeroCompleto.equals(numeroCompleto, ignoreCase = true) }
                    ?: list.firstOrNull()
                _ventaSeleccionada.value = encontrada
                if (encontrada == null) {
                    _mensajeFeedback.value = "No se encontró el comprobante '$numeroCompleto'."
                }
            }.onFailure { e ->
                _mensajeFeedback.value = "Error buscando comprobante: ${e.message}"
            }
        }
    }

    // ── CÁLCULO DE RANGOS DE FECHA Y DIAS-CLAVE ────────────────────────────────

    private fun resolverRango(periodo: PeriodoAnalitica): Triple<List<String>, Long, Long> {
        val ahora = HoraServidor.ahoraMs()
        val cal = Calendar.getInstance(TIMEZONE_LIMA).apply { timeInMillis = ahora }

        return when (periodo) {
            PeriodoAnalitica.HOY -> {
                val hoyStr = formatoDia().format(cal.time)
                val (ini, fin) = calcularInicioFinDia(cal.time)
                Triple(listOf(hoyStr), ini, fin)
            }
            PeriodoAnalitica.AYER -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val ayerStr = formatoDia().format(cal.time)
                val (ini, fin) = calcularInicioFinDia(cal.time)
                Triple(listOf(ayerStr), ini, fin)
            }
            PeriodoAnalitica.ULTIMOS_7_DIAS -> {
                generarDiasAtras(7)
            }
            PeriodoAnalitica.ULTIMOS_15_DIAS -> {
                generarDiasAtras(15)
            }
            PeriodoAnalitica.ULTIMOS_30_DIAS -> {
                generarDiasAtras(30)
            }
            PeriodoAnalitica.ESTE_MES -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val (ini, _) = calcularInicioFinDia(cal.time)
                val calFin = Calendar.getInstance(TIMEZONE_LIMA).apply { timeInMillis = ahora }
                val (_, fin) = calcularInicioFinDia(calFin.time)
                val dias = generarListaDiasEntre(cal, calFin)
                Triple(dias, ini, fin)
            }
            PeriodoAnalitica.MES_ANTERIOR -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val (ini, _) = calcularInicioFinDia(cal.time)
                val maxDia = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val calFin = cal.clone() as Calendar
                calFin.set(Calendar.DAY_OF_MONTH, maxDia)
                val (_, fin) = calcularInicioFinDia(calFin.time)
                val dias = generarListaDiasEntre(cal, calFin)
                Triple(dias, ini, fin)
            }
            PeriodoAnalitica.MES -> {
                val anio = if (customAnio > 0) customAnio else cal.get(Calendar.YEAR)
                val mes = if (customMes in 1..12) customMes - 1 else cal.get(Calendar.MONTH)
                val calIni = Calendar.getInstance(TIMEZONE_LIMA).apply {
                    clear()
                    set(Calendar.YEAR, anio)
                    set(Calendar.MONTH, mes)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val (ini, _) = calcularInicioFinDia(calIni.time)
                val maxDia = calIni.getActualMaximum(Calendar.DAY_OF_MONTH)
                val calFin = (calIni.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, maxDia)
                }
                val (_, fin) = calcularInicioFinDia(calFin.time)
                val dias = generarListaDiasEntre(calIni, calFin)
                Triple(dias, ini, fin)
            }
            PeriodoAnalitica.PERSONALIZADO -> {
                val ini = if (customInicioMs > 0) customInicioMs else ahora
                val fin = if (customFinMs > 0) customFinMs else ahora
                val calIni = Calendar.getInstance(TIMEZONE_LIMA).apply { timeInMillis = ini }
                val calFin = Calendar.getInstance(TIMEZONE_LIMA).apply { timeInMillis = fin }
                val (iniMs, _) = calcularInicioFinDia(calIni.time)
                val (_, finMs) = calcularInicioFinDia(calFin.time)
                val dias = generarListaDiasEntre(calIni, calFin)
                Triple(dias, iniMs, finMs)
            }
        }
    }

    fun resolverEtiquetaPeriodo(periodo: PeriodoAnalitica): String {
        return when (periodo) {
            PeriodoAnalitica.MES -> {
                val mesAnio = _mesSeleccionado.value
                if (mesAnio != null) {
                    val nombres = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Set", "Oct", "Nov", "Dic")
                    "Mes: ${nombres.getOrElse(mesAnio.second - 1) { "${mesAnio.second}" }} ${mesAnio.first}"
                } else "Mes Específico"
            }
            PeriodoAnalitica.PERSONALIZADO -> {
                val ini = customInicioMs
                val fin = customFinMs
                if (ini > 0 && fin > 0) {
                    val sdf = SimpleDateFormat("dd/MM/yy", Locale.US).apply { timeZone = TIMEZONE_LIMA }
                    "${sdf.format(Date(ini))} - ${sdf.format(Date(fin))}"
                } else "Personalizado"
            }
            else -> periodo.label
        }
    }

    fun resolverEtiquetaSede(sedeSel: String, sucursales: List<SucursalInfo>): String {
        return if (sedeSel == "TODAS") {
            "Todas las Sedes"
        } else {
            sucursales.firstOrNull { it.id == sedeSel }?.nombre ?: "Sede Actual"
        }
    }

    private fun generarDiasAtras(diasCount: Int): Triple<List<String>, Long, Long> {
        val ahora = HoraServidor.ahoraMs()
        val cal = Calendar.getInstance(TIMEZONE_LIMA).apply { timeInMillis = ahora }
        val (_, fin) = calcularInicioFinDia(cal.time)

        val dias = mutableListOf<String>()
        val calIni = Calendar.getInstance(TIMEZONE_LIMA).apply { timeInMillis = ahora }
        calIni.add(Calendar.DAY_OF_YEAR, -(diasCount - 1))
        val (ini, _) = calcularInicioFinDia(calIni.time)

        val cursor = calIni.clone() as Calendar
        while (!cursor.after(cal)) {
            dias.add(formatoDia().format(cursor.time))
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return Triple(dias, ini, fin)
    }

    private fun generarListaDiasEntre(inicio: Calendar, fin: Calendar): List<String> {
        val dias = mutableListOf<String>()
        val cursor = inicio.clone() as Calendar
        while (!cursor.after(fin)) {
            dias.add(formatoDia().format(cursor.time))
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dias
    }

    private fun calcularInicioFinDia(date: Date): Pair<Long, Long> {
        val cal = Calendar.getInstance(TIMEZONE_LIMA).apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val ini = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val fin = cal.timeInMillis
        return ini to fin
    }

    /**
     * Observador en tiempo real de facturas de compras (R8 Verdad Vigente).
     * Sincroniza en vivo ante cualquier nuevo registro, abono, anulación o modificación.
     */
    private fun iniciarObservadorCompras(
        farmaciaId: String,
        sedesIds: List<String>,
        inicioMs: Long,
        finMs: Long,
        jobId: Long
    ) {
        comprasObserverJob?.cancel()
        comprasObserverJob = viewModelScope.launch {
            repository.observarComprasMultisede(farmaciaId, sedesIds, inicioMs, finMs)
                .collect { facturasVivas ->
                    if (jobId != cargaJobId) return@collect
                    val nuevasCompras = AnaliticaCalculadora.calcularCompras(facturasVivas)
                    val st = _uiState.value
                    if (st is AnaliticaUiState.Exito) {
                        _uiState.value = st.copy(compras = nuevasCompras)
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeJob?.cancel()
        comprasObserverJob?.cancel()
        sucursalObserverJob?.cancel()
    }
}
