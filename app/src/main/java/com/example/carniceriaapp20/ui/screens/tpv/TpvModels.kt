package com.example.carniceriaapp20.ui.screens.tpv

import com.example.carniceriaapp20.data.local.Product
import java.util.UUID

data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val product: Product,
    val quantity: Double = 1.0,
    val customPrice: Double? = null,
    val estimatedPieces: Int? = null // NUEVO: Piezas pedidas (ej: 3 chiles)
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

enum class KeyboardType {
    QWERTY, NUMERIC
}

data class TpvUiState(
    val filteredProducts: Map<String, List<Product>> = emptyMap(),
    val searchQuery: String = "",
    val tickets: List<TicketState> = listOf(TicketState(id = 1)),
    val activeTicketIndex: Int = 0,
    val selectedCartItem: CartItem? = null,
    val keypadInput: String = "",
    val activeKeyboard: KeyboardType = KeyboardType.NUMERIC,
    val isPrinting: Boolean = false,
    val printResult: com.example.carniceriaapp20.util.PrintResult? = null,
    val showConfirmSaleDialog: Boolean = false,
    val showNoPrinterDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val pairedDevices: List<Pair<String, String>> = emptyList(),
    val selectedPrinterMac: String? = null,
    val fastProducts: List<Product> = emptyList()
) {
    val activeTicket: TicketState
        get() = tickets[activeTicketIndex]
}
