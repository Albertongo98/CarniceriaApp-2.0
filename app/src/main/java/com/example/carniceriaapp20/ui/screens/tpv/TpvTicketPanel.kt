package com.example.carniceriaapp20.ui.screens.tpv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.util.formatMoney2
import java.util.Locale

/** Ticket en curso: pestañas de tickets abiertos, renglones y pie con total + reimprimir + FINALIZAR. */
@Composable
fun TicketManagementPanel(
    modifier: Modifier = Modifier,
    uiState: TpvUiState,
    isTablet: Boolean,
    onSelectItem: (CartItem?) -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    onIncrementItem: (CartItem) -> Unit,
    onDecrementItem: (CartItem) -> Unit,
    onAddTicket: () -> Unit,
    onCloseTicket: (Int) -> Unit,
    onSetActiveTicket: (Int) -> Unit,
    onFinalizeSale: () -> Unit,
    onReprintLast: () -> Unit
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier) {
        TicketTabs(uiState, isTablet, onSetActiveTicket, onCloseTicket, onAddTicket)

        Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            if (uiState.activeTicket.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ticket Vacío", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(uiState.activeTicket.items, key = { it.id }) { item ->
                        CartItemRow(
                            item = item,
                            isSelected = uiState.selectedCartItem == item,
                            isTablet = isTablet,
                            onClick = { if (uiState.selectedCartItem == item) onSelectItem(null) else onSelectItem(item) },
                            onRemove = { onRemoveItem(item) },
                            onIncrement = { onIncrementItem(item) },
                            onDecrement = { onDecrementItem(item) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }

                // Indica que hay más renglones abajo.
                if (uiState.activeTicket.items.size > 4) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.BottomCenter).size(20.dp).alpha(0.4f),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        TicketFooter(uiState, isTablet, onReprintLast, onFinalizeSale)
    }
}

@Composable
private fun TicketTabs(
    uiState: TpvUiState,
    isTablet: Boolean,
    onSetActiveTicket: (Int) -> Unit,
    onCloseTicket: (Int) -> Unit,
    onAddTicket: () -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = uiState.activeTicketIndex,
        containerColor = Color.Transparent,
        divider = {},
        edgePadding = 8.dp
    ) {
        uiState.tickets.forEachIndexed { index, _ ->
            val selected = uiState.activeTicketIndex == index
            Tab(
                selected = selected,
                onClick = { onSetActiveTicket(index) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            "T${index + 1}",
                            style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (uiState.tickets.size > 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                            // Botón grande a propósito: uno chico cerraba tickets por accidente.
                            IconButton(
                                onClick = { onCloseTicket(index) },
                                modifier = Modifier.size(if (isTablet) 32.dp else 28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cerrar ticket ${index + 1}",
                                    modifier = Modifier.size(if (isTablet) 16.dp else 14.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
        Tab(
            selected = false,
            onClick = onAddTicket,
            text = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(if (isTablet) 18.dp else 14.dp)) }
        )
    }
}

@Composable
private fun TicketFooter(uiState: TpvUiState, isTablet: Boolean, onReprintLast: () -> Unit, onFinalizeSale: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = if (isTablet) 16.dp else 12.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${uiState.activeTicket.items.size} ITEMS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "$${formatMoney2(uiState.activeTicket.total)}",
                    style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReprintLast,
                    enabled = !uiState.isPrinting,
                    modifier = Modifier.weight(0.3f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Button(
                    onClick = onFinalizeSale,
                    enabled = !uiState.isPrinting && uiState.activeTicket.items.isNotEmpty(),
                    modifier = Modifier.weight(0.7f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    if (uiState.isPrinting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("FINALIZAR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    isSelected: Boolean,
    isTablet: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val background = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
    val isGranel = item.product.unit == ProductUnit.GRANEL

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(background)
            .padding(vertical = if (isTablet) 8.dp else 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.product.name,
                style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val unitLabel = if (isGranel) "kg" else "pz"
                val quantityText = if (isGranel) "${"%.3f".format(Locale.forLanguageTag("es-MX"), item.quantity)} kg" else "${item.quantity.toInt()} pz"
                Text(quantityText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Piezas estimadas (notas de despacho) si existen.
                if (item.estimatedPieces != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            " ${item.estimatedPieces} PZ ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))
                Text("|", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "$${item.product.price}/${unitLabel.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (item.product.unit == ProductUnit.UNIDAD) {
            QuantityStepper(item.quantity, isTablet, onDecrement, onIncrement)
        }

        Text(
            "$${formatMoney2(item.totalPrice)}",
            style = if (isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.ExtraBold
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(if (isTablet) 32.dp else 28.dp)) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun QuantityStepper(quantity: Double, isTablet: Boolean, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    val buttonSize = if (isTablet) 28.dp else 24.dp
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDecrement, modifier = Modifier.size(buttonSize)) {
            Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        }
        Text(
            quantity.toInt().toString(),
            style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onIncrement, modifier = Modifier.size(buttonSize)) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        }
    }
}
