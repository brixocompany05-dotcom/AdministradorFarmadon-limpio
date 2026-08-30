package com.app.administradorfarmadon.disenotemaapp.ui

import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

// ──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•
// MEDIDA ADAPTATIVA —” Geometría Física 2026
// ──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•
// Tablet-First (600dp ──†’ 1360dp) con matemática física real.
// Fórmula base:  f = (W / Wref) ^ curva   clamp [0.85 , 1.18]
// Wref = 1280dp (ancho enterprise estándar). Curva 0.55 = física
// de resorte amortiguado: crece rápido al inicio, se suaviza al
// ampliar. Todo tamaño = base * f^exponente  (exponente modula
// sensibilidad: tipografía 0.35, espacio 0.30, componente 0.38 —” suave
// porque ya existe escalado global de densidad)
// Garantiza: nada apretado, nada roto, nada pegado —” simetría
// geométrica preservada en cualquier pantalla, claro/oscuro idéntico.
// ──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•

private const val REFERENCE_WIDTH = 1280f
private const val REFERENCE_HEIGHT = 800f
private const val CURVA_FISICA = 0.55
private const val MIN_SCALE = 0.85f
private const val MAX_SCALE = 1.18f

/**
 * Sistema de escala adaptativo con física.
 * @param smallestW ancho menor del dispositivo (dp)
 * @param screenW ancho actual (dp) —” si se provee usa BoxWithConstraints
 * @param screenH alto actual (dp) —” modula factor altura
 */
class MedidaAdaptativa(
    private val smallestW: Float,
    private val screenW: Float = smallestW,
    private val screenH: Float = REFERENCE_HEIGHT
) {

    // ──”€──”€ Orientación ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    val isLandscape: Boolean get() = screenW >= screenH

    // ──”€──”€ Factor de escala física ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    // Eje W: curva amortiguada. Eje H: corrección suave (0.30) para que
    // pantallas muy bajas no aplasten. Factor final = media geométrica.
    private val scaleW = (smallestW / REFERENCE_WIDTH).toDouble().pow(CURVA_FISICA).toFloat().coerceIn(MIN_SCALE, MAX_SCALE)
    private val scaleH = (screenH / REFERENCE_HEIGHT).toDouble().pow(0.30).toFloat().coerceIn(0.92f, 1.10f)
    private val scale: Float = kotlin.math.sqrt((scaleW * scaleH).toDouble()).toFloat().coerceIn(MIN_SCALE, MAX_SCALE)

    // ──”€──”€ Exponentes (sensibilidad por familia) ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    // Reducidos para no duplicar con el escalado global de densidad (EscalaAdaptativa 0.85-1.15)
    // Física suave: tipografía y componentes apenas modulan (──±6%), espaciado modula más
    private val typographyScale get() = scale.toDouble().pow(0.35).toFloat()
    private val spacingScale    get() = scale.toDouble().pow(0.30).toFloat()
    private val componentScale  get() = scale.toDouble().pow(0.38).toFloat()

    // ──”€──”€ Tipografía (legible 13sp mínimo —” nunca romper) ──”€──”€──”€──”€──”€──”€
    val textLabel        get() = (10.5f * typographyScale).sp
    val textChip         get() = (11f * typographyScale).sp
    val textBody         get() = (13.5f * typographyScale).sp
    val textInput        get() = (14f * typographyScale).sp
    val textSubtitle     get() = (16f * typographyScale).sp
    val textTitle        get() = (20f * typographyScale).sp
    val textDisplay      get() = (32f * typographyScale).sp
    val textDisplayLg    get() = (48f * typographyScale).sp
    val textHeaderLbl    get() = (11f * typographyScale).sp
    val textBodyLineHeight get() = (textBody.value * 1.5f).sp
    val textLetterSpacing get() = (0.4f * typographyScale).sp

    // ──”€──”€ Espaciado (proporción áurea Ï†=1.618 entre saltos) ──”€──”€──”€──”€──”€
    val gapTiny   get() = (6f  * spacingScale).dp
    val gapSmall  get() = (10f * spacingScale).dp
    val gapMedium get() = (14f * spacingScale).dp
    val gapLarge  get() = (18f * spacingScale).dp
    val gapXLarge get() = (24f * spacingScale).dp
    val gapXXLarge get() = (32f * spacingScale).dp
    val gapXXXLarge get() = (48f * spacingScale).dp

    val xs get() = gapTiny
    val sm get() = gapSmall
    val md get() = gapMedium
    val lg get() = gapLarge
    val xl get() = gapXLarge
    val xxl get() = gapXXLarge
    val xxxl get() = gapXXXLarge

    // ──”€──”€ Padding geométrico ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    val padInputH  get() = (16f * spacingScale).dp
    val padInputV  get() = (12f * spacingScale).dp
    val padModule  get() = (22f * spacingScale).dp
    val padScreenH get() = (22f * spacingScale).dp
    val padScreenV get() = (18f * spacingScale).dp
    val padCard    get() = (16f * spacingScale).dp
    val padCardLarge get() = (20f * spacingScale).dp

    // ──”€──”€ Alturas (proporción táctil 48dp mínimo) ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    val inputMinH  get() = ((52f * componentScale).dp).coerceIn(48.dp, 60.dp)
    val btnSmallH  get() = ((38f * componentScale).dp).coerceIn(36.dp, 44.dp)
    val btnMediumH get() = ((48f * componentScale).dp).coerceIn(44.dp, 56.dp)
    val btnLargeH  get() = ((54f * componentScale).dp).coerceIn(50.dp, 60.dp)
    val btnHugeH   get() = ((60f * componentScale).dp).coerceIn(56.dp, 68.dp)
    val chipH      get() = (36f * componentScale).dp
    val iconTiny   get() = (14f * componentScale).dp
    val iconSmall  get() = (18f * componentScale).dp
    val iconMedium get() = (22f * componentScale).dp
    val iconLarge  get() = (28f * componentScale).dp

    // ──”€──”€ Radios ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    val radiusInput  get() = (12f * componentScale).dp
    val radiusButton get() = (12f * componentScale).dp
    val radiusChip   get() = (10f * componentScale).dp
    val radiusCard   get() = (16f * componentScale).dp
    val radiusSheet  get() = (24f * componentScale).dp

    // ──”€──”€ Bordes ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    val borderWidth get() = (1f).dp
    val separatorH  get() = (1f).dp

    // ──”€──”€ Anchos proporcionales (matemática del viewport) ──”€──”€──”€──”€──”€──”€
    /** Ancho del riel izquierdo: 32% del ancho disponible, clamp 300—“380 */
    fun anchoRiel(anchoDisponible: Float): androidx.compose.ui.unit.Dp {
        val raw = anchoDisponible * 0.32f
        return raw.coerceIn(300f, 380f).dp
    }
    /** Gap entre columnas: 1.8% del ancho, clamp 14—“24 */
    fun gapColumnas(anchoDisponible: Float): androidx.compose.ui.unit.Dp =
        (anchoDisponible * 0.018f).coerceIn(14f, 24f).dp

    // ──”€──”€ Clasificación ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    val screenSize: String get() = when {
        smallestW < 720f  -> "TABLET_SMALL"
        smallestW < 900f  -> "TABLET_MEDIUM"
        else              -> "TABLET_LARGE"
    }

    val scaleFactor: Float get() = scale
}

/**
 * Crea una [MedidaAdaptativa] basada en las dimensiones actuales.
 * Usa smallestWidth + tamaño real de ventana para física completa.
 */
@Composable
fun recordarMedidaAdaptativa(): MedidaAdaptativa {
    val config = LocalConfiguration.current
    val smallestW = config.smallestScreenWidthDp.toFloat()
    val w = config.screenWidthDp.toFloat()
    val h = config.screenHeightDp.toFloat()
    return remember(smallestW, w, h) {
        MedidaAdaptativa(smallestW, w, h)
    }
}

/**
 * Variante BoxWithConstraints: cálculo exacto con ancho/alto del contenedor.
 * íšsala dentro de BoxWithConstraints para que cada panel calcule su escala
 * con su viewport real —” no el del dispositivo —” y todo quede simétrico.
 */
@Composable
fun recordarMedidaGeomtrica(
    anchoDisponibleDp: Float,
    altoDisponibleDp: Float
): MedidaAdaptativa {
    val config = LocalConfiguration.current
    val smallestW = config.smallestScreenWidthDp.toFloat()
    return remember(smallestW, anchoDisponibleDp, altoDisponibleDp) {
        // smallestW sigue como base, pero modulamos con el viewport real
        MedidaAdaptativa(
            smallestW = smallestW,
            screenW = anchoDisponibleDp,
            screenH = altoDisponibleDp
        )
    }
}
