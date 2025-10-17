package com.example.carniceriaapp20.ui.screens.update

import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class UpdateFromCsvViewModelTest {

    private lateinit var viewModel: UpdateFromCsvViewModel
    private val mockRepository: ProductRepository = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = UpdateFromCsvViewModel(mockRepository)
    }

    @Test
    fun `importProductsFromCsv success scenario`() = runTest {
        val csvContent = "CODIGO;DESCRIPCION;PRECIO;DEPARTAMENTO;UNIDAD\n" +
                "1;Pollo;50,5;Polleria;UNIDAD\n" +
                "2;Res;150,0;Carniceria;GRANEL"

        viewModel.importProductsFromCsv(csvContent)

        // Verify interactions
        verify(mockRepository).deleteAllProducts()
        verify(mockRepository, times(2)).insertProduct(any())

        // Verify final state
        val result = viewModel.uiState.value.result
        assertTrue(result is UpdateResult.Success)
        assertEquals(2, (result as UpdateResult.Success).count)
    }

    @Test
    fun `importProductsFromCsv handles parsing errors gracefully`() = runTest {
        val csvContent = "CODIGO;DESCRIPCION;PRECIO;DEPARTAMENTO;UNIDAD\n" +
                "1;Pollo;PRECIO_INVALIDO;Polleria;UNIDAD\n" + // Invalid price
                "2;Res;150,0;Carniceria;GRANEL"

        viewModel.importProductsFromCsv(csvContent)

        // It should still insert the valid product
        verify(mockRepository).deleteAllProducts()
        verify(mockRepository, times(2)).insertProduct(any()) // It will insert the first with price 0.0 and the second one correctly

        val result = viewModel.uiState.value.result
        assertTrue(result is UpdateResult.Success)
        assertEquals(2, (result as UpdateResult.Success).count)
    }

    @Test
    fun `importProductsFromCsv handles empty or malformed lines`() = runTest {
        val csvContent = "CODIGO;DESCRIPCION;PRECIO;DEPARTAMENTO;UNIDAD\n" +
                "1;Pollo;50,5;Polleria;UNIDAD\n" +
                "\n" + // Empty line
                "2;Res;150,0;Carniceria" // Malformed line (missing a field)

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository).deleteAllProducts()
        verify(mockRepository, times(1)).insertProduct(any()) // Only the first product is valid

        val result = viewModel.uiState.value.result
        assertTrue(result is UpdateResult.Success)
        assertEquals(1, (result as UpdateResult.Success).count)
    }

    @Test
    fun `importProductsFromCsv shows error if all products are invalid`() = runTest {
        val csvContent = "CODIGO;DESCRIPCION;PRECIO;DEPARTAMENTO;UNIDAD\n" +
                         "invalid-line-1\n" +
                         "invalid-line-2"

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository, never()).deleteAllProducts()
        verify(mockRepository, never()).insertProduct(any())

        val result = viewModel.uiState.value.result
        assertTrue(result is UpdateResult.Error)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
