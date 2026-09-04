package com.app.administradorfarmadon.facturacion.configuracion.logica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal
import com.app.administradorfarmadon.configuracion.sucursales.datos.SucursalesRepository
import com.app.administradorfarmadon.facturacion.configuracion.datos.ActaCambioEmisor
import com.app.administradorfarmadon.facturacion.configuracion.datos.EmisorFiscal
import com.app.administradorfarmadon.facturacion.configuracion.datos.FacturacionConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FacturacionConfigUiState(
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val emisor: EmisorFiscal? = null,
    val formRuc: String = "",
    val formRazonSocial: String = "",
    val formDireccionFiscal: String = "",
    val formPersonaId: String = "",
    val formPersonaToken: String = "",
    val formModo: String = EmisorFiscal.MODO_DESARROLLO,
    val verificadoOk: Boolean = false,
    val ultimoError: String = "",
    val formErrores: Map<String, String> = emptyMap(),
    val mensajeError: String? = null,
    val mensajeExito: String? = null,
    val esAdmin: Boolean = false,
    val esSedePrincipal: Boolean = false,
    val puedeEditar: Boolean = false,
    val formularioModificado: Boolean = false,
    val sedes: List<Sucursal> = emptyList(),
    val historial: List<ActaCambioEmisor> = emptyList(),
    val cargandoHistorial: Boolean = false
) {
    /**
     * Regla 3: Estado derivado, nunca declarado.
     */
    val estaCompleta: Boolean
        get() = formRuc.trim().length == 11 &&
                formRuc.trim().all { it.isDigit() } &&
                formRazonSocial.isNotBlank() &&
                formDireccionFiscal.isNotBlank() &&
                formPersonaId.isNotBlank() &&
                formPersonaToken.isNotBlank() &&
                verificadoOk
}

class FacturacionConfigViewModel(
    private val configRepository: FacturacionConfigRepository = FacturacionConfigRepository(),
    private val sucursalesRepository: SucursalesRepository = SucursalesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacturacionConfigUiState())
    val uiState: StateFlow<FacturacionConfigUiState> = _uiState.asStateFlow()

    private val farmaciaId: String
        get() = SessionManager.clienteIdGarantizado

    init {
        evaluarPermisos()
        cargarDatos()
    }

    private fun evaluarPermisos() {
        val rolActual = SessionManager.rol
        val esAdmin = rolActual.equals("Administrador", ignoreCase = true) ||
                rolActual.equals("Dueño", ignoreCase = true) ||
                rolActual.equals("Dueno", ignoreCase = true)
        val sucursalActiva = SessionManager.sucursalIdEfectiva.ifBlank { SessionManager.sucursalId }
        val esSedePrincipal = sucursalActiva.equals("principal", ignoreCase = true)
        val puedeEditar = esAdmin && esSedePrincipal

        _uiState.update {
            it.copy(
                esAdmin = esAdmin,
                esSedePrincipal = esSedePrincipal,
                puedeEditar = puedeEditar
            )
        }
    }

    private fun cargarDatos() {
        if (_uiState.value.formRuc.isBlank()) {
            _uiState.update { it.copy(cargando = true) }
        }

        // 1. Iniciar observación inmediata del emisor en Firebase (tiempo real, cero copia local)
        viewModelScope.launch {
            configRepository.observarEmisor(farmaciaId)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            mensajeError = "Error al sincronizar datos del emisor: ${e.message}"
                        )
                    }
                }
                .collect { emisor ->
                    _uiState.update { current ->
                        if (!current.formularioModificado && emisor != null) {
                            current.copy(
                                cargando = false,
                                emisor = emisor,
                                formRuc = emisor.ruc,
                                formRazonSocial = emisor.razonSocial,
                                formDireccionFiscal = emisor.direccionFiscal,
                                formPersonaId = emisor.personaId,
                                formPersonaToken = emisor.personaToken,
                                formModo = emisor.modo,
                                verificadoOk = emisor.verificadoOk,
                                ultimoError = emisor.ultimoError
                            )
                        } else {
                            current.copy(
                                cargando = false,
                                emisor = emisor,
                                verificadoOk = emisor?.verificadoOk ?: false,
                                ultimoError = emisor?.ultimoError.orEmpty()
                            )
                        }
                    }
                }
        }

        // 2. Iniciar observación de sucursales en paralelo
        viewModelScope.launch {
            sucursalesRepository.observarSucursales(farmaciaId)
                .catch { /* Falla tolerada en sedes */ }
                .collect { sedesList ->
                    _uiState.update { it.copy(sedes = sedesList) }
                }
        }

        // 3. Backfill de series en segundo plano (nunca bloquea la lectura ni la UI)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (farmaciaId.isNotBlank()) {
                    sucursalesRepository.asegurarSeriesFaltantes(farmaciaId)
                }
            } catch (_: Exception) {
                // Falla tolerada en backfill
            }
        }

        // 4. Observación viva del historial de auditoría de cambios del emisor (R8 - Verdad Vigente)
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoHistorial = true) }
            configRepository.observarHistorialEmisor(farmaciaId)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            cargandoHistorial = false,
                            mensajeError = "No se pudo sincronizar el historial de auditoría: ${e.message}"
                        )
                    }
                }
                .collect { listaActas ->
                    _uiState.update {
                        it.copy(
                            historial = listaActas,
                            cargandoHistorial = false
                        )
                    }
                }
        }
    }

    fun onFieldChanged(campo: String, valor: String) {
        if (!_uiState.value.puedeEditar) return

        _uiState.update { current ->
            val errores = current.formErrores.toMutableMap()
            errores.remove(campo)

            when (campo) {
                "ruc" -> {
                    // Filtrar solo dígitos y máximo 11 caracteres
                    val digitos = valor.filter { it.isDigit() }.take(11)
                    current.copy(
                        formRuc = digitos,
                        formularioModificado = true,
                        formErrores = errores,
                        verificadoOk = false // Cualquier cambio invalida la verificación previa
                    )
                }
                "razonSocial" -> current.copy(
                    formRazonSocial = valor,
                    formularioModificado = true,
                    formErrores = errores
                )
                "direccionFiscal" -> current.copy(
                    formDireccionFiscal = valor,
                    formularioModificado = true,
                    formErrores = errores
                )
                "personaId" -> current.copy(
                    formPersonaId = valor.trim(),
                    formularioModificado = true,
                    formErrores = errores,
                    verificadoOk = false
                )
                "personaToken" -> current.copy(
                    formPersonaToken = valor.trim(),
                    formularioModificado = true,
                    formErrores = errores,
                    verificadoOk = false
                )
                else -> current
            }
        }
    }

    fun onModoChanged(nuevoModo: String) {
        if (!_uiState.value.puedeEditar) return
        _uiState.update {
            it.copy(
                formModo = nuevoModo,
                formularioModificado = true,
                verificadoOk = false
            )
        }
    }

    fun guardarYVerificar() {
        val state = _uiState.value
        if (!state.puedeEditar) {
            _uiState.update {
                it.copy(mensajeError = "Acceso restringido: Solo el Administrador o Dueño desde la Sede Principal puede guardar la configuración fiscal.")
            }
            return
        }

        // Validación estricta de formulario
        val errores = mutableMapOf<String, String>()
        val rucTrim = state.formRuc.trim()
        if (rucTrim.length != 11 || !rucTrim.all { it.isDigit() }) {
            errores["ruc"] = "El RUC debe tener exactamente 11 dígitos numéricos."
        }
        if (state.formRazonSocial.trim().isBlank()) {
            errores["razonSocial"] = "Ingresa la Razón Social de la farmacia."
        }
        if (state.formDireccionFiscal.trim().isBlank()) {
            errores["direccionFiscal"] = "Ingresa la Dirección Fiscal registrada en SUNAT."
        }
        if (state.formPersonaId.trim().isBlank()) {
            errores["personaId"] = "Ingresa el Persona ID generado en apisunat.com."
        }
        if (state.formPersonaToken.trim().isBlank()) {
            errores["personaToken"] = "Ingresa el Persona Token generado en apisunat.com."
        }

        if (errores.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    formErrores = errores,
                    mensajeError = "Completa todos los campos obligatorios antes de verificar."
                )
            }
            return
        }

        val emisor = EmisorFiscal(
            ruc = rucTrim,
            razonSocial = state.formRazonSocial.trim(),
            direccionFiscal = state.formDireccionFiscal.trim(),
            personaId = state.formPersonaId.trim(),
            personaToken = state.formPersonaToken.trim(),
            modo = state.formModo
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    guardando = true,
                    mensajeError = null,
                    mensajeExito = null
                )
            }

            val emailUsuario = SessionManager.email.ifBlank { SessionManager.nombreUsuario.ifBlank { SessionManager.idCajera } }
            val res = configRepository.guardarYVerificarEmisor(farmaciaId, emisor, emailUsuario)

            res.fold(
                onSuccess = { emisorGuardado ->
                    _uiState.update {
                        it.copy(
                            guardando = false,
                            emisor = emisorGuardado,
                            verificadoOk = emisorGuardado.verificadoOk,
                            ultimoError = "",
                            formularioModificado = false,
                            mensajeExito = "Configuración fiscal guardada y verificada exitosamente con APISUNAT."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            guardando = false,
                            verificadoOk = false,
                            ultimoError = error.message ?: "Fallo de conexión",
                            mensajeError = error.message
                        )
                    }
                }
            )
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(mensajeError = null, mensajeExito = null) }
    }
}
