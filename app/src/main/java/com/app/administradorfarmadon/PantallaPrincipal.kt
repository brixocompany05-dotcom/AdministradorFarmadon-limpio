package com.app.administradorfarmadon
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.app.administradorfarmadon.configuracion.preferencias_sistema.teclado.ui.AplicarBloqueoTecladoVentana
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.app.administradorfarmadon.autenticacion.navegacion.AuthNavGraph
import com.app.administradorfarmadon.autenticacion.login.logica.LoginViewModel
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.appconexioninternet.NetworkHealthMonitor
import com.app.administradorfarmadon.appconexioninternet.NetworkStatus
import com.app.administradorfarmadon.autenticacion.datos.AuthPaths
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.navegacion.ContenedorAplicacion
import com.app.administradorfarmadon.navegacion.sidebar.SidebarTheme
import com.app.administradorfarmadon.disenotemaapp.ui.ThemeViewModel
import com.app.administradorfarmadon.disenotemaapp.ui.FarmadonAppTheme
import com.app.administradorfarmadon.disenotemaapp.ui.FDColors
import com.app.administradorfarmadon.disenotemaapp.ui.tokens.TokensFarmadon
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseNetworkException
import androidx.camera.core.ExperimentalGetImage
import com.app.administradorfarmadon.suscripcion.ReportarPagoManager
import com.app.administradorfarmadon.suscripcion.ui.ReportarPagoDialog
import kotlinx.coroutines.tasks.await

/**
 * Estados de Navegación de la Aplicación (Single-Activity Architecture).
 */
sealed class EstadoApp {
    data object Autenticado : EstadoApp()
    data object NoAutenticado : EstadoApp()
    data object Verificando : EstadoApp()
    /** La verificación de sesión no pudo ejecutarse porque el dispositivo no tiene salida real a internet. */
    data object SinConexion : EstadoApp()
    /** La cuenta fue aprobada pero sus datos aún no terminan de sincronizar en Firestore. */
    data object Sincronizando : EstadoApp()

    companion object {
        /**
         * Traduce el estado REAL de red medido por NetworkHealthMonitor a una verdad
         * para el usuario. Devuelve null si la situación NO es "sin conexión"
         * (conexión lenta o central degradada pero con salida → se espera a Firebase,
         * R11: Firebase decide la conectividad, la app no se rinde antes de tiempo).
         */
        fun desdeEstadoRed(status: NetworkStatus): EstadoApp? =
            when (status) {
                NetworkStatus.DESCONECTADO -> SinConexion
                NetworkStatus.SIN_SALIDA -> SinConexion
                else -> null
            }
    }
}

/**
 * PANTALLA PRINCIPAL — Única Activity de Farmadon (Arquitectura 1+1=2).
 *
 * Flujo Maestro:
 * 1. Muestra el Splash de bienvenida durante el tiempo de aprecio (2 segundos).
 * 2. En paralelo, verifica si hay sesión activa en Firebase Auth y valida el acceso en Firestore.
 * 3. Si la sesión es válida → Muestra el Sidebar + Inventario (ContenedorAplicacion).
 * 4. Si no hay sesión o fue revocada → Muestra la pantalla de Login (AuthNavGraph).
 * 5. Al iniciar sesión o cerrar sesión → Alterna el estado en la misma ventana sin saltos de Activity.
 */
class PantallaPrincipal : AppCompatActivity() {

    companion object {
        private const val TAG = "PantallaPrincipal"
    }

    private val themeViewModel: ThemeViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ReportarPagoManager.capturarIntent(intent)

        setContent {
            @OptIn(ExperimentalGetImage::class)
            @Composable
            fun RootContent() {
                val esTemaOscuro by themeViewModel.esTemaOscuro.collectAsState()
                var estadoApp by remember { mutableStateOf<EstadoApp>(EstadoApp.Verificando) }
                val uiLoginState by loginViewModel.uiState.collectAsState()

                var reintentoTrigger by remember { mutableIntStateOf(0) }

                // Sincronizar tema con la barra del sistema y el SidebarTheme
                SidebarTheme.isDark = esTemaOscuro

                // Bloqueo GLOBAL de teclado (regla "Bloquear Teclado" / Modo Escáner Externo):
                // se aplica a la ventana principal y a cada diálogo con campos, sin excepciones.
                AplicarBloqueoTecladoVentana()

                LaunchedEffect(esTemaOscuro) {
                    val color = if (esTemaOscuro) Color.BLACK else Color.rgb(242, 242, 247)
                    window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(color))
                    window.statusBarColor = Color.TRANSPARENT
                    window.navigationBarColor = Color.TRANSPARENT
                    
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !esTemaOscuro
                        isAppearanceLightNavigationBars = !esTemaOscuro
                    }
                }

                // Sincronización al completar login desde el formulario
                LaunchedEffect(uiLoginState.loginExitoso) {
                    if (uiLoginState.loginExitoso) {
                        estadoApp = EstadoApp.Autenticado
                        loginViewModel.clearLoginSuccess()
                    }
                }

                // Validación rápida al entrar: misma puerta que el login (una regla, un solo lugar).
                LaunchedEffect(reintentoTrigger) {
                    val auth = FirebaseAuth.getInstance()
                    val currentUser = auth.currentUser

                    if (currentUser != null) {
                        try {
                            val db = FarmadonFirestore.db
                            val userDoc = AuthPaths.usuariosFarmacia(db).document(currentUser.uid).get().await()

                            if (userDoc.exists()) {
                                when (val resultado = loginViewModel.verificarAccesoPostLogin(currentUser.uid, userDoc, currentUser.email ?: "")) {
                                    is com.app.administradorfarmadon.autenticacion.login.logica.LoginViewModel.ResultadoVerificacionPostLogin.Ok -> {
                                        SessionManager.guardarSesion(
                                            context = this@PantallaPrincipal,
                                            id = currentUser.uid,
                                            nombre = resultado.nombre,
                                            rolUsuario = resultado.rol,
                                            tenantId = resultado.clienteId,
                                            sedeId = resultado.sucursalId,
                                            sedeNombre = resultado.sucursalNombre
                                        )
                                        SessionManager.email = resultado.email
                                        SessionManager.dni = userDoc.getString("dni") ?: ""
                                        estadoApp = EstadoApp.Autenticado
                                    }
                                    is com.app.administradorfarmadon.autenticacion.login.logica.LoginViewModel.ResultadoVerificacionPostLogin.AccesoRevocado -> {
                                        auth.signOut()
                                        SessionManager.limpiarSesion(this@PantallaPrincipal)
                                        loginViewModel.mostrarAccesoSuspendido(
                                            if (userDoc.getBoolean("dadoDeBaja") == true)
                                                "Tu ficha fue dada de baja por la administración de la farmacia."
                                            else
                                                "Tu acceso fue suspendido por la administración de la farmacia. Contacta a la administración para reactivarlo."
                                        )
                                        estadoApp = EstadoApp.NoAutenticado
                                    }
                                    is com.app.administradorfarmadon.autenticacion.login.logica.LoginViewModel.ResultadoVerificacionPostLogin.PerfilEnProceso -> {
                                        estadoApp = EstadoApp.Sincronizando
                                    }
                                    is com.app.administradorfarmadon.autenticacion.login.logica.LoginViewModel.ResultadoVerificacionPostLogin.FallaComunicada -> {
                                        auth.signOut()
                                        SessionManager.limpiarSesion(this@PantallaPrincipal)
                                        estadoApp = EstadoApp.NoAutenticado
                                    }
                                }
                            } else {
                                auth.signOut()
                                SessionManager.limpiarSesion(this@PantallaPrincipal)
                                estadoApp = EstadoApp.NoAutenticado
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "[AUTH] Verificación fallida por red: ${e.message}")
                            val pendientePorRed = EstadoApp.desdeEstadoRed(
                                NetworkHealthMonitor.status.value
                            ) == EstadoApp.SinConexion
                            estadoApp = if (pendientePorRed) EstadoApp.SinConexion else EstadoApp.NoAutenticado
                        }
                    } else {
                        estadoApp = EstadoApp.NoAutenticado
                    }
                }

                // Vigía de reconexión: si la verificación quedó pendiente por red y el
                // monitor reporta salida real a internet, se relanza sola. El usuario
                // no toca nada — la app misma completa lo que quedó a medias.
                LaunchedEffect(Unit) {
                    NetworkHealthMonitor.status.collect { status ->
                        if (EstadoApp.desdeEstadoRed(status) == null &&
                            estadoApp == EstadoApp.SinConexion
                        ) {
                            reintentoTrigger++
                        }
                    }
                }

                // Vigilante en tiempo real del estado de acceso y suspensión inmediata
                DisposableEffect(estadoApp) {
                    if (estadoApp != EstadoApp.Autenticado) {
                        return@DisposableEffect onDispose { }
                    }

                    val auth = FirebaseAuth.getInstance()
                    val currentUser = auth.currentUser
                    val uid = currentUser?.uid

                    if (uid == null) {
                        estadoApp = EstadoApp.NoAutenticado
                        return@DisposableEffect onDispose { }
                    }

                    val db = FarmadonFirestore.db
                    val userRef = AuthPaths.usuariosFarmacia(db).document(uid)
                    val farmaciaCol = FarmadonPaths.farmacias(db)

                    var farmaciaListener: com.google.firebase.firestore.ListenerRegistration? = null

                    fun escucharFarmacia(clienteId: String) {
                        farmaciaListener?.remove()
                        if (clienteId.isBlank()) return
                        farmaciaListener = farmaciaCol.document(clienteId).addSnapshotListener { fSnap, error ->
                            if (error != null) return@addSnapshotListener
                            if (fSnap != null && fSnap.exists()) {
                                if (fSnap.getString("estado") == "suspendido") {
                                    Log.w(TAG, "[VIGILANTE] Farmacia $clienteId suspendida en tiempo real por BRIXO")
                                    auth.signOut()
                                    SessionManager.limpiarSesion(this@PantallaPrincipal)
                                    loginViewModel.mostrarAccesoSuspendido("La suscripción de la farmacia fue suspendida. Contacta a soporte para reactivarla.")
                                    estadoApp = EstadoApp.NoAutenticado
                                }
                            }
                        }
                    }

                    val initialClienteId = SessionManager.clienteId
                    if (initialClienteId.isNotBlank()) {
                        escucharFarmacia(initialClienteId)
                    }

                    val userListener = userRef.addSnapshotListener { userSnap, error ->
                        if (error != null) return@addSnapshotListener
                        if (userSnap != null && userSnap.exists()) {
                            val acceso = userSnap.getBoolean("acceso") ?: true
                            val clienteId = userSnap.getString("clienteId") ?: ""

                            if (!acceso) {
                                Log.w(TAG, "[VIGILANTE] Acceso revocado en tiempo real para $uid")
                                auth.signOut()
                                SessionManager.limpiarSesion(this@PantallaPrincipal)
                                if (clienteId.isNotBlank()) {
                                    loginViewModel.mostrarAccesoSuspendido(
                                        if (userSnap.getBoolean("dadoDeBaja") == true)
                                            "Tu ficha fue dada de baja por la administración de la farmacia."
                                        else
                                            "Tu acceso fue suspendido por la administración de la farmacia. Contacta a la administración para reactivarlo."
                                    )
                                } else {
                                    loginViewModel.volverALogin()
                                }
                                estadoApp = EstadoApp.NoAutenticado
                                return@addSnapshotListener
                            }

                            if (clienteId.isNotBlank()) {
                                escucharFarmacia(clienteId)
                            }
                        }
                    }

                    onDispose {
                        userListener.remove()
                        farmaciaListener?.remove()
                    }
                }

                FarmadonAppTheme(darkTheme = esTemaOscuro) {
                    when (estadoApp) {
                        EstadoApp.Verificando -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(TokensFarmadon.colores.fondoBase),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.splash_background),
                                    contentDescription = "Farmadon Splash",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        EstadoApp.Sincronizando -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(TokensFarmadon.colores.fondoBase),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(color = FDColors.Primary)
                                    Text(
                                        text = "Sincronizando datos...",
                                        style = TokensFarmadon.tipografia.cuerpo,
                                        color = TokensFarmadon.colores.textoPrincipal
                                    )
                                }
                            }
                        }
                        EstadoApp.SinConexion -> {
                            PantallaSinConexionVerdad(
                                themeViewModel = themeViewModel
                            )
                        }
                        EstadoApp.NoAutenticado -> {
                            AuthNavGraph(
                                loginViewModel = loginViewModel,
                                themeViewModel = themeViewModel,
                                onRegistroExitoso = {
                                    loginViewModel.volverALogin()
                                }
                            )
                        }
                        EstadoApp.Autenticado -> {
                            // Diálogo de calibración para primera ejecución
                            val context = LocalContext.current
                            val prefs = remember { context.getSharedPreferences("FarmadonPrefs", Context.MODE_PRIVATE) }
                            var showConfirmation by remember { 
                                mutableStateOf(!prefs.getBoolean("scale_confirmation_shown", false)) 
                            }

                            if (showConfirmation) {
                                AlertDialog(
                                    onDismissRequest = { },
                                    containerColor = TokensFarmadon.colores.superficieElevada,
                                    titleContentColor = TokensFarmadon.colores.textoPrincipal,
                                    textContentColor = TokensFarmadon.colores.textoSecundario,
                                    title = { Text("Optimización de Interfaz", style = TokensFarmadon.tipografia.titulo3) },
                                    text = {
                                        Text(
                                            "Hemos calibrado la densidad visual para tu tablet. " +
                                            "Esto garantiza mayor velocidad y legibilidad en el mostrador.",
                                            style = TokensFarmadon.tipografia.cuerpo
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                prefs.edit().putBoolean("scale_confirmation_shown", true).apply()
                                                showConfirmation = false
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = TokensFarmadon.colores.estadoExito,
                                                contentColor = ComposeColor.Black
                                            )
                                        ) {
                                            Text("ENTENDIDO", style = TokensFarmadon.tipografia.etiqueta)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                prefs.edit().putBoolean("scale_confirmation_shown", true).apply()
                                                showConfirmation = false
                                            }
                                        ) {
                                            Text("CONFIGURAR LUEGO", style = TokensFarmadon.tipografia.etiqueta, color = TokensFarmadon.colores.textoTerciario)
                                        }
                                    }
                                )
                            }

                            ContenedorAplicacion(
                                themeViewModel = themeViewModel,
                                onLogout = {
                                    FirebaseAuth.getInstance().signOut()
                                    SessionManager.limpiarSesion(this@PantallaPrincipal)
                                    loginViewModel.volverALogin()
                                    estadoApp = EstadoApp.NoAutenticado
                                }
                            )
                        }
                    }

                    // Diálogo de Reportar Pago (Nativo o Compartido desde Yape/Banca Móvil)
                    val comprobanteCompartido by ReportarPagoManager.comprobanteCompartido.collectAsState()
                    val mostrarDialogoManual by ReportarPagoManager.mostrarDialogoManual.collectAsState()

                    if (estadoApp == EstadoApp.Autenticado && (comprobanteCompartido != null || mostrarDialogoManual)) {
                        ReportarPagoDialog(
                            dataCompartida = comprobanteCompartido,
                            onDismiss = {
                                ReportarPagoManager.cerrarDialogo()
                            }
                        )
                    }
                }
            }
            RootContent()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ReportarPagoManager.capturarIntent(intent)
    }
}

/**
 * PANTALLA SIN CONEXIÓN (verdad completa, no un cartel).
 *
 * Aparece SOLO cuando el monitor de red confirma que el dispositivo no tiene
 * salida real a internet (DESCONECTADO = sin wifi/datos, SIN_SALIDA = hay señal
 * pero sin datos del operador). Dice la causa verdadera y NO ofrece botones:
 * cuando la conexión vuelve, la app sola relanza la verificación de sesión.
 * No es una pantalla de login disfrazada: aquí nadie "no es bienvenido",
 * simplemente todavía no se pudo preguntar (R9 — la verdad real, siempre).
 */
@Composable
private fun PantallaSinConexionVerdad(
    themeViewModel: ThemeViewModel
) {
    val esTemaOscuro by themeViewModel.esTemaOscuro.collectAsState()
    val status by NetworkHealthMonitor.status.collectAsState()

    // La causa mostrada SIEMPRE viene del estado vivo del monitor: si el usuario
    // apaga el wifi mientras mira la pantalla, el texto cambia con la verdad.
    val (titulo, mensaje) = when (status) {
        NetworkStatus.SIN_SALIDA ->
            "Sin internet en tu tablet" to
                    "Tu tablet está conectada a una red, pero la red no tiene salida a internet. " +
                    "Revisa los datos de tu plan o el router. Farmadon continuará automáticamente " +
                    "cuando la conexión vuelva."
        else ->
            "Sin conexión a internet" to
                    "Tu tablet no tiene conexión a internet en este momento. Farmadon continuará " +
                    "automáticamente cuando la conexión vuelva."
    }

    FarmadonAppTheme(darkTheme = esTemaOscuro) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = TokensFarmadon.colores.fondoBase
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.systemBars.asPaddingValues()),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(horizontal = 48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                color = FDColors.Error.copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (status == NetworkStatus.SIN_SALIDA)
                                Icons.Default.CloudOff else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = FDColors.Error,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        text = titulo,
                        style = TokensFarmadon.tipografia.titulo1,
                        color = TokensFarmadon.colores.textoPrincipal,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = mensaje,
                        style = TokensFarmadon.tipografia.cuerpo,
                        color = TokensFarmadon.colores.textoSecundario,
                        textAlign = TextAlign.Center
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = FDColors.Primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Esperando la conexión…",
                            style = TokensFarmadon.tipografia.etiqueta,
                            color = TokensFarmadon.colores.textoTerciario
                        )
                    }
                }
            }
        }
    }
}


