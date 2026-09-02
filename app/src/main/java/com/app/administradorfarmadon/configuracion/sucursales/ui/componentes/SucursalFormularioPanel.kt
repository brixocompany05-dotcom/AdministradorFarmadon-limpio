package com.app.administradorfarmadon.configuracion.sucursales.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.login.ui.componentes.ExecutiveInput
import com.app.administradorfarmadon.configuracion.metodospago.modelo.TIPOS_PAGO_FIJOS
import com.app.administradorfarmadon.configuracion.sucursales.logica.SucursalesUiState
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import org.osmdroid.config.Configuration

@Composable
fun SucursalFormularioPanel(
    state: SucursalesUiState,
    onFieldChanged: (String, String) -> Unit,
    onActivaChanged: (Boolean) -> Unit,
    onPagoSeleccionadoChanged: (String, Boolean) -> Unit,
    onOpenMapPicker: () -> Unit,
    onSiguientePaso: () -> Unit,
    onPasoAnterior: () -> Unit,
    onGuardar: () -> Unit,
    onSolicitarEliminar: () -> Unit,
    onCerrarPanel: () -> Unit,
    s: MedidaAdaptativa,
    modifier: Modifier = Modifier,
    onAddressSelected: (String, Double?, Double?) -> Unit = { _, _, _ -> }
) {
    val scrollState = rememberScrollState()
    val colores = TokensFarmadon.colores
    val context = LocalContext.current
    val esPrincipal = state.sucursalSeleccionada?.esPrincipal == true

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val focusNombre = remember { FocusRequester() }
    val focusResponsable = remember { FocusRequester() }
    val focusTelefono = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(s.radiusCard * 1.25f),
        color = colores.cardBase
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(s.padCardLarge)
        ) {
            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "EXPEDIENTE DE SEDE",
                        style = TokensFarmadon.tipografia.etiqueta.copy(
                            fontSize = s.textLabel.value.sp * 0.95f,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colores.textoTerciario
                    )
                    Text(
                        text = if (state.esModoCreacion) "Nueva Identificación" else state.formNombre,
                        style = TokensFarmadon.tipografia.titulo1.copy(fontSize = s.textTitle.value.sp * 0.95f),
                        color = colores.textoPrincipal
                    )
                }

                IconButton(
                    onClick = onCerrarPanel,
                    modifier = Modifier
                        .size(s.btnSmallH)
                        .clip(RoundedCornerShape(s.radiusChip))
                        .background(colores.cardElevada)
                        .border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusChip))
                ) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = colores.textoPrincipal,
                        modifier = Modifier.size(s.iconSmall)
                    )
                }
            }

            Spacer(modifier = Modifier.height(s.gapMedium))

            val scrollableBody = !(state.esModoCreacion && state.pasoActual == 2)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (scrollableBody) Modifier.verticalScroll(scrollState) else Modifier)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                if (state.mensajeError != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colores.alertaSutil,
                        shape = RoundedCornerShape(s.radiusChip),
                        border = BorderStroke(s.borderWidth, colores.estadoAlerta)
                    ) {
                        Row(
                            modifier = Modifier.padding(s.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.xs)
                        ) {
                            Icon(
                                Icons.Default.WarningAmber,
                                null,
                                tint = colores.estadoAlerta,
                                modifier = Modifier.size(s.iconSmall)
                            )
                            Text(
                                text = state.mensajeError,
                                style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp),
                                color = colores.textoPrincipal,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onGuardar() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colores.estadoAlerta,
                                    contentColor = colores.botonPrimarioTexto
                                ),
                                shape = RoundedCornerShape(s.radiusChip)
                            ) {
                                Text(
                                    text = "REINTENTAR",
                                    style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp * 0.85f)
                                )
                            }
                        }
                    }
                }

                if (state.esModoCreacion) {
                    val pasoActual = state.pasoActual

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colores.cardElevada,
                        shape = RoundedCornerShape(s.radiusInput),
                        border = BorderStroke(s.borderWidth * 0.8f, colores.cardBorde.copy(alpha = 0.65f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(s.sm),
                            verticalArrangement = Arrangement.spacedBy(s.xs)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Paso $pasoActual de 3",
                                    style = TokensFarmadon.tipografia.etiqueta.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = s.textLabel.value.sp * 0.9f
                                    ),
                                    color = colores.textoPrincipal
                                )
                                Text(
                                    text = when (pasoActual) {
                                        1 -> "Datos"
                                        2 -> "Ubicación"
                                        else -> "Pagos"
                                    },
                                    style = TokensFarmadon.tipografia.etiqueta.copy(
                                        fontSize = s.textLabel.value.sp * 0.82f,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colores.textoTerciario
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..3).forEach { numeroPaso ->
                                    val completado = numeroPaso < pasoActual
                                    val actual = numeroPaso == pasoActual

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(
                                                if (completado) colores.botonPrimarioFondo.copy(alpha = 0.9f)
                                                else Color.Transparent
                                            )
                                            .border(
                                                width = if (actual) 1.1.dp else 0.8.dp,
                                                color = if (actual) colores.botonPrimarioFondo.copy(alpha = 0.75f)
                                                else colores.cardBorde.copy(alpha = 0.8f),
                                                shape = RoundedCornerShape(999.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    when (pasoActual) {
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
                                ExecutiveInput(
                                    s = s,
                                    label = "Nombre de la Sede",
                                    value = state.formNombre,
                                    icon = Icons.Default.Storefront,
                                    placeholder = "Nombre comercial de la sede",
                                    errorText = state.formErrores["nombre"],
                                    focusRequester = focusNombre,
                                    imeAction = ImeAction.Next,
                                    onImeAction = { focusResponsable.requestFocus() },
                                    onValueChange = { onFieldChanged("nombre", it) }
                                )

                                ExecutiveInput(
                                    s = s,
                                    label = "Administrador responsable",
                                    value = state.formResponsable,
                                    icon = Icons.Default.Person,
                                    placeholder = "Nombre del encargado de la sede...",
                                    errorText = state.formErrores["responsable"],
                                    focusRequester = focusResponsable,
                                    imeAction = ImeAction.Next,
                                    onImeAction = { focusTelefono.requestFocus() },
                                    onValueChange = { onFieldChanged("responsable", it) }
                                )

                                ExecutiveInput(
                                    s = s,
                                    label = "Teléfono de contacto",
                                    value = state.formTelefono,
                                    icon = Icons.Default.Phone,
                                    keyboardType = KeyboardType.Phone,
                                    placeholder = "987 654 321",
                                    errorText = state.formErrores["telefono"],
                                    focusRequester = focusTelefono,
                                    imeAction = ImeAction.Done,
                                    onImeAction = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        if (state.formNombre.isNotBlank() && state.formTelefono.isNotBlank() && state.formResponsable.isNotBlank()) {
                                            onSiguientePaso()
                                        }
                                    },
                                    onValueChange = { onFieldChanged("telefono", it) }
                                )
                            }
                        }

                        2 -> {
                            // PASO 2: MAPA CON PIN CENTRADO TIPO UBER / RAPPI DE 0
                            UbicacionPasoMapaUberStyle(
                                direccionActual = state.formDireccion,
                                latitudActual = state.formLatitud,
                                longitudActual = state.formLongitud,
                                onUbicacionSeleccionada = { direccion, lat, lng ->
                                    onAddressSelected(direccion, lat, lng)
                                },
                                s = s
                            )
                        }

                        3 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(s.gapMedium),
                                verticalAlignment = Alignment.Top
                            ) {
                                // ── COLUMNA IZQUIERDA: SELECTOR EJECUTIVO UNIFICADO DE MÉTODOS DE PAGO ──
                                Column(
                                    modifier = Modifier.weight(1.1f),
                                    verticalArrangement = Arrangement.spacedBy(s.xs)
                                ) {
                                    Text(
                                        text = "MÉTODOS DE PAGO PERMITIDOS",
                                        style = TokensFarmadon.tipografia.etiqueta.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = s.textLabel.value.sp * 0.88f,
                                            letterSpacing = 0.8.sp
                                        ),
                                        color = colores.textoTerciario
                                    )

                                    Surface(
                                        color = colores.cardElevada,
                                        shape = RoundedCornerShape(s.radiusCard),
                                        border = BorderStroke(s.borderWidth * 1.1f, colores.textoPrincipal.copy(alpha = 0.22f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(s.sm),
                                            verticalArrangement = Arrangement.spacedBy(s.xs * 0.8f)
                                        ) {
                                            val todosMarcados = state.formPagosSeleccionados.size == TIPOS_PAGO_FIJOS.size
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Habilitar todas las modalidades",
                                                    style = TokensFarmadon.tipografia.cuerpo.copy(
                                                        fontSize = s.textBody.value.sp * 0.92f,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = colores.textoPrincipal
                                                )
                                                Switch(
                                                    checked = todosMarcados,
                                                    onCheckedChange = { marcar ->
                                                        TIPOS_PAGO_FIJOS.forEach { tipo ->
                                                            onPagoSeleccionadoChanged(tipo.id, marcar)
                                                        }
                                                    },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = colores.botonPrimarioFondo,
                                                        checkedTrackColor = colores.estadoExito.copy(alpha = 0.35f),
                                                        uncheckedThumbColor = colores.textoTerciario,
                                                        uncheckedTrackColor = colores.cardBorde.copy(alpha = 0.6f)
                                                    )
                                                )
                                            }

                                            HorizontalDivider(color = colores.cardBorde.copy(alpha = 0.5f), thickness = s.borderWidth)

                                            state.formErrores["pagos"]?.let { msg ->
                                                Text(
                                                    text = msg,
                                                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                                        fontSize = s.textLabel.value.sp * 0.92f,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = colores.estadoPeligro
                                                )
                                            }

                                            TIPOS_PAGO_FIJOS.forEachIndexed { index, tipo ->
                                                val marcado = tipo.id in state.formPagosSeleccionados
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(s.radiusChip))
                                                        .background(if (marcado) colores.estadoExito.copy(alpha = 0.08f) else Color.Transparent)
                                                        .clickable { onPagoSeleccionadoChanged(tipo.id, !marcado) }
                                                        .padding(horizontal = s.xs, vertical = s.xs * 0.6f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(tipo.colorMarca.copy(alpha = 0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = tipo.icono,
                                                            contentDescription = null,
                                                            tint = tipo.colorMarca,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                                    ) {
                                                        Text(
                                                            text = tipo.nombre,
                                                            style = TokensFarmadon.tipografia.cuerpo.copy(
                                                                fontSize = s.textBody.value.sp * 0.92f,
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = colores.textoPrincipal
                                                        )
                                                        Text(
                                                            text = tipo.descripcion,
                                                            style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                                                fontSize = s.textLabel.value.sp * 0.82f
                                                            ),
                                                            color = colores.textoTerciario
                                                        )
                                                    }
                                                    Switch(
                                                        checked = marcado,
                                                        onCheckedChange = { onPagoSeleccionadoChanged(tipo.id, it) },
                                                        colors = SwitchDefaults.colors(
                                                            checkedThumbColor = colores.botonPrimarioFondo,
                                                            checkedTrackColor = colores.estadoExito.copy(alpha = 0.35f),
                                                            uncheckedThumbColor = colores.textoTerciario,
                                                            uncheckedTrackColor = colores.cardBorde.copy(alpha = 0.6f)
                                                        )
                                                    )
                                                }
                                                if (index < TIPOS_PAGO_FIJOS.size - 1) {
                                                    HorizontalDivider(color = colores.cardBorde.copy(alpha = 0.3f), thickness = s.borderWidth * 0.5f)
                                                }
                                            }
                                        }
                                    }
                                }

                                // ── COLUMNA DERECHA: PREVISUALIZACIÓN DE SEDE (SIMÉTRICA CON LA IZQUIERDA) ──
                                Column(
                                    modifier = Modifier.weight(0.9f),
                                    verticalArrangement = Arrangement.spacedBy(s.xs)
                                ) {
                                    Text(
                                        text = "PREVISUALIZACIÓN DE SEDE",
                                        style = TokensFarmadon.tipografia.etiqueta.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = s.textLabel.value.sp * 0.88f,
                                            letterSpacing = 0.8.sp
                                        ),
                                        color = colores.textoTerciario
                                    )

                                    Surface(
                                        color = colores.cardElevada,
                                        shape = RoundedCornerShape(s.radiusCard),
                                        border = BorderStroke(s.borderWidth * 1.1f, colores.textoPrincipal.copy(alpha = 0.22f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(s.sm),
                                            verticalArrangement = Arrangement.spacedBy(s.xs * 0.9f)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "EXPEDIENTE PREVIO",
                                                    style = TokensFarmadon.tipografia.etiqueta.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = s.textLabel.value.sp * 0.82f
                                                    ),
                                                    color = colores.textoTerciario
                                                )
                                                Surface(
                                                    color = colores.estadoExito.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(s.radiusChip)
                                                ) {
                                                    Text(
                                                        text = "🟢 LISTA PARA ALTA",
                                                        style = TokensFarmadon.tipografia.etiqueta.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = s.textLabel.value.sp * 0.75f
                                                        ),
                                                        color = colores.estadoExito,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            HorizontalDivider(color = colores.cardBorde.copy(alpha = 0.5f), thickness = s.borderWidth)

                                            // Identificación
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(s.xs)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(s.radiusChip))
                                                        .background(colores.botonPrimarioFondo.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Storefront,
                                                        contentDescription = null,
                                                        tint = colores.botonPrimarioFondo,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = state.formNombre.ifBlank { "Nombre Comercial de la Sede" },
                                                        style = TokensFarmadon.tipografia.titulo3.copy(
                                                            fontSize = s.textBody.value.sp * 1.05f,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = colores.textoPrincipal
                                                    )
                                                    Text(
                                                        text = "Código interno: ${state.formCodigoInterno.ifBlank { "SEDE-0X" }}",
                                                        style = TokensFarmadon.tipografia.etiqueta.copy(
                                                            fontSize = s.textLabel.value.sp * 0.85f,
                                                            fontWeight = FontWeight.SemiBold
                                                        ),
                                                        color = colores.textoTerciario
                                                    )
                                                }
                                            }

                                            // Datos clave
                                            listOf(
                                                "👤 Encargado" to state.formResponsable.ifBlank { "No asignado" },
                                                "📞 Teléfono" to state.formTelefono.ifBlank { "No registrado" },
                                                "📍 Ubicación" to state.formDireccion.ifBlank { "Sin geolocalización" }
                                            ).forEach { (label, value) ->
                                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                    Text(
                                                        text = label,
                                                        style = TokensFarmadon.tipografia.etiqueta.copy(
                                                            fontSize = s.textLabel.value.sp * 0.8f,
                                                            fontWeight = FontWeight.SemiBold
                                                        ),
                                                        color = colores.textoTerciario
                                                    )
                                                    Text(
                                                        text = value,
                                                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                                            fontSize = s.textBody.value.sp * 0.9f,
                                                            fontWeight = FontWeight.Medium
                                                        ),
                                                        color = colores.textoPrincipal,
                                                        maxLines = 2
                                                    )
                                                }
                                            }

                                            // Badges de Pagos Seleccionados
                                            Text(
                                                text = "💳 Cobros Habilitados (${state.formPagosSeleccionados.size})",
                                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                                    fontSize = s.textLabel.value.sp * 0.8f,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = colores.textoTerciario
                                            )

                                            @OptIn(ExperimentalLayoutApi::class)
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                val marcados = TIPOS_PAGO_FIJOS.filter { it.id in state.formPagosSeleccionados }
                                                if (marcados.isEmpty()) {
                                                    Text(
                                                        text = "Sin métodos seleccionados",
                                                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.85f),
                                                        color = colores.estadoPeligro
                                                    )
                                                } else {
                                                    marcados.forEach { p ->
                                                        Surface(
                                                            color = p.colorMarca.copy(alpha = 0.12f),
                                                            shape = RoundedCornerShape(s.radiusChip)
                                                        ) {
                                                            Text(
                                                                text = p.nombre,
                                                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = s.textLabel.value.sp * 0.78f
                                                                ),
                                                                color = p.colorMarca,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                } else {
                    // MODO VISTA DE EXPEDIENTE / TARJETA PRESENTATIVA CORPORATIVA DE SEDE
                    Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
                        // Tarjeta Executive con contraste mate sobre el panel
                        val esTemaOscuro = !colores.esTemaClaro
                        val fondoCardSede = if (esTemaOscuro) Color(0xFF1E2634) else Color.White
                        val bordeCardSede = if (esTemaOscuro) Color.White.copy(alpha = 0.18f) else Color(0xFFCBD5E1)

                        Surface(
                            color = fondoCardSede,
                            shape = RoundedCornerShape(s.radiusCard * 1.1f),
                            shadowElevation = if (esTemaOscuro) 6.dp else 3.dp,
                            border = BorderStroke(1.dp, bordeCardSede),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(s.sm),
                                verticalArrangement = Arrangement.spacedBy(s.xs)
                            ) {
                                // Cabecera con Nombre y Switch de Estado Operativo en fila limpia
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(s.xs)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(s.radiusChip))
                                                .background(colores.botonPrimarioFondo.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Storefront,
                                                contentDescription = null,
                                                tint = colores.botonPrimarioFondo,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(s.xs * 0.6f)
                                            ) {
                                                Text(
                                                    text = state.formNombre.ifBlank { "Sede" },
                                                    style = TokensFarmadon.tipografia.titulo2.copy(
                                                        fontSize = s.textTitle.value.sp * 0.88f,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = colores.textoPrincipal
                                                )
                                                if (esPrincipal) {
                                                    Surface(
                                                        color = colores.botonPrimarioFondo.copy(alpha = 0.18f),
                                                        shape = RoundedCornerShape(s.radiusChip * 0.5f)
                                                    ) {
                                                        Text(
                                                            text = "PRINCIPAL",
                                                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                                                fontSize = s.textLabel.value.sp * 0.72f,
                                                                fontWeight = FontWeight.Black
                                                            ),
                                                            color = colores.botonPrimarioFondo,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "Código interno: ${state.formCodigoInterno.ifBlank { "SEDE-01" }}",
                                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                                    fontSize = s.textLabel.value.sp * 0.82f,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = colores.textoTerciario
                                            )

                                            val selSede = state.sucursalSeleccionada
                                            if (selSede != null && selSede.serieBoleta.isNotBlank()) {
                                                Surface(
                                                    color = colores.textoPrincipal.copy(alpha = 0.06f),
                                                    shape = RoundedCornerShape(s.radiusChip * 0.4f),
                                                    border = BorderStroke(s.borderWidth * 0.6f, colores.cardBorde)
                                                ) {
                                                    Text(
                                                        text = "Serie ${selSede.serieBoleta} · ${selSede.serieFactura} · ${selSede.serieNotaCreditoBoleta} · ${selSede.serieNotaCreditoFactura}",
                                                        style = TokensFarmadon.tipografia.etiqueta.copy(
                                                            fontSize = s.textLabel.value.sp * 0.78f,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = colores.textoPrincipal,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Fila de Estado Operativo limpia (Sin card/chip verde envolvente)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(s.xs * 0.5f)
                                    ) {
                                        Text(
                                            text = if (state.formActiva) "🟢 OPERANDO" else "🟡 MANTENIMIENTO",
                                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = s.textLabel.value.sp * 0.82f
                                            ),
                                            color = if (state.formActiva) colores.estadoExito else colores.estadoAlerta
                                        )
                                        Switch(
                                            checked = state.formActiva,
                                            enabled = !esPrincipal,
                                            onCheckedChange = onActivaChanged,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = colores.estadoExito,
                                                checkedTrackColor = colores.estadoExito.copy(alpha = 0.35f),
                                                uncheckedThumbColor = colores.textoTerciario,
                                                uncheckedTrackColor = colores.cardBorde.copy(alpha = 0.6f)
                                            ),
                                            modifier = Modifier.scale(0.85f)
                                        )
                                    }
                                }

                                HorizontalDivider(color = colores.cardBorde.copy(alpha = 0.4f), thickness = s.borderWidth)

                                // Datos Clave Presentativos en 2 Columnas
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(s.xs)
                                    ) {
                                        Surface(
                                            color = colores.botonPrimarioFondo.copy(alpha = 0.12f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Person, null, tint = colores.botonPrimarioFondo, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                            Text(
                                                text = "RESPONSABLE",
                                                style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.78f, fontWeight = FontWeight.Bold),
                                                color = colores.textoTerciario
                                            )
                                            Text(
                                                text = state.formResponsable.ifBlank { "No asignado" },
                                                style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.95f, fontWeight = FontWeight.Bold),
                                                color = colores.textoPrincipal
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(s.xs)
                                    ) {
                                        Surface(
                                            color = colores.botonPrimarioFondo.copy(alpha = 0.12f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Phone, null, tint = colores.botonPrimarioFondo, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                            Text(
                                                text = "TELÉFONO",
                                                style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.78f, fontWeight = FontWeight.Bold),
                                                color = colores.textoTerciario
                                            )
                                            Text(
                                                text = state.formTelefono.ifBlank { "No registrado" },
                                                style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.95f, fontWeight = FontWeight.Bold),
                                                color = colores.textoPrincipal
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = colores.cardBorde.copy(alpha = 0.3f), thickness = s.borderWidth * 0.5f)

                                // Dirección Fiscal Presentativa (Sin input ni cajas grises)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                                ) {
                                    Surface(
                                        color = colores.botonPrimarioFondo.copy(alpha = 0.12f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.LocationOn, null, tint = colores.botonPrimarioFondo, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "DIRECCIÓN FISCAL REGISTRADA",
                                            style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.78f, fontWeight = FontWeight.Bold),
                                            color = colores.textoTerciario
                                        )
                                        Text(
                                            text = state.formDireccion.ifBlank { "Dirección no registrada" },
                                            style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.92f, fontWeight = FontWeight.SemiBold),
                                            color = colores.textoPrincipal
                                        )
                                    }
                                }
                            }
                        }

                        // Mapa de Geolocalización Compacto Estático (Sin tarjetas duplicadas ni pin movible)
                        UbicacionPasoMapaUberStyle(
                            direccionActual = state.formDireccion,
                            latitudActual = state.formLatitud,
                            longitudActual = state.formLongitud,
                            onUbicacionSeleccionada = { direccion, lat, lng ->
                                onAddressSelected(direccion, lat, lng)
                            },
                            s = s,
                            esModoEdicionEstatico = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(s.gapMedium))

            // Botones del pie de página
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(s.sm)
            ) {
                if (!state.esModoCreacion && !esPrincipal) {
                    OutlinedButton(
                        onClick = onSolicitarEliminar,
                        enabled = !state.guardando,
                        modifier = Modifier
                            .height(s.btnMediumH)
                            .align(Alignment.Start),
                        shape = RoundedCornerShape(s.radiusButton),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colores.estadoPeligro),
                        border = BorderStroke(s.borderWidth, colores.estadoPeligro.copy(alpha = 0.25f))
                    ) {
                        Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(s.iconSmall))
                        Spacer(Modifier.width(s.xs * 0.8f))
                        Text(
                            "ELIMINAR",
                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = s.textLabel.value.sp
                            )
                        )
                    }
                }

                if (state.esModoCreacion) {
                    val puedePaso1 = state.formNombre.isNotBlank() && state.formTelefono.isNotBlank() && state.formResponsable.isNotBlank()
                    val puedePaso2 = state.formLatitud != null && state.formLongitud != null && state.formDireccion.isNotBlank()
                    val puedeFinalizar = state.formPagosSeleccionados.isNotEmpty() && puedePaso1 && puedePaso2
                    val puedeContinuar = when (state.pasoActual) {
                        1 -> puedePaso1 && !state.guardando
                        2 -> puedePaso2 && !state.guardando
                        else -> puedeFinalizar && !state.guardando
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.pasoActual > 1) {
                            OutlinedButton(
                                onClick = onPasoAnterior,
                                modifier = Modifier.height(s.btnMediumH),
                                shape = RoundedCornerShape(s.radiusButton),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colores.textoTerciario),
                                border = BorderStroke(s.borderWidth, colores.cardBorde.copy(alpha = 0.8f))
                            ) {
                                Text(
                                    "VOLVER",
                                    style = TokensFarmadon.tipografia.etiqueta.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = s.textLabel.value.sp
                                    )
                                )
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        if (puedeContinuar) {
                            Button(
                                onClick = if (state.pasoActual < 3) onSiguientePaso else onGuardar,
                                modifier = Modifier
                                    .height(s.btnMediumH)
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(s.radiusButton),
                                        ambientColor = colores.botonPrimarioFondo.copy(alpha = 0.18f),
                                        spotColor = colores.botonPrimarioFondo.copy(alpha = 0.22f)
                                    )
                                    .bounceClick(),
                                enabled = true,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colores.botonPrimarioFondo,
                                    contentColor = colores.botonPrimarioTexto,
                                    disabledContainerColor = colores.textoPrincipal.copy(alpha = 0.05f),
                                    disabledContentColor = colores.textoTerciario.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(s.radiusButton)
                            ) {
                                if (state.guardando) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(s.iconSmall),
                                        color = colores.botonPrimarioTexto,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = when (state.pasoActual) {
                                            1 -> "SIGUIENTE"
                                            2 -> "SIGUIENTE"
                                            else -> "CREAR SEDE"
                                        },
                                        style = TokensFarmadon.tipografia.etiqueta.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = s.textLabel.value.sp,
                                            letterSpacing = 0.4.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val puedeGuardar = !state.guardando && state.hayCambiosSinGuardar

                    if (puedeGuardar) {
                        Button(
                            onClick = onGuardar,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(s.btnMediumH)
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(s.radiusButton),
                                    ambientColor = colores.botonPrimarioFondo.copy(alpha = 0.18f),
                                    spotColor = colores.botonPrimarioFondo.copy(alpha = 0.22f)
                                )
                                .bounceClick(),
                            enabled = true,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colores.botonPrimarioFondo,
                                contentColor = colores.botonPrimarioTexto,
                                disabledContainerColor = colores.textoPrincipal.copy(alpha = 0.05f),
                                disabledContentColor = colores.textoTerciario.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(s.radiusButton)
                        ) {
                            if (state.guardando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(s.iconSmall),
                                    color = colores.botonPrimarioTexto,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "GUARDAR EXPEDIENTE",
                                    style = TokensFarmadon.tipografia.etiqueta.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = s.textLabel.value.sp,
                                        letterSpacing = 0.4.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
