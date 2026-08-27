package com.app.administradorfarmadon.configuracion.plan.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.configuracion.plan.datos.BrixoCanalesPagoInfo
import com.app.administradorfarmadon.configuracion.plan.datos.HistorialPagoItem
import com.app.administradorfarmadon.configuracion.plan.datos.PlanFacturacionInfo
import com.app.administradorfarmadon.configuracion.plan.logica.PlanFacturacionViewModel
import com.app.administradorfarmadon.configuracion.plan.ui.componentes.*
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.suscripcion.ReportarPagoManager
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import java.util.Calendar

@Composable
fun GestionPlanScreen(
    onVolver: () -> Unit,
    viewModel: PlanFacturacionViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colores = TokensFarmadon.colores
    val s = recordarMedidaAdaptativa()
    var rootY by remember { mutableFloatStateOf(0f) }
    var seccionSeleccionada by remember { mutableIntStateOf(0) }
    val secciones = listOf(
        SeccionMenu("MI PLAN", Icons.Default.Diamond),
        SeccionMenu("FACTURACIÓN", Icons.AutoMirrored.Filled.ReceiptLong),
        SeccionMenu("CÓMO PAGAR", Icons.Default.AccountBalance)
    )

    var mostrandoAsentamiento by remember { mutableStateOf(false) }
    // Teclado primero: si el ime está visible, lo oculta antes de navegar
    val focusGes = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardGes = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val densityGes = androidx.compose.ui.platform.LocalDensity.current
    val isKeyboardVisibleGes = WindowInsets.ime.getBottom(densityGes) > 0
    BackHandler(enabled = true) {
        if (isKeyboardVisibleGes) { keyboardGes?.hide(); focusGes.clearFocus(force = true) }
        else if (mostrandoAsentamiento) mostrandoAsentamiento = false
        else onVolver()
    }
    var ySeleccionada by remember { mutableFloatStateOf(0f) }
    var yDetalle by remember { mutableFloatStateOf(0f) }
    val yAnimada by animateFloatAsState(
        targetValue = ySeleccionada,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "CableY"
    )

    val nombreFarmacia = SessionManager.nombreUsuario.ifBlank { "Mi Farmacia" }
    val rucFarmacia = SessionManager.dni.ifBlank { "" }
    if (uiState.mostrarModalPlanes) {
        CatalogoPlanesModal(
            planes = uiState.catalogoPlanes,
            planActualNombre = uiState.planInfo.planNombre,
            whatsappCobranzas = uiState.canalesPago.whatsappCobranzas,
            nombreFarmacia = nombreFarmacia,
            rucFarmacia = rucFarmacia,
            catalogoError = uiState.catalogoError,
            onDismiss = { viewModel.cerrarModalPlanes() }
        )
    }

    uiState.pagoSeleccionado?.let { pago ->
        ConstanciaPagoDialog(
            pago = pago,
            nombreFarmacia = nombreFarmacia,
            rucFarmacia = rucFarmacia,
            emisorRazonSocial = uiState.canalesPago.razonSocial,
            emisorRuc = uiState.canalesPago.ruc,
            onDismiss = { viewModel.seleccionarPagoParaDetalle(null) }
        )
    }

    BoxWithConstraints(modifier = modifier
            .fillMaxSize()
            .background(colores.fondoBase)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .onGloballyPositioned { coords -> rootY = coords.positionInRoot().y }
    ) {
        val anchoBox = maxWidth.value
        val padH = s.padScreenH
        val padV = s.padScreenV
        val gapCol = s.gapColumnas(anchoBox)
        val anchoRiel = s.anchoRiel(anchoBox)
        Box(Modifier.fillMaxSize().padding(horizontal = padH, vertical = padV)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(s.gapMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.gapMedium)) {
                    IconButton(
                        onClick = onVolver,
                        modifier = Modifier.size(s.btnSmallH * 1.15f).clip(RoundedCornerShape(s.radiusButton)).background(colores.cardElevada).border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusButton))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconSmall))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("CENTRO FINANCIERO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, letterSpacing = 1.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario)
                        Text("Mi Plan y Facturación", style = TokensFarmadon.tipografia.titulo1.copy(fontSize = s.textTitle.value.sp), color = colores.textoPrincipal)
                    }
                }
            }

            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(gapCol)) {
                Column(modifier = Modifier.width(anchoRiel * 0.82f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(s.xs)) {
                    secciones.forEachIndexed { index, seccion ->
                        val activa = seccionSeleccionada == index
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(s.btnLargeH).clip(RoundedCornerShape(s.radiusButton))
                                .onGloballyPositioned { if (activa) ySeleccionada = it.positionInRoot().y - rootY + (it.size.height / 2f) }
                                .clickable { seccionSeleccionada = index; if (index != 2) mostrandoAsentamiento = false }
                                .bounceClick(),
                            color = if (activa) colores.textoPrincipal.copy(alpha = 0.08f) else Color.Transparent,
                            shape = RoundedCornerShape(s.radiusButton),
                            border = if (activa) androidx.compose.foundation.BorderStroke(s.borderWidth * 1.2f, colores.textoPrincipal) else null
                        ) {
                            Row(modifier = Modifier.padding(horizontal = s.padCard * 0.85f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                                Icon(seccion.icono, null, tint = if (activa) colores.textoPrincipal else colores.textoTerciario, modifier = Modifier.size(s.iconSmall))
                                Text(seccion.titulo, style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = if (activa) FontWeight.Black else FontWeight.Bold, fontSize = s.textLabel.value.sp, letterSpacing = 0.4.sp), color = if (activa) colores.textoPrincipal else colores.textoTerciario)
                                if (activa) { Spacer(Modifier.weight(1f)); Box(Modifier.size(s.xs * 0.75f).clip(CircleShape).background(colores.textoPrincipal)) }                            }
                        }
                    }
                }

                Box(modifier = Modifier.width(gapCol).fillMaxHeight()) {
                    VinculoElectricoBrixo(yInicio = yAnimada, yFin = yDetalle, colorBase = colores.cardBorde, colorEnergia = colores.textoPrincipal, s = s)
                }

                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight().onGloballyPositioned { yDetalle = it.positionInRoot().y - rootY + s.padCard.value * 2.5f },
                    shape = RoundedCornerShape(s.radiusCard * 1.5f),
                    color = colores.cardBase,
                    border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde.copy(alpha = 0.5f))
                ) {
                    if (uiState.cargando && !mostrandoAsentamiento) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = colores.textoPrincipal, modifier = Modifier.size(s.iconLarge)) }
                    } else {
                        when (seccionSeleccionada) {
                            0 -> {
                                Column(modifier = Modifier.fillMaxSize().padding(s.padCardLarge).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(s.gapMedium)) {
                                    PlanCronogramaCard(planInfo = uiState.planInfo, onExplorarPlanes = { viewModel.abrirModalPlanes() })
                                    PlanCapacidadesCard(planInfo = uiState.planInfo)
                                }
                            }
                            1 -> FacturacionAuditoriaPanel(uiState.planInfo, uiState.historialPagos, { viewModel.seleccionarPagoParaDetalle(it) }, s)
                            2 -> Box(Modifier.fillMaxSize().padding(s.padCardLarge)) {
                                // REGLA DE LA VENTANA DE PAGO: el reporte de voucher
                                // solo abre cuando el contrato está POR VENCER (≤5 días)
                                // o VENCIDO. El ciclo de arranque que BRIXO otorga al
                                // nacer NO se paga ni se reporta — ya tiene su
                                // constancia ALTA automática con plan, monto y vigencia.
                                val estadoSus = uiState.planInfo.estadoSuscripcion
                                val ventanaAbierta = estadoSus == "por_vencer" || estadoSus == "vencida"
                                AnimatedContent(targetState = ventanaAbierta, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "VentanaPago") { abierta ->
                                    if (abierta) {
                                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                            BrixoCanalesPagoCard(canalesPago = uiState.canalesPago, nombreFarmacia = nombreFarmacia, rucFarmacia = rucFarmacia, error = uiState.canalesError, onIniciarAsentamiento = {
                                                // Puerta ÚNICA y REAL: abre el diálogo que
                                                // sí escribe en solicitudes_pago.
                                                ReportarPagoManager.abrirDialogoManual()
                                            })
                                        }
                                    } else {
                                        Column(
                                            Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(Icons.Default.LockClock, null, tint = colores.textoTerciario, modifier = Modifier.size(44.dp))
                                            Text(
                                                if (estadoSus == "prueba") "Estás en tu período de prueba"
                                                else if (estadoSus == "sin_suscripcion") "Aún no tienes un contrato activo"
                                                else "Tu ciclo está vigente",
                                                style = TokensFarmadon.tipografia.titulo3.copy(fontWeight = FontWeight.Bold),
                                                color = colores.textoPrincipal,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Text(
                                                when (estadoSus) {
                                                    "prueba" -> "Disfruta tu prueba: no requiere pago.\nLa ventana para reportar pagos abrirá al concluirla."
                                                    else -> "Vence el ${uiState.planInfo.fechaFin.ifBlank { "—" }}.\n" +
                                                        "La ventana para subir tu voucher abrirá " +
                                                        "los últimos 5 días o al vencer."
                                                },
                                                style = TokensFarmadon.tipografia.cuerpoPequeno,
                                                color = colores.textoTerciario,
                                                lineHeight = 18.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        } // inner padded Box
    } // BoxWithConstraints
}

@Composable
fun FacturacionAuditoriaPanel(planInfo: PlanFacturacionInfo, historial: List<HistorialPagoItem>, onVerConstancia: (HistorialPagoItem) -> Unit, s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa = recordarMedidaAdaptativa()) {
    val colores = TokensFarmadon.colores
    val calendar = Calendar.getInstance()
    val mesActual = calendar.get(Calendar.MONTH)
    val anoActual = calendar.get(Calendar.YEAR)
    
    var mesSeleccionado by remember { mutableIntStateOf(mesActual) }
    var anoSeleccionado by remember { mutableIntStateOf(anoActual) }
    
    val meses = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
    val anosDisponibles = (2024..anoActual).toList().reversed()

    val mesesPermitidos = if (anoSeleccionado == anoActual) meses.take(mesActual + 1) else meses
    
    LaunchedEffect(anoSeleccionado) {
        if (anoSeleccionado == anoActual && mesSeleccionado > mesActual) {
            mesSeleccionado = mesActual
        }
    }

    val historialFiltrado = remember(historial, mesSeleccionado, anoSeleccionado) {
        historial.filter { item ->
            val partes = item.fecha.split("/")
            if (partes.size == 3) {
                val m = partes[1].toIntOrNull() ?: -1
                val a = partes[2].toIntOrNull() ?: -1
                m == (mesSeleccionado + 1) && a == anoSeleccionado
            } else false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().background(colores.cardBase).padding(horizontal = s.padCardLarge, vertical = s.padCard), verticalArrangement = Arrangement.spacedBy(s.gapMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconMedium))
                Text("HISTORIAL Y AUDITORÍA", style = TokensFarmadon.tipografia.titulo2.copy(fontSize = s.textSubtitle.value.sp), color = colores.textoPrincipal)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                Surface(modifier = Modifier.weight(1f), color = colores.fondoBase, shape = RoundedCornerShape(s.radiusInput), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)) {
                    Column(Modifier.padding(s.sm)) {
                        Text("TOTAL PAGADO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f), color = colores.textoTerciario)
                        Text("${planInfo.monedaSimbolo} ${"%.2f".format(planInfo.precioPagado)}", style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp), color = colores.textoPrincipal)
                    }
                }
                Surface(modifier = Modifier.weight(1f), color = colores.fondoBase, shape = RoundedCornerShape(s.radiusInput), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)) {
                    Column(Modifier.padding(s.sm)) {
                        Text("SALDO PENDIENTE", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f), color = if (planInfo.saldoPendiente > 0) colores.estadoPeligro else colores.textoTerciario)
                        Text("${planInfo.monedaSimbolo} ${"%.2f".format(planInfo.saldoPendiente)}", style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp), color = if (planInfo.saldoPendiente > 0) colores.estadoPeligro else colores.textoPrincipal)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                SelectorPeriodo(label = "MES", opciones = mesesPermitidos, seleccionado = meses[mesSeleccionado], onSeleccion = { mesSeleccionado = meses.indexOf(it) }, modifier = Modifier.weight(1.5f), s = s)
                SelectorPeriodo(label = "AÑO", opciones = anosDisponibles.map { it.toString() }, seleccionado = anoSeleccionado.toString(), onSeleccion = { anoSeleccionado = it.toInt() }, modifier = Modifier.weight(1f), s = s)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = s.padCardLarge)) {
            if (historialFiltrado.isEmpty()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.History, null, tint = colores.textoTerciario.copy(alpha = 0.3f), modifier = Modifier.size(s.iconLarge * 1.5f))
                    Spacer(Modifier.height(s.sm))
                    Text("No hay movimientos registrados para ${meses[mesSeleccionado]} $anoSeleccionado", style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp), color = colores.textoTerciario)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(s.xs), contentPadding = PaddingValues(bottom = s.gapMedium)) {
                    items(historialFiltrado) { item -> ItemFacturaAuditoria(item, onVerConstancia, s) }
                }
            }
        }

        Surface(modifier = Modifier.fillMaxWidth().padding(s.padCard), color = colores.cardElevada, shape = RoundedCornerShape(s.radiusCard), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)) {
            Row(Modifier.padding(s.padCard), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                    Icon(Icons.Default.SupportAgent, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconSmall))
                    Column {
                        Text("SOPORTE COMERCIAL BRIXO", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp), color = colores.textoPrincipal)
                        Text("¿Dudas con tu facturación? Contáctanos.", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.92f), color = colores.textoSecundario)
                    }
                }
                Button(onClick = { /* WhatsApp logic here */ }, colors = ButtonDefaults.buttonColors(containerColor = colores.textoPrincipal), shape = RoundedCornerShape(s.radiusChip)) {
                    Text("HABLAR CON ASESOR", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp))
                }
            }
        }
    }
}

@Composable
fun AsentamientoPagoPanel(
    canalesPago: BrixoCanalesPagoInfo,
    enviando: Boolean,
    onCancelar: () -> Unit,
    onEnviar: (String, String, String, android.net.Uri?, String) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa = recordarMedidaAdaptativa()
) {
    val colores = TokensFarmadon.colores
    var bancoSeleccionado by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var numeroOperacion by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    var uriImagen by remember { mutableStateOf<android.net.Uri?>(null) }
    var menuBancosExpandido by remember { mutableStateOf(false) }

    val launcherImagen = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uriImagen = uri }
    val cuentaVerificada = remember(bancoSeleccionado) { canalesPago.metodosActivos.find { it.bancoNombre == bancoSeleccionado } }

    Column(modifier = Modifier.fillMaxWidth().imePadding(), verticalArrangement = Arrangement.spacedBy(s.gapMedium)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ASENTAMIENTO DE PAGO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.88f, letterSpacing = 1.1.sp, fontWeight = FontWeight.Black), color = colores.textoTerciario)
                Text("Vincular Comprobante", style = TokensFarmadon.tipografia.titulo2.copy(fontSize = s.textTitle.value.sp * 0.92f), color = colores.textoPrincipal)
            }
            TextButton(onClick = onCancelar, enabled = !enviando) { Text("✕ CANCELAR", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp), color = colores.estadoPeligro) }
        }

        Surface(modifier = Modifier.fillMaxWidth().height(s.btnLargeH * 2.95f).clickable(enabled = !enviando) { launcherImagen.launch("image/*") }, color = colores.cardElevada, shape = RoundedCornerShape(s.radiusCard), border = androidx.compose.foundation.BorderStroke(s.borderWidth * 1.2f, if (uriImagen != null) colores.textoPrincipal else colores.cardBorde)) {
            Box(contentAlignment = Alignment.Center) {
                if (uriImagen != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(s.xs)) {
                        Icon(Icons.Default.Image, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconLarge))
                        Text("VOUCHER CARGADO", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp), color = colores.textoPrincipal)
                        TextButton(onClick = { uriImagen = null }, enabled = !enviando) { Text("CAMBIAR FOTO", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.92f), color = colores.textoTerciario) }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(Modifier.size(s.iconLarge * 1.95f).clip(CircleShape).background(colores.textoPrincipal.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AddAPhoto, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconMedium)) }
                        Spacer(Modifier.height(s.xs))
                        Text("ADJUNTAR FOTO DEL VOUCHER", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp), color = colores.textoPrincipal)
                        Text("JPG, PNG · Máx 5MB", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.9f), color = colores.textoTerciario)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(s.gapMedium)) {
            Column(modifier = Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(s.xs)) {
                Text("BANCO DE DESTINO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.88f, fontWeight = FontWeight.Bold), color = colores.textoTerciario)
                Box {
                    Surface(modifier = Modifier.fillMaxWidth().height(s.inputMinH).clickable(enabled = !enviando) { menuBancosExpandido = true }, color = colores.fondoBase, shape = RoundedCornerShape(s.radiusInput), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)) {
                        Row(Modifier.padding(horizontal = s.padCard * 0.75f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(bancoSeleccionado.ifBlank { "Seleccionar banco..." }, style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp), color = if (bancoSeleccionado.isEmpty()) colores.textoTerciario else colores.textoPrincipal)
                            Icon(Icons.Default.ArrowDropDown, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconSmall))
                        }
                    }
                    DropdownMenu(expanded = menuBancosExpandido, onDismissRequest = { menuBancosExpandido = false }, modifier = Modifier.background(colores.cardBase).width(s.anchoRiel(320f))) {
                        canalesPago.metodosActivos.map { it.bancoNombre }.distinct().forEach { banco ->
                            DropdownMenuItem(text = { Text(banco, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp)) }, onClick = { bancoSeleccionado = banco; menuBancosExpandido = false })
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(0.4f), verticalArrangement = Arrangement.spacedBy(s.xs)) {
                Text("MONTO ABONADO", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.88f, fontWeight = FontWeight.Bold), color = colores.textoTerciario)
                Surface(modifier = Modifier.fillMaxWidth().height(s.inputMinH), color = colores.fondoBase, shape = RoundedCornerShape(s.radiusInput), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)) {
                    Row(Modifier.padding(horizontal = s.padCard * 0.75f), verticalAlignment = Alignment.CenterVertically) {
                        Text(SessionManager.monedaSimbolo.ifBlank { "S/" }, style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textBody.value.sp), color = colores.textoTerciario)
                        Spacer(modifier = Modifier.width(s.xs))
                        BasicTextField(value = monto, onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) monto = it }, enabled = !enviando, textStyle = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp, color = colores.textoPrincipal), modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
            }
        }

        OutlinedTextField(
            value = numeroOperacion, onValueChange = { numeroOperacion = it }, enabled = !enviando, label = { Text("Número de Operación", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp)) }, placeholder = { Text("Código de voucher", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colores.textoPrincipal, unfocusedTextColor = colores.textoPrincipal, focusedBorderColor = colores.textoPrincipal, unfocusedBorderColor = colores.cardBorde)
        )

        AnimatedVisibility(visible = cuentaVerificada != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            cuentaVerificada?.let { cv ->
                Surface(modifier = Modifier.fillMaxWidth(), color = colores.fondoBase, shape = RoundedCornerShape(s.radiusInput), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.textoPrincipal.copy(alpha = 0.2f))) {
                    Column(Modifier.padding(s.padCard), verticalArrangement = Arrangement.spacedBy(s.xs)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs)) {
                            Icon(Icons.Default.VerifiedUser, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconSmall))
                            Text("CUENTA OFICIAL BRIXO", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp), color = colores.textoPrincipal)
                        }
                        Text("TITULAR: ${cv.titular}", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontWeight = FontWeight.Bold, fontSize = s.textBody.value.sp), color = colores.textoSecundario)
                        Text("CTA: ${cv.numeroCuenta}  ·  CCI: ${cv.numeroCci}", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp), color = colores.textoTerciario)
                    }
                }
            }
        }

        // Única acción primaria: notificar abono — altura s.btnLargeH
        Button(
            onClick = { onEnviar(bancoSeleccionado, monto, numeroOperacion, uriImagen, nota) },
            modifier = Modifier.fillMaxWidth().height(s.btnLargeH).bounceClick(),
            enabled = bancoSeleccionado.isNotBlank() && monto.isNotBlank() && !enviando,
            colors = ButtonDefaults.buttonColors(containerColor = colores.botonPrimarioFondo, contentColor = colores.botonPrimarioTexto),
            shape = RoundedCornerShape(s.radiusButton)
        ) {
            if (enviando) {
                CircularProgressIndicator(modifier = Modifier.size(s.iconSmall), color = colores.botonPrimarioTexto, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(s.iconSmall))
                Spacer(Modifier.width(s.xs))
                Text("NOTIFICAR ABONO A BRIXO", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp))
            }
        }
    }
}

@Composable
private fun SelectorPeriodo(label: String, opciones: List<String>, seleccionado: String, onSeleccion: (String) -> Unit, modifier: Modifier = Modifier, s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa = recordarMedidaAdaptativa()) {
    val colores = TokensFarmadon.colores
    var expandido by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(s.xs * 0.7f)) {
        Text(label, style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.Bold), color = colores.textoTerciario)
        Box {
            Surface(modifier = Modifier.fillMaxWidth().height(s.inputMinH * 0.92f).clickable { expandido = true }, color = colores.fondoBase, shape = RoundedCornerShape(s.radiusChip), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde) ) {
                Row(Modifier.padding(horizontal = s.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(seleccionado, style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp), color = colores.textoPrincipal); Icon(Icons.Default.ArrowDropDown, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconSmall))
                }
            }
            DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }, modifier = Modifier.background(colores.cardBase).width(200.dp)) {
                opciones.forEach { DropdownMenuItem(text = { Text(it, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp)) }, onClick = { onSeleccion(it); expandido = false }) }
            }
        }
    }
}

@Composable
private fun ItemFacturaAuditoria(item: HistorialPagoItem, onClick: (HistorialPagoItem) -> Unit, s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa = recordarMedidaAdaptativa()) {
    val colores = TokensFarmadon.colores
    Surface(modifier = Modifier.fillMaxWidth().clickable { onClick(item) }, color = colores.cardElevada, shape = RoundedCornerShape(s.radiusInput), border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde.copy(alpha = 0.5f))) {
        Row(Modifier.padding(s.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                Box(Modifier.size(s.iconLarge).background(colores.fondoBase, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = colores.estadoExito, modifier = Modifier.size(s.iconSmall)) }
                Column {
                    Text(item.concepto, style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textBody.value.sp), color = colores.textoPrincipal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${item.fecha} · Operación: ${item.numeroOperacion.ifBlank { "N/A" }}", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp * 0.92f), color = colores.textoSecundario)
                }
            }
            Text("${SessionManager.monedaSimbolo.ifBlank { "S/" }} ${"%.2f".format(item.monto)}", style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp * 0.95f, fontWeight = FontWeight.Black), color = colores.textoPrincipal)
        }    }
}

data class SeccionMenu(val titulo: String, val icono: ImageVector)
@Composable
fun VinculoElectricoBrixo(yInicio: Float, yFin: Float, colorBase: Color, colorEnergia: Color, s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa = recordarMedidaAdaptativa()) {
    val infiniteTransition = rememberInfiniteTransition(label = "Energia")
    val fasePulso by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "Pulso")
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val startY = yInicio - 64.dp.toPx() 
        val endY = yFin - 64.dp.toPx()
        val path = Path().apply { moveTo(0f, startY); cubicTo(width * 0.4f, startY, width * 0.6f, endY, width, endY) }
        drawPath(path = path, color = colorBase.copy(alpha = 0.3f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        val pathMeasure = PathMeasure(); pathMeasure.setPath(path, false); val totalLength = pathMeasure.length
        val segmentLength = 40.dp.toPx(); val offset = fasePulso * (totalLength + segmentLength) - segmentLength
        val segmentPath = Path(); pathMeasure.getSegment(startDistance = offset.coerceAtLeast(0f), stopDistance = (offset + segmentLength).coerceAtMost(totalLength), destination = segmentPath)
        drawPath(path = segmentPath, brush = Brush.horizontalGradient(colors = listOf(colorEnergia.copy(alpha = 0f), colorEnergia, colorEnergia.copy(alpha = 0f)), startX = offset.coerceAtLeast(0f), endX = (offset + segmentLength).coerceAtMost(totalLength)), style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path = segmentPath, color = colorEnergia.copy(alpha = 0.2f), style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
    }
}