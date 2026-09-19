package com.example.ai

import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

interface AIProvider {
    suspend fun generateResponse(
        messages: List<AiChatMessage>,
        config: AIModelConfig,
        provider: AIProviderConfig
    ): Result<AiChatMessage>
}

class GeminiProvider(private val apiService: AiApiService) : AIProvider {
    override suspend fun generateResponse(
        messages: List<AiChatMessage>,
        config: AIModelConfig,
        provider: AIProviderConfig
    ): Result<AiChatMessage> = withContext(Dispatchers.IO) {
        try {
            val systemMsg = messages.find { it.role == AiRole.SYSTEM }
            val history = messages.filter { it.role != AiRole.SYSTEM }
            
            val request = GeminiGenerateRequest(
                contents = history.map { 
                    GeminiContent(
                        role = if (it.role == AiRole.USER) "user" else "model",
                        parts = listOf(GeminiPart(text = it.content))
                    )
                },
                generationConfig = GeminiConfig(
                    temperature = config.temperature,
                    maxOutputTokens = config.maxTokens
                ),
                systemInstruction = systemMsg?.let { 
                    GeminiContent(parts = listOf(GeminiPart(text = it.content)))
                }
            )

            // Defensive access to GEMINI_API_KEY which might not be generated yet
            val apiKey = provider.apiKey ?: try {
                val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
                field.get(null) as String
            } catch (e: Exception) {
                "MISSING_KEY"
            }
            
            val url = "${provider.baseUrl}/v1beta/models/${config.modelId}:generateContent?key=$apiKey"
            
            val response = apiService.generateContent(url, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("Empty response from Gemini"))

            Result.success(AiChatMessage(
                id = "AI-${System.currentTimeMillis()}",
                projectId = messages.first().projectId,
                role = AiRole.ASSISTANT,
                content = responseText,
                modelUsed = config.modelId,
                providerUsed = provider.name
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class OpenAiCompatibleProvider(private val apiService: AiApiService) : AIProvider {
    override suspend fun generateResponse(
        messages: List<AiChatMessage>,
        config: AIModelConfig,
        provider: AIProviderConfig
    ): Result<AiChatMessage> = withContext(Dispatchers.IO) {
        try {
            val request = OpenAiChatRequest(
                model = config.modelId,
                messages = messages.map { 
                    OpenAiMessage(
                        role = when(it.role) {
                            AiRole.USER -> "user"
                            AiRole.ASSISTANT -> "assistant"
                            AiRole.SYSTEM -> "system"
                        },
                        content = it.content
                    )
                },
                temperature = config.temperature,
                max_tokens = config.maxTokens
            )

            val auth = provider.apiKey?.let { "Bearer $it" }
            val url = if (provider.baseUrl.endsWith("/chat/completions")) provider.baseUrl 
                      else "${provider.baseUrl.removeSuffix("/")}/chat/completions"

            val response = apiService.chatCompletion(url, auth, request)
            val responseText = response.choices.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("Empty response from provider"))

            Result.success(AiChatMessage(
                id = "AI-${System.currentTimeMillis()}",
                projectId = messages.first().projectId,
                role = AiRole.ASSISTANT,
                content = responseText,
                modelUsed = config.modelId,
                providerUsed = provider.name
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

object AiProviderFactory {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/") // Default base, will be overridden by @Url
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val apiService = retrofit.create(AiApiService::class.java)

    fun getProvider(type: AIProviderType): AIProvider {
        return when (type) {
            AIProviderType.GEMINI -> GeminiProvider(apiService)
            AIProviderType.NVIDIA_NIM, AIProviderType.OPENAI_COMPATIBLE, AIProviderType.LOCAL -> 
                OpenAiCompatibleProvider(apiService)
        }
    }
}
