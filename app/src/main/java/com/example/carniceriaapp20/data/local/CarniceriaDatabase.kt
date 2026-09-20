package com.example.carniceriaapp20.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

const val CARNICERIA_DB_NAME = "carniceria_database"
const val CARNICERIA_DB_VERSION = 5

@Database(
    entities = [Product::class, Ticket::class, TicketItem::class, LabelHistory::class],
    version = CARNICERIA_DB_VERSION,
    exportSchema = true
)
abstract class CarniceriaDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun ticketDao(): TicketDao
    abstract fun labelHistoryDao(): LabelHistoryDao
}
