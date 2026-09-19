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
    data class Success(val count: Int, val skipped: Int = 0) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

data class UpdateUiState(val result: UpdateResult = UpdateResult.Idle)

// Parte una línea CSV respetando comillas (RFC 4180): `"Bistec, fino",10` -> ["Bistec, fino", "10"].
// Dentro de comillas, "" es una comilla literal. No soporta campos con saltos de línea.
internal fun splitCsv(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                current.append('"')
                i++
            }
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                fields.add(current.toString())
                current.clear()
            }
            else -> current.append(c)
        }
        i++
    }
    fields.add(current.toString())
    return fields
}

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
                // Excel guarda "CSV UTF-8" con BOM al inicio; sin quitarlo el encabezado no se reconoce.
                val lines = csvContent.removePrefix("﻿").lines()
                val header = lines.first().lowercase()

                // Detectar formato por encabezados
                val isExportFormat = header.startsWith("codigo,nombre,precio")
                val isExternalFormat = header.contains("producto") && header.contains("departamento")

                val products = LinkedHashMap<String, Product>()
                var skipped = 0
                lines.drop(1).filter { it.isNotBlank() }.forEach { line ->
                    val product = parseProduct(splitCsv(line), isExportFormat, isExternalFormat)
                    // Línea inválida, o código repetido (gana la última): se cuentan como omitidas.
                    if (product == null || products.put(product.code, product) != null) skipped++
                }

                if (products.isEmpty()) {
                    _uiState.value = UpdateUiState(result = UpdateResult.Error("No se encontraron productos válidos."))
                    return@launch
                }

                productRepository.replaceAllProducts(products.values.toList())
                _uiState.value = UpdateUiState(result = UpdateResult.Success(products.size, skipped))

            } catch (e: Exception) {
                _uiState.value = UpdateUiState(result = UpdateResult.Error("Error: ${e.message}. El catálogo no se modificó."))
            }
        }
    }

    // null = línea que no corresponde al formato detectado o con datos inválidos (precio, unidad, código vacío).
    private fun parseProduct(tokens: List<String>, isExportFormat: Boolean, isExternalFormat: Boolean): Product? {
        return try {
            when {
                isExportFormat && tokens.size >= 5 -> Product(
                    code = tokens[0].trim(),
                    name = tokens[1].trim(),
                    price = tokens[2].trim().toDouble(),
                    department = tokens[3].trim(),
                    unit = ProductUnit.valueOf(tokens[4].trim().uppercase())
                )
                isExternalFormat && tokens.size >= 11 -> Product(
                    code = tokens[1].trim(),
                    name = tokens[2].trim(),
                    price = tokens[4].replace("$", "").replace(",", "").trim().toDouble(),
                    department = tokens[6].trim(),
                    unit = if (tokens[10].trim().equals("GRANEL", ignoreCase = true)) ProductUnit.GRANEL else ProductUnit.UNIDAD
                )
                else -> null
            }?.takeIf { it.code.isNotBlank() }
        } catch (e: IllegalArgumentException) { // NumberFormatException (precio) o unidad desconocida
            null
        }
    }

    fun resetState() {
        _uiState.value = UpdateUiState(UpdateResult.Idle)
    }
}
