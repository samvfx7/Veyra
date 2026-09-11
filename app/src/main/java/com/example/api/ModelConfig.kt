package com.example.api

/**
 * Central configuration for Gemini AI models and routing policies in Veyra.
 * All model tiers, priority lists, and resilience parameters are maintained here.
 */
object ModelConfig {
    // Priority 1: Best / highest-quality model for complex architecture, design specification, and difficult reasoning
    val BEST_QUALITY_MODELS = listOf(
        "gemini-2.5-pro",
        "gemini-3.1-pro",
        "gemini-3.6-flash"
    )

    // Priority 2: High-quality / efficient model for normal website generation, section synthesis, and component integration
    val EFFICIENT_MODELS = listOf(
        "gemini-3.6-flash",
        "gemini-2.5-flash",
        "gemini-flash-latest"
    )

    // Priority 3: Fast / cost-effective model for simple edits, content rewrites, small CSS adjustments, and quality evaluation
    val FAST_MODELS = listOf(
        "gemini-flash-latest",
        "gemini-2.5-flash-lite",
        "gemini-3.5-flash",
        "gemini-3.6-flash"
    )

    // Priority 4: Emergency fallback models when higher-tier models are exhausted or rate-limited
    val EMERGENCY_FALLBACK_MODELS = listOf(
        "gemini-flash-latest",
        "gemini-3.6-flash",
        "gemini-2.5-flash"
    )

    // Cooldown duration when a model hits HTTP 429 (Rate Limit / Quota Exceeded): 3 minutes
    const val RATE_LIMIT_COOLDOWN_MS = 3 * 60 * 1000L

    // Cooldown duration when a model returns HTTP 503 / 500 (Service Unavailable / Overloaded): 1 minute
    const val SERVER_ERROR_COOLDOWN_MS = 60 * 1000L

    // Max retry attempts per individual model call
    const val MAX_RETRIES_PER_MODEL = 2

    // Retry backoff delay
    const val RETRY_BACKOFF_DELAY_MS = 1000L
}

enum class ModelTier {
    BEST_QUALITY,
    EFFICIENT,
    FAST,
    EMERGENCY_FALLBACK
}
