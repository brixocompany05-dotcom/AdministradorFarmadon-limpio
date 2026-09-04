package com.app.administradorfarmadon.facturacion.configuracion.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.facturacion.envio.datos.ApisunatConfig
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Modelo inmutable del Emisor Fiscal de la Farmacia.
 * Representa al contribuyente fiscal único (un solo RUC por farmacia).
 */
data class EmisorFiscal(
    val ruc: String = "",
    val razonSocial: String = "",
    val direccionFiscal: String = "",
    val personaId: String = "",
    val personaToken: String = "",
    val modo: String = MODO_DESARROLLO,
    val verificadoOk: Boolean = false,
    val ultimoError: String = "",
    val actualizadoPor: String = "",
    val actualizadoEl: Any? = null
) {
    companion object {
        const val MODO_DESARROLLO = "DESARROLLO"
        const val MODO_PRODUCCION = "PRODUCCION"
    }

    /**
     * Regla 3: Estado derivado, nunca declarado ni manipulable manualmente.
     * Completa = RUC de 11 dígitos numéricos + datos comerciales + credenciales APISUNAT + ping real verificado.
     */
    val estaCompleta: Boolean
        get() = ruc.trim().length == 11 &&
                ruc.trim().all { it.isDigit() } &&
                razonSocial.isNotBlank() &&
                direccionFiscal.isNotBlank() &&
                personaId.isNotBlank() &&
                personaToken.isNotBlank() &&
                verificadoOk

    /**
     * Lista de campos faltantes para retroalimentación honesta en UI.
     */
    fun camposFaltantes(): List<String> {
        val faltantes = mutableListOf<String>()
        if (ruc.trim().length != 11 || !ruc.trim().all { it.isDigit() }) faltantes.add("RUC (11 dígitos)")
        if (razonSocial.isBlank()) faltantes.add("Razón Social")
        if (direccionFiscal.isBlank()) faltantes.add("Dirección Fiscal")
        if (personaId.isBlank()) faltantes.add("Persona ID (APISUNAT)")
        if (personaToken.isBlank()) faltantes.add("Token (APISUNAT)")
        if (!verificadoOk) faltantes.add("Verificación de conexión con APISUNAT")
        return faltantes
    }
}

/**
 * Un campo que cambió en el emisor (auditoría). El token jamás se guarda
 * completo aquí: solo máscara (**** + últimos 4).
 */
data class DetalleCambioEmisor(
    val campo: String = "",
    val antes: String = "",
    val despues: String = ""
)

/**
 * Acta append-only de un guardado del emisor: qué cambió, quién, cuándo.
 * Vive en facturacion_config/emisor/historial/{autoId}.
 */
data class ActaCambioEmisor(
    val id: String = "",
    val fechaMs: Long = 0L,
    val fechaLegible: String = "",
    val usuarioEmail: String = "",
    val usuarioNombre: String = "",
    val sedeId: String = "principal",
    val resultado: String = "", // VERIFICADO | FALLO_VERIFICACION
    val verificadoOk: Boolean = false,
    val ultimoError: String = "",
    val cambios: List<DetalleCambioEmisor> = emptyList()
)

class FacturacionConfigRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db
) {
    companion object {
        private const val TAG = "FacturacionConfigRepo"
        private const val APISUNAT_LAST_DOCUMENT_URL = ApisunatConfig.LAST_DOCUMENT_URL
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Escucha viva de los datos del emisor fiscal (Regla R8 - Verdad Vigente).
     */
    fun observarEmisor(farmaciaId: String): Flow<EmisorFiscal?> = callbackFlow {
        if (farmaciaId.isBlank()) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }

        val ref = FarmadonPaths.facturacionEmisor(db, farmaciaId)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando emisor fiscal: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val data = snapshot.data ?: emptyMap<String, Any?>()
                val emisor = EmisorFiscal(
                    ruc = (data["ruc"] as? String)?.trim().orEmpty(),
                    razonSocial = (data["razonSocial"] as? String)?.trim().orEmpty(),
                    direccionFiscal = (data["direccionFiscal"] as? String)?.trim().orEmpty(),
                    personaId = (data["personaId"] as? String)?.trim().orEmpty(),
                    personaToken = (data["personaToken"] as? String)?.trim().orEmpty(),
                    modo = (data["modo"] as? String)?.trim()?.takeIf { it == EmisorFiscal.MODO_PRODUCCION } ?: EmisorFiscal.MODO_DESARROLLO,
                    verificadoOk = (data["verificadoOk"] as? Boolean) ?: false,
                    ultimoError = (data["ultimoError"] as? String)?.trim().orEmpty(),
                    actualizadoPor = (data["actualizadoPor"] as? String)?.trim().orEmpty(),
                    actualizadoEl = data["actualizadoEl"]
                )
                trySend(emisor)
            } else {
                trySend(null)
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Obtiene una lectura puntual del emisor fiscal.
     */
    suspend fun obtenerEmisor(farmaciaId: String): EmisorFiscal? {
        if (farmaciaId.isBlank()) return null
        val snap = FarmadonPaths.facturacionEmisor(db, farmaciaId).get().await()
        if (!snap.exists()) return null
        val data = snap.data ?: return null
        return EmisorFiscal(
            ruc = (data["ruc"] as? String)?.trim().orEmpty(),
            razonSocial = (data["razonSocial"] as? String)?.trim().orEmpty(),
            direccionFiscal = (data["direccionFiscal"] as? String)?.trim().orEmpty(),
            personaId = (data["personaId"] as? String)?.trim().orEmpty(),
            personaToken = (data["personaToken"] as? String)?.trim().orEmpty(),
            modo = (data["modo"] as? String)?.trim()?.takeIf { it == EmisorFiscal.MODO_PRODUCCION } ?: EmisorFiscal.MODO_DESARROLLO,
            verificadoOk = (data["verificadoOk"] as? Boolean) ?: false,
            ultimoError = (data["ultimoError"] as? String)?.trim().orEmpty(),
            actualizadoPor = (data["actualizadoPor"] as? String)?.trim().orEmpty(),
            actualizadoEl = data["actualizadoEl"]
        )
    }

    /**
     * Compara el emisor anterior con el nuevo y devuelve la lista de cambios.
     * Función pura (sin Firebase) para que el cálculo sea testeable y exacto.
     * El token se compara por presencia y se registra enmascarado, jamás completo.
     */
    fun calcularCambiosEmisor(antes: EmisorFiscal?, despues: EmisorFiscal): List<DetalleCambioEmisor> {
        if (antes == null) {
            return listOf(
                DetalleCambioEmisor("RUC", "", despues.ruc.trim()),
                DetalleCambioEmisor("Razón Social", "", despues.razonSocial.trim()),
                DetalleCambioEmisor("Dirección Fiscal", "", despues.direccionFiscal.trim()),
                DetalleCambioEmisor("Persona ID", "", despues.personaId.trim()),
                DetalleCambioEmisor("Persona Token", "", enmascararToken(despues.personaToken)),
                DetalleCambioEmisor("Modo", "", despues.modo)
            )
        }
        val cambios = mutableListOf<DetalleCambioEmisor>()
        if (antes.ruc.trim() != despues.ruc.trim()) {
            cambios.add(DetalleCambioEmisor("RUC", antes.ruc.trim(), despues.ruc.trim()))
        }
        if (antes.razonSocial.trim() != despues.razonSocial.trim()) {
            cambios.add(DetalleCambioEmisor("Razón Social", antes.razonSocial.trim(), despues.razonSocial.trim()))
        }
        if (antes.direccionFiscal.trim() != despues.direccionFiscal.trim()) {
            cambios.add(DetalleCambioEmisor("Dirección Fiscal", antes.direccionFiscal.trim(), despues.direccionFiscal.trim()))
        }
        if (antes.personaId.trim() != despues.personaId.trim()) {
            cambios.add(DetalleCambioEmisor("Persona ID", antes.personaId.trim(), despues.personaId.trim()))
        }
        if (antes.personaToken.trim() != despues.personaToken.trim()) {
            cambios.add(DetalleCambioEmisor("Persona Token", enmascararToken(antes.personaToken), enmascararToken(despues.personaToken)))
        }
        if (antes.modo != despues.modo) {
            cambios.add(DetalleCambioEmisor("Modo", antes.modo, despues.modo))
        }
        return cambios
    }

    fun enmascararToken(token: String): String {
        val t = token.trim()
        if (t.isBlank()) return "(vacío)"
        if (t.length <= 4) return "****"
        return "****" + t.takeLast(4)
    }

    /**
     * Escucha viva del historial de cambios del emisor (auditoría, R8).
     * Ordenados del más reciente al más antiguo en memoria (cero índices compuestos).
     */
    fun observarHistorialEmisor(farmaciaId: String, limite: Int = 50): Flow<List<ActaCambioEmisor>> = callbackFlow {
        if (farmaciaId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val ref = FarmadonPaths.facturacionEmisorHistorial(db, farmaciaId)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error escuchando historial del emisor: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }
            val lista = snapshot?.documents?.mapNotNull { doc ->
                parseActa(doc.id, doc.data)
            }?.sortedByDescending { it.fechaMs }?.take(limite) ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    private fun parseActa(id: String, data: Map<String, Any?>?): ActaCambioEmisor? {
        if (data == null) return null
        return try {
            val fechaMs = (data["fechaMs"] as? Number)?.toLong()
                ?: (data["fecha"] as? com.google.firebase.Timestamp)?.toDate()?.time
                ?: 0L
            @Suppress("UNCHECKED_CAST")
            val cambiosRaw = data["cambios"] as? List<Map<String, Any?>> ?: emptyList()
            ActaCambioEmisor(
                id = id,
                fechaMs = fechaMs,
                fechaLegible = (data["fechaLegible"] as? String).orEmpty(),
                usuarioEmail = (data["usuarioEmail"] as? String).orEmpty(),
                usuarioNombre = (data["usuarioNombre"] as? String).orEmpty(),
                sedeId = (data["sedeId"] as? String)?.ifBlank { "principal" } ?: "principal",
                resultado = (data["resultado"] as? String).orEmpty(),
                verificadoOk = (data["verificadoOk"] as? Boolean) ?: false,
                ultimoError = (data["ultimoError"] as? String).orEmpty(),
                cambios = cambiosRaw.map {
                    DetalleCambioEmisor(
                        campo = (it["campo"] as? String).orEmpty(),
                        antes = (it["antes"] as? String).orEmpty(),
                        despues = (it["despues"] as? String).orEmpty()
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando acta del emisor $id: ${e.message}", e)
            null
        }
    }

    /**
     * Realiza una llamada de validación real contra el endpoint lastDocument de APISUNAT.
     * Cero tokens expuestos en logs (Regla 4).
     */
    suspend fun pingApisunat(personaId: String, personaToken: String): Result<String> = withContext(Dispatchers.IO) {
        val pid = personaId.trim()
        val token = personaToken.trim()

        if (pid.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("El campo personaId es requerido para verificar conexión."))
        }
        if (token.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("El campo personaToken es requerido para verificar conexión."))
        }

        try {
            val jsonBody = JSONObject().apply {
                put("personaId", pid)
                put("personaToken", token)
                put("type", "03") // Tipo Boleta
                put("serie", "B001")
            }.toString()

            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(APISUNAT_LAST_DOCUMENT_URL)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                val statusCode = response.code

                if (response.isSuccessful) {
                    Result.success("Conexión con APISUNAT verificada exitosamente.")
                } else {
                    val mensajeProveedor = extraerMensajeErrorApisunat(responseBody, statusCode, response.message)
                    Result.failure(IllegalStateException(mensajeProveedor))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción de red al verificar conexión con APISUNAT: ${e.javaClass.simpleName}")
            Result.failure(IllegalStateException("No se pudo conectar con el servidor de APISUNAT: ${e.localizedMessage ?: e.message}"))
        }
    }

    /**
     * Guarda la configuración del emisor y ejecuta la verificación real en APISUNAT.
     * Si la verificación pasa: verificadoOk = true, ultimoError = ""
     * Si la verificación falla: verificadoOk = false, ultimoError = mensaje real del proveedor.
     */
    suspend fun guardarYVerificarEmisor(
        farmaciaId: String,
        emisor: EmisorFiscal,
        usuarioEmail: String
    ): Result<EmisorFiscal> {
        if (farmaciaId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID de farmacia no válido."))
        }

        // 1. Control de Rol: Solo Dueño o Administrador
        val rolActual = SessionManager.rol
        val esAdminODueno = rolActual.equals("Administrador", ignoreCase = true) ||
                rolActual.equals("Dueño", ignoreCase = true) ||
                rolActual.equals("Dueno", ignoreCase = true)
        if (!esAdminODueno) {
            return Result.failure(
                IllegalStateException("Acceso denegado: Solo el Dueño o un Administrador tiene autorización para modificar la configuración fiscal.")
            )
        }

        // 2. Control de Sede: Solo la Sede Principal
        val sedeActiva = SessionManager.sucursalIdEfectiva.ifBlank { SessionManager.sucursalId }
        if (!sedeActiva.equals("principal", ignoreCase = true)) {
            return Result.failure(
                IllegalStateException("Solo la sede principal puede guardar el emisor fiscal. Las sucursales usan automáticamente estos datos.")
            )
        }

        // 3. Validación de integridad de campos
        val rucLimpio = emisor.ruc.trim()
        if (rucLimpio.length != 11 || !rucLimpio.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("El RUC debe tener exactamente 11 dígitos numéricos."))
        }
        if (emisor.razonSocial.isBlank()) {
            return Result.failure(IllegalArgumentException("La Razón Social es obligatoria."))
        }
        if (emisor.direccionFiscal.isBlank()) {
            return Result.failure(IllegalArgumentException("La Dirección Fiscal es obligatoria."))
        }
        if (emisor.personaId.isBlank()) {
            return Result.failure(IllegalArgumentException("El identificador personaId de APISUNAT es obligatorio."))
        }
        if (emisor.personaToken.isBlank()) {
            return Result.failure(IllegalArgumentException("El token de APISUNAT es obligatorio."))
        }

        // 4. Ejecutar prueba real de conexión con APISUNAT ANTES de alterar Firestore
        val resultadoPing = pingApisunat(emisor.personaId, emisor.personaToken)
        val verificado = resultadoPing.isSuccess
        val errorDetalle = if (verificado) "" else (resultadoPing.exceptionOrNull()?.message ?: "Error desconocido de APISUNAT")

        val emisorGuardar = emisor.copy(
            ruc = rucLimpio,
            razonSocial = emisor.razonSocial.trim(),
            direccionFiscal = emisor.direccionFiscal.trim(),
            personaId = emisor.personaId.trim(),
            personaToken = emisor.personaToken.trim(),
            verificadoOk = verificado,
            ultimoError = errorDetalle,
            actualizadoPor = usuarioEmail.trim()
        )

        val docRef = FarmadonPaths.facturacionEmisor(db, farmaciaId)
        val data = mapOf(
            "ruc" to emisorGuardar.ruc,
            "razonSocial" to emisorGuardar.razonSocial,
            "direccionFiscal" to emisorGuardar.direccionFiscal,
            "personaId" to emisorGuardar.personaId,
            "personaToken" to emisorGuardar.personaToken,
            "token" to emisorGuardar.personaToken,
            "apisNetPeToken" to emisorGuardar.personaToken,
            "modo" to emisorGuardar.modo,
            "verificadoOk" to emisorGuardar.verificadoOk,
            "ultimoError" to emisorGuardar.ultimoError,
            "actualizadoPor" to emisorGuardar.actualizadoPor,
            "actualizadoEl" to FieldValue.serverTimestamp()
        )

        return try {
            // Foto del "antes" para el acta de auditoría (qué cambió).
            val antesSnap = try {
                docRef.get().await()
            } catch (_: Exception) {
                null
            }
            val emisorAntes = if (antesSnap != null && antesSnap.exists()) {
                obtenerEmisor(farmaciaId)
            } else {
                null
            }
            val cambios = calcularCambiosEmisor(emisorAntes, emisorGuardar)

            val sedeId = SessionManager.sucursalIdEfectiva.ifBlank { SessionManager.sucursalId.ifBlank { "principal" } }
            val actaRef = FarmadonPaths.facturacionEmisorHistorial(db, farmaciaId).document()
            val ahoraMs = System.currentTimeMillis()
            val sdfLegible = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("America/Lima")
            }
            val fechaLegibleInicial = sdfLegible.format(java.util.Date(ahoraMs))

            // Si APISUNAT RECHAZÓ las credenciales:
            // Prohibido sobreescribir la configuración activa en Firestore con datos erróneos.
            // Se registra el intento fallido en Auditoría para trazabilidad de quién intentó cambiar y a qué hora.
            if (!verificado) {
                try {
                    actaRef.set(
                        mapOf(
                            "id" to actaRef.id,
                            "usuarioEmail" to usuarioEmail.trim(),
                            "usuarioNombre" to SessionManager.nombreUsuario.ifBlank { "Administrador" },
                            "sedeId" to sedeId,
                            "resultado" to "FALLO_VERIFICACION",
                            "verificadoOk" to false,
                            "ultimoError" to errorDetalle,
                            "cambios" to cambios.map {
                                mapOf("campo" to it.campo, "antes" to it.antes, "despues" to it.despues)
                            },
                            "fecha" to FieldValue.serverTimestamp(),
                            "fechaMs" to ahoraMs,
                            "fechaLegible" to fechaLegibleInicial,
                            "sedeLegible" to if (sedeId.equals("principal", ignoreCase = true)) "Sede Principal" else sedeId
                        )
                    ).await()
                } catch (_: Exception) {}

                return Result.failure(
                    IllegalStateException("Conexión rechazada por APISUNAT: $errorDetalle. Los datos NO se guardaron para evitar romper la facturación de la farmacia. Corrija los datos e intente nuevamente.")
                )
            }

            // APISUNAT ACEPTÓ: Guardado atómico (Emisor + Espejo Token + Acta de Auditoría)
            val batch = db.batch()
            batch.set(docRef, data, SetOptions.merge())
            if (emisorGuardar.personaToken.isNotBlank()) {
                batch.set(
                    FarmadonPaths.farmacia(db, farmaciaId),
                    mapOf(
                        "apisNetPeToken" to emisorGuardar.personaToken,
                        "tokenApisNetPe" to emisorGuardar.personaToken,
                        "apiToken" to emisorGuardar.personaToken,
                        "token" to emisorGuardar.personaToken
                    ),
                    SetOptions.merge()
                )
            }
            batch.set(
                actaRef,
                mapOf(
                    "id" to actaRef.id,
                    "usuarioEmail" to usuarioEmail.trim(),
                    "usuarioNombre" to SessionManager.nombreUsuario.ifBlank { "Administrador" },
                    "sedeId" to sedeId,
                    "resultado" to "VERIFICADO",
                    "verificadoOk" to true,
                    "ultimoError" to "",
                    "cambios" to cambios.map {
                        mapOf("campo" to it.campo, "antes" to it.antes, "despues" to it.despues)
                    },
                    "fecha" to FieldValue.serverTimestamp(),
                    "fechaMs" to ahoraMs,
                    "fechaLegible" to fechaLegibleInicial,
                    "sedeLegible" to if (sedeId.equals("principal", ignoreCase = true)) "Sede Principal" else sedeId
                )
            )
            batch.commit().await()

            // Sincronizar fecha y hora real del servidor
            try {
                val actaFresca = actaRef.get().await()
                val msServidor = actaFresca.getTimestamp("fecha")?.toDate()?.time ?: 0L
                if (msServidor > 0L) {
                    actaRef.set(
                        mapOf(
                            "fechaMs" to msServidor,
                            "fechaLegible" to sdfLegible.format(java.util.Date(msServidor))
                        ),
                        SetOptions.merge()
                    ).await()
                }
            } catch (_: Exception) {}

            Result.success(emisorGuardar)
        } catch (e: Exception) {
            Log.e(TAG, "Error persistiendo emisor fiscal en Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun extraerMensajeErrorApisunat(responseBody: String, statusCode: Int, defaultMessage: String): String {
        if (responseBody.isBlank()) {
            return "APISUNAT respondió HTTP $statusCode: $defaultMessage"
        }
        return try {
            val json = JSONObject(responseBody)
            when {
                json.has("message") -> json.getString("message")
                json.has("error") -> json.getString("error")
                json.has("description") -> json.getString("description")
                json.has("detail") -> json.getString("detail")
                else -> "Error fiscal APISUNAT ($statusCode): $responseBody"
            }
        } catch (_: Exception) {
            "Error fiscal APISUNAT ($statusCode): $responseBody"
        }
    }
}
