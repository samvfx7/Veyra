package com.example.api

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Task classification categories used to route requests to the most appropriate,
 * token-efficient, and cost-effective Gemini model tier.
 */
enum class GenerationTaskType {
    WEBSITE_ARCHITECTURE,   // Best-quality tier: Complex reasoning & design system specification
    FULL_GENERATION,        // Efficient tier: Generating full website from pre-defined specification
    NEW_SECTION,            // Efficient tier: Synthesizing a single new section
    COMPONENT_EDIT,         // Fast tier: Modifying a single existing component
    CONTENT_REWRITE,        // Fast tier: Rewriting copy or text
    STYLE_CSS_ADJUSTMENT,   // Fast tier: Small CSS tweaks
    LOCAL_TOKEN_CHANGE,     // Zero-AI: Local deterministic CSS token modification
    BUG_FIX,                // Efficient tier: Debugging layout or script issues
    QUALITY_CHECK           // Fast tier: Lightweight validation
}

/**
 * Smart AI Router that manages model selection based on task complexity,
 * enforces token efficiency, tracks model availability/cooldowns, and gracefully
 * switches models upon rate limits or errors.
 */
object ModelRouter {
    private const val TAG = "ModelRouter"

    // Cooldown timestamps: modelName -> epoch time (ms) until which model is unavailable
    private val modelCooldowns = ConcurrentHashMap<String, Long>()

    // Notification stream for UI when fallback models are engaged
    private val _systemNotice = MutableStateFlow<String?>(null)
    val systemNotice: StateFlow<String?> = _systemNotice.asStateFlow()

    /**
     * Checks if a model is currently eligible and not in a cooldown state.
     */
    fun isModelAvailable(model: String): Boolean {
        val cooldownUntil = modelCooldowns[model] ?: return true
        val now = System.currentTimeMillis()
        if (now >= cooldownUntil) {
            modelCooldowns.remove(model)
            Log.d(TAG, "Model $model cooldown expired. Restored to eligible pool.")
            return true
        }
        return false
    }

    /**
     * Marks a model as temporarily unavailable due to rate-limiting (429), quota limits, or server failure.
     */
    fun markModelUnavailable(model: String, reason: String, cooldownMs: Long) {
        val cooldownUntil = System.currentTimeMillis() + cooldownMs
        modelCooldowns[model] = cooldownUntil
        Log.w(TAG, "Marked model '$model' unavailable for ${cooldownMs / 1000}s. Reason: $reason")
    }

    /**
     * Returns an ordered list of eligible models for the specified task type.
     * Uses the cheapest/fastest model capable of high-quality execution for that task.
     */
    fun getEligibleModelsForTask(taskType: GenerationTaskType): List<String> {
        val primaryTier = when (taskType) {
            GenerationTaskType.WEBSITE_ARCHITECTURE -> ModelTier.BEST_QUALITY
            GenerationTaskType.FULL_GENERATION -> ModelTier.EFFICIENT
            GenerationTaskType.NEW_SECTION -> ModelTier.EFFICIENT
            GenerationTaskType.BUG_FIX -> ModelTier.EFFICIENT
            GenerationTaskType.COMPONENT_EDIT -> ModelTier.FAST
            GenerationTaskType.CONTENT_REWRITE -> ModelTier.FAST
            GenerationTaskType.STYLE_CSS_ADJUSTMENT -> ModelTier.FAST
            GenerationTaskType.QUALITY_CHECK -> ModelTier.FAST
            GenerationTaskType.LOCAL_TOKEN_CHANGE -> ModelTier.FAST
        }
        return getEligibleModels(primaryTier)
    }

    /**
     * Returns eligible models for a specific tier, falling back to emergency models if needed.
     */
    fun getEligibleModels(tier: ModelTier): List<String> {
        val candidates = when (tier) {
            ModelTier.BEST_QUALITY -> ModelConfig.BEST_QUALITY_MODELS + ModelConfig.EFFICIENT_MODELS + ModelConfig.EMERGENCY_FALLBACK_MODELS
            ModelTier.EFFICIENT -> ModelConfig.EFFICIENT_MODELS + ModelConfig.FAST_MODELS + ModelConfig.EMERGENCY_FALLBACK_MODELS
            ModelTier.FAST -> ModelConfig.FAST_MODELS + ModelConfig.EFFICIENT_MODELS + ModelConfig.EMERGENCY_FALLBACK_MODELS
            ModelTier.EMERGENCY_FALLBACK -> ModelConfig.EMERGENCY_FALLBACK_MODELS
        }

        val eligible = candidates.distinct().filter { isModelAvailable(it) }
        if (eligible.isNotEmpty()) {
            return eligible
        }

        // If all candidate models in the pool are marked unavailable due to cooldown,
        // take the one whose cooldown expires earliest as a last resort rather than failing completely.
        val earliestExpiring = candidates.distinct().minByOrNull { modelCooldowns[it] ?: 0L }
        return if (earliestExpiring != null) listOf(earliestExpiring) else ModelConfig.EMERGENCY_FALLBACK_MODELS
    }

    /**
     * Posts a clean, user-friendly notice when the system switches to an alternate model.
     */
    fun notifyModelSwitched(originalModel: String, fallbackModel: String) {
        Log.i(TAG, "Switched from $originalModel to available fallback model: $fallbackModel")
        _systemNotice.value = "Veyra switched to an available AI model."
    }

    fun clearNotice() {
        _systemNotice.value = null
    }

    /**
     * Resets all session cooldowns (useful for testing or manual user retry).
     */
    fun resetSession() {
        modelCooldowns.clear()
        _systemNotice.value = null
    }
}
