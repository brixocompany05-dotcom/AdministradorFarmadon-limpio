package com.app.administradorfarmadon.configuracion.usuarios.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.usuarios.datos.UsuarioFarmacia
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun UsuarioItemCard(
    usuario: UsuarioFarmacia,
    estaSeleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    esUsuarioActual: Boolean = false,
    s: MedidaAdaptativa = recordarMedidaAdaptativa()
) {
    val colores = TokensFarmadon.colores
    val esActivo = usuario.acceso && usuario.estado == "ACTIVO"

    val borderColor = if (estaSeleccionado) {
        colores.textoPrincipal
    } else {
        colores.cardBorde
    }

    val backgroundColor = if (estaSeleccionado) {
        colores.cardElevada
    } else {
        colores.cardBase
    }

    // Iniciales en 2 letras
    val partes = usuario.nombre.trim().split(" ").filter { it.isNotBlank() }
    val iniciales = when {
        partes.size >= 2 -> "${partes[0].take(1)}${partes[1].take(1)}".uppercase()
        partes.isNotEmpty() -> partes[0].take(2).uppercase()
        else -> "US"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(s.radiusCard))
            .border(
                width = if (estaSeleccionado) s.borderWidth * 1.5f else s.borderWidth,
                color = if (estaSeleccionado) borderColor else borderColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(s.radiusCard)
            )
            .bounceClick()
            .clickable { onClick() },
        shape = RoundedCornerShape(s.radiusCard),
        color = backgroundColor,
        shadowElevation = if (estaSeleccionado) 2.dp else 0.dp,
        tonalElevation = if (estaSeleccionado) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = s.padCard, vertical = s.padCard * 0.85f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.sm)
        ) {
            // Avatar —” tamaño físico s.iconLarge (28dp base escalado)
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(s.iconLarge * 1.5f)
                        .clip(RoundedCornerShape(s.radiusChip))
                        .background(if (estaSeleccionado) colores.textoPrincipal.copy(alpha = 0.08f) else colores.fondoBase)
                        .border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusChip)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iniciales,
                        style = TokensFarmadon.tipografia.titulo3.copy(
                            fontSize = s.textInput.value.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = if (estaSeleccionado) colores.textoPrincipal else colores.textoSecundario
                    )
                }

                Surface(
                    modifier = Modifier
                        .size(s.xs * 1.5f)
                        .clip(CircleShape)
                        .border(2.dp, backgroundColor, CircleShape),
                    color = if (esActivo) colores.estadoExito else colores.estadoPeligro
                ) {}
            }

            // Información y Badge —” gaps simétricos s.xs
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(s.xs * 0.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    Text(
                        text = usuario.nombre,
                        style = TokensFarmadon.tipografia.titulo3.copy(
                            fontSize = s.textInput.value.sp,
                            fontWeight = if (estaSeleccionado) FontWeight.Black else FontWeight.Bold
                        ),
                        color = colores.textoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (esUsuarioActual) {
                        Surface(
                            color = colores.textoPrincipal.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(s.radiusChip * 0.6f)
                        ) {
                            Text(
                                text = "Tíš",
                                style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.85f, fontWeight = FontWeight.Black),
                                color = colores.textoPrincipal,
                                modifier = Modifier.padding(horizontal = s.xs * 0.75f, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs * 0.75f)
                ) {
                    Surface(
                        color = colores.fondoBase,
                        shape = RoundedCornerShape(s.radiusChip * 0.6f),
                        border = androidx.compose.foundation.BorderStroke(s.borderWidth * 0.6f, colores.cardBorde)
                    ) {
                        Text(
                            text = usuario.rolNombre.uppercase(),
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp),
                            color = colores.textoTerciario,
                            modifier = Modifier.padding(horizontal = s.xs * 0.75f, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "—¢ ${usuario.sucursalNombre}",
                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp),
                        color = colores.textoTerciario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (estaSeleccionado) colores.textoPrincipal else colores.textoTerciario.copy(alpha = 0.3f),
                modifier = Modifier.size(s.iconSmall)
            )
        }
    }
}
