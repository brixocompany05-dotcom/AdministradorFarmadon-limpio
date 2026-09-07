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
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.ResolvedProductPresentation
import com.app.administradorfarmadon.inventario.compartido.modelo.resolverPresentacionPorCodigo
import com.app.administradorfarmadon.inventario.compartido.modelo.calcularStockMaximoPresentacion
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
import kotlinx.coroutines.flow.combine
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
import com.app.administradorfarmadon.ventas.compartido.logica.ReglaBloqueoTurnoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoOperativoTurno
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.inventario.compartido.logica.ProductoParser
import com.google.firebase.firestore.FieldPath
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await

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

data class SugerenciaPresentacion(
    val presentacionId: String,
    val nombre: String,
    val precioVenta: Double,
    val stockDisponible: Int
)

sealed interface DisponibilidadItemCarrito {
    data class Disponible(
        val stockDisponible: Int = Int.MAX_VALUE
    ) : DisponibilidadItemCarrito

    data class PresentacionAgotada(
        val sugerencias: List<SugerenciaPresentacion> = emptyList()
    ) : DisponibilidadItemCarrito

    data class ProductoAgotado(
        val motivo: String = ""
    ) : DisponibilidadItemCarrito
}

/**
 * Origen del evento de alerta sobre el producto (Buscador o Carrito)
 * para evitar contaminación visual cruzada entre ambos paneles (R1/R3).
 */
enum class OrigenAlertaProducto {
    BUSCADOR,
    CARRITO
}

/**
 * Alerta contextual flotante anclada al ítem específico del producto (búsqueda o carrito).
 * En lugar de banners globales en el tope de la pantalla, cada aviso nace directamente de su producto
 * y respeta el panel desde donde se interactuó (Buscador vs Carrito).
 */
data class AlertaItemProducto(
    val productoId: String,
    val presentacionId: String,
    val mensaje: String,
    val origen: OrigenAlertaProducto,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Estado UI completo para Nueva Venta / POS (R1/R3/R8/R10).
 */
data class NuevaVentaUiState(
    val carrito: List<ItemVenta> = emptyList(),
    val mapaDisponibilidad: Map<String, DisponibilidadItemCarrito> = emptyMap(),
    val alertaItemProducto: AlertaItemProducto? = null,
    val cliente: ClienteDeVenta = ClienteDeVenta(),
    val directorioClientes: List<ClienteFarmacia> = emptyList(),
    val descuento: Double = 0.0,
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
    // Si hay más coincidencias que las mostradas, se avisa en vez de ocultarlas en silencio.
    val busquedaTruncada: Boolean = false,
    // Estado de Caja y Métodos
    val estadoCaja: EstadoCaja = EstadoCaja(),
    val metodosPagoDisponibles: List<InstanciaPago> = emptyList(),
    val lineasPago: List<PagoVenta> = emptyList(),
    // Ventas Suspendidas
    val ventasSuspendidas: List<VentaSuspendida> = emptyList(),
    val mostrarSheetSuspendidas: Boolean = false,
    val mostrarDialogoSuspender: Boolean = false,
    val ventaPendienteReanudacion: VentaSuspendida? = null,
    val suspendiendoVenta: Boolean = false,
    val reanudandoVentaId: String? = null,
    val descartandoVentaId: String? = null,
    val vaciandoCarrito: Boolean = false,
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

    /** Receta por producto: cada línea que la pide debe quedar marcada en su fila. */
    val recetaRequeridos: Int
        get() = carrito.count { it.requiereReceta }

    val recetaVerificados: Int
        get() = carrito.count { it.requiereReceta && it.recetaVerificada }

    val recetaTodoVerificado: Boolean
        get() = carrito.none { it.requiereReceta && !it.recetaVerificada }

    val cajaAbierta: Boolean
        get() = estadoCaja.estado == CajaSesion.ESTADO_ABIERTA

    val sumaPagos: Double
        get() = kotlin.math.round(lineasPago.sumOf { it.monto } * 100.0) / 100.0

    val pagosEfectivo: Double
        get() = kotlin.math.round(lineasPago.filter { it.tipoId == "EFECTIVO" }.sumOf { it.monto } * 100.0) / 100.0

    val pagosNoEfectivo: Double
        get() = kotlin.math.round(lineasPago.filter { it.tipoId != "EFECTIVO" }.sumOf { it.monto } * 100.0) / 100.0

    val excesoNoEfectivo: Double
        get() = kotlin.math.round((pagosNoEfectivo - total).coerceAtLeast(0.0) * 100.0) / 100.0

    val hayExcesoNoEfectivo: Boolean
        get() = excesoNoEfectivo > 0.009

    val vuelto: Double
        get() = if (hayExcesoNoEfectivo) 0.0 else kotlin.math.round((sumaPagos - total).coerceAtLeast(0.0) * 100.0) / 100.0

    val excedeVueltoMax: Boolean
        get() = posConfig.estaVigente && vuelto > (posConfig.caja.vueltoMax + 0.009)

    val montoFaltante: Double
        get() = kotlin.math.round((total - sumaPagos).coerceAtLeast(0.0) * 100.0) / 100.0

    /**
     * No se exige N° de operación en el cobro rápido del POS.
     */
    val operacionesCompletas: Boolean
        get() = true

    val todosMetodosActivos: Boolean
        get() = lineasPago.all { linea ->
            metodosPagoDisponibles.any { inst ->
                (linea.instanciaId.isNotBlank() && inst.id == linea.instanciaId) ||
                (linea.instanciaId.isBlank() && inst.tipoId == linea.tipoId)
            }
        }

    val hayItemsAgotados: Boolean
        get() = mapaDisponibilidad.values.any {
            it is DisponibilidadItemCarrito.PresentacionAgotada || it is DisponibilidadItemCarrito.ProductoAgotado
        }

    val hayProblemasDisponibilidad: Boolean
        get() = hayItemsAgotados

    /** Porcentaje real del descuento sobre el subtotal vivo (0 si no hay subtotal). */
    val descuentoPct: Double
        get() = if (subtotal > 0.0) (descuento / subtotal) * 100.0 else 0.0

    /** El descuento quedó igual o mayor al subtotal tras cambiar el carrito: venta en S/0, prohibido. */
    val descuentoInvalido: Boolean
        get() = descuento > 0.0 && subtotal > 0.0 && descuento >= subtotal - 0.0001

    /** El descuento superó el tope de la sede tras cambiar el carrito o los topes. */
    val descuentoExcedeTope: Boolean
        get() = descuento > 0.0 && subtotal > 0.0 && posConfig.excedeLimitesDescuento(descuentoPct, descuento)

    val puedeCobrar: Boolean
        get() = checklist.todoListo &&
            ReglaBloqueoTurnoCaja.esOperativo(estadoCaja) &&
            carrito.isNotEmpty() &&
            recetaTodoVerificado &&
            !descuentoInvalido &&
            !descuentoExcedeTope &&
            sumaPagos >= total - 0.009 &&
            !hayExcesoNoEfectivo &&
            !excedeVueltoMax &&
            operacionesCompletas &&
            lineasPago.isNotEmpty() &&
            todosMetodosActivos &&
            !hayProblemasDisponibilidad
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
    private var jobObservadorProductosActivos: Job? = null
    private var ultimosIdsObservados: Set<String> = emptySet()

    // Cache reactivo para cálculo puro de Checklist (R1/R3/R8)
    private var ultimoEmisor: EmisorFiscal? = null
    private var ultimasSucursales: List<Sucursal> = emptyList()
    private var ultimoPlan: InfoPlanCliente = InfoPlanCliente()
    private val productosEnCarrito = mutableMapOf<String, MoldeProductos>()

    init {
        observarCambiosDeSucursal()
    }

    /**
     * Purga completa del estado temporal de la venta (R14 / Regla Global de Consistencia).
     * Se ejecuta cuando el turno se vence, se cierra, o cambia de sesión de caja.
     */
    fun descartarEstadoTemporal() {
        jobBusqueda?.cancel()
        jobObservadorProductosActivos?.cancel()
        jobObservadorProductosActivos = null
        ultimosIdsObservados = emptySet()
        productosEnCarrito.clear()

        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        val cajeroId = SessionManager.idCajera
        if (farmaciaId.isNotBlank() && sucursalId.isNotBlank()) {
            VentaBorradorLocalStore.limpiarBorrador(
                farmaciaId = farmaciaId,
                sucursalId = sucursalId,
                cajeroId = cajeroId
            )
        }

        _uiState.update {
            it.copy(
                carrito = emptyList(),
                mapaDisponibilidad = emptyMap(),
                cliente = ClienteDeVenta(),
                descuento = 0.0,
                busquedaTexto = "",
                buscando = false,
                resultadosBusqueda = emptyList(),
                lineasPago = emptyList(),
                mostrarOverlayCobro = false,
                mostrarDialogoSuspender = false,
                mostrarSheetSuspendidas = false,
                mostrarDialogoDescuento = false,
                mostrarDialogoCliente = false,
                resultadoConsultaDoc = null,
                supervisorAutorizante = null,
                idempotenciaIdActual = ""
            )
        }
    }

    private fun observarCambiosDeSucursal() {
        sucursalObserverJob?.cancel()
        sucursalObserverJob = viewModelScope.launch {
            var ultimaSucursal: String? = null
            var ultimoCajero: String? = null
            combine(SessionManager.sucursalFlow, SessionManager.cajeroFlow) { suc, caj ->
                suc to caj
            }.collect { (sucursal, cajero) ->
                if (sucursal != ultimaSucursal || cajero != ultimoCajero) {
                    val esCambioReal = ultimaSucursal != null || ultimoCajero != null
                    ultimaSucursal = sucursal
                    ultimoCajero = cajero
                    if (esCambioReal) {
                        reiniciarObservadoresPorCambioDeSucursal()
                    } else {
                        iniciarObservadores()
                    }
                }
            }
        }
    }

    private fun reiniciarObservadoresPorCambioDeSucursal() {
        observadoresJobs.forEach { it.cancel() }
        observadoresJobs.clear()
        jobObservadorProductosActivos?.cancel()
        jobObservadorProductosActivos = null
        ultimosIdsObservados = emptySet()
        productosEnCarrito.clear()
        _uiState.update {
            it.copy(
                carrito = emptyList(),
                mapaDisponibilidad = emptyMap(),
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
        val cajeroId = SessionManager.idCajera

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

        // 1. Escuchar Estado de Caja individual del cajero (R1/R3: aislamiento por cajero)
        observadoresJobs += viewModelScope.launch {
            var ultimoSesionId: String? = null
            var ultimoEstadoOperativo: EstadoOperativoTurno? = null
            var borradorYaVerificado = false

            cajaRepository.observarEstadoCaja(cajeroId)
                .catch { Log.e(TAG, "Error escuchando estado de caja: ${it.message}", it) }
                .collect { nuevoEstado ->
                    val operNuevo = nuevoEstado.estadoOperativo
                    val cambioTurno = ultimoSesionId != null && ultimoSesionId != nuevoEstado.sesionId
                    val pasoANoOperativo = ultimoEstadoOperativo == EstadoOperativoTurno.OPERATIVO &&
                            operNuevo != EstadoOperativoTurno.OPERATIVO

                    if (cambioTurno || pasoANoOperativo || operNuevo == EstadoOperativoTurno.TURNO_VENCIDO) {
                        if (_uiState.value.carrito.isNotEmpty()) {
                            descartarEstadoTemporal()
                        }
                    }

                    ultimoSesionId = nuevoEstado.sesionId
                    ultimoEstadoOperativo = operNuevo

                    _uiState.update {
                        it.copy(
                            estadoCaja = nuevoEstado
                        )
                    }
                    recalcularChecklist()

                    // R8/R14: En la primera emisión válida de caja en vivo, verificar y recuperar borrador local si la caja está operativa
                    if (!borradorYaVerificado) {
                        borradorYaVerificado = true
                        if (operNuevo == EstadoOperativoTurno.OPERATIVO) {
                            verificarYRecuperarBorrador(nuevoEstado)
                        } else {
                            val farmaciaIdActual = SessionManager.clienteIdGarantizado
                            val sucursalIdActual = SessionManager.sucursalIdEfectiva
                            if (farmaciaIdActual.isNotBlank() && sucursalIdActual.isNotBlank()) {
                                VentaBorradorLocalStore.limpiarBorrador(
                                    farmaciaId = farmaciaIdActual,
                                    sucursalId = sucursalIdActual,
                                    cajeroId = cajeroId
                                )
                            }
                        }
                    }
                }
        }

        // 2. Escuchar Métodos de Pago Activos de la Sucursal (CRÍTICO 1 / Tiempo Real Preventivo)
        if (sucursalId.isNotBlank()) {
            observadoresJobs += viewModelScope.launch {
                metodosPagoRepository.observarMetodosPago(sucursalId)
                    .catch { Log.e(TAG, "Error escuchando métodos de pago: ${it.message}", it) }
                    .collect { lista ->
                        val activas = lista.filter { m -> m.activa }
                        val idsActivas = activas.map { it.id }.toSet()
                        val tiposActivos = activas.map { it.tipoId }.toSet()

                        _uiState.update { state ->
                            val lineasDesactivadas = state.lineasPago.filterNot { linea ->
                                if (linea.instanciaId.isNotBlank()) {
                                    linea.instanciaId in idsActivas
                                } else {
                                    linea.tipoId in tiposActivos
                                }
                            }

                            val avisoDesactivado = if (lineasDesactivadas.isNotEmpty()) {
                                val nombres = lineasDesactivadas.map { it.nombreMetodo.ifBlank { it.tipoId } }.distinct()
                                "El método de pago '${nombres.joinToString(", ")}' fue desactivado en la farmacia. Selecciona un método activo."
                            } else null

                            val lineasValidas = state.lineasPago.filterNot { it in lineasDesactivadas }

                            // Coherencia contable: lo elegido siempre coincide con lo activo.
                            // Si todo lo elegido quedó inactivo y hay venta en curso, se repone
                            // el primero activo (Efectivo primero) por el total. Nunca a medias.
                            val lineasFinales = if (lineasValidas.isEmpty() && state.lineasPago.isNotEmpty() && state.carrito.isNotEmpty() && activas.isNotEmpty()) {
                                val ef = activas.firstOrNull { it.tipoId == "EFECTIVO" }
                                    ?: activas.firstOrNull()
                                if (ef != null) {
                                    val tipoMetodo = TIPOS_PAGO_FIJOS.firstOrNull { it.id == ef.tipoId }
                                    listOf(
                                        PagoVenta(
                                            tipoId = ef.tipoId,
                                            instanciaId = ef.id,
                                            nombreMetodo = tipoMetodo?.nombre ?: ef.tipoId,
                                            monto = state.total,
                                            numeroOperacion = ""
                                        )
                                    )
                                } else emptyList()
                            } else lineasValidas

                            state.copy(
                                metodosPagoDisponibles = activas,
                                lineasPago = lineasFinales,
                                error = avisoDesactivado ?: state.error
                            )
                        }
                        recalcularChecklist()
                    }
            }
        }

        // 3. Escuchar Ventas Suspendidas (Aislamiento individual por cajero R1)
        observadoresJobs += viewModelScope.launch {
            ventasRepository.observarSuspendidas(SessionManager.idCajera)
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
    private fun verificarYRecuperarBorrador(estadoCaja: EstadoCaja = _uiState.value.estadoCaja) {
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        val cajeroId = SessionManager.idCajera
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return

        viewModelScope.launch {
            val borrador = VentaBorradorLocalStore.obtenerBorrador(
                farmaciaId = farmaciaId,
                sucursalId = sucursalId,
                cajeroId = cajeroId
            )
            if (borrador != null && borrador.items.isNotEmpty()) {
                val esTurnoNoOperativo = !ReglaBloqueoTurnoCaja.esOperativo(estadoCaja, cajeroId)
                val esBorradorDeOtroDia = ReglaBloqueoTurnoCaja.esFechaAnterior(borrador.timestamp)

                if (esTurnoNoOperativo || esBorradorDeOtroDia) {
                    VentaBorradorLocalStore.limpiarBorrador(
                        farmaciaId = farmaciaId,
                        sucursalId = sucursalId,
                        cajeroId = cajeroId
                    )
                    return@launch
                }

                val ventaExistente = if (borrador.idempotenciaId.isNotBlank()) {
                    ventasRepository.consultarVentaPorIdempotencia(borrador.idempotenciaId)
                } else null

                if (ventaExistente != null) {
                    VentaBorradorLocalStore.limpiarBorrador(
                        farmaciaId = farmaciaId,
                        sucursalId = sucursalId,
                        cajeroId = cajeroId
                    )
                    _uiState.update {
                        it.copy(
                            carrito = emptyList(),
                            mensajeExito = "La venta anterior ya fue registrada con éxito: ${ventaExistente.tipoComprobante} ${ventaExistente.numeroCompleto}."
                        )
                    }
                } else {
                    // Una sola verdad al volver: se revalida contra stock y precio vivos
                    // antes de pintar. Lo agotado se retira solo y se avisa; nunca se
                    // ofrece un carrito fantasma que luego rebota al cobrar.
                    val frescos = obtenerMoldesFrescos(borrador.items.map { it.productoId })
                    productosEnCarrito.clear()
                    frescos.forEach { (id, molde) -> productosEnCarrito[id] = molde }
                    val reval = revalidarItemsPausados(borrador.items, frescos)
                    if (reval.items.isEmpty()) {
                        VentaBorradorLocalStore.limpiarBorrador(
                            farmaciaId = farmaciaId,
                            sucursalId = sucursalId,
                            cajeroId = cajeroId
                        )
                        _uiState.update {
                            it.copy(
                                carrito = emptyList(),
                                mapaDisponibilidad = emptyMap(),
                                error = "La canasta anterior ya no tiene stock disponible y se descartó para no vender humo."
                            )
                        }
                        sincronizarObservacionProductos()
                        return@launch
                    }
                    val subNuevo = kotlin.math.round(reval.items.sumOf { it.precioUnitario * it.cantidad } * 100.0) / 100.0
                    var descRestaurado = borrador.descuento
                    if (descRestaurado >= subNuevo && subNuevo > 0.0) {
                        descRestaurado = topeDescuentoPermitido(subNuevo)
                    }
                    val idRestaurado = borrador.idempotenciaId.ifBlank { "v_${UUID.randomUUID()}" }
                    // Migración de borradores viejos: antes la marca era general, ahora vive
                    // en cada producto. Si todo estaba confirmado, se hereda a cada línea.
                    val itemsRestaurados = if (borrador.confirmoReceta && reval.items.none { it.recetaVerificada }) {
                        reval.items.map { if (it.requiereReceta) it.copy(recetaVerificada = true) else it }
                    } else reval.items
                    val mapaInicial = calcularMapaDisponibilidad(itemsRestaurados, productosEnCarrito)
                    val avisoCambios = buildList {
                        if (reval.eliminados.isNotEmpty()) add("se retiraron ${reval.eliminados.size} sin stock")
                        if (reval.ajustados.isNotEmpty()) add("se ajustaron ${reval.ajustados.size} a lo disponible")
                        if (reval.preciosActualizados.isNotEmpty()) add("se actualizaron ${reval.preciosActualizados.size} precios")
                    }.joinToString(". ")
                    _uiState.update {
                        autoAjustarLineasPago(
                            it.copy(
                                carrito = itemsRestaurados,
                                mapaDisponibilidad = mapaInicial,
                                cliente = borrador.cliente,
                                descuento = descRestaurado,
                                idempotenciaIdActual = idRestaurado,
                                mensajeExito = if (avisoCambios.isBlank()) "Se restauró la canasta en curso de la sesión anterior." else "Canasta restaurada con limpieza automática. $avisoCambios.",
                                error = if (reval.eliminados.isNotEmpty()) reval.eliminados.take(2).joinToString("; ") else null
                            )
                        )
                    }
                    persistirBorradorActual()
                    sincronizarObservacionProductos()
                }
            }
        }
    }

    private fun persistirBorradorActual() {
        val state = _uiState.value
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        val cajeroId = SessionManager.idCajera
        if (farmaciaId.isBlank() || sucursalId.isBlank()) return

        if (!ReglaBloqueoTurnoCaja.esOperativo(state.estadoCaja, cajeroId)) {
            // No intentar persistir si el turno no está operativo
            return
        }

        if (state.carrito.isNotEmpty()) {
            val idActual = state.idempotenciaIdActual.ifBlank { "v_${UUID.randomUUID()}" }
            if (state.idempotenciaIdActual.isBlank()) {
                _uiState.update { it.copy(idempotenciaIdActual = idActual) }
            }
            val tipoComp = if (state.cliente.tipoDocumento == "RUC" && state.cliente.numeroDocumento.length == 11) "FACTURA" else "BOLETA"
            VentaBorradorLocalStore.guardarBorrador(
                farmaciaId = farmaciaId,
                sucursalId = sucursalId,
                cajeroId = cajeroId,
                borrador = BorradorVentaLocal(
                    idempotenciaId = idActual,
                    items = state.carrito,
                    cliente = state.cliente,
                    tipoComprobante = tipoComp,
                    descuento = state.descuento,
                    confirmoReceta = state.recetaTodoVerificado,
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
            VentaBorradorLocalStore.limpiarBorrador(
                farmaciaId = farmaciaId,
                sucursalId = sucursalId,
                cajeroId = cajeroId
            )
        }
    }

    /**
     * Sincroniza la escucha reactiva en tiempo real (<50ms) de los productos visibles (R8 Verdad Vigente).
     * Mantiene los productos en el buscador y en el carrito escuchando directamente de Firestore.
     * Si otro usuario o terminal agrega una presentación, edita un precio o descuenta stock,
     * la pantalla se actualiza en vivo SIN cerrar ni reabrir.
     */
    private fun sincronizarObservacionProductos() {
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        if (farmaciaId.isBlank() || sucursalId.isBlank()) {
            jobObservadorProductosActivos?.cancel()
            jobObservadorProductosActivos = null
            ultimosIdsObservados = emptySet()
            return
        }

        val state = _uiState.value
        val idsResultados = state.resultadosBusqueda.map { it.indice }.filter { it.isNotBlank() }
        val idsCarrito = state.carrito.map { it.productoId }.filter { it.isNotBlank() }
        val nuevosIds = (idsResultados + idsCarrito).distinct().filterNot { it.startsWith("mock_") }.toSet()

        if (nuevosIds == ultimosIdsObservados && jobObservadorProductosActivos?.isActive == true) return
        ultimosIdsObservados = nuevosIds

        jobObservadorProductosActivos?.cancel()
        if (nuevosIds.isEmpty()) {
            jobObservadorProductosActivos = null
            return
        }

        jobObservadorProductosActivos = viewModelScope.launch {
            inventarioRepository.observarProductosPorIds(farmaciaId, sucursalId, nuevosIds.toList())
                .catch { Log.e(TAG, "Error en observador de productos activos: ${it.message}", it) }
                .collect { productosFrescos ->
                    aplicarActualizacionesProductosFrescos(productosFrescos)
                }
        }
    }

    private fun calcularMapaDisponibilidad(
        carrito: List<ItemVenta>,
        mapaProductos: Map<String, MoldeProductos>
    ): Map<String, DisponibilidadItemCarrito> {
        return carrito.associate { item ->
            val clave = "${item.productoId}_${item.presentacionId}"
            val prod = mapaProductos[item.productoId]
                ?: _uiState.value.resultadosBusqueda.firstOrNull { it.indice == item.productoId }

            // Una sola verdad: sin ficha viva no se afirma disponibilidad.
            // Bloquea el cobro hasta confirmar; la puerta final es la transacción al cobrar.
            val disponibilidad = if (prod == null) {
                DisponibilidadItemCarrito.ProductoAgotado(
                    motivo = "Se está verificando la disponibilidad. Si persiste, retira el producto y vuelve a agregarlo."
                )
            } else if (!prod.activo) {
                DisponibilidadItemCarrito.ProductoAgotado(
                    motivo = "El producto '${prod.nombre}' fue desactivado en el inventario."
                )
            } else {
                val presActual = prod.presentaciones.firstOrNull { it.presentacionId == item.presentacionId }
                val maxDisponible = if (presActual == null) 0 else calcularStockMaximoPresentacion(prod, item.presentacionId)

                if (maxDisponible <= 0) {
                    val sugerencias = prod.presentaciones
                        .filter { it.presentacionId != item.presentacionId }
                        .mapNotNull { otraPres ->
                            val stockOtra = calcularStockMaximoPresentacion(prod, otraPres.presentacionId)
                            if (stockOtra > 0 && otraPres.precioventa > 0.0) {
                                SugerenciaPresentacion(
                                    presentacionId = otraPres.presentacionId,
                                    nombre = otraPres.nombre,
                                    precioVenta = otraPres.precioventa,
                                    stockDisponible = stockOtra
                                )
                            } else null
                        }

                    if (sugerencias.isNotEmpty()) {
                        DisponibilidadItemCarrito.PresentacionAgotada(sugerencias = sugerencias)
                    } else {
                        DisponibilidadItemCarrito.ProductoAgotado(
                            motivo = "No queda stock disponible de '${prod.nombre}' en ninguna de sus presentaciones."
                        )
                    }
                } else {
                    DisponibilidadItemCarrito.Disponible(stockDisponible = maxDisponible)
                }
            }

            clave to disponibilidad
        }
    }

    /**
     * Negocio real: una venta pausada nunca puede volver desactualizada.
     * Si el stock se acabó, el precio cambió o el producto se desactivó,
     * se limpia solo y se deja alternativa viva para no vender humo.
     */
    private data class RevalidacionPausada(
        val items: List<ItemVenta> = emptyList(),
        val eliminados: List<String> = emptyList(),
        val ajustados: List<String> = emptyList(),
        val preciosActualizados: List<String> = emptyList()
    )

    private fun esClienteGenerico(c: ClienteDeVenta): Boolean {
        if (c.tipoDocumento == "NINGUNO") return true
        val n = c.nombre.trim()
        return n.isBlank() ||
            n.equals("Consumidor Final", ignoreCase = true) ||
            n.equals("Cliente General", ignoreCase = true)
    }

    private fun esClienteReal(c: ClienteDeVenta): Boolean {
        if (c.tipoDocumento == "NINGUNO") return false
        val n = c.nombre.trim()
        return n.isNotBlank() &&
            !n.equals("Consumidor Final", ignoreCase = true) &&
            !n.equals("Cliente General", ignoreCase = true)
    }

    /**
     * Mostrador con apuro: trae el stock y precio vivos en UN solo viaje por cada 10 productos,
     * en paralelo. Nada de uno por uno que hace esperar en caja.
     */
    private suspend fun obtenerMoldesFrescos(ids: List<String>): Map<String, MoldeProductos> {
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        val limpios = ids.distinct().filter { it.isNotBlank() && !it.startsWith("mock_") }
        if (farmaciaId.isBlank() || sucursalId.isBlank() || limpios.isEmpty()) return emptyMap()
        return try {
            withContext(Dispatchers.IO) {
                limpios.chunked(10).map { chunk ->
                    async {
                        try {
                            val snap = FarmadonPaths.inventario(FarmadonFirestore.db, farmaciaId, sucursalId)
                                .whereIn(FieldPath.documentId(), chunk).get().await()
                            snap.documents.mapNotNull { doc ->
                                try {
                                    ProductoParser.parseToMolde(doc)?.let { doc.id to it }
                                } catch (_: Exception) { null }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo revalidar lote ${chunk.size} productos: ${e.message}")
                            emptyList()
                        }
                    }
                }.awaitAll().flatten().toMap()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo revalidación rápida: ${e.message}")
            emptyMap()
        }
    }

    private fun revalidarItemsPausados(
        items: List<ItemVenta>,
        moldesFrescos: Map<String, MoldeProductos>
    ): RevalidacionPausada {
        val limpios = mutableListOf<ItemVenta>()
        val eliminados = mutableListOf<String>()
        val ajustados = mutableListOf<String>()
        val precios = mutableListOf<String>()
        for (item in items) {
            val molde = moldesFrescos[item.productoId]
            if (molde == null) {
                eliminados.add("${item.nombreProducto} (${item.presentacionNombre}): ya no existe en inventario")
                continue
            }
            if (!molde.activo) {
                eliminados.add("${molde.nombre}: fue desactivado en inventario")
                continue
            }
            val presViva = molde.presentaciones.firstOrNull { it.presentacionId == item.presentacionId }
            if (presViva == null) {
                eliminados.add("${molde.nombre} (${item.presentacionNombre}): esa presentación ya no existe")
                continue
            }
            if (presViva.precioventa <= 0.0) {
                eliminados.add("${molde.nombre} (${presViva.nombre}): quedó con precio S/ 0.00 y no se puede vender")
                continue
            }
            val maxVivo = try {
                calcularStockMaximoPresentacion(molde, item.presentacionId)
            } catch (_: Exception) { 0 }
            if (maxVivo <= 0) {
                eliminados.add("${molde.nombre} (${presViva.nombre}): se agotó mientras estaba en pausa")
                continue
            }
            var cantFinal = item.cantidad.coerceAtLeast(1)
            if (cantFinal > maxVivo) {
                ajustados.add("${molde.nombre} (${presViva.nombre}): pedías ${item.cantidad}, quedan $maxVivo")
                cantFinal = maxVivo
            }
            var precioFinal = item.precioUnitario
            if (kotlin.math.abs(presViva.precioventa - item.precioUnitario) > 0.005) {
                precios.add("${molde.nombre}: ahora S/ ${String.format(Locale.US, "%.2f", presViva.precioventa)}")
                precioFinal = presViva.precioventa
            }
            val sub = kotlin.math.round(precioFinal * cantFinal * 100.0) / 100.0
            limpios.add(
                item.copy(
                    nombreProducto = molde.nombre,
                    presentacionNombre = presViva.nombre,
                    empaque = presViva.empaque,
                    precioUnitario = precioFinal,
                    cantidad = cantFinal,
                    subtotal = sub,
                    requiereReceta = molde.requiereReceta,
                    ubicacionAnaquel = molde.ubicacion.ifBlank { item.ubicacionAnaquel }
                )
            )
        }
        return RevalidacionPausada(limpios, eliminados, ajustados, precios)
    }

    private fun topeDescuentoPermitido(subtotal: Double): Double {
        if (subtotal <= 0.0) return 0.0
        val config = _uiState.value.posConfig
        val porPct = subtotal * config.descuento.maxPct / 100.0
        return kotlin.math.round(
            minOf(config.descuento.maxMonto, porPct, (subtotal - 0.01).coerceAtLeast(0.0)) * 100.0
        ) / 100.0
    }

    private fun aplicarActualizacionesProductosFrescos(productosFrescos: List<MoldeProductos>) {
        if (productosFrescos.isEmpty()) return
        val mapaFrescos = productosFrescos.associateBy { it.indice }

        mapaFrescos.forEach { (id, molde) ->
            productosEnCarrito[id] = molde
        }

        _uiState.update { state ->
            // 1. Actualizar resultados de búsqueda si están abiertos/visibles
            val nuevosResultados = state.resultadosBusqueda.map { prod ->
                mapaFrescos[prod.indice] ?: prod
            }

            // 2. Actualizar ítems en el carrito de manera preventiva en tiempo real (R8/R14)
            var huboCambioEnCarrito = false
            var avisoProducto: String? = null
            val nuevoCarrito = mutableListOf<ItemVenta>()

            state.carrito.forEach { item ->
                val prodFresco = mapaFrescos[item.productoId]
                if (prodFresco == null) {
                    nuevoCarrito.add(item)
                } else {
                    val presFresca = prodFresco.presentaciones.firstOrNull { it.presentacionId == item.presentacionId }
                    val maxDisponible = if (presFresca == null) 0 else calcularStockMaximoPresentacion(prodFresco, item.presentacionId)

                    // Auto-ajuste de cantidad si el stock físico disminuyó por debajo de lo pedido
                    val cantidadAjustada = if (maxDisponible > 0) {
                        item.cantidad.coerceAtMost(maxDisponible)
                    } else {
                        item.cantidad
                    }

                    val nuevoPrecio = presFresca?.precioventa ?: item.precioUnitario
                    val nuevoNombrePres = presFresca?.nombre ?: item.presentacionNombre
                    val nuevoEmpaque = presFresca?.empaque ?: item.empaque
                    val nuevoSubtotal = kotlin.math.round(nuevoPrecio * cantidadAjustada * 100.0) / 100.0

                    if (nuevoPrecio != item.precioUnitario ||
                        cantidadAjustada != item.cantidad ||
                        nuevoNombrePres != item.presentacionNombre ||
                        nuevoEmpaque != item.empaque ||
                        nuevoSubtotal != item.subtotal ||
                        prodFresco.nombre != item.nombreProducto
                    ) {
                        huboCambioEnCarrito = true
                        if (nuevoPrecio != item.precioUnitario && avisoProducto == null) {
                            avisoProducto = "El precio de '${prodFresco.nombre} (${nuevoNombrePres})' se actualizó a S/ ${String.format(Locale.US, "%.2f", nuevoPrecio)}."
                        } else if (cantidadAjustada != item.cantidad && avisoProducto == null) {
                            avisoProducto = "Stock ajustado a $cantidadAjustada unidades para '${prodFresco.nombre} (${nuevoNombrePres})'."
                        }
                    }

                    // Trazabilidad de lote en vivo
                    val loteSugerido: String
                    val loteVencimientoSugerido: String
                    if (presFresca != null) {
                        val fefoSim = com.app.administradorfarmadon.inventario.compartido.logica.UnidadVentaHelper
                            .calcularDescuentoFEFO(prodFresco, presFresca)
                        val primerLote = fefoSim.getOrNull()?.firstOrNull()
                        loteSugerido = primerLote?.loteNumero ?: item.loteSugerido
                        val loteObj = prodFresco.lotes.values.firstOrNull { it.loteId == primerLote?.loteId || it.numero == primerLote?.loteNumero }
                        loteVencimientoSugerido = loteObj?.vencimiento ?: item.loteVencimientoSugerido
                    } else {
                        loteSugerido = item.loteSugerido
                        loteVencimientoSugerido = item.loteVencimientoSugerido
                    }

                    // El ítem se conserva en el carrito: si no hay stock o la presentación está agotada,
                    // se detectará en mapaDisponibilidad para sugerir cambio por modal o retirarla,
                    // sin eliminaciones silenciosas (R3/R8/R14).
                    nuevoCarrito.add(
                        item.copy(
                            nombreProducto = prodFresco.nombre,
                            empaque = nuevoEmpaque,
                            presentacionNombre = nuevoNombrePres,
                            precioUnitario = nuevoPrecio,
                            cantidad = cantidadAjustada,
                            subtotal = nuevoSubtotal,
                            requiereReceta = prodFresco.requiereReceta,
                            loteSugerido = loteSugerido,
                            loteVencimientoSugerido = loteVencimientoSugerido,
                            ubicacionAnaquel = prodFresco.ubicacion.ifBlank { item.ubicacionAnaquel }
                        )
                    )
                }
            }

            val nuevoMapaDisponibilidad = calcularMapaDisponibilidad(nuevoCarrito, productosEnCarrito)

            // Avisos preventivos específicos
            if (avisoProducto == null) {
                val primerAgotado = nuevoCarrito.firstOrNull {
                    val d = nuevoMapaDisponibilidad["${it.productoId}_${it.presentacionId}"]
                    d is DisponibilidadItemCarrito.PresentacionAgotada || d is DisponibilidadItemCarrito.ProductoAgotado
                }

                if (primerAgotado != null) {
                    val d = nuevoMapaDisponibilidad["${primerAgotado.productoId}_${primerAgotado.presentacionId}"]
                    avisoProducto = if (d is DisponibilidadItemCarrito.PresentacionAgotada) {
                        "La presentación '${primerAgotado.presentacionNombre}' de '${primerAgotado.nombreProducto}' se agotó."
                    } else {
                        "El producto '${primerAgotado.nombreProducto}' no tiene stock disponible."
                    }
                }
            }

            // 3. Ajustar líneas de pago si el total cambió por actualización de precio en vivo
            val nuevoSubtotal = kotlin.math.round(nuevoCarrito.sumOf { it.precioUnitario * it.cantidad } * 100.0) / 100.0
            val nuevoTotal = kotlin.math.round((nuevoSubtotal - state.descuento).coerceAtLeast(0.0) * 100.0) / 100.0
            val lineasAjustadas = if (huboCambioEnCarrito && state.lineasPago.size == 1) {
                listOf(state.lineasPago.first().copy(monto = nuevoTotal))
            } else {
                state.lineasPago
            }

            val sinCambiosEnResultados = nuevosResultados == state.resultadosBusqueda
            val sinCambiosEnCarrito = nuevoCarrito == state.carrito
            if (sinCambiosEnResultados && sinCambiosEnCarrito && !huboCambioEnCarrito && nuevoMapaDisponibilidad == state.mapaDisponibilidad && avisoProducto == null) {
                return@update state
            }

            state.copy(
                resultadosBusqueda = nuevosResultados,
                carrito = nuevoCarrito,
                mapaDisponibilidad = nuevoMapaDisponibilidad,
                lineasPago = lineasAjustadas,
                error = state.error
            )
        }
    }

    // ───────────────────────────── BÚSQUEDA DE PRODUCTOS (Debounce 300ms) ─────────────────────────────

    /**
     * Mostrador honesto: trae TODO lo que coincida, página por página, sin tope silencioso.
     * Tope de seguridad alto (300) solo para no tumbar la tablet; si se alcanza se avisa
     * en pantalla para afinar la búsqueda en vez de ocultar resultados a ciegas.
     */
    private data class ResultadoBusquedaCompleta(
        val productos: List<MoldeProductos> = emptyList(),
        val truncada: Boolean = false
    )

    private suspend fun buscarTodoLoQueCoincida(texto: String): ResultadoBusquedaCompleta {
        val farmaciaId = SessionManager.clienteIdGarantizado
        val sucursalId = SessionManager.sucursalIdEfectiva
        if (farmaciaId.isBlank() || sucursalId.isBlank() || texto.trim().isBlank()) {
            return ResultadoBusquedaCompleta()
        }
        val acumulados = LinkedHashMap<String, MoldeProductos>()
        var cursor: com.google.firebase.firestore.DocumentSnapshot? = null
        var truncada = false
        var paginas = 0
        while (paginas < 6) {
            val pagina = inventarioRepository.buscarInventarioPaginado(
                farmaciaId = farmaciaId,
                sucursalId = sucursalId,
                texto = texto,
                limit = 50,
                startAfterDoc = cursor
            )
            pagina.productosMolde.forEach { acumulados.putIfAbsent(it.indice, it) }
            if (acumulados.size >= 300) {
                truncada = true
                break
            }
            if (pagina.esUltimaPagina || pagina.ultimoDocumento == null) break
            cursor = pagina.ultimoDocumento
            paginas++
        }
        return ResultadoBusquedaCompleta(acumulados.values.toList(), truncada)
    }

    fun onBusquedaChange(texto: String) {
        val check = ReglaBloqueoTurnoCaja.validarPermiteAgregarAlCarrito(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(busquedaTexto = "", buscando = false, resultadosBusqueda = emptyList(), error = check.exceptionOrNull()?.message) }
            return
        }
        _uiState.update { it.copy(busquedaTexto = texto) }
        jobBusqueda?.cancel()

        if (texto.trim().isBlank()) {
            _uiState.update {
                val alerta = if (it.alertaItemProducto?.origen == OrigenAlertaProducto.BUSCADOR) null else it.alertaItemProducto
                it.copy(buscando = false, resultadosBusqueda = emptyList(), alertaItemProducto = alerta)
            }
            sincronizarObservacionProductos()
            return
        }

        jobBusqueda = viewModelScope.launch {
            delay(200)
            _uiState.update { it.copy(buscando = true) }
            try {
                val hallazgo = buscarTodoLoQueCoincida(texto)
                val moldes = hallazgo.productos

                // CRÍTICO 4 & 5: Auto-agregar SOLO si es coincidencia exacta de CÓDIGO (código de barras, código base, secundario o fracción -B/-U).
                // NUNCA auto-agregar por coincidencia de texto de nombre.
                if (moldes.size == 1 && esCoincidenciaExactaCodigo(moldes.first(), texto)) {
                    val unicoProd = moldes.first()
                    val presResuelta = unicoProd.resolverPresentacionPorCodigo(texto)
                    val cantEnCarro = _uiState.value.carrito.any { it.productoId == unicoProd.indice && it.presentacionId == presResuelta.presentacionId }
                    val origenAlerta = if (cantEnCarro) OrigenAlertaProducto.CARRITO else OrigenAlertaProducto.BUSCADOR
                    agregarItemResuelto(unicoProd, presResuelta, 1, origenAlerta)
                    if (!cantEnCarro && _uiState.value.carrito.none { it.productoId == unicoProd.indice && it.presentacionId == presResuelta.presentacionId }) {
                        _uiState.update { it.copy(buscando = false, resultadosBusqueda = listOf(unicoProd), busquedaTruncada = false) }
                    } else {
                        _uiState.update { it.copy(busquedaTexto = "", buscando = false, resultadosBusqueda = emptyList(), busquedaTruncada = false) }
                    }
                    sincronizarObservacionProductos()
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        buscando = false,
                        resultadosBusqueda = moldes,
                        busquedaTruncada = hallazgo.truncada,
                        error = null
                    )
                }
                sincronizarObservacionProductos()
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
        val check = ReglaBloqueoTurnoCaja.validarPermiteAgregarAlCarrito(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(busquedaTexto = "", buscando = false, resultadosBusqueda = emptyList(), error = check.exceptionOrNull()?.message) }
            return
        }
        val texto = _uiState.value.busquedaTexto.trim()
        if (texto.isBlank()) return
        jobBusqueda?.cancel()

        jobBusqueda = viewModelScope.launch {
            _uiState.update { it.copy(buscando = true) }
            try {
                val hallazgo = buscarTodoLoQueCoincida(texto)
                val moldes = hallazgo.productos

                // Auto-agregar si hay match exacto de código
                val matchExacto = moldes.firstOrNull { esCoincidenciaExactaCodigo(it, texto) }
                if (matchExacto != null) {
                    val presResuelta = matchExacto.resolverPresentacionPorCodigo(texto)
                    val cantEnCarro = _uiState.value.carrito.any { it.productoId == matchExacto.indice && it.presentacionId == presResuelta.presentacionId }
                    val origenAlerta = if (cantEnCarro) OrigenAlertaProducto.CARRITO else OrigenAlertaProducto.BUSCADOR
                    agregarItemResuelto(matchExacto, presResuelta, 1, origenAlerta)
                    if (!cantEnCarro && _uiState.value.carrito.none { it.productoId == matchExacto.indice && it.presentacionId == presResuelta.presentacionId }) {
                        _uiState.update { it.copy(buscando = false, resultadosBusqueda = listOf(matchExacto), busquedaTruncada = false) }
                    } else {
                        _uiState.update { it.copy(busquedaTexto = "", buscando = false, resultadosBusqueda = emptyList(), busquedaTruncada = false) }
                    }
                    sincronizarObservacionProductos()
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        buscando = false,
                        resultadosBusqueda = moldes,
                        busquedaTruncada = hallazgo.truncada,
                        error = null
                    )
                }
                sincronizarObservacionProductos()
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
        _uiState.update {
            val alerta = if (it.alertaItemProducto?.origen == OrigenAlertaProducto.BUSCADOR) null else it.alertaItemProducto
            it.copy(busquedaTexto = "", buscando = false, resultadosBusqueda = emptyList(), alertaItemProducto = alerta)
        }
        sincronizarObservacionProductos()
    }

    // ───────────────────────────── CARRITO DE VENTAS ─────────────────────────────

    fun agregarAlCarrito(
        producto: MoldeProductos,
        presentacion: PresentacionProducto,
        cantidadInicial: Int = 1,
        origen: OrigenAlertaProducto = OrigenAlertaProducto.BUSCADOR
    ) {
        val check = ReglaBloqueoTurnoCaja.validarPermiteAgregarAlCarrito(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(error = check.exceptionOrNull()?.message) }
            return
        }
        val res = ResolvedProductPresentation(
            presentacionId = presentacion.presentacionId,
            nombrePresentacion = presentacion.nombre,
            cantidadUnidades = presentacion.cantidad,
            precioVenta = presentacion.precioventa
        )
        agregarItemResuelto(producto, res, cantidadInicial, origen)
    }

    private fun calcularStockMaximoPresentacion(producto: MoldeProductos, presentacionId: String): Int {
        return producto.calcularStockMaximoPresentacion(presentacionId)
    }

    private fun agregarItemResuelto(
        producto: MoldeProductos,
        res: ResolvedProductPresentation,
        cantidadInicial: Int = 1,
        origen: OrigenAlertaProducto = OrigenAlertaProducto.BUSCADOR
    ) {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.vaciandoCarrito) return
        val checkTurno = ReglaBloqueoTurnoCaja.validarPermiteAgregarAlCarrito(_uiState.value.estadoCaja)
        if (checkTurno.isFailure) {
            _uiState.update { it.copy(error = checkTurno.exceptionOrNull()?.message) }
            return
        }
        // CRÍTICO 6: Bloquear productos resueltos con precio <= 0.00
        if (res.precioVenta <= 0.0) {
            notificarAlertaProducto(producto.indice, res.presentacionId, "La presentación '${res.nombrePresentacion}' tiene precio S/ 0.00 y no puede ser agregada.", origen)
            return
        }
        // Una sola puerta: desactivado en inventario no entra al carrito por ningún camino
        // (toque, escáner, auto-agregado por código). Se avisa en el mismo producto.
        if (!producto.activo) {
            notificarAlertaProducto(producto.indice, res.presentacionId, "El producto '${producto.nombre}' fue desactivado en inventario y no se puede vender.", origen)
            return
        }

        val maxDisponible = calcularStockMaximoPresentacion(producto, res.presentacionId)
        if (maxDisponible <= 0) {
            notificarAlertaProducto(producto.indice, res.presentacionId, "El producto '${producto.nombre}' no cuenta con stock disponible en lotes vigentes.", origen)
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
                notificarAlertaProducto(producto.indice, res.presentacionId, "Solo quedan $maxDisponible ${res.nombrePresentacion} disponibles en stock.", origen)
                return
            }
            carritoActual[indexExistente] = itemExistente.copy(
                cantidad = nuevaCant,
                subtotal = kotlin.math.round(itemExistente.precioUnitario * nuevaCant * 100.0) / 100.0
            )
        } else {
            if (cantidadInicial > maxDisponible) {
                notificarAlertaProducto(producto.indice, res.presentacionId, "Solo quedan $maxDisponible ${res.nombrePresentacion} disponibles en stock.", origen)
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

        productosEnCarrito[producto.indice] = producto
        val nuevoMapa = calcularMapaDisponibilidad(carritoActual, productosEnCarrito)
        val idActual = _uiState.value.idempotenciaIdActual.ifBlank { "v_${UUID.randomUUID()}" }
        _uiState.update { autoAjustarLineasPago(it.copy(carrito = carritoActual, mapaDisponibilidad = nuevoMapa, idempotenciaIdActual = idActual, error = null)) }
        persistirBorradorActual()
        sincronizarObservacionProductos()
    }

    fun cambiarCantidadItem(
        productoId: String,
        presentacionId: String,
        delta: Int,
        origen: OrigenAlertaProducto = OrigenAlertaProducto.CARRITO
    ) {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.vaciandoCarrito) return
        val check = ReglaBloqueoTurnoCaja.validarPermiteModificarCarrito(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(error = check.exceptionOrNull()?.message) }
            return
        }
        val carritoActual = _uiState.value.carrito.toMutableList()
        val index = carritoActual.indexOfFirst { it.productoId == productoId && it.presentacionId == presentacionId }
        if (index >= 0) {
            val item = carritoActual[index]
            val nuevaCant = item.cantidad + delta
            if (nuevaCant <= 0) {
                carritoActual.removeAt(index)
                if (carritoActual.none { it.productoId == productoId }) {
                    productosEnCarrito.remove(productoId)
                }
            } else {
                if (delta > 0) {
                    val producto = productosEnCarrito[productoId] ?: _uiState.value.resultadosBusqueda.firstOrNull { it.indice == productoId }
                    if (producto == null) {
                        notificarAlertaProducto(productoId, presentacionId, "Se está verificando la disponibilidad. Intenta de nuevo en unos segundos.", origen)
                        sincronizarObservacionProductos()
                        return
                    }
                    val maxDisponible = calcularStockMaximoPresentacion(producto, presentacionId)
                    if (nuevaCant > maxDisponible) {
                        notificarAlertaProducto(productoId, presentacionId, "Solo quedan $maxDisponible ${item.presentacionNombre} disponibles en inventario.", origen)
                        return
                    }
                }
                carritoActual[index] = item.copy(
                    cantidad = nuevaCant,
                    subtotal = kotlin.math.round(item.precioUnitario * nuevaCant * 100.0) / 100.0
                )
            }
            val nuevoMapa = calcularMapaDisponibilidad(carritoActual, productosEnCarrito)
            val idActual = _uiState.value.idempotenciaIdActual.ifBlank { "v_${UUID.randomUUID()}" }
            _uiState.update { autoAjustarLineasPago(it.copy(carrito = carritoActual, mapaDisponibilidad = nuevoMapa, idempotenciaIdActual = idActual)) }
            persistirBorradorActual()
            sincronizarObservacionProductos()
        }
    }

    /**
     * Establece directamente la cantidad de un producto en el carrito (FASE 10 velocidad de mostrador).
     */
    fun setCantidadItem(
        productoId: String,
        presentacionId: String,
        cantidadDeseada: Int,
        origen: OrigenAlertaProducto = OrigenAlertaProducto.CARRITO
    ) {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.vaciandoCarrito) return
        val check = ReglaBloqueoTurnoCaja.validarPermiteModificarCarrito(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(error = check.exceptionOrNull()?.message) }
            return
        }
        if (cantidadDeseada <= 0) {
            eliminarItemCarrito(productoId, presentacionId)
            return
        }
        val carritoActual = _uiState.value.carrito.toMutableList()
        val index = carritoActual.indexOfFirst { it.productoId == productoId && it.presentacionId == presentacionId }
        if (index >= 0) {
            val item = carritoActual[index]
            val producto = productosEnCarrito[productoId] ?: _uiState.value.resultadosBusqueda.firstOrNull { it.indice == productoId }
            if (producto == null) {
                notificarAlertaProducto(productoId, presentacionId, "Se está verificando la disponibilidad. Intenta de nuevo en unos segundos.", origen)
                sincronizarObservacionProductos()
                return
            }
            val maxDisponible = calcularStockMaximoPresentacion(producto, presentacionId)
            if (cantidadDeseada > maxDisponible) {
                notificarAlertaProducto(productoId, presentacionId, "Solo quedan $maxDisponible ${item.presentacionNombre} disponibles en inventario.", origen)
                return
            }
            carritoActual[index] = item.copy(
                cantidad = cantidadDeseada,
                subtotal = kotlin.math.round(item.precioUnitario * cantidadDeseada * 100.0) / 100.0
            )
            val nuevoMapa = calcularMapaDisponibilidad(carritoActual, productosEnCarrito)
            val idActual = _uiState.value.idempotenciaIdActual.ifBlank { "v_${UUID.randomUUID()}" }
            _uiState.update { autoAjustarLineasPago(it.copy(carrito = carritoActual, mapaDisponibilidad = nuevoMapa, idempotenciaIdActual = idActual, error = null)) }
            persistirBorradorActual()
            sincronizarObservacionProductos()
        } else {
            val producto = productosEnCarrito[productoId] ?: _uiState.value.resultadosBusqueda.firstOrNull { it.indice == productoId }
            val pres = producto?.presentaciones?.firstOrNull { it.presentacionId == presentacionId }
                ?: if (producto != null) {
                    PresentacionProducto(
                        presentacionId = presentacionId,
                        nombre = producto.empaque.ifBlank { "Unidad" },
                        empaque = producto.empaque.ifBlank { "Unidad" },
                        cantidad = 1,
                        unidadMedida = producto.unidadBase.ifBlank { "unidad" },
                        precioventa = producto.precioVenta,
                        codigoBarras = producto.codigo
                    )
                } else null
            if (producto != null && pres != null) {
                agregarAlCarrito(producto, pres, cantidadDeseada, origen)
            }
        }
    }

    fun eliminarItemCarrito(productoId: String, presentacionId: String) {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.vaciandoCarrito) return
        val check = ReglaBloqueoTurnoCaja.validarPermiteModificarCarrito(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(error = check.exceptionOrNull()?.message) }
            return
        }
        val carritoActual = _uiState.value.carrito.filterNot {
            it.productoId == productoId && it.presentacionId == presentacionId
        }
        if (carritoActual.none { it.productoId == productoId }) {
            productosEnCarrito.remove(productoId)
        }
        val nuevoMapa = calcularMapaDisponibilidad(carritoActual, productosEnCarrito)
        _uiState.update { autoAjustarLineasPago(it.copy(carrito = carritoActual, mapaDisponibilidad = nuevoMapa)) }
        persistirBorradorActual()
        sincronizarObservacionProductos()
    }

    /**
     * Cambia la presentación de un ítem agotado en el carrito hacia una presentación alternativa con stock.
     */
    fun cambiarPresentacionItem(productoId: String, presentacionIdActual: String, nuevaPresentacionId: String) {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.vaciandoCarrito) return
        val check = ReglaBloqueoTurnoCaja.validarPermiteModificarCarrito(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(error = check.exceptionOrNull()?.message) }
            return
        }
        val producto = productosEnCarrito[productoId]
            ?: _uiState.value.resultadosBusqueda.firstOrNull { it.indice == productoId }
        if (producto == null) {
            _uiState.update { it.copy(error = "No se encontró la información del producto para cambiar presentación.") }
            return
        }

        val nuevaPres = producto.presentaciones.firstOrNull { it.presentacionId == nuevaPresentacionId }
        if (nuevaPres == null) {
            _uiState.update { it.copy(error = "La presentación seleccionada no existe en el catálogo.") }
            return
        }

        val maxDisponible = calcularStockMaximoPresentacion(producto, nuevaPresentacionId)
        if (maxDisponible <= 0) {
            notificarAlertaProducto(productoId, presentacionIdActual, "La presentación '${nuevaPres.nombre}' tampoco cuenta con stock disponible.", OrigenAlertaProducto.CARRITO)
            return
        }

        val carritoActual = _uiState.value.carrito.toMutableList()
        val indexActual = carritoActual.indexOfFirst { it.productoId == productoId && it.presentacionId == presentacionIdActual }
        if (indexActual < 0) return

        val itemActual = carritoActual[indexActual]
        // Cantidad para la nueva presentación: mínimo entre la cantidad previa y el stock disponible (al menos 1)
        val nuevaCantidad = itemActual.cantidad.coerceIn(1, maxDisponible)

        // Trazabilidad de lote FEFO para la nueva presentación
        val fefoSim = com.app.administradorfarmadon.inventario.compartido.logica.UnidadVentaHelper
            .calcularDescuentoFEFO(producto, nuevaPres)
        val primerLote = fefoSim.getOrNull()?.firstOrNull()
        val loteSugerido = primerLote?.loteNumero ?: ""
        val loteObj = producto.lotes.values.firstOrNull { it.loteId == primerLote?.loteId || it.numero == primerLote?.loteNumero }
        val loteVencimientoSugerido = loteObj?.vencimiento ?: ""

        val itemModificado = itemActual.copy(
            presentacionId = nuevaPres.presentacionId,
            presentacionNombre = nuevaPres.nombre,
            empaque = nuevaPres.empaque,
            precioUnitario = nuevaPres.precioventa,
            cantidad = nuevaCantidad,
            subtotal = kotlin.math.round(nuevaPres.precioventa * nuevaCantidad * 100.0) / 100.0,
            loteSugerido = loteSugerido,
            loteVencimientoSugerido = loteVencimientoSugerido
        )

        // ¿Ya existía en el carrito la nueva presentación?
        val indexNuevaPresExistente = carritoActual.indexOfFirst {
            it.productoId == productoId && it.presentacionId == nuevaPresentacionId
        }

        if (indexNuevaPresExistente >= 0 && indexNuevaPresExistente != indexActual) {
            val itemExistente = carritoActual[indexNuevaPresExistente]
            val cantFusionada = (itemExistente.cantidad + nuevaCantidad).coerceAtMost(maxDisponible)
            carritoActual[indexNuevaPresExistente] = itemExistente.copy(
                cantidad = cantFusionada,
                subtotal = kotlin.math.round(itemExistente.precioUnitario * cantFusionada * 100.0) / 100.0
            )
            carritoActual.removeAt(indexActual)
        } else {
            carritoActual[indexActual] = itemModificado
        }

        val nuevoMapa = calcularMapaDisponibilidad(carritoActual, productosEnCarrito)

        _uiState.update { state ->
            autoAjustarLineasPago(
                state.copy(
                    carrito = carritoActual,
                    mapaDisponibilidad = nuevoMapa,
                    error = null,
                    mensajeExito = "Se cambió a la presentación '${nuevaPres.nombre}' (Stock: $maxDisponible)."
                )
            )
        }
        persistirBorradorActual()
        sincronizarObservacionProductos()
    }

    /**
     * Vaciar seguro: un solo camino, una sola verdad, instantáneo.
     * Cierra búsqueda en vuelo, limpia TODO lo temporal en memoria y en disco
     * de forma síncrona. Al ser local y síncrono no genera duplicados,
     * no deja basura y lo siguiente que se agregue nace limpio y rápido.
     */
    fun vaciarCarrito() {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.vaciandoCarrito) return
        val check = ReglaBloqueoTurnoCaja.validarPermiteModificarCarrito(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(error = check.exceptionOrNull()?.message) }
            return
        }
        if (_uiState.value.carrito.isEmpty() && _uiState.value.busquedaTexto.isBlank()) {
            return
        }

        _uiState.update { it.copy(vaciandoCarrito = true) }
        try {
            jobBusqueda?.cancel()
            productosEnCarrito.clear()
            jobObservadorProductosActivos?.cancel()
            jobObservadorProductosActivos = null
            ultimosIdsObservados = emptySet()

            _uiState.update {
                it.copy(
                    vaciandoCarrito = false,
                    carrito = emptyList(),
                    mapaDisponibilidad = emptyMap(),
                    alertaItemProducto = null,
                    cliente = ClienteDeVenta(),
                    descuento = 0.0,
                    supervisorAutorizante = null,
                    resultadoConsultaDoc = null,
                    mostrarOverlayCobro = false,
                    mostrarDialogoDescuento = false,
                    mostrarDialogoCliente = false,
                    mostrarDialogoSuspender = false,
                    lineasPago = emptyList(),
                    idempotenciaIdActual = "",
                    busquedaTexto = "",
                    buscando = false,
                    resultadosBusqueda = emptyList(),
                    error = null,
                    mensajeExito = null
                )
            }

            val farmaciaId = SessionManager.clienteIdGarantizado
            val sucursalId = SessionManager.sucursalIdEfectiva
            val cajeroId = SessionManager.idCajera
            if (farmaciaId.isNotBlank() && sucursalId.isNotBlank()) {
                try {
                    VentaBorradorLocalStore.limpiarBorrador(
                        farmaciaId = farmaciaId,
                        sucursalId = sucursalId,
                        cajeroId = cajeroId
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error limpiando borrador al vaciar: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vaciando carrito: ${e.message}", e)
            _uiState.update { it.copy(vaciandoCarrito = false, error = e.message ?: "No se pudo vaciar el carrito.") }
        }
    }

    fun abrirDialogoDescuento() {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.vaciandoCarrito) return
        val check = ReglaBloqueoTurnoCaja.validarPermiteModificarCarrito(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(error = check.exceptionOrNull()?.message) }
            return
        }
        _uiState.update { it.copy(mostrarDialogoDescuento = true, error = null) }
    }

    fun cerrarDialogoDescuento() {
        _uiState.update { it.copy(mostrarDialogoDescuento = false) }
    }

    fun setDescuento(monto: Double) {
        aplicarDescuento(monto)
    }

    /**
     * Mostrador real: si el carrito se achicó y el descuento quedó fuera de tope,
     * se recorta al máximo permitido en un toque. Nunca se cambia plata en silencio:
     * se avisa el nuevo monto.
     */
    fun ajustarDescuentoAlTope() {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.vaciandoCarrito) return
        val check = ReglaBloqueoTurnoCaja.validarPermiteModificarCarrito(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(error = check.exceptionOrNull()?.message) }
            return
        }
        val subtotal = _uiState.value.subtotal
        if (subtotal <= 0.0 || _uiState.value.descuento <= 0.0) return
        val tope = topeDescuentoPermitido(subtotal)
        _uiState.update {
            autoAjustarLineasPago(
                it.copy(
                    descuento = tope,
                    supervisorAutorizante = null,
                    error = null,
                    mensajeExito = if (tope <= 0.0) "Descuento retirado: el carrito ya no lo permite." else "Descuento ajustado al tope: S/ ${String.format(Locale.US, "%.2f", tope)}."
                )
            )
        }
        persistirBorradorActual()
    }

    fun aplicarDescuento(monto: Double, autorizante: AutorizacionSupervisor? = null) {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.vaciandoCarrito) return
        val check = ReglaBloqueoTurnoCaja.validarPermiteModificarCarrito(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(error = check.exceptionOrNull()?.message) }
            return
        }
        val d = kotlin.math.round(monto.coerceAtLeast(0.0) * 100.0) / 100.0
        val subtotal = _uiState.value.subtotal
        val config = _uiState.value.posConfig
        val pct = if (subtotal > 0.0) (d / subtotal) * 100.0 else 0.0

        if (d > 0.0 && subtotal > 0.0 && d >= subtotal) {
            _uiState.update {
                it.copy(
                    error = "El descuento (S/ ${String.format(Locale.US, "%.2f", d)}) no puede ser igual o mayor al subtotal (S/ ${String.format(Locale.US, "%.2f", subtotal)}). La venta debe quedar con total mayor a S/ 0.00."
                )
            }
            return
        }

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
            autoAjustarLineasPago(
                it.copy(
                    descuento = d,
                    supervisorAutorizante = null,
                    mostrarDialogoDescuento = false,
                    error = null
                )
            )
        }
        persistirBorradorActual()
    }

    /**
     * Marca de receta POR PRODUCTO en su fila del carrito: la cajera confirma
     * haber visto la receta de ESE producto. El cobro exige todas marcadas.
     */
    fun setRecetaItemVerificada(productoId: String, presentacionId: String, verificada: Boolean) {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.vaciandoCarrito) return
        val check = ReglaBloqueoTurnoCaja.validarPermiteModificarCarrito(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(error = check.exceptionOrNull()?.message) }
            return
        }
        val carritoActual = _uiState.value.carrito.map { item ->
            if (item.productoId == productoId && item.presentacionId == presentacionId && item.requiereReceta) {
                item.copy(recetaVerificada = verificada)
            } else item
        }
        _uiState.update { it.copy(carrito = carritoActual, error = null) }
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

    private var jobConsultaDoc: Job? = null

    /**
     * Un solo camino: primero el directorio de LA farmacia, solo si no está
     * se llama a RENIEC/SUNAT. Buscar nunca escribe: el guardado ocurre
     * una sola vez al cobrar (o al guardar manual explícito).
     */
    fun consultarDocumentoCliente(tipo: String, numero: String) {
        val numLimpio = numero.filter { it.isDigit() }.trim()
        val tipoNorm = tipo.trim().uppercase()
        if (numLimpio.isBlank()) return
        if ((tipoNorm == "DNI" && numLimpio.length != 8) || (tipoNorm == "RUC" && numLimpio.length != 11)) {
            jobConsultaDoc?.cancel()
            _uiState.update {
                it.copy(
                    consultandoDoc = false,
                    resultadoConsultaDoc = ResultadoDocUi.Error(
                        tipo = tipoNorm.ifBlank { tipo },
                        numero = numLimpio,
                        mensaje = if (tipoNorm == "RUC") "El RUC debe tener 11 dígitos." else "El DNI debe tener 8 dígitos."
                    )
                )
            }
            return
        }

        // 1. Verificar si ya existe en el directorio local de la farmacia (sin llamar fuera)
        val existente = _uiState.value.directorioClientes.firstOrNull { it.numeroDocumento.filter { c -> c.isDigit() } == numLimpio }
        if (existente != null) {
            jobConsultaDoc?.cancel()
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

        // 2. Si no existe en el directorio, consultar API oficial (RENIEC / SUNAT). Sin carrera:
        // se cancela la anterior para que un resultado viejo nunca pise al nuevo.
        jobConsultaDoc?.cancel()
        jobConsultaDoc = viewModelScope.launch {
            _uiState.update { it.copy(consultandoDoc = true, resultadoConsultaDoc = null, error = null) }
            when (val res = ApiDocumentosPeru.consultar(tipoNorm.ifBlank { tipo }, numLimpio)) {
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
                    // Solo lectura: NO se guarda en directorio por buscar.
                    // Se guarda una sola vez al aplicar/cobrar para no llenar de fichas que nunca compraron.
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

    /**
     * Aplicar es solo atar el cliente a LA venta en curso.
     * El guardado en directorio ocurre una sola vez al cobrar.
     */
    fun aplicarResultadoCliente(res: ResultadoDocUi.Encontrado) {
        jobConsultaDoc?.cancel()
        val nuevoCliente = ClienteDeVenta(
            tipoDocumento = res.tipo,
            numeroDocumento = res.numero,
            nombre = res.nombreCompleto,
            direccion = res.direccion,
            clienteId = res.numero
        )
        _uiState.update {
            it.copy(
                cliente = nuevoCliente,
                consultandoDoc = false,
                resultadoConsultaDoc = null,
                mensajeExito = "Cliente asignado a la venta: ${res.nombreCompleto}"
            )
        }
        persistirBorradorActual()
    }

    fun limpiarResultadoConsultaDoc() {
        jobConsultaDoc?.cancel()
        _uiState.update { it.copy(consultandoDoc = false, resultadoConsultaDoc = null) }
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
        jobConsultaDoc?.cancel()
        val numLimpio = numero.filter { it.isDigit() }.trim()
        val nomLimpio = nombre.trim()
        if (nomLimpio.isBlank()) {
            _uiState.update { it.copy(error = "El nombre del cliente es obligatorio.") }
            return
        }

        val tipoNorm = if (tipo.isNotBlank()) tipo.trim().uppercase() else if (numLimpio.length == 11) "RUC" else "DNI"
        if (numLimpio.isNotBlank() && !((tipoNorm == "DNI" && numLimpio.length == 8) || (tipoNorm == "RUC" && numLimpio.length == 11))) {
            _uiState.update { it.copy(error = "El documento debe ser DNI de 8 dígitos o RUC de 11 dígitos.") }
            return
        }
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
        jobConsultaDoc?.cancel()
        _uiState.update {
            it.copy(
                cliente = ClienteDeVenta(
                    tipoDocumento = "NINGUNO",
                    numeroDocumento = "",
                    nombre = "Consumidor Final",
                    clienteId = ""
                ),
                consultandoDoc = false,
                resultadoConsultaDoc = null,
                error = null
            )
        }
        persistirBorradorActual()
    }

    // ───────────────────────────── FLUJO DE COBRO E IDEMPOTENCIA ─────────────────────────────

    private fun autoAjustarLineasPago(state: NuevaVentaUiState): NuevaVentaUiState {
        val total = state.total
        if (state.carrito.isEmpty()) {
            return state.copy(lineasPago = emptyList())
        }
        val primerActivo = state.metodosPagoDisponibles.firstOrNull { it.tipoId == "EFECTIVO" }
            ?: state.metodosPagoDisponibles.firstOrNull()

        if (state.lineasPago.isEmpty()) {
            if (primerActivo != null) {
                val tipoMetodo = TIPOS_PAGO_FIJOS.firstOrNull { it.id == primerActivo.tipoId }
                val nombreMetodo = tipoMetodo?.nombre ?: primerActivo.tipoId
                return state.copy(
                    lineasPago = listOf(
                        PagoVenta(
                            tipoId = primerActivo.tipoId,
                            instanciaId = primerActivo.id,
                            nombreMetodo = nombreMetodo,
                            monto = total,
                            numeroOperacion = ""
                        )
                    )
                )
            }
        } else if (state.lineasPago.size == 1) {
            val unica = state.lineasPago.first()
            if (unica.tipoId == "EFECTIVO") {
                // Mostrador real: lo que el cliente ya entregó no se borra solo.
                // Solo se auto-completa cuando falta; si ya cubre (exacto o con vuelto), se conserva.
                if (unica.monto < total - 0.009) {
                    return state.copy(
                        lineasPago = listOf(unica.copy(monto = total))
                    )
                }
            } else if (unica.monto != total) {
                // Yape / Plin / Tarjeta / Transferencia siempre van exactos al total.
                return state.copy(
                    lineasPago = listOf(unica.copy(monto = total))
                )
            }
        }
        return state
    }

    fun abrirOverlayCobro() {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.vaciandoCarrito) return
        val estadoActual = _uiState.value
        val checkTurno = ReglaBloqueoTurnoCaja.validarPermiteVenta(estadoActual.estadoCaja)
        if (checkTurno.isFailure) {
            _uiState.update { it.copy(error = checkTurno.exceptionOrNull()?.message) }
            return
        }
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
        if (estadoActual.hayProblemasDisponibilidad) {
            _uiState.update { it.copy(error = "No puedes cobrar: hay presentaciones o productos agotados en el carrito. Retíralos o cámbialos por una presentación disponible.") }
            return
        }
        if (!estadoActual.recetaTodoVerificado) {
            _uiState.update { it.copy(error = "Marca la receta como verificada en cada producto que la requiere, en su fila del carrito.") }
            return
        }
        if (estadoActual.metodosPagoDisponibles.isEmpty()) {
            _uiState.update { it.copy(error = "No hay métodos de pago activos en esta sede. Activa al menos uno en Configuración > Métodos de Pago.") }
            return
        }
        val total = estadoActual.total
        val idIdem = estadoActual.idempotenciaIdActual.ifBlank { "v_${UUID.randomUUID()}" }

        // Si había una consulta oficial verificada pendiente de aplicar al carrito, se auto-aplica antes de abrir el cobro
        val resDoc = estadoActual.resultadoConsultaDoc
        if (estadoActual.cliente.numeroDocumento.isBlank() && resDoc is ResultadoDocUi.Encontrado) {
            aplicarResultadoCliente(resDoc)
        }

        // Si hay método activo en metodosPagoDisponibles (priorizando EFECTIVO si existe), precargarlo; si no, dejar vacío
        val primerMetodoActivo = estadoActual.metodosPagoDisponibles.firstOrNull { it.tipoId == "EFECTIVO" }
            ?: estadoActual.metodosPagoDisponibles.firstOrNull()
        val tipoMetodoInfo = primerMetodoActivo?.let { act -> TIPOS_PAGO_FIJOS.firstOrNull { it.id == act.tipoId } }
        val lineaDefecto = if (primerMetodoActivo != null) {
            listOf(
                PagoVenta(
                    tipoId = primerMetodoActivo.tipoId,
                    instanciaId = primerMetodoActivo.id,
                    nombreMetodo = tipoMetodoInfo?.nombre ?: primerMetodoActivo.tipoId,
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
        if (_uiState.value.procesandoCobro) return
        _uiState.update { it.copy(mostrarOverlayCobro = false) }
    }

    /**
     * Puerta contable: solo entran métodos activos de ESTA sucursal.
     * Si algo llegó inactivo por carrera (se desactivó hace segundos), se rechaza
     * con mensaje claro en vez de dejar información rota a medias.
     */
    fun actualizarLineasPago(nuevasLineas: List<PagoVenta>) {
        if (_uiState.value.procesandoCobro) return
        val activas = _uiState.value.metodosPagoDisponibles
        if (nuevasLineas.isNotEmpty() && activas.isNotEmpty()) {
            val idsActivas = activas.map { it.id }.toSet()
            val tiposActivos = activas.map { it.tipoId }.toSet()
            val invalida = nuevasLineas.firstOrNull { linea ->
                if (linea.instanciaId.isNotBlank()) linea.instanciaId !in idsActivas
                else linea.tipoId !in tiposActivos
            }
            if (invalida != null) {
                _uiState.update {
                    it.copy(error = "El método '${invalida.nombreMetodo.ifBlank { invalida.tipoId }}' ya no está activo en esta sucursal. Elige uno activo.")
                }
                return
            }
        }
        _uiState.update { it.copy(lineasPago = nuevasLineas) }
    }

    fun confirmarVenta() {
        val estado = _uiState.value
        if (estado.procesandoCobro || estado.suspendiendoVenta || estado.reanudandoVentaId != null || estado.vaciandoCarrito) return
        val checkTurno = ReglaBloqueoTurnoCaja.validarPermiteVenta(estado.estadoCaja)
        if (checkTurno.isFailure) {
            _uiState.update { it.copy(error = checkTurno.exceptionOrNull()?.message) }
            return
        }
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
        if (estado.carrito.any { it.productoId.startsWith("mock_") }) {
            _uiState.update {
                it.copy(
                    error = "Los productos de prueba UX son para evaluar la ergonomía visual del carrito y no pueden guardarse como venta fiscal real."
                )
            }
            return
        }
        if (estado.hayProblemasDisponibilidad) {
            _uiState.update {
                it.copy(error = "No puedes registrar la venta: hay presentaciones o productos agotados en el carrito. Retíralos o cámbialos por una presentación disponible.")
            }
            return
        }
        if (!estado.recetaTodoVerificado) {
            _uiState.update { it.copy(error = "Marca la receta como verificada en cada producto que la requiere, en su fila del carrito.") }
            return
        }
        if (estado.descuentoInvalido) {
            _uiState.update { it.copy(error = "El descuento (S/ ${String.format(Locale.US, "%.2f", estado.descuento)}) quedó igual o mayor al subtotal (S/ ${String.format(Locale.US, "%.2f", estado.subtotal)}) al cambiar el carrito. Ajusta el descuento para dejar total mayor a S/ 0.00.") }
            return
        }
        if (estado.descuentoExcedeTope) {
            val cfg = estado.posConfig.descuento
            _uiState.update { it.copy(error = "El descuento (S/ ${String.format(Locale.US, "%.2f", estado.descuento)} / ${String.format(Locale.US, "%.1f", estado.descuentoPct)}%) supera el tope de la sede (${cfg.maxPct}% / S/ ${String.format(Locale.US, "%.2f", cfg.maxMonto)}). Ajusta el descuento.") }
            return
        }
        // Cobro rápido de mostrador: no se exige N° de operación.
        // La verdad del pago es el método activo de la sucursal + el monto exacto.

        viewModelScope.launch {
            _uiState.update { it.copy(procesandoCobro = true, error = null) }

            val res = ventasRepository.registrarVenta(
                items = estado.carrito,
                cliente = estado.cliente,
                pagos = estado.lineasPago,
                descuento = estado.descuento,
                confirmoReceta = estado.recetaTodoVerificado,
                idempotenciaId = estado.idempotenciaIdActual,
                autorizadoPorId = "",
                autorizadoPorNombre = "",
                autorizadoPorRol = "",
                recetaVerificadaPor = if (estado.requiereReceta && estado.recetaTodoVerificado) SessionManager.nombreUsuario else "",
                recetaVerificadaEnMs = if (estado.requiereReceta && estado.recetaTodoVerificado) HoraServidor.ahoraMs() else 0L
            )

            res.onSuccess { ventaCompletada ->
                val farmaciaId = SessionManager.clienteIdGarantizado
                val sucursalId = SessionManager.sucursalIdEfectiva
                val cajeroId = SessionManager.idCajera
                VentaBorradorLocalStore.limpiarBorrador(
                    farmaciaId = farmaciaId,
                    sucursalId = sucursalId,
                    cajeroId = cajeroId
                )
                productosEnCarrito.clear()

                // Único guardado: al cobrar se guarda el cliente nuevo en el directorio.
                // Si ya estaba con el mismo nombre, no se reescribe (cero duplicación/sobreescritura).
                val cli = ventaCompletada.cliente
                val docCli = cli.numeroDocumento.filter { it.isDigit() }.trim()
                val nombreCli = cli.nombre.trim()
                val tipoCli = cli.tipoDocumento.trim().uppercase()
                val docValido = (tipoCli == "DNI" && docCli.length == 8) || (tipoCli == "RUC" && docCli.length == 11)
                val yaRegistrado = _uiState.value.directorioClientes.firstOrNull {
                    it.numeroDocumento.filter { c -> c.isDigit() } == docCli
                }
                if (docValido && nombreCli.isNotBlank() && !nombreCli.equals("Consumidor Final", ignoreCase = true) &&
                    (yaRegistrado == null || !yaRegistrado.nombre.equals(nombreCli, ignoreCase = true))
                ) {
                    viewModelScope.launch {
                        val resDir = clientesRepository.guardarCliente(
                            ClienteFarmacia(
                                id = docCli,
                                tipoDocumento = tipoCli,
                                numeroDocumento = docCli,
                                nombre = nombreCli,
                                direccion = cli.direccion.trim(),
                                creadoPor = SessionManager.nombreUsuario
                            )
                        )
                        resDir.onFailure { e ->
                            Log.w(TAG, "No se pudo guardar cliente al cobrar: ${e.message}")
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
                        lineasPago = emptyList(),
                        idempotenciaIdActual = "", // Se resetea solo tras el éxito
                        cliente = ClienteDeVenta(),
                        mensajeExito = "¡Venta ${ventaCompletada.numeroCompleto} registrada exitosamente!"
                    )
                }
                sincronizarObservacionProductos()
            }.onFailure { err ->
                // Carrera al cobrar (otro vendió antes, cambió precio o se agotó):
                // se mantiene el contexto para corregir y se refresca la verdad viva
                // para que el carrito muestre de inmediato el stock y precio reales.
                _uiState.update {
                    it.copy(
                        procesandoCobro = false,
                        mostrarOverlayCobro = true,
                        error = err.message ?: "No se pudo registrar la venta."
                    )
                }
                sincronizarObservacionProductos()
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

    private var suspensionEnCurso = false

    /**
     * Suspende directamente la venta en curso sin diálogos ni fricción (1 solo clic).
     * Asigna un correlativo automático "Pausa 1", "Pausa 2", etc.
     * Prevención de colisiones: extrae los números activos y asigna el primer número disponible
     * para no sobreescribir ni duplicar si se restauró una pausa anterior.
     */
    fun suspenderVentaDirecta() {
        if (suspensionEnCurso || _uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.vaciandoCarrito) return
        val estado = _uiState.value
        val check = ReglaBloqueoTurnoCaja.validarPermiteSuspension(estado.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(error = check.exceptionOrNull()?.message) }
            return
        }
        if (estado.carrito.isEmpty()) {
            _uiState.update { it.copy(error = "El carrito está vacío, no hay nada que suspender.") }
            return
        }

        suspensionEnCurso = true
        _uiState.update { it.copy(suspendiendoVenta = true, mostrarDialogoSuspender = false, error = null) }
        viewModelScope.launch {
            val res = ventasRepository.suspenderVenta(
                items = estado.carrito,
                cliente = estado.cliente,
                descuento = estado.descuento,
                nota = ""
            )
            suspensionEnCurso = false
            res.onSuccess { ventaCreada ->
                jobBusqueda?.cancel()
                productosEnCarrito.clear()
                jobObservadorProductosActivos?.cancel()
                jobObservadorProductosActivos = null
                ultimosIdsObservados = emptySet()
                _uiState.update {
                    it.copy(
                        suspendiendoVenta = false,
                        carrito = emptyList(),
                        mapaDisponibilidad = emptyMap(),
                        alertaItemProducto = null,
                        cliente = ClienteDeVenta(),
                        descuento = 0.0,
                        supervisorAutorizante = null,
                        resultadoConsultaDoc = null,
                        mostrarDialogoDescuento = false,
                        mostrarDialogoCliente = false,
                        lineasPago = emptyList(),
                        idempotenciaIdActual = "",
                        busquedaTexto = "",
                        buscando = false,
                        resultadosBusqueda = emptyList(),
                        mensajeExito = "Venta pausada como ${ventaCreada.nota}."
                    )
                }
                persistirBorradorActual()
                sincronizarObservacionProductos()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        suspendiendoVenta = false,
                        error = err.message ?: "No se pudo suspender la venta."
                    )
                }
            }
        }
    }

    fun suspenderVenta(nota: String) {
        suspenderVentaDirecta()
    }

    fun abrirSheetSuspendidas() {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta) return
        val check = ReglaBloqueoTurnoCaja.validarPermiteReanudacion(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(error = check.exceptionOrNull()?.message) }
            return
        }
        _uiState.update { it.copy(mostrarSheetSuspendidas = true, error = null) }
    }

    fun cerrarSheetSuspendidas() {
        if (_uiState.value.reanudandoVentaId != null || _uiState.value.descartandoVentaId != null) return
        _uiState.update { it.copy(mostrarSheetSuspendidas = false) }
    }

    /**
     * Reanuda una venta suspendida de forma controlada y segura:
     * - Si el carrito actual está VACÍO: reanuda directamente sin diálogos ni fricción.
     * - Si el carrito actual TIENE PRODUCTOS: NO sobreescribe ni fusiona a ciegas.
     *   Abre el diálogo de conflicto para que el cajero decida (Vaciar Carrito o Añadir a la Venta Actual).
     */
    fun solicitarReanudarVenta(v: VentaSuspendida) {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.descartandoVentaId != null || _uiState.value.vaciandoCarrito) return
        val check = ReglaBloqueoTurnoCaja.validarPermiteReanudacion(_uiState.value.estadoCaja)
        if (check.isFailure) {
            _uiState.update { it.copy(mostrarSheetSuspendidas = false, error = check.exceptionOrNull()?.message) }
            return
        }
        if (_uiState.value.carrito.isEmpty()) {
            ejecutarReanudacionReemplazando(v)
        } else {
            _uiState.update {
                it.copy(
                    ventaPendienteReanudacion = v,
                    mostrarSheetSuspendidas = false,
                    error = null
                )
            }
        }
    }

    fun reanudarVentaSuspendida(v: VentaSuspendida) {
        solicitarReanudarVenta(v)
    }

    fun cancelarDialogoReanudacion() {
        if (_uiState.value.reanudandoVentaId != null) return
        _uiState.update { it.copy(ventaPendienteReanudacion = null) }
    }

    /**
     * Opción 1: VACIAR CARRITO Y REANUDAR, sin botar nada.
     * Negocio real: lo actual se guarda en pausa primero; recién ahí se trae la otra.
     * Ni un apagón deja sin ni una ni otra.
     */
    fun confirmarReanudacionVaciarYReanudar() {
        if (_uiState.value.reanudandoVentaId != null) return
        val venta = _uiState.value.ventaPendienteReanudacion ?: return
        val actualItems = _uiState.value.carrito.toList()
        if (actualItems.isEmpty()) {
            ejecutarReanudacionReemplazando(venta)
            return
        }
        val actualCliente = _uiState.value.cliente
        val actualDescuento = _uiState.value.descuento
        _uiState.update { it.copy(reanudandoVentaId = venta.id, error = null) }
        viewModelScope.launch {
            val resGuarda = ventasRepository.suspenderVenta(
                items = actualItems,
                cliente = actualCliente,
                descuento = actualDescuento,
                nota = ""
            )
            if (resGuarda.isFailure) {
                _uiState.update {
                    it.copy(
                        reanudandoVentaId = null,
                        error = "No se pudo proteger lo actual en pausa: ${resGuarda.exceptionOrNull()?.message}. No se tocó nada."
                    )
                }
                return@launch
            }
            val pausaNueva = resGuarda.getOrNull()
            productosEnCarrito.clear()
            _uiState.update {
                it.copy(
                    carrito = emptyList(),
                    mapaDisponibilidad = emptyMap(),
                    cliente = ClienteDeVenta(),
                    descuento = 0.0,
                    lineasPago = emptyList(),
                    reanudandoVentaId = null,
                    ventaPendienteReanudacion = venta,
                    mensajeExito = "Lo actual se guardó como ${pausaNueva?.nota ?: "pausa"}. Trayendo la otra..."
                )
            }
            persistirBorradorActual()
            ejecutarReanudacionReemplazando(venta)
        }
    }

    /**
     * Opción 2: AÑADIR A LA VENTA ACTUAL
     * Fusiona los productos de la venta suspendida sumándolos al carrito actual.
     */
    fun confirmarReanudacionAnadirACarrito() {
        if (_uiState.value.reanudandoVentaId != null) return
        val venta = _uiState.value.ventaPendienteReanudacion ?: return
        ejecutarReanudacionAnadiendo(venta)
    }

    private fun ejecutarReanudacionReemplazando(v: VentaSuspendida) {
        if (_uiState.value.reanudandoVentaId != null) return
        _uiState.update { it.copy(reanudandoVentaId = v.id, error = null) }
        viewModelScope.launch {
            val res = ventasRepository.reanudarVentaSuspendida(v.id)
            res.onSuccess { ventaRecuperada ->
                val frescos = obtenerMoldesFrescos(ventaRecuperada.items.map { it.productoId })
                productosEnCarrito.clear()
                frescos.forEach { (id, molde) -> productosEnCarrito[id] = molde }
                val reval = revalidarItemsPausados(ventaRecuperada.items, frescos)
                if (reval.items.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            reanudandoVentaId = null,
                            mostrarSheetSuspendidas = false,
                            ventaPendienteReanudacion = null,
                            error = "La venta pausada ya no tiene stock disponible (${reval.eliminados.firstOrNull() ?: "sin unidades vigentes"}). Se descartó para no vender humo."
                        )
                    }
                    persistirBorradorActual()
                    sincronizarObservacionProductos()
                    return@onSuccess
                }
                val subNuevo = kotlin.math.round(reval.items.sumOf { it.precioUnitario * it.cantidad } * 100.0) / 100.0
                var descFinal = ventaRecuperada.descuento
                if (descFinal >= subNuevo && subNuevo > 0.0) {
                    descFinal = topeDescuentoPermitido(subNuevo)
                }
                val mapaInicial = calcularMapaDisponibilidad(reval.items, productosEnCarrito)
                val detalleCambios = buildList {
                    if (reval.eliminados.isNotEmpty()) add("Se retiraron ${reval.eliminados.size} sin stock")
                    if (reval.ajustados.isNotEmpty()) add("Se ajustaron ${reval.ajustados.size} a lo disponible")
                    if (reval.preciosActualizados.isNotEmpty()) add("Se actualizaron ${reval.preciosActualizados.size} precios")
                }.joinToString(". ")
                val mensaje = if (detalleCambios.isBlank()) {
                    "Venta pausada reanudada en el carrito."
                } else {
                    "Venta pausada reanudada con limpieza automática. $detalleCambios."
                }
                _uiState.update {
                    autoAjustarLineasPago(
                        it.copy(
                            reanudandoVentaId = null,
                            carrito = reval.items,
                            mapaDisponibilidad = mapaInicial,
                            cliente = ventaRecuperada.cliente,
                            descuento = descFinal,
                            mostrarSheetSuspendidas = false,
                            ventaPendienteReanudacion = null,
                            lineasPago = emptyList(),
                            mensajeExito = mensaje,
                            error = if (reval.eliminados.isNotEmpty()) reval.eliminados.take(2).joinToString("; ") else null
                        )
                    )
                }
                persistirBorradorActual()
                sincronizarObservacionProductos()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        reanudandoVentaId = null,
                        ventaPendienteReanudacion = null,
                        error = err.message ?: "No se pudo reanudar la venta suspendida."
                    )
                }
            }
        }
    }

    private fun ejecutarReanudacionAnadiendo(v: VentaSuspendida) {
        if (_uiState.value.reanudandoVentaId != null) return
        _uiState.update { it.copy(reanudandoVentaId = v.id, error = null) }
        viewModelScope.launch {
            val res = ventasRepository.reanudarVentaSuspendida(v.id)
            res.onSuccess { ventaRecuperada ->
                // 1. Fusionar cantidades sin validar todavía (la verdad la pone el stock vivo).
                val fusionado = _uiState.value.carrito.toMutableList()
                for (nuevoItem in ventaRecuperada.items) {
                    val indexExistente = fusionado.indexOfFirst {
                        it.productoId == nuevoItem.productoId && it.presentacionId == nuevoItem.presentacionId
                    }
                    if (indexExistente >= 0) {
                        val actual = fusionado[indexExistente]
                        val nuevaCantidad = actual.cantidad + nuevoItem.cantidad
                        val nuevoSubtotal = kotlin.math.round(actual.precioUnitario * nuevaCantidad * 100.0) / 100.0
                        fusionado[indexExistente] = actual.copy(
                            cantidad = nuevaCantidad,
                            subtotal = nuevoSubtotal,
                            recetaVerificada = actual.recetaVerificada && nuevoItem.recetaVerificada
                        )
                    } else {
                        fusionado.add(nuevoItem)
                    }
                }

                // 2. Revalidar TODO lo fusionado contra el stock vivo: nunca sumar a ciegas.
                val frescos = obtenerMoldesFrescos(fusionado.map { it.productoId })
                frescos.forEach { (id, molde) -> productosEnCarrito[id] = molde }
                val reval = revalidarItemsPausados(fusionado, frescos)
                if (reval.items.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            reanudandoVentaId = null,
                            ventaPendienteReanudacion = null,
                            mostrarSheetSuspendidas = false,
                            error = "Al juntar la venta pausada todo quedó sin stock disponible. No se añadió nada para no vender humo."
                        )
                    }
                    persistirBorradorActual()
                    sincronizarObservacionProductos()
                    return@onSuccess
                }

                // Cliente real nunca se pierde: Consumidor Final / NINGUNO cede ante DNI-RUC real.
                val clienteActual = _uiState.value.cliente
                val clienteFinal = if (!esClienteGenerico(clienteActual)) {
                    clienteActual
                } else if (esClienteReal(ventaRecuperada.cliente)) {
                    ventaRecuperada.cliente
                } else {
                    clienteActual
                }

                // Descuento: se suma pero con tope de sede y de subtotal, nunca a ciegas.
                val subCombinado = kotlin.math.round(reval.items.sumOf { it.precioUnitario * it.cantidad } * 100.0) / 100.0
                val sumaDeseada = kotlin.math.round((_uiState.value.descuento + ventaRecuperada.descuento) * 100.0) / 100.0
                var descuentoFinal = sumaDeseada
                var avisoTope: String? = null
                if (sumaDeseada > 0.0 && subCombinado > 0.0) {
                    val pct = (sumaDeseada / subCombinado) * 100.0
                    if (sumaDeseada >= subCombinado || _uiState.value.posConfig.excedeLimitesDescuento(pct, sumaDeseada)) {
                        descuentoFinal = topeDescuentoPermitido(subCombinado)
                        avisoTope = "El descuento sumado superaba el tope y se ajustó a S/ ${String.format(Locale.US, "%.2f", descuentoFinal)}."
                    }
                } else if (sumaDeseada >= subCombinado && subCombinado > 0.0) {
                    descuentoFinal = topeDescuentoPermitido(subCombinado)
                    avisoTope = "El descuento sumado superaba el tope y se ajustó a S/ ${String.format(Locale.US, "%.2f", descuentoFinal)}."
                }
                val mapaActualizado = calcularMapaDisponibilidad(reval.items, productosEnCarrito)
                val detalleCambios = buildList {
                    if (reval.eliminados.isNotEmpty()) add("se retiraron ${reval.eliminados.size} sin stock")
                    if (reval.ajustados.isNotEmpty()) add("se ajustaron ${reval.ajustados.size} a lo disponible")
                    if (reval.preciosActualizados.isNotEmpty()) add("se actualizaron ${reval.preciosActualizados.size} precios")
                    if (avisoTope != null) add("descuento recortado al tope")
                }.joinToString(", ")

                _uiState.update {
                    autoAjustarLineasPago(
                        it.copy(
                            reanudandoVentaId = null,
                            carrito = reval.items,
                            mapaDisponibilidad = mapaActualizado,
                            cliente = clienteFinal,
                            descuento = descuentoFinal,
                            mostrarSheetSuspendidas = false,
                            ventaPendienteReanudacion = null,
                            mensajeExito = if (detalleCambios.isBlank()) "Productos añadidos al carrito actual." else "Productos añadidos con limpieza automática: $detalleCambios.",
                            error = avisoTope ?: if (reval.eliminados.isNotEmpty()) reval.eliminados.take(2).joinToString("; ") else null
                        )
                    )
                }
                persistirBorradorActual()
                sincronizarObservacionProductos()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        reanudandoVentaId = null,
                        ventaPendienteReanudacion = null,
                        error = err.message ?: "No se pudo fusionar la venta suspendida."
                    )
                }
            }
        }
    }

    fun descartarVentaSuspendida(id: String) {
        if (_uiState.value.procesandoCobro || _uiState.value.suspendiendoVenta || _uiState.value.reanudandoVentaId != null || _uiState.value.descartandoVentaId != null || _uiState.value.vaciandoCarrito) return
        _uiState.update { it.copy(descartandoVentaId = id, error = null) }
        viewModelScope.launch {
            val res = ventasRepository.eliminarSuspendida(id)
            res.onSuccess {
                _uiState.update { it.copy(descartandoVentaId = null) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        descartandoVentaId = null,
                        error = "No se pudo eliminar la venta suspendida: ${err.message}"
                    )
                }
            }
        }
    }

    fun notificarFallo(mensaje: String) {
        _uiState.update { it.copy(error = mensaje) }
    }

    fun notificarAlertaProducto(
        productoId: String,
        presentacionId: String,
        mensaje: String,
        origen: OrigenAlertaProducto = OrigenAlertaProducto.CARRITO
    ) {
        _uiState.update {
            it.copy(
                alertaItemProducto = AlertaItemProducto(
                    productoId = productoId,
                    presentacionId = presentacionId,
                    mensaje = mensaje,
                    origen = origen
                ),
                error = null
            )
        }
    }

    fun limpiarAlertaProducto() {
        _uiState.update { it.copy(alertaItemProducto = null) }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(error = null, mensajeExito = null, alertaItemProducto = null) }
    }

    /**
     * No-op en cumplimiento estricto de Regla R12 (Cero Datos de Relleno, Cero Seeds/Mocks).
     */
    fun cargarDatosPruebaUx() {
        // Operación deshabilitada por R12
    }
}
