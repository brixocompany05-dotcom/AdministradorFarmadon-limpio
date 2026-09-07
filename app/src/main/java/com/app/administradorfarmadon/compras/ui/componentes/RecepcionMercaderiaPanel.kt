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
    facturasExistentes: List<FacturaCompra> = emptyList(),
    onDismiss: () -> Unit,
    onAsentarRecepcion: (
        numeroFactura: String,
        condicionPago: String,
        fechaVencimientoPago: String,
        fechaEmisionPapel: String,
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
    // El formulario NO se recrea por cambios en vivo del saldo: si eso pasara,
    // se borraría a medias lo que la persona ya escribió (lotes, vencimientos).
    // La verdad del saldo se re-verifica dentro de la transacción al guardar —
    // si otro lo usó mientras tanto, la operación aborta con la cifra real.
    val estado = remember(pedido.id, facturaExistente?.id) {
        RecepcionMercaderiaEstado(pedido, indiceLotes, facturaExistente, saldoAFavorDisponible, metodosPago, facturasExistentes)
    }

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
        modifier = Modifier.fillMaxSize().imePadding().onGloballyPositioned { rootY = it.positionInRoot().y }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            //    TOP BAR EJECUTIVA (ADAPTATIVA AL TEMA)   
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FDColors.Surface)
                    .padding(horizontal = 28.dp, vertical = 18.dp),
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // PANEL IZQUIERDA: MATRIZ INDUSTRIAL (60%)
                Surface(
                    modifier = Modifier.weight(1.5f).fillMaxHeight(),
                    color = colores.cardBase,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, colores.cardBorde.copy(alpha = 0.5f))
                ) {
                    Column {
                        HorizontalDivider(thickness = 0.8.dp, color = colores.cardBorde.copy(alpha = 0.3f))

                        // Encabezados de Columnas alineados 1 a 1 con las celdas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colores.textoPrincipal.copy(alpha = 0.03f))
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("DESCRIPCIÓN", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, modifier = Modifier.weight(2.0f))
                            Text("RECIBIR", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                            Text("LOTE", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, modifier = Modifier.weight(1.1f))
                            Text("VENCE", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
                            Text("COSTO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                            Text("BONIF.", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
                            Text("SUBTOTAL", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario, textAlign = TextAlign.End, modifier = Modifier.weight(1.0f))
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
                                        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(2.0f).padding(end = 6.dp)) {
                                                Text(item.productoNombre, style = TokensFarmadon.tipografia.titulo3.copy(fontSize = 13.sp, fontWeight = if(enfocado) FontWeight.Black else FontWeight.Bold), color = if (item.esCompleta) colores.textoTerciario else colores.textoPrincipal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${item.presentacion.ifBlank { "Und" }} · ped. ${item.cantidadPedida} · recib. ${item.cantidadPrevia} · falta ${item.saldoPendiente}", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = 9.sp), color = if (item.esCompleta) colores.estadoExito else colores.textoTerciario, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            CeldaIndustrialInput(item.cantidadRecibir, { item.cantidadRecibir = it.filter { c -> c.isDigit() } }, 0.8f, TextAlign.Center, KeyboardType.Number, pista = "0", enfocado = enfocado)
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
                                            CeldaIndustrialInput(item.bonificacionGratis, { item.bonificacionGratis = it.filter { c -> c.isDigit() } }, 0.6f, TextAlign.Center, KeyboardType.Number, "0", enfocado = enfocado)
                                            Text(String.format(Locale.US, "%.2f", subtotal), style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = colores.textoPrincipal, textAlign = TextAlign.End, modifier = Modifier.weight(1.0f))
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
                            Surface(
                                color = FDColors.Background,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, FDColors.Border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { yTotalLiquidacion = it.positionInRoot().y - rootY + 40f }
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Resumen físico compacto
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "MERCADERÍA EN ESTA ENTREGA",
                                            style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                                            color = FDColors.TextTertiary
                                        )
                                        Surface(
                                            color = if (estado.unidadesPendientesDespues > 0) FDColors.WarningSubtle else FDColors.SuccessSubtle,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (estado.unidadesPendientesDespues > 0) {
                                                    "Parcial (falta ${estado.unidadesPendientesDespues})"
                                                } else "Entrega Completa",
                                                style = FDType.BodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = if (estado.unidadesPendientesDespues > 0) FDColors.Warning else FDColors.Success,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    // El faltante NO se decide aquí con un interruptor global: cada producto
                                    // tiene su propio destino (uno ya no viene, otro llega mañana). Lo que no
                                    // vendrá se descarta por producto en la orden, después de asentar.
                                    if (estado.unidadesPendientesDespues > 0) {
                                        Text(
                                            "El saldo queda pendiente para la próxima entrega.",
                                            style = FDType.Caption.copy(fontSize = 9.5.sp),
                                            color = FDColors.TextTertiary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            color = FDColors.SurfaceElevated,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(0.5.dp, FDColors.Border)
                                        ) {
                                            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                Text("Items", style = FDType.Label.copy(fontSize = 9.sp), color = FDColors.TextTertiary)
                                                Text(
                                                    "${estado.items.count { (it.cantidadRecibir.toIntOrNull() ?: 0) > 0 }} de ${estado.items.size}",
                                                    style = FDType.BodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Black),
                                                    color = FDColors.TextPrimary
                                                )
                                            }
                                        }
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            color = FDColors.SurfaceElevated,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(0.5.dp, FDColors.Border)
                                        ) {
                                            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                Text("Unidades hoy", style = FDType.Label.copy(fontSize = 9.sp), color = FDColors.TextTertiary)
                                                Text(
                                                    "${estado.unidadesEstaEntrega} und",
                                                    style = FDType.BodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Black),
                                                    color = FDColors.TextPrimary
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = 0.5.dp)

                                    // Resumen financiero: UNA sola verdad. El número grande es siempre
                                    // el total de la factura: suma de filas, o del papel si se escribió.
                                    val totalManualHoy = estado.montoFacturaManual.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
                                    Text(
                                        if (totalManualHoy != null) "TOTAL DEL PAPEL" else "TOTAL DE ESTA RECEPCIÓN",
                                        style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                                        color = FDColors.TextTertiary
                                    )
                                    Text(
                                        "$simboloMoneda " + String.format(Locale.US, "%.2f", estado.totalFacturaFinal),
                                        style = FDType.NumericLg.copy(fontSize = 28.sp, fontWeight = FontWeight.Black),
                                        color = FDColors.TextPrimary
                                    )
                                    if (totalManualHoy != null) {
                                        Text(
                                            "Filas suman $simboloMoneda " + String.format(Locale.US, "%.2f", estado.totalCostoCalculado),
                                            style = FDType.Caption.copy(fontSize = 10.sp),
                                            color = FDColors.TextTertiary
                                        )
                                    }
                                    // Papel que continúa: su total ya quedó fijo, no se duplica.
                                    // Lo grande de arriba es SOLO lo que llega hoy.
                                    if (estado.facturaContinua && estado.montoPapelFijo != null) {
                                        Text(
                                            "Papel ${estado.numeroFactura.ifBlank { "S/C" }} · total S/ " +
                                                String.format(Locale.US, "%.2f", estado.montoPapelFijo) + " (fijo)",
                                            style = FDType.Caption.copy(fontSize = 10.sp),
                                            color = FDColors.TextTertiary
                                        )
                                    }
                                    // Papel nuevo con flete/descuento: si el total del papel difiere
                                    // de las filas, se escribe aquí. Vacío = suma de filas.
                                    if (!estado.facturaContinua) {
                                        OutlinedTextField(
                                            value = estado.montoFacturaManual,
                                            onValueChange = { estado.onMontoFacturaChanged(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
                                            label = { Text("TOTAL DEL PAPEL (si difiere)", fontSize = 10.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = FDColors.TextPrimary,
                                                unfocusedBorderColor = FDColors.Border,
                                                focusedContainerColor = FDColors.Background,
                                                unfocusedContainerColor = FDColors.Background
                                            )
                                        )
                                    }

                                    if (estado.saldoAFavorAplicado > 0.0) {
                                        FilaResumenIndustrial(
                                            "Saldo a favor aplicado",
                                            "- $simboloMoneda " + String.format(Locale.US, "%.2f", estado.saldoAFavorAplicado)
                                        )
                                    }

                                    if (estado.montoPagadoAntes > 0.0) {
                                        FilaResumenIndustrial(
                                            "Abonado anteriormente",
                                            "$simboloMoneda " + String.format(Locale.US, "%.2f", estado.montoPagadoAntes)
                                        )
                                    }

                                    val netoAPagar = estado.liquidacionSaldoAFavor.netoAPagar
                                    if (estado.condicionPago == "Contado") {
                                        val pagadoAhora = estado.montoPagadoFinal.coerceAtLeast(0.0)
                                        val saldoPendiente = (netoAPagar - pagadoAhora).coerceAtLeast(0.0)
                                        FilaResumenIndustrial(
                                            "Pagado en métodos",
                                            "$simboloMoneda " + String.format(Locale.US, "%.2f", pagadoAhora)
                                        )
                                        if (saldoPendiente > 0.01) {
                                            FilaResumenIndustrial(
                                                "Falta por cubrir",
                                                "$simboloMoneda " + String.format(Locale.US, "%.2f", saldoPendiente)
                                            )
                                        }
                                    } else {
                                        val vencimiento = estado.fechaVencimientoPagoVisible ?: "Por definir"
                                        FilaResumenIndustrial(
                                            "A pagar a crédito",
                                            "$simboloMoneda " + String.format(Locale.US, "%.2f", netoAPagar)
                                        )
                                        FilaResumenIndustrial("Vence", vencimiento)
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

                        LaunchedEffect(estado.totalCostoCalculado, estado.montoFacturaManual, estado.usarSaldoAFavor, estado.condicionPago) {
                            if (estado.condicionPago == "Contado") {
                                estado.sincronizarPagoContado()
                            }
                        }

                        var mostrarDatePickerEmision by remember { mutableStateOf(false) }

                        if (mostrarDatePickerEmision) {
                            val datePickerState = rememberDatePickerState()
                            DatePickerDialog(
                                onDismissRequest = { mostrarDatePickerEmision = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let { ms ->
                                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                            estado.onFechaEmisionPapelChanged(sdf.format(Date(ms)))
                                        }
                                        mostrarDatePickerEmision = false
                                    }) { Text("SELECCIONAR") }
                                }
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }

                        // ── COMPROBANTE DEL PROVEEDOR ──
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = estado.numeroFactura,
                                onValueChange = { estado.onFacturaChanged(it) },
                                readOnly = estado.facturaContinua,
                                isError = estado.esFacturaDuplicada,
                                label = { Text("N° FACTURA PROVEEDOR", fontSize = 10.sp) },
                                supportingText = when {
                                    estado.esFacturaDuplicada -> ({
                                        Text(
                                            "⚠️ Ya registrada en otra orden",
                                            color = FDColors.Error,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    })
                                    estado.facturaContinua -> ({ Text("Misma factura (entrega parcial)", fontSize = 9.sp) })
                                    else -> null
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (estado.esFacturaDuplicada) FDColors.Error else FDColors.TextPrimary,
                                    unfocusedBorderColor = if (estado.esFacturaDuplicada) FDColors.Error else FDColors.Border,
                                    errorBorderColor = FDColors.Error,
                                    focusedContainerColor = FDColors.Background,
                                    unfocusedContainerColor = FDColors.Background
                                )
                            )

                            // Emisión sutil y opcional (si no se toca, se asume hoy automáticamente)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = if (estado.fechaEmisionInvalida) FDColors.Error else FDColors.TextTertiary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (estado.fechaEmisionPapel.isNotBlank()) {
                                            "Fecha comprobante: ${estado.fechaEmisionPapel}"
                                        } else {
                                            "Fecha comprobante: Hoy (${estado.fechaEmisionFinal})"
                                        },
                                        style = FDType.BodySmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (estado.fechaEmisionPapel.isNotBlank()) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (estado.fechaEmisionInvalida) FDColors.Error else if (estado.fechaEmisionPapel.isNotBlank()) FDColors.TextPrimary else FDColors.TextSecondary
                                    )
                                }
                                if (!estado.facturaContinua) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (estado.fechaEmisionPapel.isNotBlank()) {
                                            Text(
                                                text = "Usar hoy",
                                                style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                                color = FDColors.Primary,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable { estado.onFechaEmisionPapelChanged("") }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                            Text("·", color = FDColors.TextTertiary, fontSize = 10.sp)
                                        }
                                        Text(
                                            text = if (estado.fechaEmisionPapel.isNotBlank()) "Modificar" else "Cambiar fecha",
                                            style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                            color = FDColors.Primary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { mostrarDatePickerEmision = true }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (estado.fechaEmisionInvalida) {
                                Text(
                                    "Fecha de emisión inválida o posterior a hoy",
                                    style = FDType.BodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = FDColors.Error,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        // ── SELECTOR DE CONDICIÓN DE PAGO (CONTADO / CRÉDITO) PRIMERO ──
                        Text(
                            "CONDICIÓN DE PAGO",
                            style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp),
                            color = FDColors.TextTertiary
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(FDColors.Background, RoundedCornerShape(12.dp))
                                .border(1.dp, FDColors.Border, RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            listOf("Contado", "Crédito").forEach { cond ->
                                val sel = estado.condicionPago == cond
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(enabled = !estado.facturaContinua) { estado.onCondicionPagoChanged(cond) },
                                    color = if (sel) FDColors.SurfaceElevated else Color.Transparent,
                                    shape = RoundedCornerShape(9.dp),
                                    border = if (sel) BorderStroke(1.dp, FDColors.Border) else null
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            cond.uppercase(),
                                            style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.Medium),
                                            color = if (sel) FDColors.TextPrimary else FDColors.TextTertiary
                                        )
                                    }
                                }
                            }
                        }

                        if (estado.condicionPago == "Crédito") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("PLAZO / VENCIMIENTO DEL PAGO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)

                                var mostrarDatePicker by remember { mutableStateOf(false) }

                                if (mostrarDatePicker) {
                                    val datePickerState = rememberDatePickerState()
                                    DatePickerDialog(
                                        onDismissRequest = { mostrarDatePicker = false },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                datePickerState.selectedDateMillis?.let { ms ->
                                                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
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
                                        Surface(
                                            modifier = Modifier.weight(1f).clickable { estado.onDiasCreditoChanged(dias) },
                                            color = if (selDias) FDColors.SurfaceElevated else FDColors.Background,
                                            shape = RoundedCornerShape(9.dp),
                                            border = BorderStroke(1.dp, if (selDias) FDColors.TextPrimary.copy(alpha = 0.5f) else FDColors.Border)
                                        ) {
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
                        } else {
                            // CONTADO: Medios de pago (1 método = auto 100%, 2+ métodos = divide montos)
                            if (estado.liquidacionSaldoAFavor.netoAPagar <= 0.0 && estado.totalFacturaFinal > 0.0) {
                                Surface(
                                    color = FDColors.SuccessSubtle,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, FDColors.Success.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Check, null, tint = FDColors.Success, modifier = Modifier.size(18.dp))
                                        Text(
                                            "El saldo a favor cubre el 100% de esta recepción. No se requiere pago adicional.",
                                            style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                                            color = FDColors.Success
                                        )
                                    }
                                }
                            } else {
                                PagosMixtosEditor(
                                    estado = estado.editorPagos
                                )
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
                                    onAsentarRecepcion(
                                        estado.numeroFactura.trim().uppercase(),
                                        estado.condicionPago,
                                        if (estado.condicionPago == "Crédito") estado.fechaPagoCredito() ?: "" else "",
                                        estado.fechaEmisionFinal,
                                        estado.totalFacturaFinal,
                                        if (estado.condicionPago == "Crédito") 0.0 else estado.montoPagadoFinal,
                                        if (estado.condicionPago == "Crédito") "" else (estado.editorPagos.pagos.firstOrNull()?.metodoPago ?: ""),
                                        if (estado.condicionPago == "Crédito") emptyList() else estado.editorPagos.pagos,
                                        estado.saldoAFavorAplicado,
                                        items,
                                        // La recepción asienta SOLO lo que llegó físicamente. El destino del
                                        // faltante (esperar o descartar) se decide por producto en la orden.
                                        false
                                    )
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
    Surface(color = if (alerta) FDColors.ErrorSubtle else if (enfocado) FDColors.Background else FDColors.Background.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, if (alerta) FDColors.Error else if (enfocado) FDColors.TextPrimary.copy(alpha = 0.5f) else FDColors.Border.copy(alpha = 0.3f)), modifier = Modifier.weight(peso).padding(horizontal = 3.dp).height(40.dp)) {
        Box(contentAlignment = when (alineado) { TextAlign.Center -> Alignment.Center; TextAlign.End -> Alignment.CenterEnd; else -> Alignment.CenterStart }, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            if (valor.isEmpty() && pista != null) Text(pista, fontSize = 10.5.sp, color = FDColors.TextTertiary, maxLines = 1)
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
