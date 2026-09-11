package com.example.api

import android.util.Log

sealed class TaskClassification {
    /**
     * Trivial token or global variable change applied locally in CSS with 0 API tokens.
     */
    data class LocalTokenAdjustment(
        val updatedCss: String,
        val description: String
    ) : TaskClassification()

    /**
     * Request to add a brand new section/component (e.g. Testimonials, Pricing, FAQ).
     * Only the target section is synthesized and injected.
     */
    data class NewSectionRequest(
        val componentType: String,
        val instruction: String
    ) : TaskClassification()

    /**
     * Targeted edit to an existing component (e.g. Hero, Footer, Navigation).
     * Only the target component is passed to the fast/cheap model.
     */
    data class TargetedComponentEdit(
        val componentName: String,
        val instruction: String
    ) : TaskClassification()

    /**
     * Content or copy rewrite.
     */
    data class ContentRewrite(
        val instruction: String
    ) : TaskClassification()

    /**
     * Small CSS or interaction layout change.
     */
    data class StyleAdjustment(
        val instruction: String
    ) : TaskClassification()

    /**
     * Explicit request for a complete, total redesign from scratch.
     */
    data class FullRedesign(
        val instruction: String
    ) : TaskClassification()

    /**
     * General change requiring model assistance.
     */
    data class GeneralAiEdit(
        val taskType: GenerationTaskType,
        val instruction: String
    ) : TaskClassification()
}

object TaskClassifier {
    private const val TAG = "TaskClassifier"

    /**
     * Analyzes the user's edit instruction and determines the most token-efficient execution path.
     */
    fun classify(instruction: String, currentCss: String): TaskClassification {
        val lower = instruction.trim().lowercase()

        // 1. Check for explicit Full Redesign request
        if (isFullRedesign(lower)) {
            Log.d(TAG, "Classified as FullRedesign: $instruction")
            return TaskClassification.FullRedesign(instruction)
        }

        // 2. Check for Local Token Adjustments (Zero API tokens!)
        val localTokenResult = tryApplyLocalTokenChange(lower, currentCss)
        if (localTokenResult != null) {
            Log.d(TAG, "Classified as LocalTokenAdjustment (0 Gemini tokens): ${localTokenResult.description}")
            return localTokenResult
        }

        // 3. Check for New Section addition requests
        val newSectionType = detectNewSectionType(lower)
        if (newSectionType != null) {
            Log.d(TAG, "Classified as NewSectionRequest: $newSectionType")
            return TaskClassification.NewSectionRequest(newSectionType, instruction)
        }

        // 4. Check for Targeted Component Edit
        val targetComponent = detectTargetComponent(lower)
        if (targetComponent != null) {
            Log.d(TAG, "Classified as TargetedComponentEdit for: $targetComponent")
            return TaskClassification.TargetedComponentEdit(targetComponent, instruction)
        }

        // 5. Content or copy rewrite
        if (lower.contains("rewrite") || lower.contains("change headline") || lower.contains("wording") ||
            lower.contains("translate") || lower.contains("typo") || lower.contains("text to")) {
            Log.d(TAG, "Classified as ContentRewrite")
            return TaskClassification.ContentRewrite(instruction)
        }

        // 6. Style or layout tweak
        if (lower.contains("css") || lower.contains("layout") || lower.contains("hover") || lower.contains("animation") || lower.contains("align")) {
            Log.d(TAG, "Classified as StyleAdjustment")
            return TaskClassification.StyleAdjustment(instruction)
        }

        return TaskClassification.GeneralAiEdit(GenerationTaskType.COMPONENT_EDIT, instruction)
    }

    private fun isFullRedesign(lower: String): Boolean {
        return lower.contains("redesign the whole") ||
                lower.contains("redesign entire") ||
                lower.contains("start over from scratch") ||
                lower.contains("rebuild everything") ||
                lower.contains("completely redesign")
    }

    private fun detectNewSectionType(lower: String): String? {
        if (!lower.contains("add ") && !lower.contains("include ") && !lower.contains("insert ")) {
            return null
        }
        return when {
            lower.contains("testimonial") || lower.contains("review") -> "TestimonialSection"
            lower.contains("pricing") || lower.contains("tiers") || lower.contains("rate") -> "PricingSection"
            lower.contains("faq") || lower.contains("frequently asked") -> "FAQSection"
            lower.contains("team") || lower.contains("staff") || lower.contains("stylist") -> "TeamSection"
            lower.contains("gallery") || lower.contains("photo") || lower.contains("portfolio") -> "ImageGallery"
            lower.contains("booking") || lower.contains("appointment") || lower.contains("reservation") -> "BookingSection"
            lower.contains("contact") || lower.contains("inquiry") -> "ContactSection"
            lower.contains("service") -> "ServiceList"
            lower.contains("story") || lower.contains("history") || lower.contains("heritage") -> "StorySection"
            lower.contains("cta") || lower.contains("call to action") -> "CTASection"
            else -> null
        }
    }

    private fun detectTargetComponent(lower: String): String? {
        return when {
            lower.contains("hero") || lower.contains("banner") || lower.contains("header title") -> "Hero"
            lower.contains("footer") || lower.contains("copyright") -> "Footer"
            lower.contains("nav") || lower.contains("navigation") || lower.contains("menu") -> "Navigation"
            lower.contains("service") -> "Services"
            lower.contains("testimonial") || lower.contains("review") -> "Testimonials"
            lower.contains("pricing") -> "Pricing"
            lower.contains("booking") || lower.contains("reservation") -> "Booking"
            lower.contains("contact") || lower.contains("form") -> "Contact"
            lower.contains("gallery") -> "Gallery"
            lower.contains("button") -> "Button"
            else -> null
        }
    }

    /**
     * Evaluates whether the instruction can be fulfilled deterministically by updating CSS custom properties in :root.
     * Cost: 0 API tokens.
     */
    private fun tryApplyLocalTokenChange(lower: String, currentCss: String): TaskClassification.LocalTokenAdjustment? {
        if (!currentCss.contains(":root")) return null

        var modifiedCss = currentCss
        var changeDesc: String? = null

        // Warm Palette
        if (lower.contains("warm") || lower.contains("warmer") || lower.contains("earthy")) {
            modifiedCss = replaceCssVar(modifiedCss, "--color-bg", "#faf7f2")
            modifiedCss = replaceCssVar(modifiedCss, "--color-surface", "#ffffff")
            modifiedCss = replaceCssVar(modifiedCss, "--color-primary", "#2b221e")
            modifiedCss = replaceCssVar(modifiedCss, "--color-accent", "#c25e3d")
            modifiedCss = replaceCssVar(modifiedCss, "--color-border", "#ebe3d8")
            modifiedCss = replaceCssVar(modifiedCss, "--color-text", "#1c1714")
            changeDesc = "Applied warm artisanal color palette tokens locally."
        }
        // Dark Mode / Dark Theme
        else if (lower.contains("dark mode") || lower.contains("make it dark") || lower.contains("dark theme") || lower.contains("darker")) {
            modifiedCss = replaceCssVar(modifiedCss, "--color-bg", "#0f1117")
            modifiedCss = replaceCssVar(modifiedCss, "--color-surface", "#181a22")
            modifiedCss = replaceCssVar(modifiedCss, "--color-text", "#f4f4f5")
            modifiedCss = replaceCssVar(modifiedCss, "--color-text-muted", "#a1a1aa")
            modifiedCss = replaceCssVar(modifiedCss, "--color-border", "rgba(255, 255, 255, 0.08)")
            changeDesc = "Switched to sophisticated dark mode design tokens locally."
        }
        // Light Mode / Crisp Light Theme
        else if (lower.contains("light mode") || lower.contains("make it light") || lower.contains("light theme") || lower.contains("clean white")) {
            modifiedCss = replaceCssVar(modifiedCss, "--color-bg", "#fdfcfb")
            modifiedCss = replaceCssVar(modifiedCss, "--color-surface", "#ffffff")
            modifiedCss = replaceCssVar(modifiedCss, "--color-text", "#0f172a")
            modifiedCss = replaceCssVar(modifiedCss, "--color-text-muted", "#64748b")
            modifiedCss = replaceCssVar(modifiedCss, "--color-border", "#e2e8f0")
            changeDesc = "Switched to high-contrast light mode design tokens locally."
        }
        // Accent Colors
        else if (lower.contains("accent color to terracotta") || lower.contains("accent to rust")) {
            modifiedCss = replaceCssVar(modifiedCss, "--color-accent", "#c25e3d")
            changeDesc = "Updated accent token to terracotta (#c25e3d)."
        }
        else if (lower.contains("accent color to gold") || lower.contains("accent to bronze") || lower.contains("accent to brass")) {
            modifiedCss = replaceCssVar(modifiedCss, "--color-accent", "#c59b27")
            changeDesc = "Updated accent token to artisan gold (#c59b27)."
        }
        else if (lower.contains("accent color to emerald") || lower.contains("accent to green") || lower.contains("accent to sage")) {
            modifiedCss = replaceCssVar(modifiedCss, "--color-accent", "#2d6a4f")
            changeDesc = "Updated accent token to sage emerald (#2d6a4f)."
        }
        else if (lower.contains("accent color to navy") || lower.contains("accent to blue")) {
            modifiedCss = replaceCssVar(modifiedCss, "--color-accent", "#1d4ed8")
            changeDesc = "Updated accent token to deep navy (#1d4ed8)."
        }
        else if (lower.contains("accent color to burgundy") || lower.contains("accent to red")) {
            modifiedCss = replaceCssVar(modifiedCss, "--color-accent", "#831843")
            changeDesc = "Updated accent token to burgundy (#831843)."
        }
        // Border Radius
        else if (lower.contains("square") || lower.contains("sharp edges") || lower.contains("no rounded corners")) {
            modifiedCss = replaceCssVar(modifiedCss, "--radius-sm", "0px")
            modifiedCss = replaceCssVar(modifiedCss, "--radius-md", "0px")
            modifiedCss = replaceCssVar(modifiedCss, "--radius-lg", "0px")
            modifiedCss = replaceCssVar(modifiedCss, "--button-radius", "0px")
            changeDesc = "Normalized border radius tokens to sharp, clean edges."
        }
        else if (lower.contains("more rounded") || lower.contains("soft corners") || lower.contains("softer borders")) {
            modifiedCss = replaceCssVar(modifiedCss, "--radius-sm", "8px")
            modifiedCss = replaceCssVar(modifiedCss, "--radius-md", "16px")
            modifiedCss = replaceCssVar(modifiedCss, "--radius-lg", "24px")
            modifiedCss = replaceCssVar(modifiedCss, "--button-radius", "8px")
            changeDesc = "Updated border radius tokens for soft, restrained rounded styling."
        }
        // Spacing
        else if (lower.contains("more whitespace") || lower.contains("more spacing") || lower.contains("increase padding")) {
            modifiedCss = replaceCssVar(modifiedCss, "--space-sm", "20px")
            modifiedCss = replaceCssVar(modifiedCss, "--space-md", "32px")
            modifiedCss = replaceCssVar(modifiedCss, "--space-lg", "64px")
            modifiedCss = replaceCssVar(modifiedCss, "--space-xl", "112px")
            changeDesc = "Expanded spacing scale for generous editorial whitespace."
        }
        else if (lower.contains("less whitespace") || lower.contains("tighter spacing") || lower.contains("compact layout")) {
            modifiedCss = replaceCssVar(modifiedCss, "--space-sm", "12px")
            modifiedCss = replaceCssVar(modifiedCss, "--space-md", "18px")
            modifiedCss = replaceCssVar(modifiedCss, "--space-lg", "36px")
            modifiedCss = replaceCssVar(modifiedCss, "--space-xl", "64px")
            changeDesc = "Condensed spacing scale for a compact layout rhythm."
        }

        if (changeDesc != null && modifiedCss != currentCss) {
            return TaskClassification.LocalTokenAdjustment(modifiedCss, changeDesc)
        }
        return null
    }

    private fun replaceCssVar(css: String, varName: String, newValue: String): String {
        val regex = Regex("""($varName\s*:\s*)([^;]+)(;)""")
        return if (regex.containsMatchIn(css)) {
            regex.replace(css, "$1$newValue$3")
        } else {
            // If variable was not in :root, insert it into the first :root block
            val rootIndex = css.indexOf(":root")
            if (rootIndex != -1) {
                val braceIndex = css.indexOf("{", rootIndex)
                if (braceIndex != -1) {
                    css.substring(0, braceIndex + 1) + "\n  $varName: $newValue;" + css.substring(braceIndex + 1)
                } else css
            } else css
        }
    }
}
