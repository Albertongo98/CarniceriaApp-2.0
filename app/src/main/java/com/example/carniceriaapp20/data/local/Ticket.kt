package com.example.carniceriaapp20.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tickets", indices = [Index(value = ["timestamp"])])
data class Ticket(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "total_amount")
    val totalAmount: Double,

    @ColumnInfo(name = "daily_folio")
    val dailyFolio: Int = 0,

    // Un ticket anulado NO se borra (el folio diario se calcula contando y la auditoría lo necesita):
    // se marca aquí y los reportes lo excluyen.
    @ColumnInfo(name = "voided_at")
    val voidedAt: Long? = null,

    @ColumnInfo(name = "void_reason")
    val voidReason: String? = null
)

val Ticket.isVoided: Boolean
    get() = voidedAt != null
