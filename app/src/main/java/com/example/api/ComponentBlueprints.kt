package com.example.api

/**
 * Structured component library and blueprint definitions for Veyra.
 * Enforces semantic HTML5, accessible ARIA roles, responsive CSS variables,
 * and high-craft design conventions.
 */
object ComponentBlueprints {

    val ALL_COMPONENTS = listOf(
        "EditorialHero",
        "LuxuryHero",
        "MinimalHero",
        "ServiceGrid",
        "ServiceList",
        "SplitContent",
        "StorySection",
        "ImageGallery",
        "TeamSection",
        "TestimonialSection",
        "PricingSection",
        "FAQSection",
        "CTASection",
        "BookingSection",
        "ContactSection",
        "LocationSection",
        "Footer"
    )

    /**
     * Compact schema descriptions provided to Gemini during initial component selection
     * and targeted section generation.
     */
    fun getBlueprintGuide(): String {
        return """
STRUCTURED COMPONENT BLUEPRINTS:
- EditorialHero: Asymmetric typography lockup, eyebrow badge, high-impact headline, lead paragraph, dual CTA buttons, photographic visual anchor.
- LuxuryHero: Restrained, sophisticated layout with full-bleed architectural/atmosphere imagery, subtle reservation/booking bar, and elegant serif typography.
- MinimalHero: Typographically pure header with high contrast, generous whitespace, and direct conversion action.
- ServiceList: Editorial dot-leader or tabular list of services, transparent descriptions, durations, and prices (NOT generic cards).
- ServiceGrid: Clean 2-column or 3-column asymmetric grid with subtle borders, service scope, and inquiry trigger.
- SplitContent: Photographic feature split with story narrative, pull quote, and artisan sign-off.
- StorySection: Brand craft, heritage timeline, and philosophy paragraphs.
- ImageGallery: Curated editorial photography grid with intentional aspect ratios (4:5, 16:9), captions, and subtle hover states.
- TeamSection: Practitioner/artisan roster with portraits, titles, and credentials.
- TestimonialSection: High-trust quotes with client name, publication/role, and restrained quote mark styling (no fake star ratings).
- PricingSection: Transparent price tiers or service packages with itemized deliverables.
- FAQSection: Accessible interactive accordion (<details>/<summary> or vanilla JS) with clear answers to common client questions.
- CTASection: Bold, high-contrast banner with targeted inquiry or booking trigger.
- BookingSection: Interactive booking/consultation form with date picker, service dropdown, time slot selection, and contact details.
- ContactSection: Physical address, hours of operation, phone, email, and direct message form.
- LocationSection: Geographic directions, transit/parking instructions, operational schedule table, and map placeholder.
- Footer: Semantic <footer> with brand mark, categorized navigation links, legal statement, and social channels.
        """.trimIndent()
    }

    /**
     * Helper to detect component boundaries in generated HTML.
     */
    fun findComponentInHtml(html: String, componentName: String): String? {
        val pattern = Regex(
            """(<section[^>]*?(?:data-component=["']$componentName["']|class=["'][^"']*?$componentName[^"']*?["'])[\s\S]*?</section>)""",
            RegexOption.IGNORE_CASE
        )
        return pattern.find(html)?.groupValues?.get(1)
    }

    /**
     * Injects or replaces a component in the HTML.
     */
    fun replaceOrInsertComponent(
        currentHtml: String,
        componentName: String,
        newComponentHtml: String
    ): String {
        val existing = findComponentInHtml(currentHtml, componentName)
        if (existing != null) {
            return currentHtml.replace(existing, newComponentHtml)
        }

        // If not existing, insert before <footer> or before </body>
        val footerIndex = currentHtml.indexOf("<footer", ignoreCase = true)
        if (footerIndex != -1) {
            return currentHtml.substring(0, footerIndex) +
                    "\n$newComponentHtml\n\n" +
                    currentHtml.substring(footerIndex)
        }

        val bodyCloseIndex = currentHtml.indexOf("</body>", ignoreCase = true)
        if (bodyCloseIndex != -1) {
            return currentHtml.substring(0, bodyCloseIndex) +
                    "\n$newComponentHtml\n" +
                    currentHtml.substring(bodyCloseIndex)
        }

        return currentHtml + "\n" + newComponentHtml
    }
}
