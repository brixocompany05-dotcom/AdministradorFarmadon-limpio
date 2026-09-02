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
import com.app.administradorfarmadon.facturacion.configuracion.datos.EmisorFiscal
import com.app.administradorfarmadon.facturacion.configuracion.logica.FacturacionConfigUiState
import com.app.administradorfarmadon.facturacion.configuracion.logica.FacturacionConfigViewModel

@Composable
fun ConfiguracionFiscalScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FacturacionConfigViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                    modifier = Modifier
                        .size(42.dp)
                        .clip(FDShapes.Medium)
                        .background(FDColors.Surface)
                        .border(1.dp, FDColors.Border, FDShapes.Medium)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = FDColors.TextPrimary
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
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ContenidoConfiguracionFiscal(
    isWide: Boolean,
    modifier: Modifier = Modifier,
    viewModel: FacturacionConfigViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxSize(),
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
        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(FDSpacing.xxl)
            ) {
                // Columna 1: Formulario Continuo (60%)
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(FDSpacing.lg)
                ) {
                    FormularioEmisorContenido(state = state, viewModel = viewModel)
                }

                // Columna 2: Panel Ejecutivo de Liquidación y Estado (40%)
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(FDSpacing.lg)
                ) {
                    PanelEstadoFiscal(state = state, viewModel = viewModel)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(FDSpacing.lg)
            ) {
                FormularioEmisorContenido(state = state, viewModel = viewModel)
                PanelEstadoFiscal(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun FormularioEmisorContenido(
    state: FacturacionConfigUiState,
    viewModel: FacturacionConfigViewModel
) {
    var mostrarToken by remember { mutableStateOf(false) }

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
            Text(
                text = "1. IDENTIDAD FISCAL DE LA FARMACIA",
                style = FDType.Label.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                color = FDColors.TextSecondary
            )

            // RUC
            OutlinedTextField(
                value = state.formRuc,
                onValueChange = { viewModel.onFieldChanged("ruc", it) },
                label = { Text("RUC del Emisor (11 dígitos)") },
                placeholder = { Text("20XXXXXXXXX") },
                leadingIcon = { Icon(Icons.Default.Badge, null, tint = FDColors.Primary) },
                isError = state.formErrores.containsKey("ruc"),
                supportingText = {
                    state.formErrores["ruc"]?.let {
                        Text(it, color = FDColors.Error)
                    } ?: Text("Identificador tributario único de la farmacia ante SUNAT", color = FDColors.TextTertiary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = state.puedeEditar,
                modifier = Modifier.fillMaxWidth(),
                shape = FDShapes.Medium
            )

            // Razón Social
            OutlinedTextField(
                value = state.formRazonSocial,
                onValueChange = { viewModel.onFieldChanged("razonSocial", it) },
                label = { Text("Razón Social / Nombre Comercial Fiscal") },
                placeholder = { Text("FARMACIA EJEMPLO S.A.C.") },
                leadingIcon = { Icon(Icons.Default.Business, null, tint = FDColors.Primary) },
                isError = state.formErrores.containsKey("razonSocial"),
                supportingText = {
                    state.formErrores["razonSocial"]?.let { Text(it, color = FDColors.Error) }
                },
                singleLine = true,
                enabled = state.puedeEditar,
                modifier = Modifier.fillMaxWidth(),
                shape = FDShapes.Medium
            )

            // Dirección Fiscal
            OutlinedTextField(
                value = state.formDireccionFiscal,
                onValueChange = { viewModel.onFieldChanged("direccionFiscal", it) },
                label = { Text("Dirección Fiscal Principal") },
                placeholder = { Text("AV. PRINCIPAL 123, DISTRITO, PROVINCIA") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = FDColors.Primary) },
                isError = state.formErrores.containsKey("direccionFiscal"),
                supportingText = {
                    state.formErrores["direccionFiscal"]?.let { Text(it, color = FDColors.Error) }
                },
                singleLine = true,
                enabled = state.puedeEditar,
                modifier = Modifier.fillMaxWidth(),
                shape = FDShapes.Medium
            )

            HorizontalDivider(color = FDColors.Border, modifier = Modifier.padding(vertical = FDSpacing.sm))

            Text(
                text = "2. CREDENCIALES DE CONEXIÓN APISUNAT",
                style = FDType.Label.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                color = FDColors.TextSecondary
            )

            // Persona ID
            OutlinedTextField(
                value = state.formPersonaId,
                onValueChange = { viewModel.onFieldChanged("personaId", it) },
                label = { Text("Persona ID (apisunat.com)") },
                placeholder = { Text("ID único generado en APISUNAT") },
                leadingIcon = { Icon(Icons.Default.VpnKey, null, tint = FDColors.Primary) },
                isError = state.formErrores.containsKey("personaId"),
                supportingText = {
                    state.formErrores["personaId"]?.let { Text(it, color = FDColors.Error) }
                },
                singleLine = true,
                enabled = state.puedeEditar,
                modifier = Modifier.fillMaxWidth(),
                shape = FDShapes.Medium
            )

            // Persona Token (Enmascarado con toggle)
            OutlinedTextField(
                value = state.formPersonaToken,
                onValueChange = { viewModel.onFieldChanged("personaToken", it) },
                label = { Text("Persona Token (apisunat.com)") },
                placeholder = { Text("Token secreto de autorización") },
                leadingIcon = { Icon(Icons.Default.Password, null, tint = FDColors.Primary) },
                trailingIcon = {
                    IconButton(onClick = { mostrarToken = !mostrarToken }) {
                        Icon(
                            imageVector = if (mostrarToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (mostrarToken) "Ocultar token" else "Ver token",
                            tint = FDColors.TextSecondary
                        )
                    }
                },
                visualTransformation = if (mostrarToken) VisualTransformation.None else PasswordVisualTransformation(),
                isError = state.formErrores.containsKey("personaToken"),
                supportingText = {
                    state.formErrores["personaToken"]?.let { Text(it, color = FDColors.Error) }
                        ?: Text("El token se almacena de forma segura y nunca se expone en logs", color = FDColors.TextTertiary)
                },
                singleLine = true,
                enabled = state.puedeEditar,
                modifier = Modifier.fillMaxWidth(),
                shape = FDShapes.Medium
            )

            HorizontalDivider(color = FDColors.Border, modifier = Modifier.padding(vertical = FDSpacing.sm))

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
    val bgColor = if (seleccionado) FDColors.PrimarySubtle else FDColors.Surface

    Surface(
        onClick = { if (enabled) onClick() },
        color = bgColor,
        shape = FDShapes.Medium,
        border = BorderStroke(if (seleccionado) 2.dp else 1.dp, borderColor),
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
                    style = FDType.Body.copy(fontWeight = FontWeight.Bold),
                    color = if (seleccionado) FDColors.Primary else FDColors.TextPrimary
                )
            }
            Text(
                text = subtitulo,
                style = FDType.Caption,
                color = FDColors.TextSecondary,
                modifier = Modifier.padding(start = 32.dp)
            )
        }
    }
}

@Composable
private fun PanelEstadoFiscal(
    state: FacturacionConfigUiState,
    viewModel: FacturacionConfigViewModel
) {
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
                shape = FDShapes.Medium,
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
                    shape = FDShapes.Small,
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
                    .height(52.dp)
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
