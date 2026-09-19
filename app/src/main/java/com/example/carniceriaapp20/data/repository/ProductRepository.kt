package com.example.carniceriaapp20.data.repository

import com.example.carniceriaapp20.data.local.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {

    fun getAllProducts(): Flow<List<Product>>

    suspend fun getProductByCode(code: String): Product?

    suspend fun insertProduct(product: Product)
    
    suspend fun insertProducts(products: List<Product>)

    suspend fun updateProduct(product: Product)

    suspend fun deleteProduct(product: Product)

    suspend fun deleteAllProducts()

    fun getTopSellingProducts(): Flow<List<Product>>
}
