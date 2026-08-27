package com.app.administradorfarmadon.configuracion.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

/**
 * ConfigOptionCard PREMIUM 2026 — Enterprise Atrium
 * Altura geométrica, accent top único, icon 48dp, jerarquía completa.
 * Cero fijos: todo s.* . Cero Color.White: solo tokens.
 * Sensación: calma + precisión + profundidad sutil.
 */
@Composable
fun ConfigOptionCard(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    badgeTexto: String? = null,
    accentColor: Color? = null,
    metaTexto: String? = null,
    s: MedidaAdaptativa = recordarMedidaAdaptativa()
) {
    val colores = TokensFarmadon.colores
    val accent = accentColor ?: when {
        titulo.contains("Sucursal", ignoreCase = true) -> colores.estadoExito
        titulo.contains("Plan", ignoreCase = true) -> colores.textoPrincipal
        titulo.contains("Personal", ignoreCase = true) -> colores.estadoAlerta
        else -> colores.textoTerciario
    }

    val cardHeight = s.btnLargeH * 2.35f // ~126dp geométrico, respira en cualquier tablet

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(RoundedCornerShape(s.radiusCard))
            .border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusCard))
            .bounceClick()
            .clickable { onClick() },
        shape = RoundedCornerShape(s.radiusCard),
        color = if (habilitado) colores.cardBase else colores.cardBase.copy(alpha = 0.62f),
        shadowElevation = if (habilitado) 1.dp else 0.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Accent top — s.separatorH*3 (≈3dp adaptativo) línea sutil única
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(s.separatorH * 3f)
                    .background(if (habilitado) accent else colores.cardBorde.copy(alpha = 0.5f))
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = s.padCard, vertical = s.padCard * 0.85f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                // Icono premium 48dp base escalado
                Box(
                    modifier = Modifier
                        .size(s.iconLarge * 1.7f)
                        .clip(RoundedCornerShape(s.radiusChip))
                        .background(
                            if (habilitado) accent.copy(alpha = if (colores.esTemaClaro) 0.10f else 0.14f)
                            else colores.cardElevada
                        )
                        .border(
                            s.borderWidth * 0.8f,
                            if (habilitado) accent.copy(alpha = 0.22f) else colores.cardBorde,
                            RoundedCornerShape(s.radiusChip)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = if (habilitado) accent else colores.textoTerciario,
                        modifier = Modifier.size(s.iconSmall * 1.1f)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(s.xs * 0.45f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.xs * 0.7f)
                        ) {
                            Text(
                                text = titulo,
                                style = TokensFarmadon.tipografia.titulo3.copy(
                                    fontSize = s.textSubtitle.value.sp * 0.95f,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (habilitado) colores.textoPrincipal else colores.textoSecundario,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!badgeTexto.isNullOrBlank()) {
                                Surface(
                                    color = if (habilitado) colores.cardElevada else colores.fondoBase,
                                    shape = RoundedCornerShape(s.radiusChip * 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(s.borderWidth * 0.6f, colores.cardBorde)
                                ) {
                                    Text(
                                        text = badgeTexto.uppercase(),
                                        style = TokensFarmadon.tipografia.etiqueta.copy(
                                            fontSize = s.textLabel.value.sp * 0.78f,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = colores.textoTerciario,
                                        modifier = Modifier.padding(horizontal = s.xs * 0.7f, vertical = s.xs * 0.32f)
                                    )
                                }
                            }
                        }

                        Text(
                            text = subtitulo,
                            style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                fontSize = s.textBody.value.sp * 0.92f,
                                lineHeight = s.textBody.value.sp * 1.22f
                            ),
                            color = colores.textoTerciario,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!metaTexto.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.6f)) {
                            Box(Modifier.size(s.xs * 0.6f).clip(androidx.compose.foundation.shape.CircleShape).background(accent.copy(alpha = 0.9f)))
                            Text(
                                text = metaTexto,
                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                    fontSize = s.textLabel.value.sp * 0.82f,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = colores.textoSecundario,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Chevron premium con círculo sutil
                Box(
                    modifier = Modifier
                        .size(s.iconMedium)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(colores.fondoBase)
                        .border(s.borderWidth * 0.7f, colores.cardBorde, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (habilitado) Icons.Default.ChevronRight else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (habilitado) colores.textoPrincipal else colores.textoTerciario.copy(alpha = 0.6f),
                        modifier = Modifier.size(s.iconTiny)
                    )
                }
            }
        }
    }
}
