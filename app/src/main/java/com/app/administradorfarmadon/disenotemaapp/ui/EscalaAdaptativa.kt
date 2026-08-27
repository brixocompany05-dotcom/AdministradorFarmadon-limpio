package com.app.administradorfarmadon.disenotemaapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.pow

/**
 * Motor de Escala Adaptativa Robusta de Farmadon.
 * Calcula el factor de escala global basado en el ancho físico del dispositivo.
 * 
 * Fórmula: (anchoFisico / 1280)^0.55
 * Clamp: [0.85, 1.15]
 */
object EscalaAdaptativa {
    private const val REFERENCE_WIDTH = 1280.0
    private const val SCALE_CURVE = 0.55
    private const val MIN_SCALE = 0.85f
    private const val MAX_SCALE = 1.15f

    @Composable
    fun calcularFactorEscala(): Float {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.toDouble()
        
        val factor = (screenWidth / REFERENCE_WIDTH).pow(SCALE_CURVE).toFloat()
        
        return factor.coerceIn(MIN_SCALE, MAX_SCALE)
    }
}
