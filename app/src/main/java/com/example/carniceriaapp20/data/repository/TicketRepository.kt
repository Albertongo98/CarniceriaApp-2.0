package com.example.carniceriaapp20.data.repository

import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.local.TicketItem
import com.example.carniceriaapp20.ui.screens.history.TicketWithItems
import kotlinx.coroutines.flow.Flow

interface TicketRepository {

    suspend fun saveTicket(ticket: Ticket, items: List<TicketItem>): Long

    fun getAllTicketsWithItems(): Flow<List<TicketWithItems>>

    suspend fun getLastTicket(): Ticket?

    fun getTicketWithItems(ticketId: Long): Flow<TicketWithItems?> // Added this line
}
