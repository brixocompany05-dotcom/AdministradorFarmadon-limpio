package com.app.administradorfarmadon.autenticacion.login.logica
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.administradorfarmadon.appconexioninternet.NetworkHealthMonitor
import com.app.administradorfarmadon.appconexioninternet.NetworkStatus
import com.app.administradorfarmadon.autenticacion.login.datos.ErrorVerificacionListaNegra
import com.app.administradorfarmadon.autenticacion.login.datos.ListaNegraRepository
import com.app.administradorfarmadon.autenticacion.login.datos.LoginErrorHandler
import com.app.administradorfarmadon.autenticacion.login.datos.LoginIncidenteAccion
import com.app.administradorfarmadon.autenticacion.login.datos.LoginIncidenteTipo
import com.app.administradorfarmadon.autenticacion.login.datos.LoginIncidenteUi
import com.app.administradorfarmadon.autenticacion.datos.AuthPaths
import com.app.administradorfarmadon.base_datos.FirestoreFieldUtils
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val firestore = FarmadonFirestore.db
    private var solicitudListener: ListenerRegistration? = null
    private var verificacionJob: Job? = null
    private var loginEnProceso = false
    private val TAG = "LoginViewModel"
    private var limpiado = false

    companion object {
        private const val LECTURA_UNICA_INTENTOS = 3
    }

    private fun actualizarEstado(transform: (LoginUiState) -> LoginUiState) {
        if (!limpiado) _uiState.update(transform)
    }

    private val authStateListener: FirebaseAuth.AuthStateListener by lazy {
        FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            Log.d(TAG, "[STARTUP] AuthStateListener disparado: user=${firebaseUser != null}")
            if (NetworkHealthMonitor.status.value == NetworkStatus.DESCONECTADO) {
                return@AuthStateListener
            }
            if (firebaseUser != null) {
                Log.d(TAG, "[STARTUP] Sesión activa en Firebase Auth: ${firebaseUser.email}")
                actualizarEstado {
                    it.copy(
                        sesionInicialResuelta = true,
                        resolucionSesionFallida = false,
                        errorResolucionSesion = null,
                        usuarioAutenticadoEmail = firebaseUser.email
                    )
                }
                return@AuthStateListener
            }
            actualizarEstado {
                it.copy(
                    sesionInicialResuelta = true,
                    resolucionSesionFallida = false,
                    errorResolucionSesion = null,
                    loginExitoso = false,
                    usuarioAutenticadoEmail = null
                )
            }
        }
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        viewModelScope.launch {
            NetworkHealthMonitor.status.collect { status ->
                actualizarEstado { it.copy(estadoRed = status) }
            }
        }
    }

    fun onUsuarioChange(nuevoUsuario: String) {
        actualizarEstado { it.copy(usuario = nuevoUsuario, incidente = null) }
    }

    fun onContrasenaChange(nuevaContrasena: String) {
        actualizarEstado { it.copy(contrasena = nuevaContrasena, incidente = null) }
    }

    fun descartarIncidente() {
        actualizarEstado { it.copy(incidente = null) }
    }

    fun descartarDialogo() {
        actualizarEstado { it.copy(mensajeDialogo = null) }
    }

    fun descartarEstadoSolicitud() {
        solicitudListener?.remove()
        solicitudListener = null
        verificacionJob?.cancel()
        verificacionJob = null
        actualizarEstado { it.copy(estadoSolicitud = null) }
    }

    fun clearLoginSuccess() {
        loginEnProceso = false
        actualizarEstado { it.copy(loginExitoso = false, loginExitosoReciente = false) }
    }

    fun volverALogin() {
        solicitudListener?.remove()
        solicitudListener = null
        verificacionJob?.cancel()
        verificacionJob = null
        loginEnProceso = false
        actualizarEstado {
            it.copy(
                estadoPantalla = LoginScreenState.LOGIN,
                cargando = false,
                loginExitoso = false,
                loginExitosoReciente = false,
                mensajeRestringido = null,
                contrasena = "",
                incidente = null,
                mensajeDialogo = null,
                estadoSolicitud = null
            )
        }
    }

    fun mostrarAccesoSuspendido(motivo: String = "Tu acceso fue suspendido por la administración de la farmacia.") {
        actualizarEstado {
            it.copy(
                cargando = false,
                loginExitoso = false,
                loginExitosoReciente = false,
                estadoPantalla = LoginScreenState.ACCESO_RESTRINGIDO,
                mensajeRestringido = motivo
            )
        }
    }

    fun iniciarSesion(
        usuarioInput: String = _uiState.value.usuario,
        contrasenaInput: String = _uiState.value.contrasena
    ) {
        val usuarioBuscado = usuarioInput.trim()
        if (usuarioBuscado.isBlank() || contrasenaInput.isBlank()) {
            actualizarEstado {
                it.copy(incidente = LoginErrorHandler.crearIncidente(LoginIncidenteTipo.CAMPOS_VACIOS))
            }
            return
        }
        if (_uiState.value.cargando) return

        actualizarEstado { it.copy(cargando = true, incidente = null) }
        viewModelScope.launch {
            try {
                // El acceso es SIEMPRE correo + contraseña. El nro. de documento
                // solo aplica en Consultar Expediente, jamás en el login.
                val emailToAuth = usuarioBuscado.lowercase()

                val auth = FirebaseAuth.getInstance()
                try {
                    auth.signInWithEmailAndPassword(emailToAuth, contrasenaInput).await()
                } catch (e: FirebaseAuthInvalidUserException) {
                    diagnosticarCredenciales()
                    return@launch
                } catch (e: FirebaseAuthInvalidCredentialsException) {
                    diagnosticarCredenciales()
                    return@launch
                } catch (e: Exception) {
                    actualizarEstado { it.copy(cargando = false) }
                    actualizarEstado {
                        it.copy(
                            incidente = LoginErrorHandler.crearIncidente(
                                LoginErrorHandler.mapearError(e),
                                errorOriginal = e
                            )
                        )
                    }
                    return@launch
                }

                val uid = auth.currentUser?.uid
                if (uid == null) {
                    actualizarEstado {
                        it.copy(
                            cargando = false,
                            incidente = LoginErrorHandler.crearIncidente(LoginIncidenteTipo.ERROR_DESCONOCIDO)
                        )
                    }
                    return@launch
                }

                // Verificación de Lista Negra
                val banEmailDoc = ListaNegraRepository.consultar(firestore, "", emailToAuth)
                if (banEmailDoc != null) {
                    auth.signOut()
                    actualizarEstado {
                        it.copy(
                            cargando = false,
                            incidente = LoginErrorHandler.crearIncidente(
                                LoginIncidenteTipo.USUARIO_BANEADO,
                                banEmailDoc.getString("motivo")
                            )
                        )
                    }
                    return@launch
                }

                // 1. Buscar en usuarios_farmacia (Colaboradores y Dueños activos)
                val userDoc = AuthPaths.usuariosFarmacia(firestore).document(uid).get().await()
                if (userDoc.exists()) {
                    when (intentarEntrarFarmacia(uid, userDoc, emailToAuth)) {
                        PuertaResultado.ENTRO,
                        PuertaResultado.ACCESO_REVOCADO,
                        PuertaResultado.FALLA_COMUNICADA -> return@launch
                        PuertaResultado.PERFIL_EN_PROCESO -> Unit // sigue al camino de solicitud abajo
                    }
                }

                // 2. Si no existiera en usuarios_farmacia, consultar solicitudes por uid
                val solQuery = AuthPaths.solicitudes(firestore)
                    .whereEqualTo("uid", uid)
                    .limit(1)
                    .get()
                    .await()
                val solDoc = solQuery.documents.firstOrNull()
                if (solDoc != null && solDoc.exists()) {
                    observarSolicitudEnTiempoReal(uid)
                    return@launch
                }

                // Fallback seguro: Si llegamos aquí sin haber resuelto el perfil, es un error de integridad o red.
                Log.w(TAG, "[LOGIN_OP] No se pudo determinar el perfil del usuario (uid=$uid)")
                FirebaseAuth.getInstance().signOut()
                actualizarEstado {
                    it.copy(
                        cargando = false,
                        incidente = LoginErrorHandler.crearIncidente(LoginIncidenteTipo.USUARIO_NO_REGISTRADO)
                    )
                }
                return@launch

            } catch (e: Exception) {
                Log.e(TAG, "[LOGIN_OP] Error en inicio de sesión: ${e.message}", e)
                
                // Fail-closed real: si el perfil falló, no entramos.
                FirebaseAuth.getInstance().signOut()
                actualizarEstado {
                    it.copy(
                        cargando = false,
                        incidente = LoginErrorHandler.crearIncidente(
                            tipo = LoginErrorHandler.mapearError(e),
                            errorOriginal = e
                        )
                    )
                }
                return@launch
            }
        }
    }

    /**
     * íšNICA PUERTA DE ENTRADA A UNA CUENTA ACTIVA (una regla, un solo lugar).
     * La usan tanto el ingreso con clave como la aprobación en vivo desde el
     * cartel de solicitud: verifica que la farmacia exista, que BRIXO no la
     * haya suspendido y que la sede asignada siga viva (reacomodo honesto a
     * Sede Principal si fue dada de baja) ANTES de guardar sesión y moneda.
     * Cada bloqueo pinta su pantalla verdadera aquí mismo; el llamador solo
     * deja de avanzar (R1/R3 —” nadie opera dentro de una farmacia cerrada).
     */
    private enum class PuertaResultado { ENTRO, ACCESO_REVOCADO, PERFIL_EN_PROCESO, FALLA_COMUNICADA }

    sealed class ResultadoVerificacionPostLogin {
        data class Ok(
            val clienteId: String,
            val sucursalId: String,
            val sucursalNombre: String,
            val rol: String,
            val nombre: String,
            val email: String
        ) : ResultadoVerificacionPostLogin()

        data object AccesoRevocado : ResultadoVerificacionPostLogin()
        data object PerfilEnProceso : ResultadoVerificacionPostLogin()
        data class FallaComunicada(val mensaje: String, val tipo: LoginIncidenteTipo) : ResultadoVerificacionPostLogin()
    }

    suspend fun verificarAccesoPostLogin(
        uid: String,
        userDoc: DocumentSnapshot,
        emailSesion: String
    ): ResultadoVerificacionPostLogin {
        val clienteId = userDoc.getString("clienteId") ?: ""

        if (clienteId.isBlank()) return ResultadoVerificacionPostLogin.PerfilEnProceso

        if (!FirestoreFieldUtils.parseBoolean(userDoc.get("acceso"))) {
            return ResultadoVerificacionPostLogin.AccesoRevocado
        }

        val farmaciaDoc = try {
            FarmadonPaths.farmacia(firestore, clienteId).get().await()
        } catch (e: Exception) {
            Log.e(TAG, "[LOGIN] Error de red al consultar farmacia (clienteId=$clienteId)", e)
            return ResultadoVerificacionPostLogin.FallaComunicada(
                "No pudimos conectar con los datos de tu farmacia. Verifica tu conexión a internet e intenta nuevamente.",
                LoginIncidenteTipo.ERROR_BASE_DATOS
            )
        }
        if (!farmaciaDoc.exists()) {
            Log.e(TAG, "[LOGIN] Documento de farmacia inexistente para clienteId=$clienteId")
            return ResultadoVerificacionPostLogin.FallaComunicada(
                "No se encontró el registro de la farmacia asociada a tu cuenta. Contacta a soporte de BRIXO.",
                LoginIncidenteTipo.ERROR_BASE_DATOS
            )
        }
        if ((farmaciaDoc.getString("estado") ?: "activo") == "suspendido") {
            return ResultadoVerificacionPostLogin.AccesoRevocado
        }

        suspend fun consultarSedeViva(sedeId: String): DocumentSnapshot? {
            val doc = FarmadonPaths.sucursales(firestore, clienteId).document(sedeId).get().await()
            return if (doc.exists() && doc.getBoolean("activa") != false) doc else null
        }

        val nombre = userDoc.getString("nombre") ?: emailSesion
        val rol = userDoc.getString("rol") ?: userDoc.getString("rolNombre") ?: "Colaborador"
        val sedeAsignada = userDoc.getString("sucursalId")?.trim().orEmpty()

        var sucursalIdFinal: String
        var sucursalNombreFinal: String

        when {
            sedeAsignada == "todas" -> {
                sucursalIdFinal = "todas"
                sucursalNombreFinal = userDoc.getString("sucursalNombre")?.ifBlank { "Todas las Sedes" } ?: "Todas las Sedes"
            }
            else -> {
                val sedeObjetivo = sedeAsignada.ifBlank { "principal" }
                var sedeDoc: DocumentSnapshot? = null
                var redFalloPuntual = false
                try {
                    sedeDoc = consultarSedeViva(sedeObjetivo)
                } catch (e: Exception) {
                    Log.e(TAG, "[LOGIN] No se pudo verificar la sede '$sedeObjetivo' por red", e)
                    redFalloPuntual = true
                }

                if (redFalloPuntual && !sedeAsignada.isBlank() && sedeAsignada != "principal") {
                    return ResultadoVerificacionPostLogin.FallaComunicada(
                        "No se pudo verificar el estado de tu sede. Reintenta o contacta a la administración si el problema persiste.",
                        LoginIncidenteTipo.ERROR_BASE_DATOS
                    )
                } else if (redFalloPuntual) {
                    return ResultadoVerificacionPostLogin.FallaComunicada(
                        "No pudimos verificar tu sede por un problema de conexión. Revisa tu internet e intenta de nuevo.",
                        LoginIncidenteTipo.SIN_INTERNET
                    )
                } else if (sedeDoc != null) {
                    sucursalIdFinal = sedeObjetivo
                    sucursalNombreFinal = userDoc.getString("sucursalNombre")?.takeIf { it.isNotBlank() }
                        ?: (sedeDoc.getString("nombre") ?: sedeObjetivo)
                } else if (!sedeAsignada.isBlank()) {
                    val ancla = try {
                        consultarSedeViva("principal")
                    } catch (e: Exception) {
                        Log.e(TAG, "[LOGIN] Reacomodo imposible: no se pudo verificar 'principal'", e)
                        null
                    }
                    if (ancla == null) {
                        Log.e(TAG, "[LOGIN] Sede $sedeObjetivo muerta y ancla principal indisponible (clienteId=$clienteId)")
                        return ResultadoVerificacionPostLogin.FallaComunicada(
                            "Tu sede fue cerrada y no pudimos ubicarte en la Sede Principal. Verifica tu conexión o contacta a la administración.",
                            LoginIncidenteTipo.ERROR_BASE_DATOS
                        )
                    }
                    Log.w(TAG, "[LOGIN] Sede $sedeObjetivo muerta → reacomodo verificado a principal")
                    sucursalIdFinal = "principal"
                    sucursalNombreFinal = ancla.getString("nombre") ?: "Sede Principal"
                } else {
                    Log.e(TAG, "[LOGIN] Sede ancla 'principal' inexistente para clienteId=$clienteId")
                    return ResultadoVerificacionPostLogin.FallaComunicada(
                        "Tu farmacia aún no termina de configurarse. Contacta a soporte de BRIXO.",
                        LoginIncidenteTipo.ERROR_BASE_DATOS
                    )
                }
            }
        }

        return ResultadoVerificacionPostLogin.Ok(
            clienteId = clienteId,
            sucursalId = sucursalIdFinal,
            sucursalNombre = sucursalNombreFinal,
            rol = rol,
            nombre = nombre,
            email = emailSesion
        )
    }
    private suspend fun intentarEntrarFarmacia(
        uid: String,
        userDoc: DocumentSnapshot,
        emailSesion: String
    ): PuertaResultado {
        when (val resultado = verificarAccesoPostLogin(uid, userDoc, emailSesion)) {
            is ResultadoVerificacionPostLogin.Ok -> {
                com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.guardarSesion(
                    context = getApplication(),
                    id = uid,
                    nombre = resultado.nombre,
                    rolUsuario = resultado.rol,
                    tenantId = resultado.clienteId,
                    sedeId = resultado.sucursalId,
                    sedeNombre = resultado.sucursalNombre
                )
                com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.email = resultado.email
                com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.dni = userDoc.getString("dni") ?: ""

                try {
                    val clienteDoc = AuthPaths.clientes(firestore).document(resultado.clienteId).get().await()
                    val codigo = clienteDoc.getString("monedaOperativa")?.trim()?.takeIf { it.isNotBlank() }
                    val simbolo = clienteDoc.getString("simboloMoneda")?.trim()?.takeIf { it.isNotBlank() }
                    if (codigo != null) com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaCodigo = codigo
                    if (simbolo != null) com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaSimbolo = simbolo
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "[LOGIN] No se pudo leer la moneda del cliente (${resultado.clienteId}). Se conserva '${com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaCodigo}' como moneda vigente.",
                        e
                    )
                }

                loginExitoso(uid, emailSesion)
                return PuertaResultado.ENTRO
            }
            is ResultadoVerificacionPostLogin.AccesoRevocado -> {
                FirebaseAuth.getInstance().signOut()
                actualizarEstado {
                    it.copy(
                        cargando = false,
                        estadoPantalla = LoginScreenState.ACCESO_RESTRINGIDO,
                        mensajeRestringido = if (userDoc.getBoolean("dadoDeBaja") == true)
                            "Tu ficha fue dada de baja por la administración de la farmacia."
                        else
                            "Tu acceso fue suspendido por la administración de la farmacia. Contacta a la administración para reactivarlo."
                    )
                }
                return PuertaResultado.ACCESO_REVOCADO
            }
            is ResultadoVerificacionPostLogin.PerfilEnProceso -> {
                return PuertaResultado.PERFIL_EN_PROCESO
            }
            is ResultadoVerificacionPostLogin.FallaComunicada -> {
                FirebaseAuth.getInstance().signOut()
                actualizarEstado {
                    it.copy(
                        cargando = false,
                        incidente = LoginErrorHandler.crearIncidente(resultado.tipo, customMensaje = resultado.mensaje)
                    )
                }
                return PuertaResultado.FALLA_COMUNICADA
            }
        }
    }

    private fun diagnosticarCredenciales() {
        actualizarEstado {
            it.copy(
                cargando = false,
                incidente = LoginErrorHandler.crearIncidente(
                    LoginIncidenteTipo.CREDENCIALES_INCORRECTAS,
                    customMensaje = "El correo o la contraseña son incorrectos. Verifica e intenta nuevamente."
                )
            )
        }
    }

    private fun observarSolicitudEnTiempoReal(uid: String) {
        solicitudListener?.remove()
        verificacionJob?.cancel()

        verificacionJob = viewModelScope.launch {
            var doc: DocumentSnapshot? = null
            var serverConsultadoExitosamente = false
            var intento = 0
            while (intento < LECTURA_UNICA_INTENTOS && doc == null && !serverConsultadoExitosamente) {
                try {
                    val snap = AuthPaths.solicitudes(firestore)
                        .whereEqualTo("uid", uid)
                        .limit(1)
                        .get(Source.SERVER)
                        .await()
                    serverConsultadoExitosamente = true
                    doc = snap.documents.firstOrNull()
                } catch (e: Exception) {
                    android.util.Log.w("LoginVM", "Lectura de solicitud falló por conectividad", e)
                }
                if (!serverConsultadoExitosamente && intento < LECTURA_UNICA_INTENTOS - 1) delay(1000L)
                intento++
            }

            if (!serverConsultadoExitosamente) {
                // Fallo de red: Decir la verdad técnica (Sin conexión), nunca "no registrado" (Regla R9)
                actualizarEstado {
                    it.copy(
                        cargando = false,
                        estadoSolicitud = null,
                        incidente = LoginErrorHandler.crearIncidente(
                            LoginIncidenteTipo.SIN_INTERNET,
                            customMensaje = "No pudimos conectar con la central para consultar el estado de tu solicitud. Verifica tu conexión a internet e intenta de nuevo."
                        )
                    )
                }
                return@launch
            }

            if (doc == null || !doc.exists()) {
                // El servidor respondió fehacientemente que la solicitud no existe
                actualizarEstado {
                    it.copy(
                        cargando = false,
                        estadoSolicitud = null,
                        incidente = LoginErrorHandler.crearIncidente(LoginIncidenteTipo.USUARIO_NO_REGISTRADO)
                    )
                }
                return@launch
            }

            procesarEstadoSolicitud(doc, uid)
        }

        solicitudListener = AuthPaths.solicitudes(firestore)
            .whereEqualTo("uid", uid)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val doc = snapshot.documents.firstOrNull() ?: return@addSnapshotListener
                procesarEstadoSolicitud(doc, uid)
            }
    }

    private fun procesarEstadoSolicitud(
        doc: DocumentSnapshot?,
        uid: String
    ) {
        if (doc == null || !doc.exists()) {
            if (loginEnProceso) return
            loginEnProceso = true
            resolverSesionUsuario(uid, FirebaseAuth.getInstance().currentUser?.email ?: "Usuario")
            return
        }
        val rucDocId = doc.getString("ruc") ?: doc.id
        val estado = doc.getString("estado") ?: "pendiente"
        if (estado == "aprobada") {
            if (loginEnProceso) return
            loginEnProceso = true
            actualizarEstado {
                it.copy(
                    cargando = false,
                    estadoSolicitud = SolicitudEstadoUi(
                        estado = "aprobada",
                        mensajeBrixo = "¡Tu cuenta fue aprobada! Ingresando...",
                        uid = rucDocId
                    )
                )
            }
            resolverSesionUsuario(uid, doc.getString("email") ?: "Usuario")
        } else {
            actualizarEstado {
                it.copy(
                    cargando = false,
                    estadoSolicitud = SolicitudEstadoUi(
                        estado = estado,
                        mensajeBrixo = doc.getString("mensajeBrixo") ?: doc.getString("motivoRechazo") ?: doc.getString("motivoObservacion") ?: "",
                        uid = rucDocId,
                        correccionSolicitada = doc.getBoolean("correccionSolicitada") ?: false,
                        fechaCorreccion = doc.getString("fechaCorreccion") ?: ""
                    )
                )
            }
        }
    }

    private fun resolverSesionUsuario(
        uid: String,
        email: String,
        reintentos: Int = 0
    ) {
        viewModelScope.launch {
            try {
                val userDoc = AuthPaths.usuariosFarmacia(firestore).document(uid).get().await()
                if (!userDoc.exists()) {
                    Log.w(TAG, "[LOGIN] Perfil aún no sincronizado en usuarios_farmacia para uid=$uid")
                    loginEnProceso = false
                    FirebaseAuth.getInstance().signOut()
                    actualizarEstado {
                        it.copy(
                            cargando = false,
                            estadoSolicitud = null,
                            incidente = LoginErrorHandler.crearIncidente(
                                LoginIncidenteTipo.ERROR_BASE_DATOS,
                                customMensaje = "No se encontraron los datos de tu perfil en el servidor. Por favor intenta ingresar nuevamente."
                            )
                        )
                    }
                    return@launch
                }

                // MISMA PUERTA QUE EL INGRESO CON CLAVE (una regla, un solo lugar):
                // aunque BRIXO acabe de aprobar, se verifica farmacia existente,
                // no suspendida y sede viva antes de entrar. La aprobación es una
                // transacción atómica en la central, así que en el camino feliz
                // todo pasa limpio; los bloqueos solo aparecen si algo real cambió.
                when (intentarEntrarFarmacia(uid, userDoc, email)) {
                    PuertaResultado.ENTRO -> Unit
                    PuertaResultado.ACCESO_REVOCADO -> {
                        // La puerta pintó la pantalla de ACCESO_RESTRINGIDO; aquí
                        // solo se limpia el cartel en vivo para no dejarlo encima.
                        actualizarEstado { it.copy(estadoSolicitud = null) }
                    }
                    PuertaResultado.FALLA_COMUNICADA -> {
                        // Bloqueo real (farmacia suspendida/inexistente) o error de
                        // red ya comunicado por la puerta. El usuario NO pierde su
                        // lugar: vuelve al cartel de solicitud para reintentar.
                        loginEnProceso = false
                        FirebaseAuth.getInstance().signOut()
                        actualizarEstado { it.copy(estadoSolicitud = null) }
                    }
                    PuertaResultado.PERFIL_EN_PROCESO -> {
                        // La activación del alta todavía no se refleja en el perfil:
                        // nada de fingir entrada ni mentir. Se reintenta en breve,
                        // con techo honesto: si tras varios intentos el perfil sigue
                        // sin sincronizar, se dice la verdad y no hay carga infinita.
                        if (reintentos >= 5) {
                            Log.e(TAG, "[LOGIN] Perfil nunca sincronizó tras aprobación (uid=$uid)")
                            loginEnProceso = false
                            FirebaseAuth.getInstance().signOut()
                            actualizarEstado {
                                it.copy(
                                    cargando = false,
                                    estadoSolicitud = null,
                                    incidente = LoginErrorHandler.crearIncidente(
                                        LoginIncidenteTipo.ERROR_BASE_DATOS,
                                        customMensaje = "Tu cuenta fue aprobada pero sus datos aún no terminan de sincronizar. Espera unos segundos e ingresa con tu correo y contraseña."
                                    )
                                )
                            }
                        } else {
                            Log.w(TAG, "[LOGIN] Perfil activado aún sin clienteId sincronizado (uid=$uid); reintentando")
                            delay(1500)
                            resolverSesionUsuario(uid, email, reintentos + 1)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al consultar perfil de usuario: ${e.message}", e)
                loginEnProceso = false
                FirebaseAuth.getInstance().signOut()
                val tipo = when (e) {
                    is com.google.firebase.FirebaseNetworkException -> LoginIncidenteTipo.SIN_INTERNET
                    is com.google.firebase.firestore.FirebaseFirestoreException -> {
                        if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE) LoginIncidenteTipo.SIN_INTERNET
                        else LoginIncidenteTipo.ERROR_BASE_DATOS
                    }
                    else -> LoginIncidenteTipo.ERROR_BASE_DATOS
                }
                actualizarEstado {
                    it.copy(
                        cargando = false,
                        estadoSolicitud = null,
                        incidente = LoginErrorHandler.crearIncidente(
                            tipo,
                            customMensaje = if (tipo == LoginIncidenteTipo.SIN_INTERNET)
                                "No se pudo conectar con el servidor para cargar tus datos de acceso. Revisa tu conexión a internet e inténtalo de nuevo."
                            else
                                "Ocurrió un error al consultar tus datos de acceso. Por favor intenta nuevamente."
                        )
                    )
                }
            }
        }
    }

    fun recuperarContrasena(emailInput: String = _uiState.value.usuario) {
        val email = emailInput.trim().lowercase()
        if (email.isBlank() || !email.contains("@")) {
            actualizarEstado {
                it.copy(
                    mensajeDialogo = "Recuperar Contraseña" to "Ingresa tu correo electrónico en el campo superior para enviarte el enlace de restablecimiento."
                )
            }
            return
        }
        actualizarEstado { it.copy(cargando = true) }
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
                actualizarEstado {
                    it.copy(
                        cargando = false,
                        mensajeDialogo = "Correo Enviado" to "Hemos enviado un enlace a $email para que puedas restablecer tu contraseña."
                    )
                }
            } catch (e: Exception) {
                actualizarEstado {
                    it.copy(
                        cargando = false,
                        mensajeDialogo = "Error" to (e.localizedMessage ?: "No se pudo enviar el correo de recuperación. Verifica e intenta de nuevo.")
                    )
                }
            }
        }
    }

    private fun loginExitoso(uid: String, email: String) {
        actualizarEstado {
            it.copy(
                cargando = false,
                sesionInicialResuelta = true,
                loginExitosoReciente = true,
                loginExitoso = true,
                usuarioAutenticadoEmail = email
            )
        }
    }

    fun ejecutarAccionIncidente(
        incidente: LoginIncidenteUi?,
        onIrARegistro: () -> Unit,
        onContactarSoporte: (LoginIncidenteUi) -> Unit,
        onCorregirSolicitud: ((String) -> Unit)? = null
    ) {
        when (incidente?.accionPrincipal) {
            LoginIncidenteAccion.Reintentar -> {
                descartarIncidente()
                iniciarSesion()
            }
            LoginIncidenteAccion.IrARegistro -> onIrARegistro()
            LoginIncidenteAccion.CorregirSolicitud -> {
                // El id de la solicitud (doc RUC) es la llave que el corrector
                // acepta de forma nativa; el auth uid queda como respaldo.
                val uid = _uiState.value.estadoSolicitud?.uid?.takeIf { it.isNotBlank() }
                    ?: FirebaseAuth.getInstance().currentUser?.uid
                    ?: ""
                if (uid.isNotBlank() && onCorregirSolicitud != null) {
                    onCorregirSolicitud(uid)
                } else {
                    onIrARegistro()
                }
            }
            LoginIncidenteAccion.ContactarSoporte -> incidente.let(onContactarSoporte)
            LoginIncidenteAccion.LimpiarCampos -> {
                actualizarEstado { it.copy(usuario = "", contrasena = "") }
            }
            else -> descartarIncidente()
        }
    }

    override fun onCleared() {
        super.onCleared()
        limpiado = true
        solicitudListener?.remove()
        verificacionJob?.cancel()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }
}
