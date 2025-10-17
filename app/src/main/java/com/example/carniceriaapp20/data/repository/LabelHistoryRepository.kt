package com.example.carniceriaapp20.data.repository

import com.example.carniceriaapp20.data.local.LabelHistory
import kotlinx.coroutines.flow.Flow

interface LabelHistoryRepository {
    fun getAllLabels(): Flow<List<LabelHistory>>
    suspend fun insertLabel(label: LabelHistory)
    suspend fun deleteLabel(label: LabelHistory)
    suspend fun clearHistory()
}
