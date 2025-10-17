package com.example.carniceriaapp20.data.repository

import com.example.carniceriaapp20.data.local.LabelHistory
import com.example.carniceriaapp20.data.local.LabelHistoryDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LabelHistoryRepositoryImpl @Inject constructor(
    private val labelHistoryDao: LabelHistoryDao
) : LabelHistoryRepository {

    override fun getAllLabels(): Flow<List<LabelHistory>> {
        return labelHistoryDao.getAllLabels()
    }

    override suspend fun insertLabel(label: LabelHistory) {
        labelHistoryDao.insertLabel(label)
    }

    override suspend fun deleteLabel(label: LabelHistory) {
        labelHistoryDao.deleteLabelById(label.id)
    }

    override suspend fun clearHistory() {
        labelHistoryDao.clearAll()
    }
}
