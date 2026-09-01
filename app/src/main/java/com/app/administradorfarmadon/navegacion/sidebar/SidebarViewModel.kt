package com.app.administradorfarmadon.navegacion.sidebar
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import androidx.lifecycle.ViewModel
import com.app.administradorfarmadon.compartido.datos.EcosistemaPaths
import com.app.administradorfarmadon.modulos.domain.CatalogoHerramienta
import com.app.administradorfarmadon.modulos.domain.CatalogoHijo
import com.app.administradorfarmadon.modulos.domain.ModulosResueltos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.app.administradorfarmadon.base_datos.FirestoreFieldUtils
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Sidebar dinámico en tiempo real = f(plan + rol + catálogo + overrides del cliente).
 *
 * Resiliente:
 * 1. Usa rutas jerárquicas seguras para colecciones compartidas.
 * 2. Si rolId es vacío, el usuario es dueño/admin y ve todo lo contratado.
 * 3. Lee planId tanto del cliente como de su suscripción.
 * 4. Incluye siempre la herramienta fija de Configuración.
 */
class SidebarViewModel : ViewModel() {

    private val db = FarmadonFirestore.db
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "SidebarViewModel"

    private val _items = MutableStateFlow<List<SidebarItemData>>(emptyList())
    val items: StateFlow<List<SidebarItemData>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Datos descriptivos en tiempo real del tenant
    private val _nombreFarmacia = MutableStateFlow("Mi Farmacia")
    val nombreFarmacia: StateFlow<String> = _nombreFarmacia.asStateFlow()

    private val _sucursalNombre = MutableStateFlow("Sede Principal")
    val sucursalNombre: StateFlow<String> = _sucursalNombre.asStateFlow()

    private val _planNombre = MutableStateFlow("Estándar")
    val planNombre: StateFlow<String> = _planNombre.asStateFlow()

    private val _usuarioNombre = MutableStateFlow("Personal")
    val usuarioNombre: StateFlow<String> = _usuarioNombre.asStateFlow()

    private val _rolNombre = MutableStateFlow("Administrador")
    val rolNombre: StateFlow<String> = _rolNombre.asStateFlow()

    // Sesión efectiva en tiempo real (R8): la sede/rol del usuario en turno se
    // reflejan en vivo para que la operación (caja, ventas) use el dato nuevo
    // sin obligar a reiniciar sesión.
    private val _sucursalIdEfectiva = MutableStateFlow("principal")
    val sucursalIdEfectiva: StateFlow<String> = _sucursalIdEfectiva.asStateFlow()

    private val _sucursalesDisponibles = MutableStateFlow<List<com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal>>(emptyList())
    val sucursalesDisponibles: StateFlow<List<com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal>> = _sucursalesDisponibles.asStateFlow()

    private val _esItinerante = MutableStateFlow(false)
    val esItinerante: StateFlow<Boolean> = _esItinerante.asStateFlow()

    private val _notificacionFlotante = MutableStateFlow<String?>(null)
    val notificacionFlotante: StateFlow<String?> = _notificacionFlotante.asStateFlow()

    private val _errorCarga = MutableStateFlow<String?>(null)
    val errorCarga: StateFlow<String?> = _errorCarga.asStateFlow()

    private var timeoutJob: Job? = null

    fun descartarNotificacionFlotante() {
        _notificacionFlotante.value = null
    }

    fun cambiarSucursalActiva(sucursalId: String, sucursalNombre: String) {
        _sucursalIdEfectiva.value = sucursalId
        _sucursalNombre.value = sucursalNombre
        com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalId = sucursalId
        com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalNombre = sucursalNombre
        _notificacionFlotante.value = "Operando ahora en: $sucursalNombre"
    }

    private val _rolIdEfectivo = MutableStateFlow("")
    val rolIdEfectivo: StateFlow<String> = _rolIdEfectivo.asStateFlow()

    // Bloqueos en tiempo real
    private val _vencida = MutableStateFlow(false)
    val vencida: StateFlow<Boolean> = _vencida.asStateFlow()

    // íšLTIMA VERDAD VIVA de la puerta de vigencia —” la reevalúa el latido
    // periódico para que el gate maestro TICTAQUE: una sesión abierta cruzando
    // la medianoche ahora se bloquea sola (antes operaba indefinidamente sin
    // eventos del snapshot).
    @Volatile private var ultimaFechaFinVigencia: Any? = null
    @Volatile private var ultimaEtiquetaEstado: String = ""

    private val _suspendida = MutableStateFlow(false)
    val suspendida: StateFlow<Boolean> = _suspendida.asStateFlow()

    private val _estadoBloqueo = MutableStateFlow("ACTIVO") // ACTIVO, VENCIDO, SUSPENDIDO, PAUSADO
    val estadoBloqueo: StateFlow<String> = _estadoBloqueo.asStateFlow()

    private val _motivoBloqueo = MutableStateFlow("")
    val motivoBloqueo: StateFlow<String> = _motivoBloqueo.asStateFlow()

    private val _modalidadPausa = MutableStateFlow("")
    val modalidadPausa: StateFlow<String> = _modalidadPausa.asStateFlow()

    private val _pausadoHastaTexto = MutableStateFlow("")
    val pausadoHastaTexto: StateFlow<String> = _pausadoHastaTexto.asStateFlow()

    private val _pausadoDesdeTexto = MutableStateFlow("")
    val pausadoDesdeTexto: StateFlow<String> = _pausadoDesdeTexto.asStateFlow()

    private val _diasRestantesPausa = MutableStateFlow(0L)
    val diasRestantesPausa: StateFlow<Long> = _diasRestantesPausa.asStateFlow()

    private val listeners = mutableListOf<ListenerRegistration>()
    private var rolListenerRegistration: ListenerRegistration? = null

    private var catalogo: List<CatalogoHerramienta> = emptyList()
    private var featuresPlan: Set<String> = emptySet()
    private var overridesCliente: Map<String, Boolean> = emptyMap()
    private var permisosRol: Map<String, Any> = emptyMap()
    private var permisosUsuario: Map<String, Boolean> = emptyMap()
    private var sucursalIdInicialCargada: Boolean = false

    // Bloqueo de sesión del usuario en tiempo real (R1/R3): si se borra al
    // colaborador o se le corta el acceso mientras está dentro, se revoca la
    // sesión de inmediato (no se le permite seguir operando).
    private val _sesionRevocada = MutableStateFlow<String?>(null)
    val sesionRevocada: StateFlow<String?> = _sesionRevocada.asStateFlow()

    fun confirmarSesionRevocada() {
        _sesionRevocada.value = null
    }

    fun recargarSesion() {
        latidoJob?.cancel()
        latidoJob = null
        timeoutJob?.cancel()
        timeoutJob = null
        listeners.forEach { it.remove() }
        listeners.clear()
        detenerEscuchaRol()
        permisosRol = emptyMap()
        permisosUsuario = emptyMap()
        featuresPlan = emptySet()
        overridesCliente = emptyMap()
        catalogo = emptyList()
        sucursalIdInicialCargada = false
        _usuarioNombre.value = "Personal"
        _rolNombre.value = "Administrador"
        _rolIdEfectivo.value = ""
        _vencida.value = false
        _suspendida.value = false
        _sesionRevocada.value = null
        _notificacionFlotante.value = null
        _errorCarga.value = null
        _esItinerante.value = false
        _sucursalesDisponibles.value = emptyList()
        iniciarCableadoDinamico()
    }

    fun iniciarCableadoDinamico() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        _errorCarga.value = null
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(15_000L)
            if (_isLoading.value) {
                _isLoading.value = false
                _errorCarga.value = "No se pudo cargar la sesión. Verifica tu conexión e intenta de nuevo."
            }
        }

        // 1. Escuchar Catálogo de Herramientas Global (siempre activo)
        escucharHerramientas()

        // 2. Escuchar Datos del Usuario en Turno
        val userRef = SidebarPaths.usuario(db, uid)
        val userListener = userRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando usuario: ${error.message}")
                _isLoading.value = false
                _errorCarga.value = "Error de conexión. Reintenta."
                timeoutJob?.cancel()
                return@addSnapshotListener
            }

            // Coordinación en tiempo real (vida real): el usuario fue eliminado
            // o se le revocó el acceso → su sesión activa debe terminar ya.
            val existe = snapshot != null && snapshot.exists()
            val acceso = if (existe) FirestoreFieldUtils.parseBoolean(snapshot?.get("acceso")) else false
            if (!existe || !acceso) {
                val motivo = if (!existe) "Tu cuenta fue dada de baja." else "Tu acceso fue suspendido."
                _sesionRevocada.value = motivo
                return@addSnapshotListener
            }

            val nombreUser = snapshot?.getString("nombre")
                ?: snapshot?.getString("email")
                ?: auth.currentUser?.email
                ?: "Personal"
            val rawSucursalId = snapshot?.getString("sucursalId") ?: ""
            val rawSucursalNombre = snapshot?.getString("sucursalNombre") ?: "Sede Principal"

            val rolId = snapshot?.getString("rolId") ?: ""
            val rolNombreDirecto = snapshot?.getString("rol") ?: snapshot?.getString("rolNombre") ?: if (rolId.isBlank()) "Dueño" else "Colaborador"

            val esItin = rawSucursalId == "todas" || rawSucursalId.isBlank() || rolId.isBlank()
            _esItinerante.value = esItin

            val previousSucursalId = _sucursalIdEfectiva.value

            if (!sucursalIdInicialCargada) {
                // Carga inicial determinista: Se sincroniza la sesión silenciosamente sin alertas
                sucursalIdInicialCargada = true
            } else {
                // Transición real de evento en vivo mientras el usuario está conectado
                if (previousSucursalId.isNotBlank() && previousSucursalId != rawSucursalId && !esItin) {
                    _notificacionFlotante.value = "Sede actualizada: Ahora estás operando en \"$rawSucursalNombre\""
                } else if (rawSucursalId == "todas" && previousSucursalId != "todas") {
                    _notificacionFlotante.value = "Acceso itinerante habilitado: Ahora puedes elegir sede desde el menú"
                }
            }

            if (!esItin) {
                _sucursalNombre.value = rawSucursalNombre
                _sucursalIdEfectiva.value = rawSucursalId.ifBlank { "principal" }
                com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalId = rawSucursalId.ifBlank { "principal" }
                com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalNombre = rawSucursalNombre
            }

            _rolNombre.value = rolNombreDirecto
            _rolIdEfectivo.value = if (rolId.isNotBlank()) rolId else rolNombreDirecto

            // Permisos por usuario (selector bonito): si el colaborador tiene mapa personalizado,
            // esa es la verdad final por encima del rol. Si está vacío, hereda del rol.
            @Suppress("UNCHECKED_CAST")
            permisosUsuario = (snapshot?.get("permisosModulos") as? Map<String, Boolean>) ?: emptyMap()

            if (rolId.isNotBlank()) {
                escucharRol(rolId)
            } else {
                // Es Dueño / Administrador Principal -> Cero restricciones de rol (ve todos los módulos del plan contratado)
                // Pero si dueño tiene permisosUsuario (raro), también respetar
                detenerEscuchaRol()
                permisosRol = emptyMap()
                actualizarSidebar()
            }
            // Si es colaborador, el sidebar se actualizará cuando llegue el rol; si es dueño, ya actualizamos.
            // Forzar actualización por si permisosUsuario cambió aunque rol sea mismo
            if (permisosUsuario.isNotEmpty() || snapshot?.contains("permisosModulos") == true) {
                actualizarSidebar()
            }

            // Determinar clienteId (aislamiento estricto por farmacia - R1)
            val clienteId = snapshot?.getString("clienteId")
                ?: snapshot?.getString("farmaciaId")
                ?: (snapshot?.get("clienteIds") as? List<*>)?.firstOrNull()?.toString()
                ?: ""

            if (clienteId.isBlank()) {
                _isLoading.value = false
                return@addSnapshotListener
            }

            clienteIdActivo = clienteId
            iniciarLoopLatidoPeriodico(clienteId)

            escucharCliente(clienteId)
            escucharSuscripcion(clienteId)
            escucharSucursales(clienteId)
        }
        listeners.add(userListener)
    }

    private var clienteIdActivo: String = ""
    private var ultimoLatidoEmitidoMs: Long = 0L
    private var latidoJob: Job? = null

    private fun iniciarLoopLatidoPeriodico(clienteId: String) {
        latidoJob?.cancel()
        latidoJob = viewModelScope.launch {
            // SINCRONIZACIÓN DE HORA DEL GATE (C4 sellado): mide el offset contra
            // Firestore al arrancar y lo refresca cada 30 latidos (~1 hora).
            // Sin esto, `HoraServidor.ahoraMs()` es reloj local disfrazado.
            var latidosDesdeSincronizacion = 0
            sincronizarOffsetDelGate()
            while (isActive) {
                emitirLatidoActividad(clienteId)
                reevaluarPuertaVigencia()
                delay(120_000L) // Latido periódico cada 2 minutos en segundo plano
                latidosDesdeSincronizacion++
                if (latidosDesdeSincronizacion >= 30) {
                    latidosDesdeSincronizacion = 0
                    sincronizarOffsetDelGate()
                }
            }
        }
    }

    /**
     * Mide la diferencia entre este dispositivo y el reloj de Firestore
     * (mismo patrón del módulo de inventario) e instala el offset en
     * [com.app.administradorfarmadon.compartido.logica.HoraServidor].
     * Falla en silencio logueado —” degradación honesta a reloj local.
     */
    private suspend fun sincronizarOffsetDelGate() {
        repeat(2) { intento ->
            try {
                val docRef = com.app.administradorfarmadon.compartido.datos.FarmadonFirestore.db
                    .collection("_health").document("ping")
                    .collection("hora_gate").document()
                val antes = System.currentTimeMillis()
                com.google.android.gms.tasks.Tasks.await(
                    docRef.set(mapOf("ts" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                )
                val srv = docRef.get().await().getTimestamp("ts")?.toDate()?.time
                if (srv != null) {
                    com.app.administradorfarmadon.compartido.logica.HoraServidor.establecerOffset(
                        srv - System.currentTimeMillis()
                    )
                    android.util.Log.i(TAG, "Gate: offset servidor instalado (intento ${intento + 1})")
                    return
                }
                android.util.Log.w(TAG, "Sync de hora: servidor no devolvió ts (intento ${intento + 1})")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Sync de hora falló (intento ${intento + 1}): ${e.message}")
            }
        }
    }

    /**
     * REEVALUACIÓN DE LA PUERTA MAESTRA —” misma fórmula doble cerradura del
     * listener, ejecutada cada latido: el cruce de medianoche con sesión abierta
     * ya no opera indefinidamente. Sin escuchas nuevas, cero costo extra.
     */
    private fun reevaluarPuertaVigencia() {
        val hayContrato = ultimaFechaFinVigencia != null || ultimaEtiquetaEstado.isNotBlank()
        if (!hayContrato) return
        val bloqueada = esSuscripcionVencida(ultimaFechaFinVigencia) ||
            ultimaEtiquetaEstado == "vencida"
        if (_vencida.value != bloqueada) {
            Log.i(TAG, "Gate maestro tictaquea: bloqueada=$bloqueada")
            _vencida.value = bloqueada
        }
    }

    fun emitirLatidoActividad(clienteId: String = clienteIdActivo) {
        if (clienteId.isBlank()) return
        val ahora = System.currentTimeMillis()
        // Throttling: Emitir latido como máximo una vez cada 2 minutos para no saturar Firestore
        if (ahora - ultimoLatidoEmitidoMs < 120_000L) return
        ultimoLatidoEmitidoMs = ahora

        val uid = auth.currentUser?.uid ?: ""
        // HORA SERVIDOR: la actividad que BrixoPanel lee es negocio, no decoración —”
        // jamás se ancla al reloj del dispositivo.
        val now = com.google.firebase.Timestamp(
            Date(com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs())
        )
        val sucIdEfectiva = _sucursalIdEfectiva.value.ifBlank { "principal" }
        // ACTIVIDAD POR SEDE: mapa en el doc farmacia —” permite a BrixoPanel saber
        // CUÁL sucursal concreta está operando (incluida Principal), con merge
        // seguro multi-dispositivo (cada tablet pisa solo su propia llave).
        val updates = mapOf(
            "ultimaActividad" to now,
            "actividadPorSede.$sucIdEfectiva" to now
        )

        // 1. Latido general de la Farmacia (solo sobre farmacias/{clienteId})
        SidebarPaths.farmacia(db, clienteId).update(updates)

        // 2. Latido individual del Colaborador en turno (Juan o María)
        if (uid.isNotBlank()) {
            SidebarPaths.usuario(db, uid).update(
                mapOf(
                    "ultimoAcceso" to now,
                    "estado" to "EN_TURNO"
                )
            )
        }

        // 3. Latido de la Sede/Sucursal activa en este dispositivo
        // (compat con el detalle del panel que lee la subcolección —” la llave
        // granular del mapa farm-level de arriba es la fuente nueva).
        val sucId = _sucursalIdEfectiva.value
        if (sucId.isNotBlank() && sucId != "todas" && sucId != "principal") {
            com.app.administradorfarmadon.compartido.datos.FarmadonPaths.sucursales(db, clienteId)
                .document(sucId)
                .update(mapOf("ultimaActividad" to now))
        }
    }

    fun registrarCierreSesionUsuario() {
        val uid = auth.currentUser?.uid ?: return
        SidebarPaths.usuario(db, uid).update(
            mapOf(
                "estado" to "DESCONECTADO",
                "ultimoCierreSesion" to com.google.firebase.Timestamp.now()
            )
        )
    }

    private fun escucharSucursales(clienteId: String) {
        if (clienteId.isBlank()) return
        val sucursalesRef = com.app.administradorfarmadon.compartido.datos.FarmadonPaths.sucursales(db, clienteId)
        val l = sucursalesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando sucursales para sidebar: ${error.message}")
                _isLoading.value = false
                _errorCarga.value = "Error de conexión. Reintenta."
                timeoutJob?.cancel()
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                try {
                    com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "Sede",
                        direccion = doc.getString("direccion") ?: "",
                        telefono = doc.getString("telefono") ?: "",
                        esPrincipal = doc.getBoolean("esPrincipal") ?: false,
                        activa = doc.getBoolean("activa") ?: true,
                        codigoInterno = doc.getString("codigoInterno") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
            _sucursalesDisponibles.value = list

            val sucursalIdEfectiva = _sucursalIdEfectiva.value
            if (sucursalIdEfectiva.isNotBlank() && sucursalIdEfectiva != "todas" && list.none { it.id == sucursalIdEfectiva }) {
                val principal = list.firstOrNull { it.esPrincipal }
                if (principal != null) {
                    _sucursalIdEfectiva.value = principal.id
                    _sucursalNombre.value = principal.nombre
                    com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalId = principal.id
                    com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalNombre = principal.nombre
                }
            }
        }
        listeners.add(l)
    }

    private fun escucharHerramientas() {
        val herramientasRef = EcosistemaPaths.herramientasPlan(db)
            .whereEqualTo("estado", "activo")

        val toolsListener = herramientasRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando herramientas_plan: ${error.message}")
                _isLoading.value = false
                _errorCarga.value = "Error de conexión. Reintenta."
                timeoutJob?.cancel()
                return@addSnapshotListener
            }

            catalogo = snapshot?.documents?.mapNotNull { doc ->
                val nombre = doc.getString("nombre") ?: ""
                if (nombre.isEmpty()) return@mapNotNull null

                @Suppress("UNCHECKED_CAST")
                val hijos = (doc.get("hijos") as? List<Map<String, Any?>>)?.mapNotNull { h ->
                    CatalogoHijo(
                        clave = h["clave"] as? String ?: "",
                        nombre = h["nombre"] as? String ?: "",
                        activo = (h["activo"] as? Boolean) ?: true
                    )
                } ?: emptyList()

                CatalogoHerramienta(
                    id = doc.id,
                    modulo = doc.getString("modulo") ?: doc.id,
                    nombre = nombre,
                    categoria = doc.getString("categoria") ?: "OTROS",
                    orden = doc.getDouble("orden")?.toInt() ?: 0,
                    icono = doc.getString("icono") ?: "",
                    estado = doc.getString("estado") ?: "activo",
                    hijos = hijos
                )
            } ?: emptyList()

            Log.d("FARMADON_TOOLS", "Catalogo cargado: ${catalogo.map { "${it.nombre} -> ${it.modulo}" }}")
            actualizarSidebar()
        }
        listeners.add(toolsListener)
    }

    private fun escucharCliente(clienteId: String) {
        if (clienteId.isBlank()) return

        val farmaciaRef = SidebarPaths.farmacia(db, clienteId)
        val listener = farmaciaRef.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando farmacia: ${error.message}")
                _isLoading.value = false
                _errorCarga.value = "Error de conexión. Reintenta."
                timeoutJob?.cancel()
                return@addSnapshotListener
            }

            if (snap == null || !snap.exists()) {
                return@addSnapshotListener
            }

            aplicarFarmacia(snap)
        }
        listeners.add(listener)
    }

    private fun aplicarFarmacia(snap: com.google.firebase.firestore.DocumentSnapshot) {
        val farmaciaName = snap.getString("nombreFarmacia")
            ?: snap.getString("nombre")
            ?: snap.getString("razonSocial")
            ?: "Mi Farmacia"
        _nombreFarmacia.value = farmaciaName

        val planNombreDoc = snap.getString("plan")
            ?: snap.getString("planNombre")
        if (!planNombreDoc.isNullOrBlank()) {
            _planNombre.value = planNombreDoc
        }

        // Moneda viva por cliente (B4 —” per-client). Actualiza SessionManager sin relogin.
        try {
            val codigoVivo = snap.getString("monedaOperativa")?.takeIf { it.isNotBlank() } ?: snap.getString("monedaCodigo") ?: "PEN"
            val simboloVivo = snap.getString("simboloMoneda")?.takeIf { it.isNotBlank() } ?: when (codigoVivo) {
                "USD" -> "$"; "EUR" -> "──"; "COP" -> "$"; "CLP" -> "$"; "ARS" -> "$"; "VES" -> "Bs."; else -> "S/"
            }
            if (codigoVivo != com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaCodigo ||
                simboloVivo != com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaSimbolo) {
                com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaCodigo = codigoVivo
                com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.monedaSimbolo = simboloVivo
            }
        } catch (_: Exception) {}

        @Suppress("UNCHECKED_CAST")
        overridesCliente = (snap.get("featureOverrides") as? Map<String, Boolean>) ?: emptyMap()
        val estadoSnap = snap.getString("estado")?.lowercase() ?: ""
        val motivoSnap = snap.getString("motivoSuspension") ?: snap.getString("motivoPausa") ?: snap.getString("motivo") ?: ""
        _motivoBloqueo.value = motivoSnap

        _modalidadPausa.value = snap.getString("modalidadPausa") ?: ""
        val desdeObj = snap.get("pausadoEn") ?: snap.get("pausadoDesde")
        _pausadoDesdeTexto.value = formatearFechaLegible(desdeObj)
        val hastaObj = snap.get("pausadoHasta")
        _pausadoHastaTexto.value = formatearFechaLegible(hastaObj)

        var estaPausada = estadoSnap == "pausado" || estadoSnap == "pausada"
        val estaSuspendida = estadoSnap == "suspendido" || estadoSnap == "suspendida"

        // Si la pausa fue programada con fecha límite y el tiempo actual ya superó pausadoHasta,
        // la pausa comercial culminó según el calendario y la farmacia puede volver a operar.
        if (estaPausada && _modalidadPausa.value == "PROGRAMADA" && hastaObj != null) {
            val hastaMs = when (hastaObj) {
                is com.google.firebase.Timestamp -> hastaObj.toDate().time
                is java.util.Date -> hastaObj.time
                is String -> parseIsoUtc(hastaObj) ?: 0L
                else -> 0L
            }
            val ahora = System.currentTimeMillis()
            if (hastaMs > 0 && ahora >= hastaMs) {
                estaPausada = false
                _diasRestantesPausa.value = 0L
            } else if (hastaMs > ahora) {
                val diffDias = ((hastaMs - ahora) / (1000L * 60 * 60 * 24)).coerceAtLeast(1L)
                _diasRestantesPausa.value = diffDias
            }
        } else {
            _diasRestantesPausa.value = 0L
        }

        _suspendida.value = estaPausada || estaSuspendida

        if (estaPausada) {
            _estadoBloqueo.value = "PAUSADO"
            _items.value = emptyList()
            _isLoading.value = false
        } else if (estaSuspendida) {
            _estadoBloqueo.value = "SUSPENDIDO"
            _items.value = emptyList()
            _isLoading.value = false
        } else if (_vencida.value) {
            _estadoBloqueo.value = "VENCIDO"
            _items.value = emptyList()
            _isLoading.value = false
        } else {
            _estadoBloqueo.value = "ACTIVO"
            actualizarSidebar()
        }
    }

    private fun formatearFechaLegible(fecha: Any?): String {
        if (fecha == null) return ""
        val date = when (fecha) {
            is com.google.firebase.Timestamp -> fecha.toDate()
            is java.util.Date -> fecha
            is String -> {
                if (fecha.isBlank()) return ""
                val epoch = parseIsoUtc(fecha) ?: return ""
                java.util.Date(epoch)
            }
            else -> return ""
        }
        val sdf = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale.forLanguageTag("es-PE"))
        return sdf.format(date)
    }

    private fun escucharSuscripcion(clienteId: String) {
        if (clienteId.isBlank()) return

        val subRef = SidebarPaths.suscripciones(db, clienteId)
            .limit(1)

        val subListener = subRef.addSnapshotListener { subSnapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando suscripcion: ${error.message}")
                _isLoading.value = false
                _errorCarga.value = "Error de conexión. Reintenta."
                timeoutJob?.cancel()
                return@addSnapshotListener
            }

            val subDoc = subSnapshot?.documents?.firstOrNull()
            @Suppress("UNCHECKED_CAST")
            featuresPlan = (subDoc?.get("planHerramientasContratadas") as? List<String>)?.toSet() ?: emptySet()
            val planNombreSub = subDoc?.getString("planNombre")
            if (!planNombreSub.isNullOrBlank()) {
                _planNombre.value = planNombreSub
            }

            // Memoria viva para el latido-gate (reevaluación periódica).
            ultimaFechaFinVigencia = subDoc?.get("fechaFin")
            ultimaEtiquetaEstado = subDoc?.getString("estado")?.uppercase() ?: ""

            // Vigencia —” DOBLE CERRADURA con BRIXO (pacto entre repos):
            // bloquea el RELOJ LOCAL sobre fechaFin (instantáneo, sin red)
            // O la ETIQUETA OFICIAL escrita por BrixoPanel (estado == "vencida",
            // aplicada automáticamente al superar la vigencia / corte admin).
            // Se desbloquea solo cuando AMBOS dicen activo: renovación,
            // pago validado y reactivación sobrescriben ambos dentro de sus
            // transacciones. íšnico valor reconocido por diseño: "vencida".
            val bloqueada = subDoc != null && (
                esSuscripcionVencida(subDoc.get("fechaFin")) ||
                    subDoc.getString("estado") == "vencida"
                )
            _vencida.value = bloqueada
            if (bloqueada) {
                if (_estadoBloqueo.value == "ACTIVO") {
                    _estadoBloqueo.value = "VENCIDO"
                }
                _items.value = emptyList()
                _isLoading.value = false
                return@addSnapshotListener
            }

            actualizarSidebar()
        }
        listeners.add(subListener)
    }

    private fun detenerEscuchaRol() {
        rolListenerRegistration?.remove()
        rolListenerRegistration = null
    }

    private fun escucharRol(rolId: String) {
        detenerEscuchaRol()
        if (rolId.isBlank()) {
            permisosRol = emptyMap()
            actualizarSidebar()
            return
        }

        val rolRef = EcosistemaPaths.rolesFarmacia(db).document(rolId)
        val rolListener = rolRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando rol: ${error.message}")
                _isLoading.value = false
                _errorCarga.value = "Error de conexión. Reintenta."
                timeoutJob?.cancel()
                return@addSnapshotListener
            }

            val nombreRol = snapshot?.getString("nombre")
            if (!nombreRol.isNullOrBlank()) {
                _rolNombre.value = nombreRol
            }
            @Suppress("UNCHECKED_CAST")
            permisosRol = (snapshot?.get("permisos") as? Map<String, Any>) ?: emptyMap()
            actualizarSidebar()
        }
        rolListenerRegistration = rolListener
        listeners.add(rolListener)
    }

    private fun esSuscripcionVencida(fechaFin: Any?): Boolean {
        val ahora = System.currentTimeMillis()
        return when (fechaFin) {
            is com.google.firebase.Timestamp -> ahora > fechaFin.toDate().time
            is String -> {
                if (fechaFin.isBlank()) return false
                val fin = parseIsoUtc(fechaFin) ?: return false
                ahora > fin
            }
            else -> false
        }
    }

    private fun parseIsoUtc(iso: String): Long? = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        sdf.parse(iso)?.time
    } catch (e: Exception) {
        android.util.Log.d("FARMADON_PARSE", "Parse fecha sidebar falló: ${e.message}")
        null
    }

    private fun actualizarSidebar() {
        if (_vencida.value || _suspendida.value) {
            _items.value = emptyList()
            _isLoading.value = false
            return
        }

        val modulosBase: List<com.app.administradorfarmadon.modulos.domain.ModuloResuelto> = when {
            catalogo.isNotEmpty() && featuresPlan.isNotEmpty() -> {
                ModulosResueltos.resolver(
                    catalogo = catalogo,
                    featuresPlan = featuresPlan,
                    overridesCliente = overridesCliente
                )
            }
            // CONTRATO CONGELADO: si no hay features pactadas, NO otorgar todo el
            // catálogo (privilegio por defecto). Sin contrato vivo se muestra vacío
            // (solo Configuración). Evita que un plan editado regale herramientas.
            catalogo.isNotEmpty() -> emptyList()
            else -> {
                listOf(
                    com.app.administradorfarmadon.modulos.domain.ModuloResuelto(
                        modulo = "ventas",
                        nombre = "Punto de Venta",
                        categoria = "OPERACIÓN",
                        orden = 5,
                        icono = "point_of_sale"
                    ),
                    com.app.administradorfarmadon.modulos.domain.ModuloResuelto(
                        modulo = "inventario",
                        nombre = "Inventario & Stock",
                        categoria = "OPERACIÓN",
                        orden = 10,
                        icono = "inventory2"
                    ),
                    com.app.administradorfarmadon.modulos.domain.ModuloResuelto(
                        modulo = "inventario_compras",
                        nombre = "Compras y Proveedores",
                        categoria = "OPERACIÓN",
                        orden = 20,
                        icono = "proveedores"
                    )
                )
            }
        }

        val itemsFiltrados = modulosBase
            .filter { moduloResuelto ->
                // Módulos de inventario aún no operativos no se muestran como pantallas
                // rotas. Se ocultan del sidebar hasta que tengan pantalla real y verificada.
                moduloResuelto.modulo !in setOf("inventario_vencimientos", "inventario_transferencias")
            }
            .filter { moduloResuelto ->
                // Sucursales se administra SOLO desde Configuración → Sucursales y Sedes;
                // no se muestra en el sidebar para no duplicar caminos.
                moduloResuelto.modulo != "sucursales"
            }
            .filter { moduloResuelto ->
            // RAÍZ: permisos por USUARIO con fallback fino por módulo.
            // Si usuario tiene entrada explícita para ese módulo, usa esa.
            // Si no, cae al rol. Si ambos vacíos, ve todo lo del plan (dueño).
            if (permisosUsuario.containsKey(moduloResuelto.modulo)) {
                permisosUsuario[moduloResuelto.modulo] == true
            } else if (permisosRol.isNotEmpty()) {
                (permisosRol[moduloResuelto.modulo] as? Map<*, *>)?.get("ver") == true ||
                (permisosRol[moduloResuelto.nombre] as? Map<*, *>)?.get("ver") == true
            } else {
                true
            }
        }.map { m ->
            SidebarItemData(
                nombre = m.nombre,
                modulo = m.modulo,
                icono = m.icono,
                categoria = m.categoria,
                orden = m.orden,
                badge = null
            )
        }

        val listaFinal = mutableListOf<SidebarItemData>()
        listaFinal.addAll(itemsFiltrados)
        listaFinal.add(
            SidebarItemData(
                nombre = "Configuración",
                modulo = "config_farmacia",
                icono = "settings",
                categoria = "SISTEMA",
                orden = 9999,
                badge = null
            )
        )

        _items.value = listaFinal.sortedBy { it.orden }
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        latidoJob?.cancel()
        latidoJob = null
        timeoutJob?.cancel()
        timeoutJob = null
        listeners.forEach { it.remove() }
    }
}
