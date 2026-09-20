package com.example.carniceriaapp20.ui.screens.update

import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.repository.ProductRepository
import com.example.carniceriaapp20.util.ReportExporter
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
import org.mockito.kotlin.whenever

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

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun successResult(): UpdateResult.Success {
        val result = viewModel.uiState.value.result
        assertTrue("resultado: $result", result is UpdateResult.Success)
        return result as UpdateResult.Success
    }

    // Formato "export": el que genera ReportExporter.productsToCsv (codigo,nombre,precio,departamento,unidad)
    @Test
    fun `importa formato export y reemplaza el catalogo en una sola operacion`() = runTest {
        val csvContent = "codigo,nombre,precio,departamento,unidad\n" +
                "1,Pollo,50.5,Polleria,UNIDAD\n" +
                "2,Res,150.0,Carniceria,GRANEL"

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository).replaceAllProducts(
            listOf(
                Product("1", "Pollo", 50.5, "Polleria", ProductUnit.UNIDAD),
                Product("2", "Res", 150.0, "Carniceria", ProductUnit.GRANEL)
            )
        )
        assertEquals(UpdateResult.Success(2, 0), viewModel.uiState.value.result)
    }

    // Formato "external": catálogo de terceros (columna 1=codigo, 2=nombre, 4=precio, 6=departamento, 10=unidad)
    @Test
    fun `importa formato external con precio con signo de pesos`() = runTest {
        val csvContent = "id,codigo,producto,extra,precio,extra2,departamento,extra3,extra4,extra5,unidad\n" +
                "0,55,Bistec,,\$123.45,,Carniceria,,,,GRANEL"

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository).replaceAllProducts(listOf(Product("55", "Bistec", 123.45, "Carniceria", ProductUnit.GRANEL)))
        assertEquals(1, successResult().count)
    }

    @Test
    fun `respeta comillas en nombre con coma y comillas escapadas`() = runTest {
        val csvContent = "codigo,nombre,precio,departamento,unidad\n" +
                "1,\"Bistec, corte fino\",180.0,Carniceria,GRANEL\n" +
                "2,\"Salsa \"\"Roja\"\"\",25.0,Abarrotes,UNIDAD"

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository).replaceAllProducts(
            listOf(
                Product("1", "Bistec, corte fino", 180.0, "Carniceria", ProductUnit.GRANEL),
                Product("2", "Salsa \"Roja\"", 25.0, "Abarrotes", ProductUnit.UNIDAD)
            )
        )
    }

    @Test
    fun `exportar e importar es un viaje de ida y vuelta sin perdida`() = runTest {
        val original = listOf(
            Product("1", "Bistec, corte fino", 180.0, "Carniceria", ProductUnit.GRANEL),
            Product("2", "Salsa \"Roja\"", 25.5, "Abarrotes", ProductUnit.UNIDAD)
        )

        viewModel.importProductsFromCsv(ReportExporter.productsToCsv(original))

        verify(mockRepository).replaceAllProducts(original)
    }

    @Test
    fun `ignora el BOM que agrega Excel al guardar CSV UTF-8`() = runTest {
        val csvContent = "﻿codigo,nombre,precio,departamento,unidad\n1,Pollo,50.5,Polleria,UNIDAD"

        viewModel.importProductsFromCsv(csvContent)

        assertEquals(1, successResult().count)
    }

    @Test
    fun `cuenta las lineas omitidas por precio invalido, incompletas y codigo repetido`() = runTest {
        val csvContent = "codigo,nombre,precio,departamento,unidad\n" +
                "1,Pollo,PRECIO_INVALIDO,Polleria,UNIDAD\n" + // omitida: precio
                "2,Res,150.0,Carniceria,GRANEL\n" +
                "\n" + // línea vacía: no cuenta
                "3,Cerdo,90.0,Carniceria\n" + // omitida: le falta la unidad
                "2,Res otra vez,160.0,Carniceria,GRANEL" // omitida: código repetido (gana esta última)

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository).replaceAllProducts(listOf(Product("2", "Res otra vez", 160.0, "Carniceria", ProductUnit.GRANEL)))
        assertEquals(UpdateResult.Success(1, 3), viewModel.uiState.value.result)
    }

    @Test
    fun `si todas las lineas son invalidas muestra error y no toca el catalogo`() = runTest {
        val csvContent = "codigo,nombre,precio,departamento,unidad\n" +
                "invalid-line-1\n" +
                "invalid-line-2"

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository, never()).replaceAllProducts(any())
        assertTrue(viewModel.uiState.value.result is UpdateResult.Error)
    }

    @Test
    fun `si falla el reemplazo muestra error indicando que el catalogo no cambio`() = runTest {
        whenever(mockRepository.replaceAllProducts(any())).thenThrow(IllegalStateException("disco lleno"))

        viewModel.importProductsFromCsv("codigo,nombre,precio,departamento,unidad\n1,Pollo,50.5,Polleria,UNIDAD")

        val result = viewModel.uiState.value.result
        assertTrue(result is UpdateResult.Error)
        assertTrue((result as UpdateResult.Error).message.contains("no se modific"))
    }

    @Test
    fun `importa existencia y minimo del formato external`() = runTest {
        val csvContent = "id,codigo,producto,extra,precio,extra2,departamento,existencia,minimo,extra5,unidad\n" +
                "0,55,Bistec,,\$123.45,,Carniceria,\"1,785.5\",20,,GRANEL"

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository).replaceAllProducts(
            listOf(Product("55", "Bistec", 123.45, "Carniceria", ProductUnit.GRANEL, stock = 1785.5, minStock = 20.0))
        )
    }

    @Test
    fun `unifica departamentos escritos distinto y pone Sin departamento a los vacios`() = runTest {
        val csvContent = "codigo,nombre,precio,departamento,unidad\n" +
                "1,Res,150.0,Carnicería,GRANEL\n" +
                "2,Cerdo,90.0, carniceria ,GRANEL\n" +
                "3,Sal,10.0,,UNIDAD"

        viewModel.importProductsFromCsv(csvContent)

        verify(mockRepository).replaceAllProducts(
            listOf(
                Product("1", "Res", 150.0, "Carnicería", ProductUnit.GRANEL),
                Product("2", "Cerdo", 90.0, "Carnicería", ProductUnit.GRANEL),
                Product("3", "Sal", 10.0, "Sin departamento", ProductUnit.UNIDAD)
            )
        )
    }

    @Test
    fun `la exportacion lleva existencia y minimo y se importa sin perdida`() = runTest {
        val original = listOf(
            Product("1", "Res", 150.0, "Carniceria", ProductUnit.GRANEL, stock = 12.5, minStock = 5.0),
            Product("2", "Sal", 10.0, "Abarrotes", ProductUnit.UNIDAD) // sin controlar existencia
        )

        viewModel.importProductsFromCsv(ReportExporter.productsToCsv(original))

        verify(mockRepository).replaceAllProducts(original)
    }
}
