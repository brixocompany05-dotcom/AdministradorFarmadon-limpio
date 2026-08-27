package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.tabla

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes.*

/**
 * ProductRow PREMIUM 2026 — Inventario costoso y legible 8h
 * Avatar inicial, badge categoría, dot stock, altura s.*, espaciado geométrico.
 */
@Composable
fun ProductRow(
    product: PharmProduct,
    s: MedidaAdaptativa,
    cols: InventoryColumns,
    isCompact: Boolean = false,
    isSelected: Boolean = false,
    isAlternate: Boolean = false,
    onProductClick: (PharmProduct) -> Unit
) {
    val textPrimary = SaaSTextPrimary
    val textSecondary = SaaSTextSecondary
    val borderColor = SaaSBorder
    val inicial = product.name.trim().firstOrNull()?.uppercase() ?: "•"
    val accentAvatar = when {
        product.stock <= 0 -> SaaSError
        product.stock <= 5 -> SaaSWarning
        else -> SaaSPrimary
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = s.btnMediumH * 1.18f)
                .background(
                    if (isSelected) textPrimary.copy(alpha = 0.035f) else Color.Transparent
                )
                .drawBehind {
                    if (isSelected) {
                        val strokeWidth = s.borderWidth.toPx() * 1.4f
                        drawLine(
                            color = textPrimary,
                            start = androidx.compose.ui.geometry.Offset(s.xs.toPx() * 0.3f, s.xs.toPx()),
                            end = androidx.compose.ui.geometry.Offset(s.xs.toPx() * 0.3f, size.height - s.xs.toPx()),
                            strokeWidth = strokeWidth
                        )
                    }
                    val lineY = size.height - s.separatorH.toPx()
                    drawLine(
                        color = borderColor,
                        start = androidx.compose.ui.geometry.Offset(s.padScreenH.toPx(), lineY),
                        end = androidx.compose.ui.geometry.Offset(size.width - s.padScreenH.toPx(), lineY),
                        strokeWidth = s.separatorH.toPx() * 0.7f
                    )
                }
                .clickable(onClick = { onProductClick(product) })
                .padding(horizontal = s.padScreenH, vertical = s.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar premium — inicial con fondo accent 8%
            Box(
                modifier = Modifier
                    .size(s.iconLarge * 0.95f)
                    .clip(RoundedCornerShape(s.radiusChip))
                    .background(accentAvatar.copy(alpha = 0.09f))
                    .border(s.borderWidth * 0.7f, accentAvatar.copy(alpha = 0.18f), RoundedCornerShape(s.radiusChip)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = inicial,
                    color = accentAvatar,
                    fontWeight = FontWeight.Black,
                    fontSize = s.textBody.value.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(s.xs))

            // Col 1: Producto
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs * 0.7f)
                ) {
                    Text(
                        text = product.name,
                        color = if (!product.activo) textSecondary else textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = s.textBody.value.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!product.activo) {
                        androidx.compose.material3.Surface(
                            color = SaaSBorder.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(s.radiusChip * 0.5f),
                            border = androidx.compose.foundation.BorderStroke(s.borderWidth * 0.6f, SaaSBorder)
                        ) {
                            Text(
                                text = "PAUSADO",
                                color = textSecondary,
                                fontSize = s.textLabel.value.sp * 0.75f,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp,
                                modifier = Modifier.padding(horizontal = s.xs * 0.7f, vertical = 2.dp)
                            )
                        }
                    }
                }

                val subtexto = listOfNotNull(
                    product.presentation.ifBlank { null },
                    product.laboratory.takeIf { it.isNotBlank() && it != "Genérico" },
                    product.code.takeIf { it.isNotBlank() }?.let { "• $it" }
                ).joinToString(" · ")

                if (subtexto.isNotBlank()) {
                    Text(
                        text = subtexto,
                        color = textSecondary,
                        fontSize = s.textBody.value.sp * 0.85f,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Col 2: Categoría — pill premium
            Box(modifier = Modifier.width(cols.category), contentAlignment = Alignment.CenterStart) {
                val firstCategory = product.category.split(",").firstOrNull()?.trim() ?: "General"
                androidx.compose.material3.Surface(
                    color = SaaSBackground,
                    shape = RoundedCornerShape(s.radiusChip * 0.6f),
                    border = androidx.compose.foundation.BorderStroke(s.borderWidth * 0.6f, SaaSBorder.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = firstCategory.uppercase(),
                        color = textSecondary,
                        fontSize = s.textLabel.value.sp * 0.78f,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = s.xs * 0.7f, vertical = 3.dp)
                    )
                }
            }

            // Col 3: Stock — dot + texto
            Box(modifier = Modifier.width(cols.stock), contentAlignment = Alignment.CenterEnd) {
                val enCuarentenaSola = product.stock <= 0 && product.stockBloqueado > 0
                val colorCuarentena = Color(0xFF8B5CF6)
                val stockColor = when {
                    enCuarentenaSola -> colorCuarentena
                    product.stock <= 0 -> SaaSError
                    product.stock <= 5 -> SaaSWarning
                    else -> textPrimary
                }
                val stockTexto = product.stockHumanReadable.ifBlank { "${product.stock}" }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.5f)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(stockColor))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stockTexto,
                            color = stockColor,
                            fontWeight = FontWeight.Black,
                            fontSize = s.textBody.value.sp
                        )
                        if (enCuarentenaSola || product.stockBloqueado > 0) {
                            Text(
                                text = if (enCuarentenaSola) "cuarentena" else "${product.stockBloqueado} bloq.",
                                color = colorCuarentena,
                                fontSize = s.textLabel.value.sp * 0.78f,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Col 4: Mínimo
            Box(modifier = Modifier.width(cols.min), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = product.minStockHumanReadable.ifBlank { "${product.minStock}" },
                    color = textSecondary,
                    fontSize = s.textBody.value.sp * 0.92f,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
