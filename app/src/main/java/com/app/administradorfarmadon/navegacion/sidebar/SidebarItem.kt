package com.app.administradorfarmadon.navegacion.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDType

@Composable
fun SidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badge: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) SidebarTheme.ActiveText else SidebarTheme.TextSecondary
    val iconColor = if (selected) SidebarTheme.Accent else SidebarTheme.InactiveIcon
    val bgColor = if (selected) SidebarTheme.ActiveBg else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = FDType.Body.copy(
                    color = contentColor,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(1f)
            )
            
            if (badge != null && badge > 0) {
                Surface(
                    color = SidebarTheme.Destructive,
                    shape = CircleShape,
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badge.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Indicador de acento vertical (Discreto pero Firme)
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.5f)
                    .width(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SidebarTheme.Accent)
                    .align(Alignment.CenterStart)
            )
        }
    }
}
