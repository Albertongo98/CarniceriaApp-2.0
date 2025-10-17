package com.example.carniceriaapp20.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "label_history")
data class LabelHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val price: String, // Stored as formatted string for direct use
    val code: String,
    val timestamp: Long = System.currentTimeMillis()
)
