package com.example

import com.example.api.ComponentBlueprints
import com.example.api.Content
import com.example.api.DesignSpecification
import com.example.api.GeminiApiException
import com.example.api.GeminiErrorResponse
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.GenerationTaskType
import com.example.api.ModelConfig
import com.example.api.ModelRouter
import com.example.api.ModelTier
import com.example.api.Part
import com.example.api.QualityAuditor
import com.example.api.ResilientDns
import com.example.api.RetrofitClient
import com.example.api.TaskClassification
import com.example.api.TaskClassifier
import com.example.api.WebsiteGenerator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    @Before
    fun setup() {
        ModelRouter.resetSession()
    }

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

    @Test
    fun testCleanJsonString_handlesNestedAndRawBlocks() {
        val raw = "```json\n{\"html\": \"<header><h1>Barber</h1></header>\", \"css\": \":root { --color-primary: #121417; }\", \"js\": \"console.log('ready');\"}\n```"
        val cleaned = WebsiteGenerator.cleanJsonString(raw)
        assertTrue(cleaned.startsWith("{"))
        assertTrue(cleaned.endsWith("}"))
        assertTrue(cleaned.contains("--color-primary"))
    }

    @Test
    fun testResilientDns_resolvesGoogleApiHost() {
        val addresses = ResilientDns.lookup("generativelanguage.googleapis.com")
        assertNotNull(addresses)
        assertTrue(addresses.isNotEmpty())
        for (addr in addresses) {
            assertEquals("generativelanguage.googleapis.com", addr.hostName)
            assertNotNull(addr.address)
        }
    }

    // --- Smart AI Router Tests ---

    @Test
    fun testModelRouter_taskBasedRouting() {
        val archModels = ModelRouter.getEligibleModelsForTask(GenerationTaskType.WEBSITE_ARCHITECTURE)
        assertTrue(archModels.contains("gemini-2.5-pro") || archModels.contains("gemini-3.6-flash"))

        val fullGenModels = ModelRouter.getEligibleModelsForTask(GenerationTaskType.FULL_GENERATION)
        assertTrue(fullGenModels.contains("gemini-3.6-flash"))

        val editModels = ModelRouter.getEligibleModelsForTask(GenerationTaskType.COMPONENT_EDIT)
        assertTrue(editModels.contains("gemini-flash-latest") || editModels.contains("gemini-2.5-flash-lite") || editModels.contains("gemini-3.6-flash"))
    }

    @Test
    fun testModelRouter_cooldownAndGracefulFallback() {
        val primary = ModelConfig.EFFICIENT_MODELS.first()
        assertTrue(ModelRouter.isModelAvailable(primary))

        // Mark primary as unavailable due to HTTP 429 quota exhaustion
        ModelRouter.markModelUnavailable(primary, "Rate limit test", ModelConfig.RATE_LIMIT_COOLDOWN_MS)
        assertFalse(ModelRouter.isModelAvailable(primary))

        // Get eligible models for FULL_GENERATION; primary should not be first
        val eligible = ModelRouter.getEligibleModelsForTask(GenerationTaskType.FULL_GENERATION)
        assertNotEquals(primary, eligible.first())

        // System notification check
        ModelRouter.notifyModelSwitched(primary, eligible.first())
        assertEquals("Veyra switched to an available AI model.", ModelRouter.systemNotice.value)

        ModelRouter.clearNotice()
        assertNull(ModelRouter.systemNotice.value)
    }

    // --- Task Classifier Tests ---

    @Test
    fun testTaskClassifier_localTokenAdjustment_warmTheme() {
        val baseCss = ":root { --color-bg: #ffffff; --color-primary: #000000; }"
        val result = TaskClassifier.classify("Make the website more warm and earthy", baseCss)
        assertTrue(result is TaskClassification.LocalTokenAdjustment)
        val local = result as TaskClassification.LocalTokenAdjustment
        assertTrue(local.updatedCss.contains("--color-bg: #faf7f2;"))
        assertTrue(local.updatedCss.contains("--color-accent: #c25e3d;"))
    }

    @Test
    fun testTaskClassifier_localTokenAdjustment_darkMode() {
        val baseCss = ":root { --color-bg: #ffffff; --color-text: #000000; }"
        val result = TaskClassifier.classify("Switch to dark mode", baseCss)
        assertTrue(result is TaskClassification.LocalTokenAdjustment)
        val local = result as TaskClassification.LocalTokenAdjustment
        assertTrue(local.updatedCss.contains("--color-bg: #0f1117;"))
        assertTrue(local.updatedCss.contains("--color-text: #f4f4f5;"))
    }

    @Test
    fun testTaskClassifier_detectsNewSection() {
        val baseCss = ":root { --color-bg: #ffffff; }"
        val result = TaskClassifier.classify("Add testimonials from our clients", baseCss)
        assertTrue(result is TaskClassification.NewSectionRequest)
        assertEquals("TestimonialSection", (result as TaskClassification.NewSectionRequest).componentType)
    }

    @Test
    fun testTaskClassifier_detectsTargetedComponentEdit() {
        val baseCss = ":root { --color-bg: #ffffff; }"
        val result = TaskClassifier.classify("Make the hero taller with a bigger title", baseCss)
        assertTrue(result is TaskClassification.TargetedComponentEdit)
        assertEquals("Hero", (result as TaskClassification.TargetedComponentEdit).componentName)
    }

    // --- Quality Auditor Tests ---

    @Test
    fun testQualityAuditor_sanitizesPurpleAIGradientAndMissingViewport() {
        val rawHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>Test</title></head>
            <body><img src="test.jpg"></body>
            </html>
        """.trimIndent()

        val rawCss = """
            .hero {
                background: linear-gradient(135deg, #7c3aed, #4f46e5);
                border-radius: 48px;
            }
        """.trimIndent()

        val report = QualityAuditor.auditAndSanitize(rawHtml, rawCss)
        // Missing viewport was injected
        assertTrue(report.sanitizedHtml.contains("name=\"viewport\""))
        // Missing alt attribute on img was fixed
        assertTrue(report.sanitizedHtml.contains("alt=\"Editorial photograph\""))
        // AI purple gradient was sanitized
        assertFalse(report.sanitizedCss.contains("#7c3aed"))
        assertTrue(report.sanitizedCss.contains("var(--color-surface)"))
        // Excessive border radius 48px was normalized to --radius-md
        assertFalse(report.sanitizedCss.contains("border-radius: 48px"))
        assertTrue(report.sanitizedCss.contains("var(--radius-md)"))
    }

    // --- Design Specification Tests ---

    @Test
    fun testDesignSpecification_generatesValidCssAndSerialization() {
        val spec = DesignSpecification(
            industry = "Barber & Grooming",
            brandPersonality = "Heritage, Artisanal",
            visualDirection = "High-contrast dark layout with amber accents"
        )

        val cssBlock = spec.toCssRootBlock()
        assertTrue(cssBlock.contains(":root {"))
        assertTrue(cssBlock.contains("--color-primary:"))
        assertTrue(cssBlock.contains("--font-display:"))

        val json = spec.toJson()
        val parsed = DesignSpecification.fromJson(json)
        assertNotNull(parsed)
        assertEquals("Barber & Grooming", parsed?.industry)
    }

    // --- Component Blueprints Tests ---

    @Test
    fun testComponentBlueprints_replaceOrInsertComponent() {
        val baseHtml = "<html><body><main><section data-component=\"Hero\">Old Hero</section></main><footer>Footer</footer></body></html>"
        val replaced = ComponentBlueprints.replaceOrInsertComponent(
            baseHtml,
            "Hero",
            "<section data-component=\"Hero\">New Hero</section>"
        )
        assertTrue(replaced.contains("New Hero"))
        assertFalse(replaced.contains("Old Hero"))

        // Test insertion when component does not exist
        val inserted = ComponentBlueprints.replaceOrInsertComponent(
            baseHtml,
            "TestimonialSection",
            "<section data-component=\"TestimonialSection\">Reviews</section>"
        )
        assertTrue(inserted.contains("Reviews"))
        assertTrue(inserted.contains("<footer>Footer</footer>"))
    }
}
