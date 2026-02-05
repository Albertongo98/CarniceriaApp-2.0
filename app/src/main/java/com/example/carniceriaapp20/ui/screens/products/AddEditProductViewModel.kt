package com.example.carniceriaapp20.ui.screens.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init {
        if (productCode != null) {
            viewModelScope.launch {
                val product = productRepository.getProductByCode(productCode)
                if (product != null) {
                    _uiState.value = AddEditProductUiState(
                        code = product.code,
                        name = product.name,
                        price = product.price.toString(),
                        department = product.department,
                        unit = product.unit,
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

    fun saveProduct() {
        viewModelScope.launch {
            val state = _uiState.value
            val price = state.price.toDoubleOrNull()

            if (state.code.isBlank() || state.name.isBlank() || price == null || price <= 0) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error: Revisa los campos. El código y el nombre no pueden estar vacíos y el precio debe ser mayor a 0."))
                return@launch
            }
            
            if (!state.isEditing && productRepository.getProductByCode(state.code) != null) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error: El código de producto ya existe."))
                return@launch
            }

            val product = Product(
                code = state.code,
                name = state.name,
                price = price,
                department = state.department,
                unit = state.unit
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
    val isEditing: Boolean = false
)
