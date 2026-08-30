package com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.FDShapes
import com.app.administradorfarmadon.disenotemaapp.ui.FDSizes
import com.app.administradorfarmadon.disenotemaapp.ui.FDType
import com.app.administradorfarmadon.disenotemaapp.ui.recordarMedidaAdaptativa
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.InventarioViewModel
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.notificaciones.logica.InventarioAlertasLogic
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.notificaciones.ui.AlertasSidePanel
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.animacion.rememberInventarioLayoutState
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.buscador.*
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes.*
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.metricas.*
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.tabla.ProductRow
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.tabla.ProductRowSkeleton
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.tabla.EmptyInventarioState
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.ui.ProductDetailScreen
import com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailState
import androidx.compose.runtime.snapshotFlow
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.InventarioBusquedaEstado
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.PharmProduct
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.paginacion.InfiniteLoadingFooter

@SuppressLint("UnusedBoxWithConstraintsScope", "Range")
@OptIn(ExperimentalFoundationApi::class)
object PerfTracker {
    var startTime: Long = 0
    var startTimeDetail: Long = 0
    var cachedDetailProduct: MoldeProductos? = null

    var pNombre: String = ""
    var pStockTotal: Double = 0.0
    var pUnidad: String = ""
    var pEmpaque: String = ""
    var pEmpaquePlural: String = ""
    var pContenido: String = ""
    var pCategoria: String = ""
    var pPrecioCompra: Double = 0.0

    fun clearCache() {
        cachedDetailProduct = null
        pNombre = ""
        pStockTotal = 0.0
        pUnidad = ""
        pEmpaque = ""
        pEmpaquePlural = ""
        pContenido = ""
        pCategoria = ""
        pPrecioCompra = 0.0
    }
}

@Composable
private fun MetricPremiumInline(
    label: String,
    value: String,
    sub: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLoading: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val s = recordarMedidaAdaptativa()
    val bg = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Column(
        modifier = modifier.then(bg).padding(horizontal = s.xs, vertical = s.xs * 0.35f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(s.xs * 0.35f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs * 0.55f)) {
            Box(Modifier.size(s.iconSmall * 0.92f).clip(CircleShape).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(s.iconTiny * 0.78f))
            }
            Text(label, style = FDType.Label.copy(fontSize = s.textLabel.value.sp * 0.70f, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp), color = FDColors.TextTertiary)
        }
        if (isLoading) {
            Box(Modifier.width(s.gapXLarge * 1.5f).height(s.sm * 1.1f).clip(RoundedCornerShape(s.radiusChip * 0.6f)).background(FDColors.Border.copy(alpha = 0.25f)))
        } else {
            Text(value, style = FDType.Heading3.copy(fontSize = s.textSubtitle.value.sp * 0.90f, fontWeight = FontWeight.Bold), color = FDColors.TextPrimary, maxLines = 1)
        }
        Text(sub, style = FDType.Caption.copy(fontSize = s.textLabel.value.sp * 0.74f, fontWeight = FontWeight.Normal), color = FDColors.TextSecondary, maxLines = 1)
    }
}

@Composable
private fun ElegantMetricCard(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLoading: Boolean,
    onClick: (() -> Unit)?,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa,
    modifier: Modifier = Modifier
) {
    val click = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Surface(
        color = FDColors.SurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.7.dp, FDColors.Border),
        shadowElevation = if (FDColors.isDark) 0.dp else 4.dp,
        modifier = modifier.then(click).height(78.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier.fillMaxWidth().height(1.8.dp)
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(accent.copy(alpha = 0.50f), Color.Transparent)))
                    .align(Alignment.TopCenter)
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.11f))
                        .border(0.6.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    if (isLoading) {
                        Box(Modifier.width(64.dp).height(16.dp).clip(RoundedCornerShape(6.dp)).background(FDColors.Border.copy(alpha = 0.20f)))
                    } else {
                        Text(value, style = FDType.Heading3.copy(fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.3).sp), color = FDColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(label.uppercase(), style = FDType.Label.copy(fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp), color = FDColors.TextTertiary, maxLines = 1)
                    Text(sub, style = FDType.Caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Medium), color = FDColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun InventarioScreen(
    viewModel: InventarioViewModel = viewModel(),
    onNavigateToCrearProducto: () -> Unit = {},
    onNavigateToEditarProducto: (String) -> Unit = {},
    onDetailStateChanged: (Boolean) -> Unit = {},
    onFocusModeChanged: (Boolean) -> Unit = {}
) {
    val s = recordarMedidaAdaptativa()

    val filterState by viewModel.filterState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val estadoTab by viewModel.estadoTab.collectAsState()
    val searchQuery = uiState.searchQuery

    var selectedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var isAlertasPanelOpen by remember { mutableStateOf(false) }
    var detalleTabInicial by rememberSaveable { mutableIntStateOf(0) }

    val closeSidePanels = {
        viewModel.setFilterPanelOpen(false)
        isAlertasPanelOpen = false
        PerfTracker.clearCache()
        if (selectedProductId != null) {
            selectedProductId = null
            onDetailStateChanged(false)
        }
    }

    // BackHandler: teclado primero, luego detalle/alertas/filtros (anti-cierre accidental)
    val focusManagerInventario = LocalFocusManager.current
    val keyboardControllerInventario = LocalSoftwareKeyboardController.current
    val densityInventario = LocalDensity.current
    val isKeyboardVisibleInventario = WindowInsets.ime.getBottom(densityInventario) > 0
    BackHandler(enabled = true) {
        when {
            isKeyboardVisibleInventario -> {
                keyboardControllerInventario?.hide()
                focusManagerInventario.clearFocus(force = true)
            }
            selectedProductId != null -> {
                selectedProductId = null
                onDetailStateChanged(false)
                PerfTracker.clearCache()
            }
            isAlertasPanelOpen -> isAlertasPanelOpen = false
            uiState.isFilterPanelOpen -> viewModel.setFilterPanelOpen(false)
            else -> Unit
        }
    }

    val lazyListState = rememberLazyListState()

    // Abre el detalle de un producto con contexto precargado (0ms) y todo panel cerrado.
    // Único camino de entrada al detalle: fila de la lista o alerta del centro de acción.
    // initialTab: pestaña donde debe aterrizar (ej: alerta de margen → PRECIOS).
    fun abrirDetalleProducto(p: PharmProduct, initialTab: Int = 0) {
        closeSidePanels()
        detalleTabInicial = initialTab
        selectedProductId = p.id
        // CACHE SENIOR: Pre-poblar para carga instantánea en detalle/edición
        PerfTracker.cachedDetailProduct = MoldeProductos(
            indice = p.id,
            nombre = p.name,
            codigo = p.code,
            categoriaPrincipal = p.category,
            empaque = p.empaque,
            stockMinimoBase = p.minStock.toDouble(),
            requiereReceta = p.controlReceta,
            concentracion = p.concentration,
            contenido = p.content,
            ubicacion = p.ubicacion,
            clasificacionControl = p.clasificacionControl
        )
        onDetailStateChanged(true)
    }

    // Recarga reactiva si el usuario itinerante o dueño cambia de sede desde la cabecera
    val currentSucursalId = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalIdEfectiva
    LaunchedEffect(currentSucursalId) {
        if (currentSucursalId.isNotBlank()) {
            viewModel.cargarPaginaInicial()
        }
    }

    // Contrato de frescura del badge de alertas leídas (dato que tolera minutos):
    // recarga al VOLVER a primer plano. La observación tiene dueño (esta pantalla),
    // inicio (ON_RESUME) y fin garantizado (removeObserver al salir).
    val alertasLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(alertasLifecycleOwner) {
        // Cubre el regreso por navegación interna (el efecto se re-crea al entrar).
        viewModel.refrescarAlertasLeidas()
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refrescarAlertasLeidas()
            }
        }
        alertasLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { alertasLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Detección silenciosa de fin de lista → pide siguiente lote de 50 al ViewModel (debounce 300ms interno)
    LaunchedEffect(lazyListState, uiState.pagedProducts.size, uiState.isLoadingMore, uiState.endOfListReached) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                val total = uiState.pagedProducts.size
                if (lastIndex != null && total > 0 && lastIndex >= total - 8 && !uiState.isLoadingMore && !uiState.isLoading && !uiState.endOfListReached) {
                    viewModel.cargarMas()
                }
            }
    }

    val alertas by viewModel.alertas.collectAsState()
    val unreadAlertIds by viewModel.unreadAlertIds.collectAsState()
    val readInfoMap by viewModel.readInfoMap.collectAsState()

    val productsList = uiState.productsList
    val isLoadingProducts = uiState.isLoading
    val totalProductsCount = uiState.totalProductsCount
    val totalInventoryValue = uiState.totalInventoryValue
    val lowStockCount = uiState.lowStockCount
    val nearExpiryCount = uiState.nearExpiryCount
    val isNextPageLoading = uiState.isNextPageLoading
    val pendingNewProductIds = uiState.pendingNewProductIds
    var previousSelectedProductId by remember { mutableStateOf<String?>(null) }

    val activeFiltersCount = remember(filterState, uiState) {
        uiState.selectedCategories.size + uiState.selectedUseCases.size +
            filterState.estadoStock.size + filterState.clasificacion.size + filterState.categorias.size +
            (if (filterState.laboratorio.isNotBlank()) 1 else 0) +
            (if (filterState.vencimiento.isNotBlank()) 1 else 0) +
            (if (filterState.precioMax < Float.MAX_VALUE) 1 else 0) +
            (if (filterState.ubicacion.isNotBlank()) 1 else 0) +
            (if (filterState.soloConLotes) 1 else 0) +
            (if (filterState.requiereReceta) 1 else 0) +
            (if (filterState.soloRefrigerados) 1 else 0) +
            (if (filterState.stockBajoMinimo) 1 else 0)
    }

    // Estados honestos de carga y búsqueda server-side
    val busquedaEstado = uiState.busquedaEstado
    val isBusquedaCargando = busquedaEstado is InventarioBusquedaEstado.Cargando
    val isBusquedaVacia = busquedaEstado is InventarioBusquedaEstado.BusquedaVacia || busquedaEstado is InventarioBusquedaEstado.BusquedaVaciaAlias
    val isBusquedaError = busquedaEstado is InventarioBusquedaEstado.Error
    val isCargandoInicial = isLoadingProducts && uiState.pagedProducts.isEmpty() && !isBusquedaCargando

    val isScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    LaunchedEffect(selectedProductId, previousSelectedProductId) {
        if (selectedProductId == null && previousSelectedProductId != null) {
            previousSelectedProductId?.let { viewModel.reloadProductById(it) }
        }
        previousSelectedProductId = selectedProductId
    }

    val detailViewModel: com.app.administradorfarmadon.inventario.detallesdelproductoinventario.logica.ProductDetailViewModel = viewModel()
    val detailState = detailViewModel.uiState
    LaunchedEffect(detailState) {
        val st = detailState
        if (st is ProductDetailState.Success) {
            if (st.isDeleted) closeSidePanels()
        }
    }

    // ── ESTRUCTURA PRINCIPAL (Enterprise Dark) ──────────────────
    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).imePadding().background(SaaSBackground)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val layoutState = rememberInventarioLayoutState(
                isDetailOpen = selectedProductId != null,
                isFilterPanelOpen = uiState.isFilterPanelOpen,
                screenWidth = maxWidth
            )
            val isCompactMode = layoutState.detailWidth > 10.dp || layoutState.filterWidth > 10.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // PANEL IZQUIERDO: INVENTARIO (MASTER)
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // ── 1. FIXED TOP: HEADER + MÉTRICAS + TOOLBAR ──
                    Column(
                        modifier = Modifier
                            .background(SaaSBackground)
                            .drawBehind {
                                val lineY = size.height - s.separatorH.toPx()
                                drawLine(
                                    color = SaaSBorder,
                                    start = androidx.compose.ui.geometry.Offset(0f, lineY),
                                    end = androidx.compose.ui.geometry.Offset(size.width, lineY),
                                    strokeWidth = s.separatorH.toPx()
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = s.padScreenH, vertical = s.padCard * 0.55f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(s.xs)
                        ) {
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(s.inputMinH)
                                    .clip(RoundedCornerShape(s.radiusInput))
                                    .background(FDColors.InputBackground)
                                    .border(
                                        width = if (isFocused) s.borderWidth * 1.2f else s.borderWidth,
                                        color = if (isFocused) FDColors.BorderFocus else FDColors.InputBorder,
                                        shape = RoundedCornerShape(s.radiusInput)
                                    )
                                    .padding(horizontal = s.padCard),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(s.xs)) {
                                    Icon(
                                        Icons.Default.Search, null,
                                        tint = if (isFocused) FDColors.Primary else FDColors.TextTertiary,
                                        modifier = Modifier.size(s.iconSmall)
                                    )
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                                        interactionSource = interactionSource,
                                        textStyle = FDType.Body.copy(color = FDColors.TextPrimary, fontSize = s.textBody.value.sp, letterSpacing = 0.1.sp),
                                        cursorBrush = SolidColor(FDColors.Primary),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        decorationBox = { inner ->
                                            if (searchQuery.isEmpty()) Text(
                                                "Buscar producto…",
                                                style = FDType.Body.copy(color = FDColors.InputPlaceholder, fontSize = s.textBody.value.sp * 0.92f),
                                                maxLines = 1
                                            )
                                            inner()
                                        }
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        Icon(
                                            Icons.Default.Close, null,
                                            tint = FDColors.TextTertiary,
                                            modifier = Modifier.size(16.dp).clickable { viewModel.onSearchQueryChanged("") }
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .height(s.inputMinH)
                                    .clip(RoundedCornerShape(s.radiusButton))
                                    .background(FDColors.Glass)
                                    .border(s.borderWidth * 0.6f, FDColors.Border, RoundedCornerShape(s.radiusButton))
                                    .clickable { viewModel.setFilterPanelOpen(true) }
                                    .padding(horizontal = s.padCard * 0.85f),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(s.xs * 0.7f)
                                ) {
                                    Icon(Icons.Default.FilterList, null, tint = if (activeFiltersCount > 0) FDColors.Primary else FDColors.TextPrimary, modifier = Modifier.size(s.iconSmall))
                                    Text("FILTROS", color = if (activeFiltersCount > 0) FDColors.Primary else FDColors.TextPrimary, fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
                                    if (activeFiltersCount > 0) {
                                        Box(Modifier.size(s.iconTiny).clip(CircleShape).background(FDColors.Primary), contentAlignment = Alignment.Center) {
                                            Text("$activeFiltersCount", color = FDColors.PrimaryText, fontSize = s.textLabel.value.sp * 0.75f, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(s.inputMinH)
                                    .clip(RoundedCornerShape(s.radiusButton * 0.7f))
                                    .background(FDColors.Glass)
                                    .border(s.borderWidth * 0.6f, FDColors.Border, RoundedCornerShape(s.radiusButton * 0.7f))
                                    .clickable { isAlertasPanelOpen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.Notifications, null, tint = if (unreadAlertIds.isNotEmpty()) FDColors.Warning else FDColors.TextSecondary, modifier = Modifier.size(s.iconSmall))
                                    if (unreadAlertIds.isNotEmpty()) {
                                        Box(
                                            Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-4).dp)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(FDColors.Error)
                                                .border(1.dp, FDColors.Surface, CircleShape)
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = { onNavigateToCrearProducto() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FDColors.Primary,
                                    contentColor = FDColors.PrimaryText
                                ),
                                shape = RoundedCornerShape(s.radiusButton),
                                modifier = Modifier.height(s.inputMinH),
                                contentPadding = PaddingValues(horizontal = s.padCard)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(s.iconTiny))
                                Spacer(Modifier.width(s.xs * 0.7f))
                                Text("NUEVO", style = FDType.Label.copy(color = FDColors.PrimaryText, fontSize = s.textLabel.value.sp, fontWeight = FontWeight.Black))
                            }
                        }

                        val formattedValue = try {
                            "S/ " + String.format(java.util.Locale.US, "%,.2f", totalInventoryValue)
                        } catch (e: Exception) {
                            "S/ $totalInventoryValue"
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = s.padScreenH, vertical = s.xs),
                            horizontalArrangement = Arrangement.spacedBy(s.xs)
                        ) {
                            ElegantMetricCard(label = "Valor", value = formattedValue, sub = "${totalProductsCount} productos", accent = FDColors.Primary, icon = Icons.Outlined.AccountBalanceWallet, isLoading = isCargandoInicial, onClick = null, s = s, modifier = Modifier.weight(1f))
                            ElegantMetricCard(label = "Activos", value = uiState.activeProductsCount.toString(), sub = "en venta", accent = FDColors.Success, icon = Icons.Outlined.Inventory2, isLoading = isCargandoInicial, onClick = { viewModel.seleccionarEstadoTab("TODOS") }, s = s, modifier = Modifier.weight(1f))
                            ElegantMetricCard(label = "Por reponer", value = lowStockCount.toString(), sub = if (estadoTab == "POR_REPONER") "filtrado" else "críticos", accent = FDColors.Warning, icon = Icons.Outlined.WarningAmber, isLoading = isCargandoInicial, onClick = { viewModel.seleccionarEstadoTab(if (estadoTab == "POR_REPONER") "TODOS" else "POR_REPONER") }, s = s, modifier = Modifier.weight(1f))
                            ElegantMetricCard(label = "Por vencer", value = nearExpiryCount.toString(), sub = "30 días", accent = FDColors.Error, icon = Icons.Outlined.Schedule, isLoading = isCargandoInicial, onClick = { viewModel.seleccionarEstadoTab(if (estadoTab == "POR_VENCER") "TODOS" else "POR_VENCER") }, s = s, modifier = Modifier.weight(1f))
                        }

                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = s.padScreenH, vertical = s.xs)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.productsList.isNotEmpty() || activeFiltersCount > 0 || searchQuery.isNotEmpty()) {
                                    com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.componentes.comunes.CategoryTabsRow(
                                        categories = uiState.categories,
                                        selectedCategories = uiState.selectedCategories,
                                        onCategoryClick = { viewModel.toggleCategoryFilter(it) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // ── 2. SCROLLABLE MIDDLE: TABLA ──
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        @OptIn(ExperimentalFoundationApi::class)
                        stickyHeader(key = "table_header") {
                            Row(modifier = Modifier.fillMaxWidth().background(SaaSBackground).padding(horizontal = s.padScreenH, vertical = s.xs), verticalAlignment = Alignment.CenterVertically) {
                                Row(modifier = Modifier.weight(1f).clickable { viewModel.toggleSort(com.app.administradorfarmadon.inventario.inventariopantallaprincipal.logica.InventarioSortColumn.PRODUCTO) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("PRODUCTO", color = FDColors.TextTertiary, fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp * 0.85f, letterSpacing = 1.1.sp)
                                    Icon(Icons.Default.UnfoldMore, null, tint = FDColors.TextTertiary.copy(alpha = 0.5f), modifier = Modifier.size(s.iconTiny * 0.9f))
                                }
                                Box(modifier = Modifier.width(layoutState.cols.code)) { Text("CÓDIGO", color = FDColors.TextTertiary, fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp * 0.85f, letterSpacing = 1.1.sp) }
                                Box(modifier = Modifier.width(layoutState.cols.stock), contentAlignment = Alignment.CenterEnd) { Text("STOCK ACTUAL", color = FDColors.TextTertiary, fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp * 0.85f, letterSpacing = 1.1.sp) }
                                Box(modifier = Modifier.width(layoutState.cols.min), contentAlignment = Alignment.CenterEnd) { Text("STOCK MÍNIMO", color = FDColors.TextTertiary, fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp * 0.85f, letterSpacing = 1.1.sp) }
                                Box(modifier = Modifier.width(layoutState.cols.expiry), contentAlignment = Alignment.CenterEnd) { Text("VENCIMIENTO", color = FDColors.TextTertiary, fontWeight = FontWeight.Black, fontSize = s.textLabel.value.sp * 0.85f, letterSpacing = 1.1.sp) }
                            }
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = s.padScreenH).height(s.separatorH).background(FDColors.Border.copy(alpha = 0.6f)))
                        }

                        if (isBusquedaCargando) {
                            items(5) {
                                ProductRowSkeleton(cols = layoutState.cols)
                            }
                        } else if (isBusquedaError) {
                            item(key = "busqueda_error") {
                                val msg = (busquedaEstado as? InventarioBusquedaEstado.Error)?.mensaje ?: uiState.busquedaError ?: "Error buscando inventario"
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.ErrorOutline, null, tint = FDColors.Error, modifier = Modifier.size(40.dp))
                                        Text(msg, color = FDColors.TextPrimary, style = FDType.Body, fontSize = 14.sp)
                                        Text("Reintenta la búsqueda", color = FDColors.Primary, style = FDType.Label, modifier = Modifier.clickable { viewModel.onSearchQueryChanged(uiState.searchQuery) })
                                    }
                                }
                            }
                        } else if (isBusquedaVacia) {
                            item(key = "busqueda_vacia") {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.SearchOff, null, tint = FDColors.TextTertiary, modifier = Modifier.size(36.dp))
                                        Text("Sin resultados para \"${uiState.searchQuery}\"", color = FDColors.TextPrimary, style = FDType.Body, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Prueba con otro nombre", color = FDColors.TextTertiary, style = FDType.Caption)
                                    }
                                }
                            }
                        } else if (isCargandoInicial) {
                            items(10) {
                                ProductRowSkeleton(cols = layoutState.cols)
                            }
                        } else if (uiState.pagedProducts.isEmpty()) {
                            item(key = "empty") { 
                                EmptyInventarioState(
                                    hasFilters = activeFiltersCount > 0, 
                                    onClearFilters = { 
                                        viewModel.clearAllFilters()
                                        viewModel.seleccionarEstadoTab("TODOS")
                                    }, 
                                    s = s,
                                    tab = estadoTab
                                ) 
                            }
                        } else {
                            items(
                                items = uiState.pagedProducts,
                                key = { it.id },
                                contentType = { "product_row" }
                            ) { product ->
                                ProductRow(
                                    product = product, s = s, cols = layoutState.cols, isCompact = isCompactMode,
                                    isSelected = product.id == selectedProductId,
                                    onProductClick = { p -> abrirDetalleProducto(p) }
                                )
                            }
                        }

                            // Footer silencioso: "Cargando más..." o fin de lista — nunca "Página X de Y"
                            item(key = "infinite_footer") {
                                InfiniteLoadingFooter(
                                    isLoadingMore = uiState.isLoadingMore || uiState.isNextPageLoading,
                                    errorMessage = null
                                )
                            }
                        }
                    }

                    // ── 3. FOOTER INFINITO SILENCIOSO ──
                    // La UI nunca muestra "Página X de Y"; solo "Cargando más..." gestionado por ViewModel (limit 50 interno)
                    // La paginación es 100% silenciosa: ViewModel acumula lotes, UI solo detecta scroll y pide cargarMas()
                }

                // PANEL DERECHO: FILTROS
                if (layoutState.filterWidth > 0.5.dp) {
                    Box(modifier = Modifier.width(layoutState.filterWidth).fillMaxHeight()) {
                        FilterSidePanel(
                            filterState = filterState, products = productsList, modifier = Modifier.fillMaxSize(), onClose = { viewModel.setFilterPanelOpen(false) },
                            onToggleEstadoStock = { viewModel.toggleEstadoStockFilter(it) }, onToggleClasificacion = { viewModel.toggleClasificacionFilter(it) },
                            onToggleCategoria = { viewModel.toggleCategoryFilter(it) }, onLaboratorioChanged = { viewModel.updateFilter { copy(laboratorio = it) } },
                            onVencimientoChanged = { viewModel.updateFilter { copy(vencimiento = it) } }, onPrecioMaxChanged = { viewModel.updateFilter { copy(precioMax = it) } },
                            onUbicacionChanged = { viewModel.updateFilter { copy(ubicacion = it) } }, onToggleSoloConLotes = { viewModel.updateFilter { copy(soloConLotes = !soloConLotes) } },
                            onToggleRequiereReceta = { viewModel.updateFilter { copy(requiereReceta = !requiereReceta) } }, onToggleSoloRefrigerados = { viewModel.updateFilter { copy(soloRefrigerados = !soloRefrigerados) } },
                            onToggleStockBajoMinimo = { viewModel.updateFilter { copy(stockBajoMinimo = !stockBajoMinimo) } }, onClearAll = { viewModel.clearAllFilters() },
                            onApply = { viewModel.setFilterPanelOpen(false) }, s = s,
                            laboratorios = viewModel.getLaboratorios(), ubicaciones = viewModel.getUbicaciones(), categorias = viewModel.getCategorias(), precioMaxDisponible = viewModel.getPrecioMax()
                        )
                    }
                }
            } // Row

            // PANEL ALERTAS (OVERLAY)
            if (isAlertasPanelOpen) {
                Box(modifier = Modifier.fillMaxSize().background(FDColors.Overlay).clickable { isAlertasPanelOpen = false })
                Box(modifier = Modifier.fillMaxSize().align(Alignment.CenterEnd), contentAlignment = Alignment.CenterEnd) {
                    AlertasSidePanel(
                        alertas = alertas, unreadAlertIds = unreadAlertIds, readInfoMap = readInfoMap,
                        modifier = Modifier.width(340.dp).fillMaxHeight(), onClose = { isAlertasPanelOpen = false },
                        onProductClick = { alerta ->
                            // UX: al actuar sobre una alerta TODO panel se cierra y se abre el
                            // detalle del producto directamente. La alerta de margen aterriza en
                            // PRECIOS (donde vive la solución); las demás, en el panorama general.
                            // Al volver, nada queda abierto.
                            val tabInicial = when (alerta.tipo) {
                                InventarioAlertasLogic.TipoAlerta.MARGEN_BAJO -> 1
                                else -> 0
                            }
                            val producto = uiState.pagedProducts.firstOrNull { it.id == alerta.productId }
                                ?: uiState.productsList.firstOrNull { it.id == alerta.productId }
                            if (producto != null) {
                                abrirDetalleProducto(producto, tabInicial)
                            } else {
                                viewModel.setFilterPanelOpen(false)
                                isAlertasPanelOpen = false
                                detalleTabInicial = tabInicial
                                selectedProductId = alerta.productId
                                onDetailStateChanged(true)
                            }
                        },
                        onMarkAllAsRead = { viewModel.markAlertsAsRead() },
                        onMarkAsRead = { viewModel.markAsRead(it.productId, it.tipo.name) }
                    )
                }
            } // if isAlertasPanelOpen
        } // BoxWithConstraints

        // PANEL DETALLES: PANTALLA COMPLETA (overlay) — ocupa todo, no queda aplastado al lado
        if (selectedProductId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FDColors.Background)
                    // Bloquea el fondo: captura los toques para que NO lleguen a la lista detrás.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                ProductDetailScreen(
                    productId = selectedProductId!!, initialTabIndex = detalleTabInicial,
                    onClose = { closeSidePanels() },
                    onEdit = { product -> onNavigateToEditarProducto(product.indice) },
                    onFocusModeChanged = onFocusModeChanged
                )
            }
        }

    }


@Composable
private fun OptionEntradaEnterprise(
    titulo: String,
    descripcion: String,
    badge: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    s: com.app.administradorfarmadon.disenotemaapp.ui.MedidaAdaptativa
) {
    Surface(
        onClick = onClick,
        color = FDColors.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, FDColors.BorderStrong.copy(alpha = if (FDColors.isDark) 1f else 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FDColors.Primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, null, tint = FDColors.Primary, modifier = Modifier.size(26.dp))
            }
            
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        titulo.uppercase(),
                        style = FDType.Label.copy(fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                        color = FDColors.TextPrimary
                    )
                    Surface(
                        color = FDColors.Primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            badge,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = FDType.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = FDColors.Primary
                        )
                    }
                }
                Text(
                    descripcion,
                    style = FDType.Body.copy(fontSize = 13.sp),
                    color = FDColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Icon(
                Icons.Default.ArrowForward, 
                null, 
                tint = FDColors.TextTertiary, 
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
