package com.example.carniceriaapp20.ui.screens.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.isLowStock
import com.example.carniceriaapp20.data.repository.ProductRepository
import com.example.carniceriaapp20.util.BluetoothPrinterHelper
import com.example.carniceriaapp20.util.PrintResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val printerHelper: BluetoothPrinterHelper
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _lowStockOnly = MutableStateFlow(false)
    val lowStockOnly: StateFlow<Boolean> = _lowStockOnly.asStateFlow()

    private val allProducts = productRepository.getAllProducts()

    // Cuántos productos están en o por debajo de su inventario mínimo (solo los que controlan existencia).
    val lowStockCount: StateFlow<Int> = allProducts
        .map { list -> list.count { it.isLowStock } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Filtramos y ORDENAMOS alfabéticamente
    val products: StateFlow<List<Product>> = combine(allProducts, _searchQuery, _lowStockOnly) { all, query, lowOnly ->
        all.asSequence()
            .filter { !lowOnly || it.isLowStock }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true) }
            .sortedBy { it.name.lowercase() }
            .toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun toggleLowStockOnly() {
        _lowStockOnly.value = !_lowStockOnly.value
    }

    // Imprime TODOS los productos en o bajo su mínimo (no solo los que deje visibles el buscador).
    fun printRestockList() {
        viewModelScope.launch {
            val low = allProducts.first().filter { it.isLowStock }
            if (low.isEmpty()) {
                _message.value = "No hay productos con bajo inventario"
                return@launch
            }
            _message.value = when (val result = printerHelper.printRestockList(low, System.currentTimeMillis())) {
                PrintResult.Success -> "Lista de resurtido enviada a la impresora"
                is PrintResult.Error -> "Error de impresión: ${result.message}"
            }
        }
    }

    fun onMessageConsumed() {
        _message.value = null
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
        }
    }
}
