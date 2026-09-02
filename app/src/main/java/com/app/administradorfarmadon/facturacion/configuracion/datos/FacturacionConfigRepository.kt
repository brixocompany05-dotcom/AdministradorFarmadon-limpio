package com.app.administradorfarmadon.facturacion.configuracion.datos

import android.util.Log
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

        // Ejecutar prueba real de conexión
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
            "modo" to emisorGuardar.modo,
            "verificadoOk" to emisorGuardar.verificadoOk,
            "ultimoError" to emisorGuardar.ultimoError,
            "actualizadoPor" to emisorGuardar.actualizadoPor,
            "actualizadoEl" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(data, SetOptions.merge()).await()
            if (verificado) {
                Result.success(emisorGuardar)
            } else {
                Result.failure(IllegalStateException("Credenciales guardadas, pero la verificación fiscal falló: $errorDetalle"))
            }
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
