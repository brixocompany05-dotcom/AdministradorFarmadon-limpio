package com.app.administradorfarmadon.configuracion.sucursales.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun SucursalItemCard(
    sucursal: Sucursal,
    estaSeleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    s: MedidaAdaptativa = recordarMedidaAdaptativa()
) {
    val colores = TokensFarmadon.colores

    val borderColor = if (estaSeleccionada) {
        colores.textoPrincipal.copy(alpha = 0.5f)
    } else {
        colores.cardBorde
    }

    val backgroundColor = if (estaSeleccionada) {
        colores.cardElevada
    } else {
        colores.cardBase
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(s.radiusCard * 0.75f))
            .border(
                width = if (estaSeleccionada) s.borderWidth * 1.2f else s.borderWidth * 0.7f,
                color = borderColor.copy(alpha = if (estaSeleccionada) 0.6f else 0.2f),
                shape = RoundedCornerShape(s.radiusCard * 0.75f)
            )
            .bounceClick()
            .clickable { onClick() },
        shape = RoundedCornerShape(s.radiusCard * 0.75f),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = s.padCard * 0.75f, vertical = s.padCard * 0.6f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.xs)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(s.iconLarge * 1.28f)
                        .clip(RoundedCornerShape(s.radiusChip))
                        .background(if (colores.esTemaClaro) colores.fondoBase else colores.textoPrincipal.copy(alpha = 0.05f))
                        .border(s.borderWidth * 0.6f, colores.cardBorde, RoundedCornerShape(s.radiusChip)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = if (estaSeleccionada) colores.textoPrincipal else colores.textoTerciario,
                        modifier = Modifier.size(s.iconSmall)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(s.xs * 1.15f)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (sucursal.activa) colores.estadoExito else colores.estadoAlerta)
                        .border(s.borderWidth, backgroundColor, androidx.compose.foundation.shape.CircleShape)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs * 0.7f)
                ) {
                    Text(
                        text = sucursal.nombre,
                        style = TokensFarmadon.tipografia.titulo3.copy(
                            fontSize = s.textBody.value.sp,
                            fontWeight = if (estaSeleccionada) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = colores.textoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (sucursal.esPrincipal) {
                        Surface(
                            color = colores.textoPrincipal.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(s.radiusChip * 0.4f)
                        ) {
                            Text(
                                text = "M",
                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                    fontSize = s.textLabel.value.sp * 0.78f,
                                    fontWeight = FontWeight.Black
                                ),
                                color = colores.textoSecundario,
                                modifier = Modifier.padding(horizontal = s.xs * 0.5f, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = sucursal.direccion.ifBlank { "Sin dirección" },
                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.85f),
                    color = colores.textoTerciario.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (estaSeleccionada) colores.textoPrincipal else colores.textoTerciario.copy(alpha = 0.2f),
                modifier = Modifier.size(s.iconTiny)
            )
        }
    }
}
