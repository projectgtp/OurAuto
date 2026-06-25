package com.example.network

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getNestedValue(map: Map<*, *>, vararg keys: String): Any? {
        var current: Any? = map
        for (key in keys) {
            if (current is Map<*, *>) {
                current = current[key]
            } else if (current is List<*>) {
                val idx = key.toIntOrNull()
                if (idx != null && idx in current.indices) {
                    current = current[idx]
                } else {
                    return null
                }
            } else {
                return null
            }
        }
        return current
    }

    suspend fun generateContent(
        prompt: String,
        systemInstructionText: String? = null,
        provider: String = "Gemini",
        apiKeyArg: String = "",
        modelNameArg: String = "",
        customEndpointArg: String = ""
    ): String = withContext(Dispatchers.IO) {
        val providerNormalized = provider.trim()
        val apiKey = apiKeyArg.trim().ifEmpty {
            if (providerNormalized.equals("Gemini", ignoreCase = true)) {
                BuildConfig.GEMINI_API_KEY
            } else ""
        }
        
        val modelName = modelNameArg.trim().ifEmpty {
            when (providerNormalized.lowercase()) {
                "openai" -> "gpt-4o-mini"
                "anthropic" -> "claude-3-5-sonnet-20241022"
                "deepseek" -> "deepseek-chat"
                "custom" -> "llama3"
                else -> "gemini-1.5-flash"
            }
        }
        val customEndpoint = customEndpointArg.trim()

        // Proceed with the API call. If it fails later, we can handle it gracefully.
        val targetApiKey = if (providerNormalized.equals("Gemini", ignoreCase = true) && (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY")) {
            BuildConfig.GEMINI_API_KEY.ifBlank { "MY_GEMINI_API_KEY" }
        } else {
            apiKey
        }
        if (!providerNormalized.equals("Gemini", ignoreCase = true) && apiKey.isBlank() && !providerNormalized.equals("custom", ignoreCase = true)) {
            return@withContext "Error: API Key is blank. Please enter your $provider API Key in the Settings page."
        }

        val url: String
        val requestBodyMap: Map<String, Any>
        val headersMap = mutableMapOf<String, String>()

        when {
            providerNormalized.equals("Gemini", ignoreCase = true) -> {
                url = if (customEndpoint.isNotBlank()) customEndpoint else "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$targetApiKey"
                val mutableReq = mutableMapOf<String, Any>(
                    "contents" to listOf(
                        mapOf("parts" to listOf(mapOf("text" to prompt)))
                    )
                )
                if (!systemInstructionText.isNullOrBlank()) {
                    mutableReq["systemInstruction"] = mapOf(
                        "parts" to listOf(mapOf("text" to systemInstructionText))
                    )
                }
                requestBodyMap = mutableReq
            }
            providerNormalized.equals("Anthropic", ignoreCase = true) -> {
                url = if (customEndpoint.isNotBlank()) customEndpoint else "https://api.anthropic.com/v1/messages"
                val mutableReq = mutableMapOf<String, Any>(
                    "model" to modelName,
                    "max_tokens" to 2048,
                    "messages" to listOf(
                        mapOf("role" to "user", "content" to prompt)
                    )
                )
                if (!systemInstructionText.isNullOrBlank()) {
                    mutableReq["system"] = systemInstructionText
                }
                requestBodyMap = mutableReq
                headersMap["x-api-key"] = apiKey
                headersMap["anthropic-version"] = "2023-06-01"
            }
            else -> { // OpenAI, DeepSeek, Custom / Ollama (OpenAI-compatible)
                url = when {
                    customEndpoint.isNotBlank() -> customEndpoint
                    providerNormalized.equals("DeepSeek", ignoreCase = true) -> "https://api.deepseek.com/v1/chat/completions"
                    else -> "https://api.openai.com/v1/chat/completions"
                }
                val messages = mutableListOf<Map<String, String>>()
                if (!systemInstructionText.isNullOrBlank()) {
                    messages.add(mapOf("role" to "system", "content" to systemInstructionText))
                }
                messages.add(mapOf("role" to "user", "content" to prompt))
                
                val mutableReq = mutableMapOf<String, Any>(
                    "model" to modelName,
                    "messages" to messages
                )
                requestBodyMap = mutableReq
                if (apiKey.isNotBlank()) {
                    headersMap["Authorization"] = "Bearer $apiKey"
                }
            }
        }

        try {
            val jsonAdapter = moshi.adapter(Map::class.java)
            val jsonBodyStr = jsonAdapter.toJson(requestBodyMap)
            
            val requestBuilder = okhttp3.Request.Builder()
                .url(url)
                .post(okhttp3.RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), jsonBodyStr))
            
            headersMap.forEach { (k, v) ->
                requestBuilder.addHeader(k, v)
            }
            
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext "Error: API Response negative (${response.code})\n$responseBody"
            }
            
            val responseMap = jsonAdapter.fromJson(responseBody) ?: emptyMap<Any, Any>()
            
            val finalResult = when {
                providerNormalized.equals("Gemini", ignoreCase = true) -> {
                    getNestedValue(responseMap, "candidates", "0", "content", "parts", "0", "text") as? String
                }
                providerNormalized.equals("Anthropic", ignoreCase = true) -> {
                    getNestedValue(responseMap, "content", "0", "text") as? String
                }
                else -> { // OpenAI, DeepSeek, Custom / Ollama
                    getNestedValue(responseMap, "choices", "0", "message", "content") as? String
                }
            }
            
            finalResult ?: "Error: Failed to parse valid text response from $provider. Full reply was:\n$responseBody"
        } catch (e: Exception) {
            "Error calling $provider API: ${e.localizedMessage ?: e.message}"
        }
    }

    suspend fun generateContentWithImage(
        prompt: String,
        base64Image: String,
        apiKeyArg: String = "",
        modelNameArg: String = "gemini-1.5-flash"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = apiKeyArg.trim().ifEmpty { BuildConfig.GEMINI_API_KEY.ifBlank { "" } }
        val modelName = modelNameArg.trim().ifEmpty { "gemini-1.5-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val requestBodyMap = mapOf(
            "contents" to listOf(
                mapOf("parts" to listOf(
                    mapOf("text" to prompt),
                    mapOf("inline_data" to mapOf("mime_type" to "image/jpeg", "data" to base64Image))
                ))
            )
        )
        try {
            val jsonAdapter = moshi.adapter(Map::class.java)
            val jsonBodyStr = jsonAdapter.toJson(requestBodyMap)
            val request = okhttp3.Request.Builder()
                .url(url)
                .post(okhttp3.RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), jsonBodyStr))
                .build()
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext "Error: Vision API (${response.code}): $responseBody"
            val responseMap = jsonAdapter.fromJson(responseBody) ?: emptyMap<Any, Any>()
            getNestedValue(responseMap, "candidates", "0", "content", "parts", "0", "text") as? String
                ?: "Error: No text in vision response"
        } catch (e: Exception) {
            "Error: Gemini Vision: ${e.localizedMessage ?: e.message}"
        }
    }
}

