package com.app.administradorfarmadon.configuracion.sucursales.logica
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.logica.UbicacionHelper
import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal
import com.app.administradorfarmadon.configuracion.sucursales.datos.SucursalesRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SucursalesViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: SucursalesRepository = SucursalesRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SucursalesUiState())
    val uiState: StateFlow<SucursalesUiState> = _uiState.asStateFlow()

    // Escuchas en vivo (R8): se cancelan limpio al salir de la pantalla.
    private val jobsObservacion = mutableListOf<Job>()

    init {
        cargarDatos()
    }

    override fun onCleared() {
        jobsObservacion.forEach { it.cancel() }
        jobsObservacion.clear()
        super.onCleared()
    }

    fun cargarDatos() {
        jobsObservacion.forEach { it.cancel() }
        jobsObservacion.clear()

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            val clienteId = repository.resolverClienteId()
            if (clienteId.isBlank()) {
                _uiState.update { it.copy(cargando = false, mensajeError = "No se pudo identificar la farmacia.") }
                return@launch
            }

            _uiState.update { it.copy(clienteId = clienteId) }

            // Observar Plan y Cupos
            jobsObservacion += launch {
                repository.observarInfoPlan(clienteId)
                    .catch { e ->
                        Log.e("SucursalesViewModel", "Error observando plan: ${e.message}", e)
                        _uiState.update { it.copy(mensajeError = "No se pudo cargar el plan de la farmacia: ${e.message}") }
                    }
                    .collect { infoPlan ->
                        _uiState.update {
                            it.copy(
                                planNombre = infoPlan.planNombre,
                                maxSucursales = infoPlan.maxSucursales
                            )
                        }
                    }
            }

            // Observar Sucursales (fuente de verdad en vivo: subcolección real)
            jobsObservacion += launch {
                repository.observarSucursales(clienteId)
                    .catch { e ->
                        Log.e("SucursalesViewModel", "Error observando sucursales: ${e.message}", e)
                        _uiState.update {
                            it.copy(
                                cargando = false,
                                mensajeError = "No se pudo cargar la lista de sedes: ${e.message}"
                            )
                        }
                    }
                    .collect { lista ->
                    _uiState.update { currentState ->
                        val seleccionada = if (currentState.esModoCreacion) {
                            null
                        } else {
                            currentState.sucursalSeleccionada?.let { sel ->
                                lista.find { it.id == sel.id }
                            }
                        }

                        val updated = currentState.copy(
                            sucursales = lista,
                            cargando = false,
                            mensajeError = if (lista.isNotEmpty() && currentState.mensajeError?.contains("cargar", ignoreCase = true) == true) null else currentState.mensajeError,
                            sucursalSeleccionada = seleccionada
                        )

                        if (!currentState.esModoCreacion && seleccionada != null) {
                            if (!currentState.hayCambiosSinGuardar) {
                                cargarFormularioDesdeSucursal(updated, seleccionada)
                            } else {
                                updated
                            }
                        } else if (!currentState.esModoCreacion && currentState.sucursalSeleccionada != null && seleccionada == null) {
                            // La sede fue eliminada remotamente mientras el usuario la visualizaba
                            updated.copy(
                                sucursalSeleccionada = null,
                                mensajeError = "La sede que estabas visualizando fue dada de baja o eliminada."
                            )
                        } else {
                            updated
                        }
                    }
                }
            }
        }
    }

    private var sucursalPendiente: Sucursal? = null
    private var accionPendienteVolver: (() -> Unit)? = null

    fun solicitarSeleccionarSucursal(sucursal: Sucursal) {
        if (_uiState.value.hayCambiosSinGuardar && _uiState.value.sucursalSeleccionada?.id != sucursal.id) {
            sucursalPendiente = sucursal
            _uiState.update { it.copy(mostrarDialogoDescartar = true) }
        } else {
            seleccionarSucursal(sucursal)
        }
    }

    fun solicitarIniciarNuevaSucursal() {
        if (_uiState.value.hayCambiosSinGuardar && !_uiState.value.esModoCreacion) {
            sucursalPendiente = null
            _uiState.update { it.copy(mostrarDialogoDescartar = true) }
        } else {
            iniciarNuevaSucursal()
        }
    }

    fun solicitarCerrarPanel() {
        if (_uiState.value.hayCambiosSinGuardar) {
            sucursalPendiente = null
            _uiState.update { it.copy(mostrarDialogoDescartar = true) }
        } else {
            cerrarPanel()
        }
    }

    fun cerrarPanel() {
        _uiState.update {
            it.copy(
                esModoCreacion = false,
                sucursalSeleccionada = null,
                formErrores = emptyMap()
            )
        }
    }

    fun solicitarVolver(onVolver: () -> Unit) {
        if (_uiState.value.hayCambiosSinGuardar) {
            accionPendienteVolver = onVolver
            _uiState.update { it.copy(mostrarDialogoDescartar = true) }
        } else {
            onVolver()
        }
    }

    fun confirmarDescartar() {
        _uiState.update { it.copy(mostrarDialogoDescartar = false) }
        val accionVolver = accionPendienteVolver
        if (accionVolver != null) {
            accionPendienteVolver = null
            accionVolver()
            return
        }
        val pendiente = sucursalPendiente
        if (pendiente != null) {
            sucursalPendiente = null
            seleccionarSucursal(pendiente)
        } else {
            cerrarPanel()
        }
    }

    fun seleccionarSucursal(sucursal: Sucursal) {
        _uiState.update { currentState ->
            val updated = currentState.copy(
                sucursalSeleccionada = sucursal,
                esModoCreacion = false,
                formErrores = emptyMap(),
                mensajeExito = null,
                mensajeError = null
            )
            cargarFormularioDesdeSucursal(updated, sucursal)
        }
    }

    fun iniciarNuevaSucursal() {
        val s = _uiState.value
        if (!s.puedeCrearMas) {
            _uiState.update { it.copy(mostrarDialogoLimite = true) }
            return
        }

        val siguienteNumero = s.totalSucursales + 1
        _uiState.update {
            it.copy(
                esModoCreacion = true,
                sucursalSeleccionada = null,
                formNombre = "",
                formDireccion = "",
                formTelefono = "",
                formResponsable = "",
                formCodigoInterno = "SEDE-0$siguienteNumero",
                formLatitud = null,
                formLongitud = null,
                formActiva = true,
                formErrores = emptyMap(),
                mensajeExito = null,
                mensajeError = null
            )
        }
    }

    fun cancelarCreacion() {
        val s = _uiState.value
        val primera = s.sucursales.firstOrNull()
        if (primera != null) {
            seleccionarSucursal(primera)
        } else {
            _uiState.update { it.copy(esModoCreacion = false) }
        }
    }

    fun onFieldChanged(field: String, value: String) {
        _uiState.update { currentState ->
            val nextErrors = currentState.formErrores.toMutableMap().apply { remove(field) }
            when (field) {
                "nombre" -> currentState.copy(formNombre = value, formErrores = nextErrors, mensajeError = null)
                "direccion" -> currentState.copy(formDireccion = value, formErrores = nextErrors, mensajeError = null)
                "telefono" -> currentState.copy(formTelefono = value.filter { it.isDigit() || it == '+' || it == ' ' }, formErrores = nextErrors, mensajeError = null)
                "responsable" -> currentState.copy(formResponsable = value, formErrores = nextErrors, mensajeError = null)
                "codigoInterno" -> currentState.copy(formCodigoInterno = value.uppercase(), formErrores = nextErrors, mensajeError = null)
                else -> currentState
            }
        }
    }

    fun onActivaChanged(activa: Boolean) {
        _uiState.update { it.copy(formActiva = activa) }
    }

    fun setFiltroEstado(estado: String) {
        _uiState.update { it.copy(filtroEstado = estado) }
    }

    fun onAddressSelected(direccion: String, latitud: Double?, longitud: Double?) {
        _uiState.update { currentState ->
            val nextErrors = currentState.formErrores.toMutableMap().apply { remove("direccion") }
            currentState.copy(
                formDireccion = direccion,
                formLatitud = latitud,
                formLongitud = longitud,
                formErrores = nextErrors
            )
        }
    }

    fun guardarSucursal() {
        val s = _uiState.value
        if (!s.esModoCreacion && !s.hayCambiosSinGuardar) return

        val errores = mutableMapOf<String, String>()

        val nombreTrim = s.formNombre.trim()
        val direccionTrim = s.formDireccion.trim()
        val telefonoTrim = s.formTelefono.trim()
        val responsableTrim = s.formResponsable.trim()
        val idAguardar = if (s.esModoCreacion) "" else (s.sucursalSeleccionada?.id ?: "")

        if (nombreTrim.isBlank()) {
            errores["nombre"] = "El nombre de la sucursal es obligatorio"
        } else if (nombreTrim.length < 3) {
            errores["nombre"] = "El nombre debe tener al menos 3 caracteres"
        } else if (s.sucursales.any { it.nombre.trim().equals(nombreTrim, ignoreCase = true) && it.id != idAguardar }) {
            errores["nombre"] = "Ya tienes una sede registrada con este nombre"
        }

        if (direccionTrim.isBlank()) {
            errores["direccion"] = "La dirección física de la sede es obligatoria"
        } else if (direccionTrim.length < 5) {
            errores["direccion"] = "Ingresa una dirección completa (calle, número o referencia)"
        } else if (s.sucursales.any { it.direccion.trim().equals(direccionTrim, ignoreCase = true) && it.id != idAguardar }) {
            errores["direccion"] = "Ya tienes una sede registrada en esta misma dirección"
        }

        val digitosTelefono = telefonoTrim.filter { it.isDigit() }
        if (telefonoTrim.isBlank()) {
            errores["telefono"] = "El teléfono de contacto de la sede es obligatorio"
        } else if (digitosTelefono.length < 7) {
            errores["telefono"] = "Ingresa un número de contacto válido (mínimo 7 dígitos)"
        }

        if (responsableTrim.isBlank()) {
            errores["responsable"] = "El nombre del responsable o encargado es obligatorio"
        }

        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(formErrores = errores) }
            return
        }

        if (s.esModoCreacion && !s.puedeCrearMas) {
            _uiState.update {
                it.copy(
                    mensajeError = "Has alcanzado el límite de ${s.maxSucursales} sedes contratadas en tu plan. Contacta a BRIXO para ampliar tu cobertura.",
                    mostrarDialogoLimite = true
                )
            }
            return
        }

        val clienteId = s.clienteId
        if (clienteId.isBlank()) {
            _uiState.update { it.copy(mensajeError = "No se pudo identificar la sesión de la farmacia.") }
            return
        }

        val esSedePrincipal = !s.esModoCreacion && s.sucursalSeleccionada?.esPrincipal == true
        val codigoFijo = if (esSedePrincipal) "SEDE-01" else (s.sucursalSeleccionada?.codigoInterno?.ifBlank { s.formCodigoInterno } ?: s.formCodigoInterno).trim()
        val estadoActivoReal = if (esSedePrincipal) true else s.formActiva

        // Bloqueo inmediato del botón (<50ms)
        _uiState.update { it.copy(guardando = true, mensajeError = null, mensajeExito = null) }

        viewModelScope.launch {
            try {
                val sucursalAguardar = Sucursal(
                    id = idAguardar,
                    nombre = nombreTrim,
                    direccion = direccionTrim,
                    telefono = telefonoTrim,
                    latitud = s.formLatitud,
                    longitud = s.formLongitud,
                    esPrincipal = esSedePrincipal,
                    activa = estadoActivoReal,
                    responsable = responsableTrim,
                    codigoInterno = codigoFijo
                )

                repository.guardarSucursal(clienteId, sucursalAguardar)

                _uiState.update {
                    it.copy(
                        guardando = false,
                        esModoCreacion = false,
                        sucursalSeleccionada = null,
                        mensajeExito = if (s.esModoCreacion) "¡Sede registrada exitosamente!" else "Cambios guardados correctamente."
                    )
                }
            } catch (e: Exception) {
                Log.e("SucursalesViewModel", "Error al guardar sucursal: ${e.message}", e)
                val esLimite = e.message?.contains("LIMITE_ALCANZADO") == true
                val msg = when {
                    esLimite -> "Has alcanzado el límite de sedes permitido por tu plan."
                    e.message?.contains("NOMBRE_DUPLICADO") == true -> "Ya tienes una sede registrada con este nombre."
                    e.message?.contains("DIRECCION_DUPLICADA") == true -> "Ya tienes una sede registrada en esta misma dirección."
                    e.message?.contains("No se encontró") == true -> "No se encontró el registro de la farmacia."
                    else -> "No se pudo guardar la sede. Revisa tu conexión a internet e intenta de nuevo."
                }
                _uiState.update {
                    it.copy(
                        guardando = false,
                        // Si es límite, el diálogo detallado es la única fuente (evita Toast redundante/contradictorio).
                        // Para el resto, el Toast lleva la causa real (R9: cero maquillaje).
                        mensajeError = if (esLimite) null else msg,
                        mostrarDialogoLimite = esLimite
                    )
                }
            }
        }
    }

    fun solicitarEliminar() {
        val s = _uiState.value
        val sel = s.sucursalSeleccionada ?: return
        if (sel.esPrincipal) {
            _uiState.update { it.copy(mensajeError = "La Sede Principal no puede ser eliminada.") }
            return
        }

        viewModelScope.launch {
            try {
                val db = FarmadonFirestore.db
                val usersSnap = com.app.administradorfarmadon.configuracion.sucursales.datos.SucursalesPaths.usuarios(db, s.clienteId)
                    .whereEqualTo("sucursalId", sel.id)
                    .get()
                    .await()
                val nombres = usersSnap.documents.mapNotNull { it.getString("nombre") }
                _uiState.update {
                    it.copy(
                        mostrarDialogoEliminar = true,
                        colaboradoresAsignadosNombres = nombres
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        mostrarDialogoEliminar = true,
                        colaboradoresAsignadosNombres = emptyList()
                    )
                }
            }
        }
    }

    fun confirmarEliminar() {
        val s = _uiState.value
        val sel = s.sucursalSeleccionada ?: return
        val clienteId = s.clienteId
        if (clienteId.isBlank()) return

        _uiState.update { it.copy(guardando = true, mostrarDialogoEliminar = false) }

        viewModelScope.launch {
            try {
                repository.eliminarSucursal(clienteId, sel.id)
                _uiState.update {
                    it.copy(
                        guardando = false,
                        sucursalSeleccionada = null,
                        mensajeExito = "Sucursal eliminada correctamente."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        guardando = false,
                        mensajeError = e.message ?: "Error al eliminar sucursal."
                    )
                }
            }
        }
    }

    fun cerrarDialogos() {
        sucursalPendiente = null
        accionPendienteVolver = null
        _uiState.update {
            it.copy(
                mostrarDialogoLimite = false,
                mostrarDialogoEliminar = false,
                mostrarDialogoDescartar = false
            )
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(mensajeExito = null, mensajeError = null) }
    }

    fun reintentarCarga() {
        _uiState.update { it.copy(mensajeError = null, cargando = true) }
        cargarDatos()
    }

    private fun cargarFormularioDesdeSucursal(state: SucursalesUiState, sucursal: Sucursal): SucursalesUiState {
        return state.copy(
            formNombre = sucursal.nombre,
            formDireccion = sucursal.direccion,
            formTelefono = sucursal.telefono,
            formResponsable = sucursal.responsable,
            formCodigoInterno = sucursal.codigoInterno,
            formLatitud = sucursal.latitud,
            formLongitud = sucursal.longitud,
            formActiva = sucursal.activa,
            formErrores = emptyMap()
        )
    }
}
