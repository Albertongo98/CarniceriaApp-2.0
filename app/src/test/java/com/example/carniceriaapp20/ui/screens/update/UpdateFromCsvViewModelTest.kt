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

    // Formato "export": el que genera ReportExporter.productsToCsv (codigo,nombre,precio,departamento,unidad)
    @Test
    fun `importProductsFromCsv success scenario with export format`() = runTest {
        val csvContent = "codigo,nombre,precio,departamento,unidad\n" +
                "1,Pollo,50.5,Polleria,UNIDAD\n" +
                "2,Res,150.0,Carniceria,GRANEL"

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository).deleteAllProducts()
        verify(mockRepository).insertProducts(
            listOf(
                Product("1", "Pollo", 50.5, "Polleria", ProductUnit.UNIDAD),
                Product("2", "Res", 150.0, "Carniceria", ProductUnit.GRANEL)
            )
        )

        val result = viewModel.uiState.value.result
        assertTrue(result is UpdateResult.Success)
        assertEquals(2, (result as UpdateResult.Success).count)
    }

    // Formato "external": catálogo de terceros (columna 1=codigo, 2=nombre, 4=precio, 6=departamento, 10=unidad)
    @Test
    fun `importProductsFromCsv success scenario with external format`() = runTest {
        val csvContent = "id,codigo,producto,extra,precio,extra2,departamento,extra3,extra4,extra5,unidad\n" +
                "0,55,Bistec,,\$123.45,,Carniceria,,,,GRANEL"

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository).deleteAllProducts()
        verify(mockRepository).insertProducts(listOf(Product("55", "Bistec", 123.45, "Carniceria", ProductUnit.GRANEL)))

        val result = viewModel.uiState.value.result
        assertTrue(result is UpdateResult.Success)
        assertEquals(1, (result as UpdateResult.Success).count)
    }

    @Test
    fun `importProductsFromCsv skips lines with unparseable price`() = runTest {
        val csvContent = "codigo,nombre,precio,departamento,unidad\n" +
                "1,Pollo,PRECIO_INVALIDO,Polleria,UNIDAD\n" +
                "2,Res,150.0,Carniceria,GRANEL"

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository).deleteAllProducts()
        verify(mockRepository).insertProducts(any()) // Solo la línea 2 es válida

        val result = viewModel.uiState.value.result
        assertTrue(result is UpdateResult.Success)
        assertEquals(1, (result as UpdateResult.Success).count)
    }

    @Test
    fun `importProductsFromCsv skips empty or malformed lines`() = runTest {
        val csvContent = "codigo,nombre,precio,departamento,unidad\n" +
                "1,Pollo,50.5,Polleria,UNIDAD\n" +
                "\n" + // Línea vacía
                "2,Res,150.0,Carniceria" // Línea incompleta (le falta la unidad)

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository).deleteAllProducts()
        verify(mockRepository).insertProducts(any()) // Solo la primera línea es válida

        val result = viewModel.uiState.value.result
        assertTrue(result is UpdateResult.Success)
        assertEquals(1, (result as UpdateResult.Success).count)
    }

    @Test
    fun `importProductsFromCsv shows error if all products are invalid`() = runTest {
        val csvContent = "codigo,nombre,precio,departamento,unidad\n" +
                "invalid-line-1\n" +
                "invalid-line-2"

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository, never()).deleteAllProducts()
        verify(mockRepository, never()).insertProducts(any())

        val result = viewModel.uiState.value.result
        assertTrue(result is UpdateResult.Error)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
