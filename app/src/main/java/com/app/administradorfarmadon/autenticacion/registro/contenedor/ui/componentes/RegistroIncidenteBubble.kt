package com.app.administradorfarmadon.autenticacion.registro.contenedor.ui.componentes

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RegistroIncidenteAccion
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RegistroIncidenteTipo
import com.app.administradorfarmadon.autenticacion.registro.contenedor.datos.RegistroIncidenteUi
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun RegistroIncidenteBubble(
    incidente: RegistroIncidenteUi,
    onAccionPrincipal: (RegistroIncidenteAccion) -> Unit,
    onAccionSecundaria: (RegistroIncidenteAccion) -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    Surface(
        modifier = modifier
            .widthIn(max = 400.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer { this.alpha = alpha.value }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo((offsetX.value + dragAmount.x).coerceAtLeast(0f))
                            offsetY.snapTo((offsetY.value + dragAmount.y).coerceAtLeast(0f))
                        }
                    },
                    onDragEnd = {
                        if (offsetX.value > 120f || offsetY.value > 120f) {
                            scope.launch {
                                launch { offsetX.animateTo(offsetX.value + 400f, tween(300)) }
                                launch { offsetY.animateTo(offsetY.value + 400f, tween(300)) }
                                launch { alpha.animateTo(0f, tween(300)) }
                                onDismiss()
                            }
                        } else {
                            scope.launch {
                                launch { offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy)) }
                                launch { offsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy)) }
                            }
                        }
                    }
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = FDColors.Border,
                shape = RoundedCornerShape(16.dp)
            ),
        color = FDColors.SurfaceElevated,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FDColors.Glass),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = incidente.icono,
                    contentDescription = null,
                    tint = when (incidente.tipo) {
                        RegistroIncidenteTipo.SIN_INTERNET -> FDColors.Warning
                        else -> FDColors.Error
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = incidente.titulo,
                        style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextPrimary
                    )

                    // Target táctil mínimo 48x48dp: es la acción de escape del panel.
                    IconButton(
                        onClick = { onAccionSecundaria(RegistroIncidenteAccion.Descartar) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = FDColors.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = incidente.mensaje,
                    style = FDType.BodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = FDColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    incidente.accionSecundaria?.let { accion ->
                        TextButton(
                            onClick = { onAccionSecundaria(accion) },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = when (accion) {
                                    RegistroIncidenteAccion.Descartar -> "Descartar"
                                    RegistroIncidenteAccion.VolverYCorregir -> "Corregir"
                                    else -> "Cerrar"
                                },
                                style = FDType.Label.copy(fontSize = 12.sp),
                                color = FDColors.TextTertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = { onAccionPrincipal(incidente.accionPrincipal) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Primary,
                            contentColor = FDColors.PrimaryText
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = when (incidente.accionPrincipal) {
                                RegistroIncidenteAccion.ContactarSoporte -> "CONTACTAR SOPORTE"
                                RegistroIncidenteAccion.IrALogin -> "IR A LOGIN"
                                RegistroIncidenteAccion.Reintentar -> "REINTENTAR"
                                RegistroIncidenteAccion.ElegirOtroPlan -> "ELEGIR OTRO PLAN"
                                RegistroIncidenteAccion.VolverYCorregir -> "CORREGIR"
                                else -> "ACEPTAR"
                            },
                            style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
