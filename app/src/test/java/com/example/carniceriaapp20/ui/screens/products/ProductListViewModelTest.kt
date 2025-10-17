package com.example.carniceriaapp20.ui.screens.products

import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ProductListViewModelTest {

    private lateinit var viewModel: ProductListViewModel
    private val mockRepository: ProductRepository = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `products state flow exposes data from repository`() = runTest {
        val testProducts = listOf(Product("1", "Test Product", 10.0, "Test", ProductUnit.UNIDAD))
        whenever(mockRepository.getAllProducts()).thenReturn(flowOf(testProducts))

        // When
        viewModel = ProductListViewModel(mockRepository)

        // Then
        // With UnconfinedTestDispatcher, the coroutine for stateIn is launched and executed immediately.
        // No need to advance the scheduler.
        assertEquals(testProducts, viewModel.products.value)
    }

    @Test
    fun `deleteProduct calls repository`() = runTest {
        whenever(mockRepository.getAllProducts()).thenReturn(flowOf(emptyList()))
        viewModel = ProductListViewModel(mockRepository)
        val productToDelete = Product("1", "Test", 1.0, "Dept", ProductUnit.UNIDAD)

        viewModel.deleteProduct(productToDelete)

        // With UnconfinedTestDispatcher, the launch block is also executed immediately.
        verify(mockRepository).deleteProduct(productToDelete)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
