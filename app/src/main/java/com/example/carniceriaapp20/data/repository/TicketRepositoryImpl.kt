package com.example.carniceriaapp20.data.repository

import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.local.TicketDao
import com.example.carniceriaapp20.data.local.TicketItem
import com.example.carniceriaapp20.ui.screens.history.TicketWithItems
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TicketRepositoryImpl @Inject constructor(
    private val ticketDao: TicketDao
) : TicketRepository {

    override suspend fun saveTicket(ticket: Ticket, items: List<TicketItem>): Long {
        val ticketId = ticketDao.insertTicket(ticket)
        val itemsWithTicketId = items.map { it.copy(ticketId = ticketId) }
        ticketDao.insertTicketItems(itemsWithTicketId)
        return ticketId
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

    override suspend fun deleteTicketsOlderThan(threshold: Long) {
        ticketDao.deleteTicketsOlderThan(threshold)
    }

    override suspend fun countTicketsOfDay(startOfDay: Long): Int {
        return ticketDao.countTicketsOfDay(startOfDay)
    }
}
