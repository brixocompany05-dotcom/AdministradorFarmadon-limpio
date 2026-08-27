package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick

private enum class VistaLote { FICHA, CUARENTENA, DEVOLUCION, MERMA, ANULACION }

/**
 * Ficha Oficial de Lote — Enterprise Side-Drawer Premium INLINE (v2026.08).
 * Sin diálogos que saltan: todo ocurre dentro del mismo panel 440dp.
 * Sistema 2 pasos adelante: todo viene pre-llenado, el usuario solo confirma.
 */
@Composable
fun LoteDetailDrawer(
    product: MoldeProductos,
    lote: LoteProducto,
    esEsteElLotePrioritario: Boolean = false,
    onDefinirPrioridad: (marcar: Boolean) -> Unit = {},
    isPrivileged: Boolean = false,
    onDismiss: () -> Unit,
    onIngresarMasStock: (LoteProducto) -> Unit,
    onRegistrarDevolucion: (lote: LoteProducto, cantidad: Double, guiaRetiro: String, notaCredito: String, motivo: String, modalidad: String, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onRegistrarCanje: (lote: LoteProducto, cantidad: Double, nuevoLote: String, nuevoVencimiento: String, guiaCanje: String, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onCambiarBloqueo: (lote: LoteProducto, ponerEnCuarentena: Boolean, cantidad: Double, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onAnularLote: (lote: LoteProducto, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit,
    onRegistrarMerma: (lote: LoteProducto, cantidad: Double, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit
) {
    var vista by remember { mutableStateOf(VistaLote.FICHA) }

    val diasVencimiento = ProductDetailMapper.diasHastaVencer(lote.vencimiento)
    val colorVencimiento = ProductDetailMapper.colorVencimiento(diasVencimiento)
    val estaEnCuarentena = lote.cantidadBloqueada > 0
    val esRefrigerado = product.temperaturaAlmacenamiento == "REFRIGERACION"
    val esControlado = product.clasificacionControl.uppercase() in listOf("PSICOTROPICO", "CONTROLADO", "ESTUPEFACIENTE") || product.requiereReceta

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FDColors.Overlay)
                .clickable(onClick = onDismiss)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(440.dp)
                    .align(Alignment.CenterEnd)
                    .clickable(enabled = false) {}
                    .windowInsetsPadding(WindowInsets.systemBars),
                color = FDColors.SurfaceElevated,
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                border = BorderStroke(0.5.dp, FDColors.BorderStrong.copy(alpha = 0.9f)),
                shadowElevation = 16.dp,
                tonalElevation = 0.dp
            ) {
                AnimatedContent(
                    targetState = vista,
                    transitionSpec = {
                        slideInHorizontally(tween(280)) { w -> w } + fadeIn(tween(220)) togetherWith
                            slideOutHorizontally(tween(280)) { w -> -w } + fadeOut(tween(220))
                    },
                    label = "vistaLote"
                ) { target ->
                    when (target) {
                        VistaLote.FICHA -> ContenidoFicha(
                            product = product,
                            lote = lote,
                            esEsteElLotePrioritario = esEsteElLotePrioritario,
                            onDefinirPrioridad = onDefinirPrioridad,
                            isPrivileged = isPrivileged,
                            onDismiss = onDismiss,
                            onIngresarMasStock = onIngresarMasStock,
                            onAbrirCuarentena = { vista = VistaLote.CUARENTENA },
                            onAbrirDevolucion = { vista = VistaLote.DEVOLUCION },
                            onAbrirMerma = { vista = VistaLote.MERMA },
                            onAbrirAnulacion = { vista = VistaLote.ANULACION },
                            diasVencimiento = diasVencimiento,
                            colorVencimiento = colorVencimiento,
                            estaEnCuarentena = estaEnCuarentena,
                            esRefrigerado = esRefrigerado,
                            esControlado = esControlado
                        )
                        VistaLote.CUARENTENA -> ContenidoCuarentenaInline(
                            product = product,
                            lote = lote,
                            onVolver = { vista = VistaLote.FICHA },
                            onDismissDrawer = onDismiss,
                            onCambiarBloqueo = onCambiarBloqueo
                        )
                        VistaLote.DEVOLUCION -> ContenidoDevolucionInline(
                            product = product,
                            lote = lote,
                            onVolver = { vista = VistaLote.FICHA },
                            onDismissDrawer = onDismiss,
                            onRegistrarDevolucion = onRegistrarDevolucion,
                            onRegistrarCanje = onRegistrarCanje
                        )
                        VistaLote.MERMA -> ContenidoMermaInline(
                            product = product,
                            lote = lote,
                            onVolver = { vista = VistaLote.FICHA },
                            onDismissDrawer = onDismiss,
                            onRegistrarMerma = onRegistrarMerma
                        )
                        VistaLote.ANULACION -> ContenidoAnulacionInline(
                            product = product,
                            lote = lote,
                            onVolver = { vista = VistaLote.FICHA },
                            onDismissDrawer = onDismiss,
                            onAnularLote = onAnularLote
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FICHA — vista principal nítida
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContenidoFicha(
    product: MoldeProductos,
    lote: LoteProducto,
    esEsteElLotePrioritario: Boolean,
    onDefinirPrioridad: (Boolean) -> Unit,
    isPrivileged: Boolean,
    onDismiss: () -> Unit,
    onIngresarMasStock: (LoteProducto) -> Unit,
    onAbrirCuarentena: () -> Unit,
    onAbrirDevolucion: () -> Unit,
    onAbrirMerma: () -> Unit,
    onAbrirAnulacion: () -> Unit,
    diasVencimiento: Int?,
    colorVencimiento: Color,
    estaEnCuarentena: Boolean,
    esRefrigerado: Boolean,
    esControlado: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "LOTE",
                        style = FDType.Label.copy(fontSize = 10.sp, letterSpacing = 1.4.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.Bold)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = lote.numero.ifBlank { "S/N" },
                            style = FDType.Heading2.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = FDColors.TextPrimary, letterSpacing = (-0.3).sp)
                        )
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (estaEnCuarentena) FDColors.Warning else FDColors.Success))
                    }
                    Text(
                        text = buildString {
                            append(product.categoriaPrincipal.ifBlank { "General" })
                            append(" · ")
                            append(lote.proveedorNombre.ifBlank { "Almacén General" })
                            val fecha = lote.fecha.ifBlank { lote.createdAt }
                            if (fecha.isNotBlank()) { append(" · "); append(fecha.take(10)) }
                        },
                        style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(FDColors.Background).border(0.5.dp, FDColors.Border, CircleShape).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Close, null, tint = FDColors.TextSecondary, modifier = Modifier.size(16.dp)) }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PremiumPill(
                    texto = when {
                        lote.cantidadBloqueada > 0 && lote.cantidad == 0.0 -> "CUARENTENA TOTAL"
                        lote.cantidadBloqueada > 0 -> "CUARENTENA · ${lote.cantidadBloqueada.toInt()} BLOQ."
                        else -> "ACTIVO · EN VENTA"
                    },
                    color = if (estaEnCuarentena) FDColors.Warning else FDColors.Success, filled = true
                )
                if (esRefrigerado) PremiumPill(texto = "FRÍO 2°–8°", color = FDColors.TextSecondary, filled = false, icono = Icons.Outlined.AcUnit)
                if (esControlado) PremiumPill(texto = "CONTROLADO", color = FDColors.Warning, filled = false, icono = Icons.Outlined.Shield)
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.7f), thickness = 0.5.dp)

            // Hero
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("EXISTENCIA FÍSICA", style = FDType.Label.copy(fontSize = 10.sp, letterSpacing = 0.9.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.Bold))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${lote.cantidad.toInt()}", style = FDType.NumericLg.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary, letterSpacing = (-0.8).sp))
                        Text(product.empaque.ifBlank { "Und." }, style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = FDColors.TextSecondary), modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Text(
                        if (lote.cantidadBloqueada > 0) "${(lote.cantidad).toInt()} disp. · ${lote.cantidadBloqueada.toInt()} en cuarentena" else "Disponible en mostrador · listo para venta",
                        style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                    )
                }
                Box(modifier = Modifier.height(64.dp).width(0.5.dp).background(FDColors.Border.copy(alpha = 0.8f)))
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("VENCIMIENTO", style = FDType.Label.copy(fontSize = 10.sp, letterSpacing = 0.9.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.Bold))
                    Text(lote.vencimiento.ifBlank { "—" }, style = FDType.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colorVencimiento, fontFamily = FontFamily.Monospace))
                    Surface(color = colorVencimiento.copy(alpha = 0.12f), shape = RoundedCornerShape(100.dp), border = BorderStroke(0.5.dp, colorVencimiento.copy(alpha = 0.35f))) {
                        Text(
                            when {
                                diasVencimiento == null -> "Vigente"
                                diasVencimiento < 0 -> "Vencido ${-diasVencimiento}d"
                                diasVencimiento == 0 -> "Vence hoy"
                                diasVencimiento <= 30 -> "Vence en $diasVencimiento días"
                                else -> "Vigente · $diasVencimiento días"
                            },
                            style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorVencimiento),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.7f), thickness = 0.5.dp)

            // Trazabilidad plana
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("TRAZABILIDAD", style = FDType.Label.copy(fontSize = 10.sp, letterSpacing = 1.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 10.dp))
                PremiumDossierRow(label = "Proveedor", value = lote.proveedorNombre.ifBlank { "Almacén General" })
                PremiumDivider()
                PremiumDossierRow(label = "Comprobante", value = lote.nroFactura.ifBlank { "S/C" })
                val fechaNacimiento = lote.fecha.ifBlank { lote.createdAt }
                if (fechaNacimiento.isNotBlank()) { PremiumDivider(); PremiumDossierRow(label = "Ingresó", value = fechaNacimiento) }
                if (product.registroSanitario.isNotBlank()) { PremiumDivider(); PremiumDossierRow(label = "Registro Sanitario", value = product.registroSanitario) }
                if (product.principioActivo.isNotBlank()) { PremiumDivider(); PremiumDossierRow(label = "Principio Activo", value = product.principioActivo) }
                if (product.ubicacion.isNotBlank()) { PremiumDivider(); PremiumDossierRow(label = "Ubicación", value = product.ubicacion) }
                if (lote.costoCompraUnitario > 0 || lote.costoUltimoIngresoUnitario > 0) {
                    val costo = if (lote.costoCompraUnitario > 0) lote.costoCompraUnitario else lote.costoUltimoIngresoUnitario
                    PremiumDivider()
                    PremiumDossierRow(label = "Costo unitario", value = "S/ ${String.format(Locale.US, "%.2f", costo)}", isMonospace = true)
                }
            }
        }

        // Acciones ancladas
        Column(
            modifier = Modifier.fillMaxWidth().background(FDColors.SurfaceElevated).padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.6f), thickness = 0.5.dp)
            Button(
                onClick = { onDismiss(); onIngresarMasStock(lote) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).bounceClick()
            ) {
                Icon(Icons.Outlined.AddBox, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("INGRESAR MÁS STOCK A ESTE LOTE", style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = FDColors.PrimaryText))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccionLoteButtonPremium(texto = if (esEsteElLotePrioritario) "SIN PRIORIDAD" else "USAR PRIMERO", icono = if (esEsteElLotePrioritario) Icons.Filled.Star else Icons.Outlined.Star, tinteIcono = if (esEsteElLotePrioritario) FDColors.Warning else FDColors.TextSecondary, onClick = { onDefinirPrioridad(!esEsteElLotePrioritario) }, enabled = isPrivileged, modifier = Modifier.weight(1f))
                AccionLoteButtonPremium(texto = "DEVOLUCIÓN", icono = Icons.AutoMirrored.Outlined.AssignmentReturn, tinteIcono = FDColors.TextSecondary, onClick = onAbrirDevolucion, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccionLoteButtonPremium(texto = "CUARENTENA", icono = Icons.Outlined.Lock, tinteIcono = FDColors.Warning, onClick = onAbrirCuarentena, modifier = Modifier.weight(1f))
                AccionLoteButtonPremium(texto = "MERMA", icono = Icons.Outlined.DeleteOutline, tinteIcono = FDColors.Error, onClick = onAbrirMerma, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onAbrirAnulacion).padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.Undo, null, tint = FDColors.TextTertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Anular ingreso por error de digitación", style = FDType.Caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = FDColors.TextTertiary))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CUARENTENA INLINE — sistema 2 pasos adelante: Todo + motivo ya listo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContenidoCuarentenaInline(
    product: MoldeProductos,
    lote: LoteProducto,
    onVolver: () -> Unit,
    onDismissDrawer: () -> Unit,
    onCambiarBloqueo: (LoteProducto, Boolean, Double, String, (Result<Unit>) -> Unit) -> Unit
) {
    val totalDisponible = lote.cantidad
    val totalBloqueada = lote.cantidadBloqueada
    val hayDisponibles = totalDisponible > 0

    var selectedIndex by remember { mutableIntStateOf(if (hayDisponibles) 0 else 1) }
    val esBloquear = selectedIndex == 0
    val maxPermitido = if (esBloquear) totalDisponible else totalBloqueada

    // Automatizado: Todo por defecto, cantidad ya lista
    var modoCantidadIdx by remember { mutableIntStateOf(1) } // 1 = Todo (sistema adelante)
    var cantidadStr by remember(maxPermitido) { mutableStateOf(maxPermitido.toInt().toString()) }
    var motivo by remember(esBloquear) { mutableStateOf(if (esBloquear) "Alerta sanitaria / En investigación" else "Inspección superada / Lote apto") }
    var isProcesando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val cantNum = if (modoCantidadIdx == 1) maxPermitido else (cantidadStr.replace(",", ".").toDoubleOrNull() ?: 0.0)
    val esValido = cantNum > 0 && cantNum <= maxPermitido && motivo.isNotBlank()
    var mostrarEdicion by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        InlineHeader(titulo = "Cuarentena — ${lote.numero}", icono = if (esBloquear) Icons.Outlined.Lock else Icons.Outlined.LockOpen, colorIcono = if (esBloquear) FDColors.Warning else FDColors.Success, onVolver = onVolver, onClose = onDismissDrawer)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (mensajeError != null) InlineError(mensajeError!!)

            // Barra visual nítida — verde disponible · ámbar cuarentena (sin weight 0)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(100.dp)).background(FDColors.Border)) {
                    val total = (totalDisponible + totalBloqueada).coerceAtLeast(1.0)
                    val dispFrac = (totalDisponible / total).toFloat().coerceIn(0f, 1f)
                    when {
                        dispFrac <= 0f -> Box(modifier = Modifier.fillMaxSize().background(FDColors.Warning))
                        dispFrac >= 1f -> Box(modifier = Modifier.fillMaxSize().background(FDColors.Success))
                        else -> {
                            Box(modifier = Modifier.fillMaxHeight().weight(dispFrac).background(FDColors.Success))
                            Box(modifier = Modifier.fillMaxHeight().weight(1f - dispFrac).background(FDColors.Warning))
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mostrador ${totalDisponible.toInt()}", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.Success, fontWeight = FontWeight.Bold))
                    Text("Cuarentena ${totalBloqueada.toInt()}", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.Warning, fontWeight = FontWeight.Bold))
                }
            }

            // Resumen automático — sistema 2 pasos adelante
            Surface(color = FDColors.Background, shape = RoundedCornerShape(10.dp), border = BorderStroke(0.5.dp, FDColors.Border)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            if (esBloquear) "Retener ${cantNum.toInt()} ${product.empaque} de ${lote.numero}" else "Liberar ${cantNum.toInt()} ${product.empaque} a mostrador",
                            style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextPrimary)
                        )
                        Text(motivo, style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { mostrarEdicion = !mostrarEdicion }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(if (mostrarEdicion) "OCULTAR" else "EDITAR", style = FDType.Label.copy(fontSize = 10.sp, color = FDColors.Primary))
                    }
                }
            }

            AnimatedVisibility(visible = mostrarEdicion, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    EnterpriseSegmentedControl(
                        options = listOf("Retener (${totalDisponible.toInt()})", "Liberar (${totalBloqueada.toInt()})"),
                        selectedIndex = selectedIndex,
                        onSelect = {
                            selectedIndex = it
                            motivo = if (it == 0) "Alerta sanitaria / En investigación" else "Inspección superada / Lote apto"
                            modoCantidadIdx = 1
                            cantidadStr = (if (it == 0) totalDisponible else totalBloqueada).toInt().toString()
                            mensajeError = null
                        },
                        enabled = !isProcesando,
                        activeColor = if (esBloquear) FDColors.Warning else FDColors.Success
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (esBloquear) "¿Cuánto retienes? *" else "¿Cuánto liberas? *", style = FDType.Label.copy(fontSize = 10.5.sp, color = FDColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
                        EnterpriseSegmentedControl(
                            options = listOf("Parcial", "Todo (${maxPermitido.toInt()})"),
                            selectedIndex = modoCantidadIdx,
                            onSelect = {
                                modoCantidadIdx = it
                                cantidadStr = if (it == 1) maxPermitido.toInt().toString() else ""
                                mensajeError = null
                            },
                            enabled = !isProcesando
                        )
                        if (modoCantidadIdx == 0) {
                            val hayError = cantNum > maxPermitido || (cantidadStr.isNotBlank() && cantNum <= 0)
                            EnterpriseInputField(
                                value = cantidadStr,
                                onValueChange = { cantidadStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' }; mensajeError = null },
                                enabled = !isProcesando,
                                label = "Unidades *",
                                placeholder = "Máx: ${maxPermitido.toInt()}",
                                isError = hayError,
                                errorMessage = if (cantNum > maxPermitido) "Supera saldo (${maxPermitido.toInt()})" else "Mínimo 1"
                            )
                        }
                    }

                    EnterpriseInputField(
                        value = motivo,
                        onValueChange = { motivo = it; mensajeError = null },
                        enabled = !isProcesando,
                        label = "Motivo sanitario *",
                        minLines = 2, singleLine = false
                    )
                }
            }

            if (!mostrarEdicion) {
                Text(
                    if (esBloquear) "Quedarán ${(totalDisponible - cantNum).toInt()} en mostrador · listo para confirmar."
                    else "Quedarán ${(totalBloqueada - cantNum).toInt()} en cuarentena · listo para confirmar.",
                    style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextTertiary)
                )
            }
        }

        // Barra inferior fija — como ficha, pero dentro del mismo panel
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onVolver, enabled = !isProcesando, shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, FDColors.BorderStrong), modifier = Modifier.weight(1f).height(44.dp)
            ) { Text("VOLVER", style = FDType.Label.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary)) }

            Button(
                onClick = {
                    if (isProcesando) return@Button
                    isProcesando = true; mensajeError = null
                    onCambiarBloqueo(lote, esBloquear, cantNum, motivo) { result ->
                        isProcesando = false
                        if (result.isSuccess) onDismissDrawer()
                        else mensajeError = result.exceptionOrNull()?.message ?: "Error al actualizar cuarentena."
                    }
                },
                enabled = esValido && !isProcesando,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (esBloquear) FDColors.Warning else FDColors.Success, contentColor = Color.White),
                modifier = Modifier.weight(1.4f).height(44.dp)
            ) {
                if (isProcesando) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)); Text("PROCESANDO", style = FDType.Label.copy(fontSize = 11.sp, color = Color.White)) }
                else Text(if (esBloquear) "CONFIRMAR RETENCIÓN" else "LIBERAR", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DEVOLUCIÓN INLINE — Canje/Nota con todo pre-llenado
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContenidoDevolucionInline(
    product: MoldeProductos,
    lote: LoteProducto,
    onVolver: () -> Unit,
    onDismissDrawer: () -> Unit,
    onRegistrarDevolucion: (LoteProducto, Double, String, String, String, String, (Result<Unit>) -> Unit) -> Unit,
    onRegistrarCanje: (LoteProducto, Double, String, String, String, String, (Result<Unit>) -> Unit) -> Unit
) {
    val totalDisponible = lote.cantidad
    val costoUnitario = remember(lote) { calcularCostoUnitario(lote) }

    // Sistema adelante: Canje por defecto, Todo, motivo ya elegido
    var esCanje by remember { mutableStateOf(true) }
    var cantidadStr by remember { mutableStateOf(totalDisponible.toInt().toString()) }
    var motivo by remember(esCanje) { mutableStateOf(motivosPorModalidad(esCanje).first()) }
    var guiaCanje by remember { mutableStateOf("") }
    var guiaRetiro by remember { mutableStateOf("") }
    var notaCredito by remember { mutableStateOf("") }
    var esLoteNuevo by remember { mutableStateOf(false) }
    var nuevoLote by remember { mutableStateOf("") }
    var nuevoVenc by remember { mutableStateOf("") }
    var isProcesando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val cantNum = cantidadStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val hayErrorCant = cantNum > totalDisponible || (cantidadStr.isNotBlank() && cantNum <= 0)
    val esValido = if (esCanje) {
        cantNum > 0 && cantNum <= totalDisponible && motivo.isNotBlank() && guiaCanje.isNotBlank() && (if (esLoteNuevo) nuevoLote.isNotBlank() && esFechaVencimientoValida(nuevoVenc) else true)
    } else {
        cantNum > 0 && cantNum <= totalDisponible && motivo.isNotBlank() && guiaRetiro.isNotBlank()
    }
    var mostrarEdicion by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        InlineHeader(titulo = "Devolución — ${lote.numero}", icono = Icons.AutoMirrored.Outlined.AssignmentReturn, colorIcono = FDColors.Primary, onVolver = onVolver, onClose = onDismissDrawer)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (mensajeError != null) InlineError(mensajeError!!)

            // Tabs — sistema adivina Canje si vence pronto
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                TabInline(texto = "Canje", seleccionado = esCanje, onClick = { esCanje = true; motivo = motivosPorModalidad(true).first(); mensajeError = null }, modifier = Modifier.weight(1f))
                TabInline(texto = "Nota de crédito", seleccionado = !esCanje, onClick = { esCanje = false; motivo = motivosPorModalidad(false).first(); mensajeError = null }, modifier = Modifier.weight(1f))
            }
            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

            // Resumen 1 toque — hermoso y nítido
            Surface(color = FDColors.Background, shape = RoundedCornerShape(10.dp), border = BorderStroke(0.5.dp, FDColors.Border)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            "${if (esCanje) "Canje" else "Nota"} · ${cantNum.toInt()} ${product.empaque} · ${motivo.take(22)}",
                            style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextPrimary),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (esCanje) "Salen ${cantNum.toInt()} → Entran ${cantNum.toInt()} · Guía: ${guiaCanje.ifBlank { "—" }}" else "Descuento S/ ${String.format(Locale.US, "%.2f", cantNum * costoUnitario)} · Guía: ${guiaRetiro.ifBlank { "—" }}",
                            style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { mostrarEdicion = !mostrarEdicion }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text(if (mostrarEdicion) "OCULTAR" else "EDITAR", style = FDType.Label.copy(fontSize = 10.sp, color = FDColors.Primary))
                    }
                }
            }

            AnimatedVisibility(visible = mostrarEdicion, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    EnterpriseInputField(
                        value = cantidadStr,
                        onValueChange = { cantidadStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' }; mensajeError = null },
                        enabled = !isProcesando,
                        label = "CANTIDAD *",
                        placeholder = "Máx: ${totalDisponible.toInt()} ${product.empaque}",
                        isError = hayErrorCant,
                        errorMessage = if (cantNum > totalDisponible) "Supera saldo (${totalDisponible.toInt()})" else "Mínimo 1"
                    )

                    if (esCanje) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("¿Lote nuevo?", style = FDType.Caption.copy(color = FDColors.TextSecondary))
                            Switch(checked = esLoteNuevo, onCheckedChange = { esLoteNuevo = it; mensajeError = null }, enabled = !isProcesando, colors = SwitchDefaults.colors(checkedThumbColor = FDColors.Primary, checkedTrackColor = FDColors.Primary.copy(alpha = 0.4f)))
                            Text(if (esLoteNuevo) "Sí" else "Mismo lote", style = FDType.Caption.copy(color = FDColors.TextPrimary, fontWeight = FontWeight.Bold))
                        }
                        if (esLoteNuevo) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                EnterpriseInputField(value = nuevoLote, onValueChange = { nuevoLote = it.uppercase(); mensajeError = null }, enabled = !isProcesando, label = "N° LOTE NUEVO *", placeholder = "Lote entrega", modifier = Modifier.weight(1.2f))
                                EnterpriseInputField(value = nuevoVenc, onValueChange = { nuevoVenc = it; mensajeError = null }, enabled = !isProcesando, label = "VENC. *", placeholder = "MM/AAAA", isError = nuevoVenc.isNotBlank() && !esFechaVencimientoValida(nuevoVenc), modifier = Modifier.weight(0.8f))
                            }
                        }
                        EnterpriseInputField(value = guiaCanje, onValueChange = { guiaCanje = it.uppercase(); mensajeError = null }, enabled = !isProcesando, label = "GUÍA DE CANJE *", placeholder = "Comprobante de entrega")
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            EnterpriseInputField(value = guiaRetiro, onValueChange = { guiaRetiro = it.uppercase(); mensajeError = null }, enabled = !isProcesando, label = "GUÍA RETIRO *", placeholder = "Ticket", modifier = Modifier.weight(1f))
                            EnterpriseInputField(value = notaCredito, onValueChange = { notaCredito = it.uppercase(); mensajeError = null }, enabled = !isProcesando, label = "NOTA CRÉDITO", placeholder = "Opcional", modifier = Modifier.weight(1f))
                        }
                    }

                    Text("Motivo *", style = FDType.Label.copy(fontSize = 10.5.sp, color = FDColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
                    EnterpriseReasonList(reasons = motivosPorModalidad(esCanje), selectedReason = motivo, onSelectReason = { motivo = it; mensajeError = null }, enabled = !isProcesando)
                }
            }

            if (!mostrarEdicion) {
                Text(
                    if (esCanje) "Stock intacto tras canje · listo para confirmar." else "Se descontará de inventario y cuentas por pagar.",
                    style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextTertiary)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onVolver, enabled = !isProcesando, shape = RoundedCornerShape(10.dp), border = BorderStroke(0.5.dp, FDColors.BorderStrong), modifier = Modifier.weight(1f).height(44.dp)) {
                Text("VOLVER", style = FDType.Label.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary))
            }
            Button(
                onClick = {
                    if (isProcesando) return@Button
                    isProcesando = true; mensajeError = null
                    if (esCanje) {
                        onRegistrarCanje(lote, cantNum, if (esLoteNuevo) nuevoLote else lote.numero, if (esLoteNuevo) nuevoVenc else lote.vencimiento, guiaCanje, motivo) { result ->
                            isProcesando = false
                            if (result.isSuccess) onDismissDrawer() else mensajeError = result.exceptionOrNull()?.message ?: "Error canje."
                        }
                    } else {
                        onRegistrarDevolucion(lote, cantNum, guiaRetiro, notaCredito, motivo, "NOTA_CREDITO_DINERO") { result ->
                            isProcesando = false
                            if (result.isSuccess) onDismissDrawer() else mensajeError = result.exceptionOrNull()?.message ?: "Error devolución."
                        }
                    }
                },
                enabled = esValido && !isProcesando,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText),
                modifier = Modifier.weight(1.4f).height(44.dp)
            ) {
                if (isProcesando) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = FDColors.PrimaryText); Spacer(Modifier.width(8.dp)); Text("PROCESANDO", style = FDType.Label.copy(fontSize = 11.sp)) }
                else Text(if (esCanje) "PROCESAR CANJE" else "GENERAR NOTA", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun TabInline(texto: String, seleccionado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable(onClick = onClick).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(texto.uppercase(), style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium, color = if (seleccionado) FDColors.Primary else FDColors.TextSecondary))
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(if (seleccionado) FDColors.Primary else Color.Transparent))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MERMA INLINE — Todo + motivo listo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContenidoMermaInline(
    product: MoldeProductos,
    lote: LoteProducto,
    onVolver: () -> Unit,
    onDismissDrawer: () -> Unit,
    onRegistrarMerma: (LoteProducto, Double, String, (Result<Unit>) -> Unit) -> Unit
) {
    val totalLote = lote.cantidad
    var modoCantidadIdx by remember { mutableIntStateOf(1) } // Todo por defecto
    var cantidadStr by remember(totalLote) { mutableStateOf(totalLote.toInt().toString()) }
    var motivoMerma by remember { mutableStateOf("Vencimiento / Caducado") }
    var isProcesando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val costoUnitario = if (lote.costoCompraUnitario > 0) lote.costoCompraUnitario else if (lote.costoUltimoIngresoUnitario > 0) lote.costoUltimoIngresoUnitario else if (lote.cantidad > 0) lote.costoUltimoIngreso / lote.cantidad else 0.0
    val cantNum = if (modoCantidadIdx == 1) totalLote else (cantidadStr.replace(",", ".").toDoubleOrNull() ?: 0.0)
    val totalPerdida = cantNum * costoUnitario
    val esValido = cantNum > 0 && cantNum <= totalLote
    var mostrarEdicion by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        InlineHeader(titulo = "Merma — ${lote.numero}", icono = Icons.Outlined.DeleteOutline, colorIcono = FDColors.Error, onVolver = onVolver, onClose = onDismissDrawer)

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (mensajeError != null) InlineError(mensajeError!!)

            // Barra hermosa — pérdida visual (sin weight 0)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(100.dp)).background(FDColors.Border)) {
                    val totalM = (totalLote + lote.cantidadBloqueada).coerceAtLeast(1.0)
                    val frac = (cantNum / totalM).toFloat().coerceIn(0f, 1f)
                    when {
                        frac <= 0f -> Box(modifier = Modifier.fillMaxSize().background(Color.Transparent))
                        frac >= 1f -> Box(modifier = Modifier.fillMaxSize().background(FDColors.Error))
                        else -> {
                            Box(modifier = Modifier.fillMaxHeight().weight(frac).background(FDColors.Error))
                            Box(modifier = Modifier.fillMaxHeight().weight(1f - frac).background(Color.Transparent))
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${cantNum.toInt()} ${product.empaque} a mermar", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.Error, fontWeight = FontWeight.Bold))
                    Text("Quedan ${(totalLote - cantNum).toInt()} ${product.empaque}", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.TextSecondary))
                }
            }

            // Resumen 1 toque
            Surface(color = FDColors.ErrorSubtle, shape = RoundedCornerShape(10.dp), border = BorderStroke(0.5.dp, FDColors.Error.copy(alpha = 0.28f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Text("Merma ${cantNum.toInt()} ${product.empaque} · ${motivoMerma}", style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Pérdida S/ ${String.format(Locale.US, "%.2f", totalPerdida)}", style = FDType.Caption.copy(color = FDColors.Error, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                    }
                    TextButton(onClick = { mostrarEdicion = !mostrarEdicion }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text(if (mostrarEdicion) "OCULTAR" else "EDITAR", style = FDType.Label.copy(fontSize = 10.sp, color = FDColors.Error))
                    }
                }
            }
            if (lote.cantidadBloqueada > 0) Text("⚠ ${lote.cantidadBloqueada.toInt()} en cuarentena — desbloquea antes.", style = FDType.Caption.copy(fontSize = 10.sp, color = FDColors.Warning, fontWeight = FontWeight.Bold))

            AnimatedVisibility(visible = mostrarEdicion, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("¿Cuánto se perdió? *", style = FDType.Label.copy(fontSize = 10.5.sp, color = FDColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
                    EnterpriseSegmentedControl(
                        options = listOf("Parcial", "Todo (${totalLote.toInt()} ${product.empaque})"),
                        selectedIndex = modoCantidadIdx,
                        onSelect = { modoCantidadIdx = it; cantidadStr = if (it == 1) totalLote.toInt().toString() else ""; mensajeError = null },
                        enabled = !isProcesando
                    )
                    if (modoCantidadIdx == 0) {
                        val hayError = cantNum > totalLote || (cantidadStr.isNotBlank() && cantNum <= 0)
                        EnterpriseInputField(
                            value = cantidadStr, onValueChange = { cantidadStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' }; mensajeError = null },
                            enabled = !isProcesando, label = "Unidades *", placeholder = "Máx: ${totalLote.toInt()}",
                            isError = hayError, errorMessage = if (cantNum > totalLote) "Supera saldo (${totalLote.toInt()})" else "Mínimo 1"
                        )
                    }
                    Text("¿Por qué?", style = FDType.Label.copy(fontSize = 10.5.sp, color = FDColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
                    EnterpriseReasonList(
                        reasons = listOf("Vencimiento / Caducado", "Frasco Roto / Daño Físico", "Pérdida Cadena Frío", "Defecto de Fábrica"),
                        selectedReason = motivoMerma, onSelectReason = { motivoMerma = it; mensajeError = null }, enabled = !isProcesando
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onVolver, enabled = !isProcesando, shape = RoundedCornerShape(10.dp), border = BorderStroke(0.5.dp, FDColors.BorderStrong), modifier = Modifier.weight(1f).height(44.dp)) {
                Text("VOLVER", style = FDType.Label.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary))
            }
            Button(
                onClick = {
                    if (isProcesando) return@Button
                    isProcesando = true; mensajeError = null
                    onRegistrarMerma(lote, cantNum, motivoMerma) { result ->
                        isProcesando = false
                        if (result.isSuccess) onDismissDrawer() else mensajeError = result.exceptionOrNull()?.message ?: "Error merma."
                    }
                },
                enabled = esValido && !isProcesando, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FDColors.Error, contentColor = Color.White),
                modifier = Modifier.weight(1.4f).height(44.dp)
            ) {
                if (isProcesando) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)); Text("REGISTRANDO", style = FDType.Label.copy(fontSize = 11.sp, color = Color.White)) }
                else Text("CONFIRMAR MERMA", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ANULACIÓN INLINE — motivo ya listo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContenidoAnulacionInline(
    product: MoldeProductos,
    lote: LoteProducto,
    onVolver: () -> Unit,
    onDismissDrawer: () -> Unit,
    onAnularLote: (LoteProducto, String, (Result<Unit>) -> Unit) -> Unit
) {
    var motivo by remember { mutableStateOf("Corrección administrativa por error al ingresar factura") }
    var isProcesando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        InlineHeader(titulo = "Anular — ${lote.numero}", icono = Icons.Outlined.Warning, colorIcono = FDColors.Error, onVolver = onVolver, onClose = onDismissDrawer)

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (mensajeError != null) InlineError(mensajeError!!)

            Surface(color = FDColors.ErrorSubtle, shape = RoundedCornerShape(8.dp), border = BorderStroke(0.5.dp, FDColors.Error.copy(alpha = 0.25f))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Se eliminarán ${(lote.cantidad + lote.cantidadBloqueada).toInt()} ${product.empaque} de este ingreso. No se puede deshacer.", style = FDType.Caption.copy(color = FDColors.Error, fontWeight = FontWeight.Bold))
                    Text("Lote: ${lote.numero} · Factura: ${lote.nroFactura.ifBlank { "S/C" }}", style = FDType.Caption.copy(color = FDColors.TextSecondary))
                }
            }

            EnterpriseInputField(value = motivo, onValueChange = { motivo = it; mensajeError = null }, enabled = !isProcesando, label = "¿Por qué lo anulas? *", placeholder = "Ej: factura duplicada", minLines = 3, singleLine = false)
            Text("Borra el lote y ajusta la factura.", style = FDType.Caption.copy(color = FDColors.TextSecondary, fontSize = 11.sp))
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onVolver, enabled = !isProcesando, shape = RoundedCornerShape(10.dp), border = BorderStroke(0.5.dp, FDColors.BorderStrong), modifier = Modifier.weight(1f).height(44.dp)) {
                Text("VOLVER", style = FDType.Label.copy(fontSize = 11.5.sp, color = FDColors.TextSecondary))
            }
            Button(
                onClick = {
                    if (isProcesando) return@Button
                    isProcesando = true; mensajeError = null
                    onAnularLote(lote, motivo) { result ->
                        isProcesando = false
                        if (result.isSuccess) onDismissDrawer() else mensajeError = result.exceptionOrNull()?.message ?: "Error al anular."
                    }
                },
                enabled = motivo.isNotBlank() && !isProcesando, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FDColors.Error, contentColor = Color.White),
                modifier = Modifier.weight(1.4f).height(44.dp)
            ) {
                if (isProcesando) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)); Text("ANULANDO", style = FDType.Label.copy(fontSize = 11.sp, color = Color.White)) }
                else Text("ANULAR REGISTRO", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTES INLINE PREMIUM — nítidos y reutilizables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InlineHeader(titulo: String, icono: ImageVector, colorIcono: Color, onVolver: () -> Unit, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(FDColors.Background).border(0.5.dp, FDColors.Border, CircleShape).clickable(onClick = onVolver),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = FDColors.TextPrimary, modifier = Modifier.size(16.dp)) }
                Icon(icono, null, tint = colorIcono, modifier = Modifier.size(18.dp))
                Text(titulo, style = FDType.Heading3.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(FDColors.Background).border(0.5.dp, FDColors.Border, CircleShape).clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Close, null, tint = FDColors.TextSecondary, modifier = Modifier.size(16.dp)) }
        }
        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.6f), thickness = 0.5.dp)
    }
}

@Composable
private fun InlineError(mensaje: String) {
    Surface(color = FDColors.Error.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), border = BorderStroke(0.5.dp, FDColors.Error.copy(alpha = 0.35f)), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = FDColors.Error, modifier = Modifier.size(16.dp))
            Text(mensaje, style = FDType.Caption.copy(color = FDColors.Error, fontWeight = FontWeight.Bold, fontSize = 11.sp))
        }
    }
}

@Composable
private fun PremiumPill(texto: String, color: Color, filled: Boolean = true, icono: ImageVector? = null) {
    Surface(color = if (filled) color.copy(alpha = 0.12f) else Color.Transparent, shape = RoundedCornerShape(100.dp), border = BorderStroke(0.5.dp, color.copy(alpha = if (filled) 0.32f else 0.45f))) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (icono != null) Icon(icono, null, tint = color, modifier = Modifier.size(12.dp))
            Text(texto, style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = color))
        }
    }
}

@Composable
private fun PremiumDivider() {
    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.55f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 1.dp))
}

@Composable
private fun PremiumDossierRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = FDType.Caption.copy(fontSize = 11.5.sp, color = FDColors.TextTertiary))
        Text(value, style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextPrimary, fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun AccionLoteButtonPremium(texto: String, icono: ImageVector, tinteIcono: Color, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, FDColors.BorderStrong.copy(alpha = if (enabled) 0.9f else 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = FDColors.Surface, contentColor = FDColors.TextPrimary, disabledContainerColor = FDColors.Surface.copy(alpha = 0.6f), disabledContentColor = FDColors.TextTertiary),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp), modifier = modifier.height(44.dp)
    ) {
        Icon(icono, null, tint = tinteIcono.copy(alpha = if (enabled) 1f else 0.5f), modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(texto, style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp, color = if (enabled) FDColors.TextPrimary else FDColors.TextTertiary), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
