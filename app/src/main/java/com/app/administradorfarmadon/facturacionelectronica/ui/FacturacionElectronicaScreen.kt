package com.app.administradorfarmadon.facturacionelectronica.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType

/**
 * Módulo de Facturación Electrónica — Esqueleto UI Adaptativo
 *
 * Diseñado para productividad Enterprise en tablet y desktop.
 * Soporta Tema Claro y Tema Oscuro con alto contraste y cero elementos apretados.
 */
@Composable
fun FacturacionElectronicaScreen(
    onVolver: (() -> Unit)? = null
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Resumen",
        "Datos del emisor",
        "SUNAT",
        "Series y numeración",
        "Comprobantes",
        "Plantillas",
        "Envíos",
        "Reglas",
        "Incidencias",
        "Historial"
    )

    val verticalScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FDColors.Background)
            .padding(16.dp)
            .verticalScroll(verticalScrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. CABECERA SUPERIOR ──────────────────────────────────────────────────
        HeaderFacturacionElectronica()

        // ── 2. TARJETAS DE ESTADO DEL SISTEMA (5 Tarjetas) ────────────────────────
        BarraEstadoSistema()

        // ── 3. NAVEGACIÓN POR PESTAÑAS Y ACCIONES RÁPIDAS ────────────────────────
        BarraNavegacionPestanas(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it }
        )

        // ── 4. CONTENIDO PRINCIPAL SEGÚN PESTAÑA SELECCIONADA ────────────────────
        when (selectedTabIndex) {
            0 -> ContenidoResumen(onIrAPlantillas = { selectedTabIndex = 5 })
            1 -> ContenidoDatosEmisor()
            2 -> ContenidoConfiguracionSunat()
            3 -> ContenidoSeriesNumeracion()
            4 -> ContenidoComprobantes()
            5 -> ContenidoPlantillas()
            6 -> ContenidoEnvios()
            7 -> ContenidoReglas()
            8 -> ContenidoIncidencias()
            9 -> ContenidoHistorial()
            else -> ContenidoResumen(onIrAPlantillas = { selectedTabIndex = 5 })
        }
    }
}

// =========================================================================================
// 1. COMPONENTES DE CABECERA Y ESTADO
// =========================================================================================

@Composable
private fun HeaderFacturacionElectronica() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val esAncho = maxWidth >= 800.dp

        if (esAncho) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoCabecera()
                AccionesCabecera()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCabecera()
                AccionesCabecera()
            }
        }
    }
}

@Composable
private fun InfoCabecera() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Facturación Electrónica",
            style = FDType.Heading1.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
            color = FDColors.TextPrimary
        )
        Text(
            text = "Configura, controla y gestiona toda tu facturación electrónica",
            style = FDType.BodySmall,
            color = FDColors.TextSecondary
        )
    }
}

@Composable
private fun AccionesCabecera() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón ¿Necesitas ayuda?
        OutlinedButton(
            onClick = { },
            shape = FDShapes.Medium,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = FDColors.TextPrimary
            ),
            border = BorderStroke(1.dp, FDColors.BorderStrong),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = FDColors.TextSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "¿Necesitas ayuda?",
                style = FDType.Caption.copy(fontWeight = FontWeight.Medium),
                color = FDColors.TextPrimary
            )
        }

        // Notificación con Campana
        Box {
            IconButton(
                onClick = { },
                modifier = Modifier
                    .size(36.dp)
                    .background(FDColors.SurfaceElevated, CircleShape)
                    .border(1.dp, FDColors.Border, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notificaciones",
                    tint = FDColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(16.dp)
                    .background(Color(0xFFEF4444), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "3",
                    style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        // Perfil de Administrador
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(FDColors.SurfaceElevated, FDShapes.Full)
                .border(1.dp, FDColors.Border, FDShapes.Full)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF3B82F6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AD",
                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = Color.White
                )
            }
            Column {
                Text(
                    text = "Administrador",
                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = "Administrador",
                    style = FDType.Caption.copy(fontSize = 9.5.sp),
                    color = FDColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun BarraEstadoSistema() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val verdeTexto = if (FDColors.isDark) Color(0xFF10B981) else Color(0xFF059669)
        val verdeFondo = if (FDColors.isDark) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFFD1FAE5)

        val azulTexto = if (FDColors.isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
        val azulFondo = if (FDColors.isDark) Color(0xFF3B82F6).copy(alpha = 0.18f) else Color(0xFFDBEAFE)

        val moradoTexto = if (FDColors.isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
        val moradoFondo = if (FDColors.isDark) Color(0xFFA855F7).copy(alpha = 0.18f) else Color(0xFFF3E8FF)

        CardEstadoItem(
            modifier = Modifier.width(230.dp),
            icon = Icons.Default.CheckCircle,
            iconTint = verdeTexto,
            iconBg = verdeFondo,
            titulo = "Estado del sistema",
            tagText = "Operativo",
            tagBg = verdeFondo,
            tagTextColor = verdeTexto,
            subtitulo = "Todo funcionando correctamente"
        )

        CardEstadoItem(
            modifier = Modifier.width(230.dp),
            icon = Icons.Default.Wifi,
            iconTint = verdeTexto,
            iconBg = verdeFondo,
            titulo = "Conexión SUNAT",
            tagText = "Conectado",
            tagBg = verdeFondo,
            tagTextColor = verdeTexto,
            subtitulo = "Última prueba: Hoy 10:23 a.m."
        )

        CardEstadoItem(
            modifier = Modifier.width(240.dp),
            icon = Icons.Default.VerifiedUser,
            iconTint = verdeTexto,
            iconBg = verdeFondo,
            titulo = "Certificado digital",
            tagText = "Vigente",
            tagBg = verdeFondo,
            tagTextColor = verdeTexto,
            subtitulo = "Vence: 24/10/2027 (356 días)"
        )

        CardEstadoItem(
            modifier = Modifier.width(230.dp),
            icon = Icons.Default.Layers,
            iconTint = azulTexto,
            iconBg = azulFondo,
            titulo = "Ambiente",
            tagText = "Producción",
            tagBg = azulFondo,
            tagTextColor = azulTexto,
            subtitulo = "Operando en ambiente real"
        )

        CardEstadoItem(
            modifier = Modifier.width(240.dp),
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            iconTint = moradoTexto,
            iconBg = moradoFondo,
            titulo = "Último comprobante",
            tagText = "B001-0003482",
            tagBg = moradoFondo,
            tagTextColor = moradoTexto,
            subtitulo = "31/05/2026 10:28 a.m."
        )
    }
}

@Composable
private fun CardEstadoItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    titulo: String,
    tagText: String,
    tagBg: Color,
    tagTextColor: Color,
    subtitulo: String
) {
    Surface(
        modifier = modifier,
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = titulo,
                    style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                    color = FDColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .background(tagBg, FDShapes.Full)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tagText,
                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = tagTextColor,
                        maxLines = 1
                    )
                }
                Text(
                    text = subtitulo,
                    style = FDType.Caption.copy(fontSize = 10.sp),
                    color = FDColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BarraNavegacionPestanas(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val esEspacioso = maxWidth >= 900.dp

        if (esEspacioso) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FDColors.SurfaceElevated, FDShapes.Medium)
                    .border(1.dp, FDColors.Border, FDShapes.Medium)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PestanasScrollable(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = onTabSelected,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BotonAccionesRapidas()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FDColors.SurfaceElevated, FDShapes.Medium)
                        .border(1.dp, FDColors.Border, FDShapes.Medium)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    PestanasScrollable(
                        tabs = tabs,
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = onTabSelected,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(modifier = Modifier.align(Alignment.End)) {
                    BotonAccionesRapidas()
                }
            }
        }
    }
}

@Composable
private fun PestanasScrollable(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedTabIndex
            Box(
                modifier = Modifier
                    .clip(FDShapes.Small)
                    .background(
                        if (isSelected) FDColors.Primary.copy(alpha = 0.15f) else Color.Transparent
                    )
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = FDType.Caption.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = if (isSelected) FDColors.PrimaryText else FDColors.TextSecondary
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(2.dp)
                                .background(FDColors.Primary, FDShapes.Full)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonAccionesRapidas() {
    Button(
        onClick = { },
        shape = FDShapes.Medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF6366F1),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.FlashOn,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Acciones rápidas",
            style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}

// =========================================================================================
// 2. CONTENIDO PESTAÑA: RESUMEN (PANEL EJECUTIVO ADAPTATIVO)
// =========================================================================================

@Composable
private fun ContenidoResumen(onIrAPlantillas: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val esPantallaAncha = maxWidth >= 1050.dp

        if (esPantallaAncha) {
            // Distribución en 2 Columnas (65% / 35%)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(0.65f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SeccionResumenDia()
                    SeccionComprobantesRecientes()
                    BannerInformativoPlantillas(onIrAPlantillas = onIrAPlantillas)
                }

                Column(
                    modifier = Modifier.weight(0.35f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CardAlertasImportantes()
                    CardPruebaConexionSunat()
                    CardSeriesNumeracionResumen()
                }
            }
        } else {
            // Distribución vertical limpia (Cada panel a ancho completo)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SeccionResumenDia()
                SeccionComprobantesRecientes()
                BannerInformativoPlantillas(onIrAPlantillas = onIrAPlantillas)
                CardAlertasImportantes()
                CardPruebaConexionSunat()
                CardSeriesNumeracionResumen()
            }
        }
    }
}

// ── SUB-COMPONENTES RESUMEN ──────────────────────────────────────────────────────────────

@Composable
private fun SeccionResumenDia() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header con selector de fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Resumen del día",
                    style = FDType.Heading3.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = FDColors.TextPrimary
                )

                Surface(
                    shape = FDShapes.Small,
                    color = FDColors.SurfaceHover,
                    border = BorderStroke(1.dp, FDColors.Border)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Hoy, 31 de mayo",
                            style = FDType.Caption.copy(fontSize = 11.sp),
                            color = FDColors.TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = FDColors.TextSecondary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = FDColors.TextSecondary
                        )
                    }
                }
            }

            // 5 Tarjetas de Métricas en Row Scrollable Cómodo
            val azulMonto = if (FDColors.isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
            val verdeMonto = if (FDColors.isDark) Color(0xFF34D399) else Color(0xFF059669)
            val amarilloMonto = if (FDColors.isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
            val rojoMonto = if (FDColors.isDark) Color(0xFFF87171) else Color(0xFFDC2626)
            val moradoMonto = if (FDColors.isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCardItem("Facturas", "24", "S/ 8,450.00", azulMonto)
                MetricCardItem("Boletas", "58", "S/ 2,135.00", verdeMonto)
                MetricCardItem("Notas de crédito", "02", "S/ 120.00", amarilloMonto)
                MetricCardItem("Notas de débito", "01", "S/ 45.00", rojoMonto)
                MetricCardItem("Pendientes", "03", "S/ 120.00", moradoMonto)
            }

            // Gráficos Adaptativos
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val esAnchoGrafico = maxWidth >= 680.dp

                if (esAnchoGrafico) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CardGraficoLineas(modifier = Modifier.weight(0.6f))
                        CardGraficoDona(modifier = Modifier.weight(0.4f))
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CardGraficoLineas(modifier = Modifier.fillMaxWidth())
                        CardGraficoDona(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCardItem(
    titulo: String,
    cantidad: String,
    monto: String,
    montoColor: Color
) {
    Surface(
        modifier = Modifier.width(140.dp),
        shape = FDShapes.Small,
        color = FDColors.SurfaceHover,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = titulo,
                style = FDType.Caption.copy(fontSize = 11.sp),
                color = FDColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = cantidad,
                style = FDType.NumericLg.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = FDColors.TextPrimary
            )
            Text(
                text = monto,
                style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = montoColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CardGraficoLineas(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = FDShapes.Small,
        color = FDColors.SurfaceHover,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Comprobantes emitidos (últimos 7 días)",
                style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = FDColors.TextPrimary
            )

            // Canvas de Gráfico de Tendencias
            val grillaColor = if (FDColors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val w = size.width
                val h = size.height

                for (i in 1..3) {
                    val y = h * (i / 4f)
                    drawLine(
                        color = grillaColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // Facturas (Azul)
                val p1 = Path().apply {
                    moveTo(0f, h * 0.7f)
                    lineTo(w * 0.16f, h * 0.5f)
                    lineTo(w * 0.33f, h * 0.35f)
                    lineTo(w * 0.50f, h * 0.25f)
                    lineTo(w * 0.66f, h * 0.45f)
                    lineTo(w * 0.83f, h * 0.30f)
                    lineTo(w, h * 0.20f)
                }
                drawPath(path = p1, color = Color(0xFF3B82F6), style = Stroke(width = 3f, cap = StrokeCap.Round))

                // Boletas (Verde)
                val p2 = Path().apply {
                    moveTo(0f, h * 0.85f)
                    lineTo(w * 0.16f, h * 0.70f)
                    lineTo(w * 0.33f, h * 0.60f)
                    lineTo(w * 0.50f, h * 0.45f)
                    lineTo(w * 0.66f, h * 0.55f)
                    lineTo(w * 0.83f, h * 0.50f)
                    lineTo(w, h * 0.38f)
                }
                drawPath(path = p2, color = Color(0xFF10B981), style = Stroke(width = 3f, cap = StrokeCap.Round))

                // Notas crédito (Amarillo)
                val p3 = Path().apply {
                    moveTo(0f, h * 0.92f)
                    lineTo(w * 0.16f, h * 0.88f)
                    lineTo(w * 0.33f, h * 0.85f)
                    lineTo(w * 0.50f, h * 0.78f)
                    lineTo(w * 0.66f, h * 0.82f)
                    lineTo(w * 0.83f, h * 0.80f)
                    lineTo(w, h * 0.75f)
                }
                drawPath(path = p3, color = Color(0xFFF59E0B), style = Stroke(width = 2f, cap = StrokeCap.Round))

                // Notas débito (Rojo)
                val p4 = Path().apply {
                    moveTo(0f, h * 0.98f)
                    lineTo(w * 0.16f, h * 0.96f)
                    lineTo(w * 0.33f, h * 0.95f)
                    lineTo(w * 0.50f, h * 0.92f)
                    lineTo(w * 0.66f, h * 0.94f)
                    lineTo(w * 0.83f, h * 0.93f)
                    lineTo(w, h * 0.92f)
                }
                drawPath(path = p4, color = Color(0xFFEF4444), style = Stroke(width = 2f, cap = StrokeCap.Round))
            }

            // Fechas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("25 may", "26 may", "27 may", "28 may", "29 may", "30 may", "31 may").forEach { dia ->
                    Text(
                        text = dia,
                        style = FDType.Caption.copy(fontSize = 9.sp),
                        color = FDColors.TextTertiary
                    )
                }
            }

            // Leyendas de colores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LeyendaChartItem("Facturas", Color(0xFF3B82F6))
                LeyendaChartItem("Boletas", Color(0xFF10B981))
                LeyendaChartItem("Notas crédito", Color(0xFFF59E0B))
                LeyendaChartItem("Notas débito", Color(0xFFEF4444))
            }
        }
    }
}

@Composable
private fun CardGraficoDona(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = FDShapes.Small,
        color = FDColors.SurfaceHover,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Estado de comprobantes",
                style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = FDColors.TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 18f
                        val arcSize = Size(size.width - strokeW, size.height - strokeW)
                        val offset = Offset(strokeW / 2, strokeW / 2)

                        drawArc(
                            color = Color(0xFF10B981),
                            startAngle = -90f,
                            sweepAngle = 338f,
                            useCenter = false,
                            topLeft = offset,
                            size = arcSize,
                            style = Stroke(width = strokeW)
                        )
                        drawArc(
                            color = Color(0xFFF59E0B),
                            startAngle = 248f,
                            sweepAngle = 11f,
                            useCenter = false,
                            topLeft = offset,
                            size = arcSize,
                            style = Stroke(width = strokeW)
                        )
                        drawArc(
                            color = Color(0xFFEF4444),
                            startAngle = 260f,
                            sweepAngle = 7f,
                            useCenter = false,
                            topLeft = offset,
                            size = arcSize,
                            style = Stroke(width = strokeW)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "85",
                            style = FDType.Heading2.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            text = "Total",
                            style = FDType.Caption.copy(fontSize = 9.sp),
                            color = FDColors.TextSecondary
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ItemEstadoDonut("Aceptados", "80 (94%)", Color(0xFF10B981))
                    ItemEstadoDonut("Pendientes", "3 (3%)", Color(0xFFF59E0B))
                    ItemEstadoDonut("Rechazados", "2 (2%)", Color(0xFFEF4444))
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            val enlaceColor = if (FDColors.isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
            Text(
                text = "Ver todos los comprobantes →",
                style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = enlaceColor,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { }
            )
        }
    }
}

@Composable
private fun LeyendaChartItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = FDType.Caption.copy(fontSize = 9.sp),
            color = FDColors.TextSecondary
        )
    }
}

@Composable
private fun ItemEstadoDonut(label: String, valor: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = FDType.Caption.copy(fontSize = 10.sp),
            color = FDColors.TextSecondary
        )
        Text(
            text = valor,
            style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = FDColors.TextPrimary
        )
    }
}

@Composable
private fun SeccionComprobantesRecientes() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val enlaceColor = if (FDColors.isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comprobantes recientes",
                    style = FDType.Heading3.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = "Ver todas",
                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = enlaceColor,
                    modifier = Modifier.clickable { }
                )
            }

            // TABLA DE COMPROBANTES CON SCROLL HORIZONTAL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.width(760.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // Cabecera Tabla
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FDColors.SurfaceHover, FDShapes.Small)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tipo", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.14f))
                        Text("Número", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.18f))
                        Text("Cliente", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.26f))
                        Text("Fecha", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.18f))
                        Text("Total", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.12f))
                        Text("Estado SUNAT", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.14f))
                        Text("Acciones", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.12f))
                    }

                    HorizontalDivider(color = FDColors.Border, thickness = 1.dp)

                    val verdeText = if (FDColors.isDark) Color(0xFF10B981) else Color(0xFF059669)
                    val azulText = if (FDColors.isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
                    val amarilloText = if (FDColors.isDark) Color(0xFFF59E0B) else Color(0xFFD97706)

                    FilaComprobanteItem("Boleta", verdeText, "B001-0003482", "JUAN PÉREZ GARCÍA", "31/05/2026 10:28", "S/ 35.50", "Aceptado", verdeText)
                    FilaComprobanteItem("Factura", azulText, "F001-0001245", "FARMACIA SALUD SAC", "31/05/2026 10:23", "S/ 820.00", "Aceptado", verdeText)
                    FilaComprobanteItem("Boleta", verdeText, "B001-0003481", "MARÍA LÓPEZ RAMOS", "31/05/2026 10:18", "S/ 18.00", "Aceptado", verdeText)
                    FilaComprobanteItem("Nota crédito", amarilloText, "FC01-0000018", "FARMACIA SALUD SAC", "31/05/2026 09:40", "S/ 120.00", "Aceptado", verdeText)
                    FilaComprobanteItem("Factura", azulText, "F001-0001244", "DROGUERÍA LIMA SAC", "31/05/2026 09:30", "S/ 1,250.00", "Pendiente", amarilloText)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ver todos los comprobantes →",
                style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = enlaceColor,
                modifier = Modifier.clickable { }
            )
        }
    }
}

@Composable
private fun FilaComprobanteItem(
    tipo: String,
    tipoColor: Color,
    numero: String,
    cliente: String,
    fecha: String,
    total: String,
    estadoSunat: String,
    estadoColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tipo Badge
        Box(
            modifier = Modifier
                .weight(0.14f)
                .wrapContentWidth(Alignment.Start)
        ) {
            Box(
                modifier = Modifier
                    .background(tipoColor.copy(alpha = 0.15f), FDShapes.Small)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tipo,
                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = tipoColor
                )
            }
        }

        Text(numero, style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = FDColors.TextPrimary, modifier = Modifier.weight(0.18f))
        Text(cliente, style = FDType.Caption.copy(fontSize = 10.5.sp), color = FDColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(0.26f))
        Text(fecha, style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.18f))
        Text(total, style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = FDColors.TextPrimary, modifier = Modifier.weight(0.12f))

        // Estado SUNAT Badge
        Row(
            modifier = Modifier.weight(0.14f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(estadoColor, CircleShape)
            )
            Text(
                text = estadoSunat,
                style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                color = estadoColor
            )
        }

        // Iconos de Acción
        Row(
            modifier = Modifier.weight(0.12f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Visibility, contentDescription = "Ver", tint = FDColors.TextSecondary, modifier = Modifier.size(14.dp).clickable { })
            Icon(Icons.Default.Download, contentDescription = "Descargar", tint = FDColors.TextSecondary, modifier = Modifier.size(14.dp).clickable { })
            Icon(Icons.Default.Email, contentDescription = "Enviar", tint = FDColors.TextSecondary, modifier = Modifier.size(14.dp).clickable { })
            Icon(Icons.Default.MoreVert, contentDescription = "Más", tint = FDColors.TextSecondary, modifier = Modifier.size(14.dp).clickable { })
        }
    }
}

@Composable
private fun BannerInformativoPlantillas(onIrAPlantillas: () -> Unit) {
    val fondoBanner = if (FDColors.isDark) Color(0xFF1E1B4B).copy(alpha = 0.6f) else Color(0xFFEEF2FF)
    val bordeBanner = if (FDColors.isDark) Color(0xFF4338CA).copy(alpha = 0.5f) else Color(0xFFC7D2FE)
    val tituloColor = if (FDColors.isDark) Color.White else Color(0xFF1E1B4B)
    val textoColor = if (FDColors.isDark) Color(0xFFC7D2FE) else Color(0xFF3730A3)
    val enlaceColor = if (FDColors.isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = fondoBanner,
        border = BorderStroke(1.dp, bordeBanner)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF6366F1).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "¿Sabías que?",
                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = tituloColor
                    )
                    Text(
                        text = "Puedes personalizar el pie de tus comprobantes y el mensaje del correo en la sección de Plantillas.",
                        style = FDType.Caption.copy(fontSize = 11.sp),
                        color = textoColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Ir a Plantillas →",
                style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = enlaceColor,
                modifier = Modifier.clickable { onIrAPlantillas() }
            )
        }
    }
}

// ── SUB-COMPONENTES COLUMNA DERECHA ───────────────────────────────────────────────────────

@Composable
private fun CardAlertasImportantes() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val enlaceColor = if (FDColors.isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alertas importantes",
                    style = FDType.Heading3.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = "Ver todas",
                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = enlaceColor,
                    modifier = Modifier.clickable { }
                )
            }

            val amarilloVal = if (FDColors.isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
            val azulVal = if (FDColors.isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
            val rojoVal = if (FDColors.isDark) Color(0xFFEF4444) else Color(0xFFDC2626)

            ItemAlertaImportante(
                icon = Icons.Default.Warning,
                iconTint = amarilloVal,
                iconBg = amarilloVal.copy(alpha = 0.15f),
                titulo = "Certificado digital vence en 356 días",
                subtitulo = "Renueva tu certificado para evitar interrupciones."
            )

            ItemAlertaImportante(
                icon = Icons.Default.Info,
                iconTint = azulVal,
                iconBg = azulVal.copy(alpha = 0.15f),
                titulo = "3 comprobantes pendientes de envío",
                subtitulo = "Tienes comprobantes sin enviar a SUNAT."
            )

            ItemAlertaImportante(
                icon = Icons.Default.ErrorOutline,
                iconTint = rojoVal,
                iconBg = rojoVal.copy(alpha = 0.15f),
                titulo = "2 comprobantes rechazados",
                subtitulo = "Revisa y corrige para volver a enviar."
            )
        }
    }
}

@Composable
private fun ItemAlertaImportante(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    titulo: String,
    subtitulo: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Small,
        color = FDColors.SurfaceHover,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Row(
            modifier = Modifier
                .clickable { }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(titulo, style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = FDColors.TextPrimary)
                Text(subtitulo, style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.TextSecondary)
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = FDColors.TextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun CardPruebaConexionSunat() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val verdeVal = if (FDColors.isDark) Color(0xFF10B981) else Color(0xFF059669)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prueba de conexión SUNAT",
                    style = FDType.Heading3.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = FDColors.TextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Última prueba: Hoy 10:23 a.m.", style = FDType.Caption.copy(fontSize = 9.sp), color = FDColors.TextTertiary)
                    Box(
                        modifier = Modifier
                            .background(verdeVal.copy(alpha = 0.15f), FDShapes.Full)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Conectado", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = verdeVal)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FDColors.SurfaceHover, FDShapes.Small)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ItemCheckSunat("Autenticación")
                ItemCheckSunat("Envío de comprobante")
                ItemCheckSunat("Consulta de comprobante")
                ItemCheckSunat("Recepción de CDR")
            }

            val botonBorde = if (FDColors.isDark) Color(0xFF6366F1) else Color(0xFF4F46E5)
            val botonTexto = if (FDColors.isDark) Color(0xFF818CF8) else Color(0xFF4338CA)

            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                shape = FDShapes.Small,
                border = BorderStroke(1.dp, botonBorde),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = botonTexto)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Probar nuevamente", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
            }
        }
    }
}

@Composable
private fun ItemCheckSunat(label: String) {
    val verdeVal = if (FDColors.isDark) Color(0xFF10B981) else Color(0xFF059669)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = FDType.Caption.copy(fontSize = 11.sp), color = FDColors.TextSecondary)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = verdeVal, modifier = Modifier.size(12.dp))
            Text("OK", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = verdeVal)
        }
    }
}

@Composable
private fun CardSeriesNumeracionResumen() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val enlaceColor = if (FDColors.isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Series y numeración",
                    style = FDType.Heading3.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = "Ver todas",
                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = enlaceColor,
                    modifier = Modifier.clickable { }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.width(360.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FDColors.SurfaceHover, FDShapes.Small)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("Comprobante", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.4f))
                        Text("Serie", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.2f))
                        Text("Próximo número", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.25f))
                        Text("Estado", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.15f))
                    }

                    FilaSerieResumen("Factura electrónica", "F001", "00001245")
                    FilaSerieResumen("Boleta electrónica", "B001", "00003482")
                    FilaSerieResumen("Nota crédito electrónica", "FC01", "00000128")
                    FilaSerieResumen("Nota débito electrónica", "FD01", "00000003")
                    FilaSerieResumen("Guía de remisión", "GR01", "00000015")
                }
            }

            val botonBorde = if (FDColors.isDark) Color(0xFF6366F1) else Color(0xFF4F46E5)
            val botonTexto = if (FDColors.isDark) Color(0xFF818CF8) else Color(0xFF4338CA)

            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                shape = FDShapes.Small,
                border = BorderStroke(1.dp, botonBorde),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = botonTexto)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nueva serie", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
            }
        }
    }
}

@Composable
private fun FilaSerieResumen(nombre: String, serie: String, proximoNum: String) {
    val verdeVal = if (FDColors.isDark) Color(0xFF10B981) else Color(0xFF059669)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(nombre, style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.TextPrimary, modifier = Modifier.weight(0.4f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(serie, style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = FDColors.TextPrimary, modifier = Modifier.weight(0.2f))
        Text(proximoNum, style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.TextSecondary, modifier = Modifier.weight(0.25f))
        Box(
            modifier = Modifier
                .weight(0.15f)
                .background(verdeVal.copy(alpha = 0.15f), FDShapes.Small)
                .padding(horizontal = 4.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Activa", style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = verdeVal)
        }
    }
}

// =========================================================================================
// 3. CONTENIDOS DE OTRAS PESTAÑAS (SKELETONS LIMPIOS Y FUNCIONALES)
// =========================================================================================

@Composable
private fun ContenidoDatosEmisor() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Datos Fiscales del Emisor", style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val esAncho = maxWidth >= 600.dp
                if (esAncho) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = "", onValueChange = {},
                            label = { Text("RUC") },
                            placeholder = { Text("RUC de la empresa") },
                            modifier = Modifier.weight(1f),
                            shape = FDShapes.Small
                        )
                        OutlinedTextField(
                            value = "", onValueChange = {},
                            label = { Text("Razón Social") },
                            placeholder = { Text("Razón social registrada en SUNAT") },
                            modifier = Modifier.weight(2f),
                            shape = FDShapes.Small
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = "", onValueChange = {},
                            label = { Text("RUC") },
                            placeholder = { Text("RUC de la empresa") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = FDShapes.Small
                        )
                        OutlinedTextField(
                            value = "", onValueChange = {},
                            label = { Text("Razón Social") },
                            placeholder = { Text("Razón social registrada en SUNAT") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = FDShapes.Small
                        )
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val esAncho = maxWidth >= 600.dp
                if (esAncho) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = "", onValueChange = {},
                            label = { Text("Nombre Comercial") },
                            modifier = Modifier.weight(1f),
                            shape = FDShapes.Small
                        )
                        OutlinedTextField(
                            value = "", onValueChange = {},
                            label = { Text("Dirección Fiscal") },
                            modifier = Modifier.weight(1f),
                            shape = FDShapes.Small
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = "", onValueChange = {},
                            label = { Text("Nombre Comercial") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = FDShapes.Small
                        )
                        OutlinedTextField(
                            value = "", onValueChange = {},
                            label = { Text("Dirección Fiscal") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = FDShapes.Small
                        )
                    }
                }
            }

            Button(
                onClick = {},
                shape = FDShapes.Small,
                colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText)
            ) {
                Text("Guardar Datos del Emisor", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun ContenidoConfiguracionSunat() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Parámetros de Conexión SUNAT / OSE", style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val esAncho = maxWidth >= 600.dp
                if (esAncho) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Usuario SOL") }, modifier = Modifier.weight(1f), shape = FDShapes.Small)
                        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Clave SOL") }, modifier = Modifier.weight(1f), shape = FDShapes.Small)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Usuario SOL") }, modifier = Modifier.fillMaxWidth(), shape = FDShapes.Small)
                        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Clave SOL") }, modifier = Modifier.fillMaxWidth(), shape = FDShapes.Small)
                    }
                }
            }

            OutlinedTextField(value = "", onValueChange = {}, label = { Text("URL Endpoint OSE / SUNAT") }, modifier = Modifier.fillMaxWidth(), shape = FDShapes.Small)

            Button(
                onClick = {},
                shape = FDShapes.Small,
                colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText)
            ) {
                Text("Guardar Configuración SUNAT", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun ContenidoSeriesNumeracion() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Series y Numeración de Comprobantes", style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
            CardSeriesNumeracionResumen()
        }
    }
}

@Composable
private fun ContenidoComprobantes() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Listado General de Comprobantes Emitidos", style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
            SeccionComprobantesRecientes()
        }
    }
}

@Composable
private fun ContenidoPlantillas() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Plantillas de Impresión y Correo", style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
            Text("Personaliza el pie de página, logo, formato de impresión ticket/A4 y el cuerpo de mensaje enviado por email.", style = FDType.BodySmall, color = FDColors.TextSecondary)
        }
    }
}

@Composable
private fun ContenidoEnvios() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Monitor de Envíos y Resúmenes Diarios", style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
            Text("Muestra la cola de transmisión hacia SUNAT/OSE y el envío de resúmenes diarios de boletas.", style = FDType.BodySmall, color = FDColors.TextSecondary)
        }
    }
}

@Composable
private fun ContenidoReglas() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Reglas de Negocio de Facturación", style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
            Text("Configura límites de monto para DNI, reglas de IGV, exoneraciones y detracciones.", style = FDType.BodySmall, color = FDColors.TextSecondary)
        }
    }
}

@Composable
private fun ContenidoIncidencias() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Registro de Incidencias y Observaciones SUNAT", style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
            Text("Detalle de tickets rechazados o con observaciones para rápida rectificación.", style = FDType.BodySmall, color = FDColors.TextSecondary)
        }
    }
}

@Composable
private fun ContenidoHistorial() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FDShapes.Medium,
        color = FDColors.SurfaceElevated,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Historial de Auditoría de Facturación", style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
            Text("Bitácora completa de eventos, cambios de estado y sincronización de comprobantes.", style = FDType.BodySmall, color = FDColors.TextSecondary)
        }
    }
}
