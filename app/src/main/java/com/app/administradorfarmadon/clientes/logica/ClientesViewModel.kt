package com.app.administradorfarmadon.clientes.logica

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.clientes.datos.ClientesRepository
import com.app.administradorfarmadon.clientes.modelo.ClienteFarmacia
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI del Directorio de Clientes (R8 Verdad Vigente).
 */
data class ClientesUiState(
    val clientes: List<ClienteFarmacia> = emptyList(),
    val totalClientesServidor: Int = 0,
    val cargando: Boolean = true,
    val filtroTexto: String = "",
    val clienteSeleccionado: ClienteFarmacia? = null,
    val historialVentas: List<Venta> = emptyList(),
    val cargandoHistorial: Boolean = false,
    val error: String? = null,
    val mensajeExito: String? = null
) {
    val clientesFiltrados: List<ClienteFarmacia>
        get() {
            if (filtroTexto.isBlank()) return clientes
            val q = filtroTexto.trim().uppercase()
            return clientes.filter {
                it.nombre.uppercase().contains(q) ||
                    it.numeroDocumento.contains(q) ||
                    it.telefono.contains(q) ||
                    it.direccion.uppercase().contains(q)
            }
        }

    val totalComprasCliente: Double
        get() = kotlin.math.round(
            historialVentas.filter { it.estado != Venta.ESTADO_DEVOLUCION_TOTAL && it.estado != Venta.ESTADO_ANULADA }
                .sumOf { v ->
                    if (v.estado == Venta.ESTADO_DEVOLUCION_PARCIAL) (v.total - v.totalDevuelto).coerceAtLeast(0.0)
                    else v.total
                } * 100.0
        ) / 100.0

    val totalOperacionesCliente: Int
        get() = historialVentas.count { it.estado != Venta.ESTADO_ANULADA }

    val ticketPromedioCliente: Double
        get() = if (totalOperacionesCliente > 0 && totalComprasCliente > 0.0) {
            kotlin.math.round((totalComprasCliente / totalOperacionesCliente) * 100.0) / 100.0
        } else 0.0
}

/**
 * ViewModel del módulo de Directorio de Clientes (Solo Lectura / Consulta Informativa R1/R3/R8).
 * La única puerta de registro de clientes es el módulo de Ventas (POS).
 */
class ClientesViewModel(
    private val clientesRepository: ClientesRepository = ClientesRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "ClientesViewModel"
    }

    private val _uiState = MutableStateFlow(ClientesUiState())
    val uiState: StateFlow<ClientesUiState> = _uiState.asStateFlow()

    private var jobHistorial: Job? = null

    init {
        recargarTotalClientes()
        iniciarObservadorClientes()
    }

    fun recargarTotalClientes() {
        viewModelScope.launch {
            val total = clientesRepository.obtenerTotalClientes()
            if (total > 0) {
                _uiState.update { it.copy(totalClientesServidor = total) }
            }
        }
    }

    private fun iniciarObservadorClientes() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            clientesRepository.observarClientes()
                .catch { err ->
                    Log.e(TAG, "Error observando directorio de clientes: ${err.message}", err)
                    _uiState.update { it.copy(cargando = false, error = err.message ?: "No se pudieron cargar los clientes.") }
                }
                .collect { lista ->
                    _uiState.update { estado ->
                        val selActualizada = estado.clienteSeleccionado?.let { sel ->
                            lista.firstOrNull { it.id == sel.id }
                        }
                        val totalSrv = if (estado.totalClientesServidor > 0) estado.totalClientesServidor else lista.size
                        estado.copy(
                            clientes = lista,
                            totalClientesServidor = maxOf(totalSrv, lista.size),
                            cargando = false,
                            clienteSeleccionado = selActualizada
                        )
                    }
                    recargarTotalClientes()
                }
        }
    }

    fun setFiltroTexto(texto: String) {
        _uiState.update { it.copy(filtroTexto = texto) }
    }

    fun seleccionarCliente(cliente: ClienteFarmacia?) {
        _uiState.update { it.copy(clienteSeleccionado = cliente, historialVentas = emptyList()) }
        jobHistorial?.cancel()

        if (cliente == null || cliente.id.isBlank()) return

        jobHistorial = viewModelScope.launch {
            _uiState.update { it.copy(cargandoHistorial = true) }
            clientesRepository.observarHistorialVentas(cliente.id)
                .catch { err ->
                    Log.e(TAG, "Error escuchando historial de ventas: ${err.message}", err)
                    _uiState.update { it.copy(cargandoHistorial = false) }
                }
                .collect { ventas ->
                    _uiState.update { it.copy(historialVentas = ventas, cargandoHistorial = false) }
                }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(error = null, mensajeExito = null) }
    }
}
