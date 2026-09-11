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

        private val DESIGN_SPEC_INSTRUCTION = """
            You are the Executive Creative Director and Principal Digital Architect at a world-class digital design agency.
            Your task is to take any website request and develop an exhaustive, compact Design Architecture Specification.

            CORE DESIGN PRINCIPLE:
            The generated website must prioritize professional visual hierarchy, usability, brand identity, typography, spacing, layout composition, and realistic content.
            It must NOT look like a generic AI landing page.
            Think like an experienced product designer and frontend developer creating a real website for a paying client.

            STRICTLY AVOID:
            - NEVER use purple/blue AI gradients, neon glows, or gradient text.
            - NEVER use glassmorphism or floating gradient blobs.
            - NEVER put every piece of information into rounded cards.
            - NEVER use generic headlines like "Welcome to..." or "Elevate your experience".
            - NEVER use fake statistics or fake testimonials.

            Select components strictly from this library:
            EditorialHero, LuxuryHero, MinimalHero, ServiceList, ServiceGrid, SplitContent, StorySection, ImageGallery, TeamSection, TestimonialSection, PricingSection, FAQSection, CTASection, BookingSection, ContactSection, LocationSection, Footer.
        """.trimIndent()

        private val ENHANCE_PROMPT_INSTRUCTION = """
            You are the Executive Creative Director and Principal Digital Architect at a world-class digital design agency (like Pentagram, Area 17, or Instrument).
            Your task is to take any website request and develop an exhaustive, bespoke Design Architecture Specification for a production-ready website.

            CORE PHILOSOPHY:
            Every website must look like it was designed and engineered by senior human designers and developers for a real paying client.
            It must NEVER look like an "AI-generated landing page" or generic template.

            STRICTLY AVOID (ZERO TOLERANCE):
            - NEVER recommend purple/blue "AI gradients", neon glowing borders, or floating gradient mesh blobs.
            - NEVER recommend turning every piece of information into cards or repeating three-column card grids across sections.
            - NEVER recommend generic hollow headlines like "Welcome to...", "Elevate your experience with our innovative solutions", or "Empower your journey".
            - NEVER invent fake statistical metrics ("99% Satisfaction", "10,000+ Happy Customers") or fake testimonials ("John Doe: Outstanding!").
            - NEVER recommend visual effects simply because they are possible. Every visual element must have a clear purpose.

            Output this as a comprehensive architectural blueprint ready for frontend implementation.
        """.trimIndent()

        private val GENERATE_CODE_INSTRUCTION = """
            You are a Senior Principal Frontend Engineer and Lead UI/UX Designer at a world-class digital design studio.
            You are building a bespoke, production-ready, client-deliverable website based on the provided design specification.

            CORE DESIGN PRINCIPLE:
            The generated website must prioritize professional visual hierarchy, usability, brand identity, typography, spacing, layout composition, and realistic content.
            It must NOT look like a generic AI landing page.
            Think like an experienced product designer and frontend developer creating a real website for a paying client.

            STRICTLY AVOID (ZERO TOLERANCE):
            - NO purple/blue AI gradients or rainbow gradient fills.
            - NO giant gradient text.
            - NO excessive glassmorphism or blurry frosted panels.
            - NO floating gradient blobs or decorative mesh spheres.
            - NO excessive rounded cards. Do NOT put every piece of information in a card.
            - NO repeated three-column card grids. Vary the layout across sections.
            - NO huge, centered, hollow hero sections with generic "Welcome to..." or "Empowering your journey..." headlines.
            - NO fake statistics ("99.9% Happy Clients", "10,000+ Projects") or generic testimonials ("John Doe: Great service!").
            - NO excessive pill-shaped buttons with heavy drop shadows.
            - NO excessive animations, glowing borders, or random geometric confetti.
            - NO emojis as icons. Use clean SVG icons or semantic typography.
            - NO generic SaaS dashboard aesthetics unless the user explicitly requested a SaaS tool.

            TECHNICAL DELIVERABLE REQUIREMENTS:
            1. HTML REQUIREMENTS:
               - Valid, semantic HTML5 in the "html" field.
               - In <head>: meta charset, viewport, Google Fonts <link>, and <link rel="stylesheet" href="styles.css">.
               - Semantic landmarks: <header>, <nav>, <main>, <section>, <article>, <aside>, <footer>.
               - Mobile navigation toggle button with aria-label="Toggle navigation menu" and aria-expanded="false".
               - Accessible mobile navigation drawer with navigation links and close button.
               - Accessible forms with explicit <label> elements for every input, select, or textarea.
               - Include <script src="script.js"></script> before </body>.

            2. CSS REQUIREMENTS:
               - Clean, modern, responsive CSS in the "css" field.
               - Define CSS custom properties in :root:
                 --color-primary, --color-secondary, --color-accent, --color-bg, --color-surface, --color-text, --color-text-muted, --color-border.
                 --font-display, --font-body.
                 --space-xs, --space-sm, --space-md, --space-lg, --space-xl.
                 --radius-sm, --radius-md, --radius-lg.
                 --button-padding, --button-radius.
               - Respect prefers-reduced-motion media query.
               - Fluid typography and responsive grid/flexbox.

            3. JAVASCRIPT REQUIREMENTS:
               - Vanilla JS in "js" field.
               - Mobile navigation toggle with aria-expanded and drawer open/close.
               - Accessible modals/accordions and form submit handling without full page reload.
               - Zero external library dependencies.
        """.trimIndent()

        private val SECTION_SYNTHESIS_INSTRUCTION = """
            You are a Senior Frontend Engineer creating a single targeted component for an existing website.
            Generate ONLY the HTML and CSS required for the requested section.
            Do NOT generate <html>, <head>, or <body> tags.
            Ensure the section:
            1. Conforms strictly to the provided Design Tokens and Component Blueprint.
            2. Contains realistic, domain-specific content (no fake reviews or stats).
            3. Uses semantic tags and responsive styling with CSS variables.
        """.trimIndent()

        private val COMPONENT_EDIT_INSTRUCTION = """
            You are a Senior Frontend Engineer modifying a specific component on an existing website.
            Modify only the provided component HTML and relevant CSS to satisfy the user's instruction.
            Preserve all existing design tokens, typography, and color variables.
            Do NOT regenerate unrelated sections.
        """.trimIndent()

        private val MODIFY_CODE_INSTRUCTION = """
            You are a Senior Principal Frontend Engineer and UI/UX Designer modifying an existing client website.
            Apply the user's requested changes while strictly adhering to professional design standards.

            CORE MANDATES:
            - Preserve and enhance the existing design system (typography, color variables, spacing scale, brand voice).
            - NEVER introduce generic AI tropes: no purple/blue gradients, no floating gradient blobs, no glowing borders, no cards for every piece of information.
            - Ensure all markup is semantic HTML5, accessible (labels, aria attributes, alt text), and responsive.
            - Ensure all CSS uses existing CSS custom properties and respects prefers-reduced-motion.
            - Output the COMPLETE modified HTML, CSS, and JS. Do not truncate.
        """.trimIndent()
    }

    /**
     * Generates a compact, structured Design Architecture Specification.
     * Stored in the project and reused throughout the session so Gemini never has to rediscover the visual identity.
     */
    suspend fun generateDesignSpecification(prompt: String): DesignSpecification {
        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("industry") { put("type", "STRING") }
                putJsonObject("targetAudience") { put("type", "STRING") }
                putJsonObject("brandPersonality") { put("type", "STRING") }
                putJsonObject("visualDirection") { put("type", "STRING") }
                putJsonObject("primaryColor") { put("type", "STRING") }
                putJsonObject("accentColor") { put("type", "STRING") }
                putJsonObject("bgColor") { put("type", "STRING") }
                putJsonObject("surfaceColor") { put("type", "STRING") }
                putJsonObject("textColor") { put("type", "STRING") }
                putJsonObject("displayFont") { put("type", "STRING") }
                putJsonObject("bodyFont") { put("type", "STRING") }
                putJsonObject("googleFontsUrl") { put("type", "STRING") }
                putJsonObject("heroComposition") { put("type", "STRING") }
                putJsonObject("sectionOrder") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
                putJsonObject("componentSelection") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("industry"))
                add(JsonPrimitive("brandPersonality"))
                add(JsonPrimitive("primaryColor"))
                add(JsonPrimitive("accentColor"))
                add(JsonPrimitive("displayFont"))
                add(JsonPrimitive("bodyFont"))
            }
        }

        return try {
            val raw = geminiService.generateStructuredContent(
                prompt = "Create a structured design architecture specification for this website request:\n$prompt\n\n${ComponentBlueprints.getBlueprintGuide()}",
                systemInstruction = DESIGN_SPEC_INSTRUCTION,
                schema = schema,
                taskType = GenerationTaskType.WEBSITE_ARCHITECTURE
            )
            parseDesignSpecification(raw, prompt)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate structured design spec: ${e.message}. Using high-craft default spec.")
            deriveDefaultSpecification(prompt)
        }
    }

    private fun parseDesignSpecification(rawJson: String, prompt: String): DesignSpecification {
        return try {
            val clean = cleanJsonString(rawJson)
            val json = Json.parseToJsonElement(clean).jsonObject
            val industry = json["industry"]?.jsonPrimitive?.content ?: "Bespoke Services"
            val brandPersonality = json["brandPersonality"]?.jsonPrimitive?.content ?: "Artisanal, High-Craft, Contemporary"
            val visualDirection = json["visualDirection"]?.jsonPrimitive?.content ?: "Editorial asymmetric layout with rich typography"
            val primary = json["primaryColor"]?.jsonPrimitive?.content ?: "#1e293b"
            val accent = json["accentColor"]?.jsonPrimitive?.content ?: "#c25e3d"
            val bg = json["bgColor"]?.jsonPrimitive?.content ?: "#fdfcfb"
            val surface = json["surfaceColor"]?.jsonPrimitive?.content ?: "#ffffff"
            val text = json["textColor"]?.jsonPrimitive?.content ?: "#0f172a"
            val displayFont = json["displayFont"]?.jsonPrimitive?.content ?: "Playfair Display"
            val bodyFont = json["bodyFont"]?.jsonPrimitive?.content ?: "Plus Jakarta Sans"
            val fontsUrl = json["googleFontsUrl"]?.jsonPrimitive?.content
                ?: "https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,400..900;1,400..900&family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap"
            val heroComp = json["heroComposition"]?.jsonPrimitive?.content ?: "Editorial asymmetric header"

            val sections = try {
                json["sectionOrder"]?.let { element ->
                    kotlinx.serialization.json.Json.decodeFromJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>()),
                        element
                    )
                } ?: listOf("Hero", "Services", "Story", "Gallery", "Booking", "Footer")
            } catch (_: Exception) {
                listOf("Hero", "Services", "Story", "Gallery", "Booking", "Footer")
            }

            val components = try {
                json["componentSelection"]?.let { element ->
                    kotlinx.serialization.json.Json.decodeFromJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>()),
                        element
                    )
                } ?: listOf("EditorialHero", "ServiceList", "SplitContent", "ImageGallery", "BookingSection", "Footer")
            } catch (_: Exception) {
                listOf("EditorialHero", "ServiceList", "SplitContent", "ImageGallery", "BookingSection", "Footer")
            }

            DesignSpecification(
                industry = industry,
                brandPersonality = brandPersonality,
                visualDirection = visualDirection,
                colorSystem = ColorTokens(
                    primary = primary,
                    secondary = "#475569",
                    accent = accent,
                    bg = bg,
                    surface = surface,
                    text = text,
                    textMuted = "#64748b",
                    border = "#e2e8f0"
                ),
                typographySystem = TypographyTokens(
                    displayFont = displayFont,
                    bodyFont = bodyFont,
                    googleFontsUrl = fontsUrl
                ),
                heroComposition = heroComp,
                sectionOrder = sections,
                componentSelection = components
            )
        } catch (e: Exception) {
            deriveDefaultSpecification(prompt)
        }
    }

    private fun deriveDefaultSpecification(prompt: String): DesignSpecification {
        val lower = prompt.lowercase()
        return when {
            lower.contains("barber") || lower.contains("salon") -> DesignSpecification(
                industry = "Grooming & Salon",
                brandPersonality = "Heritage, High-Craft, Artisanal",
                visualDirection = "Rich charcoal and warm amber palette with sharp editorial typography",
                colorSystem = ColorTokens(
                    primary = "#18181b",
                    secondary = "#3f3f46",
                    accent = "#d97706",
                    bg = "#0f1013",
                    surface = "#181a20",
                    text = "#f4f4f5",
                    textMuted = "#a1a1aa",
                    border = "rgba(255, 255, 255, 0.08)"
                ),
                typographySystem = TypographyTokens("Fraunces", "Plus Jakarta Sans"),
                componentSelection = listOf("EditorialHero", "ServiceList", "TeamSection", "ImageGallery", "BookingSection", "Footer")
            )
            lower.contains("restaurant") || lower.contains("bistro") || lower.contains("cafe") -> DesignSpecification(
                industry = "Dining & Culinary",
                brandPersonality = "Warm, Intimate, Artisanal",
                visualDirection = "Warm linen surfaces with terracotta accents and generous whitespace",
                colorSystem = ColorTokens(
                    primary = "#292524",
                    secondary = "#57534e",
                    accent = "#c25e3d",
                    bg = "#faf8f5",
                    surface = "#ffffff",
                    text = "#1c1917",
                    textMuted = "#78716c",
                    border = "#e7e5e4"
                ),
                typographySystem = TypographyTokens("Playfair Display", "Plus Jakarta Sans"),
                componentSelection = listOf("LuxuryHero", "SplitContent", "ServiceList", "BookingSection", "ContactSection", "Footer")
            )
            else -> DesignSpecification()
        }
    }

    suspend fun enhancePrompt(originalPrompt: String): String {
        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("enhancedPrompt") {
                    put("type", "STRING")
                    put("description", "A highly detailed website specification based on the user's intent.")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("enhancedPrompt"))
            }
        }

        return try {
            val jsonString = geminiService.generateStructuredContent(
                prompt = originalPrompt,
                systemInstruction = ENHANCE_PROMPT_INSTRUCTION,
                schema = schema,
                taskType = GenerationTaskType.WEBSITE_ARCHITECTURE
            )
            val clean = cleanJsonString(jsonString)
            val json = Json.parseToJsonElement(clean).jsonObject
            val enhanced = json["enhancedPrompt"]?.jsonPrimitive?.content
            if (!enhanced.isNullOrBlank()) enhanced else originalPrompt
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse enhancedPrompt JSON: ${e.message}. Using original prompt.")
            originalPrompt
        }
    }

    suspend fun generateCode(enhancedPrompt: String, designSpec: DesignSpecification? = null): GeneratedCode {
        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("html") {
                    put("type", "STRING")
                    put("description", "The complete, semantic HTML5 code.")
                }
                putJsonObject("css") {
                    put("type", "STRING")
                    put("description", "The complete, professional CSS code with :root tokens.")
                }
                putJsonObject("js") {
                    put("type", "STRING")
                    put("description", "The complete vanilla JavaScript code.")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("html"))
                add(JsonPrimitive("css"))
                add(JsonPrimitive("js"))
            }
        }

        val promptPayload = if (designSpec != null) {
            """
                $enhancedPrompt

                ${designSpec.toCompactSummary()}

                MANDATORY CSS ROOT TOKENS TO USE:
                ${designSpec.toCssRootBlock()}

                SELECTED COMPONENTS TO IMPLEMENT IN ORDER:
                ${designSpec.componentSelection.joinToString(" -> ")}
            """.trimIndent()
        } else {
            enhancedPrompt
        }

        val rawResponse = geminiService.generateStructuredContent(
            prompt = promptPayload,
            systemInstruction = GENERATE_CODE_INSTRUCTION,
            schema = schema,
            taskType = GenerationTaskType.FULL_GENERATION
        )

        val rawCode = parseGeneratedCode(rawResponse)

        // Perform lightweight quality audit and targeted sanitation (Point 8)
        val qualityReport = QualityAuditor.auditAndSanitize(rawCode.html, rawCode.css)
        return GeneratedCode(
            html = qualityReport.sanitizedHtml,
            css = qualityReport.sanitizedCss,
            js = rawCode.js,
            changeDescription = "Generated website based on professional design specification."
        )
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
            Log.w(TAG, "JSON parse failed for generated code: ${e.message}. Attempting regex fallback.")
        }

        // Regex markdown extraction fallback
        val htmlMatch = Regex("```(?:html)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE).find(rawResponse)?.groupValues?.get(1)?.trim()
        val cssMatch = Regex("```css\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE).find(rawResponse)?.groupValues?.get(1)?.trim()
        val jsMatch = Regex("```(?:javascript|js)\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE).find(rawResponse)?.groupValues?.get(1)?.trim()

        if (!htmlMatch.isNullOrBlank()) {
            return GeneratedCode(htmlMatch, cssMatch ?: "", jsMatch ?: "")
        }

        if (rawResponse.contains("<html", ignoreCase = true) || rawResponse.contains("<!DOCTYPE", ignoreCase = true)) {
            return GeneratedCode(rawResponse, "", "")
        }

        val fallbackHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Website</title>
                <link rel="stylesheet" href="styles.css">
            </head>
            <body>
                <main class="container">
                    <h1>Website Preview</h1>
                    <p>Your website structure has been updated.</p>
                </main>
                <script src="script.js"></script>
            </body>
            </html>
        """.trimIndent()

        return GeneratedCode(fallbackHtml, "body { margin: 0; padding: 2rem; font-family: sans-serif; }", "")
    }

    /**
     * Smart targeted modification engine:
     * - Classifies task before calling Gemini.
     * - Trivial color/font/spacing token edits execute locally in CSS with 0 API tokens.
     * - New section requests synthesize only that specific component and inject it cleanly.
     * - Component edits isolate that specific component snippet without re-sending the whole page.
     * - Full redesigns only occur when explicitly requested.
     */
    suspend fun modifyCode(
        instruction: String,
        currentHtml: String,
        currentCss: String,
        currentJs: String,
        designSpec: DesignSpecification? = null
    ): GeneratedCode {
        // 1. Task Classification
        val classification = TaskClassifier.classify(instruction, currentCss)

        when (classification) {
            // ZERO-AI: Local deterministic token adjustment
            is TaskClassification.LocalTokenAdjustment -> {
                Log.i(TAG, "Executing 0-AI local token adjustment for instruction: $instruction")
                val audited = QualityAuditor.auditAndSanitize(currentHtml, classification.updatedCss)
                return GeneratedCode(
                    html = audited.sanitizedHtml,
                    css = audited.sanitizedCss,
                    js = currentJs,
                    changeDescription = classification.description
                )
            }

            // TARGETED NEW SECTION: Generate ONLY the requested component
            is TaskClassification.NewSectionRequest -> {
                Log.i(TAG, "Executing targeted section synthesis for: ${classification.componentType}")
                return synthesizeTargetSection(
                    componentType = classification.componentType,
                    instruction = instruction,
                    currentHtml = currentHtml,
                    currentCss = currentCss,
                    currentJs = currentJs,
                    designSpec = designSpec
                )
            }

            // TARGETED COMPONENT EDIT: Send only target component snippet to fast model
            is TaskClassification.TargetedComponentEdit -> {
                Log.i(TAG, "Executing targeted component edit for: ${classification.componentName}")
                val existingSection = ComponentBlueprints.findComponentInHtml(currentHtml, classification.componentName)
                if (existingSection != null) {
                    return editSingleComponent(
                        componentName = classification.componentName,
                        componentHtml = existingSection,
                        instruction = instruction,
                        currentHtml = currentHtml,
                        currentCss = currentCss,
                        currentJs = currentJs,
                        designSpec = designSpec
                    )
                }
            }

            else -> {}
        }

        // Default / General AI Edit: Route to fast or efficient model based on task type
        val taskType = when (classification) {
            is TaskClassification.ContentRewrite -> GenerationTaskType.CONTENT_REWRITE
            is TaskClassification.StyleAdjustment -> GenerationTaskType.STYLE_CSS_ADJUSTMENT
            is TaskClassification.FullRedesign -> GenerationTaskType.FULL_GENERATION
            else -> GenerationTaskType.COMPONENT_EDIT
        }

        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("html") { put("type", "STRING") }
                putJsonObject("css") { put("type", "STRING") }
                putJsonObject("js") { put("type", "STRING") }
                putJsonObject("changeDescription") { put("type", "STRING") }
            }
            putJsonArray("required") {
                add(JsonPrimitive("html"))
                add(JsonPrimitive("css"))
                add(JsonPrimitive("js"))
                add(JsonPrimitive("changeDescription"))
            }
        }

        val grounding = designSpec?.toCompactSummary() ?: ""
        val prompt = """
            $grounding

            User Instruction: $instruction

            CURRENT HTML:
            $currentHtml

            CURRENT CSS:
            $currentCss

            CURRENT JS:
            $currentJs
        """.trimIndent()

        val rawResponse = geminiService.generateStructuredContent(
            prompt = prompt,
            systemInstruction = MODIFY_CODE_INSTRUCTION,
            schema = schema,
            taskType = taskType
        )

        val parsed = try {
            val clean = cleanJsonString(rawResponse)
            val json = Json.parseToJsonElement(clean).jsonObject
            val html = json["html"]?.jsonPrimitive?.content ?: currentHtml
            val css = json["css"]?.jsonPrimitive?.content ?: currentCss
            val js = json["js"]?.jsonPrimitive?.content ?: currentJs
            val desc = json["changeDescription"]?.jsonPrimitive?.content ?: "Changes applied successfully."
            GeneratedCode(html, css, js, desc)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse modifyCode response JSON: ${e.message}")
            val fallback = parseGeneratedCode(rawResponse)
            GeneratedCode(
                html = if (fallback.html.isNotBlank()) fallback.html else currentHtml,
                css = if (fallback.css.isNotBlank()) fallback.css else currentCss,
                js = if (fallback.js.isNotBlank()) fallback.js else currentJs,
                changeDescription = "Applied edits to website."
            )
        }

        val audited = QualityAuditor.auditAndSanitize(parsed.html, parsed.css)
        return parsed.copy(html = audited.sanitizedHtml, css = audited.sanitizedCss)
    }

    private suspend fun synthesizeTargetSection(
        componentType: String,
        instruction: String,
        currentHtml: String,
        currentCss: String,
        currentJs: String,
        designSpec: DesignSpecification?
    ): GeneratedCode {
        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("sectionHtml") {
                    put("type", "STRING")
                    put("description", "Semantic <section data-component=\"$componentType\">...</section> markup.")
                }
                putJsonObject("sectionCss") {
                    put("type", "STRING")
                    put("description", "CSS rules specific to this section, referencing CSS custom variables.")
                }
                putJsonObject("changeDescription") {
                    put("type", "STRING")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("sectionHtml"))
                add(JsonPrimitive("sectionCss"))
                add(JsonPrimitive("changeDescription"))
            }
        }

        val grounding = designSpec?.toCompactSummary() ?: ""
        val prompt = """
            $grounding

            Synthesize a new '$componentType' section for the website.
            User Request: $instruction

            EXISTING CSS VARIABLES AVAILABLE IN :root:
            ${designSpec?.toCssRootBlock() ?: ":root { --color-primary: #1e293b; --color-accent: #c25e3d; }"}
        """.trimIndent()

        val raw = geminiService.generateStructuredContent(
            prompt = prompt,
            systemInstruction = SECTION_SYNTHESIS_INSTRUCTION,
            schema = schema,
            taskType = GenerationTaskType.NEW_SECTION
        )

        return try {
            val clean = cleanJsonString(raw)
            val json = Json.parseToJsonElement(clean).jsonObject
            val newHtml = json["sectionHtml"]?.jsonPrimitive?.content ?: ""
            val newCss = json["sectionCss"]?.jsonPrimitive?.content ?: ""
            val desc = json["changeDescription"]?.jsonPrimitive?.content ?: "Added $componentType section."

            val mergedHtml = ComponentBlueprints.replaceOrInsertComponent(currentHtml, componentType, newHtml)
            val mergedCss = currentCss + "\n\n/* --- $componentType --- */\n" + newCss

            val audited = QualityAuditor.auditAndSanitize(mergedHtml, mergedCss)
            GeneratedCode(audited.sanitizedHtml, audited.sanitizedCss, currentJs, desc)
        } catch (e: Exception) {
            Log.e(TAG, "Failed section synthesis: ${e.message}. Falling back to standard edit.", e)
            modifyCode(instruction, currentHtml, currentCss, currentJs, designSpec)
        }
    }

    private suspend fun editSingleComponent(
        componentName: String,
        componentHtml: String,
        instruction: String,
        currentHtml: String,
        currentCss: String,
        currentJs: String,
        designSpec: DesignSpecification?
    ): GeneratedCode {
        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("modifiedComponentHtml") { put("type", "STRING") }
                putJsonObject("additionalCss") { put("type", "STRING") }
                putJsonObject("changeDescription") { put("type", "STRING") }
            }
            putJsonArray("required") {
                add(JsonPrimitive("modifiedComponentHtml"))
                add(JsonPrimitive("changeDescription"))
            }
        }

        val grounding = designSpec?.toCompactSummary() ?: ""
        val prompt = """
            $grounding

            Instruction: $instruction

            TARGET COMPONENT ($componentName) TO MODIFY:
            $componentHtml
        """.trimIndent()

        val raw = geminiService.generateStructuredContent(
            prompt = prompt,
            systemInstruction = COMPONENT_EDIT_INSTRUCTION,
            schema = schema,
            taskType = GenerationTaskType.COMPONENT_EDIT
        )

        return try {
            val clean = cleanJsonString(raw)
            val json = Json.parseToJsonElement(clean).jsonObject
            val modHtml = json["modifiedComponentHtml"]?.jsonPrimitive?.content ?: componentHtml
            val addCss = json["additionalCss"]?.jsonPrimitive?.content ?: ""
            val desc = json["changeDescription"]?.jsonPrimitive?.content ?: "Updated $componentName."

            val mergedHtml = ComponentBlueprints.replaceOrInsertComponent(currentHtml, componentName, modHtml)
            val mergedCss = if (addCss.isNotBlank()) currentCss + "\n" + addCss else currentCss

            val audited = QualityAuditor.auditAndSanitize(mergedHtml, mergedCss)
            GeneratedCode(audited.sanitizedHtml, audited.sanitizedCss, currentJs, desc)
        } catch (e: Exception) {
            Log.e(TAG, "Single component edit failed: ${e.message}. Falling back to standard edit.", e)
            modifyCode(instruction, currentHtml, currentCss, currentJs, designSpec)
        }
    }
}

data class GeneratedCode(
    val html: String,
    val css: String,
    val js: String,
    val changeDescription: String? = null
)
