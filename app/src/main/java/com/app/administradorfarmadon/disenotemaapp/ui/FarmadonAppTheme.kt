package com.app.administradorfarmadon.disenotemaapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.*

/**
 * Tema Maestro de Farmadon con Soporte para Escala Adaptativa y Tokens Universales.
 * Centraliza la estética empresarial y la densidad visual dinámica.
 */
@Composable
fun FarmadonAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val factorEscala = EscalaAdaptativa.calcularFactorEscala()
    val density = LocalDensity.current
    
    // Provee la densidad escalada manteniendo fontScale intacto para accesibilidad
    val scaledDensity = Density(
        density = density.density * factorEscala,
        fontScale = density.fontScale
    )

    val coloresFarmadon = if (darkTheme) ColoresOscurosFarmadon else ColoresClarosFarmadon
    val espaciadoFarmadon = EspaciadoFarmadon()
    val tipografiaFarmadon = TipografiaFarmadon()
    val formasFarmadon = FormasFarmadon()

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = coloresFarmadon.botonPrimarioFondo, // Ahora el primario es Negro/Blanco (Neutro)
            surface = coloresFarmadon.superficieDefecto,
            background = coloresFarmadon.fondoBase,
            onSurface = coloresFarmadon.textoPrincipal,
            onBackground = coloresFarmadon.textoPrincipal,
            surfaceVariant = coloresFarmadon.inputFondo, // Fondo del cajón de texto
            onSurfaceVariant = coloresFarmadon.inputPlaceholder,
            outline = coloresFarmadon.inputBorde,
            error = coloresFarmadon.estadoPeligro
        )
    } else {
        lightColorScheme(
            primary = coloresFarmadon.botonPrimarioFondo, // Ahora el primario es Negro Carbón (Neutro)
            surface = coloresFarmadon.superficieDefecto,
            background = coloresFarmadon.fondoBase,
            onSurface = coloresFarmadon.textoPrincipal,
            onBackground = coloresFarmadon.textoPrincipal,
            surfaceVariant = coloresFarmadon.inputFondo, // Fondo blanco sobre panel gris
            onSurfaceVariant = coloresFarmadon.inputPlaceholder,
            outline = coloresFarmadon.inputBorde,
            error = coloresFarmadon.estadoPeligro
        )
    }

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalColoresFarmadon provides coloresFarmadon,
        LocalEspaciadoFarmadon provides espaciadoFarmadon,
        LocalTipografiaFarmadon provides tipografiaFarmadon,
        LocalFormasFarmadon provides formasFarmadon
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = {
                Surface(
                    color = coloresFarmadon.fondoBase
                ) {
                    content()
                }
            }
        )
    }
}
