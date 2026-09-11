package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String,
    val name: String,
    val originalPrompt: String,
    val enhancedPrompt: String? = null,
    val designSpecification: String? = null,
    val htmlContent: String = "",
    val cssContent: String = "",
    val jsContent: String = "",
    val previousHtml: String? = null,
    val previousCss: String? = null,
    val previousJs: String? = null,
    val creationDate: Long = System.currentTimeMillis(),
    val lastModifiedDate: Long = System.currentTimeMillis()
)
