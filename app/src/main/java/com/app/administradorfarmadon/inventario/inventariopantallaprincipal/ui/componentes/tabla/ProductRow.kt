package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.tabla

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes.*

/**
 * ProductRow ENTERPRISE LARGE-SCREEN — Alta Legibilidad y Confort Visual
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

    val firstCategory = product.category.substringBefore(',').trim().ifBlank { "Sin categoría" }
    val labVal = product.laboratory
    val subtexto = when {
        product.presentation.isNotBlank() && labVal.isNotBlank() && labVal != "Genérico" ->
            "${product.presentation} · $labVal"
        product.presentation.isNotBlank() -> product.presentation
        labVal.isNotBlank() && labVal != "Genérico" -> labVal
        else -> ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(
                if (isSelected) textPrimary.copy(alpha = 0.05f) else Color.Transparent
            )
            .drawBehind {
                if (isSelected) {
                    val strokeWidth = s.borderWidth.toPx() * 1.6f
                    drawLine(
                        color = textPrimary,
                        start = Offset(s.xs.toPx() * 0.3f, s.xs.toPx()),
                        end = androidx.compose.ui.geometry.Offset(s.xs.toPx() * 0.3f, size.height - s.xs.toPx()),
                        strokeWidth = strokeWidth
                    )
                }
                val lineY = size.height - s.separatorH.toPx()
                drawLine(
                    color = borderColor,
                    start = androidx.compose.ui.geometry.Offset(s.padScreenH.toPx(), lineY),
                    end = androidx.compose.ui.geometry.Offset(size.width - s.padScreenH.toPx(), lineY),
                    strokeWidth = s.separatorH.toPx() * 0.8f
                )
            }
            .clickable(onClick = { onProductClick(product) })
            .padding(horizontal = s.padScreenH, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar grande — inicial con fondo accent
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentAvatar.copy(alpha = 0.12f))
                .border(1.dp, accentAvatar.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = inicial,
                color = accentAvatar,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(12.dp))

        // Col 1: Producto
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = product.name,
                    color = if (!product.activo) textSecondary else textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!product.activo) {
                    Surface(
                        color = SaaSBorder.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, SaaSBorder)
                    ) {
                        Text(
                            text = "PAUSADO",
                            color = textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (subtexto.isNotBlank()) {
                Text(
                    text = subtexto,
                    color = textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Col 2: Categoría
        Box(modifier = Modifier.width(cols.category), contentAlignment = Alignment.CenterStart) {
            Surface(
                color = SaaSBackground,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, SaaSBorder.copy(alpha = 0.8f))
            ) {
                Text(
                    text = firstCategory.uppercase(),
                    color = textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Col 3: Stock
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

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(stockColor))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stockTexto,
                        color = stockColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    if (enCuarentenaSola || product.stockBloqueado > 0) {
                        Text(
                            text = if (enCuarentenaSola) "cuarentena" else "${product.stockBloqueado} bloq.",
                            color = colorCuarentena,
                            fontSize = 11.5.sp,
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
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
