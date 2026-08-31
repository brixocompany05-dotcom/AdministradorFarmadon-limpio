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
import androidx.compose.foundation.lazy.itemsIndexed
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
        color = colores.fondoBase,
        modifier = Modifier.fillMaxSize().onGloballyPositioned { rootY = it.positionInRoot().y }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            //  TOP BAR EJECUTIVA 
            Row(modifier = Modifier.fillMaxWidth().background(colores.textoPrincipal).padding(horizontal = 28.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("RECEPCIÓN FÍSICA · ${pedido.numeroOrden}", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.2.sp, color = colores.botonPrimarioTexto.copy(alpha = 0.6f)))
                    Text(pedido.proveedorNombre, style = TokensFarmadon.tipografia.titulo2.copy(fontSize = 18.sp), color = colores.botonPrimarioTexto)
                }
                IconButton(onClick = { if (!procesando) onDismiss() }, modifier = Modifier.clip(CircleShape).background(colores.botonPrimarioTexto.copy(alpha = 0.1f))) {
                    Icon(Icons.Default.Close, null, tint = colores.botonPrimarioTexto)
                }
            }

            Row(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // PANEL IZQUIERDA: MATRIZ INDUSTRIAL
                Surface(modifier = Modifier.weight(1.5f).fillMaxHeight(), color = colores.cardBase, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, colores.cardBorde.copy(alpha = 0.5f))) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().background(colores.textoPrincipal.copy(alpha = 0.03f)).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("DESCRIPCIÓN DEL PRODUCTO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, modifier = Modifier.weight(2f))
                            Text("HOY", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f))
                            Text("LOTE", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, modifier = Modifier.weight(1.1f))
                            Text("VENCE", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
                            Text("COSTO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("REG HOY", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
                            Text("SUBTOTAL", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(thickness = 1.dp, color = colores.cardBorde.copy(alpha = 0.4f))
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(estado.items, key = { _, item -> item.productoId }) { index, item ->
                                val subtotal = (item.cantidadRecibir.toDoubleOrNull() ?: 0.0) * (item.costoUnitario.replace(',', '.').toDoubleOrNull() ?: 0.0)
                                val enfocado = indexFilaEnFoco == index
                                val coincidencias = item.coincidenciasLote(indiceLotes)
                                Surface(modifier = Modifier.fillMaxWidth().onGloballyPositioned { if (enfocado) yFilaSeleccionada = it.positionInRoot().y - rootY + (it.size.height / 2f) }.clickable { indexFilaEnFoco = index }, color = if (enfocado) colores.textoPrincipal.copy(alpha = 0.04f) else Color.Transparent) {
                                    Column {
                                        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(2f).padding(end = 8.dp)) {
                                                Text(item.productoNombre, style = TokensFarmadon.tipografia.titulo3.copy(fontSize = 13.5.sp, fontWeight = if(enfocado) FontWeight.Black else FontWeight.Bold), color = if (item.esCompleta) colores.textoTerciario else colores.textoPrincipal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${item.presentacion.ifBlank { "Und" }} · pedido ${item.cantidadPedida} · antes ${item.cantidadPrevia} · falta ${item.saldoPendiente}", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 9.5.sp), color = if (item.esCompleta) colores.estadoExito else colores.textoTerciario, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            CeldaIndustrialInput(item.cantidadRecibir, { item.cantidadRecibir = it.filter { c -> c.isDigit() } }, 0.7f, TextAlign.Center, KeyboardType.Number, enfocado = enfocado)
                                            CeldaIndustrialInput(item.loteNumero, { item.loteNumero = it.uppercase() }, 1.1f, pista = "LOTE", enfocado = enfocado)
                                            CeldaIndustrialInput(item.vencimiento, { item.vencimiento = it }, 0.9f, TextAlign.Center, pista = "MM/AA", enfocado = enfocado)
                                            CeldaIndustrialInput(item.costoUnitario, { item.costoUnitario = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, 1f, TextAlign.End, KeyboardType.Decimal, enfocado = enfocado)
                                            CeldaIndustrialInput(item.bonificacionGratis, { item.bonificacionGratis = it.filter { c -> c.isDigit() } }, 0.6f, TextAlign.Center, KeyboardType.Number, "0", enfocado = enfocado)
                                            Text(String.format(java.util.Locale.US, "%.2f", subtotal), style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = colores.textoPrincipal, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
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

                // PANEL DERECHA: LIQUIDACIÓN
                Surface(modifier = Modifier.weight(0.9f).fillMaxHeight(), color = colores.cardBase, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, colores.cardBorde.copy(alpha = 0.5f))) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("LIQUIDACIÓN", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp), color = colores.textoTerciario)
                            Surface(color = colores.fondoBase, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, colores.cardBorde), modifier = Modifier.fillMaxWidth().onGloballyPositioned { yTotalLiquidacion = it.positionInRoot().y - rootY + 40f }) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FilaResumenIndustrial("Items Inspeccionados", "${estado.items.count { (it.cantidadRecibir.toIntOrNull() ?: 0) > 0 }}")
                                    FilaResumenIndustrial("Total Unidades", "${estado.unidadesCompradas + estado.unidadesRegalo}")
                                    HorizontalDivider(color = colores.cardBorde.copy(alpha = 0.4f), thickness = 0.5.dp)
                                    FilaResumenIndustrial("Recibido antes", estado.unidadesRecibidasAntes.toString())
                                    FilaResumenIndustrial("Esta entrega", estado.unidadesEstaEntrega.toString())
                                    FilaResumenIndustrial("Faltará después", estado.unidadesPendientesDespues.toString())
                                    FilaResumenIndustrial("Costo de mercadería hoy", "$simboloMoneda " + String.format(java.util.Locale.US, "%,.2f", estado.totalCostoCalculado))
                                    HorizontalDivider(color = colores.cardBorde.copy(alpha = 0.4f), thickness = 0.5.dp)
                                    Text("TOTAL DEL DOCUMENTO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = colores.textoTerciario)
                                    Text("$simboloMoneda " + String.format(java.util.Locale.US, "%,.2f", estado.totalFacturaFinal), style = TokensFarmadon.tipografia.titulo1.copy(fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = colores.textoPrincipal)
                                    FilaResumenIndustrial("Pagado antes", "$simboloMoneda " + String.format(java.util.Locale.US, "%,.2f", estado.montoPagadoAntes))
                                    Text("Pagado ahora: $simboloMoneda " + String.format(java.util.Locale.US, "%,.2f", estado.montoPagadoFinal.coerceAtLeast(0.0)), style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = colores.textoSecundario)
                                    FilaResumenIndustrial("Saldo después", "$simboloMoneda " + String.format(java.util.Locale.US, "%,.2f", (estado.totalFacturaFinal - estado.montoPagadoAntes - estado.montoPagadoFinal.coerceAtLeast(0.0) - estado.saldoAFavorAplicado).coerceAtLeast(0.0)))
                                    Text(
                                        text = if (estado.unidadesPendientesDespues > 0) "El proveedor aún debe ${estado.unidadesPendientesDespues} unidad(es)" else "El pedido quedará completo",
                                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                        color = if (estado.unidadesPendientesDespues > 0) colores.estadoAlerta else colores.estadoExito
                                    )
                                }
                            }
                        }
                        SaldoAFavorBanner(
                            liquidacion = estado.liquidacionSaldoAFavor,
                            usar = estado.usarSaldoAFavor,
                            onUsarChange = { estado.onUsarSaldoAFavorChanged(it) },
                            simboloMoneda = simboloMoneda
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = estado.numeroFactura, onValueChange = { estado.onFacturaChanged(it) }, readOnly = estado.facturaContinua, label = { Text("N° FACTURA PROVEEDOR", fontSize = 10.sp) }, supportingText = if (estado.facturaContinua) ({ Text("Continuando la misma factura", fontSize = 10.sp) }) else null, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colores.textoPrincipal, unfocusedBorderColor = colores.cardBorde, focusedContainerColor = colores.fondoBase, unfocusedContainerColor = colores.fondoBase))
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
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colores.textoPrincipal, unfocusedBorderColor = colores.cardBorde, focusedContainerColor = colores.fondoBase, unfocusedContainerColor = colores.fondoBase)
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
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colores.textoPrincipal, unfocusedBorderColor = colores.cardBorde, focusedContainerColor = colores.fondoBase, unfocusedContainerColor = colores.fondoBase)
                            )
                            if (estado.montoPagadoFinal > 0.0) {
                                PagosMixtosEditor(
                                    estado = estado.editorPagos
                                )
                            }
                            Row(Modifier.fillMaxWidth().height(48.dp).background(colores.fondoBase, RoundedCornerShape(12.dp)).border(1.dp, colores.cardBorde, RoundedCornerShape(12.dp)).padding(4.dp)) {
                                listOf("Contado", "Crédito").forEach { cond ->
                                    val sel = estado.condicionPago == cond
                                    Surface(modifier = Modifier.weight(1f).fillMaxHeight().clickable(enabled = !estado.facturaContinua) { estado.onCondicionPagoChanged(cond) }, color = if (sel) colores.cardElevada else Color.Transparent, shape = RoundedCornerShape(9.dp), border = if (sel) BorderStroke(1.dp, colores.cardBorde) else null) { Box(contentAlignment = Alignment.Center) { Text(cond.uppercase(), style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 10.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.Medium), color = if (sel) colores.textoPrincipal else colores.textoTerciario) } }
                                }
                            }
                            if (estado.condicionPago == "Crédito" && estado.fechaVencimientoPagoVisible.isNullOrBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("DÍAS DE CRÉDITO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = colores.textoTerciario)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(15, 30, 45, 60).forEach { dias ->
                                            val selDias = estado.diasCredito == dias
                                            Surface(modifier = Modifier.weight(1f).clickable { estado.onDiasCreditoChanged(dias) }, color = if (selDias) colores.cardElevada else colores.fondoBase, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, if (selDias) colores.textoPrincipal.copy(alpha = 0.5f) else colores.cardBorde)) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 9.dp)) {
                                                    Text("$dias DÍAS", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 10.sp, fontWeight = if (selDias) FontWeight.Black else FontWeight.Medium), color = if (selDias) colores.textoPrincipal else colores.textoTerciario)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (estado.condicionPago == "Crédito") {
                                Text("Vencimiento guardado: ${estado.fechaVencimientoPagoVisible}", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = colores.textoSecundario)
                            }
                            if (estado.lotesRepetidosNuevos.isNotEmpty()) {
                                Text(
                                    "Mismo lote nuevo en varias filas: ${estado.lotesRepetidosNuevos.joinToString()}. Revisa antes de asentar.",
                                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                    color = colores.estadoPeligro
                                )
                            }
                            estado.errorGeneral?.let { err ->
                                Surface(color = colores.peligroSutil, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, colores.estadoPeligro.copy(alpha = 0.4f))) {
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Warning, null, tint = colores.estadoPeligro, modifier = Modifier.size(16.dp))
                                        Text(err, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold), color = colores.estadoPeligro)
                                    }
                                }
                            }
                            if (!estado.puedeAsentar && !procesando && estado.errorGeneral == null) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    estado.razonesBloqueo.forEach { razon ->
                                        Text(" ·  $razon", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 11.sp), color = colores.textoTerciario)
                                    }
                                }
                            }
                        }
                        Button(onClick = {
                            val items = estado.construirItemsAAsentar()
                            if (items != null) {
                                estado.onErrorMostrado()
                                onAsentarRecepcion(estado.numeroFactura.trim().uppercase(), estado.condicionPago, if (estado.condicionPago == "Crédito") estado.fechaPagoCredito() ?: "" else "", estado.totalFacturaFinal, estado.montoPagadoFinal, estado.editorPagos.pagos.firstOrNull()?.metodoPago ?: "", estado.editorPagos.pagos, estado.saldoAFavorAplicado, items, estado.decisionFaltante == "AJUSTE")
                            }
                        }, enabled = estado.puedeAsentar && !procesando, modifier = Modifier.fillMaxWidth().height(54.dp).bounceClick(), colors = ButtonDefaults.buttonColors(containerColor = colores.textoPrincipal, contentColor = colores.botonPrimarioTexto), shape = RoundedCornerShape(14.dp)) {
                            if (procesando) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colores.botonPrimarioTexto, strokeWidth = 2.dp)
                            else Text("ASENTAR RECEPCIÓN", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = 12.sp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.CeldaIndustrialInput(valor: String, alCambiar: (String) -> Unit, peso: Float, alineado: TextAlign = TextAlign.Start, teclado: KeyboardType = KeyboardType.Text, pista: String? = null, alerta: Boolean = false, colorTexto: Color? = null, habilitado: Boolean = true, enfocado: Boolean = false) {
    val colores = TokensFarmadon.colores
    Surface(color = if (alerta) colores.peligroSutil else if (enfocado) colores.fondoBase else colores.fondoBase.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, if (alerta) colores.estadoPeligro else if (enfocado) colores.textoPrincipal.copy(alpha = 0.5f) else colores.cardBorde.copy(alpha = 0.3f)), modifier = Modifier.weight(peso).padding(horizontal = 4.dp).height(38.dp)) {
        Box(contentAlignment = when (alineado) { TextAlign.Center -> Alignment.Center; TextAlign.End -> Alignment.CenterEnd; else -> Alignment.CenterStart }, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            if (valor.isEmpty() && pista != null) Text(pista, fontSize = 10.sp, color = colores.textoTerciario, maxLines = 1)
            BasicTextField(value = valor, onValueChange = alCambiar, singleLine = true, enabled = habilitado, keyboardOptions = KeyboardOptions(keyboardType = teclado), textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = alineado, color = colorTexto ?: colores.textoPrincipal, fontFamily = FontFamily.Monospace), modifier = Modifier.fillMaxWidth())
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
