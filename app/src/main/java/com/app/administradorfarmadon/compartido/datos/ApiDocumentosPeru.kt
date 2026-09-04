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
        val mensaje: String = "No se detectó token de consulta automática. Puedes ingresar el nombre del cliente directamente."
    ) : ResultadoConsultaDoc

    data class Error(val mensaje: String) : ResultadoConsultaDoc
}

/** DTO de respuesta para DNI vía apis.net.pe */
data class DniNetPeResponse(
    @field:Json(name = "numero") val numero: String? = null,
    @field:Json(name = "nombre") val nombre: String? = null,
    @field:Json(name = "apellidoPaterno") val apellidoPaterno: String? = null,
    @field:Json(name = "apellidoMaterno") val apellidoMaterno: String? = null,
    @field:Json(name = "nombres") val nombres: String? = null,
    @field:Json(name = "nombreCompleto") val nombreCompleto: String? = null,
    @field:Json(name = "direccion") val direccion: String? = null
) {
    fun extraerNombre(): String {
        if (!nombreCompleto.isNullOrBlank()) return nombreCompleto.trim()
        if (!nombre.isNullOrBlank()) return nombre.trim()
        val partes = listOfNotNull(nombres, apellidoPaterno, apellidoMaterno).filter { it.isNotBlank() }
        return partes.joinToString(" ").trim()
    }
}

/** DTO de respuesta para RUC vía apis.net.pe */
data class RucNetPeResponse(
    @field:Json(name = "numero") val numero: String? = null,
    @field:Json(name = "razonSocial") val razonSocial: String? = null,
    @field:Json(name = "nombre") val nombre: String? = null,
    @field:Json(name = "nombreComercial") val nombreComercial: String? = null,
    @field:Json(name = "estado") val estado: String? = null,
    @field:Json(name = "condicion") val condicion: String? = null,
    @field:Json(name = "direccion") val direccion: String? = null,
    @field:Json(name = "distrito") val distrito: String? = null,
    @field:Json(name = "provincia") val provincia: String? = null,
    @field:Json(name = "departamento") val departamento: String? = null
) {
    fun extraerRazonSocial(): String {
        return (razonSocial?.takeIf { it.isNotBlank() }
            ?: nombre?.takeIf { it.isNotBlank() }
            ?: nombreComercial?.takeIf { it.isNotBlank() }
            ?: "").trim()
    }
}

interface ApisNetPeApi {
    @GET("v2/reniec/dni")
    suspend fun consultarDni(
        @Header("Authorization") authHeader: String,
        @Query("numero") numero: String
    ): Response<DniNetPeResponse>

    @GET("v2/sunat/ruc")
    suspend fun consultarRuc(
        @Header("Authorization") authHeader: String,
        @Query("numero") numero: String
    ): Response<RucNetPeResponse>

    @GET("v1/dni")
    suspend fun consultarDniV1(
        @Query("numero") numero: String
    ): Response<DniNetPeResponse>

    @GET("v1/ruc")
    suspend fun consultarRucV1(
        @Query("numero") numero: String
    ): Response<RucNetPeResponse>
}

/**
 * Cliente singleton de consulta de documentos oficiales para Perú (R8 / R1).
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

    val api: ApisNetPeApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApisNetPeApi::class.java)
    }

    /**
     * Obtiene el token de consultas DNI/RUC 100% desde Firebase en cascada
     * (cero memoria local: toda sede lee el mismo dato de la sede principal):
     * 1. facturacion_config/emisor (apisNetPeToken, tokenConsultas, token, personaToken)
     * 2. farmacias/{farmaciaId} (apisNetPeToken, tokenApisNetPe, apiToken, token, personaToken)
     * 3. brixo_configuracion/empresa (token provisto por BRIXO Central)
     */
    suspend fun obtenerTokenConfigurado(): String {
        val farmaciaId = SessionManager.clienteIdGarantizado.ifBlank {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        }
        if (farmaciaId.isBlank()) return ""
        val db = FarmadonFirestore.db
        return try {
            // 1. Emisor Fiscal (Facturación Electrónica de la farmacia)
            val emisorSnap = FarmadonPaths.facturacionEmisor(db, farmaciaId).get().await()
            if (emisorSnap.exists()) {
                val tokenEmisor = emisorSnap.getString("apisNetPeToken")
                    ?: emisorSnap.getString("tokenConsultas")
                    ?: emisorSnap.getString("apiToken")
                    ?: emisorSnap.getString("token")
                    ?: emisorSnap.getString("personaToken")
                if (!tokenEmisor.isNullOrBlank()) {
                    return tokenEmisor.trim()
                }
            }

            // 2. Ficha Farmacia
            val doc = FarmadonPaths.farmacia(db, farmaciaId).get().await()
            if (doc.exists()) {
                val tokenFarmacia = doc.getString("apisNetPeToken")
                    ?: doc.getString("tokenApisNetPe")
                    ?: doc.getString("tokenConsultas")
                    ?: doc.getString("apiToken")
                    ?: doc.getString("token")
                    ?: doc.getString("personaToken")
                if (!tokenFarmacia.isNullOrBlank()) {
                    return tokenFarmacia.trim()
                }
            }

            // 3. Central BRIXO (token global provisto en ecosistema)
            val brixoDoc = db.collection("brixo_configuracion").document("empresa").get().await()
            if (brixoDoc.exists()) {
                val tokenBrixo = brixoDoc.getString("apisNetPeToken")
                    ?: brixoDoc.getString("tokenConsultas")
                    ?: brixoDoc.getString("apiToken")
                if (!tokenBrixo.isNullOrBlank()) return tokenBrixo.trim()
            }

            ""
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo token de consultas: ${e.message}", e)
            ""
        }
    }

    /**
     * Consulta un documento (DNI o RUC) de forma ultra-resiliente:
     * - Intenta con token privado si existe.
     * - Si no hay token o responde 401/403, conmuta al canal público v1 sin interrumpir al cajero.
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

        try {
            // Intento 1: Con token (v2) si está disponible
            if (token.isNotBlank()) {
                val bearer = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
                if (tipoUpper == "DNI") {
                    val resp = api.consultarDni(bearer, numLimpio)
                    if (resp.isSuccessful) {
                        val nombre = resp.body()?.extraerNombre().orEmpty()
                        if (nombre.isNotBlank()) {
                            return ResultadoConsultaDoc.Encontrado(nombreCompleto = nombre, numero = numLimpio, tipo = "DNI")
                        }
                    }
                } else if (tipoUpper == "RUC") {
                    val resp = api.consultarRuc(bearer, numLimpio)
                    if (resp.isSuccessful) {
                        val body = resp.body()
                        val razon = body?.extraerRazonSocial().orEmpty()
                        if (razon.isNotBlank()) {
                            return ResultadoConsultaDoc.Encontrado(nombreCompleto = razon, numero = numLimpio, tipo = "RUC", direccion = body?.direccion.orEmpty())
                        }
                    }
                }
            }

            // Intento 2: Canal público / fallback libre (v1)
            if (tipoUpper == "DNI") {
                val respV1 = api.consultarDniV1(numLimpio)
                if (respV1.isSuccessful) {
                    val nombre = respV1.body()?.extraerNombre().orEmpty()
                    if (nombre.isNotBlank()) {
                        return ResultadoConsultaDoc.Encontrado(nombreCompleto = nombre, numero = numLimpio, tipo = "DNI")
                    }
                }
            } else if (tipoUpper == "RUC") {
                val respV1 = api.consultarRucV1(numLimpio)
                if (respV1.isSuccessful) {
                    val body = respV1.body()
                    val razon = body?.extraerRazonSocial().orEmpty()
                    if (razon.isNotBlank()) {
                        return ResultadoConsultaDoc.Encontrado(nombreCompleto = razon, numero = numLimpio, tipo = "RUC", direccion = body?.direccion.orEmpty())
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Intento directo falló, evaluando resultado: ${e.message}")
        }

        return ResultadoConsultaDoc.NoEncontrado
    }
}
