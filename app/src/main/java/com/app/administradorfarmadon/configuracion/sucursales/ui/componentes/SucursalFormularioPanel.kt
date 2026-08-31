package com.app.administradorfarmadon.configuracion.sucursales.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.app.administradorfarmadon.autenticacion.login.ui.componentes.ExecutiveInput
import com.app.administradorfarmadon.configuracion.metodospago.modelo.TIPOS_PAGO_FIJOS
import com.app.administradorfarmadon.configuracion.sucursales.logica.SucursalesUiState
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

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
    onSiguientePaso: () -> Unit,
    onPasoAnterior: () -> Unit,
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
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = colores.textoPrincipal,
                        modifier = Modifier.size(s.iconSmall)
                    )
                }
            }

            Spacer(modifier = Modifier.height(s.gapMedium))

            // Cuerpo — gaps s.md, nada fijo 20dp
            val scrollableBody = !(state.esModoCreacion && state.pasoActual == 2)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (scrollableBody) Modifier.verticalScroll(scrollState) else Modifier)
                    .imePadding(), verticalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                if (state.mensajeError != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colores.alertaSutil,
                        shape = RoundedCornerShape(s.radiusChip),
                        border = androidx.compose.foundation.BorderStroke(
                            s.borderWidth, colores.estadoAlerta
                        )
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
                        }
                    }
                }

                if (state.esModoCreacion) {
                    val pasoActual = state.pasoActual

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colores.cardElevada,
                        shape = RoundedCornerShape(s.radiusInput),
                        border = androidx.compose.foundation.BorderStroke(
                            s.borderWidth * 0.8f, colores.cardBorde.copy(alpha = 0.65f)
                        )
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(s.gapMedium),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(s.sm)
                        ) {
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
                                            onValueChange = { onFieldChanged("nombre", it) })

                                        ExecutiveInput(
                                            s = s,
                                            label = "Administrador responsable",
                                            value = state.formResponsable,
                                            icon = Icons.Default.Person,
                                            placeholder = "Nombre del encargado de la sede...",
                                            errorText = state.formErrores["responsable"],
                                            onValueChange = { onFieldChanged("responsable", it) })

                                        ExecutiveInput(
                                            s = s,
                                            label = "Teléfono de contacto",
                                            value = state.formTelefono,
                                            icon = Icons.Default.Phone,
                                            keyboardType = KeyboardType.Phone,
                                            placeholder = "987 654 321",
                                            errorText = state.formErrores["telefono"],
                                            onValueChange = { onFieldChanged("telefono", it) })
                                    }
                                }

                                2 -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(s.sm)
                                    ) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = colores.cardElevada,
                                            shape = RoundedCornerShape(s.radiusInput),
                                            border = androidx.compose.foundation.BorderStroke(
                                                s.borderWidth * 0.8f, colores.cardBorde.copy(alpha = 0.7f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = s.sm, vertical = s.xs),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(s.xs)
                                            ) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    null,
                                                    tint = colores.botonPrimarioFondo,
                                                    modifier = Modifier.size(s.iconSmall)
                                                )
                                                Text(
                                                    text = if (state.formDireccion.isNotBlank()) state.formDireccion else "Ubicación por pin en el centro del mapa",
                                                    style = TokensFarmadon.tipografia.cuerpo.copy(
                                                        fontSize = s.textBody.value.sp * 0.92f,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = colores.textoPrincipal,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 420.dp, max = 620.dp)
                                                .clip(RoundedCornerShape(s.radiusInput)),
                                            color = if (colores.esTemaClaro) colores.fondoBase else colores.textoPrincipal.copy(
                                                alpha = 0.03f
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                s.borderWidth * 0.8f, colores.cardBorde
                                            )
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                AndroidView(
                                                    factory = { ctx ->
                                                        MapView(ctx).apply {
                                                            setTileSource(TileSourceFactory.MAPNIK)
                                                            setMultiTouchControls(true)
                                                            setBuiltInZoomControls(false)
                                                            isClickable = true
                                                            isFocusable = true
                                                            isVerticalMapRepetitionEnabled = false
                                                            isHorizontalMapRepetitionEnabled = false
                                                            controller.setZoom(16.5)
                                                            val point = if (state.formLatitud != null && state.formLongitud != null) {
                                                                GeoPoint(state.formLatitud, state.formLongitud)
                                                            } else {
                                                                GeoPoint(-12.0464, -77.0428)
                                                            }
                                                            controller.setCenter(point)
                                                            val marker = Marker(this)
                                                            marker.position = point
                                                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                            marker.icon = ctx.getDrawable(org.osmdroid.library.R.drawable.marker_default)
                                                            overlays.add(marker)
                                                        }
                                                    },
                                                    update = { view ->
                                                        val point = if (state.formLatitud != null && state.formLongitud != null) {
                                                            GeoPoint(state.formLatitud, state.formLongitud)
                                                        } else {
                                                            GeoPoint(-12.0464, -77.0428)
                                                        }
                                                        view.controller.setCenter(point)
                                                        view.overlays.filterIsInstance<Marker>().firstOrNull()?.let { marker ->
                                                            marker.position = point
                                                            view.invalidate()
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                Surface(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .fillMaxWidth()
                                                        .padding(s.sm),
                                                    color = colores.cardBase.copy(alpha = 0.96f),
                                                    shape = RoundedCornerShape(s.radiusInput),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        s.borderWidth * 0.8f, colores.cardBorde.copy(alpha = 0.7f)
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(s.sm),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(s.xs)
                                                    ) {
                                                        Surface(
                                                            color = colores.botonPrimarioFondo.copy(alpha = 0.12f),
                                                            shape = RoundedCornerShape(s.radiusChip),
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    Icons.Default.LocationOn,
                                                                    null,
                                                                    tint = colores.botonPrimarioFondo,
                                                                    modifier = Modifier.size(s.iconSmall)
                                                                )
                                                            }
                                                        }

                                                        Column(
                                                            modifier = Modifier.weight(1f),
                                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Text(
                                                                "Dirección exacta",
                                                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                                                    fontSize = s.textLabel.value.sp * 0.8f,
                                                                    letterSpacing = 0.5.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                ),
                                                                color = colores.textoTerciario
                                                            )
                                                            Text(
                                                                text = if (state.formDireccion.isNotBlank()) state.formDireccion else "Pin central: ubicación por confirmar",
                                                                style = TokensFarmadon.tipografia.cuerpo.copy(
                                                                    fontSize = s.textBody.value.sp * 0.9f,
                                                                    fontWeight = FontWeight.Medium
                                                                ),
                                                                color = colores.textoPrincipal,
                                                                maxLines = 2
                                                            )
                                                        }
                                                    }
                                                }

                                                Surface(
                                                    onClick = onOpenMapPicker,
                                                    color = colores.cardBase.copy(alpha = 0.94f),
                                                    shape = RoundedCornerShape(s.radiusChip),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        s.borderWidth * 0.6f, colores.cardBorde
                                                    ),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(s.xs)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(
                                                            horizontal = s.xs, vertical = s.xs * 0.7f
                                                        ),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(s.xs * 0.5f)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.EditLocationAlt,
                                                            null,
                                                            tint = colores.textoPrincipal,
                                                            modifier = Modifier.size(s.iconTiny)
                                                        )
                                                        Text(
                                                            "UBICAR",
                                                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                                                fontSize = s.textLabel.value.sp * 0.85f,
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = colores.textoPrincipal
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                3 -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(s.gapMedium),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(s.sm)
                                        ) {
                                            Text(
                                                "Esta sede nacerá con estos métodos disponibles. Todos vienen marcados; desmarca los que esta sede no usará.",
                                                style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.95f),
                                                color = colores.textoTerciario
                                            )

                                            val todosMarcados =
                                                state.formPagosSeleccionados.size == TIPOS_PAGO_FIJOS.size
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "Incluir todos los métodos",
                                                    style = TokensFarmadon.tipografia.cuerpo.copy(
                                                        fontSize = s.textBody.value.sp * 0.95f,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = colores.textoPrincipal
                                                )
                                                Switch(
                                                    checked = todosMarcados, onCheckedChange = { marcar ->
                                                        TIPOS_PAGO_FIJOS.forEach { tipo ->
                                                            onPagoSeleccionadoChanged(
                                                                tipo.id, marcar
                                                            )
                                                        }
                                                    }, colors = SwitchDefaults.colors(
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
                                                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                                        fontSize = s.textLabel.value.sp * 0.95f,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = colores.estadoPeligro
                                                )
                                            }

                                            TIPOS_PAGO_FIJOS.forEach { tipo ->
                                                val marcado = tipo.id in state.formPagosSeleccionados
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
                                                        modifier = Modifier.padding(
                                                            horizontal = s.sm, vertical = s.xs * 0.85f
                                                        ),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(s.xs)
                                                    ) {
                                                        Icon(
                                                            tipo.icono,
                                                            null,
                                                            tint = tipo.colorMarca,
                                                            modifier = Modifier.size(s.iconSmall)
                                                        )
                                                        Column(
                                                            modifier = Modifier.weight(1f),
                                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Text(
                                                                tipo.nombre,
                                                                style = TokensFarmadon.tipografia.cuerpo.copy(
                                                                    fontSize = s.textBody.value.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                ),
                                                                color = colores.textoPrincipal
                                                            )
                                                            Text(
                                                                tipo.descripcion,
                                                                style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                                                    fontSize = s.textLabel.value.sp * 0.92f
                                                                ),
                                                                color = colores.textoTerciario
                                                            )
                                                        }
                                                        Checkbox(
                                                            checked = marcado, onCheckedChange = {
                                                                onPagoSeleccionadoChanged(
                                                                    tipo.id, it
                                                                )
                                                            }, colors = CheckboxDefaults.colors(
                                                                checkedColor = colores.estadoExito,
                                                                uncheckedColor = colores.textoTerciario.copy(
                                                                    alpha = 0.5f
                                                                ),
                                                                checkmarkColor = colores.fondoBase
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier.widthIn(min = 220.dp, max = 300.dp),
                                            color = colores.cardElevada,
                                            shape = RoundedCornerShape(s.radiusInput),
                                            border = androidx.compose.foundation.BorderStroke(
                                                s.borderWidth * 0.8f, colores.cardBorde.copy(alpha = 0.7f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(s.sm),
                                                verticalArrangement = Arrangement.spacedBy(s.sm)
                                            ) {
                                                Text(
                                                    "RESUMEN OPERATIVO",
                                                    style = TokensFarmadon.tipografia.etiqueta.copy(
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = s.textLabel.value.sp * 0.85f,
                                                        letterSpacing = 0.8.sp
                                                    ),
                                                    color = colores.textoTerciario
                                                )

                                                val resumenNombre = state.formNombre.ifBlank { "Sin nombre" }
                                                val resumenResponsable = state.formResponsable.ifBlank { "Sin responsable" }
                                                val resumenTelefono = state.formTelefono.ifBlank { "Sin teléfono" }
                                                val resumenUbicacion = if (state.formLatitud != null && state.formLongitud != null) "Ubicación fijada" else "Sin ubicación"
                                                val resumenPagos = if (state.formPagosSeleccionados.isNotEmpty()) "${state.formPagosSeleccionados.size} métodos" else "Sin métodos"

                                                listOf(
                                                    "Sede" to resumenNombre,
                                                    "Responsable" to resumenResponsable,
                                                    "Teléfono" to resumenTelefono,
                                                    "Ubicación" to resumenUbicacion,
                                                    "Pagos" to resumenPagos
                                                ).forEach { (label, value) ->
                                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Text(
                                                            label,
                                                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                                                fontSize = s.textLabel.value.sp * 0.82f,
                                                                fontWeight = FontWeight.SemiBold
                                                            ),
                                                            color = colores.textoTerciario
                                                        )
                                                        Text(
                                                            value,
                                                            style = TokensFarmadon.tipografia.cuerpo.copy(
                                                                fontSize = s.textBody.value.sp * 0.95f,
                                                                fontWeight = FontWeight.Medium
                                                            ),
                                                            color = colores.textoPrincipal
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
                } else {
                    // SECCIÓN 1: ESTADO OPERATIVO — altura s.inputMinH, radios s.radiusInput, dots s.xs*0.9
                    Column(verticalArrangement = Arrangement.spacedBy(s.xs)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.xs * 0.8f)
                        ) {
                            Icon(
                                Icons.Default.PowerSettingsNew,
                                null,
                                tint = colores.textoTerciario,
                                modifier = Modifier.size(s.iconTiny)
                            )
                            Text(
                                "ESTADO OPERATIVO DE LA SEDE",
                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = s.textLabel.value.sp * 0.9f,
                                    letterSpacing = 0.8.sp
                                ),
                                color = colores.textoTerciario
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(s.inputMinH),
                            color = colores.fondoBase,
                            shape = RoundedCornerShape(s.radiusInput),
                            border = androidx.compose.foundation.BorderStroke(
                                s.borderWidth * 0.8f, colores.cardBorde
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(s.xs * 0.7f)
                            ) {
                                val estados = listOf(true to "OPERANDO", false to "MANTENIMIENTO")
                                estados.forEach { (activa, label) ->
                                    val isSelected = state.formActiva == activa
                                    val enabled = !esPrincipal || activa

                                    val selectedColor =
                                        if (activa) colores.estadoExito else colores.estadoPeligro
                                    val bgColor =
                                        if (isSelected) selectedColor.copy(alpha = 0.12f) else Color.Transparent
                                    val textColor =
                                        if (!enabled) colores.textoTerciario.copy(alpha = 0.3f)
                                        else if (isSelected) selectedColor
                                        else colores.textoTerciario

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(s.radiusChip))
                                            .clickable(enabled = enabled) { onActivaChanged(activa) },
                                        color = bgColor,
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                            s.borderWidth * 1.2f, selectedColor
                                        ) else null,
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
                                                    .background(
                                                        if (isSelected) selectedColor else colores.textoTerciario.copy(
                                                            alpha = 0.4f
                                                        )
                                                    )
                                            )
                                            Spacer(Modifier.width(s.xs))
                                            Text(
                                                text = label,
                                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                                    fontSize = s.textLabel.value.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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

                    HorizontalDivider(
                        color = colores.cardBorde.copy(alpha = 0.4f), thickness = s.separatorH
                    )

                    // SECCIÓN 2: DETALLES COMERCIALES — gaps s.sm
                    Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.xs * 0.8f)
                        ) {
                            Icon(
                                Icons.Default.Business,
                                null,
                                tint = colores.textoTerciario,
                                modifier = Modifier.size(s.iconTiny)
                            )
                            Text(
                                "DETALLES COMERCIALES",
                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = s.textLabel.value.sp * 0.9f,
                                    letterSpacing = 0.8.sp
                                ),
                                color = colores.textoTerciario
                            )
                        }

                        ExecutiveInput(
                            s = s,
                            label = "Nombre de la Sede",
                            value = state.formNombre,
                            icon = Icons.Default.Storefront,
                            placeholder = "Nombre comercial de la sede",
                            errorText = state.formErrores["nombre"],
                            onValueChange = { onFieldChanged("nombre", it) })

                        ExecutiveInput(
                            s = s,
                            label = "Administrador responsable",
                            value = state.formResponsable,
                            icon = Icons.Default.Person,
                            placeholder = "Nombre del encargado de la sede...",
                            errorText = state.formErrores["responsable"],
                            onValueChange = { onFieldChanged("responsable", it) })

                        ExecutiveInput(
                            s = s,
                            label = "Teléfono de contacto",
                            value = state.formTelefono,
                            icon = Icons.Default.Phone,
                            keyboardType = KeyboardType.Phone,
                            placeholder = "987 654 321",
                            errorText = state.formErrores["telefono"],
                            onValueChange = { onFieldChanged("telefono", it) })

                        if (!state.esModoCreacion && state.formCodigoInterno.isNotBlank()) {
                            ExecutiveInput(
                                s = s,
                                label = "Código Interno Asignado",
                                value = state.formCodigoInterno,
                                icon = Icons.Default.Tag,
                                readOnly = true,
                                placeholder = "SEDE-01",
                                onValueChange = {})
                        }
                    }

                    if (esPrincipal) {
                        HorizontalDivider(
                            color = colores.cardBorde.copy(alpha = 0.4f), thickness = s.separatorH
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(s.sm)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(s.xs * 0.8f)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    null,
                                    tint = colores.textoTerciario,
                                    modifier = Modifier.size(s.iconTiny)
                                )
                                Text(
                                    "GEOLOCALIZACIÓN Y UBICACIÓN",
                                    style = TokensFarmadon.tipografia.etiqueta.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = s.textLabel.value.sp * 0.9f,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = colores.textoTerciario
                                )
                            }

                            ExecutiveInput(
                                s = s,
                                label = "Dirección fiscal completa",
                                value = state.formDireccion,
                                icon = Icons.Default.LocationOn,
                                placeholder = "Se completa desde la ubicación del pin",
                                readOnly = true,
                                errorText = state.formErrores["direccion"],
                                onValueChange = {})

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(s.btnLargeH * 3.4f)
                                    .clip(RoundedCornerShape(s.radiusInput)),
                                color = if (colores.esTemaClaro) colores.fondoBase else colores.textoPrincipal.copy(
                                    alpha = 0.03f
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    s.borderWidth * 0.8f, colores.cardBorde
                                )
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
                                                val point = GeoPoint(
                                                    state.formLatitud, state.formLongitud
                                                )
                                                controller.setCenter(point)
                                                val marker = Marker(this)
                                                marker.position = point
                                                marker.setAnchor(
                                                    Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM
                                                )
                                                marker.icon =
                                                    ctx.getDrawable(org.osmdroid.library.R.drawable.marker_default)
                                                overlays.add(marker)
                                            }
                                        }, update = { view ->
                                            val point =
                                                GeoPoint(state.formLatitud, state.formLongitud)
                                            view.controller.setCenter(point)
                                            view.overlays.filterIsInstance<Marker>().firstOrNull()
                                                ?.let { marker ->
                                                    marker.position = point
                                                    view.invalidate()
                                                }
                                        }, modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable { onOpenMapPicker() },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Map,
                                                null,
                                                tint = colores.textoTerciario,
                                                modifier = Modifier.size(s.iconLarge)
                                            )
                                            Spacer(Modifier.height(s.xs))
                                            Text(
                                                "UBICACIÓN REQUERIDA EN EL MAPA",
                                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                                    fontSize = s.textLabel.value.sp
                                                ),
                                                color = colores.textoTerciario
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = onOpenMapPicker,
                                        color = colores.cardBase.copy(alpha = 0.92f),
                                        shape = RoundedCornerShape(s.radiusChip),
                                        border = androidx.compose.foundation.BorderStroke(
                                            s.borderWidth * 0.6f, colores.cardBorde
                                        ),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(s.xs)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = s.xs, vertical = s.xs * 0.7f
                                            ),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(s.xs * 0.5f)
                                        ) {
                                            Icon(
                                                Icons.Default.EditLocationAlt,
                                                null,
                                                tint = colores.textoPrincipal,
                                                modifier = Modifier.size(s.iconTiny)
                                            )
                                            Text(
                                                "UBICAR",
                                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                                    fontSize = s.textLabel.value.sp * 0.85f,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = colores.textoPrincipal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }

            Spacer(modifier = Modifier.height(s.gapMedium))

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
                        border = androidx.compose.foundation.BorderStroke(
                            s.borderWidth, colores.estadoPeligro.copy(alpha = 0.25f)
                        )
                    ) {
                        Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(s.iconSmall))
                        Spacer(Modifier.width(s.xs * 0.8f))
                        Text(
                            "ELIMINAR", style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp
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

                    if (state.pasoActual > 1) {
                        OutlinedButton(
                            onClick = onPasoAnterior,
                            modifier = Modifier
                                .height(s.btnMediumH)
                                .align(Alignment.Start),
                            shape = RoundedCornerShape(s.radiusButton),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colores.textoTerciario),
                            border = androidx.compose.foundation.BorderStroke(
                                s.borderWidth, colores.cardBorde.copy(alpha = 0.8f)
                            )
                        ) {
                            Text(
                                "VOLVER", style = TokensFarmadon.tipografia.etiqueta.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = s.textLabel.value.sp
                                )
                            )
                        }
                    }

                    if (puedeContinuar) {
                        Button(
                            onClick = if (state.pasoActual < 3) onSiguientePaso else onGuardar,
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
                                    text = when (state.pasoActual) {
                                        1 -> "SIGUIENTE"
                                        2 -> "SIGUIENTE"
                                        else -> "CREAR SEDE"
                                    }, style = TokensFarmadon.tipografia.etiqueta.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = s.textLabel.value.sp,
                                        letterSpacing = 0.4.sp
                                    )
                                )
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

