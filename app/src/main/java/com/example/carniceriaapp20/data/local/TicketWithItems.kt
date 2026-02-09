package com.example.carniceriaapp20.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class TicketWithItems(
    @Embedded val ticket: Ticket,
    @Relation(
        parentColumn = "id",
        entityColumn = "ticket_id"
    )
    val items: List<TicketItem>
)
