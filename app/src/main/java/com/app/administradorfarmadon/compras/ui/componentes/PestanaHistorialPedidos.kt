package com.app.administradorfarmadon.compras.ui.componentes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.compartido.logica.HoraServidor
import com.app.administradorfarmadon.compras.datos.PedidoCompra
import com.app.administradorfarmadon.compras.logica.MaquinaEstadosPedido
import com.app.administradorfarmadon.compras.recepcion.logica.RecepcionMercaderiaEstado
import com.app.administradorfarmadon.compras.ui.componentes.reposicion.ReposicionDetallePedidoRealizado
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pestaña HISTORIAL DE PEDIDOS:
 * Espacio de trabajo enterprise dedicado exclusivamente a consultar, auditar y recibir
 * órdenes de compra emitidas a los proveedores. No se mezcla con la reposición ni el catálogo.
 * Permite filtrar por fechas (Hoy, Esta Semana, Este Mes, Fecha puntual) y por estado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PestanaHistorialPedidos(
    pedidosGuardados: List<PedidoCompra>,
    simboloMoneda: String,
    s: MedidaAdaptativa,
    pedidoIdInicial: String? = null,
    onRecibirMercaderia: (PedidoCompra) -> Unit,
    onCerrarConAjuste: (PedidoCompra) -> Unit,
    onDescartarProducto: (String, String) -> Unit,
    onCancelarPedido: (String) -> Unit,
    anexandoFactura: Boolean = false,
    onAnexarFactura: (String, String, Double, String, String, String) -> Unit = { _, _, _, _, _, _ -> }
) {
    var pedidoSeleccionadoId by rememberSaveable { mutableStateOf(pedidoIdInicial) }
    var busquedaQuery by rememberSaveable { mutableStateOf("") }
    var filtroEstado by rememberSaveable { mutableStateOf("TODOS") } // "TODOS", "PARCIALES", "RECIBIDOS", "CON_AJUSTE", "CANCELADOS"
    var filtroFechaRapido by rememberSaveable { mutableStateOf("TODAS") } // "TODAS", "HOY", "SEMANA", "MES", "MES_PUNTUAL"
    var mesSeleccionado by rememberSaveable { mutableStateOf<Int?>(null) } // 0..11
    var anioSeleccionado by rememberSaveable { mutableStateOf<Int?>(null) } // ej. 2026
    var mostrarDialogoMes by remember { mutableStateOf(false) }
    var mostrarMenuEstado by remember { mutableStateOf(false) }
    var mostrarMenuFecha by remember { mutableStateOf(false) }

    // Base del historial: órdenes resueltas (recibidas, con ajuste, canceladas) MÁS parciales,
    // que aparecen como pendientes por completar. La parcial sigue viva en En Camino para
    // recibir; aquí deja rastro auditable de lo ya llegado.
    // Se ordenan de la actividad más reciente a la más antigua (según recepción o emisión real).
    val pedidosHistorial = remember(pedidosGuardados) {
        pedidosGuardados
            .filter { MaquinaEstadosPedido.perteneceAHistorial(it.estado) }
            .sortedByDescending { fechaReferenciaMsPedido(it) }
    }

    // El detalle seleccionado se resuelve desde la lista del historial
    val pedidoSeleccionado = remember(pedidosHistorial, pedidoSeleccionadoId) {
        pedidoSeleccionadoId?.let { id -> pedidosHistorial.firstOrNull { it.id == id } }
    }

    // Botón atrás del sistema: si hay un pedido seleccionado, lo deselecciona
    BackHandler(enabled = pedidoSeleccionadoId != null) {
        pedidoSeleccionadoId = null
    }

    // Auto-cierre post-mutación: si la orden seleccionada fue eliminada o mutó, se deselecciona sola.
    LaunchedEffect(pedidosHistorial) {
        if (pedidoSeleccionadoId != null && pedidoSeleccionado == null) {
            pedidoSeleccionadoId = null
        }
    }

    // Diálogo Selector de Mes y Año (reemplaza el calendario de días por requerimiento de negocio)
    if (mostrarDialogoMes) {
        val zonaLima = TimeZone.getTimeZone("America/Lima")
        val calAhora = Calendar.getInstance(zonaLima).apply { timeInMillis = HoraServidor.ahoraMs() }
        DialogoSelectorMes(
            anioInicial = anioSeleccionado ?: calAhora.get(Calendar.YEAR),
            mesInicial = mesSeleccionado ?: calAhora.get(Calendar.MONTH),
            onMesSeleccionado = { anio, mes ->
                anioSeleccionado = anio
                mesSeleccionado = mes
                filtroFechaRapido = "MES_PUNTUAL"
                mostrarDialogoMes = false
            },
            onDismissRequest = { mostrarDialogoMes = false }
        )
    }

    // ── FILTRADO RIGUROSO POR FECHA, ESTADO Y BÚSQUEDA ──
    val pedidosFiltrados = remember(
        pedidosHistorial,
        busquedaQuery,
        filtroEstado,
        filtroFechaRapido,
        mesSeleccionado,
        anioSeleccionado
    ) {
        val zonaLima = TimeZone.getTimeZone("America/Lima")
        val ahoraMs = HoraServidor.ahoraMs()

        val calHoyInicio = Calendar.getInstance(zonaLima).apply {
            timeInMillis = ahoraMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calHoyFin = Calendar.getInstance(zonaLima).apply {
            timeInMillis = ahoraMs
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val calSemana = Calendar.getInstance(zonaLima).apply {
            timeInMillis = ahoraMs
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calAhora = Calendar.getInstance(zonaLima).apply { timeInMillis = ahoraMs }
        val anioActual = calAhora.get(Calendar.YEAR)
        val mesActual = calAhora.get(Calendar.MONTH)

        pedidosHistorial.filter { pedido ->
            // 1. Filtro de Estado
            val coincideEstado = when (filtroEstado) {
                "PARCIALES" -> pedido.estado == "ENTREGA_PARCIAL"
                "RECIBIDOS" -> pedido.estado == "RECIBIDO"
                "CON_AJUSTE" -> pedido.estado == "COMPLETADA_AJUSTE"
                "CANCELADOS" -> pedido.estado == "CANCELADO"
                else -> true
            }
            if (!coincideEstado) return@filter false

            // 2. Filtro de Fecha robusto (resuelve tanto recepción física como emisión contable)
            val timestampsPedido = extraerTimestampsPedido(pedido)
            val coincideFecha = when (filtroFechaRapido) {
                "HOY" -> {
                    if (timestampsPedido.isEmpty()) true
                    else timestampsPedido.any { it in calHoyInicio.timeInMillis..calHoyFin.timeInMillis }
                }
                "SEMANA" -> {
                    if (timestampsPedido.isEmpty()) true
                    else timestampsPedido.any { it >= calSemana.timeInMillis }
                }
                "MES" -> {
                    if (timestampsPedido.isEmpty()) true
                    else timestampsPedido.any { ts ->
                        val c = Calendar.getInstance(zonaLima).apply { timeInMillis = ts }
                        c.get(Calendar.YEAR) == anioActual && c.get(Calendar.MONTH) == mesActual
                    }
                }
                "MES_PUNTUAL" -> {
                    if (mesSeleccionado == null || anioSeleccionado == null) true
                    else if (timestampsPedido.isEmpty()) false
                    else timestampsPedido.any { ts ->
                        val c = Calendar.getInstance(zonaLima).apply { timeInMillis = ts }
                        c.get(Calendar.YEAR) == anioSeleccionado && c.get(Calendar.MONTH) == mesSeleccionado
                    }
                }
                else -> true
            }
            if (!coincideFecha) return@filter false

            // 3. Búsqueda por texto
            if (busquedaQuery.isNotBlank()) {
                val q = busquedaQuery.trim()
                val coincideOrden = pedido.numeroOrden.contains(q, ignoreCase = true)
                val coincideProv = pedido.proveedorNombre.contains(q, ignoreCase = true)
                val coincideProd = pedido.items.any { it.productoNombre.contains(q, ignoreCase = true) }
                coincideOrden || coincideProv || coincideProd
            } else {
                true
            }
        }.sortedByDescending { fechaReferenciaMsPedido(it) }
    }

    // Conteos rápidos para badges en filtros
    val conteoParciales = remember(pedidosHistorial) {
        pedidosHistorial.count { it.estado == "ENTREGA_PARCIAL" }
    }
    val conteoRecibidos = remember(pedidosHistorial) {
        pedidosHistorial.count { it.estado == "RECIBIDO" }
    }
    val conteoConAjuste = remember(pedidosHistorial) {
        pedidosHistorial.count { it.estado == "COMPLETADA_AJUSTE" }
    }
    val conteoCancelados = remember(pedidosHistorial) {
        pedidosHistorial.count { it.estado == "CANCELADO" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = s.padScreenH, vertical = s.padScreenV)
    ) {
        // ── 1. BARRA SUPERIOR DE CONTROL Y FILTROS ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HISTORIAL DE PEDIDOS A PROVEEDORES",
                    style = FDType.Heading1.copy(fontSize = 18.sp, fontWeight = FontWeight.Black),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = "Control de órdenes de compra, actas de recepción, facturas de compras y auditoría.",
                    style = FDType.BodySmall.copy(fontSize = 12.sp),
                    color = FDColors.TextSecondary
                )
            }

            // Buscador
            OutlinedTextField(
                value = busquedaQuery,
                onValueChange = { busquedaQuery = it },
                placeholder = { Text("Buscar por N° orden, proveedor o producto…", style = FDType.BodySmall.copy(fontSize = 11.5.sp)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = FDColors.TextTertiary, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (busquedaQuery.isNotBlank()) {
                        IconButton(onClick = { busquedaQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = FDColors.TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FDColors.Primary,
                    unfocusedBorderColor = FDColors.Border,
                    focusedContainerColor = FDColors.Surface,
                    unfocusedContainerColor = FDColors.Surface
                ),
                modifier = Modifier
                    .width(360.dp)
                    .height(s.inputMinH)
            )
        }

        Spacer(modifier = Modifier.height(s.gapMedium))

        // ── 2. FILTROS RÁPIDOS POR ESTADO Y POR FECHA ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Selector de Estado: un solo desplegable en vez de 5 chips.
            // (TODOS, PARCIALES, RECIBIDOS, CON AJUSTE, CANCELADOS con su conteo)
            val opcionesEstado = listOf(
                Triple("TODOS", "Todos", pedidosHistorial.size),
                Triple("PARCIALES", "Parciales", conteoParciales),
                Triple("RECIBIDOS", "Recibidos", conteoRecibidos),
                Triple("CON_AJUSTE", "Con ajuste", conteoConAjuste),
                Triple("CANCELADOS", "Cancelados", conteoCancelados)
            )
            val etiquetaEstado = opcionesEstado.firstOrNull { it.first == filtroEstado }?.second ?: "Todos"
            val conteoEstado = opcionesEstado.firstOrNull { it.first == filtroEstado }?.third ?: 0
            Box {
                Surface(
                    color = if (filtroEstado == "TODOS") FDColors.Surface else FDColors.Primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (filtroEstado == "TODOS") FDColors.Border else FDColors.Primary),
                    modifier = Modifier.clickable { mostrarMenuEstado = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Estado",
                            style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            color = FDColors.TextSecondary
                        )
                        Text(
                            text = "$etiquetaEstado ($conteoEstado)",
                            style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                            color = if (filtroEstado == "TODOS") FDColors.TextPrimary else FDColors.Primary
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            null,
                            tint = FDColors.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = mostrarMenuEstado,
                    onDismissRequest = { mostrarMenuEstado = false },
                    containerColor = FDColors.SurfaceElevated
                ) {
                    opcionesEstado.forEach { (clave, etiqueta, conteo) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "$etiqueta ($conteo)",
                                    style = FDType.Body.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = if (filtroEstado == clave) FontWeight.Black else FontWeight.Medium
                                    ),
                                    color = if (filtroEstado == clave) FDColors.Primary else FDColors.TextPrimary
                                )
                            },
                            onClick = {
                                filtroEstado = clave
                                mostrarMenuEstado = false
                            }
                        )
                    }
                }
            }

            // Selector de Fecha: un solo desplegable en vez de 5 botones.
            val opcionesFecha = listOf(
                "TODAS" to "Todas",
                "HOY" to "Hoy",
                "SEMANA" to "Últ. 7 días",
                "MES" to "Este mes"
            )
            val etiquetaFecha = when (filtroFechaRapido) {
                "MES_PUNTUAL" -> if (mesSeleccionado != null && anioSeleccionado != null) {
                    "${NOMBRES_MESES[mesSeleccionado!!]} $anioSeleccionado"
                } else "Por mes"
                else -> opcionesFecha.firstOrNull { it.first == filtroFechaRapido }?.second ?: "Todas"
            }
            Box {
                Surface(
                    color = if (filtroFechaRapido == "TODAS") FDColors.Surface else FDColors.Primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (filtroFechaRapido == "TODAS") FDColors.Border else FDColors.Primary),
                    modifier = Modifier.clickable { mostrarMenuFecha = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            null,
                            tint = if (filtroFechaRapido == "TODAS") FDColors.TextTertiary else FDColors.Primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Fecha",
                            style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            color = FDColors.TextSecondary
                        )
                        Text(
                            text = etiquetaFecha,
                            style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                            color = if (filtroFechaRapido == "TODAS") FDColors.TextPrimary else FDColors.Primary
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            null,
                            tint = FDColors.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = mostrarMenuFecha,
                    onDismissRequest = { mostrarMenuFecha = false },
                    containerColor = FDColors.SurfaceElevated
                ) {
                    opcionesFecha.forEach { (clave, etiqueta) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    etiqueta,
                                    style = FDType.Body.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = if (filtroFechaRapido == clave) FontWeight.Black else FontWeight.Medium
                                    ),
                                    color = if (filtroFechaRapido == clave) FDColors.Primary else FDColors.TextPrimary
                                )
                            },
                            onClick = {
                                filtroFechaRapido = clave
                                mostrarMenuFecha = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Por mes…",
                                style = FDType.Body.copy(
                                    fontSize = 12.5.sp,
                                    fontWeight = if (filtroFechaRapido == "MES_PUNTUAL") FontWeight.Black else FontWeight.Medium
                                ),
                                color = if (filtroFechaRapido == "MES_PUNTUAL") FDColors.Primary else FDColors.TextPrimary
                            )
                        },
                        onClick = {
                            mostrarMenuFecha = false
                            mostrarDialogoMes = true
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(s.gapMedium))

        // ── 3. WORKSPACE ENTERPRISE A DOS COLUMNAS (LISTA + DETALLE) ──
        Surface(
            color = FDColors.Surface,
            shape = RoundedCornerShape(s.radiusCard),
            border = BorderStroke(s.borderWidth, FDColors.Border),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(s.padCard),
                horizontalArrangement = Arrangement.spacedBy(s.gapLarge)
            ) {
                // COLUMNA IZQUIERDA: LISTA DE ÓRDENES (40% ancho)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ÓRDENES DE COMPRA",
                            style = FDType.Label.copy(fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp),
                            color = FDColors.TextTertiary
                        )
                        Text(
                            text = "${pedidosFiltrados.size} resultados",
                            style = FDType.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.TextSecondary
                        )
                    }

                    HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                    if (pedidosFiltrados.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.DateRange, null, tint = FDColors.TextTertiary, modifier = Modifier.size(36.dp))
                                Text(
                                    text = if (pedidosHistorial.isEmpty()) "Aún no hay pedidos en el historial."
                                    else "Ninguna orden coincide con los filtros seleccionados.",
                                    style = FDType.BodySmall.copy(fontSize = 12.sp),
                                    color = FDColors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(pedidosFiltrados, key = { it.id }) { pedido ->
                                val sel = pedidoSeleccionadoId == pedido.id
                                TarjetaOrdenHistorial(
                                    pedido = pedido,
                                    simboloMoneda = simboloMoneda,
                                    seleccionado = sel,
                                    s = s,
                                    onClick = {
                                        pedidoSeleccionadoId = if (pedidoSeleccionadoId == pedido.id) null else pedido.id
                                    }
                                )
                            }
                        }
                    }
                }

                // SEPARADOR VERTICAL
                VerticalDivider(
                    thickness = 1.dp,
                    color = FDColors.Border.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp)
                )

                // COLUMNA DERECHA: DETALLE EJECUTIVO DE LA ORDEN (60% ancho)
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                ) {
                    if (pedidoSeleccionado != null) {
                        // Papel tardío: orden cerrada con mercadería sin comprobante.
                        // En camino no lo muestra: allá el papel nace con la recepción.
                        val puedeAnexar = (pedidoSeleccionado.estado == "RECIBIDO" ||
                            pedidoSeleccionado.estado == "COMPLETADA_AJUSTE") &&
                            pedidoSeleccionado.recepciones.any { rec ->
                                rec.numeroFactura.isBlank() ||
                                    RecepcionMercaderiaEstado.esSinComprobante(rec.numeroFactura)
                            }
                        ReposicionDetallePedidoRealizado(
                            pedido = pedidoSeleccionado,
                            simboloMoneda = simboloMoneda,
                            s = s,
                            // Historial = vitrina: todo visible, nada accionable.
                            soloLectura = true,
                            onVolver = { pedidoSeleccionadoId = null },
                            onRecibirMercaderia = { onRecibirMercaderia(pedidoSeleccionado) },
                            onCerrarConAjuste = { onCerrarConAjuste(pedidoSeleccionado) },
                            onCancelar = { onCancelarPedido(pedidoSeleccionado.id) },
                            onDescartarProducto = { prodId -> onDescartarProducto(pedidoSeleccionado.id, prodId) },
                            mostrarAnexarFactura = puedeAnexar,
                            anexandoFactura = anexandoFactura,
                            onAnexarFactura = { num, monto, cond, venc, emi ->
                                onAnexarFactura(pedidoSeleccionado.id, num, monto, cond, venc, emi)
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(s.padCardLarge),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = FDColors.TextTertiary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "SELECCIONA UNA ORDEN DE COMPRA",
                                    style = FDType.Heading2.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                    color = FDColors.TextTertiary
                                )
                                Text(
                                    text = "Selecciona cualquier pedido de la izquierda para revisar sus productos solicitados, asentar la recepción de mercadería física o consultar su bitácora de auditoría.",
                                    style = FDType.BodySmall.copy(fontSize = 12.sp),
                                    color = FDColors.TextTertiary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.widthIn(max = 380.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaOrdenHistorial(
    pedido: PedidoCompra,
    simboloMoneda: String,
    seleccionado: Boolean,
    s: MedidaAdaptativa,
    onClick: () -> Unit
) {
    val (etiquetaEstado, colorEstado) = when (pedido.estado) {
        "RECIBIDO" -> "RECIBIDO" to FDColors.Success
        "COMPLETADA_AJUSTE" -> "CON AJUSTE" to FDColors.Warning
        "CANCELADO" -> "CANCELADO" to FDColors.Error
        "ENTREGA_PARCIAL" -> "PARCIAL" to FDColors.Warning
        else -> pedido.estado to FDColors.TextSecondary
    }

    Surface(
        color = if (seleccionado) FDColors.Primary.copy(alpha = 0.07f) else FDColors.Background,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (seleccionado) FDColors.Primary else FDColors.Border.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = pedido.proveedorNombre.uppercase(),
                    style = FDType.Label.copy(fontWeight = FontWeight.Black, fontSize = 12.sp),
                    color = FDColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                )

                Surface(
                    color = colorEstado.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, colorEstado.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = etiquetaEstado,
                        style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                        color = colorEstado,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ORDEN: ${pedido.numeroOrden.ifBlank { "S/N" }}",
                    style = FDType.Numeric.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = if (seleccionado) FDColors.Primary else FDColors.TextTertiary
                )
                Text(
                    text = "$simboloMoneda ${String.format(Locale.US, "%.2f", pedido.totalInversion)}",
                    style = FDType.Numeric.copy(fontSize = 13.sp, fontWeight = FontWeight.Black),
                    color = FDColors.TextPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (pedido.estado == "ENTREGA_PARCIAL") {
                        "Recibido ${pedido.totalUnidadesRecibidas} de ${pedido.totalUnidades} · Falta ${pedido.totalUnidadesPendientes}"
                    } else {
                        "${pedido.items.size} productos · ${pedido.totalUnidades} und."
                    },
                    style = FDType.BodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (pedido.estado == "ENTREGA_PARCIAL") FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (pedido.estado == "ENTREGA_PARCIAL") FDColors.Warning else FDColors.TextSecondary
                )
                val fechaTarjeta = if (pedido.fechaRecepcion.isNotBlank()) {
                    "Recibido: ${pedido.fechaRecepcion}"
                } else if (pedido.fechaEmision.isNotBlank()) {
                    "Emisión: ${pedido.fechaEmision}"
                } else {
                    ""
                }
                Text(
                    text = fechaTarjeta,
                    style = FDType.Caption.copy(fontSize = 10.5.sp),
                    color = FDColors.TextTertiary
                )
            }
        }
    }
}

// ── MESES DEL AÑO Y SELECTOR ENTERPRISE ──
private val NOMBRES_MESES = listOf(
    "Enero", "Febrero", "Marzo", "Abril",
    "Mayo", "Junio", "Julio", "Agosto",
    "Setiembre", "Octubre", "Noviembre", "Diciembre"
)

private val ABREV_MESES = listOf(
    "ENE", "FEB", "MAR", "ABR",
    "MAY", "JUN", "JUL", "AGO",
    "SET", "OCT", "NOV", "DIC"
)

/**
 * Diálogo enterprise para filtrar por Año y Mes.
 * Reemplaza el calendario de días para permitir auditoría contable y operativa mes a mes.
 */
@Composable
private fun DialogoSelectorMes(
    anioInicial: Int,
    mesInicial: Int,
    onMesSeleccionado: (anio: Int, mes: Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    var anio by remember { mutableStateOf(anioInicial) }
    var mesTemp by remember { mutableStateOf(mesInicial) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "SELECCIONAR MES",
                    style = FDType.Heading2.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
                    color = FDColors.TextPrimary
                )
                Text(
                    text = "Elige el año y mes para consultar las órdenes del historial.",
                    style = FDType.BodySmall.copy(fontSize = 11.5.sp),
                    color = FDColors.TextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Selector de Año: [ < ]  2026  [ > ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { anio -= 1 }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Año anterior",
                            tint = FDColors.Primary
                        )
                    }
                    Text(
                        text = "$anio",
                        style = FDType.Heading1.copy(fontSize = 20.sp, fontWeight = FontWeight.Black),
                        color = FDColors.TextPrimary
                    )
                    IconButton(onClick = { anio += 1 }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Año siguiente",
                            tint = FDColors.Primary
                        )
                    }
                }

                HorizontalDivider(color = FDColors.Border.copy(alpha = 0.5f))

                // Matriz de 12 Meses (3 filas x 4 columnas)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (fila in 0..2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..3) {
                                val idxMes = fila * 4 + col
                                val esSel = mesTemp == idxMes
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            mesTemp = idxMes
                                            onMesSeleccionado(anio, idxMes)
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (esSel) FDColors.Primary else FDColors.SurfaceElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (esSel) FDColors.Primary else FDColors.Border
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = ABREV_MESES[idxMes],
                                            style = FDType.Label.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black
                                            ),
                                            color = if (esSel) FDColors.PrimaryText else FDColors.TextPrimary
                                        )
                                        Text(
                                            text = NOMBRES_MESES[idxMes],
                                            style = FDType.Caption.copy(fontSize = 9.sp),
                                            color = if (esSel) FDColors.PrimaryText.copy(alpha = 0.85f) else FDColors.TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onMesSeleccionado(anio, mesTemp) }) {
                Text("ACEPTAR", fontWeight = FontWeight.Black, color = FDColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("CANCELAR", color = FDColors.TextSecondary)
            }
        },
        containerColor = FDColors.Surface,
        shape = RoundedCornerShape(14.dp)
    )
}

/**
 * Resuelve todos los timestamps asociados a un pedido (emisión, recepciones, bitácora y textos legibles).
 * Permite que los filtros por fecha encuentren la orden independientemente de si la acción ocurrió al
 * emitir la orden o al recibir mercadería física.
 */
private fun extraerTimestampsPedido(pedido: PedidoCompra): List<Long> {
    val timestamps = mutableListOf<Long>()
    if (pedido.fechaEmisionMs > 0L) timestamps.add(pedido.fechaEmisionMs)
    pedido.recepciones.forEach { r ->
        if (r.fechaMs > 0L) timestamps.add(r.fechaMs)
        val ms = parsearFechaSegura(r.fechaLegible)
        if (ms > 0L) timestamps.add(ms)
    }
    pedido.bitacora.forEach { b ->
        if (b.fechaMs > 0L) timestamps.add(b.fechaMs)
    }
    val msRec = parsearFechaSegura(pedido.fechaRecepcion)
    if (msRec > 0L) timestamps.add(msRec)
    val msEmision = parsearFechaSegura(pedido.fechaEmision)
    if (msEmision > 0L) timestamps.add(msEmision)
    return timestamps
}

/**
 * Parsea de manera ultra-resiliente cadenas de fecha con o sin hora, en formatos dd/MM/yyyy o yyyy-MM-dd.
 */
private fun parsearFechaSegura(texto: String): Long {
    if (texto.isBlank()) return 0L
    val zonaLima = TimeZone.getTimeZone("America/Lima")
    val formatos = listOf(
        "dd/MM/yyyy HH:mm:ss",
        "dd/MM/yyyy HH:mm",
        "dd/MM/yyyy",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd"
    )
    for (f in formatos) {
        try {
            val sdf = SimpleDateFormat(f, Locale.getDefault()).apply {
                timeZone = zonaLima
                isLenient = true
            }
            val d = sdf.parse(texto.trim())
            if (d != null) return d.time
        } catch (_: Exception) {}
    }
    try {
        val limpia = texto.trim().take(10)
        val partes = if (limpia.contains("/")) limpia.split("/") else limpia.split("-")
        if (partes.size == 3) {
            val cal = Calendar.getInstance(zonaLima)
            if (partes[0].length == 4) {
                cal.set(partes[0].toInt(), partes[1].toInt() - 1, partes[2].toInt(), 12, 0, 0)
            } else {
                cal.set(partes[2].toInt(), partes[1].toInt() - 1, partes[0].toInt(), 12, 0, 0)
            }
            return cal.timeInMillis
        }
    } catch (_: Exception) {}
    return 0L
}

/**
 * Fecha de referencia para ordenamiento en historial: la actividad más reciente del pedido.
 */
private fun fechaReferenciaMsPedido(pedido: PedidoCompra): Long {
    val todos = extraerTimestampsPedido(pedido)
    return todos.maxOrNull() ?: 0L
}

