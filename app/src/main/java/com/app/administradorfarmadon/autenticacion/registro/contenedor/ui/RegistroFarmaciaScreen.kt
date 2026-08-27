package com.app.administradorfarmadon.autenticacion.registro.contenedor.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import com.app.administradorfarmadon.autenticacion.registro.contenedor.logica.RegistroFarmaciaViewModel
import com.app.administradorfarmadon.compartido.util.SoporteContacto
import com.app.administradorfarmadon.autenticacion.registro.contenedor.ui.componentes.RegistroIncidenteBubble
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.ui.AddressPickerDialog
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.ui.MiniMapaConfirmacion
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.ui.PasoDatosNegocioForm
import com.app.administradorfarmadon.autenticacion.registro.paso2_plan.ui.ExitoRegistroScreen
import com.app.administradorfarmadon.autenticacion.registro.paso2_plan.ui.MinimalProgressLine
import com.app.administradorfarmadon.autenticacion.registro.paso2_plan.ui.PasoSeleccionPlan
import com.app.administradorfarmadon.disenotemaapp.ui.*
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.*
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistroFarmaciaScreen(
    viewModel: RegistroFarmaciaViewModel,
    onBackClick: () -> Unit,
    onRegistroExitoso: () -> Unit,
    onNavigateToLogin: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = recordarMedidaAdaptativa()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    
    val isImeVisible = WindowInsets.isImeVisible
    val dismissThresholdPx = with(density) { 90.dp.toPx() }
    var dragAccumulated by remember { mutableFloatStateOf(0f) }
    var ignoreDragByFocus by remember { mutableStateOf(false) }
    val isProgrammaticScroll = remember { mutableStateOf(false) }

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) dragAccumulated = 0f
    }
    
    LaunchedEffect(ignoreDragByFocus) {
        if (ignoreDragByFocus) {
            delay(700)
            ignoreDragByFocus = false
        }
    }

    val nestedScrollConnection = remember(isImeVisible, ignoreDragByFocus, isProgrammaticScroll.value) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput 
                    && isImeVisible 
                    && !ignoreDragByFocus
                    && !isProgrammaticScroll.value
                ) {
                    dragAccumulated += abs(available.y)
                    if (dragAccumulated > dismissThresholdPx) {
                        focusManager.clearFocus()
                        dragAccumulated = 0f
                    }
                }
                return Offset.Zero
            }
            
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || !isImeVisible) {
                    dragAccumulated = 0f
                }
                return Offset.Zero
            }
        }
    }

    var showMapPickerForField by remember { mutableStateOf<String?>(null) }
    var showRestoreDraftDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // BackHandler: prioridad -> cerrar incidentes, picker mapa, teclado, paso atrás, salida
    BackHandler(enabled = state.incidenteActual != null) { viewModel.descartarIncidente() }
    BackHandler(enabled = state.incidenteActual == null && showMapPickerForField != null) { showMapPickerForField = null }
    BackHandler(enabled = state.incidenteActual == null && showMapPickerForField == null && showRestoreDraftDialog) { showRestoreDraftDialog = false }
    BackHandler(enabled = state.incidenteActual == null && showMapPickerForField == null && !showRestoreDraftDialog && isImeVisible) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    BackHandler(enabled = state.incidenteActual == null && showMapPickerForField == null && !showRestoreDraftDialog && !isImeVisible) {
        if (state.pasoActual > 1) viewModel.previousStep() else onBackClick()
    }

    LaunchedEffect(state.pasoActual) {
        scrollState.scrollTo(0)
    }

    LaunchedEffect(Unit) {
        // Solo preguntar por el borrador si NO es una corrección
        if (!state.esCorreccion && viewModel.hayBorrador()) {
            showRestoreDraftDialog = true
        }
    }

    if (showRestoreDraftDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDraftDialog = false },
            containerColor = TokensFarmadon.colores.superficieElevada,
            shape = TokensFarmadon.formas.grande,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.RestorePage, null, tint = TokensFarmadon.colores.textoPrincipal)
                    Text("Registro Pendiente", style = TokensFarmadon.tipografia.titulo3, color = TokensFarmadon.colores.textoPrincipal)
                }
            },
            text = {
                Text(
                    "Detectamos que tienes un registro iniciado. ¿Deseas continuar desde donde lo dejaste o empezar de cero?",
                    style = TokensFarmadon.tipografia.cuerpo,
                    color = TokensFarmadon.colores.textoSecundario
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDraftDialog = false
                        viewModel.restaurarDesdeBorrador()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TokensFarmadon.colores.botonPrimarioFondo, 
                        contentColor = TokensFarmadon.colores.botonPrimarioTexto
                    ),
                    shape = TokensFarmadon.formas.mediana
                ) {
                    Text("CONTINUAR", style = TokensFarmadon.tipografia.etiqueta)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreDraftDialog = false
                        viewModel.limpiarBorradorYReiniciar()
                    }
                ) {
                    Text("EMPEZAR DE CERO", style = TokensFarmadon.tipografia.etiqueta, color = TokensFarmadon.colores.estadoPeligro)
                }
            }
        )
    }

    if (showMapPickerForField != null) {
        AddressPickerDialog(
            onAddressSelected = { address, lat, lng ->
                val field = showMapPickerForField
                if (field != null) {
                    if (field == "direccion") {
                        viewModel.onAddressSelected(address, lat, lng)
                    } else {
                        viewModel.onFieldChanged(field, address)
                    }
                }
                showMapPickerForField = null
            },
            onDismiss = { showMapPickerForField = null }
        )
    }

    Scaffold(
        topBar = {
            if (!state.exitoso) {
                TopAppBar(
                    title = { 
                        Text(
                            if (state.esCorreccion) "CORRECCIÓN DE EXPEDIENTE" else "REGISTRO",
                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = TokensFarmadon.colores.textoSecundario
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (state.pasoActual > 1) viewModel.previousStep()
                            else onBackClick()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = TokensFarmadon.colores.textoTerciario)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        containerColor = FDColors.Background,
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // 1. Fondo Adaptativo Gradiente Mate — tokens adaptativos (sin Color fijo)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = if (FDColors.isDark) {
                            listOf(FDColors.Surface, FDColors.Background)
                        } else {
                            listOf(FDColors.SurfaceElevated, FDColors.Background)
                        },
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.maxDimension * 0.8f
                    )
                )

                // 2. Foco de Luz Atmosférico — adaptativo
                drawRect(
                    brush = Brush.linearGradient(
                        colors = if (FDColors.isDark) {
                            listOf(FDColors.TextPrimary.copy(alpha = 0.08f), FDColors.Primary.copy(alpha = 0.03f), Color.Transparent)
                        } else {
                            listOf(FDColors.TextPrimary.copy(alpha = 0.03f), FDColors.Primary.copy(alpha = 0.01f), Color.Transparent)
                        },
                        start = Offset(size.width, size.height * 0.5f),
                        end = Offset(size.width * 0.3f, size.height * 0.5f)
                    )
                )
            },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            if (state.exitoso) {
                ExitoRegistroScreen(
                    onNavigateToLogin = onNavigateToLogin,
                    email = state.paso1.email,
                    s = s,
                    esCorreccion = state.esCorreccion
                )
            } else if (state.precargandoCorreccion) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = TokensFarmadon.colores.textoPrincipal,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            "Cargando expediente observado...",
                            style = TokensFarmadon.tipografia.cuerpoPequeno,
                            color = TokensFarmadon.colores.textoTerciario
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = s.padScreenH),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!state.esCorreccion) {
                        MinimalProgressLine(pasoActual = state.pasoActual, s = s)
                    } else {
                        Spacer(modifier = Modifier.height(s.gapLarge))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 900.dp)
                    ) {
                        AnimatedContent(
                            targetState = state.pasoActual,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                                } else {
                                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                                }
                            },
                            label = "WizardStepTransition"
                        ) { step ->
                            when (step) {
                                1 -> Column(modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        if (state.esCorreccion) "Corrige los datos observados" else "Paso 1: Datos del negocio",
                                        style = TokensFarmadon.tipografia.titulo1,
                                        color = TokensFarmadon.colores.textoPrincipal
                                    )
                                    if (state.esCorreccion) {
                                        Text(
                                            "Modifica los campos señalados por la central para continuar con la aprobación.",
                                            style = TokensFarmadon.tipografia.cuerpoPequeno,
                                            color = TokensFarmadon.colores.textoTerciario,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                        )
                                    }

                                    val mapVisible = state.paso1.latitud != null && state.paso1.longitud != null

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .nestedScroll(nestedScrollConnection),
                                        horizontalArrangement = Arrangement.spacedBy(s.gapXXLarge)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(if (mapVisible) 1.2f else 1f)
                                                .widthIn(max = 900.dp)
                                                .imePadding()
                                                .verticalScroll(scrollState),
                                            verticalArrangement = Arrangement.spacedBy(s.gapLarge)
                                        ) {
                                            PasoDatosNegocioForm(
                                                state = state.paso1,
                                                esCorreccion = state.esCorreccion,
                                                motivoRechazo = state.motivoRechazo,
                                                camposACorregir = state.camposACorregir,
                                                onFieldChanged = viewModel::onFieldChanged,
                                                onPaisSeleccionado = { iso, moneda, simbolo ->
                                                    viewModel.setPaisSeleccionado(iso, moneda, simbolo)
                                                },
                                                onNextStep = viewModel::nextStep,
                                                s = s,
                                                onMapClick = { showMapPickerForField = it },
                                                isProgrammaticScroll = isProgrammaticScroll,
                                                onFocus = {
                                                    dragAccumulated = 0f
                                                    ignoreDragByFocus = true
                                                }
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = mapVisible,
                                            enter = expandHorizontally() + fadeIn(),
                                            exit = shrinkHorizontally() + fadeOut(),
                                            modifier = Modifier.weight(0.8f)
                                        ) {
                                            if (mapVisible) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .widthIn(max = 420.dp)
                                                        .padding(top = s.gapLarge),
                                                    verticalArrangement = Arrangement.spacedBy(s.gapLarge)
                                                ) {
                                                    MiniMapaConfirmacion(
                                                        lat = state.paso1.latitud!!,
                                                        lng = state.paso1.longitud!!,
                                                        direccion = state.paso1.direccion,
                                                        onLocationManual = { lat, lng -> viewModel.onLocationManual(lat, lng) }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = !isImeVisible && state.incidenteActual == null,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .navigationBarsPadding()
                                                .padding(top = s.gapLarge)
                                        ) {
                                            Button(
                                                onClick = { viewModel.nextStep() },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(s.btnLargeH)
                                                    .bounceClick(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = TokensFarmadon.colores.botonPrimarioFondo,
                                                    contentColor = TokensFarmadon.colores.botonPrimarioTexto
                                                ),
                                                shape = TokensFarmadon.formas.completa
                                            ) {
                                                Text(
                                                    if (state.esCorreccion && state.camposACorregir.none { it == "planId" || it == "plan" })
                                                        "ENVIAR CORRECCIÓN"
                                                    else
                                                        "CONTINUAR",
                                                    style = TokensFarmadon.tipografia.titulo3.copy(fontWeight = FontWeight.Bold),
                                                    color = TokensFarmadon.colores.botonPrimarioTexto
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(s.gapXXLarge))
                                }
                                2 -> PasoSeleccionPlan(
                                    state = state.paso2,
                                    esCorreccion = state.esCorreccion,
                                    sugerirCambioPlan = state.sugerirCambioPlan,
                                    accionSugerida = state.accionSugerida,
                                    camposACorregir = state.camposACorregir,
                                    onPlanSelected = viewModel::onPlanSelected,
                                    onNextStep = viewModel::nextStep,
                                    onRetryCargaPlanes = viewModel::reintentarCargaPlanes,
                                    onBackClick = viewModel::previousStep,
                                    cargando = state.cargando,
                                    resumenNombre = state.paso1.nombreFarmacia,
                                    resumenRuc = state.paso1.ruc,
                                    resumenEmail = state.paso1.email,
                                    paisNombre = state.paso1.paisIso,
                                    s = s,
                                    incidenteVisible = state.incidenteActual != null
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = state.incidenteActual != null,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(s.gapXXLarge)
                        .padding(bottom = 16.dp)
                ) {
                    state.incidenteActual?.let { incidente ->
                        RegistroIncidenteBubble(
                            incidente = incidente,
                            onAccionPrincipal = { accion ->
                                viewModel.ejecutarAccionIncidente(
                                    accion = accion,
                                    navigateToLogin = { onNavigateToLogin(state.paso1.email) },
                                    onSoporteClick = { incident ->
                                        SoporteContacto.abrir(
                                            context = context,
                                            origen = "Registro",
                                            tipoIncidente = incident.tipo.toString(),
                                            tituloIncidente = incident.titulo,
                                            mensaje = incident.mensaje,
                                            usuario = state.paso1.email
                                        )
                                    }
                                )
                            },
                            onAccionSecundaria = { accion ->
                                viewModel.ejecutarAccionIncidente(
                                    accion = accion,
                                    navigateToLogin = { onNavigateToLogin(state.paso1.email) },
                                    onSoporteClick = { incident ->
                                        SoporteContacto.abrir(
                                            context = context,
                                            origen = "Registro",
                                            tipoIncidente = incident.tipo.toString(),
                                            tituloIncidente = incident.titulo,
                                            mensaje = incident.mensaje,
                                            usuario = state.paso1.email
                                        )
                                    }
                                )
                            },
                            onDismiss = { viewModel.descartarIncidente() }
                        )
                    }
                }

                CargandoOverlay(
                    cargando = state.cargando && (state.progresoEnvio > 0f || viewModel.enviando),
                    esCorreccion = state.esCorreccion,
                    progreso = state.progresoEnvio,
                    mensajeProgreso = state.mensajeProgreso,
                    s = s
                )
            }
        }
    }
}

@Composable
fun CargandoOverlay(
    cargando: Boolean,
    esCorreccion: Boolean,
    progreso: Float,
    mensajeProgreso: String,
    s: MedidaAdaptativa
) {
    AnimatedVisibility(
        visible = cargando,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TokensFarmadon.colores.velo)
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .border(1.dp, TokensFarmadon.colores.bordeSutil, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = TokensFarmadon.colores.superficieDefecto
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "iconPulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer(
                                scaleX = pulseScale,
                                scaleY = pulseScale,
                                alpha = pulseAlpha
                            )
                            .background(TokensFarmadon.colores.estadoExito.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = TokensFarmadon.colores.estadoExito,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = if (esCorreccion) "ENVIANDO CORRECCIÓN" else "ENVIANDO SOLICITUD",
                        color = TokensFarmadon.colores.textoPrincipal,
                        style = TokensFarmadon.tipografia.etiqueta.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 12.sp
                        )
                    )

                    Text(
                        text = "Conectando con la central. Por favor, no cierres la aplicación.",
                        color = TokensFarmadon.colores.textoSecundario,
                        style = TokensFarmadon.tipografia.cuerpoPequeno,
                        textAlign = TextAlign.Center
                    )

                    val animatedProgress by animateFloatAsState(
                        targetValue = progreso,
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                        label = "progressBar"
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(TokensFarmadon.colores.bordeSutil)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                TokensFarmadon.colores.estadoExito.copy(alpha = 0.7f),
                                                TokensFarmadon.colores.estadoExito
                                            )
                                        )
                                    )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mensajeProgreso,
                                color = TokensFarmadon.colores.textoTerciario,
                                style = TokensFarmadon.tipografia.leyenda
                            )
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                color = TokensFarmadon.colores.estadoExito,
                                style = TokensFarmadon.tipografia.leyenda.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
