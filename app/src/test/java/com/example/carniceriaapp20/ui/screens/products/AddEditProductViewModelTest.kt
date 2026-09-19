package com.example.carniceriaapp20.ui.screens.products

import androidx.lifecycle.SavedStateHandle
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.kotlin.never

@ExperimentalCoroutinesApi
class AddEditProductViewModelTest {

    private lateinit var viewModel: AddEditProductViewModel
    private val mockRepository: ProductRepository = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // El ViewModel deriva "departments" de getAllProducts() en su inicializador.
        whenever(mockRepository.getAllProducts()).thenReturn(flowOf(emptyList()))
    }

    @Test
    fun `init loads product when productCode is provided`() = runTest {
        val product = Product("1", "Test", 10.0, "Dept", ProductUnit.UNIDAD)
        whenever(mockRepository.getProductByCode("1")).thenReturn(product)
        val savedStateHandle = SavedStateHandle(mapOf("productCode" to "1"))

        viewModel = AddEditProductViewModel(mockRepository, savedStateHandle)

        val expectedState = AddEditProductUiState(
            code = "1",
            name = "Test",
            price = "10.0",
            department = "Dept",
            unit = ProductUnit.UNIDAD,
            isEditing = true
        )
        assertEquals(expectedState, viewModel.uiState.value)
    }

    @Test
    fun `saveProduct calls insert when not editing`() = runTest {
        val savedStateHandle = SavedStateHandle() // No productCode
        viewModel = AddEditProductViewModel(mockRepository, savedStateHandle)

        viewModel.onCodeChange("2")
        viewModel.onNameChange("New Product")
        viewModel.onPriceChange("20.0")
        viewModel.onDepartmentChange("New Dept")

        viewModel.saveProduct()

        val expectedProduct = Product("2", "New Product", 20.0, "New Dept", ProductUnit.UNIDAD)
        verify(mockRepository).insertProduct(expectedProduct)
        verify(mockRepository, never()).updateProduct(any())
    }

    @Test
    fun `saveProduct calls update when editing`() = runTest {
        val product = Product("1", "Test", 10.0, "Dept", ProductUnit.UNIDAD)
        whenever(mockRepository.getProductByCode("1")).thenReturn(product)
        val savedStateHandle = SavedStateHandle(mapOf("productCode" to "1"))

        viewModel = AddEditProductViewModel(mockRepository, savedStateHandle)
        viewModel.onNameChange("Updated Name")

        viewModel.saveProduct()

        val updatedProduct = product.copy(name = "Updated Name")
        verify(mockRepository).updateProduct(updatedProduct)
        verify(mockRepository, never()).insertProduct(any())
    }

    @Test
    fun `saveProduct does nothing if validation fails`() = runTest {
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditProductViewModel(mockRepository, savedStateHandle)

        // Missing name
        viewModel.onCodeChange("3")
        viewModel.onPriceChange("30.0")
        viewModel.saveProduct()

        verify(mockRepository, never()).insertProduct(any())
        verify(mockRepository, never()).updateProduct(any())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
