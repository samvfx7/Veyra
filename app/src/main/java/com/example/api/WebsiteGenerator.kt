package com.example.api

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class WebsiteGenerator(private val geminiService: GeminiService) {

    companion object {
        private const val TAG = "WebsiteGenerator"

        fun cleanJsonString(raw: String): String {
            var text = raw.trim()
            if (text.startsWith("```json")) {
                text = text.removePrefix("```json").trim()
            } else if (text.startsWith("```")) {
                text = text.removePrefix("```").trim()
            }
            if (text.endsWith("```")) {
                text = text.removeSuffix("```").trim()
            }
            return text
        }
    }
    
    suspend fun enhancePrompt(originalPrompt: String): String {
        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("enhancedPrompt") {
                    put("type", "STRING")
                    put("description", "A highly detailed website specification based on the user's intent. Must include visual direction, color palette, typography direction, spacing, component styling, responsive requirements, and accessibility requirements.")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("enhancedPrompt"))
            }
        }
        
        val instruction = """
            You are Veyra, an expert AI website architect.
            The user will give you a brief or vague website idea.
            Your job is to enhance it into a detailed, structured design specification.
            Interpret visual direction, color palette, typography, spacing, component styling, responsive behavior, and accessibility.
            Do not invent unnecessary business information. Keep it faithful to the original intent, just expanded for a developer to build from.
        """.trimIndent()
        
        val jsonString = geminiService.generateStructuredContent(originalPrompt, instruction, schema)
        
        try {
            val clean = cleanJsonString(jsonString)
            val json = Json.parseToJsonElement(clean).jsonObject
            val enhanced = json["enhancedPrompt"]?.jsonPrimitive?.content
            if (!enhanced.isNullOrBlank()) {
                return enhanced
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse enhancedPrompt JSON: ${e.message}. Attempting fallback.")
        }

        // If the model returned plain text directly instead of a JSON object
        if (jsonString.isNotBlank() && !jsonString.trim().startsWith("{")) {
            return jsonString.trim()
        }
        
        return originalPrompt
    }
    
    suspend fun generateCode(enhancedPrompt: String): GeneratedCode {
        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("html") {
                    put("type", "STRING")
                    put("description", "The complete, semantic HTML5 code. Do not include CSS or JS in this string, reference them via <link> and <script>. Ensure responsive meta tags are present.")
                }
                putJsonObject("css") {
                    put("type", "STRING")
                    put("description", "The complete, polished CSS code. Must be modern, responsive, and accessible.")
                }
                putJsonObject("js") {
                    put("type", "STRING")
                    put("description", "The complete, functional JavaScript code (if any needed).")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("html"))
                add(JsonPrimitive("css"))
                add(JsonPrimitive("js"))
            }
        }
        
        val instruction = """
            You are Veyra, a world-class frontend engineer and UI/UX designer.
            You are building a production-quality, premium website based on the provided design specification.
            Output HTML, CSS, and JS separated into their respective fields.
            The HTML should reference 'styles.css' and 'script.js' relatively.
            Use modern semantic HTML5, modern CSS (Flexbox, Grid, CSS Variables), and vanilla JS.
            The design must be dark-first by default (unless specified otherwise), sleek, and minimalist.
            Ensure excellent responsive behavior and accessibility.
        """.trimIndent()
        
        val rawResponse = geminiService.generateStructuredContent(enhancedPrompt, instruction, schema)
        return parseGeneratedCode(rawResponse)
    }

    private fun parseGeneratedCode(rawResponse: String): GeneratedCode {
        try {
            val clean = cleanJsonString(rawResponse)
            val json = Json.parseToJsonElement(clean).jsonObject
            val html = json["html"]?.jsonPrimitive?.content
            val css = json["css"]?.jsonPrimitive?.content ?: ""
            val js = json["js"]?.jsonPrimitive?.content ?: ""
            
            if (!html.isNullOrBlank()) {
                return GeneratedCode(html, css, js)
            }
        } catch (e: Exception) {
            Log.w(TAG, "JSON parse failed for generated code: ${e.message}. Attempting regex markdown extraction.")
        }

        // Fallback: Check if response contains markdown code blocks
        val htmlMatch = Regex("```(?:html)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE).find(rawResponse)?.groupValues?.get(1)?.trim()
        val cssMatch = Regex("```css\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE).find(rawResponse)?.groupValues?.get(1)?.trim()
        val jsMatch = Regex("```(?:javascript|js)\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE).find(rawResponse)?.groupValues?.get(1)?.trim()

        if (!htmlMatch.isNullOrBlank()) {
            return GeneratedCode(htmlMatch, cssMatch ?: "", jsMatch ?: "")
        }

        // Fallback: If raw response is HTML markup itself
        if (rawResponse.contains("<html", ignoreCase = true) || rawResponse.contains("<!DOCTYPE", ignoreCase = true)) {
            return GeneratedCode(rawResponse, "", "")
        }

        // Fallback: Graceful error landing card inside WebView
        val fallbackHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Preview</title>
                <link rel="stylesheet" href="styles.css">
            </head>
            <body>
                <div class="error-container">
                    <h2>Website Content Received</h2>
                    <p>The layout could not be automatically formatted into distinct files. You can view or regenerate using the editor prompt below.</p>
                </div>
                <script src="script.js"></script>
            </body>
            </html>
        """.trimIndent()

        val fallbackCss = """
            body {
                margin: 0;
                padding: 2rem;
                background-color: #0d1117;
                color: #c9d1d9;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                display: flex;
                align-items: center;
                justify-content: center;
                min-height: 80vh;
            }
            .error-container {
                max-width: 500px;
                padding: 2rem;
                background: #161b22;
                border: 1px solid #30363d;
                border-radius: 8px;
                text-align: center;
            }
            h2 { color: #f0f6fc; margin-top: 0; }
            p { color: #8b949e; line-height: 1.5; }
        """.trimIndent()

        return GeneratedCode(fallbackHtml, fallbackCss, "")
    }
    
    suspend fun modifyCode(instruction: String, currentHtml: String, currentCss: String, currentJs: String): GeneratedCode {
        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("html") {
                    put("type", "STRING")
                }
                putJsonObject("css") {
                    put("type", "STRING")
                }
                putJsonObject("js") {
                    put("type", "STRING")
                }
                putJsonObject("changeDescription") {
                    put("type", "STRING")
                    put("description", "A concise 1-2 sentence description of what was changed.")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("html"))
                add(JsonPrimitive("css"))
                add(JsonPrimitive("js"))
                add(JsonPrimitive("changeDescription"))
            }
        }
        
        val sysInstruction = """
            You are Veyra, an expert AI website editor.
            You are given the current HTML, CSS, and JS of a project, and an instruction from the user on what to change.
            Modify the project as requested. Preserve existing functionality unless explicitly asked to change it.
            Output the complete modified HTML, CSS, and JS. Do not output partial files.
        """.trimIndent()
        
        val prompt = """
            User Instruction: $instruction
            
            CURRENT HTML:
            $currentHtml
            
            CURRENT CSS:
            $currentCss
            
            CURRENT JS:
            $currentJs
        """.trimIndent()
        
        val rawResponse = geminiService.generateStructuredContent(prompt, sysInstruction, schema)
        
        try {
            val clean = cleanJsonString(rawResponse)
            val json = Json.parseToJsonElement(clean).jsonObject
            val html = json["html"]?.jsonPrimitive?.content ?: currentHtml
            val css = json["css"]?.jsonPrimitive?.content ?: currentCss
            val js = json["js"]?.jsonPrimitive?.content ?: currentJs
            val desc = json["changeDescription"]?.jsonPrimitive?.content ?: "Changes applied successfully."
            return GeneratedCode(html, css, js, desc)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse modifyCode response JSON: ${e.message}")
            val parsed = parseGeneratedCode(rawResponse)
            return GeneratedCode(
                html = if (parsed.html.isNotBlank()) parsed.html else currentHtml,
                css = if (parsed.css.isNotBlank()) parsed.css else currentCss,
                js = if (parsed.js.isNotBlank()) parsed.js else currentJs,
                changeDescription = "Applied edits to website."
            )
        }
    }
}

data class GeneratedCode(
    val html: String,
    val css: String,
    val js: String,
    val changeDescription: String? = null
)
