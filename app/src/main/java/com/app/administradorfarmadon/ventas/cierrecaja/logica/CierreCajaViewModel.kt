package com.app.administradorfarmadon.ventas.cierrecaja.logica

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.ventas.compartido.datos.CajaRepository
import com.app.administradorfarmadon.ventas.compartido.modelo.CajaSesion
import com.app.administradorfarmadon.ventas.compartido.modelo.EstadoCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.MovimientoCaja
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI completo del Cierre y Control de Caja (R3/R8).
 */
data class CierreCajaUiState(
    val cargando: Boolean = true,
    val error: String? = null,
    val mensajeExito: String? = null,
    val procesandoAccion: Boolean = false,
    val estadoCaja: EstadoCaja = EstadoCaja(),
    val movimientos: List<MovimientoCaja> = emptyList(),
    val conteoDenominaciones: Map<String, Int> = emptyMap(),
    // Diálogos
    val mostrarDialogoApertura: Boolean = false,
    val mostrarDialogoMovimiento: Boolean = false,
    val tipoMovimientoManual: String = MovimientoCaja.TIPO_INGRESO,
    val mostrarDialogoConfirmarCierre: Boolean = false,
    val sesionCerradaResultado: CajaSesion? = null
) {
    /** Total contado físicamente sumando cada billete y moneda ingresada. */
    val totalContado: Double
        get() = conteoDenominaciones.entries.sumOf { (denom, cant) ->
            (denom.toDoubleOrNull() ?: 0.0) * cant
        }

    /** Diferencia respecto al efectivo que DEBE haber en el cajón (contado - esperado). */
    val diferenciaEfectivo: Double
        get() = kotlin.math.round((totalContado - estadoCaja.efectivoEsperado) * 100.0) / 100.0

    val estaAbierta: Boolean
        get() = estadoCaja.estado == CajaSesion.ESTADO_ABIERTA
}

/**
 * ViewModel de Cierre y Control de Caja (R1/R3/R8).
 * Administra la apertura, conteo físico de denominaciones, registro de ingresos/retiros,
 * escucha en vivo de movimientos y el cierre definitivo del turno.
 */
class CierreCajaViewModel(
    private val cajaRepository: CajaRepository = CajaRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "CierreCajaViewModel"

        val DENOMINACIONES_BILLETES = listOf("200", "100", "50", "20", "10")
        val DENOMINACIONES_MONEDAS = listOf("5", "2", "1", "0.50", "0.20", "0.10")
    }

    private val _uiState = MutableStateFlow(CierreCajaUiState())
    val uiState: StateFlow<CierreCajaUiState> = _uiState.asStateFlow()

    private var jobMovimientos: Job? = null

    init {
        iniciarObservacionEstadoCaja()
    }

    /**
     * Escucha en tiempo real el puntero atómico del estado de caja (R8).
     */
    private fun iniciarObservacionEstadoCaja() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            cajaRepository.observarEstadoCaja()
                .catch { e ->
                    Log.e(TAG, "Error escuchando estado de caja: ${e.message}", e)
                    _uiState.update { it.copy(cargando = false, error = e.message) }
                }
                .collect { nuevoEstado ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            estadoCaja = nuevoEstado,
                            error = null
                        )
                    }
                    // Si la caja está abierta y tiene sesión, sincronizar listener de movimientos
                    if (nuevoEstado.estado == CajaSesion.ESTADO_ABIERTA && nuevoEstado.sesionId.isNotBlank()) {
                        conectarMovimientosSesion(nuevoEstado.sesionId)
                    } else {
                        jobMovimientos?.cancel()
                        jobMovimientos = null
                        _uiState.update { it.copy(movimientos = emptyList()) }
                    }
                }
        }
    }

    private fun conectarMovimientosSesion(sesionId: String) {
        jobMovimientos?.cancel()
        jobMovimientos = viewModelScope.launch {
            cajaRepository.observarMovimientosSesion(sesionId)
                .catch { e ->
                    Log.e(TAG, "Error escuchando movimientos de sesión $sesionId: ${e.message}", e)
                }
                .collect { listaMovs ->
                    _uiState.update { it.copy(movimientos = listaMovs) }
                }
        }
    }

    // ───────────────────────────── ACCIONES DE CAJA ─────────────────────────────

    fun abrirDialogoApertura() {
        _uiState.update { it.copy(mostrarDialogoApertura = true, error = null, mensajeExito = null) }
    }

    fun cerrarDialogoApertura() {
        _uiState.update { it.copy(mostrarDialogoApertura = false) }
    }

    /**
     * Abre el turno de caja con fondo inicial.
     */
    fun abrirCaja(fondoInicial: Double) {
        if (fondoInicial < 0.0) {
            _uiState.update { it.copy(error = "El fondo inicial no puede ser negativo.") }
            return
        }
        if (_uiState.value.procesandoAccion) return

        viewModelScope.launch {
            _uiState.update { it.copy(procesandoAccion = true, error = null, mensajeExito = null) }
            val result = cajaRepository.abrirCaja(fondoInicial)
            result.onSuccess { sesion ->
                _uiState.update {
                    it.copy(
                        procesandoAccion = false,
                        mostrarDialogoApertura = false,
                        mensajeExito = "Caja abierta exitosamente con fondo inicial de S/ ${String.format(java.util.Locale.US, "%.2f", fondoInicial)}",
                        conteoDenominaciones = emptyMap()
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        procesandoAccion = false,
                        error = err.message ?: "No se pudo abrir la caja."
                    )
                }
            }
        }
    }

    fun abrirDialogoMovimiento(tipo: String) {
        _uiState.update {
            it.copy(
                mostrarDialogoMovimiento = true,
                tipoMovimientoManual = tipo,
                error = null,
                mensajeExito = null
            )
        }
    }

    fun cerrarDialogoMovimiento() {
        _uiState.update { it.copy(mostrarDialogoMovimiento = false) }
    }

    /**
     * Registra un ingreso o retiro manual de efectivo en el turno vigente.
     */
    fun registrarMovimientoManual(monto: Double, motivo: String) {
        if (monto <= 0.0) {
            _uiState.update { it.copy(error = "El monto debe ser mayor a 0.") }
            return
        }
        if (motivo.trim().length < 4) {
            _uiState.update { it.copy(error = "Describe el motivo del movimiento (mínimo 4 caracteres).") }
            return
        }
        if (_uiState.value.procesandoAccion) return

        val tipo = _uiState.value.tipoMovimientoManual
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoAccion = true, error = null, mensajeExito = null) }
            val result = cajaRepository.registrarMovimientoManual(tipo, monto, motivo)
            result.onSuccess { mov ->
                val tipoLegible = if (tipo == MovimientoCaja.TIPO_INGRESO) "Ingreso" else "Retiro"
                _uiState.update {
                    it.copy(
                        procesandoAccion = false,
                        mostrarDialogoMovimiento = false,
                        mensajeExito = "$tipoLegible de S/ ${String.format(java.util.Locale.US, "%.2f", monto)} registrado correctamente."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        procesandoAccion = false,
                        error = err.message ?: "Error al registrar movimiento."
                    )
                }
            }
        }
    }

    // ───────────────────────────── CONTEO FÍSICO ─────────────────────────────

    fun actualizarDenominacion(denominacion: String, cantidad: Int) {
        val cantFinal = cantidad.coerceAtLeast(0)
        val nuevoMapa = _uiState.value.conteoDenominaciones.toMutableMap()
        if (cantFinal == 0) {
            nuevoMapa.remove(denominacion)
        } else {
            nuevoMapa[denominacion] = cantFinal
        }
        _uiState.update { it.copy(conteoDenominaciones = nuevoMapa) }
    }

    fun limpiarConteo() {
        _uiState.update { it.copy(conteoDenominaciones = emptyMap()) }
    }

    // ───────────────────────────── CIERRE DE CAJA ─────────────────────────────

    fun abrirDialogoConfirmarCierre() {
        _uiState.update { it.copy(mostrarDialogoConfirmarCierre = true, error = null) }
    }

    fun cerrarDialogoConfirmarCierre() {
        _uiState.update { it.copy(mostrarDialogoConfirmarCierre = false) }
    }

    fun cerrarCaja(observaciones: String) {
        if (_uiState.value.procesandoAccion) return

        val totalContado = _uiState.value.totalContado
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoAccion = true, error = null, mensajeExito = null) }
            val result = cajaRepository.cerrarCaja(totalContado, observaciones)
            result.onSuccess { sesionCerrada ->
                _uiState.update {
                    it.copy(
                        procesandoAccion = false,
                        mostrarDialogoConfirmarCierre = false,
                        sesionCerradaResultado = sesionCerrada,
                        conteoDenominaciones = emptyMap(),
                        mensajeExito = "Caja cerrada exitosamente."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        procesandoAccion = false,
                        error = err.message ?: "No se pudo cerrar la caja."
                    )
                }
            }
        }
    }

    fun descartarResultadoCierre() {
        _uiState.update { it.copy(sesionCerradaResultado = null) }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(error = null, mensajeExito = null) }
    }

    override fun onCleared() {
        super.onCleared()
        jobMovimientos?.cancel()
    }
}
