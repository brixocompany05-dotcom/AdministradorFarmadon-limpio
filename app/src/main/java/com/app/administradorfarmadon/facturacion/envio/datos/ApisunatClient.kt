package com.app.administradorfarmadon.facturacion.envio.datos

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
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

    @GET("documents/{id}/getPDF")
    suspend fun getDocumentPdf(
        @Path("id") documentId: String,
        @Query("personaId") personaId: String,
        @Query("personaToken") personaToken: String,
        @Query("format") format: String = "ticket80mm"
    ): Response<ResponseBody>

    @GET("documents/{id}/getXML")
    suspend fun getDocumentXml(
        @Path("id") documentId: String,
        @Query("personaId") personaId: String,
        @Query("personaToken") personaToken: String
    ): Response<ResponseBody>

    @GET("documents/{id}/getCDR")
    suspend fun getDocumentCdr(
        @Path("id") documentId: String,
        @Query("personaId") personaId: String,
        @Query("personaToken") personaToken: String
    ): Response<ResponseBody>
}

sealed class ApisunatResultado {
    data class Exito(
        val documentId: String,
        val estadoSunat: String,
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
    private val api: ApisunatApi = crearRetrofitApi(ApisunatConfig.BASE_URL)
) {
    companion object {
        private const val TAG = "ApisunatClient"

        private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        private val mapAdapter = moshi.adapter<Map<String, Any?>>(mapType)

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

    suspend fun emitirDocumento(payload: Map<String, Any?>): ApisunatResultado {
        return try {
            val response = api.sendBill(payload)
            procesarRespuesta(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error emitiendo documento: ${e.message}", e)
            ApisunatResultado.FallaRed(e, e.message ?: "Falla de conexión al emitir")
        }
    }

    suspend fun anularDocumento(payload: Map<String, Any?>): ApisunatResultado {
        return try {
            val response = api.voidBill(payload)
            procesarRespuesta(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error comunicando baja: ${e.message}", e)
            ApisunatResultado.FallaRed(e, e.message ?: "Falla de conexión al comunicar baja")
        }
    }

    suspend fun consultarDocumento(
        documentId: String,
        personaId: String,
        personaToken: String
    ): ApisunatResultado {
        return try {
            val response = api.getDocumentById(documentId, personaId, personaToken)
            procesarRespuesta(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando documento $documentId: ${e.message}", e)
            ApisunatResultado.FallaRed(e, e.message ?: "Falla al consultar documento")
        }
    }

    internal fun procesarRespuesta(response: Response<ResponseBody>): ApisunatResultado {
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
                val json = mapAdapter.fromJson(bodyString) ?: emptyMap()
                val docId = ((json["_id"] as? String) ?: (json["documentId"] as? String)).orEmpty().trim()

                // Determinar el estado SUNAT devuelto con igualdad estricta (cero contains engañosos)
                val statusRaw = (json["status"] as? String).orEmpty().trim().uppercase()
                @Suppress("UNCHECKED_CAST")
                val sunatResponse = json["sunatResponse"] as? Map<String, Any?>
                @Suppress("UNCHECKED_CAST")
                val cdrResponse = sunatResponse?.get("cdrResponse") as? Map<String, Any?>
                val descripcionCdr = (cdrResponse?.get("description") as? String).orEmpty().ifBlank {
                    (json["message"] as? String).orEmpty()
                }

                val xmlUrl = ((json["xml"] as? String) ?: (json["xmlUrl"] as? String)).orEmpty().trim()
                val cdrUrl = ((json["cdr"] as? String) ?: (json["cdrUrl"] as? String)).orEmpty().trim()
                val pdfUrl = ((json["pdf"] as? String) ?: (json["pdfUrl"] as? String)).orEmpty().trim()

                when (statusRaw) {
                    "ACEPTADO" -> {
                        // ACEPTADO exige docId y constancia física (CDR o XML firmado). Sin constancia no hay éxito fiscal.
                        if (docId.isBlank() || (cdrUrl.isBlank() && xmlUrl.isBlank())) {
                            ApisunatResultado.ErrorProveedor(
                                codigo = statusCode,
                                mensaje = "ACEPTADO sin constancia (falta documentId o CDR/XML de SUNAT)",
                                esRechazoSunat = false,
                                rawJson = bodyString
                            )
                        } else {
                            ApisunatResultado.Exito(
                                documentId = docId,
                                estadoSunat = "ACEPTADO",
                                xmlUrl = xmlUrl,
                                cdrUrl = cdrUrl,
                                pdfUrl = pdfUrl,
                                mensajeRespuesta = descripcionCdr,
                                rawJson = bodyString
                            )
                        }
                    }
                    "RECHAZADO" -> {
                        if (docId.isBlank()) {
                            ApisunatResultado.ErrorProveedor(
                                codigo = statusCode,
                                mensaje = "RECHAZADO sin identificador de documento: $descripcionCdr",
                                esRechazoSunat = true,
                                rawJson = bodyString
                            )
                        } else {
                            ApisunatResultado.Exito(
                                documentId = docId,
                                estadoSunat = "RECHAZADO",
                                xmlUrl = xmlUrl,
                                cdrUrl = cdrUrl,
                                pdfUrl = pdfUrl,
                                mensajeRespuesta = descripcionCdr.ifBlank { "Comprobante RECHAZADO por SUNAT" },
                                rawJson = bodyString
                            )
                        }
                    }
                    "EXCEPCION" -> {
                        // EXCEPCION previa a SUNAT: número NO quemado, libre para reintentar tras corrección
                        ApisunatResultado.Exito(
                            documentId = docId,
                            estadoSunat = "EXCEPCION",
                            xmlUrl = xmlUrl,
                            cdrUrl = cdrUrl,
                            pdfUrl = pdfUrl,
                            mensajeRespuesta = descripcionCdr.ifBlank { "Excepción técnica antes de validación SUNAT (número libre)" },
                            rawJson = bodyString
                        )
                    }
                    "PENDIENTE", "ENVIADO" -> {
                        // ENVIADO / PENDIENTE exige docId para consulta posterior getById; sin ID es Error
                        if (docId.isBlank()) {
                            ApisunatResultado.ErrorProveedor(
                                codigo = statusCode,
                                mensaje = "Respuesta $statusRaw sin identificador de documento para seguimiento",
                                esRechazoSunat = false,
                                rawJson = bodyString
                            )
                        } else {
                            ApisunatResultado.Exito(
                                documentId = docId,
                                estadoSunat = "ENVIADO",
                                xmlUrl = xmlUrl,
                                cdrUrl = cdrUrl,
                                pdfUrl = pdfUrl,
                                mensajeRespuesta = descripcionCdr,
                                rawJson = bodyString
                            )
                        }
                    }
                    else -> {
                        // Estados compuestos o desconocidos ("NO ACEPTADO", "BAJA_ACEPTADA", "EN_PROCESO", "")
                        if (docId.isNotBlank() && (statusRaw == "EN_PROCESO" || statusRaw == "EN TRAMITE")) {
                            ApisunatResultado.Exito(
                                documentId = docId,
                                estadoSunat = "ENVIADO",
                                xmlUrl = xmlUrl,
                                cdrUrl = cdrUrl,
                                pdfUrl = pdfUrl,
                                mensajeRespuesta = descripcionCdr,
                                rawJson = bodyString
                            )
                        } else {
                            ApisunatResultado.ErrorProveedor(
                                codigo = statusCode,
                                mensaje = "Estado desconocido o no procesable de proveedor: '${statusRaw.ifBlank { "VACÍO" }}'",
                                esRechazoSunat = false,
                                rawJson = bodyString
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parseando JSON exitoso de APISUNAT: ${e.message}", e)
                ApisunatResultado.ErrorProveedor(
                    codigo = statusCode,
                    mensaje = "Error interpretando JSON de respuesta: ${e.message}",
                    esRechazoSunat = false,
                    rawJson = bodyString
                )
            }
        }

        // Falla HTTP o error del proveedor
        var statusErr = ""
        val mensajeError = try {
            val errJson = mapAdapter.fromJson(bodyString) ?: emptyMap()
            statusErr = (errJson["status"] as? String).orEmpty().trim().uppercase()
            val msg = (errJson["message"] as? String) ?: (errJson["error"] as? String) ?: response.message()
            msg.ifBlank { "Error HTTP $statusCode" }
        } catch (_: Exception) {
            response.message().ifBlank { "Error HTTP $statusCode" }
        }

        // Quemado SOLO si status == "RECHAZADO" o error semántico 422 de SUNAT. Error 400 de formato = número LIBRE.
        val esRechazoSunat = statusErr == "RECHAZADO" || statusCode == 422

        return ApisunatResultado.ErrorProveedor(
            codigo = statusCode,
            mensaje = mensajeError,
            esRechazoSunat = esRechazoSunat,
            rawJson = bodyString
        )
    }
}
