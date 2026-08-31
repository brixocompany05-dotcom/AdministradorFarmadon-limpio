package com.app.administradorfarmadon.navegacion.sidebar

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa

@Composable
fun FarmadonSidebar(
    currentRoute: String?,
    items: List<SidebarItemData>,
    isLoading: Boolean,
    nombreFarmacia: String,
    sucursalNombre: String,
    planNombre: String,
    usuarioNombre: String,
    rolNombre: String,
    isDarkMode: Boolean,
    esItinerante: Boolean = false,
    sucursales: List<com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal> = emptyList(),
    onCambiarSucursal: (String, String) -> Unit = { _, _ -> },
    onToggleTheme: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    // Ancho geométrico: 280dp * scaleFactor (0.85-1.18) clamp 268-320 → adaptativo a cualquier tablet
    val sidebarWidth = (280f * s.scaleFactor).coerceIn(268f, 318f).dp

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(sidebarWidth)
            .background(SidebarTheme.Background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
    ) {
        // Borde derecho degradado sutil —” profundidad premium
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(s.borderWidth)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SidebarTheme.Border.copy(alpha = 0f),
                            SidebarTheme.Border.copy(alpha = 0.45f),
                            SidebarTheme.Border.copy(alpha = 0f)
                        )
                    )
                )
        )
        // Grain sutil superior —” aura 4% (solo decoración, no interfiere)
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height * 0.22f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SidebarTheme.ActiveBg.copy(alpha = 0.06f),
                        androidx.compose.ui.graphics.Color.Transparent
                    )
                ),
                size = androidx.compose.ui.geometry.Size(w, h)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            SidebarHeader(
                nombreFarmacia = nombreFarmacia,
                sucursalNombre = sucursalNombre,
                planNombre = planNombre,
                isLoading = isLoading,
                esItinerante = esItinerante,
                sucursales = sucursales,
                onCambiarSucursal = onCambiarSucursal
            )

            HorizontalDivider(color = SidebarTheme.Border.copy(alpha = 0.32f), thickness = s.separatorH)

            if (isLoading) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = s.sm, horizontal = s.sm),
                    verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                ) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .width(s.btnLargeH * 1.4f)
                                .height(s.textLabel.value.dp * 0.7f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SidebarTheme.Border.copy(alpha = 0.25f))
                        )
                        repeat(3) { SidebarItemSkeleton() }
                        Spacer(modifier = Modifier.height(s.xs))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = s.xs),
                    verticalArrangement = Arrangement.spacedBy(s.xs * 0.3f)
                ) {
                    val categorias = items.groupBy { it.categoria }
                        .toList()
                        .sortedBy { (_, items) -> items.minOfOrNull { it.orden } ?: 0 }

                    // Grupos OPERACIÓN / GESTIÓN etc —” quiet
                    val (sistemaGrupos, restoGrupos) = categorias.partition { it.first.equals("SISTEMA", ignoreCase = true) }
                    restoGrupos.forEach { (categoria, itemsModulo) ->
                        SidebarGrupo(
                            label = categoria.uppercase(),
                            isActive = itemsModulo.any { currentRoute == it.modulo }
                        ) {
                            itemsModulo.forEach { item ->
                                SidebarItem(
                                    label = item.nombre,
                                    icon = getIconForName(item.icono),
                                    selected = currentRoute == item.modulo,
                                    badge = item.badge,
                                    onClick = { onNavigate(item.modulo) }
                                )
                            }
                        }
                    }
                    // SISTEMA —” Configuración como tarjeta premium pinned, imposible confundir
                    sistemaGrupos.forEach { (_, itemsModulo) ->
                        // Separador premium antes de SISTEMA
                        Spacer(Modifier.height(s.gapMedium))
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxWidth().height(s.separatorH).background(SidebarTheme.Border.copy(alpha = 0.18f)).padding(horizontal = s.padCard)
                        )
                        androidx.compose.material3.Surface(
                            color = SidebarTheme.ActiveBg.copy(alpha = 0.06f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(s.radiusCard * 0.7f),
                            border = androidx.compose.foundation.BorderStroke(s.borderWidth * 0.7f, SidebarTheme.Border.copy(alpha = 0.22f)),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = s.xs, vertical = s.xs)
                        ) {
                            Column(Modifier.padding(vertical = s.xs * 0.6f)) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = s.sm, vertical = s.xs * 0.7f),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.6f)
                                ) {
                                    androidx.compose.foundation.layout.Box(
                                        Modifier.size(14.dp).clip(androidx.compose.foundation.shape.CircleShape).background(SidebarTheme.Accent.copy(alpha = 0.13f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.layout.Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(SidebarTheme.Accent))
                                    }
                                    androidx.compose.material3.Text(
                                        "SISTEMA", style = com.app.administradorfarmadon.disenotemaapp.ui.FDType.Label.copy(
                                            color = SidebarTheme.TextSecondary.copy(alpha = 0.62f),
                                            fontFamily = com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium,
                                            fontSize = s.textLabel.value.sp * 0.74f,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                            letterSpacing = 1.1.sp
                                        )
                                    )
                                }
                                itemsModulo.forEach { item ->
                                    SidebarItem(
                                        label = item.nombre,
                                        icon = getIconForName(item.icono),
                                        selected = currentRoute == item.modulo,
                                        badge = item.badge,
                                        onClick = { onNavigate(item.modulo) }
                                    )
                                }
                            }
                        }
                    }

                    if (items.isEmpty()) {
                        Box(modifier = Modifier.padding(s.padCard)) {
                            Text(
                                text = "No hay herramientas disponibles para este plan.",
                                color = SidebarTheme.TextSecondary,
                                fontSize = s.textBody.value.sp * 0.92f
                            )
                        }
                    }
                    Spacer(Modifier.height(s.gapMedium))
                }
            }

            SidebarFooter(
                usuarioNombre = usuarioNombre,
                rolNombre = rolNombre,
                isDarkMode = isDarkMode,
                isLoading = isLoading,
                onToggleTheme = onToggleTheme
            )
        }
    }
}

@Composable
private fun SidebarItemSkeleton() {
    val s = recordarMedidaAdaptativa()
    val infiniteTransition = rememberInfiniteTransition(label = "sidebar_skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(s.btnMediumH * 0.92f)
            .padding(horizontal = s.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(s.xs)
    ) {
        Box(
            modifier = Modifier
                .size(s.iconMedium * 1.05f)
                .clip(RoundedCornerShape(s.radiusChip * 0.7f))
                .background(SidebarTheme.Border.copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(s.textBody.value.dp * 0.65f)
                .clip(RoundedCornerShape(4.dp))
                .background(SidebarTheme.Border.copy(alpha = alpha))
        )
    }
}

@Composable
private fun getIconForName(name: String): ImageVector {
    return when (name.lowercase()) {
        "pointofsale", "shoppingcart", "point_of_sale", "caja", "ventas" -> Icons.Default.PointOfSale
        "inventory2", "storage", "inventario", "medicamentos" -> Icons.Default.Inventory2
        "localshipping", "proveedores", "compras" -> Icons.Default.LocalShipping
        "receiptlong", "assignment", "facturacion", "sunat" -> Icons.Default.ReceiptLong
        "groups", "people", "contactpage", "clientes" -> Icons.Default.Groups
        "assessment", "analytics", "barchart", "reportes" -> Icons.Default.Assessment
        "supportagent", "soporte" -> Icons.Default.SupportAgent
        "manageaccounts", "settings", "configuracion", "config_farmacia" -> Icons.Default.Settings
        "notificationsactive", "alertas" -> Icons.Default.NotificationsActive
        "api" -> Icons.Default.Api
        "autoawesomemotion" -> Icons.Default.AutoAwesomeMotion
        "insights" -> Icons.Default.Insights
        "storefront", "sucursales" -> Icons.Default.Storefront
        "backup" -> Icons.Default.Backup
        "monitorheart" -> Icons.Default.MonitorHeart
        "smarttoy" -> Icons.Default.SmartToy
        "psychology" -> Icons.Default.Psychology
        "accounttree" -> Icons.Default.AccountTree
        "qrcodescanner" -> Icons.Default.QrCodeScanner
        "description", "recetas" -> Icons.Default.Description
        "videocam" -> Icons.Default.Videocam
        "payment", "payments", "creditcard" -> Icons.Default.Payment
        "emojievents" -> Icons.Default.EmojiEvents
        "star" -> Icons.Default.Star
        "badge", "personal" -> Icons.Default.Badge
        "accountbalance", "gavel" -> Icons.Default.AccountBalance
        "devices" -> Icons.Default.Devices
        "store", "factory", "business" -> Icons.Default.Store
        "handshake" -> Icons.Default.Handshake
        "public" -> Icons.Default.Public
        "verifieduser" -> Icons.Default.VerifiedUser
        "cloudoff" -> Icons.Default.CloudOff
        "rocketlaunch" -> Icons.Default.RocketLaunch
        "healthandsafety" -> Icons.Default.HealthAndSafety
        "palette" -> Icons.Default.Palette
        "headsetmic" -> Icons.Default.HeadsetMic
        "medication" -> Icons.Default.Medication
        "report", "warning" -> Icons.Default.Report
        "science" -> Icons.Default.Science
        "lock" -> Icons.Default.Lock
        "alarm" -> Icons.Default.Alarm
        "localoffer" -> Icons.Default.LocalOffer
        "documentscanner" -> Icons.Default.DocumentScanner
        "calculate" -> Icons.Default.Calculate
        "listalt", "history" -> Icons.Default.ListAlt
        "eventbusy" -> Icons.Default.EventBusy
        "movetoinbox", "transfer_within_a_station" -> Icons.Default.MoveToInbox
        "escalatorwarning" -> Icons.Default.EscalatorWarning
        else -> Icons.Default.Circle
    }
}
