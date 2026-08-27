package com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.inventario.compartido.modelo.stockDisponibleUnidades
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.componentes.bounceClick
import com.app.administradorfarmadon.inventario.compartido.modelo.LoteProducto
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailMapper
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.modelo.MovimientoInventario
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes.LoteDetailDrawer
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes.ModuloLotesYStock
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.componentes.ModuloPreciosYFraccionamiento
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion.LabelPdfExporter
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.configuracion.ModuloConfiguracionPrincipal
import androidx.compose.ui.platform.LocalContext

/**
 * Vista de Detalle de Producto Enterprise a Pantalla Completa.
 * Organizada en 4 módulos operativos directos (Lotes, Precios, Kardex, Configuración y Logística).
 */
@Composable
fun ProductDetailContent(
    p: MoldeProductos,
    movements: List<MovimientoInventario>,
    isPrivileged: Boolean,
    initialTabIndex: Int = 0,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onAdjustStock: (LoteProducto?) -> Unit,
    ubicacionesDisponibles: List<String> = emptyList(),
    onEliminarProducto: (product: MoldeProductos, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit = { _, _, _ -> },
    onEliminadoExito: () -> Unit = {},
    onCambiarBloqueoLote: (lote: LoteProducto, ponerEnCuarentena: Boolean, cantidad: Double, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onRegistrarDevolucion: (lote: LoteProducto, cantidad: Double, guiaRetiro: String, notaCredito: String, motivo: String, modalidad: String, onComplete: (Result<Unit>) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    onRegistrarCanje: (lote: LoteProducto, cantidad: Double, nuevoLote: String, nuevoVencimiento: String, guiaCanje: String, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    onGuardarPrecios: (unidadBase: String, presentaciones: List<com.app.administradorfarmadon.inventario.compartido.modelo.PresentacionProducto>, onComplete: (Result<Unit>) -> Unit) -> Unit = { _, _, _ -> },
    onGuardarConfiguracion: (ubicacion: String, stockMinimo: Double, activo: Boolean, diasAlertaVencimiento: Int, nuevoCodigo: String?, onComplete: (Result<Unit>) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onGenerarCodigoUnico: suspend () -> String = { "" },
    onVerificarDuplicadoCodigo: suspend (String) -> String? = { null },
    onMarcarEtiquetaImpresa: () -> Unit = {},
    onAnularIngreso: (lote: LoteProducto, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit = { _, _, _ -> },
    onRegistrarMerma: (lote: LoteProducto, cantidad: Double, motivo: String, onComplete: (Result<Unit>) -> Unit) -> Unit = { _, _, _, _ -> },
    onDefinirPrioridadLote: (loteId: String?) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTabIndex by remember(initialTabIndex) { mutableIntStateOf(initialTabIndex) }
    // Búsqueda pre-cargada del Kardex cuando se pide "historial de ESTE lote" desde Lotes.
    // Al SALIR de la pestaña Kardex se limpia: jamás un filtro fantasma que esconda datos.
    var filtroKardexInicial by remember { mutableStateOf("") }
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex != 2 && filtroKardexInicial.isNotBlank()) filtroKardexInicial = ""
    }

    val stockDisponible = remember(p.lotes) { p.stockDisponibleUnidades }
    val totalStock = stockDisponible
    val stockMinimo = p.stockMinimoBase
    val isLowStock = stockDisponible <= stockMinimo && stockMinimo > 0
    val isAgotado = stockDisponible <= 0

    val valorTotalInventario = remember(p.lotes, p.precioCompra) {
        if (p.lotes.isNotEmpty()) {
            p.lotes.values.sumOf { lote ->
                val costoLote = if (lote.costoCompraUnitario > 0) lote.costoCompraUnitario else p.precioCompra
                (lote.cantidad + lote.cantidadBloqueada) * costoLote
            }
        } else {
            0.0
        }
    }

    val nearestExpiryLote = remember(p.lotes) {
        p.lotes.values.minByOrNull { ProductDetailMapper.diasHastaVencer(it.vencimiento) ?: Int.MAX_VALUE }
    }
    val diasVencimiento = nearestExpiryLote?.let { ProductDetailMapper.diasHastaVencer(it.vencimiento) }

    val lotesActivos = remember(p.lotes) { p.lotes.values.count { it.cantidad + it.cantidadBloqueada > 0 } }
    val tabs = listOf(
        "Lotes" to if (lotesActivos > 0) "$lotesActivos" else if (p.lotes.isNotEmpty()) "${p.lotes.size}" else "0",
        "Precios y Presentaciones" to "${p.presentaciones.size}",
        "Kardex y Auditoría" to if (movements.isNotEmpty()) "${movements.size}" else null,
        "Configuración" to null
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FDColors.Background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // ── 1. CABECERA SUPERIOR ESBELTA ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp) // Altura esbelta
                .background(FDColors.Background)
                .padding(horizontal = 20.dp), // Padding reducido
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Izquierda: Botón Volver + Identidad del Medicamento
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FDColors.SurfaceElevated)
                        .border(0.5.dp, FDColors.Border, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = FDColors.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = p.nombre.ifBlank { "SIN NOMBRE" }.uppercase(),
                            style = FDType.Heading3.copy(
                                fontWeight = FontWeight.Bold,
                                color = FDColors.TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Badges de Estado más sutiles
                        when {
                            isAgotado -> EnterpriseStatusPill("AGOTADO", FDColors.Error)
                            isLowStock -> EnterpriseStatusPill("BAJO STOCK", FDColors.Warning)
                        }
                    }

                    // Breadcrumb Compacto
                    Text(
                        text = "${p.categoriaPrincipal.ifBlank { "General" }}  ·  Lab: ${p.proveedorBaseNombre.ifBlank { "Genérico" }}  ·  ${p.ubicacion.ifBlank { "Sin Ubicación" }}",
                        style = FDType.Caption.copy(fontSize = 11.sp, color = FDColors.TextSecondary)
                    )
                }
            }

            // Derecha: Restaurar Acción de Edición de Ficha
            OutlinedButton(
                onClick = onEdit,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, FDColors.Border),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Editar Ficha", style = FDType.Label.copy(fontSize = 11.5.sp))
            }
        }

        // ── BANNER ACCIONABLE DE ETIQUETAS PENDIENTES POR CAMBIO DE PRECIOS ──
        if (p.etiquetaPendienteReimpresion && p.etiquetaPendienteDetalle.isNotBlank()) {
            Surface(
                color = FDColors.SurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, FDColors.Border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalOffer,
                            contentDescription = null,
                            tint = FDColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "ETIQUETAS PENDIENTES DE REIMPRESIÓN",
                                style = FDType.Caption.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FDColors.TextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = p.etiquetaPendienteDetalle,
                                style = FDType.Body.copy(
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FDColors.TextPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedTabIndex = 3 },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, FDColors.Border),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "Ver Panel",
                                style = FDType.Caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
                            )
                        }

                        Button(
                            onClick = {
                                val itemsToPrint = if (p.etiquetasPendientesLista.isNotEmpty()) {
                                    p.etiquetasPendientesLista
                                } else {
                                    val pres = p.presentaciones.firstOrNull { it.presentacionId == p.etiquetaPendientePresentacionId }
                                        ?: p.presentaciones.firstOrNull()
                                    if (pres != null) {
                                        listOf(
                                            com.app.administradorfarmadon.inventario.compartido.modelo.EtiquetaPendienteItem(
                                                presentacionId = pres.presentacionId,
                                                nombre = pres.nombre,
                                                precio = if (p.etiquetaPendientePrecio > 0) p.etiquetaPendientePrecio else pres.precioventa,
                                                cantidad = pres.cantidad
                                            )
                                        )
                                    } else emptyList()
                                }

                                if (itemsToPrint.isNotEmpty()) {
                                    val pdfFile = LabelPdfExporter.generarPdfEtiquetasLote(
                                        context = context,
                                        producto = p,
                                        items = itemsToPrint,
                                        formatoGondola = true
                                    )
                                    LabelPdfExporter.imprimirPdfDirecto(
                                        context = context,
                                        pdfFile = pdfFile,
                                        nombreTrabajo = "Etiquetas_${p.nombre.take(15)}",
                                        onImpresionConfirmada = { onMarcarEtiquetaImpresa() }
                                    )
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FDColors.Primary,
                                contentColor = FDColors.PrimaryText
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Print, null, modifier = Modifier.size(14.dp))
                                Text(
                                    text = if (p.etiquetasPendientesLista.size > 1) "Imprimir (${p.etiquetasPendientesLista.size})" else "Imprimir",
                                    style = FDType.Caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)

        // ── 2. NAVEGACIÓN ADAPTABLE (SCROLLABLE TAB ROW) ──
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 20.dp,
            containerColor = FDColors.Background,
            divider = {}, // Mantener zonificación limpia
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = FDColors.Primary,
                    height = 2.dp
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, (title, count) ->
                val isSelected = selectedTabIndex == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedTabIndex = index },
                    unselectedContentColor = FDColors.TextSecondary,
                    selectedContentColor = FDColors.Primary,
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title.uppercase(),
                                style = FDType.Label.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) FDColors.Primary else FDColors.TextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            if (count != null) {
                                Text(
                                    text = "[ $count ]",
                                    style = FDType.Caption.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) FDColors.Primary.copy(alpha = 0.7f) else FDColors.TextTertiary
                                    )
                                )
                            }
                        }
                    }
                )
            }
        }

        HorizontalDivider(color = FDColors.Border, thickness = 0.5.dp)

        // ── 3. CONTENIDO PRINCIPAL (ZONIFICACIÓN VISUAL) ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(FDColors.Surface) // Cambio de tono para separar de la navegación
        ) {
            // Sombra interna sutil bajo las pestañas para dar profundidad
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.03f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Documento continuo a ancho completo — el panel derecho de "liquidación" fue retirado.
            // Cerrar vive en la flecha de la cabecera (y gesto atrás); editar en "Editar Ficha".
            // SIN verticalScroll aquí: cada módulo gestiona su propio scroll interno y necesita
            // altura ACOTADA — una capa extra entregaba altura infinita y tumbaba la app.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                    when (selectedTabIndex) {
                0 -> ModuloLotesYStock(
                    p = p,
                    totalStock = totalStock,
                    stockMinimo = stockMinimo,
                    valorTotalInventario = valorTotalInventario,
                    nearestExpiryLote = nearestExpiryLote,
                    diasVencimiento = diasVencimiento,
                    isPrivileged = isPrivileged,
                    movements = movements,
                    onVerKardexDelLote = { numero ->
                        filtroKardexInicial = numero
                        selectedTabIndex = 2
                    },
                    onCambiarBloqueoLote = onCambiarBloqueoLote,
                    onRegistrarDevolucion = onRegistrarDevolucion,
                    onRegistrarCanje = onRegistrarCanje,
                    onAnularIngreso = onAnularIngreso,
                    onRegistrarMerma = onRegistrarMerma,
                    onDefinirPrioridadLote = onDefinirPrioridadLote,
                    onAdjustStock = onAdjustStock
                )
                1 -> ModuloPreciosYFraccionamiento(
                    product = p,
                    precioCosto = p.precioCompra, // Usar directamente del modelo para evitar problemas de referencia
                    isPrivileged = true, // Edición habilitada para todos según instrucción
                    onGuardarPrecios = onGuardarPrecios
                )
                2 -> ModuloKardexYAuditoria(movements, p, busquedaInicial = filtroKardexInicial)
                3 -> ModuloConfiguracionPrincipal(
                    product = p,
                    movements = movements,
                    ubicacionesDisponibles = ubicacionesDisponibles,
                    onGuardarConfiguracion = onGuardarConfiguracion,
                    onGenerarCodigoUnico = onGenerarCodigoUnico,
                    onVerificarDuplicadoCodigo = onVerificarDuplicadoCodigo,
                    onMarcarEtiquetaImpresa = onMarcarEtiquetaImpresa,
                    isPrivileged = isPrivileged,
                    onEliminarProducto = { motivo, cb -> onEliminarProducto(p, motivo, cb) },
                    onEliminadoExito = onEliminadoExito
                )
            }
            }
        }
    }
}
