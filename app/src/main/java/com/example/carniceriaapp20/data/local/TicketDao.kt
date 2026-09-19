package com.example.carniceriaapp20.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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
    val unit: ProductUnit? = null
) {
    // ticket_items no guarda la unidad de venta: esta se obtiene por JOIN contra el catálogo
    // actual y viene null cuando el producto ya no existe (ej. tras reimportar el CSV). En ese
    // caso se infiere igual que en el resto de la app: cantidad fraccionaria = GRANEL (kg).
    val effectiveUnit: ProductUnit
        get() = unit ?: (if (totalQuantity % 1.0 != 0.0) ProductUnit.GRANEL else ProductUnit.UNIDAD)
}

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
            FROM ticket_items ti
            INNER JOIN tickets t ON ti.ticket_id = t.id
            WHERE t.timestamp >= :since
            GROUP BY product_code
        ) v ON p.code = v.product_code
        ORDER BY v.totalQty DESC
        LIMIT 10
    """)
    fun getTopSellingProducts(since: Long): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM tickets WHERE timestamp >= :startOfDay")
    suspend fun countTicketsOfDay(startOfDay: Long): Int

    @Query("""
        SELECT 
            ti.product_name as productName, 
            ti.product_code as productCode, 
            SUM(ti.total_price) as totalAmount, 
            SUM(ti.quantity) as totalQuantity,
            ti.product_department as department,
            p.unit as unit
        FROM ticket_items ti
        INNER JOIN tickets t ON ti.ticket_id = t.id
        LEFT JOIN products p ON ti.product_code = p.code
        WHERE t.timestamp >= :startTime AND t.timestamp <= :endTime
        GROUP BY ti.product_name, ti.product_code, ti.product_department, p.unit
        ORDER BY totalAmount DESC
    """)
    suspend fun getSalesReportByProduct(startTime: Long, endTime: Long): List<ProductSalesReport>
}
