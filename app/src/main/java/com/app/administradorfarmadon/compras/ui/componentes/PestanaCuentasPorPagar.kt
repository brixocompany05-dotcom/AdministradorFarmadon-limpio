package com.app.administradorfarmadon.compras.ui.componentes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    val fechaFact = parsearFechaFlexible(factura.fechaRegistro) ?: return true

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
    if (factura.esContado) {
        val fechaEmision = factura.fechaRegistro.ifBlank { "Reciente" }
        return InfoVencimientoHumano(
            esContado = true,
            textoTarjeta = "Emitida: $fechaEmision · Contado",
            textoDetalle = "Liquidado al recibir mercadería · Cero deuda pendiente",
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
    onRevertirPago: (String, String, String) -> Unit = { _, _, _ -> },
    onAbrirDialogoAbono: (FacturaCompra) -> Unit = {},
    onAnularAbono: (facturaId: String, abonoId: String) -> Unit = { _, _ -> },
    onAbrirDialogoProrroga: (FacturaCompra) -> Unit = {}
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

    val facturasFiltradasPorEstado = remember(facturasDelPeriodo, filtroEstado) {
        when (filtroEstado) {
            "PENDIENTES" -> facturasDelPeriodo.filter { !it.esTotalmentePagada }
            "VENCIDAS" -> facturasDelPeriodo.filter {
                val v = calcularVencimientoHumano(it)
                !it.esTotalmentePagada && v.esVencido
            }
            "PAGADAS" -> facturasDelPeriodo.filter { it.esTotalmentePagada }
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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = s.padScreenH, vertical = s.padScreenV),
        horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
    ) {
        // ══════════════════════════════════════════════════════════════════════════
        // PANEL IZQUIERDO: LISTADO Y FILTROS CONTABLES (40%)
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
                // ── CABECERA DE FILTROS ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(s.padCard * 0.75f),
                    verticalArrangement = Arrangement.spacedBy(s.gapMedium * 0.85f)
                ) {
                    // FILA 1: BUSCADOR + PERÍODO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(s.gapSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = busquedaComprobante,
                            onValueChange = { busquedaComprobante = it },
                            placeholder = { Text("Factura o droguería...", fontSize = 12.sp, color = FDColors.InputPlaceholder) },
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
                                .weight(1f)
                                .height(s.inputMinH)
                        )

                        var mostrarMenuPeriodo by remember { mutableStateOf(false) }
                        Box {
                            Surface(
                                onClick = { mostrarMenuPeriodo = true },
                                color = FDColors.SurfaceElevated,
                                shape = FDShapes.Small,
                                border = BorderStroke(s.borderWidth, FDColors.Border),
                                modifier = Modifier.height(s.btnMediumH)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, null, tint = FDColors.Primary, modifier = Modifier.size(s.iconTiny))
                                    Text(
                                        text = periodoSeleccionado.label,
                                        style = FDType.Label.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = FDColors.TextPrimary
                                    )
                                    Icon(Icons.Default.ArrowDropDown, null, tint = FDColors.TextTertiary, modifier = Modifier.size(s.iconSmall * 0.88f))
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
                    }

                    // ── FILA 2: PESTAÑAS DE ESTADO (CONTEO EN VIVO) ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(s.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val countPendientes = facturasDelPeriodo.count { !it.esTotalmentePagada }
                        val countVencidas = facturasDelPeriodo.count {
                            val v = calcularVencimientoHumano(it)
                            !it.esTotalmentePagada && v.esVencido
                        }
                        val countPagadas = facturasDelPeriodo.count { it.esTotalmentePagada }

                        val opcionesFiltro = listOf(
                            Triple("TODAS", "Todas", facturasDelPeriodo.size),
                            Triple("PENDIENTES", "Pend.", countPendientes),
                            Triple("VENCIDAS", "Venc.", countVencidas),
                            Triple("PAGADAS", "Pag.", countPagadas)
                        )

                        opcionesFiltro.forEach { (idFiltro, label, count) ->
                            val isSel = filtroEstado == idFiltro
                            Surface(
                                color = if (isSel) FDColors.SurfaceElevated else Color.Transparent,
                                shape = FDShapes.Small,
                                border = BorderStroke(
                                    if (isSel) 1.2.dp else 0.8.dp,
                                    if (isSel) FDColors.TextPrimary.copy(alpha = 0.5f) else FDColors.Border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onCambiarFiltroEstado(idFiltro) }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = FDType.Label.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Black else FontWeight.Medium
                                        ),
                                        color = if (isSel) FDColors.TextPrimary else FDColors.TextSecondary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "$count",
                                        style = FDType.Label.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isSel) {
                                            when (idFiltro) {
                                                "VENCIDAS" -> if (count > 0) FDColors.Warning else FDColors.TextPrimary
                                                "PENDIENTES" -> if (count > 0) FDColors.Warning else FDColors.TextPrimary
                                                "PAGADAS" -> if (count > 0) FDColors.Success else FDColors.TextPrimary
                                                else -> FDColors.TextPrimary
                                            }
                                        } else FDColors.TextTertiary
                                    )
                                }
                            }
                        }
                    }

                    // ── FILA 3: TOTALES CONTABLES DEL PERÍODO ──
                    val montoPendientePeriodo = facturasDelPeriodo
                        .filter { !it.esTotalmentePagada }
                        .sumOf { it.saldoPendienteReal }
                    val montoPagadoPeriodo = facturasDelPeriodo
                        .sumOf { if (it.esContado) it.totalEfectivo else it.totalAbonadoReal }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = FDColors.SurfaceElevated,
                            shape = FDShapes.XSmall,
                            border = BorderStroke(s.borderWidth * 0.6f, FDColors.Border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Deuda:",
                                    style = FDType.BodySmall.copy(fontSize = 10.5.sp),
                                    color = FDColors.TextTertiary
                                )
                                Text(
                                    text = "$simboloMoneda " + String.format(Locale.US, "%,.2f", montoPendientePeriodo),
                                    style = FDType.Label.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (montoPendientePeriodo > 0) FDColors.Warning else FDColors.TextPrimary
                                )
                            }
                        }

                        Surface(
                            color = FDColors.SurfaceElevated,
                            shape = FDShapes.XSmall,
                            border = BorderStroke(s.borderWidth * 0.6f, FDColors.Border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pagado:",
                                    style = FDType.BodySmall.copy(fontSize = 10.5.sp),
                                    color = FDColors.TextTertiary
                                )
                                Text(
                                    text = "$simboloMoneda " + String.format(Locale.US, "%,.2f", montoPagadoPeriodo),
                                    style = FDType.Label.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = FDColors.TextPrimary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f), thickness = s.separatorH)

                    // Lista de Tarjetas
                    if (facturasMostradas.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(s.padCard * 0.75f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)
                            ) {
                                Icon(
                                    Icons.Default.FilterListOff,
                                    contentDescription = null,
                                    tint = FDColors.TextTertiary,
                                    modifier = Modifier.size(s.iconMedium)
                                )
                                Text(
                                    text = if (busquedaComprobante.isNotBlank()) "Sin resultados para '$busquedaComprobante'" else "Sin facturas ${filtroEstado.lowercase()} en ${periodoSeleccionado.label}",
                                    style = FDType.Heading3.copy(fontSize = 13.5.sp),
                                    color = FDColors.TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (busquedaComprobante.isNotBlank()) "Verifica el número de comprobante o nombre de la droguería." else "Cambia el período o selecciona 'Todas' para ver el historial completo.",
                                    style = FDType.BodySmall.copy(fontSize = 11.5.sp),
                                    color = FDColors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)
                        ) {
                            items(facturasMostradas, key = { it.id }) { fact ->
                                val isSelected = fact.id == (facturaActiva?.id ?: "")
                                val esContado = fact.esContado
                                val esPagada = fact.esTotalmentePagada
                                val tieneAbonosParciales = fact.totalAbonadoReal > 0 && !esPagada
                                val infoVenc = calcularVencimientoHumano(fact)

                                Surface(
                                    color = if (isSelected) FDColors.SurfaceElevated else FDColors.Surface,
                                    shape = FDShapes.Medium,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 0.8.dp,
                                        if (isSelected) FDColors.Primary else FDColors.Border
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSeleccionarFactura(fact.id) }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(s.padCard * 0.5f),
                                        verticalArrangement = Arrangement.spacedBy(s.xs)
                                    ) {
                                        // ── Nivel 1: N° Factura + Badge de Estado Discreto ──
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(s.xs),
                                                modifier = Modifier.weight(1f).padding(end = 6.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ReceiptLong,
                                                    null,
                                                    tint = if (isSelected) FDColors.Primary else FDColors.TextTertiary,
                                                    modifier = Modifier.size(s.iconSmall * 0.88f)
                                                )
                                                Text(
                                                    text = fact.numeroFactura.ifBlank { "Sin N°" },
                                                    style = FDType.Heading3.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.5.sp
                                                    ),
                                                    color = FDColors.TextPrimary,
                                                    maxLines = 1
                                                )
                                            }

                                            Surface(
                                                color = when {
                                                    esContado || esPagada -> FDColors.SuccessSubtle
                                                    tieneAbonosParciales -> FDColors.Primary.copy(alpha = 0.10f)
                                                    infoVenc.esVencido -> FDColors.WarningSubtle
                                                    else -> FDColors.WarningSubtle
                                                },
                                                shape = FDShapes.XSmall,
                                                border = BorderStroke(
                                                    0.8.dp,
                                                    when {
                                                        esContado || esPagada -> FDColors.Success.copy(alpha = 0.35f)
                                                        tieneAbonosParciales -> FDColors.Primary.copy(alpha = 0.35f)
                                                        infoVenc.esVencido -> FDColors.Warning.copy(alpha = 0.35f)
                                                        else -> FDColors.Warning.copy(alpha = 0.35f)
                                                    }
                                                )
                                            ) {
                                                Text(
                                                    text = when {
                                                        esContado -> "✓ CONTADO"
                                                        esPagada -> "● PAGADA"
                                                        tieneAbonosParciales -> "● ABONO PARCIAL"
                                                        infoVenc.esVencido -> "● VENCIDA"
                                                        else -> "● PENDIENTE"
                                                    },
                                                    style = FDType.Label.copy(
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = when {
                                                        esContado || esPagada -> FDColors.Success
                                                        tieneAbonosParciales -> FDColors.Primary
                                                        infoVenc.esVencido -> FDColors.Warning
                                                        else -> FDColors.Warning
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // ── Nivel 2: Proveedor Comercial ──
                                        Text(
                                            text = fact.proveedorNombre.ifBlank { "Proveedor Sin Asignar" },
                                            style = FDType.Body.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            ),
                                            color = FDColors.TextPrimary,
                                            maxLines = 1
                                        )

                                        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = s.separatorH)

                                        // ── Nivel 3: Condición + Monto / Saldo ──
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (esContado) "Emitida: ${fact.fechaRegistro.ifBlank { "Reciente" }} · Contado" else infoVenc.textoTarjeta,
                                                style = FDType.BodySmall.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = if (!esContado && (infoVenc.esVencido || infoVenc.esAlertaPronta)) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = when {
                                                    esContado -> FDColors.TextSecondary
                                                    esPagada -> FDColors.TextTertiary
                                                    infoVenc.esVencido -> FDColors.Warning
                                                    infoVenc.esAlertaPronta -> FDColors.Warning
                                                    else -> FDColors.TextSecondary
                                                },
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f).padding(end = 6.dp)
                                            )

                                            Column(horizontalAlignment = Alignment.End) {
                                                if (esContado) {
                                                    Text(
                                                        text = "$simboloMoneda " + String.format(Locale.US, "%,.2f", fact.totalEfectivo),
                                                        style = FDType.Heading3.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                                        color = FDColors.TextPrimary
                                                    )
                                                } else if (tieneAbonosParciales) {
                                                    Text(
                                                        text = "Saldo: $simboloMoneda " + String.format(Locale.US, "%,.2f", fact.saldoPendienteReal),
                                                        style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Black),
                                                        color = FDColors.Warning
                                                    )
                                                } else {
                                                    Text(
                                                        text = "$simboloMoneda " + String.format(Locale.US, "%,.2f", fact.totalEfectivo),
                                                        style = FDType.Heading3.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                                        color = FDColors.TextPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
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
                        onAnularAbono = onAnularAbono,
                        onAbrirDialogoProrroga = onAbrirDialogoProrroga,
                        onRevertirPago = onRevertirPago
                    )
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
    onAnularAbono: (facturaId: String, abonoId: String) -> Unit,
    onAbrirDialogoProrroga: (FacturaCompra) -> Unit,
    onRevertirPago: (String, String, String) -> Unit
) {
    val s = recordarMedidaAdaptativa()
    val infoVenc = calcularVencimientoHumano(factura)
    val esContado = factura.esContado
    val esPagada = factura.esTotalmentePagada
    val saldoRestante = factura.saldoPendienteReal

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
            }

            // Acciones Rápidas (Prorrogar Vencimiento)
            if (!esContado && !esPagada) {
                FDBotonSecundario(
                    texto = "PRORROGAR",
                    onClick = { onAbrirDialogoProrroga(factura) },
                    modifier = Modifier.height(s.btnSmallH)
                )
            }
        }

        HorizontalDivider(color = FDColors.Border, thickness = s.separatorH)

        // ── 2. CUERPO SCROLLABLE: INFO + ITEMS + ABONOS ──
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(s.padCard),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // BLOQUE A: ESTADO FINANCIERO ACTUAL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                // Tarjeta: Vencimiento
                Surface(
                    color = if (infoVenc.esVencido) FDColors.WarningSubtle else FDColors.SurfaceElevated,
                    shape = FDShapes.Small,
                    border = BorderStroke(s.borderWidth, if (infoVenc.esVencido) FDColors.Warning.copy(alpha = 0.4f) else FDColors.Border),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(s.padCard * 0.65f)) {
                        Text("VENCIMIENTO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (esContado) "CONTADO" else infoVenc.textoDetalle,
                            style = FDType.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            color = if (infoVenc.esVencido) FDColors.Warning else FDColors.TextPrimary
                        )
                    }
                }

                // Tarjeta: Saldo Pendiente
                Surface(
                    color = if (saldoRestante > 0) FDColors.WarningSubtle else FDColors.SuccessSubtle,
                    shape = FDShapes.Small,
                    border = BorderStroke(s.borderWidth, if (saldoRestante > 0) FDColors.Warning.copy(alpha = 0.4f) else FDColors.Success.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(s.padCard * 0.65f)) {
                        Text("SALDO RESTANTE", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$simboloMoneda " + String.format(Locale.US, "%,.2f", saldoRestante),
                            style = FDType.Numeric.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
                            color = if (saldoRestante > 0) FDColors.Warning else FDColors.Success
                        )
                    }
                }
            }

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

                if (factura.abonos.isEmpty() && !esContado) {
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
                } else if (esContado) {
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
                    Column(verticalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)) {
                        factura.abonos.sortedByDescending { it.fechaMs }.forEach { abono ->
                            ItemAbonoRow(abono, simboloMoneda, onAnular = { onAnularAbono(factura.id, abono.id) })
                        }
                    }
                }
            }
        }

        // ── 3. PIE DE PANEL: TOTALES Y BOTÓN DE ACCIÓN ──
        Surface(
            color = FDColors.SurfaceElevated,
            border = BorderStroke(s.borderWidth, FDColors.Border)
        ) {
            Column(modifier = Modifier.padding(s.padCard), verticalArrangement = Arrangement.spacedBy(s.gapMedium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL DE LA FACTURA", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                        Text("$simboloMoneda " + String.format(Locale.US, "%,.2f", factura.totalEfectivo), style = FDType.Numeric.copy(fontSize = 18.sp, fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
                    }

                    if (factura.totalAbonadoReal > 0 && !esContado) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TOTAL ABONADO", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = FDColors.TextTertiary)
                            Text("$simboloMoneda " + String.format(Locale.US, "%,.2f", factura.totalAbonadoReal), style = FDType.Numeric.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = FDColors.Success)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(s.gapSmall * 1.2f)
                ) {
                    if (esPagada && !esContado) {
                        var showConfirmRevert by remember { mutableStateOf(false) }
                        FDBotonSecundario(
                            texto = "REVERTIR LIQUIDACIÓN",
                            onClick = { showConfirmRevert = true },
                            modifier = Modifier.weight(1f)
                        )

                        if (showConfirmRevert) {
                            AlertDialog(
                                onDismissRequest = { showConfirmRevert = false },
                                title = { Text("¿Revertir pago total?") },
                                text = { Text("Se anularán todos los abonos registrados y la factura volverá a estar pendiente de pago. Esta acción no se puede deshacer.") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        onRevertirPago(factura.id, factura.numeroFactura, factura.proveedorNombre)
                                        showConfirmRevert = false
                                    }) {
                                        Text("REVERTIR TODO", color = FDColors.Error, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showConfirmRevert = false }) { Text("CANCELAR") }
                                }
                            )
                        }
                    }

                    if (!esPagada && !esContado) {
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
    simboloMoneda: String,
    onAnular: () -> Unit
) {
    val s = recordarMedidaAdaptativa()
    var showConfirmAnular by remember { mutableStateOf(false) }

    Surface(
        color = FDColors.Surface,
        shape = FDShapes.Small,
        border = BorderStroke(s.borderWidth * 0.6f, FDColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
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
                        text = abono.metodoPago.ifBlank { "Sin método" } + (if (abono.numeroOperacion.isNotBlank()) " · ${abono.numeroOperacion}" else ""),
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

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.gapSmall * 1.2f)) {
                Text(
                    text = "$simboloMoneda " + String.format(Locale.US, "%,.2f", abono.monto),
                    style = FDType.Numeric.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Black),
                    color = FDColors.TextPrimary
                )

                IconButton(onClick = { showConfirmAnular = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, tint = FDColors.TextTertiary, modifier = Modifier.size(s.iconSmall))
                }
            }
        }
    }

    if (showConfirmAnular) {
        AlertDialog(
            onDismissRequest = { showConfirmAnular = false },
            title = { Text("¿Anular este abono?") },
            text = { Text("El monto de $simboloMoneda ${abono.monto} volverá a sumarse a la deuda pendiente de la factura.") },
            confirmButton = {
                TextButton(onClick = {
                    onAnular()
                    showConfirmAnular = false
                }) {
                    Text("SÍ, ANULAR", color = FDColors.Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmAnular = false }) { Text("CANCELAR") }
            }
        )
    }
}
