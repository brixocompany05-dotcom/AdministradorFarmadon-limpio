package com.app.administradorfarmadon.navegacion

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.camera.core.ExperimentalGetImage
import com.app.administradorfarmadon.analitica_reportes.ui.AnaliticaReportesScreen
import kotlin.OptIn
import com.app.administradorfarmadon.inventario.crearproductogeneral.logica.CrearProductoGeneralViewModel
import com.app.administradorfarmadon.inventario.crearproductogeneral.ui.CrearProductoGeneralScreen
import com.app.administradorfarmadon.inventario.editarproductosinventario.logica.EditarProductoViewModel
import com.app.administradorfarmadon.inventario.editarproductosinventario.ui.EditarProductoScreen
import com.app.administradorfarmadon.inventario.inventariopantallaprincipal.ui.InventarioScreen
import com.app.administradorfarmadon.navegacion.sidebar.FarmadonSidebar
import com.app.administradorfarmadon.navegacion.sidebar.SidebarTheme
import com.app.administradorfarmadon.navegacion.sidebar.SidebarViewModel
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.ThemeViewModel
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import com.app.administradorfarmadon.configuracion.sucursales.logica.SucursalesViewModel
import com.app.administradorfarmadon.configuracion.sucursales.ui.SucursalesScreen
import com.app.administradorfarmadon.configuracion.usuarios.logica.UsuariosViewModel
import com.app.administradorfarmadon.configuracion.usuarios.ui.UsuariosScreen
import com.app.administradorfarmadon.configuracion.plan.ui.GestionPlanScreen
import com.app.administradorfarmadon.configuracion.plan.ui.PlanFacturacionScreen
import com.app.administradorfarmadon.facturacionelectronica.ui.FacturacionElectronicaScreen
import com.app.administradorfarmadon.configuracion.ui.ConfiguracionScreen
import com.app.administradorfarmadon.configuracion.metodospago.ui.MetodosPagoScreen
import com.app.administradorfarmadon.configuracion.pos.ui.PosConfigScreen
import com.app.administradorfarmadon.notificaciones.suscripcion.logica.AlertaSuscripcionViewModel
import com.app.administradorfarmadon.notificaciones.suscripcion.ui.AlertaFlotanteBanner
import com.app.administradorfarmadon.appconexioninternet.NetworkHealthMonitor
import com.app.administradorfarmadon.appconexioninternet.NetworkStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MIN_SUPPORTED_WIDTH_DP = 600

@ExperimentalGetImage
@Composable
fun ContenedorAplicacion(
    navController: NavHostController = rememberNavController(),
    sidebarViewModel: SidebarViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel(),
    alertaViewModel: AlertaSuscripcionViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val alertaVisible by alertaViewModel.alertaVisible.collectAsState()
    val networkStatus by NetworkHealthMonitor.status.collectAsState()
    val ultimaConexionMs by NetworkHealthMonitor.ultimaConexionMs.collectAsState()

    val sidebarItems by sidebarViewModel.items.collectAsState()
    val isSidebarLoading by sidebarViewModel.isLoading.collectAsState()
    val suscripcionVencida by sidebarViewModel.vencida.collectAsState()
    val clienteSuspendido by sidebarViewModel.suspendida.collectAsState()
    val estadoBloqueo by sidebarViewModel.estadoBloqueo.collectAsState()
    val motivoBloqueo by sidebarViewModel.motivoBloqueo.collectAsState()
    val modalidadPausa by sidebarViewModel.modalidadPausa.collectAsState()
    val pausadoDesdeTexto by sidebarViewModel.pausadoDesdeTexto.collectAsState()
    val pausadoHastaTexto by sidebarViewModel.pausadoHastaTexto.collectAsState()
    val diasRestantesPausa by sidebarViewModel.diasRestantesPausa.collectAsState()
    val sesionRevocada by sidebarViewModel.sesionRevocada.collectAsState()
    val sucursalIdEfectiva by sidebarViewModel.sucursalIdEfectiva.collectAsState()
    val rolIdEfectivo by sidebarViewModel.rolIdEfectivo.collectAsState()
    val esTemaOscuro by themeViewModel.esTemaOscuro.collectAsState()
    SideEffect { SidebarTheme.isDark = esTemaOscuro }

    val nombreFarmacia by sidebarViewModel.nombreFarmacia.collectAsState()
    val sucursalNombre by sidebarViewModel.sucursalNombre.collectAsState()
    val planNombre by sidebarViewModel.planNombre.collectAsState()
    val usuarioNombre by sidebarViewModel.usuarioNombre.collectAsState()
    val rolNombre by sidebarViewModel.rolNombre.collectAsState()
    val esItinerante by sidebarViewModel.esItinerante.collectAsState()
    val planPermiteMultiSede by sidebarViewModel.planPermiteMultiSede.collectAsState()
    val sucursalesDisponibles by sidebarViewModel.sucursalesDisponibles.collectAsState()
    val notificacionFlotante by sidebarViewModel.notificacionFlotante.collectAsState()
    val errorCarga by sidebarViewModel.errorCarga.collectAsState()

    val contexto = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorCarga) {
        if (errorCarga != null) {
            val result = snackbarHostState.showSnackbar(
                message = errorCarga!!,
                actionLabel = "Reintentar",
                withDismissAction = true
            )
            if (result == SnackbarResult.ActionPerformed) {
                sidebarViewModel.recargarSesion()
            }
        }
    }

    val currentAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Sincronización de sesión en tiempo real (R8/HL12): la sede y rol efectivos
    // del usuario en turno se reflejan en SessionManager sin reiniciar, para que
    // la operación (caja/ventas) use el dato nuevo del servidor de inmediato.
    LaunchedEffect(sucursalIdEfectiva, rolIdEfectivo, rolNombre) {
        com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.init(contexto)
        if (sucursalIdEfectiva.isNotBlank()) {
            com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalId = sucursalIdEfectiva
        }
        val rolFinal = if (rolNombre.isNotBlank()) rolNombre else rolIdEfectivo
        if (rolFinal.isNotBlank()) {
            com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.rol = rolFinal
        }
    }

    var isInventoryDetailOpen by remember { mutableStateOf(false) }
    var isFocusModeActive by remember { mutableStateOf(false) }
    val shouldBlur by remember { derivedStateOf { isFocusModeActive } }

    LaunchedEffect(currentAuthUid) {
        if (currentAuthUid.isNotBlank()) {
            sidebarViewModel.recargarSesion()
        }
    }

    if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
        LaunchedEffect(Unit) {
            onLogout()
        }
        return
    }

    // Bloqueo de acceso (reglas 1.2 y 1.3): suscripción vencida o tenant suspendido/pausado.
    // Se evalúa en tiempo real; al renovar/reactivar, la app se desbloquea sola.
    if ((suscripcionVencida || clienteSuspendido) && !isSidebarLoading) {
        PantallaSuscripcionVencida(
            estadoBloqueo = estadoBloqueo,
            motivoBloqueo = motivoBloqueo,
            modalidadPausa = modalidadPausa,
            pausadoDesdeTexto = pausadoDesdeTexto,
            pausadoHastaTexto = pausadoHastaTexto,
            diasRestantesPausa = diasRestantesPausa,
            onVolver = { navController.popBackStack() }
        )
        return
    }

    // Bloqueo de sesión del usuario en tiempo real (R1/R3): si se borró la cuenta
    // o se suspendió el acceso mientras estaba dentro, se cierra la sesión ya.
    if (sesionRevocada != null && !isSidebarLoading) {
        LaunchedEffect(sesionRevocada) {
            FirebaseAuth.getInstance().signOut()
            onLogout()
        }
        PantallaMensajeBloqueo(
            titulo = "Sesión finalizada",
            mensaje = sesionRevocada ?: "Tu acceso fue cerrado."
        )
        return
    }

    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
     
    Box(modifier = Modifier.fillMaxSize()) {
        if (screenWidth < MIN_SUPPORTED_WIDTH_DP.dp) {
            PantallaNoCompatible()
            return@Box
        }
        if (isSidebarLoading) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = TokensFarmadon.colores.fondoBase
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FDColors.Primary)
                }
            }
            return@Box
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(TokensFarmadon.colores.fondoBase)
                .then(if (shouldBlur) Modifier.blur(12.dp) else Modifier)
        ) {
            FarmadonSidebar(
                currentRoute = currentRoute,
                items = sidebarItems,
                isLoading = isSidebarLoading,
                nombreFarmacia = nombreFarmacia,
                sucursalNombre = sucursalNombre,
                planNombre = planNombre,
                usuarioNombre = usuarioNombre,
                rolNombre = rolNombre,
                isDarkMode = esTemaOscuro,
                esItinerante = esItinerante,
                planPermiteMultiSede = planPermiteMultiSede,
                sucursales = sucursalesDisponibles,
                onCambiarSucursal = { id, nom ->
                    sidebarViewModel.cambiarSucursalActiva(id, nom)
                },
                onToggleTheme = { themeViewModel.alternarTema() },
                onNavigate = { route ->
                    navigateToTab(navController, route)
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(SidebarTheme.Border)
            )

            val handleLogout = {
                sidebarViewModel.registrarCierreSesionUsuario()
                onLogout()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TokensFarmadon.colores.fondoBase)
            ) {
                BannerConexionGlobal(
                    status = networkStatus,
                    ultimaConexionMs = ultimaConexionMs
                )

                AppNavHost(
                    navController = navController,
                    onLogout = handleLogout,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .safeDrawingPadding(),
                    onInventoryDetailStateChanged = { isInventoryDetailOpen = it },
                    onFocusModeChanged = { isFocusModeActive = it }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                // 1. Notificación Flotante Superior de Cambio de Sede / Itinerancia (Dynamic Island)
                androidx.compose.animation.AnimatedVisibility(
                    visible = notificacionFlotante != null,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut()
                ) {
                    notificacionFlotante?.let { texto ->
                        LaunchedEffect(texto) {
                            kotlinx.coroutines.delay(4000)
                            sidebarViewModel.descartarNotificacionFlotante()
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .border(0.8.dp, TokensFarmadon.colores.cardBorde, RoundedCornerShape(30.dp)),
                            color = TokensFarmadon.colores.cardElevada,
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(SidebarTheme.Accent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = SidebarTheme.Accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = texto,
                                    style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = TokensFarmadon.colores.textoPrincipal
                                )
                                IconButton(
                                    onClick = { sidebarViewModel.descartarNotificacionFlotante() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar",
                                        tint = TokensFarmadon.colores.textoTerciario,
                                        modifier = Modifier.size(12.dp)
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.wrapContentSize(Alignment.BottomCenter)
        )
    }
                    }
                }

                // 2. Banner Flotante Global de Suscripción
                AlertaFlotanteBanner(
                    alerta = alertaVisible,
                    onVerDetalle = {
                        alertaVisible?.let { a ->
                            alertaViewModel.descartarAlertaManual(a.id)
                            when (a.tipo) {
                                // Solo el comprobante observado lleva al flujo de pago/subsanación
                                com.app.administradorfarmadon.notificaciones.suscripcion.datos.TipoAlertaSuscripcion.COMPROBANTE_OBSERVADO ->
                                    com.app.administradorfarmadon.suscripcion.ReportarPagoManager.abrirDialogoManual()
                                // Todo lo demás promete VER EL PLAN → llevar al plan de verdad
                                else -> navController.navigate("config_plan")
                            }
                        }
                    },
                    onDescartar = { alertaId ->
                        alertaViewModel.descartarAlertaManual(alertaId)
                    }
                )
            }
        }
    }


private fun navigateToTab(navController: NavHostController, route: String) {
    android.util.Log.d("FARMADON_NAV", "navigateToTab solicitado con ruta: '$route'")
    try {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    } catch (e: Exception) {
        android.util.Log.w("FARMADON_NAV", "Navegación a módulo sin ruta: ${e.message}", e)
        // Guard anti-crash: módulo sin pantalla definida aún en el NavHost.
        // No navegar en lugar de lanzar (el sidebar navega por código de módulo).
    }
}

@ExperimentalGetImage
@Composable
private fun AppNavHost(
    navController: NavHostController,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    onInventoryDetailStateChanged: (Boolean) -> Unit = {},
    onFocusModeChanged: (Boolean) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = "inventario",
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(150)) },
        exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) },
        popEnterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(150)) },
        popExitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) }
    ) {
        // --- INVENTARIO ---
        val pantallaInventario: @Composable () -> Unit = {
            InventarioScreen(
                onNavigateToCrearProducto = { navController.navigate("nuevo_producto") },
                onNavigateToEditarProducto = { productId ->
                    navController.navigate("editar_producto/$productId")
                },
                onDetailStateChanged = onInventoryDetailStateChanged,
                onFocusModeChanged = onFocusModeChanged
            )
        }
        composable("inventario") { pantallaInventario() }

        composable("nuevo_producto") {
            val vm: CrearProductoGeneralViewModel = viewModel()
            CrearProductoGeneralScreen(
                viewModel = vm,
                onProductoCreado = { _ ->
                    navController.popBackStack("inventario", inclusive = false)
                },
                onAtras = { navController.popBackStack() },
            )
        }

        composable(
            route = "editar_producto/{productId}",
            arguments = listOf(androidx.navigation.navArgument("productId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val vm: EditarProductoViewModel = viewModel()
            EditarProductoScreen(
                productoId = productId,
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        // --- VENTAS ---
        composable("ventas") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "NUEVA VENTA",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }
        composable("ventas_nueva") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "NUEVA VENTA",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }
        composable("ventas_dia") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "VENTAS DEL DÍA",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }
        composable("ventas_devoluciones") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "DEVOLUCIONES",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }
        composable("ventas_caja") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "CIERRE DE CAJA",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }
        composable("pos") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "NUEVA VENTA",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }
        composable("caja") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "CIERRE DE CAJA",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }
        composable("cierre_caja") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "CIERRE DE CAJA",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }

        // --- DISPENSACIÓN (Atención directa en caja / mostrador) ---
        composable("dispensacion_recetas") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "NUEVA VENTA",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }
        composable("dispensacion_controlados") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "NUEVA VENTA",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }
        composable("dispensacion_adulto") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "NUEVA VENTA",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }

        // --- INVENTARIO Y COMPRAS ---
        val pantallaCompras: @Composable (pestana: String) -> Unit = { pestana ->
            // UN solo ViewModel para TODAS las rutas de compras (dueño: la actividad).
            // Al navegar entre módulos los datos siguen vivos en tiempo real y la
            // pantalla se pinta al instante: jamás se recarga ni se recrea de cero.
            val owner = LocalContext.current as? androidx.activity.ComponentActivity
            val vm: com.app.administradorfarmadon.compras.logica.ComprasViewModel =
                if (owner != null) viewModel(viewModelStoreOwner = owner) else viewModel()
            com.app.administradorfarmadon.compras.ui.ComprasScreen(
                viewModel = vm,
                pestanaInicial = pestana
            )
        }
        composable("inventario_compras") { pantallaCompras("REPOSICION") }
        composable("compras") { pantallaCompras("REPOSICION") }
        composable("compras_reposicion") { pantallaCompras("REPOSICION") }
        composable("compras_pedidos") { pantallaCompras("REPOSICION") }
        composable("gestion_compras") { pantallaCompras("REPOSICION") }
        composable("compras_proveedores") { pantallaCompras("PROVEEDORES") }
        composable("proveedores") { pantallaCompras("PROVEEDORES") }
        composable("inventario_proveedores") { pantallaCompras("PROVEEDORES") }
        composable("compras_distribuidores") { pantallaCompras("PROVEEDORES") }
        composable("compras_facturas") { pantallaCompras("CUENTAS") }
        composable("inventario_vencimientos") { pantallaInventario() }
        composable("inventario_transferencias") { PantallaEnConstruccion("Transferencias entre Sucursales") { navController.popBackStack() } }

        // --- CLIENTES ---
        val pantallaClientes: @Composable () -> Unit = {
            com.app.administradorfarmadon.clientes.ui.ClientesScreen(
                onVolver = { navController.popBackStack() }
            )
        }
        composable("clientes") { pantallaClientes() }
        composable("clientes_directorio") { pantallaClientes() }
        composable("clientes_crm") { pantallaClientes() }
        composable("clientes_puntos") { pantallaClientes() }
        composable("clientes_historial") { pantallaClientes() }

        // --- SOPORTE & MESA DE AYUDA BRIXO ---
        val pantallaSoporte: @Composable () -> Unit = {
            com.app.administradorfarmadon.soporte.ui.SoporteScreen(
                onVolver = { navController.popBackStack() }
            )
        }
        composable("soporte") { pantallaSoporte() }
        composable("soporte_inapp") { pantallaSoporte() }

        // --- ANALÍTICA & REPORTES ---
        val pantallaAnaliticaReportes: @Composable (pestanaInicial: String) -> Unit = { pestana ->
            AnaliticaReportesScreen(
                pestanaInicial = pestana,
                onVolver = { navController.popBackStack() }
            )
        }
        composable("analitica_reportes") { pantallaAnaliticaReportes("ANALITICA") }
        composable("analitica") { pantallaAnaliticaReportes("ANALITICA") }
        composable("reportes") { pantallaAnaliticaReportes("ANALITICA") }
        composable("bi") { pantallaAnaliticaReportes("ANALITICA") }
        composable("reportes_dashboard") { pantallaAnaliticaReportes("ANALITICA") }
        composable("reportes_caja") { pantallaAnaliticaReportes("REPORTES") }
        composable("reportes_inventario") { pantallaAnaliticaReportes("REPORTES") }
        composable("reportes_fiscal") { pantallaAnaliticaReportes("REPORTES") }
        composable("reportes_ventas") { pantallaAnaliticaReportes("REPORTES") }
        composable("reportes_compras") { pantallaAnaliticaReportes("REPORTES") }
        composable("reportes_clientes") { pantallaAnaliticaReportes("REPORTES") }

        // --- FACTURACIÓN ELECTRÓNICA ---
        val pantallaFacturacion: @Composable (Int) -> Unit = { pestana ->
            FacturacionElectronicaScreen(
                pestanaInicial = pestana,
                onVolver = { navController.popBackStack() }
            )
        }
        composable("facturacion") { pantallaFacturacion(0) }
        composable("facturacion_electronica") { pantallaFacturacion(0) }
        composable("config_facturacion") { pantallaFacturacion(2) }
        composable("facturacion_config") { pantallaFacturacion(2) }
        composable("facturacion_emisor") { pantallaFacturacion(2) }

        // --- CONFIGURACIÓN ---
        val pantallaConfiguracion: @Composable () -> Unit = {
            ConfiguracionScreen(
                onNavigateToSucursales = { navController.navigate("config_sucursales") },
                onNavigateToPlan = { navController.navigate("config_plan") },
                onNavigateToUsuarios = { navController.navigate("config_usuarios") },
                onNavigateToMetodosPago = { navController.navigate("config_metodos_pago") },
                onNavigateToPosConfig = { navController.navigate("config_pos") },
                onNavigateToFacturacion = { navController.navigate("facturacion_electronica") },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                }
            )
        }
        composable("config_farmacia") { pantallaConfiguracion() }
        composable("configuracion") { pantallaConfiguracion() }
        composable("config_metodos_pago") { MetodosPagoScreen(onBack = { navController.popBackStack() }) }
        val pantallaPosConfig: @Composable () -> Unit = {
            PosConfigScreen(onBack = { navController.popBackStack() })
        }
        composable("config_pos") { pantallaPosConfig() }
        composable("reglas_negocio") { pantallaPosConfig() }
        composable("hardware") { pantallaPosConfig() }
        
        val pantallaSucursales: @Composable () -> Unit = {
            val vm: SucursalesViewModel = viewModel()
            SucursalesScreen(
                viewModel = vm,
                onVolver = { navController.popBackStack() },
                onIrAConfiguracionFiscal = { navController.navigate("facturacion_electronica") }
            )
        }
        composable("config_sucursales") {
            if (com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalIdEfectiva.equals("principal", true)) {
                pantallaSucursales()
            } else {
                PantallaMensajeBloqueo(
                    titulo = "Acceso restringido",
                    mensaje = "Solo la sede principal puede gestionar sucursales. Desde otra sede el módulo queda oculto."
                )
            }
        }
        composable("sucursales") {
            if (com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.sucursalIdEfectiva.equals("principal", true)) {
                pantallaSucursales()
            } else {
                PantallaMensajeBloqueo(
                    titulo = "Acceso restringido",
                    mensaje = "Solo la sede principal puede gestionar sucursales. Desde otra sede el módulo queda oculto."
                )
            }
        }
        
        val pantallaUsuarios: @Composable () -> Unit = {
            val vm: UsuariosViewModel = viewModel()
            UsuariosScreen(
                viewModel = vm,
                onVolver = { navController.popBackStack() }
            )
        }
        composable("config_usuarios") { pantallaUsuarios() }
        composable("usuarios") { pantallaUsuarios() }
        
        val pantallaPlan: @Composable () -> Unit = {
            // Evolución Visual: GestionPlanScreen (Master-Detail)
            GestionPlanScreen(
                onVolver = { navController.popBackStack() }
            )
        }
        composable("config_plan") { pantallaPlan() }
        composable("plan") { pantallaPlan() }

        // Mapeos de catálogo directamente a pantallas operativas reales (SaaS 2026)
        composable("recetas") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "NUEVA VENTA",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }
        composable("sustancias_controladas") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "NUEVA VENTA",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }
        composable("cupones_campanas") {
            com.app.administradorfarmadon.ventas.ui.PuntoVentaScreen(
                pestanaInicial = "NUEVA VENTA",
                onVolver = { navController.popBackStack() },
                onNavigate = { ruta -> navController.navigate(ruta) }
            )
        }
        composable("portal_proveedores") { pantallaCompras("PROVEEDORES") }
        composable("kardex_valorizacion") { pantallaAnaliticaReportes("REPORTES") }
        composable("finanzas") { pantallaAnaliticaReportes("ANALITICA") }
        composable("estado_servicio") { pantallaSoporte() }
        composable("marca_blanca") { pantallaConfiguracion() }

        // --- MÓDULOS DEL CATÁLOGO (rutas por código canónico) ---
        val modulosPendientes = listOf(
            "notificaciones", "api", "automatizaciones", "marketplace",
            "backups", "observabilidad", "ia_copiloto", "ia_sugeridor",
            "camara_qr", "gestion_documental", "telemedicina", "pagos_embebidos",
            "gamificacion", "reputacion", "rrhh",
            "localizacion", "cumplimiento", "offline", "onboarding",
            "farmacovigilancia", "preparados_magistrales", "adherencia",
            "ocr_documentos"
        )
        modulosPendientes.forEach { code ->
            composable(code) {
                PantallaEnConstruccion(
                    titulo = code.replace("_", " ").replaceFirstChar { it.uppercase() },
                    onVolver = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun PantallaMensajeBloqueo(titulo: String, mensaje: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(TokensFarmadon.colores.fondoBase),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(TokensFarmadon.colores.superficieDefecto, TokensFarmadon.formas.extraGrande)
                    .border(1.dp, TokensFarmadon.colores.estadoAlerta, TokensFarmadon.formas.extraGrande),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = TokensFarmadon.colores.estadoAlerta,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text("SESION FINALIZADA", color = TokensFarmadon.colores.textoPrincipal, style = TokensFarmadon.tipografia.titulo1)

            Text(
                text = titulo,
                color = TokensFarmadon.colores.textoPrincipal,
                style = TokensFarmadon.tipografia.titulo2
            )

            Text(
                text = mensaje,
                color = TokensFarmadon.colores.textoSecundario,
                style = TokensFarmadon.tipografia.cuerpo,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PantallaSuscripcionVencida(
    estadoBloqueo: String,
    motivoBloqueo: String,
    modalidadPausa: String,
    pausadoDesdeTexto: String,
    pausadoHastaTexto: String,
    diasRestantesPausa: Long,
    onVolver: () -> Unit
) {
    val (titulo, subtitulo, mensaje, icono) = when (estadoBloqueo) {
        "PAUSADO" -> {
            val mensajeDetallado = when {
                modalidadPausa == "PROGRAMADA" && pausadoHastaTexto.isNotBlank() ->
                    "Tu farmacia se encuentra en pausa comercial programada${if (pausadoDesdeTexto.isNotBlank()) " desde el $pausadoDesdeTexto" else ""} hasta el $pausadoHastaTexto${if (motivoBloqueo.isNotBlank()) " por motivo: $motivoBloqueo" else ""}. Tus días pagados están protegidos y se reanudarán automáticamente."
                pausadoDesdeTexto.isNotBlank() ->
                    "Tu farmacia se encuentra en pausa comercial desde el $pausadoDesdeTexto${if (motivoBloqueo.isNotBlank()) " ($motivoBloqueo)" else ""}. Tus días pagados están protegidos y se reanudarán al reactivar el servicio."
                else ->
                    "Tu farmacia se encuentra en pausa comercial por solicitud${if (motivoBloqueo.isNotBlank()) " ($motivoBloqueo)" else ""}. Tus días pagados están protegidos y se reanudarán al reactivar el servicio."
            }
            Quadruple(
                "Servicio en Pausa Comercial",
                "Contrato congelado temporalmente",
                mensajeDetallado,
                Icons.Filled.PauseCircle
            )
        }
        "SUSPENDIDO" -> Quadruple(
            "Servicio Suspendido",
            "Pausa administrativa activa",
            if (motivoBloqueo.isNotBlank()) "Tu servicio se encuentra suspendido por regularización ($motivoBloqueo). Contacta a soporte para reactivar el acceso." else "Tu servicio se encuentra en pausa por regularización de pago o gestión administrativa. Contacta a soporte para reactivar el acceso.",
            Icons.Filled.WarningAmber
        )
        else -> Quadruple(
            "Acceso Restringido",
            "Suscripción no vigente",
            "Tu periodo contratado ha finalizado. Renueva tu mensualidad con BRIXO para continuar operando.",
            Icons.Filled.Info
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(TokensFarmadon.colores.fondoBase),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .widthIn(max = 580.dp)
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(TokensFarmadon.colores.superficieDefecto, TokensFarmadon.formas.extraGrande)
                    .border(1.dp, TokensFarmadon.colores.estadoAlerta, TokensFarmadon.formas.extraGrande),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = TokensFarmadon.colores.estadoAlerta,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text("FARMADON", color = TokensFarmadon.colores.textoPrincipal, style = TokensFarmadon.tipografia.titulo1)

            Text(
                text = titulo,
                color = TokensFarmadon.colores.textoPrincipal,
                style = TokensFarmadon.tipografia.titulo2,
                textAlign = TextAlign.Center
            )

            Text(
                text = mensaje,
                color = TokensFarmadon.colores.textoSecundario,
                style = TokensFarmadon.tipografia.cuerpo,
                textAlign = TextAlign.Center
            )

            if (estadoBloqueo == "PAUSADO") {
                Surface(
                    color = TokensFarmadon.colores.superficieDefecto,
                    shape = TokensFarmadon.formas.grande,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TokensFarmadon.colores.bordeDefecto),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = null,
                                tint = TokensFarmadon.colores.textoPrincipal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (modalidadPausa == "PROGRAMADA" && pausadoHastaTexto.isNotBlank())
                                    "Periodo: $pausadoDesdeTexto —” $pausadoHastaTexto"
                                else "Pausado desde: $pausadoDesdeTexto",
                                style = TokensFarmadon.tipografia.titulo3,
                                color = TokensFarmadon.colores.textoPrincipal
                            )
                        }

                        if (modalidadPausa == "PROGRAMADA" && diasRestantesPausa > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = TokensFarmadon.colores.estadoAlerta,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reanudación programada en: $diasRestantesPausa días",
                                    style = TokensFarmadon.tipografia.titulo3,
                                    color = TokensFarmadon.colores.estadoAlerta
                                )
                            }
                        }

                        Text(
                            text = "Tus días contratados están congelados y no se consumen mientras dure la pausa.",
                            style = TokensFarmadon.tipografia.cuerpoPequeno,
                            color = TokensFarmadon.colores.textoSecundario
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        onVolver()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TokensFarmadon.colores.textoPrincipal)
                ) {
                    Text("Cerrar Sesión")
                }

                Button(
                    onClick = { com.app.administradorfarmadon.suscripcion.ReportarPagoManager.abrirDialogoManual() },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF3B82F6))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reportar Pago / Subir Voucher")
                }

                Button(
                    onClick = onVolver,
                    colors = ButtonDefaults.buttonColors(containerColor = SidebarTheme.Accent)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Verificar Estado")
                }
            }
        }
    }
}

private data class Quadruple<T1, T2, T3, T4>(
    val first: T1,
    val second: T2,
    val third: T3,
    val fourth: T4
)

@Composable
private fun PantallaNoCompatible() {
    Box(
        modifier = Modifier.fillMaxSize().background(TokensFarmadon.colores.fondoBase),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(TokensFarmadon.colores.superficieDefecto, TokensFarmadon.formas.extraGrande)
                    .border(1.dp, TokensFarmadon.colores.bordeDefecto, TokensFarmadon.formas.extraGrande),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Tablet,
                    contentDescription = null,
                    tint = TokensFarmadon.colores.textoPrincipal,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text("FARMADON", color = TokensFarmadon.colores.textoPrincipal, style = TokensFarmadon.tipografia.titulo1)

            Text(
                text = "Esta aplicación está diseñada\nexclusivamente para tablets.",
                color = TokensFarmadon.colores.textoSecundario,
                style = TokensFarmadon.tipografia.cuerpo,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BannerConexionGlobal(
    status: NetworkStatus,
    ultimaConexionMs: Long
) {
    // Solo se muestra cuando REALMENTE no hay conexión física (DESCONECTADO)
    // o el WiFi/red no tiene salida a internet (SIN_SALIDA).
    // Si la conexión es inestable, lenta o degradada, NO se molesta al usuario.
    val esVisible = status == NetworkStatus.DESCONECTADO || status == NetworkStatus.SIN_SALIDA

    AnimatedVisibility(
        visible = esVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        val horaFormateada = remember(ultimaConexionMs) {
            val ms = if (ultimaConexionMs > 0L) ultimaConexionMs else System.currentTimeMillis()
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
        }

        val (colorPunto, textoBanner) = when (status) {
            NetworkStatus.DESCONECTADO ->
                Color(0xFFEF4444) to "Sin conexión — datos de $horaFormateada, no son actuales"
            NetworkStatus.SIN_SALIDA ->
                Color(0xFFF59E0B) to "Con wifi pero sin megas — datos de $horaFormateada"
            else ->
                Color(0xFFEF4444) to ""
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            color = if (status == NetworkStatus.DESCONECTADO)
                Color(0xFF450A0A)
            else
                Color(0xFF451A03),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colorPunto)
                    )
                    Text(
                        text = textoBanner,
                        style = TokensFarmadon.tipografia.cuerpoPequeno.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}
