package com.app.administradorfarmadon.disenotemaapp.ui.tokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Punto de entrada único para acceder a los tokens de diseño de Farmadon.
 * Cumple con el Principio 12 de AGENTS.md (Nomenclatura en español).
 */
object TokensFarmadon {
    val colores: ColoresFarmadon
        @Composable
        @ReadOnlyComposable
        get() = LocalColoresFarmadon.current

    val espaciado: EspaciadoFarmadon
        @Composable
        @ReadOnlyComposable
        get() = LocalEspaciadoFarmadon.current

    val tipografia: TipografiaFarmadon
        @Composable
        @ReadOnlyComposable
        get() = LocalTipografiaFarmadon.current

    val formas: FormasFarmadon
        @Composable
        @ReadOnlyComposable
        get() = LocalFormasFarmadon.current
}
