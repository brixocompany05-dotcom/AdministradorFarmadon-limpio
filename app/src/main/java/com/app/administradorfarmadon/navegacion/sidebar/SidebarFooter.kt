package com.app.administradorfarmadon.navegacion.sidebar

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa

@Composable
fun SidebarFooter(
    usuarioNombre: String,
    rolNombre: String,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    val infiniteTransition = rememberInfiniteTransition(label = "footer_skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val rot by animateFloatAsState(
        targetValue = if (isDarkMode) 0f else 180f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "rot"
    )

    val iniciales = if (usuarioNombre.isNotBlank()) {
        usuarioNombre.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
    } else ""

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .height(s.separatorH)
                .fillMaxWidth()
                .background(SidebarTheme.Border.copy(alpha = 0.45f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = s.padCard * 0.85f, vertical = s.padCard * 0.7f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.sm)
        ) {
            if (isLoading || iniciales.isBlank()) {
                Box(
                    modifier = Modifier
                        .size(s.iconLarge)
                        .clip(CircleShape)
                        .background(SidebarTheme.Border.copy(alpha = alpha))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(s.iconLarge)
                        .clip(CircleShape)
                        .background(SidebarTheme.ActiveBg)
                        .border(s.borderWidth, SidebarTheme.Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iniciales,
                        style = FDType.Label.copy(
                            color = SidebarTheme.ActiveText,
                            fontSize = s.textLabel.value.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                if (isLoading || usuarioNombre.isBlank()) {
                    Box(
                        modifier = Modifier
                            .width(s.btnMediumH * 2f)
                            .height(s.textBody.value.dp * 0.7f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SidebarTheme.Border.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(s.xs * 0.7f))
                    Box(
                        modifier = Modifier
                            .width(s.btnSmallH * 1.5f)
                            .height(s.textLabel.value.dp * 0.7f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SidebarTheme.Border.copy(alpha = alpha))
                    )
                } else {
                    Text(
                        text = usuarioNombre,
                        style = FDType.Body.copy(
                            color = SidebarTheme.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = s.textBody.value.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.6f)) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF34C759)))
                        Text(
                            text = rolNombre.ifBlank { "Conectado" },
                            style = FDType.BodySmall.copy(
                                color = SidebarTheme.TextSecondary,
                                fontSize = s.textLabel.value.sp * 0.88f
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(s.btnSmallH)
                    .clip(CircleShape)
                    .background(SidebarTheme.Border.copy(alpha = 0.18f))
                    .border(s.borderWidth * 0.8f, SidebarTheme.Border.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Cambiar tema",
                    tint = SidebarTheme.TextSecondary,
                    modifier = Modifier.size(s.iconSmall).rotate(rot)
                )
            }
        }
    }
}
