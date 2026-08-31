package com.app.administradorfarmadon.configuracion.plan.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.plan.datos.PlanFacturacionInfo
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun PlanCronogramaCard(
    planInfo: PlanFacturacionInfo,
    onExplorarPlanes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colores = TokensFarmadon.colores
    val s = recordarMedidaAdaptativa()

    val badgeTexto = when {
        planInfo.estadoSuscripcion == "sin_suscripcion" -> "SIN SUSCRIPCIÓN"
        planInfo.estadoSuscripcion == "pausado" -> "EN PAUSA COMERCIAL"
        planInfo.estadoSuscripcion == "suspendido" -> "SUSPENDIDO"
        planInfo.estadoSuscripcion == "vencida" -> "VENCIDA"
        planInfo.estadoSuscripcion == "por_vencer" -> "POR VENCER"
        planInfo.tieneBeneficioCortesia -> "CORTESÍA (+${planInfo.diasCortesia} DÍAS)"
        planInfo.estadoSuscripcion == "prueba" -> "PERÍODO DE PRUEBA"
        else -> "ACTIVA"
    }

    val badgeColor = when (planInfo.estadoSuscripcion) {
        "vencida", "suspendido" -> colores.estadoPeligro
        "por_vencer" -> colores.estadoAlerta
        "pausado" -> colores.textoPrincipal
        else -> colores.estadoExito
    }

    val tieneCicloReal = (planInfo.fechaInicioMs > 0L && planInfo.fechaFinMs > planInfo.fechaInicioMs) ||
            (planInfo.fechaInicio.isNotBlank() && planInfo.fechaFin.isNotBlank())
    val estaVencida = planInfo.estadoSuscripcion == "vencida"
    val sinCiclo = planInfo.estadoSuscripcion == "sin_suscripcion" || !tieneCicloReal
    val totalDiasCiclo = remember(planInfo.fechaInicioMs, planInfo.fechaFinMs) {
        if (planInfo.fechaInicioMs > 0L && planInfo.fechaFinMs > planInfo.fechaInicioMs) {
            ((planInfo.fechaFinMs - planInfo.fechaInicioMs) / (24 * 60 * 60 * 1000f)).coerceAtLeast(1f)
        } else {
            0f
        }
    }
    val diasConsumidos = if (totalDiasCiclo > 0f) (totalDiasCiclo - planInfo.diasRestantesTotal).coerceIn(0f, totalDiasCiclo) else 0f
    val progresoCiclo = if (sinCiclo || estaVencida || totalDiasCiclo <= 0f) 0f else (diasConsumidos / totalDiasCiclo).coerceIn(0f, 1f)
    val colorBarra = if (estaVencida) colores.estadoPeligro else colores.botonPrimarioFondo

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colores.cardElevada,
        shape = RoundedCornerShape(s.radiusCard),
        border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)
    ) {
        Column(
            modifier = Modifier.padding(s.padCard),
            verticalArrangement = Arrangement.spacedBy(s.gapMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    Box(
                        modifier = Modifier
                            .size(s.iconLarge * 1.35f)
                            .background(colores.cardElevada, RoundedCornerShape(s.radiusChip))
                            .border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusChip)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = colores.textoPrincipal,
                            modifier = Modifier.size(s.iconSmall)
                        )
                    }
                    Column {
                        Text(
                            text = "Cronograma y Vigencia",
                            style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp * 0.95f),
                            color = colores.textoPrincipal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Control del ciclo de facturación y vigencia del servicio",
                            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.85f),
                            color = colores.textoSecundario,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(s.xs))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs * 0.8f)
                ) {
                    if (!planInfo.sincronizadoServidor) {
                        Surface(
                            color = colores.alertaSutil,
                            shape = RoundedCornerShape(s.radiusChip),
                            border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.estadoAlerta)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = s.xs, vertical = s.xs * 0.6f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(s.xs * 0.5f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = colores.estadoAlerta,
                                    modifier = Modifier.size(s.iconTiny)
                                )
                                Text(
                                    text = "SINCRONIZANDO...",
                                    style = TokensFarmadon.tipografia.etiqueta.copy(
                                        fontSize = s.textLabel.value.sp * 0.9f,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colores.estadoAlerta
                                )
                            }
                        }
                    }

                    Surface(
                        color = colores.cardElevada,
                        shape = RoundedCornerShape(s.radiusChip),
                        border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = s.xs, vertical = s.xs * 0.7f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.xs * 0.7f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(s.xs * 0.75f)
                                    .background(badgeColor, RoundedCornerShape(s.xs * 0.4f))
                            )
                            Text(
                                text = badgeTexto,
                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                    fontSize = s.textLabel.value.sp * 0.9f,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = colores.textoPrincipal,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colores.divisor, thickness = s.separatorH)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(s.xs)
            ) {
                DatoVigenciaBloque(
                    label = "FECHA DE ALTA",
                    valor = planInfo.fechaInicio.ifBlank { "—”" },
                    icono = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f)
                )
                DatoVigenciaBloque(
                    label = if (planInfo.tieneBeneficioCortesia) "PRÓRROGA" else "DÍAS PRUEBA",
                    valor = if (planInfo.tieneBeneficioCortesia) "+${planInfo.diasCortesia}d" else if (planInfo.diasPruebaContratados > 0) "${planInfo.diasPruebaContratados}d" else "—”",
                    icono = Icons.Default.CardGiftcard,
                    modifier = Modifier.weight(1f)
                )
                DatoVigenciaBloque(
                    label = "PRÓXIMO VENCIMIENTO",
                    valor = planInfo.fechaFin.ifBlank { "—”" },
                    icono = Icons.Default.Event,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                color = colores.fondoBase,
                shape = RoundedCornerShape(s.radiusInput),
                border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(s.sm),
                    verticalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONSUMO DEL CICLO MENSUAL",
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.95f, fontWeight = FontWeight.Bold),
                            color = colores.textoTerciario
                        )
                        Text(
                            text = when {
                                planInfo.estadoSuscripcion == "sin_suscripcion" -> "Sin suscripción"
                                !tieneCicloReal -> "Sin ciclo activo"
                                else -> "${planInfo.diasRestantesTotal} días restantes"
                            },
                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontSize = s.textLabel.value.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (planInfo.diasRestantesTotal <= 2
                                && planInfo.estadoSuscripcion != "sin_suscripcion"
                                && planInfo.estadoSuscripcion != "vencida") colores.estadoPeligro else colores.textoPrincipal
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(s.separatorH * 6f)
                            .clip(RoundedCornerShape(s.xs * 0.4f))
                            .background(colores.cardElevada)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progresoCiclo)
                                .clip(RoundedCornerShape(s.xs * 0.4f))
                                .background(colorBarra)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Inicio: ${planInfo.fechaInicio.ifBlank { "—”" }}",
                            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.85f),
                            color = colores.textoTerciario
                        )
                        Text(
                            text = "Corte: ${planInfo.fechaFin.ifBlank { "—”" }}",
                            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.85f),
                            color = colores.textoTerciario
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onExplorarPlanes,
                modifier = Modifier.fillMaxWidth().height(s.btnMediumH * 0.88f),
                shape = RoundedCornerShape(s.radiusButton),
                border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = colores.cardElevada)
            ) {
                Icon(Icons.Default.Layers, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconSmall))
                Spacer(Modifier.width(s.xs))
                Text(
                    text = "CATÁLOGO DE PLANES Y OPCIONES DE MEJORA",
                    style = TokensFarmadon.tipografia.etiqueta.copy(
                        fontSize = s.textLabel.value.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    ),
                    color = colores.textoPrincipal
                )
            }
        }
    }
}

@Composable
private fun DatoVigenciaBloque(
    label: String,
    valor: String,
    icono: ImageVector,
    modifier: Modifier = Modifier
) {
    val colores = TokensFarmadon.colores
    val s = recordarMedidaAdaptativa()

    Surface(
        modifier = modifier.height(s.btnLargeH * 1.33f),
        color = colores.fondoBase,
        shape = RoundedCornerShape(s.radiusInput),
        border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = s.sm, vertical = s.xs),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(s.xs * 0.7f)
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colores.textoTerciario,
                    modifier = Modifier.size(s.iconTiny)
                )
                Text(
                    text = label,
                    style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Bold),
                    color = colores.textoTerciario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = valor,
                style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Bold),
                color = colores.textoPrincipal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }
    }
}
