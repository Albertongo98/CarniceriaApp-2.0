package com.example.carniceriaapp20.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Product::class, Ticket::class, TicketItem::class, LabelHistory::class],
    version = 2, // Incremetamos la versión para aplicar cambios
    exportSchema = false
)
abstract class CarniceriaDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun ticketDao(): TicketDao
    abstract fun labelHistoryDao(): LabelHistoryDao
}
