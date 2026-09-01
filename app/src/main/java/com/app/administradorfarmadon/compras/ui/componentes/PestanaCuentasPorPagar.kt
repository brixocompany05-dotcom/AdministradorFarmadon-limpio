package com.app.administradorfarmadon.compras.ui.componentes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compras.pagos.logica.EtiquetaMetodoPago
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.compartido.modelo.AbonoFactura
import com.app.administradorfarmadon.inventario.compartido.modelo.FacturaCompra
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonSecundario
import java.text.SimpleDateFormat
import java.util.*

enum class PeriodoContableFactura(val label: String) {
    ESTE_MES("Este Mes"),
    MES_ANTERIOR("Mes Pasado"),
    ULTIMOS_3_MESES("Últimos 3 Meses"),
    TODO_HISTORIAL("Todo el Historial")
}

private fun parsearFechaFlexible(fechaStr: String): Date? {
    if (fechaStr.isBlank()) return null
    val formatos = listOf(
        "dd/MM/yyyy",
        "dd/MM/yyyy HH:mm",
        "dd/MM/yyyy HH:mm:ss",
        "yyyy-MM-dd",
        "yyyy-MM-dd HH:mm:ss",
        "dd-MM-yyyy"
    )
    val limpia = fechaStr.trim()
    for (formato in formatos) {
        try {
            val sdf = SimpleDateFormat(formato, Locale.getDefault())
            sdf.isLenient = false
            val d = sdf.parse(limpia)
            if (d != null) return d
        } catch (_: Exception) {}
    }
    return null
}

private fun perteneceAPeriodo(factura: FacturaCompra, periodo: PeriodoContableFactura): Boolean {
    if (periodo == PeriodoContableFactura.TODO_HISTORIAL) return true
    // Fecha ilegible = verdad desconocida: jamás se infla un período contable contándola
    // en todos (esto mintía en "Este Mes", "Mes Pasado" y "3 Meses" a la vez). La factura
    // sigue visible completa en TODO_HISTORIAL.
    val fechaFact = parsearFechaFlexible(factura.fechaRegistro) ?: return false

    val calFact = Calendar.getInstance().apply { time = fechaFact }
    val hoy = Calendar.getInstance()

    return when (periodo) {
        PeriodoContableFactura.ESTE_MES -> {
            calFact.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) &&
                    calFact.get(Calendar.MONTH) == hoy.get(Calendar.MONTH)
        }
        PeriodoContableFactura.MES_ANTERIOR -> {
            val mesPasado = (hoy.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            calFact.get(Calendar.YEAR) == mesPasado.get(Calendar.YEAR) &&
                    calFact.get(Calendar.MONTH) == mesPasado.get(Calendar.MONTH)
        }
        PeriodoContableFactura.ULTIMOS_3_MESES -> {
            val hace90Dias = (hoy.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -90) }
            !calFact.before(hace90Dias)
        }
        PeriodoContableFactura.TODO_HISTORIAL -> true
    }
}

private data class InfoVencimientoHumano(
    val esContado: Boolean,
    val textoTarjeta: String,
    val textoDetalle: String,
    val esVencido: Boolean,
    val esAlertaPronta: Boolean
)

private fun calcularVencimientoHumano(factura: FacturaCompra): InfoVencimientoHumano {
    if (factura.esAnulada) {
        return InfoVencimientoHumano(
            esContado = false,
            textoTarjeta = "Anulada" + (if (factura.motivoAnulacion.isNotBlank()) " · ${factura.motivoAnulacion}" else ""),
            textoDetalle = "Factura anulada · No se debe nada",
            esVencido = false,
            esAlertaPronta = false
        )
    }
    if (factura.esContado) {
        val fechaEmision = factura.fechaRegistro.ifBlank { "Reciente" }
        if (factura.esTotalmentePagada) {
            return InfoVencimientoHumano(
                esContado = true,
                textoTarjeta = "Emitida: $fechaEmision · Contado",
                textoDetalle = "Liquidado al recibir mercadería · Cero deuda pendiente",
                esVencido = false,
                esAlertaPronta = false
            )
        }
        val simbolo = SessionManager.monedaSimbolo.ifBlank { "S/" }
        val saldo = factura.saldoPendienteReal
        return InfoVencimientoHumano(
            esContado = true,
            textoTarjeta = "Contado · Falta pagar $simbolo " + String.format(Locale.US, "%.2f", saldo),
            textoDetalle = "Factura al contado con saldo pendiente: $simbolo " + String.format(Locale.US, "%.2f", saldo),
            esVencido = false,
            esAlertaPronta = false
        )
    }

    val fechaVenc = parsearFechaFlexible(factura.fechaVencimientoPago)

    if (fechaVenc == null) {
        return InfoVencimientoHumano(
            esContado = false,
            textoTarjeta = "Vence: ${factura.fechaVencimientoPago.ifBlank { "Sin fecha" }}",
            textoDetalle = "Fecha límite de pago: ${factura.fechaVencimientoPago.ifBlank { "Sin fecha fijada" }}",
            esVencido = false,
            esAlertaPronta = false
        )
    }

    val calHoy = Calendar.getInstance().apply {
        timeInMillis = com.app.administradorfarmadon.compartido.logica.HoraServidor.ahoraMs()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val calVenc = Calendar.getInstance().apply {
        time = fechaVenc
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val diffMillis = calVenc.timeInMillis - calHoy.timeInMillis
    val diffDias = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

    return when {
        diffDias < 0 -> {
            val diasPasados = -diffDias
            val txtCard = if (diasPasados == 1) "Venció ayer (${factura.fechaVencimientoPago})" else "Venció hace $diasPasados días (${factura.fechaVencimientoPago})"
            InfoVencimientoHumano(
                esContado = false,
                textoTarjeta = txtCard,
                textoDetalle = "⚠️ Factura vencida hace $diasPasados días (${factura.fechaVencimientoPago})",
                esVencido = true,
                esAlertaPronta = false
            )
        }
        diffDias == 0 -> InfoVencimientoHumano(
            esContado = false,
            textoTarjeta = "Vence hoy (${factura.fechaVencimientoPago})",
            textoDetalle = "⚠️ Vence hoy (${factura.fechaVencimientoPago})",
            esVencido = false,
            esAlertaPronta = true
        )
        diffDias == 1 -> InfoVencimientoHumano(
            esContado = false,
            textoTarjeta = "Vence mañana (${factura.fechaVencimientoPago})",
            textoDetalle = "⏱️ Te falta 1 día para saldar (${factura.fechaVencimientoPago})",
            esVencido = false,
            esAlertaPronta = true
        )
        diffDias in 2..7 -> InfoVencimientoHumano(
            esContado = false,
            textoTarjeta = "Vence en $diffDias días (${factura.fechaVencimientoPago})",
            textoDetalle = "⏱️ Te faltan $diffDias días para saldar (${factura.fechaVencimientoPago})",
            esVencido = false,
            esAlertaPronta = true
        )
        else -> InfoVencimientoHumano(
            esContado = false,
            textoTarjeta = "Vence en $diffDias días (${factura.fechaVencimientoPago})",
            textoDetalle = "Te faltan $diffDias días para saldar (${factura.fechaVencimientoPago})",
            esVencido = false,
            esAlertaPronta = false
        )
    }
}

@Composable
fun PestanaCuentasPorPagar(
    facturas: List<FacturaCompra>,
    facturaSeleccionada: FacturaCompra?,
    filtroEstado: String,
    procesandoPago: Boolean,
    onSeleccionarFactura: (String) -> Unit,
    onCambiarFiltroEstado: (String) -> Unit,
    onAbrirDialogoAbono: (FacturaCompra) -> Unit = {},
    onAbrirDialogoNotaCredito: (FacturaCompra) -> Unit = {},
    onAbrirDialogoProrroga: (FacturaCompra) -> Unit = {},
    onAbrirDialogoAnular: (FacturaCompra) -> Unit = {},
    listaState: LazyListState = LazyListState()
) {
    val s = recordarMedidaAdaptativa()
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }
    // Verdad primero: al entrar se ve TODO el historial (la deuda completa);
    // estrechar el período es decisión del usuario, jamás un escondite por defecto.
    var periodoSeleccionado by remember { mutableStateOf(PeriodoContableFactura.TODO_HISTORIAL) }
    var busquedaComprobante by remember { mutableStateOf("") }
    // BackHandler quiet — teclado primero, luego limpia búsqueda (nunca cierra pantalla)
    val focusManagerCuentas = LocalFocusManager.current
    val keyboardControllerCuentas = LocalSoftwareKeyboardController.current
    val densityCuentas = LocalDensity.current
    val isKeyboardVisibleCuentas = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(densityCuentas) > 0
    BackHandler(enabled = true) {
        when {
            isKeyboardVisibleCuentas -> { keyboardControllerCuentas?.hide(); focusManagerCuentas.clearFocus(force = true) }
            busquedaComprobante.isNotBlank() -> busquedaComprobante = ""
            else -> { }
        }
    }

    val facturasDelPeriodo = remember(facturas, periodoSeleccionado) {
        facturas.filter { perteneceAPeriodo(it, periodoSeleccionado) }
    }

    // Las anuladas ya no deben nada: se ven en "Todas" (historial), pero jamás
    // cuentan como deuda pendiente, vencida ni pagada del período.
    val facturasVivasPeriodo = remember(facturasDelPeriodo) { facturasDelPeriodo.filter { !it.esAnulada } }

    val facturasFiltradasPorEstado = remember(facturasDelPeriodo, facturasVivasPeriodo, filtroEstado) {
        when (filtroEstado) {
            "PENDIENTES" -> facturasVivasPeriodo.filter { !it.esTotalmentePagada }
            "VENCIDAS" -> facturasVivasPeriodo.filter {
                val v = calcularVencimientoHumano(it)
                !it.esTotalmentePagada && v.esVencido
            }
            "PAGADAS" -> facturasVivasPeriodo.filter { it.esTotalmentePagada }
            else -> facturasDelPeriodo
        }
    }

    val facturasMostradas = remember(facturasFiltradasPorEstado, busquedaComprobante) {
        if (busquedaComprobante.isBlank()) facturasFiltradasPorEstado
        else facturasFiltradasPorEstado.filter {
            it.numeroFactura.contains(busquedaComprobante, ignoreCase = true) ||
                    it.proveedorNombre.contains(busquedaComprobante, ignoreCase = true)
        }
    }

    val facturaActiva = facturaSeleccionada ?: facturasMostradas.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = s.padScreenH, vertical = s.padScreenV),
        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
    ) {
        // ══════════════════════════════════════════════════════════════════════════
        // CABECERA ESTRATÉGICA (KPIS AIREADOS + FILTROS)
        // ══════════════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = s.xs, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GRUPO 1: CONTEXTO (SELECTOR) + KPIs DE DINERO
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                // Selector de Periodo Minimalista
                var mostrarMenuPeriodo by remember { mutableStateOf(false) }
                Box {
                    Surface(
                        onClick = { mostrarMenuPeriodo = true },
                        color = Color.Transparent,
                        modifier = Modifier.height(s.btnMediumH)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, null, tint = FDColors.Primary, modifier = Modifier.size(20.dp))
                            Text(
                                text = periodoSeleccionado.label,
                                style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = FDColors.TextPrimary
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = FDColors.TextTertiary, modifier = Modifier.size(20.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = mostrarMenuPeriodo,
                        onDismissRequest = { mostrarMenuPeriodo = false },
                        modifier = Modifier.background(FDColors.SurfaceElevated)
                    ) {
                        PeriodoContableFactura.values().forEach { per ->
                            val isSel = per == periodoSeleccionado
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = per.label,
                                        style = FDType.Body.copy(
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSel) FDColors.Primary else FDColors.TextPrimary
                                    )
                                },
                                onClick = {
                                    periodoSeleccionado = per
                                    mostrarMenuPeriodo = false
                                }
                            )
                        }
                    }
                }

                // KPIs Financieros
                val montoPendienteTotal = facturasVivasPeriodo.sumOf { it.saldoPendienteReal }
                val montoPagadoTotal = facturasVivasPeriodo.sumOf { it.totalAbonadoReal }

                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    MetricaAireada(
                        "DEUDA PENDIENTE",
                        "$simboloMoneda " + String.format(Locale.US, "%.2f", montoPendienteTotal),
                        color = if (montoPendienteTotal > 0.01) FDColors.Warning else FDColors.TextPrimary
                    )
                    MetricaAireada(
                        "TOTAL PAGADO",
                        "$simboloMoneda " + String.format(Locale.US, "%.2f", montoPagadoTotal),
                        color = FDColors.TextPrimary
                    )
                }
            }

            // GRUPO 2: FILTRO DE ACCIÓN (SEGMENTADO)
            val countPendientes = facturasVivasPeriodo.count { !it.esTotalmentePagada }
            val countVencidas = facturasVivasPeriodo.count {
                val v = calcularVencimientoHumano(it)
                !it.esTotalmentePagada && v.esVencido
            }
            val countPagadas = facturasVivasPeriodo.count { it.esTotalmentePagada }

            val opcionesFiltro = listOf(
                Triple("TODAS", "Todas", facturasDelPeriodo.size),
                Triple("PENDIENTES", "Pend.", countPendientes),
                Triple("VENCIDAS", "Venc.", countVencidas),
                Triple("PAGADAS", "Pag.", countPagadas)
            )

            Surface(
                color = FDColors.InputBackground.copy(alpha = 0.4f),
                shape = CircleShape,
                modifier = Modifier.width(360.dp)
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    opcionesFiltro.forEach { (idFiltro, label, count) ->
                        val isSel = filtroEstado == idFiltro
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(CircleShape)
                                .background(if (isSel) FDColors.SurfaceElevated else Color.Transparent)
                                .clickable { onCambiarFiltroEstado(idFiltro) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = FDType.Label.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSel) FDColors.TextPrimary else FDColors.TextTertiary,
                                    maxLines = 1
                                )
                                if (count > 0) {
                                    Text(
                                        text = "$count",
                                        style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                                        color = if (isSel) FDColors.Primary else FDColors.TextTertiary.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
        ) {
            // ══════════════════════════════════════════════════════════════════════════
            // PANEL IZQUIERDO: LISTADO ESTILO DOCUMENTO (40%)
            // ══════════════════════════════════════════════════════════════════════════
            Surface(
                color = FDColors.Surface,
                shape = FDShapes.Medium,
                border = BorderStroke(s.borderWidth, FDColors.Border),
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ── BUSCADOR ──
                    OutlinedTextField(
                        value = busquedaComprobante,
                        onValueChange = { busquedaComprobante = it },
                        placeholder = { Text("Buscar factura o droguería...", fontSize = 12.sp, color = FDColors.InputPlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = FDColors.TextTertiary, modifier = Modifier.size(s.iconSmall)) },
                        singleLine = true,
                        shape = FDShapes.Small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FDColors.InputBackground,
                            unfocusedContainerColor = FDColors.InputBackground,
                            focusedBorderColor = FDColors.BorderFocus,
                            unfocusedBorderColor = FDColors.InputBorder,
                            focusedTextColor = FDColors.InputText,
                            unfocusedTextColor = FDColors.InputText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(s.padCard * 0.75f)
                            .height(s.inputMinH)
                    )

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = 0.5.dp)

                    // Lista de Facturas
                    if (facturasMostradas.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(s.padCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(s.gapSmall)
                            ) {
                                Icon(Icons.Default.FilterListOff, null, tint = FDColors.TextTertiary, modifier = Modifier.size(s.iconMedium))
                                Text("Sin resultados", style = FDType.Body.copy(fontWeight = FontWeight.Bold), color = FDColors.TextPrimary)
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listaState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(facturasMostradas, key = { it.id }) { fact ->
                                val isSelected = fact.id == (facturaActiva?.id ?: "")
                                val esAnulada = fact.esAnulada

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) FDColors.Primary.copy(alpha = 0.08f) else Color.Transparent)
                                        .clickable { onSeleccionarFactura(fact.id) }
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = fact.numeroFactura.ifBlank { "SIN N°" },
                                                style = FDType.Label.copy(fontWeight = FontWeight.Black, fontSize = 12.sp),
                                                color = if (esAnulada) FDColors.Error else if (isSelected) FDColors.Primary else FDColors.TextPrimary
                                            )
                                            Text(
                                                text = "$simboloMoneda " + String.format(Locale.US, "%.2f", fact.totalEfectivo),
                                                style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Black),
                                                color = if (esAnulada) FDColors.Error else FDColors.TextPrimary
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = fact.proveedorNombre.uppercase(),
                                                style = FDType.BodySmall.copy(fontSize = 12.sp),
                                                color = if (esAnulada) FDColors.Error.copy(alpha = 0.6f) else FDColors.TextSecondary,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                                            )
                                            Text(
                                                text = fact.fechaRegistro,
                                                style = FDType.Caption,
                                                color = FDColors.TextTertiary
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = FDColors.Border.copy(alpha = 0.3f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // PANEL DERECHO: DETALLE, HISTORIAL DE ABONOS Y LIQUIDACIÓN
            Surface(
                color = FDColors.Surface,
                shape = FDShapes.Medium,
                border = BorderStroke(s.borderWidth, FDColors.Border),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (facturaActiva == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(s.padCardLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                null,
                                tint = FDColors.TextTertiary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "SELECCIONA UNA FACTURA",
                                style = FDType.Heading3.copy(fontWeight = FontWeight.Bold),
                                color = FDColors.TextTertiary
                            )
                            Text(
                                text = "Aquí podrás ver el detalle de productos, historial de abonos y liquidar la deuda.",
                                style = FDType.BodySmall,
                                color = FDColors.TextTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    DetalleFacturaLiquidacion(
                        factura = facturaActiva,
                        simboloMoneda = simboloMoneda,
                        procesandoPago = procesandoPago,
                        onAbrirDialogoAbono = onAbrirDialogoAbono,
                        onAbrirDialogoProrroga = onAbrirDialogoProrroga,
                        onAbrirDialogoNotaCredito = onAbrirDialogoNotaCredito,
                        onAbrirDialogoAnular = onAbrirDialogoAnular
                    )
                }
            }
        }
    }
}

@Composable
private fun DetalleFacturaLiquidacion(
    factura: FacturaCompra,
    simboloMoneda: String,
    procesandoPago: Boolean,
    onAbrirDialogoAbono: (FacturaCompra) -> Unit,
    onAbrirDialogoProrroga: (FacturaCompra) -> Unit,
    onAbrirDialogoNotaCredito: (FacturaCompra) -> Unit,
    onAbrirDialogoAnular: (FacturaCompra) -> Unit
) {
    val s = recordarMedidaAdaptativa()
    val esContado = factura.esContado
    val esPagada = factura.esTotalmentePagada
    val saldoRestante = factura.saldoPendienteReal
    val etiquetaEstado = when {
        factura.esAnulada -> "ANULADA"
        esPagada -> if (esContado) "CONTADO · PAGADO" else "PAGADA"
        factura.totalAbonadoReal > 0.01 -> "ABONO PARCIAL"
        else -> "PENDIENTE"
    }
    val colorEstado = when {
        factura.esAnulada -> FDColors.Error
        esPagada -> FDColors.Success
        factura.totalAbonadoReal > 0.01 -> FDColors.Primary
        else -> FDColors.Warning
    }
    val fechaPagoTexto = when {
        factura.esAnulada -> "Anulada"
        factura.fechaVencimientoPago.isNotBlank() -> factura.fechaVencimientoPago
        esPagada -> "Al contado (pagado)"
        else -> "Sin fecha · pago pendiente"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 1. CABECERA EJECUTIVA DEL DETALLE ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FDColors.SurfaceElevated)
                .padding(s.padCard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = factura.proveedorNombre.uppercase(),
                    style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
                    color = FDColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Factura N° ${factura.numeroFactura} · Emitida: ${factura.fechaRegistro}",
                    style = FDType.BodySmall.copy(fontSize = 11.5.sp),
                    color = FDColors.TextSecondary
                )
                if (factura.esAnulada) {
                    Text(
                        text = "✖ FACTURA ANULADA" + (if (factura.motivoAnulacion.isNotBlank()) " — ${factura.motivoAnulacion}" else ""),
                        style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Black),
                        color = FDColors.Error
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(s.xs)
            ) {
                Surface(
                    color = colorEstado.copy(alpha = 0.10f),
                    shape = FDShapes.XSmall,
                    border = BorderStroke(0.8.dp, colorEstado.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = etiquetaEstado,
                        style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                        color = colorEstado,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                // Acciones Rápidas (Prorrogar Vencimiento)
                if (!factura.esAnulada && !esContado && !esPagada) {
                    FDBotonSecundario(
                        texto = "PRORROGAR",
                        onClick = { onAbrirDialogoProrroga(factura) },
                        modifier = Modifier.height(s.btnSmallH)
                    )
                }
            }
        }

        HorizontalDivider(color = FDColors.Border, thickness = s.separatorH)

        // ── 2. CUERPO SCROLLABLE: HISTORIAL DE PAGOS Y NOTAS DE CRÉDITO ──
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(s.padCard),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // BLOQUE B: HISTORIAL DE ABONOS REALIZADOS
            Column(verticalArrangement = Arrangement.spacedBy(s.gapSmall)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("HISTORIAL DE PAGOS", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary)
                    if (esPagada && !esContado) {
                        Text(
                            text = "FACTURA LIQUIDADA",
                            style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.Success
                        )
                    }
                }

                if (factura.abonos.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)) {
                        factura.abonos.sortedByDescending { it.fechaMs }.forEach { abono ->
                            ItemAbonoRow(abono, simboloMoneda)
                        }
                    }
                } else if (esContado && esPagada) {
                    Surface(
                        color = FDColors.SuccessSubtle,
                        shape = FDShapes.Small,
                        border = BorderStroke(s.borderWidth * 0.8f, FDColors.Success.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(s.padCard * 0.65f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
                        ) {
                            Icon(Icons.Default.Verified, null, tint = FDColors.Success, modifier = Modifier.size(s.iconSmall))
                            Text(
                                "Pago al contado registrado el ${factura.fechaRegistro} por el monto total de la factura.",
                                style = FDType.BodySmall.copy(fontSize = 11.5.sp),
                                color = FDColors.TextSecondary
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(FDColors.TextPrimary.copy(alpha = 0.02f), FDShapes.Small)
                            .border(1.dp, FDColors.Border.copy(alpha = 0.3f), FDShapes.Small),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No se han registrado abonos todavía.", style = FDType.BodySmall, color = FDColors.TextTertiary)
                    }
                }
            }

            // NOTAS DE CRÉDITO: cada ajuste del papel con su documento (append-only)
            if (factura.ajustesFactura.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(s.gapSmall)) {
                    Text("NOTAS DE CRÉDITO", style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = FDColors.TextSecondary)
                    factura.ajustesFactura.sortedByDescending { it.fechaMs }.forEach { ajuste ->
                        Surface(
                            color = FDColors.Surface,
                            shape = FDShapes.Small,
                            border = BorderStroke(s.borderWidth * 0.6f, FDColors.Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "N° ${ajuste.numeroDocumento.ifBlank { "Sin N°" }}",
                                        style = FDType.Label.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Black),
                                        color = FDColors.TextPrimary,
                                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                                    )
                                    Text(
                                        text = "- $simboloMoneda " + String.format(Locale.US, "%.2f", ajuste.monto),
                                        style = FDType.Numeric.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Black),
                                        color = FDColors.Warning
                                    )
                                }
                                Text(
                                    text = ajuste.motivo.ifBlank { "Ajuste por nota de crédito" },
                                    style = FDType.BodySmall.copy(fontSize = 10.5.sp),
                                    color = FDColors.TextSecondary
                                )
                                Text(
                                    text = ajuste.fechaLegible.ifBlank { "—" } + (if (ajuste.usuarioNombre.isNotBlank()) " · ${ajuste.usuarioNombre}" else ""),
                                    style = FDType.Caption.copy(fontSize = 10.sp),
                                    color = FDColors.TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 3. PIE FIJO: RESUMEN CONTABLE DE LA FACTURA ──
        Surface(
            color = FDColors.SurfaceElevated,
            border = BorderStroke(s.borderWidth, FDColors.Border)
        ) {
            Column(modifier = Modifier.padding(s.padCard), verticalArrangement = Arrangement.spacedBy(s.gapMedium)) {
                Text(
                    "RESUMEN CONTABLE DE LA FACTURA",
                    style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                    color = FDColors.TextTertiary
                )

                FilaContable("Emitida", factura.fechaRegistro.ifBlank { "—" }, s)
                FilaContable("Fecha de pago", fechaPagoTexto, s)
                if (factura.totalAjustes > 0.01) {
                    FilaContable("Notas de crédito", "- $simboloMoneda " + String.format(Locale.US, "%.2f", factura.totalAjustes), s, color = FDColors.Warning)
                }
                FilaContable("Abonado", "$simboloMoneda " + String.format(Locale.US, "%.2f", factura.totalAbonadoReal), s, color = FDColors.Success)
                FilaContable(
                    "Pendiente",
                    "$simboloMoneda " + String.format(Locale.US, "%.2f", saldoRestante),
                    s,
                    color = if (saldoRestante > 0.01) FDColors.Warning else FDColors.Success,
                    bold = true
                )
                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.6f), thickness = s.separatorH)
                FilaContable(
                    "Total de factura",
                    "$simboloMoneda " + String.format(Locale.US, "%.2f", factura.totalPapel),
                    s,
                    color = FDColors.TextPrimary,
                    bold = true,
                    grande = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(s.gapSmall * 1.2f)
                ) {
                    if (!factura.esAnulada) {
                        if (!esContado) {
                            FDBotonSecundario(
                                texto = "NOTA DE CRÉDITO",
                                onClick = { onAbrirDialogoNotaCredito(factura) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        FDBotonSecundario(
                            texto = "ANULAR FACTURA",
                            onClick = { onAbrirDialogoAnular(factura) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!esPagada && !factura.esAnulada) {
                        FDBotonPrimario(
                            texto = if (factura.totalAbonadoReal > 0) "REGISTRAR OTRO ABONO" else "REGISTRAR PAGO / ABONO",
                            onClick = { onAbrirDialogoAbono(factura) },
                            icono = Icons.Default.AddCard,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemAbonoRow(
    abono: AbonoFactura,
    simboloMoneda: String
) {
    val s = recordarMedidaAdaptativa()

    Surface(
        color = if (abono.anulado) FDColors.Error.copy(alpha = 0.05f) else FDColors.Surface,
        shape = FDShapes.Small,
        border = BorderStroke(
            s.borderWidth * 0.6f,
            if (abono.anulado) FDColors.Error.copy(alpha = 0.35f) else FDColors.Border
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.gapSmall), modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(s.iconMedium)
                        .clip(CircleShape)
                        .background(FDColors.Success.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Payments, null, tint = FDColors.Success, modifier = Modifier.size(s.iconSmall * 0.88f))
                }
                Column {
                    Text(
                        text = EtiquetaMetodoPago.nombreCorto(abono.metodoPago).ifBlank { "Sin método" } + (if (abono.numeroOperacion.isNotBlank()) " · Op. ${abono.numeroOperacion}" else ""),
                        style = FDType.Body.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextPrimary
                    )
                    Text(
                        text = abono.fechaLegible,
                        style = FDType.BodySmall.copy(fontSize = 11.sp),
                        color = FDColors.TextTertiary
                    )
                }
            }

                    Text(
                        text = "$simboloMoneda " + String.format(Locale.US, "%.2f", abono.monto),
                        style = FDType.Numeric.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Black),
                        color = if (abono.anulado) FDColors.Error else FDColors.TextPrimary
                    )
            }

            if (abono.anulado) {
                Text(
                    text = "ABONO ANULADO" + (if (abono.motivoAnulacion.isNotBlank()) " · ${abono.motivoAnulacion}" else "") +
                        (if (abono.anuladoPorNombre.isNotBlank()) " · ${abono.anuladoPorNombre}" else ""),
                    style = FDType.Label.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Black),
                    color = FDColors.Error,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // PAGO MIXTO: se muestra el desglose real ("en este medio pagué tanto, en este otro tanto").
            if (abono.pagos.size > 1) {
                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = s.separatorH * 0.7f)
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    abono.pagos.forEach { pago ->
                        Text(
                            text = "${EtiquetaMetodoPago.nombreCorto(pago.metodoPago).ifBlank { "Sin método" }} · $simboloMoneda " + String.format(Locale.US, "%.2f", pago.monto) +
                                (if (pago.numeroOperacion.isNotBlank()) " · Op. ${pago.numeroOperacion}" else ""),
                            style = FDType.BodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = FDColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricaAireada(
    etiqueta: String,
    valor: String,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = etiqueta,
            style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
            color = FDColors.TextTertiary.copy(alpha = 0.7f)
        )
        Text(
            text = valor,
            style = FDType.Numeric.copy(fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
            color = color
        )
    }
}

@Composable
private fun FilaContable(
    etiqueta: String,
    valor: String,
    s: MedidaAdaptativa,
    color: Color = FDColors.TextSecondary,
    bold: Boolean = false,
    grande: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = etiqueta.uppercase(),
            style = FDType.Label.copy(fontSize = if (grande) 11.sp else 10.sp, fontWeight = FontWeight.Bold),
            color = FDColors.TextTertiary
        )
        Text(
            text = valor,
            style = if (grande) {
                FDType.Numeric.copy(fontSize = 17.sp, fontWeight = FontWeight.Black)
            } else {
                FDType.Numeric.copy(fontSize = if (bold) 13.5.sp else 12.5.sp, fontWeight = if (bold) FontWeight.Black else FontWeight.SemiBold)
            },
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
