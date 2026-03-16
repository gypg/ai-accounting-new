package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_butlers")
data class CustomButlerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val title: String,
    val description: String,

    // Avatar selection
    val avatarType: String, // RESOURCE | URI
    val avatarValue: String, // resource name or uri string

    // Basic interaction
    val userCallName: String, // how butler calls user
    val butlerSelfName: String, // how butler refers to itself

    // Personality sliders (0-100)
    val communicationStyle: Int,
    val emotionIntensity: Int,
    val professionalism: Int,
    val humor: Int,
    val proactivity: Int,

    // Feature preferences
    val featureFlagsJson: String, // JSON map: feature -> enabled
    val priorityJson: String, // JSON list: ordered module priorities

    // Prompt
    val systemPrompt: String,
    val promptVersion: Int,

    // Metadata
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)
