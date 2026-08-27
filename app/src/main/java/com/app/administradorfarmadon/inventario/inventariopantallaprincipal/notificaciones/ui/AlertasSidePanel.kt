package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.notificaciones.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.notificaciones.logica.InventarioAlertasLogic

@Composable
fun AlertasSidePanel(
    alertas: List<InventarioAlertasLogic.AlertaProducto>,
    unreadAlertIds: Set<String>,
    readInfoMap: Map<String, String>,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onProductClick: (InventarioAlertasLogic.AlertaProducto) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onMarkAsRead: (InventarioAlertasLogic.AlertaProducto) -> Unit
) {
    val unreadAlerts = remember(alertas, unreadAlertIds) {
        alertas.filter { al ->
            com.app.administradorfarmadon.inventario.inventariopantallaprincipal.notificaciones.base_datos.AlertPersistenceManager
                .readKey(al.productId, al.tipo.name) in unreadAlertIds
        }
    }

    // Notificaciones puras: sin filtros (ya filtras en inventario). Muestra todas ordenadas por severidad.

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(FDColors.Background)
            .border(
                width = 0.5.dp,
                color = FDColors.Border,
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
            )
            .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
    ) {
        HeaderSeccion(
            unreadCount = unreadAlerts.size,
            onClose = onClose,
            onMarkAllRead = onMarkAllAsRead
        )

        if (alertas.isEmpty()) {
            EmptyAlertsState()
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                alertas.forEach { alerta ->
                    val isUnread = com.app.administradorfarmadon.inventario.inventariopantallaprincipal.notificaciones.base_datos.AlertPersistenceManager
                        .readKey(alerta.productId, alerta.tipo.name) in unreadAlertIds
                    AlertaCardPremium(
                        alerta = alerta,
                        isUnread = isUnread,
                        readTime = readInfoMap[com.app.administradorfarmadon.inventario.inventariopantallaprincipal.notificaciones.base_datos.AlertPersistenceManager
                            .readKey(alerta.productId, alerta.tipo.name)],
                        onClick = {
                            onMarkAsRead(alerta)
                            onProductClick(alerta)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun HeaderSeccion(
    unreadCount: Int,
    onClose: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "CENTRO DE ACCIÓN",
                    style = FDType.Label.copy(
                        color = FDColors.TextSecondary,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Notificaciones",
                        style = FDType.Heading1.copy(fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                    )
                    if (unreadCount > 0) {
                        Surface(
                            color = FDColors.Error,
                            shape = CircleShape,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(38.dp)
                    .background(FDColors.SurfaceElevated, CircleShape)
                    .border(0.5.dp, FDColors.Border, CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = FDColors.TextPrimary, modifier = Modifier.size(18.dp))
            }
        }

        if (unreadCount > 0) {
            Surface(
                onClick = onMarkAllRead,
                color = FDColors.Primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, FDColors.Border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.DoneAll, null, tint = FDColors.Success, modifier = Modifier.size(16.dp))
                    Text(
                        "Marcar todo como leído",
                        style = FDType.BodySmall.copy(color = FDColors.TextPrimary, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertaCardPremium(
    alerta: InventarioAlertasLogic.AlertaProducto,
    isUnread: Boolean,
    readTime: String?,
    onClick: () -> Unit
) {
    val severityColor = when (alerta.severidad) {
        InventarioAlertasLogic.Severidad.CRITICA -> FDColors.Error
        InventarioAlertasLogic.Severidad.ALTA -> FDColors.Warning
        else -> FDColors.Success
    }

    val cardAlpha by animateFloatAsState(if (isUnread) 1f else 0.65f, label = "cardAlpha")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable { onClick() },
        color = FDColors.SurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, if (isUnread) severityColor.copy(alpha = 0.5f) else FDColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    color = severityColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = getIconoParaTipo(alerta.tipo),
                        contentDescription = null,
                        tint = severityColor,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                if (isUnread) {
                    Surface(
                        color = severityColor,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(9.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-3).dp)
                            .border(1.5.dp, FDColors.SurfaceElevated, CircleShape)
                    ) {}
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                // Adaptativo matemático: nombre largo → texto más pequeño, corto → más grande, nunca se aplasta ni salta línea
                val nameLen = alerta.productName.length
                val titleSize = when {
                    nameLen > 28 -> 11.sp
                    nameLen > 22 -> 12.sp
                    nameLen > 16 -> 12.5.sp
                    else -> 13.5.sp
                }
                Text(
                    text = alerta.productName.uppercase(),
                    style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = titleSize, color = FDColors.TextPrimary, letterSpacing = (-0.2).sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Mensaje también adaptativo si es largo
                val msgSize = if (alerta.mensaje.length > 32) 11.sp else 12.sp
                Text(
                    text = alerta.mensaje,
                    style = FDType.BodySmall.copy(color = FDColors.TextSecondary, fontSize = msgSize, lineHeight = (msgSize.value * 1.25f).sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!readTime.isNullOrBlank()) {
                    Text(
                        text = "Visto $readTime",
                        style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextTertiary)
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentedPicker(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(FDColors.SurfaceElevated, RoundedCornerShape(8.dp))
            .border(0.5.dp, FDColors.Border, RoundedCornerShape(8.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Surface(
                onClick = { onSelectionChange(index) },
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) FDColors.Primary else Color.Transparent,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = title,
                        style = FDType.Label.copy(
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) FDColors.PrimaryText else FDColors.TextSecondary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAlertsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = FDColors.Success.copy(alpha = 0.25f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            "TODO EN ORDEN",
            style = FDType.Label.copy(color = FDColors.TextTertiary, letterSpacing = 2.sp)
        )
        Text(
            "No hay alertas pendientes de revisión.",
            style = FDType.BodySmall.copy(textAlign = TextAlign.Center, color = FDColors.TextSecondary),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

private fun getIconoParaTipo(tipo: InventarioAlertasLogic.TipoAlerta): ImageVector {
    return when (tipo) {
        InventarioAlertasLogic.TipoAlerta.STOCK_AGOTADO -> Icons.Default.RemoveShoppingCart
        InventarioAlertasLogic.TipoAlerta.STOCK_BAJO -> Icons.Default.Warning
        InventarioAlertasLogic.TipoAlerta.VENCIMIENTO_CRITICO -> Icons.Default.Timer
        InventarioAlertasLogic.TipoAlerta.VENCIMIENTO_PROXIMO -> Icons.Default.Schedule
        InventarioAlertasLogic.TipoAlerta.VENCIDO -> Icons.Default.Block
        InventarioAlertasLogic.TipoAlerta.MARGEN_BAJO -> Icons.AutoMirrored.Filled.TrendingDown
    }
}
