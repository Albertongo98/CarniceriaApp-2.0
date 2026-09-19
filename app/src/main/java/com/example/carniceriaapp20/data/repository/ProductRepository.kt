package com.example.carniceriaapp20.data.repository

import com.example.carniceriaapp20.data.local.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {

    fun getAllProducts(): Flow<List<Product>>

    suspend fun getProductByCode(code: String): Product?

    suspend fun insertProduct(product: Product)

    // Borra todo el catálogo e inserta el nuevo en UNA transacción: si algo falla, no cambia nada.
    suspend fun replaceAllProducts(products: List<Product>)

    suspend fun updateProduct(product: Product)

    suspend fun deleteProduct(product: Product)

    fun getTopSellingProducts(): Flow<List<Product>>
}
