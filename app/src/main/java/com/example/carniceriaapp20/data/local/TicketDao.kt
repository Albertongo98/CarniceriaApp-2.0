package com.example.carniceriaapp20.data.local

import androidx.room.*
import com.example.carniceriaapp20.ui.screens.history.TicketWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: Ticket): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicketItems(items: List<TicketItem>)

    @Transaction
    @Query("SELECT * FROM tickets ORDER BY timestamp DESC")
    fun getAllTicketsWithItems(): Flow<List<TicketWithItems>>
    
    @Transaction
    @Query("SELECT * FROM tickets WHERE id = :ticketId")
    fun getTicketWithItems(ticketId: Long): Flow<TicketWithItems?>

    @Query("SELECT * FROM tickets ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastTicket(): Ticket?
}
