package com.app.administradorfarmadon.navegacion.sidebar

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun SidebarHeader(
    nombreFarmacia: String,
    sucursalNombre: String,
    planNombre: String,
    isLoading: Boolean = false,
    esItinerante: Boolean = false,
    sucursales: List<Sucursal> = emptyList(),
    onCambiarSucursal: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "header_skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Nombre de la Farmacia — Look Apple Premium
        if (isLoading || nombreFarmacia.isBlank()) {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SidebarTheme.Border.copy(alpha = alpha))
            )
        } else {
            Text(
                text = nombreFarmacia.uppercase(),
                style = FDType.Heading2.copy(
                    color = SidebarTheme.TextPrimary,
                    fontFamily = InterPremium,
                    fontWeight = FontWeight.Black, // Más peso para identidad Apple Premium
                    fontSize = 17.5.sp,
                    letterSpacing = 2.sp,
                    lineHeight = 24.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Sede y Estado
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SidebarTheme.Border.copy(alpha = alpha))
                )
            } else if (esItinerante && sucursales.size > 1) {
                var menuSedesExpandido by remember { mutableStateOf(false) }
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { menuSedesExpandido = true }
                            .padding(vertical = 1.dp)
                    ) {
                        Text(
                            text = sucursalNombre,
                            color = SidebarTheme.Accent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Cambiar sede activa",
                            tint = SidebarTheme.Accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuSedesExpandido,
                        onDismissRequest = { menuSedesExpandido = false },
                        modifier = Modifier
                            .background(SidebarTheme.Background)
                            .border(0.8.dp, SidebarTheme.Border, RoundedCornerShape(8.dp))
                    ) {
                        sucursales.filter { it.activa }.forEach { sede ->
                            val seleccionada = sede.nombre == sucursalNombre
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = sede.nombre,
                                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        ),
                                        color = if (seleccionada) SidebarTheme.Accent else SidebarTheme.TextPrimary
                                    )
                                },
                                onClick = {
                                    onCambiarSucursal(sede.id, sede.nombre)
                                    menuSedesExpandido = false
                                }
                            )
                        }
                    }
                }
            } else if (sucursalNombre.isNotBlank()) {
                Text(
                    text = sucursalNombre,
                    color = SidebarTheme.TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Badge de Plan y estado Online
        if (isLoading || planNombre.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SidebarTheme.Border.copy(alpha = alpha))
                    )
                } else {
                    Surface(
                        color = SidebarTheme.Border.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SidebarTheme.Border)
                    ) {
                        Text(
                            text = "PLAN ${planNombre.uppercase()}",
                            color = SidebarTheme.TextPrimary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (!isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759))
                        )
                        Text(
                            text = "En línea",
                            color = SidebarTheme.TextSecondary.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
