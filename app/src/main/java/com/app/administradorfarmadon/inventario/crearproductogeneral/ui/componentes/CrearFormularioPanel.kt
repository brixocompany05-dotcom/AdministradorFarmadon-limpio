package com.app.administradorfarmadon.inventario.crearproductogeneral.ui.componentes

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques
import com.app.administradorfarmadon.inventario.crearproductogeneral.logica.CrearProductoUiState

/**
 * Panel Izquierdo (60%): Formulario Continuo Enterprise para Crear Producto.
 * Soporta corrección ortográfica estricta sin palabras extra y lector de cámara instantáneo.
 */
@Composable
fun CrearFormularioPanel(
    state: CrearProductoUiState,
    onNombreChange: (String) -> Unit,
    onEjecutarAnalisisIa: () -> Unit,
    onAplicarNombreCorregido: () -> Unit,
    onAbrirCamaraScanner: () -> Unit,
    onCategoriaChange: (String) -> Unit,
    onLaboratorioChange: (String) -> Unit,
    onPrincipioActivoChange: (String) -> Unit,
    onEmpaqueChange: (String) -> Unit,
    onCantidadContenidoChange: (String) -> Unit,
    onUnidadMedidaChange: (String) -> Unit,
    onVarianteSeleccionada: (String) -> Unit,
    onCodigoBarrasChange: (String) -> Unit,
    onRequiereRecetaChange: (Boolean) -> Unit,
    onEsRefrigeradoChange: (Boolean) -> Unit,
    onPermiteFraccionarChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
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
            // ── 1. SECCIÓN: IDENTIFICACIÓN Y CLASIFICACIÓN ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FDColors.SurfaceElevated, RoundedCornerShape(s.radiusInput))
                    .padding(s.lg),
                verticalArrangement = Arrangement.spacedBy(s.lg)
            ) {
                FormSectionHeader(
                    title = "1. IDENTIFICACIÓN Y CLASIFICACIÓN",
                    description = if (esMedicamento) "Información farmacológica y categoría oficial" else "Información general y marca del artículo"
                )

                // Campo Principal: Nombre del Producto
                Column(verticalArrangement = Arrangement.spacedBy(s.xs)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(s.textLabel.value.dp * 1.1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (esMedicamento) "NOMBRE DEL MEDICAMENTO *" else "NOMBRE DEL PRODUCTO *",
                            style = FDType.Label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (state.errores.containsKey("nombre")) FDColors.Error else FDColors.TextSecondary
                            )
                        )

                        when {
                            state.clasificandoIa -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                                ) {
                                    CircularProgressIndicator(
                                        color = FDColors.Primary,
                                        strokeWidth = 1.5.dp,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Analizando...",
                                        style = FDType.Caption.copy(fontSize = 10.5.sp, color = FDColors.Primary, fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                            state.usuarioModificoManualmente -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = FDColors.Warning,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Edición manual",
                                        style = FDType.Caption.copy(fontSize = 10.5.sp, color = FDColors.Warning, fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                            state.sugerenciaIaAplicada -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = FDColors.Success,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (esMedicamento) "Medicamento detectado" else "Artículo retail detectado",
                                        style = FDType.Caption.copy(fontSize = 10.5.sp, color = FDColors.Success, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    CrearTextFieldSimetrico(
                        value = state.nombre,
                        onValueChange = onNombreChange,
                        placeholder = "Escribe el nombre: Ej. Amoxicilina 500mg, Panadol, Coca Cola 1.5L...",
                        leadingIcon = if (esMedicamento) Icons.Outlined.Medication else Icons.Outlined.ShoppingBag,
                        isError = state.errores.containsKey("nombre"),
                        errorMessage = state.errores["nombre"] ?: "",
                        imeAction = ImeAction.Search,
                        onImeAction = onEjecutarAnalisisIa
                    )

                    // Chip de Corrección Ortográfica Estricta (Solo si la IA corrigió el tipeo exacto)
                    if (state.nombreSugeridoCorregido.isNotBlank()) {
                        Surface(
                            color = FDColors.Primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(s.radiusChip),
                            border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(s.xs),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Spellcheck,
                                        contentDescription = null,
                                        tint = FDColors.Primary,
                                        modifier = Modifier.size(s.iconSmall)
                                    )
                                    Text(
                                        text = "¿Corregir ortografía a: \"${state.nombreSugeridoCorregido}\"?",
                                        style = FDType.Caption.copy(fontSize = 11.5.sp, color = FDColors.TextPrimary)
                                    )
                                }

                                TextButton(
                                    onClick = onAplicarNombreCorregido,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "✓ Aplicar corrección",
                                        style = FDType.Label.copy(fontSize = 11.sp, color = FDColors.Primary, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(s.lg)
                ) {
                    CrearDropdownSimetrico(
                        label = "Categoría Principal *",
                        value = state.categoriaNombre,
                        onValueChange = onCategoriaChange,
                        options = state.categoriasDisponibles,
                        placeholder = "Seleccionar categoría...",
                        leadingIcon = Icons.Outlined.Category,
                        modifier = Modifier.weight(1f)
                    )

                    CrearTextFieldSimetrico(
                        label = if (esMedicamento) "Laboratorio / Fabricante" else "Marca / Fabricante",
                        value = state.laboratorio,
                        onValueChange = onLaboratorioChange,
                        placeholder = if (esMedicamento) "Ej. Genfar, Bagó, Pfizer" else "Ej. Coca-Cola, P&G, Nestlé",
                        leadingIcon = Icons.Outlined.Business,
                        modifier = Modifier.weight(1f)
                    )
                }

                // PRINCIPIO ACTIVO (Condicional: solo productos médicos)
                AnimatedVisibility(
                    visible = esMedicamento,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    CrearTextFieldSimetrico(
                        label = "Principio Activo (Fórmula Genérica / DCI)",
                        value = state.principioActivo,
                        onValueChange = onPrincipioActivoChange,
                        placeholder = "Ej. Amoxicilina Trihidrato, Paracetamol, Ibuprofeno",
                        leadingIcon = Icons.Outlined.Science
                    )
                }
            }

            // ── 2. SECCIÓN: PRESENTACIÓN FÍSICA Y CÓDIGO ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FDColors.SurfaceElevated, RoundedCornerShape(s.radiusInput))
                    .padding(s.lg),
                verticalArrangement = Arrangement.spacedBy(s.lg)
            ) {
                FormSectionHeader(
                    title = "2. PRESENTACIÓN FÍSICA Y CÓDIGO",
                    description = "Envase oficial, cantidad/contenido, unidad de medida estricta y código de barras"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(s.md)
                ) {
                    CrearDropdownSimetrico(
                        label = "Tipo de Envase *",
                        value = state.empaque,
                        onValueChange = onEmpaqueChange,
                        options = state.empaquesDisponibles,
                        placeholder = "Envase...",
                        leadingIcon = Icons.Outlined.Inventory2,
                        modifier = Modifier.weight(1.1f)
                    )

                    CrearTextFieldSimetrico(
                        label = "Contenido *",
                        value = state.cantidadContenido,
                        onValueChange = onCantidadContenidoChange,
                        placeholder = "500, 1.5, 400...",
                        keyboardType = KeyboardType.Number,
                        leadingIcon = Icons.Outlined.Straighten,
                        modifier = Modifier.weight(0.9f)
                    )

                    CrearDropdownSimetrico(
                        label = "Unidad *",
                        value = state.unidadMedida,
                        onValueChange = onUnidadMedidaChange,
                        options = state.unidadesDisponibles,
                        placeholder = "Unidad...",
                        leadingIcon = Icons.Outlined.Speed,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Aviso de coherencia en vivo (R3): si empaque y unidad no combinan, decirlo YA
                if (state.empaque.isNotBlank() && state.unidadMedida.isNotBlank()) {
                    val familia = CatalogoEmpaques.detectarFamiliaFisica(state.empaque, state.unidadMedida)
                    val empOk = familia.empaquesCompatibles.any { it.equals(state.empaque, ignoreCase = true) }
                    val uniOk = familia.unidadesCompatibles.any { it.equals(state.unidadMedida, ignoreCase = true) }
                    if (!empOk || !uniOk) {
                        Surface(
                            color = FDColors.ErrorSubtle,
                            shape = RoundedCornerShape(s.radiusInput),
                            border = BorderStroke(0.5.dp, FDColors.Error.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(s.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(s.xs)
                            ) {
                                val sugerenciasEmpaques = familia.empaquesCompatibles.take(3).joinToString(", ")
                                Text(
                                    text = "Envase '${state.empaque}' y unidad '${state.unidadMedida}' no combinan. Para ${state.unidadMedida} usa: $sugerenciasEmpaques",
                                    style = FDType.Caption.copy(color = FDColors.Error, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // Botones de Variantes Comerciales del Mercado (1 Toque)
                if (state.variantesSugeridas.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(s.xs)) {
                        Text(
                            text = "PRESENTACIONES COMERCIALES SUGERIDAS (1 TOQUE):",
                            style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.SemiBold)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(s.xs)
                        ) {
                            state.variantesSugeridas.forEach { variante ->
                                val (varCant, varUnidad) = CatalogoEmpaques.separarContenidoYUnidad(variante)
                                val isSelected = state.varianteSeleccionada == variante || (
                                    varCant.isNotBlank() &&
                                    varCant == state.cantidadContenido &&
                                    (varUnidad.isBlank() || varUnidad.equals(state.unidadMedida, ignoreCase = true))
                                )

                                Surface(
                                    color = if (isSelected) FDColors.Primary.copy(alpha = 0.08f) else FDColors.Surface,
                                    shape = RoundedCornerShape(s.radiusChip),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) FDColors.Primary else FDColors.Border
                                    ),
                                    modifier = Modifier.clickable { onVarianteSeleccionada(variante) }
                                ) {
                                    Text(
                                        text = variante,
                                        style = FDType.Caption.copy(
                                            fontSize = 10.5.sp,
                                            color = if (isSelected) FDColors.TextPrimary else FDColors.TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Campo de Código de Barras con botón de Cámara
                CrearTextFieldSimetrico(
                    label = "Código de Barras (EAN-13 / Lector)",
                    value = state.codigoBarras,
                    onValueChange = onCodigoBarrasChange,
                    placeholder = "Pistolea o escanea con la cámara...",
                    leadingIcon = Icons.Outlined.QrCodeScanner,
                    trailingIcon = Icons.Outlined.PhotoCamera,
                    onTrailingIconClick = onAbrirCamaraScanner,
                    isError = state.errores.containsKey("codigoBarras"),
                    errorMessage = state.errores["codigoBarras"] ?: "",
                    helpText = "Si no lo escribes, el sistema creará un código único (ej. FMD-123456). Podrás imprimir su etiqueta y el escáner lo encontrará."
                )

                state.productoExistenteDuplicado?.let { (_, dupNombre) ->
                    Surface(
                        color = FDColors.WarningSubtle,
                        shape = RoundedCornerShape(s.radiusInput),
                        border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(s.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.sm)
                        ) {
                            Icon(Icons.Outlined.Inventory2, null, tint = FDColors.Warning, modifier = Modifier.size(20.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "PRODUCTO YA REGISTRADO EN INVENTARIO",
                                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, color = FDColors.Warning, fontSize = 10.5.sp)
                                )
                                Text(
                                    text = "Este código ya pertenece a \"$dupNombre\". Toca el botón para ingresar stock o ver el producto.",
                                    style = FDType.BodySmall.copy(color = FDColors.TextPrimary, fontSize = 12.sp)
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. CONDICIÓN DE FRACCIONAMIENTO (LÓGICA DE NEGOCIO) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FDColors.SurfaceElevated, RoundedCornerShape(s.radiusInput))
                    .padding(s.lg),
                verticalArrangement = Arrangement.spacedBy(s.md)
            ) {
                Text(
                    text = "¿SE PUEDE VENDER FRACCIONADO?",
                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextSecondary)
                )
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CrearSwitchSimetrico(
                        label = "Fraccionamiento",
                        title = if (!state.permiteFraccionar) "Sellado" else "Fraccionado",
                        subtitle = if (state.permiteFraccionar) "Venta en fracciones (ml, g, etc)" else "Venta solo por envase completo",
                        checked = !state.permiteFraccionar,
                        onCheckedChange = { checked -> onPermiteFraccionarChange(!checked) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (!state.permiteFraccionar) {
                    Text(
                        text = "Sellado no permite fracciones después. Crea otro producto a granel si necesitas fraccionar.",
                        style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextTertiary)
                    )
                }
            }

            // ── 4. CONDICIONES SANITARIAS (RECETA & FRÍO) ──
            if (esMedicamento) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FDColors.SurfaceElevated, RoundedCornerShape(s.radiusInput))
                    .padding(s.padCard),
                    verticalArrangement = Arrangement.spacedBy(s.lg)
                ) {
                    FormSectionHeader(
                        title = "3. CONDICIONES SANITARIAS",
                        description = "Requisitos legales y de conservación del medicamento"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(s.md)
                    ) {
                        CrearSwitchSimetrico(
                            label = "Normativa Sanitaria",
                            title = "Requiere Receta",
                            subtitle = if (state.requiereReceta) "Venta con receta" else "Venta libre (OTC)",
                            checked = state.requiereReceta,
                            onCheckedChange = onRequiereRecetaChange,
                            modifier = Modifier.weight(1f)
                        )

                        CrearSwitchSimetrico(
                            label = "Conservación",
                            title = "Cadena de Frío",
                            subtitle = if (state.esRefrigerado) "Requiere nevera" else "Temp. ambiente",
                            checked = state.esRefrigerado,
                            onCheckedChange = onEsRefrigeradoChange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
