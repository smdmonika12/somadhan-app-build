package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "faqs")
data class FaqEntity(
    @PrimaryKey val id: String,
    val question: String,
    val answer: String,
    val targetAudience: String = "USER", // "USER" or "SOLVER"
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

