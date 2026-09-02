package com.app.administradorfarmadon.compartido.datos

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Resultado honesto de la consulta de documentos oficiales en Perú (R3/R9).
 */
sealed interface ResultadoConsultaDoc {
    data class Encontrado(
        val nombreCompleto: String,
        val numero: String,
        val tipo: String,
        val direccion: String = ""
    ) : ResultadoConsultaDoc

    object NoEncontrado : ResultadoConsultaDoc

    data class SinToken(
        val mensaje: String = "Configura el token de apis.net.pe en Configuración → Integraciones"
    ) : ResultadoConsultaDoc

    data class Error(val mensaje: String) : ResultadoConsultaDoc
}

/** DTO de respuesta para DNI vía apis.net.pe */
data class DniNetPeResponse(
    @field:Json(name = "numero") val numero: String? = null,
    @field:Json(name = "nombres") val nombres: String? = null,
    @field:Json(name = "apellidoPaterno") val apellidoPaterno: String? = null,
    @field:Json(name = "apellidoMaterno") val apellidoMaterno: String? = null,
    @field:Json(name = "nombreCompleto") val nombreCompleto: String? = null
) {
    fun extraerNombre(): String {
        if (!nombreCompleto.isNullOrBlank()) return nombreCompleto.trim()
        val partes = listOfNotNull(nombres, apellidoPaterno, apellidoMaterno).filter { it.isNotBlank() }
        return partes.joinToString(" ").trim()
    }
}

/** DTO de respuesta para RUC vía apis.net.pe */
data class RucNetPeResponse(
    @field:Json(name = "numero") val numero: String? = null,
    @field:Json(name = "razonSocial") val razonSocial: String? = null,
    @field:Json(name = "nombreComercial") val nombreComercial: String? = null,
    @field:Json(name = "estado") val estado: String? = null,
    @field:Json(name = "condicion") val condicion: String? = null,
    @field:Json(name = "direccion") val direccion: String? = null
) {
    fun extraerRazonSocial(): String {
        return (razonSocial?.takeIf { it.isNotBlank() }
            ?: nombreComercial?.takeIf { it.isNotBlank() }
            ?: "").trim()
    }
}

/** Interfaz de Retrofit para endpoints de apis.net.pe */
interface ApisNetPeApi {
    @GET("v2/reniec/dni")
    suspend fun consultarDni(
        @Header("Authorization") bearerToken: String,
        @Query("numero") numero: String
    ): Response<DniNetPeResponse>

    @GET("v2/sunat/ruc")
    suspend fun consultarRuc(
        @Header("Authorization") bearerToken: String,
        @Query("numero") numero: String
    ): Response<RucNetPeResponse>
}

/**
 * Servicio centralizado de consulta de DNI / RUC en Perú (RENIEC / SUNAT).
 * Timeout corto (8s), consulta honesta sin datos falsos.
 */
object ApiDocumentosPeru {

    private const val TAG = "ApiDocumentosPeru"
    private const val BASE_URL = "https://api.apis.net.pe/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private val api: ApisNetPeApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApisNetPeApi::class.java)
    }

    /**
     * Obtiene el token configurado para la farmacia activa en Firestore.
     */
    suspend fun obtenerTokenConfigurado(): String {
        val farmaciaId = SessionManager.clienteIdGarantizado
        if (farmaciaId.isBlank()) return ""
        return try {
            val doc = FarmadonPaths.farmacia(FarmadonFirestore.db, farmaciaId).get().await()
            val token = doc.getString("apisNetPeToken")
                ?: doc.getString("tokenApisNetPe")
                ?: doc.getString("apiToken")
                ?: ""
            token.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo token de apis.net.pe: ${e.message}", e)
            ""
        }
    }

    /**
     * Consulta un documento (DNI o RUC) contra el servicio de apis.net.pe.
     */
    suspend fun consultar(tipo: String, numero: String): ResultadoConsultaDoc {
        val numLimpio = numero.trim()
        val tipoUpper = tipo.trim().uppercase()

        if (tipoUpper == "DNI" && numLimpio.length != 8) {
            return ResultadoConsultaDoc.Error("El DNI debe tener 8 dígitos.")
        }
        if (tipoUpper == "RUC" && numLimpio.length != 11) {
            return ResultadoConsultaDoc.Error("El RUC debe tener 11 dígitos.")
        }

        val token = obtenerTokenConfigurado()
        if (token.isBlank()) {
            return ResultadoConsultaDoc.SinToken()
        }

        val bearer = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"

        return try {
            if (tipoUpper == "DNI") {
                val resp = api.consultarDni(bearer, numLimpio)
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val nombre = body?.extraerNombre().orEmpty()
                    if (nombre.isNotBlank()) {
                        ResultadoConsultaDoc.Encontrado(
                            nombreCompleto = nombre,
                            numero = numLimpio,
                            tipo = "DNI"
                        )
                    } else {
                        ResultadoConsultaDoc.NoEncontrado
                    }
                } else if (resp.code() == 404 || resp.code() == 422) {
                    ResultadoConsultaDoc.NoEncontrado
                } else if (resp.code() == 401 || resp.code() == 403) {
                    ResultadoConsultaDoc.Error("Token de apis.net.pe inválido o no autorizado.")
                } else {
                    ResultadoConsultaDoc.Error("Error en consulta RENIEC (${resp.code()}): ${resp.message()}")
                }
            } else if (tipoUpper == "RUC") {
                val resp = api.consultarRuc(bearer, numLimpio)
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val razon = body?.extraerRazonSocial().orEmpty()
                    if (razon.isNotBlank()) {
                        ResultadoConsultaDoc.Encontrado(
                            nombreCompleto = razon,
                            numero = numLimpio,
                            tipo = "RUC",
                            direccion = body?.direccion.orEmpty()
                        )
                    } else {
                        ResultadoConsultaDoc.NoEncontrado
                    }
                } else if (resp.code() == 404 || resp.code() == 422) {
                    ResultadoConsultaDoc.NoEncontrado
                } else if (resp.code() == 401 || resp.code() == 403) {
                    ResultadoConsultaDoc.Error("Token de apis.net.pe inválido o no autorizado.")
                } else {
                    ResultadoConsultaDoc.Error("Error en consulta SUNAT (${resp.code()}): ${resp.message()}")
                }
            } else {
                ResultadoConsultaDoc.Error("Tipo de documento '$tipo' no soportado para consulta oficial.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción consultando documento $tipoUpper $numLimpio: ${e.message}", e)
            ResultadoConsultaDoc.Error(e.message ?: "No se pudo conectar con el servicio de consulta.")
        }
    }
}
