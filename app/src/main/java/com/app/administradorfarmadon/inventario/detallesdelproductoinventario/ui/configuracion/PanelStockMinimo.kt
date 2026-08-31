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
import com.app.administradorfarmadon.inventario.compartido.logica.PerfilUnidades
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
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
    modifier: Modifier = Modifier,
    producto: MoldeProductos? = null
) {
    // Unidad coherente = contenedor con el que se cuenta y vende (Caja/Frasco/Bloque), nunca genérico
    val unidadSingular = unidadBase.trim().ifBlank { "Unidad" }
    val unidadPlural = when {
        unidadSingular.equals("Unidad", ignoreCase = true) -> "Unidades"
        unidadSingular.endsWith("s", ignoreCase = true) -> unidadSingular
        else -> "${unidadSingular}s"
    }
    // Control de unidad para fraccionables: el usuario elige si el mínimo es en Frascos, ml o L
    val esFraccionable = producto?.permiteFraccionar == true
    val unidadContenidoRaw = producto?.contenidoUnidad?.ifBlank { producto?.inventarioPerfilUnidadContenido }?.ifBlank { unidadBase } ?: unidadBase
    val factorContenido = if (producto != null) com.app.administradorfarmadon.inventario.compartido.logica.UnidadVentaHelper.factorContenido(producto.contenido, producto.presentaciones) else 1.0
    var unidadSeleccionada by remember(producto?.indice, unidadBase) { mutableStateOf(unidadBase) }
    val opcionesUnidad = remember(producto, unidadBase, unidadContenidoRaw) {
        val base = unidadBase.ifBlank { "Unidad" }
        val contenido = unidadContenidoRaw.ifBlank { base }
        val lista = mutableListOf<String>()
        lista.add(base)
        if (!contenido.equals(base, ignoreCase = true)) lista.add(contenido)
        if (contenido.equals("ml", ignoreCase = true) && lista.none { it.equals("L", true) }) lista.add("L")
        if (contenido.equals("L", ignoreCase = true) && lista.none { it.equals("ml", true) }) lista.add("ml")
        lista.distinct()
    }
    fun convertirBaseAUnidad(valorBase: Double): String {
        if (producto == null || unidadSeleccionada.equals(unidadBase, true)) {
            return if (valorBase % 1.0 == 0.0) valorBase.toInt().toString() else String.format(java.util.Locale.US, "%.2f", valorBase).trimEnd('0').trimEnd('.')
        }
        val valorEnUnidad = PerfilUnidades.normalizarA(valorBase * factorContenido, unidadBase, unidadSeleccionada)
        return if (valorEnUnidad % 1.0 == 0.0) valorEnUnidad.toInt().toString() else String.format(java.util.Locale.US, "%.2f", valorEnUnidad).trimEnd('0').trimEnd('.')
    }
    fun convertirUnidadABase(valorUnidad: Double): Double {
        if (producto == null || unidadSeleccionada.equals(unidadBase, true)) return valorUnidad
        val valorEnBaseUnidad = PerfilUnidades.normalizarA(valorUnidad, unidadSeleccionada, unidadBase)
        return valorEnBaseUnidad / factorContenido
    }
    var inputTexto by remember(stockMinimoActual, unidadSeleccionada) {
        mutableStateOf(convertirBaseAUnidad(stockMinimoActual))
    }

    // Guardado único con debounce — sin duplicar
    LaunchedEffect(inputTexto, unidadSeleccionada) {
        val numUnidad = inputTexto.toDoubleOrNull()
        if (numUnidad != null && numUnidad >= 0) {
            val numBase = convertirUnidadABase(numUnidad)
            if (kotlin.math.abs(numBase - stockMinimoActual) > 0.001) {
                delay(600)
                if (inputTexto.toDoubleOrNull() == numUnidad) {
                    onStockMinimoChange(numBase)
                }
            }
        } else if (inputTexto.isEmpty()) {
            delay(600)
            if (inputTexto.isEmpty() && stockMinimoActual != 0.0) onStockMinimoChange(0.0)
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

        // Selector de unidad para fraccionables — el usuario elige si el mínimo es en Frascos o ml/L
        if (esFraccionable && opcionesUnidad.size > 1) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Avisar en:",
                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextSecondary)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    opcionesUnidad.forEach { unidad ->
                        val seleccionado = unidadSeleccionada == unidad
                        androidx.compose.material3.FilterChip(
                            selected = seleccionado,
                            onClick = {
                                unidadSeleccionada = unidad
                                inputTexto = convertirBaseAUnidad(stockMinimoActual)
                            },
                            label = { Text(unidad, style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium)) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FDColors.Primary.copy(alpha = 0.15f),
                                selectedLabelColor = FDColors.Primary
                            )
                        )
                    }
                }
                Text(
                    text = "El mínimo se guarda en ${unidadBase} pero lo ves en ${unidadSeleccionada}. Ej: 50 ml = ${String.format(java.util.Locale.US, "%.2f", convertirUnidadABase(50.0))} ${unidadBase}",
                    style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary)
                )
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
                    val displayChip = if (esFraccionable && !unidadSeleccionada.equals(unidadBase, true)) {
                        PerfilUnidades.normalizarA(stockChip * factorContenido, unidadBase, unidadSeleccionada)
                    } else stockChip
                    val displayChipStr = if (displayChip % 1.0 == 0.0) displayChip.toInt().toString() else String.format(java.util.Locale.US, "%.2f", displayChip).trimEnd('0').trimEnd('.')
                    val unidadChip = if (esFraccionable) unidadSeleccionada else if (stockChip == 1.0) unidadSingular else unidadPlural
                    Surface(
                        color = if (isSelected) FDColors.Primary.copy(alpha = 0.15f) else FDColors.Surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isSelected) FDColors.Primary else FDColors.Border),
                        modifier = Modifier
                            .clickable {
                                inputTexto = displayChipStr
                                onStockMinimoChange(stockChip)
                            }
                            .bounceClick()
                    ) {
                        Text(
                            text = if (stockChip == 0.0) "0 (Sin alerta)" else "$displayChipStr $unidadChip",
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
            label = "Mínimo en $unidadSeleccionada *",
            value = inputTexto,
            onValueChange = { nuevo ->
                val filtrado = nuevo.replace(',', '.').filter { it.isDigit() || it == '.' }
                val partes = filtrado.split('.')
                val limpio = if (partes.size > 2) partes[0] + "." + partes.drop(1).joinToString("") else filtrado
                inputTexto = limpio.take(7)
            },
            placeholder = if (esFraccionable) "Ej: 50" else "5",
            keyboardType = KeyboardType.Decimal,
            leadingIcon = Icons.Outlined.NotificationsActive
        )
    }
}
