package com.app.administradorfarmadon.navegacion.sidebar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.ColoresClarosFarmadon
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.ColoresOscurosFarmadon

/**
 * Colores específicos para el Sidebar de Farmadon (Enterprise Monocromático 2026).
 *
 * CERO AZUL: Toda la paleta utiliza tonos carbón, grafito, platino y obsidiana
 * con perfecta armonía y alto contraste.
 */
object SidebarTheme {
    var isDark by mutableStateOf(true)

    val Background: Color
        get() = if (isDark) ColoresOscurosFarmadon.sidebarFondo else ColoresClarosFarmadon.sidebarFondo

    val TextPrimary: Color
        get() = if (isDark) ColoresOscurosFarmadon.textoPrincipal else ColoresClarosFarmadon.textoPrincipal

    val TextSecondary: Color
        get() = if (isDark) ColoresOscurosFarmadon.sidebarItemInactivoTexto else ColoresClarosFarmadon.sidebarItemInactivoTexto

    val Border: Color
        get() = if (isDark) ColoresOscurosFarmadon.sidebarBorde else ColoresClarosFarmadon.sidebarBorde

    val Accent: Color
        get() = if (isDark) ColoresOscurosFarmadon.sidebarItemActivoIcono else ColoresClarosFarmadon.sidebarItemActivoTexto

    val ActiveBg: Color
        get() = if (isDark) ColoresOscurosFarmadon.sidebarItemActivoFondo else ColoresClarosFarmadon.sidebarItemActivoFondo

    val ActiveText: Color
        get() = if (isDark) ColoresOscurosFarmadon.sidebarItemActivoTexto else ColoresClarosFarmadon.sidebarItemActivoTexto

    val InactiveIcon: Color
        get() = if (isDark) ColoresOscurosFarmadon.sidebarItemInactivoIcono else ColoresClarosFarmadon.sidebarItemInactivoIcono

    val Destructive: Color
        get() = if (isDark) ColoresOscurosFarmadon.estadoPeligro else ColoresClarosFarmadon.estadoPeligro
}
