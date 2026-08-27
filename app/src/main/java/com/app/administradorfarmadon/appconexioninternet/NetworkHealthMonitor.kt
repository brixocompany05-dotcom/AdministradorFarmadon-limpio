package com.app.administradorfarmadon.appconexioninternet
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Enterprise Network Lifecycle Monitor (v2026 - Senior Edition).
 */
enum class NetworkStatus {
    /** Hardware conectado, internet validado y servidor de datos (Firebase) operativo. */
    CONECTADO,
    /** Hay internet general, pero el túnel hacia Firebase está cerrado o bloqueado. */
    ESTADO_DEGRADADO,
    /** Latencia 300ms - 2000ms: Operación lenta. */
    CONEXION_LENTA,
    /** El dispositivo está conectado a una red (WiFi/LTE) pero no tiene salida al exterior. */
    SIN_SALIDA,
    /** Sin hardware de red activo. */
    DESCONECTADO
}

object NetworkHealthMonitor {

    private val _status = MutableStateFlow(NetworkStatus.CONECTADO)
    val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var hardwareConnected = false
    private var lastLatency = -1L
    private var firebaseConnected = false
    private var connectivityManager: ConnectivityManager? = null

    /**
     * Inicializa el monitor. Lógica reactiva basada en flows y validación activa.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun init(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager = cm
        
        // 1. Capa Física & Transporte: Observar cambios y validar salida real
        monitorScope.launch {
            observeHardwareChanges(context)
                .flatMapLatest { hasHardware ->
                    hardwareConnected = hasHardware
                    if (hasHardware) {
                        // Si hay hardware, validamos salida a internet real cada 10s
                        tickerFlow(10000L).map { 
                            val startTime = System.currentTimeMillis()
                            val reached = checkReachability()
                            lastLatency = if (reached) System.currentTimeMillis() - startTime else -1L
                            reached
                        }
                    } else {
                        flowOf(false)
                    }
                }
                .collect { hasInternet ->
                    updateGlobalStatus(hasInternet)
                }
        }

        // 2. Capa de Aplicación: Ping periódico a Firebase
        monitorScope.launch {
            // Inicializar el documento de ping al arrancar
            try {
                FarmadonFirestore.db
                    .collection("_health")
                    .document("ping")
                    .set(mapOf("ultimoArranque" to System.currentTimeMillis()))
            } catch (e: Exception) {
                android.util.Log.e("NetworkHealth", "ping inicial _health falló", e)
                firebaseConnected = false
                _status.value = NetworkStatus.ESTADO_DEGRADADO
            }

            while (true) {
                // El probe HTTP/TCP es la verdad de fondo: aunque el callback de
                // hardware llegue tarde (o falle), un probe exitoso evita que el
                // estado se congele en DESCONECTADO con internet real disponible.
                val reachable = checkReachability()
                firebaseConnected = if (reachable) checkFirestoreConnectivity() else false
                if (!reachable) lastLatency = -1L
                updateGlobalStatus(reachable)
                delay(30_000L) // Ping cada 30 segundos
            }
        }
    }

    private fun observeHardwareChanges(context: Context) = callbackFlow<Boolean> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        cm.registerNetworkCallback(request, callback)
        
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        trySend(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)

        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    /**
     * Validación de Capa de Transporte (TCP Reachability).
     * Prueba varios endpoints conocidos: muchos ISP/redes corporativas filtran
     * el puerto 53 hacia un solo destino, así que un solo socket era un falso
     * "sin internet" recurrente. Basta con que UNO conecte para dar por buena
     * la salida a internet.
     */
    private val proEndpoints = listOf(
        InetSocketAddress("8.8.8.8", 53),
        InetSocketAddress("1.1.1.1", 443),
        InetSocketAddress("208.67.222.222", 53)
    )

    private suspend fun checkReachability(): Boolean = withContext(Dispatchers.IO) {
        for (endpoint in proEndpoints) {
            try {
                Socket().use { socket ->
                    socket.connect(endpoint, 2000)
                    return@withContext true
                }
            } catch (e: Exception) {
                android.util.Log.d("NetworkHealth", "endpoint $endpoint falló, probando siguiente", e)
            }
        }
        false
    }

    /**
     * Validación de Capa de Aplicación (Firestore Connectivity).
     * Realiza un get() ligero al servidor para confirmar que el SDK puede hablar con Firestore.
     */
    private suspend fun checkFirestoreConnectivity(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Intentamos leer un documento de control (o cualquier documento ligero) directamente del servidor
            FarmadonFirestore.db
                .collection("_health")
                .document("ping")
                .get(Source.SERVER)
                .await()
            true
        } catch (e: Exception) {
            android.util.Log.e("NetworkHealth", "Firestore ping falló", e)
            false
        }
    }

    private fun updateGlobalStatus(hasInternet: Boolean) {
        _status.update { 
            when {
                // DESCONECTADO solo es real si NO hay salida Y el hardware tampoco
                // confirma. Si el probe de transporte funciona pero el callback de
                // hardware aún no emitió, seguimos en estados conectados para no
                // bloquear operaciones con internet real.
                !hasInternet && !hardwareConnected -> NetworkStatus.DESCONECTADO
                !hasInternet -> NetworkStatus.SIN_SALIDA
                !firebaseConnected -> NetworkStatus.ESTADO_DEGRADADO
                lastLatency in 1L..300L -> NetworkStatus.CONECTADO
                lastLatency > 300L -> NetworkStatus.CONEXION_LENTA
                else -> NetworkStatus.CONECTADO
            }
        }
    }

    private fun tickerFlow(period: Long) = flow {
        while (true) {
            emit(Unit)
            delay(period)
        }
    }

    fun isOnline(): Boolean = _status.value == NetworkStatus.CONECTADO
}
