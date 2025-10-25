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
import kotlinx.coroutines.delay
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
    val printResult: PrintResult? = null,
    val productToPrint: Any? = null, // Can be LabelHistory or a new one
    val showQuantityDialog: Boolean = false
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

    fun onPriceChange(price: String) {
        val filtered = price.filter { it.isDigit() || it == '.' || it == ',' }
        val parts = filtered.replace(",", ".").split(".")
        if (parts.size <= 2 && (parts.getOrNull(1)?.length ?: 0) <= 2) {
            _uiState.update { it.copy(formPrice = filtered) }
        }
    }

    fun onPrintResultConsumed() {
        _uiState.update { it.copy(printResult = null) }
    }

    fun showPrintNewDialog() {
        val currentState = _uiState.value
        val name = currentState.formName.trim()
        val price = currentState.formPrice.trim().replace(',', '.').toDoubleOrNull()
        val code = currentState.formCode.trim()

        if (name.isBlank() || price == null || price <= 0 || code.isBlank()) {
            _uiState.update { it.copy(printResult = PrintResult.Error("Todos los campos son obligatorios.")) }
            return
        }

        val formattedPrice = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).format(price)
        val etiqueta = EtiquetaProducto(name, formattedPrice, code)
        _uiState.update { it.copy(productToPrint = etiqueta, showQuantityDialog = true) }
    }

    fun showReprintDialog(label: LabelHistory) {
        _uiState.update { it.copy(productToPrint = label, showQuantityDialog = true) }
    }

    fun dismissQuantityDialog() {
        _uiState.update { it.copy(showQuantityDialog = false, productToPrint = null) }
    }

    fun printLabels(quantity: Int) {
        val productToPrint = _uiState.value.productToPrint ?: return
        val quantityToPrint = quantity.coerceAtLeast(1)

        val etiqueta = when (productToPrint) {
            is EtiquetaProducto -> productToPrint
            is LabelHistory -> EtiquetaProducto(productToPrint.name, productToPrint.price, productToPrint.code)
            else -> return
        }

        viewModelScope.launch {
            var finalResult: PrintResult = PrintResult.Success
            withContext(Dispatchers.IO) {
                for (i in 1..quantityToPrint) {
                    val result = printerHelper.printEtiqueta(etiqueta, 1)
                    if (result is PrintResult.Success) {
                        delay(400) // Shorter delay for labels
                        printerHelper.flushPrinter()
                    } else {
                        finalResult = result
                        break // Stop on first error
                    }
                }
            }
            _uiState.update { it.copy(printResult = finalResult) }

            // If printing was successful and it was a new label, save it.
            if (finalResult is PrintResult.Success && productToPrint is EtiquetaProducto) {
                val labelToSave = LabelHistory(name = etiqueta.nombre, price = etiqueta.precio, code = etiqueta.codigo)
                labelHistoryRepository.insertLabel(labelToSave)
                _uiState.update {
                    it.copy(
                        formName = "",
                        formPrice = "",
                        formCode = ""
                    )
                }
            }
        }
        dismissQuantityDialog()
    }

    fun deleteLabelFromHistory(label: LabelHistory) {
        viewModelScope.launch {
            labelHistoryRepository.deleteLabel(label)
        }
    }
}
