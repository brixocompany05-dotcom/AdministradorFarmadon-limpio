package com.app.administradorfarmadon.disenotemaapp.ui.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Sistema de Colores Comercial y Armónico de Farmadon (Enterprise 2026).
 *
 * REGLAS FUNDAMENTALES:
 * 1. CERO AZUL CHILLÓN: No se usan azules saturados ni colores fluorescentes.
 * 2. ARMONÍA Y ALTO CONTRASTE: Tonalidades grafito, platino, carbón y obsidiana con
 *    estados semánticos sobrios (esmeralda, ámbar, carmesí).
 * 3. CONTROL TOTAL POR TEMA: Cada elemento (pantalla, card, texto, botón, sidebar,
 *    divisor y estados) tiene su token exacto tanto para Tema Claro como Oscuro.
 */
@Immutable
data class ColoresFarmadon(
    // ── 1. Pantallas y Fondos ────────────────────────────────────────────
    val fondoBase: Color,
    val fondoSidebar: Color,
    val fondoModal: Color,
    val fondoOverlay: Color,

    // ── 2. Cards y Superficies ──────────────────────────────────────────
    val cardBase: Color,
    val cardElevada: Color,
    val cardHover: Color,
    val cardBorde: Color,

    // ── 3. Controles e Inputs (Legibilidad) ──────────────────────────────
    val inputFondo: Color,
    val inputFondoFoco: Color,
    val inputBorde: Color,
    val inputTexto: Color,
    val inputPlaceholder: Color,

    // ── 4. Jerarquía de Textos ──────────────────────────────────────────
    val textoPrincipal: Color,
    val textoSecundario: Color,
    val textoTerciario: Color,
    val textoInvertido: Color,

    // ── 4. Botones y Controles ──────────────────────────────────────────
    val botonPrimarioFondo: Color,
    val botonPrimarioTexto: Color,
    val botonSecundarioFondo: Color,
    val botonSecundarioBorde: Color,
    val botonSecundarioTexto: Color,
    val botonGhostTexto: Color,

    // ── 5. Sidebar y Navegación ────────────────────────────────────────
    val sidebarFondo: Color,
    val sidebarBorde: Color,
    val sidebarItemActivoFondo: Color,
    val sidebarItemActivoTexto: Color,
    val sidebarItemActivoIcono: Color,
    val sidebarItemInactivoTexto: Color,
    val sidebarItemInactivoIcono: Color,

    // ── 6. Bordes y Divisores ──────────────────────────────────────────
    val bordeSutil: Color,
    val bordeDefecto: Color,
    val bordeEnfoque: Color,
    val divisor: Color,

    // ── 7. Estados Comerciales Sobrios (Sin estridencias) ────────────────
    val estadoExito: Color,
    val exitoSutil: Color,
    val estadoAlerta: Color,
    val alertaSutil: Color,
    val estadoPeligro: Color,
    val peligroSutil: Color,
    val estadoNeutral: Color,
    val neutroSutil: Color,

    // ── 8. Efectos y Transparencias ────────────────────────────────────
    val cristal: Color,
    val velo: Color,
    val esTemaClaro: Boolean
) {
    // Aliases para compatibilidad con código existente
    val superficieDefecto: Color get() = cardBase
    val superficieElevada: Color get() = cardElevada
}

/** Paleta Comercial Modo Oscuro (Grafito Obsidiana · Alto Confort) */
val ColoresOscurosFarmadon = ColoresFarmadon(
    // Fondos
    fondoBase = Color(0xFF0C0D0F),
    fondoSidebar = Color(0xFF131417),
    fondoModal = Color(0xFF181A1E),
    fondoOverlay = Color(0xCC000000),

    // Cards
    cardBase = Color(0xFF16181C),
    cardElevada = Color(0xFF1D2025),
    cardHover = Color(0xFF24272E),
    cardBorde = Color(0xFF282B32),

    // Inputs
    inputFondo = Color(0xFF13151A), // Más profundo que cardBase (Efecto "Hueco")
    inputFondoFoco = Color(0xFF1C1F26), // Sutilmente más claro al enfocar (+5% aprox)
    inputBorde = Color(0xFF374151),
    inputTexto = Color(0xFFF9FAFB),
    inputPlaceholder = Color(0xFF6B7280),

    // Textos
    textoPrincipal = Color(0xFFF9FAFB),
    textoSecundario = Color(0xFF9CA3AF),
    textoTerciario = Color(0xFF6B7280),
    textoInvertido = Color(0xFF111827),

    // Botones
    botonPrimarioFondo = Color(0xFFF9FAFB),
    botonPrimarioTexto = Color(0xFF111827),
    botonSecundarioFondo = Color(0xFF1E2026),
    botonSecundarioBorde = Color(0xFF2E323B),
    botonSecundarioTexto = Color(0xFFF9FAFB),
    botonGhostTexto = Color(0xFF9CA3AF),

    // Sidebar
    sidebarFondo = Color(0xFF111215),
    sidebarBorde = Color(0xFF1F2126),
    sidebarItemActivoFondo = Color(0xFF22252C),
    sidebarItemActivoTexto = Color(0xFFFFFFFF),
    sidebarItemActivoIcono = Color(0xFFFFFFFF),
    sidebarItemInactivoTexto = Color(0xFF8E939D),
    sidebarItemInactivoIcono = Color(0xFF717682),

    // Bordes y Divisores
    bordeSutil = Color(0xFF1E2025),
    bordeDefecto = Color(0xFF282B32),
    bordeEnfoque = Color(0xFF4B5563),
    divisor = Color(0xFF1F2126),

    // Estados
    estadoExito = Color(0xFF10B981),
    exitoSutil = Color(0x1A10B981),
    estadoAlerta = Color(0xFFF59E0B),
    alertaSutil = Color(0x1AF59E0B),
    estadoPeligro = Color(0xFFEF4444),
    peligroSutil = Color(0x1AEF4444),
    estadoNeutral = Color(0xFF6B7280),
    neutroSutil = Color(0x1A6B7280),

    // Efectos
    cristal = Color(0x0AFFFFFF),
    velo = Color(0xE6000000),
    esTemaClaro = false
)

/** Paleta Comercial Modo Claro (Platino Puro · Enterprise Mate) */
val ColoresClarosFarmadon = ColoresFarmadon(
    // Fondos
    fondoBase = Color(0xFFF8FAFC), // Neutro Níveo (Fondo de página limpio)
    fondoSidebar = Color(0xFFFFFFFF),
    fondoModal = Color(0xFFFFFFFF),
    fondoOverlay = Color(0xB3000000), // Negro 70% para enfoque total (Enterprise Focus)

    // Cards
    cardBase = Color(0xFFE2E8F0), // Gris Acero (Estructura sólida y definida)
    cardElevada = Color(0xFFCBD5E1), // Gris Mercurio para sub-secciones
    cardHover = Color(0xFFBCC1C9),
    cardBorde = Color(0xFF94A3B8), // Borde más presente para definición

    // Inputs
    inputFondo = Color(0xFFFFFFFF), // Blanco puro para inputs
    inputFondoFoco = Color(0xFFF1F3F5),
    inputBorde = Color(0xFF64748B),
    inputTexto = Color(0xFF0F172A),
    inputPlaceholder = Color(0xFF4B5563),

    // Textos
    textoPrincipal = Color(0xFF0F172A), // Negro Profundo para máxima nitidez
    textoSecundario = Color(0xFF334155),
    textoTerciario = Color(0xFF475569),
    textoInvertido = Color(0xFFFFFFFF),

    // Botones
    botonPrimarioFondo = Color(0xFF000000), // Negro Puro para máxima autoridad
    botonPrimarioTexto = Color(0xFFFFFFFF), // Blanco Puro
    botonSecundarioFondo = Color(0xFFFFFFFF),
    botonSecundarioBorde = Color(0xFF9CA3AF),
    botonSecundarioTexto = Color(0xFF000000),
    botonGhostTexto = Color(0xFF374151),

    // Sidebar
    sidebarFondo = Color(0xFFF1F5F9),
    sidebarBorde = Color(0xFFCBD5E1),
    sidebarItemActivoFondo = Color(0xFFE2E8F0),
    sidebarItemActivoTexto = Color(0xFF000000),
    sidebarItemActivoIcono = Color(0xFF000000),
    sidebarItemInactivoTexto = Color(0xFF64748B),
    sidebarItemInactivoIcono = Color(0xFF64748B),

    // Bordes y Divisores
    bordeSutil = Color(0xFFE2E8F0),
    bordeDefecto = Color(0xFFCBD5E1),
    bordeEnfoque = Color(0xFF000000),
    divisor = Color(0xFFCBD5E1),

    // Estados
    estadoExito = Color(0xFF059669),
    exitoSutil = Color(0xFFECFDF5),
    estadoAlerta = Color(0xFFD97706),
    alertaSutil = Color(0xFFFFFBEB),
    estadoPeligro = Color(0xFFDC2626),
    peligroSutil = Color(0xFFFEF2F2),
    estadoNeutral = Color(0xFF64748B),
    neutroSutil = Color(0xFFF1F5F9),

    // Efectos
    cristal = Color(0x0F000000),
    velo = Color(0xB3FFFFFF),
    esTemaClaro = true
)

val LocalColoresFarmadon = staticCompositionLocalOf { ColoresOscurosFarmadon }
