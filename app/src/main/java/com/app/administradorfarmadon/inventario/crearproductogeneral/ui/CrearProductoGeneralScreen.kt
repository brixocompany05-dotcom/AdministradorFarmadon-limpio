package com.app.administradorfarmadon.inventario.crearproductogeneral.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonSecundario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.inventario.compartido.ui.LectorCodigoBarrasCamaraDialog
import com.app.administradorfarmadon.inventario.crearproductogeneral.logica.CrearProductoGeneralViewModel
import com.app.administradorfarmadon.inventario.crearproductogeneral.logica.EstadoGuardadoProducto
import com.app.administradorfarmadon.inventario.crearproductogeneral.ui.componentes.CrearFormularioPanel
import com.app.administradorfarmadon.inventario.crearproductogeneral.ui.componentes.CrearResumenPanel

/**
 * Pantalla de Creación de Producto bajo el estándar SaaS Tablet Horizontal (Skill 06).
 * Diálogos ejecutivos adaptables 100% al tema de la aplicación sin colores fijos.
 */
@Composable
fun CrearProductoGeneralScreen(
    viewModel: CrearProductoGeneralViewModel,
    onProductoCreado: (String) -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var mostrarDialogoDescartar by remember { mutableStateOf(false) }

    fun intentarSalir() {
        if (state.estadoGuardado == EstadoGuardadoProducto.EXITO) {
            onProductoCreado(state.productoGuardadoId ?: "")
        } else if (state.nombre.isNotBlank() || state.codigoBarras.isNotBlank() || state.principioActivo.isNotBlank()) {
            mostrarDialogoDescartar = true
        } else {
            onAtras()
        }
    }

    BackHandler {
        intentarSalir()
    }

    // ── DIÁLOGO EJECUTIVO: CÓDIGO DE BARRAS YA EXISTENTE ──
    state.productoExistenteDuplicado?.let { (prodId, nombreExistente) ->
        Dialog(
            onDismissRequest = { viewModel.descartarProductoDuplicadoDetectado() },
            properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true)
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .clip(FDShapes.Medium),
                color = FDColors.SurfaceElevated,
                border = BorderStroke(1.dp, FDColors.Border),
                shape = FDShapes.Medium
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        color = FDColors.WarningSubtle,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.3f)),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = FDColors.Warning,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Código de barras ya registrado",
                            style = FDType.Heading3.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "El código ingresado ya pertenece a \"$nombreExistente\" en el inventario de esta farmacia.",
                            style = FDType.Body,
                            color = FDColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FDBotonPrimario(
                            texto = "Ingresar Stock / Ver Producto",
                            onClick = {
                                viewModel.descartarProductoDuplicadoDetectado()
                                onProductoCreado(prodId)
                            },
                            icono = Icons.Outlined.Inventory2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick()
                        )

                        FDBotonSecundario(
                            texto = "Continuar creando nuevo",
                            onClick = { viewModel.descartarProductoDuplicadoDetectado() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // ── ESCÁNER DE CÁMARA INSTANTÁNEO GOOGLE ML KIT ──
    if (state.mostrarCamaraScanner) {
        LectorCodigoBarrasCamaraDialog(
            onCodigoDetectado = { codigo ->
                viewModel.onCodigoBarrasDetectadoPorCamara(codigo)
            },
            onDismiss = {
                viewModel.onAbrirCamaraScanner(false)
            }
        )
    }

    // ── DIÁLOGO EJECUTIVO AL DESCARTAR: 100% ADAPTABLE AL TEMA ──
    if (mostrarDialogoDescartar) {
        Dialog(
            onDismissRequest = { mostrarDialogoDescartar = false },
            properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true)
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .clip(TokensFarmadon.formas.mediana),
                color = FDColors.SurfaceElevated,
                border = BorderStroke(1.dp, FDColors.Border),
                shape = TokensFarmadon.formas.mediana
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Ícono de Alerta en Badge Circular
                    Surface(
                        color = FDColors.Warning.copy(alpha = 0.12f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.3f)),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = FDColors.Warning,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Títulos y Explicación
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "¿Descartar cambios en la ficha?",
                            style = FDType.Heading3.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FDColors.TextPrimary
                            ),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tienes información ingresada para este producto. Si sales ahora, los datos no guardados se perderán.",
                            style = FDType.Body.copy(
                                fontSize = 13.sp,
                                color = FDColors.TextSecondary
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Botones Adaptables con Tokens de Tema
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FDBotonPrimario(
                            texto = "Seguir editando",
                            onClick = { mostrarDialogoDescartar = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick()
                        )

                        FDBotonSecundario(
                            texto = "Descartar y salir",
                            onClick = {
                                mostrarDialogoDescartar = false
                                onAtras()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .background(FDColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 1. BARRA SUPERIOR (Efecto Glass Refinado) ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                color = FDColors.Background.copy(alpha = 0.94f),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                                IconButton(
                                    onClick = { intentarSalir() },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(TokensFarmadon.formas.pequena)
                                        .background(FDColors.SurfaceElevated)
                                        .border(1.dp, FDColors.Border, TokensFarmadon.formas.pequena)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Volver",
                                        tint = FDColors.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "NUEVO PRODUCTO",
                                style = FDType.Heading2.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FDColors.TextPrimary
                                )
                            )
                            Text(
                                text = "Registro directo en el inventario de la farmacia",
                                style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)

            // ── 2. CONTENIDO: ENTRADA HERO O FICHA 60/40 ──
            AnimatedContent(
                targetState = state.formularioDesplegado,
                transitionSpec = {
                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "FormularioProgresivo"
            ) { desplegado ->
                if (!desplegado) {
                    // ESTADO INICIAL: HERO INPUT CENTRADO
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth()
                                .clip(TokensFarmadon.formas.mediana)
                                .background(FDColors.Surface)
                                .border(0.8.dp, FDColors.Border, TokensFarmadon.formas.mediana)
                                .padding(32.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "¿QUÉ PRODUCTO VAS A REGISTRAR?",
                                    style = FDType.Heading2.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FDColors.TextPrimary
                                    )
                                )
                                Text(
                                    text = "Escribe el nombre o pistolea la caja para comenzar la ficha",
                                    style = FDType.Body.copy(
                                        fontSize = 13.sp,
                                        color = FDColors.TextSecondary
                                    )
                                )
                            }

                            // Fila de Entrada + Botón de Autocompletar (52.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    HeroSearchInput(
                                        value = state.nombre,
                                        onValueChange = viewModel::onNombreChanged,
                                        onSearch = { viewModel.ejecutarAnalisisIa() },
                                        placeholder = "Ej: Amoxicilina 500mg cápsulas, Coca Cola 1.5L..."
                                    )
                                }

                                FDBotonPrimario(
                                    texto = if (state.clasificandoIa) "ANALIZANDO..." else "AUTOCOMPLETAR",
                                    onClick = { viewModel.ejecutarAnalisisIa() },
                                    habilitado = state.nombre.trim().length >= 2 && !state.clasificandoIa,
                                    icono = if (state.clasificandoIa) null else Icons.Default.AutoAwesome,
                                    cargando = state.clasificandoIa,
                                    modifier = Modifier
                                        .height(52.dp)
                                        .bounceClick()
                                )
                            }

                            // Opción manual paso a paso
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier.clickable { viewModel.onDesplegarFormularioManual() }
                            ) {
                                Text(
                                    text = "o llenar formulario en blanco paso a paso →",
                                    style = FDType.Label.copy(
                                        fontSize = 12.sp,
                                        color = FDColors.Primary,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                } else {
                    // ESTADO ACTIVO: PATRÓN TABLET HORIZONTAL 60% / 40%
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Formulario Continuo a la Izquierda (60%)
                        CrearFormularioPanel(
                            state = state,
                            onNombreChange = viewModel::onNombreChanged,
                            onEjecutarAnalisisIa = viewModel::ejecutarAnalisisIa,
                            onAplicarNombreCorregido = viewModel::onAplicarNombreCorregido,
                            onAbrirCamaraScanner = { viewModel.onAbrirCamaraScanner(true) },
                            onCategoriaChange = viewModel::onCategoriaSelected,
                            onLaboratorioChange = viewModel::onLaboratorioChanged,
                            onPrincipioActivoChange = viewModel::onPrincipioActivoChanged,
                            onEmpaqueChange = viewModel::onEmpaqueSelected,
                            onCantidadContenidoChange = viewModel::onCantidadContenidoChanged,
                            onUnidadMedidaChange = viewModel::onUnidadMedidaSelected,
                            onVarianteSeleccionada = viewModel::onVarianteSeleccionada,
                            onCodigoBarrasChange = viewModel::onCodigoBarrasChanged,
                            onRequiereRecetaChange = viewModel::onRequiereRecetaChanged,
                            onEsRefrigeradoChange = viewModel::onEsRefrigeradoChanged,
                            onPermiteFraccionarChange = viewModel::onPermiteFraccionarChanged,
                            modifier = Modifier.weight(0.60f)
                        )

                        // Panel de Resumen Ejecutivo y Liquidación a la Derecha (40%)
                        CrearResumenPanel(
                            state = state,
                            onSave = viewModel::guardarProducto,
                            onRegistrarOtro = viewModel::onRegistrarOtroProducto,
                            onSalir = { onProductoCreado(state.productoGuardadoId ?: "") },
                            onReintentar = viewModel::onReintentarGuardado,
                            modifier = Modifier.weight(0.40f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(TokensFarmadon.formas.pequena)
            .background(if (isFocused) FDColors.SurfaceElevated else FDColors.Surface)
            .border(
                width = if (isFocused) 1.5.dp else 0.8.dp,
                color = if (isFocused) FDColors.Primary else FDColors.Border,
                shape = TokensFarmadon.formas.pequena
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = if (isFocused) FDColors.Primary else FDColors.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                textStyle = FDType.Body.copy(
                    color = FDColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(FDColors.Primary),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = FDType.Body.copy(
                                color = FDColors.TextTertiary,
                                fontSize = 13.5.sp
                            )
                        )
                    }
                    inner()
                }
            )
        }
    }
}
