package com.example.carniceriaapp20.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "ticket_items",
    foreignKeys = [
        ForeignKey(
            entity = Ticket::class,
            parentColumns = ["id"],
            childColumns = ["ticket_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["code"],
            childColumns = ["product_code"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TicketItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "ticket_id", index = true)
    val ticketId: Long,

    @ColumnInfo(name = "product_code", index = true)
    val productCode: String?,

    @ColumnInfo(name = "product_name")
    val productName: String,

    @ColumnInfo(name = "product_department") // NUEVO: Para reportes por departamento
    val productDepartment: String,

    @ColumnInfo(name = "quantity")
    val quantity: Double,

    @ColumnInfo(name = "unit_price")
    val unitPrice: Double,

    @ColumnInfo(name = "total_price")
    val totalPrice: Double,

    @ColumnInfo(name = "estimated_pieces")
    val estimatedPieces: Int? = null
)
