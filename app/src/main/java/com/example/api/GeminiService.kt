package com.example.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class GeminiService {
    private val apiKey = BuildConfig.GEMINI_API_KEY

    suspend fun generateContent(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) }
        )
        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw Exception("Empty response from Gemini")
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun generateStructuredContent(prompt: String, systemInstruction: String, schema: kotlinx.serialization.json.JsonObject): String = withContext(Dispatchers.IO) {
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                temperature = 0.2f, // low temperature for structured tasks
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(
                        mimeType = "application/json",
                        schema = schema
                    )
                )
            )
        )
        try {
             val response = RetrofitClient.service.generateContent(apiKey, request)
             response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw Exception("Empty response from Gemini")
        } catch (e: Exception) {
            throw e
        }
    }
}
