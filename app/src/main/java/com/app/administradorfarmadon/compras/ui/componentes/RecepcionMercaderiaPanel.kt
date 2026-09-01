package com.app.administradorfarmadon.compras.ui.componentes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDDialogoContenedor
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDSpacing
import com.app.administradorfarmadon.disenotemaapp.ui.FDSizes
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compras.datos.ItemRecepcionEntrega
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.recepcion.logica.RecepcionMercaderiaEstado
import com.app.administradorfarmadon.compras.saldoafavor.ui.SaldoAFavorBanner
import com.app.administradorfarmadon.compras.pagos.logica.EtiquetaMetodoPago
import com.app.administradorfarmadon.compras.pagos.ui.PagosMixtosEditor
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra

@Composable
fun RecepcionMercaderiaPanel(
    pedido: PedidoCompra,
    facturaExistente: FacturaCompra? = null,
    procesando: Boolean = false,
    indiceLotes: Map<String, List<com.app.administradorfarmadon.compras.datos.LoteExistenteVista>> = emptyMap(),
    saldoAFavorDisponible: Double = 0.0,
    metodosPago: List<InstanciaPago> = emptyList(),
    onDismiss: () -> Unit,
    onAsentarRecepcion: (
        numeroFactura: String,
        condicionPago: String,
        fechaVencimientoPago: String,
        montoFactura: Double,
        montoPagado: Double,
        metodoPago: String,
        pagosRecepcion: List<com.app.administradorfarmadon.compras.pagos.datos.PagoDetalle>,
        saldoAFavorUsado: Double,
        itemsRecepcion: List<ItemRecepcionEntrega>,
        cerrarConAjuste: Boolean
    ) -> Unit
) {
    val colores = TokensFarmadon.colores
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }
    val estado = remember(pedido.id, facturaExistente?.id, saldoAFavorDisponible, metodosPago) { RecepcionMercaderiaEstado(pedido, indiceLotes, facturaExistente, saldoAFavorDisponible, metodosPago) }

    var indexFilaEnFoco by remember { mutableIntStateOf(-1) }
    var yFilaSeleccionada by remember { mutableFloatStateOf(0f) }
    var yTotalLiquidacion by remember { mutableFloatStateOf(0f) }
    var rootY by remember { mutableFloatStateOf(0f) }
    
    val yAnimada by animateFloatAsState(
        targetValue = yFilaSeleccionada,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "CableY"
    )

    Surface(
        color = FDColors.Background,
        modifier = Modifier.fillMaxSize().onGloballyPositioned { rootY = it.positionInRoot().y }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            //  TOP BAR EJECUTIVA (ADAPTATIVA AL TEMA) 
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FDColors.Surface)
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "RECEPCIÓN FÍSICA · ${pedido.numeroOrden}", 
                        style = FDType.Label.copy(fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.2.sp), 
                        color = FDColors.TextTertiary
                    )
                    Text(
                        text = pedido.proveedorNombre, 
                        style = FDType.Heading2.copy(fontSize = 19.sp, fontWeight = FontWeight.Black), 
                        color = FDColors.TextPrimary
                    )
                }
                IconButton(
                    onClick = { if (!procesando) onDismiss() }, 
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(FDColors.TextPrimary.copy(alpha = 0.08f))
                ) {
                    Icon(Icons.Default.Close, null, tint = FDColors.TextPrimary)
                }
            }
            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.6f), thickness = 1.dp)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // PANEL IZQUIERDA: MATRIZ INDUSTRIAL (60%)
                Surface(
                    modifier = Modifier.weight(1.5f).fillMaxHeight(),
                    color = colores.cardBase,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, colores.cardBorde.copy(alpha = 0.5f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colores.textoPrincipal.copy(alpha = 0.03f))
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("DESCRIPCIÓN DEL PRODUCTO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, modifier = Modifier.weight(2f))
                            Text("HOY", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
                            Text("LOTE", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, modifier = Modifier.weight(1.1f))
                            Text("VENCE", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
                            Text("COSTO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                            Text("REG", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.Center, modifier = Modifier.weight(0.5f))
                            Text("SUBTOTAL", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(thickness = 1.dp, color = colores.cardBorde.copy(alpha = 0.4f))
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(estado.items, key = { _, item -> item.productoId }) { index, item ->
                                val subtotal = (item.cantidadRecibir.toDoubleOrNull() ?: 0.0) * (item.costoUnitario.replace(',', '.').toDoubleOrNull() ?: 0.0)
                                val enfocado = indexFilaEnFoco == index
                                val coincidencias = item.coincidenciasLote(indiceLotes)
                                
                                // Auditoría de Verdad en tiempo real (R13)
                                val vtoNorm = com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper.normalizar(item.vencimiento)
                                val diasVto = vtoNorm?.let { com.app.administradorfarmadon.inventario.compartido.logica.FechaVencimientoHelper.diasHastaVencer(it) }
                                val vtoInvalido = item.vencimiento.isNotBlank() && (diasVto == null || diasVto <= 0)

                                var mostrarSelectorVencimiento by remember { mutableStateOf(false) }

                                if (mostrarSelectorVencimiento) {
                                    SelectorMesAnio(
                                        valorActual = item.vencimiento,
                                        onFechaSeleccionada = { 
                                            item.vencimiento = it
                                            mostrarSelectorVencimiento = false
                                        },
                                        onDismiss = { mostrarSelectorVencimiento = false }
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { if (enfocado) yFilaSeleccionada = it.positionInRoot().y - rootY + (it.size.height / 2f) }
                                        .clickable { indexFilaEnFoco = index },
                                    color = if (enfocado) colores.textoPrincipal.copy(alpha = 0.04f) else Color.Transparent
                                ) {
                                    Column {
                                        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(2f).padding(end = 8.dp)) {
                                                Text(item.productoNombre, style = TokensFarmadon.tipografia.titulo3.copy(fontSize = 13.5.sp, fontWeight = if(enfocado) FontWeight.Black else FontWeight.Bold), color = if (item.esCompleta) colores.textoTerciario else colores.textoPrincipal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${item.presentacion.ifBlank { "Und" }} · pedido ${item.cantidadPedida} · antes ${item.cantidadPrevia} · falta ${item.saldoPendiente}", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 9.5.sp), color = if (item.esCompleta) colores.estadoExito else colores.textoTerciario, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            CeldaIndustrialInput(item.loteNumero, { item.loteNumero = it.uppercase() }, 1.1f, pista = "LOTE", enfocado = enfocado, alerta = item.loteNumero.isBlank() && (item.cantidadRecibir.toIntOrNull() ?: 0) > 0)
                                            
                                            // VENCE: Selector con validación visual inmediata (R3)
                                            CeldaSelectorFecha(
                                                valor = item.vencimiento,
                                                onClick = { mostrarSelectorVencimiento = true },
                                                peso = 0.9f,
                                                pista = "MM/AA",
                                                enfocado = enfocado,
                                                alerta = vtoInvalido || (item.vencimiento.isBlank() && (item.cantidadRecibir.toIntOrNull() ?: 0) > 0)
                                            )

                                            CeldaIndustrialInput(item.costoUnitario, { item.costoUnitario = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, 0.9f, TextAlign.End, KeyboardType.Decimal, enfocado = enfocado)
                                            CeldaIndustrialInput(item.bonificacionGratis, { item.bonificacionGratis = it.filter { c -> c.isDigit() } }, 0.5f, TextAlign.Center, KeyboardType.Number, "0", enfocado = enfocado)
                                            Text(String.format(Locale.US, "%.2f", subtotal), style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = colores.textoPrincipal, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                                        }
                                        // Inteligencia de lote en vivo: protege contra duplicados y mezclas.
                                        when {
                                            coincidencias.isNotEmpty() -> {
                                                val primera = coincidencias.first()
                                                Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Info, null, tint = colores.estadoAlerta, modifier = Modifier.size(12.dp))
                                                    Text(
                                                        "Lote ${primera.numero} YA EXISTE · quedan ${primera.cantidad.toInt()} · vence ${primera.vencimiento.ifBlank { "?" }}" + if (coincidencias.size > 1) " (+${coincidencias.size - 1} prod.)" else "",
                                                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                                        color = colores.estadoAlerta
                                                    )
                                                }
                                            }
                                        }
                                        // Aviso de sobreentrega: llegó más de lo pedido (legítimo, pero nunca silencioso).
                                        if (item.esSobrante && !item.esCompleta) {
                                            val exceso = (item.cantidadPrevia + (item.cantidadRecibir.toIntOrNull() ?: 0)) - item.cantidadPedida
                                            Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Warning, null, tint = colores.estadoAlerta, modifier = Modifier.size(12.dp))
                                                Text(
                                                    "Excede lo pedido en $exceso und. (entrega mayor a la orden)",
                                                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                                    color = colores.estadoAlerta
                                                )
                                            }
                                        }
                                        if (index < estado.items.lastIndex) HorizontalDivider(thickness = 0.5.dp, color = colores.cardBorde.copy(alpha = 0.2f))
                                    }
                                }
                            }
                        }
                    }
                }

                VinculoElectricoLocal(yInicio = yAnimada, yFin = yTotalLiquidacion, colorEnergia = colores.textoPrincipal)

                // PANEL DERECHA: LIQUIDACIÓN (40% - CON SCROLL TOTAL)
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    color = FDColors.Surface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f))
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp), 
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("LIQUIDACIÓN EJECUTIVA", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp), color = FDColors.TextTertiary)
                            Surface(color = FDColors.Background, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, FDColors.Border), modifier = Modifier.fillMaxWidth().onGloballyPositioned { yTotalLiquidacion = it.positionInRoot().y - rootY + 40f }) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    FilaResumenIndustrial("Items Inspeccionados", "${estado.items.count { (it.cantidadRecibir.toIntOrNull() ?: 0) > 0 }}")
                                    FilaResumenIndustrial("Total Unidades", "${estado.unidadesCompradas + estado.unidadesRegalo}")
                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = 0.5.dp)
                                    FilaResumenIndustrial("Recibido antes", estado.unidadesRecibidasAntes.toString())
                                    FilaResumenIndustrial("Esta entrega", estado.unidadesEstaEntrega.toString())
                                    FilaResumenIndustrial("Faltará después", estado.unidadesPendientesDespues.toString())
                                    FilaResumenIndustrial("Costo de mercadería hoy", "$simboloMoneda " + String.format(Locale.US, "%,.2f", estado.totalCostoCalculado))
                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = 0.5.dp)
                                    Text("TOTAL DEL DOCUMENTO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                                    Text("$simboloMoneda " + String.format(Locale.US, "%,.2f", estado.totalFacturaFinal), style = FDType.NumericLg.copy(fontSize = 32.sp, fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
                                    FilaResumenIndustrial("Pagado antes", "$simboloMoneda " + String.format(Locale.US, "%,.2f", estado.montoPagadoAntes))
                                    Text("Pagado ahora: $simboloMoneda " + String.format(Locale.US, "%,.2f", estado.montoPagadoFinal.coerceAtLeast(0.0)), style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold), color = FDColors.TextSecondary)
                                    FilaResumenIndustrial("Saldo después", "$simboloMoneda " + String.format(Locale.US, "%,.2f", (estado.totalFacturaFinal - estado.montoPagadoAntes - estado.montoPagadoFinal.coerceAtLeast(0.0) - estado.saldoAFavorAplicado).coerceAtLeast(0.0)))
                                    Surface(
                                        color = if (estado.unidadesPendientesDespues > 0) FDColors.WarningSubtle else FDColors.SuccessSubtle,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (estado.unidadesPendientesDespues > 0) "El proveedor aún debe ${estado.unidadesPendientesDespues} unidad(es)" else "El pedido quedará completo",
                                            style = FDType.BodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                            color = if (estado.unidadesPendientesDespues > 0) FDColors.Warning else FDColors.Success,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        SaldoAFavorBanner(
                            liquidacion = estado.liquidacionSaldoAFavor,
                            usar = estado.usarSaldoAFavor,
                            onUsarChange = { estado.onUsarSaldoAFavorChanged(it) },
                            simboloMoneda = simboloMoneda
                        )

                        OutlinedTextField(value = estado.numeroFactura, onValueChange = { estado.onFacturaChanged(it) }, readOnly = estado.facturaContinua, label = { Text("N° FACTURA PROVEEDOR", fontSize = 10.sp) }, supportingText = if (estado.facturaContinua) ({ Text("Continuando la misma factura", fontSize = 10.sp) }) else null, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FDColors.TextPrimary, unfocusedBorderColor = FDColors.Border, focusedContainerColor = FDColors.Background, unfocusedContainerColor = FDColors.Background))

                        OutlinedTextField(
                            value = estado.montoFacturaManual,
                            onValueChange = { estado.onMontoFacturaChanged(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
                            readOnly = estado.facturaContinua,
                            label = { Text("TOTAL DE LA FACTURA", fontSize = 10.sp) },
                            supportingText = if (estado.facturaContinua) ({ Text("Continuando la misma factura", fontSize = 10.sp) }) else null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FDColors.TextPrimary, unfocusedBorderColor = FDColors.Border, focusedContainerColor = FDColors.Background, unfocusedContainerColor = FDColors.Background)
                        )

                        OutlinedTextField(
                            value = estado.montoPagadoManual,
                            onValueChange = { estado.onMontoPagadoChanged(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
                            label = { Text("PAGO REGISTRADO AHORA", fontSize = 10.sp) },
                            supportingText = { Text("Si no pagaste todavía, deja 0.00", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FDColors.TextPrimary, unfocusedBorderColor = FDColors.Border, focusedContainerColor = FDColors.Background, unfocusedContainerColor = FDColors.Background)
                        )

                        if (estado.montoPagadoFinal > 0.0) {
                            PagosMixtosEditor(
                                estado = estado.editorPagos
                            )
                        }

                        Row(Modifier.fillMaxWidth().height(48.dp).background(FDColors.Background, RoundedCornerShape(12.dp)).border(1.dp, FDColors.Border, RoundedCornerShape(12.dp)).padding(4.dp)) {
                            listOf("Contado", "Crédito").forEach { cond ->
                                val sel = estado.condicionPago == cond
                                Surface(modifier = Modifier.weight(1f).fillMaxHeight().clickable(enabled = !estado.facturaContinua) { estado.onCondicionPagoChanged(cond) }, color = if (sel) FDColors.SurfaceElevated else Color.Transparent, shape = RoundedCornerShape(9.dp), border = if (sel) BorderStroke(1.dp, FDColors.Border) else null) { Box(contentAlignment = Alignment.Center) { Text(cond.uppercase(), style = FDType.Label.copy(fontSize = 10.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.Medium), color = if (sel) FDColors.TextPrimary else FDColors.TextTertiary) } }
                            }
                        }

                        if (estado.condicionPago == "Crédito") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("VENCIMIENTO DEL PAGO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                                
                                var mostrarDatePicker by remember { mutableStateOf(false) }
                                
                                if (mostrarDatePicker) {
                                    val datePickerState = rememberDatePickerState()
                                    DatePickerDialog(
                                        onDismissRequest = { mostrarDatePicker = false },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                datePickerState.selectedDateMillis?.let { ms ->
                                                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                                    estado.onFechaVencimientoPagoManualChanged(sdf.format(Date(ms)))
                                                }
                                                mostrarDatePicker = false
                                            }) { Text("SELECCIONAR") }
                                        }
                                    ) {
                                        DatePicker(state = datePickerState)
                                    }
                                }

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(15, 30, 45, 60).forEach { dias ->
                                        val selDias = estado.diasCredito == dias
                                        Surface(modifier = Modifier.weight(1f).clickable { estado.onDiasCreditoChanged(dias) }, color = if (selDias) FDColors.SurfaceElevated else FDColors.Background, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, if (selDias) FDColors.TextPrimary.copy(alpha = 0.5f) else FDColors.Border)) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 9.dp)) {
                                                Text("$dias DÍAS", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = if (selDias) FontWeight.Black else FontWeight.Medium), color = if (selDias) FDColors.TextPrimary else FDColors.TextTertiary)
                                            }
                                        }
                                    }
                                    // Opción de calendario manual
                                    val esManual = estado.fechaVencimientoPagoVisible != null && estado.diasCredito == null
                                    Surface(
                                        modifier = Modifier.weight(0.7f).clickable { mostrarDatePicker = true }, 
                                        color = if (esManual) FDColors.SurfaceElevated else FDColors.Background, 
                                        shape = RoundedCornerShape(9.dp), 
                                        border = BorderStroke(1.dp, if (esManual) FDColors.TextPrimary.copy(alpha = 0.5f) else FDColors.Border)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 9.dp)) {
                                            Icon(Icons.Default.CalendarMonth, null, tint = if (esManual) FDColors.TextPrimary else FDColors.TextTertiary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                
                                if (estado.fechaVencimientoPagoVisible != null) {
                                    Text("Se pagará el: ${estado.fechaVencimientoPagoVisible}", style = FDType.BodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
                                }
                            }
                        }

                        if (estado.lotesRepetidosNuevos.isNotEmpty()) {
                            Text(
                                "Mismo lote nuevo en varias filas: ${estado.lotesRepetidosNuevos.joinToString()}. Revisa antes de asentar.",
                                style = FDType.BodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = FDColors.Error
                            )
                        }

                        estado.errorGeneral?.let { err ->
                            Surface(color = FDColors.ErrorSubtle, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.4f))) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Warning, null, tint = FDColors.Error, modifier = Modifier.size(16.dp))
                                    Text(err, style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold), color = FDColors.Error)
                                }
                            }
                        }

                        if (!estado.puedeAsentar && !procesando && estado.errorGeneral == null) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                estado.razonesBloqueo.forEach { razon ->
                                    Text(" ·  $razon", style = FDType.BodySmall.copy(fontSize = 11.sp), color = FDColors.TextTertiary)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val items = estado.construirItemsAAsentar()
                                if (items != null) {
                                    estado.onErrorMostrado()
                                    onAsentarRecepcion(estado.numeroFactura.trim().uppercase(), estado.condicionPago, if (estado.condicionPago == "Crédito") estado.fechaPagoCredito() ?: "" else "", estado.totalFacturaFinal, estado.montoPagadoFinal, estado.editorPagos.pagos.firstOrNull()?.metodoPago ?: "", estado.editorPagos.pagos, estado.saldoAFavorAplicado, items, estado.decisionFaltante == "AJUSTE")
                                }
                            }, 
                            enabled = estado.puedeAsentar && !procesando, 
                            modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(), 
                            colors = ButtonDefaults.buttonColors(containerColor = FDColors.TextPrimary, contentColor = FDColors.PrimaryText), 
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (procesando) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = FDColors.PrimaryText, strokeWidth = 2.dp)
                            else Text("ASENTAR RECEPCIÓN", style = FDType.Label.copy(color = FDColors.PrimaryText, fontWeight = FontWeight.Black, fontSize = 13.sp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.CeldaSelectorFecha(
    valor: String,
    onClick: () -> Unit,
    peso: Float,
    pista: String,
    enfocado: Boolean = false,
    alerta: Boolean = false
) {
    val colores = TokensFarmadon.colores
    Surface(
        color = if (alerta) colores.peligroSutil else if (enfocado) colores.fondoBase else colores.fondoBase.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (alerta) colores.estadoPeligro else if (enfocado) colores.textoPrincipal.copy(alpha = 0.5f) else colores.cardBorde.copy(alpha = 0.3f)),
        modifier = Modifier
            .weight(peso)
            .padding(horizontal = 4.dp)
            .height(38.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            Text(
                text = valor.ifBlank { pista },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (alerta) colores.estadoPeligro else if (valor.isBlank()) colores.textoTerciario else colores.textoPrincipal,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SelectorMesAnio(
    valorActual: String,
    onFechaSeleccionada: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val anioHoy = Calendar.getInstance().get(Calendar.YEAR)
    
    val partes = valorActual.split("/")
    var mesSeleccionado by remember { mutableIntStateOf(partes.firstOrNull()?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) }
    var anioSeleccionado by remember { mutableIntStateOf(partes.getOrNull(1)?.let { 
        val a = if (it.length == 2) 2000 + it.toInt() else it.toInt()
        a
    } ?: anioHoy) }
    
    // Rango de años flexible: 5 años al pasado y 15 al futuro
    val listaAnios = remember { (anioHoy - 5..anioHoy + 15).toList() }

    FDDialogoContenedor(
        titulo = "SELECCIONAR VENCIMIENTO",
        subtitulo = "Mes y Año de expiración del producto",
        onDismiss = onDismiss,
        iconoCabecera = Icons.Default.CalendarToday,
        anchoMaximo = 520.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                // Columna MESES con diseño Premium
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MES", style = FDType.Label, color = FDColors.TextTertiary)
                    Surface(
                        color = FDColors.Background,
                        shape = FDShapes.Medium,
                        border = BorderStroke(1.dp, FDColors.Border)
                    ) {
                        LazyColumn(modifier = Modifier.height(280.dp).padding(4.dp)) {
                            items(12) { i ->
                                val mes = i + 1
                                val sel = mes == mesSeleccionado
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable { mesSeleccionado = mes },
                                    color = if (sel) FDColors.Primary else Color.Transparent,
                                    shape = FDShapes.Small
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%02d — %s", mes, obtenerNombreMes(mes)),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        style = FDType.Body.copy(fontWeight = if (sel) FontWeight.Black else FontWeight.Medium),
                                        color = if (sel) FDColors.PrimaryText else FDColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Columna AÑOS
                Column(modifier = Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AÑO", style = FDType.Label, color = FDColors.TextTertiary)
                    Surface(
                        color = FDColors.Background,
                        shape = FDShapes.Medium,
                        border = BorderStroke(1.dp, FDColors.Border)
                    ) {
                        LazyColumn(modifier = Modifier.height(280.dp).padding(4.dp)) {
                            items(listaAnios) { anio ->
                                val sel = anio == anioSeleccionado
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable { anioSeleccionado = anio },
                                    color = if (sel) FDColors.Primary else Color.Transparent,
                                    shape = FDShapes.Small
                                ) {
                                    Text(
                                        text = anio.toString(),
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        textAlign = TextAlign.Center,
                                        style = FDType.Body.copy(fontWeight = if (sel) FontWeight.Black else FontWeight.Medium),
                                        color = if (sel) FDColors.PrimaryText else FDColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = FDShapes.Medium,
                    border = BorderStroke(1.dp, FDColors.Border)
                ) {
                    Text("CANCELAR", style = FDType.Label.copy(color = FDColors.TextSecondary))
                }
                Button(
                    onClick = { 
                        onFechaSeleccionada(String.format(Locale.US, "%02d/%d", mesSeleccionado, anioSeleccionado))
                    },
                    modifier = Modifier.weight(1.2f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary),
                    shape = FDShapes.Medium
                ) {
                    Text("ESTABLECER FECHA", style = FDType.Label.copy(color = FDColors.PrimaryText, fontWeight = FontWeight.Black))
                }
            }
        }
    }
}

private fun obtenerNombreMes(mes: Int): String {
    return when(mes) {
        1 -> "Enero"; 2 -> "Febrero"; 3 -> "Marzo"; 4 -> "Abril"; 5 -> "Mayo"; 6 -> "Junio"
        7 -> "Julio"; 8 -> "Agosto"; 9 -> "Septiembre"; 10 -> "Octubre"; 11 -> "Noviembre"; 12 -> "Diciembre"
        else -> ""
    }
}

@Composable
private fun RowScope.CeldaIndustrialInput(valor: String, alCambiar: (String) -> Unit, peso: Float, alineado: TextAlign = TextAlign.Start, teclado: KeyboardType = KeyboardType.Text, pista: String? = null, alerta: Boolean = false, colorTexto: Color? = null, habilitado: Boolean = true, enfocado: Boolean = false) {
    Surface(color = if (alerta) FDColors.ErrorSubtle else if (enfocado) FDColors.Background else FDColors.Background.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, if (alerta) FDColors.Error else if (enfocado) FDColors.TextPrimary.copy(alpha = 0.5f) else FDColors.Border.copy(alpha = 0.3f)), modifier = Modifier.weight(peso).padding(horizontal = 4.dp).height(38.dp)) {
        Box(contentAlignment = when (alineado) { TextAlign.Center -> Alignment.Center; TextAlign.End -> Alignment.CenterEnd; else -> Alignment.CenterStart }, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            if (valor.isEmpty() && pista != null) Text(pista, fontSize = 10.sp, color = FDColors.TextTertiary, maxLines = 1)
            BasicTextField(value = valor, onValueChange = alCambiar, singleLine = true, enabled = habilitado, keyboardOptions = KeyboardOptions(keyboardType = teclado), textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = alineado, color = colorTexto ?: FDColors.TextPrimary, fontFamily = FontFamily.Monospace), modifier = Modifier.fillMaxWidth(), cursorBrush = SolidColor(FDColors.TextPrimary))
        }
    }
}

@Composable
private fun FilaResumenIndustrial(etiqueta: String, valor: String) {
    val colores = TokensFarmadon.colores
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = TokensFarmadon.tipografia.cuerpoPequeno, color = colores.textoSecundario)
        Text(valor, style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black), color = colores.textoPrincipal)
    }
}

@Composable
private fun VinculoElectricoLocal(yInicio: Float, yFin: Float, colorEnergia: Color) {
    if (yInicio <= 0f) return
    val infiniteTransition = rememberInfiniteTransition(label = "Energia")
    val fase by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "Pulso")
    Box(Modifier.width(16.dp).fillMaxHeight()) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val path = Path().apply { moveTo(0f, yInicio - 64.dp.toPx()); cubicTo(w * 0.5f, yInicio - 64.dp.toPx(), w * 0.5f, yFin - 64.dp.toPx(), w, yFin - 64.dp.toPx()) }
            drawPath(path, colorEnergia.copy(alpha = 0.1f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            val pm = PathMeasure(); pm.setPath(path, false); val len = pm.length; val segLen = 30.dp.toPx(); val offset = fase * (len + segLen) - segLen
            val segPath = Path(); pm.getSegment(offset.coerceAtLeast(0f), (offset + segLen).coerceAtMost(len), segPath)
            drawPath(segPath, Brush.horizontalGradient(listOf(colorEnergia.copy(0f), colorEnergia, colorEnergia.copy(0f)), startX = offset.coerceAtLeast(0f), endX = (offset + segLen).coerceAtMost(len)), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
