package com.example.carniceriaapp20.ui.screens.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.repository.ProductRepository
import com.example.carniceriaapp20.util.formatPlain
import com.example.carniceriaapp20.util.normalizeDepartment
import com.example.carniceriaapp20.util.parseDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productCode: String? = savedStateHandle.get("productCode")

    private val _uiState = MutableStateFlow(AddEditProductUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val departments: StateFlow<List<String>> = productRepository.getAllProducts()
        .map { products -> products.map { it.department }.distinct().sortedBy { it.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (productCode != null) {
            viewModelScope.launch {
                val product = productRepository.getProductByCode(productCode)
                if (product != null) {
                    _uiState.value = AddEditProductUiState(
                        code = product.code,
                        name = product.name,
                        price = formatPlain(product.price),
                        department = product.department,
                        unit = product.unit,
                        stock = product.stock?.let(::formatPlain) ?: "",
                        minStock = if (product.minStock > 0) formatPlain(product.minStock) else "",
                        isEditing = true
                    )
                }
            }
        }
    }

    fun onCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(code = code)
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onPriceChange(price: String) {
        _uiState.value = _uiState.value.copy(price = price)
    }

    fun onDepartmentChange(department: String) {
        _uiState.value = _uiState.value.copy(department = department)
    }

    fun onUnitChange(unit: ProductUnit) {
        _uiState.value = _uiState.value.copy(unit = unit)
    }

    fun onStockChange(stock: String) {
        _uiState.value = _uiState.value.copy(stock = stock)
    }

    fun onMinStockChange(minStock: String) {
        _uiState.value = _uiState.value.copy(minStock = minStock)
    }

    fun saveProduct() {
        viewModelScope.launch {
            val state = _uiState.value
            val price = parseDecimal(state.price)
            val stock = if (state.stock.isBlank()) null else parseDecimal(state.stock)
            val minStock = if (state.minStock.isBlank()) 0.0 else parseDecimal(state.minStock)

            if (state.code.isBlank() || state.name.isBlank() || state.department.isBlank() || price == null || price <= 0) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error: Revisa los campos. Código, nombre y departamento son obligatorios y el precio debe ser mayor a 0."))
                return@launch
            }
            if ((state.stock.isNotBlank() && stock == null) || minStock == null || minStock < 0) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error: La existencia y el inventario mínimo deben ser números (el mínimo no puede ser negativo)."))
                return@launch
            }

            if (!state.isEditing && productRepository.getProductByCode(state.code) != null) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error: El código de producto ya existe."))
                return@launch
            }

            // Reutiliza la escritura de un departamento ya existente ("carniceria" -> "Carnicería").
            val knownDepartments = productRepository.getAllProducts().first().map { it.department }.distinct()

            val product = Product(
                code = state.code,
                name = state.name.trim(),
                price = price,
                department = normalizeDepartment(state.department, knownDepartments),
                unit = state.unit,
                stock = stock,
                minStock = minStock
            )

            if (state.isEditing) {
                productRepository.updateProduct(product)
            } else {
                productRepository.insertProduct(product)
            }
            _eventFlow.emit(UiEvent.SaveSuccess)
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object SaveSuccess : UiEvent()
    }
}

data class AddEditProductUiState(
    val code: String = "",
    val name: String = "",
    val price: String = "",
    val department: String = "",
    val unit: ProductUnit = ProductUnit.UNIDAD,
    val stock: String = "",
    val minStock: String = "",
    val isEditing: Boolean = false
)
