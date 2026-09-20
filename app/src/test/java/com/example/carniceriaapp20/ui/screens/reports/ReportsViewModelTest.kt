package com.example.carniceriaapp20.ui.screens.reports

import com.example.carniceriaapp20.data.local.ProductSalesReport
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.repository.TicketRepository
import com.example.carniceriaapp20.util.BluetoothPrinterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Calendar

@ExperimentalCoroutinesApi
class ReportsViewModelTest {

    private val repository: TicketRepository = mock()
    private val printer: BluetoothPrinterHelper = mock()
    private val today = startOfDay(System.currentTimeMillis())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun stubRepository(
        products: List<ProductSalesReport> = emptyList(),
        tickets: List<Ticket> = emptyList()
    ) {
        whenever(repository.getProductSalesReport(any(), any())).thenReturn(products)
        whenever(repository.getTicketsBetween(any(), any())).thenReturn(tickets)
    }

    @Test
    fun `report separa piezas de kilos segun la unidad guardada en la venta`() = runTest {
        stubRepository(
            products = listOf(
                ProductSalesReport("Bistec", "1", 500.0, 2.5, "Carniceria", ProductUnit.GRANEL),
                ProductSalesReport("Chorizo", "2", 60.0, 3.0, "Carniceria", ProductUnit.UNIDAD),
                ProductSalesReport("Tomate", "3", 40.0, 2.0, "Verdura", ProductUnit.GRANEL)
            )
        )

        val state = ReportsViewModel(repository, printer).uiState.value

        val carniceria = state.departmentSales.first { it.department == "Carniceria" }
        assertEquals(3.0, carniceria.totalPieces, 0.0)
        assertEquals(2.5, carniceria.totalKilos, 0.0)
        assertEquals(560.0, carniceria.totalAmount, 0.0)
        assertEquals(600.0, state.total, 0.0)
        assertEquals("Carniceria", state.departmentSales.first().department) // ordenado por importe
    }

    @Test
    fun `ventas por dia agrupa tickets por dia local en orden ascendente`() = runTest {
        val yesterdayNoon = addDays(today, -1) + 12 * 3_600_000L
        val todayMorning = today + 10 * 3_600_000L
        val todayAfternoon = today + 15 * 3_600_000L
        stubRepository(
            tickets = listOf(
                Ticket(1, todayAfternoon, 100.0, 2),
                Ticket(2, yesterdayNoon, 50.0, 1),
                Ticket(3, todayMorning, 25.0, 1)
            )
        )

        val state = ReportsViewModel(repository, printer).uiState.value

        assertEquals(3, state.ticketCount)
        assertEquals(2, state.dailySales.size)
        assertEquals(addDays(today, -1), state.dailySales[0].dayStart)
        assertEquals(1, state.dailySales[0].tickets)
        assertEquals(today, state.dailySales[1].dayStart)
        assertEquals(2, state.dailySales[1].tickets)
        assertEquals(125.0, state.dailySales[1].total, 0.0)
    }

    @Test
    fun `elegir un inicio muy anterior ajusta el fin para no pasar de 31 dias`() = runTest {
        stubRepository()
        val viewModel = ReportsViewModel(repository, printer)

        viewModel.onStartDateSelected(addDays(today, -60))

        val state = viewModel.uiState.value
        assertEquals(addDays(today, -60), state.startDate)
        assertEquals(addDays(today, -30), state.endDate)
        assertEquals(31, state.dayCount)
    }

    @Test
    fun `elegir un fin anterior al inicio mueve el inicio`() = runTest {
        stubRepository()
        val viewModel = ReportsViewModel(repository, printer)

        viewModel.onEndDateSelected(addDays(today, -3))

        val state = viewModel.uiState.value
        assertEquals(addDays(today, -3), state.startDate)
        assertEquals(addDays(today, -3), state.endDate)
        assertEquals(1, state.dayCount)
    }

    @Test
    fun `selectLastDays nunca supera 31 dias`() = runTest {
        stubRepository()
        val viewModel = ReportsViewModel(repository, printer)

        viewModel.selectLastDays(100)

        val state = viewModel.uiState.value
        assertEquals(today, state.endDate)
        assertEquals(31, state.dayCount)
    }

    @Test
    fun `mes anterior cubre el mes calendario completo`() = runTest {
        stubRepository()
        val viewModel = ReportsViewModel(repository, printer)

        viewModel.selectPreviousMonth()

        val state = viewModel.uiState.value
        val start = Calendar.getInstance().apply { timeInMillis = state.startDate }
        assertEquals(1, start.get(Calendar.DAY_OF_MONTH))
        assertEquals(firstOfMonth(today), addDays(state.endDate, 1)) // el día siguiente al fin es el 1 del mes actual
        assertEquals(start.getActualMaximum(Calendar.DAY_OF_MONTH), state.dayCount)
    }

    @Test
    fun `este mes va del dia 1 a hoy`() = runTest {
        stubRepository()
        val viewModel = ReportsViewModel(repository, printer)

        viewModel.selectThisMonth()

        val state = viewModel.uiState.value
        assertEquals(firstOfMonth(today), state.startDate)
        assertEquals(today, state.endDate)
    }
}
