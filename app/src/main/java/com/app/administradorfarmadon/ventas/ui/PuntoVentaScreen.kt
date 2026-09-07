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
    onVolver: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── CABECERA ENTERPRISE: PESTAÑAS CONTINUAS (UNDERLINE TABS) ──
        Surface(
            color = FDColors.Surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pestañas operativas
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                            Column(
                                modifier = Modifier
                                    .clickable { submoduloActivo = titulo }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Icon(
                                        imageVector = icono,
                                        contentDescription = null,
                                        tint = if (isSel) FDColors.Primary else FDColors.TextSecondary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = titulo,
                                        style = FDType.Label.copy(
                                            fontSize = 12.sp,
                                            fontWeight = if (isSel) FontWeight.Black else FontWeight.SemiBold,
                                            letterSpacing = 0.4.sp
                                        ),
                                        color = if (isSel) FDColors.Primary else FDColors.TextSecondary
                                    )
                                    // Contador de carrito o indicador de estado de caja
                                    if (titulo == "NUEVA VENTA" && uiStateVenta.totalItems > 0) {
                                        Surface(
                                            color = if (isSel) FDColors.Primary else FDColors.InputBackground,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "${uiStateVenta.totalItems}",
                                                style = FDType.Caption.copy(
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isSel) FDColors.PrimaryText else FDColors.TextPrimary
                                                ),
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (titulo == "CIERRE DE CAJA") {
                                        val esTurnoVencido = uiStateVenta.estadoCaja.esTurnoVencido
                                        val cajaAbierta = uiStateVenta.cajaAbierta
                                        if (esTurnoVencido) {
                                            Surface(
                                                color = FDColors.Warning,
                                                shape = CircleShape
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.PriorityHigh,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = "CIERRE PENDIENTE",
                                                        style = FDType.Caption.copy(
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.White
                                                        )
                                                    )
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (cajaAbierta) FDColors.Success else FDColors.Warning)
                                            )
                                        }
                                    }
                                }

                                // Indicador underline moderno
                                Box(
                                    modifier = Modifier
                                        .height(3.dp)
                                        .width(if (isSel) 48.dp else 0.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(if (isSel) FDColors.Primary else Color.Transparent)
                                )
                            }
                        }
                    }

                    // Sede activa
                    Surface(
                        color = FDColors.InputBackground.copy(alpha = 0.5f),
                        shape = FDShapes.Small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Store, null, modifier = Modifier.size(15.dp), tint = FDColors.Primary)
                            Text(
                                SessionManager.sucursalNombre.ifBlank { "Sede Mostrador" },
                                style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                color = FDColors.TextPrimary
                            )
                        }
                    }
                }
                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = 1.dp)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (submoduloActivo) {
                "NUEVA VENTA" -> SubmoduloNuevaVenta(
                    simboloMoneda = simboloMoneda,
                    viewModel = nuevaVentaViewModel,
                    onNavigate = { ruta ->
                        if (ruta == "caja" || ruta == "CIERRE DE CAJA") {
                            submoduloActivo = "CIERRE DE CAJA"
                        } else {
                            onNavigate(ruta)
                        }
                    }
                )
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
