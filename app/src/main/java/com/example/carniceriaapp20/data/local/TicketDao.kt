package com.example.carniceriaapp20.data.local

import androidx.room.*
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

    @Query("""
        SELECT p.* FROM products p
        INNER JOIN (
            SELECT product_code, SUM(quantity) as totalQty 
            FROM ticket_items 
            GROUP BY product_code
        ) v ON p.code = v.product_code
        ORDER BY v.totalQty DESC
        LIMIT 10
    """)
    fun getTopSellingProducts(): Flow<List<Product>>

    /**
     * TAREA DE MANTENIMIENTO: Borra tickets más viejos que el timestamp proporcionado.
     * Room se encargará de borrar los items asociados gracias al ForeignKey CASCADE.
     */
    @Query("DELETE FROM tickets WHERE timestamp < :threshold")
    suspend fun deleteTicketsOlderThan(threshold: Long)

    /**
     * Cuenta cuántos tickets se han hecho hoy para generar el Folio.
     */
    @Query("SELECT COUNT(*) FROM tickets WHERE timestamp >= :startOfDay")
    suspend fun countTicketsOfDay(startOfDay: Long): Int
}
