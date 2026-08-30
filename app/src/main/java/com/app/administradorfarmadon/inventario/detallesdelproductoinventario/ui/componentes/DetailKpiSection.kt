package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes.*

@Composable
fun DetailKpiSection(
    product: MoldeProductos,
    totalStock: Double
) {
    val nearestExpiry = product.lotes.values
        .mapNotNull { ProductDetailMapper.diasHastaVencer(it.vencimiento) }
        .minOrNull()

    val stockMinimo = product.stockMinimoBase
    // Comparación real en base (Tab vs Tab): 40 Tab ──‰¤ 50 Tab ──†’ 4 Cajas ──‰¤ 5 Cajas
    val isLowStock = totalStock <= stockMinimo && stockMinimo > 0.0
    val stockColor = if (isLowStock) SaaSError else SaaSSuccess
    
    val stockActualRaw = ProductDetailMapper.resumenStockVisual(product, totalStock)
    val stockActualValue = stockActualRaw.split(" (")[0]
    
    val stockMinimoValue = ProductDetailMapper.resumenStockMinimoVisual(product).split(" (")[0]

    val expiryColor = when {
        nearestExpiry == null -> Color.White
        nearestExpiry < 0 -> SaaSError
        nearestExpiry <= 30 -> SaaSWarning
        else -> Color.White
    }
    val expiryText = when {
        nearestExpiry == null -> "N/A"
        nearestExpiry < 0 -> "VENCIDO hace ${-nearestExpiry} días"
        nearestExpiry <= 30 -> "en $nearestExpiry días"
        else -> "en ${nearestExpiry / 30} meses"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .background(SaaSSurface, RoundedCornerShape(8.dp))
            .border(0.5.dp, SaaSBorder, RoundedCornerShape(8.dp))
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RibbonKpiItem(
            icon = Icons.Default.Inventory,
            label = "Stock Actual",
            value = stockActualValue,
            valueColor = stockColor,
            modifier = Modifier.weight(1f),
            showPing = true,
            progress = if (stockMinimo > 0) (totalStock / (stockMinimo * 2f).coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) else null
        )
        
        VerticalDivider(color = SaaSBorder, modifier = Modifier.padding(vertical = 12.dp).width(0.5.dp))

        RibbonKpiItem(
            icon = Icons.Default.WarningAmber,
            label = "Mínimo",
            value = stockMinimoValue,
            valueColor = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f)
        )

        VerticalDivider(color = SaaSBorder, modifier = Modifier.padding(vertical = 12.dp).width(0.5.dp))

        RibbonKpiItem(
            icon = Icons.Default.Event,
            label = "Vencimiento",
            value = expiryText,
            valueColor = expiryColor,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
private fun RibbonKpiItem(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    showPing: Boolean = false,
    progress: Float? = null
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showPing) {
                StatusPing(color = valueColor)
                Spacer(modifier = Modifier.width(2.dp))
            } else {
                Icon(icon, null, tint = SaaSTextSecondary, modifier = Modifier.size(9.dp))
            }
            Text(
                text = label.uppercase(),
                color = SaaSTextSecondary,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (value == "N/A" || value == "Sin fecha") "—”" else value,
                color = valueColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            
            // Stock Meter (UI 2030)
            if (progress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(1.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(valueColor, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}
