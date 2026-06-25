package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workflows")
data class Workflow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val initialUrl: String,
    val createdAt: Long = System.currentTimeMillis(),
    val stepsJson: String // Serialized list of WorkflowStep
)

data class WorkflowStep(
    val id: String,
    val type: String, // "CLICK", "INPUT", "NAVIGATE", "WAIT"
    val target: String, // CSS selector, or URL for NAVIGATE
    val value: String = "", // text to type or wait duration in milliseconds
    val timestamp: Long = System.currentTimeMillis()
)
