package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.buscador

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors

@Composable
fun CompactActionButton(
    icon: ImageVector,
    label: String,
    count: Int = 0,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        color = FDColors.SurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            0.5.dp,
            if (count > 0) FDColors.BorderFocus else FDColors.Border
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (count > 0) FDColors.TextPrimary else FDColors.TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = if (count > 0) FDColors.TextPrimary else FDColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(FDColors.Primary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        count.toString(),
                        color = if (FDColors.isDark) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
