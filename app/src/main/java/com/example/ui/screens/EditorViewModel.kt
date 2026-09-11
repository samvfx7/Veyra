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
                    previousHtml = currentProject.htmlContent,
                    previousCss = currentProject.cssContent,
                    previousJs = currentProject.jsContent,
                    htmlContent = newCode.html,
                    cssContent = newCode.css,
                    jsContent = newCode.js,
                    lastModifiedDate = System.currentTimeMillis()
                )
                repository.updateProject(updatedProject)
                _lastChangeDescription.value = newCode.changeDescription
                _editInstruction.value = ""
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is com.example.api.GeminiApiException -> e.message ?: "Failed to apply edit"
                    else -> e.localizedMessage ?: e.message ?: "Failed to apply edit"
                }
                _editError.value = errorMsg
            } finally {
                _isEditing.value = false
            }
        }
    }
    
    fun undoEdit() {
        val currentProject = _project.value ?: return
        if (currentProject.previousHtml == null) return

        viewModelScope.launch {
            val restoredProject = currentProject.copy(
                htmlContent = currentProject.previousHtml,
                cssContent = currentProject.previousCss ?: "",
                jsContent = currentProject.previousJs ?: "",
                previousHtml = null,
                previousCss = null,
                previousJs = null,
                lastModifiedDate = System.currentTimeMillis()
            )
            repository.updateProject(restoredProject)
            _lastChangeDescription.value = "Reverted to previous version"
        }
    }

    fun renameProject(newName: String) {
        val current = _project.value ?: return
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateProject(current.copy(name = newName.trim()))
        }
    }

    fun dismissError() {
        _editError.value = null
    }
}
