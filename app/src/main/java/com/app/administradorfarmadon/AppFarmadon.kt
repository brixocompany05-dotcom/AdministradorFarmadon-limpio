package com.app.administradorfarmadon

import android.app.Application
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.appconexioninternet.NetworkHealthMonitor
import com.app.administradorfarmadon.appconexioninternet.NetworkStatus
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.logica.RelojServidorSincronizador
import com.app.administradorfarmadon.notificaciones.NotificationChannels
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AppFarmadon : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Inicializar OSMDroid Configuration
        Configuration.getInstance().userAgentValue = packageName

        // Configurar la caché local de Firestore UNA sola vez, antes de cualquier uso real.
        // FarmadonFirestore.db aplica el interruptor oculto (persistente vs memoria) y es
        // la única fuente de verdad que usan todas las clases que tocan Firebase.
        FarmadonFirestore.db

        // Firebase Persistence debe activarse ANTES de cualquier otra llamada a la DB
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            android.util.Log.e("AppFarmadon", "persistencia offline falló", e)
        }

        // Inicializar Canales de Notificación
        NotificationChannels.createNotificationChannels(this)

        // Inicializar SharedPreferences esenciales de forma inmediata y sincrónica
        SessionManager.init(this)
        com.app.administradorfarmadon.ventas.compartido.datos.VentaBorradorLocalStore.init(this)
        com.app.administradorfarmadon.configuracion.preferencias_sistema.teclado.datos.TecladoPrefs.init(this)
        com.app.administradorfarmadon.configuracion.preferencias_sistema.impresion.datos.ImpresionPrefs.init(this)
        com.app.administradorfarmadon.configuracion.preferencias_sistema.ux.datos.UxPrefs.init(this)

        appScope.launch {
            // Calibrar el reloj del servidor lo antes posible (al arranque de la app,
            // no solo al abrir Inventario/Menú). Restaura el último offset bueno y luego
            // reintenta medir contra Firestore; si no hay red, queda el último conocido.
            RelojServidorSincronizador.cargar(this@AppFarmadon)
            RelojServidorSincronizador.sincronizar(this@AppFarmadon)
            NetworkHealthMonitor.init(this@AppFarmadon)
        }

        // El reloj del servidor se re-sincroniza cada vez que el cortafuego reporta
        // conexión plena (CONECTADO). Así la tablet "siempre habla con el servidor"
        // en cuanto tiene internet, y nunca se queda con un offset viejo en silencio.
        appScope.launch {
            var prevConectado = false
            NetworkHealthMonitor.status.collect { status ->
                val conectado = status == NetworkStatus.CONECTADO
                if (conectado && !prevConectado) {
                    RelojServidorSincronizador.sincronizar(this@AppFarmadon)
                }
                prevConectado = conectado
            }
        }
    }
}
