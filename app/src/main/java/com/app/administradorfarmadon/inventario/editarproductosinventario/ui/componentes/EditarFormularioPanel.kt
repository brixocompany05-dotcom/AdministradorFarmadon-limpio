package com.app.administradorfarmadon.inventario.editarproductosinventario.ui.componentes

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.inventario.editarproductosinventario.logica.EditarProductoUiState

/**
 * Panel Izquierdo (60%): Formulario Continuo Enterprise para Edición de Producto.
 * Reflejo exacto 1 a 1 de Crear Producto (Ficha Técnica e Identidad del Producto).
 */
@Composable
fun EditarFormularioPanel(
    state: EditarProductoUiState,
    onNombreChange: (String) -> Unit,
    onTipoProductoChange: (String) -> Unit,
    onPrincipioActivoChange: (String) -> Unit,
    onCategoriaChange: (String) -> Unit,
    onLaboratorioChange: (String) -> Unit,
    onEmpaqueChange: (String) -> Unit,
    onCantidadContenidoChange: (String) -> Unit,
    onUnidadMedidaChange: (String) -> Unit,
    onCodigoBarrasChange: (String) -> Unit,
    onOpenCamaraScanner: () -> Unit,
    onRequiereRecetaChange: (Boolean) -> Unit,
    onEsRefrigeradoChange: (Boolean) -> Unit,
    onPermiteFraccionarChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val orig = state.originalProduct
    val esMedicamento = state.esMedicamento
    val s = recordarMedidaAdaptativa()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(s.radiusCard)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        color = FDColors.Surface,
        shape = RoundedCornerShape(s.radiusCard),
        border = BorderStroke(s.borderWidth, FDColors.Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(s.padCardLarge)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.ime),
            verticalArrangement = Arrangement.spacedBy(s.gapLarge)
        ) {
            // ── SELECTOR SEGMENTADO: TIPO DE FICHA ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FDColors.SurfaceElevated, RoundedCornerShape(s.radiusInput))
                    .padding(s.padCard),
                verticalArrangement = Arrangement.spacedBy(s.sm)
            ) {
                Text(
                    text = "TIPO DE PRODUCTO",
                    style = FDType.Label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FDColors.TextSecondary
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(s.btnMediumH)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FDColors.Surface)
                        .border(1.dp, FDColors.Border, RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isMed = state.tipoProducto.equals("MEDICAMENTO", ignoreCase = true)
                    
                    // Opción Medicamento
                    Surface(
                        color = if (isMed) FDColors.Primary.copy(alpha = 0.15f) else FDColors.Surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isMed) FDColors.Primary else FDColors.Border.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTipoProductoChange("MEDICAMENTO") }
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Medication,
                                contentDescription = null,
                                tint = if (isMed) FDColors.Primary else FDColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Medicamento",
                                style = FDType.Label.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (isMed) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isMed) FDColors.Primary else FDColors.TextSecondary
                                )
                            )
                        }
                    }

                    // Opción Producto General
                    Surface(
                        color = if (!isMed) FDColors.Primary.copy(alpha = 0.15f) else FDColors.Surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (!isMed) FDColors.Primary else FDColors.Border.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTipoProductoChange("GENERAL") }
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingBag,
                                contentDescription = null,
                                tint = if (!isMed) FDColors.Primary else FDColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "General",
                                style = FDType.Label.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (!isMed) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isMed) FDColors.Primary else FDColors.TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // ── 1. SECCIÓN: IDENTIFICACIÓN Y CLASIFICACIÓN ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FDColors.SurfaceElevated, RoundedCornerShape(s.radiusInput))
                    .padding(s.padCard),
                verticalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                FormSectionHeader(
                    title = "1. IDENTIFICACIÓN Y CLASIFICACIÓN",
                    description = if (esMedicamento) "Ficha de medicamento y categoría oficial" else "Ficha general y categoría del artículo"
                )

                EditarTextFieldSimetrico(
                    label = if (esMedicamento) "Nombre Comercial del Medicamento *" else "Nombre Comercial del Producto *",
                    value = state.nombre,
                    onValueChange = onNombreChange,
                    placeholder = if (esMedicamento) "Ej. Amoxicilina 500mg, Paracetamol Forte..." else "Ej. Sprite, Champú Pantene, Pañales Huggies...",
                    leadingIcon = if (esMedicamento) Icons.Outlined.Medication else Icons.Outlined.ShoppingBag,
                    isError = state.fieldErrors.containsKey("nombre"),
                    errorMessage = state.fieldErrors["nombre"] ?: "",
                    isModified = orig != null && state.nombre.trim() != orig.nombre.trim()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
                ) {
                    EditarDropdownSimetrico(
                        label = "Categoría Principal *",
                        value = state.categoriaNombre,
                        onValueChange = onCategoriaChange,
                        options = state.categoriasDisponibles,
                        placeholder = "Seleccionar categoría...",
                        leadingIcon = Icons.Outlined.Category,
                        isModified = orig != null && state.categoriaNombre.trim() != orig.categoriaNombre.trim(),
                        modifier = Modifier.weight(1f)
                    )

                    EditarTextFieldSimetrico(
                        label = if (esMedicamento) "Laboratorio / Fabricante" else "Marca / Fabricante",
                        value = state.laboratorio,
                        onValueChange = onLaboratorioChange,
                        placeholder = if (esMedicamento) "Ej. Genfar, Bagó, Pfizer..." else "Ej. Coca-Cola, Unilever, P&G...",
                        leadingIcon = Icons.Outlined.Business,
                        isModified = orig != null && state.laboratorio.trim() != orig.proveedorBaseNombre.trim(),
                        modifier = Modifier.weight(1f)
                    )
                }

                // PRINCIPIO ACTIVO
                AnimatedVisibility(
                    visible = esMedicamento,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    EditarTextFieldSimetrico(
                        label = "Principio Activo (Fórmula Genérica / DCI)",
                        value = state.principioActivo,
                        onValueChange = onPrincipioActivoChange,
                        placeholder = "Ej: Amoxicilina Trihidrato, Paracetamol, Ibuprofeno...",
                        leadingIcon = Icons.Outlined.Science,
                        isModified = orig != null && state.principioActivo.trim() != orig.principioActivo.trim()
                    )
                }
            }

            // ── 2. SECCIÓN: PRESENTACIÓN FÍSICA Y CÓDIGO ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FDColors.SurfaceElevated, RoundedCornerShape(s.radiusInput))
                    .padding(s.padCard),
                verticalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                FormSectionHeader(
                    title = "2. PRESENTACIÓN FÍSICA Y CÓDIGO",
                    description = "Formato de envase oficial, contenido neto, unidad de medida estricta y código de barras"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    EditarDropdownSimetrico(
                        label = "Tipo de Envase *",
                        value = state.empaque,
                        onValueChange = onEmpaqueChange,
                        options = state.empaquesDisponibles,
                        placeholder = "Envase...",
                        leadingIcon = Icons.Outlined.Inventory2,
                        isModified = orig != null && state.empaque.trim() != orig.empaque.trim(),
                        modifier = Modifier.weight(1.1f)
                    )

                    EditarTextFieldSimetrico(
                        label = "Contenido *",
                        value = state.cantidadContenido,
                        onValueChange = onCantidadContenidoChange,
                        placeholder = "500, 1.5, 400...",
                        keyboardType = KeyboardType.Number,
                        leadingIcon = Icons.Outlined.Straighten,
                        isModified = orig != null && state.cantidadContenido.trim() != orig.contenido.trim(),
                        modifier = Modifier.weight(0.9f)
                    )

                    EditarDropdownSimetrico(
                        label = "Unidad *",
                        value = state.unidadMedida,
                        onValueChange = onUnidadMedidaChange,
                        options = state.unidadesDisponibles,
                        placeholder = "Unidad...",
                        leadingIcon = Icons.Outlined.Speed,
                        isModified = orig != null && state.unidadMedida.trim() != orig.contenidoUnidad.trim(),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Campo de Código de Barras con botón de Cámara
                EditarTextFieldSimetrico(
                    label = "Código de Barras (EAN-13 / Lector)",
                    value = state.codigoBarras,
                    onValueChange = onCodigoBarrasChange,
                    placeholder = "Pistolea o escanea con la cámara...",
                    leadingIcon = Icons.Outlined.QrCodeScanner,
                    trailingIcon = Icons.Outlined.PhotoCamera,
                    onTrailingIconClick = onOpenCamaraScanner,
                    isError = state.fieldErrors.containsKey("codigoBarras"),
                    errorMessage = state.fieldErrors["codigoBarras"] ?: "",
                    isModified = orig != null && state.codigoBarras.trim() != orig.codigo.trim()
                )
            }

            // ── 3. CONDICIONES SANITARIAS ──
            if (esMedicamento) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FDColors.SurfaceElevated, RoundedCornerShape(s.radiusInput))
                        .padding(s.padCard),
                    verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                ) {
                    FormSectionHeader(
                        title = "3. CONDICIONES SANITARIAS",
                        description = "Normativa de dispensación médica y control térmico"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(s.sm)
                    ) {
                        EditarSwitchSimetrico(
                            label = "Normativa Sanitaria",
                            title = "Requiere Receta",
                            subtitle = if (state.requiereReceta) "Venta con receta" else "Venta libre (OTC)",
                            checked = state.requiereReceta,
                            onCheckedChange = onRequiereRecetaChange,
                            icon = Icons.Outlined.Description,
                            isModified = orig != null && state.requiereReceta != orig.requiereReceta,
                            modifier = Modifier.weight(1f)
                        )

                        EditarSwitchSimetrico(
                            label = "Conservación",
                            title = "Cadena de Frío",
                            subtitle = if (state.esRefrigerado) "Requiere nevera" else "Temp. ambiente",
                            checked = state.esRefrigerado,
                            onCheckedChange = onEsRefrigeradoChange,
                            icon = Icons.Outlined.AcUnit,
                            isModified = orig != null && state.esRefrigerado != (orig.temperaturaAlmacenamiento.contains("REFRIG", ignoreCase = true)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── 4. CONDICIÓN DE FRACCIONAMIENTO ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FDColors.SurfaceElevated, RoundedCornerShape(s.radiusInput))
                    .padding(s.padCard),
                verticalArrangement = Arrangement.spacedBy(s.sm)
            ) {
                Text(
                    text = "¿SE PUEDE VENDER FRACCIONADO?",
                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextSecondary)
                )
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EditarSwitchSimetrico(
                        label = "Fraccionamiento",
                        title = if (!state.permiteFraccionar) "Sellado" else "Fraccionado",
                        subtitle = if (!state.permiteFraccionar) "Solo envase completo" else "Venta en fracciones (ml, g, etc)",
                        checked = !state.permiteFraccionar,
                        onCheckedChange = { checked -> onPermiteFraccionarChange(!checked) },
                        icon = Icons.Outlined.ContentCut,
                        isModified = orig != null && state.permiteFraccionar != orig.permiteFraccionar,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (!state.permiteFraccionar) {
                    Text(
                        text = "Sellado no permite fracciones después.",
                        style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextTertiary)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormSectionHeader(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = FDType.Heading3.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = FDColors.TextPrimary,
                letterSpacing = 0.5.sp
            )
        )
        Text(
            text = description,
            style = FDType.Caption.copy(
                fontSize = 11.5.sp,
                color = FDColors.TextSecondary
            )
        )
    }
}
