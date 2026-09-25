package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val nameBangla: String,
    val nameEnglish: String,
    val isPhysical: Boolean,
    val iconName: String,
    val keywords: String, // comma-separated keywords for AI matching
    val minBudget: Double,
    val maxBudget: Double,
    val isActive: Boolean = true,
    val instantJobEnabled: Boolean = true,
    val instantJobRadiusKm: Double = 5.0
)
