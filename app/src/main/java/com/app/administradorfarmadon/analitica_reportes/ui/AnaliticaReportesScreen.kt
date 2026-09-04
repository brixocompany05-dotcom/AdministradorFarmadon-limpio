package com.app.administradorfarmadon.analitica_reportes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.analitica_reportes.comunes.ui.DialogoDetalleVenta
import com.app.administradorfarmadon.analitica_reportes.logica.AnaliticaViewModel
import com.app.administradorfarmadon.analitica_reportes.modelo.AnaliticaUiState
import com.app.administradorfarmadon.analitica_reportes.modelo.PeriodoAnalitica
import com.app.administradorfarmadon.analitica_reportes.ui.componentes.DialogoSelectorMes
import com.app.administradorfarmadon.analitica_reportes.ui.componentes.DialogoSelectorRangoPersonalizado
import com.app.administradorfarmadon.analitica_reportes.ui.componentes.PestanaAnalitica
import com.app.administradorfarmadon.analitica_reportes.ui.componentes.PestanaReportes
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PANTALLA PRINCIPAL DE ANALÍTICA Y REPORTES — Enterprise SaaS 2026.
 *
 * Arquitectura mínima y limpia conectada al ViewModel real (R4/R8/R12).
 */
@Composable
fun AnaliticaReportesScreen(
    pestanaInicial: String = "ANALITICA",
    onVolver: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AnaliticaViewModel = viewModel()
) {
    val s = recordarMedidaAdaptativa()
    var moduloSeleccionado by remember(pestanaInicial) { mutableStateOf(pestanaInicial) }
    var selectorPeriodoAbierto by remember { mutableStateOf(false) }

    LaunchedEffect(pestanaInicial) {
        moduloSeleccionado = pestanaInicial
    }

    val uiState by viewModel.uiState.collectAsState()
    val periodoActivo by viewModel.periodoSeleccionado.collectAsState()
    val mesSeleccionado by viewModel.mesSeleccionado.collectAsState()
    val rangoPersonalizado by viewModel.rangoPersonalizado.collectAsState()
    val sucursalesDisponibles by viewModel.sucursalesDisponibles.collectAsState()
    val sedeSeleccionada by viewModel.sedeSeleccionada.collectAsState()
    val ventaDetalle by viewModel.ventaSeleccionada.collectAsState()
    val feedbackMsg by viewModel.mensajeFeedback.collectAsState()

    var dialogoMesAbierto by remember { mutableStateOf(false) }
    var dialogoRangoAbierto by remember { mutableStateOf(false) }

    val textoPeriodo = when (periodoActivo) {
        PeriodoAnalitica.MES -> {
            if (mesSeleccionado != null) {
                val nombresMeses = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Set", "Oct", "Nov", "Dic")
                "Mes: ${nombresMeses.getOrElse(mesSeleccionado!!.second - 1) { "${mesSeleccionado!!.second}" }} ${mesSeleccionado!!.first}"
            } else "Mes Específico"
        }
        PeriodoAnalitica.PERSONALIZADO -> {
            if (rangoPersonalizado != null) {
                val sdf = SimpleDateFormat("dd/MM/yy", Locale.US).apply { timeZone = AnaliticaViewModel.TIMEZONE_LIMA }
                "${sdf.format(Date(rangoPersonalizado!!.first))} - ${sdf.format(Date(rangoPersonalizado!!.second))}"
            } else "Personalizado"
        }
        else -> periodoActivo.label
    }

    val nombreSedeActiva = if (sedeSeleccionada == "TODAS") {
        "Todas las sedes"
    } else {
        sucursalesDisponibles.find { it.id == sedeSeleccionada }?.nombre ?: "Sede"
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMsg) {
        feedbackMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarFeedback()
        }
    }

    BackHandler(enabled = true) {
        onVolver()
    }

    Scaffold(
        containerColor = FDColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── TOPBAR PRINCIPAL SOBRE FONDO BASE (CONTRASTE PERFECTO CON CARDS) ───
            Surface(
                color = FDColors.Background,
                border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ── PESTAÑAS PRINCIPALES (CONTROL SEGMENTADO DESTACADO) ──────
                        Surface(
                            color = FDColors.Surface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, FDColors.Border),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val pestanas = listOf(
                                    "ANALITICA" to "Analítica de Farmacia",
                                    "REPORTES" to "Centro de Reportes"
                                )
                                pestanas.forEach { (clave, titulo) ->
                                    val esSeleccionada = moduloSeleccionado == clave
                                    Surface(
                                        color = if (esSeleccionada) FDColors.Primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { moduloSeleccionado = clave }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (clave == "ANALITICA") Icons.Default.Analytics else Icons.Default.Assessment,
                                                contentDescription = null,
                                                tint = if (esSeleccionada) FDColors.PrimaryText else FDColors.TextTertiary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Text(
                                                text = titulo,
                                                style = FDType.Label.copy(
                                                    fontSize = 13.sp,
                                                    fontWeight = if (esSeleccionada) FontWeight.Bold else FontWeight.SemiBold,
                                                    fontFamily = InterPremium
                                                ),
                                                color = if (esSeleccionada) FDColors.PrimaryText else FDColors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── SELECTORES DE SEDE Y PERÍODO (SIEMPRE VISIBLES Y AMPLIOS) ───
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            var selectorSedeAbierto by remember { mutableStateOf(false) }

                            // Selector de Sede (Siempre Visible)
                            Box {
                                Surface(
                                    color = FDColors.SurfaceElevated,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, FDColors.Border),
                                    modifier = Modifier.clickable { selectorSedeAbierto = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = FDColors.Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = nombreSedeActiva,
                                            style = FDType.Label.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = InterPremium
                                            ),
                                            color = FDColors.TextPrimary,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = FDColors.TextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = selectorSedeAbierto,
                                    onDismissRequest = { selectorSedeAbierto = false },
                                    modifier = Modifier.background(FDColors.SurfaceElevated)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Todas las sedes",
                                                style = FDType.Body.copy(fontSize = 13.sp),
                                                color = if (sedeSeleccionada == "TODAS") FDColors.TextPrimary else FDColors.TextSecondary,
                                                fontWeight = if (sedeSeleccionada == "TODAS") FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.seleccionarSede("TODAS")
                                            selectorSedeAbierto = false
                                        }
                                    )
                                    sucursalesDisponibles.forEach { suc ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (suc.activa) suc.nombre else "${suc.nombre} (Inactiva)",
                                                    style = FDType.Body.copy(fontSize = 13.sp),
                                                    color = if (sedeSeleccionada == suc.id) FDColors.TextPrimary else FDColors.TextSecondary,
                                                    fontWeight = if (sedeSeleccionada == suc.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewModel.seleccionarSede(suc.id)
                                                selectorSedeAbierto = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Selector de Período (Siempre Visible)
                            Box {
                                Surface(
                                    color = FDColors.SurfaceElevated,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, FDColors.Border),
                                    modifier = Modifier.clickable { selectorPeriodoAbierto = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = FDColors.Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = textoPeriodo,
                                            style = FDType.Label.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = InterPremium
                                            ),
                                            color = FDColors.TextPrimary,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = FDColors.TextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = selectorPeriodoAbierto,
                                    onDismissRequest = { selectorPeriodoAbierto = false },
                                    modifier = Modifier.background(FDColors.SurfaceElevated)
                                ) {
                                    PeriodoAnalitica.entries.forEach { opcion ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = opcion.label,
                                                    style = FDType.Body.copy(fontSize = 13.sp),
                                                    color = if (opcion == periodoActivo) FDColors.TextPrimary else FDColors.TextSecondary,
                                                    fontWeight = if (opcion == periodoActivo) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                selectorPeriodoAbierto = false
                                                when (opcion) {
                                                    PeriodoAnalitica.MES -> dialogoMesAbierto = true
                                                    PeriodoAnalitica.PERSONALIZADO -> dialogoRangoAbierto = true
                                                    else -> viewModel.seleccionarPeriodo(opcion)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (dialogoMesAbierto) {
                        DialogoSelectorMes(
                            anioInicial = mesSeleccionado?.first ?: 0,
                            mesInicial = mesSeleccionado?.second ?: 0,
                            onDismiss = { dialogoMesAbierto = false },
                            onConfirmar = { anio, mes ->
                                viewModel.seleccionarMesEspecifico(anio, mes)
                                dialogoMesAbierto = false
                            }
                        )
                    }

                    if (dialogoRangoAbierto) {
                        DialogoSelectorRangoPersonalizado(
                            inicioMsInicial = rangoPersonalizado?.first ?: 0L,
                            finMsInicial = rangoPersonalizado?.second ?: 0L,
                            onDismiss = { dialogoRangoAbierto = false },
                            onConfirmar = { iniMs, finMs ->
                                viewModel.seleccionarRangoPersonalizado(iniMs, finMs)
                                dialogoRangoAbierto = false
                            }
                        )
                    }

                    val exitoState = uiState as? AnaliticaUiState.Exito
                    if (exitoState != null && exitoState.esParcial) {
                        Surface(
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Información Parcial: Fuentes pendientes por red: ${exitoState.fuentesFallidas.keys.joinToString { it.label }}",
                                        style = FDType.BodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF92400E)
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.recargar() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Reintentar", style = FDType.Label.copy(fontSize = 11.sp), color = Color(0xFFB45309))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = FDColors.Border.copy(alpha = 0.3f),
                thickness = s.separatorH
            )

            val exitoActual = uiState as? AnaliticaUiState.Exito
            if (exitoActual?.estaActualizando == true) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = FDColors.Primary,
                    trackColor = FDColors.Primary.copy(alpha = 0.12f)
                )
            }

            // ── WORKSPACE PRINCIPAL ESPACIOSO Y DE ALTA DENSIDAD ────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(FDColors.Background)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Crossfade(
                    targetState = moduloSeleccionado,
                    label = "AnaliticaReportesTransition"
                ) { modulo ->
                    val etiquetaPeriodoVigente = if (uiState is AnaliticaUiState.Exito && (uiState as AnaliticaUiState.Exito).periodoEtiqueta.isNotBlank()) {
                        (uiState as AnaliticaUiState.Exito).periodoEtiqueta
                    } else textoPeriodo

                    when (modulo) {
                        "ANALITICA" -> PestanaAnalitica(
                            uiState = uiState,
                            periodo = etiquetaPeriodoVigente,
                            sedeNombre = nombreSedeActiva,
                            s = s,
                            onBuscarComprobante = { numero -> viewModel.buscarVentaParaDrillDown(numero) },
                            onVerDetalleVenta = { venta -> viewModel.seleccionarVenta(venta) },
                            onReintentar = { viewModel.recargar() },
                            onSeleccionarSede = { sedeId -> viewModel.seleccionarSede(sedeId) }
                        )
                        "REPORTES" -> PestanaReportes(
                            uiState = uiState,
                            periodoInicial = etiquetaPeriodoVigente,
                            sedeNombre = nombreSedeActiva,
                            s = s,
                            onReintentar = { viewModel.recargar() }
                        )
                        else -> PestanaAnalitica(
                            uiState = uiState,
                            periodo = etiquetaPeriodoVigente,
                            sedeNombre = nombreSedeActiva,
                            s = s,
                            onBuscarComprobante = { numero -> viewModel.buscarVentaParaDrillDown(numero) },
                            onVerDetalleVenta = { venta -> viewModel.seleccionarVenta(venta) },
                            onReintentar = { viewModel.recargar() },
                            onSeleccionarSede = { sedeId -> viewModel.seleccionarSede(sedeId) }
                        )
                    }
                }
            }
        }
    }

    // Modal de Auditoría / Drill-down
    if (ventaDetalle != null) {
        val devolucionesActuales = (uiState as? AnaliticaUiState.Exito)?.listaDevoluciones ?: emptyList()
        DialogoDetalleVenta(
            venta = ventaDetalle!!,
            devoluciones = devolucionesActuales,
            onCerrar = { viewModel.cerrarDetalleVenta() }
        )
    }
}
