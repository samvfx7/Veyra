package com.example.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ColorTokens(
    val primary: String = "#1e293b",
    val secondary: String = "#475569",
    val accent: String = "#c25e3d",
    val bg: String = "#fdfcfb",
    val surface: String = "#ffffff",
    val text: String = "#0f172a",
    val textMuted: String = "#64748b",
    val border: String = "#e2e8f0"
)

@Serializable
data class TypographyTokens(
    val displayFont: String = "Playfair Display",
    val bodyFont: String = "Plus Jakarta Sans",
    val googleFontsUrl: String = "https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,400..900;1,400..900&family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap"
)

@Serializable
data class SpacingTokens(
    val spaceXs: String = "8px",
    val spaceSm: String = "16px",
    val spaceMd: String = "24px",
    val spaceLg: String = "48px",
    val spaceXl: String = "80px",
    val sectionPaddingDesktop: String = "96px 24px",
    val sectionPaddingMobile: String = "56px 16px"
)

@Serializable
data class RadiusTokens(
    val radiusSm: String = "4px",
    val radiusMd: String = "8px",
    val radiusLg: String = "16px"
)

@Serializable
data class ButtonStyleTokens(
    val padding: String = "12px 28px",
    val radius: String = "6px",
    val style: String = "solid-contrast"
)

@Serializable
data class DesignSpecification(
    val industry: String = "General Business",
    val targetAudience: String = "Discerning clients",
    val brandPersonality: String = "Refined, Authoritative, Contemporary",
    val visualDirection: String = "Editorial high-craft with strong typographic rhythm",
    val colorSystem: ColorTokens = ColorTokens(),
    val typographySystem: TypographyTokens = TypographyTokens(),
    val spacingSystem: SpacingTokens = SpacingTokens(),
    val borderRadiusSystem: RadiusTokens = RadiusTokens(),
    val buttonStyle: ButtonStyleTokens = ButtonStyleTokens(),
    val navigationStyle: String = "Clean desktop header with brand mark + anchor links; accessible slide-out mobile drawer",
    val heroComposition: String = "Editorial asymmetric split layout with photographic backdrop and primary conversion trigger",
    val sectionOrder: List<String> = listOf("Hero", "Services", "Philosophy", "Gallery", "Booking", "Footer"),
    val componentSelection: List<String> = listOf("EditorialHero", "ServiceList", "SplitContent", "ImageGallery", "BookingSection", "Footer"),
    val animationDirection: String = "Subtle, restrained opacity and transform transitions with prefers-reduced-motion compliance",
    val mobileBehavior: String = "Drawer navigation, stacked high-contrast content blocks, touch targets >= 48px"
) {
    /**
     * Converts the design tokens into standard CSS custom properties for injection into :root.
     */
    fun toCssRootBlock(): String {
        return """
:root {
  /* Colors */
  --color-primary: ${colorSystem.primary};
  --color-secondary: ${colorSystem.secondary};
  --color-accent: ${colorSystem.accent};
  --color-bg: ${colorSystem.bg};
  --color-surface: ${colorSystem.surface};
  --color-text: ${colorSystem.text};
  --color-text-muted: ${colorSystem.textMuted};
  --color-border: ${colorSystem.border};

  /* Typography */
  --font-display: '${typographySystem.displayFont}', serif;
  --font-body: '${typographySystem.bodyFont}', -apple-system, BlinkMacSystemFont, sans-serif;

  /* Spacing */
  --space-xs: ${spacingSystem.spaceXs};
  --space-sm: ${spacingSystem.spaceSm};
  --space-md: ${spacingSystem.spaceMd};
  --space-lg: ${spacingSystem.spaceLg};
  --space-xl: ${spacingSystem.spaceXl};

  /* Border Radius */
  --radius-sm: ${borderRadiusSystem.radiusSm};
  --radius-md: ${borderRadiusSystem.radiusMd};
  --radius-lg: ${borderRadiusSystem.radiusLg};

  /* Buttons */
  --button-padding: ${buttonStyle.padding};
  --button-radius: ${buttonStyle.radius};
}
        """.trimIndent()
    }

    /**
     * Compact summary used to ground Gemini during subsequent targeted edits without
     * wasting thousands of tokens re-explaining visual identity.
     */
    fun toCompactSummary(): String {
        return """
[DESIGN SYSTEM GROUNDING]
- Industry: $industry | Voice: $brandPersonality
- Colors: bg=${colorSystem.bg}, text=${colorSystem.text}, primary=${colorSystem.primary}, accent=${colorSystem.accent}
- Fonts: Display='${typographySystem.displayFont}', Body='${typographySystem.bodyFont}'
- Radius: sm=${borderRadiusSystem.radiusSm}, md=${borderRadiusSystem.radiusMd}
- Components: ${componentSelection.joinToString(", ")}
- Strict Rules: No AI purple gradients, no floating blobs, no excessive cards, no fake stats.
        """.trimIndent()
    }

    fun toJson(): String {
        return Json.encodeToString(serializer(), this)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

        fun fromJson(raw: String?): DesignSpecification? {
            if (raw.isNullOrBlank()) return null
            return try {
                json.decodeFromString(serializer(), raw)
            } catch (e: Exception) {
                null
            }
        }
    }
}
