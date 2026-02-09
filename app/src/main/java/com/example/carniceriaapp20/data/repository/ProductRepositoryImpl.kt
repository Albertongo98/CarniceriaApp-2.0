package com.example.carniceriaapp20.data.repository

import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductDao
import com.example.carniceriaapp20.data.local.TicketDao
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val ticketDao: TicketDao
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

    override fun getTopSellingProducts(): Flow<List<Product>> {
        // Calculamos el inicio del día actual (00:00:00) para la moda dinámica
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return ticketDao.getTopSellingProducts(calendar.timeInMillis)
    }
}
