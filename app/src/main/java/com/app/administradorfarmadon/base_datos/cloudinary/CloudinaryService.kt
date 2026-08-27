package com.app.administradorfarmadon.base_datos.cloudinary

import android.graphics.Bitmap
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PartMap
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class CloudinaryResponse(
    @Json(name = "secure_url")
    val secureUrl: String
)

interface CloudinaryApi {
    @Multipart
    @POST("image/upload")
    suspend fun upload(
        @PartMap parts: Map<String, @JvmSuppressWildcards okhttp3.RequestBody>
    ): CloudinaryResponse
}

class CloudinaryService(
    private val cloudName: String = "dxg4zmtsk",
    private val uploadPreset: String = "dfb8217cfbc45dcd201a00ec31443d",
) {
    private val api: CloudinaryApi

    init {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder().build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.cloudinary.com/v1_1/$cloudName/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        api = retrofit.create(CloudinaryApi::class.java)
    }

    suspend fun uploadInvoice(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        val bytes = stream.toByteArray()
        val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())

        val parts = mapOf(
            "upload_preset" to uploadPreset.toRequestBody("text/plain".toMediaType()),
            "file" to requestBody
        )

        val response = api.upload(parts)
        return response.secureUrl
    }

    suspend fun uploadRawFile(bytes: ByteArray, mimeType: String): String {
        val requestBody = bytes.toRequestBody(mimeType.toMediaType())
        val parts = mapOf(
            "upload_preset" to uploadPreset.toRequestBody("text/plain".toMediaType()),
            "file" to requestBody
        )
        val response = api.upload(parts)
        return response.secureUrl
    }
}
