package com.app.administradorfarmadon.disenotemaapp.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.ColoresClarosFarmadon
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.ColoresOscurosFarmadon
import com.app.administradorfarmadon.navegacion.sidebar.SidebarTheme

// ──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•
// FARMADON DESIGN SYSTEM —” Enterprise SaaS Tablet 2026
// Paleta comercial, armónica y de alto contraste (Light & Dark)
// ──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•──•

object FDColors {
    val isDark: Boolean get() = SidebarTheme.isDark

    // ──”€──”€ Fondos ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    val Background: Color      get() = if (isDark) ColoresOscurosFarmadon.fondoBase else ColoresClarosFarmadon.fondoBase
    val Surface: Color         get() = if (isDark) ColoresOscurosFarmadon.cardBase else ColoresClarosFarmadon.cardBase
    val SurfaceElevated: Color get() = if (isDark) ColoresOscurosFarmadon.cardElevada else ColoresClarosFarmadon.cardElevada
    val SurfaceHover: Color    get() = if (isDark) ColoresOscurosFarmadon.cardHover else ColoresClarosFarmadon.cardHover
    val Glass: Color           get() = if (isDark) ColoresOscurosFarmadon.cristal else ColoresClarosFarmadon.cristal

    // ──”€──”€ Acento Comercial ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    val Primary: Color         get() = if (isDark) ColoresOscurosFarmadon.botonPrimarioFondo else ColoresClarosFarmadon.botonPrimarioFondo
    val PrimaryText: Color     get() = if (isDark) ColoresOscurosFarmadon.botonPrimarioTexto else ColoresClarosFarmadon.botonPrimarioTexto
    val PrimaryHover: Color    get() = if (isDark) Color(0xFFE5E7EB) else Color(0xFF1F2937)
    val PrimarySubtle: Color   get() = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)

    // ──”€──”€ Inputs y Controles (Alto Contraste) ──”€──”€──”€──”€──”€──”€
    val InputBackground: Color get() = if (isDark) ColoresOscurosFarmadon.inputFondo else ColoresClarosFarmadon.inputFondo
    val InputBorder: Color     get() = if (isDark) ColoresOscurosFarmadon.inputBorde else ColoresClarosFarmadon.inputBorde
    val InputPlaceholder: Color get() = if (isDark) ColoresOscurosFarmadon.inputPlaceholder else ColoresClarosFarmadon.inputPlaceholder
    val InputText: Color        get() = if (isDark) ColoresOscurosFarmadon.inputTexto else ColoresClarosFarmadon.inputTexto

    // ──”€──”€ Estados Semánticos Sobrios ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    val Success: Color         get() = if (isDark) ColoresOscurosFarmadon.estadoExito else ColoresClarosFarmadon.estadoExito
    val SuccessSubtle: Color   get() = if (isDark) ColoresOscurosFarmadon.exitoSutil else ColoresClarosFarmadon.exitoSutil
    val Warning: Color         get() = if (isDark) ColoresOscurosFarmadon.estadoAlerta else ColoresClarosFarmadon.estadoAlerta
    val WarningSubtle: Color   get() = if (isDark) ColoresOscurosFarmadon.alertaSutil else ColoresClarosFarmadon.alertaSutil
    val Error: Color           get() = if (isDark) ColoresOscurosFarmadon.estadoPeligro else ColoresClarosFarmadon.estadoPeligro
    val ErrorSubtle: Color     get() = if (isDark) ColoresOscurosFarmadon.peligroSutil else ColoresClarosFarmadon.peligroSutil
    val Health: Color          get() = Success
    val HealthSubtle: Color    get() = SuccessSubtle
    val Info: Color            get() = TextPrimary
    val InfoSubtle: Color      get() = PrimarySubtle

    // ──”€──”€ Textos de Alto Contraste ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    val TextPrimary: Color     get() = if (isDark) ColoresOscurosFarmadon.textoPrincipal else ColoresClarosFarmadon.textoPrincipal
    val TextSecondary: Color   get() = if (isDark) ColoresOscurosFarmadon.textoSecundario else ColoresClarosFarmadon.textoSecundario
    val TextTertiary: Color    get() = if (isDark) ColoresOscurosFarmadon.textoTerciario else ColoresClarosFarmadon.textoTerciario
    val TextDisabled: Color    get() = if (isDark) Color(0xFF4B5563) else Color(0xFF9CA3AF)

    // ──”€──”€ Bordes y Líneas ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    val Border: Color          get() = if (isDark) ColoresOscurosFarmadon.bordeSutil else ColoresClarosFarmadon.bordeSutil
    val BorderStrong: Color    get() = if (isDark) ColoresOscurosFarmadon.bordeDefecto else ColoresClarosFarmadon.bordeDefecto
    val BorderFocus: Color     get() = if (isDark) ColoresOscurosFarmadon.bordeEnfoque else ColoresClarosFarmadon.bordeEnfoque
    val BorderError: Color     get() = Error
    val BorderSuccess: Color   get() = Success

    // ──”€──”€ Overlays ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€
    val Overlay: Color         get() = if (isDark) ColoresOscurosFarmadon.fondoOverlay else ColoresClarosFarmadon.fondoOverlay
    val Scrim: Color           get() = if (isDark) ColoresOscurosFarmadon.velo else ColoresClarosFarmadon.velo
}

// ──”€──”€──”€ FORMAS ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€

object FDShapes {
    val XSmall  = RoundedCornerShape(6.dp)
    val Small   = RoundedCornerShape(8.dp)
    val Medium  = RoundedCornerShape(12.dp)
    val Large   = RoundedCornerShape(16.dp)
    val XLarge  = RoundedCornerShape(20.dp)
    val Full    = RoundedCornerShape(50)
}

// ──”€──”€──”€ TIPOGRAFíA DINÁMICA DE ALTO CONTRASTE ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€

object FDType {
    val Display: TextStyle get() = TextStyle(
        fontWeight = FontWeight.Bold,     fontSize = 32.sp,
        lineHeight = 40.sp,  letterSpacing = (-0.5).sp,
        color = FDColors.TextPrimary
    )

    val Heading1: TextStyle get() = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 24.sp,
        lineHeight = 32.sp,  letterSpacing = (-0.3).sp,
        color = FDColors.TextPrimary
    )

    val Heading2: TextStyle get() = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 20.sp,
        lineHeight = 28.sp,
        color = FDColors.TextPrimary
    )

    val Heading3: TextStyle get() = TextStyle(
        fontWeight = FontWeight.Medium,   fontSize = 16.sp,
        lineHeight = 22.sp,
        color = FDColors.TextPrimary
    )

    val Body: TextStyle get() = TextStyle(
        fontWeight = FontWeight.Normal,   fontSize = 13.sp,
        lineHeight = 18.sp,
        color = FDColors.TextPrimary
    )

    val BodySmall: TextStyle get() = TextStyle(
        fontWeight = FontWeight.Normal,   fontSize = 11.sp,
        lineHeight = 16.sp,
        color = FDColors.TextSecondary
    )

    val Caption: TextStyle get() = TextStyle(
        fontWeight = FontWeight.Medium,   fontSize = 12.sp,
        lineHeight = 18.sp,  letterSpacing = 0.3.sp,
        color = FDColors.TextTertiary
    )

    val Label: TextStyle get() = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
        lineHeight = 16.sp,  letterSpacing = 0.8.sp,
        color = FDColors.TextTertiary
    )

    val Numeric: TextStyle get() = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
        lineHeight = 24.sp,
        color = FDColors.TextPrimary
    )

    val NumericLg: TextStyle get() = TextStyle(
        fontWeight = FontWeight.Bold,     fontSize = 28.sp,
        lineHeight = 34.sp,
        color = FDColors.TextPrimary
    )
}

// ──”€──”€──”€ ESPACIADO ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€

object FDSpacing {
    val xs      = 4.dp
    val sm      = 8.dp
    val md      = 10.dp
    val lg      = 12.dp
    val xl      = 16.dp
    val xxl     = 24.dp
    val xxxl    = 32.dp
    val screenH = 16.dp
    val screenV = 12.dp
}

// ──”€──”€──”€ TAMAÑOS ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€

object FDSizes {
    val inputHeight       = 52.dp
    val buttonHeight      = 48.dp
    val buttonHeightSm    = 38.dp
    val buttonHeightLg    = 56.dp

    val iconSm   = 18.dp
    val iconMd   = 22.dp
    val iconLg   = 28.dp
    val iconXl   = 36.dp

    val touchMin = 48.dp

    val cardPadding     = 24.dp
    val cardPaddingSm   = 16.dp
    val contentMaxWidth = 900.dp

    val sheetHandleW    = 48.dp
    val sheetHandleH    = 4.dp
    val sheetRadius     = 28.dp
}

// ──”€──”€──”€ ELEVACIí“N ──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€──”€

object FDElevation {
    val CardDefault  = 2.dp
    val CardHover    = 4.dp
    val Dropdown     = 8.dp
    val Dialog       = 16.dp
    val BottomSheet  = 12.dp
}
