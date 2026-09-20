package com.example.carniceriaapp20.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class DaySalesReport(
    val dayStart: Long,
    val tickets: Int,
    val total: Double
)

data class DepartmentSalesReport(
    val department: String,
    val totalAmount: Double,
    val totalPieces: Double,
    val totalKilos: Double
)

data class ProductSalesReport(
    val productName: String,
    val productCode: String?,
    val totalAmount: Double,
    val totalQuantity: Double,
    val department: String,
    val unit: ProductUnit
)

@Dao
interface TicketDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: Ticket): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicketItems(items: List<TicketItem>)

    // El historial incluye los tickets anulados (se muestran marcados).
    @Transaction
    @Query("SELECT * FROM tickets ORDER BY timestamp DESC")
    fun getAllTicketsWithItems(): Flow<List<TicketWithItems>>

    @Transaction
    @Query("SELECT * FROM tickets WHERE id = :ticketId")
    fun getTicketWithItems(ticketId: Long): Flow<TicketWithItems?>

    @Query("SELECT * FROM ticket_items WHERE ticket_id = :ticketId")
    suspend fun getItemsOfTicket(ticketId: Long): List<TicketItem>

    @Query("SELECT * FROM tickets ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastTicket(): Ticket?

    @Query("""
        SELECT p.* FROM products p
        INNER JOIN (
            SELECT product_code, SUM(quantity) as totalQty
            FROM ticket_items ti
            INNER JOIN tickets t ON ti.ticket_id = t.id
            WHERE t.timestamp >= :since AND t.voided_at IS NULL
            GROUP BY product_code
        ) v ON p.code = v.product_code
        ORDER BY v.totalQty DESC
        LIMIT 10
    """)
    fun getTopSellingProducts(since: Long): Flow<List<Product>>

    // Cuenta también los anulados: el folio diario no se debe repetir.
    @Query("SELECT COUNT(*) FROM tickets WHERE timestamp >= :startOfDay")
    suspend fun countTicketsOfDay(startOfDay: Long): Int

    @Query("""
        SELECT
            ti.product_name as productName,
            ti.product_code as productCode,
            SUM(ti.total_price) as totalAmount,
            SUM(ti.quantity) as totalQuantity,
            ti.product_department as department,
            ti.unit as unit
        FROM ticket_items ti
        INNER JOIN tickets t ON ti.ticket_id = t.id
        WHERE t.timestamp >= :startTime AND t.timestamp < :endTime AND t.voided_at IS NULL
        GROUP BY ti.product_name, ti.product_code, ti.product_department, ti.unit
        ORDER BY totalAmount DESC
    """)
    suspend fun getSalesReportByProduct(startTime: Long, endTime: Long): List<ProductSalesReport>

    @Query("SELECT * FROM tickets WHERE timestamp >= :startTime AND timestamp < :endTime AND voided_at IS NULL ORDER BY timestamp")
    suspend fun getTicketsBetween(startTime: Long, endTime: Long): List<Ticket>

    // Devuelve las filas afectadas: 0 si el ticket no existe o ya estaba anulado.
    @Query("UPDATE tickets SET voided_at = :voidedAt, void_reason = :reason WHERE id = :ticketId AND voided_at IS NULL")
    suspend fun markVoided(ticketId: Long, voidedAt: Long, reason: String): Int
}
