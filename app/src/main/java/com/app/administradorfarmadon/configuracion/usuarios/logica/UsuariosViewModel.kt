package com.app.administradorfarmadon.configuracion.usuarios.logica

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.compartido.datos.EcosistemaPaths
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal
import com.app.administradorfarmadon.configuracion.usuarios.datos.UsuarioFarmacia
import com.app.administradorfarmadon.configuracion.usuarios.datos.UsuariosRepository
import com.app.administradorfarmadon.modulos.domain.CatalogoHerramienta
import com.app.administradorfarmadon.modulos.domain.CatalogoHijo
import com.app.administradorfarmadon.modulos.domain.ModulosResueltos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UsuariosViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: UsuariosRepository = UsuariosRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UsuariosUiState())
    val uiState: StateFlow<UsuariosUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val TAG = "UsuariosViewModel"

    private var usuarioPendiente: UsuarioFarmacia? = null
    private var accionPendienteVolver: (() -> Unit)? = null
    private var observadoresJob: kotlinx.coroutines.Job? = null

    init {
        iniciarObservadores()
    }

    private fun iniciarObservadores() {
        observadoresJob?.cancel()
        observadoresJob = viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }

            val clienteId = repository.obtenerClienteIdActual()
            val currentUid = auth.currentUser?.uid ?: ""
            _uiState.update { it.copy(clienteId = clienteId, usuarioActualUid = currentUid) }

            if (clienteId.isBlank()) {
                Log.w(TAG, "No se encontró clienteId para el usuario actual.")
                _uiState.update { it.copy(cargando = false, mensajeError = "No se pudo identificar la farmacia activa.") }
                return@launch
            }

            // 1. Escuchar Roles de Farmacia (Excluyendo Dueño)
            launch {
                repository.observarRoles().collect { roles ->
                    _uiState.update { currentState ->
                        val rolPorDefecto = roles.firstOrNull { it.nombre.equals("Cajero", ignoreCase = true) } ?: roles.firstOrNull()
                        val formRolId = if (currentState.formRolId.isBlank()) (rolPorDefecto?.id ?: "") else currentState.formRolId
                        val formRolNombre = if (currentState.formRolNombre.isBlank()) (rolPorDefecto?.nombre ?: "") else currentState.formRolNombre
                        currentState.copy(
                            roles = roles,
                            formRolId = formRolId,
                            formRolNombre = formRolNombre
                        )
                    }
                }
            }

            // 2. Escuchar Herramientas del Plan Contratado (para selector por usuario) — R8 vivo
            launch { observarHerramientasDelPlan(clienteId) }

            // 3. Escuchar Sucursales de la Farmacia
            launch {
                repository.observarSucursales(clienteId)
                    .catch { e ->
                        emit(emptyList<Sucursal>())
                        _uiState.update { it.copy(mensajeError = "No se pudieron cargar las sedes: ${e.message}", cargando = false) }
                    }
                    .collect { sucursales ->
                        _uiState.update { it.copy(sucursales = sucursales) }
                    }
            }

            // 4. Escuchar Usuarios en Tiempo Real (incluye dados de baja para la sección de recontratación)
            launch {
                repository.observarUsuarios(clienteId, incluirDadasDeBaja = true)
                    .catch { e ->
                        emit(emptyList<UsuarioFarmacia>())
                        _uiState.update { it.copy(mensajeError = "No se pudieron cargar los colaboradores: ${e.message}", cargando = false) }
                    }
                    .collect { usuarios ->
                    _uiState.update { currentState ->
                        val seleccionActual = currentState.usuarioSeleccionado
                        val seleccionActualizada = if (seleccionActual != null) {
                            usuarios.find { it.id == seleccionActual.id }
                        } else {
                            null
                        }

                        val actualizado = currentState.copy(
                            usuarios = usuarios,
                            usuarioSeleccionado = if (!currentState.esModoCreacion) seleccionActualizada else currentState.usuarioSeleccionado,
                            cargando = false,
                            mensajeError = if (usuarios.isNotEmpty() && currentState.mensajeError?.contains("cargar", ignoreCase = true) == true) null else currentState.mensajeError
                        )

                        if (!currentState.esModoCreacion && seleccionActualizada != null) {
                            if (!currentState.hayCambiosSinGuardar) {
                                cargarFormularioDesdeUsuario(actualizado, seleccionActualizada)
                            } else {
                                actualizado
                            }
                        } else if (!currentState.esModoCreacion && seleccionActual != null && seleccionActualizada == null) {
                            // El colaborador seleccionado fue dado de baja remotamente
                            actualizado.copy(
                                usuarioSeleccionado = null,
                                mensajeError = "El colaborador \"${seleccionActual.nombre}\" ya no está disponible (fue dado de baja)."
                            )
                        } else {
                            actualizado
                        }
                    }
                }
            }
        }
    }

    private val dbHerramientas = FarmadonFirestore.db
    private var catalogoHerramientas: List<CatalogoHerramienta> = emptyList()
    private var featuresPlanHerramientas: Set<String> = emptySet()
    private var overridesClienteHerramientas: Map<String, Boolean> = emptyMap()
    private var planNombreCache: String = ""

    private suspend fun observarHerramientasDelPlan(clienteId: String) {
        if (clienteId.isBlank()) {
            _uiState.update { it.copy(herramientasPlan = emptyList(), cargandoHerramientas = false) }
            return
        }
        // Listener catálogo global
        try {
            EcosistemaPaths.herramientasPlan(dbHerramientas).whereEqualTo("estado", "activo").get().await().let { snap ->
                catalogoHerramientas = snap.documents.mapNotNull { doc ->
                    val nombre = doc.getString("nombre") ?: return@mapNotNull null
                    if (nombre.isBlank()) return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    val hijos = (doc.get("hijos") as? List<Map<String, Any?>>)?.mapNotNull { h ->
                        CatalogoHijo(clave = h["clave"] as? String ?: "", nombre = h["nombre"] as? String ?: "", activo = (h["activo"] as? Boolean) ?: true)
                    } ?: emptyList()
                    CatalogoHerramienta(id = doc.id, modulo = doc.getString("modulo") ?: doc.id, nombre = nombre, categoria = doc.getString("categoria") ?: "OTROS", orden = doc.getDouble("orden")?.toInt() ?: 0, icono = doc.getString("icono") ?: "", estado = doc.getString("estado") ?: "activo", hijos = hijos)
                }
            }
        } catch (_: Exception) { catalogoHerramientas = emptyList() }

        // Listener suscripción + farmacia en vivo (simplificado con snapshot listeners)
        val susRef = FarmadonFirestore.db.collection("farmaciapp").document("app").collection("farmacias").document(clienteId).collection("suscripciones").limit(1)
        val farmRef = FarmadonFirestore.db.collection("farmaciapp").document("app").collection("farmacias").document(clienteId)

        // Lectura inicial + escucha viva con callbackFlow manual simplificado: usamos listeners y resolvemos
        try {
            val subSnap = susRef.get().await().documents.firstOrNull()
            @Suppress("UNCHECKED_CAST")
            featuresPlanHerramientas = (subSnap?.get("planHerramientasContratadas") as? List<String>)?.toSet() ?: emptySet()
            planNombreCache = subSnap?.getString("planNombre") ?: ""
            val farmSnap = farmRef.get().await()
            @Suppress("UNCHECKED_CAST")
            overridesClienteHerramientas = (farmSnap.get("featureOverrides") as? Map<String, Boolean>) ?: emptyMap()
            if (planNombreCache.isBlank()) planNombreCache = farmSnap.getString("planNombre") ?: farmSnap.getString("plan") ?: "Plan Contratado"
        } catch (_: Exception) { }

        fun resolverYActualizar() {
            // CONTRATO CONGELADO: sin features pactadas no se hereda todo el catálogo.
            // Vacío honesto = permisos vacíos (el selector no inventa accesos).
            val resueltos = if (catalogoHerramientas.isNotEmpty() && featuresPlanHerramientas.isNotEmpty()) {
                ModulosResueltos.resolver(catalogoHerramientas, featuresPlanHerramientas, overridesClienteHerramientas)
            } else if (catalogoHerramientas.isNotEmpty()) {
                emptyList()
            } else emptyList()
            _uiState.update { it.copy(herramientasPlan = resueltos, planNombre = planNombreCache, cargandoHerramientas = false) }
            // Base nueva: todo usuario nace con mapa explícito. Solo inicializar en creación.
            val s = _uiState.value
            if (s.esModoCreacion && s.formPermisosModulos.isEmpty() && resueltos.isNotEmpty()) {
                val todosTrue = resueltos.associate { it.modulo to true }
                _uiState.update { it.copy(formPermisosModulos = todosTrue) }
            }
        }
        resolverYActualizar()

        // Escuchas vivas para que selector se repinte si BRIXO cambia plan
        try {
            EcosistemaPaths.herramientasPlan(dbHerramientas).whereEqualTo("estado", "activo").addSnapshotListener { snap, _ ->
                catalogoHerramientas = snap?.documents?.mapNotNull { doc ->
                    val nombre = doc.getString("nombre") ?: return@mapNotNull null
                    if (nombre.isBlank()) return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    val hijos = (doc.get("hijos") as? List<Map<String, Any?>>)?.mapNotNull { h -> CatalogoHijo(h["clave"] as? String ?: "", h["nombre"] as? String ?: "", (h["activo"] as? Boolean) ?: true) } ?: emptyList()
                    CatalogoHerramienta(doc.id, doc.getString("modulo") ?: doc.id, nombre, doc.getString("categoria") ?: "OTROS", doc.getDouble("orden")?.toInt() ?: 0, doc.getString("icono") ?: "", doc.getString("estado") ?: "activo", hijos)
                } ?: emptyList()
                resolverYActualizar()
            }
            susRef.addSnapshotListener { snap, _ ->
                val doc = snap?.documents?.firstOrNull()
                @Suppress("UNCHECKED_CAST")
                featuresPlanHerramientas = (doc?.get("planHerramientasContratadas") as? List<String>)?.toSet() ?: emptySet()
                planNombreCache = doc?.getString("planNombre") ?: planNombreCache
                resolverYActualizar()
            }
            farmRef.addSnapshotListener { snap, _ ->
                @Suppress("UNCHECKED_CAST")
                overridesClienteHerramientas = (snap?.get("featureOverrides") as? Map<String, Boolean>) ?: emptyMap()
                if (planNombreCache.isBlank()) planNombreCache = snap?.getString("planNombre") ?: snap?.getString("plan") ?: planNombreCache
                resolverYActualizar()
            }
        } catch (_: Exception) { }
    }

    fun solicitarSeleccionarUsuario(usuario: UsuarioFarmacia) {
        if (_uiState.value.guardando) return
        if (_uiState.value.hayCambiosSinGuardar && _uiState.value.usuarioSeleccionado?.id != usuario.id) {
            usuarioPendiente = usuario
            _uiState.update { it.copy(mostrarDialogoDescartar = true) }
        } else {
            seleccionarUsuario(usuario)
        }
    }

    fun solicitarIniciarNuevoUsuario() {
        if (_uiState.value.guardando) return
        if (_uiState.value.hayCambiosSinGuardar && !_uiState.value.esModoCreacion) {
            usuarioPendiente = null
            _uiState.update { it.copy(mostrarDialogoDescartar = true) }
        } else {
            iniciarNuevoUsuario()
        }
    }

    fun solicitarVolver(onVolver: () -> Unit) {
        if (_uiState.value.guardando) return
        if (_uiState.value.hayCambiosSinGuardar) {
            accionPendienteVolver = onVolver
            _uiState.update { it.copy(mostrarDialogoDescartar = true) }
        } else {
            onVolver()
        }
    }

    fun solicitarCerrarPanel() {
        if (_uiState.value.guardando) return
        if (_uiState.value.hayCambiosSinGuardar) {
            usuarioPendiente = null
            _uiState.update { it.copy(mostrarDialogoDescartar = true) }
        } else {
            cerrarPanel()
        }
    }

    fun cerrarPanel() {
        _uiState.update {
            it.copy(
                esModoCreacion = false,
                usuarioSeleccionado = null,
                formPassword = "",
                formErrores = emptyMap()
            )
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
        val pendiente = usuarioPendiente
        if (pendiente != null) {
            usuarioPendiente = null
            seleccionarUsuario(pendiente)
        } else {
            cerrarPanel()
        }
    }

    fun seleccionarUsuario(usuario: UsuarioFarmacia) {
        _uiState.update { currentState ->
            val updated = currentState.copy(
                usuarioSeleccionado = usuario,
                esModoCreacion = false,
                formPassword = "",
                formErrores = emptyMap(),
                mensajeExito = null,
                mensajeError = null
            )
            cargarFormularioDesdeUsuario(updated, usuario)
        }
    }

    fun iniciarNuevoUsuario() {
        val s = _uiState.value
        val primerRol = s.roles.firstOrNull { it.nombre.equals("Cajero", ignoreCase = true) } ?: s.roles.firstOrNull()
        val primeraSede = s.sucursales.firstOrNull { it.esPrincipal } ?: s.sucursales.firstOrNull()

        // Inicializar permisos con todo lo del plan habilitado (herencia base)
        val permisosIniciales = _uiState.value.herramientasPlan.associate { it.modulo to true }
        _uiState.update {
            it.copy(
                esModoCreacion = true,
                usuarioSeleccionado = null,
                formNombre = "",
                formDni = "",
                formTelefono = "",
                formEmail = "",
                formPassword = "",
                formRolId = primerRol?.id ?: "",
                formRolNombre = primerRol?.nombre ?: "Cajero",
                formSucursalId = primeraSede?.id ?: "todas",
                formSucursalNombre = primeraSede?.nombre ?: "Todas las Sedes",
                formAcceso = true,
                formErrores = emptyMap(),
                mensajeExito = null,
                mensajeError = null,
                formPermisosModulos = permisosIniciales
            )
        }
    }

    fun onFieldChanged(field: String, value: String) {
        _uiState.update { currentState ->
            val nextErrors = currentState.formErrores.toMutableMap().apply { remove(field) }
            when (field) {
                "nombre" -> currentState.copy(formNombre = value.take(60), formErrores = nextErrors, mensajeError = null)
                "dni" -> currentState.copy(formDni = value.filter { it.isDigit() }.take(12), formErrores = nextErrors, mensajeError = null)
                "telefono" -> currentState.copy(formTelefono = value.filter { it.isDigit() || it == '+' }.take(15), formErrores = nextErrors, mensajeError = null)
                "email" -> currentState.copy(formEmail = value.trim().lowercase().take(60), formErrores = nextErrors, mensajeError = null)
                "password" -> currentState.copy(formPassword = value.trim().take(40), formErrores = nextErrors, mensajeError = null)
                else -> currentState
            }
        }
    }

    fun onRolSelected(rolId: String, rolNombre: String) {
        _uiState.update { it.copy(formRolId = rolId, formRolNombre = rolNombre) }
    }

    fun onSucursalSelected(sucursalId: String, sucursalNombre: String) {
        _uiState.update { it.copy(formSucursalId = sucursalId, formSucursalNombre = sucursalNombre) }
    }

    fun onAccesoChanged(acceso: Boolean) {
        _uiState.update { it.copy(formAcceso = acceso) }
    }

    fun onPermisoModuloChanged(modulo: String, habilitado: Boolean) {
        _uiState.update { current ->
            // Si es primera edición de usuario migrado (mapa vacío), inicializar con
            // todas las herramientas del plan en true, luego aplicar el toggle.
            // Así no se deja mapa con una sola clave y el resto huérfano.
            val base = if (current.formPermisosModulos.isEmpty() && current.herramientasPlan.isNotEmpty()) {
                current.herramientasPlan.associate { it.modulo to true }.toMutableMap()
            } else {
                current.formPermisosModulos.toMutableMap()
            }
            base[modulo] = habilitado
            // Limpiar permisos que no están en el plan (higiene)
            val planModulos = current.herramientasPlan.map { it.modulo }.toSet()
            val limpio = base.filterKeys { it in planModulos }
            current.copy(formPermisosModulos = limpio)
        }
    }

    fun onToggleTodosPermisos(habilitarTodos: Boolean) {
        _uiState.update { current ->
            val todos = current.herramientasPlan.associate { it.modulo to habilitarTodos }
            current.copy(formPermisosModulos = todos)
        }
    }

    fun setFiltroRol(rol: String?) {
        _uiState.update { it.copy(filtroRol = rol, verDadasDeBaja = false) }
    }

    fun setBusquedaQuery(query: String) {
        _uiState.update { it.copy(busquedaQuery = query) }
    }

    fun guardarUsuario() {
        val s = _uiState.value
        if (s.guardando) return
        if (!s.esModoCreacion && !s.hayCambiosSinGuardar) return

        val errores = mutableMapOf<String, String>()
        val nombreTrim = s.formNombre.trim()
        val dniTrim = s.formDni.trim().filter { it.isDigit() }
        val emailTrim = s.formEmail.trim().lowercase()
        val passTrim = s.formPassword.trim()
        val idAguardar = if (s.esModoCreacion) "" else (s.usuarioSeleccionado?.id ?: "")

        val nombreSanitizado = nombreTrim.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(" ") { palabra ->
                palabra.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

        if (nombreSanitizado.isBlank()) {
            errores["nombre"] = "El nombre y apellidos son obligatorios"
        } else if (nombreSanitizado.length < 3) {
            errores["nombre"] = "El nombre debe tener al menos 3 caracteres"
        } else if (s.usuarios.any { it.nombre.trim().equals(nombreSanitizado, ignoreCase = true) && it.rolId == s.formRolId && it.id != idAguardar }) {
            errores["nombre"] = "Ya existe un colaborador con este mismo nombre y rol (${s.formRolNombre})"
        }

        if (dniTrim.isBlank()) {
            errores["dni"] = "El DNI o documento es obligatorio"
        } else if (dniTrim.length < 6) {
            errores["dni"] = "El documento debe tener al menos 6 dígitos"
        } else if (s.usuarios.any { it.dni.trim() == dniTrim && it.id != idAguardar }) {
            errores["dni"] = "Ya existe un colaborador registrado con este DNI"
        }

        if (emailTrim.isBlank()) {
            errores["email"] = "El correo electrónico es obligatorio para el acceso"
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches()) {
            errores["email"] = "Ingresa un correo válido (ej. usuario@gmail.com)"
        } else if (s.usuarios.any { it.email.trim().equals(emailTrim, ignoreCase = true) && it.id != idAguardar }) {
            errores["email"] = "Ya existe un colaborador registrado con este correo"
        }

        // Validación de Contraseña de Acceso (Solo en creación)
        if (s.esModoCreacion) {
            if (passTrim.isBlank()) {
                errores["password"] = "Define una contraseña de acceso para el colaborador"
            } else if (passTrim.length < 6) {
                errores["password"] = "La contraseña debe tener al menos 6 caracteres"
            }
        }

        if (s.formRolId.isBlank()) {
            errores["rol"] = "Selecciona un rol para el colaborador"
        }

        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(formErrores = errores) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mensajeError = null) }
            try {
                // Filtrar permisos solo a módulos del plan (raíz: no guardar basura)
                val permisosFiltrados = s.formPermisosModulos.filterKeys { k -> s.herramientasPlan.any { it.modulo == k } }
                repository.guardarUsuario(
                    clienteId = s.clienteId,
                    usuarioId = if (s.esModoCreacion) null else s.usuarioSeleccionado?.id,
                    nombre = nombreSanitizado,
                    dni = dniTrim,
                    telefono = s.formTelefono.trim().filter { it.isDigit() || it == '+' },
                    email = emailTrim,
                    password = passTrim.ifBlank { null },
                    rolId = s.formRolId,
                    rolNombre = s.formRolNombre,
                    sucursalId = s.formSucursalId,
                    sucursalNombre = s.formSucursalNombre,
                    acceso = s.formAcceso,
                    permisosModulos = permisosFiltrados
                )

                _uiState.update {
                    it.copy(
                        guardando = false,
                        esModoCreacion = false,
                        usuarioSeleccionado = null,
                        mensajeExito = if (s.esModoCreacion) "Colaborador registrado exitosamente." else "Datos del colaborador actualizados."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando colaborador: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        guardando = false,
                        mensajeError = traducirError(e, "Error al guardar colaborador.")
                    )
                }
            }
        }
    }

    fun solicitarSuspender() {
        _uiState.update { it.copy(mostrarDialogoSuspender = true) }
    }

    fun solicitarEliminar() {
        _uiState.update { it.copy(mostrarDialogoEliminar = true) }
    }

    fun toggleVerDadasDeBaja() {
        _uiState.update {
            it.copy(
                verDadasDeBaja = !it.verDadasDeBaja,
                usuarioSeleccionado = null,
                esModoCreacion = false,
                filtroRol = null,
                mensajeExito = null,
                mensajeError = null
            )
        }
    }

    /**
     * U1 — Recontratar: abre la ficha sellada en modo edición.
     * Guardar escribirá dadoDeBaja=false + acceso según formulario, con la MISMA
     * identidad (uid/correo), evitando el choque "correo ya registrado".
     */
    fun recontratarUsuario(usuario: UsuarioFarmacia) {
        if (_uiState.value.guardando) return
        seleccionarUsuario(usuario)
        _uiState.update {
            it.copy(
                formAcceso = true,
                verDadasDeBaja = true,
                mensajeError = null,
                mensajeExito = "Modo recontratación: actualiza los datos y guarda para reactivar su acceso. Usa el envío de restablecimiento si no recuerda su clave."
            )
        }
    }

    fun confirmarEliminar(motivo: String) {
        val s = _uiState.value
        val usuario = s.usuarioSeleccionado ?: return
        val motivoLimpio = motivo.trim()
        if (motivoLimpio.isBlank()) return

        if (s.guardando) return
        if (usuario.id == s.usuarioActualUid) {
            _uiState.update {
                it.copy(
                    mostrarDialogoEliminar = false,
                    mensajeError = "Operación inválida: No puedes dar de baja tu propia cuenta activa."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mostrarDialogoEliminar = false, mensajeError = null, mensajeExito = null) }
            try {
                repository.darDeBajaUsuario(
                    clienteId = s.clienteId,
                    usuarioId = usuario.id,
                    nombreUsuario = usuario.nombre,
                    motivo = motivoLimpio
                )

                _uiState.update {
                    it.copy(
                        guardando = false,
                        esModoCreacion = false,
                        usuarioSeleccionado = null,
                        mensajeError = null,
                        mensajeExito = "${usuario.nombre} fue dado de baja. Puedes recontratarlo desde la sección DADOS DE BAJA."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dando de baja colaborador: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        guardando = false,
                        mensajeExito = null,
                        mensajeError = traducirError(e, "Error al dar de baja al colaborador.")
                    )
                }
            }
        }
    }

    fun confirmarCambioAcceso(nuevoAcceso: Boolean) {
        val s = _uiState.value
        val usuario = s.usuarioSeleccionado ?: return
        if (s.guardando) return

        if (usuario.id == s.usuarioActualUid && !nuevoAcceso) {
            _uiState.update {
                it.copy(
                    mostrarDialogoSuspender = false,
                    mensajeError = "Operación inválida: No puedes suspender tu propia cuenta activa."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mostrarDialogoSuspender = false, mensajeError = null, mensajeExito = null) }
            try {
                val motivo = if (nuevoAcceso) "Reactivación de acceso por administración" else "Suspensión de acceso al punto de venta"
                repository.cambiarEstadoAcceso(s.clienteId, usuario.id, nuevoAcceso, motivo)

                _uiState.update {
                    it.copy(
                        guardando = false,
                        formAcceso = nuevoAcceso,
                        mensajeError = null,
                        mensajeExito = if (nuevoAcceso) "Acceso reactivado correctamente." else "Acceso suspendido correctamente."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cambiando acceso de colaborador: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        guardando = false,
                        mensajeExito = null,
                        mensajeError = traducirError(e, "Error al cambiar estado de acceso.")
                    )
                }
            }
        }
    }

    fun cerrarDialogos() {
        usuarioPendiente = null
        accionPendienteVolver = null
        _uiState.update {
            it.copy(
                mostrarDialogoSuspender = false,
                mostrarDialogoEliminar = false,
                mostrarDialogoDescartar = false
            )
        }
    }

    fun aplicarDominioEmail(dominio: String) {
        _uiState.update { currentState ->
            val actual = currentState.formEmail.trim()
            val usuarioParte = if (actual.contains("@")) {
                actual.substringBefore("@")
            } else {
                actual
            }
            val nuevoEmail = if (usuarioParte.isNotBlank()) {
                "$usuarioParte$dominio".lowercase()
            } else {
                dominio.lowercase()
            }
            val nextErrors = currentState.formErrores.toMutableMap().apply { remove("email") }
            currentState.copy(formEmail = nuevoEmail, formErrores = nextErrors)
        }
    }

    fun solicitarEnviarRestablecimiento() {
        val s = _uiState.value
        if (s.guardando) return
        val email = s.formEmail.trim().lowercase()
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(mensajeError = "Ingresa un correo electrónico válido para enviar el restablecimiento.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mensajeError = null) }
            try {
                repository.enviarRestablecimientoPassword(email)
                _uiState.update {
                    it.copy(
                        guardando = false,
                        mensajeExito = "Se ha enviado un correo a $email para restablecer la contraseña."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando restablecimiento de contraseña: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        guardando = false,
                        mensajeError = traducirError(e, "No se pudo enviar el correo de restablecimiento.")
                    )
                }
            }
        }
    }

    private fun traducirError(e: Exception, accionDefecto: String): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "No tienes permisos suficientes para realizar esta acción. Tus permisos fueron actualizados por la administración."
            msg.contains("UNAVAILABLE", ignoreCase = true) || msg.contains("offline", ignoreCase = true) ->
                "Sin conexión con el servidor. Verifica tu conexión a internet."
            msg.isNotBlank() -> msg
            else -> accionDefecto
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(mensajeExito = null, mensajeError = null) }
    }

    /** Reintenta cargar toda la pantalla (roles, sucursales, usuarios) si la
     *  carga inicial falló por red o clienteId no resuelto. Sin esto el usuario
     *  quedaría atrapado en un error sin salida (estancamiento). */
    fun reintentarCarga() {
        _uiState.update { it.copy(mensajeError = null, cargando = true) }
        iniciarObservadores()
    }

    private fun cargarFormularioDesdeUsuario(state: UsuariosUiState, usuario: UsuarioFarmacia): UsuariosUiState {
        // RAÍZ: si usuario ya tiene mapa explícito, es la verdad. Si está vacío (migrado),
        // NO inventar todo true — dejar vacío para heredar del rol (coherente con Sidebar).
        // El UI mostrará switches según efectivo (ver onPermisoModuloChanged).
        val permisosInit = if (usuario.permisosModulos.isNotEmpty()) {
            usuario.permisosModulos
        } else emptyMap()
        return state.copy(
            formNombre = usuario.nombre,
            formDni = usuario.dni,
            formTelefono = usuario.telefono,
            formEmail = usuario.email,
            formPassword = "",
            formRolId = usuario.rolId,
            formRolNombre = usuario.rolNombre,
            formSucursalId = usuario.sucursalId,
            formSucursalNombre = usuario.sucursalNombre,
            formAcceso = usuario.acceso,
            formErrores = emptyMap(),
            formPermisosModulos = permisosInit
        )
    }
}
