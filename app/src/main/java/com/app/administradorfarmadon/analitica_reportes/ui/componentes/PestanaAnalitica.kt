package com.app.administradorfarmadon.analitica_reportes.ui.componentes

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium

/**
 * PESTAÑA 1: ANALÍTICA ("¿Cómo está funcionando mi farmacia?")
 *
 * Diseño cómodo, amplio y con navegación clara por sub-secciones.
 */
@Composable
fun PestanaAnalitica(
    periodo: String,
    s: MedidaAdaptativa,
    modifier: Modifier = Modifier
) {
    var subSeccionSeleccionada by remember { mutableStateOf("RESUMEN") }

    val subSecciones = listOf(
        SubSeccionSaaS("RESUMEN", "Resumen General", Icons.Default.Dashboard),
        SubSeccionSaaS("VENTAS", "Ventas", Icons.Default.TrendingUp),
        SubSeccionSaaS("RENTABILIDAD", "Rentabilidad", Icons.Default.AttachMoney),
        SubSeccionSaaS("INVENTARIO", "Inventario", Icons.Default.Inventory2),
        SubSeccionSaaS("COMPRAS", "Compras", Icons.Default.LocalShipping),
        SubSeccionSaaS("CLIENTES", "Clientes", Icons.Default.Groups),
        SubSeccionSaaS("CAJA", "Caja & Pagos", Icons.Default.PointOfSale),
        SubSeccionSaaS("SUCURSALES", "Sucursales", Icons.Default.Storefront),
        SubSeccionSaaS("INSIGHTS", "Insights AI", Icons.Default.AutoAwesome)
    )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── SELECTOR DE SUB-SECCIÓN ACCIONABLE Y ESPACIOSO ─────────────────────────
        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                subSecciones.forEach { item ->
                    val esSel = subSeccionSeleccionada == item.clave
                    Surface(
                        color = if (esSel) FDColors.SurfaceElevated else FDColors.Surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (esSel) FDColors.BorderFocus else FDColors.Border.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.clickable { subSeccionSeleccionada = item.clave }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = item.icono,
                                contentDescription = null,
                                tint = if (esSel) FDColors.TextPrimary else FDColors.TextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = item.titulo,
                                style = FDType.Label.copy(
                                    fontSize = 12.5.sp,
                                    fontWeight = if (esSel) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = InterPremium
                                ),
                                color = if (esSel) FDColors.TextPrimary else FDColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // ── WORKSPACE DE CONTENIDO ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Crossfade(
                targetState = subSeccionSeleccionada,
                label = "AnaliticaSubSectionTransition"
            ) { sub ->
                when (sub) {
                    "RESUMEN" -> AnaliticaResumenSection(periodo = periodo, s = s)
                    "VENTAS" -> AnaliticaVentasSection(periodo = periodo, s = s)
                    "RENTABILIDAD" -> AnaliticaRentabilidadSection(periodo = periodo, s = s)
                    "INVENTARIO" -> AnaliticaInventarioSection(s = s)
                    "COMPRAS" -> AnaliticaComprasSection(periodo = periodo, s = s)
                    "CLIENTES" -> AnaliticaClientesSection(periodo = periodo, s = s)
                    "CAJA" -> AnaliticaCajaSection(periodo = periodo, s = s)
                    "SUCURSALES" -> AnaliticaSucursalesSection(s = s)
                    "INSIGHTS" -> AnaliticaInsightsSection(s = s)
                    else -> AnaliticaResumenSection(periodo = periodo, s = s)
                }
            }
        }
    }
}

private data class SubSeccionSaaS(
    val clave: String,
    val titulo: String,
    val icono: ImageVector
)
