package com.app.administradorfarmadon.navegacion.sidebar

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    planPermiteMultiSede: Boolean = true,
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
        // Nombre de la Farmacia —” Look Apple Premium
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

        // Selector de Sede — Look Apple Selector Premium
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SidebarTheme.Border.copy(alpha = alpha))
            )
        } else if (esItinerante && planPermiteMultiSede && sucursales.filter { it.activa }.size > 1) {
            var menuSedesExpandido by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { menuSedesExpandido = true },
                    shape = RoundedCornerShape(10.dp),
                    color = SidebarTheme.ActiveBg.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SidebarTheme.Border.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SidebarTheme.Accent.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = SidebarTheme.Accent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "SEDE ACTIVA",
                                    style = FDType.Label.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = SidebarTheme.TextSecondary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = sucursalNombre,
                                    color = SidebarTheme.TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = "Cambiar sede",
                            tint = SidebarTheme.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = menuSedesExpandido,
                    onDismissRequest = { menuSedesExpandido = false },
                    modifier = Modifier
                        .background(SidebarTheme.Background)
                        .border(1.dp, SidebarTheme.Border, RoundedCornerShape(12.dp))
                        .padding(vertical = 4.dp)
                ) {
                    sucursales.filter { it.activa }.forEach { sede ->
                        val seleccionada = sede.nombre == sucursalNombre
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (seleccionada) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = SidebarTheme.Accent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    } else {
                                        Spacer(Modifier.size(15.dp))
                                    }
                                    Text(
                                        text = sede.nombre,
                                        style = FDType.BodySmall.copy(
                                            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.5.sp
                                        ),
                                        color = if (seleccionada) SidebarTheme.Accent else SidebarTheme.TextPrimary
                                    )
                                }
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
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SidebarTheme.ActiveBg.copy(alpha = 0.04f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, SidebarTheme.Border.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SidebarTheme.Accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = SidebarTheme.Accent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "SEDE",
                            style = FDType.Label.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                            color = SidebarTheme.TextSecondary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = sucursalNombre,
                            color = SidebarTheme.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
