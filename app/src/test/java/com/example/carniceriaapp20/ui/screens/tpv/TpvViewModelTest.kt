package com.example.carniceriaapp20.ui.screens.tpv

import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.repository.ProductRepository
import com.example.carniceriaapp20.data.repository.TicketRepository
import com.example.carniceriaapp20.util.BluetoothPrinterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    private val testDispatcher = UnconfinedTestDispatcher()

    private val product1 = Product("1", "Pollo", 50.0, "Polleria", ProductUnit.UNIDAD)
    private val product2 = Product("2", "Res", 150.0, "Carniceria", ProductUnit.GRANEL)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Common setup: always return a flow of products
        whenever(productRepository.getAllProducts()).thenReturn(flowOf(listOf(product1, product2)))
        viewModel = TpvViewModel(productRepository, ticketRepository, printerHelper)
    }

    @Test
    fun `addProductToCart adds new item to active ticket`() {
        viewModel.addProductToCart(product1)

        val activeTicket = viewModel.uiState.value.activeTicket
        assertEquals(1, activeTicket.items.size)
        assertEquals(product1.code, activeTicket.items.first().product.code)
    }

    @Test
    fun `addProductToCart increments quantity for existing UNIDAD item`() {
        viewModel.addProductToCart(product1) // Add once
        viewModel.addProductToCart(product1) // Add again

        val activeTicket = viewModel.uiState.value.activeTicket
        assertEquals(1, activeTicket.items.size) // Should still be 1 item
        assertEquals(2.0, activeTicket.items.first().quantity, 0.0)
    }

    @Test
    fun `addTicket creates a new ticket and makes it active`() {
        val initialTicketCount = viewModel.uiState.value.tickets.size
        viewModel.addTicket()

        val newTickets = viewModel.uiState.value.tickets
        assertEquals(initialTicketCount + 1, newTickets.size)
        assertEquals(newTickets.size - 1, viewModel.uiState.value.activeTicketIndex)
    }

    @Test
    fun `setActiveTicket changes the active ticket`() {
        viewModel.addTicket() // We have 2 tickets now (index 0 and 1)
        viewModel.setActiveTicket(0)

        assertEquals(0, viewModel.uiState.value.activeTicketIndex)
    }

    @Test
    fun `closeTicket removes a ticket and adjusts active index`() {
        viewModel.addTicket()
        viewModel.addTicket() // 3 tickets, active is 2

        viewModel.closeTicket(1) // Close the middle ticket

        assertEquals(2, viewModel.uiState.value.tickets.size)
        assertEquals(1, viewModel.uiState.value.activeTicketIndex) // Active should now be the new index 1
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
