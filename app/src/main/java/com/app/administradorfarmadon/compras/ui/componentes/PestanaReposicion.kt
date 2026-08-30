package com.app.administradorfarmadon.compras.ui.componentes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.logica.ItemPedidoCompra
import com.app.administradorfarmadon.compras.logica.PedidoProveedor
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonPrimario
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.FDBotonSecundario
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PestanaReposicion(
    productosAgrupadosPorProveedor: Map<String, List<PharmProduct>>,
    pedidosActivos: List<PedidoProveedor>,
    pedidosGuardados: List<PedidoCompra> = emptyList(),
    proveedores: List<com.app.administradorfarmadon.inventario.compartido.modelo.Proveedor> = emptyList(),
    pedidosPorProveedor: Map<String, Map<String, Int>>,
    proveedoresExpandidos: Set<String>,
    subTabPedidosDerecha: String = "BORRADOR",
    pedidoEnRevision: PedidoProveedor?,
    mostrarModalRevision: Boolean,
    mostrarModalConfirmacionEnvio: Boolean = false,
    pedidoParaConfirmarEnvio: PedidoProveedor? = null,
    enviandoPedido: Boolean = false,
    onToggleExpandirProveedor: (String) -> Unit,
    onModificarCantidadProducto: (PharmProduct, Int) -> Unit,
    onReponerSugeridosProveedor: (String) -> Unit = {},
    onReponerTodosSugeridosGlobal: () -> Unit = {},
    onAbrirRevisionPedido: (PedidoProveedor) -> Unit,
    onCerrarRevisionPedido: () -> Unit,
    onLimpiarPedidoProveedor: (String) -> Unit,
    onRemoverProductoDePedido: (String, String) -> Unit = { _, _ -> },
    onSeleccionarSubTabPedidosDerecha: (String) -> Unit = {},
    onPrepararConfirmacionEnvio: (PedidoProveedor) -> Unit = {},
    onConfirmarPedidoEnviado: (PedidoProveedor) -> Unit = {},
    onCerrarConfirmacionEnvio: () -> Unit = {},
    onCancelarPedidoEnviado: (String) -> Unit = {},
    onRecibirMercaderia: (PedidoCompra) -> Unit = {},
    onCerrarOrdenConAjuste: (PedidoCompra) -> Unit = {},
    onDescartarProductoDePedido: (String, String) -> Unit = { _, _ -> },
    onActualizarTelefonoProveedor: (String, String) -> Unit = { _, _ -> },
    enCaminoPorProducto: Map<String, com.app.administradorfarmadon.compras.logica.ProductoEnCamino> = emptyMap(),
    contribuidoresCarrito: Map<String, Map<String, Map<String, Int>>> = emptyMap(),
    productoPendienteConfirmar: PharmProduct? = null,
    cantidadExtraPropuesta: Int = 0,
    onConfirmarAdicionExtra: () -> Unit = {},
    onDescartarAdicionExtra: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    val context = LocalContext.current
    val simboloMoneda = SessionManager.monedaSimbolo.ifBlank { "S/" }
    // BackHandler quiet teclado primero, luego diálogos locales (sin competencia)
    val focusManagerRepo = LocalFocusManager.current
    val keyboardControllerRepo = LocalSoftwareKeyboardController.current
    val densityRepo = LocalDensity.current
    val isKeyboardVisibleRepo =
        androidx.compose.foundation.layout.WindowInsets.ime.getBottom(densityRepo) > 0
    BackHandler(enabled = true) {
        when {
            isKeyboardVisibleRepo -> {
                keyboardControllerRepo?.hide(); focusManagerRepo.clearFocus(force = true)
            }

            mostrarModalRevision && pedidoEnRevision != null -> onCerrarRevisionPedido()
            mostrarModalConfirmacionEnvio && pedidoParaConfirmarEnvio != null -> onCerrarConfirmacionEnvio()
            productoPendienteConfirmar != null -> onDescartarAdicionExtra()
            else -> {}
        }
    }

    // CÁLCULO DE MíTRICAS GLOBALES (Rigor Logístico Brixo)
    val totalCriticosGlobal = remember(productosAgrupadosPorProveedor) {
        productosAgrupadosPorProveedor.values.flatten()
            .count { (it.minStock > 0 && it.stock <= it.minStock) || it.stock <= 0 }
    }
    val montoEnBorrador = remember(pedidosActivos) { pedidosActivos.sumOf { it.totalInversion } }

    // DIÁLOGO DE CONFIRMACIíN DE ENVíO
    if (mostrarModalConfirmacionEnvio && pedidoParaConfirmarEnvio != null) {
        Dialog(onDismissRequest = onCerrarConfirmacionEnvio) {
            Surface(
                shape = FDShapes.Large,
                color = FDColors.SurfaceElevated,
                border = BorderStroke(s.borderWidth, FDColors.Border),
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .padding(s.lg)
            ) {
                Column(
                    modifier = Modifier.padding(s.padCard),
                    verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(FDColors.Success.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = FDColors.Success,
                                modifier = Modifier.size(s.iconSmall * 1.2f)
                            )
                        }
                        Column {
                            Text(
                                text = "¿Enviaste el pedido a la droguería?",
                                style = FDType.Heading3.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = FDColors.TextPrimary
                            )
                            Text(
                                text = pedidoParaConfirmarEnvio.proveedorNombre,
                                style = FDType.BodySmall.copy(fontSize = 12.sp),
                                color = FDColors.TextSecondary
                            )
                        }
                    }

                    Text(
                        text = "Al confirmar, la orden quedará registrada en 'Pedidos Enviados' como 'Esperando Entrega' y se limpiará del borrador.",
                        style = FDType.Body.copy(fontSize = 12.5.sp),
                        color = FDColors.TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(s.gapMedium * 0.85f)
                    ) {
                        OutlinedButton(
                            onClick = onCerrarConfirmacionEnvio,
                            modifier = Modifier
                                .weight(1f)
                                .height(s.btnMediumH),
                            shape = RoundedCornerShape(s.radiusInput),
                            border = BorderStroke(s.borderWidth, FDColors.Border)
                        ) {
                            Text(
                                "SEGUIR EDITANDO",
                                style = FDType.Label.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = FDColors.TextSecondary
                            )
                        }
                        Button(
                            onClick = { onConfirmarPedidoEnviado(pedidoParaConfirmarEnvio) },
                            enabled = !enviandoPedido,
                            modifier = Modifier
                                .weight(1f)
                                .height(s.btnMediumH),
                            shape = RoundedCornerShape(s.radiusInput),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FDColors.Primary,
                                contentColor = FDColors.PrimaryText
                            )
                        ) {
                            if (enviandoPedido) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = FDColors.PrimaryText,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "──œ“ Sí, GUARDAR ENVIADO",
                                    style = FDType.Label.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ESCUDO ANTI DOBLE PEDIDO
    productoPendienteConfirmar?.let { prod ->
        val info = enCaminoPorProducto[prod.id]
        AlertDialog(
            onDismissRequest = onDescartarAdicionExtra,
            containerColor = FDColors.SurfaceElevated,
            title = {
                Text(
                    "Este producto ya está en camino",
                    style = FDType.Heading3.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FDColors.TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)) {
                    val ordenesTxt = info?.ordenes?.filter { o -> o.isNotBlank() }?.joinToString()
                        ?.let { o -> " ($o)" } ?: ""
                    Text(
                        "${prod.name}: ${info?.unidades ?: 0} unidades ya pedidas y en camino$ordenesTxt.",
                        style = FDType.Body.copy(fontSize = 12.5.sp),
                        color = FDColors.TextPrimary
                    )
                    Text(
                        "¿Quieres añadir $cantidadExtraPropuesta más como pedido NUEVO a la misma droguería?",
                        style = FDType.BodySmall.copy(fontSize = 12.sp),
                        color = FDColors.TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmarAdicionExtra,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FDColors.Primary,
                        contentColor = FDColors.PrimaryText
                    ),
                    shape = RoundedCornerShape(s.radiusInput * 0.75f)
                ) {
                    Text(
                        "Sí, AÑADIR $cantidadExtraPropuesta MÁS",
                        style = FDType.Label.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDescartarAdicionExtra,
                    shape = RoundedCornerShape(s.radiusInput * 0.75f)
                ) {
                    Text(
                        "NO, MEJOR NO",
                        style = FDType.Label.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        )
    }

    // Raíz con overlay: el panel de revisión vive ARRIBA de las dos columnas (zIndex), nunca debajo
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (productosAgrupadosPorProveedor.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(s.padCardLarge),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = FDColors.Surface,
                    shape = RoundedCornerShape(s.radiusCard),
                    border = BorderStroke(s.borderWidth, FDColors.Border),
                    modifier = Modifier.widthIn(max = 520.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(s.padCardLarge),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(s.iconLarge * 1.8f)
                                .clip(CircleShape)
                                .background(FDColors.TextPrimary.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = FDColors.TextSecondary,
                                modifier = Modifier.size(s.iconMedium * 1.3f)
                            )
                        }
                        Text(
                            text = "Catálogo de Productos Vacío",
                            style = FDType.Heading2.copy(fontSize = 19.sp),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            text = "No se encontraron productos registrados en el inventario. Al agregar productos y asignarles proveedor o laboratorio, se organizarán aquí.",
                            style = FDType.Body.copy(fontSize = 13.5.sp),
                            color = FDColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val totalWidth = maxWidth
                val isPantallaGrande = totalWidth >= 960.dp
                val isPantallaMediana = totalWidth in 720.dp..959.dp

                val paddingHorizontal =
                    if (isPantallaGrande) 24.dp else if (isPantallaMediana) 16.dp else 12.dp
                val paddingVertical = if (isPantallaGrande) 16.dp else 12.dp
                val espacioEntreColumnas = if (isPantallaGrande) 16.dp else 12.dp
                val paddingTarjeta = if (isPantallaGrande) 18.dp else 14.dp

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
                    horizontalArrangement = Arrangement.spacedBy(espacioEntreColumnas)
                ) {
                    //
                    // COLUMNA IZQUIERDA: CATÁLOGO DE PRODUCTOS AGRUPADOS POR PROVEEDOR (55%)
                    //
                    Surface(
                        color = FDColors.Surface,
                        shape = RoundedCornerShape(s.radiusCard),
                        border = BorderStroke(s.borderWidth, FDColors.Border),
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingTarjeta)
                        ) {
                            // Header Dashboard de Salud de Stock (Grado Brixo)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "MONITOREO DE ABASTECIMIENTO",
                                        style = FDType.Label.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.2.sp
                                        ),
                                        color = FDColors.TextTertiary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Salud del Inventario",
                                        style = FDType.Heading2.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = FDColors.TextPrimary
                                    )
                                }

                                // Métricas de Auditoría Real
                                Row(horizontalArrangement = Arrangement.spacedBy(s.gapLarge)) {
                                    MetricItemReposicion(
                                        label = "QUIEBRES CRíTICOS",
                                        value = "$totalCriticosGlobal íTEMS",
                                        color = if (totalCriticosGlobal > 0) FDColors.Error else FDColors.Success
                                    )
                                    VerticalDivider(
                                        modifier = Modifier.height(s.btnSmallH * 0.85f),
                                        color = FDColors.Border
                                    )
                                    MetricItemReposicion(
                                        label = "MONTO EN BORRADOR",
                                        value = "$simboloMoneda ${
                                            String.format(
                                                Locale.US,
                                                "%,.2f",
                                                montoEnBorrador
                                            )
                                        }",
                                        color = FDColors.Primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(s.gapLarge))

                            // BARRA DE NAVEGACIíN EJECUTIVA CONTINUA (UNDERLINE TABS ENTERPRISE)
                            var filtroRapido by remember { mutableStateOf("TODOS") }
                            val totalProdsGlobal =
                                productosAgrupadosPorProveedor.values.sumOf { it.size }
                            val todosLosProdsFlatten =
                                productosAgrupadosPorProveedor.values.flatten()
                            val totalCriticosGlobal =
                                todosLosProdsFlatten.count { it.stock <= it.minStock }
                            val criticosPendientesGlobal =
                                productosAgrupadosPorProveedor.entries.sumOf { (provKey, prods) ->
                                    val carro = pedidosPorProveedor[provKey] ?: emptyMap()
                                    prods.count {
                                        it.stock <= it.minStock &&
                                                (carro[it.id] ?: 0) == 0 &&
                                                (enCaminoPorProducto[it.id]?.unidades ?: 0) == 0
                                    }
                                }

                            Spacer(modifier = Modifier.height(14.dp))

                            // NAVEGACIíN TÁCTICA DE INVENTARIO (Underline Tabs)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(28.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                listOf(
                                    Triple("TODOS", "TODOS LOS PRODUCTOS", totalProdsGlobal),
                                    Triple("CRITICOS", "STOCK POR AGOTARSE", totalCriticosGlobal)
                                ).forEach { (idFiltro, label, count) ->
                                    val isSel = filtroRapido == idFiltro
                                    Column(
                                        modifier = Modifier
                                            .clickable { filtroRapido = idFiltro }
                                            .padding(bottom = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)
                                        ) {
                                            Text(
                                                text = label,
                                                style = FDType.Label.copy(
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSel) FontWeight.Black else FontWeight.Medium,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = if (isSel) FDColors.Primary else FDColors.TextSecondary
                                            )

                                            // Badge Nítido
                                            Surface(
                                                color = if (isSel) FDColors.Primary.copy(alpha = 0.1f) else FDColors.TextPrimary.copy(
                                                    alpha = 0.05f
                                                ),
                                                shape = FDShapes.XSmall
                                            ) {
                                                Text(
                                                    text = "$count",
                                                    style = FDType.Label.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black
                                                    ),
                                                    color = if (isSel) FDColors.Primary else FDColors.TextTertiary,
                                                    modifier = Modifier.padding(
                                                        horizontal = 6.dp,
                                                        vertical = 1.dp
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        AnimatedVisibility(visible = isSel) {
                                            Box(
                                                modifier = Modifier
                                                    .height(3.dp)
                                                    .width(32.dp)
                                                    .clip(FDShapes.Full)
                                                    .background(FDColors.Primary)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                            Spacer(Modifier.height(12.dp))

                            // Lista Agrupada por Tarjeta de Proveedor
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                            ) {
                                val gruposFiltrados =
                                    productosAgrupadosPorProveedor.mapValues { (_, prods) ->
                                        when (filtroRapido) {
                                            "CRITICOS" -> prods.filter { (it.minStock > 0 && it.stock <= it.minStock) || it.stock <= 0 }
                                            else -> prods
                                        }
                                    }.filter { it.value.isNotEmpty() }

                                items(
                                    items = gruposFiltrados.entries.toList(),
                                    key = { it.key }
                                ) { (proveedorNombre, productos) ->
                                    val estaColapsado =
                                        !proveedoresExpandidos.contains(proveedorNombre)
                                    val carroProv =
                                        pedidosPorProveedor[proveedorNombre] ?: emptyMap()

                                    TarjetaProveedorCatalogo(
                                        proveedorNombre = proveedorNombre,
                                        productos = productos,
                                        carroProv = carroProv,
                                        enCaminoPorProducto = enCaminoPorProducto,
                                        contribuidoresProv = contribuidoresCarrito[proveedorNombre]
                                            ?: emptyMap(),
                                        estaColapsado = estaColapsado,
                                        simboloMoneda = simboloMoneda,
                                        onToggleExpandir = {
                                            onToggleExpandirProveedor(
                                                proveedorNombre
                                            )
                                        },
                                        onReponerSugeridos = {
                                            onReponerSugeridosProveedor(
                                                proveedorNombre
                                            )
                                        },
                                        onModificarCantidad = { prod, delta ->
                                            onModificarCantidadProducto(
                                                prod,
                                                delta
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    //
                    // COLUMNA DERECHA: GESTIíN DE PEDIDOS Y MERCADERíA EN TRÁNSITO (45%)
                    //
                    Surface(
                        color = FDColors.Surface,
                        shape = RoundedCornerShape(s.radiusCard),
                        border = BorderStroke(s.borderWidth, FDColors.Border),
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingTarjeta)
                        ) {
                            val pedidosPendientes = remember(pedidosGuardados) {
                                pedidosGuardados.filter { it.estado == "ENVIADO" || it.estado == "ENTREGA_PARCIAL" }
                            }

                            // NAVEGACIíN DE íRDENES (Underline Tabs)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
                            ) {
                                listOf(
                                    "BORRADOR" to "EN BORRADOR",
                                    "ENVIADOS" to "PENDIENTES DE INGRESO"
                                ).forEach { (clave, etiqueta) ->
                                    val isSelected = subTabPedidosDerecha == clave
                                    val count =
                                        if (clave == "BORRADOR") pedidosActivos.size else pedidosPendientes.size

                                    Column(
                                        modifier = Modifier
                                            .clickable { onSeleccionarSubTabPedidosDerecha(clave) }
                                            .padding(vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)
                                        ) {
                                            Text(
                                                text = etiqueta,
                                                style = FDType.Label.copy(
                                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                                    fontSize = 11.sp,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = if (isSelected) FDColors.Primary else FDColors.TextSecondary
                                            )
                                            // Badge Nítido
                                            Surface(
                                                color = if (isSelected) FDColors.Primary.copy(alpha = 0.1f) else FDColors.TextPrimary.copy(
                                                    alpha = 0.05f
                                                ),
                                                shape = FDShapes.XSmall
                                            ) {
                                                Text(
                                                    text = "$count",
                                                    style = FDType.Label.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black
                                                    ),
                                                    color = if (isSelected) FDColors.Primary else FDColors.TextTertiary,
                                                    modifier = Modifier.padding(
                                                        horizontal = 6.dp,
                                                        vertical = 1.dp
                                                    )
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        AnimatedVisibility(visible = isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .height(3.dp)
                                                    .width(24.dp)
                                                    .clip(FDShapes.Full)
                                                    .background(FDColors.Primary)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                            Spacer(modifier = Modifier.height(s.sm))

                            // Contenido según subtab (Borrador vs Enviados)
                            if (subTabPedidosDerecha == "BORRADOR") {
                                if (pedidosActivos.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(s.padCard),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(s.gapSmall)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(CircleShape)
                                                    .background(FDColors.TextPrimary.copy(alpha = 0.05f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.AddShoppingCart,
                                                    null,
                                                    tint = FDColors.TextTertiary,
                                                    modifier = Modifier.size(s.iconSmall * 1.3f)
                                                )
                                            }
                                            Text(
                                                text = "Sin productos seleccionados",
                                                style = FDType.Heading3.copy(fontSize = 14.5.sp),
                                                color = FDColors.TextPrimary
                                            )
                                            Text(
                                                text = "Usa los botones [+] en el catálogo de la izquierda para agregar medicamentos. Tus pedidos se organizarán automáticamente aquí por droguería.",
                                                style = FDType.BodySmall.copy(
                                                    fontSize = 12.sp
                                                ),
                                                color = FDColors.TextSecondary,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                                    ) {
                                        items(
                                            pedidosActivos,
                                            key = { it.proveedorNombre }) { pedido ->
                                            val proveedorObj = proveedores.find {
                                                it.nombre.equals(
                                                    pedido.proveedorNombre,
                                                    ignoreCase = true
                                                ) ||
                                                        it.id == pedido.proveedorId
                                            }
                                            val montoMinimo = proveedorObj?.montoMinimoPedido ?: 0.0

                                            TarjetaPedidoProveedor(
                                                pedido = pedido,
                                                simboloMoneda = simboloMoneda,
                                                montoMinimoPedido = montoMinimo,
                                                onRevisar = { onAbrirRevisionPedido(pedido) },
                                                onVaciar = { onLimpiarPedidoProveedor(pedido.proveedorNombre) },
                                                onRemoverProducto = { prodId ->
                                                    onRemoverProductoDePedido(
                                                        pedido.proveedorNombre,
                                                        prodId
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Subtab ENVIADOS (estrictamente los pendientes de entrega)
                                if (pedidosPendientes.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(s.padCard),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(s.gapSmall)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(CircleShape)
                                                    .background(FDColors.TextPrimary.copy(alpha = 0.05f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.LocalShipping,
                                                    null,
                                                    tint = FDColors.TextTertiary,
                                                    modifier = Modifier.size(s.iconSmall * 1.3f)
                                                )
                                            }
                                            Text(
                                                text = "No tienes pedidos pendientes de entrega",
                                                style = FDType.Heading3.copy(fontSize = 14.5.sp),
                                                color = FDColors.TextPrimary
                                            )
                                            Text(
                                                text = "Los pedidos enviados o con entrega parcial aparecerán aquí para asentar la llegada de mercadería. Las compras liquidadas se guardan en Cuentas por Pagar.",
                                                style = FDType.BodySmall.copy(
                                                    fontSize = 12.sp
                                                ),
                                                color = FDColors.TextSecondary,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(s.gapMedium * 0.85f)
                                    ) {
                                        items(
                                            pedidosPendientes,
                                            key = { it.id }) { pedidoGuardado ->
                                            TarjetaPedidoEnviado(
                                                pedido = pedidoGuardado,
                                                simboloMoneda = simboloMoneda,
                                                onRecibirMercaderia = {
                                                    onRecibirMercaderia(
                                                        pedidoGuardado
                                                    )
                                                },
                                                onCerrarConAjuste = {
                                                    onCerrarOrdenConAjuste(
                                                        pedidoGuardado
                                                    )
                                                },
                                                onDescartarProducto = { prodId ->
                                                    onDescartarProductoDePedido(
                                                        pedidoGuardado.id,
                                                        prodId
                                                    )
                                                },
                                                onCancelar = {
                                                    onCancelarPedidoEnviado(
                                                        pedidoGuardado.id
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // REVISIÓN Y ENVÍO panel POR ENCIMA de los paneles (zIndex 10) nunca oculto
            if (mostrarModalRevision && pedidoEnRevision != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(10f)
                ) {
                    DialogoRevisionPedido(
                        pedido = pedidoEnRevision,
                        simboloMoneda = simboloMoneda,
                        onDismiss = onCerrarRevisionPedido,
                        onConfirmarWhatsApp = { telUsado, guardarEnFicha ->
                            if (guardarEnFicha && telUsado.isNotBlank()) {
                                onActualizarTelefonoProveedor(
                                    pedidoEnRevision.proveedorNombre,
                                    telUsado
                                )
                            }
                            val textoPedido = generarTextoWhatsApp(
                                farmaciaNombre = SessionManager.sucursalNombre.ifBlank { "Mi Farmacia" },
                                usuarioNombre = SessionManager.nombreUsuario.ifBlank { "Administración" },
                                simboloMoneda = simboloMoneda,
                                pedido = pedidoEnRevision
                            )
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Pedido WhatsApp", textoPedido)
                            clipboard.setPrimaryClip(clip)
                            try {
                                val telLimpio = telUsado.filter { it.isDigit() }
                                val sendIntent = if (telLimpio.isNotBlank()) {
                                    Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(
                                            "https://api.whatsapp.com/send?phone=$telLimpio&text=${
                                                Uri.encode(textoPedido)
                                            }"
                                        )
                                    }
                                } else {
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, textoPedido)
                                    }
                                }
                                context.startActivity(sendIntent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "¡Orden copiada al portapapeles!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            onPrepararConfirmacionEnvio(pedidoEnRevision)
                        },
                        onMarcarEnviadoDirecto = {
                            onConfirmarPedidoEnviado(pedidoEnRevision)
                            onCerrarRevisionPedido()
                        }
                    )
                }
            }
        }
    }
}

// TARJETA INDEPENDIENTE DE PEDIDO POR PROVEEDOR
    @Composable
    private fun TarjetaPedidoProveedor(
        pedido: PedidoProveedor,
        simboloMoneda: String,
        montoMinimoPedido: Double = 0.0,
        onRevisar: () -> Unit,
        onVaciar: () -> Unit,
        onRemoverProducto: (String) -> Unit
    ) {
        val s = recordarMedidaAdaptativa()
        if (pedido.items.isEmpty()) return

        Surface(
            color = FDColors.SurfaceElevated.copy(alpha = 0.5f),
            shape = FDShapes.Medium,
            border = BorderStroke(s.borderWidth, FDColors.Border.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(s.gapMedium * 0.85f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(s.gapMedium * 0.85f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(s.iconMedium)
                                .clip(CircleShape)
                                .background(FDColors.Primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Business,
                                null,
                                tint = FDColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = pedido.proveedorNombre.uppercase(),
                                style = FDType.Body.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                ),
                                color = FDColors.TextPrimary
                            )
                            Text(
                                text = "${pedido.items.size} íTEMS SELECCIONADOS",
                                style = FDType.Label.copy(fontSize = 9.sp),
                                color = FDColors.TextTertiary
                            )
                        }
                    }

                    Text(
                        text = "$simboloMoneda " + String.format(
                            Locale.US,
                            "%,.2f",
                            pedido.totalInversion
                        ),
                        style = FDType.Numeric.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = FDColors.TextPrimary
                    )
                }

                // Guía suave de mínimo no bloquea, solo enseña. Se puede romper y la próxima vez vuelve a avisar.
                if (montoMinimoPedido > 0 && pedido.totalInversion < montoMinimoPedido) {
                    val faltante = montoMinimoPedido - pedido.totalInversion
                    Surface(
                        color = FDColors.Primary.copy(alpha = 0.06f),
                        shape = FDShapes.XSmall,
                        border = BorderStroke(0.5.dp, FDColors.Primary.copy(alpha = 0.18f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                tint = FDColors.Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Sugerido: mínimo $simboloMoneda ${
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            montoMinimoPedido
                                        )
                                    } —” te faltan $simboloMoneda ${
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            faltante
                                        )
                                    } para despacho óptimo",
                                    style = FDType.Label.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = FDColors.Primary
                                )
                                Text(
                                    text = "Puedes enviar igual; la próxima vez se te recordará. Edita el mínimo en la ficha del proveedor si cambió.",
                                    style = FDType.Caption.copy(fontSize = 9.sp),
                                    color = FDColors.TextTertiary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
                ) {
                    IconButton(
                        onClick = onVaciar,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(FDShapes.Small)
                            .background(FDColors.TextPrimary.copy(alpha = 0.04f))
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            null,
                            tint = FDColors.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    FDBotonPrimario(
                        texto = "REVISAR Y ENVIAR PEDIDO",
                        onClick = onRevisar,
                        icono = Icons.AutoMirrored.Filled.Chat,
                        modifier = Modifier
                            .weight(1f)
                            .height(s.btnSmallH)
                    )
                }
            }
        }
    }

    // TARJETA DE PEDIDO ENVIADO (PENDIENTE DE INGRESO)
    @Composable
    private fun TarjetaPedidoEnviado(
        pedido: PedidoCompra,
        simboloMoneda: String,
        onRecibirMercaderia: () -> Unit,
        onCerrarConAjuste: () -> Unit,
        onDescartarProducto: (String) -> Unit = {},
        onCancelar: () -> Unit
    ) {
        val s = recordarMedidaAdaptativa()
        var expandirHistorial by remember { mutableStateOf(false) }
        var expandirPendientes by remember { mutableStateOf(false) }
        var confirmarAjuste by remember { mutableStateOf(false) }
        var confirmarCancelacion by remember { mutableStateOf(false) }
        var productoParaDescartar by remember { mutableStateOf<ItemPedidoCompra?>(null) }

        val productosPendientes = pedido.items.filter { it.saldoPendiente > 0 }
        val unidadesPendientes = productosPendientes.sumOf { it.saldoPendiente }

        Surface(
            color = FDColors.SurfaceElevated.copy(alpha = 0.4f),
            shape = FDShapes.Medium,
            border = BorderStroke(s.borderWidth, FDColors.Border.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(s.gapSmall)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = pedido.proveedorNombre.uppercase(),
                            style = FDType.Body.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = FDColors.TextPrimary
                        )
                        Text(
                            text = (if (pedido.estado == "ENTREGA_PARCIAL") "PARCIAL DESDE" else "ENVIADO") +
                                    ": ${pedido.fechaEmision} | ${pedido.items.size} íTEMS",
                            style = FDType.Label.copy(fontSize = 9.sp),
                            color = FDColors.TextTertiary
                        )
                    }

                    Surface(
                        color = FDColors.Primary.copy(alpha = 0.08f),
                        shape = FDShapes.XSmall
                    ) {
                        Text(
                            text = if (pedido.estado == "ENTREGA_PARCIAL") "ENTREGA PARCIAL" else "ESPERANDO ENTREGA",
                            style = FDType.Label.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = FDColors.Primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f))

                // Historial de Recepciones (Acordeón de Trazabilidad)
                if (pedido.recepciones.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(s.radiusInput * 0.75f))
                            .background(FDColors.TextPrimary.copy(alpha = 0.02f))
                            .border(
                                0.6.dp,
                                FDColors.Border.copy(alpha = 0.5f),
                                RoundedCornerShape(s.radiusInput * 0.75f)
                            )
                            .clickable { expandirHistorial = !expandirHistorial }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(s.gapTiny * 1.0f)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ReceiptLong,
                                    null,
                                    tint = FDColors.Primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Trazabilidad: ${pedido.recepciones.size} entrega(s) física(s)",
                                    style = FDType.Label.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = FDColors.TextPrimary
                                )
                            }
                            Icon(
                                imageVector = if (expandirHistorial) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = FDColors.TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        AnimatedVisibility(visible = expandirHistorial) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(s.gapTiny * 1.0f)
                            ) {
                                pedido.recepciones.forEach { rec ->
                                    Surface(
                                        color = FDColors.Surface,
                                        shape = RoundedCornerShape(s.radiusInput * 0.55f),
                                        border = BorderStroke(
                                            s.borderWidth * 0.6f,
                                            FDColors.Border
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = "Factura: ${rec.numeroFactura} · Entrega: $simboloMoneda ${
                                                    String.format(
                                                        Locale.US,
                                                        "%.2f",
                                                        rec.items.sumOf { it.costoTotalReal })
                                                }",
                                                style = FDType.Label.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = FDColors.TextPrimary
                                            )
                                            Text(
                                                text = "Fecha: ${rec.fechaLegible}",
                                                style = FDType.BodySmall.copy(fontSize = 10.sp),
                                                color = FDColors.TextTertiary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                // Faltantes de la orden: visibles, contados y descartables uno por uno
                if (productosPendientes.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(s.radiusInput * 0.75f))
                            .background(FDColors.TextPrimary.copy(alpha = 0.02f))
                            .border(
                                0.6.dp,
                                FDColors.Border.copy(alpha = 0.5f),
                                RoundedCornerShape(s.radiusInput * 0.75f)
                            )
                            .clickable { expandirPendientes = !expandirPendientes }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(s.gapTiny * 1.0f)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    null,
                                    tint = FDColors.Warning,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Faltan ${productosPendientes.size} producto(s) · $unidadesPendientes und.",
                                    style = FDType.Label.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = FDColors.Warning
                                )
                            }
                            Icon(
                                imageVector = if (expandirPendientes) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = FDColors.TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        AnimatedVisibility(visible = expandirPendientes) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                productosPendientes.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.productoNombre,
                                                style = FDType.BodySmall.copy(
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = FDColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Quedan ${item.saldoPendiente} de ${item.cantidad} pedidas",
                                                style = FDType.BodySmall.copy(fontSize = 10.sp),
                                                color = FDColors.TextTertiary
                                            )
                                        }
                                        IconButton(
                                            onClick = { productoParaDescartar = item },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.RemoveShoppingCart,
                                                "Descartar faltante",
                                                tint = FDColors.Warning,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                // Footer de Acción Industrial
                FDBotonPrimario(
                    texto = "INGRESAR MERCADERíA A STOCK",
                    onClick = onRecibirMercaderia,
                    icono = Icons.Default.Inventory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(s.btnSmallH)
                )

                // UNA sola salida según la verdad de esta orden (jamás dos juntas):
                // nada recibido todavía CANCELAR ORDEN ya llegó algo CERRAR CON AJUSTE.
                if (pedido.estado == "ENVIADO") {
                    OutlinedButton(
                        onClick = { confirmarCancelacion = true },
                        shape = RoundedCornerShape(s.radiusInput),
                        border = BorderStroke(s.borderWidth, FDColors.Warning.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.Warning),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(s.btnSmallH * 0.95f)
                    ) {
                        Text(
                            "CANCELAR ORDEN",
                            style = FDType.Label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    FDBotonSecundario(
                        texto = "CERRAR CON AJUSTE",
                        onClick = { confirmarAjuste = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(s.btnSmallH * 0.95f)
                    )
                }
            }
        }

        if (confirmarAjuste) {
            AlertDialog(
                onDismissRequest = { confirmarAjuste = false },
                containerColor = FDColors.SurfaceElevated,
                title = {
                    Text(
                        "¿Cerrar con ajuste?",
                        style = FDType.Heading3.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = FDColors.TextPrimary
                    )
                },
                text = {
                    Text(
                        "Las $unidadesPendientes unidades que faltan NO llegarán nunca. La orden queda cerrada y sus faltantes dejan de contar como \"en camino\". Lo ya recibido NO se toca.",
                        style = FDType.Body.copy(fontSize = 12.5.sp),
                        color = FDColors.TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { confirmarAjuste = false; onCerrarConAjuste() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Primary,
                            contentColor = FDColors.PrimaryText
                        ),
                        shape = RoundedCornerShape(s.radiusInput * 0.75f)
                    ) {
                        Text(
                            "Sí, CERRAR CON AJUSTE",
                            style = FDType.Label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { confirmarAjuste = false },
                        shape = RoundedCornerShape(s.radiusInput * 0.75f)
                    ) {
                        Text(
                            "NO, MEJOR NO",
                            style = FDType.Label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            )
        }

        if (confirmarCancelacion) {
            AlertDialog(
                onDismissRequest = { confirmarCancelacion = false },
                containerColor = FDColors.SurfaceElevated,
                title = {
                    Text(
                        "¿Cancelar esta orden?",
                        style = FDType.Heading3.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = FDColors.TextPrimary
                    )
                },
                text = {
                    Text(
                        "Se cancela la orden completa de ${pedido.proveedorNombre}. Todavía no se recibió mercadería en ella y deja de contar como \"en camino\".",
                        style = FDType.Body.copy(fontSize = 12.5.sp),
                        color = FDColors.TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { confirmarCancelacion = false; onCancelar() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Primary,
                            contentColor = FDColors.PrimaryText
                        ),
                        shape = RoundedCornerShape(s.radiusInput * 0.75f)
                    ) {
                        Text(
                            "Sí, CANCELAR ORDEN",
                            style = FDType.Label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { confirmarCancelacion = false },
                        shape = RoundedCornerShape(s.radiusInput * 0.75f)
                    ) {
                        Text(
                            "NO, MEJOR NO",
                            style = FDType.Label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            )
        }

        productoParaDescartar?.let { prod ->
            AlertDialog(
                onDismissRequest = { productoParaDescartar = null },
                containerColor = FDColors.SurfaceElevated,
                title = {
                    Text(
                        "¿Descartar el faltante?",
                        style = FDType.Heading3.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = FDColors.TextPrimary
                    )
                },
                text = {
                    Text(
                        "'${prod.productoNombre}': las ${prod.saldoPendiente} unidades que faltan se marcan como quebradas en el proveedor y dejan de esperarse. La orden se recalcula sola y lo ya recibido no se toca.",
                        style = FDType.Body.copy(fontSize = 12.5.sp),
                        color = FDColors.TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val idProd = prod.productoId; productoParaDescartar =
                            null; onDescartarProducto(idProd)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FDColors.Primary,
                            contentColor = FDColors.PrimaryText
                        ),
                        shape = RoundedCornerShape(s.radiusInput * 0.75f)
                    ) {
                        Text(
                            "Sí, DESCARTAR FALTANTE",
                            style = FDType.Label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { productoParaDescartar = null },
                        shape = RoundedCornerShape(s.radiusInput * 0.75f)
                    ) {
                        Text(
                            "NO, MEJOR NO",
                            style = FDType.Label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            )
        }
    }

    // MODAL EJECUTIVO DE REVISIíN Y CONFIRMACIíN DE PEDIDO
    @Composable
    private fun DialogoRevisionPedido(
        pedido: PedidoProveedor,
        simboloMoneda: String,
        onDismiss: () -> Unit,
        onConfirmarWhatsApp: (telefono: String, guardarEnProveedor: Boolean) -> Unit,
        onMarcarEnviadoDirecto: () -> Unit
    ) {
        val s = recordarMedidaAdaptativa()
        // migrated: s already defined at header from colores
        var telefonoEnvio by remember { mutableStateOf(pedido.proveedorTelefono) }
        var guardarTelefonoEnProveedor by remember { mutableStateOf(true) }
        var errorTelefono by remember { mutableStateOf<String?>(null) }

        // El número cambió respecto a la ficha: ofrecer guardarlo SIEMPRE, no solo cuando no existía.
        val telefonoModificado =
            telefonoEnvio.filter { it.isDigit() } != pedido.proveedorTelefono.filter { it.isDigit() }

        // Panel enterprise: overlay dentro del área de Reposición vive POR ENCIMA de las dos
        // columnas (zIndex 10 en el Box padre), nunca debajo. Ocupa todo el área útil de la
        // pestaña y respira con scroll propio + IME, sin volver a aplicar systemBars (ya lo
        // hace el Scaffold padre).
        Surface(
            color = FDColors.Background,
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(s.padScreenH, s.padScreenV),
                verticalArrangement = Arrangement.spacedBy(s.gapMedium)
            ) {
                // Barra superior del panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(s.btnSmallH)
                            .clip(RoundedCornerShape(s.radiusButton))
                            .background(FDColors.Surface)
                            .border(
                                s.borderWidth,
                                FDColors.Border,
                                RoundedCornerShape(s.radiusButton)
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null,
                            tint = FDColors.TextPrimary,
                            modifier = Modifier.size(s.iconSmall)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "REVISAR Y CONFIRMAR PEDIDO",
                            style = FDType.Label.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = s.textLabel.value.sp,
                                letterSpacing = 1.sp
                            ),
                            color = FDColors.TextTertiary
                        )
                        Text(
                            text = pedido.proveedorNombre,
                            style = FDType.Heading1.copy(fontSize = s.textTitle.value.sp),
                            color = FDColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pedido.totalProductos} productos seleccionados · ${pedido.totalUnidades} unidades",
                            style = FDType.BodySmall.copy(fontSize = s.textBody.value.sp * 0.92f),
                            color = FDColors.TextSecondary
                        )
                    }
                }

                Surface(
                    color = FDColors.Surface,
                    shape = RoundedCornerShape(s.radiusCard),
                    border = BorderStroke(s.borderWidth, FDColors.Border),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(s.padCardLarge),
                        verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                    ) {
                        // Tabla de Desglose con scroll propio (nunca un hueco de 200dp)
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)
                        ) {
                            items(pedido.itemsValidos, key = { it.productoId }) { item ->
                                Surface(
                                    color = FDColors.SurfaceElevated,
                                    shape = RoundedCornerShape(s.radiusInput * 0.75f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.productoNombre,
                                                style = FDType.Body.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = s.textBody.value.sp
                                                ),
                                                color = FDColors.TextPrimary
                                            )
                                            Text(
                                                text = "${item.cantidad} ${item.presentacion} í— $simboloMoneda " + String.format(
                                                    Locale.US,
                                                    "%.2f",
                                                    item.precioCompra
                                                ),
                                                style = FDType.BodySmall.copy(
                                                    fontSize = s.textBody.value.sp * 0.92f
                                                ),
                                                color = FDColors.TextTertiary
                                            )
                                        }

                                        Text(
                                            text = "$simboloMoneda " + String.format(
                                                Locale.US,
                                                "%.2f",
                                                item.subtotal
                                            ),
                                            style = FDType.Heading3.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = s.textBody.value.sp
                                            ),
                                            color = FDColors.TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Sección fija inferior: total + teléfono + acciones (siempre visibles)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                        ) {
                            HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                            // Resumen Total
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "TOTAL ESTIMADO DE ESTA ORDEN:",
                                    style = FDType.Label.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = s.textLabel.value.sp
                                    ),
                                    color = FDColors.TextTertiary
                                )
                                Text(
                                    text = "$simboloMoneda " + String.format(
                                        Locale.US,
                                        "%,.2f",
                                        pedido.totalInversion
                                    ),
                                    style = FDType.Heading1.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = s.textSubtitle.value.sp
                                    ),
                                    color = FDColors.TextPrimary
                                )
                            }

                            // Teléfono para WhatsApp
                            Surface(
                                color = if (telefonoEnvio.isBlank()) FDColors.Primary.copy(alpha = 0.08f) else FDColors.SurfaceElevated,
                                shape = RoundedCornerShape(s.radiusInput),
                                border = BorderStroke(
                                    s.borderWidth,
                                    if (errorTelefono != null) FDColors.Error else FDColors.Border
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(s.padCard),
                                    verticalArrangement = Arrangement.spacedBy(s.gapTiny * 1.0f)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(s.gapTiny * 1.0f)
                                        ) {
                                            Icon(
                                                Icons.Default.Phone,
                                                null,
                                                tint = if (telefonoEnvio.isNotBlank()) FDColors.Success else FDColors.TextTertiary,
                                                modifier = Modifier.size(s.iconSmall)
                                            )
                                            Text(
                                                text = "TELí‰FONO DE WHATSAPP DEL DESTINATARIO:",
                                                style = FDType.Label.copy(
                                                    fontSize = s.textLabel.value.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = FDColors.TextTertiary
                                            )
                                        }
                                        if (pedido.proveedorTelefono.isBlank()) {
                                            Text(
                                                text = "──š ï¸ No registrado previamente",
                                                style = FDType.Label.copy(
                                                    fontSize = s.textLabel.value.sp * 0.9f,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = FDColors.Warning
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = telefonoEnvio,
                                        onValueChange = {
                                            telefonoEnvio = it
                                            errorTelefono = null
                                        },
                                        placeholder = { Text("Número de WhatsApp") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Chat,
                                                null,
                                                tint = FDColors.Success,
                                                modifier = Modifier.size(s.iconSmall)
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = FDColors.Primary,
                                            unfocusedBorderColor = FDColors.Border
                                        ),
                                        shape = RoundedCornerShape(s.radiusInput * 0.75f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(s.inputMinH)
                                    )
                                    if (errorTelefono != null) {
                                        Text(
                                            text = errorTelefono ?: "",
                                            style = FDType.Label.copy(
                                                fontSize = s.textLabel.value.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = FDColors.Error
                                        )
                                    }
                                    if (telefonoModificado && telefonoEnvio.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                guardarTelefonoEnProveedor =
                                                    !guardarTelefonoEnProveedor
                                            }
                                        ) {
                                            Checkbox(
                                                checked = guardarTelefonoEnProveedor,
                                                onCheckedChange = {
                                                    guardarTelefonoEnProveedor = it
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = FDColors.Primary)
                                            )
                                            Text(
                                                text = "Guardar este número en la ficha de ${pedido.proveedorNombre} para futuros pedidos",
                                                style = FDType.Label.copy(fontSize = s.textLabel.value.sp),
                                                color = FDColors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            // Botones de Acción
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(s.gapSmall)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(s.radiusInput),
                                    border = BorderStroke(s.borderWidth, FDColors.Border),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.TextTertiary),
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .height(s.btnMediumH * 0.95f)
                                ) {
                                    Text(
                                        "VOLVER",
                                        style = FDType.Label.copy(fontSize = s.textLabel.value.sp)
                                    )
                                }

                                OutlinedButton(
                                    onClick = onMarcarEnviadoDirecto,
                                    shape = RoundedCornerShape(s.radiusInput),
                                    border = BorderStroke(
                                        s.borderWidth,
                                        FDColors.Success.copy(alpha = 0.6f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FDColors.Success),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(s.btnMediumH * 0.95f)
                                ) {
                                    Text(
                                        "──œ“ MARCAR ENVIADO",
                                        style = FDType.Label.copy(
                                            fontSize = s.textLabel.value.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Button(
                                    onClick = {
                                        val digitos = telefonoEnvio.filter { it.isDigit() }
                                        if (digitos.length < 7) {
                                            errorTelefono =
                                                "──š ï¸ Ingresa un número de teléfono real (mínimo 7-9 dígitos) para enviar por WhatsApp."
                                            return@Button
                                        }
                                        onConfirmarWhatsApp(
                                            telefonoEnvio,
                                            guardarTelefonoEnProveedor
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = FDColors.Primary,
                                        contentColor = FDColors.PrimaryText
                                    ),
                                    shape = RoundedCornerShape(s.radiusInput),
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(s.btnMediumH * 0.95f)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "ENVIAR POR WHATSAPP",
                                        style = FDType.Label.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = s.textLabel.value.sp
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

    private fun generarTextoWhatsApp(
        farmaciaNombre: String,
        usuarioNombre: String,
        simboloMoneda: String,
        pedido: PedidoProveedor
    ): String {
        // Hora del servidor: la fecha y el saludo del mensaje jamás dependen del reloj del celular.
        val ahoraMs = HoraServidor.ahoraMs()
        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ahoraMs))
        val horaActual =
            Calendar.getInstance().apply { timeInMillis = ahoraMs }.get(Calendar.HOUR_OF_DAY)
        val saludoHora = when (horaActual) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }

        val nombreSolicitante = usuarioNombre.trim().split(" ").firstOrNull()?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        } ?: "Administración"

        val sb = StringBuilder()
        sb.append("*¡Hola, $saludoHora!*\n")
        sb.append("Te saluda *$nombreSolicitante* de *$farmaciaNombre*.\n\n")
        sb.append("Por favor, ayúdanos atendiendo el siguiente pedido de compra:\n\n")
        sb.append("*ORDEN DE COMPRA EXCLUSIVA*\n")
        sb.append("*Proveedor / Droguería:* ${pedido.proveedorNombre}\n")
        if (pedido.proveedorRuc.isNotBlank()) {
            sb.append("*RUC:* ${pedido.proveedorRuc}\n")
        }
        sb.append("*Fecha:* $fecha\n\n")

        pedido.itemsValidos.forEach { item ->
            val precioUnitFmt =
                "$simboloMoneda " + String.format(Locale.US, "%.2f", item.precioCompra)
            val subtotalFmt = "$simboloMoneda " + String.format(Locale.US, "%.2f", item.subtotal)
            // Sin categoría real no hay paréntesis vacío: al proveedor se le habla en limpio.
            val categoriaTxt = if (item.categoria.isNotBlank()) " (${item.categoria})" else ""
            sb.append(" —¢ *${item.productoNombre}*$categoriaTxt: *Pedir ${item.cantidad} ${item.presentacion}* a $precioUnitFmt c/u (Subtotal: $subtotalFmt)\n")
        }
        sb.append("\n")

        val totalFmt = try {
            "$simboloMoneda " + String.format(Locale.US, "%,.2f", pedido.totalInversion)
        } catch (e: Exception) {
            "$simboloMoneda ${pedido.totalInversion}"
        }

        sb.append("*Inversión Total Estimada:* $totalFmt (${pedido.totalProductos} productos / ${pedido.totalUnidades} unidades)\n\n")
        sb.append("Quedo atento a la confirmación de stock y tiempo estimado de entrega. ¡Muchas gracias!")

        return sb.toString()
    }

    // TARJETA CONTENEDORA DE PROVEEDOR COMERCIAL (CATÁLOGO IZQUIERDA)
    @Composable
    private fun TarjetaProveedorCatalogo(
        proveedorNombre: String,
        productos: List<PharmProduct>,
        carroProv: Map<String, Int>,
        enCaminoPorProducto: Map<String, com.app.administradorfarmadon.compras.logica.ProductoEnCamino>,
        contribuidoresProv: Map<String, Map<String, Int>>,
        estaColapsado: Boolean,
        simboloMoneda: String,
        onToggleExpandir: () -> Unit,
        onReponerSugeridos: () -> Unit = {},
        onModificarCantidad: (PharmProduct, Int) -> Unit
    ) {
        val s = recordarMedidaAdaptativa()
        val productosEnPedido = carroProv.count { it.value > 0 }
        val subtotalTotalProv = productos.sumOf { (carroProv[it.id] ?: 0) * it.purchasePrice }
        val productosCriticos = productos.count { it.stock <= it.minStock }

        Surface(
            color = FDColors.SurfaceElevated,
            shape = RoundedCornerShape(s.radiusCard * 0.88f),
            border = BorderStroke(s.borderWidth, FDColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // HEADER DEL PROVEEDOR (ESTRUCTURA HOLGADA 100% INMUNE A APLASTAMIENTO)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleExpandir)
                        .background(
                            if (productosEnPedido > 0) FDColors.TextPrimary.copy(alpha = 0.02f)
                            else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Fila Principal: Icono + Nombre + Subtítulo (Izquierda) y Badge Pedido + Chevron (Derecha)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.gapSmall),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(s.iconMedium * 1.3f)
                                    .clip(RoundedCornerShape(s.radiusInput * 0.75f))
                                    .background(FDColors.TextPrimary.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocalShipping,
                                    null,
                                    tint = FDColors.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = proveedorNombre,
                                    style = FDType.Heading3.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = FDColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    text = "${productos.size} productos suministrados",
                                    style = FDType.BodySmall.copy(fontSize = 11.5.sp),
                                    color = FDColors.TextSecondary
                                )
                            }
                        }

                        // Derecha: Chevron Limpio para Expandir / Colapsar
                        Icon(
                            imageVector = if (estaColapsado) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = FDColors.TextTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Fila Secundaria: Si tiene faltantes críticos, mostrar barra de acción dedicada
                    val criticosPendientesProv =
                        productos.count {
                            it.stock <= it.minStock &&
                                    (carroProv[it.id] ?: 0) == 0 &&
                                    (enCaminoPorProducto[it.id]?.unidades ?: 0) == 0
                        }
                    if (productosCriticos > 0) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(s.radiusInput * 0.55f))
                                .background(FDColors.TextPrimary.copy(alpha = 0.03f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(s.gapTiny * 1.0f)
                            ) {
                                Icon(
                                    imageVector = if (criticosPendientesProv > 0) Icons.Default.Bolt else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (criticosPendientesProv > 0) FDColors.Warning else FDColors.Success,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = when {
                                        criticosPendientesProv == productosCriticos -> "$productosCriticos por reponer"
                                        criticosPendientesProv > 0 -> "$criticosPendientesProv pendientes"
                                        else -> "Todo cubierto"
                                    },
                                    style = FDType.Label.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = if (criticosPendientesProv > 0) FDColors.Warning else FDColors.Success
                                )
                            }

                            if (criticosPendientesProv > 0) {
                                OutlinedButton(
                                    onClick = onReponerSugeridos,
                                    border = BorderStroke(
                                        1.dp,
                                        FDColors.Primary.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(s.radiusInput * 0.55f),
                                    contentPadding = PaddingValues(
                                        horizontal = 10.dp,
                                        vertical = 3.dp
                                    ),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Bolt,
                                        null,
                                        tint = FDColors.Primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = "Pedir los $criticosPendientesProv faltantes",
                                        style = FDType.Label.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = FDColors.Primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Cuerpo: Lista de productos dentro de la tarjeta
                if (!estaColapsado) {
                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(s.gapSmall * 0.8f)
                    ) {
                        productos.forEach { prod ->
                            val cantPedir = carroProv[prod.id] ?: 0
                            val subtotal = cantPedir * prod.purchasePrice
                            val tienePedido = cantPedir > 0
                            val empaqueStr = prod.empaque.ifBlank { "Und" }

                            val stockActual = prod.stock
                            val stockMinimo = prod.minStock
                            // Faltante NETO: lo que ya viene en camino no cuenta como faltante
                            val enCaminoUndFila = enCaminoPorProducto[prod.id]?.unidades ?: 0
                            val faltanteBase =
                                if (stockMinimo > 0) (stockMinimo - stockActual).coerceAtLeast(0) else if (stockActual <= 0) 1 else 0
                            val faltante = (faltanteBase - enCaminoUndFila).coerceAtLeast(0)
                            val esAgotado = stockActual <= 0
                            val esCritico =
                                stockActual > 0 && stockMinimo > 0 && stockActual <= stockMinimo

                            Surface(
                                color = FDColors.Surface,
                                shape = RoundedCornerShape(s.radiusInput),
                                border = BorderStroke(
                                    0.8.dp,
                                    if (esAgotado) FDColors.Warning.copy(alpha = 0.35f)
                                    else if (esCritico) FDColors.Warning
                                        .copy(alpha = 0.3f)
                                    else FDColors.Border.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(s.gapSmall)
                                ) {
                                    // FILA 1: DATOS DEL MEDICAMENTO (IZQUIERDA) Y ESTADO DEL ESTANTE (DERECHA)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 12.dp)
                                        ) {
                                            Text(
                                                text = prod.name,
                                                style = FDType.Heading3.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.5.sp
                                                ),
                                                color = FDColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            val detalleSub = listOfNotNull(
                                                prod.laboratory.takeIf { it.isNotBlank() && it != "Genérico" },
                                                prod.category.takeIf { it.isNotBlank() }
                                            ).joinToString(" —¢ ")

                                            if (detalleSub.isNotBlank()) {
                                                Text(
                                                    text = detalleSub,
                                                    style = FDType.BodySmall.copy(
                                                        fontSize = 11.5.sp
                                                    ),
                                                    color = FDColors.TextTertiary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Surface(
                                                color = when {
                                                    esAgotado -> FDColors.Warning.copy(alpha = 0.12f)
                                                    esCritico -> FDColors.Warning.copy(alpha = 0.12f)

                                                    else -> FDColors.TextPrimary.copy(alpha = 0.05f)
                                                },
                                                shape = RoundedCornerShape(s.radiusChip),
                                                border = BorderStroke(
                                                    0.8.dp,
                                                    when {
                                                        esAgotado -> FDColors.Warning.copy(alpha = 0.4f)
                                                        esCritico -> FDColors.Warning.copy(alpha = 0.4f)

                                                        else -> FDColors.Border
                                                    }
                                                )
                                            ) {
                                                Text(
                                                    text = when {
                                                        esAgotado -> "──— Agotado (0 / Mín $stockMinimo)"
                                                        esCritico -> "──— Quedan $stockActual (Mínimo $stockMinimo)"
                                                        else -> "──— En estante: $stockActual $empaqueStr"
                                                    },
                                                    style = FDType.Label.copy(
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = when {
                                                        esAgotado -> FDColors.Warning
                                                        esCritico -> FDColors.Warning

                                                        else -> FDColors.TextSecondary
                                                    },
                                                    modifier = Modifier.padding(
                                                        horizontal = 7.dp,
                                                        vertical = 2.5.dp
                                                    )
                                                )
                                            }

                                            if (prod.purchasePrice > 0.0) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = "$simboloMoneda " + String.format(
                                                        Locale.US,
                                                        "%.2f",
                                                        prod.purchasePrice
                                                    ) + " / $empaqueStr",
                                                    style = FDType.Label.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = FDColors.TextTertiary
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.4f))

                                    // FILA 2: RESUMEN DE PEDIDO EN CURSO (SI YA SE ELIGIí CANTIDAD)
                                    if (tienePedido) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(s.radiusInput * 0.55f))
                                                .background(FDColors.TextPrimary.copy(alpha = 0.03f))
                                                .padding(horizontal = s.sm, vertical = s.xs),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "$cantPedir $empaqueStr",
                                                    style = FDType.Label.copy(
                                                        fontSize = s.textBody.value.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = FDColors.TextPrimary
                                                )
                                                Text(
                                                    text = "Subtotal: $simboloMoneda " + String.format(
                                                        Locale.US,
                                                        "%,.2f",
                                                        subtotal
                                                    ),
                                                    style = FDType.Label.copy(
                                                        fontSize = s.textBody.value.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = FDColors.Primary
                                                )
                                            }

                                            // Quién aportó cada unidad (carrito compartido transparente).
                                            val contrib = contribuidoresProv[prod.id] ?: emptyMap()
                                            if (contrib.isNotEmpty()) {
                                                Text(
                                                    text = contrib.entries
                                                        .sortedByDescending { it.value }
                                                        .joinToString(" · ") { (nombre, cant) ->
                                                            "$nombre: $cant"
                                                        },
                                                    style = FDType.BodySmall.copy(
                                                        fontSize = s.textLabel.value.sp * 0.88f,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = FDColors.TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    // FILA 3: FILA DEDICADA EXCLUSIVAMENTE A BOTONES Y CONTROLES
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Izquierda: Botón Atajo o Estado
                                        if (faltante > 0 && !tienePedido) {
                                            Surface(
                                                color = FDColors.SurfaceElevated,
                                                shape = RoundedCornerShape(s.radiusInput * 0.55f),
                                                border = BorderStroke(
                                                    s.borderWidth * 0.8f,
                                                    FDColors.Border
                                                ),
                                                modifier = Modifier.clickable {
                                                    onModificarCantidad(
                                                        prod,
                                                        faltante
                                                    )
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(
                                                        horizontal = 10.dp,
                                                        vertical = 6.dp
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Bolt,
                                                        null,
                                                        tint = FDColors.TextPrimary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "Pedir las $faltante $empaqueStr que faltan",
                                                        style = FDType.Label.copy(
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        ),
                                                        color = FDColors.TextPrimary
                                                    )
                                                }
                                            }
                                        } else if (!tienePedido && enCaminoUndFila > 0) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.LocalShipping,
                                                    null,
                                                    tint = FDColors.Success,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "$enCaminoUndFila en camino",
                                                    style = FDType.Label.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = FDColors.Success
                                                )
                                            }
                                        } else if (!tienePedido) {
                                            Text(
                                                text = "──œ“ Cubierto",
                                                style = FDType.BodySmall.copy(
                                                    fontSize = 11.5.sp
                                                ),
                                                color = FDColors.TextTertiary
                                            )
                                        } else {
                                            Text(
                                                text = "Ajustar unidades:",
                                                style = FDType.Label.copy(fontSize = 11.sp),
                                                color = FDColors.TextTertiary
                                            )
                                        }

                                        // Derecha: Botones Táctiles Espaciosos [ - ] [ N ] [ + ]
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(s.gapTiny * 1.0f)
                                        ) {
                                            Surface(
                                                color = FDColors.SurfaceElevated,
                                                shape = RoundedCornerShape(s.radiusInput * 0.55f),
                                                border = BorderStroke(
                                                    s.borderWidth * 0.8f,
                                                    FDColors.Border
                                                ),
                                                modifier = Modifier
                                                    .size(s.btnMediumH)
                                                    .clickable { onModificarCantidad(prod, -1) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Remove,
                                                        contentDescription = "Menos",
                                                        tint = if (cantPedir > 0) FDColors.TextPrimary else FDColors.TextTertiary,
                                                        modifier = Modifier.size(s.iconSmall)
                                                    )
                                                }
                                            }

                                            Surface(
                                                color = FDColors.SurfaceElevated,
                                                shape = RoundedCornerShape(s.radiusInput * 0.55f),
                                                border = BorderStroke(
                                                    s.borderWidth,
                                                    FDColors.Border
                                                ),
                                                modifier = Modifier
                                                    .widthIn(min = s.btnMediumH)
                                                    .height(s.btnMediumH)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = s.xs)
                                                ) {
                                                    Text(
                                                        text = "$cantPedir",
                                                        style = FDType.Label.copy(
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = s.textInput.value.sp
                                                        ),
                                                        color = FDColors.TextPrimary
                                                    )
                                                }
                                            }

                                            Surface(
                                                color = FDColors.SurfaceElevated,
                                                shape = RoundedCornerShape(s.radiusInput * 0.55f),
                                                border = BorderStroke(
                                                    s.borderWidth * 0.8f,
                                                    FDColors.Border
                                                ),
                                                modifier = Modifier
                                                    .size(s.btnMediumH)
                                                    .clickable { onModificarCantidad(prod, 1) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Más",
                                                        tint = FDColors.TextPrimary,
                                                        modifier = Modifier.size(s.iconSmall)
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
            }
        }
    }

    @Composable
    fun MetricItemReposicion(label: String, value: String, color: Color) {
        val s = recordarMedidaAdaptativa()
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = label,
                style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = FDColors.TextTertiary
            )
            Text(
                text = value,
                style = FDType.Numeric.copy(fontSize = 15.sp, fontWeight = FontWeight.Black),
                color = color
            )
        }
    }

