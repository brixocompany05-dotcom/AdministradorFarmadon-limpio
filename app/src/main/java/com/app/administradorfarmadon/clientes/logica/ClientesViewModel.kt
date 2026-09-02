package com.app.administradorfarmadon.clientes.logica

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.clientes.datos.ClientesRepository
import com.app.administradorfarmadon.clientes.modelo.ClienteFarmacia
import com.app.administradorfarmadon.compartido.datos.ApiDocumentosPeru
import com.app.administradorfarmadon.compartido.datos.ResultadoConsultaDoc
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
    val cargando: Boolean = true,
    val filtroTexto: String = "",
    val clienteSeleccionado: ClienteFarmacia? = null,
    val historialVentas: List<Venta> = emptyList(),
    val cargandoHistorial: Boolean = false,
    val mostrarDialogoCrearEditar: Boolean = false,
    val clienteEnEdicion: ClienteFarmacia? = null,
    val consultandoDoc: Boolean = false,
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
}

/**
 * ViewModel del módulo de Directorio de Clientes.
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
        iniciarObservadorClientes()
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
                        estado.copy(
                            clientes = lista,
                            cargando = false,
                            clienteSeleccionado = selActualizada
                        )
                    }
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

    fun abrirDialogoCrear() {
        _uiState.update { it.copy(mostrarDialogoCrearEditar = true, clienteEnEdicion = null, error = null) }
    }

    fun abrirDialogoEditar(cliente: ClienteFarmacia) {
        _uiState.update { it.copy(mostrarDialogoCrearEditar = true, clienteEnEdicion = cliente, error = null) }
    }

    fun cerrarDialogoCrearEditar() {
        _uiState.update { it.copy(mostrarDialogoCrearEditar = false, clienteEnEdicion = null) }
    }

    fun consultarDocumentoOficial(tipo: String, numero: String, onResultado: (nombre: String, direccion: String) -> Unit) {
        if (numero.trim().isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(consultandoDoc = true, error = null) }
            when (val res = ApiDocumentosPeru.consultar(tipo, numero)) {
                is ResultadoConsultaDoc.Encontrado -> {
                    _uiState.update { it.copy(consultandoDoc = false) }
                    onResultado(res.nombreCompleto, res.direccion)
                }
                is ResultadoConsultaDoc.NoEncontrado -> {
                    _uiState.update {
                        it.copy(
                            consultandoDoc = false,
                            error = "No se encontraron datos en los registros oficiales para el $tipo $numero."
                        )
                    }
                }
                is ResultadoConsultaDoc.SinToken -> {
                    _uiState.update {
                        it.copy(
                            consultandoDoc = false,
                            error = res.mensaje
                        )
                    }
                }
                is ResultadoConsultaDoc.Error -> {
                    _uiState.update {
                        it.copy(
                            consultandoDoc = false,
                            error = res.mensaje
                        )
                    }
                }
            }
        }
    }

    fun guardarCliente(cliente: ClienteFarmacia) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val res = clientesRepository.guardarCliente(cliente)
            res.onSuccess { guardado ->
                _uiState.update {
                    it.copy(
                        mostrarDialogoCrearEditar = false,
                        clienteEnEdicion = null,
                        clienteSeleccionado = guardado,
                        mensajeExito = "Cliente '${guardado.nombre}' guardado exitosamente."
                    )
                }
                seleccionarCliente(guardado)
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message ?: "No se pudo guardar el cliente.") }
            }
        }
    }

    fun eliminarCliente(clienteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val res = clientesRepository.eliminarCliente(clienteId)
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        clienteSeleccionado = null,
                        historialVentas = emptyList(),
                        mensajeExito = "Cliente eliminado del directorio."
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message ?: "No se pudo eliminar el cliente.") }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(error = null, mensajeExito = null) }
    }
}
