package com.app.administradorfarmadon.autenticacion.expediente.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.expediente.datos.ResultadoExpediente
import com.app.administradorfarmadon.autenticacion.expediente.logica.ConsultarExpedienteViewModel
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConsultarExpedienteScreen(
    viewModel: ConsultarExpedienteViewModel,
    onBack: () -> Unit,
    onNavigateToCorregir: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val s = recordarMedidaAdaptativa()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val isImeVisible = WindowInsets.isImeVisible

    BackHandler(enabled = isImeVisible) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    BackHandler(enabled = !isImeVisible) { onBack() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(280)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.systemBars,
        modifier = Modifier.fillMaxSize().drawBehind {
            // Fondo adaptativo con tokens
            drawRect(
                brush = Brush.radialGradient(
                    colors = if (FDColors.isDark) listOf(FDColors.Surface, FDColors.Background)
                    else listOf(FDColors.SurfaceElevated, FDColors.Background),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.maxDimension * 0.8f
                )
            )
            drawRect(
                brush = Brush.linearGradient(
                    colors = if (FDColors.isDark) listOf(FDColors.TextPrimary.copy(alpha = 0.08f), FDColors.Primary.copy(alpha = 0.03f), Color.Transparent)
                    else listOf(FDColors.TextPrimary.copy(alpha = 0.03f), FDColors.Primary.copy(alpha = 0.01f), Color.Transparent),
                    start = Offset(size.width, size.height * 0.5f),
                    end = Offset(size.width * 0.3f, size.height * 0.5f)
                )
            )
        }
    ) { padding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(s.padScreenH).padding(top = 12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FDColors.TextSecondary)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = s.padScreenH)
                .padding(top = 36.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "EXPEDIENTE",
                color = FDColors.TextTertiary,
                style = TokensFarmadon.tipografia.etiqueta.copy(
                    fontFamily = InterPremium, fontWeight = FontWeight.SemiBold, letterSpacing = 5.sp, fontSize = 11.sp
                )
            )
            Text(
                "CONSULTA RÁPIDA",
                color = FDColors.TextPrimary,
                style = TokensFarmadon.tipografia.visual.copy(
                    fontFamily = InterPremium, fontSize = 40.sp, letterSpacing = (-1).sp, fontWeight = FontWeight.Light
                ),
                textAlign = TextAlign.Center
            )
            Text(
                "Solo tu documento. Sin correo, sin fricción.",
                style = TokensFarmadon.tipografia.cuerpo,
                color = FDColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
            )

            Surface(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .border(1.dp, FDColors.Border, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = FDColors.Glass
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("DOCUMENTO", style = FDType.Label.copy(letterSpacing = 1.5.sp), color = FDColors.TextTertiary)

                    val bringIntoView = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
                    OutlinedTextField(
                        value = state.ruc,
                        onValueChange = { viewModel.onRucChanged(it) },
                        placeholder = { Text("RUC 11 dígitos o DNI 8", color = TokensFarmadon.colores.inputPlaceholder, style = TokensFarmadon.tipografia.cuerpoPequeno) },
                        leadingIcon = { Icon(Icons.Default.Badge, null, tint = TokensFarmadon.colores.textoTerciario) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (state.ruc.length >= 8) viewModel.consultar()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = s.inputMinH)
                            .focusRequester(focusRequester)
                            .bringIntoViewRequester(bringIntoView)
                            .onFocusChanged { 
                                if(it.isFocused) scope.launch { bringIntoView.bringIntoView() } 
                            },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TokensFarmadon.colores.textoPrincipal,
                            unfocusedTextColor = TokensFarmadon.colores.textoPrincipal,
                            focusedContainerColor = TokensFarmadon.colores.inputFondoFoco,
                            unfocusedContainerColor = TokensFarmadon.colores.inputFondo,
                            focusedBorderColor = TokensFarmadon.colores.bordeEnfoque,
                            unfocusedBorderColor = TokensFarmadon.colores.inputBorde.copy(alpha = 0.5f),
                            cursorColor = TokensFarmadon.colores.textoPrincipal
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (state.errorValidacion != null) {
                        Text(state.errorValidacion!!, color = FDColors.Error, style = TokensFarmadon.tipografia.etiqueta, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.consultar()
                        },
                        modifier = Modifier.fillMaxWidth().height(s.inputMinH).bounceClick(),
                        enabled = !state.cargando && state.ruc.length >= 8,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Primary, 
                            disabledContainerColor = FDColors.Primary.copy(alpha = 0.12f), 
                            contentColor = FDColors.PrimaryText, 
                            disabledContentColor = FDColors.TextDisabled
                        )
                    ) {
                        if (state.cargando) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = FDColors.PrimaryText, strokeWidth = 2.dp)
                        else Text("CONSULTAR", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                    }

                    AnimatedVisibility(visible = state.resultado != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        state.resultado?.let { res -> ResultadoExpedienteCard(res, s, onNavigateToCorregir, onBack) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }
}

@Composable
fun ResultadoExpedienteCard(
    resultado: ResultadoExpediente,
    s: MedidaAdaptativa,
    onNavigateToCorregir: (String) -> Unit,
    onBackToLogin: () -> Unit
) {
    when (resultado) {
        is ResultadoExpediente.Encontrado -> CardEstadoExpediente(resultado, s, onNavigateToCorregir, onBackToLogin)
        is ResultadoExpediente.NoEncontrado -> Surface(
            modifier = Modifier.fillMaxWidth(), 
            shape = TokensFarmadon.formas.grande, 
            color = FDColors.Glass, 
            border = androidx.compose.foundation.BorderStroke(1.dp, FDColors.Border)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SearchOff, null, tint = FDColors.TextTertiary, modifier = Modifier.size(36.dp))
                Text("Sin resultados", style = FDType.Heading3, color = FDColors.TextPrimary, textAlign = TextAlign.Center)
                Text("No encontramos expediente con ese documento.", style = TokensFarmadon.tipografia.cuerpo, color = FDColors.TextSecondary, textAlign = TextAlign.Center)
            }
        }
        is ResultadoExpediente.Error -> Surface(
            modifier = Modifier.fillMaxWidth(), 
            shape = TokensFarmadon.formas.grande, 
            color = FDColors.ErrorSubtle, 
            border = androidx.compose.foundation.BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.2f))
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Error, null, tint = FDColors.Error)
                Text(resultado.mensaje, style = TokensFarmadon.tipografia.cuerpo, color = FDColors.TextPrimary)
            }
        }
    }
}

@Composable
fun CardEstadoExpediente(
    exp: ResultadoExpediente.Encontrado,
    s: MedidaAdaptativa,
    onNavigateToCorregir: (String) -> Unit,
    onBackToLogin: () -> Unit
) {
    val colorEstado = when (exp.estado) {
        "pendiente" -> FDColors.Warning
        "rechazada" -> FDColors.Error
        "aprobada" -> FDColors.Success
        else -> if (exp.camposACorregir.isNotEmpty()) FDColors.Warning else FDColors.TextTertiary
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).border(1.dp, FDColors.Border, RoundedCornerShape(20.dp)), 
        color = FDColors.Glass, 
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colorEstado))
                Spacer(Modifier.width(8.dp))
                Text("SOLICITUD —¢ ${exp.ruc}", style = FDType.Label.copy(letterSpacing = 1.sp), color = FDColors.TextTertiary)
            }
            Text(exp.estado.uppercase(), style = FDType.Heading3.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = colorEstado)
            val textoExplicativo = when {
                !exp.mensajeBrixo.isNullOrBlank() -> exp.mensajeBrixo
                exp.estado == "rechazada" -> "Tu solicitud no ha sido aprobada por la administración de Brixo."
                exp.estado == "pendiente" -> "Tu solicitud fue recibida y se encuentra en cola de revisión."
                else -> null
            }
            if (textoExplicativo != null) {
                Text(textoExplicativo, style = FDType.BodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp), color = FDColors.TextPrimary)
            }
            // Botón solo si la observación está VIVA: pedida y sin respuesta del
            // cliente. Tras enviar la corrección, camposACorregir sigue lleno en el
            // servidor (BrixoPanel lo necesita) pero aquí ya no debe ofrecerse
            // "CORREGIR SOLICITUD" —” sería una pantalla fantasma.
            val observacionViva = (exp.camposACorregir.isNotEmpty() || exp.estado.equals("observada", ignoreCase = true)) &&
                    !exp.correccionYaRespondida
            if (observacionViva) {
                Button(
                    onClick = { onNavigateToCorregir(exp.ruc) },
                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Warning, contentColor = FDColors.SurfaceElevated),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).bounceClick()
                ) {
                    Icon(Icons.Default.EditNote, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CORREGIR SOLICITUD", style = FDType.Label.copy(fontWeight = FontWeight.Bold))
                }
            }
            if (exp.estado == "aprobada") {
                Button(
                    onClick = onBackToLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = FDColors.Primary, contentColor = FDColors.PrimaryText),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).bounceClick()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("IR A INICIAR SESIí“N", style = FDType.Label.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
