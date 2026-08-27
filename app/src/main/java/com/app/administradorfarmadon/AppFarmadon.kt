package com.app.administradorfarmadon

import android.app.Application
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.LocalDraftManager
import com.app.administradorfarmadon.appconexioninternet.NetworkHealthMonitor
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.notificaciones.NotificationChannels
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

        appScope.launch {
            SessionManager.init(this@AppFarmadon)
            NetworkHealthMonitor.init(this@AppFarmadon)
            LocalDraftManager.init(this@AppFarmadon)
        }
    }
}
