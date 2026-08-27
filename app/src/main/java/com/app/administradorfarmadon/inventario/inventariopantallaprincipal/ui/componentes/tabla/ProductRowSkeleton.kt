package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.tabla

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes.ShimmerBlock

@Composable
fun ProductRowSkeleton(cols: InventoryColumns) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .drawBehind {
                val lineY = size.height - 0.5.dp.toPx()
                drawLine(
                    color = FDColors.Border,
                    start = androidx.compose.ui.geometry.Offset(24.dp.toPx(), lineY),
                    end = androidx.compose.ui.geometry.Offset(size.width - 24.dp.toPx(), lineY),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Col 1: Producto
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBlock(modifier = Modifier.width(160.dp).height(12.dp))
            ShimmerBlock(modifier = Modifier.width(100.dp).height(8.dp))
        }

        // Col 2: Categoría
        Box(modifier = Modifier.width(cols.category), contentAlignment = Alignment.CenterStart) {
            ShimmerBlock(modifier = Modifier.width(80.dp).height(10.dp))
        }

        // Col 3: Stock
        Box(modifier = Modifier.width(cols.stock), contentAlignment = Alignment.CenterEnd) {
            ShimmerBlock(modifier = Modifier.width(40.dp).height(12.dp))
        }

        // Col 4: Mínimo
        Box(modifier = Modifier.width(cols.min), contentAlignment = Alignment.CenterEnd) {
            ShimmerBlock(modifier = Modifier.width(30.dp).height(10.dp))
        }
    }
}
