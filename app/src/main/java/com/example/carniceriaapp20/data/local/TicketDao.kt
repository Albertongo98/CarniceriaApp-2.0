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

    /**
     * Obtiene los productos más vendidos uniendo los ítems de tickets con la tabla de productos.
     * Los ordena por la suma total de cantidad vendida.
     * Corregido: se usa 'product_code' que es el nombre real de la columna en la BD.
     */
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
}
