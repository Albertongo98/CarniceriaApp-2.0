package com.example.carniceriaapp20.ui.screens.generador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.local.LabelHistory
import com.example.carniceriaapp20.data.model.EtiquetaProducto
import com.example.carniceriaapp20.data.repository.LabelHistoryRepository
import com.example.carniceriaapp20.util.BluetoothPrinterHelper
import com.example.carniceriaapp20.util.PrintResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

data class LabelGeneratorUiState(
    val history: List<LabelHistory> = emptyList(),
    val formName: String = "",
    val formPrice: String = "",
    val formCode: String = "",
    val printQuantity: Int = 1,
    val isFormVisible: Boolean = false,
    val printResult: PrintResult? = null
)

@HiltViewModel
class LabelGeneratorViewModel @Inject constructor(
    private val labelHistoryRepository: LabelHistoryRepository,
    private val printerHelper: BluetoothPrinterHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(LabelGeneratorUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            labelHistoryRepository.getAllLabels().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(formName = name) }
    }

    fun onCodeChange(code: String) {
        _uiState.update { it.copy(formCode = code) }
    }

    fun onPrintQuantityChange(qty: Int) {
        _uiState.update { it.copy(printQuantity = qty.coerceAtLeast(1)) }
    }

    fun toggleFormVisibility() {
        _uiState.update { it.copy(isFormVisible = !it.isFormVisible) }
    }

    fun onPrintResultConsumed() {
        _uiState.update { it.copy(printResult = null) }
    }

    fun onPriceChange(price: String) {
        val filtered = price.filter { it.isDigit() || it == '.' || it == ',' }
        val parts = filtered.replace(",", ".").split(".")
        if (parts.size <= 2 && (parts.getOrNull(1)?.length ?: 0) <= 2) {
            _uiState.update { it.copy(formPrice = filtered) }
        }
    }

    fun printNewLabel() {
        val currentState = _uiState.value
        val name = currentState.formName.trim()
        val price = currentState.formPrice.trim().replace(',', '.').toDoubleOrNull()
        val code = currentState.formCode.trim()

        if (name.isBlank() || price == null || price <= 0 || code.isBlank()) {
            _uiState.update { it.copy(printResult = PrintResult.Error("Todos los campos son obligatorios.")) }
            return
        }

        viewModelScope.launch {
            val formattedPrice = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).format(price)
            val etiqueta = EtiquetaProducto(name, formattedPrice, code)
            val labelToSave = LabelHistory(name = name, price = formattedPrice, code = code)

            val result = withContext(Dispatchers.IO) {
                printerHelper.printEtiqueta(etiqueta, currentState.printQuantity)
            }
            _uiState.update { it.copy(printResult = result) }

            if (result is PrintResult.Success) {
                labelHistoryRepository.insertLabel(labelToSave)
                _uiState.update { 
                    it.copy(
                        formName = "",
                        formPrice = "",
                        formCode = "",
                        printQuantity = 1,
                        isFormVisible = false
                    )
                }
            }
        }
    }

    fun reprintLabel(label: LabelHistory, quantity: Int = 1) {
        viewModelScope.launch {
            val etiqueta = EtiquetaProducto(label.name, label.price, label.code)
            val result = withContext(Dispatchers.IO) {
                printerHelper.printEtiqueta(etiqueta, quantity)
            }
            _uiState.update { it.copy(printResult = result) }
        }
    }

    fun deleteLabelFromHistory(label: LabelHistory) {
        viewModelScope.launch {
            labelHistoryRepository.deleteLabel(label)
        }
    }
}
