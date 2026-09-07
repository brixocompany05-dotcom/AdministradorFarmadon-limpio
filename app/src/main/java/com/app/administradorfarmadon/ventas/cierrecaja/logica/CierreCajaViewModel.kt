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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.app.administradorfarmadon.configuracion.pos.datos.PosConfigRepository
import com.app.administradorfarmadon.configuracion.pos.modelo.PosConfig
import com.app.administradorfarmadon.ventas.compartido.logica.ReglaBloqueoTurnoCaja

/**
 * Estado UI completo del Cierre y Control de Caja (R3/R8).
 */
data class CierreCajaUiState(
    val cargando: Boolean = true,
    val error: String? = null,
    val mensajeExito: String? = null,
    val procesandoAccion: Boolean = false,
    val estadoCaja: EstadoCaja = EstadoCaja(),
    val posConfig: PosConfig = PosConfig(),
    val movimientos: List<MovimientoCaja> = emptyList(),
    val conteoDenominaciones: Map<String, Int> = emptyMap(),
    // Ventas en pausa del cajero: no se cierra con canastas colgadas sin resolver.
    val ventasEnPausa: Int = 0,
    // Diálogos
    val mostrarDialogoApertura: Boolean = false,
    val mostrarDialogoMovimiento: Boolean = false,
    val tipoMovimientoManual: String = MovimientoCaja.TIPO_INGRESO,
    val mostrarDialogoConfirmarCierre: Boolean = false,
    val sesionCerradaResultado: CajaSesion? = null,
    val historialSesiones: List<CajaSesion> = emptyList(),
    val pestanaHistorialActiva: Boolean = false
) {
    /** Total contado físicamente sumando cada billete y moneda ingresada. */
    val totalContado: Double
        get() = conteoDenominaciones.entries.sumOf { (denom, cant) ->
            (com.app.administradorfarmadon.ventas.compartido.logica.MontoFormateador.normalizarMonto(denom) ?: 0.0) * cant
        }

    /** Diferencia respecto al efectivo que DEBE haber en el cajón (contado - esperado). */
    val diferenciaEfectivo: Double
        get() = kotlin.math.round((totalContado - estadoCaja.efectivoEsperado) * 100.0) / 100.0

    val estaAbierta: Boolean
        get() = estadoCaja.estado == CajaSesion.ESTADO_ABIERTA

    /** Modalidad Entrega Ciega: oculta el saldo teórico y la diferencia durante el arqueo. */
    val esCierreCiego: Boolean
        get() = posConfig.estaVigente && posConfig.caja.entregaCiegaTurno
}

/**
 * ViewModel de Cierre y Control de Caja (R1/R3/R8).
 * Administra la apertura, conteo físico de denominaciones, registro de ingresos/retiros,
 * escucha en vivo de movimientos y el cierre definitivo del turno.
 */
class CierreCajaViewModel(
    private val cajaRepository: CajaRepository = CajaRepository(),
    private val posConfigRepository: PosConfigRepository = PosConfigRepository(),
    private val ventasRepository: com.app.administradorfarmadon.ventas.compartido.datos.VentasRepository = com.app.administradorfarmadon.ventas.compartido.datos.VentasRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "CierreCajaViewModel"

        val DENOMINACIONES_BILLETES = listOf("200", "100", "50", "20", "10")
        val DENOMINACIONES_MONEDAS = listOf("5", "2", "1", "0.50", "0.20", "0.10", "0.05")
    }

    private val _uiState = MutableStateFlow(CierreCajaUiState())
    val uiState: StateFlow<CierreCajaUiState> = _uiState.asStateFlow()

    private var jobMovimientos: Job? = null
    private var jobEstadoCaja: Job? = null
    private var jobHistorial: Job? = null
    private var jobPosConfig: Job? = null
    private var jobPausas: Job? = null
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
                        jobMovimientos?.cancel()
                        jobMovimientos = null
                        jobHistorial?.cancel()
                        jobHistorial = null
                        jobPosConfig?.cancel()
                        jobPosConfig = null
                        jobPausas?.cancel()
                        jobPausas = null
                        _uiState.update {
                            it.copy(
                                cargando = true,
                                estadoCaja = EstadoCaja(),
                                posConfig = PosConfig(),
                                movimientos = emptyList(),
                                historialSesiones = emptyList(),
                                conteoDenominaciones = emptyMap(),
                                error = null,
                                mensajeExito = null
                            )
                        }
                        iniciarObservacionEstadoCaja()
                        iniciarObservacionHistorial()
                        iniciarObservacionPosConfig(sucursal)
                        iniciarObservacionPausas()
                    }
                }
        }
    }

    private fun iniciarObservacionPosConfig(sucursalId: String) {
        jobPosConfig?.cancel()
        if (sucursalId.isBlank()) return
        jobPosConfig = viewModelScope.launch {
            posConfigRepository.observar(sucursalId)
                .catch { Log.e(TAG, "Error escuchando posConfig: ${it.message}", it) }
                .collect { config ->
                    _uiState.update { it.copy(posConfig = config) }
                }
        }
    }

    private fun iniciarObservacionHistorial() {
        jobHistorial?.cancel()
        val cajeroId = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.idCajera
        jobHistorial = viewModelScope.launch {
            cajaRepository.observarHistorialSesiones(50, cajeroId = cajeroId)
                .catch { e ->
                    Log.e(TAG, "Error escuchando historial de caja: ${e.message}", e)
                }
                .collect { historial ->
                    _uiState.update { it.copy(historialSesiones = historial) }
                }
        }
    }

    fun togglePestanaHistorial(mostrar: Boolean? = null) {
        _uiState.update { it.copy(pestanaHistorialActiva = mostrar ?: !it.pestanaHistorialActiva) }
    }

    /**
     * Cuadre honesto: el turno no se cierra con canastas en pausa colgadas.
     * Se cobra, se reanuda o se descartan en Nueva Venta; si no, quedan ventas fantasma.
     */
    private fun iniciarObservacionPausas() {
        jobPausas?.cancel()
        val cajeroId = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.idCajera
        jobPausas = viewModelScope.launch {
            ventasRepository.observarSuspendidas(cajeroId)
                .catch { e ->
                    Log.e(TAG, "Error escuchando pausas para cierre: ${e.message}", e)
                }
                .collect { pausas ->
                    _uiState.update { it.copy(ventasEnPausa = pausas.size) }
                }
        }
    }

    /**
     * Escucha en tiempo real el puntero atómico del estado de caja individual del cajero (R8/R1).
     */
    private fun iniciarObservacionEstadoCaja() {
        jobEstadoCaja?.cancel()
        val cajeroId = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.idCajera
        jobEstadoCaja = viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            cajaRepository.observarEstadoCaja(cajeroId)
                .catch { e ->
                    Log.e(TAG, "Error escuchando estado de caja: ${e.message}", e)
                    _uiState.update { it.copy(cargando = false, error = e.message) }
                }
                .collect { nuevoEstado ->
                    val estadoPrevio = _uiState.value

                    var cerrarDialogoCierre = false
                    var cerrarDialogoApertura = false
                    var cerrarDialogoMovimiento = false
                    var mensajeAviso: String? = null

                    // Si el diálogo de confirmación de cierre estaba abierto y otra terminal ya cerró el turno
                    if (estadoPrevio.mostrarDialogoConfirmarCierre && nuevoEstado.estado != CajaSesion.ESTADO_ABIERTA) {
                        cerrarDialogoCierre = true
                        mensajeAviso = "El turno de caja ya fue cerrado desde otra terminal."
                    }

                    // Si el diálogo de apertura estaba abierto y otra terminal ya abrió la caja
                    if (estadoPrevio.mostrarDialogoApertura && nuevoEstado.estado == CajaSesion.ESTADO_ABIERTA) {
                        cerrarDialogoApertura = true
                        val responsable = nuevoEstado.abiertoPorNombre.ifBlank { "otro usuario" }
                        mensajeAviso = "La caja ya fue abierta por $responsable."
                    }

                    // Si el diálogo de movimiento estaba abierto y la caja ya fue cerrada
                    if (estadoPrevio.mostrarDialogoMovimiento && nuevoEstado.estado != CajaSesion.ESTADO_ABIERTA) {
                        cerrarDialogoMovimiento = true
                        if (mensajeAviso == null) {
                            mensajeAviso = "No se puede registrar movimientos porque la caja fue cerrada desde otra terminal."
                        }
                    }

                    // Si la caja no está abierta, limpiar cualquier conteo físico residual
                    val conteoLimpio = if (nuevoEstado.estado != CajaSesion.ESTADO_ABIERTA) emptyMap() else estadoPrevio.conteoDenominaciones

                    _uiState.update {
                        it.copy(
                            cargando = false,
                            estadoCaja = nuevoEstado,
                            mostrarDialogoConfirmarCierre = if (cerrarDialogoCierre) false else it.mostrarDialogoConfirmarCierre,
                            mostrarDialogoApertura = if (cerrarDialogoApertura) false else it.mostrarDialogoApertura,
                            mostrarDialogoMovimiento = if (cerrarDialogoMovimiento) false else it.mostrarDialogoMovimiento,
                            conteoDenominaciones = conteoLimpio,
                            error = mensajeAviso ?: it.error
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
        if (_uiState.value.estadoCaja.esTurnoVencido) {
            _uiState.update { it.copy(error = ReglaBloqueoTurnoCaja.MENSAJE_BLOQUEO_TURNO_VENCIDO) }
            return
        }
        _uiState.update { it.copy(mostrarDialogoApertura = true, error = null, mensajeExito = null) }
    }

    fun cerrarDialogoApertura() {
        _uiState.update { it.copy(mostrarDialogoApertura = false) }
    }

    /**
     * Abre el turno de caja con fondo inicial.
     */
    fun abrirCaja(fondoInicial: Double) {
        if (_uiState.value.estadoCaja.esTurnoVencido) {
            _uiState.update { it.copy(error = ReglaBloqueoTurnoCaja.MENSAJE_BLOQUEO_TURNO_VENCIDO) }
            return
        }
        if (fondoInicial < 0.0) {
            _uiState.update { it.copy(error = "El fondo inicial no puede ser negativo.") }
            return
        }
        if (_uiState.value.procesandoAccion) return

        val cId = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.idCajera
        val cNom = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.nombreUsuario

        viewModelScope.launch {
            _uiState.update { it.copy(procesandoAccion = true, error = null, mensajeExito = null) }
            val result = cajaRepository.abrirCaja(fondoInicial, cajeroId = cId, cajeroNombre = cNom)
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
        val cId = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.idCajera
        val validacion = ReglaBloqueoTurnoCaja.validarPermiteMovimientoManual(_uiState.value.estadoCaja, cId)
        if (validacion.isFailure) {
            _uiState.update { it.copy(error = validacion.exceptionOrNull()?.message) }
            return
        }
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
        val cId = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.idCajera
        val validacion = ReglaBloqueoTurnoCaja.validarPermiteMovimientoManual(_uiState.value.estadoCaja, cId)
        if (validacion.isFailure) {
            _uiState.update { it.copy(error = validacion.exceptionOrNull()?.message) }
            return
        }
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
            val result = cajaRepository.registrarMovimientoManual(tipo, monto, motivo, cajeroId = cId)
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

        if (_uiState.value.ventasEnPausa > 0) {
            val n = _uiState.value.ventasEnPausa
            _uiState.update {
                it.copy(error = "Tienes $n venta${if (n == 1) "" else "s"} en pausa. Córbralas, reanúdalas o elimínalas en Nueva Venta antes de cerrar, para no dejar ventas fantasma.")
            }
            return
        }

        val totalContado = _uiState.value.totalContado
        val cId = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.idCajera
        viewModelScope.launch {
            _uiState.update { it.copy(procesandoAccion = true, error = null, mensajeExito = null) }
            val result = cajaRepository.cerrarCaja(totalContado, observaciones, cajeroId = cId)
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
        jobEstadoCaja?.cancel()
        jobHistorial?.cancel()
        jobPosConfig?.cancel()
        jobPausas?.cancel()
        sucursalObserverJob?.cancel()
    }
}
