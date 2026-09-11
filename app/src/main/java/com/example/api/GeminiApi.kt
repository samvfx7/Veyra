package com.example.api

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList(),
    val modelVersion: String? = null
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

@Serializable
data class GeminiErrorResponse(
    val error: GeminiErrorDetails? = null
)

@Serializable
data class GeminiErrorDetails(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

class GeminiApiException(
    val httpCode: Int,
    val errorStatus: String?,
    val rawMessage: String,
    cause: Throwable? = null
) : Exception(
    formatMessage(httpCode, errorStatus, rawMessage),
    cause
) {
    companion object {
        private fun formatMessage(httpCode: Int, errorStatus: String?, rawMessage: String): String {
            val statusPart = errorStatus ?: "HTTP $httpCode"
            val clean = rawMessage.trim()
            return "Gemini API Error [$statusPart]: $clean"
        }
    }
}

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val TAG = "GeminiApi"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Sanitizes any URL to ensure API keys are never exposed in logs
    fun sanitizeUrl(url: String): String {
        return url.replace(Regex("([?&]key=)[^&]+"), "$1[REDACTED]")
    }

    private val loggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        val sanitizedUrl = sanitizeUrl(request.url.toString())
        
        Log.d(TAG, "--> ${request.method} $sanitizedUrl")
        val startTime = System.currentTimeMillis()
        
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            Log.e(TAG, "<-- HTTP FAILED for $sanitizedUrl: ${e.message}")
            throw e
        }
        
        val duration = System.currentTimeMillis() - startTime
        if (response.isSuccessful) {
            Log.d(TAG, "<-- HTTP ${response.code} OK ($duration ms)")
        } else {
            // Read and log error body for development diagnostics without consuming it
            try {
                val errorBody = response.peekBody(1024 * 64).string()
                Log.e(TAG, "<-- HTTP ${response.code} error ($duration ms) from $sanitizedUrl:\n$errorBody")
            } catch (ignored: Exception) {
                Log.e(TAG, "<-- HTTP ${response.code} error ($duration ms) from $sanitizedUrl")
            }
        }
        
        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}
