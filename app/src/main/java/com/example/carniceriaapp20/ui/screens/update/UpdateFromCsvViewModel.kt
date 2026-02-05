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
                if (lines.isEmpty()) return@launch

                val header = lines[0]
                // Detectar si el formato es el nuevo (coma) o el viejo (punto y coma)
                val isNewFormat = header.contains(",") && header.contains("Producto")

                lines.drop(1).forEach { line ->
                    if (line.isNotBlank()) {
                        val tokens = if (isNewFormat) splitCsv(line) else line.split(';')
                        
                        try {
                            if (isNewFormat && tokens.size >= 11) {
                                // Formato Nuevo: productos.csv
                                products.add(
                                    Product(
                                        code = tokens[1].trim(),
                                        name = tokens[2].trim(),
                                        price = parsePrice(tokens[4]),
                                        department = tokens[6].trim(),
                                        unit = if (tokens[10].trim().equals("GRANEL", ignoreCase = true)) ProductUnit.GRANEL else ProductUnit.UNIDAD
                                    )
                                )
                            } else if (!isNewFormat && tokens.size >= 5) {
                                // Formato Antiguo: 8477.csv
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
                        } catch (e: Exception) {
                            // Ignorar líneas con errores individuales para no detener toda la importación
                        }
                    }
                }

                if (products.isEmpty()) {
                    _uiState.value = UpdateUiState(result = UpdateResult.Error("No se encontraron productos válidos. Verifique el formato del archivo."))
                    return@launch
                }

                productRepository.deleteAllProducts()
                products.forEach { productRepository.insertProduct(it) }
                _uiState.value = UpdateUiState(result = UpdateResult.Success(products.size))

            } catch (e: Exception) {
                _uiState.value = UpdateUiState(result = UpdateResult.Error("Error: ${e.message}"))
            }
        }
    }

    // Función para limpiar el precio de símbolos como '$'
    private fun parsePrice(priceStr: String): Double {
        val clean = priceStr.replace("$", "").replace(",", "").trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    // Lógica básica para manejar comas dentro de textos si fuera necesario
    private fun splitCsv(line: String): List<String> {
        return line.split(',')
    }

    fun resetState() {
        _uiState.value = UpdateUiState(UpdateResult.Idle)
    }
}
