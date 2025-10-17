package com.example.carniceriaapp20.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: LabelHistory)

    @Query("SELECT * FROM label_history ORDER BY timestamp DESC")
    fun getAllLabels(): Flow<List<LabelHistory>>

    @Query("DELETE FROM label_history WHERE id = :id")
    suspend fun deleteLabelById(id: Int)

    @Query("DELETE FROM label_history")
    suspend fun clearAll()
}
