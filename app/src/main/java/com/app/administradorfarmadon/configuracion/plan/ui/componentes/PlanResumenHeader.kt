package com.app.administradorfarmadon.configuracion.plan.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun PlanResumenHeader(
    onVolver: () -> Unit,
    planNombre: String,
    modifier: Modifier = Modifier
) {
    val colores = TokensFarmadon.colores
    val s = recordarMedidaAdaptativa()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
        ) {
            Surface(
                onClick = onVolver,
                color = colores.cardBase,
                shape = RoundedCornerShape(s.radiusButton),
                border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)
            ) {
                Row(
                    modifier = Modifier
                        .height(s.btnSmallH * 1.15f)
                        .padding(horizontal = s.padCard * 0.85f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = colores.textoPrincipal,
                        modifier = Modifier.size(s.iconTiny)
                    )
                    Text(
                        text = "VOLVER",
                        style = TokensFarmadon.tipografia.etiqueta.copy(
                            fontSize = s.textLabel.value.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = colores.textoPrincipal,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Plan y Facturación",
                    style = TokensFarmadon.tipografia.titulo1.copy(fontSize = s.textTitle.value.sp),
                    color = colores.textoPrincipal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Expediente de suscripción, cronograma de vigencia y comprobantes",
                    style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp),
                    color = colores.textoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(s.gapMedium))

        Surface(
            color = colores.cardElevada,
            shape = RoundedCornerShape(s.radiusChip),
            border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)
        ) {
            Row(
                modifier = Modifier
                    .height(s.btnSmallH * 1.15f)
                    .padding(horizontal = s.padCard * 0.75f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(s.xs)
            ) {
                Box(
                    modifier = Modifier
                        .size(s.xs)
                        .background(colores.estadoExito, RoundedCornerShape(s.xs * 0.5f))
                )
                Text(
                    text = "PLAN ${planNombre.ifBlank { "SIN PLAN ASIGNADO" }.uppercase()}",
                    style = TokensFarmadon.tipografia.etiqueta.copy(
                        fontSize = s.textLabel.value.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp
                    ),
                    color = colores.textoPrincipal,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
