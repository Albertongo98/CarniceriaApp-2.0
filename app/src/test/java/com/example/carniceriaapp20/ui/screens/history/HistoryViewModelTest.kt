package com.example.carniceriaapp20.ui.screens.history

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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class HistoryViewModelTest {

    private val repository: TicketRepository = mock()
    private val printer: BluetoothPrinterHelper = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(repository.getAllTicketsWithItems()).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `anular un ticket lo manda al repositorio con el motivo sin espacios de sobra y avisa`() = runTest {
        whenever(repository.voidTicket(7, "Error de captura")).thenReturn(true)
        val viewModel = HistoryViewModel(repository, printer)

        viewModel.voidTicket(7, "  Error de captura  ")

        verify(repository).voidTicket(7, "Error de captura")
        assertEquals("Ticket anulado", viewModel.uiState.first { it.message != null }.message)
    }

    @Test
    fun `si el ticket ya estaba anulado avisa que no se pudo`() = runTest {
        whenever(repository.voidTicket(7, "x")).thenReturn(false)
        val viewModel = HistoryViewModel(repository, printer)

        viewModel.voidTicket(7, "x")

        val message = viewModel.uiState.first { it.message != null }.message!!
        assertEquals(true, message.startsWith("No se pudo anular"))
    }
}
