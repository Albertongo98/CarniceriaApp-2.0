package com.example.carniceriaapp20.data.repository

import androidx.room.withTransaction
import com.example.carniceriaapp20.data.local.CarniceriaDatabase
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductDao
import com.example.carniceriaapp20.data.local.TicketDao
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val database: CarniceriaDatabase,
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

    override suspend fun replaceAllProducts(products: List<Product>) {
        database.withTransaction {
            // No se usa "borrar todo + insertar": con las claves foráneas activas, borrar un producto
            // deja en NULL el product_code de sus ventas históricas. Solo se borran los que ya no vienen.
            val newCodes = products.mapTo(HashSet()) { it.code }
            productDao.getAllCodes()
                .filter { it !in newCodes }
                .chunked(500) // límite de variables de SQLite (999 en Android antiguos)
                .forEach { productDao.deleteByCodes(it) }
            productDao.upsertProducts(products)
        }
    }

    override suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    override suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    override fun getTopSellingProducts(): Flow<List<Product>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return ticketDao.getTopSellingProducts(calendar.timeInMillis)
    }
}
