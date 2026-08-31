package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Star
import kotlinx.coroutines.delay
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
import com.app.administradorfarmadon.inventario.compartido.logica.CostoRealLote
import com.app.administradorfarmadon.inventario.compartido.ui.SelectorVencimiento
import com.app.administradorfarmadon.inventario.compartido.ui.StepperCantidad
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick

// FICHA —” vista principal nítida

@Composable
internal fun ContenidoFicha(
    product: MoldeProductos,
    lote: LoteProducto,
    fefoAutomatico: Boolean,
    esEsteElLotePrioritario: Boolean,
    onDefinirPrioridad: (Boolean) -> Unit,
    isPrivileged: Boolean,
    onDismiss: () -> Unit,
    onAbrirCuarentena: () -> Unit,
    onAbrirHistorial: () -> Unit,
    onAbrirAnulacion: () -> Unit,
    diasVencimiento: Int?,
    colorVencimiento: Color,
    estaEnCuarentena: Boolean,
    esRefrigerado: Boolean,
    esControlado: Boolean,
    isProcesandoPrioridadExterno: Boolean = false
) {
    var isProcesandoPrioridadLocal by remember { mutableStateOf(false) }
    val isProcesandoPrioridad = isProcesandoPrioridadExterno || isProcesandoPrioridadLocal
    // Se resetea al cambiar de lote FEFO (éxito) o tras timeout si falla — evita spinner pegado
    LaunchedEffect(fefoAutomatico, esEsteElLotePrioritario, product.lotePrioritarioId) {
        isProcesandoPrioridadLocal = false
    }
    LaunchedEffect(isProcesandoPrioridadLocal) {
        if (isProcesandoPrioridadLocal) {
            delay(3500)
            isProcesandoPrioridadLocal = false
        }
    }
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
                        style = FDType.Label.copy(fontSize = 10.sp, letterSpacing = 1.4.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.Black)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = lote.numero.ifBlank { "S/N" },
                            style = FDType.Heading2.copy(fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = FDColors.TextPrimary, letterSpacing = (-0.5).sp)
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
                if (esRefrigerado) PremiumPill(texto = "FRÍO 2° – 8°", color = FDColors.TextSecondary, filled = false, icono = Icons.Outlined.AcUnit)
                if (esControlado) PremiumPill(texto = "CONTROLADO", color = FDColors.Warning, filled = false, icono = Icons.Outlined.Shield)
            }

            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.7f), thickness = 0.5.dp)

            // Hero
            Surface(
                color = FDColors.SurfaceElevated,
                shape = FDShapes.Medium,
                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("EXISTENCIA FÍSICA", style = FDType.Label.copy(fontSize = 10.sp, letterSpacing = 0.9.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.Black))
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${lote.cantidad.toInt()}", style = FDType.NumericLg.copy(fontSize = 34.sp, fontWeight = FontWeight.Black, color = FDColors.Primary, letterSpacing = (-1).sp))
                            Text(product.empaque.ifBlank { "Und." }, style = FDType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FDColors.TextSecondary), modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Text(
                            if (lote.cantidadBloqueada > 0) "${(lote.cantidad).toInt()} disp. · ${lote.cantidadBloqueada.toInt()} en cuarentena" else "Disponible en mostrador · listo para venta",
                            style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                        )
                    }
                    Box(modifier = Modifier.height(70.dp).width(1.dp).background(FDColors.Border.copy(alpha = 0.5f)))
                    Column(modifier = Modifier.weight(0.9f).padding(start = 16.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("VENCIMIENTO", style = FDType.Label.copy(fontSize = 10.sp, letterSpacing = 0.9.sp, color = FDColors.TextTertiary, fontWeight = FontWeight.Black))
                        Text(lote.vencimiento.ifBlank { "—”" }, style = FDType.Body.copy(fontSize = 16.sp, fontWeight = FontWeight.Black, color = colorVencimiento, fontFamily = FontFamily.Monospace))
                        Surface(color = colorVencimiento.copy(alpha = 0.12f), shape = RoundedCornerShape(100.dp), border = BorderStroke(1.dp, colorVencimiento.copy(alpha = 0.35f))) {
                            Text(
                                when {
                                    diasVencimiento == null -> "Vigente"
                                    diasVencimiento < 0 -> "Vencido ${-diasVencimiento}d"
                                    diasVencimiento == 0 -> "Vence hoy"
                                    diasVencimiento <= 30 -> "Vence en $diasVencimiento días"
                                    else -> "Vigente · $diasVencimiento días"
                                },
                                style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = colorVencimiento),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (fefoAutomatico) {
                    Surface(
                        modifier = Modifier.weight(1f).height(52.dp),
                        color = FDColors.Primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.DateRange, null, tint = FDColors.Primary, modifier = Modifier.size(18.dp))
                            Column(Modifier.weight(1f)) {
                                Text("FEFO AUTOMÁTICO ACTIVO", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.4.sp), color = FDColors.Primary)
                                Text("El lote que vence antes se consume primero", style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.TextSecondary)
                            }
                        }
                    }
                } else if (esEsteElLotePrioritario) {
                    Surface(
                        modifier = Modifier.weight(1f).height(52.dp),
                        color = FDColors.Warning.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Star, null, tint = FDColors.Warning, modifier = Modifier.size(18.dp))
                            Column(Modifier.weight(1f)) {
                                Text("LOTE PRINCIPAL DE CONSUMO", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.4.sp), color = FDColors.Warning)
                                Text("Se usa primero en cada venta", style = FDType.Caption.copy(fontSize = 10.sp), color = FDColors.TextSecondary)
                            }
                        }
                    }
                } else {
                    AccionLoteButtonPremium(
                        texto = if (isProcesandoPrioridad) "GUARDANDO..." else "ELEGIR ESTE LOTE DE CONSUMO",
                        icono = Icons.Outlined.Star,
                        tinteIcono = FDColors.TextSecondary,
                        onClick = {
                            if (!isProcesandoPrioridad) {
                                isProcesandoPrioridadLocal = true
                                onDefinirPrioridad(true)
                            }
                        },
                        enabled = isPrivileged && !isProcesandoPrioridad,
                        isProcesando = isProcesandoPrioridad,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (!fefoAutomatico && esEsteElLotePrioritario && isPrivileged) {
                val restaurarProcesando = isProcesandoPrioridad
                if (restaurarProcesando) {
                    Row(modifier = Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = FDColors.Warning)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESTAURANDO...", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FDColors.Warning))
                    }
                } else {
                    TextButton(onClick = {
                        if (!isProcesandoPrioridad) {
                            isProcesandoPrioridadLocal = true
                            onDefinirPrioridad(false)
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Restaurar orden automático (FEFO)", style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = FDColors.Warning)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccionLoteButtonPremium(texto = "HISTORIAL DEL LOTE", icono = Icons.Outlined.History, tinteIcono = FDColors.Primary, onClick = onAbrirHistorial, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccionLoteButtonPremium(texto = "CUARENTENA", icono = Icons.Outlined.Lock, tinteIcono = FDColors.Warning, onClick = onAbrirCuarentena, modifier = Modifier.weight(1f))
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

// CUARENTENA INLINE —” sistema 2 pasos adelante: Todo + motivo ya listo

@Composable
internal fun ContenidoCuarentenaInline(
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

    Column(modifier = Modifier.fillMaxSize()) {
        InlineHeader(
            titulo = "Cuarentena · ${lote.numero}",
            icono = if (esBloquear) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
            colorIcono = if (esBloquear) FDColors.Warning else FDColors.Success,
            onVolver = { if (!isProcesando) onVolver() },
            onClose = { if (!isProcesando) onDismissDrawer() }
        )

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (mensajeError != null) InlineError(mensajeError!!)

            // Barra visual nítida —” verde disponible · ámbar cuarentena (sin weight 0)
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

            // Formulario directo (sin tarjetas decorativas)
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
                    StepperCantidad(
                        cantidad = cantNum.toInt().coerceAtLeast(1),
                        onCantidadChange = { cantidadStr = it.toString(); mensajeError = null },
                        minimo = 1,
                        maximo = maxPermitido.toInt().coerceAtLeast(1),
                        unidad = product.empaque.ifBlank { "Und" }
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

            Text(
                if (esBloquear) "Quedarán ${(totalDisponible - cantNum).toInt()} ${product.empaque} en mostrador."
                else "Quedarán ${(totalBloqueada - cantNum).toInt()} ${product.empaque} en cuarentena.",
                style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = if (esBloquear) FDColors.Warning else FDColors.Success
            )
        }

        // Botón principal fijo (fuera del scroll); el volver ya está en la flecha de la cabecera
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp).height(48.dp)
        ) {
            if (isProcesando) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)); Text("PROCESANDO", style = FDType.Label.copy(fontSize = 11.sp, color = Color.White)) }
            else Text(if (esBloquear) "CONFIRMAR RETENCIÓN" else "LIBERAR", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// DEVOLUCIÓN INLINE —” Canje/Nota con todo pre-llenado
// ──────────────────────────────────────────────────────────────────────────────

@Composable
internal fun ContenidoDevolucionInline(
    product: MoldeProductos,
    lote: LoteProducto,
    onVolver: () -> Unit,
    onDismissDrawer: () -> Unit,
    onRegistrarDevolucion: (LoteProducto, Double, String, String, String, String, (Result<Unit>) -> Unit) -> Unit,
    onRegistrarCanje: (LoteProducto, Double, String, String, String, String, (Result<Unit>) -> Unit) -> Unit
) {
    val totalDisponible = lote.cantidad
    val costoUnitario = remember(lote) { calcularCostoUnitario(lote) }
    val unidad = product.empaque.ifBlank { product.unidadBase }.ifBlank { "Unidad" }

    // Sistema adelante: Canje por defecto, todo el lote, motivo ya elegido
    var esCanje by remember { mutableStateOf(true) }
    var cantidadStr by remember { mutableStateOf(totalDisponible.toInt().toString()) }
    var motivo by remember(esCanje) { mutableStateOf(motivosPorModalidad(esCanje).first()) }
    var guiaCanje by remember { mutableStateOf("") }
    var guiaRetiro by remember { mutableStateOf("") }
    var notaCredito by remember { mutableStateOf("") }
    var nuevoLote by remember { mutableStateOf("") }
    var nuevoVenc by remember { mutableStateOf("") }
    var mismoLoteReposicion by remember { mutableStateOf(false) }
    var isProcesando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val cantNum = cantidadStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val hayErrorCant = cantNum > totalDisponible || (cantidadStr.isNotBlank() && cantNum <= 0)
    val nuevoLoteInvalido = nuevoLote.isNotBlank() && nuevoLote.equals(lote.numero, true)
    val esValido = if (esCanje) {
        cantNum > 0 && cantNum <= totalDisponible && motivo.isNotBlank() && guiaCanje.isNotBlank() &&
            (mismoLoteReposicion || (nuevoLote.isNotBlank() && !nuevoLoteInvalido && esFechaVencimientoValida(nuevoVenc)))
    } else {
        cantNum > 0 && cantNum <= totalDisponible && motivo.isNotBlank() && guiaRetiro.isNotBlank()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        InlineHeader(
            titulo = "Devolución · ${lote.numero}",
            icono = Icons.AutoMirrored.Outlined.AssignmentReturn,
            colorIcono = FDColors.Primary,
            onVolver = { if (!isProcesando) onVolver() },
            onClose = { if (!isProcesando) onDismissDrawer() }
        )

        // Pestañas fijas: Canje / Nota de crédito (siempre visibles, fuera del scroll)
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            TabInline(texto = "Canje", seleccionado = esCanje, onClick = { esCanje = true; motivo = motivosPorModalidad(true).first(); mensajeError = null }, modifier = Modifier.weight(1f))
            TabInline(texto = "Nota de crédito", seleccionado = !esCanje, onClick = { esCanje = false; motivo = motivosPorModalidad(false).first(); mensajeError = null }, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (mensajeError != null) InlineError(mensajeError!!)

            // Contexto real del lote con el que se está trabajando
            Text(
                "Lote ${lote.numero} · Disponible: ${totalDisponible.toInt()} $unidad",
                style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                color = FDColors.Primary
            )

            // Formulario directo, sin tarjetas decorativas
            StepperCantidad(
                cantidad = cantNum.toInt().coerceAtLeast(1),
                onCantidadChange = { cantidadStr = it.toString(); mensajeError = null },
                minimo = 1,
                maximo = totalDisponible.toInt().coerceAtLeast(1),
                unidad = unidad
            )
            Text(
                "Puedes canjear o devolver desde 1 hasta lo disponible del lote (${totalDisponible.toInt()} $unidad). Nunca más del saldo.",
                style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
            )

            if (esCanje) {
                Text(
                    "¿La reposición llega en este mismo lote o en un lote nuevo?",
                    style = FDType.Label.copy(fontSize = 10.5.sp, color = FDColors.TextSecondary, fontWeight = FontWeight.Bold)
                )
                EnterpriseSegmentedControl(
                    options = listOf("Lote nuevo", "Mismo lote"),
                    selectedIndex = if (mismoLoteReposicion) 1 else 0,
                    onSelect = { mismoLoteReposicion = it == 1; mensajeError = null },
                    enabled = !isProcesando
                )
                if (mismoLoteReposicion) {
                    Text(
                        "Las unidades de reposición se suman a este mismo lote: el saldo queda igual, pero el canje queda registrado con su guía y su motivo.",
                        style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                    )
                } else {
                    Text("Lote de reposición (el que entrega el proveedor)", style = FDType.Label.copy(fontSize = 10.5.sp, color = FDColors.TextSecondary, fontWeight = FontWeight.Bold))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        EnterpriseInputField(
                            value = nuevoLote,
                            onValueChange = { nuevoLote = it.uppercase(); mensajeError = null },
                            enabled = !isProcesando,
                            label = "N° LOTE DE REPOSICIÓN *",
                            placeholder = "Lote que entrega el proveedor",
                            isError = nuevoLoteInvalido,
                            errorMessage = if (nuevoLoteInvalido) "No puede ser el mismo lote que se devuelve" else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SelectorVencimiento(
                            vencimiento = nuevoVenc,
                            onVencimientoChange = { nuevoVenc = it; mensajeError = null },
                            label = "Vencimiento",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        "Debe ser un lote distinto del que devuelves. El lote nuevo recibe exactamente la cantidad canjeada.",
                        style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                    )
                }
                EnterpriseInputField(value = guiaCanje, onValueChange = { guiaCanje = it.uppercase(); mensajeError = null }, enabled = !isProcesando, label = "GUÍA DE CANJE *", placeholder = "Comprobante de entrega")
                Text(
                    "Documento físico que prueba la entrega y recepción del canje. Queda guardado para auditoría.",
                    style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    EnterpriseInputField(value = guiaRetiro, onValueChange = { guiaRetiro = it.uppercase(); mensajeError = null }, enabled = !isProcesando, label = "GUÍA DE RETIRO *", placeholder = "Ticket", modifier = Modifier.weight(1f))
                    EnterpriseInputField(value = notaCredito, onValueChange = { notaCredito = it.uppercase(); mensajeError = null }, enabled = !isProcesando, label = "NOTA DE CRÉDITO", placeholder = "Opcional", modifier = Modifier.weight(1f))
                }
                Text(
                    "La guía de retiro prueba que el proveedor se llevó la mercadería. El número de nota de crédito es el documento del proveedor; si aún no lo tienes, déjalo vacío.",
                    style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                )
                val montoReal = CostoRealLote.monto(cantNum, costoUnitario)
                Text(
                    if (costoUnitario > 0)
                        "Costo unitario de compra de este lote: S/ ${String.format(Locale.US, "%.2f", costoUnitario)} · Monto de la nota: S/ ${String.format(Locale.US, "%.2f", montoReal)}"
                    else
                        "Este lote no tiene costo de compra registrado; la nota de crédito se registrará sin monto.",
                    style = FDType.Caption.copy(fontSize = 11.sp),
                    color = if (costoUnitario > 0) FDColors.TextSecondary else FDColors.Warning
                )
            }

            Text("Motivo *", style = FDType.Label.copy(fontSize = 10.5.sp, color = FDColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
            EnterpriseReasonList(reasons = motivosPorModalidad(esCanje), selectedReason = motivo, onSelectReason = { motivo = it; mensajeError = null }, enabled = !isProcesando)
            Text(
                "Explica por qué sale esta mercadería; queda registrado en el historial del lote y en el reclamo.",
                style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
            )
        }

        // Botón principal fijo (fuera del scroll); el volver ya está en la flecha de la cabecera
        Button(
            onClick = {
                if (isProcesando) return@Button
                isProcesando = true; mensajeError = null
                if (esCanje) {
                    onRegistrarCanje(
                        lote,
                        cantNum,
                        if (mismoLoteReposicion) lote.numero else nuevoLote,
                        if (mismoLoteReposicion) lote.vencimiento else nuevoVenc,
                        guiaCanje,
                        motivo
                    ) { result ->
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp).height(48.dp)
        ) {
            if (isProcesando) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = FDColors.PrimaryText); Spacer(Modifier.width(8.dp)); Text("PROCESANDO", style = FDType.Label.copy(fontSize = 11.sp)) }
            else Text(if (esCanje) "PROCESAR CANJE" else "GENERAR NOTA", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
internal fun TabInline(texto: String, seleccionado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable(onClick = onClick).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(texto.uppercase(), style = FDType.Label.copy(fontSize = 11.5.sp, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium, color = if (seleccionado) FDColors.Primary else FDColors.TextSecondary))
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(if (seleccionado) FDColors.Primary else Color.Transparent))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// MERMA INLINE —” Todo + motivo listo
// ──────────────────────────────────────────────────────────────────────────────

// ──────────────────────────────────────────────────────────────────────────────
// ANULACIÓN INLINE —” motivo ya listo
// ──────────────────────────────────────────────────────────────────────────────

@Composable
internal fun ContenidoAnulacionInline(
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
        InlineHeader(
            titulo = "Anular · ${lote.numero}",
            icono = Icons.Outlined.Warning,
            colorIcono = FDColors.Error,
            onVolver = { if (!isProcesando) onVolver() },
            onClose = { if (!isProcesando) onDismissDrawer() }
        )

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

        // Botón principal fijo (fuera del scroll); el volver ya está en la flecha de la cabecera
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp).height(48.dp)
        ) {
            if (isProcesando) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)); Text("ANULANDO", style = FDType.Label.copy(fontSize = 11.sp, color = Color.White)) }
            else Text("ANULAR REGISTRO", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// COMPONENTES INLINE PREMIUM —” nítidos y reutilizables
// ──────────────────────────────────────────────────────────────────────────────

@Composable
internal fun ContenidoHistorialLote(
    lote: LoteProducto,
    movimientos: List<MovimientoInventario>,
    onVolver: () -> Unit,
    onDismissDrawer: () -> Unit
) {
    val delLote = remember(movimientos, lote.numero) {
        movimientos
            .filter { it.loteNumero.trim().equals(lote.numero.trim(), ignoreCase = true) }
            .sortedByDescending { it.fecha }
    }
    Column(Modifier.fillMaxSize()) {
        InlineHeader(
            titulo = "Historial · Lote ${lote.numero.ifBlank { "S/N" }}",
            icono = Icons.Outlined.History,
            colorIcono = FDColors.Primary,
            onVolver = onVolver,
            onClose = onDismissDrawer
        )
        if (delLote.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.History, null, tint = FDColors.TextTertiary.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                    Text("Este lote aún no tiene movimientos registrados", style = FDType.BodySmall, color = FDColors.TextSecondary)
                    Text(
                        "Las entradas, salidas, ventas y anulaciones de ESTE lote aparecerán aquí en tiempo real.",
                        style = FDType.Caption.copy(fontSize = 11.sp),
                        color = FDColors.TextTertiary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().background(FDColors.TextPrimary.copy(alpha = 0.05f)).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TIPO OPERACIÓN", style = FDType.Label.copy(fontSize = 9.5.sp, color = FDColors.TextSecondary), modifier = Modifier.weight(1.6f))
                Text("CANT", style = FDType.Label.copy(fontSize = 9.5.sp, color = FDColors.TextSecondary), modifier = Modifier.weight(0.7f))
                Text("MOTIVO / DOCUMENTO", style = FDType.Label.copy(fontSize = 9.5.sp, color = FDColors.TextSecondary), modifier = Modifier.weight(1.7f))
                Text("RESPONSABLE", style = FDType.Label.copy(fontSize = 9.5.sp, color = FDColors.TextSecondary), modifier = Modifier.weight(1.1f))
                Text("FECHA", style = FDType.Label.copy(fontSize = 9.5.sp, color = FDColors.TextSecondary), modifier = Modifier.weight(1.2f))
            }
            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = 0.5.dp)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(delLote) { mov ->
                    val esEntrada = mov.cantidad > 0
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            ProductDetailMapper.formatearTipoMovimiento(mov.tipo).uppercase(),
                            style = FDType.Body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1.6f)
                        )
                        Text(
                            "${if (esEntrada) "+" else ""}${mov.cantidad.toInt()}",
                            style = FDType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace),
                            color = if (esEntrada) FDColors.Success else FDColors.Error,
                            modifier = Modifier.weight(0.7f)
                        )
                        Text(
                            mov.referencia.ifBlank { "—" },
                            style = FDType.Caption.copy(fontSize = 10.sp),
                            color = FDColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1.7f)
                        )
                        Text(
                            mov.usuarioNombre.ifBlank { "Administrador" },
                            style = FDType.Caption.copy(fontSize = 10.sp),
                            color = FDColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1.1f)
                        )
                        Text(
                            ProductDetailMapper.formatRelativeDate(mov.fecha),
                            style = FDType.Caption.copy(fontSize = 10.sp),
                            color = FDColors.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
internal fun InlineHeader(titulo: String, icono: ImageVector, colorIcono: Color, onVolver: () -> Unit, onClose: () -> Unit) {
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
internal fun InlineError(mensaje: String) {
    Surface(color = FDColors.Error.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), border = BorderStroke(0.5.dp, FDColors.Error.copy(alpha = 0.35f)), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = FDColors.Error, modifier = Modifier.size(16.dp))
            Text(mensaje, style = FDType.Caption.copy(color = FDColors.Error, fontWeight = FontWeight.Bold, fontSize = 11.sp))
        }
    }
}

@Composable
internal fun PremiumPill(texto: String, color: Color, filled: Boolean = true, icono: ImageVector? = null) {
    Surface(color = if (filled) color.copy(alpha = 0.12f) else Color.Transparent, shape = RoundedCornerShape(100.dp), border = BorderStroke(0.5.dp, color.copy(alpha = if (filled) 0.32f else 0.45f))) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (icono != null) Icon(icono, null, tint = color, modifier = Modifier.size(12.dp))
            Text(texto, style = FDType.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = color))
        }
    }
}

@Composable
internal fun PremiumDivider() {
    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.55f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 1.dp))
}

@Composable
internal fun PremiumDossierRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = FDType.Caption.copy(fontSize = 11.5.sp, color = FDColors.TextTertiary))
        Text(value, style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = FDColors.TextPrimary, fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
internal fun AccionLoteButtonPremium(texto: String, icono: ImageVector, tinteIcono: Color, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, isProcesando: Boolean = false) {
    OutlinedButton(
        onClick = onClick, enabled = enabled && !isProcesando, shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, FDColors.BorderStrong.copy(alpha = if (enabled && !isProcesando) 0.9f else 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = FDColors.Surface, contentColor = FDColors.TextPrimary, disabledContainerColor = FDColors.Surface.copy(alpha = 0.6f), disabledContentColor = FDColors.TextTertiary),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp), modifier = modifier.height(44.dp)
    ) {
        if (isProcesando) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = tinteIcono)
            Spacer(Modifier.width(7.dp))
        } else {
            Icon(icono, null, tint = tinteIcono.copy(alpha = if (enabled) 1f else 0.5f), modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(texto, style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp, color = if (enabled && !isProcesando) FDColors.TextPrimary else FDColors.TextTertiary), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

