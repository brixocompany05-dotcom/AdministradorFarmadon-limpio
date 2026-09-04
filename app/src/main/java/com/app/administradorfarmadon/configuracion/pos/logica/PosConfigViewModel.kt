package com.app.administradorfarmadon.configuracion.pos.logica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.configuracion.metodospago.datos.SucursalCatalogo
import com.app.administradorfarmadon.configuracion.pos.datos.PosConfigRepository
import com.app.administradorfarmadon.configuracion.pos.modelo.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class PosConfigUiState(
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val sucursales: List<SucursalCatalogo> = emptyList(),
    val sucursalSeleccionadaId: String = "",
    val sucursalSeleccionadaNombre: String = "",
    // Campos del formulario (editables) - Reglas fijas sin burocracia
    val maxPctStr: String = "10",
    val maxMontoStr: String = "50.00",
    val retiroMaxStr: String = "500.00",
    val vueltoMaxStr: String = "200.00",
    val entregaCiegaTurno: Boolean = true,
    val copiasTicketStr: String = "1",
    val pieTicket: String = "Gracias por su compra",
    val exigirConfirmacionReceta: Boolean = true,
    // Estado de permanencia en servidor
    val existeEnServidor: Boolean = false,
    val esBorrador: Boolean = true,
    val mostrarDialogoAplicarTodas: Boolean = false,
    val aplicandoATodas: Boolean = false,
    // Auditoría vigente
    val actualizadoPorNombre: String = "",
    val actualizadoPorRol: String = "",
    val actualizadoEnMs: Long = 0L,
    // Feedback
    val mensajeExito: String? = null,
    val error: String? = null
)

class PosConfigViewModel(
    private val repository: PosConfigRepository = PosConfigRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosConfigUiState())
    val uiState: StateFlow<PosConfigUiState> = _uiState.asStateFlow()

    private var configJob: Job? = null

    init {
        val actual = SessionManager.sucursalIdEfectiva.ifBlank { "principal" }
        _uiState.update {
            it.copy(
                sucursalSeleccionadaId = actual,
                sucursalSeleccionadaNombre = SessionManager.sucursalNombre.ifBlank { "Sede Principal" }
            )
        }
        escucharConfiguracion(actual)
        cargarSucursales()
    }

    private fun cargarSucursales() {
        viewModelScope.launch {
            repository.observarSucursales()
                .catch { e ->
                    _uiState.update { it.copy(error = "No se pudieron cargar las sucursales: ${e.message}") }
                }
                .collect { lista ->
                    val actual = SessionManager.sucursalIdEfectiva
                    val sucursalIdInicial = if (lista.any { it.id == actual }) actual else (lista.firstOrNull()?.id ?: "principal")
                    val sucursalNombreInicial = lista.firstOrNull { it.id == sucursalIdInicial }?.nombre
                        ?: SessionManager.sucursalNombre.ifBlank { "Sede Principal" }

                    val cambioId = _uiState.value.sucursalSeleccionadaId != sucursalIdInicial
                    _uiState.update {
                        it.copy(
                            sucursales = lista,
                            sucursalSeleccionadaId = sucursalIdInicial,
                            sucursalSeleccionadaNombre = sucursalNombreInicial
                        )
                    }
                    if (cambioId) {
                        escucharConfiguracion(sucursalIdInicial)
                    }
                }
        }
    }

    fun seleccionarSucursal(id: String) {
        if (_uiState.value.sucursalSeleccionadaId == id) return
        val nombre = _uiState.value.sucursales.firstOrNull { it.id == id }?.nombre ?: id
        _uiState.update {
            it.copy(
                sucursalSeleccionadaId = id,
                sucursalSeleccionadaNombre = nombre,
                cargando = true,
                mensajeExito = null,
                error = null
            )
        }
        escucharConfiguracion(id)
    }

    private fun escucharConfiguracion(sucursalId: String) {
        configJob?.cancel()
        configJob = viewModelScope.launch {
            repository.observar(sucursalId)
                .catch { e ->
                    _uiState.update { it.copy(cargando = false, error = "Error al leer reglas de sede: ${e.message}") }
                }
                .collect { config ->
                    _uiState.update { current ->
                        current.copy(
                            cargando = false,
                            maxPctStr = if (config.descuento.maxPct % 1.0 == 0.0) config.descuento.maxPct.toInt().toString() else config.descuento.maxPct.toString(),
                            maxMontoStr = String.format(Locale.US, "%.2f", config.descuento.maxMonto),
                            retiroMaxStr = String.format(Locale.US, "%.2f", config.caja.retiroMax),
                            vueltoMaxStr = String.format(Locale.US, "%.2f", config.caja.vueltoMax),
                            entregaCiegaTurno = config.caja.entregaCiegaTurno,
                            copiasTicketStr = config.ticket.copias.toString(),
                            pieTicket = config.ticket.pie,
                            exigirConfirmacionReceta = config.receta.exigirConfirmacion,
                            existeEnServidor = config.existeEnServidor,
                            esBorrador = config.esBorrador,
                            actualizadoPorNombre = config.actualizadoPorNombre,
                            actualizadoPorRol = config.actualizadoPorRol,
                            actualizadoEnMs = config.actualizadoEnMs
                        )
                    }
                }
        }
    }

    // Handlers de campos
    fun onMaxPctChange(v: String) = _uiState.update { it.copy(maxPctStr = v) }
    fun onMaxMontoChange(v: String) = _uiState.update { it.copy(maxMontoStr = v) }
    fun onRetiroMaxChange(v: String) = _uiState.update { it.copy(retiroMaxStr = v) }
    fun onVueltoMaxChange(v: String) = _uiState.update { it.copy(vueltoMaxStr = v) }
    fun onEntregaCiegaTurnoChange(v: Boolean) = _uiState.update { it.copy(entregaCiegaTurno = v) }
    fun onCopiasTicketChange(v: String) = _uiState.update { it.copy(copiasTicketStr = v) }
    fun onPieTicketChange(v: String) = _uiState.update { it.copy(pieTicket = v) }
    fun onExigirConfirmacionRecetaChange(v: Boolean) = _uiState.update { it.copy(exigirConfirmacionReceta = v) }

    fun guardarConfiguracion() {
        val s = _uiState.value
        if (s.guardando) return

        val maxPct = s.maxPctStr.toDoubleOrNull()
        val maxMonto = s.maxMontoStr.toDoubleOrNull()
        val retiroMax = s.retiroMaxStr.toDoubleOrNull()
        val vueltoMax = s.vueltoMaxStr.toDoubleOrNull()
        val copias = s.copiasTicketStr.toIntOrNull()

        if (maxPct == null || maxPct < 0.0 || maxPct > 100.0) {
            _uiState.update { it.copy(error = "El porcentaje máximo de descuento debe estar entre 0% y 100%.") }
            return
        }
        if (maxMonto == null || maxMonto < 0.0) {
            _uiState.update { it.copy(error = "El monto máximo de descuento en soles debe ser mayor o igual a 0.") }
            return
        }
        if (retiroMax == null || retiroMax < 0.0) {
            _uiState.update { it.copy(error = "El retiro máximo permitido en soles debe ser mayor o igual a 0.") }
            return
        }
        if (vueltoMax == null || vueltoMax < 0.0) {
            _uiState.update { it.copy(error = "El vuelto máximo permitido en soles debe ser mayor o igual a 0.") }
            return
        }
        if (copias == null || copias < 1 || copias > 5) {
            _uiState.update { it.copy(error = "El número de copias del ticket debe ser entre 1 y 5.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null, mensajeExito = null) }

            val configAGuardar = PosConfig(
                farmaciaId = SessionManager.clienteIdGarantizado,
                sucursalId = s.sucursalSeleccionadaId,
                descuento = PosDescuentoConfig(
                    maxPct = maxPct,
                    maxMonto = maxMonto
                ),
                caja = PosCajaConfig(
                    retiroMax = retiroMax,
                    vueltoMax = vueltoMax,
                    entregaCiegaTurno = s.entregaCiegaTurno
                ),
                ticket = PosTicketConfig(
                    copias = copias,
                    pie = s.pieTicket.trim().ifBlank { "Gracias por su compra" }
                ),
                receta = PosRecetaConfig(
                    exigirConfirmacion = s.exigirConfirmacionReceta
                )
            )

            val uId = SessionManager.idCajera
            val uNombre = SessionManager.nombreUsuario.ifBlank { "Administrador" }
            val uRol = SessionManager.rol.ifBlank { "Administrador" }

            val resultado = repository.guardar(
                sucursalId = s.sucursalSeleccionadaId,
                config = configAGuardar,
                usuarioId = uId,
                usuarioNombre = uNombre,
                rolUsuario = uRol
            )

            resultado.onSuccess {
                _uiState.update {
                    it.copy(
                        guardando = false,
                        mensajeExito = "Reglas de POS guardadas correctamente para ${s.sucursalSeleccionadaNombre}."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        guardando = false,
                        error = "No se pudieron guardar las reglas: ${err.message}"
                    )
                }
            }
        }
    }

    fun abrirDialogoAplicarTodas() {
        _uiState.update { it.copy(mostrarDialogoAplicarTodas = true) }
    }

    fun cerrarDialogoAplicarTodas() {
        _uiState.update { it.copy(mostrarDialogoAplicarTodas = false) }
    }

    fun aplicarATodasLasSedes() {
        val s = _uiState.value
        val maxPct = s.maxPctStr.toDoubleOrNull() ?: 10.0
        val maxMonto = s.maxMontoStr.toDoubleOrNull() ?: 50.0
        val retiroMax = s.retiroMaxStr.toDoubleOrNull() ?: 500.0
        val vueltoMax = s.vueltoMaxStr.toDoubleOrNull() ?: 200.0
        val copias = s.copiasTicketStr.toIntOrNull() ?: 1

        val configAGuardar = PosConfig(
            farmaciaId = SessionManager.clienteIdGarantizado,
            sucursalId = "",
            descuento = PosDescuentoConfig(maxPct = maxPct, maxMonto = maxMonto),
            caja = PosCajaConfig(retiroMax = retiroMax, vueltoMax = vueltoMax, entregaCiegaTurno = s.entregaCiegaTurno),
            ticket = PosTicketConfig(copias = copias, pie = s.pieTicket.trim().ifBlank { "Gracias por su compra" }),
            receta = PosRecetaConfig(exigirConfirmacion = s.exigirConfirmacionReceta)
        )

        val uId = SessionManager.idCajera
        val uNombre = SessionManager.nombreUsuario.ifBlank { "Administrador" }
        val uRol = SessionManager.rol.ifBlank { "Administrador" }

        viewModelScope.launch {
            _uiState.update { it.copy(aplicandoATodas = true, mostrarDialogoAplicarTodas = false, error = null) }
            val res = repository.aplicarATodasLasSedes(configAGuardar, uId, uNombre, uRol)
            res.onSuccess { total ->
                _uiState.update {
                    it.copy(
                        aplicandoATodas = false,
                        mensajeExito = "Se aplicaron estas reglas exitosamente a las $total sedes registradas."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        aplicandoATodas = false,
                        error = "Error al replicar en todas las sedes: ${err.message}"
                    )
                }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(mensajeExito = null, error = null) }
    }
}
