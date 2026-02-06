package com.example.carniceriaapp20.ui.screens.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtramos y ORDENAMOS alfabéticamente
    val products: StateFlow<List<Product>> = productRepository.getAllProducts()
        .combine(_searchQuery) { allProducts, query ->
            val filtered = if (query.isBlank()) {
                allProducts
            } else {
                allProducts.filter { 
                    it.name.contains(query, ignoreCase = true) || 
                    it.code.contains(query, ignoreCase = true) 
                }
            }
            // Ordenamos por nombre
            filtered.sortedBy { it.name.lowercase() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
        }
    }
}
