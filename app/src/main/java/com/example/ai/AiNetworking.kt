package com.example.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

// --- Shared Request/Response Models ---

@JsonClass(generateAdapter = true)
data class AiPrompt(
    val role: String,
    val content: String
)

// --- OpenAI / NVIDIA NIM Compatible Models ---

@JsonClass(generateAdapter = true)
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Float = 0.1f,
    val max_tokens: Int = 2048,
    val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class OpenAiMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenAiChatResponse(
    val id: String,
    @Json(name = "object") val obj: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage?
)

@JsonClass(generateAdapter = true)
data class OpenAiChoice(
    val index: Int,
    val message: OpenAiMessage,
    val finish_reason: String?
)

@JsonClass(generateAdapter = true)
data class OpenAiUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

// --- Gemini REST Models ---

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = "text/plain"
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?,
    val finishReason: String?
)

// --- Retrofit Interfaces ---

interface AiApiService {
    // For OpenAI-compatible endpoints (including NVIDIA NIM)
    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") auth: String?,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse

    // For Gemini REST API
    @POST
    suspend fun generateContent(
        @Url url: String,
        @Body request: GeminiGenerateRequest
    ): GeminiResponse
}
