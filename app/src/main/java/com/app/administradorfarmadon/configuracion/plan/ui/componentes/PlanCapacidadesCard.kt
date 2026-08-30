package com.app.administradorfarmadon.configuracion.plan.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.plan.datos.PlanFacturacionInfo
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun PlanCapacidadesCard(
    planInfo: PlanFacturacionInfo,
    modifier: Modifier = Modifier
) {
    val colores = TokensFarmadon.colores
    val s = recordarMedidaAdaptativa()

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
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = colores.textoPrincipal,
                        modifier = Modifier.size(s.iconSmall)
                    )
                }
                Column {
                    Text(
                        text = "Capacidades y Cobertura",
                        style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp * 0.95f),
                        color = colores.textoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tarifa pactada y cupo de locales habilitados",
                        style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.85f),
                        color = colores.textoSecundario,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = colores.divisor, thickness = s.separatorH)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(s.xs)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(s.btnLargeH * 1.44f),
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TARIFA COMERCIAL",
                                style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.92f, fontWeight = FontWeight.Bold),
                                color = colores.textoTerciario,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (planInfo.precioProximaRenovacion != null && planInfo.precioProximaRenovacion > 0.0 && planInfo.precioProximaRenovacion != planInfo.precioMensual) {
                                Text(
                                    text = "Próx. ${planInfo.monedaSimbolo} ${"%.2f".format(planInfo.precioProximaRenovacion)}",
                                    style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Medium),
                                    color = colores.textoSecundario
                                )
                            }
                        }
                        Text(
                            text = "${planInfo.monedaSimbolo} ${"%.2f".format(planInfo.precioMensual)} / ${planInfo.periodo}",
                            style = TokensFarmadon.tipografia.titulo2.copy(fontSize = s.textSubtitle.value.sp, fontWeight = FontWeight.Black),
                            color = colores.textoPrincipal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(s.btnLargeH * 1.33f),
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CUPO DE LOCALES",
                                style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Bold),
                                color = colores.textoTerciario,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (planInfo.sucursalesActivas >= planInfo.maxSucursales && planInfo.sucursalesActivas > 0) {
                                Text(
                                    text = "Cupo cubierto",
                                    style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.85f, fontWeight = FontWeight.Medium),
                                    color = colores.textoSecundario
                                )
                            }
                        }
                        Text(
                            text = "${planInfo.sucursalesActivas} de ${planInfo.maxSucursales} ${if (planInfo.maxSucursales == 1) "Sede" else "Sedes"}",
                            style = TokensFarmadon.tipografia.titulo2.copy(fontSize = s.textSubtitle.value.sp, fontWeight = FontWeight.Black),
                            color = colores.textoPrincipal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }

            if (planInfo.features.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(s.xs)) {
                    Text(
                        text = "HERRAMIENTAS INCLUIDAS",
                        style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.95f, fontWeight = FontWeight.Bold),
                        color = colores.textoTerciario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(s.xs * 0.8f),
                        verticalArrangement = Arrangement.spacedBy(s.xs * 0.8f)
                    ) {
                        planInfo.features.forEach { feature ->
                            Surface(
                                color = colores.cardElevada,
                                shape = RoundedCornerShape(s.radiusChip),
                                border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .height(s.btnSmallH * 0.84f)
                                        .padding(horizontal = s.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(s.xs * 0.7f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(s.xs * 0.5f)
                                            .background(colores.textoTerciario, RoundedCornerShape(s.xs * 0.25f))
                                    )
                                    Text(
                                        text = feature.uppercase(),
                                        style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp, fontWeight = FontWeight.SemiBold),
                                        color = colores.textoPrincipal,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
