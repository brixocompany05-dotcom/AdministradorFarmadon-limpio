package com.app.administradorfarmadon.ventas.ventasdia.logica

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.ventas.compartido.datos.CajaRepository
import com.app.administradorfarmadon.ventas.compartido.datos.TicketComprobantePdf
import com.app.administradorfarmadon.ventas.compartido.datos.VentasRepository
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.DevolucionVenta
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.ventas.compartido.logica.ReglaBloqueoTurnoCaja
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado UI de Ventas del Día (R8 Verdad Vigente).
 * Cuadre contable del día: neto = brutas de hoy − reembolsos de hoy.
 * Los reembolsos de hoy incluyen devoluciones de ventas de ayer cobradas hoy en caja,
 * por eso se observan las devoluciones del día y no solo lo devuelto dentro de cada venta.
 * Así coincide con Caja (puntero neto) y con Analítica HOY.
 */
data class VentasDiaUiState(
    val ventas: List<Venta> = emptyList(),
    val devolucionesDia: List<DevolucionVenta> = emptyList(),
    val estadoCaja: EstadoCaja = EstadoCaja(),
    val cargando: Boolean = true,
    val error: String? = null,
    val mensajeExito: String? = null,
    val filtroTexto: String = "",
    val filtroEstado: String = "TODOS", // "TODOS", "COMPLETADA", "DEVOLUCION_PARCIAL", "DEVOLUCION_TOTAL", "ANULADA"
    val filtroMetodo: String = "TODOS",
    val ventaSeleccionada: Venta? = null,
    val mostrarDialogoAnular: Boolean = false,
    val procesandoAnulacion: Boolean = false,
    val ventaAAnular: Venta? = null
) {
    /**
     * Determina si una venta pertenece a un turno de caja ya cerrado o distinto al activo.
     * En esos casos, no se puede anular directamente contra el saldo del turno actual;
     * debe procesarse vía Devolución (Nota de Crédito).
     */
    fun ventaPerteneceATurnoCerrado(v: Venta): Boolean {
        if (estadoCaja.esTurnoVencido) return true
        if (estadoCaja.estado != CajaSesion.ESTADO_ABIERTA || estadoCaja.sesionId.isBlank()) return true
        return v.cajaSesionId.isNotBlank() && v.cajaSesionId != estadoCaja.sesionId
    }
    // Brutas del día: suma de totales no anulados (con descuento ya aplicado).
    val totalVentasBrutas: Double
        get() = kotlin.math.round(
            ventas.filter { it.estado != Venta.ESTADO_ANULADA }.sumOf { it.total } * 100.0
        ) / 100.0

    // Reembolsos efectuados HOY (notas de crédito del día, aunque sean de ventas de ayer).
    val totalDevolucionesDia: Double
        get() = kotlin.math.round(devolucionesDia.sumOf { it.montoReembolso } * 100.0) / 100.0

    // Neto del día que cuadra con caja y con Analítica HOY (sin piso artificial para auditar días con más reembolsos que ventas).
    val totalVentasNeto: Double
        get() = kotlin.math.round((totalVentasBrutas - totalDevolucionesDia) * 100.0) / 100.0

    // 2. N° Operaciones registradas válidas (no anuladas)
    val totalOperaciones: Int
        get() = ventas.count { it.estado != Venta.ESTADO_ANULADA }

    val totalOperacionesValidas: Int
        get() = ventas.count { it.estado != Venta.ESTADO_DEVOLUCION_TOTAL && it.estado != Venta.ESTADO_ANULADA }

    // 3. Ticket Promedio neto del día (si el neto es negativo o cero por reembolsos cruzados, muestra 0 para no confundir).
    val ticketPromedio: Double
        get() = if (totalOperacionesValidas > 0 && totalVentasNeto > 0.0) kotlin.math.round((totalVentasNeto / totalOperacionesValidas) * 100.0) / 100.0 else 0.0

    // 4. Descuentos sumados
    val totalDescuentos: Double
        get() = kotlin.math.round(ventas.filter { it.estado != Venta.ESTADO_ANULADA }.sumOf { it.descuento } * 100.0) / 100.0

    // 5. Devoluciones del día (reembolsos hoy). Coincide con Caja y Analítica.
    val totalDevoluciones: Double
        get() = totalDevolucionesDia

    // 6. Reembolsos emitidos hoy (notas de crédito del día).
    val totalConDevolucion: Int
        get() = devolucionesDia.size

    // Ventas de hoy que tienen alguna devolución acumulada (informativo por comprobante).
    val ventasConDevolucionAcumulada: Int
        get() = ventas.count { it.estado == Venta.ESTADO_DEVOLUCION_PARCIAL || it.estado == Venta.ESTADO_DEVOLUCION_TOTAL }

    // 7. Total anuladas
    val totalAnuladas: Int
        get() = ventas.count { it.estado == Venta.ESTADO_ANULADA }

    // 8. Recaudado NETO por método: cobros netos (vuelto descontado del efectivo) − reembolsos de hoy. Coincide con Caja y Analítica.
    val ventasPorMetodo: Map<String, Double>
        get() {
            val mapa = mutableMapOf<String, Double>()
            fun red2(x: Double) = kotlin.math.round(x * 100.0) / 100.0
            ventas.filter { it.estado != Venta.ESTADO_ANULADA }.forEach { v ->
                var vueltoRestante = red2(v.vuelto.coerceAtLeast(0.0))
                v.pagos.forEach { p ->
                    val clave = p.tipoId.trim().ifBlank { "SIN_ESPECIFICAR" }
                    val netoLinea = if (p.tipoId == "EFECTIVO" && vueltoRestante > 0.0) {
                        val asignado = minOf(vueltoRestante, p.monto)
                        vueltoRestante = red2(vueltoRestante - asignado)
                        red2(p.monto - asignado)
                    } else red2(p.monto)
                    mapa[clave] = red2((mapa[clave] ?: 0.0) + netoLinea)
                }
            }
            devolucionesDia.forEach { d ->
                val clave = d.metodoReembolso.trim().ifBlank { "SIN_ESPECIFICAR" }
                val actual = mapa[clave] ?: 0.0
                mapa[clave] = kotlin.math.round((actual - d.montoReembolso) * 100.0) / 100.0
            }
            return mapa
        }

    /** Métodos con movimiento hoy para el filtro (brutos + reembolsos). */
    val metodosDisponibles: List<String>
        get() = ventasPorMetodo.keys.sorted()

    // 9. Lista de ventas filtradas en memoria (Cero queries complejas)
    val ventasFiltradas: List<Venta>
        get() {
            return ventas.filter { v ->
                val cumpleTexto = if (filtroTexto.isBlank()) true else {
                    val q = filtroTexto.trim().uppercase()
                    v.numeroCompleto.uppercase().contains(q) ||
                        v.cliente.nombre.uppercase().contains(q) ||
                        v.cliente.numeroDocumento.contains(q) ||
                        v.cajeroNombre.uppercase().contains(q)
                }
                val cumpleEstado = when (filtroEstado) {
                    "TODOS" -> true
                    "COMPLETADA" -> v.estado == Venta.ESTADO_COMPLETADA
                    "DEVOLUCION_PARCIAL" -> v.estado == Venta.ESTADO_DEVOLUCION_PARCIAL
                    "DEVOLUCION_TOTAL" -> v.estado == Venta.ESTADO_DEVOLUCION_TOTAL
                    "DEVOLUCIONES" -> v.estado == Venta.ESTADO_DEVOLUCION_PARCIAL || v.estado == Venta.ESTADO_DEVOLUCION_TOTAL
                    "ANULADA" -> v.estado == Venta.ESTADO_ANULADA
                    else -> true
                }
                val cumpleMetodo = if (filtroMetodo == "TODOS") true else {
                    v.pagos.any { it.tipoId.equals(filtroMetodo, ignoreCase = true) }
                }
                cumpleTexto && cumpleEstado && cumpleMetodo
            }
        }
}

/**
 * ViewModel para Ventas del Día.
 * Mantiene la lista viva conectada a Firestore y gestiona filtros, selección, anulación e impresión.
 */
class VentasDiaViewModel(
    private val ventasRepository: VentasRepository = VentasRepository(),
    private val cajaRepository: CajaRepository = CajaRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "VentasDiaViewModel"
    }

    private val _uiState = MutableStateFlow(VentasDiaUiState())
    val uiState: StateFlow<VentasDiaUiState> = _uiState.asStateFlow()

    private var jobObservador: Job? = null
    private var sucursalObserverJob: Job? = null

    init {
        observarCambiosDeSucursal()
    }

    private fun observarCambiosDeSucursal() {
        sucursalObserverJob?.cancel()
        sucursalObserverJob = viewModelScope.launch {
            var ultimaSucursal: String? = null
            var ultimoCajero: String? = null
            combine(
                com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalFlow,
                com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.cajeroFlow
            ) { suc, caj -> suc to caj }
                .collect { (sucursal, cajero) ->
                    if (sucursal != ultimaSucursal || cajero != ultimoCajero) {
                        ultimaSucursal = sucursal
                        ultimoCajero = cajero
                        _uiState.update { it.copy(ventas = emptyList(), devolucionesDia = emptyList(), ventaSeleccionada = null, cargando = true) }
                        reconectar()
                    }
                }
        }
    }

    /**
     * Reconecta los listeners vivos del día: ventas + devoluciones + estado de caja (R8).
     * Al entrar a la pestaña o cruzar medianoche, re-calcula la fecha actual.
     * El neto del día = brutas de hoy − reembolsos de hoy (cuadra con caja y analítica).
     * Respeta el aislamiento por cajero (R1/R3).
     */
    fun reconectar() {
        jobObservador?.cancel()
        _uiState.update { it.copy(cargando = true, error = null) }

        val cajeroId = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.idCajera

        jobObservador = viewModelScope.launch {
            combine(
                ventasRepository.observarVentasDelDia(cajeroId),
                ventasRepository.observarDevolucionesDelDia(cajeroId),
                cajaRepository.observarEstadoCaja(cajeroId)
            ) { ventas, devs, estadoCaja -> Triple(ventas, devs, estadoCaja) }
                .catch { err ->
                    Log.e(TAG, "Error observando ventas del día: ${err.message}", err)
                    _uiState.update { it.copy(cargando = false, error = err.message ?: "No se pudieron cargar las ventas del día.") }
                }
                .collect { (listaVentas, listaDevs, estadoCaja) ->
                    _uiState.update { estadoPrevio ->
                        // Mantener la venta seleccionada actualizada si sufrió algún cambio
                        val selActualizada = estadoPrevio.ventaSeleccionada?.let { sel ->
                            listaVentas.firstOrNull { it.id == sel.id }
                        }
                        // Mantener la venta del diálogo de anulación actualizada si está abierto
                        val anularActualizada = estadoPrevio.ventaAAnular?.let { a ->
                            listaVentas.firstOrNull { it.id == a.id }
                        }
                        val yaAnuladaPorOtro = anularActualizada != null && anularActualizada.estado == Venta.ESTADO_ANULADA
                        val cerrarDialogoAnular = estadoPrevio.mostrarDialogoAnular && yaAnuladaPorOtro
                        val errorNuevo = if (cerrarDialogoAnular && anularActualizada != null) {
                            "La venta '${anularActualizada.numeroCompleto}' ya fue anulada por otro usuario."
                        } else estadoPrevio.error

                        // Si el filtro de método quedó sin movimiento hoy, volver a TODOS para no dejar lista vacía fantasma.
                        val filtroMetodoVigente = if (estadoPrevio.filtroMetodo != "TODOS") {
                            val metodosAhora = mutableSetOf<String>()
                            listaVentas.filter { it.estado != Venta.ESTADO_ANULADA }.forEach { v ->
                                v.pagos.forEach { metodosAhora.add(it.tipoId.trim().ifBlank { "SIN_ESPECIFICAR" }) }
                            }
                            listaDevs.forEach { metodosAhora.add(it.metodoReembolso.trim().ifBlank { "SIN_ESPECIFICAR" }) }
                            if (estadoPrevio.filtroMetodo in metodosAhora) estadoPrevio.filtroMetodo else "TODOS"
                        } else "TODOS"
                        estadoPrevio.copy(
                            ventas = listaVentas,
                            devolucionesDia = listaDevs,
                            estadoCaja = estadoCaja,
                            cargando = false,
                            ventaSeleccionada = selActualizada,
                            ventaAAnular = if (cerrarDialogoAnular) null else (anularActualizada ?: estadoPrevio.ventaAAnular),
                            mostrarDialogoAnular = if (cerrarDialogoAnular) false else estadoPrevio.mostrarDialogoAnular,
                            error = errorNuevo,
                            filtroMetodo = filtroMetodoVigente
                        )
                    }
                }
        }
    }

    fun seleccionarVenta(venta: Venta?) {
        _uiState.update { it.copy(ventaSeleccionada = venta) }
    }

    fun setFiltroTexto(texto: String) {
        _uiState.update { it.copy(filtroTexto = texto) }
    }

    fun setFiltroEstado(estado: String) {
        _uiState.update { it.copy(filtroEstado = estado) }
    }

    fun setFiltroMetodo(metodo: String) {
        _uiState.update { it.copy(filtroMetodo = metodo) }
    }

    fun abrirDialogoAnular(venta: Venta) {
        val cId = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.idCajera
        val validacionTurno = ReglaBloqueoTurnoCaja.validarPermiteAnulacion(uiState.value.estadoCaja, cId)
        if (validacionTurno.isFailure) {
            _uiState.update { it.copy(error = validacionTurno.exceptionOrNull()?.message) }
            return
        }
        _uiState.update { it.copy(mostrarDialogoAnular = true, ventaAAnular = venta, error = null) }
    }

    fun cerrarDialogoAnular() {
        _uiState.update { it.copy(mostrarDialogoAnular = false, ventaAAnular = null) }
    }

    fun anularVenta(ventaId: String, motivo: String) {
        if (uiState.value.procesandoAnulacion) return
        val motivoLimpio = motivo.trim()
        if (motivoLimpio.length < 5) {
            _uiState.update { it.copy(error = "Debe ingresar un motivo de anulación de al menos 5 caracteres.") }
            return
        }

        val estadoActual = uiState.value
        val cId = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.idCajera
        val validacionTurno = ReglaBloqueoTurnoCaja.validarPermiteAnulacion(estadoActual.estadoCaja, cId)
        if (validacionTurno.isFailure) {
            _uiState.update { it.copy(error = validacionTurno.exceptionOrNull()?.message) }
            return
        }
        val venta = estadoActual.ventas.firstOrNull { it.id == ventaId }
        if (venta != null && estadoActual.ventaPerteneceATurnoCerrado(venta)) {
            _uiState.update {
                it.copy(
                    error = "Esta venta no pertenece al turno de caja activo. " +
                        "Para ventas de turnos cerrados, utilice DEVOLUCIÓN (Nota de Crédito) para no descuadrar la caja."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(procesandoAnulacion = true, error = null) }
            val res = ventasRepository.anularVenta(ventaId, motivoLimpio)
            res.onSuccess { ventaAnulada ->
                _uiState.update {
                    it.copy(
                        procesandoAnulacion = false,
                        mostrarDialogoAnular = false,
                        ventaAAnular = null,
                        ventaSeleccionada = ventaAnulada,
                        mensajeExito = "Venta ${ventaAnulada.numeroCompleto} anulada correctamente. Stock restituido a sus lotes."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        procesandoAnulacion = false,
                        error = err.message ?: "No se pudo anular la venta."
                    )
                }
            }
        }
    }

    fun imprimirTicket(context: Context, venta: Venta) {
        viewModelScope.launch {
            val emisorRes = withContext(Dispatchers.IO) { ventasRepository.obtenerEmisor() }
            val emisor = emisorRes.getOrNull()
            if (emisor == null) {
                _uiState.update { it.copy(error = "No se pudieron obtener los datos de la farmacia para imprimir: ${emisorRes.exceptionOrNull()?.message}") }
                return@launch
            }

            val res = withContext(Dispatchers.IO) {
                TicketComprobantePdf.imprimirTicket(context, venta, emisor)
            }
            res.onSuccess {
                _uiState.update { it.copy(mensajeExito = "Comprobante ${venta.numeroCompleto} enviado a impresión.") }
            }.onFailure { err ->
                _uiState.update { it.copy(error = "No se pudo imprimir el comprobante: ${err.message}") }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(error = null, mensajeExito = null) }
    }
}
