package com.app.administradorfarmadon.ventas.compartido.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.TipoEstadoFarmadon
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import java.util.Locale

/**
 * COMPONENTES COMPARTIDOS POS
 * Estandarización visual para el módulo de Ventas (SaaS Enterprise).
 */

@Composable
fun POSHeader(
    titulo: String = "",
    cajaCerrada: Boolean = false
) {
    val s = recordarMedidaAdaptativa()
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { // Reducido de gapMedium
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
            if (titulo.isNotEmpty()) {
                Text(
                    text = titulo.uppercase(),
                    style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 22.sp),
                    color = FDColors.TextPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                POSHeaderSelector(icono = Icons.Default.AccountBalance, label = "Caja 01")
                POSHeaderSelector(icono = Icons.Default.Update, label = "Turno: Mañana")
                POSHeaderSelector(icono = Icons.Default.Person, label = "Cajero: Sin seleccionar")
                
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Apertura", style = FDType.Caption.copy(fontSize = 9.sp), color = FDColors.TextTertiary)
                    Text("--/-- --:--", style = FDType.Label.copy(fontSize = 10.sp), color = FDColors.TextSecondary)
                }

                Surface(
                    color = FDColors.Warning.copy(alpha = 0.15f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(FDColors.Warning, CircleShape))
                        Text("En Proceso", style = FDType.Label.copy(fontSize = 10.sp, color = FDColors.Warning))
                    }
                }
            }
        }
    }
}

@Composable
private fun POSHeaderSelector(icono: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable { }
    ) {
        Icon(icono, null, modifier = Modifier.size(16.dp), tint = FDColors.TextSecondary)
        Text(label, style = FDType.Label.copy(fontSize = 11.sp), color = FDColors.TextSecondary)
        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = FDColors.TextTertiary)
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
                color = FDColors.Surface.copy(alpha = 0.6f) // Más traslúcido
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 2.dp) // Más fino
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
    Column(modifier = modifier.fillMaxWidth()) {
        // Headers
        Surface(
            color = FDColors.InputBackground.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                headers.forEach { header ->
                    Text(
                        text = header.uppercase(),
                        style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                        color = FDColors.TextTertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))
        
        // Rows container
        Column(modifier = Modifier.weight(1f)) {
            content()
        }
        
        // Pagination visual
        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "0 registros", style = FDType.Caption.copy(fontSize = 11.sp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {}, enabled = false, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(18.dp)) }
                IconButton(onClick = {}, enabled = false, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp)) }
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
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
            modifier = Modifier.height(24.dp).widthIn(min = 28.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                Text(text = tecla, style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = FDColors.TextTertiary)
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
    modifier: Modifier = Modifier,
    simbolo: String = "",
    tipo: TipoEstadoFarmadon = TipoEstadoFarmadon.NEUTRO
) {
    POSMetricCard(
        titulo = titulo,
        valor = valor,
        simbolo = simbolo,
        tipo = tipo,
        modifier = modifier
    )
}

@Composable
fun POSMetricCard(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier,
    simbolo: String = "",
    subtitulo: String = "",
    icono: ImageVector? = null,
    tipo: TipoEstadoFarmadon = TipoEstadoFarmadon.NEUTRO
) {
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
        Box(modifier = Modifier.padding(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = titulo.uppercase(),
                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                    color = FDColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (simbolo.isNotEmpty()) {
                            Text(text = simbolo, style = FDType.Numeric.copy(fontSize = 12.sp, color = colorAcento))
                        }
                        Text(
                            text = valor,
                            style = FDType.NumericLg.copy(color = colorAcento, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        )
                    }
                    
                    if (subtitulo.isNotEmpty()) {
                        Text(
                            text = subtitulo,
                            style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextSecondary
                        )
                    }
                }
            }

            if (icono != null) {
                Surface(
                    color = colorAcento.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp).align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icono, null, modifier = Modifier.size(16.dp), tint = colorAcento)
                    }
                }
            }
        }
    }
}

@Composable
fun POSSurfacePanel(
    titulo: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val s = recordarMedidaAdaptativa()
    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Medium,
        border = BorderStroke(s.borderWidth, FDColors.Border),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = titulo,
                style = FDType.Heading3.copy(fontWeight = FontWeight.Black, fontSize = 16.sp),
                color = FDColors.TextPrimary
            )
            Spacer(Modifier.height(16.dp))
            content()
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
            text = "$simbolo " + String.format(Locale.US, "%.2f", totalRow),
            style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = if (cantidad > 0) FDColors.TextPrimary else FDColors.TextDisabled,
            modifier = Modifier.width(90.dp),
            textAlign = TextAlign.End
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FDSearchField(
    busqueda: String,
    onBusquedaChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar...",
    leadingIcon: ImageVector = Icons.Default.Search,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
    val s = recordarMedidaAdaptativa()
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        color = FDColors.InputBackground,
        shape = FDShapes.Small,
        modifier = modifier.height(s.inputMinH)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(leadingIcon, null, tint = FDColors.Primary, modifier = Modifier.size(s.iconSmall))
            
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (busqueda.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = FDType.Body.copy(color = FDColors.TextTertiary, fontSize = s.textBody.value.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = busqueda,
                    onValueChange = onBusquedaChange,
                    interactionSource = interactionSource,
                    textStyle = FDType.Body.copy(color = FDColors.TextPrimary, fontSize = s.textBody.value.sp),
                    cursorBrush = SolidColor(FDColors.Primary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (busqueda.isNotEmpty()) {
                IconButton(onClick = { onBusquedaChange("") }, modifier = Modifier.size(s.iconLarge)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(s.iconSmall), tint = FDColors.TextTertiary)
                }
            }
            
            if (trailingContent != null) {
                VerticalDivider(modifier = Modifier.height(s.gapLarge).padding(horizontal = s.gapTiny))
                trailingContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FDTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val s = recordarMedidaAdaptativa()
    val interactionSource = remember { MutableInteractionSource() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Text(text = label.uppercase(), style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp), color = FDColors.TextTertiary)
        }
        
        Surface(
            color = FDColors.InputBackground,
            shape = FDShapes.Small,
            modifier = Modifier.fillMaxWidth().then(if (singleLine) Modifier.height(s.inputMinH) else Modifier.heightIn(min = s.inputMinH))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = if (singleLine) 0.dp else 12.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                leadingIcon?.let { Icon(it, null, tint = FDColors.Primary, modifier = Modifier.size(s.iconSmall).then(if (!singleLine) Modifier.padding(top = 2.dp) else Modifier)) }
                prefix?.invoke()

                Box(modifier = Modifier.weight(1f), contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = FDType.Body.copy(color = FDColors.TextTertiary, fontSize = s.textBody.value.sp)
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        interactionSource = interactionSource,
                        textStyle = FDType.Body.copy(color = FDColors.TextPrimary, fontSize = s.textBody.value.sp),
                        cursorBrush = SolidColor(FDColors.Primary),
                        singleLine = singleLine,
                        minLines = minLines,
                        keyboardOptions = keyboardOptions,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                suffix?.invoke()
            }
        }
    }
}
