package com.example.carniceriaapp20.ui.screens.products

import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.repository.ProductRepository
import com.example.carniceriaapp20.util.BluetoothPrinterHelper
import com.example.carniceriaapp20.util.PrintResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ProductListViewModelTest {

    private lateinit var viewModel: ProductListViewModel
    private val mockRepository: ProductRepository = mock()
    private val printer: BluetoothPrinterHelper = mock()
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
        viewModel = ProductListViewModel(mockRepository, printer)

        // Then
        // products usa SharingStarted.WhileSubscribed: el StateFlow no arranca a coleccionar el
        // upstream hasta que alguien lo suscribe. Leer .value directo se queda con el valor inicial.
        assertEquals(testProducts, viewModel.products.first())
    }

    @Test
    fun `deleteProduct calls repository`() = runTest {
        whenever(mockRepository.getAllProducts()).thenReturn(flowOf(emptyList()))
        viewModel = ProductListViewModel(mockRepository, printer)
        val productToDelete = Product("1", "Test", 1.0, "Dept", ProductUnit.UNIDAD)

        viewModel.deleteProduct(productToDelete)

        // With UnconfinedTestDispatcher, the launch block is also executed immediately.
        verify(mockRepository).deleteProduct(productToDelete)
    }

    @Test
    fun `el filtro de bajo inventario muestra solo los productos en o bajo su minimo`() = runTest {
        val low = Product("1", "Bistec", 180.0, "Carniceria", ProductUnit.GRANEL, stock = 2.0, minStock = 5.0)
        val ok = Product("2", "Chorizo", 10.0, "Embutidos", ProductUnit.UNIDAD, stock = 50.0, minStock = 5.0)
        val untracked = Product("3", "Sal", 5.0, "Abarrotes", ProductUnit.UNIDAD) // sin controlar existencia
        whenever(mockRepository.getAllProducts()).thenReturn(flowOf(listOf(low, ok, untracked)))
        viewModel = ProductListViewModel(mockRepository, printer)

        assertEquals(1, viewModel.lowStockCount.first { it > 0 })

        viewModel.toggleLowStockOnly()

        assertEquals(listOf(low), viewModel.products.first { it.size == 1 })
    }

    @Test
    fun `la lista de resurtido imprime solo los productos bajo minimo aunque el buscador filtre`() = runTest {
        val low1 = Product("1", "Bistec", 180.0, "Carniceria", ProductUnit.GRANEL, stock = 2.0, minStock = 5.0)
        val low2 = Product("2", "Sal", 10.0, "Abarrotes", ProductUnit.UNIDAD, stock = 0.0, minStock = 5.0)
        val ok = Product("3", "Chorizo", 10.0, "Embutidos", ProductUnit.UNIDAD, stock = 50.0, minStock = 5.0)
        whenever(mockRepository.getAllProducts()).thenReturn(flowOf(listOf(low1, low2, ok)))
        whenever(printer.printRestockList(any(), any())).thenReturn(PrintResult.Success)
        viewModel = ProductListViewModel(mockRepository, printer)
        viewModel.onSearchQueryChange("chorizo") // el buscador no debe afectar lo que se imprime

        viewModel.printRestockList()

        verify(printer).printRestockList(argThat { toSet() == setOf(low1, low2) }, any())
        assertEquals("Lista de resurtido enviada a la impresora", viewModel.message.first { it != null })
    }

    @Test
    fun `sin productos bajo minimo no imprime y avisa`() = runTest {
        whenever(mockRepository.getAllProducts()).thenReturn(
            flowOf(listOf(Product("3", "Chorizo", 10.0, "Embutidos", ProductUnit.UNIDAD, stock = 50.0, minStock = 5.0)))
        )
        viewModel = ProductListViewModel(mockRepository, printer)

        viewModel.printRestockList()

        verify(printer, never()).printRestockList(any(), any())
        assertEquals("No hay productos con bajo inventario", viewModel.message.first { it != null })
    }

    @Test
    fun `si la impresora falla el mensaje lo indica`() = runTest {
        val low = Product("1", "Bistec", 180.0, "Carniceria", ProductUnit.GRANEL, stock = 2.0, minStock = 5.0)
        whenever(mockRepository.getAllProducts()).thenReturn(flowOf(listOf(low)))
        whenever(printer.printRestockList(any(), any())).thenReturn(PrintResult.Error("Impresora no configurada"))
        viewModel = ProductListViewModel(mockRepository, printer)

        viewModel.printRestockList()

        assertEquals("Error de impresión: Impresora no configurada", viewModel.message.first { it != null })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
