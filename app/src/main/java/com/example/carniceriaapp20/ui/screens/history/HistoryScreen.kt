package com.example.carniceriaapp20.ui.screens.history

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.carniceriaapp20.util.formatMoney2
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.carniceriaapp20.data.local.TicketWithItems
import com.example.carniceriaapp20.data.local.isVoided
import com.example.carniceriaapp20.util.PrintResult
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPreviewDialog by remember { mutableStateOf<TicketWithItems?>(null) }
    var voidTarget by remember { mutableStateOf<TicketWithItems?>(null) }

    LaunchedEffect(key1 = uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageConsumed()
        }
    }

    LaunchedEffect(key1 = uiState.printResult) {
        uiState.printResult?.let { result ->
            val message = when (result) {
                is PrintResult.Success -> "Impresión enviada correctamente"
                is PrintResult.Error -> "Error de impresión: ${result.message}"
            }
            snackbarHostState.showSnackbar(message)
            viewModel.onPrintResultConsumed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (uiState.isSelectionMode) "${uiState.selectedTicketIds.size} seleccionados" else "Historial de Ventas",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { if (uiState.isSelectionMode) viewModel.clearSelection() else onNavigateBack() }) {
                        Icon(if (uiState.isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.printSelectedTicketsForAudit() }, enabled = !uiState.isPrinting) {
                            if (uiState.isPrinting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp), 
                                    color = MaterialTheme.colorScheme.onPrimary, 
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Print, contentDescription = "Imprimir")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (uiState.isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.printSelectedTicketsForAudit() },
                    expanded = !uiState.isPrinting,
                    containerColor = Color(0xFF1B5E20),
                    contentColor = Color.White,
                    icon = { 
                        if (uiState.isPrinting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null) 
                        }
                    },
                    text = { Text("IMPRIMIR SELECCIÓN") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.tickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay ventas registradas", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.tickets, key = { it.ticket.id }) { ticketWithItems ->
                        TicketListItem(
                            ticketWithItems = ticketWithItems,
                            isSelected = uiState.selectedTicketIds.contains(ticketWithItems.ticket.id),
                            isSelectionMode = uiState.isSelectionMode,
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleTicketSelection(ticketWithItems.ticket.id)
                                } else {
                                    showPreviewDialog = ticketWithItems
                                }
                            },
                            onLongClick = { viewModel.toggleTicketSelection(ticketWithItems.ticket.id) }
                        )
                    }
                }
            }
        }
    }

    if (showPreviewDialog != null) {
        TicketPreviewDialog(
            ticketWithItems = showPreviewDialog!!,
            onDismiss = { showPreviewDialog = null },
            onPrint = {
                val id = showPreviewDialog!!.ticket.id
                viewModel.toggleTicketSelection(id)
                viewModel.printSelectedTicketsForAudit()
                showPreviewDialog = null
            },
            onVoid = {
                val target = showPreviewDialog!!
                showPreviewDialog = null
                voidTarget = target
            }
        )
    }

    voidTarget?.let { target ->
        VoidReasonDialog(
            folio = target.ticket.dailyFolio.toString().padStart(3, '0'),
            onConfirm = { reason ->
                viewModel.voidTicket(target.ticket.id, reason)
                voidTarget = null
            },
            onDismiss = { voidTarget = null }
        )
    }
}

@Composable
fun VoidReasonDialog(folio: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Anular ticket $folio", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "El ticket no se borra: queda marcado como ANULADO, sale de los reportes y lo vendido regresa a existencias.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(60) },
                    label = { Text("Motivo (obligatorio)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("ANULAR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TicketListItem(
    ticketWithItems: TicketWithItems,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ticketWithItems.ticket.isVoided -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF1B5E20),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "FOLIO ${ticketWithItems.ticket.dailyFolio.toString().padStart(3, '0')}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = dateFormat.format(Date(ticketWithItems.ticket.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (ticketWithItems.ticket.isVoided) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "ANULADO",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${ticketWithItems.items.size} productos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "$" + formatMoney2(ticketWithItems.ticket.totalAmount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textDecoration = if (ticketWithItems.ticket.isVoided) TextDecoration.LineThrough else null,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TicketPreviewDialog(
    ticketWithItems: TicketWithItems,
    onDismiss: () -> Unit,
    onPrint: () -> Unit,
    onVoid: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Detalles de Venta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    color = Color(0xFFF9F9F9),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (ticketWithItems.ticket.isVoided) {
                            Text(
                                "*** ANULADO ***" + (ticketWithItems.ticket.voidReason?.let { "\nMotivo: $it" } ?: ""),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            "LA PALMA CARNICERIA",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                        Text(
                            "--------------------------------",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                        
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(ticketWithItems.items) { item ->
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(item.productName.uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${item.quantity} x $${item.unitPrice}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        Text("$${formatMoney2(item.totalPrice)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                        
                        Text(
                            "--------------------------------",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL:", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("$${formatMoney2(ticketWithItems.ticket.totalAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF1B5E20))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Folio Diario: ${ticketWithItems.ticket.dailyFolio.toString().padStart(3, '0')}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onPrint,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("REIMPRIMIR TICKET", fontWeight = FontWeight.Bold)
                }

                if (!ticketWithItems.ticket.isVoided) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onVoid,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("ANULAR TICKET", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
