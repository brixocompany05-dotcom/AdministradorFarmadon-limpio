package com.app.administradorfarmadon.configuracion.usuarios.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.administradorfarmadon.configuracion.usuarios.logica.UsuariosViewModel
import com.app.administradorfarmadon.configuracion.usuarios.ui.componentes.*
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import androidx.compose.foundation.layout.BoxWithConstraints

@Composable
fun UsuariosScreen(
    viewModel: UsuariosViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = recordarMedidaAdaptativa()
    val context = LocalContext.current
    val colores = TokensFarmadon.colores
    val panelAbierto = state.esModoCreacion || state.usuarioSeleccionado != null
    val listState = rememberLazyListState()

    // Interceptor del botón Atrás físico/gestual — teclado primero, luego panel, luego salir (anti-cierre accidental)
    val focusManagerUsuarios = LocalFocusManager.current
    val keyboardControllerUsuarios = LocalSoftwareKeyboardController.current
    val densityUsuarios = LocalDensity.current
    val isKeyboardVisibleUsuarios = WindowInsets.ime.getBottom(densityUsuarios) > 0
    BackHandler(enabled = true) {
        when {
            isKeyboardVisibleUsuarios -> { keyboardControllerUsuarios?.hide(); focusManagerUsuarios.clearFocus(force = true) }
            state.mostrarDialogoSuspender || state.mostrarDialogoEliminar || state.mostrarDialogoDescartar -> viewModel.cerrarDialogos()
            panelAbierto -> viewModel.solicitarCerrarPanel()
            else -> viewModel.solicitarVolver(onVolver)
        }
    }

    // Mensaje Toast de éxito
    LaunchedEffect(state.mensajeExito) {
        state.mensajeExito?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.limpiarMensajes()
        }
    }

    // Diálogos de Confirmación
    if (state.mostrarDialogoSuspender) {
        val usuario = state.usuarioSeleccionado
        if (usuario != null) {
            DialogoConfirmarSuspension(
                nombreUsuario = usuario.nombre,
                estaActivo = usuario.acceso,
                onConfirm = { viewModel.confirmarCambioAcceso(!usuario.acceso) },
                onDismiss = { viewModel.cerrarDialogos() }
            )
        }
    }

    if (state.mostrarDialogoEliminar) {
        val usuario = state.usuarioSeleccionado
        if (usuario != null) {
            var motivoBaja by remember { mutableStateOf("") }
            DialogoConfirmarEliminarUsuario(
                nombreUsuario = usuario.nombre,
                motivo = motivoBaja,
                onMotivoChange = { motivoBaja = it },
                onConfirm = {
                    viewModel.confirmarEliminar(motivoBaja)
                    motivoBaja = ""
                },
                onDismiss = {
                    motivoBaja = ""
                    viewModel.cerrarDialogos()
                }
            )
        }
    }

    if (state.mostrarDialogoDescartar) {
        DialogoDescartarCambiosUsuarios(
            onConfirmDescartar = { viewModel.confirmarDescartar() },
            onDismiss = { viewModel.cerrarDialogos() }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .background(colores.fondoBase)
    ) {
        // Geometría física: todo se calcula del viewport real, no de constantes
        val ancho = maxWidth.value
        val alto = maxHeight.value
        // Escala simétrica: usar s para modulación fina + proporciones viewport
        val padH = s.padScreenH
        val padV = s.padScreenV
        val gapPrincipal = s.gapLarge // 18dp * spacingScale (aire enterprise)
        val gapColumnas = s.gapColumnas(ancho) // 1.8% del ancho, clamp 14–24
        val rielW = s.anchoRiel(ancho) // 32% clamp 300–380

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padH, vertical = padV),
            verticalArrangement = Arrangement.spacedBy(gapPrincipal)
        ) {
            // 1. Barra Superior — altura y gaps adaptativos, nada fijo
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
                        onClick = { viewModel.solicitarVolver(onVolver) },
                        modifier = Modifier
                            .size(s.btnSmallH)
                            .clip(RoundedCornerShape(s.radiusButton))
                            .background(colores.cardElevada)
                            .border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusButton))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = colores.textoPrincipal,
                            modifier = Modifier.size(s.iconSmall)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "GESTIÓN DE PERSONAL",
                            style = TokensFarmadon.tipografia.etiqueta.copy(
                                fontSize = s.textLabel.value.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = colores.textoTerciario
                        )
                        Text(
                            text = "Expedientes de Usuarios",
                            style = TokensFarmadon.tipografia.titulo1.copy(fontSize = s.textTitle.value.sp),
                            color = colores.textoPrincipal
                        )
                    }
                }

                Surface(
                    color = colores.cardElevada,
                    shape = RoundedCornerShape(s.radiusChip),
                    border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.cardBorde)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = s.padInputH * 0.7f, vertical = s.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.xs)
                    ) {
                        Icon(Icons.Default.Group, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconTiny))
                        Text(
                            text = "${state.usuarios.size} COLABORADORES",
                            style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold, fontSize = s.textLabel.value.sp),
                            color = colores.textoPrincipal
                        )
                    }
                }
            }

            // 2. Área de Trabajo — proporción geométrica 32% / 68% + gap 1.8%
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(gapColumnas)
            ) {
                // PANEL IZQUIERDO: RIEL (32% clamp 300–380) — nunca fijo 340dp
                Column(
                    modifier = Modifier
                        .width(rielW)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(gapPrincipal)
                ) {
                    // Filtros de Roles Integrados en el Maestro
                    UsuariosFilterTabs(
                        selectedRol = state.filtroRol,
                        roles = state.roles,
                        usuarios = state.usuarios,
                        onRolSelected = { viewModel.setFiltroRol(it) },
                        verDadasDeBaja = state.verDadasDeBaja,
                        totalDadasDeBaja = state.totalDadasDeBaja,
                        onToggleDadasDeBaja = { viewModel.toggleVerDadasDeBaja() }
                    )

                    // Buscador — altura y padding adaptativos
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(s.radiusInput))
                            .border(s.borderWidth, colores.cardBorde, RoundedCornerShape(s.radiusInput)),
                        shape = RoundedCornerShape(s.radiusInput),
                        color = colores.cardBase
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = s.padCard)
                                .heightIn(min = s.inputMinH * 0.9f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = colores.textoTerciario, modifier = Modifier.size(s.iconSmall))
                            TextField(
                                value = state.busquedaQuery,
                                onValueChange = viewModel::setBusquedaQuery,
                                placeholder = { Text("Buscar por nombre o DNI...", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp), color = colores.textoTerciario) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = colores.textoPrincipal,
                                    focusedTextColor = colores.textoPrincipal,
                                    unfocusedTextColor = colores.textoPrincipal
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                // Banner de error — adaptativo
                if (state.mensajeError != null && state.usuariosFiltrados.isEmpty() && !state.esModoCreacion) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colores.alertaSutil,
                        shape = RoundedCornerShape(s.radiusChip),
                        border = androidx.compose.foundation.BorderStroke(s.borderWidth, colores.estadoAlerta)
                    ) {
                        Row(
                            modifier = Modifier.padding(s.padCard),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.xs)
                        ) {
                            Icon(Icons.Default.WarningAmber, null, tint = colores.estadoAlerta, modifier = Modifier.size(s.iconSmall))
                            Text(
                                text = state.mensajeError ?: "",
                                style = TokensFarmadon.tipografia.cuerpo.copy(fontSize = s.textBody.value.sp),
                                color = colores.textoPrincipal,
                                modifier = Modifier.weight(1f)
                            )
                            if (state.cargando) {
                                CircularProgressIndicator(modifier = Modifier.size(s.iconSmall), color = colores.estadoAlerta, strokeWidth = 2.dp)
                            } else {
                                TextButton(onClick = { viewModel.reintentarCarga() }) {
                                    Text("REINTENTAR", style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Bold), color = colores.estadoAlerta)
                                }
                            }
                        }
                    }
                }

                // Lista — gaps adaptativos, nunca 8.dp fijo
                if (state.cargando) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colores.textoPrincipal, strokeWidth = 2.dp, modifier = Modifier.size(s.iconLarge))
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(s.xs)
                    ) {
                        if (state.esModoCreacion) {
                            item(key = "creacion_activa") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(s.radiusCard * 0.75f))
                                        .border(s.borderWidth * 1.5f, colores.textoPrincipal, RoundedCornerShape(s.radiusCard * 0.75f)),
                                    shape = RoundedCornerShape(s.radiusCard * 0.75f),
                                    color = colores.cardElevada
                                ) {
                                    Row(modifier = Modifier.padding(s.padCard), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                                        Box(modifier = Modifier.size(s.iconLarge).clip(RoundedCornerShape(s.radiusChip)).background(colores.textoPrincipal.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Add, null, tint = colores.textoPrincipal, modifier = Modifier.size(s.iconSmall))
                                        }
                                        Column {
                                            Text("Nuevo Colaborador", style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textBody.value.sp, fontWeight = FontWeight.Bold), color = colores.textoPrincipal)
                                            Text("Completando ficha...", style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textLabel.value.sp), color = colores.textoSecundario)
                                        }
                                    }
                                }
                            }
                        }

                        if (state.usuariosFiltrados.isEmpty() && !state.esModoCreacion) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = s.xxl),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(s.xs)
                                    ) {
                                        Icon(
                                            imageVector = if (state.verDadasDeBaja) Icons.Default.Inventory2 else Icons.Default.Group,
                                            contentDescription = null,
                                            tint = colores.textoTerciario.copy(alpha = 0.5f),
                                            modifier = Modifier.size(s.iconLarge)
                                        )
                                        Text(
                                            text = if (state.verDadasDeBaja) "No hay colaboradores dados de baja"
                                            else "Sin colaboradores en esta lista",
                                            style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp),
                                            color = colores.textoTerciario
                                        )
                                    }
                                }
                            }
                        } else {
                            items(state.usuariosFiltrados, key = { it.id }) { usuario ->
                                UsuarioItemCard(
                                    usuario = usuario,
                                    estaSeleccionado = !state.esModoCreacion && state.usuarioSeleccionado?.id == usuario.id,
                                    esUsuarioActual = state.usuarioActualUid == usuario.id,
                                    s = s,
                                    onClick = {
                                        if (state.verDadasDeBaja) viewModel.recontratarUsuario(usuario)
                                        else viewModel.solicitarSeleccionarUsuario(usuario)
                                    }
                                )
                            }
                        }
                    }
                }

                // Quiet premium: una acción primaria — el guardar del panel domina; este botón pasa a secundario cuando el panel está abierto
                val rielBtnEsPrimario = !panelAbierto
                Button(
                    onClick = { viewModel.solicitarIniciarNuevoUsuario() },
                    enabled = !state.esModoCreacion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(s.btnLargeH)
                        .bounceClick(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (rielBtnEsPrimario) colores.botonPrimarioFondo else colores.cardElevada,
                        contentColor = if (rielBtnEsPrimario) colores.botonPrimarioTexto else colores.textoSecundario,
                        disabledContainerColor = colores.cardElevada,
                        disabledContentColor = colores.textoTerciario
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = s.borderWidth,
                        color = if (rielBtnEsPrimario) colores.botonPrimarioFondo else colores.cardBorde
                    ),
                    shape = RoundedCornerShape(s.radiusButton),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = if (state.esModoCreacion) Icons.Default.Check else Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(s.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(s.xs))
                    Text(
                        text = if (state.esModoCreacion) "EDITANDO..." else "REGISTRAR NUEVO",
                        style = TokensFarmadon.tipografia.etiqueta.copy(fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp, letterSpacing = 0.8.sp)
                    )
                }
            }

            // ── PANEL DE DETALLE (FICHA DE IDENTIDAD PROFESIONAL) ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (panelAbierto) {
                    UsuarioFormularioPanel(
                        state = state,
                        onFieldChanged = viewModel::onFieldChanged,
                        onRolSelected = viewModel::onRolSelected,
                        onSucursalSelected = viewModel::onSucursalSelected,
                        onAccesoChanged = viewModel::onAccesoChanged,
                        onPermisoModuloChanged = viewModel::onPermisoModuloChanged,
                        onToggleTodosPermisos = viewModel::onToggleTodosPermisos,
                        onReintentarHerramientas = { viewModel.reintentarHerramientas() },
                        onGuardar = { viewModel.guardarUsuario() },
                        onSolicitarSuspender = { viewModel.solicitarSuspender() },
                        onSolicitarEliminar = { viewModel.solicitarEliminar() },
                        onEnviarRestablecimiento = { viewModel.solicitarEnviarRestablecimiento() },
                        onDominioSeleccionado = viewModel::aplicarDominioEmail,
                        onCerrarPanel = { viewModel.solicitarCerrarPanel() },
                        s = s
                    )
                } else {
                    // Empty State — géométrico, radii y gaps adaptativos
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(s.radiusCard * 1.5f))
                            .background(colores.cardBase.copy(alpha = 0.4f))
                            .border(s.borderWidth, colores.cardBorde.copy(alpha = 0.6f), RoundedCornerShape(s.radiusCard * 1.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(s.lg)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(s.iconLarge * 2.2f)
                                    .clip(CircleShape)
                                    .background(colores.textoPrincipal.copy(alpha = 0.04f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = colores.textoTerciario.copy(alpha = 0.4f),
                                    modifier = Modifier.size(s.iconLarge)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(s.xs)) {
                                Text(
                                    text = "Gestión de Expedientes",
                                    style = TokensFarmadon.tipografia.titulo3.copy(fontSize = s.textSubtitle.value.sp),
                                    color = colores.textoSecundario
                                )
                                Text(
                                    text = "Seleccione un colaborador para ver sus credenciales",
                                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(fontSize = s.textBody.value.sp),
                                    color = colores.textoTerciario
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
