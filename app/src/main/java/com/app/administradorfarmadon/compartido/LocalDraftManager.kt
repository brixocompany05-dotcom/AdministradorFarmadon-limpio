package com.app.administradorfarmadon.compartido

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Metadata básica para listar borradores sin cargar el estado completo.
 */
data class DraftSummary(
    val key: String,
    val title: String,
    val timestamp: Long,
    val mode: String = "MANUAL",
    val subtitle: String = ""
)

/**
 * Gestor de borradores locales para prevenir pérdida de datos.
 * Implementa auto-limpieza (TTL 12h) y persistencia en SharedPreferences.
 */
object LocalDraftManager {

    private const val PREFS_NAME = "farmadon_drafts"
    private const val TTL_MILLIS = 12 * 60 * 60 * 1000L // 12 horas

    private var prefs: SharedPreferences? = null
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cleanupOldDrafts()
    }

    /**
     * Guarda un borrador con un título identificativo (opcional) y el modo de entrada.
     */
    fun <T> saveDraft(key: String, data: T, type: Class<T>, title: String = "", mode: String = "MANUAL", subtitle: String = "") {
        val json = moshi.adapter(type).toJson(data)
        prefs?.edit()?.apply {
            putString(key, json)
            putLong("${key}_timestamp", System.currentTimeMillis())
            if (title.isNotBlank()) {
                putString("${key}_title", title)
            }
            putString("${key}_mode", mode)
            putString("${key}_subtitle", subtitle)
            apply()
        }
    }

    fun getDraftMode(key: String): String? {
        return prefs?.getString("${key}_mode", null)
    }

    fun <T> loadDraft(key: String, type: Class<T>): T? {
        val json = prefs?.getString(key, null) ?: return null
        val timestamp = prefs?.getLong("${key}_timestamp", 0L) ?: 0L
        
        if (System.currentTimeMillis() - timestamp > TTL_MILLIS) {
            deleteDraft(key)
            return null
        }

        return try {
            moshi.adapter(type).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtiene una lista de resúmenes de borradores que coincidan con un prefijo.
     */
    fun getDraftsList(prefix: String): List<DraftSummary> {
        val allPrefs = prefs?.all ?: return emptyList()
        val summaries = mutableListOf<DraftSummary>()

        allPrefs.keys.forEach { key ->
            if (key.startsWith(prefix) && !key.endsWith("_timestamp") && !key.endsWith("_title") && !key.endsWith("_mode") && !key.endsWith("_subtitle")) {
                val timestamp = allPrefs["${key}_timestamp"] as? Long ?: 0L
                val title = allPrefs["${key}_title"] as? String ?: "Sin nombre"
                val mode = allPrefs["${key}_mode"] as? String ?: "MANUAL"
                val subtitle = allPrefs["${key}_subtitle"] as? String ?: ""
                
                // Solo añadir si no ha expirado
                if (System.currentTimeMillis() - timestamp <= TTL_MILLIS) {
                    summaries.add(DraftSummary(key, title, timestamp, mode, subtitle))
                }
            }
        }

        return summaries.sortedByDescending { it.timestamp }
    }

    fun deleteDraft(key: String) {
        prefs?.edit()?.apply {
            remove(key)
            remove("${key}_timestamp")
            remove("${key}_title")
            remove("${key}_mode")
            remove("${key}_subtitle")
            apply()
        }
    }

    private fun cleanupOldDrafts() {
        val now = System.currentTimeMillis()
        val allPrefs = prefs?.all ?: return
        val toRemove = mutableListOf<String>()

        allPrefs.forEach { (key, value) ->
            if (key.endsWith("_timestamp") && value is Long) {
                if (now - value > TTL_MILLIS) {
                    toRemove.add(key.substringBefore("_timestamp"))
                }
            }
        }

        toRemove.forEach { deleteDraft(it) }
    }
}
