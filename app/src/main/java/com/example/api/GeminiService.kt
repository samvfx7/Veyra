package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

class GeminiService(
    private val defaultModel: String? = null
) {
    companion object {
        private const val TAG = "GeminiService"
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

    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null,
        taskType: GenerationTaskType = GenerationTaskType.CONTENT_REWRITE
    ): String = withContext(Dispatchers.IO) {
        validateApiKey()
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) }
        )
        executeWithDiagnostics(request, taskType)
    }

    suspend fun generateStructuredContent(
        prompt: String,
        systemInstruction: String,
        schema: JsonObject,
        taskType: GenerationTaskType = GenerationTaskType.FULL_GENERATION
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
        executeWithDiagnostics(request, taskType)
    }

    private suspend fun executeWithDiagnostics(
        request: GenerateContentRequest,
        taskType: GenerationTaskType
    ): String {
        val eligibleModels = if (defaultModel != null && ModelRouter.isModelAvailable(defaultModel)) {
            listOf(defaultModel) + ModelRouter.getEligibleModelsForTask(taskType).filter { it != defaultModel }
        } else {
            ModelRouter.getEligibleModelsForTask(taskType)
        }

        var lastException: Exception? = null
        val attemptedModels = mutableSetOf<String>()

        for (i in eligibleModels.indices) {
            val model = eligibleModels[i]
            if (attemptedModels.contains(model)) continue
            attemptedModels.add(model)

            var attempt = 0
            val maxAttempts = ModelConfig.MAX_RETRIES_PER_MODEL

            while (attempt < maxAttempts) {
                attempt++
                try {
                    Log.d(TAG, "Attempting execution for task $taskType with model: $model (attempt $attempt)")
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
                    // Strictly sanitize logs and messages to prevent credential leaks
                    val sanitizedMsg = if (apiKey.isNotEmpty()) rawMsg.replace(apiKey, "[REDACTED]") else rawMsg

                    Log.e(TAG, "Gemini API Error with model $model: [$status] (code ${e.code()}): $sanitizedMsg")
                    val apiException = GeminiApiException(e.code(), status, sanitizedMsg, e)

                    // 1. Rate Limit / Quota Exceeded (HTTP 429)
                    if (e.code() == 429 || status.contains("RESOURCE_EXHAUSTED", ignoreCase = true)) {
                        ModelRouter.markModelUnavailable(model, "HTTP 429 Quota/Rate Limit", ModelConfig.RATE_LIMIT_COOLDOWN_MS)
                        lastException = apiException

                        // Check if an alternate model is available
                        val nextModel = eligibleModels.getOrNull(i + 1)
                        if (nextModel != null) {
                            ModelRouter.notifyModelSwitched(model, nextModel)
                            Log.w(TAG, "Model $model hit quota limit. Gracefully routing to next available model: $nextModel")
                            break // Break inner retry loop to try next model in outer loop
                        }
                    }

                    // 2. Service Unavailable or Temporary Server Error (HTTP 503 / 500 / 504)
                    if (e.code() == 503 || e.code() == 500 || e.code() == 504 || e.code() == 404) {
                        ModelRouter.markModelUnavailable(model, "HTTP ${e.code()} Service Error", ModelConfig.SERVER_ERROR_COOLDOWN_MS)
                        lastException = apiException

                        val nextModel = eligibleModels.getOrNull(i + 1)
                        if (nextModel != null) {
                            ModelRouter.notifyModelSwitched(model, nextModel)
                            Log.w(TAG, "Model $model unavailable (HTTP ${e.code()}). Routing to fallback: $nextModel")
                            break
                        }
                    }

                    // 3. Unrecoverable Client errors (400 Bad Request or 401 Auth)
                    if (e.code() == 400 || e.code() == 401) {
                        throw apiException
                    }

                    lastException = apiException
                    break // Non-retryable HTTP error for this model, move to next model if available
                } catch (e: SocketTimeoutException) {
                    Log.e(TAG, "Gemini API timeout with model $model (attempt $attempt): ${e.message}")
                    lastException = GeminiApiException(408, "TIMEOUT", "Connection to Gemini timed out. Please try again.", e)
                    if (attempt < maxAttempts) {
                        delay(ModelConfig.RETRY_BACKOFF_DELAY_MS)
                        continue
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Network error with model $model (attempt $attempt): ${e.message}")
                    lastException = GeminiApiException(0, "NETWORK_ERROR", "Network failure connecting to Gemini: ${e.message}", e)
                    if (attempt < maxAttempts) {
                        delay(ModelConfig.RETRY_BACKOFF_DELAY_MS)
                        continue
                    }
                } catch (e: GeminiApiException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error calling Gemini API with model $model: ${e.message}", e)
                    throw GeminiApiException(500, "INTERNAL_ERROR", e.message ?: "Unexpected error", e)
                }
            }
        }

        val finalError = lastException ?: GeminiApiException(503, "ALL_MODELS_BUSY", "AI generation services are currently experiencing high demand. Please try again in a few moments.")
        throw finalError
    }
}
