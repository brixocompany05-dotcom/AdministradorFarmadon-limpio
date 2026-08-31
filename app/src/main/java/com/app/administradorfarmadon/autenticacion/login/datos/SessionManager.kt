package com.app.administradorfarmadon.autenticacion.login.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Almacenamiento local de sesión activa para Farmadon (Multi-Tenant).
 * Preserva en todo momento la identidad del usuario, su tenant (clienteId) y su sucursal asignada.
 */
object SessionManager {
    private const val TAG = "SessionManager"
    private const val PREF_NAME = "usuario"
    
    private var prefs: SharedPreferences? = null

    // Delegados para persistencia automática de sesión
    var idCajera: String by StringPreference("id", "")
    var nombreUsuario: String by StringPreference("usuario", "")
    var rol: String by StringPreference("rol", "Administrador")
    var clienteId: String
        get() = prefs?.getString("cliente_id", "") ?: ""
        set(value) {
            prefs?.edit()?.putString("cliente_id", value)?.apply()
        }

    val clienteIdGarantizado: String get() = clienteId

    /** La farmacia usa el mismo id que el cliente (vínculo 1:1: el uid del dueño es su farmacia). */
    val farmaciaId: String get() = clienteId

    /** Sucursal activa de la sesión; la Sede Principal es el ancla de la farmacia. */
    val sucursalIdEfectiva: String get() = sucursalId.ifBlank { "principal" }

    private val _sucursalFlow = kotlinx.coroutines.flow.MutableStateFlow(sucursalId)
    val sucursalFlow: kotlinx.coroutines.flow.StateFlow<String> = _sucursalFlow

    var sucursalId: String
        get() = prefs?.getString("sucursal_id", "") ?: ""
        set(value) {
            prefs?.edit()?.putString("sucursal_id", value)?.apply()
            _sucursalFlow.value = value
        }
    var sucursalNombre: String by StringPreference("sucursal_nombre", "")
    var dni: String by StringPreference("dni", "")
    var email: String by StringPreference("email", "")

    var monedaCodigo: String by StringPreference("moneda_codigo", "PEN")
    var monedaSimbolo: String by StringPreference("moneda_simbolo", "S/")

    // ── CONTEXTO DE ORGANIZACIÓN (esquema Organization-first, Fase 0) ──
    var orgIdActiva: String by StringPreference("org_id_activa", "")
    var farmaciaIdActiva: String by StringPreference("farmacia_id_activa", "")
    var membresiasJson: String by StringPreference("membresias_json", "[]")

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun cargarSesion(context: Context) {
        init(context)
    }

    fun guardarSesion(
        context: Context,
        id: String,
        nombre: String,
        rolUsuario: String = "Administrador",
        tenantId: String = "",
        sedeId: String = "",
        sedeNombre: String = ""
    ) {
        cargarSesion(context)
        idCajera = id
        nombreUsuario = nombre
        rol = rolUsuario
        if (tenantId.isNotBlank()) clienteId = tenantId
        if (sedeId.isNotBlank()) {
            sucursalId = sedeId
        }
        if (sedeNombre.isNotBlank()) {
            sucursalNombre = sedeNombre
        }
    }

    fun guardarMonedaConfigurada(context: Context, codigo: String, simbolo: String) {
        cargarSesion(context)
        monedaCodigo = codigo.ifBlank { "PEN" }
        monedaSimbolo = simbolo.ifBlank { "S/" }
    }

    // ── MEMBRESÍAS Y CONTEXTO ORGANIZATIVO ──

    /** El login alimenta aquí las organizaciones a las que pertenece la persona. */
    fun guardarMembresias(lista: List<com.app.administradorfarmadon.organizacion.modelo.MembresiaSesion>) {
        val arr = org.json.JSONArray()
        lista.forEach { arr.put(org.json.JSONObject(it.toJson())) }
        membresiasJson = arr.toString()
    }

    fun leerMembresias(): List<com.app.administradorfarmadon.organizacion.modelo.MembresiaSesion> =
        com.app.administradorfarmadon.organizacion.modelo.MembresiaSesion.listaDesdeJson(membresiasJson)

    /** La membresía de la organización actualmente seleccionada (null si aún no eligió). */
    fun membresiaActiva(): com.app.administradorfarmadon.organizacion.modelo.MembresiaSesion? =
        leerMembresias().find { it.orgId == orgIdActiva }

    /** Fija el contexto de trabajo: en qué org y en qué farmacia se opera. */
    fun seleccionarContextoOrganizacion(orgId: String, farmaciaId: String) {
        orgIdActiva = orgId
        farmaciaIdActiva = farmaciaId
    }

    fun limpiarSesion(context: Context) {
        cargarSesion(context)
        prefs?.edit()?.clear()?.apply()
        
        idCajera = ""
        nombreUsuario = ""
        rol = "Administrador"
        clienteId = ""
        sucursalId = ""
        sucursalNombre = ""
        dni = ""
        email = ""
        orgIdActiva = ""
        farmaciaIdActiva = ""
        membresiasJson = "[]"
        monedaCodigo = "PEN"
        monedaSimbolo = "S/"

        // ── HIGIENE ANTI-FANTASMA (tablet compartida / base borrada) ──
        // La caché offline de Firestore y los borradores del registro viven en
        // el DISPOSITIVO, fuera de la sesión. Si no se borran al cerrar, el
        // siguiente usuario puede ver reflejos de datos que ya no existen en
        // el servidor o de OTRA farmacia. Dos intentos por si los listeners
        // aún se están despegando.
        CoroutineScope(Dispatchers.IO).launch {
            var limpiada = false
            var intento = 0
            while (!limpiada && intento < 2) {
                intento++
                try {
                    FarmadonFirestore.db.clearPersistence().await()
                    limpiada = true
                    Log.i(TAG, "[HIGIENE] Caché local de Firestore eliminada al cerrar sesión")
                } catch (e: Exception) {
                    Log.w(TAG, "[HIGIENE] Limpieza de caché falló (intento $intento): ${e.message}")
                    if (intento < 2) kotlinx.coroutines.delay(800)
                }
            }
        }
        context.getSharedPreferences("registro_draft_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    // --- Implementación de Delegados ---

    private class StringPreference(val key: String, val defaultValue: String) : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String {
            return prefs?.getString(key, defaultValue) ?: defaultValue
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            prefs?.edit()?.putString(key, value)?.apply()
        }
    }
}
