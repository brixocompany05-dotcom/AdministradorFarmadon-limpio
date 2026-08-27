package com.app.administradorfarmadon.autenticacion.login.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.login.datos.*
import com.app.administradorfarmadon.autenticacion.login.logica.*
import com.app.administradorfarmadon.autenticacion.login.ui.componentes.ExecutiveInput
import com.app.administradorfarmadon.autenticacion.login.ui.componentes.LoginIncidenteBubble
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.ThemeViewModel
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.compartido.util.SoporteContacto
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    themeViewModel: ThemeViewModel,
    emailPrellenado: String? = null,
    onNavigateToRegistro: () -> Unit = {},
    onNavigateToRegistroCorrection: (String) -> Unit = {},
    onNavigateToExpediente: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val s = recordarMedidaAdaptativa()
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.isImeVisible
    val isProgrammaticScroll = remember { mutableStateOf(false) }
    val dismissThresholdPx = with(density) { 90.dp.toPx() }
    var dragAccumulated by remember { mutableFloatStateOf(0f) }
    var ignoreDragByFocus by remember { mutableStateOf(false) }

    LaunchedEffect(isImeVisible) { if (!isImeVisible) dragAccumulated = 0f }
    LaunchedEffect(ignoreDragByFocus) {
        if (ignoreDragByFocus) {
            kotlinx.coroutines.delay(700)
            ignoreDragByFocus = false
        }
    }
    LaunchedEffect(isProgrammaticScroll.value) {
        if (isProgrammaticScroll.value) {
            ignoreDragByFocus = true
            dragAccumulated = 0f
        }
    }

    val userFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // ── BackHandler: overlays primero, luego teclado, luego navegación ──
    BackHandler(enabled = uiState.incidente != null) {
        viewModel.descartarIncidente()
    }
    BackHandler(enabled = uiState.incidente == null && uiState.estadoSolicitud != null) {
        viewModel.descartarEstadoSolicitud()
    }
    BackHandler(enabled = uiState.incidente == null && uiState.estadoSolicitud == null && uiState.mensajeDialogo != null) {
        viewModel.descartarDialogo()
    }
    BackHandler(enabled = uiState.incidente == null && uiState.estadoSolicitud == null && uiState.mensajeDialogo == null && isImeVisible) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val nestedScrollConnection = remember(isImeVisible, ignoreDragByFocus, isProgrammaticScroll.value) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && isImeVisible && !ignoreDragByFocus && !isProgrammaticScroll.value) {
                    dragAccumulated += abs(available.y)
                    if (dragAccumulated > dismissThresholdPx) {
                        focusManager.clearFocus()
                        dragAccumulated = 0f
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || !isImeVisible) dragAccumulated = 0f
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(emailPrellenado) {
        emailPrellenado?.let {
            viewModel.onUsuarioChange(it)
            passwordFocusRequester.requestFocus()
        }
    }

    // Autofoco: foco inmediato en correo si no hay prellenado; delay corto evita carrera con NavHost
    LaunchedEffect(Unit) {
        if (emailPrellenado.isNullOrBlank()) {
            kotlinx.coroutines.delay(250)
            try { userFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Scaffold(containerColor = FDColors.Background, contentWindowInsets = WindowInsets.systemBars) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // 1. Fondo Adaptativo Gradiente Mate
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

                    // 2. Foco de Luz Atmosférico
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
                }
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ═══════════════════════════════════════════════════════
                // IDENTIDAD FARMADON (LADO IZQUIERDO)
                // ═══════════════════════════════════════════════════════
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = s.padScreenH * 3.2f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "VIORA",
                        style = TokensFarmadon.tipografia.visual.copy(
                            fontFamily = InterPremium,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Thin,
                            letterSpacing = 14.sp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    FDColors.TextPrimary,
                                    FDColors.TextPrimary.copy(alpha = 0.4f)
                                )
                            )
                        ),
                        modifier = Modifier.graphicsLayer(alpha = 0.99f)
                    )
                    Text(
                        text = "BY BRIXO",
                        style = TokensFarmadon.tipografia.etiqueta.copy(
                            fontFamily = InterPremium,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp,
                            color = FDColors.Success
                        ),
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                    )
                }

                // ═══════════════════════════════════════════════════════
                // CONSOLA DE ACCESO (LADO DERECHO)
                // ═══════════════════════════════════════════════════════
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.45f)
                        .navigationBarsPadding()
                        .imePadding()
                        .nestedScroll(nestedScrollConnection)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = s.padScreenH * 2.2f, vertical = s.padScreenV),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isImeVisible) Spacer(modifier = Modifier.height(s.gapLarge))

                    Surface(
                        modifier = Modifier
                            .widthIn(max = 440.dp)
                            .fillMaxWidth(),
                        color = FDColors.SurfaceElevated.copy(alpha = if (FDColors.isDark) 0.85f else 0.98f),
                        shape = RoundedCornerShape(s.radiusSheet),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (FDColors.isDark) {
                                Brush.verticalGradient(colors = listOf(FDColors.TextPrimary.copy(alpha = 0.15f), Color.Transparent))
                            } else {
                                SolidColor(FDColors.Border)
                            }
                        ),
                        shadowElevation = if (FDColors.isDark) 32.dp else 12.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = s.padModule, vertical = s.padModule * 1.4f)
                        ) {
                            when (uiState.estadoPantalla) {
                                LoginScreenState.LOGIN -> {
                                    LoginContent(
                                        s = s,
                                        uiState = uiState,
                                        userFocusRequester = userFocusRequester,
                                        passwordFocusRequester = passwordFocusRequester,
                                        isProgrammaticScroll = isProgrammaticScroll,
                                        onUsuarioChange = viewModel::onUsuarioChange,
                                        onContrasenaChange = viewModel::onContrasenaChange,
                                        onIngresarClick = { viewModel.iniciarSesion() },
                                        onRecuperarContrasena = { viewModel.recuperarContrasena() },
                                        onNavigateToRegistro = onNavigateToRegistro,
                                        onNavigateToExpediente = onNavigateToExpediente
                                    )
                                }
                                LoginScreenState.ACCESO_RESTRINGIDO -> {
                                    AccesoRestringidoContent(
                                        s = s,
                                        message = uiState.mensajeRestringido.orEmpty(),
                                        onVolverClick = { viewModel.volverALogin() }
                                    )
                                }
                            }
                        }
                    }

                    if (isImeVisible) Spacer(modifier = Modifier.height(s.gapLarge))
                }
            }

            // ═══════════════════════════════════════════════════════
            // OVERLAYS Y ESTADOS DE SOLICITUD / INCIDENTES
            // ═══════════════════════════════════════════════════════
            if (uiState.cargando && uiState.incidente == null && uiState.estadoSolicitud == null) {
                LoadingOverlay(s)
            }

            uiState.estadoSolicitud?.let { sol ->
                SolicitudEstadoOverlay(
                    estado = sol,
                    onCerrar = { viewModel.descartarEstadoSolicitud() },
                    onCorregir = { onNavigateToRegistroCorrection(sol.uid) },
                    modifier = Modifier.padding(s.padModule)
                )
            }

            uiState.mensajeDialogo?.let { (titulo, mensaje) ->
                AlertDialog(
                    onDismissRequest = { viewModel.descartarDialogo() },
                    modifier = Modifier.border(1.dp, FDColors.TextPrimary.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                    containerColor = FDColors.SurfaceElevated,
                    shape = RoundedCornerShape(20.dp),
                    title = {
                        Text(titulo, style = TokensFarmadon.tipografia.titulo3, color = FDColors.TextPrimary)
                    },
                    text = {
                        Text(mensaje, style = TokensFarmadon.tipografia.cuerpo, color = FDColors.TextSecondary)
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.descartarDialogo() },
                            colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ENTENDIDO", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = uiState.incidente != null,
                enter = fadeIn(tween(350)),
                exit = fadeOut(tween(250))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FDColors.Overlay)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    uiState.incidente?.let { incidente ->
                        LoginIncidenteBubble(
                            incidente = incidente,
                            onAccionPrincipal = {
                                viewModel.ejecutarAccionIncidente(
                                    incidente = incidente,
                                    onIrARegistro = onNavigateToRegistro,
                                    onContactarSoporte = { incident ->
                                        SoporteContacto.abrir(
                                            context = context,
                                            origen = "Login",
                                            tipoIncidente = incident.tipo.toString(),
                                            tituloIncidente = incident.titulo,
                                            mensaje = incident.mensaje,
                                            usuario = uiState.usuario
                                        )
                                    },
                                    onCorregirSolicitud = onNavigateToRegistroCorrection
                                )
                            },
                            onAccionSecundaria = {
                                viewModel.ejecutarAccionIncidente(
                                    incidente = incidente,
                                    onIrARegistro = onNavigateToRegistro,
                                    onContactarSoporte = { incident ->
                                        SoporteContacto.abrir(
                                            context = context,
                                            origen = "Login",
                                            tipoIncidente = incident.tipo.toString(),
                                            tituloIncidente = incident.titulo,
                                            mensaje = incident.mensaje,
                                            usuario = uiState.usuario
                                        )
                                    },
                                    onCorregirSolicitud = onNavigateToRegistroCorrection
                                )
                            },
                            modifier = Modifier
                                .padding(s.padModule)
                                .bounceClick()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoginContent(
    s: MedidaAdaptativa,
    uiState: LoginUiState,
    userFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    isProgrammaticScroll: MutableState<Boolean>,
    onUsuarioChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onIngresarClick: () -> Unit,
    onRecuperarContrasena: () -> Unit,
    onNavigateToRegistro: () -> Unit = {},
    onNavigateToExpediente: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(s.gapLarge)) {
        // Título de la consola
        Column(verticalArrangement = Arrangement.spacedBy(s.gapTiny)) {
            Text(
                text = "Iniciar Sesión",
                color = FDColors.TextPrimary,
                style = TokensFarmadon.tipografia.titulo1.copy(
                    fontFamily = InterPremium,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = "Ingresa tus credenciales para acceder al sistema.",
                color = FDColors.TextSecondary,
                style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = 13.5.sp)
            )
        }

        // Formulario de Inputs (52dp adaptativo via s.inputMinH)
        Column(verticalArrangement = Arrangement.spacedBy(s.gapMedium)) {
            ExecutiveInput(
                s = s,
                label = "Correo Electrónico",
                value = uiState.usuario,
                icon = Icons.Default.Email,
                placeholder = "Ingresa tu correo",
                onValueChange = onUsuarioChange,
                focusRequester = userFocusRequester,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                isProgrammaticScroll = isProgrammaticScroll,
                onImeAction = {
                    passwordFocusRequester.requestFocus()
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(s.gapSmall)) {
                ExecutiveInput(
                    s = s,
                    label = "Contraseña",
                    value = uiState.contrasena,
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    placeholder = "••••••••••••",
                    onValueChange = onContrasenaChange,
                    focusRequester = passwordFocusRequester,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    isProgrammaticScroll = isProgrammaticScroll,
                    onImeAction = {
                        if (uiState.usuario.isNotBlank() && uiState.contrasena.isNotBlank()) {
                            keyboardController?.hide()
                            onIngresarClick()
                        }
                    }
                )

                // Enlace con feedback real: ripple + pressed (no un texto muerto al tacto).
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(s.radiusChip))
                        .clickable { onRecuperarContrasena() }
                        .padding(horizontal = s.gapSmall, vertical = s.gapSmall)
                ) {
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = FDColors.TextTertiary,
                        style = TokensFarmadon.tipografia.leyenda.copy(fontSize = 12.sp)
                    )
                }
            }
        }

        // Botón Principal de Acceso (52dp adaptativo)
        Button(
            onClick = {
                keyboardController?.hide()
                onIngresarClick()
            },
            enabled = !uiState.cargando && uiState.usuario.isNotBlank() && uiState.contrasena.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(s.inputMinH)
                .bounceClick(),
            colors = ButtonDefaults.buttonColors(
                containerColor = FDColors.Primary,
                contentColor = FDColors.PrimaryText,
                disabledContainerColor = FDColors.Primary.copy(alpha = 0.12f),
                disabledContentColor = FDColors.TextDisabled
            ),
            shape = RoundedCornerShape(s.radiusInput)
        ) {
            if (uiState.cargando) {
                CircularProgressIndicator(
                    color = FDColors.PrimaryText,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
                ) {
                    Text(
                        text = "ENTRAR A VIORA",
                        style = TokensFarmadon.tipografia.titulo3.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Separador y enlaces
        HorizontalDivider(
            color = FDColors.Border,
            thickness = 1.dp
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(s.gapTiny * 0.5f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¿Nueva farmacia?",
                    color = FDColors.TextTertiary,
                    style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = 12.5.sp)
                )
                TextButton(
                    onClick = onNavigateToRegistro,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Registrar mi farmacia",
                        color = FDColors.TextPrimary,
                        style = TokensFarmadon.tipografia.etiqueta.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            TextButton(
                onClick = onNavigateToExpediente,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Consultar estado de expediente →",
                    color = FDColors.TextSecondary,
                    style = TokensFarmadon.tipografia.leyenda.copy(fontSize = 11.5.sp)
                )
            }
        }
    }
}
