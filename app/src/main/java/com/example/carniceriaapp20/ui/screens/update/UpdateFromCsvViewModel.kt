package com.example.carniceriaapp20.ui.screens.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UpdateResult {
    object Idle : UpdateResult()
    object InProgress : UpdateResult()
    data class Success(val count: Int) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

data class UpdateUiState(val result: UpdateResult = UpdateResult.Idle)

@HiltViewModel
class UpdateFromCsvViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState = _uiState.asStateFlow()

    fun importProductsFromCsv(csvContent: String) {
        viewModelScope.launch {
            _uiState.value = UpdateUiState(result = UpdateResult.InProgress)
            try {
                val products = mutableListOf<Product>()
                val lines = csvContent.lines()

                lines.drop(1).forEach { line -> // Drop header row
                    if (line.isNotBlank()) {
                        val tokens = line.split(';')
                        if (tokens.size >= 5) {
                            products.add(
                                Product(
                                    code = tokens[0].trim(),
                                    name = tokens[1].trim(),
                                    price = tokens[2].replace(',', '.').toDoubleOrNull() ?: 0.0,
                                    department = tokens[3].trim(),
                                    unit = if (tokens[4].trim().equals("GRANEL", ignoreCase = true)) ProductUnit.GRANEL else ProductUnit.UNIDAD
                                )
                            )
                        }
                    }
                }

                if (products.isEmpty() && lines.size > 1) {
                    _uiState.value = UpdateUiState(result = UpdateResult.Error("No se pudieron encontrar productos válidos en el archivo."))
                    return@launch
                }

                productRepository.deleteAllProducts()
                products.forEach { productRepository.insertProduct(it) }
                _uiState.value = UpdateUiState(result = UpdateResult.Success(products.size))

            } catch (e: Exception) {
                _uiState.value = UpdateUiState(result = UpdateResult.Error(e.message ?: "Error desconocido durante la importación."))
            }
        }
    }

    fun resetState() {
        _uiState.value = UpdateUiState(UpdateResult.Idle)
    }
}
