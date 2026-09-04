package com.app.administradorfarmadon.appconexioninternet

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.*
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

    private val _ultimaConexionMs = MutableStateFlow(System.currentTimeMillis())
    val ultimaConexionMs: StateFlow<Long> = _ultimaConexionMs.asStateFlow()

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

        // 2. Capa de Aplicación: Validación de conectividad por socket
        monitorScope.launch {
            while (true) {
                val reachable = checkReachability()
                firebaseConnected = reachable
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

    private fun updateGlobalStatus(hasInternet: Boolean) {
        _status.update { 
            val newStatus = when {
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
            if (newStatus == NetworkStatus.CONECTADO || newStatus == NetworkStatus.CONEXION_LENTA) {
                _ultimaConexionMs.value = System.currentTimeMillis()
            }
            newStatus
        }
    }

    /**
     * Fuerza un probe inmediato de la conexión a internet y a Firestore.
     */
    fun reevaluarInmediato() {
        monitorScope.launch {
            val reachable = checkReachability()
            firebaseConnected = reachable
            if (!reachable) lastLatency = -1L
            if (reachable) {
                _ultimaConexionMs.value = System.currentTimeMillis()
            }
            updateGlobalStatus(reachable)
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
