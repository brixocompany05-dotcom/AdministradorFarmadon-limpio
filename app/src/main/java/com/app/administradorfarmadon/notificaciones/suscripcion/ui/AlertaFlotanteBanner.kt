package com.app.administradorfarmadon.notificaciones.suscripcion.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.notificaciones.suscripcion.datos.AlertaSuscripcionItem
import com.app.administradorfarmadon.notificaciones.suscripcion.datos.TipoAlertaSuscripcion

@Composable
fun AlertaFlotanteBanner(
    alerta: AlertaSuscripcionItem?,
    onVerDetalle: () -> Unit,
    onDescartar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colores = TokensFarmadon.colores

    AnimatedVisibility(
        visible = alerta != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.9f, stiffness = 500f)
        ) + fadeOut(),
        modifier = modifier
    ) {
        if (alerta != null) {
            val (icono, colorAcento, colorFondoSutil) = when (alerta.tipo) {
                TipoAlertaSuscripcion.BIENVENIDA_INICIAL -> Triple(
                    Icons.Default.Celebration,
                    colores.botonPrimarioFondo,
                    colores.cardElevada
                )
                TipoAlertaSuscripcion.PAGO_EXITOSO -> Triple(
                    Icons.Default.CheckCircle,
                    colores.estadoExito,
                    colores.exitoSutil
                )
                TipoAlertaSuscripcion.CORTESIA_OTORGADA -> Triple(
                    Icons.Default.CardGiftcard,
                    colores.estadoExito,
                    colores.exitoSutil
                )
                TipoAlertaSuscripcion.FIN_PRUEBA -> Triple(
                    Icons.Default.Verified,
                    colores.botonPrimarioFondo,
                    colores.cardElevada
                )
                TipoAlertaSuscripcion.CAMBIO_PLAN -> Triple(
                    Icons.Default.SwapHoriz,
                    colores.botonPrimarioFondo,
                    colores.cardElevada
                )
                TipoAlertaSuscripcion.AVISO_PREVENTIVO_48H -> Triple(
                    Icons.Default.Warning,
                    colores.estadoAlerta,
                    colores.alertaSutil
                )
                TipoAlertaSuscripcion.AVISO_PREVENTIVO_7D -> Triple(
                    Icons.Default.Event,
                    colores.estadoAlerta,
                    colores.alertaSutil
                )
                TipoAlertaSuscripcion.COMPROBANTE_OBSERVADO -> Triple(
                    Icons.Default.ReportProblem,
                    colores.estadoAlerta,
                    colores.alertaSutil
                )
                TipoAlertaSuscripcion.COMPROBANTE_EN_REVISION -> Triple(
                    Icons.Default.Schedule,
                    colores.botonPrimarioFondo,
                    colores.cardElevada
                )
            }


            Surface(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .shadow(14.dp, RoundedCornerShape(16.dp), spotColor = colorAcento.copy(alpha = 0.3f))
                    .pointerInput(alerta.id) {
                        // Gesto táctil Swipe-Up (deslizar hacia arriba con el dedo para descartar)
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -12) {
                                onDescartar(alerta.id)
                            }
                        }
                    },
                color = colores.cardElevada,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    colorAcento.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Ícono con aura sutil
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorFondoSutil)
                            .border(1.dp, colorAcento.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icono,
                            contentDescription = null,
                            tint = colorAcento,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Textos descriptivos de la alerta
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = alerta.titulo,
                            style = TokensFarmadon.tipografia.titulo3.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colores.textoPrincipal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = alerta.mensaje,
                            style = TokensFarmadon.tipografia.cuerpo.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            color = colores.textoSecundario,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Botón de Acción Rápida (Ver Plan / Subsanar)
                    Surface(
                        onClick = onVerDetalle,
                        color = colorAcento.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorAcento.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (alerta.tipo == TipoAlertaSuscripcion.COMPROBANTE_OBSERVADO) "SUBSANAR" else "VER PLAN",
                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = colorAcento,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }


                    // Botón de Cierre Manual
                    IconButton(
                        onClick = { onDescartar(alerta.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar aviso",
                            tint = colores.textoTerciario,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
