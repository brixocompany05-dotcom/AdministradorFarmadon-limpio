package com.app.administradorfarmadon.inventario.editarproductosinventario.ui.componentes

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.inventario.editarproductosinventario.logica.EditarProductoUiState
import com.app.administradorfarmadon.inventario.editarproductosinventario.logica.EstadoGuardadoEdicion

/**
 * Panel Derecho (40%): Resumen Ejecutivo y Guardado en Vivo con Lottie (Enterprise SaaS).
 * El botón se activa ÚNICAMENTE si hubo cambios reales en la ficha.
 */
@Composable
fun EditarResumenPanel(
    state: EditarProductoUiState,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onSuccessExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isChanged = state.isAnyFieldChanged
    val esMedicamento = state.esMedicamento

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp)),
        shadowElevation = 10.dp,
        tonalElevation = 2.dp,
        color = Color(0xFFFDFDFE), // Satin White
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, FDColors.Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            AnimatedContent(
                targetState = state.estadoGuardado,
                label = "EstadoGuardadoEdicionTransition",
                modifier = Modifier.fillMaxSize()
            ) { estado ->
                when (estado) {
                    EstadoGuardadoEdicion.IDLE -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Parte superior: Resumen de Ficha
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RESUMEN EN VIVO",
                                        style = FDType.Label.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FDColors.TextSecondary,
                                            letterSpacing = 1.sp
                                        )
                                    )

                                    if (isChanged) {
                                        Surface(
                                            color = FDColors.Warning.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.6f))
                                        ) {
                                            Text(
                                                text = "CAMBIOS PENDIENTES",
                                                style = FDType.Caption.copy(
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = FDColors.Warning
                                                ),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            color = FDColors.Surface,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(0.5.dp, FDColors.Border)
                                        ) {
                                            Text(
                                                text = "SIN CAMBIOS",
                                                style = FDType.Caption.copy(
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = FDColors.TextTertiary
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                // Nombre del Producto
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = state.nombre.ifBlank { "Nombre del producto..." },
                                        style = FDType.Heading2.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state.nombre.isBlank()) FDColors.TextTertiary else FDColors.TextPrimary
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val subLabel = if (state.laboratorio.isNotBlank()) {
                                        "${state.laboratorio} • ${state.categoriaNombre}"
                                    } else {
                                        state.categoriaNombre
                                    }

                                    Text(
                                        text = subLabel,
                                        style = FDType.Caption.copy(
                                            fontSize = 12.sp,
                                            color = FDColors.TextSecondary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)

                                // Especificaciones Técnicas
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ResumenRowSimetrico(
                                        icon = Icons.Outlined.Inventory2,
                                        label = "Presentación",
                                        value = if (state.medidaConcentracion.isNotBlank()) "${state.empaque} · ${state.medidaConcentracion}" else state.empaque
                                    )

                                    if (esMedicamento && state.principioActivo.isNotBlank()) {
                                        ResumenRowSimetrico(
                                            icon = Icons.Outlined.Science,
                                            label = "Principio Activo",
                                            value = state.principioActivo
                                        )
                                    }

                                    ResumenRowSimetrico(
                                        icon = Icons.Outlined.QrCodeScanner,
                                        label = "Código de Barras",
                                        value = state.codigoBarras.ifBlank { "—" }
                                    )

                                    if (esMedicamento) {
                                        ResumenRowSimetrico(
                                            icon = Icons.Outlined.VerifiedUser,
                                            label = "Normativa",
                                            value = if (state.requiereReceta) "Receta" else "Libre"
                                        )

                                        if (state.esRefrigerado) {
                                            ResumenRowSimetrico(
                                                icon = Icons.Outlined.AcUnit,
                                                label = "Conservación",
                                                value = "Refrigerado"
                                            )
                                        }
                                    }
                                }
                            }

                            // Botón Primario Dominante (52dp) - SOLO ACTIVO SI HUBO CAMBIOS REALES
                            Button(
                                onClick = onSave,
                                enabled = isChanged,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FDColors.Primary,
                                    contentColor = FDColors.PrimaryText,
                                    disabledContainerColor = FDColors.Surface,
                                    disabledContentColor = FDColors.TextDisabled
                                ),
                                border = if (!isChanged) BorderStroke(0.5.dp, FDColors.Border) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .let { if (isChanged) it.bounceClick() else it }
                            ) {
                                Icon(
                                    imageVector = if (isChanged) Icons.Filled.Check else Icons.Filled.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isChanged) "GUARDAR CAMBIOS EN FICHA" else "SIN CAMBIOS PENDIENTES",
                                    style = FDType.Label.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    }

                    EstadoGuardadoEdicion.GUARDANDO -> {
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

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "ACTUALIZANDO FICHA...",
                                style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Guardando cambios de \"${state.nombre}\" en el inventario.",
                                style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextSecondary, textAlign = TextAlign.Center),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    EstadoGuardadoEdicion.EXITO -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Spacer(modifier = Modifier.height(20.dp))

                                com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDLottieFeedback(
                                    resId = R.raw.anim_exito,
                                    isLoop = false,
                                    size = 90.dp
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "¡FICHA ACTUALIZADA CON ÉXITO!",
                                        style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FDColors.Success),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Los datos de \"${state.nombre}\" han sido sincronizados en tu inventario.",
                                        style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextSecondary, textAlign = TextAlign.Center),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = onSuccessExit,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FDColors.Success,
                                    contentColor = FDColors.PrimaryText
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .bounceClick()
                            ) {
                                Text(
                                    text = "VOLVER AL INVENTARIO",
                                    style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    EstadoGuardadoEdicion.ERROR -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Spacer(modifier = Modifier.height(20.dp))

                                com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDLottieFeedback(
                                    resId = R.raw.anim_error,
                                    isLoop = false,
                                    size = 90.dp
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "NO SE PUDIERON GUARDAR LOS CAMBIOS",
                                        style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FDColors.Error),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = state.mensajeErrorGuardado ?: "Ocurrió un error inesperado al actualizar la ficha.",
                                        style = FDType.Body.copy(fontSize = 12.5.sp, color = FDColors.TextSecondary, textAlign = TextAlign.Center),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = onRetry,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FDColors.Primary,
                                    contentColor = FDColors.PrimaryText
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .bounceClick()
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "REINTENTAR ACTUALIZACIÓN",
                                    style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumenRowSimetrico(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FDColors.TextTertiary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = FDType.Body.copy(
                    fontSize = 12.sp,
                    color = FDColors.TextSecondary
                )
            )
        }

        Text(
            text = value,
            style = FDType.Body.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FDColors.TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
