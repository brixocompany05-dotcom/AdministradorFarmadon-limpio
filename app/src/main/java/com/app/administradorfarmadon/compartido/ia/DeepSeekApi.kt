package com.app.administradorfarmadon.compartido.ia

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class DeepSeekChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<DeepSeekMessage>,
    @Json(name = "response_format") val responseFormat: DeepSeekResponseFormat? = null,
    @Json(name = "temperature") val temperature: Double = 0.1,
    @Json(name = "max_tokens") val maxTokens: Int = 1000
)

@JsonClass(generateAdapter = true)
data class DeepSeekMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class DeepSeekResponseFormat(
    @Json(name = "type") val type: String = "json_object"
)

@JsonClass(generateAdapter = true)
data class DeepSeekChatResponse(
    @Json(name = "id") val id: String?,
    @Json(name = "choices") val choices: List<DeepSeekChoice>?
)

@JsonClass(generateAdapter = true)
data class DeepSeekChoice(
    @Json(name = "index") val index: Int?,
    @Json(name = "message") val message: DeepSeekMessage?,
    @Json(name = "finish_reason") val finishReason: String?
)

interface DeepSeekApi {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekChatRequest
    ): Response<DeepSeekChatResponse>
}
