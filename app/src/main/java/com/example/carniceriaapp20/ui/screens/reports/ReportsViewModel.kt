package com.example.carniceriaapp20.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.local.DaySalesReport
import com.example.carniceriaapp20.data.local.DepartmentSalesReport
import com.example.carniceriaapp20.data.local.ProductSalesReport
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.repository.TicketRepository
import com.example.carniceriaapp20.util.BluetoothPrinterHelper
import com.example.carniceriaapp20.util.PrintResult
import com.example.carniceriaapp20.util.ReportExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

// 31 para poder cubrir un mes calendario completo (hay meses de 31 días).
const val MAX_RANGE_DAYS = 31
private const val DAY_MS = 86_400_000.0

internal fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun addDays(millis: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    add(Calendar.DAY_OF_YEAR, days)
}.timeInMillis

// Primer día (medianoche local) del mes de `millis`, desplazado `monthOffset` meses.
internal fun firstOfMonth(millis: Long, monthOffset: Int = 0): Long = Calendar.getInstance().apply {
    timeInMillis = startOfDay(millis)
    set(Calendar.DAY_OF_MONTH, 1)
    add(Calendar.MONTH, monthOffset)
}.timeInMillis

// Se redondea porque un cambio de horario de verano hace que dos medianoches locales no distan exactamente 24 h.
internal fun daysBetween(start: Long, end: Long): Int = ((end - start) / DAY_MS).roundToInt()

data class ReportsUiState(
    val startDate: Long = startOfDay(System.currentTimeMillis()),
    val endDate: Long = startOfDay(System.currentTimeMillis()),
    val departmentSales: List<DepartmentSalesReport> = emptyList(),
    val productSales: List<ProductSalesReport> = emptyList(),
    val dailySales: List<DaySalesReport> = emptyList(),
    val ticketCount: Int = 0,
    val total: Double = 0.0,
    val isLoading: Boolean = false,
    val isPrinting: Boolean = false,
    val printResult: PrintResult? = null
) {
    val isSingleDay: Boolean get() = startDate == endDate
    val dayCount: Int get() = daysBetween(startDate, endDate) + 1
}

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val printerHelper: BluetoothPrinterHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadReport()
    }

    // Si el nuevo inicio deja el fin fuera de [inicio, inicio + 30 días], el fin se ajusta.
    fun onStartDateSelected(date: Long) {
        val start = startOfDay(date)
        val end = _uiState.value.endDate.coerceIn(start, addDays(start, MAX_RANGE_DAYS - 1))
        setRange(start, end)
    }

    // Si el nuevo fin deja el inicio fuera de [fin - 30 días, fin], el inicio se ajusta.
    fun onEndDateSelected(date: Long) {
        val end = startOfDay(date)
        val start = _uiState.value.startDate.coerceIn(addDays(end, -(MAX_RANGE_DAYS - 1)), end)
        setRange(start, end)
    }

    fun selectLastDays(days: Int) {
        val today = startOfDay(System.currentTimeMillis())
        setRange(addDays(today, -(days.coerceIn(1, MAX_RANGE_DAYS) - 1)), today)
    }

    fun selectThisMonth() {
        val today = startOfDay(System.currentTimeMillis())
        setRange(firstOfMonth(today), today)
    }

    fun selectPreviousMonth() {
        val today = startOfDay(System.currentTimeMillis())
        setRange(firstOfMonth(today, -1), addDays(firstOfMonth(today), -1))
    }

    private fun setRange(start: Long, end: Long) {
        _uiState.update { it.copy(startDate = start, endDate = end) }
        loadReport()
    }

    private fun loadReport() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val from = _uiState.value.startDate
            val until = addDays(_uiState.value.endDate, 1)

            val prodSales = ticketRepository.getProductSalesReport(from, until)
            val tickets = ticketRepository.getTicketsBetween(from, until)

            // Se deriva del desglose por producto (que ya trae la unidad de cada uno) en vez de
            // sumar quantity a nivel departamento, porque mezclar piezas con kilos en una sola
            // cifra no significa nada (ej. "5.5 movs." de 3 piezas + 2.5 kg).
            val deptSales = prodSales.groupBy { it.department }
                .map { (department, items) ->
                    val (granel, unidad) = items.partition { it.effectiveUnit == ProductUnit.GRANEL }
                    DepartmentSalesReport(
                        department = department,
                        totalAmount = items.sumOf { it.totalAmount },
                        totalPieces = unidad.sumOf { it.totalQuantity },
                        totalKilos = granel.sumOf { it.totalQuantity }
                    )
                }
                .sortedByDescending { it.totalAmount }

            val dailySales = tickets.groupBy { startOfDay(it.timestamp) }
                .map { (day, dayTickets) -> DaySalesReport(day, dayTickets.size, dayTickets.sumOf { it.totalAmount }) }
                .sortedBy { it.dayStart }

            _uiState.update {
                it.copy(
                    departmentSales = deptSales,
                    productSales = prodSales,
                    dailySales = dailySales,
                    ticketCount = tickets.size,
                    total = deptSales.sumOf { dept -> dept.totalAmount },
                    isLoading = false
                )
            }
        }
    }

    fun getHtmlReportContent(): Pair<String, String> {
        val state = _uiState.value
        val content = ReportExporter.generateHtmlReport(
            state.startDate,
            state.endDate,
            state.total,
            state.ticketCount,
            state.departmentSales,
            state.productSales,
            state.dailySales
        )
        val fileDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val name = if (state.isSingleDay) {
            "reporte_${fileDate.format(Date(state.startDate))}.html"
        } else {
            "reporte_${fileDate.format(Date(state.startDate))}_${fileDate.format(Date(state.endDate))}.html"
        }
        return name to content
    }

    fun printReport() {
        if (_uiState.value.isPrinting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPrinting = true) }
            val state = _uiState.value

            val result = printerHelper.printSalesReport(
                period = ReportExporter.formatPeriod(state.startDate, state.endDate),
                total = state.total,
                ticketCount = state.ticketCount,
                deptSales = state.departmentSales,
                prodSales = state.productSales,
                dailySales = state.dailySales
            )

            _uiState.update { it.copy(isPrinting = false, printResult = result) }
        }
    }

    fun onPrintResultConsumed() {
        _uiState.update { it.copy(printResult = null) }
    }
}
