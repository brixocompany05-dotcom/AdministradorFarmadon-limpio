package com.app.administradorfarmadon.facturacion.configuracion.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDSpacing
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal
import com.app.administradorfarmadon.facturacion.configuracion.datos.EmisorFiscal
import com.app.administradorfarmadon.facturacion.configuracion.logica.FacturacionConfigUiState
import com.app.administradorfarmadon.facturacion.configuracion.logica.FacturacionConfigViewModel

import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ConfiguracionFiscalScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    onIrAAuditoria: (() -> Unit)? = null,
    viewModel: FacturacionConfigViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // ── BLINDAJE ANTI-SALIDA ACCIDENTAL: PROHIBIDO RETROCEDER MIENTRAS SE GUARDA ──
    BackHandler(enabled = state.guardando) {
        // Bloqueo estricto del botón físico o gesto atrás de Android mientras corre la verificación
    }

    DialogBloqueoGuardando(visible = state.guardando)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(FDColors.Background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
    ) {
        val isWide = maxWidth >= 840.dp
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FDSpacing.xxl, vertical = FDSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FDSpacing.lg)
        ) {
            // ── CABECERA ENTERPRISE ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
            ) {
                IconButton(
                    onClick = onVolver,
                    enabled = !state.guardando,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(FDShapes.Medium)
                        .background(FDColors.Surface)
                        .border(1.dp, FDColors.Border, FDShapes.Medium)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = if (!state.guardando) FDColors.TextPrimary else FDColors.TextTertiary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FACTURACIÓN ELECTRÓNICA SUNAT",
                        style = FDType.Label.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterPremium
                        ),
                        color = FDColors.TextTertiary
                    )
                    Text(
                        text = "Emisor Fiscal y Credenciales APISUNAT",
                        style = FDType.Heading1.copy(fontFamily = InterPremium),
                        color = FDColors.TextPrimary
                    )
                }
            }

            ContenidoConfiguracionFiscal(
                isWide = isWide,
                modifier = Modifier.weight(1f),
                onIrAAuditoria = onIrAAuditoria,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ContenidoConfiguracionFiscal(
    isWide: Boolean,
    modifier: Modifier = Modifier,
    onIrAAuditoria: (() -> Unit)? = null,
    viewModel: FacturacionConfigViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    if (state.cargando && state.formRuc.isBlank()) {
        Box(
            modifier = modifier.fillMaxSize().padding(vertical = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = FDColors.Primary, modifier = Modifier.size(36.dp))
                Text(
                    "Sincronizando emisor fiscal...",
                    style = FDType.Body.copy(color = FDColors.TextSecondary, fontWeight = FontWeight.Medium)
                )
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxSize().imePadding(),
        verticalArrangement = Arrangement.spacedBy(FDSpacing.lg)
    ) {
        // ── BANNER DE RESTRICCIÓN DE ACCESO (SI NO ES ADMIN EN PRINCIPAL) ──
        if (!state.puedeEditar) {
            Surface(
                color = FDColors.Warning.copy(alpha = 0.12f),
                shape = FDShapes.Medium,
                border = BorderStroke(1.dp, FDColors.Warning),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(FDSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = FDColors.Warning,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "ACCESO DE SOLO LECTURA",
                            style = FDType.Label.copy(fontWeight = FontWeight.Bold),
                            color = FDColors.Warning
                        )
                        Text(
                            text = if (!state.esAdmin) {
                                "Solo el Dueño o Administrador general tiene permiso para configurar el emisor fiscal."
                            } else {
                                "La configuración fiscal solo puede modificarse desde la Sede Principal de la farmacia."
                            },
                            style = FDType.BodySmall,
                            color = FDColors.TextPrimary
                        )
                    }
                }
            }
        }

        // ── BANNERS DE MENSAJES DE ÉXITO O ERROR ──
        state.mensajeExito?.let { exito ->
            Surface(
                color = FDColors.Success.copy(alpha = 0.12f),
                shape = FDShapes.Medium,
                border = BorderStroke(1.dp, FDColors.Success),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(FDSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = FDColors.Success)
                    Text(exito, style = FDType.Body, color = FDColors.TextPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.limpiarMensajes() }) {
                        Icon(Icons.Default.Close, null, tint = FDColors.TextSecondary)
                    }
                }
            }
        }

        state.mensajeError?.let { error ->
            Surface(
                color = FDColors.Error.copy(alpha = 0.12f),
                shape = FDShapes.Medium,
                border = BorderStroke(1.dp, FDColors.Error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(FDSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
                ) {
                    Icon(Icons.Default.WarningAmber, null, tint = FDColors.Error)
                    Text(error, style = FDType.Body, color = FDColors.TextPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.limpiarMensajes() }) {
                        Icon(Icons.Default.Close, null, tint = FDColors.TextSecondary)
                    }
                }
            }
        }

        // ── CUERPO PRINCIPAL: FORMULARIO CONTINUO (60%) + RESUMEN EJECUTIVO (40%) ──
        if (state.cargando && state.formRuc.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
                ) {
                    CircularProgressIndicator(
                        color = FDColors.Primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Sincronizando emisor fiscal...",
                        style = FDType.BodySmall,
                        color = FDColors.TextSecondary
                    )
                }
            }
        } else if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.xl)
            ) {
                // Columna 1: Panel Izquierdo - Formulario Continuo (60%) ocupa alto total con scroll interno
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Large,
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(FDSpacing.xl),
                        verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
                    ) {
                        FormularioEmisorContenido(state = state, viewModel = viewModel)
                    }
                }

                // Columna 2: Panel Derecho - Estado y Guardar (40%) ocupa el mismo alto exacto
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Large,
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(FDSpacing.xl),
                        verticalArrangement = Arrangement.spacedBy(FDSpacing.lg)
                    ) {
                        PanelEstadoFiscal(state = state, viewModel = viewModel, onIrAAuditoria = onIrAAuditoria)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(FDSpacing.lg)
            ) {
                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Large,
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(FDSpacing.xl),
                        verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
                    ) {
                        FormularioEmisorContenido(state = state, viewModel = viewModel)
                    }
                }

                Surface(
                    color = FDColors.Surface,
                    shape = FDShapes.Large,
                    border = BorderStroke(1.dp, FDColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(FDSpacing.xl),
                        verticalArrangement = Arrangement.spacedBy(FDSpacing.lg)
                    ) {
                        PanelEstadoFiscal(state = state, viewModel = viewModel, onIrAAuditoria = onIrAAuditoria)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// COMPONENTE DE CAMPO DE ENTRADA ENTERPRISE SERIO (SIN REDONDEO, COMPACTO Y LEGIBLE)
// ─────────────────────────────────────────────

@Composable
private fun FDCampoEntradaSerio(
    valor: String,
    onValorCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    iconoInicio: ImageVector? = null,
    iconoFin: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    habilitado: Boolean = true,
    esError: Boolean = false,
    textoAyuda: String? = null,
    textoError: String? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = etiqueta,
            style = FDType.Label.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = if (esError) FDColors.Error else FDColors.TextSecondary
        )

        OutlinedTextField(
            value = valor,
            onValueChange = onValorCambio,
            placeholder = {
                if (placeholder.isNotBlank()) {
                    Text(
                        text = placeholder,
                        style = FDType.BodySmall.copy(fontSize = 12.5.sp),
                        color = FDColors.TextTertiary
                    )
                }
            },
            leadingIcon = iconoInicio?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (valor.isNotBlank()) FDColors.Primary else FDColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            trailingIcon = iconoFin,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = true,
            enabled = habilitado,
            isError = esError,
            textStyle = FDType.Body.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = FDColors.TextPrimary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (esError) FDColors.Error else FDColors.Primary,
                unfocusedBorderColor = if (esError) FDColors.Error else FDColors.Border,
                focusedContainerColor = FDColors.Background,
                unfocusedContainerColor = FDColors.Background,
                disabledContainerColor = FDColors.Background.copy(alpha = 0.5f),
                focusedTextColor = FDColors.TextPrimary,
                unfocusedTextColor = FDColors.TextPrimary,
                disabledTextColor = FDColors.TextTertiary
            ),
            shape = RoundedCornerShape(0.dp), // ◄◄ 0.dp REDONDEO: SHARP RECTANGULAR ENTERPRISE
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 42.dp)
        )

        val msj = textoError ?: textoAyuda
        if (!msj.isNullOrBlank()) {
            Text(
                text = msj,
                style = FDType.Caption.copy(fontSize = 10.5.sp),
                color = if (esError) FDColors.Error else FDColors.TextTertiary
            )
        }
    }
}

@Composable
private fun FormularioEmisorContenido(
    state: FacturacionConfigUiState,
    viewModel: FacturacionConfigViewModel
) {
    var mostrarToken by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
    ) {
        Text(
            text = "1. IDENTIDAD FISCAL DE LA FARMACIA",
            style = FDType.Label.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
            color = FDColors.TextSecondary
        )

        // RUC
        FDCampoEntradaSerio(
            valor = state.formRuc,
            onValorCambio = { viewModel.onFieldChanged("ruc", it) },
            etiqueta = "RUC DEL EMISOR (11 DÍGITOS)",
            placeholder = "20XXXXXXXXX",
            iconoInicio = Icons.Default.Badge,
            esError = state.formErrores.containsKey("ruc"),
            textoError = state.formErrores["ruc"],
            textoAyuda = if (!state.formErrores.containsKey("ruc")) "Identificador tributario único de la farmacia ante SUNAT" else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            habilitado = state.puedeEditar
        )

        // Razón Social
        FDCampoEntradaSerio(
            valor = state.formRazonSocial,
            onValorCambio = { viewModel.onFieldChanged("razonSocial", it) },
            etiqueta = "RAZÓN SOCIAL / NOMBRE COMERCIAL FISCAL",
            placeholder = "Razón social declarada ante SUNAT",
            iconoInicio = Icons.Default.Business,
            esError = state.formErrores.containsKey("razonSocial"),
            textoError = state.formErrores["razonSocial"],
            habilitado = state.puedeEditar
        )

        // Dirección Fiscal
        FDCampoEntradaSerio(
            valor = state.formDireccionFiscal,
            onValorCambio = { viewModel.onFieldChanged("direccionFiscal", it) },
            etiqueta = "DIRECCIÓN FISCAL PRINCIPAL",
            placeholder = "Dirección del domicilio tributario",
            iconoInicio = Icons.Default.LocationOn,
            esError = state.formErrores.containsKey("direccionFiscal"),
            textoError = state.formErrores["direccionFiscal"],
            habilitado = state.puedeEditar
        )

        HorizontalDivider(color = FDColors.Border, modifier = Modifier.padding(vertical = FDSpacing.xs))

        Text(
            text = "2. CREDENCIALES DE CONEXIÓN APISUNAT",
            style = FDType.Label.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
            color = FDColors.TextSecondary
        )

        // Persona ID
        FDCampoEntradaSerio(
            valor = state.formPersonaId,
            onValorCambio = { viewModel.onFieldChanged("personaId", it) },
            etiqueta = "PERSONA ID (APISUNAT.COM)",
            placeholder = "ID único generado en APISUNAT",
            iconoInicio = Icons.Default.VpnKey,
            esError = state.formErrores.containsKey("personaId"),
            textoError = state.formErrores["personaId"],
            habilitado = state.puedeEditar
        )

        // Persona Token (Enmascarado con toggle)
        FDCampoEntradaSerio(
            valor = state.formPersonaToken,
            onValorCambio = { viewModel.onFieldChanged("personaToken", it) },
            etiqueta = "PERSONA TOKEN (APISUNAT.COM)",
            placeholder = "Token secreto de autorización",
            iconoInicio = Icons.Default.Password,
            iconoFin = {
                IconButton(onClick = { mostrarToken = !mostrarToken }) {
                    Icon(
                        imageVector = if (mostrarToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (mostrarToken) "Ocultar token" else "Ver token",
                        tint = FDColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            visualTransformation = if (mostrarToken) VisualTransformation.None else PasswordVisualTransformation(),
            esError = state.formErrores.containsKey("personaToken"),
            textoError = state.formErrores["personaToken"],
            textoAyuda = if (!state.formErrores.containsKey("personaToken")) "El token se almacena de forma segura y nunca se expone en logs" else null,
            habilitado = state.puedeEditar
        )

        HorizontalDivider(color = FDColors.Border, modifier = Modifier.padding(vertical = FDSpacing.xs))

        Text(
            text = "3. MODO DE OPERACIÓN",
            style = FDType.Label.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
            color = FDColors.TextSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
        ) {
            SelectorModoBoton(
                titulo = "DESARROLLO (Pruebas)",
                subtitulo = "Comprobantes de prueba sin validez fiscal legal",
                seleccionado = state.formModo == EmisorFiscal.MODO_DESARROLLO,
                enabled = state.puedeEditar,
                onClick = { viewModel.onModoChanged(EmisorFiscal.MODO_DESARROLLO) },
                modifier = Modifier.weight(1f)
            )

            SelectorModoBoton(
                titulo = "PRODUCCIÓN (Oficial)",
                subtitulo = "Comprobantes reales declarados ante SUNAT",
                seleccionado = state.formModo == EmisorFiscal.MODO_PRODUCCION,
                enabled = state.puedeEditar,
                onClick = { viewModel.onModoChanged(EmisorFiscal.MODO_PRODUCCION) },
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = FDColors.Border, modifier = Modifier.padding(vertical = FDSpacing.xs))

        Text(
            text = "4. SERIES FISCALES ASIGNADAS POR SEDE (4/4 SUNAT)",
            style = FDType.Label.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
            color = FDColors.TextSecondary
        )

        if (state.sedes.isEmpty()) {
            Text(
                text = "No hay sedes registradas.",
                style = FDType.Caption,
                color = FDColors.TextTertiary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(FDSpacing.sm)) {
                for (sede in state.sedes) {
                    Surface(
                        color = FDColors.Background,
                        shape = RoundedCornerShape(0.dp),
                        border = BorderStroke(1.dp, FDColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(FDSpacing.md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sede.nombre.ifBlank { "Sede" },
                                    style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = FDColors.TextPrimary
                                )
                                if (sede.esPrincipal) {
                                    Surface(
                                        color = FDColors.Primary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(0.dp)
                                    ) {
                                        Text(
                                            "PRINCIPAL",
                                            style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = FDColors.Primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FDSpacing.md)
                            ) {
                                Text("Boleta: ${sede.serieBoleta.ifBlank { "B001" }}", style = FDType.Caption, color = FDColors.TextSecondary)
                                Text("Factura: ${sede.serieFactura.ifBlank { "F001" }}", style = FDType.Caption, color = FDColors.TextSecondary)
                                Text("NC Boleta: ${sede.serieNotaCreditoBoleta.ifBlank { "BC01" }}", style = FDType.Caption, color = FDColors.TextSecondary)
                                Text("NC Factura: ${sede.serieNotaCreditoFactura.ifBlank { "FC01" }}", style = FDType.Caption, color = FDColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectorModoBoton(
    titulo: String,
    subtitulo: String,
    seleccionado: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (seleccionado) FDColors.Primary else FDColors.Border
    val bgColor = if (seleccionado) FDColors.PrimarySubtle else FDColors.Background

    Surface(
        onClick = { if (enabled) onClick() },
        color = bgColor,
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(if (seleccionado) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(FDSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FDSpacing.xs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
            ) {
                RadioButton(
                    selected = seleccionado,
                    onClick = { if (enabled) onClick() },
                    enabled = enabled,
                    colors = RadioButtonDefaults.colors(selectedColor = FDColors.Primary)
                )
                Text(
                    text = titulo,
                    style = FDType.BodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (seleccionado) FDColors.Primary else FDColors.TextPrimary
                )
            }
            Text(
                text = subtitulo,
                style = FDType.Caption.copy(fontSize = 11.sp),
                color = FDColors.TextSecondary,
                modifier = Modifier.padding(start = 32.dp)
            )
        }
    }
}

@Composable
private fun PanelEstadoFiscal(
    state: FacturacionConfigUiState,
    viewModel: FacturacionConfigViewModel,
    onIrAAuditoria: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(FDSpacing.lg)
    ) {
        Text(
            text = "ESTADO DEL EMISOR FISCAL",
            style = FDType.Label.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
            color = FDColors.TextSecondary
        )

        // Indicador semáforo según estado derivado
        val badgeColor = when {
            state.estaCompleta -> FDColors.Success
            state.verificadoOk -> FDColors.Primary
            state.ultimoError.isNotBlank() -> FDColors.Error
            else -> FDColors.Warning
        }

        val badgeTexto = when {
            state.estaCompleta -> "CONFIGURACIÓN COMPLETA Y VERIFICADA"
            state.verificadoOk -> "CONEXIÓN VERIFICADA"
            state.ultimoError.isNotBlank() -> "ERROR EN CONEXIÓN FISCAL"
            else -> "CONFIGURACIÓN PENDIENTE"
        }

        Surface(
            color = badgeColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(1.dp, badgeColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(FDSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Text(
                    text = badgeTexto,
                    style = FDType.Label.copy(fontWeight = FontWeight.Black),
                    color = badgeColor
                )
            }
        }

        // Explicación de estado
        if (state.estaCompleta) {
            Text(
                text = "El emisor fiscal cuenta con credenciales válidas y verificadas ante el servidor de APISUNAT. Las cajas registradoras están autorizadas para emitir comprobantes de pago.",
                style = FDType.BodySmall,
                color = FDColors.TextSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(FDSpacing.xs)) {
                Text(
                    text = "Requisitos pendientes para operar:",
                    style = FDType.BodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = FDColors.TextPrimary
                )
                if (state.formRuc.trim().length != 11) {
                    FilaRequisitoItem(cumplido = false, texto = "RUC de 11 dígitos válido")
                }
                if (state.formRazonSocial.isBlank()) {
                    FilaRequisitoItem(cumplido = false, texto = "Razón Social registrada")
                }
                if (state.formDireccionFiscal.isBlank()) {
                    FilaRequisitoItem(cumplido = false, texto = "Dirección Fiscal")
                }
                if (state.formPersonaId.isBlank() || state.formPersonaToken.isBlank()) {
                    FilaRequisitoItem(cumplido = false, texto = "Credenciales Persona ID y Token de APISUNAT")
                }
                if (!state.verificadoOk) {
                    FilaRequisitoItem(cumplido = false, texto = "Ping de conexión exitoso con APISUNAT")
                }
            }
        }

        // Último error reportado por APISUNAT
        if (state.ultimoError.isNotBlank()) {
            Surface(
                color = FDColors.Error.copy(alpha = 0.08f),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, FDColors.Error.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FDSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "DETALLE DE RESPUESTA DEL PROVEEDOR:",
                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold),
                        color = FDColors.Error
                    )
                    Text(
                        text = state.ultimoError,
                        style = FDType.BodySmall,
                        color = FDColors.TextPrimary
                    )
                }
            }
        }

        // ── ÚLTIMA AUDITORÍA REGISTRADA ──
        if (state.historial.isNotEmpty()) {
            val ultimo = state.historial.first()
            Surface(
                color = FDColors.Background,
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, FDColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FDSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(FDSpacing.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ÚLTIMA AUDITORÍA FISCAL",
                            style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                            color = FDColors.TextTertiary
                        )
                        Text(
                            text = ultimo.fechaLegible.ifBlank { "Reciente" },
                            style = FDType.Caption.copy(fontWeight = FontWeight.Bold),
                            color = FDColors.TextSecondary
                        )
                    }

                    Text(
                        text = "Por: ${ultimo.usuarioNombre.ifBlank { "Administrador" }} (${ultimo.usuarioEmail})",
                        style = FDType.BodySmall.copy(fontWeight = FontWeight.Medium),
                        color = FDColors.TextPrimary
                    )

                    Text(
                        text = if (ultimo.verificadoOk) "• Resultado: Conexión validada exitosamente" else "• Resultado: Rechazado por APISUNAT",
                        style = FDType.Caption,
                        color = if (ultimo.verificadoOk) FDColors.Success else FDColors.Error
                    )

                    if (onIrAAuditoria != null) {
                        OutlinedButton(
                            onClick = onIrAAuditoria,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver pestaña Auditoría (${state.historial.size})", style = FDType.Caption.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón primario de guardado y verificación en vivo
        FDBotonPrimario(
            texto = if (state.guardando) "VERIFICANDO CON APISUNAT..." else "GUARDAR Y VERIFICAR CONEXIÓN",
            onClick = { viewModel.guardarYVerificar() },
            icono = Icons.Default.CloudSync,
            habilitado = state.puedeEditar && !state.guardando,
            cargando = state.guardando,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        if (!state.puedeEditar) {
            Text(
                text = "Botón inhabilitado: se requieren privilegios de Administrador en la Sede Principal.",
                style = FDType.Caption,
                color = FDColors.TextTertiary
            )
        }
    }
}

@Composable
private fun FilaRequisitoItem(cumplido: Boolean, texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FDSpacing.xs)
    ) {
        Icon(
            imageVector = if (cumplido) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (cumplido) FDColors.Success else FDColors.Error,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = texto,
            style = FDType.Caption,
            color = if (cumplido) FDColors.TextSecondary else FDColors.Error
        )
    }
}

/**
 * Modal inquebrantable de bloqueo visual durante el guardado y verificación con APISUNAT.
 * Evita toques en pantalla, salidas o retrocesos mientras la operación transaccional se completa.
 */
@Composable
fun DialogBloqueoGuardando(visible: Boolean) {
    if (!visible) return
    Dialog(
        onDismissRequest = { /* Bloqueo estricto */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = FDShapes.Large,
            color = FDColors.Surface,
            border = BorderStroke(1.dp, FDColors.Border),
            modifier = Modifier.widthIn(max = 440.dp)
        ) {
            Column(
                modifier = Modifier.padding(FDSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FDSpacing.md)
            ) {
                CircularProgressIndicator(
                    color = FDColors.Primary,
                    modifier = Modifier.size(44.dp),
                    strokeWidth = 3.dp
                )
                Text(
                    text = "Verificando con APISUNAT y Guardando",
                    style = FDType.Heading2.copy(fontWeight = FontWeight.Bold, fontFamily = InterPremium),
                    color = FDColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "1. Conectando con el servidor oficial de APISUNAT...\n2. Validando Token y vigencia de contribuyente...\n3. Registrando acta inmutable en Firebase.",
                    style = FDType.BodySmall,
                    color = FDColors.TextSecondary,
                    textAlign = TextAlign.Start
                )
                Surface(
                    color = FDColors.Warning.copy(alpha = 0.12f),
                    shape = FDShapes.Small,
                    border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Por favor, espere. No cierre la app ni toque volver.",
                        style = FDType.Caption.copy(fontWeight = FontWeight.Bold),
                        color = FDColors.Warning,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
