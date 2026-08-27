package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import kotlinx.coroutines.delay

private val CHIPS_STOCK_MINIMO = listOf(0.0, 5.0, 10.0, 15.0, 20.0, 50.0)

private data class DiagnosticoStock(
    val color: Color,
    val icon: ImageVector,
    val titulo: String,
    val descripcion: String
)

/**
 * Sub-módulo 2: Alerta Preventiva de Stock Mínimo con Diagnóstico en Vivo de Existencias.
 * Basado en reglas de negocio farmacéuticas reales (Unidad Base, Agotamiento y Umbral Crítico).
 */
@Composable
fun PanelStockMinimo(
    stockMinimoActual: Double,
    stockTotalActual: Double,
    unidadBase: String,
    onStockMinimoChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    // Unidad coherente = contenedor con el que se cuenta y vende (Caja/Frasco/Bloque), nunca genérico
    val unidadSingular = unidadBase.trim().ifBlank { "Unidad" }
    val unidadPlural = when {
        unidadSingular.equals("Unidad", ignoreCase = true) -> "Unidades"
        unidadSingular.endsWith("s", ignoreCase = true) -> unidadSingular
        else -> "${unidadSingular}s"
    }
    var inputTexto by remember(stockMinimoActual) {
        mutableStateOf(if (stockMinimoActual > 0) stockMinimoActual.toInt().toString() else "0")
    }

    LaunchedEffect(inputTexto) {
        val num = inputTexto.toDoubleOrNull()
        if (num != null && num >= 0 && num != stockMinimoActual) {
            delay(600)
            onStockMinimoChange(num)
        }
    }
    DisposableEffect(inputTexto, stockMinimoActual) {
        onDispose {
            val num = inputTexto.toDoubleOrNull()
            if (num != null && num >= 0 && num != stockMinimoActual) {
                onStockMinimoChange(num)
            }
        }
    }

    // Diagnóstico en tiempo real coherente con consumo: usa plural real (Caja->Cajas)
    val nombreStock = remember(stockTotalActual, unidadSingular, unidadPlural) {
        if (kotlin.math.abs(stockTotalActual - 1.0) < 0.0001) unidadSingular else unidadPlural
    }
    val nombreMinimo = remember(stockMinimoActual, unidadSingular, unidadPlural) {
        if (kotlin.math.abs(stockMinimoActual - 1.0) < 0.0001) unidadSingular else unidadPlural
    }
    val diagnostico = remember(stockTotalActual, stockMinimoActual, unidadSingular, unidadPlural, FDColors.isDark) {
        when {
            stockTotalActual <= 0 -> DiagnosticoStock(
                color = FDColors.Error,
                icon = Icons.Outlined.Warning,
                titulo = "AGOTADO (0 $unidadPlural)",
                descripcion = "No hay existencias. Reabastecer $unidadPlural."
            )
            stockMinimoActual > 0 && stockTotalActual <= stockMinimoActual -> DiagnosticoStock(
                color = FDColors.Warning,
                icon = Icons.Outlined.Warning,
                titulo = "CRÍTICO (${stockTotalActual.toInt()} $nombreStock)",
                descripcion = "Quedan ${stockTotalActual.toInt()} $nombreStock, mínimo es ${stockMinimoActual.toInt()} $nombreMinimo."
            )
            stockMinimoActual > 0 -> DiagnosticoStock(
                color = FDColors.Success,
                icon = Icons.Outlined.CheckCircle,
                titulo = "SALUDABLE (${stockTotalActual.toInt()} $nombreStock)",
                descripcion = "Supera el mínimo de ${stockMinimoActual.toInt()} $nombreMinimo."
            )
            else -> DiagnosticoStock(
                color = FDColors.TextSecondary,
                icon = Icons.Outlined.Info,
                titulo = "SIN ALERTA (${stockTotalActual.toInt()} $nombreStock)",
                descripcion = "Mínimo en 0. No avisa escasez."
            )
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Avisa cuando queden pocos $unidadPlural.",
            style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextSecondary)
        )

        // ── TARJETA DE DIAGNÓSTICO EN VIVO ──
        Surface(
            color = diagnostico.color.copy(alpha = 0.08f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, diagnostico.color.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = diagnostico.color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = diagnostico.icon,
                            contentDescription = null,
                            tint = diagnostico.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = diagnostico.titulo,
                        style = FDType.Heading3.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = diagnostico.color)
                    )
                    Text(
                        text = diagnostico.descripcion,
                        style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                    )
                }
            }
        }

        // Chips de selección rápida de 1 toque (Auto-guardado directo)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "ATAJOS ($unidadPlural):",
                style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextSecondary)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CHIPS_STOCK_MINIMO.forEach { stockChip ->
                    val isSelected = stockMinimoActual == stockChip
                    Surface(
                        color = if (isSelected) FDColors.Primary.copy(alpha = 0.15f) else FDColors.Surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isSelected) FDColors.Primary else FDColors.Border),
                        modifier = Modifier
                            .clickable {
                                inputTexto = stockChip.toInt().toString()
                                onStockMinimoChange(stockChip)
                            }
                            .bounceClick()
                    ) {
                        Text(
                            text = if (stockChip == 0.0) "0 (Sin alerta)" else {
                                val nom = if (stockChip == 1.0) unidadSingular else unidadPlural
                                "${stockChip.toInt()} $nom"
                            },
                            style = FDType.Caption.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) FDColors.Primary else FDColors.TextSecondary
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        ConfiguracionTextField(
            label = "Mínimo en $unidadPlural *",
            value = inputTexto,
            onValueChange = { nuevo ->
                val soloDigitos = nuevo.filter { it.isDigit() }.take(6)
                inputTexto = soloDigitos
            },
            placeholder = "5",
            keyboardType = KeyboardType.Number,
            leadingIcon = Icons.Outlined.NotificationsActive
        )
    }
}
