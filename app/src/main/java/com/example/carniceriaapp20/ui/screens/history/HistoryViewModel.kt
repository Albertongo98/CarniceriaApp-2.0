package com.example.carniceriaapp20.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Embedded
import androidx.room.Relation
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.local.TicketItem
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
    val isSelectionMode: Boolean = false
)

data class TicketWithItems(
    @Embedded val ticket: Ticket,
    @Relation(
        parentColumn = "id",
        entityColumn = "ticket_id"
    )
    val items: List<TicketItem>
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val printerHelper: BluetoothPrinterHelper
) : ViewModel() {

    private val _printResult = MutableStateFlow<PrintResult?>(null)
    private val _ticketsWithItems = ticketRepository.getAllTicketsWithItems()
    private val _selectedTicketIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<HistoryUiState> = combine(
        _ticketsWithItems, 
        _printResult, 
        _selectedTicketIds
    ) { tickets, printResult, selectedIds ->
        HistoryUiState(
            tickets = tickets,
            printResult = printResult,
            selectedTicketIds = selectedIds,
            isSelectionMode = selectedIds.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun toggleTicketSelection(ticketId: Long) { 
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
        val allTickets = uiState.value.tickets
        val selectedTickets = allTickets.filter { 
            _selectedTicketIds.value.contains(it.ticket.id)
        }
        if (selectedTickets.isEmpty()) return

        viewModelScope.launch {
            var finalResult: PrintResult? = null
            withContext(Dispatchers.IO) {
                for (ticketWithItems in selectedTickets) {
                    val cartItems = ticketWithItems.items.map { ticketItem ->
                        // Reconstruct CartItem from TicketItem for printing
                        val isProbablyGranel = ticketItem.quantity.rem(1.0) != 0.0
                        CartItem(
                            product = Product(
                                code = ticketItem.productCode ?: "",
                                name = ticketItem.productName ?: "Producto Borrado",
                                price = ticketItem.unitPrice,
                                department = "", // Not needed for printing
                                unit = if(isProbablyGranel) ProductUnit.GRANEL else ProductUnit.UNIDAD
                            ),
                            quantity = ticketItem.quantity,
                            customPrice = null // totalPrice is derived from unitPrice * quantity
                        )
                    }
                    
                    finalResult = printerHelper.printTicket(
                        ticket = ticketWithItems.ticket,
                        items = cartItems,
                        folio = ticketWithItems.ticket.id.toString()
                    )
                }
            }
            _printResult.value = finalResult

            if (finalResult is PrintResult.Success) {
                clearSelection()
            }
        }
    }

    fun onPrintResultConsumed() {
        _printResult.value = null
    }
}
