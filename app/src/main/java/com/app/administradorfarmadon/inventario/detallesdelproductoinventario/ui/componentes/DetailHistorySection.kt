package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes.*
import com.app.administradorfarmadon.base_datos.MonedaHelper

/*
@Composable
fun DetailHistorySection(
    movements: List<MovimientoInventario>,
    product: MoldeProductos
) {
    if (movements.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SIN ACTIVIDAD RECIENTE",
                color = SaaSTextSecondary.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            movements.take(8).forEach { mov ->
                HistoryRow(mov, product)
            }
        }
    }
}
*/

@Composable
internal fun HistoryRow(mov: MovimientoInventario, p: MoldeProductos) {
    val isAdd = mov.cantidad > 0
    val color = if (isAdd) SaaSSuccess else SaaSTextSecondary
    
    val delta = if (mov.cantidad == 0.0) {
        "—"
    } else {
        val qtyVisualCompleto = ProductDetailMapper.resumenStockVisual(p, kotlin.math.abs(mov.cantidad))
        val qtyFormat = qtyVisualCompleto.split(" (")[0]
        "${if (isAdd) "+" else "-"}$qtyFormat"
    }
    
    val relativeDate = ProductDetailMapper.formatRelativeDate(mov.fecha)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ProductDetailMapper.formatearTipoMovimiento(mov.tipo).uppercase(),
                color = SaaSTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "$relativeDate · ${mov.usuarioNombre}",
                color = SaaSTextSecondary,
                fontSize = 10.sp
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = delta,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            if (mov.costoTotal > 0.0) {
                Text(
                    text = MonedaHelper.formatearSimple(mov.costoTotal),
                    color = SaaSTextSecondary.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
