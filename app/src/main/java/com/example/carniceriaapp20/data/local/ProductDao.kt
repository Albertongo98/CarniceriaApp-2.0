package com.example.carniceriaapp20.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY department, name")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE code = :code")
    suspend fun getProductByCode(code: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    // Upsert (INSERT + UPDATE) y NO REPLACE: REPLACE borra la fila vieja y, con las claves foráneas
    // activas, deja en NULL el product_code de todas las ventas históricas de ese producto.
    @Upsert
    suspend fun upsertProducts(products: List<Product>)

    @Query("SELECT code FROM products")
    suspend fun getAllCodes(): List<String>

    @Query("DELETE FROM products WHERE code IN (:codes)")
    suspend fun deleteByCodes(codes: List<String>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)
}
