package com.app.administradorfarmadon.inventario.editarproductosinventario.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.compartido.ui.LectorCodigoBarrasCamaraDialog
import com.app.administradorfarmadon.inventario.editarproductosinventario.logica.EditarProductoViewModel
import com.app.administradorfarmadon.inventario.editarproductosinventario.ui.componentes.EditarFormularioPanel
import com.app.administradorfarmadon.inventario.editarproductosinventario.ui.componentes.EditarResumenPanel

/**
 * Pantalla de Edición de Producto Enterprise para Tablet Horizontal (v2026).
 * Estructurada bajo el patrón obligatorio 60% Formulario Continuo / 40% Resumen en Vivo.
 */
@Composable
fun EditarProductoScreen(
    productoId: String,
    viewModel: EditarProductoViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(productoId) {
        viewModel.init(productoId)
    }

    fun intentarVolver() {
        if (state.isAnyFieldChanged) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler {
        intentarVolver()
    }

    // Diálogo del Escáner de Cámara integrado
    if (state.mostrarCamaraScanner) {
        LectorCodigoBarrasCamaraDialog(
            onCodigoDetectado = { codigo ->
                viewModel.onBarcodeScanned(codigo)
            },
            onDismiss = {
                viewModel.onToggleCamaraScanner(false)
            }
        )
    }

    // Diálogo de confirmación al descartar cambios
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(
                    text = "¿Descartar cambios no guardados?",
                    style = FDType.Heading3.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FDColors.TextPrimary)
                )
            },
            text = {
                Text(
                    text = "Has modificado información en la ficha del producto. Si sales ahora, los cambios se perderán.",
                    style = FDType.Body.copy(fontSize = 13.sp, color = FDColors.TextSecondary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FDColors.Error,
                        contentColor = FDColors.PrimaryText
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Descartar y salir", style = FDType.Label.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDiscardDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.TextPrimary),
                    border = BorderStroke(0.5.dp, FDColors.Border),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Seguir editando", style = FDType.Label.copy(fontSize = 12.sp))
                }
            },
            containerColor = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(14.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .background(FDColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 1. CABECERA SUPERIOR (Efecto Glass Refinado) ──
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
                            onClick = { intentarVolver() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FDColors.SurfaceElevated)
                                .border(0.5.dp, FDColors.Border, RoundedCornerShape(8.dp))
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
                                text = "EDITAR FICHA DEL PRODUCTO",
                                style = FDType.Heading2.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FDColors.TextPrimary
                                )
                            )
                            Text(
                                text = "ID: ${productoId.take(16)} · Actualización directa en inventario",
                                style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)

            // ── 2. CONTENIDO PRINCIPAL: PATRÓN TABLET 60/40 ──
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 2.5.dp)
                        Text(
                            text = "Cargando ficha del producto...",
                            style = FDType.Caption.copy(color = FDColors.TextSecondary, fontSize = 12.sp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Formulario Continuo a la Izquierda (60%)
                    EditarFormularioPanel(
                        state = state,
                        onNombreChange = viewModel::onNombreChanged,
                        onTipoProductoChange = viewModel::onTipoProductoChanged,
                        onPrincipioActivoChange = viewModel::onPrincipioActivoChanged,
                        onCategoriaChange = viewModel::onCategoriaChanged,
                        onLaboratorioChange = viewModel::onLaboratorioChanged,
                        onEmpaqueChange = viewModel::onEmpaqueChanged,
                        onCantidadContenidoChange = viewModel::onCantidadContenidoChanged,
                        onUnidadMedidaChange = viewModel::onUnidadMedidaChanged,
                        onCodigoBarrasChange = viewModel::onCodigoBarrasChanged,
                        onOpenCamaraScanner = { viewModel.onToggleCamaraScanner(true) },
                        onRequiereRecetaChange = viewModel::onRequiereRecetaChanged,
                        onEsRefrigeradoChange = viewModel::onEsRefrigeradoChanged,
                        onPermiteFraccionarChange = viewModel::onPermiteFraccionarChanged,
                        modifier = Modifier.weight(0.60f)
                    )

                    // Panel de Resumen Ejecutivo y Liquidación a la Derecha (40%)
                    EditarResumenPanel(
                        state = state,
                        onSave = viewModel::guardarCambios,
                        onRetry = viewModel::reintentarGuardado,
                        onSuccessExit = onSuccess,
                        modifier = Modifier.weight(0.40f)
                    )
                }
            }
        }
    }
}
