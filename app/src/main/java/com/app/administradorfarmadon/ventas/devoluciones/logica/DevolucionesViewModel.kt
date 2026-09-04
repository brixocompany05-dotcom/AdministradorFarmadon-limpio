package com.app.administradorfarmadon.ventas.devoluciones.logica

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.configuracion.metodospago.datos.MetodosPagoRepository
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.ventas.compartido.datos.CajaRepository
import com.app.administradorfarmadon.ventas.compartido.datos.VentasRepository
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemDevolucionParam
import com.app.administradorfarmadon.ventas.compartido.modelo.ItemVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Estado UI de Devoluciones y Notas de Crédito (R1/R3/R8).
 */
data class DevolucionesUiState(
    val busquedaTexto: String = "",
    val buscando: Boolean = false,
    val resultadosBusqueda: List<Venta> = emptyList(),
    val ventasDelDia: List<Venta> = emptyList(),
    val ventaSeleccionada: Venta? = null,
    // Llave: "${item.productoId}_${item.presentacionId}" -> cantidad a devolver
    val itemsSeleccionados: Map<String, Int> = emptyMap(),
    val motivo: String = "",
    val metodosPagoDisponibles: List<InstanciaPago> = emptyList(),
    val instanciaReembolsoId: String = "",
    val estadoCaja: EstadoCaja = EstadoCaja(),
    val procesandoDevolucion: Boolean = false,
    val devolucionExitosa: DevolucionVenta? = null,
    val idempotenciaIdActual: String = "",
    val error: String? = null,
    val mensajeExito: String? = null
) {
    val cajaAbierta: Boolean
        get() = estadoCaja.estado == CajaSesion.ESTADO_ABIERTA

    val totalItemsADevolver: Int
        get() = itemsSeleccionados.values.sum()

    val montoBrutoDevolucion: Double
        get() {
            val v = ventaSeleccionada ?: return 0.0
            val suma = v.items.sumOf { item ->
                val cant = itemsSeleccionados["${item.productoId}_${item.presentacionId}"] ?: 0
                item.precioUnitario * cant
            }
            return kotlin.math.round(suma * 100.0) / 100.0
        }

    val factorDescuento: Double
        get() {
            val v = ventaSeleccionada ?: return 1.0
            return if (v.subtotal > 0.0) (1.0 - (v.descuento / v.subtotal)).coerceAtLeast(0.0) else 1.0
        }

    val montoReembolsoCalculado: Double
        get() {
            val v = ventaSeleccionada ?: return 0.0
            val factor = factorDescuento
            val sumaReembolsos = v.items.sumOf { item ->
                val cant = itemsSeleccionados["${item.productoId}_${item.presentacionId}"] ?: 0
                if (cant > 0) {
                    val montoBrutoItem = item.precioUnitario * cant
                    kotlin.math.round(montoBrutoItem * factor * 100.0) / 100.0
                } else 0.0
            }
            return kotlin.math.round(sumaReembolsos * 100.0) / 100.0
        }

    val instanciaReembolsoSeleccionada: InstanciaPago?
        get() = metodosPagoDisponibles.firstOrNull { it.id == instanciaReembolsoId }
            ?: metodosPagoDisponibles.firstOrNull { it.tipoId == "EFECTIVO" }
            ?: metodosPagoDisponibles.firstOrNull()

    val puedeRegistrarDevolucion: Boolean
        get() = cajaAbierta &&
            ventaSeleccionada != null &&
            totalItemsADevolver > 0 &&
            motivo.trim().length >= 5 &&
            instanciaReembolsoSeleccionada != null &&
            !procesandoDevolucion
}

/**
 * ViewModel del módulo de Devoluciones POS.
 */
class DevolucionesViewModel(
    private val ventasRepository: VentasRepository = VentasRepository(),
    private val cajaRepository: CajaRepository = CajaRepository(),
    private val metodosPagoRepository: MetodosPagoRepository = MetodosPagoRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "DevolucionesViewModel"
    }

    private val _uiState = MutableStateFlow(DevolucionesUiState())
    val uiState: StateFlow<DevolucionesUiState> = _uiState.asStateFlow()

    private var jobBusqueda: Job? = null
    private val observadoresJobs = mutableListOf<Job>()
    private var sucursalObserverJob: Job? = null

    init {
        observarCambiosDeSucursal()
    }

    private fun observarCambiosDeSucursal() {
        sucursalObserverJob?.cancel()
        sucursalObserverJob = viewModelScope.launch {
            var ultimaSucursal: String? = null
            SessionManager.sucursalFlow.collect { sucursal ->
                if (sucursal != ultimaSucursal) {
                    ultimaSucursal = sucursal
                    reiniciarObservadores()
                }
            }
        }
    }

    private fun reiniciarObservadores() {
        observadoresJobs.forEach { it.cancel() }
        observadoresJobs.clear()
        _uiState.update {
            it.copy(
                ventaSeleccionada = null,
                itemsSeleccionados = emptyMap(),
                motivo = "",
                resultadosBusqueda = emptyList(),
                busquedaTexto = "",
                error = null,
                mensajeExito = null
            )
        }
        iniciarObservadores()
    }

    private fun iniciarObservadores() {
        val sucursalId = SessionManager.sucursalIdEfectiva

        // 1. Escuchar Estado de Caja (El reembolso sale del turno abierto)
        observadoresJobs += viewModelScope.launch {
            cajaRepository.observarEstadoCaja()
                .catch { Log.e(TAG, "Error escuchando estado de caja: ${it.message}", it) }
                .collect { estado ->
                    _uiState.update { it.copy(estadoCaja = estado) }
                }
        }

        // 2. Escuchar Métodos de Pago Activos de la Sucursal
        if (sucursalId.isNotBlank()) {
            observadoresJobs += viewModelScope.launch {
                metodosPagoRepository.observarMetodosPago(sucursalId)
                    .catch { Log.e(TAG, "Error escuchando métodos de pago: ${it.message}", it) }
                    .collect { metodos ->
                        val activas = metodos.filter { it.activa }
                        _uiState.update { estadoPrevio ->
                            val sigueActiva = activas.any { it.id == estadoPrevio.instanciaReembolsoId }
                            val defaultId = if (sigueActiva) {
                                estadoPrevio.instanciaReembolsoId
                            } else {
                                activas.firstOrNull { it.tipoId == "EFECTIVO" }?.id ?: activas.firstOrNull()?.id ?: ""
                            }
                            estadoPrevio.copy(
                                metodosPagoDisponibles = activas,
                                instanciaReembolsoId = defaultId
                            )
                        }
                    }
            }
        }

        // 3. Escuchar Ventas del Día como lista viva de respaldo
        observadoresJobs += viewModelScope.launch {
            ventasRepository.observarVentasDelDia()
                .catch { Log.e(TAG, "Error escuchando ventas del día: ${it.message}", it) }
                .collect { ventas ->
                    val devolvibles = ventas.filter { it.estado != Venta.ESTADO_DEVOLUCION_TOTAL && it.estado != Venta.ESTADO_ANULADA }
                    _uiState.update { it.copy(ventasDelDia = devolvibles) }
                }
        }
    }

    /**
     * Precarga una venta proveniente de Ventas del Día.
     */
    fun precargarVenta(venta: Venta) {
        seleccionarVenta(venta)
    }

    // ───────────────────────────── BÚSQUEDA DE VENTAS ─────────────────────────────

    fun onBusquedaChange(texto: String) {
        _uiState.update { it.copy(busquedaTexto = texto) }
        jobBusqueda?.cancel()

        if (texto.trim().isBlank()) {
            _uiState.update { it.copy(buscando = false, resultadosBusqueda = emptyList()) }
            return
        }

        jobBusqueda = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(buscando = true) }
            val res = ventasRepository.buscarVentaPorNumero(texto)
            res.onSuccess { lista ->
                val devolvibles = lista.filter { it.estado != Venta.ESTADO_DEVOLUCION_TOTAL && it.estado != Venta.ESTADO_ANULADA }
                _uiState.update { it.copy(buscando = false, resultadosBusqueda = devolvibles) }
            }.onFailure { err ->
                _uiState.update { it.copy(buscando = false, error = "Error buscando venta: ${err.message}") }
            }
        }
    }

    fun seleccionarVenta(venta: Venta) {
        if (venta.estado == Venta.ESTADO_ANULADA) {
            _uiState.update { it.copy(error = "La venta '${venta.numeroCompleto}' fue anulada. No se pueden procesar devoluciones.") }
            return
        }
        val idIdem = "dev_${UUID.randomUUID()}"
        _uiState.update {
            it.copy(
                ventaSeleccionada = venta,
                itemsSeleccionados = emptyMap(),
                motivo = "",
                idempotenciaIdActual = idIdem,
                error = null,
                resultadosBusqueda = emptyList(),
                busquedaTexto = ""
            )
        }
    }

    fun limpiarSeleccionVenta() {
        _uiState.update {
            it.copy(
                ventaSeleccionada = null,
                itemsSeleccionados = emptyMap(),
                motivo = "",
                idempotenciaIdActual = "",
                error = null
            )
        }
    }

    // ───────────────────────────── SELECCIÓN DE ÍTEMS Y CANTIDADES ─────────────────────────────

    fun toggleItem(item: ItemVenta, seleccionado: Boolean) {
        val clave = "${item.productoId}_${item.presentacionId}"
        val mapa = _uiState.value.itemsSeleccionados.toMutableMap()
        if (seleccionado) {
            val cantDefault = if (item.cantidadDevolvible > 0) 1 else 0
            mapa[clave] = cantDefault
        } else {
            mapa.remove(clave)
        }
        _uiState.update { it.copy(itemsSeleccionados = mapa) }
    }

    fun setCantidadItem(item: ItemVenta, cantidad: Int) {
        val clave = "${item.productoId}_${item.presentacionId}"
        val mapa = _uiState.value.itemsSeleccionados.toMutableMap()
        val cantAjustada = cantidad.coerceIn(0, item.cantidadDevolvible)
        if (cantAjustada <= 0) {
            mapa.remove(clave)
        } else {
            mapa[clave] = cantAjustada
        }
        _uiState.update { it.copy(itemsSeleccionados = mapa) }
    }

    fun setMotivo(motivo: String) {
        _uiState.update { it.copy(motivo = motivo) }
    }

    fun setMetodoReembolso(instanciaId: String) {
        _uiState.update { it.copy(instanciaReembolsoId = instanciaId) }
    }

    // ───────────────────────────── REGISTRO DE DEVOLUCIÓN (ATÓMICO) ─────────────────────────────

    fun confirmarDevolucion() {
        val estado = _uiState.value
        if (estado.procesandoDevolucion) return

        val venta = estado.ventaSeleccionada
        if (venta == null) {
            _uiState.update { it.copy(error = "No hay ninguna venta seleccionada.") }
            return
        }
        if (!estado.cajaAbierta) {
            _uiState.update { it.copy(error = "La caja está cerrada. No se pueden realizar reembolsos de dinero.") }
            return
        }
        if (estado.totalItemsADevolver <= 0) {
            _uiState.update { it.copy(error = "Selecciona al menos un producto y cantidad a devolver.") }
            return
        }
        if (estado.motivo.trim().length < 5) {
            _uiState.update { it.copy(error = "El motivo de la devolución debe tener al menos 5 caracteres.") }
            return
        }

        val metodoInstancia = estado.instanciaReembolsoSeleccionada
        val tipoReembolso = metodoInstancia?.tipoId ?: "EFECTIVO"

        val params = venta.items.mapNotNull { item ->
            val cant = estado.itemsSeleccionados["${item.productoId}_${item.presentacionId}"] ?: 0
            if (cant > 0) {
                ItemDevolucionParam(
                    productoId = item.productoId,
                    presentacionId = item.presentacionId,
                    cantidad = cant
                )
            } else null
        }

        if (params.isEmpty()) {
            _uiState.update { it.copy(error = "No hay productos seleccionados para devolver.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(procesandoDevolucion = true, error = null) }

            val res = ventasRepository.registrarDevolucion(
                ventaId = venta.id,
                itemsADevolver = params,
                motivo = estado.motivo.trim(),
                metodoReembolso = tipoReembolso,
                idempotenciaId = estado.idempotenciaIdActual
            )

            res.onSuccess { dev ->
                _uiState.update {
                    it.copy(
                        procesandoDevolucion = false,
                        devolucionExitosa = dev,
                        ventaSeleccionada = null,
                        itemsSeleccionados = emptyMap(),
                        motivo = "",
                        idempotenciaIdActual = "",
                        mensajeExito = "Nota de crédito ${dev.numeroCompleto.ifBlank { dev.id }} registrada. Reembolso de S/ ${String.format(java.util.Locale.US, "%.2f", dev.montoReembolso)} por ${dev.metodoReembolso}."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        procesandoDevolucion = false,
                        error = err.message ?: "No se pudo registrar la devolución."
                    )
                }
            }
        }
    }

    fun cerrarDialogoExito() {
        _uiState.update { it.copy(devolucionExitosa = null, error = null, mensajeExito = null) }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(error = null, mensajeExito = null) }
    }
}
