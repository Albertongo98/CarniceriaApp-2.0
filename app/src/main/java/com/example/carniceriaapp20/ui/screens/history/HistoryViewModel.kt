package com.example.carniceriaapp20.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.local.TicketItem
import com.example.carniceriaapp20.data.local.TicketWithItems
import com.example.carniceriaapp20.data.repository.TicketRepository
import com.example.carniceriaapp20.ui.screens.tpv.CartItem
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
    val isPrinting: Boolean = false
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

    val uiState: StateFlow<HistoryUiState> = combine(
        _ticketsWithItems, 
        _printResult, 
        _selectedTicketIds,
        _isPrinting
    ) { tickets: List<TicketWithItems>, printResult: PrintResult?, selectedIds: Set<Long>, printing: Boolean ->
        HistoryUiState(
            tickets = tickets,
            printResult = printResult,
            selectedTicketIds = selectedIds,
            isSelectionMode = selectedIds.isNotEmpty(),
            isPrinting = printing
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
                val cartItems: List<CartItem> = ticketWithItems.items.map { ticketItem: TicketItem ->
                    val isProbablyGranel = ticketItem.quantity % 1.0 != 0.0 || ticketItem.unitPrice == ticketItem.totalPrice
                    CartItem(
                        product = Product(
                            code = ticketItem.productCode ?: "",
                            name = ticketItem.productName,
                            price = ticketItem.unitPrice,
                            department = "", 
                            unit = if(isProbablyGranel) ProductUnit.GRANEL else ProductUnit.UNIDAD
                        ),
                        quantity = ticketItem.quantity,
                        customPrice = if (ticketItem.unitPrice == ticketItem.totalPrice && ticketItem.quantity != 1.0) ticketItem.totalPrice else null
                    )
                }
                Pair(ticketWithItems.ticket, cartItems)
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

    fun onPrintResultConsumed() {
        _printResult.value = null
    }
}
