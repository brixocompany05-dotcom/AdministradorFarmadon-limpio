package com.app.administradorfarmadon.disenotemaapp.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor de Tema Centralizado.
 * Cumple con el Principio 12 de AGENTS.md (Nomenclatura en español).
 */
class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencias = application.getSharedPreferences("preferencias_tema", Context.MODE_PRIVATE)
    
    private val _esTemaOscuro = MutableStateFlow(preferencias.getBoolean("es_tema_oscuro", true))
    val esTemaOscuro: StateFlow<Boolean> = _esTemaOscuro.asStateFlow()
    
    // Alias para compatibilidad con código legado (isDarkMode) - Se eliminará en Fase 3
    val isDarkMode: StateFlow<Boolean> = esTemaOscuro

    fun alternarTema() {
        val nuevoModo = !_esTemaOscuro.value
        _esTemaOscuro.value = nuevoModo
        preferencias.edit().putBoolean("es_tema_oscuro", nuevoModo).apply()
    }
    
    fun establecerTemaOscuro(oscuro: Boolean) {
        _esTemaOscuro.value = oscuro
        preferencias.edit().putBoolean("es_tema_oscuro", oscuro).apply()
    }
}
