package com.app.administradorfarmadon.configuracion.metodospago.logica

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.configuracion.metodospago.datos.MetodosPagoRepository
import com.app.administradorfarmadon.configuracion.metodospago.datos.SucursalCatalogo
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.configuracion.metodospago.modelo.TIPOS_PAGO_FIJOS
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Configuración de métodos de pago AISLADA POR SUCURSAL.
 * El administrador elige qué sucursal está configurando; cada sucursal tiene su
 * propio catálogo y los cambios jamás tocan otra sucursal ni el historial de pagos.
 */
class MetodosPagoViewModel(
    private val repository: MetodosPagoRepository = MetodosPagoRepository()
) : ViewModel() {

    var instancias by mutableStateOf<List<InstanciaPago>>(emptyList())
        private set
    var seleccionTipoId by mutableStateOf<String?>(null)
        private set
    var cargando by mutableStateOf(true)
        private set
    var procesandoId by mutableStateOf<String?>(null)
        private set
    var mensajeError by mutableStateOf<String?>(null)
        private set
    var mensajeExito by mutableStateOf<String?>(null)
        private set

    /** Error real de carga (red/permisos). No es lo mismo que una lista vacía. */
    var errorCarga by mutableStateOf<String?>(null)
        private set

    /** Lista de sucursales de la farmacia (para el selector del administrador). */
    var sucursales by mutableStateOf<List<SucursalCatalogo>>(emptyList())
        private set

    /** Sucursal que se está configurando en este momento. */
    var sucursalConfiguradaId by mutableStateOf<String?>(null)
        private set

    private val nombreSucursal: String
        get() = sucursales.firstOrNull { it.id == sucursalConfiguradaId }?.nombre
            ?: SessionManager.sucursalNombre.ifBlank { "Sede Principal" }

    private var jobSucursales: Job? = null
    private var jobMetodos: Job? = null

    init {
        val sesionSucursal = SessionManager.sucursalIdEfectiva
        val inicial = if (sesionSucursal.isBlank() || sesionSucursal == "todas") "principal" else sesionSucursal
        sucursalConfiguradaId = inicial
        iniciarObservacionSucursales()
        iniciarObservacionMetodos(inicial)
    }

    private fun iniciarObservacionSucursales() {
        jobSucursales?.cancel()
        jobSucursales = viewModelScope.launch {
            repository.observarSucursales()
                .catch { e ->
                    // Verdad, no silencio (R9): la falla se muestra y tiene salida con Reintentar.
                    errorCarga = "No se pudieron cargar las sucursales: ${e.message ?: "error de red"}"
                }
                .collect { lista ->
                    sucursales = lista
                    errorCarga = null
                    // Si la sucursal configurada ya no existe (fue eliminada), se cae a la principal.
                    val actual = sucursalConfiguradaId
                    if (actual != null && actual != "principal" && lista.isNotEmpty() && lista.none { it.id == actual }) {
                        seleccionarSucursal("principal")
                    }
                }
        }
    }

    private fun iniciarObservacionMetodos(sucursalId: String) {
        jobMetodos?.cancel()
        cargando = true
        jobMetodos = viewModelScope.launch {
            repository.observarMetodosPago(sucursalId)
                .catch { e ->
                    cargando = false
                    errorCarga = "No se pudieron cargar los métodos de pago: ${e.message ?: "error de red"}"
                }
                .collect { lista ->
                    instancias = lista
                    cargando = false
                    errorCarga = null
                }
        }
    }

    /** Reintenta la carga tras un fallo de red o permisos (R3: todo error tiene salida). */
    fun reintentarCarga() {
        errorCarga = null
        iniciarObservacionSucursales()
        iniciarObservacionMetodos(sucursalConfiguradaId ?: "principal")
    }

    /** El administrador elige qué sucursal configurar. Cada una tiene sus propios métodos. */
    fun seleccionarSucursal(sucursalId: String) {
        if (sucursalId == sucursalConfiguradaId) return
        sucursalConfiguradaId = sucursalId
        seleccionTipoId = null
        procesandoId = null
        limpiarFeedback()
        iniciarObservacionMetodos(sucursalId)
    }

    fun seleccionarTipo(id: String) {
        seleccionTipoId = id
        limpiarFeedback()
    }

    fun limpiarSeleccion() {
        seleccionTipoId = null
    }

    fun agregarInstancia(tipoId: String, datos: Map<String, String>) {
        if (procesandoId != null) return
        val sucursal = sucursalConfiguradaId ?: return
        val sucursalOrigen = sucursal
        val limpios = datos.mapValues { (_, v) -> v.trim() }
        // Regla de negocio: UNA sola cuenta por tipo de pago (Yape = una, Plin = una…).
        if (instancias.any { it.tipoId == tipoId }) {
            mensajeError = "Este tipo de pago ya tiene una cuenta registrada. Solo se permite una: edítala en vez de duplicarla."
            return
        }
        if (instancias.any { it.tipoId == tipoId && it.datos == limpios }) {
            mensajeError = "Ya existe una cuenta con estos datos en esta sucursal. No se duplica."
            return
        }
        procesandoId = "nueva-$tipoId"
        mensajeError = null
        mensajeExito = null
        viewModelScope.launch {
            repository.agregarInstancia(sucursal, tipoId, limpios).fold(
                onSuccess = {
                    aplicarResultado(sucursalOrigen) {
                        mensajeExito = "Cuenta agregada en $nombreSucursal"
                    }
                },
                onFailure = { e ->
                    aplicarResultado(sucursalOrigen) {
                        mensajeError = e.message ?: "No se pudo agregar la cuenta."
                    }
                }
            )
        }
    }

    /**
     * Tipos ÚNICOS (Efectivo, POS): no se crean cuentas. Encender = crear la
     * instancia única activa en la sucursal configurada; apagar = eliminarla.
     */
    fun setTipoUnicoActivo(tipoId: String, activo: Boolean) {
        if (procesandoId != null) return
        val sucursal = sucursalConfiguradaId ?: return
        val sucursalOrigen = sucursal
        val nombre = TIPOS_PAGO_FIJOS.firstOrNull { it.id == tipoId }?.nombre ?: tipoId
        if (!activo) {
            val instancia = instancias.firstOrNull { it.tipoId == tipoId }
            if (instancia == null) return
            procesandoId = "unico-$tipoId"
            mensajeError = null
            mensajeExito = null
            viewModelScope.launch {
                repository.eliminarInstancia(sucursal, instancia.id).fold(
                    onSuccess = {
                        aplicarResultado(sucursalOrigen) {
                            mensajeExito = "$nombre desactivado en $nombreSucursal"
                        }
                    },
                    onFailure = { e ->
                        aplicarResultado(sucursalOrigen) {
                            mensajeError = e.message ?: "No se pudo desactivar $nombre."
                        }
                    }
                )
            }
            return
        }
        val instancia = instancias.firstOrNull { it.tipoId == tipoId }
        if (instancia != null) {
            if (instancia.activa) return
            setInstanciaActiva(instancia.id, true)
            return
        }
        procesandoId = "unico-$tipoId"
        mensajeError = null
        mensajeExito = null
        viewModelScope.launch {
            repository.agregarInstancia(sucursal, tipoId, emptyMap()).fold(
                onSuccess = {
                    aplicarResultado(sucursalOrigen) {
                        mensajeExito = "$nombre activado en $nombreSucursal"
                    }
                },
                onFailure = { e ->
                    aplicarResultado(sucursalOrigen) {
                        mensajeError = e.message ?: "No se pudo activar $nombre."
                    }
                }
            )
        }
    }

    fun setInstanciaActiva(id: String, activa: Boolean) {
        if (procesandoId != null) return
        val sucursal = sucursalConfiguradaId ?: return
        val sucursalOrigen = sucursal
        procesandoId = "activa-$id"
        mensajeError = null
        mensajeExito = null
        viewModelScope.launch {
            repository.setInstanciaActiva(sucursal, id, activa).fold(
                onSuccess = {
                    aplicarResultado(sucursalOrigen) {
                        mensajeExito = if (activa) "Cuenta activada en $nombreSucursal" else "Cuenta desactivada en $nombreSucursal"
                    }
                },
                onFailure = { e ->
                    aplicarResultado(sucursalOrigen) {
                        mensajeError = e.message ?: "No se pudo actualizar la cuenta."
                    }
                }
            )
        }
    }

    fun guardarDatosInstancia(id: String, datos: Map<String, String>) {
        if (procesandoId != null) return
        val sucursal = sucursalConfiguradaId ?: return
        val sucursalOrigen = sucursal
        procesandoId = "datos-$id"
        mensajeError = null
        mensajeExito = null
        viewModelScope.launch {
            repository.guardarDatosInstancia(sucursal, id, datos).fold(
                onSuccess = {
                    aplicarResultado(sucursalOrigen) {
                        mensajeExito = "Datos guardados en $nombreSucursal"
                    }
                },
                onFailure = { e ->
                    aplicarResultado(sucursalOrigen) {
                        mensajeError = e.message ?: "No se pudieron guardar los datos."
                    }
                }
            )
        }
    }

    fun eliminarInstancia(id: String) {
        if (procesandoId != null) return
        val sucursal = sucursalConfiguradaId ?: return
        val sucursalOrigen = sucursal
        procesandoId = "eliminar-$id"
        mensajeError = null
        mensajeExito = null
        viewModelScope.launch {
            repository.eliminarInstancia(sucursal, id).fold(
                onSuccess = {
                    aplicarResultado(sucursalOrigen) {
                        mensajeExito = "Cuenta eliminada de $nombreSucursal"
                    }
                },
                onFailure = { e ->
                    aplicarResultado(sucursalOrigen) {
                        mensajeError = e.message ?: "No se pudo eliminar la cuenta."
                    }
                }
            )
        }
    }

    /**
     * Aplica el resultado de una operación solo si el usuario sigue en la misma
     * sucursal donde la inició; si cambió de sede mientras tanto, el mensaje no
     * se muestra en el contexto equivocado (R8: cero datos desactualizados).
     */
    private fun aplicarResultado(sucursalOrigen: String, bloque: () -> Unit) {
        procesandoId = null
        if (sucursalConfiguradaId != sucursalOrigen) {
            limpiarFeedback()
            return
        }
        bloque()
    }

    fun limpiarFeedback() {
        mensajeError = null
        mensajeExito = null
    }

    override fun onCleared() {
        jobSucursales?.cancel()
        jobMetodos?.cancel()
        super.onCleared()
    }
}
