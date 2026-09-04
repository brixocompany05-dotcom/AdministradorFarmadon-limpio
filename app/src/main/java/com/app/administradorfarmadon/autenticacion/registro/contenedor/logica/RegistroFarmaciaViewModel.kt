package com.app.administradorfarmadon.autenticacion.registro.contenedor.logica
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.app.administradorfarmadon.autenticacion.datos.AuthPaths
import com.app.administradorfarmadon.base_datos.PlanSuscripcion
import com.app.administradorfarmadon.compartido.datos.EcosistemaPaths
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.organizacion.datos.CatalogoPaises
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.*
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.datos.Paso1UiState
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.logica.Paso1Validator
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.logica.UbicacionHelper
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.logica.EnviarCorreccionUseCase
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.logica.EnviarCorreccionUseCaseImpl
import com.app.administradorfarmadon.autenticacion.registro.paso2_plan.datos.Paso2UiState
import com.app.administradorfarmadon.autenticacion.registro.paso2_plan.datos.PlanesRepository
import com.app.administradorfarmadon.compartido.sha256
import java.util.*

data class RegistroFarmaciaUiState(
    val pasoActual: Int = 1,
    val paso1: Paso1UiState = Paso1UiState(),
    val paso2: Paso2UiState = Paso2UiState(),
    val cargando: Boolean = false,
    val precargandoCorreccion: Boolean = false,
    val exitoso: Boolean = false,
    val incidenteActual: RegistroIncidenteUi? = null,
    val progresoEnvio: Float = 0.0f,
    val mensajeProgreso: String = "",
    val esCorreccion: Boolean = false,
    val uidCorreccion: String? = null,
    val camposACorregir: List<String> = emptyList(),
    val planesRechazados: List<String> = emptyList(),
    val sugerirCambioPlan: Boolean = false,
    val accionSugerida: String? = null,
    val motivoRechazo: String? = null,
    val originalSnapshot: Map<String, Any?> = emptyMap(),
    val versionOriginal: Long = 1L,
    val requestIdCorreccion: String? = null,
)

@OptIn(FlowPreview::class)
class RegistroFarmaciaViewModel @JvmOverloads constructor(
    application: Application,
    internal val planesRepository: PlanesRepository = PlanesRepository(),
    internal val enviarCorreccionUseCase: EnviarCorreccionUseCase = EnviarCorreccionUseCaseImpl()
) : AndroidViewModel(application) {
    internal val _state = MutableStateFlow(RegistroFarmaciaUiState())
    val state: StateFlow<RegistroFarmaciaUiState> = _state.asStateFlow()

    internal var rawPlanes: List<PlanSuscripcion> = emptyList()
    internal var enviando = false
    private var planesJob: kotlinx.coroutines.Job? = null

    init {
        observarPlanes()
        
        // Optimización: Solo persistir el borrador después de 2 segundos de inactividad
        // Esto evita el lag al escribir (I/O excesivo)
        viewModelScope.launch {
            state
                .map { it.paso1 to it.paso2.planSeleccionado?.id }
                .distinctUntilChanged()
                .debounce(2000)
                .collect { 
                    val s = _state.value
                    if (!s.cargando && !s.exitoso && !s.paso2.cargandoPlanes) {
                        persistirBorrador()
                    }
                }
        }
    }

    /**
     * Escucha viva del catálogo de planes. Relanzable: un blip de red cierra el
     * flujo con la causa real en errorPlanes y REINTENTAR vuelve a abrirlo —”
     * jamás se convierte un fallo de lectura en "no hay planes publicados".
     */
    internal fun observarPlanes() {
        planesJob?.cancel()
        planesJob = viewModelScope.launch {
            _state.update { it.copy(paso2 = it.paso2.copy(cargandoPlanes = true, errorPlanes = null)) }
            planesRepository.getPlanesActivos()
                .catch { e ->
                    Log.e("REGISTRO", "Error al consultar planes comerciales", e)
                    _state.update { currentState ->
                        currentState.copy(
                            paso2 = currentState.paso2.copy(
                                cargandoPlanes = false,
                                errorPlanes = mapearErrorAViso(e)
                            )
                        )
                    }
                }
                .collect { lista ->
                    rawPlanes = lista
                    _state.update { currentState ->
                        actualizarPlanesDisponibles(currentState)
                    }
                }
        }
    }

    /** El aspirante toca REINTENTAR tras un fallo de lectura del catálogo. */
    fun reintentarCargaPlanes() = observarPlanes()

    internal fun actualizarPlanesDisponibles(currentState: RegistroFarmaciaUiState): RegistroFarmaciaUiState {
        // Catálogo por país: el aspirante solo ve planes publicados para su
        // país de operación. Sin país (transitorio pre-elección) → NADA:
        // jamás casar con planes legados sin país (la vieja mentira, cerrada).
        val paisActual = currentState.paso1.paisIso.trim().uppercase()
        val delPais = if (paisActual.isBlank()) emptyList()
        else rawPlanes.filter { it.paisIso.trim().uppercase() == paisActual }
        val filtrados = if (currentState.esCorreccion) {
            delPais.filter { it.id !in currentState.planesRechazados }
        } else {
            delPais
        }
        // Lógica honesta: si el plan precargado fue rechazado/observado, NO puede
        // seguir siendo la selección —” quedaría un botón "ENVIAR" activo con un
        // plan que no se ve en ninguna tarjeta. Se deselecciona para forzar una
        // elección real y visible.
        val planASeleccionar = currentState.paso2.planSeleccionado?.let { sel ->
            filtrados.find { it.id == sel.id }
        }
        
        val nextPaso2 = currentState.paso2.copy(
            planesDisponibles = filtrados,
            planSeleccionado = planASeleccionar,
            cargandoPlanes = false
        )
        
        return currentState.copy(paso2 = nextPaso2)
    }

    internal fun persistirBorrador() {
        val s = _state.value
        
        // Bloqueo estricto: No guardar si ya se tuvo éxito o si es una corrección
        if (s.exitoso || s.esCorreccion) return
        
        // Solo guardar si hay datos significativos para evitar archivos vacíos
        val tieneDatos = s.paso1.nombreFarmacia.isNotBlank() || 
                         s.paso1.dueno.isNotBlank() || 
                         s.paso1.email.isNotBlank() ||
                         s.paso1.ruc.isNotBlank()
        
        if (tieneDatos) {
            val draft = RegistroDraft(
                pasoActual = s.pasoActual,
                planSeleccionado = s.paso2.planSeleccionado?.id ?: "",
                nombreFarmacia = s.paso1.nombreFarmacia,
                dueno = s.paso1.dueno,
                email = s.paso1.email,
                telefono = s.paso1.telefono,
                ruc = s.paso1.ruc,
                direccion = s.paso1.direccion,
                latitud = s.paso1.latitud,
                longitud = s.paso1.longitud,
                paisIso = s.paso1.paisIso
            )
            RegistroDraftManager.saveDraft(getApplication(), draft)
        }
    }

    fun hayBorrador(): Boolean {
        return RegistroDraftManager.hasDraft(getApplication())
    }

    fun limpiarBorradorYReiniciar() {
        borrarBorrador()
        _state.update { 
            RegistroFarmaciaUiState(
                pasoActual = 1,
                paso1 = Paso1UiState(),
                paso2 = Paso2UiState()
            )
        }
    }

    fun restaurarDesdeBorrador() {
        viewModelScope.launch {
            val draft = withContext(Dispatchers.IO) {
                RegistroDraftManager.loadDraft(getApplication())
            } ?: return@launch

            _state.update { currentState ->
                // País + moneda del borrador: el aspirante reabre en el país que
                // eligió y el catálogo se filtra para ese país desde este render.
                // Borrador legado sin país → vacío honesto: vuelve a elegir.
                val paisRestaurado = draft.paisIso.uppercase()
                val monedaRestaurada = CatalogoPaises.monedaDe(paisRestaurado)
                val p1 = Paso1UiState(
                    nombreFarmacia = draft.nombreFarmacia,
                    dueno = draft.dueno,
                    email = draft.email,
                    telefono = CatalogoPaises.telefonoSinPrefijo(paisRestaurado, draft.telefono),
                    ruc = draft.ruc,
                    direccion = draft.direccion,
                    latitud = draft.latitud,
                    longitud = draft.longitud,
                    paisIso = paisRestaurado,
                    monedaIso = monedaRestaurada.first,
                    monedaSimbolo = monedaRestaurada.second
                )
                val p2 = Paso2UiState(
                    planSeleccionado = if (draft.planSeleccionado.isNotEmpty()) PlanSuscripcion(id = draft.planSeleccionado) else null
                )
                
                val updatedState = currentState.copy(
                    pasoActual = 1,
                    paso1 = p1,
                    paso2 = p2
                )
                // Recalcular YA el catálogo con el país restaurado: sin esto,
                // la pantalla esperaba una emisión futura de Firestore para
                // mostrar planes (esqueletos eternos tras reabrir el borrador).
                actualizarPlanesDisponibles(updatedState)
            }
        }
    }

    internal fun borrarBorrador() {
        RegistroDraftManager.clearDraft(getApplication())
    }

    /** País → moneda/prefijo derivan del catálogo (jamás se editan a mano). */
    fun setPaisSeleccionado(iso: String, monedaIso: String, simboloMoneda: String) {
        _state.update { currentState ->
            val isoNormalizado = iso.uppercase()
            val cambioDePais = currentState.paso1.paisIso != isoNormalizado
            // Cambiar de país invalida documento y teléfono tipados con la regla anterior.
            val p1 = currentState.paso1.copy(
                paisIso = isoNormalizado,
                monedaIso = monedaIso,
                monedaSimbolo = simboloMoneda,
                ruc = if (cambioDePais) "" else currentState.paso1.ruc,
                telefono = if (cambioDePais) "" else currentState.paso1.telefono,
                erroresCampos = currentState.paso1.erroresCampos.toMutableMap().apply {
                    remove("pais"); remove("ruc"); remove("telefono")
                }
            )
            // El catálogo depende del país: si el plan elegido pertenece a otro
            // país, se suelta la selección para forzar una elección real y visible.
            actualizarPlanesDisponibles(currentState.copy(paso1 = p1))
        }
    }

    fun onFieldChanged(field: String, value: String) {        _state.update { currentState ->
            val nextErrors = currentState.paso1.erroresCampos.toMutableMap().apply {
                remove(field)
            }

            var nextPaso1 = currentState.paso1.copy(erroresCampos = nextErrors)

            nextPaso1 = when (field) {
                "nombreFarmacia" -> nextPaso1.copy(nombreFarmacia = value)
                "dueno" -> nextPaso1.copy(dueno = value)
                "email" -> nextPaso1.copy(email = value.filterNot { it.isWhitespace() })
                "contrasena" -> {
                    val clean = value.filterNot { it.isWhitespace() }
                    val errors = nextErrors.toMutableMap()
                    if (currentState.paso1.confirmarContrasena.isNotEmpty() && clean != currentState.paso1.confirmarContrasena) {
                        errors["confirmarContrasena"] = "Las contraseñas no coinciden"
                    } else if (clean == currentState.paso1.confirmarContrasena) {
                        errors.remove("confirmarContrasena")
                    }
                    nextPaso1.copy(contrasena = clean, erroresCampos = errors)
                }

                "confirmarContrasena" -> {
                    val clean = value.filterNot { it.isWhitespace() }
                    val errors = nextErrors.toMutableMap()
                    if (clean.isNotEmpty() && clean != currentState.paso1.contrasena) {
                        errors["confirmarContrasena"] = "Las contraseñas no coinciden"
                    } else {
                        errors.remove("confirmarContrasena")
                    }
                    nextPaso1.copy(confirmarContrasena = clean, erroresCampos = errors)
                }

                "telefono" -> nextPaso1.copy(telefono = value.filter { it.isDigit() || it == '+' })
                "ruc" -> nextPaso1.copy(ruc = value.filterNot { it.isWhitespace() })
                "direccion" -> nextPaso1.copy(direccion = value, latitud = null, longitud = null)
                "notaAclaratoria" -> nextPaso1.copy(notaAclaratoria = value)
                else -> nextPaso1
            }


            currentState.copy(paso1 = nextPaso1)
        }
    }

    fun onAddressSelected(direccion: String, latitud: Double?, longitud: Double?) {
        _state.update { currentState ->
            val nextPaso1 = currentState.paso1.copy(
                direccion = direccion,
                latitud = latitud,
                longitud = longitud,
                erroresCampos = currentState.paso1.erroresCampos.toMutableMap().apply { remove("direccion") }
            )
            currentState.copy(paso1 = nextPaso1)
        }
    }

    fun onLocationManual(latitud: Double, longitud: Double) {
        _state.update { currentState ->
            val nextPaso1 = currentState.paso1.copy(latitud = latitud, longitud = longitud)
            currentState.copy(paso1 = nextPaso1)
        }

        UbicacionHelper.obtenerDireccion(getApplication(), latitud, longitud) { address ->
            _state.update { currentState ->
                val actual = currentState.paso1.direccion
                val lower = actual.lowercase()
                val esMzLt = lower.contains("mz") || lower.contains("lt") || lower.contains("manzana") || lower.contains("lote")
                if (esMzLt) return@update currentState
                val p1 = currentState.paso1.copy(direccion = address)
                currentState.copy(paso1 = p1)
            }
        }
    }

    fun onPlanSelected(plan: PlanSuscripcion) {
        _state.update { currentState ->
            val nextPaso2 = currentState.paso2.copy(planSeleccionado = plan, aviso = null)
            currentState.copy(paso2 = nextPaso2)
        }
        verificarAvisoPlan(plan)
    }

    fun nextStep() {
        val currentState = _state.value
        val step = currentState.pasoActual

        val errores = when (step) {
            1 -> Paso1Validator.validar(currentState.paso1, currentState.esCorreccion)
            else -> emptyMap()
        }

        if (errores.isNotEmpty()) {
            _state.update { 
                val p1 = it.paso1.copy(erroresCampos = errores)
                it.copy(paso1 = p1) 
            }
            return
        }

        when (step) {
            1 -> {
                val soloFormulario = currentState.esCorreccion &&
                    currentState.camposACorregir.none { it == "planId" || it == "plan" }
                if (soloFormulario) {
                    enviarCorreccion()
                } else {
                    _state.update { it.copy(pasoActual = 2, incidenteActual = null, paso2 = it.paso2.copy(aviso = null)) }
                    verificarAvisoTemprano(currentState.paso1.ruc)
                }
            }

            2 -> {
                if (currentState.esCorreccion) {
                    enviarCorreccion()
                } else {
                    registrarFarmacia()
                }
            }
        }
    }

    fun previousStep() {
        _state.update {
            if (it.pasoActual > 1) it.copy(
                pasoActual = it.pasoActual - 1,
                paso1 = it.paso1.copy(erroresCampos = emptyMap())
            )
            else it
        }
    }

    fun registrarFarmacia() {
        if (enviando || _state.value.cargando || _state.value.exitoso || _state.value.incidenteActual != null) {
            return
        }
        val errores = validar()
        if (errores.isNotEmpty()) {
            _state.update { it.copy(paso1 = it.paso1.copy(erroresCampos = errores)) }
            return
        }

        enviando = true
        ejecutarRegistroConFirebaseImpl()
    }

    fun descartarIncidente() {
        _state.update { it.copy(incidenteActual = null) }
    }

    fun ejecutarAccionIncidente(
        accion: RegistroIncidenteAccion,
        navigateToLogin: () -> Unit,
        onSoporteClick: (RegistroIncidenteUi) -> Unit
    ) {
        val stepActual = _state.value.pasoActual
        val incidente = _state.value.incidenteActual
        descartarIncidente()
        when (accion) {
            RegistroIncidenteAccion.ContactarSoporte -> {
                incidente?.let { onSoporteClick(it) }
            }

            RegistroIncidenteAccion.IrALogin -> navigateToLogin()
            RegistroIncidenteAccion.Reintentar -> {
                if (stepActual == 1) nextStep()
                else registrarFarmacia()
            }

            RegistroIncidenteAccion.ElegirOtroPlan -> {
                _state.update { it.copy(pasoActual = 2, paso2 = it.paso2.copy(planSeleccionado = null)) }
            }

            RegistroIncidenteAccion.VolverYCorregir -> _state.update { it.copy(pasoActual = 1) }
            RegistroIncidenteAccion.Descartar -> {}

            RegistroIncidenteAccion.RecargarSolicitud -> {
                val uid = state.value.uidCorreccion
                if (uid != null) precargarSolicitudCorreccion(uid)
            }
        }
    }

    fun precargarSolicitudCorreccion(uid: String) = precargarSolicitudCorreccionImpl(uid)

    fun enviarCorreccion() = enviarCorreccionImpl()

    private fun validar(): Map<String, String> {
        val currentState = _state.value
        val errores = mutableMapOf<String, String>().apply {
            putAll(Paso1Validator.validar(currentState.paso1, currentState.esCorreccion))
        }

        val planRequerido = !currentState.esCorreccion ||
            currentState.camposACorregir.any { it == "planId" || it == "plan" }
        if (planRequerido && currentState.paso2.planSeleccionado == null) {
            errores["plan"] = "Debe seleccionar un plan"
        }

        return errores
    }

    internal fun verificarAvisoTemprano(ruc: String) {
        viewModelScope.launch {
            try {
                val db = FarmadonFirestore.db
                val rucTrim = ruc.trim()
                if (rucTrim.isBlank()) {
                    _state.update { it.copy(paso2 = it.paso2.copy(aviso = null)) }
                    return@launch
                }
                val emailLower = _state.value.paso1.email.trim().lowercase()
                val resultado = RegistroFiltros.verificar(db, rucTrim, emailLower, null)
                val aviso: String? = when (resultado) {
                    is FiltroRegistroResultado.FALLA -> resultado.mensaje
                    FiltroRegistroResultado.PASA -> null
                }
                _state.update { it.copy(paso2 = it.paso2.copy(aviso = aviso)) }
            } catch (e: Exception) {
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    _state.update { it.copy(paso2 = it.paso2.copy(aviso = null)) }
                } else {
                    val mensaje = mapearErrorAViso(e)
                    _state.update { it.copy(paso2 = it.paso2.copy(aviso = mensaje)) }
                }
            }
        }
    }

    internal fun verificarAvisoPlan(plan: PlanSuscripcion) {
        if (plan.id.isBlank()) return
        viewModelScope.launch {
            try {
                val db = FarmadonFirestore.db
                // Verificación ACOTADA al plan: el RUC/correo ya fue validado al
                // entrar al paso (aviso temprano). Re-ejecutar el pipeline completo
                // aquí mostraba errores ajenos ("Atención con el RUC") al solo
                // tocar una tarjeta.
                val planDoc = EcosistemaPaths.planes(db)
                    .document(plan.id)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .await()
                val planMuerto = !planDoc.exists() ||
                        planDoc.getBoolean("activo") == false ||
                        planDoc.getBoolean("eliminado") == true
                if (planMuerto) {
                    _state.update { cs ->
                        // Suelta la selección: un plan muerto no puede seguir elegible.
                        cs.copy(
                            paso2 = cs.paso2.copy(
                                aviso = "El plan seleccionado ya no está disponible. Elige otro.",
                                planSeleccionado = null
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    _state.update { it.copy(paso2 = it.paso2.copy(aviso = null)) }
                } else {
                    val mensaje = mapearErrorAViso(e)
                    _state.update { it.copy(paso2 = it.paso2.copy(aviso = mensaje)) }
                }
            }
        }
    }

    private fun mapearErrorAViso(e: Throwable): String {
        return when {
            e is FirebaseNetworkException -> "No hay internet. Revisá tu conexión para poder verificar los datos."
            e is FirebaseFirestoreException && e.code in listOf(
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
            ) -> "El servidor no responde. Intentá de nuevo en unos segundos."
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Error de permisos. Contactá a soporte."
            else -> "Error al verificar: ${e.message ?: e.toString()}"
        }
    }
}
