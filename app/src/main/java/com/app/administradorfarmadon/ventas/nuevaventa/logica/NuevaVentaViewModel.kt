package com.app.administradorfarmadon.ventas.nuevaventa.logica

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.ApiDocumentosPeru
import com.app.administradorfarmadon.compartido.datos.ResultadoConsultaDoc
import com.app.administradorfarmadon.configuracion.metodospago.datos.MetodosPagoRepository
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.configuracion.metodospago.modelo.TIPOS_PAGO_FIJOS
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.ResolvedProductPresentation
import com.app.administradorfarmadon.inventario.compartido.modelo.resolverPresentacionPorCodigo
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.datos.InventarioFirestoreRepository
import com.app.administradorfarmadon.ventas.compartido.datos.CajaRepository
import com.app.administradorfarmadon.ventas.compartido.datos.TicketComprobantePdf
import com.app.administradorfarmadon.ventas.compartido.datos.VentasRepository
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.ClienteDeVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.PagoVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.ventas.compartido.modelo.VentaSuspendida
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

import com.app.administradorfarmadon.clientes.datos.ClientesRepository
import com.app.administradorfarmadon.clientes.modelo.ClienteFarmacia
import com.app.administradorfarmadon.facturacion.configuracion.datos.FacturacionConfigRepository
import com.app.administradorfarmadon.facturacion.configuracion.datos.EmisorFiscal
import com.app.administradorfarmadon.ventas.compartido.datos.BorradorVentaLocal
import com.app.administradorfarmadon.ventas.compartido.datos.VentaBorradorLocalStore
import com.app.administradorfarmadon.configuracion.pos.datos.PosConfigRepository
import com.app.administradorfarmadon.configuracion.pos.modelo.AutorizacionSupervisor
import com.app.administradorfarmadon.configuracion.pos.modelo.PosConfig
import com.app.administradorfarmadon.configuracion.sucursales.datos.InfoPlanCliente
import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal
import com.app.administradorfarmadon.configuracion.sucursales.datos.SucursalesRepository
import com.app.administradorfarmadon.ventas.compartido.modelo.ChecklistAperturaSede

sealed interface ResultadoDocUi {
    data class Encontrado(
        val tipo: String,
        val numero: String,
        val nombreCompleto: String,
        val direccion: String = "",
        val esDeDirectorio: Boolean = false
    ) : ResultadoDocUi

    data class Error(
        val tipo: String,
        val numero: String,
        val mensaje: String
    ) : ResultadoDocUi
}

/**
 * Estado UI completo para Nueva Venta / POS (R1/R3/R8/R10).
 */
data class NuevaVentaUiState(
    val carrito: List<ItemVenta> = emptyList(),
    val cliente: ClienteDeVenta = ClienteDeVenta(),
    val directorioClientes: List<ClienteFarmacia> = emptyList(),
    val descuento: Double = 0.0,
    val confirmoReceta: Boolean = false,
    // Puerta Única: Checklist de Apertura de Sede (R1/R3/R8/R12)
    val checklist: ChecklistAperturaSede = ChecklistAperturaSede(),
    val checklistCargado: Boolean = false,
    // Reglas de Venta y Caja POS (por sede)
    val posConfig: PosConfig = PosConfig(),
    val mostrarDialogoDescuento: Boolean = false,
    val supervisorAutorizante: AutorizacionSupervisor? = null,
    // Facturación Electrónica SUNAT (FASE F2)
    val emisorCompleto: Boolean = false,
    // Búsqueda y Resultados
    val busquedaTexto: String = "",
    val buscando: Boolean = false,
    val resultadosBusqueda: List<MoldeProductos> = emptyList(),
    // Estado de Caja y Métodos
    val estadoCaja: EstadoCaja = EstadoCaja(),
    val metodosPagoDisponibles: List<InstanciaPago> = emptyList(),
    val lineasPago: List<PagoVenta> = emptyList(),
    // Ventas Suspendidas
    val ventasSuspendidas: List<VentaSuspendida> = emptyList(),
    val mostrarSheetSuspendidas: Boolean = false,
    val mostrarDialogoSuspender: Boolean = false,
    // Diálogo y Flujo de Cobro
    val mostrarOverlayCobro: Boolean = false,
    val procesandoCobro: Boolean = false,
    val ventaExitosa: Venta? = null,
    val idempotenciaIdActual: String = "",
    // Consulta DNI/RUC
    val consultandoDoc: Boolean = false,
    val resultadoConsultaDoc: ResultadoDocUi? = null,
    val mostrarDialogoCliente: Boolean = false,
    // Notificaciones
    val error: String? = null,
    val mensajeExito: String? = null
) {
    val subtotal: Double
        get() = kotlin.math.round(carrito.sumOf { it.precioUnitario * it.cantidad } * 100.0) / 100.0

    val total: Double
        get() = kotlin.math.round((subtotal - descuento).coerceAtLeast(0.0) * 100.0) / 100.0

    val totalItems: Int
        get() = carrito.sumOf { it.cantidad }

    val requiereReceta: Boolean
        get() = carrito.any { it.requiereReceta }

    val cajaAbierta: Boolean
        get() = estadoCaja.estado == CajaSesion.ESTADO_ABIERTA

    val sumaPagos: Double
        get() = kotlin.math.round(lineasPago.sumOf { it.monto } * 100.0) / 100.0

    val pagosNoEfectivo: Double
        get() = kotlin.math.round(lineasPago.filter { it.tipoId != "EFECTIVO" }.sumOf { it.monto } * 100.0) / 100.0

    val vuelto: Double
        get() = kotlin.math.round((sumaPagos - total).coerceAtLeast(0.0) * 100.0) / 100.0

    val montoFaltante: Double
        get() = kotlin.math.round((total - sumaPagos).coerceAtLeast(0.0) * 100.0) / 100.0

    /**
     * Valida que todo método que requiera N° de operación lo tenga lleno (CRÍTICO 2).
     */
    val operacionesCompletas: Boolean
        get() {
            val tiposRequierenOp = TIPOS_PAGO_FIJOS.filter { it.requiereOperacion }.map { it.id }.toSet()
            return lineasPago.all { p ->
                if (p.tipoId in tiposRequierenOp) p.numeroOperacion.isNotBlank() else true
            }
        }

    val puedeCobrar: Boolean
        get() = checklist.todoListo &&
            !estadoCaja.esDeJornadaAnterior() &&
            carrito.isNotEmpty() &&
            (!requiereReceta || confirmoReceta) &&
            sumaPagos >= total - 0.009 &&
            pagosNoEfectivo <= total + 0.009 &&
            operacionesCompletas &&
            lineasPago.isNotEmpty()
}

/**
 * ViewModel principal del Punto de Venta (POS).
 */
class NuevaVentaViewModel(
    private val ventasRepository: VentasRepository = VentasRepository(),
    private val inventarioRepository: InventarioFirestoreRepository = InventarioFirestoreRepository(),
    private val cajaRepository: CajaRepository = CajaRepository(),
    private val metodosPagoRepository: MetodosPagoRepository = MetodosPagoRepository(),
    private val clientesRepository: ClientesRepository = ClientesRepository(),
    private val facturacionConfigRepository: FacturacionConfigRepository = FacturacionConfigRepository(),
    private val posConfigRepository: PosConfigRepository = PosConfigRepository(),
    private val sucursalesRepository: SucursalesRepository = SucursalesRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "NuevaVentaViewModel"
    }

    private val _uiState = MutableStateFlow(NuevaVentaUiState())
    val uiState: StateFlow<NuevaVentaUiState> = _uiState.asStateFlow()

    private var jobBusqueda: Job? = null
    private val observadoresJobs = mutableListOf<Job>()
    private var sucursalObserverJob: Job? = null

    // Cache reactivo para cálculo puro de Checklist (R1/R3/R8)
    private var ultimoEmisor: EmisorFiscal? = null
    private var ultimasSucursales: List<Sucursal> = emptyList()
    private var ultimoPlan: InfoPlanCliente = InfoPlanCliente()

    init {
        observarCambiosDeSucursal()
        verificarYRecuperarBorrador()
    }

    private fun observarCambiosDeSucursal() {
        sucursalObserverJob?.cancel()
        sucursalObserverJob = viewModelScope.launch {
            var ultimaSucursal: String? = null
            SessionManager.sucursalFlow.collect { sucursal ->
                if (sucursal != ultimaSucursal) {
                    ultimaSucursal = sucursal
                    reiniciarObservadoresPorCambioDeSucursal()
                }
            }
        }
    }

    private fun reiniciarObservadoresPorCambioDeSucursal() {
        observadoresJobs.forEach { it.cancel() }
        observadoresJobs.clear()
        _uiState.update {
            it.copy(
                carrito = emptyList(),
                descuento = 0.0,
                lineasPago = emptyList(),
                supervisorAutorizante = null,
                busquedaTexto = "",
                resultadosBusqueda = emptyList(),
                error = null,
                mensajeExito = null
            )
        }
        iniciarObservadores()
    }

    private fun iniciarObservadores() {
        val sucursalId = SessionManager.sucursalIdEfectiva
        val farmaciaId = SessionManager.clienteIdGarantizado

        // 0. Escuchar Reglas POS de la Sucursal (R1/R3)
        if (sucursalId.isNotBlank()) {
            observadoresJobs += viewModelScope.launch {
                posConfigRepository.observar(sucursalId)
                    .catch { Log.e(TAG, "Error escuchando posConfig: ${it.message}", it) }
                    .collect { config ->
                        _uiState.update { it.copy(posConfig = config) }
                        recalcularChecklist()
                    }
            }
        }

        // 1. Escuchar Estado de Caja
        observadoresJobs += viewModelScope.launch {
            cajaRepository.observarEstadoCaja()
                .catch { Log.e(TAG, "Error escuchando estado de caja: ${it.message}", it) }
                .collect { nuevoEstado ->
                    _uiState.update { it.copy(estadoCaja = nuevoEstado) }
                    recalcularChecklist()
                }
        }

        // 2. Escuchar Métodos de Pago Activos de la Sucursal (CRÍTICO 1)
        if (sucursalId.isNotBlank()) {
            observadoresJobs += viewModelScope.launch {
                metodosPagoRepository.observarMetodosPago(sucursalId)
                    .catch { Log.e(TAG, "Error escuchando métodos de pago: ${it.message}", it) }
                    .collect { lista ->
                        val activas = lista.filter { m -> m.activa }
                        _uiState.update { state ->
                            val lineasValidas = state.lineasPago.filter { linea ->
                                activas.any { it.id == linea.instanciaId || it.tipoId == linea.tipoId }
                            }
                            state.copy(
                                metodosPagoDisponibles = activas,
                                lineasPago = lineasValidas
                            )
                        }
                        recalcularChecklist()
                    }
            }
        }

        // 3. Escuchar Ventas Suspendidas
        observadoresJobs += viewModelScope.launch {
            ventasRepository.observarSuspendidas()
                .catch { Log.e(TAG, "Error escuchando suspendidas: ${it.message}", it) }
                .collect { suspList ->
                    _uiState.update { it.copy(ventasSuspendidas = suspList) }
                }
        }

        // 4. Escuchar Directorio de Clientes de la Farmacia (FASE 6)
        observadoresJobs += viewModelScope.launch {
            clientesRepository.observarClientes()
                .catch { Log.e(TAG, "Error escuchando directorio de clientes: ${it.message}", it) }
                .collect { clientes ->
                    _uiState.update { it.copy(directorioClientes = clientes) }
                }
        }

        // 5. Escuchar Estado de Facturación Electrónica (F2 - Regla de Oro POS)
        if (farmaciaId.isNotBlank()) {
            observadoresJobs += viewModelScope.launch {
                facturacionConfigRepository.observarEmisor(farmaciaId)
                    .catch { Log.e(TAG, "Error escuchando emisor fiscal: ${it.message}", it) }
                    .collect { emisor ->
                        ultimoEmisor = emisor
                        val completo = emisor?.estaCompleta == true
                        _uiState.update { it.copy(emisorCompleto = completo) }
                        recalcularChecklist()
                    }
            }
        }

        // 6. Escuchar Sedes para auditar estado activo y series 4/4
        if (farmaciaId.isNotBlank()) {
            observadoresJobs += viewModelScope.launch {
                sucursalesRepository.observarSucursales(farmaciaId)
                    .catch { Log.e(TAG, "Error escuchando sucursales para checklist: ${it.message}", it) }
                    .collect { sucursales ->
                        ultimasSucursales = sucursales
                        recalcularChecklist()
                    }
            }
        }

        // 7. Escuchar Plan y Contrato de Farmacia
        if (farmaciaId.isNotBlank()) {
            observadoresJobs += viewModelScope.launch {
                sucursalesRepository.observarInfoPlan(farmaciaId)
                    .catch { Log.e(TAG, "Error escuchando plan para checklist: ${it.message}", it) }
                    .collect { plan ->
                        ultimoPlan = plan
                        recalcularChecklist()
                    }
            }
        }
    }

    private fun recalcularChecklist() {
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        val estado = _uiState.value

        val farmaciaActiva = farmaciaId.isNotBlank()
        val suscripcionValida = farmaciaActiva && !ultimoPlan.limiteNoConfigurado && ultimoPlan.planId.isNotBlank()
        val sedeActual = ultimasSucursales.firstOrNull { it.id == sucursalId }
            ?: ultimasSucursales.firstOrNull { it.esPrincipal }
            ?: ultimasSucursales.firstOrNull()
        val sedeActiva = sedeActual?.activa ?: sucursalId.isNotBlank()

        val emisor = ultimoEmisor
        val emisorCompleto = emisor?.estaCompleta == true
        val seriesCompletas = sedeActual != null &&
                sedeActual.serieBoleta.isNotBlank() &&
                sedeActual.serieFactura.isNotBlank() &&
                sedeActual.serieNotaCreditoBoleta.isNotBlank() &&
                sedeActual.serieNotaCreditoFactura.isNotBlank()

        val metodosActivos = estado.metodosPagoDisponibles
        val tieneMetodos = metodosActivos.isNotEmpty()

        val posConfigGuardado = estado.posConfig.estaVigente
        val cajaAbierta = estado.estadoCaja.estado == CajaSesion.ESTADO_ABIERTA
        val esDeJornadaAnterior = estado.estadoCaja.esDeJornadaAnterior()
        val fechaCajaAnterior = if (esDeJornadaAnterior) estado.estadoCaja.fechaAperturaLegible() else ""

        val checklistActualizado = ChecklistAperturaSede(
            farmaciaActiva = farmaciaActiva,
            farmaciaNombre = SessionManager.nombreUsuario.ifBlank { "Farmacia Activa" },
            suscripcionValida = suscripcionValida,
            suscripcionDetalle = ultimoPlan.planNombre,
            sedeActiva = sedeActiva,
            sedeNombre = sedeActual?.nombre ?: "Sede actual",
            emisorFiscalCompleto = emisorCompleto,
            emisorRuc = emisor?.ruc ?: "",
            seriesFiscalesCompletas = seriesCompletas,
            seriesDetalle = if (seriesCompletas) "${sedeActual?.serieBoleta} / ${sedeActual?.serieFactura}" else "Faltan series",
            metodosPagoConfigurados = tieneMetodos,
            cantidadMetodosActivos = metodosActivos.size,
            posConfigGuardado = posConfigGuardado,
            posConfigDetalle = if (posConfigGuardado) "Guardado por ${estado.posConfig.actualizadoPorNombre}" else "Borrador sin guardar",
            cajaAbierta = cajaAbierta,
            cajaDetalle = if (cajaAbierta) "Abierta (${estado.estadoCaja.abiertoPorNombre})" else "Cerrada",
            cajaPendienteDeCierreAnterior = esDeJornadaAnterior,
            fechaCajaPendiente = fechaCajaAnterior
        )

        _uiState.update {
            it.copy(
                checklist = checklistActualizado,
                emisorCompleto = emisorCompleto,
                checklistCargado = true
            )
        }
    }

    /**
     * Recupera el borrador persistente local tras caída de app o reinicio (FASE 11 H1).
     * Si la venta ya se había grabado en Firestore antes del cierre, informa al cajero y limpia.
     */
    private fun verificarYRecuperarBorrador() {
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return

        viewModelScope.launch {
            val borrador = VentaBorradorLocalStore.obtenerBorrador(farmaciaId = farmaciaId, sucursalId = sucursalId)
            if (borrador != null && borrador.items.isNotEmpty()) {
                val ventaExistente = if (borrador.idempotenciaId.isNotBlank()) {
                    ventasRepository.consultarVentaPorIdempotencia(borrador.idempotenciaId)
                } else null

                if (ventaExistente != null) {
                    VentaBorradorLocalStore.limpiarBorrador(farmaciaId = farmaciaId, sucursalId = sucursalId)
                    _uiState.update {
                        it.copy(
                            carrito = emptyList(),
                            mensajeExito = "La venta anterior ya fue registrada con éxito: ${ventaExistente.tipoComprobante} ${ventaExistente.numeroCompleto}."
                        )
                    }
                } else {
                    val idRestaurado = borrador.idempotenciaId.ifBlank { "v_${UUID.randomUUID()}" }
                    _uiState.update {
                        it.copy(
                            carrito = borrador.items,
                            cliente = borrador.cliente,
                            descuento = borrador.descuento,
                            confirmoReceta = borrador.confirmoReceta,
                            idempotenciaIdActual = idRestaurado,
                            mensajeExito = "Se restauró la venta en curso de la sesión anterior."
                        )
                    }
                }
            }
        }
    }

    private fun persistirBorradorActual() {
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        val state = _uiState.value
        if (farmaciaId.isNotBlank() && sucursalId.isNotBlank()) {
            if (state.carrito.isNotEmpty()) {
                val idActual = state.idempotenciaIdActual.ifBlank { "v_${UUID.randomUUID()}" }
                if (state.idempotenciaIdActual.isBlank()) {
                    _uiState.update { it.copy(idempotenciaIdActual = idActual) }
                }
                val tipoComp = if (state.cliente.tipoDocumento == "RUC" && state.cliente.numeroDocumento.length == 11) "FACTURA" else "BOLETA"
                VentaBorradorLocalStore.guardarBorrador(
                    farmaciaId = farmaciaId,
                    sucursalId = sucursalId,
                    borrador = BorradorVentaLocal(
                        idempotenciaId = idActual,
                        items = state.carrito,
                        cliente = state.cliente,
                        tipoComprobante = tipoComp,
                        descuento = state.descuento,
                        confirmoReceta = state.confirmoReceta,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } else {
                VentaBorradorLocalStore.limpiarBorrador(farmaciaId = farmaciaId, sucursalId = sucursalId)
            }
        }
    }

    // ───────────────────────────── BÚSQUEDA DE PRODUCTOS (Debounce 300ms) ─────────────────────────────

    fun onBusquedaChange(texto: String) {
        _uiState.update { it.copy(busquedaTexto = texto) }
        jobBusqueda?.cancel()

        if (texto.trim().isBlank()) {
            _uiState.update { it.copy(buscando = false, resultadosBusqueda = emptyList()) }
            return
        }

        jobBusqueda = viewModelScope.launch {
            delay(200)
            _uiState.update { it.copy(buscando = true) }
            try {
                val farmaciaId = SessionManager.clienteIdGarantizado
                val sucursalId = SessionManager.sucursalIdEfectiva
                val pagina = inventarioRepository.buscarInventarioPaginado(
                    farmaciaId = farmaciaId,
                    sucursalId = sucursalId,
                    texto = texto,
                    limit = 20
                )

                // CRÍTICO 4 & 5: Auto-agregar SOLO si es coincidencia exacta de CÓDIGO (código de barras, código base, secundario o fracción -B/-U).
                // NUNCA auto-agregar por coincidencia de texto de nombre.
                if (pagina.productosMolde.size == 1 && esCoincidenciaExactaCodigo(pagina.productosMolde.first(), texto)) {
                    val unicoProd = pagina.productosMolde.first()
                    val presResuelta = unicoProd.resolverPresentacionPorCodigo(texto)
                    agregarItemResuelto(unicoProd, presResuelta)
                    _uiState.update { it.copy(busquedaTexto = "", buscando = false, resultadosBusqueda = emptyList()) }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        buscando = false,
                        resultadosBusqueda = pagina.productosMolde,
                        error = null
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Error buscando productos en POS: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        buscando = false,
                        resultadosBusqueda = emptyList(),
                        error = e.message ?: "Error al buscar producto"
                    )
                }
            }
        }
    }

    /**
     * Ejecución inmediata de búsqueda al recibir Enter o escaneo de pistola rápida.
     * Cancela el debounce y procesa el código o término al instante.
     */
    fun ejecutarBusquedaInmediata() {
        val texto = _uiState.value.busquedaTexto.trim()
        if (texto.isBlank()) return
        jobBusqueda?.cancel()

        jobBusqueda = viewModelScope.launch {
            _uiState.update { it.copy(buscando = true) }
            try {
                val farmaciaId = SessionManager.clienteIdGarantizado
                val sucursalId = SessionManager.sucursalIdEfectiva
                val pagina = inventarioRepository.buscarInventarioPaginado(
                    farmaciaId = farmaciaId,
                    sucursalId = sucursalId,
                    texto = texto,
                    limit = 20
                )

                // Auto-agregar si hay match exacto de código
                val matchExacto = pagina.productosMolde.firstOrNull { esCoincidenciaExactaCodigo(it, texto) }
                if (matchExacto != null) {
                    val presResuelta = matchExacto.resolverPresentacionPorCodigo(texto)
                    agregarItemResuelto(matchExacto, presResuelta)
                    _uiState.update { it.copy(busquedaTexto = "", buscando = false, resultadosBusqueda = emptyList()) }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        buscando = false,
                        resultadosBusqueda = pagina.productosMolde,
                        error = null
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Error en búsqueda inmediata POS: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        buscando = false,
                        resultadosBusqueda = emptyList(),
                        error = e.message ?: "Error al buscar producto"
                    )
                }
            }
        }
    }

    /**
     * Verifica si el texto de búsqueda es un código exacto del producto (barcode, código base, alias o fracción).
     */
    private fun esCoincidenciaExactaCodigo(prod: MoldeProductos, texto: String): Boolean {
        val codLimpio = texto.trim().replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()
        if (codLimpio.isBlank()) return false

        val codProdLimpio = prod.codigo.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase()
        if (codProdLimpio == codLimpio) return true

        if (prod.codigosSecundarios.any { it.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase() == codLimpio }) return true

        if (prod.presentaciones.any { pres ->
            (pres.codigoBarras.isNotBlank() && pres.codigoBarras.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase() == codLimpio) ||
                pres.codigosAnteriores.any { it.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase() == codLimpio }
        }) return true

        val matchFraccion = Regex("-(B|U)\\d+$", RegexOption.IGNORE_CASE).find(codLimpio)
        if (matchFraccion != null) {
            val baseCodigo = codLimpio.substring(0, matchFraccion.range.first)
            if (baseCodigo == codProdLimpio || prod.codigosSecundarios.any { it.replace(Regex("[^a-zA-Z0-9_-]"), "").uppercase() == baseCodigo }) {
                return true
            }
        }
        return false
    }

    fun limpiarBusqueda() {
        jobBusqueda?.cancel()
        _uiState.update { it.copy(busquedaTexto = "", buscando = false, resultadosBusqueda = emptyList()) }
    }

    // ───────────────────────────── CARRITO DE VENTAS ─────────────────────────────

    fun agregarAlCarrito(producto: MoldeProductos, presentacion: PresentacionProducto, cantidadInicial: Int = 1) {
        val res = ResolvedProductPresentation(
            presentacionId = presentacion.presentacionId,
            nombrePresentacion = presentacion.nombre,
            cantidadUnidades = presentacion.cantidad,
            precioVenta = presentacion.precioventa
        )
        agregarItemResuelto(producto, res, cantidadInicial)
    }

    private fun calcularStockMaximoPresentacion(producto: MoldeProductos, presentacionId: String): Int {
        val pres = producto.presentaciones.firstOrNull { it.presentacionId == presentacionId } ?: return Int.MAX_VALUE
        val factor = com.app.administradorfarmadon.inventario.compartido.logica.UnidadVentaHelper.factorContenido(producto.contenido, producto.presentaciones)
        val unidadProducto = producto.contenidoUnidad.ifBlank { producto.empaque }
        val cantUnidadProd = com.app.administradorfarmadon.inventario.compartido.logica.PerfilUnidades.normalizarA(
            pres.cantidad.toDouble(),
            pres.unidadMedida,
            unidadProducto
        )
        val fisicoPorUnidad = com.app.administradorfarmadon.inventario.compartido.logica.UnidadVentaHelper.stockFisicoParaVender(cantUnidadProd, factor)
        if (fisicoPorUnidad <= 0.0) return Int.MAX_VALUE

        val stockFisicoDisponible = producto.lotes.values
            .filter {
                val dias = com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper.diasHastaVencer(it.vencimiento)
                (dias == null || dias > 0) && it.cantidad > 0.0
            }
            .sumOf { it.cantidad }

        return (stockFisicoDisponible / fisicoPorUnidad).toInt().coerceAtLeast(0)
    }

    private fun agregarItemResuelto(producto: MoldeProductos, res: ResolvedProductPresentation, cantidadInicial: Int = 1) {
        // CRÍTICO 6: Bloquear productos resueltos con precio <= 0.00
        if (res.precioVenta <= 0.0) {
            _uiState.update { it.copy(error = "La presentación '${res.nombrePresentacion}' tiene precio S/ 0.00 y no puede ser agregada.") }
            return
        }

        val maxDisponible = calcularStockMaximoPresentacion(producto, res.presentacionId)
        if (maxDisponible <= 0) {
            _uiState.update { it.copy(error = "El producto '${producto.nombre}' no cuenta con stock disponible en lotes vigentes.") }
            return
        }

        val carritoActual = _uiState.value.carrito.toMutableList()
        val indexExistente = carritoActual.indexOfFirst {
            it.productoId == producto.indice && it.presentacionId == res.presentacionId
        }

        if (indexExistente >= 0) {
            val itemExistente = carritoActual[indexExistente]
            val nuevaCant = itemExistente.cantidad + cantidadInicial
            if (nuevaCant > maxDisponible) {
                _uiState.update { it.copy(error = "Solo quedan $maxDisponible ${res.nombrePresentacion} disponibles en stock.") }
                return
            }
            carritoActual[indexExistente] = itemExistente.copy(
                cantidad = nuevaCant,
                subtotal = kotlin.math.round(itemExistente.precioUnitario * nuevaCant * 100.0) / 100.0
            )
        } else {
            if (cantidadInicial > maxDisponible) {
                _uiState.update { it.copy(error = "Solo quedan $maxDisponible ${res.nombrePresentacion} disponibles en stock.") }
                return
            }

            // FASE 11 H2: Trazabilidad del lote físico y ubicación de anaquel
            val presParaFefo = producto.presentaciones.firstOrNull { it.presentacionId == res.presentacionId }
                ?: PresentacionProducto(
                    presentacionId = res.presentacionId,
                    nombre = res.nombrePresentacion,
                    cantidad = res.cantidadUnidades,
                    precioventa = res.precioVenta
                )
            val fefoSim = com.app.administradorfarmadon.inventario.compartido.logica.UnidadVentaHelper
                .calcularDescuentoFEFO(producto, presParaFefo)
            val primerLote = fefoSim.getOrNull()?.firstOrNull()
            val loteSugerido = primerLote?.loteNumero ?: ""
            val loteObj = producto.lotes.values.firstOrNull { it.loteId == primerLote?.loteId || it.numero == primerLote?.loteNumero }
            val loteVencimientoSugerido = loteObj?.vencimiento ?: ""
            val ubicacionAnaquel = producto.ubicacion.ifBlank { "" }

            val nuevoItem = ItemVenta(
                productoId = producto.indice,
                nombreProducto = producto.nombre,
                empaque = producto.empaque,
                presentacionId = res.presentacionId,
                presentacionNombre = res.nombrePresentacion,
                cantidad = cantidadInicial,
                precioUnitario = res.precioVenta,
                subtotal = kotlin.math.round(res.precioVenta * cantidadInicial * 100.0) / 100.0,
                requiereReceta = producto.requiereReceta,
                loteSugerido = loteSugerido,
                loteVencimientoSugerido = loteVencimientoSugerido,
                ubicacionAnaquel = ubicacionAnaquel,
                cantidadDevuelta = 0
            )
            carritoActual.add(nuevoItem)
        }

        _uiState.update { it.copy(carrito = carritoActual, error = null) }
        persistirBorradorActual()
    }

    fun cambiarCantidadItem(productoId: String, presentacionId: String, delta: Int) {
        val carritoActual = _uiState.value.carrito.toMutableList()
        val index = carritoActual.indexOfFirst { it.productoId == productoId && it.presentacionId == presentacionId }
        if (index >= 0) {
            val item = carritoActual[index]
            val nuevaCant = item.cantidad + delta
            if (nuevaCant <= 0) {
                carritoActual.removeAt(index)
            } else {
                if (delta > 0) {
                    val producto = _uiState.value.resultadosBusqueda.firstOrNull { it.indice == productoId }
                    if (producto != null) {
                        val maxDisponible = calcularStockMaximoPresentacion(producto, presentacionId)
                        if (nuevaCant > maxDisponible) {
                            _uiState.update { it.copy(error = "Solo quedan $maxDisponible ${item.presentacionNombre} disponibles en inventario.") }
                            return
                        }
                    }
                }
                carritoActual[index] = item.copy(
                    cantidad = nuevaCant,
                    subtotal = kotlin.math.round(item.precioUnitario * nuevaCant * 100.0) / 100.0
                )
            }
            _uiState.update { it.copy(carrito = carritoActual) }
            persistirBorradorActual()
        }
    }

    /**
     * Establece directamente la cantidad de un producto en el carrito (FASE 10 velocidad de mostrador).
     */
    fun setCantidadItem(productoId: String, presentacionId: String, cantidadDeseada: Int) {
        if (cantidadDeseada <= 0) {
            eliminarItemCarrito(productoId, presentacionId)
            return
        }
        val carritoActual = _uiState.value.carrito.toMutableList()
        val index = carritoActual.indexOfFirst { it.productoId == productoId && it.presentacionId == presentacionId }
        if (index >= 0) {
            val item = carritoActual[index]
            val producto = _uiState.value.resultadosBusqueda.firstOrNull { it.indice == productoId }
            if (producto != null) {
                val maxDisponible = calcularStockMaximoPresentacion(producto, presentacionId)
                if (cantidadDeseada > maxDisponible) {
                    _uiState.update { it.copy(error = "Solo quedan $maxDisponible ${item.presentacionNombre} disponibles en inventario.") }
                    return
                }
            }
            carritoActual[index] = item.copy(
                cantidad = cantidadDeseada,
                subtotal = kotlin.math.round(item.precioUnitario * cantidadDeseada * 100.0) / 100.0
            )
            _uiState.update { it.copy(carrito = carritoActual, error = null) }
            persistirBorradorActual()
        }
    }

    fun eliminarItemCarrito(productoId: String, presentacionId: String) {
        val carritoActual = _uiState.value.carrito.filterNot {
            it.productoId == productoId && it.presentacionId == presentacionId
        }
        _uiState.update { it.copy(carrito = carritoActual) }
        persistirBorradorActual()
    }

    fun vaciarCarrito() {
        _uiState.update {
            it.copy(
                carrito = emptyList(),
                cliente = ClienteDeVenta(),
                descuento = 0.0,
                confirmoReceta = false,
                lineasPago = emptyList(),
                error = null
            )
        }
        persistirBorradorActual()
    }

    fun abrirDialogoDescuento() {
        _uiState.update { it.copy(mostrarDialogoDescuento = true, error = null) }
    }

    fun cerrarDialogoDescuento() {
        _uiState.update { it.copy(mostrarDialogoDescuento = false) }
    }

    fun setDescuento(monto: Double) {
        aplicarDescuento(monto)
    }

    fun aplicarDescuento(monto: Double, autorizante: AutorizacionSupervisor? = null) {
        val d = kotlin.math.round(monto.coerceAtLeast(0.0) * 100.0) / 100.0
        val subtotal = _uiState.value.subtotal
        val config = _uiState.value.posConfig
        val pct = if (subtotal > 0.0) (d / subtotal) * 100.0 else 0.0

        if (d > 0.0) {
            if (config.excedeLimitesDescuento(pct, d)) {
                _uiState.update {
                    it.copy(
                        error = "El descuento (S/ ${String.format(Locale.US, "%.2f", d)} / ${String.format(Locale.US, "%.1f", pct)}%) supera el tope permitido para esta sede (${config.descuento.maxPct}% / S/ ${String.format(Locale.US, "%.2f", config.descuento.maxMonto)})."
                    )
                }
                return
            }
        }

        _uiState.update {
            it.copy(
                descuento = d,
                supervisorAutorizante = null,
                mostrarDialogoDescuento = false,
                error = null
            )
        }
        persistirBorradorActual()
    }

    fun setConfirmoReceta(confirmo: Boolean) {
        _uiState.update { it.copy(confirmoReceta = confirmo) }
        persistirBorradorActual()
    }

    // ───────────────────────────── CLIENTE Y CONSULTAS ─────────────────────────────

    fun abrirDialogoCliente() {
        _uiState.update { it.copy(mostrarDialogoCliente = true, error = null) }
    }

    fun cerrarDialogoCliente() {
        _uiState.update { it.copy(mostrarDialogoCliente = false) }
    }

    fun establecerCliente(cliente: ClienteDeVenta) {
        val idEnlazado = if (cliente.tipoDocumento != "NINGUNO" && cliente.numeroDocumento.isNotBlank()) {
            _uiState.value.directorioClientes.firstOrNull { it.numeroDocumento == cliente.numeroDocumento.trim() }?.id
                ?: cliente.numeroDocumento.trim()
        } else ""

        _uiState.update { it.copy(cliente = cliente.copy(clienteId = idEnlazado), mostrarDialogoCliente = false) }
        persistirBorradorActual()
    }

    fun guardarClienteYEstablecer(cliente: ClienteDeVenta, guardarEnDirectorio: Boolean = true) {
        viewModelScope.launch {
            val numLimpio = cliente.numeroDocumento.filter { it.isDigit() }.trim()
            val nomLimpio = cliente.nombre.trim()
            val tipoLimpio = cliente.tipoDocumento.trim().uppercase()

            if (tipoLimpio != "NINGUNO" && numLimpio.isNotBlank() && nomLimpio.isNotBlank() && !nomLimpio.equals("Consumidor Final", ignoreCase = true)) {
                val ficha = ClienteFarmacia(
                    id = numLimpio,
                    tipoDocumento = tipoLimpio,
                    numeroDocumento = numLimpio,
                    nombre = nomLimpio,
                    creadoPor = SessionManager.nombreUsuario
                )
                val res = clientesRepository.guardarCliente(ficha)
                res.onFailure { err ->
                    _uiState.update { it.copy(error = "Venta enlazada, pero la ficha no se guardó en el directorio: ${err.message}") }
                }
            }
            val clienteSaneado = cliente.copy(
                tipoDocumento = if (numLimpio.isBlank()) "NINGUNO" else tipoLimpio,
                numeroDocumento = numLimpio,
                nombre = nomLimpio,
                clienteId = numLimpio
            )

            _uiState.update { it.copy(cliente = clienteSaneado, mostrarDialogoCliente = false) }
            persistirBorradorActual()
        }
    }

    fun consultarDocumentoCliente(tipo: String, numero: String) {
        val numLimpio = numero.trim()
        if (numLimpio.isBlank()) return

        // 1. Verificar si ya existe en el directorio local de la farmacia
        val existente = _uiState.value.directorioClientes.firstOrNull { it.numeroDocumento == numLimpio }
        if (existente != null) {
            _uiState.update {
                it.copy(
                    consultandoDoc = false,
                    resultadoConsultaDoc = ResultadoDocUi.Encontrado(
                        tipo = existente.tipoDocumento,
                        numero = existente.numeroDocumento,
                        nombreCompleto = existente.nombre,
                        direccion = existente.direccion,
                        esDeDirectorio = true
                    )
                )
            }
            return
        }

        // 2. Si no existe en el directorio, consultar API oficial (RENIEC / SUNAT)
        viewModelScope.launch {
            _uiState.update { it.copy(consultandoDoc = true, resultadoConsultaDoc = null, error = null) }
            when (val res = ApiDocumentosPeru.consultar(tipo, numLimpio)) {
                is ResultadoConsultaDoc.Encontrado -> {
                    _uiState.update {
                        it.copy(
                            consultandoDoc = false,
                            resultadoConsultaDoc = ResultadoDocUi.Encontrado(
                                tipo = res.tipo,
                                numero = res.numero,
                                nombreCompleto = res.nombreCompleto,
                                direccion = res.direccion,
                                esDeDirectorio = false
                            )
                        )
                    }

                    // Auto-guardar en directorio de clientes de la farmacia
                    viewModelScope.launch {
                        try {
                            clientesRepository.guardarCliente(
                                com.app.administradorfarmadon.clientes.modelo.ClienteFarmacia(
                                    id = res.numero,
                                    tipoDocumento = res.tipo,
                                    numeroDocumento = res.numero,
                                    nombre = res.nombreCompleto,
                                    direccion = res.direccion
                                )
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo auto-guardar en directorio: ${e.message}")
                        }
                    }
                }
                is ResultadoConsultaDoc.NoEncontrado -> {
                    _uiState.update {
                        it.copy(
                            consultandoDoc = false,
                            resultadoConsultaDoc = ResultadoDocUi.Error(
                                tipo = tipo,
                                numero = numLimpio,
                                mensaje = "No se encontró el $tipo $numLimpio en el padrón nacional."
                            )
                        )
                    }
                }
                is ResultadoConsultaDoc.SinToken -> {
                    _uiState.update {
                        it.copy(
                            consultandoDoc = false,
                            resultadoConsultaDoc = ResultadoDocUi.Error(
                                tipo = tipo,
                                numero = numLimpio,
                                mensaje = res.mensaje
                            )
                        )
                    }
                }
                is ResultadoConsultaDoc.Error -> {
                    _uiState.update {
                        it.copy(
                            consultandoDoc = false,
                            resultadoConsultaDoc = ResultadoDocUi.Error(
                                tipo = tipo,
                                numero = numLimpio,
                                mensaje = res.mensaje
                            )
                        )
                    }
                }
            }
        }
    }

    fun aplicarResultadoCliente(res: ResultadoDocUi.Encontrado) {
        val nuevoCliente = ClienteDeVenta(
            tipoDocumento = res.tipo,
            numeroDocumento = res.numero,
            nombre = res.nombreCompleto,
            clienteId = res.numero
        )
        _uiState.update {
            it.copy(
                cliente = nuevoCliente,
                resultadoConsultaDoc = null,
                mensajeExito = "Cliente asignado a la venta: ${res.nombreCompleto}"
            )
        }
        persistirBorradorActual()

        // Si fue consultado en RENIEC/SUNAT y no estaba en el directorio de la farmacia, se incorpora automáticamente
        if (!res.esDeDirectorio && res.numero.isNotBlank() && res.nombreCompleto.isNotBlank()) {
            viewModelScope.launch {
                try {
                    clientesRepository.guardarCliente(
                        ClienteFarmacia(
                            id = res.numero,
                            tipoDocumento = res.tipo,
                            numeroDocumento = res.numero,
                            nombre = res.nombreCompleto,
                            direccion = res.direccion.trim()
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo auto-guardar cliente consultado en directorio: ${e.message}")
                }
            }
        }
    }

    fun limpiarResultadoConsultaDoc() {
        _uiState.update { it.copy(resultadoConsultaDoc = null) }
    }

    fun limpiarError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consultarDocumentoAuto(numero: String) {
        val numLimpio = numero.filter { it.isDigit() }.trim()
        if (numLimpio.isBlank()) {
            limpiarCliente()
            limpiarResultadoConsultaDoc()
            return
        }
        // Validación estricta Perú: Solo 8 dígitos (DNI) o 11 dígitos (RUC). Prohibido buscar con otras longitudes.
        val tipo = when (numLimpio.length) {
            8 -> "DNI"
            11 -> "RUC"
            else -> {
                limpiarResultadoConsultaDoc()
                return
            }
        }
        consultarDocumentoCliente(tipo, numLimpio)
    }

    fun asignarClienteManual(
        tipo: String,
        numero: String,
        nombre: String,
        direccion: String = "",
        guardarEnDirectorio: Boolean = true
    ) {
        val numLimpio = numero.trim()
        val nomLimpio = nombre.trim()
        if (nomLimpio.isBlank()) return

        val tipoNorm = if (tipo.isNotBlank()) tipo.trim().uppercase() else if (numLimpio.length == 11) "RUC" else "DNI"
        val nuevoCliente = ClienteDeVenta(
            tipoDocumento = tipoNorm,
            numeroDocumento = numLimpio,
            nombre = nomLimpio,
            clienteId = numLimpio
        )

        _uiState.update {
            it.copy(
                cliente = nuevoCliente,
                consultandoDoc = false,
                mostrarDialogoCliente = false,
                error = null,
                mensajeExito = "Cliente asignado a la venta: $nomLimpio"
            )
        }
        persistirBorradorActual()

        if (guardarEnDirectorio && numLimpio.isNotBlank()) {
            viewModelScope.launch {
                try {
                    clientesRepository.guardarCliente(
                        com.app.administradorfarmadon.clientes.modelo.ClienteFarmacia(
                            id = numLimpio,
                            tipoDocumento = tipoNorm,
                            numeroDocumento = numLimpio,
                            nombre = nomLimpio,
                            direccion = direccion.trim()
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo auto-guardar en directorio: ${e.message}")
                }
            }
        }
    }

    fun limpiarCliente() {
        _uiState.update {
            it.copy(
                cliente = ClienteDeVenta(
                    tipoDocumento = "NINGUNO",
                    numeroDocumento = "",
                    nombre = "Consumidor Final",
                    clienteId = ""
                ),
                error = null
            )
        }
        persistirBorradorActual()
    }

    // ───────────────────────────── FLUJO DE COBRO E IDEMPOTENCIA ─────────────────────────────

    fun abrirOverlayCobro() {
        val estadoActual = _uiState.value
        if (!estadoActual.checklist.todoListo) {
            _uiState.update { it.copy(error = "Apertura pendiente (${estadoActual.checklist.totalCompletados}/${estadoActual.checklist.totalRequisitos}): completa el checklist de la sede antes de cobrar.") }
            return
        }
        if (!estadoActual.cajaAbierta) {
            _uiState.update { it.copy(error = "La caja está cerrada. Ábrela en 'Cierre de Caja' para cobrar.") }
            return
        }
        if (estadoActual.estadoCaja.esDeJornadaAnterior()) {
            _uiState.update { it.copy(error = "Caja pendiente de cierre del ${estadoActual.estadoCaja.fechaAperturaLegible()}: ciérrala con arqueo físico antes de cobrar hoy.") }
            return
        }
        if (estadoActual.carrito.isEmpty()) {
            _uiState.update { it.copy(error = "El carrito no tiene productos para cobrar.") }
            return
        }
        if (estadoActual.requiereReceta && !estadoActual.confirmoReceta) {
            _uiState.update { it.copy(error = "Debes confirmar la receta médica para los medicamentos que la requieren.") }
            return
        }
        val total = estadoActual.total
        val idIdem = estadoActual.idempotenciaIdActual.ifBlank { "v_${UUID.randomUUID()}" }

        // Si había una consulta oficial verificada pendiente de aplicar al carrito, se auto-aplica antes de abrir el cobro
        val resDoc = estadoActual.resultadoConsultaDoc
        if (estadoActual.cliente.numeroDocumento.isBlank() && resDoc is ResultadoDocUi.Encontrado) {
            aplicarResultadoCliente(resDoc)
        }

        // CRÍTICO 1: Si hay EFECTIVO activo en metodosPagoDisponibles, precargarlo por defecto; si no, dejar vacío
        val metodoEfectivoActivo = estadoActual.metodosPagoDisponibles.firstOrNull { it.tipoId == "EFECTIVO" }
        val lineaDefecto = if (metodoEfectivoActivo != null) {
            listOf(
                PagoVenta(
                    tipoId = "EFECTIVO",
                    instanciaId = metodoEfectivoActivo.id,
                    nombreMetodo = "Efectivo",
                    monto = total,
                    numeroOperacion = ""
                )
            )
        } else {
            emptyList()
        }

        _uiState.update {
            it.copy(
                mostrarOverlayCobro = true,
                idempotenciaIdActual = idIdem,
                lineasPago = lineaDefecto,
                error = null
            )
        }
        persistirBorradorActual()
    }

    fun cerrarOverlayCobro() {
        _uiState.update { it.copy(mostrarOverlayCobro = false) }
    }

    fun actualizarLineasPago(nuevasLineas: List<PagoVenta>) {
        _uiState.update { it.copy(lineasPago = nuevasLineas) }
    }

    fun confirmarVenta() {
        val estado = _uiState.value
        if (estado.procesandoCobro) return
        if (!estado.checklist.todoListo) {
            _uiState.update { it.copy(error = "No se puede procesar el cobro: la apertura de la sede está incompleta. Revisa el checklist de requisitos.") }
            return
        }
        if (!estado.cajaAbierta) {
            _uiState.update { it.copy(error = "La caja está cerrada. Ábrela en 'Cierre de Caja' para cobrar.") }
            return
        }
        if (estado.carrito.isEmpty()) {
            _uiState.update { it.copy(error = "El carrito no tiene productos.") }
            return
        }
        if (estado.requiereReceta && !estado.confirmoReceta) {
            _uiState.update { it.copy(error = "Debes confirmar la receta médica para los medicamentos que la requieren.") }
            return
        }
        if (!estado.operacionesCompletas) {
            _uiState.update { it.copy(error = "Debe ingresar el N° de operación para los pagos que lo requieren (Yape, Plin, Transferencia o Cheque).") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(procesandoCobro = true, error = null) }

            val res = ventasRepository.registrarVenta(
                items = estado.carrito,
                cliente = estado.cliente,
                pagos = estado.lineasPago,
                descuento = estado.descuento,
                confirmoReceta = estado.confirmoReceta,
                idempotenciaId = estado.idempotenciaIdActual,
                autorizadoPorId = "",
                autorizadoPorNombre = "",
                autorizadoPorRol = ""
            )

            res.onSuccess { ventaCompletada ->
                val farmaciaId = SessionManager.clienteIdGarantizado
                val sucursalId = SessionManager.sucursalIdEfectiva
                VentaBorradorLocalStore.limpiarBorrador(farmaciaId = farmaciaId, sucursalId = sucursalId)

                // R1/R3: Si la venta se emitió a un cliente identificado, asegurar su ficha en el directorio de la farmacia
                val cli = ventaCompletada.cliente
                val docCli = cli.numeroDocumento.trim()
                if (docCli.isNotBlank() && !cli.nombre.equals("Consumidor Final", ignoreCase = true)) {
                    viewModelScope.launch {
                        try {
                            clientesRepository.guardarCliente(
                                ClienteFarmacia(
                                    id = docCli,
                                    tipoDocumento = cli.tipoDocumento,
                                    numeroDocumento = docCli,
                                    nombre = cli.nombre.trim()
                                )
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo asegurar cliente de la venta en directorio: ${e.message}")
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        procesandoCobro = false,
                        mostrarOverlayCobro = false,
                        ventaExitosa = ventaCompletada,
                        carrito = emptyList(),
                        descuento = 0.0,
                        confirmoReceta = false,
                        lineasPago = emptyList(),
                        idempotenciaIdActual = "", // Se resetea solo tras el éxito
                        cliente = ClienteDeVenta(),
                        mensajeExito = "¡Venta ${ventaCompletada.numeroCompleto} registrada exitosamente!"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        procesandoCobro = false,
                        mostrarOverlayCobro = false,
                        error = err.message ?: "No se pudo registrar la venta."
                    )
                }
            }
        }
    }

    fun finalizarVentaExitosa() {
        _uiState.update { it.copy(ventaExitosa = null, error = null, mensajeExito = null) }
    }

    // MENOR M2 & M3 & MEDIO 6: Sin datos suplentes, manejo de error honesto y ejecución en Dispatchers.IO
    fun imprimirComprobante(context: Context) {
        val venta = _uiState.value.ventaExitosa ?: return
        viewModelScope.launch {
            val emisorRes = withContext(Dispatchers.IO) { ventasRepository.obtenerEmisor() }
            val emisor = emisorRes.getOrNull()
            if (emisor == null) {
                _uiState.update { it.copy(error = "No se pudieron obtener los datos de la farmacia para imprimir el comprobante: ${emisorRes.exceptionOrNull()?.message}") }
                return@launch
            }

            val resImpresion = withContext(Dispatchers.IO) {
                TicketComprobantePdf.imprimirTicket(context, venta, emisor)
            }
            resImpresion.onFailure { err ->
                _uiState.update { it.copy(error = "No se pudo imprimir el comprobante: ${err.message}") }
            }
        }
    }

    // ───────────────────────────── SUSPENDER / REANUDAR VENTAS ─────────────────────────────

    fun abrirDialogoSuspender() {
        if (_uiState.value.carrito.isEmpty()) {
            _uiState.update { it.copy(error = "El carrito está vacío, no hay nada que suspender.") }
            return
        }
        _uiState.update { it.copy(mostrarDialogoSuspender = true, error = null) }
    }

    fun cerrarDialogoSuspender() {
        _uiState.update { it.copy(mostrarDialogoSuspender = false) }
    }

    fun suspenderVenta(nota: String) {
        val estado = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(mostrarDialogoSuspender = false, error = null) }
            val res = ventasRepository.suspenderVenta(
                items = estado.carrito,
                cliente = estado.cliente,
                descuento = estado.descuento,
                nota = nota
            )
            res.onSuccess {
                vaciarCarrito()
                _uiState.update {
                    it.copy(mensajeExito = "Venta suspendida correctamente.")
                }
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message ?: "No se pudo suspender la venta.") }
            }
        }
    }

    fun abrirSheetSuspendidas() {
        _uiState.update { it.copy(mostrarSheetSuspendidas = true, error = null) }
    }

    fun cerrarSheetSuspendidas() {
        _uiState.update { it.copy(mostrarSheetSuspendidas = false) }
    }

    // ALTO 3 & MEDIO 7: Reanudar venta suspendida atómica con transacción y restauración de descuento
    fun reanudarVentaSuspendida(v: VentaSuspendida) {
        viewModelScope.launch {
            val res = ventasRepository.reanudarVentaSuspendida(v.id)
            res.onSuccess { ventaRecuperada ->
                _uiState.update {
                    it.copy(
                        carrito = ventaRecuperada.items,
                        cliente = ventaRecuperada.cliente,
                        descuento = ventaRecuperada.descuento,
                        mostrarSheetSuspendidas = false,
                        mensajeExito = "Venta reanudada en el carrito."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(error = err.message ?: "No se pudo reanudar la venta suspendida.")
                }
            }
        }
    }

    fun descartarVentaSuspendida(id: String) {
        viewModelScope.launch {
            val res = ventasRepository.eliminarSuspendida(id)
            res.onFailure { err ->
                _uiState.update { it.copy(error = "No se pudo eliminar la venta suspendida: ${err.message}") }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(error = null, mensajeExito = null) }
    }
}
