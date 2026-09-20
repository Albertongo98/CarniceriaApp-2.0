package com.example.carniceriaapp20.data.repository

import com.example.carniceriaapp20.data.local.ProductSalesReport
import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.local.TicketItem
import com.example.carniceriaapp20.data.local.TicketWithItems
import kotlinx.coroutines.flow.Flow

interface TicketRepository {

    // Guarda ticket + renglones y descuenta existencias, todo en una transacción.
    suspend fun saveTicket(ticket: Ticket, items: List<TicketItem>): Long

    // Marca el ticket como anulado (no lo borra) y regresa lo vendido a existencias.
    // false si no existe o ya estaba anulado.
    suspend fun voidTicket(ticketId: Long, reason: String): Boolean

    fun getAllTicketsWithItems(): Flow<List<TicketWithItems>>

    suspend fun getLastTicket(): Ticket?

    fun getTicketWithItems(ticketId: Long): Flow<TicketWithItems?>

    suspend fun countTicketsOfDay(startOfDay: Long): Int

    suspend fun getProductSalesReport(startTime: Long, endTime: Long): List<ProductSalesReport>

    suspend fun getTicketsBetween(startTime: Long, endTime: Long): List<Ticket>
}
