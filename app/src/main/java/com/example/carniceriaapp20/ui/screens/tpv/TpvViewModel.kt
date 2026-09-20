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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

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
    private val _activeKeyboard = MutableStateFlow(KeyboardType.NUMERIC)
    private val _isPrinting = MutableStateFlow(false)
    private val _showSettingsDialog = MutableStateFlow(false)
    private val _pairedDevices = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    private val _selectedPrinterMac = userPreferencesRepository.printerMacAddress

    // Aviso al abrir el TPV si hace 7 días o más que no se respalda (o nunca): el historial vive solo en la tablet.
    private val _backupReminder = MutableStateFlow<String?>(null)
    val backupReminder: StateFlow<String?> = _backupReminder.asStateFlow()

    init {
        viewModelScope.launch {
            val last = userPreferencesRepository.lastBackupAt.first()
            val days = last?.let { (System.currentTimeMillis() - it) / 86_400_000L }
            _backupReminder.value = when {
                last == null -> "Aún no has respaldado tus ventas. Hazlo en Menú > Respaldo y diagnóstico."
                days!! >= 7 -> "Hace $days días que no respaldas tus ventas (Menú > Respaldo y diagnóstico)."
                else -> null
            }
        }
    }

    fun onBackupReminderShown() { _backupReminder.value = null }

    private val _fastProducts = productRepository.getTopSellingProducts()
        .onStart { emit(emptyList()) }
        .catch { emit(emptyList()) }

    private suspend fun generateDailyFolio(): Int {
        return try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            ticketRepository.countTicketsOfDay(startOfDay) + 1
        } catch (e: Exception) { 1 }
    }

    val uiState: StateFlow<TpvUiState> = combine(
        productRepository.getAllProducts().onStart { emit(emptyList()) }.catch { emit(emptyList()) },
        _searchQuery,
        _tickets,
        _activeTicketIndex,
        _selectedCartItem
    ) { allProducts, query, tickets, activeIndex, selectedItem ->
        val filtered = allProducts
            .filter { it.name.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true) }
            .groupBy { it.department }
            
        TpvUiState(
            filteredProducts = filtered,
            searchQuery = query,
            tickets = tickets,
            activeTicketIndex = activeIndex,
            selectedCartItem = selectedItem
        )
    }
    .combine(_fastProducts) { state, fast -> state.copy(fastProducts = fast) }
    .combine(_keypadInput) { state, keypad -> state.copy(keypadInput = keypad) }
    .combine(_activeKeyboard) { state, kb -> state.copy(activeKeyboard = kb) }
    .combine(_isPrinting) { state, printing -> state.copy(isPrinting = printing) }
    .combine(_showConfirmSaleDialog) { state, show -> state.copy(showConfirmSaleDialog = show) }
    .combine(_showNoPrinterDialog) { state, show -> state.copy(showNoPrinterDialog = show) }
    .combine(_showSettingsDialog) { state, show -> state.copy(showSettingsDialog = show) }
    .combine(_pairedDevices) { state, devices -> state.copy(pairedDevices = devices) }
    .combine(_selectedPrinterMac.onStart { emit(null) }.catch { emit(null) }) { state, mac -> state.copy(selectedPrinterMac = mac) }
    .combine(_printResult) { state, result -> state.copy(printResult = result) }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TpvUiState())

    fun confirmSale() {
        if (_isPrinting.value) return
        viewModelScope.launch {
            _isPrinting.value = true
            try {
                val activeTicketState = uiState.value.activeTicket
                if (activeTicketState.items.isEmpty()) return@launch

                val nextFolio = generateDailyFolio()
                val ticket = Ticket(
                    timestamp = System.currentTimeMillis(), 
                    totalAmount = activeTicketState.total,
                    dailyFolio = nextFolio
                )
                
                val savedTicketId = ticketRepository.saveTicket(ticket, activeTicketState.items.map {
                    TicketItem(
                        id = 0,
                        ticketId = 0,
                        productCode = it.product.code,
                        productName = it.product.name,
                        productDepartment = it.product.department, // MODIFICADO: Guardar departamento para reportes
                        quantity = it.quantity,
                        unitPrice = it.customPrice ?: it.product.price,
                        totalPrice = it.totalPrice,
                        estimatedPieces = it.estimatedPieces,
                        unit = it.product.unit
                    )
                })

                withContext(Dispatchers.IO) {
                    val finalResult = printerHelper.printTicket(ticket.copy(id = savedTicketId), activeTicketState.items, nextFolio.toString().padStart(3, '0'))
                    _printResult.value = finalResult
                }
                
                closeTicket(_activeTicketIndex.value)
                _showConfirmSaleDialog.value = false
            } catch (e: Exception) {
                _printResult.value = PrintResult.Error("Error al guardar venta: ${e.message}")
            } finally {
                _isPrinting.value = false
            }
        }
    }

    fun applyEstimatedPieces() {
        val inputAsInt = _keypadInput.value.toIntOrNull() ?: return
        val selectedItem = _selectedCartItem.value ?: return
        
        val currentTickets = _tickets.value.toMutableList()
        val activeTicketIndex = _activeTicketIndex.value
        val activeTicket = currentTickets[activeTicketIndex]
        val newItems = activeTicket.items.toMutableList()
        val itemIndex = newItems.indexOf(selectedItem)
        
        if (itemIndex != -1) {
            newItems[itemIndex] = selectedItem.copy(estimatedPieces = inputAsInt)
            currentTickets[activeTicketIndex] = activeTicket.copy(items = newItems)
            _tickets.value = currentTickets
        }
        onSelectItem(null)
    }

    fun applyQuickAmount(amount: Double) {
        val currentTickets = _tickets.value.toMutableList()
        val activeIndex = _activeTicketIndex.value
        val activeTicket = currentTickets[activeIndex]
        val targetItem = _selectedCartItem.value ?: activeTicket.items.lastOrNull() ?: return
        val newItems = activeTicket.items.toMutableList()
        val itemIndex = newItems.indexOf(targetItem)
        if (itemIndex != -1) {
            val updatedItem = if (targetItem.product.unit == ProductUnit.GRANEL) {
                val newQuantity = if (targetItem.product.price > 0) amount / targetItem.product.price else 1.0
                targetItem.copy(customPrice = amount, quantity = newQuantity)
            } else {
                val newQuantity = if (targetItem.product.price > 0) (amount / targetItem.product.price).toInt().toDouble().coerceAtLeast(1.0) else 1.0
                targetItem.copy(quantity = newQuantity, customPrice = null)
            }
            newItems[itemIndex] = updatedItem
            currentTickets[activeIndex] = activeTicket.copy(items = newItems)
            _tickets.value = currentTickets
        }
        onSelectItem(null)
    }

    fun onQwertyKeyPress(key: String) {
        when (key) {
            "BACKSPACE" -> if (_searchQuery.value.isNotEmpty()) _searchQuery.value = _searchQuery.value.dropLast(1)
            " " -> _searchQuery.value += " "
            else -> _searchQuery.value += key
        }
    }

    fun onKeypadInput(key: String) { _keypadInput.value += key }
    fun onKeypadClear() { _keypadInput.value = "" }
    fun onKeypadBackspace() { if (_keypadInput.value.isNotEmpty()) _keypadInput.value = _keypadInput.value.dropLast(1) }

    fun onApplyKeypadInput(isPrice: Boolean) {
        val inputAsDouble = _keypadInput.value.toDoubleOrNull() ?: return
        val selectedItem = _selectedCartItem.value ?: return
        val currentTickets = _tickets.value.toMutableList()
        val activeTicketIndex = _activeTicketIndex.value
        val activeTicket = currentTickets[activeTicketIndex]
        val newItems = activeTicket.items.toMutableList()
        val itemIndex = newItems.indexOf(selectedItem)
        if (itemIndex != -1) {
            val updatedItem = if (isPrice && selectedItem.product.unit == ProductUnit.GRANEL) {
                val newQuantity = if (selectedItem.product.price > 0) inputAsDouble / selectedItem.product.price else 1.0
                selectedItem.copy(customPrice = inputAsDouble, quantity = newQuantity)
            } else {
                selectedItem.copy(quantity = inputAsDouble, customPrice = null)
            }
            newItems[itemIndex] = updatedItem
            currentTickets[activeTicketIndex] = activeTicket.copy(items = newItems)
            _tickets.value = currentTickets
        }
        onSelectItem(null) 
    }

    fun addProductToCart(product: Product) {
        val currentTickets = _tickets.value.toMutableList()
        val activeTicketIndex = _activeTicketIndex.value
        val activeTicket = currentTickets[activeTicketIndex]
        val newItems = activeTicket.items.toMutableList()
        val existingItem = newItems.find { it.product.code == product.code && it.product.unit == ProductUnit.UNIDAD }
        if (existingItem != null) {
            newItems[newItems.indexOf(existingItem)] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            newItems.add(CartItem(product = product))
        }
        currentTickets[activeTicketIndex] = activeTicket.copy(items = newItems)
        _tickets.value = currentTickets
        onSearchQueryChange("") 
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query; _activeKeyboard.value = KeyboardType.QWERTY }
    fun onSelectItem(item: CartItem?) { _selectedCartItem.value = item; _keypadInput.value = ""; _activeKeyboard.value = if (item != null) KeyboardType.NUMERIC else KeyboardType.QWERTY }
    fun addTicket() { val newTickets = _tickets.value.toMutableList(); newTickets.add(TicketState(id = nextTicketId++)); _tickets.value = newTickets; _activeTicketIndex.value = newTickets.size - 1 }
    fun setActiveTicket(index: Int) { if (index >= 0 && index < _tickets.value.size) { _activeTicketIndex.value = index; onSelectItem(null) } }
    fun closeTicket(index: Int) {
        val currentTickets = _tickets.value.toMutableList()
        if (index < 0 || index >= currentTickets.size) return
        if (_selectedCartItem.value in currentTickets[index].items) onSelectItem(null)
        currentTickets.removeAt(index)
        if (currentTickets.isEmpty()) { currentTickets.add(TicketState(id = nextTicketId++)); _activeTicketIndex.value = 0 } 
        else if (_activeTicketIndex.value >= index) { _activeTicketIndex.value = (_activeTicketIndex.value - 1).coerceAtLeast(0) }
        _tickets.value = currentTickets
    }

    fun removeItemFromCart(item: CartItem) {
        if (_selectedCartItem.value == item) onSelectItem(null)
        val currentTickets = _tickets.value.toMutableList()
        val activeIndex = _activeTicketIndex.value
        val activeTicket = currentTickets[activeIndex]
        val newItems = activeTicket.items.toMutableList()
        newItems.remove(item)
        currentTickets[activeIndex] = activeTicket.copy(items = newItems)
        _tickets.value = currentTickets
    }

    fun incrementCartItemQuantity(item: CartItem) {
        if (item.product.unit != ProductUnit.UNIDAD) return
        val currentTickets = _tickets.value.toMutableList()
        val activeIndex = _activeTicketIndex.value
        val activeTicket = currentTickets[activeIndex]
        val newItems = activeTicket.items.toMutableList()
        val itemIndex = newItems.indexOf(item)
        if (itemIndex != -1) {
            newItems[itemIndex] = item.copy(quantity = item.quantity + 1)
            currentTickets[activeIndex] = activeTicket.copy(items = newItems)
            _tickets.value = currentTickets
        }
    }

    fun decrementCartItemQuantity(item: CartItem) {
        if (item.product.unit != ProductUnit.UNIDAD) return
        if (item.quantity > 1) {
            val currentTickets = _tickets.value.toMutableList()
            val activeIndex = _activeTicketIndex.value
            val activeTicket = currentTickets[activeIndex]
            val newItems = activeTicket.items.toMutableList()
            val itemIndex = newItems.indexOf(item)
            if (itemIndex != -1) {
                newItems[itemIndex] = item.copy(quantity = item.quantity - 1)
                currentTickets[activeIndex] = activeTicket.copy(items = newItems)
                _tickets.value = currentTickets
            }
        } else {
            removeItemFromCart(item)
        }
    }

    fun onFinalizeSaleClick() {
        viewModelScope.launch {
            if (uiState.value.activeTicket.items.isEmpty()) return@launch
            val printerAddress = userPreferencesRepository.printerMacAddress.firstOrNull()
            if (printerAddress == null) _showNoPrinterDialog.value = true else _showConfirmSaleDialog.value = true
        }
    }

    fun onDismissFinalizeSaleDialog() { _showConfirmSaleDialog.value = false }
    fun onDismissNoPrinterDialog() { _showNoPrinterDialog.value = false }
    fun onPrintResultConsumed() { _printResult.value = null }
    fun onSettingsClick() { refreshPairedDevices(); _showSettingsDialog.value = true }
    fun onDismissSettingsDialog() { _showSettingsDialog.value = false }
    
    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        _pairedDevices.value = printerHelper.getPairedDevices()?.map { it.name to it.address } ?: emptyList()
    }

    fun selectPrinter(macAddress: String) { viewModelScope.launch { userPreferencesRepository.savePrinterMacAddress(macAddress) } }
    
    fun reprintLastTicket() {
        if (_isPrinting.value) return
        viewModelScope.launch {
            _isPrinting.value = true
            try {
                val lastTicket = ticketRepository.getLastTicket()
                if (lastTicket != null) {
                    val lastTicketWithItems = ticketRepository.getTicketWithItems(lastTicket.id).first()
                    if (lastTicketWithItems != null) {
                        val cartItems = lastTicketWithItems.items.map { it.toCartItem() }
                        withContext(Dispatchers.IO) {
                            val result = printerHelper.printTicket(lastTicketWithItems.ticket, cartItems, lastTicketWithItems.ticket.dailyFolio.toString().padStart(3, '0'))
                            _printResult.value = result
                        }
                    }
                }
            } finally {
                _isPrinting.value = false
            }
        }
    }
}
