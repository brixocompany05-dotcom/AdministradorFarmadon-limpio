package com.app.administradorfarmadon.configuracion.sucursales.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.administradorfarmadon.autenticacion.registro.paso1_datos.ui.AddressPickerDialog
import com.app.administradorfarmadon.configuracion.sucursales.logica.SucursalesViewModel
import com.app.administradorfarmadon.configuracion.sucursales.ui.componentes.*
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDSizes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.FDSpacing
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon

@Composable
fun SucursalesScreen(
    viewModel: SucursalesViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = recordarMedidaAdaptativa()
    val context = LocalContext.current
    val colores = TokensFarmadon.colores
    val panelAbierto = state.esModoCreacion || state.sucursalSeleccionada != null
    val listState = rememberLazyListState()

    var mostrarSelectorMapa by remember { mutableStateOf(false) }

    // ── ANIMACIÓN DE CORRIENTE ELÉCTRICA (PULSO VIVO) ──
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseOffset"
    )

    // Interceptor de botón Atrás — teclado primero, luego diálogos/panel, nunca cierre accidental
    val focusManagerSuc = LocalFocusManager.current
    val keyboardControllerSuc = LocalSoftwareKeyboardController.current
    val densitySuc = LocalDensity.current
    val isKeyboardVisibleSuc = WindowInsets.ime.getBottom(densitySuc) > 0
    BackHandler(enabled = true) {
        when {
            isKeyboardVisibleSuc -> { keyboardControllerSuc?.hide(); focusManagerSuc.clearFocus(force = true) }
            mostrarSelectorMapa -> mostrarSelectorMapa = false
            state.mostrarDialogoEliminar || state.mostrarDialogoDescartar || state.mostrarDialogoLimite -> viewModel.cerrarDialogos()
            panelAbierto -> viewModel.solicitarCerrarPanel()
            else -> viewModel.solicitarVolver(onVolver)
        }
    }

    // Manejo de diálogos
    if (state.mostrarDialogoLimite) {
        DialogoLimitePlan(state.planNombre, state.maxSucursales) { viewModel.cerrarDialogos() }
    }
    if (state.mostrarDialogoEliminar && state.sucursalSeleccionada != null) {
        DialogoConfirmarEliminar(state.sucursalSeleccionada!!.nombre, state.colaboradoresAsignadosNombres, { viewModel.confirmarEliminar() }) { viewModel.cerrarDialogos() }
    }
    if (state.mostrarDialogoDescartar) {
        DialogoDescartarCambios({ viewModel.confirmarDescartar() }) { viewModel.cerrarDialogos() }
    }
    if (mostrarSelectorMapa) {
        AddressPickerDialog(
            onDismiss = { mostrarSelectorMapa = false },
            onAddressSelected = { address, lat, lng ->
                viewModel.onAddressSelected(address, lat, lng)
                mostrarSelectorMapa = false
            }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .background(colores.fondoBase)
    ) {
        val ancho = maxWidth.value
        val padH = s.padScreenH
        val padV = s.padScreenV
        val gapPrincipal = s.gapLarge
        val gapColumnas = s.gapColumnas(ancho)

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = padH, vertical = padV),
            verticalArrangement = Arrangement.spacedBy(gapPrincipal)
        ) {
        // ── NIVEL 1: CABECERA — adaptativa (nada fijo 44dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(s.gapMedium)
        ) {
            IconButton(
                onClick = { viewModel.solicitarVolver(onVolver) },
                modifier = Modifier
                    .size(s.btnSmallH * 1.15f)
                    .clip(RoundedCornerShape(s.radiusButton))
                    .background(colores.cardBase)
                    .border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusButton))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = colores.textoPrincipal, modifier = Modifier.size(s.iconSmall))
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "GESTIÓN OPERATIVA",
                    style = FDType.Label.copy(fontSize = s.textLabel.value.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Black),
                    color = colores.textoTerciario
                )
                Text(
                    text = "Sedes y Sucursales",
                    style = FDType.Heading1.copy(fontSize = s.textTitle.value.sp, fontWeight = FontWeight.Black),
                    color = colores.textoPrincipal
                )
            }
        }

        // ── NIVEL 2: FILTROS OPERATIVOS (BARRA LIMPIA) ──
        Box(modifier = Modifier.fillMaxWidth()) {
            SucursalesFilterTabs(
                selectedFilter = state.filtroEstado,
                counts = mapOf(
                    "TODAS" to state.sucursales.size,
                    "OPERANDO" to state.sucursales.count { it.activa },
                    "MANTENIMIENTO" to state.sucursales.count { !it.activa }
                ),
                onFilterSelected = { viewModel.setFiltroEstado(it) }
            )
        }

        // ── NIVEL 3: ÁREA TRABAJO — geometría 35/65 con gap físico
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gapColumnas * 0.5f)
        ) {
            // PANEL IZQUIERDO: 35% — gaps y bordes adaptativos
            Column(
                modifier = Modifier.weight(0.35f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                PlanCapacidadBanner(
                    planNombre = state.planNombre,
                    totalSucursales = state.totalSucursales,
                    maxSucursales = state.maxSucursales,
                    porcentaje = state.porcentajeOcupado,
                    puedeCrearMas = state.puedeCrearMas,
                    s = s
                )

                Box(
                    modifier = Modifier
                        .width(s.separatorH)
                        .height(s.sm)
                        .background(FDColors.Border)
                )

                Surface(
                    color = colores.cardBase,
                    shape = RoundedCornerShape(s.radiusCard * 0.75f),
                    border = BorderStroke(s.borderWidth, colores.cardBorde),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    if (state.cargando) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colores.textoPrincipal, strokeWidth = 2.dp, modifier = Modifier.size(s.iconLarge))
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(s.xs),
                            verticalArrangement = Arrangement.spacedBy(s.xs)
                        ) {
                            if (state.esModoCreacion) {
                                item(key = "creacion_activa") {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(s.radiusChip)).border(s.borderWidth * 1.5f, colores.textoPrincipal, RoundedCornerShape(s.radiusChip)),
                                        shape = RoundedCornerShape(s.radiusChip), color = colores.cardElevada
                                    ) {
                                        Row(modifier = Modifier.padding(s.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                                            Box(modifier = Modifier.size(s.iconLarge).clip(RoundedCornerShape(s.radiusChip * 0.6f)).background(colores.textoPrincipal.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Add, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconSmall))
                                            }
                                            Column {
                                                Text("Nueva Sucursal", style = FDType.Body.copy(fontWeight = FontWeight.Bold, fontSize = s.textBody.value.sp), color = colores.textoPrincipal)
                                                Text("Completando datos...", style = FDType.BodySmall.copy(fontSize = s.textBody.value.sp * 0.92f), color = colores.textoSecundario)
                                            }
                                        }
                                    }
                                }
                            }

                            items(state.sucursalesFiltradas, key = { it.id }) { sucursal ->
                                val sel = !state.esModoCreacion && state.sucursalSeleccionada?.id == sucursal.id
                                SucursalItemCard(
                                    sucursal = sucursal,
                                    estaSeleccionada = sel,
                                    onClick = { viewModel.solicitarSeleccionarSucursal(sucursal) }
                                )
                            }
                        }
                    }
                }

                // Quiet premium: una acción primaria por pantalla — ocultar crear cuando el panel está abierto (el guardar domina)
                if (state.puedeCrearMas && !panelAbierto) {
                    Spacer(Modifier.height(s.sm))
                    FDBotonPrimario(
                        texto = "CREAR NUEVA SEDE",
                        onClick = { viewModel.solicitarIniciarNuevaSucursal() },
                        icono = Icons.Default.Add,
                        modifier = Modifier.fillMaxWidth().height(s.btnMediumH)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .width(gapColumnas)
                    .height(s.separatorH * 2f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                this.AnimatedVisibility(
                    visible = panelAbierto,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(FDColors.Border, FDColors.Primary, FDColors.Border),
                                    start = androidx.compose.ui.geometry.Offset(x = pulseOffset * 100f, y = 0f),
                                    end = androidx.compose.ui.geometry.Offset(x = (pulseOffset + 0.5f) * 100f, y = 0f)
                                )
                            )
                    )
                }
                if (!panelAbierto) {
                    Box(modifier = Modifier.fillMaxWidth().height(s.separatorH * 2f).background(FDColors.Border.copy(alpha = 0.5f)))
                }
            }

            // Sin cajas anidadas: el detalle ya es una Surface única — el contenedor externo es solo Box con borde vivo
            Box(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(s.radiusCard * 0.75f))
                    .background(colores.cardBase)
                    .border(s.borderWidth, if (panelAbierto) FDColors.Primary.copy(alpha = 0.4f) else colores.cardBorde, RoundedCornerShape(s.radiusCard * 0.75f))
            ) {
                if (panelAbierto) {
                    SucursalFormularioPanel(state, viewModel::onFieldChanged, viewModel::onActivaChanged, { mostrarSelectorMapa = true }, { viewModel.guardarSucursal() }, { viewModel.solicitarEliminar() }, { viewModel.solicitarCerrarPanel() }, s)
                } else {
                    EmptyDetailPlaceholder(colores, s)
                }
            }
        }
        } // BoxWithConstraints
    }
}

@Composable
private fun EmptyDetailPlaceholder(colores: com.app.administradorfarmadon.disenotemaapp.ui.tokens.ColoresFarmadon, s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(s.gapMedium)) {
            Box(modifier = Modifier.size(s.iconLarge * 2f).clip(CircleShape).background(colores.textoPrincipal.copy(alpha = 0.03f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Storefront, null, tint = colores.textoTerciario.copy(alpha = 0.5f), modifier = Modifier.size(s.iconLarge))
            }
            Text("Selecciona una sede para ver su configuración", style = FDType.Body.copy(fontSize = s.textBody.value.sp), color = colores.textoTerciario)
        }
    }
}
