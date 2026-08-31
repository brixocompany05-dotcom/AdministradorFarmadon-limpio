package com.app.administradorfarmadon.configuracion.metodospago.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.configuracion.metodospago.logica.MetodosPagoViewModel
import com.app.administradorfarmadon.configuracion.metodospago.modelo.CampoTipoPago
import com.app.administradorfarmadon.configuracion.metodospago.modelo.InstanciaPago
import com.app.administradorfarmadon.configuracion.metodospago.modelo.TIPOS_PAGO_FIJOS
import com.app.administradorfarmadon.configuracion.metodospago.modelo.TipoPagoFijo
import com.app.administradorfarmadon.configuracion.metodospago.datos.SucursalCatalogo
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.InterPremium
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import kotlinx.coroutines.delay

@Composable
private fun SelectorSucursal(
    sucursales: List<SucursalCatalogo>,
    sucursalConfiguradaId: String?,
    onSeleccionar: (String) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa,
    colores: com.app.administradorfarmadon.disenotemaapp.ui.tokens.ColoresFarmadon
) {
    var abierto by remember { mutableStateOf(false) }
    val nombreActual = sucursales.firstOrNull { it.id == sucursalConfiguradaId }?.nombre
        ?: com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalNombre.ifBlank { "Sede Principal" }

    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(s.xs * 0.5f)) {
        Text(
            text = "CONFIGURANDO SUCURSAL",
            style = TokensFarmadon.tipografia.etiqueta.copy(
                fontSize = s.textLabel.value.sp * 0.78f,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontFamily = InterPremium
            ),
            color = colores.textoTerciario
        )
        Box {
            Surface(
                onClick = { abierto = true },
                color = colores.cardElevada,
                shape = RoundedCornerShape(s.radiusButton),
                border = BorderStroke(s.borderWidth, colores.cardBorde)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = s.padInputH * 0.8f, vertical = s.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        null,
                        tint = colores.textoPrincipal,
                        modifier = Modifier.size(s.iconSmall)
                    )
                    Text(
                        nombreActual,
                        style = TokensFarmadon.tipografia.titulo3.copy(
                            fontSize = s.textBody.value.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterPremium
                        ),
                        color = colores.textoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        null,
                        tint = colores.textoTerciario,
                        modifier = Modifier.size(s.iconTiny)
                    )
                }
            }

            DropdownMenu(
                expanded = abierto,
                onDismissRequest = { abierto = false },
                containerColor = colores.cardElevada,
                shape = RoundedCornerShape(s.radiusCard * 0.75f),
                modifier = Modifier.widthIn(min = 260.dp)
            ) {
                Text(
                    text = "Elige la sucursal a configurar",
                    modifier = Modifier.padding(horizontal = s.padCard, vertical = s.xs),
                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                        fontSize = s.textLabel.value.sp,
                        fontFamily = InterPremium
                    ),
                    color = colores.textoTerciario
                )
                if (sucursales.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Sede Principal",
                                style = TokensFarmadon.tipografia.cuerpo.copy(
                                    fontSize = s.textBody.value.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterPremium
                                ),
                                color = colores.textoPrincipal
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Storefront, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconSmall))
                        },
                        onClick = {
                            onSeleccionar("principal")
                            abierto = false
                        }
                    )
                } else {
                    sucursales.forEach { sucursal ->
                        val seleccionada = sucursal.id == sucursalConfiguradaId
                        DropdownMenuItem(
                            text = {
                                Text(
                                    sucursal.nombre,
                                    style = TokensFarmadon.tipografia.cuerpo.copy(
                                        fontSize = s.textBody.value.sp,
                                        fontWeight = if (seleccionada) FontWeight.Black else FontWeight.Normal,
                                        fontFamily = InterPremium
                                    ),
                                    color = if (seleccionada) colores.textoPrincipal else colores.textoSecundario
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (seleccionada) Icons.Default.Check else Icons.Default.Storefront,
                                    null,
                                    tint = if (seleccionada) colores.estadoExito else colores.textoTerciario,
                                    modifier = Modifier.size(s.iconSmall)
                                )
                            },
                            onClick = {
                                onSeleccionar(sucursal.id)
                                abierto = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetodosPagoScreen(
    onBack: () -> Unit,
    viewModel: MetodosPagoViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val s = recordarMedidaAdaptativa()
    val colores = TokensFarmadon.colores
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val tipoSeleccionado = TIPOS_PAGO_FIJOS.firstOrNull { it.id == viewModel.seleccionTipoId }
    val instanciasTipo = viewModel.instancias.filter { it.tipoId == tipoSeleccionado?.id }

    // Al cambiar de sucursal, se cierran los formularios abiertos y se elige el primer tipo
    LaunchedEffect(viewModel.sucursalConfiguradaId) {
        if (viewModel.seleccionTipoId == null && TIPOS_PAGO_FIJOS.isNotEmpty()) {
            viewModel.seleccionarTipo(TIPOS_PAGO_FIJOS.first().id)
        }
    }

    LaunchedEffect(viewModel.instancias.size) {
        if (viewModel.seleccionTipoId == null && TIPOS_PAGO_FIJOS.isNotEmpty()) {
            viewModel.seleccionarTipo(TIPOS_PAGO_FIJOS.first().id)
        }
    }

    LaunchedEffect(viewModel.mensajeExito) {
        if (viewModel.mensajeExito != null) {
            delay(3500)
            viewModel.limpiarFeedback()
        }
    }

    BackHandler {
        when {
            else -> onBack()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.fondoBase)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
    ) {
        val ancho = maxWidth.value
        val padH = s.padScreenH
        val padV = s.padScreenV
        val gapPrincipal = s.gapLarge
        val gapColumnas = s.gapColumnas(ancho)
        val rielW = s.anchoRiel(ancho)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padH, vertical = padV),
            verticalArrangement = Arrangement.spacedBy(gapPrincipal)
        ) {
            // ── Cabecera ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
                ) {
                    IconButton(
                        onClick = {
                            onBack()
                        },
                        modifier = Modifier
                            .size(s.btnSmallH)
                            .clip(RoundedCornerShape(s.radiusButton))
                            .background(colores.cardElevada)
                            .border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusButton))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = colores.textoPrincipal,
                            modifier = Modifier.size(s.iconSmall)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "CONFIGURACIÓN DE PAGOS",
                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontSize = s.textLabel.value.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = InterPremium
                            ),
                            color = colores.textoTerciario
                        )
                        Text(
                            text = "Métodos de pago",
                            style = TokensFarmadon.tipografia.titulo1.copy(
                                fontSize = s.textTitle.value.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = InterPremium
                            ),
                            color = colores.textoPrincipal
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(s.xs * 0.5f)
                ) {
                    val activas = viewModel.instancias.count { it.activa }
                    SelectorSucursal(
                        sucursales = viewModel.sucursales,
                        sucursalConfiguradaId = viewModel.sucursalConfiguradaId,
                        onSeleccionar = { id ->
                            viewModel.seleccionarSucursal(id)
                        },
                        s = s,
                        colores = colores
                    )
                    Surface(
                        color = colores.cardElevada,
                        shape = RoundedCornerShape(s.radiusChip),
                        border = BorderStroke(s.borderWidth, colores.cardBorde)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = s.padInputH * 0.7f, vertical = s.xs * 0.8f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.xs)
                        ) {
                            Icon(Icons.Default.Payments, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconTiny))
                            Text(
                                text = "$activas ACTIVAS",
                                style = TokensFarmadon.tipografia.etiqueta.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = s.textLabel.value.sp,
                                    fontFamily = InterPremium
                                ),
                                color = colores.textoPrincipal
                            )
                        }
                    }
                }
            }

            if (viewModel.errorCarga != null) {
                // Verdad visible: la falla real de carga, con salida (R3/R9).
                Surface(
                    color = colores.estadoPeligro.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(s.radiusCard * 0.75f),
                    border = BorderStroke(s.borderWidth, colores.estadoPeligro.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(s.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.sm)
                    ) {
                        Icon(Icons.Default.WarningAmber, null, tint = colores.estadoPeligro, modifier = Modifier.size(s.iconSmall))
                        Text(
                            text = viewModel.errorCarga ?: "",
                            style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp),
                            color = colores.textoPrincipal,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.reintentarCarga() }) {
                            Text("REINTENTAR", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp), color = colores.estadoPeligro)
                        }
                    }
                }
            }

            // ── Área de trabajo: riel de tipos (izquierda) + detalle (derecha) ──
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gapColumnas)
            ) {
                // ── Panel izquierdo: tipos fijos ──
                Column(
                    modifier = Modifier
                        .width(rielW)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(gapPrincipal)
                ) {
                    Text(
                        text = "TIPOS DE PAGO",
                        style = TokensFarmadon.tipografia.etiqueta.copy(
                            fontSize = s.textLabel.value.sp * 0.9f,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp,
                            fontFamily = InterPremium
                        ),
                        color = colores.textoTerciario
                    )

                    Surface(
                        color = colores.cardBase,
                        shape = RoundedCornerShape(s.radiusCard * 0.75f),
                        border = BorderStroke(s.borderWidth, colores.cardBorde),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        if (viewModel.cargando) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = colores.textoPrincipal, strokeWidth = 2.dp, modifier = Modifier.size(s.iconLarge))
                            }
                        } else {
                            LazyColumn(
                                state = rememberLazyListState(),
                                modifier = Modifier.fillMaxSize().padding(s.xs),
                                verticalArrangement = Arrangement.spacedBy(s.xs)
                            ) {
                                items(TIPOS_PAGO_FIJOS, key = { it.id }) { tipo ->
                                    FilaTipoRiel(
                                        tipo = tipo,
                                        totalInstancias = viewModel.instancias.count { it.tipoId == tipo.id },
                                        activasInstancias = viewModel.instancias.count { it.tipoId == tipo.id && it.activa },
                                        seleccionado = tipo.id == tipoSeleccionado?.id,
                        onClick = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            viewModel.seleccionarTipo(tipo.id)
                        },
                                        s = s,
                                        colores = colores
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Panel derecho: detalle del tipo seleccionado ──
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(s.radiusCard * 0.75f))
                        .background(colores.cardBase)
                        .border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusCard * 0.75f))
                ) {
                    if (tipoSeleccionado == null) {
                        EmptyTipoPlaceholder(colores, s)
                    } else {
                        PanelDetalleTipo(
                            tipo = tipoSeleccionado,
                            instancias = instanciasTipo,
                            esUnico = tipoSeleccionado.unica,
                            tipoUnicoActivo = instanciasTipo.firstOrNull()?.activa == true,
                            procesandoId = viewModel.procesandoId,
                            mensajeError = viewModel.mensajeError,
                            mensajeExito = viewModel.mensajeExito,
                            onAgregar = { datos -> viewModel.agregarInstancia(tipoSeleccionado.id, datos) },
                            onGuardarDatos = { id, datos -> viewModel.guardarDatosInstancia(id, datos) },
                            onSetActiva = { id, activa -> viewModel.setInstanciaActiva(id, activa) },
                            onToggleUnico = { activo -> viewModel.setTipoUnicoActivo(tipoSeleccionado.id, activo) },
                            s = s,
                            colores = colores
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun FilaTipoRiel(
    tipo: TipoPagoFijo,
    totalInstancias: Int,
    activasInstancias: Int,
    seleccionado: Boolean,
    onClick: () -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa,
    colores: com.app.administradorfarmadon.disenotemaapp.ui.tokens.ColoresFarmadon
) {
    val fondo = if (seleccionado) tipo.colorMarca.copy(alpha = 0.10f) else Color.Transparent
    val borde = if (seleccionado) tipo.colorMarca.copy(alpha = 0.45f) else colores.cardBorde.copy(alpha = 0.55f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(s.radiusCard * 0.75f))
            .border(s.borderWidth, borde, RoundedCornerShape(s.radiusCard * 0.75f))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(s.radiusCard * 0.75f),
        color = fondo
    ) {
        Row(
            modifier = Modifier.padding(s.padCard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(s.iconLarge + 8.dp)
                    .clip(RoundedCornerShape(s.radiusChip))
                    .background(tipo.colorMarca.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tipo.icono, null, tint = tipo.colorMarca, modifier = Modifier.size(s.iconMedium))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    tipo.nombre,
                    style = TokensFarmadon.tipografia.titulo3.copy(
                        fontSize = s.textBody.value.sp * 0.98f,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterPremium
                    ),
                    color = colores.textoPrincipal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (totalInstancias > 0) {
                    Text(
                        text = if (tipo.unica) {
                            if (activasInstancias > 0) "ACTIVO · disponible para pagos" else "INACTIVO · apagado"
                        } else if (activasInstancias == totalInstancias) {
                            "$totalInstancias cuenta(s) activa(s)"
                        } else {
                            "$activasInstancias de $totalInstancias activa(s)"
                        },
                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                            fontSize = s.textLabel.value.sp * 0.84f,
                            fontFamily = InterPremium
                        ),
                        color = if (tipo.unica && activasInstancias > 0) colores.estadoExito else colores.textoTerciario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        if (tipo.unica) "INACTIVO · apagado" else "Sin cuentas aún",
                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                            fontSize = s.textLabel.value.sp * 0.84f,
                            fontFamily = InterPremium
                        ),
                        color = colores.textoTerciario.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (totalInstancias > 0) {
                Surface(
                    color = if (activasInstancias > 0) tipo.colorMarca.copy(alpha = 0.14f) else colores.cardElevada,
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(s.borderWidth * 0.7f, if (activasInstancias > 0) tipo.colorMarca.copy(alpha = 0.35f) else colores.cardBorde)
                ) {
                    Text(
                        if (tipo.unica) if (activasInstancias > 0) "ON" else "OFF" else "$activasInstancias",
                        modifier = Modifier.padding(horizontal = s.xs, vertical = 2.dp),
                        style = TokensFarmadon.tipografia.etiqueta.copy(
                            fontSize = s.textLabel.value.sp * 0.86f,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterPremium
                        ),
                        color = if (activasInstancias > 0) tipo.colorMarca else colores.textoTerciario
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelDetalleTipo(
    tipo: TipoPagoFijo,
    instancias: List<InstanciaPago>,
    esUnico: Boolean,
    tipoUnicoActivo: Boolean,
    procesandoId: String?,
    mensajeError: String?,
    mensajeExito: String?,
    onAgregar: (Map<String, String>) -> Unit,
    onGuardarDatos: (String, Map<String, String>) -> Unit,
    onSetActiva: (String, Boolean) -> Unit,
    onToggleUnico: (Boolean) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa,
    colores: com.app.administradorfarmadon.disenotemaapp.ui.tokens.ColoresFarmadon
) {
    val scroll = rememberScrollState()
    val cuentaActual = instancias.firstOrNull()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(s.padCardLarge),
        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
    ) {
        // Cabecera del tipo
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.sm)) {
            Box(
                modifier = Modifier
                    .size(s.iconLarge + 14.dp)
                    .clip(RoundedCornerShape(s.radiusChip))
                    .background(tipo.colorMarca.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tipo.icono, null, tint = tipo.colorMarca, modifier = Modifier.size(s.iconMedium + 4.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    tipo.nombre,
                    style = TokensFarmadon.tipografia.titulo2.copy(
                        fontSize = s.textSubtitle.value.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterPremium
                    ),
                    color = colores.textoPrincipal
                )
                Text(
                    tipo.descripcion,
                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                        fontSize = s.textBody.value.sp * 0.9f,
                        fontFamily = InterPremium
                    ),
                    color = colores.textoTerciario
                )
            }
        }

        // Feedback real de Firebase: error claro o éxito
        mensajeError?.let {
            Surface(
                color = colores.peligroSutil,
                shape = RoundedCornerShape(s.radiusChip),
                border = BorderStroke(s.borderWidth, colores.estadoPeligro.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = s.padCard, vertical = s.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    Icon(Icons.Default.WarningAmber, null, tint = colores.estadoPeligro, modifier = Modifier.size(s.iconSmall))
                    Text(
                        it,
                        modifier = Modifier.weight(1f),
                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                            fontSize = s.textBody.value.sp * 0.92f,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterPremium
                        ),
                        color = colores.estadoPeligro
                    )
                }
            }
        }
        mensajeExito?.let {
            Surface(
                color = colores.exitoSutil,
                shape = RoundedCornerShape(s.radiusChip),
                border = BorderStroke(s.borderWidth, colores.estadoExito.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = s.padCard, vertical = s.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    Icon(Icons.Default.Check, null, tint = colores.estadoExito, modifier = Modifier.size(s.iconSmall))
                    Text(
                        it,
                        modifier = Modifier.weight(1f),
                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                            fontSize = s.textBody.value.sp * 0.92f,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterPremium
                        ),
                        color = colores.estadoExito
                    )
                }
            }
        }

        // Explicación honesta: qué es una cuenta y por qué varias cuentas del mismo tipo
        Surface(
            color = tipo.colorMarca.copy(alpha = 0.05f),
            shape = RoundedCornerShape(s.radiusChip),
            border = BorderStroke(s.borderWidth * 0.7f, tipo.colorMarca.copy(alpha = 0.18f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Cada cuenta es un lugar real donde el dinero entra o sale: un número de Yape, una cuenta del BCP, una billetera Plin… " +
                    "Así cada pago queda identificado y el dinero nunca se mezcla en una sola bolsa.",
                modifier = Modifier.padding(horizontal = s.padCard, vertical = s.sm),
                style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                    fontSize = s.textBody.value.sp * 0.88f,
                    fontFamily = InterPremium
                ),
                color = colores.textoSecundario
            )
        }

        if (esUnico) {
            PanelTipoUnico(
                tipo = tipo,
                activo = tipoUnicoActivo,
                procesando = procesandoId != null,
                onToggle = onToggleUnico,
                s = s,
                colores = colores
            )
        } else {
            // El formulario SIEMPRE está abierto: vacío si es nuevo, con sus datos si ya existe.
            FormularioCampos(
                tipo = tipo,
                valoresIniciales = cuentaActual?.datos ?: emptyMap(),
                procesando = if (cuentaActual != null) procesandoId == "datos-${cuentaActual.id}" else procesandoId == "nueva-${tipo.id}",
                onGuardar = { datos ->
                    if (cuentaActual != null) {
                        onGuardarDatos(cuentaActual.id, datos)
                    } else {
                        onAgregar(datos)
                    }
                },
                s = s,
                colores = colores
            )
        }
    }
}

@Composable
private fun FormularioCampos(
    tipo: TipoPagoFijo,
    valoresIniciales: Map<String, String>,
    procesando: Boolean,
    onGuardar: (Map<String, String>) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa,
    colores: com.app.administradorfarmadon.disenotemaapp.ui.tokens.ColoresFarmadon
) {
    val campos = tipo.campos
    val estadoValores = remember(tipo.id, valoresIniciales) {
        mutableStateOf(valoresIniciales.toMutableMap())
    }
    val valores = estadoValores.value
    // Solo hay algo que guardar si la persona cambió algún campo.
    val hayCambios = valores != valoresIniciales

    Surface(
        color = colores.cardElevada,
        shape = RoundedCornerShape(s.radiusCard * 0.75f),
        border = BorderStroke(s.borderWidth * 1.2f, tipo.colorMarca.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(s.padCard),
            verticalArrangement = Arrangement.spacedBy(s.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs)) {
                Box(
                    modifier = Modifier
                        .size(s.iconSmall + 6.dp)
                        .clip(RoundedCornerShape(s.radiusChip))
                        .background(tipo.colorMarca.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tipo.icono, null, tint = tipo.colorMarca, modifier = Modifier.size(s.iconTiny))
                }
                Text(
                    if (valoresIniciales.isEmpty()) "Nueva cuenta · ${tipo.nombre}" else "Editando · ${tipo.nombre}",
                    style = TokensFarmadon.tipografia.titulo3.copy(
                        fontSize = s.textBody.value.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterPremium
                    ),
                    color = colores.textoPrincipal
                )
            }

            if (campos.isEmpty()) {
                Text(
                    "Este tipo se usa solo con referencia (constancia). No necesita datos obligatorios: podrás guardar el movimiento con una descripción breve.",
                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                        fontSize = s.textBody.value.sp * 0.9f,
                        fontFamily = InterPremium
                    ),
                    color = colores.textoTerciario
                )
            } else {
                val columnas = if (campos.size == 1) listOf(campos) else campos.chunked(2)
                columnas.forEach { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(s.sm)
                    ) {
                        fila.forEach { campo ->
                            Box(modifier = Modifier.weight(1f)) {
                                CampoPagoTexto(
                                    campo = campo,
                                    valor = valores[campo.id] ?: "",
                                    habilitado = !procesando,
                                    onCambio = { nuevo ->
                                        // Estado inmutable: se crea un mapa nuevo en cada tecla para
                                        // que Compose detecte el cambio y el campo muestre lo escrito.
                                        estadoValores.value = estadoValores.value.toMutableMap().apply {
                                            this[campo.id] = nuevo
                                        }
                                    },
                                    s = s
                                )
                            }
                        }
                    }
                }
            }

            if (hayCambios || procesando) {
                Button(
                    onClick = { onGuardar(valores.toMap()) },
                    enabled = !procesando,
                    modifier = Modifier.fillMaxWidth().height(s.btnMediumH),
                    shape = RoundedCornerShape(s.radiusButton),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tipo.colorMarca,
                        contentColor = colores.textoInvertido
                    )
                ) {
                    if (procesando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(s.iconSmall),
                            strokeWidth = 2.dp,
                            color = colores.textoInvertido
                        )
                    } else {
                        Text(
                            "GUARDAR CAMBIOS",
                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontSize = s.textLabel.value.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp,
                                fontFamily = InterPremium
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelTipoUnico(
    tipo: TipoPagoFijo,
    activo: Boolean,
    procesando: Boolean,
    onToggle: (Boolean) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa,
    colores: com.app.administradorfarmadon.disenotemaapp.ui.tokens.ColoresFarmadon
) {
    Surface(
        color = colores.cardElevada,
        shape = RoundedCornerShape(s.radiusCard * 0.75f),
        border = BorderStroke(
            s.borderWidth,
            if (activo) tipo.colorMarca.copy(alpha = 0.5f) else colores.cardBorde
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(s.padCardLarge),
            verticalArrangement = Arrangement.spacedBy(s.gapMedium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                Box(
                    modifier = Modifier
                        .size(s.iconLarge + 8.dp)
                        .clip(RoundedCornerShape(s.radiusChip))
                        .background(tipo.colorMarca.copy(alpha = if (activo) 0.18f else 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        tipo.icono,
                        null,
                        tint = if (activo) tipo.colorMarca else colores.textoTerciario.copy(alpha = 0.5f),
                        modifier = Modifier.size(s.iconMedium)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (activo) "Activo" else "Inactivo",
                        style = TokensFarmadon.tipografia.titulo3.copy(
                            fontSize = s.textBody.value.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterPremium
                        ),
                        color = if (activo) colores.estadoExito else colores.textoSecundario
                    )
                    Text(
                        if (activo) "Disponible para pagos y cobros" else "No se ofrece hasta que lo actives",
                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                            fontSize = s.textBody.value.sp * 0.9f,
                            fontFamily = InterPremium
                        ),
                        color = colores.textoTerciario
                    )
                }
                Switch(
                    checked = activo,
                    onCheckedChange = onToggle,
                    enabled = !procesando,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = tipo.colorMarca,
                        checkedThumbColor = colores.textoInvertido,
                        uncheckedTrackColor = colores.cardBorde.copy(alpha = 0.6f),
                        uncheckedThumbColor = colores.textoTerciario.copy(alpha = 0.5f)
                    )
                )
            }

            Surface(
                color = tipo.colorMarca.copy(alpha = 0.06f),
                shape = RoundedCornerShape(s.radiusChip),
                border = BorderStroke(s.borderWidth * 0.7f, tipo.colorMarca.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (tipo.id == "EFECTIVO") {
                        "El efectivo es uno solo: el dinero de caja. No necesita números ni cuentas. Cuando lo actives, los pagos y cobros podrán registrarse como efectivo."
                    } else {
                        "El punto de venta es uno solo: la tarjeta pasa por tu POS. No necesita números ni cuentas. Cuando lo actives, los pagos con tarjeta podrán registrarse como POS."
                    },
                    modifier = Modifier.padding(horizontal = s.padCard, vertical = s.sm),
                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                        fontSize = s.textBody.value.sp * 0.9f,
                        fontFamily = InterPremium
                    ),
                    color = colores.textoSecundario
                )
            }
        }
    }
}

@Composable
private fun CampoPagoTexto(
    campo: CampoTipoPago,
    valor: String,
    habilitado: Boolean,
    onCambio: (String) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    val colores = TokensFarmadon.colores
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo ->
            val limpio = if (campo.esNumerico) nuevo.filter { it.isDigit() || it == ' ' } else nuevo
            onCambio(limpio)
        },
        label = { Text(campo.etiqueta, style = TokensFarmadon.tipografia.etiqueta.copy(fontSize = s.textLabel.value.sp), fontFamily = InterPremium) },
        placeholder = { Text(campo.placeholder, style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp * 0.9f), fontFamily = InterPremium, color = colores.textoTerciario) },
        singleLine = true,
        enabled = habilitado,
        textStyle = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textInput.value.sp, fontFamily = InterPremium, color = colores.textoPrincipal),
        keyboardOptions = KeyboardOptions(keyboardType = if (campo.esNumerico) KeyboardType.Number else KeyboardType.Text),
        shape = RoundedCornerShape(s.radiusInput),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colores.textoPrincipal,
            unfocusedBorderColor = colores.cardBorde,
            focusedContainerColor = colores.cardBase,
            unfocusedContainerColor = colores.cardBase,
            cursorColor = colores.textoPrincipal,
            focusedTextColor = colores.textoPrincipal,
            unfocusedTextColor = colores.textoPrincipal,
            focusedLabelColor = colores.textoSecundario,
            unfocusedLabelColor = colores.textoTerciario
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = s.xs * 0.4f)
    )
}

@Composable
private fun EmptyTipoPlaceholder(
    colores: com.app.administradorfarmadon.disenotemaapp.ui.tokens.ColoresFarmadon,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(s.gapMedium)) {
            Box(
                modifier = Modifier
                    .size(s.iconLarge * 2.2f)
                    .clip(CircleShape)
                    .background(colores.textoPrincipal.copy(alpha = 0.04f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Payments, null, tint = colores.textoTerciario.copy(alpha = 0.4f), modifier = Modifier.size(s.iconLarge))
            }
            Text(
                "Selecciona un tipo de pago",
                style = TokensFarmadon.tipografia.titulo3.copy(
                    fontSize = s.textSubtitle.value.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterPremium
                ),
                color = colores.textoSecundario
            )
            Text(
                "A la izquierda están todos los tipos que maneja el sistema.",
                style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                    fontSize = s.textBody.value.sp,
                    fontFamily = InterPremium
                ),
                color = colores.textoTerciario
            )
        }
    }
}
