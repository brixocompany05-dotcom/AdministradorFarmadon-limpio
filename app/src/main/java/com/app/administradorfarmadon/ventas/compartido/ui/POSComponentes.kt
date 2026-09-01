package com.app.administradorfarmadon.ventas.compartido.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.TipoEstadoFarmadon

/**
 * COMPONENTES COMPARTIDOS POS
 * Estandarización visual para el módulo de Ventas (SaaS Enterprise).
 */

@Composable
fun POSHeader(
    titulo: String,
    cajaNombre: String = "",
    usuarioNombre: String = "",
    estaConectado: Boolean = true,
    cajaCerrada: Boolean = false
) {
    val s = recordarMedidaAdaptativa()
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = s.gapMedium)) {
        if (cajaCerrada) {
            POSNotificationBar(
                mensaje = "LA CAJA SE ENCUENTRA CERRADA. ALGUNAS FUNCIONES ESTÁN DESHABILITADAS.",
                tipo = TipoEstadoFarmadon.PELIGRO,
                icono = Icons.Default.Lock
            )
            Spacer(Modifier.height(s.gapSmall))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = titulo.uppercase(),
                    style = FDType.Heading1.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                    color = FDColors.TextPrimary
                )
                if (cajaNombre.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.PointOfSale, null, modifier = Modifier.size(14.dp), tint = FDColors.TextTertiary)
                        Text(text = cajaNombre, style = FDType.Caption, color = FDColors.TextTertiary)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.gapMedium)) {
                // Usuario
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = FDColors.PrimarySubtle,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp), tint = FDColors.Primary)
                        }
                    }
                    Column {
                        Text(text = usuarioNombre.ifEmpty { "Cajero Farmadon" }, style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                        Text(text = "Cajero(a)", style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.TextTertiary)
                    }
                }

                // Status Conexión
                Surface(
                    color = if (estaConectado) FDColors.SuccessSubtle else FDColors.ErrorSubtle,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, if (estaConectado) FDColors.Success.copy(alpha = 0.2f) else FDColors.Error.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (estaConectado) FDColors.Success else FDColors.Error))
                        Icon(if (estaConectado) Icons.Default.Wifi else Icons.Default.WifiOff, null, modifier = Modifier.size(12.dp), tint = if (estaConectado) FDColors.Success else FDColors.Error)
                        Text(text = if (estaConectado) "CONECTADO" else "SIN RED", style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = if (estaConectado) FDColors.Success else FDColors.Error)
                    }
                }
            }
        }
    }
}

@Composable
fun POSNotificationBar(
    mensaje: String,
    tipo: TipoEstadoFarmadon = TipoEstadoFarmadon.ALERTA,
    icono: ImageVector = Icons.Default.Info
) {
    val (fondo, texto) = when (tipo) {
        TipoEstadoFarmadon.EXITO -> FDColors.SuccessSubtle to FDColors.Success
        TipoEstadoFarmadon.ALERTA -> FDColors.WarningSubtle to FDColors.Warning
        TipoEstadoFarmadon.PELIGRO -> FDColors.ErrorSubtle to FDColors.Error
        TipoEstadoFarmadon.NEUTRO -> FDColors.InputBackground to FDColors.TextPrimary
    }

    Surface(
        color = fondo,
        shape = FDShapes.Small,
        border = BorderStroke(1.dp, texto.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icono, null, modifier = Modifier.size(18.dp), tint = texto)
            Text(
                text = mensaje,
                style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                color = texto,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun POSStatusOverlay(
    loading: Boolean = false,
    error: String? = null,
    empty: Boolean = false,
    emptyIcon: ImageVector = Icons.Default.Inbox,
    emptyTitle: String = "No hay datos",
    emptySub: String = "Todavía no se ha registrado información en esta sección.",
    onRetry: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (loading) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = FDColors.Surface.copy(alpha = 0.7f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 3.dp)
                }
            }
        }

        if (error != null) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = FDColors.Surface
            ) {
                POSEmptyState(
                    icono = Icons.Default.ErrorOutline,
                    titulo = "Hubo un problema",
                    subtitulo = error
                )
                if (onRetry != null) {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.BottomCenter) {
                        Button(onClick = onRetry) { Text("REINTENTAR") }
                    }
                }
            }
        }

        if (empty && !loading && error == null) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = FDColors.Surface
            ) {
                POSEmptyState(icono = emptyIcon, titulo = emptyTitle, subtitulo = emptySub)
            }
        }
    }
}

@Composable
fun POSEmptyState(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                color = FDColors.InputBackground.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icono, null, modifier = Modifier.size(48.dp), tint = FDColors.TextTertiary.copy(alpha = 0.5f))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = titulo, style = FDType.Heading3.copy(fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
                Text(text = subtitulo, style = FDType.Body, color = FDColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp))
            }
        }
    }
}

@Composable
fun POSTable(
    headers: List<String>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val s = recordarMedidaAdaptativa()
    Column(modifier = modifier.fillMaxWidth()) {
        // Headers
        Surface(
            color = FDColors.InputBackground.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                headers.forEach { header ->
                    Text(
                        text = header.uppercase(),
                        style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                        color = FDColors.TextTertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        HorizontalDivider(color = FDColors.Border)
        
        // Rows container
        Column(modifier = Modifier.weight(1f)) {
            content()
        }
        
        // Pagination visual
        HorizontalDivider(color = FDColors.Border)
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Mostrando 0 de 0 registros", style = FDType.Caption)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = {}, enabled = false) { Icon(Icons.Default.ChevronLeft, null) }
                IconButton(onClick = {}, enabled = false) { Icon(Icons.Default.ChevronRight, null) }
            }
        }
    }
}

@Composable
fun POSTableRow(
    cells: List<String>,
    onClick: () -> Unit = {},
    status: TipoEstadoFarmadon? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = if (isHovered) FDColors.SurfaceHover else FDColors.Surface,
        modifier = Modifier.fillMaxWidth().hoverable(interactionSource)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                cells.forEachIndexed { index, cell ->
                    if (index == cells.lastIndex && status != null) {
                        Box(modifier = Modifier.weight(1f)) {
                            POSBadge(texto = cell, tipo = status)
                        }
                    } else {
                        Text(
                            text = cell,
                            style = FDType.Body.copy(fontSize = 13.sp),
                            color = FDColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 20.dp))
        }
    }
}

@Composable
fun POSBadge(texto: String, tipo: TipoEstadoFarmadon) {
    val (fondo, color) = when (tipo) {
        TipoEstadoFarmadon.EXITO -> FDColors.SuccessSubtle to FDColors.Success
        TipoEstadoFarmadon.ALERTA -> FDColors.WarningSubtle to FDColors.Warning
        TipoEstadoFarmadon.PELIGRO -> FDColors.ErrorSubtle to FDColors.Error
        TipoEstadoFarmadon.NEUTRO -> FDColors.InputBackground to FDColors.TextTertiary
    }
    Surface(
        color = fondo,
        shape = FDShapes.XSmall,
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
            color = color
        )
    }
}

@Composable
fun POSShortcutHint(tecla: String, label: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            color = FDColors.InputBackground,
            shape = FDShapes.XSmall,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.height(20.dp).widthIn(min = 24.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(text = tecla, style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
            }
        }
        if (label.isNotEmpty()) {
            Text(text = label, style = FDType.Label.copy(fontSize = 9.sp), color = FDColors.TextTertiary)
        }
    }
}

@Composable
fun POSSideSheet(
    visible: Boolean,
    onClose: () -> Unit,
    titulo: String,
    subtitulo: String = "",
    acciones: @Composable (RowScope.() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FDColors.Scrim.copy(alpha = 0.4f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(500.dp)
                    .clickable(enabled = false, onClick = {}),
                color = FDColors.Surface,
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, FDColors.Border)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header SideSheet
                    Row(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = titulo, style = FDType.Heading2.copy(fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
                            if (subtitulo.isNotEmpty()) {
                                Text(text = subtitulo, style = FDType.Caption, color = FDColors.TextTertiary)
                            }
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))
                    
                    Column(modifier = Modifier.weight(1f).padding(24.dp).verticalScroll(rememberScrollState())) {
                        contenido()
                    }

                    if (acciones != null) {
                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            acciones()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun POSSummaryCard(
    titulo: String,
    valor: String,
    simbolo: String = "",
    tipo: TipoEstadoFarmadon = TipoEstadoFarmadon.NEUTRO,
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    val colorAcento = when (tipo) {
        TipoEstadoFarmadon.EXITO -> FDColors.Success
        TipoEstadoFarmadon.ALERTA -> FDColors.Warning
        TipoEstadoFarmadon.PELIGRO -> FDColors.Error
        TipoEstadoFarmadon.NEUTRO -> FDColors.Primary
    }

    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Medium,
        border = BorderStroke(1.dp, FDColors.Border),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(s.padCard), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = titulo.uppercase(), style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (simbolo.isNotEmpty()) {
                    Text(text = simbolo, style = FDType.Numeric.copy(fontSize = 14.sp, color = colorAcento))
                }
                Text(text = valor, style = FDType.NumericLg.copy(color = colorAcento, fontWeight = FontWeight.Black))
            }
        }
    }
}

@Composable
fun POSDenominationCounter(
    denominacion: String,
    cantidad: Int,
    onCantidadChange: (Int) -> Unit,
    simbolo: String = "S/",
    esMoneda: Boolean = false
) {
    val s = recordarMedidaAdaptativa()
    val totalRow = try { denominacion.toDouble() * cantidad } catch (e: Exception) { 0.0 }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Label
        Surface(
            color = if (esMoneda) FDColors.InputBackground else FDColors.Primary.copy(alpha = 0.05f),
            shape = FDShapes.Small,
            border = if (!esMoneda) BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.1f)) else null,
            modifier = Modifier.size(width = 85.dp, height = 40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = denominacion, style = FDType.Numeric.copy(fontSize = 16.sp, fontWeight = FontWeight.Black), color = if (esMoneda) FDColors.TextPrimary else FDColors.Primary)
            }
        }
        
        Text(text = "x", style = FDType.Body, color = FDColors.TextTertiary)
        
        // Counter
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                onClick = { if (cantidad > 0) onCantidadChange(cantidad - 1) },
                color = FDColors.InputBackground,
                shape = CircleShape,
                modifier = Modifier.size(36.dp),
                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp)) }
            }
            
            Text(
                text = "$cantidad",
                style = FDType.Numeric.copy(fontSize = 20.sp, fontWeight = FontWeight.Black),
                color = FDColors.TextPrimary,
                modifier = Modifier.widthIn(min = 40.dp),
                textAlign = TextAlign.Center
            )
            
            Surface(
                onClick = { onCantidadChange(cantidad + 1) },
                color = FDColors.Primary.copy(alpha = 0.08f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp),
                border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = FDColors.Primary) }
            }
        }

        // Subtotal visual
        Text(
            text = "$simbolo " + String.format("%.2f", totalRow),
            style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = if (cantidad > 0) FDColors.TextPrimary else FDColors.TextDisabled,
            modifier = Modifier.width(90.dp),
            textAlign = TextAlign.End
        )
    }
}
