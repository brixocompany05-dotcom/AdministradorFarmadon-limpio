package com.app.administradorfarmadon.inventario.ajustesinventario.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.inventario.ajustesinventario.logica.AjusteInventarioViewModel
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.compartido.modelo.stockDisponibleFisico
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes.ContenidoAnulacionInline
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes.ContenidoCuarentenaInline
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes.ContenidoDevolucionInline
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes.ContenidoFicha
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes.ContenidoHistorialLote
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper

/**
 * PUERTA ÚNICA de lotes y movimientos. Todo lo que le pasa al stock o al lote vive aquí:
 *  1 · ENTRAR (compra → Compras; o no compra: muestra/regalo/sobrante/primera carga)
 *  2 · SALIR  (devolver al proveedor con documento; o daño/pérdida/error de conteo)
 *  3 · ESTADO (prioridad FEFO, cuarentena, anular ingreso) del lote seleccionado
 */
@Composable
fun WorkspaceRegistrarEntrada(
    producto: MoldeProductos,
    ajusteInventarioViewModel: AjusteInventarioViewModel,
    loteInicial: LoteProducto? = null,
    isPrivileged: Boolean = false,
    movimientos: List<com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario> = emptyList(),
    onVolver: () -> Unit,
    onDefinirPrioridad: (String?) -> Unit = {},
    onCambiarBloqueo: (LoteProducto, Boolean, Double, String, (Result<Unit>) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onRegistrarDevolucion: (LoteProducto, Double, String, String, String, String, (Result<Unit>) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    onRegistrarCanje: (LoteProducto, Double, String, String, String, String, (Result<Unit>) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    onAnularIngreso: (LoteProducto, String, (Result<Unit>) -> Unit) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    val estado by ajusteInventarioViewModel.state.collectAsState()

    var grupo by remember { mutableStateOf("ENTRADA") } 
    var loteSeleccionado by remember { mutableStateOf<LoteProducto?>(loteInicial) }
    var vistaLote by remember { mutableStateOf("FICHA") }
    var enDevolucion by remember { mutableStateOf(false) }

    val onSeleccionarLoteInterno: (LoteProducto?) -> Unit = { loteSeleccionado = it; vistaLote = "FICHA" }

    LaunchedEffect(loteInicial) {
        if (loteInicial != null) {
            grupo = "ESTADO"
            loteSeleccionado = loteInicial
            vistaLote = "FICHA"
        }
    }

    LaunchedEffect(estado.mostrandoDialogo) {
        if (!estado.mostrandoDialogo && estado.producto != null) {
            ajusteInventarioViewModel.reiniciar()
        }
    }

    fun volverUnNivel() {
        if (estado.procesando || estado.exito) return
        when {
            estado.mostrandoDialogo -> ajusteInventarioViewModel.cerrar()
            grupo == "SALIDA" && enDevolucion -> enDevolucion = false
            grupo == "ESTADO" && vistaLote != "FICHA" -> vistaLote = "FICHA"
            grupo == "ESTADO" && loteSeleccionado != null -> loteSeleccionado = null
            else -> onVolver()
        }
    }

    val lotesConStock = producto.lotes.values.filter { it.cantidad > 0 }.sortedByDescending { it.cantidad }
    val pasoActual = when {
        estado.mostrandoDialogo -> if (grupo == "ENTRADA") "1 · Algo entró" else "2 · Algo sale"
        grupo == "ENTRADA" -> "1 · Registro de Entrada"
        grupo == "SALIDA" && enDevolucion -> "2 · Devolver al proveedor"
        grupo == "SALIDA" -> "2 · Registro de Salida"
        else -> "3 · Estado del lote"
    }

    Column(modifier.fillMaxSize().background(FDColors.Background)) {
        // CABECERA (Enterprise Standard)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = s.padScreenH, vertical = s.padCard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs)) {
                IconButton(onClick = { volverUnNivel() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FDColors.TextPrimary)
                }
                Column {
                    Text(
                        pasoActual,
                        style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = FDColors.TextTertiary
                    )
                    Text(
                        producto.nombre,
                        style = FDType.Heading3.copy(fontSize = s.textSubtitle.value.sp, fontWeight = FontWeight.Bold),
                        color = FDColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                color = FDColors.Primary.copy(alpha = 0.08f),
                shape = FDShapes.Full,
                border = BorderStroke(1.dp, FDColors.Primary.copy(alpha = 0.25f))
            ) {
                Text(
                    "Stock actual: ${producto.stockDisponibleFisico.toInt()} und",
                    style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                    color = FDColors.Primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        
        HorizontalDivider(color = FDColors.Border.copy(alpha = 0.3f), thickness = 0.5.dp)

        Row(Modifier.fillMaxSize()) {
            // SIDEBAR SaaS MINIMALISTA (Background-Integrated)
            Surface(
                modifier = Modifier.width(196.dp).fillMaxHeight(),
                color = FDColors.Background, // Mismo color que la cabecera para integración total
                border = BorderStroke(0.dp, Color.Transparent)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ItemSidebarSaaS("1 · Entrar", Icons.Default.LocalShipping, grupo == "ENTRADA") {
                        if (!estado.procesando && !estado.exito) { grupo = "ENTRADA"; enDevolucion = false }
                    }
                    ItemSidebarSaaS("2 · Salir", Icons.Default.DeleteOutline, grupo == "SALIDA") {
                        if (!estado.procesando && !estado.exito) { grupo = "SALIDA"; enDevolucion = false }
                    }
                    ItemSidebarSaaS("3 · Estado", Icons.Default.Lock, grupo == "ESTADO") {
                        if (!estado.procesando && !estado.exito) { grupo = "ESTADO"; vistaLote = "FICHA" }
                    }
                }
            }

            // ESPACIO DE TRABAJO (SaaS Workspace - Sin bordes internos redundantes)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                color = FDColors.Surface,
                shape = RoundedCornerShape(topStart = 16.dp), // Solo redondeo en la esquina interna
                border = BorderStroke(0.dp, Color.Transparent)
            ) {
                Box(Modifier.fillMaxSize()) {
                    when {
                        estado.mostrandoDialogo -> {
                            DialogoAjusteInventario(
                                producto = producto,
                                procesando = estado.procesando,
                                mensajeError = estado.mensajeError,
                                exito = estado.exito,
                                onReintentar = { ajusteInventarioViewModel.reintentar() },
                                direccionInicial = if (grupo == "ENTRADA") "ENTRADA" else "SALIDA",
                                onDismiss = { ajusteInventarioViewModel.cerrar() },
                                onRegistrarEntrada = { lote, venc, cant, tipo, motivo ->
                                    ajusteInventarioViewModel.registrarEntrada(lote, venc, cant, tipo, motivo)
                                },
                                onRegistrarSalida = { lote, cant, tipo, motivo ->
                                    ajusteInventarioViewModel.registrarSalida(lote, cant, tipo, motivo)
                                }
                            )
                        }

                        grupo == "ENTRADA" -> ContenidoEntradaPremium(
                            producto = producto,
                            ajusteInventarioViewModel = ajusteInventarioViewModel,
                            loteInicial = loteSeleccionado,
                            exito = estado.exito,
                            procesando = estado.procesando,
                            mensajeError = estado.mensajeError,
                            onReintentar = { ajusteInventarioViewModel.reintentar() },
                            s = s
                        )

                        grupo == "SALIDA" && enDevolucion -> {
                            val lote = loteSeleccionado
                            if (lote == null) {
                                EmptyStatePanel(mensaje = "Este producto no tiene lotes con stock para devolver.", s = s)
                            } else {
                                ContenidoDevolucionInline(
                                    product = producto,
                                    lote = lote,
                                    onVolver = { loteSeleccionado = null },
                                    onDismissDrawer = { loteSeleccionado = null; enDevolucion = false },
                                    onRegistrarDevolucion = onRegistrarDevolucion,
                                    onRegistrarCanje = onRegistrarCanje
                                )
                            }
                        }

                        grupo == "SALIDA" -> ContenidoSalidaPremium(
                            producto = producto,
                            ajusteInventarioViewModel = ajusteInventarioViewModel,
                            loteInicial = loteSeleccionado,
                            exito = estado.exito,
                            procesando = estado.procesando,
                            mensajeError = estado.mensajeError,
                            onReintentar = { ajusteInventarioViewModel.reintentar() },
                            onIrADevolucion = { lote ->
                                loteSeleccionado = lote ?: loteSeleccionado ?: lotesConStock.firstOrNull()
                                enDevolucion = true
                            },
                            s = s
                        )

                        grupo == "ESTADO" -> ContenidoEstadoPremium(
                            producto = producto,
                            loteSeleccionado = loteSeleccionado,
                            onSeleccionarLote = onSeleccionarLoteInterno,
                            vistaLote = vistaLote,
                            onCambiarVistaLote = { vistaLote = it },
                            isPrivileged = isPrivileged,
                            movimientos = movimientos,
                            onDefinirPrioridad = onDefinirPrioridad,
                            onCambiarBloqueo = onCambiarBloqueo,
                            onAnularIngreso = onAnularIngreso,
                            s = s
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemSidebarSaaS(
    titulo: String,
    icono: ImageVector,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (seleccionado) FDColors.Primary.copy(alpha = 0.08f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icono,
                null,
                tint = if (seleccionado) FDColors.Primary else FDColors.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = titulo,
                modifier = Modifier.weight(1f),
                style = FDType.Label.copy(
                    fontSize = 12.sp,
                    fontWeight = if (seleccionado) FontWeight.Black else FontWeight.SemiBold
                ),
                color = if (seleccionado) FDColors.Primary else FDColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ContenidoEstadoPremium(
    producto: MoldeProductos,
    loteSeleccionado: LoteProducto?,
    onSeleccionarLote: (LoteProducto?) -> Unit,
    vistaLote: String,
    onCambiarVistaLote: (String) -> Unit,
    isPrivileged: Boolean,
    movimientos: List<com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario>,
    onDefinirPrioridad: (String?) -> Unit,
    onCambiarBloqueo: (LoteProducto, Boolean, Double, String, (Result<Unit>) -> Unit) -> Unit,
    onAnularIngreso: (LoteProducto, String, (Result<Unit>) -> Unit) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    val lotesConStock = remember(producto.lotes, producto.lotePrioritarioId, producto.fefoAutomatico) {
        ProductDetailMapper.ordenarLotesParaConsumo(
            producto.lotes.values.filter { it.cantidad > 0 },
            if (producto.fefoAutomatico) "" else producto.lotePrioritarioId
        )
    }

    // Selección automática si solo hay uno
    LaunchedEffect(lotesConStock) {
        if (lotesConStock.size == 1 && loteSeleccionado == null) {
            onSeleccionarLote(lotesConStock.first())
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val esAncho = maxWidth >= 720.dp
        if (esAncho) {
            Row(Modifier.fillMaxSize()) {
                // MASTER: Lista de Lotes
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .padding(s.padScreenH),
                    verticalArrangement = Arrangement.spacedBy(s.gapMedium)
                ) {
                    Text("LOTES CON STOCK", style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp), color = FDColors.TextTertiary)
                    Text(
                        "Historial, cuarentena, prioridad de consumo o anulación.",
                        style = FDType.Caption.copy(fontSize = 10.5.sp),
                        color = FDColors.TextTertiary
                    )
                    if (lotesConStock.isEmpty()) {
                        Text("No hay lotes disponibles", style = FDType.BodySmall, color = FDColors.TextTertiary)
                    } else {
                        TablaLotesAnimada(
                            lotes = lotesConStock,
                            seleccionadoNumero = loteSeleccionado?.numero,
                            onSeleccionar = { numero -> lotesConStock.firstOrNull { it.numero == numero }?.let(onSeleccionarLote) },
                            s = s,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                VerticalDivider(color = FDColors.Border.copy(alpha = 0.4f), thickness = s.separatorH)
                
                // DETAIL: Panel de Gestión
                Box(Modifier.weight(0.58f).fillMaxHeight()) {
                    if (loteSeleccionado != null) {
                        DetalleDeEstadoLote(
                            producto = producto,
                            lote = loteSeleccionado,
                            vistaActual = vistaLote,
                            onCambiarVista = onCambiarVistaLote,
                            onCerrar = { /* Master-Detail: se queda abierto el último */ },
                            isPrivileged = isPrivileged,
                            movimientos = movimientos,
                            onDefinirPrioridad = onDefinirPrioridad,
                            onCambiarBloqueo = onCambiarBloqueo,
                            onAnularIngreso = onAnularIngreso,
                            s = s
                        )
                    } else {
                        EmptyStatePanel(
                            mensaje = "Selecciona un lote a la izquierda para gestionar su estado.",
                            s = s
                        )
                    }
                }
            }
        } else {
            // MODO ESTRECHO: Secuencial
            if (loteSeleccionado == null) {
                SeleccionarLote(
                    lotes = lotesConStock,
                    seleccionadoNumero = null,
                    onSeleccionar = onSeleccionarLote,
                    s = s
                )
            } else {
                DetalleDeEstadoLote(
                    producto = producto,
                    lote = loteSeleccionado,
                    vistaActual = vistaLote,
                    onCambiarVista = onCambiarVistaLote,
                    onCerrar = { onSeleccionarLote(null) }, 
                    isPrivileged = isPrivileged,
                    movimientos = movimientos,
                    onDefinirPrioridad = onDefinirPrioridad,
                    onCambiarBloqueo = onCambiarBloqueo,
                    onAnularIngreso = onAnularIngreso,
                    s = s
                )
            }
        }
    }
}

@Composable
private fun DetalleDeEstadoLote(
    producto: MoldeProductos,
    lote: LoteProducto,
    vistaActual: String,
    onCambiarVista: (String) -> Unit,
    onCerrar: () -> Unit,
    isPrivileged: Boolean,
    movimientos: List<com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario>,
    onDefinirPrioridad: (String?) -> Unit,
    onCambiarBloqueo: (LoteProducto, Boolean, Double, String, (Result<Unit>) -> Unit) -> Unit,
    onAnularIngreso: (LoteProducto, String, (Result<Unit>) -> Unit) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    when (vistaActual) {
        "HISTORIAL" -> ContenidoHistorialLote(
            lote = lote,
            movimientos = movimientos,
            onVolver = { onCambiarVista("FICHA") },
            onDismissDrawer = onCerrar
        )
        "CUARENTENA" -> ContenidoCuarentenaInline(
            product = producto,
            lote = lote,
            onVolver = { onCambiarVista("FICHA") },
            onDismissDrawer = onCerrar,
            onCambiarBloqueo = onCambiarBloqueo
        )
        "ANULACION" -> ContenidoAnulacionInline(
            product = producto,
            lote = lote,
            onVolver = { onCambiarVista("FICHA") },
            onDismissDrawer = onCerrar,
            onAnularLote = onAnularIngreso
        )
        else -> {
            val diasVenc = ProductDetailMapper.diasHastaVencer(lote.vencimiento)
            val colorVenc = ProductDetailMapper.colorVencimiento(diasVenc)
            ContenidoFicha(
                product = producto,
                lote = lote,
                fefoAutomatico = producto.fefoAutomatico,
                esEsteElLotePrioritario = producto.lotePrioritarioId.isNotBlank() &&
                    (producto.lotePrioritarioId.equals(lote.loteId, true) || producto.lotePrioritarioId.equals(lote.numero, true)),
                onDefinirPrioridad = { marcar ->
                    onDefinirPrioridad(if (marcar) lote.loteId.ifBlank { lote.numero } else null)
                },
                isPrivileged = isPrivileged,
                onDismiss = onCerrar,
                onAbrirCuarentena = { onCambiarVista("CUARENTENA") },
                onAbrirHistorial = { onCambiarVista("HISTORIAL") },
                onAbrirAnulacion = { onCambiarVista("ANULACION") },
                diasVencimiento = diasVenc,
                colorVencimiento = colorVenc,
                estaEnCuarentena = lote.cantidadBloqueada > 0,
                esRefrigerado = producto.temperaturaAlmacenamiento == "REFRIGERACION",
                esControlado = producto.clasificacionControl.uppercase() in listOf("PSICOTROPICO", "CONTROLADO", "ESTUPEFACIENTE") || producto.requiereReceta
            )
        }
    }
}

@Composable
private fun EmptyStatePanel(mensaje: String, s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa) {
    Box(Modifier.fillMaxSize().padding(s.padScreenH), contentAlignment = Alignment.Center) {
        Text(mensaje, style = FDType.BodySmall, color = FDColors.TextTertiary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ContenidoEntradaPremium(
    producto: MoldeProductos,
    ajusteInventarioViewModel: AjusteInventarioViewModel,
    loteInicial: LoteProducto?,
    exito: Boolean,
    procesando: Boolean,
    mensajeError: String?,
    onReintentar: () -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    // Formulario de entrada sin compra integrado totalmente en el Workspace.
    // Usamos direccionInicial = "ENTRADA" porque ya estamos en la pestaña de entrada.
    Box(Modifier.fillMaxSize()) {
        DialogoAjusteInventario(
            producto = producto,
            direccionInicial = "ENTRADA",
            loteEntradaInicial = loteInicial,
            exito = exito,
            procesando = procesando,
            mensajeError = mensajeError,
            onReintentar = onReintentar,
            onDismiss = { /* In-place workspace */ },
            onRegistrarEntrada = { lote, venc, cant, tipo, motivo ->
                ajusteInventarioViewModel.registrarEntrada(lote, venc, cant, tipo, motivo)
            },
            onRegistrarSalida = { lote, cant, tipo, motivo ->
                ajusteInventarioViewModel.registrarSalida(lote, cant, tipo, motivo)
            }
        )
    }
}

@Composable
private fun ContenidoSalidaPremium(
    producto: MoldeProductos,
    ajusteInventarioViewModel: AjusteInventarioViewModel,
    loteInicial: LoteProducto?,
    exito: Boolean,
    procesando: Boolean,
    mensajeError: String?,
    onReintentar: () -> Unit,
    onIrADevolucion: (LoteProducto?) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    // Formulario de salida sin documento (merma/pérdida/error) integrado.
    // Si elige "Devolución", se le redirige al flujo formal.
    Box(Modifier.fillMaxSize()) {
        DialogoAjusteInventario(
            producto = producto,
            direccionInicial = "SALIDA",
            loteSalidaInicial = loteInicial,
            exito = exito,
            procesando = procesando,
            mensajeError = mensajeError,
            onReintentar = onReintentar,
            onDismiss = { /* In-place workspace */ },
            onRegistrarEntrada = { lote, venc, cant, tipo, motivo ->
                ajusteInventarioViewModel.registrarEntrada(lote, venc, cant, tipo, motivo)
            },
            onRegistrarSalida = { lote, cant, tipo, motivo ->
                ajusteInventarioViewModel.registrarSalida(lote, cant, tipo, motivo)
            },
            onMotivoEspecialClick = { tipo, lote ->
                if (tipo == "DEVOLUCION") onIrADevolucion(lote)
            }
        )
    }
}

@Composable
private fun SeleccionarLote(
    lotes: List<LoteProducto>,
    seleccionadoNumero: String?,
    onSeleccionar: (LoteProducto) -> Unit,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(s.padScreenH),
        verticalArrangement = Arrangement.spacedBy(s.gapMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text("3 · Elige el lote para gestionar", style = FDType.Heading3.copy(fontSize = 17.sp, fontWeight = FontWeight.Black), color = FDColors.TextPrimary)
        Text("Toca un lote con stock para cambiar su prioridad o estado.", style = FDType.BodySmall.copy(fontSize = 13.sp), color = FDColors.TextSecondary)

        if (lotes.isEmpty()) {
            Surface(
                color = FDColors.Warning.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FDColors.Warning.copy(alpha = 0.3f))
            ) {
                Text(
                    "Este producto no tiene lotes con stock disponible en este momento.",
                    modifier = Modifier.padding(24.dp),
                    style = FDType.Body.copy(color = FDColors.Warning, fontWeight = FontWeight.Bold),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            TablaLotesAnimada(
                lotes = lotes,
                seleccionadoNumero = seleccionadoNumero,
                onSeleccionar = { numero -> lotes.firstOrNull { it.numero.equals(numero, true) }?.let(onSeleccionar) },
                s = s,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.weight(1.2f))
    }
}
