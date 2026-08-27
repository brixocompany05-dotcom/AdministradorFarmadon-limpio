package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.componentes_maestros.ui.DeletedProductOverlay
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailState
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailViewModel

@Composable
fun ProductDetailScreen(
    productId: String,
    initialTabIndex: Int = 0,
    onClose: () -> Unit,
    onEdit: (MoldeProductos) -> Unit,
    onAdjustStock: (MoldeProductos, LoteProducto?) -> Unit = { _, _ -> },
    viewModel: ProductDetailViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val focusManagerDetail = LocalFocusManager.current
    val keyboardControllerDetail = LocalSoftwareKeyboardController.current
    val densityDetail = LocalDensity.current
    val isKeyboardVisibleDetail = WindowInsets.ime.getBottom(densityDetail) > 0
    BackHandler(enabled = true) {
        if (isKeyboardVisibleDetail) {
            keyboardControllerDetail?.hide()
            focusManagerDetail.clearFocus(force = true)
        } else {
            onClose()
        }
    }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
        viewModel.cargarCatalogoUbicaciones()
    }

    var enterpriseMsg by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    // enterpriseMsg: Pair(message, isError) -> isError true = error, false = éxito

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FDColors.Background)
    ) {
        when (uiState) {
            is ProductDetailState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 2.5.dp)
                }
            }
            is ProductDetailState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.message,
                        style = FDType.Body.copy(color = FDColors.Error, fontSize = 14.sp)
                    )
                }
            }
            is ProductDetailState.Success -> {
                // Blindaje: solo mostrar borrado si el producto cargado es el mismo que se pidió (evita parpadeo al cambiar rápido de ficha)
                if (uiState.isDeleted && uiState.product.indice == productId) {
                    DeletedProductOverlay(show = true, onBack = onClose)
                } else if (uiState.isDeleted) {
                    // Estado stale de otro producto borrado → mostrar carga, no overlay ajeno
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FDColors.Primary, strokeWidth = 2.5.dp)
                    }
                } else {
                    ProductDetailContent(
                    p = uiState.product,
                    movements = uiState.movements,
                    isPrivileged = uiState.isPrivileged,
                    initialTabIndex = initialTabIndex,
                    onClose = onClose,
                    onEdit = { onEdit(uiState.product) },
                    onAdjustStock = { lote -> onAdjustStock(uiState.product, lote) },
                    ubicacionesDisponibles = viewModel.ubicacionesDisponibles,
                    onGuardarConfiguracion = { ubicacion, stockMinimo, activo, diasAlertaVencimiento, nuevoCodigo, onComplete ->
                        viewModel.guardarConfiguracionYLogistica(uiState.product.indice, ubicacion, stockMinimo, activo, diasAlertaVencimiento, nuevoCodigo) { result ->
                            onComplete(result)
                        }
                    },
                    onGenerarCodigoUnico = { viewModel.generarCodigoInternoUnico() },
                    onVerificarDuplicadoCodigo = { codigo -> viewModel.buscarDuplicadoCodigo(codigo, uiState.product.indice) },
                    onMarcarEtiquetaImpresa = { viewModel.marcarEtiquetaImpresa(uiState.product.indice) },
                    onCambiarBloqueoLote = { lote, ponerEnCuarentena, cantidad, motivo, onComplete ->
                        viewModel.cambiarBloqueoLote(uiState.product.indice, lote, ponerEnCuarentena, cantidad, motivo) { result ->
                            onComplete(result)
                            if (result.isFailure) {
                                enterpriseMsg = "Error: ${result.exceptionOrNull()?.message}" to true
                            } else {
                                val msg = if (ponerEnCuarentena) "${cantidad.toInt()} unidades en cuarentena" else "${cantidad.toInt()} unidades liberadas"
                                enterpriseMsg = msg to false
                            }
                        }
                    },
                    onDefinirPrioridadLote = { loteId ->
                        viewModel.definirLotePrioritario(uiState.product.indice, loteId) { result ->
                            if (result.isFailure) {
                                enterpriseMsg = "Error: ${result.exceptionOrNull()?.message}" to true
                            } else {
                                val texto = if (loteId.isNullOrBlank()) "Orden FEFO restaurado" else "Este lote se venderá primero"
                                enterpriseMsg = texto to false
                            }
                        }
                    },
                    onRegistrarDevolucion = { lote, cantidad, guiaRetiro, notaCredito, motivo, modalidad, onComplete ->
                        viewModel.registrarDevolucionProveedor(uiState.product.indice, lote, cantidad, guiaRetiro, notaCredito, motivo, modalidad) { result ->
                            onComplete(result)
                            if (result.isFailure) {
                                enterpriseMsg = "Error: ${result.exceptionOrNull()?.message}" to true
                            } else {
                                enterpriseMsg = "Devolución registrada" to false
                            }
                        }
                    },
                    onRegistrarCanje = { lote, cantidad, nuevoLote, nuevoVenc, guiaCanje, motivo, onComplete ->
                        viewModel.registrarCanjeProducto(uiState.product.indice, lote, cantidad, nuevoLote, nuevoVenc, guiaCanje, motivo) { result ->
                            onComplete(result)
                            if (result.isFailure) {
                                enterpriseMsg = "Error: ${result.exceptionOrNull()?.message}" to true
                            } else {
                                enterpriseMsg = "Canje registrado" to false
                            }
                        }
                    },
                    onAnularIngreso = { lote, motivo, onComplete ->
                        viewModel.anularIngreso(uiState.product.indice, lote, motivo) { result ->
                            onComplete(result)
                            if (result.isFailure) {
                                enterpriseMsg = "Error: ${result.exceptionOrNull()?.message}" to true
                            } else {
                                enterpriseMsg = "Lote anulado" to false
                            }
                        }
                    },
                    onRegistrarMerma = { lote, cantidad, motivo, onComplete ->
                        viewModel.registrarMerma(uiState.product.indice, lote, cantidad, motivo) { result ->
                            onComplete(result)
                            if (result.isFailure) {
                                enterpriseMsg = "Error: ${result.exceptionOrNull()?.message}" to true
                            } else {
                                enterpriseMsg = "Merma registrada" to false
                            }
                        }
                    },
                    onEliminarProducto = { product, motivo, onComplete ->
                        viewModel.eliminarProductoDefinitivo(product.indice, motivo) { result ->
                            onComplete(result)
                            if (result.isFailure) {
                                enterpriseMsg = "Error: ${result.exceptionOrNull()?.message}" to true
                            } else {
                                enterpriseMsg = "Producto eliminado" to false
                                onClose()
                            }
                        }
                    },
                    onEliminadoExito = { onClose() },
                    onGuardarPrecios = { unidadBase, presentaciones, onComplete ->
                        viewModel.guardarPresentacionesYPrecios(uiState.product.indice, unidadBase, presentaciones) { result ->
                            onComplete(result)
                            if (result.isFailure) {
                                enterpriseMsg = "Error: ${result.exceptionOrNull()?.message}" to true
                            } else {
                                enterpriseMsg = "Precios actualizados" to false
                            }
                        }
                    }
                )
                }
            }
        }
        // ── Banner enterprise animado (reemplaza Toast viejo) ──
        AnimatedVisibility(
            visible = enterpriseMsg != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        ) {
            enterpriseMsg?.let { (msg, isError) ->
                Surface(
                    color = if (isError) FDColors.Error.copy(alpha = 0.95f) else FDColors.Success.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isError) FDColors.Error else FDColors.Success),
                    tonalElevation = 6.dp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = FDColors.PrimaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = msg,
                            style = FDType.Body.copy(color = FDColors.PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            maxLines = 2
                        )
                    }
                }
            }
        }
        // Auto-dismiss tras 3.5s
        if (enterpriseMsg != null) {
            LaunchedEffect(enterpriseMsg) {
                delay(3500)
                enterpriseMsg = null
            }
        }
    }
}
