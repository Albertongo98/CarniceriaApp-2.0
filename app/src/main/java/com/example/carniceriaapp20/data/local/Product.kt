package com.example.carniceriaapp20.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "price")
    val price: Double,

    @ColumnInfo(name = "department")
    val department: String,

    @ColumnInfo(name = "unit")
    val unit: ProductUnit,

    // null = la existencia de este producto no se controla. En GRANEL son kg; en UNIDAD, piezas.
    @ColumnInfo(name = "stock")
    val stock: Double? = null,

    @ColumnInfo(name = "min_stock")
    val minStock: Double = 0.0
)

val Product.isLowStock: Boolean
    get() = stock != null && stock <= minStock
