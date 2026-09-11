package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()

    suspend fun getProject(id: String): Project? {
        return projectDao.getProjectById(id)
    }
    
    fun getProjectFlow(id: String): Flow<Project?> {
        return projectDao.getProjectByIdFlow(id)
    }

    suspend fun createProject(name: String, prompt: String): String {
        val id = UUID.randomUUID().toString()
        val newProject = Project(
            id = id,
            name = name,
            originalPrompt = prompt
        )
        projectDao.insertOrUpdateProject(newProject)
        return id
    }

    suspend fun updateProject(project: Project) {
        projectDao.insertOrUpdateProject(project.copy(lastModifiedDate = System.currentTimeMillis()))
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteProjectById(id)
    }
}
