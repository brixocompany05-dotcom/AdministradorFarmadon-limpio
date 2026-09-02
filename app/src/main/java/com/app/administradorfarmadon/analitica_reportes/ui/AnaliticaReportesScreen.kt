package com.app.administradorfarmadon.analitica_reportes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.analitica_reportes.ui.componentes.PestanaAnalitica
import com.app.administradorfarmadon.analitica_reportes.ui.componentes.PestanaReportes
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium

/**
 * PANTALLA PRINCIPAL DE ANALÍTICA Y REPORTES — Enterprise SaaS 2026.
 *
 * Máxima comodidad visual, espacios amplios (breathing room),
 * arquitectura de alta claridad y navegación intuitiva.
 */
@Composable
fun AnaliticaReportesScreen(
    pestanaInicial: String = "ANALITICA",
    onVolver: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    var moduloSeleccionado by remember { mutableStateOf(pestanaInicial) }
    var periodoSeleccionado by remember { mutableStateOf("Este Mes (Agosto 2026)") }
    var selectorPeriodoAbierto by remember { mutableStateOf(false) }

    val opcionesPeriodo = listOf(
        "Hoy (21 Ago 2026)",
        "Esta Semana",
        "Este Mes (Agosto 2026)",
        "Mes Anterior (Julio 2026)",
        "Año 2026",
        "Rango Personalizado..."
    )

    BackHandler(enabled = true) {
        onVolver()
    }

    Scaffold(
        containerColor = FDColors.Background,
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── HEADER PRINCIPAL CON ESPACIO AMPLIO Y ELEGANTE ────────────────────
            Surface(
                color = FDColors.Surface,
                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Título con Icono y Descripción Clara
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        color = FDColors.SurfaceElevated,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = FDColors.TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Analítica & Reportes",
                                    style = FDType.Heading1.copy(
                                        fontSize = 19.sp,
                                        fontFamily = InterPremium,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = FDColors.TextPrimary
                                )
                                Text(
                                    text = "Métricas en tiempo real, análisis de rentabilidad y exportación de documentos",
                                    style = FDType.BodySmall.copy(
                                        fontSize = 12.5.sp,
                                        fontFamily = InterPremium
                                    ),
                                    color = FDColors.TextSecondary
                                )
                            }
                        }

                        // Selector de Período Amplio y Cómodo
                        Box {
                            Surface(
                                color = FDColors.SurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, FDColors.Border),
                                modifier = Modifier.clickable { selectorPeriodoAbierto = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = FDColors.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = periodoSeleccionado,
                                        style = FDType.Label.copy(
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = InterPremium
                                        ),
                                        color = FDColors.TextPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = FDColors.TextTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = selectorPeriodoAbierto,
                                onDismissRequest = { selectorPeriodoAbierto = false },
                                modifier = Modifier.background(FDColors.SurfaceElevated)
                            ) {
                                opcionesPeriodo.forEach { opcion ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = opcion,
                                                style = FDType.Body.copy(fontSize = 13.sp),
                                                color = if (opcion == periodoSeleccionado) FDColors.TextPrimary else FDColors.TextSecondary,
                                                fontWeight = if (opcion == periodoSeleccionado) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            periodoSeleccionado = opcion
                                            selectorPeriodoAbierto = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── PESTAÑAS PRINCIPALES CÓMODAS (Underline Tabs) ────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        TabNavegacionSaaS(
                            label = "📊 Analítica de la Farmacia",
                            subLabel = "Resumen de ventas, margen y rendimiento",
                            selected = moduloSeleccionado == "ANALITICA",
                            onClick = { moduloSeleccionado = "ANALITICA" }
                        )
                        TabNavegacionSaaS(
                            label = "📄 Centro de Reportes",
                            subLabel = "Generar y descargar reportes oficiales",
                            selected = moduloSeleccionado == "REPORTES",
                            onClick = { moduloSeleccionado = "REPORTES" }
                        )
                    }
                }
            }

            HorizontalDivider(
                color = FDColors.Border.copy(alpha = 0.35f),
                thickness = s.separatorH
            )

            // ── WORKSPACE PRINCIPAL ESPACIOSO ────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(FDColors.Background)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Crossfade(
                    targetState = moduloSeleccionado,
                    label = "AnaliticaReportesTransition"
                ) { modulo ->
                    when (modulo) {
                        "ANALITICA" -> PestanaAnalitica(
                            periodo = periodoSeleccionado,
                            s = s
                        )
                        "REPORTES" -> PestanaReportes(
                            periodoInicial = periodoSeleccionado,
                            s = s
                        )
                        else -> PestanaAnalitica(
                            periodo = periodoSeleccionado,
                            s = s
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabNavegacionSaaS(
    label: String,
    subLabel: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = FDType.Label.copy(
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = InterPremium
            ),
            color = if (selected) FDColors.TextPrimary else FDColors.TextTertiary
        )
        Text(
            text = subLabel,
            style = FDType.BodySmall.copy(
                fontSize = 11.sp,
                fontFamily = InterPremium
            ),
            color = if (selected) FDColors.TextSecondary else Color.Transparent
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(3.dp)
                .background(
                    color = if (selected) FDColors.Primary else Color.Transparent,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}
