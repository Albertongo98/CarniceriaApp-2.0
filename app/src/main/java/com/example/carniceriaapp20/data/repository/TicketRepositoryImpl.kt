package com.example.carniceriaapp20.data.repository

import androidx.room.withTransaction
import com.example.carniceriaapp20.data.local.CarniceriaDatabase
import com.example.carniceriaapp20.data.local.ProductDao
import com.example.carniceriaapp20.data.local.ProductSalesReport
import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.local.TicketDao
import com.example.carniceriaapp20.data.local.TicketItem
import com.example.carniceriaapp20.data.local.TicketWithItems
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TicketRepositoryImpl @Inject constructor(
    private val database: CarniceriaDatabase,
    private val ticketDao: TicketDao,
    private val productDao: ProductDao
) : TicketRepository {

    // Ticket + renglones + descuento de existencias en una sola transacción: no puede quedar un
    // ticket sin renglones (o existencias a medias) si la app se cierra a mitad de la venta.
    override suspend fun saveTicket(ticket: Ticket, items: List<TicketItem>): Long =
        database.withTransaction {
            val ticketId = ticketDao.insertTicket(ticket)
            ticketDao.insertTicketItems(items.map { it.copy(ticketId = ticketId) })
            items.forEach { item -> item.productCode?.let { productDao.adjustStock(it, -item.quantity) } }
            ticketId
        }

    override suspend fun voidTicket(ticketId: Long, reason: String): Boolean =
        database.withTransaction {
            if (ticketDao.markVoided(ticketId, System.currentTimeMillis(), reason) == 0) {
                false
            } else {
                // Lo vendido regresa a existencias.
                ticketDao.getItemsOfTicket(ticketId).forEach { item ->
                    item.productCode?.let { productDao.adjustStock(it, item.quantity) }
                }
                true
            }
        }

    override fun getAllTicketsWithItems(): Flow<List<TicketWithItems>> {
        return ticketDao.getAllTicketsWithItems()
    }

    override suspend fun getLastTicket(): Ticket? {
        return ticketDao.getLastTicket()
    }

    override fun getTicketWithItems(ticketId: Long): Flow<TicketWithItems?> {
        return ticketDao.getTicketWithItems(ticketId)
    }

    override suspend fun countTicketsOfDay(startOfDay: Long): Int {
        return ticketDao.countTicketsOfDay(startOfDay)
    }

    override suspend fun getProductSalesReport(startTime: Long, endTime: Long): List<ProductSalesReport> {
        return ticketDao.getSalesReportByProduct(startTime, endTime)
    }

    override suspend fun getTicketsBetween(startTime: Long, endTime: Long): List<Ticket> {
        return ticketDao.getTicketsBetween(startTime, endTime)
    }
}
