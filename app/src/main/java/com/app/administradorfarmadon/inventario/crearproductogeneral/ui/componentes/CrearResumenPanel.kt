package com.app.administradorfarmadon.inventario.crearproductogeneral.ui.componentes

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.R
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.FDSpacing
import com.app.administradorfarmadon.disenotemaapp.ui.FDSizes
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonSecundario
import com.app.administradorfarmadon.inventario.crearproductogeneral.logica.CrearProductoUiState
import com.app.administradorfarmadon.inventario.crearproductogeneral.logica.EstadoGuardadoProducto

/**
 * Panel Derecho (40%): Resumen Ejecutivo y Feedback de Guardado en Vivo.
 * Transición fluida entre estados: Ficha ──ž” Subiendo ──ž” Éxito ──ž” Error con Reintento.
 */
@Composable
fun CrearResumenPanel(
    state: CrearProductoUiState,
    onSave: () -> Unit,
    onRegistrarOtro: () -> Unit,
    onSalir: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(FDShapes.Medium),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        color = FDColors.Surface,
        shape = FDShapes.Medium,
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(FDSpacing.xxl)
        ) {
            AnimatedContent(
                targetState = state.estadoGuardado,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "EstadoGuardadoTransicion"
            ) { estado ->
                when (estado) {
                    EstadoGuardadoProducto.IDLE -> {
                        EstadoIdlePanel(
                            state = state,
                            onSave = onSave
                        )
                    }

                    EstadoGuardadoProducto.GUARDANDO -> {
                        EstadoGuardandoPanel(
                            productoNombre = state.nombre
                        )
                    }

                    EstadoGuardadoProducto.EXITO -> {
                        EstadoExitoPanel(
                            productoNombre = state.productoGuardadoNombre.ifBlank { state.nombre },
                            onRegistrarOtro = onRegistrarOtro,
                            onSalir = onSalir
                        )
                    }

                    EstadoGuardadoProducto.ERROR -> {
                        EstadoErrorPanel(
                            mensajeError = state.mensajeErrorGuardado ?: "Ocurrió un error al guardar. Revisa tu conexión.",
                            onReintentar = onReintentar
                        )
                    }
                }
            }
        }
    }
}

// ── 1. ESTADO IDLE: FICHA Y BOTÓN PRIMARIO DOMINANTE ──
@Composable
private fun EstadoIdlePanel(
    state: CrearProductoUiState,
    onSave: () -> Unit
) {
    val esMedicamento = state.esMedicamento
    val listoParaGuardar = state.nombre.trim().length >= 3 && state.productoExistenteDuplicado == null

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FDSpacing.xl)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESUMEN DE LA FICHA",
                    style = FDType.Label.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = FDColors.TextSecondary,
                        letterSpacing = 1.sp
                    )
                )

                if (listoParaGuardar) {
                    Surface(
                        color = FDColors.SuccessSubtle,
                        shape = FDShapes.XSmall,
                        border = BorderStroke(1.dp, FDColors.Success.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "LISTO PARA GUARDAR",
                            style = FDType.Caption.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FDColors.Success
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        color = if (state.productoExistenteDuplicado != null) FDColors.ErrorSubtle else FDColors.TextTertiary.copy(alpha = 0.1f),
                        shape = FDShapes.XSmall,
                        border = BorderStroke(0.5.dp, if (state.productoExistenteDuplicado != null) FDColors.Error.copy(alpha = 0.4f) else FDColors.Border)
                    ) {
                        Text(
                            text = if (state.productoExistenteDuplicado != null) "CÓDIGO DUPLICADO" else "DATOS INCOMPLETOS",
                            style = FDType.Caption.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.productoExistenteDuplicado != null) FDColors.Error else FDColors.TextTertiary
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(FDSpacing.xs)) {
                Text(
                    text = state.nombre.ifBlank { "NOMBRE DEL PRODUCTO" }.uppercase(),
                    style = FDType.Heading2.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.nombre.isBlank()) FDColors.TextTertiary else FDColors.TextPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${state.categoriaNombre.ifBlank { "Sin categoría" }} · ${if (esMedicamento) "Lab" else "Marca"}: ${state.laboratorio.ifBlank { "Sin laboratorio" }}",
                    style = FDType.Caption.copy(fontSize = 12.sp, color = FDColors.TextSecondary)
                )
            }

            HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)

            Column(verticalArrangement = Arrangement.spacedBy(FDSpacing.md)) {
                if (state.principioActivo.isNotBlank()) {
                    ResumenItemRow(
                        icon = Icons.Outlined.Science,
                        label = "Principio Activo",
                        value = state.principioActivo
                    )
                }

                val presTexto = when {
                    state.empaque.isNotBlank() && state.medidaConcentracion.isNotBlank() -> "${state.empaque} · ${state.medidaConcentracion}"
                    state.empaque.isNotBlank() -> state.empaque
                    state.medidaConcentracion.isNotBlank() -> state.medidaConcentracion
                    else -> "—”"
                }

                ResumenItemRow(
                    icon = Icons.Outlined.Inventory2,
                    label = "Presentación",
                    value = presTexto
                )

                if (state.codigoBarras.isNotBlank()) {
                    ResumenItemRow(
                        icon = Icons.Outlined.QrCodeScanner,
                        label = "Código de Barras",
                        value = state.codigoBarras,
                        isError = state.productoExistenteDuplicado != null
                    )
                }

                if (esMedicamento || state.requiereReceta || state.esRefrigerado) {
                    ResumenItemRow(
                        icon = Icons.AutoMirrored.Outlined.Assignment,
                        label = "Condición de Venta",
                        value = if (state.requiereReceta) "Receta" else "Libre"
                    )
                    ResumenItemRow(
                        icon = Icons.Outlined.AcUnit,
                        label = "Conservación",
                        value = if (state.esRefrigerado) "Refrigerado" else "Ambiente"
                    )
                }
            }
        }

        // íšNICO BOTÓN PRIMARIO DOMINANTE
        FDBotonPrimario(
            texto = "GUARDAR PRODUCTO EN INVENTARIO",
            onClick = onSave,
            habilitado = listoParaGuardar,
            icono = Icons.Default.Check,
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick()
        )
    }
}

// ── 2. ESTADO SUBIENDO / GUARDANDO EN PROCESO ──
@Composable
private fun EstadoGuardandoPanel(productoNombre: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDLottieFeedback(
            resId = R.raw.anim_guardando,
            isLoop = true,
            size = 90.dp
        )

        Spacer(modifier = Modifier.height(FDSpacing.xl))

        Text(
            text = "GUARDANDO PRODUCTO...",
            style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
        )
        Spacer(modifier = Modifier.height(FDSpacing.xs))
        Text(
            text = "Registrando \"$productoNombre\" en la base de datos de tu farmacia.",
            style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextSecondary, textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = FDSpacing.xl)
        )
    }
}

// ── 3. ESTADO ÉXITO: CONFIRMACIÓN Y BOTONES DE CONTINUIDAD ──
@Composable
private fun EstadoExitoPanel(
    productoNombre: String,
    onRegistrarOtro: () -> Unit,
    onSalir: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FDSpacing.xl)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDLottieFeedback(
                resId = R.raw.anim_exito,
                isLoop = false,
                size = 90.dp
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FDSpacing.xs)
            ) {
                Text(
                    text = "¡PRODUCTO GUARDADO CON ÉXITO!",
                    style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FDColors.Success),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "\"$productoNombre\" ya forma parte del catálogo de tu farmacia.",
                    style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextSecondary, textAlign = TextAlign.Center),
                    modifier = Modifier.padding(horizontal = FDSpacing.xl)
                )
            }
        }

        // ACCIONES DE SALIDA O REGISTRAR SIGUIENTE
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            FDBotonPrimario(
                texto = "REGISTRAR SIGUIENTE PRODUCTO",
                onClick = onRegistrarOtro,
                icono = Icons.Default.Add,
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick()
            )

            FDBotonSecundario(
                texto = "Salir al Inventario",
                onClick = onSalir,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── 4. ESTADO ERROR: ALERTA CLARA Y BOTÓN REINTENTAR ──
@Composable
private fun EstadoErrorPanel(
    mensajeError: String,
    onReintentar: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FDSpacing.xl)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDLottieFeedback(
                resId = R.raw.anim_error,
                isLoop = false,
                size = 90.dp
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FDSpacing.xs)
            ) {
                Text(
                    text = "NO SE PUDO GUARDAR",
                    style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FDColors.Error),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = mensajeError,
                    style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextSecondary, textAlign = TextAlign.Center),
                    modifier = Modifier.padding(horizontal = FDSpacing.xl)
                )
            }

            Surface(
                color = FDColors.Surface,
                shape = FDShapes.Small,
                border = BorderStroke(1.dp, FDColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(FDSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = FDColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Tus datos siguen intactos en el formulario de la izquierda. Si cambiaste algo, se incluirá al reintentar.",
                        style = FDType.Caption.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary)
                    )
                }
            }
        }

        // BOTÓN REINTENTAR DOMINANTE (52.dp)
        FDBotonPrimario(
            texto = "REINTENTAR GUARDADO",
            onClick = onReintentar,
            icono = Icons.Default.Refresh,
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick()
        )
    }
}

@Composable
private fun ResumenItemRow(
    icon: ImageVector,
    label: String,
    value: String,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isError) FDColors.Error else FDColors.TextTertiary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = FDType.Caption.copy(color = if (isError) FDColors.Error else FDColors.TextSecondary, fontSize = 11.5.sp)
            )
        }
        Text(
            text = value,
            style = FDType.BodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isError) FDColors.Error else FDColors.TextPrimary,
                fontSize = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
