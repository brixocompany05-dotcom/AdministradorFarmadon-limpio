package com.app.administradorfarmadon.inventario.ingresarstockproductos.ui.componentes

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.ingresarstockproductos.logica.ModoIngresoStock
import com.app.administradorfarmadon.inventario.ingresarstockproductos.logica.StockEntryState
import com.app.administradorfarmadon.inventario.ingresarstockproductos.logica.StockEntryViewModel

/**
 * Sección de Cantidades Físicas, Bonificaciones Comerciales y Costos.
 * Manejo claro de productos nuevos/consignación (Unidades directas) vs Cajas/Bultos.
 */
@Composable
fun SeccionCantidadYCosto(
    state: StockEntryState,
    viewModel: StockEntryViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Subtítulo de sección ordenada — no mezclado
        Text("Cantidad", style = FDType.Label.copy(fontSize = 10.sp, letterSpacing = 1.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.Bold))

        // Cuadro único Cantidad — envuelve selector + campos para que se entienda el orden
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = FDColors.SurfaceElevated,
            border = BorderStroke(0.5.dp, FDColors.Border)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 1. Selector flip-card premium 52dp
                val esPorEmpaque = state.modoIngreso == ModoIngresoStock.POR_EMPAQUE_MULTIPLO
                val esUnidades = state.modoIngreso == ModoIngresoStock.UNIDADES_DIRECTAS
                val sinSeleccion = state.modoIngreso == null
                val sugerencia = if (state.contenidoPorUnidad > 1) ModoIngresoStock.POR_EMPAQUE_MULTIPLO else ModoIngresoStock.UNIDADES_DIRECTAS
                FlipSelectorCard(
                    esPorEmpaque = esPorEmpaque,
                    esUnidades = esUnidades,
                    sinSeleccion = sinSeleccion,
                    sugerenciaEsBultos = sugerencia == ModoIngresoStock.POR_EMPAQUE_MULTIPLO,
                    etiquetaBultos = state.etiquetaModoMultiplo,
                    etiquetaUnidades = state.etiquetaModoDirecto,
                    onSelectBultos = { viewModel.onModoIngresoChanged(ModoIngresoStock.POR_EMPAQUE_MULTIPLO) },
                    onSelectUnidades = { viewModel.onModoIngresoChanged(ModoIngresoStock.UNIDADES_DIRECTAS) }
                )

                // 2. Inputs compactos 180dp
                if (state.modoIngreso == ModoIngresoStock.POR_EMPAQUE_MULTIPLO) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                StepperFieldCompact(
                    label = state.etiquetaCantidadBultos,
                    value = state.cantidadBultos,
                    placeholder = "5",
                    icon = Icons.Outlined.Inventory2,
                    onValueChange = { viewModel.onCantidadBultosChanged(it) },
                    onStep = { delta -> val cur = state.cantidadBultos.toIntOrNull() ?: 0; val nxt = (cur + delta).coerceAtLeast(0); viewModel.onCantidadBultosChanged(if (nxt==0) "" else nxt.toString()) },
                    modifier = Modifier.weight(1f)
                )
                StepperFieldCompact(
                    label = state.etiquetaUnidadesPorBulto,
                    value = state.unidadesPorBulto,
                    placeholder = "100",
                    icon = Icons.Outlined.Category,
                    onValueChange = { viewModel.onUnidadesPorBultoChanged(it) },
                    onStep = { delta -> val cur = state.unidadesPorBulto.toIntOrNull() ?: 0; val nxt = (cur + delta).coerceAtLeast(0); viewModel.onUnidadesPorBultoChanged(if (nxt==0) "" else nxt.toString()) },
                    modifier = Modifier.weight(1f),
                    isError = state.tieneUnidadesPorCajaInvalida
                )
                    }
                } else if (state.modoIngreso == ModoIngresoStock.UNIDADES_DIRECTAS) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        StepperFieldCompact(
                            label = state.etiquetaCantidadDirecta,
                            value = state.cantidadDirecta,
                            placeholder = "10",
                            icon = Icons.Outlined.Numbers,
                            onValueChange = { viewModel.onCantidadDirectaChanged(it) },
                            onStep = { delta -> val cur = state.cantidadDirecta.toIntOrNull() ?: 0; val nxt = (cur + delta).coerceAtLeast(0); viewModel.onCantidadDirectaChanged(if (nxt==0) "" else nxt.toString()) },
                            modifier = Modifier.width(220.dp)
                        )
                    }
                } else {
                    Surface(color = FDColors.Background, shape = RoundedCornerShape(8.dp), border = BorderStroke(0.5.dp, FDColors.Border.copy(alpha = 0.6f)), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.TouchApp, null, tint = FDColors.TextTertiary, modifier = Modifier.size(16.dp))
                            Text("Elige arriba cómo cuentas — bultos o unidades", style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary))
                        }
                    }
                }

                // 3. Bonificación — integrado sutil dentro del mismo cuadro, no estorbo
                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.CardGiftcard, null, tint = if (state.activarBonificacion) FDColors.Primary else FDColors.TextTertiary.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Text("¿Viene con regalo?", style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = FDColors.TextTertiary))
                    Text("(opcional)", style = FDType.Caption.copy(fontSize = 9.5.sp, color = FDColors.TextTertiary.copy(alpha = 0.7f)))
                        if (state.bonificacionUnidadesTotales > 0) {
                            Text("+${state.bonificacionUnidadesTotales.toInt()}", style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FDColors.Success))
                        }
                    }
                    TextButton(onClick = { viewModel.onToggleBonificacion(!state.activarBonificacion) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp), modifier = Modifier.height(28.dp)) {
                        Text(if (state.activarBonificacion) "Quitar" else "Agregar", style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = if (state.activarBonificacion) FDColors.TextTertiary else FDColors.Primary))
                    }
                }
                AnimatedVisibility(visible = state.activarBonificacion, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    EnterpriseTextField(
                        label = if (state.modoIngreso == ModoIngresoStock.POR_EMPAQUE_MULTIPLO) "Cajas de regalo *" else "Unidades gratis *",
                        placeholder = if (state.modoIngreso == ModoIngresoStock.POR_EMPAQUE_MULTIPLO) {
                            val unidPorCaja = state.unidadesPorBulto.toIntOrNull() ?: 0
                            if (unidPorCaja > 0) "Ej: 2 cajas = ${2 * unidPorCaja} und" else "Ej: 2"
                        } else "Ej: 5",
                        value = state.cantidadBonificacion,
                        leadingIcon = Icons.Outlined.AddBox,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.width(180.dp),
                        isError = state.tieneBonificacionActivadaSinCantidad,
                        onValueChange = { viewModel.onCantidadBonificacionChanged(it) }
                    )
                }
            }
        }

        // Resumen vivo — barra 6dp + texto nítido
        if (state.cantidadTotalIngresada > 0) {
            val detalleBonif = if (state.bonificacionUnidadesTotales > 0) " (${state.cantidadCompradaBase.toInt()} + ${state.bonificacionUnidadesTotales.toInt()} regalo)" else ""
            val empaqueLabel = state.empaque.ifBlank { "unidades" }
            val totalUnidades = state.cantidadTotalIngresada.toInt()
            val equivalencia = if (state.contenidoPorUnidad > 1 && state.unidadContenido.isNotBlank()) " = ${totalUnidades * state.contenidoPorUnidad} ${state.unidadContenido}" else ""
            Surface(color = FDColors.Success.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp), border = BorderStroke(0.5.dp, FDColors.Success.copy(alpha = 0.28f)), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(FDColors.Success, CircleShape))
                    Text("Total: $totalUnidades $empaqueLabel$equivalencia$detalleBonif", style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.Success, fontWeight = FontWeight.SemiBold))
                }
            }
        }

        // 4. Costo — total compacto 200dp + costo vivo texto (no card)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            EnterpriseTextField(
                label = "Total pagado *",
                placeholder = "S/ 0.00",
                value = state.costoCompraLote,
                leadingIcon = Icons.Outlined.AttachMoney,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.width(200.dp),
                onValueChange = { viewModel.onCostoCompraLoteChanged(it) }
            )

            val tieneCosto = state.costoTotalIngreso > 0
            val tieneCantidad = state.cantidadTotalIngresada > 0

            val textoResultado = when {
                tieneCosto && tieneCantidad -> "$ ${String.format(java.util.Locale.US, "%.2f", state.costoUnitarioCalculado)} / und"
                tieneCosto && !tieneCantidad -> "Falta ingresar cantidad"
                !tieneCosto && tieneCantidad -> "Ingresa el costo pagado"
                else -> "---"
            }

            val helperResultado = when {
                tieneCosto && tieneCantidad && state.bonificacionUnidadesTotales > 0 -> {
                    "✨ $${String.format(java.util.Locale.US, "%.2f", state.costoTotalIngreso)} ÷ ${state.cantidadTotalIngresada.toInt()} und (con regalo)"
                }
                tieneCosto && tieneCantidad -> {
                    "$${String.format(java.util.Locale.US, "%.2f", state.costoTotalIngreso)} ÷ ${state.cantidadTotalIngresada.toInt()} und"
                }
                tieneCosto && !tieneCantidad -> "Para dividir el costo entre las unidades"
                !tieneCosto && tieneCantidad -> "Para calcular costo de las ${state.cantidadTotalIngresada.toInt()} und"
                else -> "Cálculo automático en vivo"
            }

            // Costo por unidad — texto vivo, no card input
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Costo por unidad", style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = FDColors.TextTertiary, letterSpacing = 0.5.sp))
                if (tieneCosto && tieneCantidad) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(textoResultado, style = FDType.Body.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FDColors.Primary))
                        Icon(Icons.Outlined.CheckCircle, null, tint = FDColors.Success, modifier = Modifier.size(16.dp))
                    }
                    Text(helperResultado, style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextSecondary))
                } else {
                    Text(textoResultado, style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = FDColors.TextTertiary))
                    Text(helperResultado, style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary))
                }
            }
        }
    }
}

@Composable
private fun StepperFieldCompact(
    label: String,
    value: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onValueChange: (String) -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Column(modifier = modifier.widthIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), style = FDType.Label.copy(fontSize = 10.5.sp, color = if (isError) FDColors.Error else FDColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(FDColors.SurfaceElevated).border(0.5.dp, if (isError) FDColors.Error else FDColors.BorderStrong, RoundedCornerShape(8.dp)).clickable { onStep(-1) },
                contentAlignment = Alignment.Center
            ) { Text("−", style = FDType.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isError) FDColors.Error else FDColors.TextPrimary)) }
            EnterpriseTextField(label = "", placeholder = placeholder, value = value, leadingIcon = icon, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f), onValueChange = onValueChange, isError = isError)
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(if (isError) FDColors.Error else FDColors.Primary).border(0.5.dp, if (isError) FDColors.Error else FDColors.Primary, RoundedCornerShape(8.dp)).clickable { onStep(1) },
                contentAlignment = Alignment.Center
            ) { Text("+", style = FDType.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FDColors.PrimaryText)) }
        }
    }
}

@Composable
private fun FlipSelectorCard(
    esPorEmpaque: Boolean,
    esUnidades: Boolean,
    sinSeleccion: Boolean,
    sugerenciaEsBultos: Boolean,
    etiquetaBultos: String,
    etiquetaUnidades: String,
    onSelectBultos: () -> Unit,
    onSelectUnidades: () -> Unit
) {
    val targetRot = when {
        esPorEmpaque -> 0f
        esUnidades -> 180f
        else -> 0f
    }
    val rotY by androidx.compose.animation.core.animateFloatAsState(targetValue = targetRot, animationSpec = androidx.compose.animation.core.tween(420), label = "flipRot")
    val mostrarBultos = rotY < 90f
    Box(
        modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(10.dp)).background(FDColors.Background).border(0.5.dp, if (sinSeleccion) FDColors.Warning.copy(alpha = 0.45f) else FDColors.BorderStrong.copy(alpha = 0.9f), RoundedCornerShape(10.dp)).clickable {
            if (esPorEmpaque) onSelectUnidades() else onSelectBultos()
        }.graphicsLayer { rotationY = rotY; cameraDistance = 12f * density },
        contentAlignment = Alignment.Center
    ) {
        if (mostrarBultos) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.graphicsLayer { rotationY = 0f }) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(if (esPorEmpaque) FDColors.Primary else FDColors.SurfaceElevated).border(0.5.dp, FDColors.Border, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Inventory2, null, tint = if (esPorEmpaque) FDColors.PrimaryText else FDColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
                Column {
                    Text(etiquetaBultos, style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (esPorEmpaque) FDColors.TextPrimary else FDColors.TextSecondary))
                    if (sinSeleccion && sugerenciaEsBultos) Text("Sugerido • toca para girar", style = FDType.Caption.copy(fontSize = 9.sp, color = FDColors.Primary))
                    else if (!esPorEmpaque) Text("Toca para girar → unidades", style = FDType.Caption.copy(fontSize = 9.sp, color = FDColors.TextTertiary))
                }
                if (esPorEmpaque) Icon(Icons.Outlined.CheckCircle, null, tint = FDColors.Success, modifier = Modifier.size(16.dp))
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(if (esUnidades) FDColors.Primary else FDColors.SurfaceElevated).border(0.5.dp, FDColors.Border, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Numbers, null, tint = if (esUnidades) FDColors.PrimaryText else FDColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
                Column {
                    Text(etiquetaUnidades, style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (esUnidades) FDColors.TextPrimary else FDColors.TextSecondary))
                    if (sinSeleccion && !sugerenciaEsBultos) Text("Sugerido • toca para girar", style = FDType.Caption.copy(fontSize = 9.sp, color = FDColors.Primary))
                    else if (!esUnidades) Text("Toca para girar → bultos", style = FDType.Caption.copy(fontSize = 9.sp, color = FDColors.TextTertiary))
                }
                if (esUnidades) Icon(Icons.Outlined.CheckCircle, null, tint = FDColors.Success, modifier = Modifier.size(16.dp))
            }
        }
    }
}
