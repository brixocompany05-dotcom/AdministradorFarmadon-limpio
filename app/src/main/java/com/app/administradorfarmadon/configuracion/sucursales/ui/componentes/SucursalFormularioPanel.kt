package com.app.administradorfarmadon.configuracion.sucursales.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.autenticacion.login.ui.componentes.ExecutiveInput
import com.app.administradorfarmadon.configuracion.sucursales.logica.SucursalesUiState
import com.app.administradorfarmadon.configuracion.metodospago.modelo.TIPOS_PAGO_FIJOS
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors

import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.platform.LocalContext

/**
 * Panel Sede — Geometría Física 2026
 * Documento adaptativo simétrico, nada fijo 24/20/48dp.
 * Todo deriva de s (f= (W/1280)^0.55 ) + viewport proporcional.
 * Colores 100% tokens claro/oscuro, mapa altura adaptativa.
 * Sin scroll parche: gaps escalados, scroll solo fallback suave.
 */
@Composable
fun SucursalFormularioPanel(
    state: SucursalesUiState,
    onFieldChanged: (String, String) -> Unit,
    onActivaChanged: (Boolean) -> Unit,
    onPagoSeleccionadoChanged: (String, Boolean) -> Unit,
    onOpenMapPicker: () -> Unit,
    onGuardar: () -> Unit,
    onSolicitarEliminar: () -> Unit,
    onCerrarPanel: () -> Unit,
    s: MedidaAdaptativa,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val colores = TokensFarmadon.colores
    val context = LocalContext.current
    val esPrincipal = state.sucursalSeleccionada?.esPrincipal == true

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
            // Cabecera — gaps y tamaños s.*
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
                    Icon(Icons.Default.Close, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconSmall))
                }
            }

            Spacer(modifier = Modifier.height(s.gapMedium))

            // Cuerpo — gaps s.md, nada fijo 20dp
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                if (state.mensajeError != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colores.alertaSutil,
                        shape = RoundedCornerShape(s.radiusChip),
                        border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.estadoAlerta)
                    ) {
                        Row(
                            modifier = Modifier.padding(s.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.xs)
                        ) {
                            Icon(Icons.Default.WarningAmber, null, tint = colores.estadoAlerta, modifier = Modifier.size(s.iconSmall))
                            Text(
                                text = state.mensajeError,
                                style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp),
                                color = colores.textoPrincipal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // SECCIÓN 1: ESTADO OPERATIVO — altura s.inputMinH, radios s.radiusInput, dots s.xs*0.9
                Column(verticalArrangement = Arrangement.spacedBy(s.xs)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.8f)) {
                        Icon(Icons.Default.PowerSettingsNew, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconTiny))
                        Text(
                            "ESTADO OPERATIVO DE LA SEDE",
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp * 0.9f, letterSpacing = 0.8.sp),
                            color = colores.textoTerciario
                        )
                    }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(s.inputMinH),
                            color = colores.fondoBase,
                            shape = RoundedCornerShape(s.radiusInput),
                            border = androidx.compose.foundation.BorderStroke(s.borderWidth * 0.8f, colores.cardBorde)
                        ) {
                            Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(s.xs * 0.7f)) {
                                val estados = listOf(true to "OPERANDO", false to "MANTENIMIENTO")
                                estados.forEach { (activa, label) ->
                                    val isSelected = state.formActiva == activa
                                    val enabled = !esPrincipal || activa

                                    val selectedColor = if (activa) colores.estadoExito else colores.estadoPeligro
                                    val bgColor = if (isSelected) selectedColor.copy(alpha = 0.12f) else Color.Transparent
                                    val textColor = if (!enabled) colores.textoTerciario.copy(alpha = 0.3f)
                                                    else if (isSelected) selectedColor
                                                    else colores.textoTerciario

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(s.radiusChip))
                                            .clickable(enabled = enabled) { onActivaChanged(activa) },
                                        color = bgColor,
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(s.borderWidth * 1.2f, selectedColor) else null,
                                        shape = RoundedCornerShape(s.radiusChip)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(s.xs * 0.75f)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(if (isSelected) selectedColor else colores.textoTerciario.copy(alpha = 0.4f))
                                            )
                                            Spacer(Modifier.width(s.xs))
                                            Text(
                                                text = label,
                                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                                    fontSize = s.textLabel.value.sp,
                                                    fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    letterSpacing = 0.4.sp
                                                ),
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (esPrincipal) {
                            Text(
                                "La sede matriz es el núcleo fiscal y no puede ser desactivada.",
                                style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.95f),
                                color = colores.textoTerciario.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = s.xs * 0.5f)
                            )
                        }
                    }

                    HorizontalDivider(color = colores.cardBorde.copy(alpha = 0.4f), thickness = s.separatorH)

                    // SECCIÓN 2: DETALLES COMERCIALES — gaps s.sm
                    Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.8f)) {
                            Icon(Icons.Default.Business, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconTiny))
                            Text(
                                "DETALLES COMERCIALES",
                                style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp * 0.9f, letterSpacing = 0.8.sp),
                                color = colores.textoTerciario
                            )
                        }

                        ExecutiveInput(
                            s = s,
                            label = "Nombre de la Sede",
                            value = state.formNombre,
                            icon = Icons.Default.Storefront,
                            placeholder = "Ej. Farmacia Farmadon - Principal",
                            errorText = state.formErrores["nombre"],
                            onValueChange = { onFieldChanged("nombre", it) }
                        )

                        ExecutiveInput(
                            s = s,
                            label = "Administrador responsable",
                            value = state.formResponsable,
                            icon = Icons.Default.Person,
                            placeholder = "Nombre del encargado de la sede...",
                            errorText = state.formErrores["responsable"],
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
                            onValueChange = { onFieldChanged("telefono", it) }
                        )

                        if (!state.esModoCreacion && state.formCodigoInterno.isNotBlank()) {
                            ExecutiveInput(
                                s = s,
                                label = "Código Interno Asignado",
                                value = state.formCodigoInterno,
                                icon = Icons.Default.Tag,
                                readOnly = true,
                                placeholder = "SEDE-01",
                                onValueChange = {}
                            )
                        }
                    }

                    // SECCIÓN 3 (solo al crear): CONTRATO DE MÉTODOS DE PAGO DE LA SEDE NUEVA.
                    // Todos vienen marcados por defecto; desmarcar excluye ese pago del nacimiento.
                    if (state.esModoCreacion) {
                        HorizontalDivider(color = colores.cardBorde.copy(alpha = 0.4f), thickness = s.separatorH)
                        Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.8f)) {
                                Icon(Icons.Default.Payments, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconTiny))
                                Text(
                                    "MÉTODOS DE PAGO DE LA SEDE",
                                    style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp * 0.9f, letterSpacing = 0.8.sp),
                                    color = colores.textoTerciario
                                )
                            }
                            Text(
                                "Esta sede nacerá con estos métodos disponibles. Todos vienen marcados; desmarca los que esta sede no usará.",
                                style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.95f),
                                color = colores.textoTerciario
                            )

                            val todosMarcados = state.formPagosSeleccionados.size == TIPOS_PAGO_FIJOS.size
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Incluir todos los métodos",
                                    style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp * 0.95f, fontWeight = FontWeight.SemiBold),
                                    color = colores.textoPrincipal
                                )
                                Switch(
                                    checked = todosMarcados,
                                    onCheckedChange = { marcar ->
                                        TIPOS_PAGO_FIJOS.forEach { tipo -> onPagoSeleccionadoChanged(tipo.id, marcar) }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colores.botonPrimarioFondo,
                                        checkedTrackColor = colores.estadoExito.copy(alpha = 0.35f),
                                        uncheckedThumbColor = colores.textoTerciario,
                                        uncheckedTrackColor = colores.cardBorde.copy(alpha = 0.6f),
                                        uncheckedBorderColor = colores.cardBorde
                                    )
                                )
                            }

                            state.formErrores["pagos"]?.let { msg ->
                                Text(
                                    text = msg,
                                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.95f, fontWeight = FontWeight.Bold),
                                    color = colores.estadoPeligro
                                )
                            }

                            TIPOS_PAGO_FIJOS.forEach { tipo ->
                                val marcado = tipo.id in state.formPagosSeleccionados
                                val sinDisponibilidad = tipo.id in state.pagosMarcadosSinDisponibilidad
                                Surface(
                                    onClick = { onPagoSeleccionadoChanged(tipo.id, !marcado) },
                                    color = if (marcado) colores.estadoExito.copy(alpha = 0.07f) else colores.fondoBase,
                                    shape = RoundedCornerShape(s.radiusChip),
                                    border = androidx.compose.foundation.BorderStroke(
                                        s.borderWidth * 0.8f,
                                        if (marcado) colores.estadoExito.copy(alpha = 0.35f) else colores.cardBorde
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = s.sm, vertical = s.xs * 0.85f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(s.xs)
                                    ) {
                                        Icon(tipo.icono, null, tint = tipo.colorMarca, modifier = Modifier.size(s.iconSmall))
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                tipo.nombre,
                                                style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Bold),
                                                color = colores.textoPrincipal
                                            )
                                            Text(
                                                tipo.descripcion,
                                                style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.92f),
                                                color = colores.textoTerciario
                                            )
                                            if (sinDisponibilidad) {
                                                Text(
                                                    "La sede principal aún no configura este pago: la sede nacerá con la marca, pero sin cuenta hasta que lo configures en Métodos de Pago.",
                                                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.9f, fontWeight = FontWeight.SemiBold),
                                                    color = colores.estadoAlerta
                                                )
                                            }
                                        }
                                        Checkbox(
                                            checked = marcado,
                                            onCheckedChange = { onPagoSeleccionadoChanged(tipo.id, it) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = colores.estadoExito,
                                                uncheckedColor = colores.textoTerciario.copy(alpha = 0.5f),
                                                checkmarkColor = colores.fondoBase
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECCIÓN 3 (solo la sede principal): las sedes hijas nacen sin
                    // dirección física, como se decidió en producto — cero falsedad.
                    if (esPrincipal) {
                    HorizontalDivider(color = colores.cardBorde.copy(alpha = 0.4f), thickness = s.separatorH)

                    // SECCIÓN 3: GEOLOCALIZACIÓN — mapa altura adaptativa s.btnLargeH*3.3 (~180dp escalado)
                    Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.8f)) {
                            Icon(Icons.Default.LocationOn, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconTiny))
                            Text(
                                "GEOLOCALIZACIÓN Y UBICACIÓN",
                                style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp * 0.9f, letterSpacing = 0.8.sp),
                                color = colores.textoTerciario
                            )
                        }

                        ExecutiveInput(
                            s = s,
                            label = "Dirección fiscal completa",
                            value = state.formDireccion,
                            icon = Icons.Default.LocationOn,
                            placeholder = "Av. Principal 123...",
                            errorText = state.formErrores["direccion"],
                            onValueChange = { onFieldChanged("direccion", it) }
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(s.btnLargeH * 3.4f)
                                .clip(RoundedCornerShape(s.radiusInput)),
                            color = if (colores.esTemaClaro) colores.fondoBase else colores.textoPrincipal.copy(alpha = 0.03f),
                            border = androidx.compose.foundation.BorderStroke(s.borderWidth * 0.8f, colores.cardBorde)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (state.formLatitud != null && state.formLongitud != null) {
                                    AndroidView(
                                        factory = { ctx ->
                                            MapView(ctx).apply {
                                                setTileSource(TileSourceFactory.MAPNIK)
                                                setMultiTouchControls(false)
                                                setBuiltInZoomControls(false)
                                                isClickable = false
                                                isFocusable = false
                                                isVerticalMapRepetitionEnabled = false
                                                isHorizontalMapRepetitionEnabled = false
                                                setOnTouchListener { _, _ -> false }

                                                controller.setZoom(16.5)
                                                val point = GeoPoint(state.formLatitud, state.formLongitud)
                                                controller.setCenter(point)

                                                val marker = Marker(this)
                                                marker.position = point
                                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                marker.icon = ctx.getDrawable(org.osmdroid.library.R.drawable.marker_default)
                                                overlays.add(marker)
                                            }
                                        },
                                        update = { view ->
                                            val point = GeoPoint(state.formLatitud, state.formLongitud)
                                            view.controller.setCenter(point)
                                            view.overlays.filterIsInstance<Marker>().firstOrNull()?.let { marker ->
                                                marker.position = point
                                                view.invalidate()
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(s.xs),
                                        horizontalArrangement = Arrangement.spacedBy(s.xs * 0.8f)
                                    ) {
                                        Surface(
                                            onClick = {
                                                val gmmIntentUri = android.net.Uri.parse("geo:${state.formLatitud},${state.formLongitud}?q=${state.formLatitud},${state.formLongitud}(${state.formNombre})")
                                                val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                try {
                                                    context.startActivity(mapIntent)
                                                } catch (e: Exception) {
                                                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri))
                                                }
                                            },
                                            color = colores.cardBase.copy(alpha = 0.92f),
                                            shape = RoundedCornerShape(s.radiusChip),
                                            border = androidx.compose.foundation.BorderStroke(s.borderWidth * 0.6f, colores.cardBorde)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = s.xs, vertical = s.xs * 0.7f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(s.xs * 0.5f)
                                            ) {
                                                Icon(Icons.Default.Navigation, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconTiny))
                                                Text("NAVEGAR", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.85f, fontWeight = FontWeight.Bold), color = colores.textoPrincipal)
                                            }
                                        }

                                        Surface(
                                            onClick = onOpenMapPicker,
                                            color = colores.cardBase.copy(alpha = 0.92f),
                                            shape = RoundedCornerShape(s.radiusChip),
                                            border = androidx.compose.foundation.BorderStroke(s.borderWidth * 0.6f, colores.cardBorde)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = s.xs, vertical = s.xs * 0.7f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(s.xs * 0.5f)
                                            ) {
                                                Icon(Icons.Default.EditLocationAlt, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconTiny))
                                                Text("CAMBIAR", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp * 0.85f, fontWeight = FontWeight.Bold), color = colores.textoPrincipal)
                                            }
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { onOpenMapPicker() },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Map, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconLarge))
                                        Spacer(Modifier.height(s.xs))
                                        Text("ASIGNAR UBICACIÓN", style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp), color = colores.textoTerciario)
                                    }
                                }
                            }
                        }
                    }
                    }

                }

                Spacer(modifier = Modifier.height(s.gapMedium))

            // ── ACCIÓN DOMINANTE — altura s.btnMediumH simétrica
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(s.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!state.esModoCreacion && !esPrincipal) {
                    OutlinedButton(
                        onClick = onSolicitarEliminar,
                        enabled = !state.guardando,
                        modifier = Modifier.height(s.btnMediumH),
                        shape = RoundedCornerShape(s.radiusButton),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colores.estadoPeligro),
                        border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.estadoPeligro.copy(alpha = 0.25f))
                    ) {
                        Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(s.iconSmall))
                        Spacer(Modifier.width(s.xs * 0.8f))
                        Text("ELIMINAR", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp))
                    }
                }

                val puedeGuardar = if (state.esModoCreacion) {
                    !state.guardando && state.formNombre.isNotBlank() && state.formTelefono.isNotBlank() && state.formResponsable.isNotBlank()
                } else {
                    !state.guardando && state.hayCambiosSinGuardar
                }

                Button(
                    onClick = onGuardar,
                    modifier = Modifier.weight(1f).height(s.btnMediumH).bounceClick(),
                    enabled = puedeGuardar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colores.botonPrimarioFondo,
                        contentColor = colores.botonPrimarioTexto,
                        disabledContainerColor = colores.textoPrincipal.copy(alpha = 0.05f),
                        disabledContentColor = colores.textoTerciario.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(s.radiusButton)
                ) {
                    if (state.guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(s.iconSmall), color = colores.botonPrimarioTexto, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (state.esModoCreacion) "CREAR NUEVA SEDE" else "GUARDAR EXPEDIENTE",
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp, letterSpacing = 0.4.sp)
                        )
                    }
                }
            }
        }
    }
}
