package com.example.api

import android.util.Log

data class QualityReport(
    val score: Int,
    val isPass: Boolean,
    val issuesFound: List<String>,
    val appliedFixes: List<String>,
    val sanitizedHtml: String,
    val sanitizedCss: String
)

/**
 * Lightweight quality auditor that statically validates generated websites against
 * professional human design standards and automatically applies targeted corrections
 * without calling Gemini or regenerating the entire website.
 */
object QualityAuditor {
    private const val TAG = "QualityAuditor"

    fun auditAndSanitize(html: String, css: String): QualityReport {
        var currentHtml = html
        var currentCss = css
        val issues = mutableListOf<String>()
        val fixes = mutableListOf<String>()
        var score = 100

        // 1. Check & fix missing viewport meta tag
        if (!currentHtml.contains("name=\"viewport\"", ignoreCase = true) && !currentHtml.contains("name='viewport'", ignoreCase = true)) {
            issues.add("Missing viewport meta tag for mobile responsiveness.")
            val headIndex = currentHtml.indexOf("<head>", ignoreCase = true)
            if (headIndex != -1) {
                currentHtml = currentHtml.substring(0, headIndex + 6) +
                        "\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                        currentHtml.substring(headIndex + 6)
                fixes.add("Injected standard responsive viewport meta tag into <head>.")
            }
            score -= 5
        }

        // 2. Check & fix missing alt attributes on images
        val imgWithoutAltRegex = Regex("""<img\s+((?!alt=)[^>])*?>""", RegexOption.IGNORE_CASE)
        if (imgWithoutAltRegex.containsMatchIn(currentHtml)) {
            issues.add("Found image elements missing alt text.")
            currentHtml = imgWithoutAltRegex.replace(currentHtml) { match ->
                val tag = match.value
                if (tag.endsWith("/>")) {
                    tag.removeSuffix("/>") + " alt=\"Editorial photograph\" />"
                } else {
                    tag.removeSuffix(">") + " alt=\"Editorial photograph\">"
                }
            }
            fixes.add("Added accessible fallback alt attributes to image tags.")
            score -= 5
        }

        // 3. Detect & sanitize forbidden purple/blue AI gradients
        val aiGradientPattern = Regex(
            """linear-gradient\([^)]*?(?:#8[0-9a-f]{5}|#7[0-9a-f]{5}|#6[0-9a-f]{5}|purple|indigo|violet|rgba\(\s*1(?:2|3|4)\d)[^)]*?\)""",
            RegexOption.IGNORE_CASE
        )
        if (aiGradientPattern.containsMatchIn(currentCss)) {
            issues.add("Detected generic AI purple/blue gradient styling.")
            currentCss = aiGradientPattern.replace(currentCss, "var(--color-surface)")
            fixes.add("Replaced generic AI gradient with clean surface token.")
            score -= 10
        }

        // 4. Detect & sanitize neon/glowing borders
        val neonGlowPattern = Regex(
            """box-shadow:\s*0\s+0\s+(?:1[5-9]|[2-9]\d)px\s+[^;]+;""",
            RegexOption.IGNORE_CASE
        )
        if (neonGlowPattern.containsMatchIn(currentCss)) {
            issues.add("Detected high-intensity neon glow shadow effects.")
            currentCss = neonGlowPattern.replace(currentCss, "box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);")
            fixes.add("Softened neon glow to restrained subtle box-shadow.")
            score -= 5
        }

        // 5. Detect & normalize excessive border-radius on cards (> 28px)
        val excessiveRadiusPattern = Regex(
            """(border-radius:\s*)(?:[3-9]\d|1\d{2})px(\s*;)""",
            RegexOption.IGNORE_CASE
        )
        if (excessiveRadiusPattern.containsMatchIn(currentCss)) {
            issues.add("Detected excessive rounded corners on container elements.")
            currentCss = excessiveRadiusPattern.replace(currentCss, "$1var(--radius-md)$2")
            fixes.add("Normalized excessive container corner radius to --radius-md.")
            score -= 5
        }

        // 6. Check for missing CSS custom properties in :root
        if (!currentCss.contains(":root")) {
            issues.add("Missing centralized CSS :root design token block.")
            val defaultTokens = """
:root {
  --color-primary: #1e293b;
  --color-secondary: #475569;
  --color-accent: #c25e3d;
  --color-bg: #fdfcfb;
  --color-surface: #ffffff;
  --color-text: #0f172a;
  --color-text-muted: #64748b;
  --color-border: #e2e8f0;
  --font-display: 'Playfair Display', serif;
  --font-body: 'Plus Jakarta Sans', sans-serif;
  --space-sm: 16px;
  --space-md: 24px;
  --space-lg: 48px;
  --radius-sm: 4px;
  --radius-md: 8px;
}
            """.trimIndent()
            currentCss = "$defaultTokens\n\n$currentCss"
            fixes.add("Synthesized missing :root design token block into stylesheet.")
            score -= 10
        }

        // 7. Detect generic AI filler headlines ("Welcome to...", "Elevate your experience")
        val genericHeadlines = listOf("Welcome to our", "Welcome to", "Elevate your experience", "Empowering your journey")
        for (generic in genericHeadlines) {
            if (currentHtml.contains(generic, ignoreCase = true)) {
                issues.add("Found generic AI headline phrasing: '$generic'")
                score -= 5
                break
            }
        }

        // 8. Ensure prefers-reduced-motion is respected
        if (!currentCss.contains("prefers-reduced-motion")) {
            currentCss += """
                
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}
            """.trimIndent()
            fixes.add("Appended prefers-reduced-motion accessibility media query.")
        }

        Log.d(TAG, "Quality audit completed. Score: $score/100. Issues: ${issues.size}, Fixes applied: ${fixes.size}")

        return QualityReport(
            score = score.coerceIn(0, 100),
            isPass = score >= 75,
            issuesFound = issues,
            appliedFixes = fixes,
            sanitizedHtml = currentHtml,
            sanitizedCss = currentCss
        )
    }
}
