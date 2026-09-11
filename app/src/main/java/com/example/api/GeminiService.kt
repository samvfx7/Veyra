package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

class GeminiService(
    private val primaryModel: String = "gemini-3.6-flash"
) {
    companion object {
        private const val TAG = "GeminiService"
        private val FALLBACK_MODELS = listOf("gemini-3.6-flash", "gemini-flash-latest", "gemini-3.5-flash")
    }

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private fun validateApiKey() {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw GeminiApiException(
                httpCode = 401,
                errorStatus = "MISSING_API_KEY",
                rawMessage = "Gemini API key is missing or not configured. Please add your GEMINI_API_KEY in the Secrets panel."
            )
        }
    }

    suspend fun generateContent(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        validateApiKey()
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) }
        )
        executeWithDiagnostics(request)
    }
    
    suspend fun generateStructuredContent(
        prompt: String,
        systemInstruction: String,
        schema: JsonObject
    ): String = withContext(Dispatchers.IO) {
        validateApiKey()
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                responseMimeType = "application/json",
                responseSchema = schema
            )
        )
        executeWithDiagnostics(request)
    }

    private suspend fun executeWithDiagnostics(request: GenerateContentRequest): String {
        val modelsToTry = buildList {
            add(primaryModel)
            for (m in FALLBACK_MODELS) {
                if (m != primaryModel && !contains(m)) {
                    add(m)
                }
            }
        }

        var lastException: Exception? = null

        for (model in modelsToTry) {
            try {
                Log.d(TAG, "Attempting generation with model: $model")
                val response = RetrofitClient.service.generateContent(model, apiKey, request)
                
                val candidate = response.candidates.firstOrNull()
                if (candidate == null) {
                    throw GeminiApiException(200, "NO_CANDIDATES", "Gemini returned no response candidates.")
                }
                
                val text = candidate.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    return text
                }
                
                val finishReason = candidate.finishReason ?: "UNKNOWN"
                throw GeminiApiException(
                    200,
                    "EMPTY_PART",
                    "Gemini finished with reason '$finishReason' without returning text."
                )
            } catch (e: HttpException) {
                val errorBody = try {
                    e.response()?.errorBody()?.string()
                } catch (_: Exception) {
                    null
                }

                val parsed = try {
                    errorBody?.let { RetrofitClient.json.decodeFromString<GeminiErrorResponse>(it) }
                } catch (_: Exception) {
                    null
                }

                val status = parsed?.error?.status ?: "HTTP_${e.code()}"
                val rawMsg = parsed?.error?.message ?: e.message()
                // Never expose apiKey in logs or exception messages
                val sanitizedMsg = if (apiKey.isNotEmpty()) rawMsg.replace(apiKey, "[REDACTED]") else rawMsg

                Log.e(TAG, "Gemini API Error with model $model: [$status] (code ${e.code()}): $sanitizedMsg")
                val apiException = GeminiApiException(e.code(), status, sanitizedMsg, e)

                // If error is 503 or 404 (service unavailable or model not found), try next model
                if (e.code() == 503 || e.code() == 404) {
                    Log.w(TAG, "Model $model returned HTTP ${e.code()}. Trying fallback model...")
                    lastException = apiException
                    continue
                }

                // If 400 (Bad Request), 401 (Auth), or 429 (Quota), do not retry with identical payload
                throw apiException
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Gemini API timeout with model $model: ${e.message}")
                lastException = GeminiApiException(408, "TIMEOUT", "Connection to Gemini timed out. Please try again.", e)
            } catch (e: IOException) {
                Log.e(TAG, "Network error with model $model: ${e.message}")
                lastException = GeminiApiException(0, "NETWORK_ERROR", "Network failure connecting to Gemini: ${e.message}", e)
            } catch (e: GeminiApiException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error calling Gemini API: ${e.message}", e)
                throw GeminiApiException(500, "INTERNAL_ERROR", e.message ?: "Unexpected error", e)
            }
        }

        throw lastException ?: GeminiApiException(500, "UNKNOWN_ERROR", "Failed to generate content with Gemini.")
    }
}
