package com.example

import com.example.api.Content
import com.example.api.GeminiApiException
import com.example.api.GeminiErrorResponse
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.api.WebsiteGenerator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testUrlSanitization_redactsApiKey() {
        val rawUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=AIzaSyD_secret_test_key_123"
        val sanitized = RetrofitClient.sanitizeUrl(rawUrl)
        assertFalse(sanitized.contains("AIzaSyD_secret_test_key_123"))
        assertTrue(sanitized.contains("key=[REDACTED]"))
    }

    @Test
    fun testCleanJsonString_stripsMarkdownFences() {
        val markdownJson = "```json\n{\"html\": \"<h1>Hello</h1>\"}\n```"
        val cleaned = WebsiteGenerator.cleanJsonString(markdownJson)
        assertEquals("{\"html\": \"<h1>Hello</h1>\"}", cleaned)
    }

    @Test
    fun testGenerationConfigSerialization_hasCorrectFields() {
        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("html") { put("type", "STRING") }
            }
            putJsonArray("required") {
                add(JsonPrimitive("html"))
            }
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Hello")))),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                responseMimeType = "application/json",
                responseSchema = schema
            )
        )

        val jsonStr = RetrofitClient.json.encodeToString(GenerateContentRequest.serializer(), request)
        assertTrue(jsonStr.contains("\"responseMimeType\":\"application/json\""))
        assertTrue(jsonStr.contains("\"responseSchema\":{"))
        // Must NOT contain invalid responseFormat
        assertFalse(jsonStr.contains("responseFormat"))
    }

    @Test
    fun testGeminiErrorResponse_parsing() {
        val errorJson = """
            {
              "error": {
                "code": 400,
                "message": "Invalid value at generation_config",
                "status": "INVALID_ARGUMENT"
              }
            }
        """.trimIndent()

        val parsed = RetrofitClient.json.decodeFromString<GeminiErrorResponse>(errorJson)
        assertNotNull(parsed.error)
        assertEquals(400, parsed.error?.code)
        assertEquals("INVALID_ARGUMENT", parsed.error?.status)

        val exception = GeminiApiException(400, parsed.error?.status, parsed.error?.message ?: "")
        assertTrue(exception.message!!.contains("INVALID_ARGUMENT"))
        assertTrue(exception.message!!.contains("Invalid value at generation_config"))
    }
}
