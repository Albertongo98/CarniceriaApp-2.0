package com.example.carniceriaapp20.ui.screens.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.local.DepartmentSalesReport
import com.example.carniceriaapp20.data.local.ProductSalesReport
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.local.TicketItem
import com.example.carniceriaapp20.data.repository.TicketRepository
import com.example.carniceriaapp20.util.BluetoothPrinterHelper
import com.example.carniceriaapp20.util.PrintResult
import com.example.carniceriaapp20.util.ReportExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ReportsUiState(
    val selectedDate: Long = System.currentTimeMillis(),
    val departmentSales: List<DepartmentSalesReport> = emptyList(),
    val productSales: List<ProductSalesReport> = emptyList(),
    val isLoading: Boolean = false,
    val isPrinting: Boolean = false,
    val printResult: PrintResult? = null,
    val totalDay: Double = 0.0
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val printerHelper: BluetoothPrinterHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDailyReport()
    }

    fun onDateSelected(date: Long) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadDailyReport()
    }

    fun loadDailyReport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val calendar = Calendar.getInstance().apply {
                timeInMillis = _uiState.value.selectedDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = calendar.timeInMillis

            val prodSales = ticketRepository.getProductSalesReport(startOfDay, endOfDay)
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
            val total = deptSales.sumOf { it.totalAmount }
            
            _uiState.value = _uiState.value.copy(
                departmentSales = deptSales,
                productSales = prodSales,
                isLoading = false,
                totalDay = total
            )
        }
    }

    fun getHtmlReportContent(): Pair<String, String> {
        val state = _uiState.value
        val content = ReportExporter.generateHtmlReport(
            state.selectedDate,
            state.totalDay,
            state.departmentSales,
            state.productSales
        )
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(state.selectedDate))
        return "reporte_$dateStr.html" to content
    }

    fun printReport() {
        if (_uiState.value.isPrinting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrinting = true)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateStr = dateFormat.format(Date(_uiState.value.selectedDate))
            
            val result = printerHelper.printSalesReport(
                date = dateStr,
                totalDay = _uiState.value.totalDay,
                deptSales = _uiState.value.departmentSales,
                prodSales = _uiState.value.productSales
            )
            
            _uiState.value = _uiState.value.copy(
                isPrinting = false,
                printResult = result
            )
        }
    }

    fun onPrintResultConsumed() {
        _uiState.value = _uiState.value.copy(printResult = null)
    }

    fun createDemoData() {
        viewModelScope.launch {
            val now = _uiState.value.selectedDate
            ticketRepository.saveTicket(
                Ticket(timestamp = now, totalAmount = 450.50, dailyFolio = 991),
                listOf(
                    TicketItem(0, 0, null, "Bisteck de Res", "CARNICERIA", 1.5, 180.0, 270.0),
                    TicketItem(0, 0, null, "Tomate", "VERDURA", 2.0, 45.0, 90.0),
                    TicketItem(0, 0, null, "Cebolla", "VERDURA", 1.0, 35.0, 35.0),
                    TicketItem(0, 0, null, "Molida de Res", "CARNICERIA", 0.5, 111.0, 55.50)
                )
            )
            loadDailyReport()
        }
    }

    fun generateReportText(): String {
        val state = _uiState.value
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"))
        val dateStr = dateFormat.format(Date(state.selectedDate))

        val sb = StringBuilder()
        sb.append("📊 REPORTE DE VENTAS - $dateStr\n\n")
        state.departmentSales.forEach { report ->
            sb.append("📍 ${report.department}: ${currencyFormat.format(report.totalAmount)}\n")
        }
        sb.append("\n💰 TOTAL: ${currencyFormat.format(state.totalDay)}")
        return sb.toString()
    }
}
