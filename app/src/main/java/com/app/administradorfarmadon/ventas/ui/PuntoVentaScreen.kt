package com.app.administradorfarmadon.ventas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.configuracion.preferencias_sistema.ux.datos.UxPrefs
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.ventas.cierrecaja.ui.SubmoduloCierreCaja
import com.app.administradorfarmadon.ventas.compartido.modelo.Venta
import com.app.administradorfarmadon.ventas.devoluciones.ui.SubmoduloDevoluciones
import com.app.administradorfarmadon.ventas.nuevaventa.logica.NuevaVentaViewModel
import com.app.administradorfarmadon.ventas.nuevaventa.ui.SubmoduloNuevaVenta
import com.app.administradorfarmadon.ventas.ventasdia.ui.SubmoduloVentasDia

/**
 * PANTALLA PRINCIPAL DE VENTAS / POS (R1/R3/R8).
 * Administra los submódulos: Nueva Venta, Ventas del Día, Cierre de Caja y Devoluciones.
 */
@Composable
fun PuntoVentaScreen(
    pestanaInicial: String = "NUEVA VENTA",
    onVolver: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        UxPrefs.init(context)
    }

    val s = recordarMedidaAdaptativa()
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }

    // El carrito y estado del POS vive a nivel de pantalla general para sobrevivir a cambios de pestaña
    val nuevaVentaViewModel: NuevaVentaViewModel = viewModel()
    val uiStateVenta by nuevaVentaViewModel.uiState.collectAsState()

    var submoduloActivo by remember { mutableStateOf(pestanaInicial) }
    var ventaParaDevolver by remember { mutableStateOf<Venta?>(null) }
    var mostrarConfirmacionSalida by remember { mutableStateOf(false) }

    LaunchedEffect(pestanaInicial) {
        submoduloActivo = pestanaInicial
    }

    val hayItemsEnCarrito = uiStateVenta.carrito.isNotEmpty()
    val debeConfirmarSalida = UxPrefs.confirmarSalidaVentas && hayItemsEnCarrito

    val intentarSalir: () -> Unit = {
        if (debeConfirmarSalida) {
            mostrarConfirmacionSalida = true
        } else {
            onVolver()
        }
    }

    BackHandler(enabled = true) {
        intentarSalir()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = s.padScreenH, vertical = s.padScreenV),
        verticalArrangement = Arrangement.spacedBy(s.gapLarge)
    ) {
        // ── SELECTOR FLOTANTE TIPO APPLE (SUBMÓDULOS) ──
        Surface(
            color = FDColors.SurfaceElevated,
            shape = CircleShape,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, FDColors.Border.copy(alpha = 0.5f)),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = s.gapSmall)
        ) {
            Row(
                modifier = Modifier.padding(s.gapTiny),
                horizontalArrangement = Arrangement.spacedBy(s.gapTiny),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val opciones = listOf(
                    "NUEVA VENTA" to Icons.Default.AddShoppingCart,
                    "VENTAS DEL DÍA" to Icons.Default.History,
                    "CIERRE DE CAJA" to Icons.Default.AccountBalanceWallet,
                    "DEVOLUCIONES" to Icons.AutoMirrored.Filled.AssignmentReturn
                )

                opciones.forEach { (titulo, icono) ->
                    val isSel = submoduloActivo == titulo
                    Surface(
                        onClick = { submoduloActivo = titulo },
                        color = if (isSel) FDColors.Primary else Color.Transparent,
                        shape = CircleShape,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = icono,
                                contentDescription = null,
                                tint = if (isSel) FDColors.PrimaryText else FDColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = titulo,
                                style = FDType.Label.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (isSel) FDColors.PrimaryText else FDColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (submoduloActivo) {
                "NUEVA VENTA" -> SubmoduloNuevaVenta(simboloMoneda, nuevaVentaViewModel)
                "VENTAS DEL DÍA" -> SubmoduloVentasDia(
                    simboloMoneda = simboloMoneda,
                    onDevolver = { v ->
                        ventaParaDevolver = v
                        submoduloActivo = "DEVOLUCIONES"
                    }
                )
                "CIERRE DE CAJA" -> SubmoduloCierreCaja(simboloMoneda)
                "DEVOLUCIONES" -> SubmoduloDevoluciones(
                    simboloMoneda = simboloMoneda,
                    ventaInicial = ventaParaDevolver,
                    onVentaConsumida = { ventaParaDevolver = null }
                )
            }
        }
    }

    // ── DIÁLOGO DE CONFIRMACIÓN DE SALIDA CON CARRITO LLENO ──
    if (mostrarConfirmacionSalida) {
        Dialog(onDismissRequest = { mostrarConfirmacionSalida = false }) {
            Surface(
                shape = FDShapes.Medium,
                color = FDColors.Surface,
                border = BorderStroke(1.dp, FDColors.Border),
                modifier = Modifier.width(420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "¿Salir del Punto de Venta?",
                        style = FDType.Heading2.copy(fontWeight = FontWeight.Black, fontSize = 18.sp),
                        color = FDColors.Warning
                    )
                    Text(
                        "Tienes ${uiStateVenta.carrito.size} producto(s) agregados en el carrito de venta. Si sales ahora, los ítems agregados se descartarán.",
                        style = FDType.Body,
                        color = FDColors.TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { mostrarConfirmacionSalida = false },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = FDShapes.Small
                        ) {
                            Text("QUEDARSE", color = FDColors.TextPrimary, style = FDType.Label.copy(fontWeight = FontWeight.Bold))
                        }
                        Button(
                            onClick = {
                                mostrarConfirmacionSalida = false
                                nuevaVentaViewModel.vaciarCarrito()
                                onVolver()
                            },
                            modifier = Modifier.weight(1.3f).height(42.dp),
                            shape = FDShapes.Small,
                            colors = ButtonDefaults.buttonColors(containerColor = FDColors.Error)
                        ) {
                            Text("SALIR Y DESCARTAR", style = FDType.Label.copy(fontWeight = FontWeight.Black))
                        }
                    }
                }
            }
        }
    }
}
