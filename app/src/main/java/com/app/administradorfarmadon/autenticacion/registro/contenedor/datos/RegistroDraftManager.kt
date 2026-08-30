package com.app.administradorfarmadon.autenticacion.registro.contenedor.datos

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class RegistroDraft(
    val pasoActual: Int,
    val planSeleccionado: String,
    val nombreFarmacia: String,
    val dueno: String,
    val email: String,
    val telefono: String,
    val ruc: String,
    val direccion: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    // País elegido en el Paso 1. Sin esto, un borrador reabierto renacía
    // peruano: el aspirante veía planes PEN y podía enviar la solicitud
    // con país equivocado. Legados sin país ──†’ vacío: vuelve a elegirlo.
    val paisIso: String = ""
)

object RegistroDraftManager {
    private const val PREFS_NAME = "registro_draft_prefs"
    private const val KEY_DRAFT = "registro_draft_json"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val adapter = moshi.adapter(RegistroDraft::class.java)

    fun saveDraft(context: Context, draft: RegistroDraft) {
        val json = adapter.toJson(draft)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DRAFT, json)
            .apply()
    }

    fun loadDraft(context: Context): RegistroDraft? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DRAFT, null) ?: return null
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            android.util.Log.d("FARMADON_PARSE", "Parse opcional falló (fallback null): ${e.message}")
            null
        }
    }

    fun clearDraft(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DRAFT)
            .apply()
    }

    fun hasDraft(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .contains(KEY_DRAFT)
    }
}
