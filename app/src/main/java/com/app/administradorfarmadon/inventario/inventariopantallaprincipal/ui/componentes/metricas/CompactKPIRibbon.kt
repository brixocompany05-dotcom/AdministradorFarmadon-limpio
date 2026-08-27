package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.metricas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa

@Composable
fun CompactKPIRibbon(
    value: String,
    refs: String,
    alerts: String,
    s: MedidaAdaptativa,
    modifier: Modifier = Modifier
) {
    Surface(
        color = FDColors.Surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, FDColors.Border),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Valor
            RibbonItem(
                icon = Icons.Default.Payments,
                value = value,
                color = FDColors.Success,
                modifier = Modifier.weight(1.2f)
            )
            
            VerticalDivider(color = FDColors.Border, modifier = Modifier.height(20.dp))
            
            // Referencias
            RibbonItem(
                icon = Icons.Default.Inventory2,
                value = refs,
                color = FDColors.TextPrimary,
                modifier = Modifier.weight(0.8f)
            )
            
            VerticalDivider(color = FDColors.Border, modifier = Modifier.height(20.dp))
            
            // Alertas
            RibbonItem(
                icon = Icons.Default.WarningAmber,
                value = alerts,
                color = if (alerts != "0") FDColors.Error else FDColors.TextTertiary,
                modifier = Modifier.weight(0.8f)
            )
        }
    }
}

@Composable
private fun RibbonItem(
    icon: ImageVector,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.8f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = FDColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
