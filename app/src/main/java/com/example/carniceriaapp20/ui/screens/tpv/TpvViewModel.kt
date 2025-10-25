package com.example.carniceriaapp20.ui.screens.tpv

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.local.TicketItem
import com.example.carniceriaapp20.data.preferences.UserPreferencesRepository
import com.example.carniceriaapp20.data.repository.ProductRepository
import com.example.carniceriaapp20.data.repository.TicketRepository
import com.example.carniceriaapp20.util.BluetoothPrinterHelper
import com.example.carniceriaapp20.util.PrintResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// Data classes for UI State
data class CartItem(
    val product: Product,
    var quantity: Double = 1.0,
    var customPrice: Double? = null // For GRANEL products, manual total price
) {
    val totalPrice: Double
        get() = customPrice ?: (product.price * quantity)
}

data class TicketState(
    val id: Int,
    val items: List<CartItem> = emptyList()
) {
    val total: Double
        get() = items.sumOf { it.totalPrice }
}

data class TpvUiState(
    val allProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val tickets: List<TicketState> = listOf(TicketState(id = 1)),
    val activeTicketIndex: Int = 0,
    val showConfirmSaleDialog: Boolean = false,
    val showNoPrinterDialog: Boolean = false,
    val selectedCartItem: CartItem? = null,
    val keypadInput: String = "",
    val printResult: PrintResult? = null,
    // Settings Dialog State
    val showSettingsDialog: Boolean = false,
    val pairedDevices: List<Pair<String, String>> = emptyList(),
    val selectedPrinterMac: String? = null
) {
    val activeTicket: TicketState
        get() = tickets.getOrElse(activeTicketIndex) { tickets.first() }

    val filteredProducts: Map<String, List<Product>>
        get() = allProducts
            .filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.code.contains(searchQuery, ignoreCase = true) 
            }
            .groupBy { it.department }
    
    val topSellingProducts: List<Product>
        get() = allProducts.take(10) // Placeholder for actual top selling logic
}

@HiltViewModel
class TpvViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val ticketRepository: TicketRepository,
    private val printerHelper: BluetoothPrinterHelper,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _tickets = MutableStateFlow(listOf(TicketState(id = 1)))
    private val _activeTicketIndex = MutableStateFlow(0)
    private var nextTicketId = 2
    private val _showConfirmSaleDialog = MutableStateFlow(false)
    private val _showNoPrinterDialog = MutableStateFlow(false)
    private val _selectedCartItem = MutableStateFlow<CartItem?>(null)
    private val _keypadInput = MutableStateFlow("")
    private val _printResult = MutableStateFlow<PrintResult?>(null)
    // Settings Dialog State
    private val _showSettingsDialog = MutableStateFlow(false)
    private val _pairedDevices = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    private val _selectedPrinterMac = userPreferencesRepository.printerMacAddress


    val uiState: StateFlow<TpvUiState> = combine(
        productRepository.getAllProducts(),
        _searchQuery,
        _tickets,
        _activeTicketIndex,
        _showConfirmSaleDialog,
    ) { allProducts, query, tickets, activeIndex, showDialog ->
        TpvUiState(
            allProducts = allProducts,
            searchQuery = query,
            tickets = tickets,
            activeTicketIndex = activeIndex,
            showConfirmSaleDialog = showDialog
        )
    }.combine(_showNoPrinterDialog) { uiState, showNoPrinter ->
        uiState.copy(showNoPrinterDialog = showNoPrinter)
    }.combine(_selectedCartItem) { uiState, selected ->
        uiState.copy(selectedCartItem = selected)
    }.combine(_keypadInput) { uiState, keypad ->
        uiState.copy(keypadInput = keypad)
    }.combine(_printResult) { uiState, printResult ->
        uiState.copy(printResult = printResult)
    }.combine(_showSettingsDialog) { uiState, showSettings ->
        uiState.copy(showSettingsDialog = showSettings)
    }.combine(_pairedDevices) { uiState, devices ->
        uiState.copy(pairedDevices = devices)
    }.combine(_selectedPrinterMac) { uiState, selectedMac ->
        uiState.copy(selectedPrinterMac = selectedMac)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TpvUiState()
    )

    // TPV Logic
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun addProductToCart(product: Product) {
        val currentTickets = _tickets.value.toMutableList()
        val activeTicket = uiState.value.activeTicket
        val newItems = activeTicket.items.toMutableList()

        val existingItem = newItems.find { it.product.code == product.code && it.product.unit == ProductUnit.UNIDAD }

        if (existingItem != null) {
            val updatedItem = existingItem.copy(quantity = existingItem.quantity + 1)
            val itemIndex = newItems.indexOf(existingItem)
            newItems[itemIndex] = updatedItem
        } else {
            newItems.add(CartItem(product = product))
        }

        currentTickets[uiState.value.activeTicketIndex] = activeTicket.copy(items = newItems)
        _tickets.value = currentTickets
        onSearchQueryChange("")
    }

    fun incrementCartItemQuantity(item: CartItem) {
        if (item.product.unit != ProductUnit.UNIDAD) return

        val currentTickets = _tickets.value.toMutableList()
        val activeTicket = uiState.value.activeTicket
        val newItems = activeTicket.items.toMutableList()

        val itemIndex = newItems.indexOf(item)
        if (itemIndex != -1) {
            val updatedItem = item.copy(quantity = item.quantity + 1)
            newItems[itemIndex] = updatedItem
            currentTickets[uiState.value.activeTicketIndex] = activeTicket.copy(items = newItems)
            _tickets.value = currentTickets
        }
    }

    fun decrementCartItemQuantity(item: CartItem) {
        if (item.product.unit != ProductUnit.UNIDAD) return

        if (item.quantity > 1) {
            val currentTickets = _tickets.value.toMutableList()
            val activeTicket = uiState.value.activeTicket
            val newItems = activeTicket.items.toMutableList()
            val itemIndex = newItems.indexOf(item)
            if (itemIndex != -1) {
                val updatedItem = item.copy(quantity = item.quantity - 1)
                newItems[itemIndex] = updatedItem
                currentTickets[uiState.value.activeTicketIndex] = activeTicket.copy(items = newItems)
                _tickets.value = currentTickets
            }
        } else {
            removeItemFromCart(item)
        }
    }

    fun removeItemFromCart(item: CartItem) {
        if (_selectedCartItem.value == item) {
            onSelectItem(null)
        }
        val currentTickets = _tickets.value.toMutableList()
        val activeTicket = uiState.value.activeTicket
        val newItems = activeTicket.items.toMutableList()

        newItems.remove(item)

        currentTickets[uiState.value.activeTicketIndex] = activeTicket.copy(items = newItems)
        _tickets.value = currentTickets
    }

    fun onSelectItem(item: CartItem?) {
        _selectedCartItem.value = item
        _keypadInput.value = ""
    }

    fun onKeypadInput(key: String) {
        _keypadInput.value += key
    }

    fun onKeypadClear() {
        _keypadInput.value = ""
    }

    fun onKeypadBackspace() {
        _keypadInput.value = _keypadInput.value.dropLast(1)
    }

    fun onApplyKeypadInput(isPrice: Boolean) {
        val inputAsDouble = _keypadInput.value.toDoubleOrNull() ?: return
        val selectedItem = _selectedCartItem.value ?: return

        val currentTickets = _tickets.value.toMutableList()
        val activeTicket = uiState.value.activeTicket
        val newItems = activeTicket.items.toMutableList()
        val itemIndex = newItems.indexOf(selectedItem)

        if (itemIndex != -1) {
            val updatedItem = if (isPrice && selectedItem.product.unit == ProductUnit.GRANEL) {
                val newQuantity = if (selectedItem.product.price > 0) {
                    inputAsDouble / selectedItem.product.price
                } else { 1.0 } // Avoid division by zero
                selectedItem.copy(customPrice = inputAsDouble, quantity = newQuantity)
            } else {
                selectedItem.copy(quantity = inputAsDouble, customPrice = null)
            }
            newItems[itemIndex] = updatedItem
            currentTickets[uiState.value.activeTicketIndex] = activeTicket.copy(items = newItems)
            _tickets.value = currentTickets
        }
        onSelectItem(null) // Deselect after applying
    }

    fun addTicket() {
        val newTickets = _tickets.value.toMutableList()
        newTickets.add(TicketState(id = nextTicketId++))
        _tickets.value = newTickets
        _activeTicketIndex.value = newTickets.size - 1
    }

    fun setActiveTicket(index: Int) {
        if (index >= 0 && index < _tickets.value.size) {
            _activeTicketIndex.value = index
            onSelectItem(null)
        }
    }

    fun closeTicket(index: Int) {
        val currentTickets = _tickets.value.toMutableList()
        if (index < 0 || index >= currentTickets.size) return

        if (_selectedCartItem.value in currentTickets[index].items) {
            onSelectItem(null)
        }
        currentTickets.removeAt(index)

        if (currentTickets.isEmpty()) {
            currentTickets.add(TicketState(id = nextTicketId++))
            _activeTicketIndex.value = 0
        } else if (_activeTicketIndex.value >= index) {
            _activeTicketIndex.value = (_activeTicketIndex.value - 1).coerceAtLeast(0)
        }
        
        _tickets.value = currentTickets
    }

    fun onFinalizeSaleClick() {
        viewModelScope.launch {
            if (uiState.value.activeTicket.items.isEmpty()) return@launch

            val printerAddress = userPreferencesRepository.printerMacAddress.first()
            if (printerAddress == null) {
                _showNoPrinterDialog.value = true
            } else {
                _showConfirmSaleDialog.value = true
            }
        }
    }

    fun onDismissFinalizeSaleDialog() {
        _showConfirmSaleDialog.value = false
    }

    fun onDismissNoPrinterDialog() {
        _showNoPrinterDialog.value = false
    }
    
    fun onPrintResultConsumed() {
        _printResult.value = null
    }

    fun confirmSale() {
        viewModelScope.launch {
            val activeTicketState = uiState.value.activeTicket
            if (activeTicketState.items.isEmpty()) return@launch

            val ticket = Ticket(
                timestamp = System.currentTimeMillis(),
                totalAmount = activeTicketState.total
            )
            val ticketItems = activeTicketState.items.map { cartItem ->
                TicketItem(
                    ticketId = 0, 
                    productCode = cartItem.product.code,
                    productName = cartItem.product.name,
                    quantity = cartItem.quantity,
                    unitPrice = cartItem.customPrice ?: cartItem.product.price,
                    totalPrice = cartItem.totalPrice
                )
            }
            
            val savedTicketId = ticketRepository.saveTicket(ticket, ticketItems)
            
            withContext(Dispatchers.IO) {
                var finalResult = printerHelper.printTicket(ticket, activeTicketState.items, savedTicketId.toString())
                if (finalResult is PrintResult.Success) {
                    val dynamicDelay = 1000L + (activeTicketState.items.size * 600L)
                    delay(dynamicDelay) // Dynamic delay based on ticket size
                    val flushResult = printerHelper.flushPrinter()
                    if (flushResult is PrintResult.Error) { // Report flush error if it happens
                        finalResult = flushResult
                    }
                }
                _printResult.value = finalResult
            }
            
            closeTicket(uiState.value.activeTicketIndex)
            _showConfirmSaleDialog.value = false
        }
    }

    fun reprintLastTicket() {
        viewModelScope.launch {
            val lastTicket = ticketRepository.getLastTicket()
            if (lastTicket != null) {
                val lastTicketWithItems = ticketRepository.getTicketWithItems(lastTicket.id).first()
                if (lastTicketWithItems != null) {
                    val allProducts = productRepository.getAllProducts().first()
                    val productMap = allProducts.associateBy { it.code }

                    val cartItems = lastTicketWithItems.items.mapNotNull { ticketItem ->
                        val product = productMap[ticketItem.productCode]
                        if (product != null) {
                            CartItem(
                                product = product,
                                quantity = ticketItem.quantity,
                                // Restore custom price only if it was a GRANEL product and the unit price differs
                                customPrice = if (product.unit == ProductUnit.GRANEL && ticketItem.unitPrice != product.price) {
                                    ticketItem.totalPrice
                                } else {
                                    null
                                }
                            )
                        } else {
                            null // Product not found, skip
                        }
                    }

                    withContext(Dispatchers.IO) {
                        var finalResult = printerHelper.printTicket(
                            lastTicketWithItems.ticket,
                            cartItems,
                            lastTicketWithItems.ticket.id.toString(),
                            withLogo = false // Do not print logo on reprint
                        )
                        if (finalResult is PrintResult.Success) {
                            val dynamicDelay = 1000L + (cartItems.size * 600L)
                            delay(dynamicDelay) // Dynamic delay based on ticket size
                            val flushResult = printerHelper.flushPrinter()
                             if (flushResult is PrintResult.Error) { // Report flush error if it happens
                                finalResult = flushResult
                            }
                        }
                        _printResult.value = finalResult
                    }
                } else {
                    _printResult.value = PrintResult.Error("No se encontraron los productos del último ticket.")
                }
            } else {
                 _printResult.value = PrintResult.Error("No hay ningún ticket para reimprimir.")
            }
        }
    }

    // Settings Logic
    fun onSettingsClick() {
        refreshPairedDevices()
        _showSettingsDialog.value = true
    }

    fun onDismissSettingsDialog() {
        _showSettingsDialog.value = false
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val devices = printerHelper.getPairedDevices()?.map { 
            it.name to it.address 
        } ?: emptyList()
        _pairedDevices.value = devices
    }

    fun selectPrinter(macAddress: String) {
        viewModelScope.launch {
            userPreferencesRepository.savePrinterMacAddress(macAddress)
        }
    }
}
