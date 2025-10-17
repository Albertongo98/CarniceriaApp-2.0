package com.example.carniceriaapp20.data.repository

import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao
) : ProductRepository {

    override fun getAllProducts(): Flow<List<Product>> {
        return productDao.getAllProducts()
    }

    override suspend fun getProductByCode(code: String): Product? {
        return productDao.getProductByCode(code)
    }

    override suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
    }

    override suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    override suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    override suspend fun deleteAllProducts() {
        productDao.deleteAllProducts()
    }
}
