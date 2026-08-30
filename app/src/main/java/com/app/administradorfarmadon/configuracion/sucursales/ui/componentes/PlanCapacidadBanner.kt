package com.app.administradorfarmadon.configuracion.sucursales.ui.componentes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun PlanCapacidadBanner(
    planNombre: String,
    totalSucursales: Int,
    maxSucursales: Int,
    porcentaje: Float,
    puedeCrearMas: Boolean,
    modifier: Modifier = Modifier,
    s: MedidaAdaptativa = recordarMedidaAdaptativa()
) {
    val colores = TokensFarmadon.colores
    val animatedProgress by animateFloatAsState(
        targetValue = porcentaje,
        animationSpec = tween(durationMillis = 500),
        label = "capacidadProgress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusCard)),
        shape = RoundedCornerShape(s.radiusCard),
        color = colores.cardBase
    ) {
        Column(
            modifier = Modifier.padding(s.padCard * 0.75f),
            verticalArrangement = Arrangement.spacedBy(s.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "PLAN CONTRATADO",
                        style = TokensFarmadon.tipografia.etiqueta.copy(
                            fontSize = s.textLabel.value.sp * 0.9f,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colores.textoTerciario
                    )
                    Text(
                        text = planNombre.ifBlank { "Plan Estándar" }.uppercase(),
                        style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Bold),
                        color = colores.textoPrincipal
                    )
                }

                Text(
                    text = "$totalSucursales / $maxSucursales",
                    style = TokensFarmadon.tipografia.etiqueta.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = s.textLabel.value.sp
                    ),
                    color = colores.textoPrincipal
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(s.separatorH * 4f)
                    .clip(CircleShape)
                    .background(FDColors.Border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(FDColors.TextPrimary)
                )
            }
        }
    }
}
