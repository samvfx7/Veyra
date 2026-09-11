package com.example.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class WebsiteGenerator(private val geminiService: GeminiService) {
    
    suspend fun enhancePrompt(originalPrompt: String): String {
        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("enhancedPrompt") {
                    put("type", "STRING")
                    put("description", "A highly detailed website specification based on the user's intent. Must include visual direction, color palette, typography direction, spacing, component styling, responsive requirements, and accessibility requirements.")
                }
            }
            putJsonObject("required") {
                // Not supported in all Gemini structured outputs yet depending on exact API shape, but we can leave schema as is.
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
            val json = Json.parseToJsonElement(jsonString).jsonObject
            return json["enhancedPrompt"]?.jsonPrimitive?.content ?: originalPrompt
        } catch (e: Exception) {
            return originalPrompt
        }
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
        
        val jsonString = geminiService.generateStructuredContent(enhancedPrompt, instruction, schema)
        
        try {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val html = json["html"]?.jsonPrimitive?.content ?: "<h1>Error generating HTML</h1>"
            val css = json["css"]?.jsonPrimitive?.content ?: ""
            val js = json["js"]?.jsonPrimitive?.content ?: ""
            return GeneratedCode(html, css, js)
        } catch (e: Exception) {
            return GeneratedCode("<h1>Error parsing response</h1><p>${e.message}</p>", "", "")
        }
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
        
        val jsonString = geminiService.generateStructuredContent(prompt, sysInstruction, schema)
        
        try {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val html = json["html"]?.jsonPrimitive?.content ?: currentHtml
            val css = json["css"]?.jsonPrimitive?.content ?: currentCss
            val js = json["js"]?.jsonPrimitive?.content ?: currentJs
            val desc = json["changeDescription"]?.jsonPrimitive?.content ?: "Changes applied."
            return GeneratedCode(html, css, js, desc)
        } catch (e: Exception) {
            throw Exception("Failed to modify code: ${e.message}")
        }
    }
}

data class GeneratedCode(
    val html: String,
    val css: String,
    val js: String,
    val changeDescription: String? = null
)
