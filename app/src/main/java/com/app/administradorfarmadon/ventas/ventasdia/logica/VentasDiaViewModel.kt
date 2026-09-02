package com.app.administradorfarmadon.ventas.ventasdia.logica

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.ventas.compartido.datos.TicketComprobantePdf
import com.app.administradorfarmadon.ventas.compartido.datos.VentasRepository
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado UI de Ventas del Día (R8 Verdad Vigente).
 * Todas las métricas y resúmenes se computan en vivo sobre la lista real.
 */
data class VentasDiaUiState(
    val ventas: List<Venta> = emptyList(),
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
    // 1. Total Ventas Neto: COMPLETADA -> total, DEVOLUCION_PARCIAL -> (total - totalDevuelto), DEVOLUCION_TOTAL/ANULADA -> 0.0
    val totalVentasNeto: Double
        get() = kotlin.math.round(
            ventas.filter { it.estado != Venta.ESTADO_DEVOLUCION_TOTAL && it.estado != Venta.ESTADO_ANULADA }
                .sumOf { v ->
                    if (v.estado == Venta.ESTADO_DEVOLUCION_PARCIAL) (v.total - v.totalDevuelto).coerceAtLeast(0.0)
                    else v.total
                } * 100.0
        ) / 100.0

    // 2. N° Operaciones registradas válidas (no anuladas)
    val totalOperaciones: Int
        get() = ventas.count { it.estado != Venta.ESTADO_ANULADA }

    val totalOperacionesValidas: Int
        get() = ventas.count { it.estado != Venta.ESTADO_DEVOLUCION_TOTAL && it.estado != Venta.ESTADO_ANULADA }

    // 3. Ticket Promedio (divide entre operaciones válidas que aportan al neto)
    val ticketPromedio: Double
        get() = if (totalOperacionesValidas > 0) kotlin.math.round((totalVentasNeto / totalOperacionesValidas) * 100.0) / 100.0 else 0.0

    // 4. Descuentos sumados
    val totalDescuentos: Double
        get() = kotlin.math.round(ventas.filter { it.estado != Venta.ESTADO_ANULADA }.sumOf { it.descuento } * 100.0) / 100.0

    // 5. Devoluciones sumadas
    val totalDevoluciones: Double
        get() = kotlin.math.round(ventas.filter { it.estado != Venta.ESTADO_ANULADA }.sumOf { it.totalDevuelto } * 100.0) / 100.0

    // 6. Total con devolución
    val totalConDevolucion: Int
        get() = ventas.count { it.estado == Venta.ESTADO_DEVOLUCION_PARCIAL || it.estado == Venta.ESTADO_DEVOLUCION_TOTAL }

    // 7. Total anuladas
    val totalAnuladas: Int
        get() = ventas.count { it.estado == Venta.ESTADO_ANULADA }

    // 8. Desglose por métodos de pago reales (calculado a partir de pagos de ventas válidas)
    val ventasPorMetodo: Map<String, Double>
        get() {
            val mapa = mutableMapOf<String, Double>()
            ventas.filter { it.estado != Venta.ESTADO_ANULADA }.forEach { v ->
                v.pagos.forEach { p ->
                    val actual = mapa[p.tipoId] ?: 0.0
                    mapa[p.tipoId] = kotlin.math.round((actual + p.monto) * 100.0) / 100.0
                }
            }
            return mapa
        }

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
    private val ventasRepository: VentasRepository = VentasRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "VentasDiaViewModel"
    }

    private val _uiState = MutableStateFlow(VentasDiaUiState())
    val uiState: StateFlow<VentasDiaUiState> = _uiState.asStateFlow()

    private var jobObservador: Job? = null

    init {
        reconectar()
    }

    /**
     * Reconecta el listener en vivo de ventas del día.
     * Al entrar a la pestaña o cruzar medianoche, re-calcula la fecha actual (R8/N1).
     */
    fun reconectar() {
        jobObservador?.cancel()
        _uiState.update { it.copy(cargando = true, error = null) }

        jobObservador = viewModelScope.launch {
            ventasRepository.observarVentasDelDia()
                .catch { err ->
                    Log.e(TAG, "Error observando ventas del día: ${err.message}", err)
                    _uiState.update { it.copy(cargando = false, error = err.message ?: "No se pudieron cargar las ventas del día.") }
                }
                .collect { listaVentas ->
                    _uiState.update { estadoPrevio ->
                        // Mantener la venta seleccionada actualizada si sufrió algún cambio
                        val selActualizada = estadoPrevio.ventaSeleccionada?.let { sel ->
                            listaVentas.firstOrNull { it.id == sel.id }
                        }
                        estadoPrevio.copy(
                            ventas = listaVentas,
                            cargando = false,
                            ventaSeleccionada = selActualizada
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
