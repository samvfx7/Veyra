package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiService
import com.example.api.WebsiteGenerator
import com.example.data.Project
import com.example.data.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EditorViewModel(
    private val repository: ProjectRepository,
    private val projectId: String
) : ViewModel() {

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project
    
    private val _editInstruction = MutableStateFlow("")
    val editInstruction: StateFlow<String> = _editInstruction
    
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing
    
    private val _editError = MutableStateFlow<String?>(null)
    val editError: StateFlow<String?> = _editError

    private val _lastChangeDescription = MutableStateFlow<String?>(null)
    val lastChangeDescription: StateFlow<String?> = _lastChangeDescription

    init {
        loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            repository.getProjectFlow(projectId).collect {
                _project.value = it
            }
        }
    }
    
    fun onEditInstructionChange(instruction: String) {
        _editInstruction.value = instruction
    }

    fun applyEdit() {
        val currentProject = _project.value ?: return
        val instruction = _editInstruction.value
        if (instruction.isBlank()) return
        
        _isEditing.value = true
        _editError.value = null
        
        viewModelScope.launch {
            try {
                val generator = WebsiteGenerator(GeminiService())
                val newCode = generator.modifyCode(
                    instruction,
                    currentProject.htmlContent,
                    currentProject.cssContent,
                    currentProject.jsContent
                )
                
                val updatedProject = currentProject.copy(
                    htmlContent = newCode.html,
                    cssContent = newCode.css,
                    jsContent = newCode.js,
                    lastModifiedDate = System.currentTimeMillis()
                )
                repository.updateProject(updatedProject)
                _lastChangeDescription.value = newCode.changeDescription
                _editInstruction.value = ""
            } catch (e: Exception) {
                _editError.value = e.message ?: "Failed to apply edit"
            } finally {
                _isEditing.value = false
            }
        }
    }
    
    fun dismissError() {
        _editError.value = null
    }
}
