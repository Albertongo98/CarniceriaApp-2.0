package com.example.carniceriaapp20.ui.screens.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.repository.ProductRepository
import com.example.carniceriaapp20.util.ReportExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    /**
     * Obtiene el contenido CSV del catálogo actual para guardado local (SAF).
     */
    suspend fun getProductsCsvContent(): String {
        val products = productRepository.getAllProducts().first()
        return ReportExporter.productsToCsv(products)
    }

    fun importProductsFromCsv(csvContent: String) {
        viewModelScope.launch {
            _uiState.value = UpdateUiState(result = UpdateResult.InProgress)
            try {
                val products = mutableListOf<Product>()
                val lines = csvContent.lines()
                if (lines.isEmpty()) return@launch

                val header = lines[0].lowercase()
                
                // Detectar formato por encabezados
                val isExportFormat = header.startsWith("codigo,nombre,precio")
                val isExternalFormat = header.contains("producto") && header.contains("departamento")

                lines.drop(1).forEach { line ->
                    if (line.isNotBlank()) {
                        val tokens = line.split(',')
                        
                        try {
                            when {
                                isExportFormat && tokens.size >= 5 -> {
                                    products.add(Product(
                                        code = tokens[0].trim(),
                                        name = tokens[1].trim(),
                                        price = tokens[2].trim().toDouble(),
                                        department = tokens[3].trim(),
                                        unit = ProductUnit.valueOf(tokens[4].trim().uppercase())
                                    ))
                                }
                                isExternalFormat && tokens.size >= 11 -> {
                                    products.add(Product(
                                        code = tokens[1].trim(),
                                        name = tokens[2].trim(),
                                        price = tokens[4].replace("$", "").replace(",", "").trim().toDouble(),
                                        department = tokens[6].trim(),
                                        unit = if (tokens[10].trim().equals("GRANEL", ignoreCase = true)) ProductUnit.GRANEL else ProductUnit.UNIDAD
                                    ))
                                }
                            }
                        } catch (e: Exception) { }
                    }
                }

                if (products.isEmpty()) {
                    _uiState.value = UpdateUiState(result = UpdateResult.Error("No se encontraron productos válidos."))
                    return@launch
                }

                productRepository.deleteAllProducts()
                productRepository.insertProducts(products)
                _uiState.value = UpdateUiState(result = UpdateResult.Success(products.size))

            } catch (e: Exception) {
                _uiState.value = UpdateUiState(result = UpdateResult.Error("Error: ${e.message}"))
            }
        }
    }

    fun resetState() {
        _uiState.value = UpdateUiState(UpdateResult.Idle)
    }
}
