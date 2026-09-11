package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiService
import com.example.api.WebsiteGenerator
import com.example.data.Project
import com.example.data.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    val recentProjects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val prompt = MutableStateFlow("")
    val enhancePromptEnabled = MutableStateFlow(true)
    
    val generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)

    fun onPromptChange(newPrompt: String) {
        prompt.value = newPrompt
    }

    fun toggleEnhancePrompt() {
        enhancePromptEnabled.value = !enhancePromptEnabled.value
    }
    
    fun setExamplePrompt(example: String) {
        prompt.value = example
    }

    fun generateWebsite(onComplete: (String) -> Unit) {
        if (prompt.value.isBlank()) return
        
        viewModelScope.launch {
            try {
                val userPrompt = prompt.value
                val isEnhanceEnabled = enhancePromptEnabled.value
                
                generationState.value = GenerationState.Understanding
                val generator = WebsiteGenerator(GeminiService())
                
                val finalPrompt = if (isEnhanceEnabled) {
                    generationState.value = GenerationState.Enhancing
                    generator.enhancePrompt(userPrompt)
                } else {
                    userPrompt
                }
                
                generationState.value = GenerationState.Architecting
                // Simulate slight delay for UX architecture step if needed, or proceed directly
                
                generationState.value = GenerationState.Generating
                val generatedCode = generator.generateCode(finalPrompt)
                
                generationState.value = GenerationState.PreparingPreview
                
                val projectId = repository.createProject(
                    name = determineProjectName(userPrompt),
                    prompt = userPrompt
                )
                
                val project = repository.getProject(projectId)
                if (project != null) {
                    val updated = project.copy(
                        enhancedPrompt = if (isEnhanceEnabled) finalPrompt else null,
                        htmlContent = generatedCode.html,
                        cssContent = generatedCode.css,
                        jsContent = generatedCode.js,
                        lastModifiedDate = System.currentTimeMillis()
                    )
                    repository.updateProject(updated)
                    generationState.value = GenerationState.Complete
                    onComplete(projectId)
                } else {
                    generationState.value = GenerationState.Error("Failed to save project")
                }
                
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is com.example.api.GeminiApiException -> e.message ?: "Gemini API Error (${e.httpCode})"
                    else -> e.localizedMessage ?: e.message ?: "An unexpected error occurred during generation."
                }
                generationState.value = GenerationState.Error(errorMsg)
            }
        }
    }
    
    fun clearPrompt() {
        prompt.value = ""
    }

    fun dismissError() {
        generationState.value = GenerationState.Idle
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }

    private fun determineProjectName(prompt: String): String {
        val cleanWords = prompt
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 1 && !listOf("a", "an", "the", "for", "with", "and", "in", "of").contains(it.lowercase()) }
        
        return when {
            cleanWords.size >= 3 -> {
                "${cleanWords[0]} ${cleanWords[1]} ${cleanWords[2]}"
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            cleanWords.size == 2 -> {
                "${cleanWords[0]} ${cleanWords[1]}"
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            cleanWords.size == 1 -> {
                cleanWords[0].replaceFirstChar { it.uppercase() } + " Website"
            }
            else -> "Modern Web Project"
        }
    }
    
    fun resetState() {
        generationState.value = GenerationState.Idle
    }
}

sealed class GenerationState {
    object Idle : GenerationState()
    object Understanding : GenerationState()
    object Enhancing : GenerationState()
    object Architecting : GenerationState()
    object Generating : GenerationState()
    object PreparingPreview : GenerationState()
    object Complete : GenerationState()
    data class Error(val message: String) : GenerationState()
}
