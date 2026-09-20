package com.example.carniceriaapp20.ui.screens.tpv

import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.preferences.UserPreferencesRepository
import com.example.carniceriaapp20.data.repository.ProductRepository
import com.example.carniceriaapp20.data.repository.TicketRepository
import com.example.carniceriaapp20.util.BluetoothPrinterHelper
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class TpvViewModelTest {

    private lateinit var viewModel: TpvViewModel
    private val productRepository: ProductRepository = mock()
    private val ticketRepository: TicketRepository = mock()
    private val printerHelper: BluetoothPrinterHelper = mock()
    private val userPreferencesRepository: UserPreferencesRepository = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val product1 = Product("1", "Pollo", 50.0, "Polleria", ProductUnit.UNIDAD)
    private val product2 = Product("2", "Res", 150.0, "Carniceria", ProductUnit.GRANEL)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Common setup: always return a flow of products
        whenever(productRepository.getAllProducts()).thenReturn(flowOf(listOf(product1, product2)))
        whenever(userPreferencesRepository.printerMacAddress).thenReturn(flowOf(null))
        whenever(userPreferencesRepository.lastBackupAt).thenReturn(flowOf(System.currentTimeMillis()))
        viewModel = TpvViewModel(productRepository, ticketRepository, printerHelper, userPreferencesRepository)
    }

    // uiState combina sus fuentes con flowOn(Dispatchers.Default), un dispatcher real que el
    // scheduler virtual de runTest no controla. Por eso no se puede leer `.value` justo después
    // de una acción: hay que esperar (suspender) hasta que la condición esperada se cumpla.
    @Test
    fun `addProductToCart adds new item to active ticket`() = runTest {
        viewModel.addProductToCart(product1)

        val activeTicket = viewModel.uiState.first { it.activeTicket.items.isNotEmpty() }.activeTicket
        assertEquals(1, activeTicket.items.size)
        assertEquals(product1.code, activeTicket.items.first().product.code)
    }

    @Test
    fun `addProductToCart increments quantity for existing UNIDAD item`() = runTest {
        viewModel.addProductToCart(product1) // Add once
        viewModel.addProductToCart(product1) // Add again

        val activeTicket = viewModel.uiState.first { it.activeTicket.items.firstOrNull()?.quantity == 2.0 }.activeTicket
        assertEquals(1, activeTicket.items.size) // Should still be 1 item
        assertEquals(2.0, activeTicket.items.first().quantity, 0.0)
    }

    @Test
    fun `addTicket creates a new ticket and makes it active`() = runTest {
        val initialTicketCount = viewModel.uiState.first().tickets.size
        viewModel.addTicket()

        val state = viewModel.uiState.first { it.tickets.size == initialTicketCount + 1 }
        assertEquals(initialTicketCount + 1, state.tickets.size)
        assertEquals(state.tickets.size - 1, state.activeTicketIndex)
    }

    @Test
    fun `setActiveTicket changes the active ticket`() = runTest {
        viewModel.addTicket() // We have 2 tickets now (index 0 and 1)
        viewModel.uiState.first { it.tickets.size == 2 } // esperar a que el nuevo ticket se refleje

        viewModel.setActiveTicket(0)

        assertEquals(0, viewModel.uiState.first { it.activeTicketIndex == 0 }.activeTicketIndex)
    }

    @Test
    fun `closeTicket removes a ticket and adjusts active index`() = runTest {
        viewModel.addTicket()
        viewModel.addTicket() // 3 tickets, active is 2
        viewModel.uiState.first { it.tickets.size == 3 }

        viewModel.closeTicket(1) // Close the middle ticket

        val state = viewModel.uiState.first { it.tickets.size == 2 }
        assertEquals(2, state.tickets.size)
        assertEquals(1, state.activeTicketIndex) // Active should now be the new index 1
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
