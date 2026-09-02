package com.app.administradorfarmadon.facturacion.envio.datos

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface ApisunatApi {

    @POST("personas/v1/sendBill")
    suspend fun sendBill(
        @Body request: Map<String, Any?>
    ): Response<ResponseBody>

    @POST("personas/v1/voidBill")
    suspend fun voidBill(
        @Body request: Map<String, Any?>
    ): Response<ResponseBody>

    @GET("documents/{id}/getById")
    suspend fun getDocumentById(
        @Path("id") documentId: String,
        @Query("personaId") personaId: String,
        @Query("personaToken") personaToken: String
    ): Response<ResponseBody>

    @GET("documents/{id}/getPDF/{format}")
    suspend fun getDocumentPdf(
        @Path("id") documentId: String,
        @Path("format") format: String = "ticket",
        @Query("personaId") personaId: String,
        @Query("personaToken") personaToken: String
    ): Response<ResponseBody>
}

/**
 * Resultado de una invocación al API de APISUNAT.
 */
sealed class ApisunatResultado {
    data class Exito(
        val documentId: String,
        val estadoSunat: String, // ACEPTADO | RECHAZADO | EXCEPCION | EN_PROCESO | ENVIADO
        val xmlUrl: String = "",
        val cdrUrl: String = "",
        val pdfUrl: String = "",
        val mensajeRespuesta: String = "",
        val rawJson: String = ""
    ) : ApisunatResultado()

    data class ErrorProveedor(
        val codigo: Int,
        val mensaje: String,
        val esRechazoSunat: Boolean = false,
        val rawJson: String = ""
    ) : ApisunatResultado()

    data class FallaRed(
        val excepcion: Throwable,
        val mensaje: String
    ) : ApisunatResultado()
}

class ApisunatClient(
    baseUrl: String = ApisunatConfig.BASE_URL,
    private val api: ApisunatApi = crearRetrofitApi(baseUrl)
) {
    companion object {
        private const val TAG = "ApisunatClient"

        fun crearRetrofitApi(baseUrl: String): ApisunatApi {
            val loggingInterceptor = HttpLoggingInterceptor { mensaje ->
                // Regla 4: Prohibido imprimir tokens en logs
                val mensajeSeguro = mensaje.replace(Regex("\"personaToken\"\\s*:\\s*\"[^\"]+\""), "\"personaToken\": \"***\"")
                Log.d(TAG, mensajeSeguro)
            }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(ApisunatConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(ApisunatConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(ApisunatConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .build()

            return retrofit.create(ApisunatApi::class.java)
        }
    }

    /**
     * Envía una Boleta, Factura o Nota de Crédito a SUNAT vía APISUNAT (/personas/v1/sendBill).
     */
    suspend fun emitirDocumento(payload: Map<String, Any?>): ApisunatResultado {
        return try {
            val response = api.sendBill(payload)
            procesarRespuesta(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error de red/comunicación al emitir documento: ${e.message}", e)
            ApisunatResultado.FallaRed(e, e.message ?: "Falla de conexión con APISUNAT")
        }
    }

    /**
     * Envía una comunicación de baja o anulación (/personas/v1/voidBill).
     */
    suspend fun anularDocumento(payload: Map<String, Any?>): ApisunatResultado {
        return try {
            val response = api.voidBill(payload)
            procesarRespuesta(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error de red/comunicación al anular documento: ${e.message}", e)
            ApisunatResultado.FallaRed(e, e.message ?: "Falla de conexión con APISUNAT")
        }
    }

    /**
     * Consulta el estado final de un documento por su ID (/documents/{id}/getById).
     */
    suspend fun consultarDocumento(documentId: String, personaId: String, personaToken: String): ApisunatResultado {
        return try {
            val response = api.getDocumentById(documentId, personaId, personaToken)
            procesarRespuesta(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando documento $documentId: ${e.message}", e)
            ApisunatResultado.FallaRed(e, e.message ?: "Falla al consultar documento")
        }
    }

    private fun procesarRespuesta(response: Response<ResponseBody>): ApisunatResultado {
        val statusCode = response.code()
        val bodyString = try {
            if (response.isSuccessful) {
                response.body()?.string().orEmpty()
            } else {
                response.errorBody()?.string().orEmpty()
            }
        } catch (e: Exception) {
            ""
        }

        if (response.isSuccessful && bodyString.isNotBlank()) {
            return try {
                val json = JSONObject(bodyString)
                val docId = json.optString("_id").ifBlank { json.optString("documentId", "") }
                
                // Determinar el estado SUNAT devuelto
                val statusRaw = json.optString("status", "").uppercase()
                val sunatResponse = json.optJSONObject("sunatResponse")
                val cdrResponse = sunatResponse?.optJSONObject("cdrResponse")
                val descripcionCdr = cdrResponse?.optString("description", "")
                    ?: json.optString("message", "")

                val estadoFinal = when {
                    statusRaw.contains("ACEPTAD") || statusRaw == "0" -> "ACEPTADO"
                    statusRaw.contains("RECHAZ") || statusRaw.contains("BAJA") -> "RECHAZADO"
                    statusRaw.contains("EXCEPCION") || statusRaw.contains("EXCEPTION") -> "EXCEPCION"
                    statusRaw.contains("ENVIADO") || statusRaw.contains("PROCESO") -> "ENVIADO"
                    else -> if (docId.isNotBlank()) "ENVIADO" else "ACEPTADO"
                }

                val xmlUrl = json.optString("xml", "").ifBlank { json.optString("xmlUrl", "") }
                val cdrUrl = json.optString("cdr", "").ifBlank { json.optString("cdrUrl", "") }
                val pdfUrl = json.optString("pdf", "").ifBlank { json.optString("pdfUrl", "") }

                ApisunatResultado.Exito(
                    documentId = docId,
                    estadoSunat = estadoFinal,
                    xmlUrl = xmlUrl,
                    cdrUrl = cdrUrl,
                    pdfUrl = pdfUrl,
                    mensajeRespuesta = descripcionCdr,
                    rawJson = bodyString
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parseando JSON exitoso de APISUNAT: ${e.message}", e)
                ApisunatResultado.Exito(
                    documentId = "",
                    estadoSunat = "ENVIADO",
                    rawJson = bodyString
                )
            }
        }

        // Falla HTTP o error del proveedor
        val mensajeError = try {
            val jsonErr = JSONObject(bodyString)
            jsonErr.optString("message", "").ifBlank {
                jsonErr.optString("error", response.message())
            }
        } catch (_: Exception) {
            response.message().ifBlank { "Error HTTP $statusCode" }
        }

        val esRechazoSunat = bodyString.contains("rechaz", ignoreCase = true) ||
                bodyString.contains("quemad", ignoreCase = true) ||
                statusCode == 422

        return ApisunatResultado.ErrorProveedor(
            codigo = statusCode,
            mensaje = mensajeError,
            esRechazoSunat = esRechazoSunat,
            rawJson = bodyString
        )
    }
}
