package com.app.administradorfarmadon.configuracion.plan.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.plan.datos.HistorialPagoItem
import com.app.administradorfarmadon.configuracion.plan.datos.PlanFacturacionInfo
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun HistorialFacturacionCard(
    planInfo: PlanFacturacionInfo,
    historial: List<HistorialPagoItem>,
    onVerConstancia: (HistorialPagoItem) -> Unit = {},
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
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = colores.textoPrincipal,
                        modifier = Modifier.size(s.iconSmall)
                    )
                }
                Column {
                    Text(
                        text = "Historial de Pagos y Facturación",
                        style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp * 0.95f),
                        color = colores.textoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Comprobantes y abonos asentados por BRIXO",
                        style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.92f),
                        color = colores.textoSecundario,
                        maxLines = 1,
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
                        Text(
                            text = "TOTAL PAGADO",
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Bold),
                            color = colores.textoTerciario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${planInfo.monedaSimbolo} ${"%.2f".format(planInfo.precioPagado)}",
                            style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp * 0.95f, fontWeight = FontWeight.Black),
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
                        Text(
                            text = "CUOTAS ABONADAS",
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Bold),
                            color = colores.textoTerciario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${planInfo.pagosRealizados} ${if (planInfo.pagosRealizados == 1) "Pago" else "Pagos"}",
                            style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp * 0.95f, fontWeight = FontWeight.Black),
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
                        Text(
                            text = "SALDO PENDIENTE",
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Bold),
                            color = if (planInfo.saldoPendiente > 0.0) colores.estadoPeligro else colores.textoTerciario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${planInfo.monedaSimbolo} ${"%.2f".format(planInfo.saldoPendiente)}",
                            style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp * 0.95f, fontWeight = FontWeight.Black),
                            color = if (planInfo.saldoPendiente > 0.0) colores.estadoPeligro else colores.textoPrincipal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }

            Text(
                text = "REGISTRO DE MOVIMIENTOS",
                style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.95f, fontWeight = FontWeight.Bold),
                color = colores.textoTerciario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (historial.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colores.fondoBase,
                    shape = RoundedCornerShape(s.radiusInput),
                    border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)
                ) {
                    Column(
                        modifier = Modifier.padding(s.padCard),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(s.xs * 0.7f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = colores.textoTerciario,
                            modifier = Modifier.size(s.iconMedium)
                        )
                        Text(
                            text = "Sin comprobantes registrados",
                            style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Bold),
                            color = colores.textoPrincipal
                        )
                        Text(
                            text = "Las renovaciones y pagos asentados por BRIXO aparecerán aquí automáticamente.",
                            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.92f),
                            color = colores.textoSecundario
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(s.xs * 0.8f)
                ) {
                    historial.take(5).forEach { item ->
                        Surface(
                            onClick = { onVerConstancia(item) },
                            modifier = Modifier.fillMaxWidth(),
                            color = colores.cardElevada,
                            shape = RoundedCornerShape(s.radiusInput),
                            border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)
                        ) {
                            Row(
                                modifier = Modifier.padding(s.sm),
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
                                            .size(s.iconLarge * 0.95f)
                                            .background(colores.fondoBase, RoundedCornerShape(s.radiusChip * 0.6f))
                                            .border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusChip * 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                            contentDescription = null,
                                            tint = colores.textoPrincipal,
                                            modifier = Modifier.size(s.iconTiny)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = item.concepto,
                                            style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Bold),
                                            color = colores.textoPrincipal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.fecha} · ${item.admin}",
                                            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.85f),
                                            color = colores.textoSecundario,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(Modifier.width(s.xs))
                                Text(
                                    text = "${planInfo.monedaSimbolo} ${"%.2f".format(item.monto)}",
                                    style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Black),
                                    color = if (item.monto > 0.0) colores.estadoExito else colores.textoTerciario,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colores.fondoBase,
                shape = RoundedCornerShape(s.radiusInput),
                border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)
            ) {
                Row(
                    modifier = Modifier.padding(s.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(s.iconLarge * 1.1f)
                            .background(colores.cardElevada, RoundedCornerShape(s.radiusChip)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = colores.textoPrincipal,
                            modifier = Modifier.size(s.iconSmall)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¿Necesitas ampliar cupos o cambiar de plan?",
                            style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Bold),
                            color = colores.textoPrincipal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Comunícate con tu asesor comercial de BRIXO para coordinar mejoras o reportar pagos bancarios.",
                            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.88f, lineHeight = s.textBody.value.sp * 1.15f),
                            color = colores.textoSecundario
                        )
                    }
                }
            }
        }
    }
}
