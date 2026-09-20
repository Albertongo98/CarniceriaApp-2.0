package com.example.carniceriaapp20.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.local.TicketWithItems
import com.example.carniceriaapp20.data.repository.TicketRepository
import com.example.carniceriaapp20.ui.screens.tpv.CartItem
import com.example.carniceriaapp20.ui.screens.tpv.toCartItem
import com.example.carniceriaapp20.util.BluetoothPrinterHelper
import com.example.carniceriaapp20.util.PrintResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HistoryUiState(
    val tickets: List<TicketWithItems> = emptyList(),
    val printResult: PrintResult? = null,
    val selectedTicketIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isPrinting: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val printerHelper: BluetoothPrinterHelper
) : ViewModel() {

    private val _printResult = MutableStateFlow<PrintResult?>(null)
    private val _ticketsWithItems: Flow<List<TicketWithItems>> = ticketRepository.getAllTicketsWithItems()
    private val _selectedTicketIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isPrinting = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        _ticketsWithItems, 
        _printResult, 
        _selectedTicketIds,
        _isPrinting,
        _message
    ) { tickets: List<TicketWithItems>, printResult: PrintResult?, selectedIds: Set<Long>, printing: Boolean, message: String? ->
        HistoryUiState(
            tickets = tickets,
            printResult = printResult,
            selectedTicketIds = selectedIds,
            isSelectionMode = selectedIds.isNotEmpty(),
            isPrinting = printing,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun toggleTicketSelection(ticketId: Long) { 
        if (_isPrinting.value) return
        val currentSelection = _selectedTicketIds.value.toMutableSet()
        if (currentSelection.contains(ticketId)) {
            currentSelection.remove(ticketId)
        } else {
            currentSelection.add(ticketId)
        }
        _selectedTicketIds.value = currentSelection
    }

    fun clearSelection() {
        _selectedTicketIds.value = emptySet()
    }

    fun printSelectedTicketsForAudit() {
        if (_isPrinting.value) return
        val allTickets: List<TicketWithItems> = uiState.value.tickets
        val selectedTickets: List<TicketWithItems> = allTickets.filter { ticketWithItems: TicketWithItems -> 
            _selectedTicketIds.value.contains(ticketWithItems.ticket.id)
        }.sortedBy { it.ticket.timestamp } 

        if (selectedTickets.isEmpty()) return

        viewModelScope.launch {
            _isPrinting.value = true
            
            // Especificamos el tipo de batchData para ayudar al compilador
            val batchData: List<Pair<Ticket, List<CartItem>>> = selectedTickets.map { ticketWithItems: TicketWithItems ->
                Pair(ticketWithItems.ticket, ticketWithItems.items.map { it.toCartItem() })
            }

            // Especificamos el tipo genérico <PrintResult> en withContext
            val finalResult: PrintResult = withContext<PrintResult>(Dispatchers.IO) {
                printerHelper.printTicketsBatch(batchData)
            }

            _printResult.value = finalResult
            _isPrinting.value = false
            if (finalResult is PrintResult.Success) clearSelection()
        }
    }

    fun voidTicket(ticketId: Long, reason: String) {
        viewModelScope.launch {
            val voided = ticketRepository.voidTicket(ticketId, reason.trim())
            _message.value = if (voided) "Ticket anulado" else "No se pudo anular el ticket (ya estaba anulado)"
        }
    }

    fun onMessageConsumed() {
        _message.value = null
    }

    fun onPrintResultConsumed() {
        _printResult.value = null
    }
}
