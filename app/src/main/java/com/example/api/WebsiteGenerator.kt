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

            THE 11 MANDATORY DESIGN DECISIONS YOU MUST FORMULATE:
            1. BUSINESS TYPE & INDUSTRY CONTEXT: Identify the exact industry niche, business model, and physical operational reality.
            2. TARGET AUDIENCE & POSITIONING: Specific clientele demographics, taste level, expectations, and trust triggers.
            3. BRAND PERSONALITY & VOICE: Tailored tone (e.g. Warm Artisanal, Authoritative Heritage, Restrained Luxury, Clinical Precision, Bold Editorial, High-Craft Hospitality).
            4. BESPOKE SECTION COMPOSITION & LAYOUT RHYTHM:
               - Detail a varied sequence of section structures. Strictly forbid repeating identical 3-card grids.
               - Industry-specific layouts:
                 * Barber / Salon: Strong photographic hero, service & pricing list/table (dot leaders or subtle borders, NOT cards), master barber/stylist roster, shop interior gallery, operational hours table, mobile booking flow.
                 * Restaurant / Dining: Atmosphere/food imagery hero, culinary philosophy split layout, categorized menu with descriptions & prices, table reservation flow, hours & location.
                 * Law Firm / Advisory: Conservative high-trust typography, practice areas list or clean accordion, attorney profiles with bar credentials, confidential consultation CTA.
                 * Hotel / Hospitality: Immersive architectural photography, rooms & suites showcase with specs (dimensions, bed, views), amenities, experiences, direct reservation bar.
                 * Real Estate / Architecture: Property imagery, interactive search/filter interface, verified property listings, neighborhood guide, agent contact.
                 * Studio / Agency: Bold typography-driven hero, asymmetrical case study showcases, client roster, capabilities list, inquiry form.
                 * Medical / Clinic: Calm reassuring palette, practitioner credentials, specialized treatments, patient intake / appointment scheduling.
            5. CONTENT HIERARCHY & REALISTIC COPY:
               - Specific, authentic copy based strictly on the business.
               - If facts (address, phone, hours, prices) are not provided, specify clearly marked bracketed placeholders: [124 Mercer Street, Soho], [(212) 555-0198], [Tuesday–Sunday: 11:30 AM – 10:00 PM], [Pricing upon consultation].
            6. TYPOGRAPHY SYSTEM:
               - Select maximum 2 Google Font families tailored to the business (e.g., Playfair Display + Plus Jakarta Sans, DM Serif Display + DM Sans, Cormorant Garamond + Inter, Syne + Plus Jakarta Sans, Cinzel + Lato, Fraunces + Outfit, Space Grotesk + Inter, Lora + Work Sans).
               - Define explicit roles: Display Header, H1-H3, Body Copy, Small Uppercase Eyebrow tracking (+0.08em).
            7. COLOR SYSTEM:
               - Specify 8 semantic hex tokens: --color-primary, --color-secondary, --color-accent, --color-bg, --color-surface, --color-text, --color-text-muted, --color-border.
               - Avoid defaulting to purple/blue/neon gradients. Use organic, contextual palettes (charcoals, warm creams, terracottas, sage, navy, espresso, bronze, slate).
               - If dark theme is requested: sophisticated dark surfaces (#121417, #18181b, #0f1117), controlled contrast, subtle borders (rgba(255,255,255,0.08)), restrained accents. NO glowing effects.
            8. SPACING & RHYTHM SYSTEM:
               - Generous whitespace: 80-120px desktop section padding, 48-64px mobile section padding. Fluid component gap scale (8px, 16px, 24px, 32px, 48px).
            9. IMAGE DIRECTION:
               - Concrete subject matter, aspect ratios (16:9, 4:5, 1:1), and curated Unsplash query themes.
            10. NAVIGATION & MOBILE DRAWER:
                - Desktop header with brand mark, anchor links, and distinct CTA button.
                - Accessible mobile drawer with toggle button and close action.
            11. CONVERSION GOALS & CORE INTERACTION:
                - Primary conversion flow (booking, reservation, consultation request, inquiry) with realistic interaction pattern.

            Output this as a comprehensive, structured architectural blueprint ready for frontend implementation.
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
            - Do not add visual effects simply because they are technically possible. Every visual element must have a purpose.

            BUSINESS-SPECIFIC DESIGN PATTERNS:
            - BARBER / SALON / GROOMING:
              * Hero: Asymmetrical layout or high-impact photographic header with left-aligned editorial typography and clear booking CTA.
              * Services & Pricing: Clean tabular layout or dot-leader price list with service descriptions and duration (NOT 3 cards).
              * Barber/Team: Stylist portraits with specialties and years of craft.
              * Atmosphere / Gallery: Grid of curated shop interior and grooming photos.
              * Location & Hours: Clear operational hours table, address, and mobile-friendly booking modal or form.
            - RESTAURANT / BISTRO / DINING:
              * Hero: Strong food & atmosphere imagery, restaurant identity, immediate reservation CTA.
              * Culinary Story: Editorial split layout about culinary philosophy and locally sourced ingredients.
              * Menu: Categorized menu (Starters, Mains, Desserts, Cocktails) with dish names, descriptions, dietary tags (GF, V), and prices in clean typographic rows.
              * Reservation: Dedicated reservation section or interactive modal (date, time, guests, seating).
              * Location & Hours: Lunch/Dinner service hours, address, parking/transit note.
            - LAW FIRM / ADVISORY / FINANCE:
              * Tone: Conservative typography, strong information hierarchy, trust-focused layout.
              * Practice Areas: Detailed list or interactive accordion with specific legal scopes.
              * Attorneys: Partner profiles with bar admissions, education, and credentials.
              * Credentials: Realistic bar affiliations or recognitions.
              * Consultation: Confidential case evaluation form.
              * Strictly avoid flashy gradients and excessive animation.
            - HOTEL / RESORT / HOSPITALITY:
              * Hero: Large immersive architectural photography with date/guest booking bar.
              * Rooms & Suites: Detailed suite showcase with square footage, bed configuration, views, and amenities.
              * Experiences & Amenities: Spa, dining, curated local activities.
              * Booking Bar: Interactive dates/guests check-in bar.
            - REAL ESTATE / ARCHITECTURE:
              * Hero: Striking architectural photography with property highlight.
              * Search/Filter: Interactive filter bar (Location, Type, Price, Beds).
              * Property Listings: High-resolution listing cards with real architectural metrics (sqft, bedrooms, baths, location, price).
              * Agent / Brokerage: Profile and direct private viewing request.
            - CREATIVE STUDIO / PORTFOLIO:
              * Hero: Bold editorial typography-driven lockup with concise mission statement.
              * Selected Work: Asymmetrical case study showcases with full-width or offset project imagery.
              * Capabilities / Services: Typographic index with detailed deliverables.
              * Client List & Inquiry: Clean tabular client list and project inquiry form.

            TECHNICAL DELIVERABLE REQUIREMENTS:

            1. HTML REQUIREMENTS:
               - Return valid, semantic HTML5 in the "html" field.
               - In <head>, you MUST include:
                 * <meta charset="UTF-8">
                 * <meta name="viewport" content="width=device-width, initial-scale=1.0">
                 * <link rel="preconnect" href="https://fonts.googleapis.com">
                 * <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                 * Real Google Fonts stylesheet <link> for the selected 1-2 font families.
                 * <link rel="stylesheet" href="styles.css">
               - Semantic landmarks: <header>, <nav>, <main>, <section>, <article>, <aside>, <footer>, <figure>, <time>.
               - Mobile navigation toggle button with aria-label="Toggle navigation menu" and aria-expanded="false".
               - Accessible mobile navigation drawer with navigation links and close button.
               - Accessible forms with explicit <label> elements for every input, select, or textarea.
               - Include <script src="script.js"></script> before </body>.

            2. CSS REQUIREMENTS:
               - Return clean, modern, responsive CSS in the "css" field.
               - Define CSS custom properties in :root:
                 --color-primary, --color-secondary, --color-accent, --color-bg, --color-surface, --color-text, --color-text-muted, --color-border.
                 --font-display, --font-body.
               - If dark theme is requested, use sophisticated dark surfaces (#121417, #18181b, #0f1117), crisp readable text (#f4f4f5), subtle borders (1px solid rgba(255,255,255,0.08)), and restrained accents. NO neon glows or purple/blue gradients.
               - Responsive grid and flexbox layouts with varied rhythm (asymmetrical splits, editorial lists, full-width photo breaks, tabular rows).
               - Generous, intentional whitespace: 80-120px desktop section padding, 48-64px mobile section padding. Max container width: 1200px centered.
               - Fluid typography using clamp() or clean responsive breakpoints.
               - Subtle, purposeful interactions: clear hover states, button feedback, focus-visible outlines.
               - Images: aspect-ratio, object-fit: cover, subtle border or frame.
               - Respect prefers-reduced-motion:
                 @media (prefers-reduced-motion: reduce) {
                   *, *::before, *::after {
                     animation-duration: 0.01ms !important;
                     animation-iteration-count: 1 !important;
                     transition-duration: 0.01ms !important;
                     scroll-behavior: auto !important;
                   }
                 }
               - Deliberate mobile responsiveness:
                 * Responsive hamburger menu and drawer styles.
                 * Touch targets >= 44px x 44px.
                 * Clean overflow handling for tables and lists.

            3. JAVASCRIPT REQUIREMENTS:
               - Return clean, modular vanilla JavaScript in the "js" field.
               - Mobile navigation toggle:
                 * Opens/closes mobile menu drawer.
                 * Updates aria-expanded attribute on hamburger button.
                 * Closes menu when clicking links or backdrop.
               - Interactive components:
                 * If booking/reservation/inquiry modal exists: open button, close button, backdrop click dismissal, Escape key listener.
                 * If tabs or accordions exist: clean tab switching and aria-selected / hidden state management.
               - Form handling:
                 * Event listener on forms to prevent default page reload, validate inputs, and display a polite, styled success/confirmation notice.
               - Smooth anchor scrolling.
               - Zero runtime errors, zero external JS library dependencies.

            4. REALISTIC CONTENT & PLACEHOLDERS:
               - Generate realistic, concise content appropriate for the business.
               - Never invent factual claims (awards, years of experience, customer numbers, reviews, prices, addresses) unless provided.
               - Use clearly marked bracketed placeholders: [124 Mercer Street, Soho, NY], [(212) 555-0198], [Tuesday – Sunday: 11:30 AM – 10:00 PM], [Price upon consultation].

            5. IMAGERY:
               - Use high-quality, topic-relevant Unsplash images with proper dimensions and aspect ratios:
                 e.g., https://images.unsplash.com/photo-[id]?auto=format&fit=crop&w=1200&q=80 or curated Unsplash photography IDs matching the industry (barber, culinary, architecture, law, hotel).
               - Every <img> must have a descriptive, meaningful alt attribute.

            DESIGN REVIEW BEFORE OUTPUT:
            Before returning generated code, internally evaluate the design:
            - Does this look like a professional agency-built website?
            - Does the design fit the specific business?
            - Is the typography intentional?
            - Is the spacing consistent?
            - Are there unnecessary cards?
            - Are there unnecessary gradients?
            - Are there unnecessary animations?
            - Does it look like a generic AI template?
            - Does every section have a purpose?
            - Would a real business owner be comfortable publishing this?
            If the answer to the AI-template question is yes, redesign it before returning the code.
        """.trimIndent()

        private val MODIFY_CODE_INSTRUCTION = """
            You are a Senior Principal Frontend Engineer and UI/UX Designer modifying an existing client website.
            Apply the user's requested changes while strictly adhering to the highest professional design standards.

            CORE MANDATES:
            - Preserve and enhance the existing design system (typography, color variables, spacing scale, brand voice).
            - NEVER introduce generic AI tropes: no purple/blue gradients, no floating gradient blobs, no glowing borders, no cards for every piece of information, no repeated 3-column card grids.
            - Ensure all new markup is semantic HTML5, accessible (labels, aria attributes, alt text), and responsive.
            - Ensure all new CSS uses existing CSS custom properties and respects prefers-reduced-motion.
            - Ensure all new JavaScript is clean vanilla JS, handles user interactions gracefully, and throws no errors.
            - If adding content, write realistic, business-specific copy or use bracketed placeholders [Like This] for missing facts.
            - Output the COMPLETE modified HTML, CSS, and JS. Do not truncate or omit any section.
        """.trimIndent()
    }
    
    suspend fun enhancePrompt(originalPrompt: String): String {
        val schema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("enhancedPrompt") {
                    put("type", "STRING")
                    put("description", "A highly detailed website specification based on the user's intent. Must include business type, target audience, brand personality, bespoke layout patterns, content hierarchy, typography system, color system, spacing, image direction, navigation, and conversion goals.")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("enhancedPrompt"))
            }
        }
        
        val jsonString = geminiService.generateStructuredContent(originalPrompt, ENHANCE_PROMPT_INSTRUCTION, schema)
        
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
                    put("description", "The complete, semantic HTML5 code. Must include Google Fonts link in <head>, semantic landmark tags, and reference styles.css and script.js.")
                }
                putJsonObject("css") {
                    put("type", "STRING")
                    put("description", "The complete, professional CSS code. Must define CSS variables in :root, fluid typography, responsive grid/flexbox, and respect prefers-reduced-motion.")
                }
                putJsonObject("js") {
                    put("type", "STRING")
                    put("description", "The complete, functional vanilla JavaScript code handling mobile navigation toggle, modal/accordion interactions, and accessible form handling.")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("html"))
                add(JsonPrimitive("css"))
                add(JsonPrimitive("js"))
            }
        }
        
        val rawResponse = geminiService.generateStructuredContent(enhancedPrompt, GENERATE_CODE_INSTRUCTION, schema)
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
        
        val prompt = """
            User Instruction: $instruction
            
            CURRENT HTML:
            $currentHtml
            
            CURRENT CSS:
            $currentCss
            
            CURRENT JS:
            $currentJs
        """.trimIndent()
        
        val rawResponse = geminiService.generateStructuredContent(prompt, MODIFY_CODE_INSTRUCTION, schema)
        
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
