package com.example.carniceriaapp20.ui.screens.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.carniceriaapp20.util.PrintResult
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPreviewDialog by remember { mutableStateOf<TicketWithItems?>(null) }

    LaunchedEffect(key1 = uiState.printResult) {
        uiState.printResult?.let {
            val message = when (it) {
                is PrintResult.Success -> "Impresión enviada correctamente"
                is PrintResult.Error -> "Error de impresión: ${it.message}"
            }
            snackbarHostState.showSnackbar(message)
            viewModel.onPrintResultConsumed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = { Text("${uiState.selectedTicketIds.size} seleccionados") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Cancelar Selección")
                        }
                    }
                )
            } else {
                TopAppBar(title = { Text("Historial de Ventas") })
            }
        },
        floatingActionButton = {
            if (uiState.isSelectionMode) {
                FloatingActionButton(onClick = { viewModel.printSelectedTicketsForAudit() }) {
                    Icon(Icons.Default.Print, contentDescription = "Imprimir Selección")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 8.dp)) {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(uiState.tickets, key = { it.ticket.id }) { ticketWithItems ->
                    val isSelected = uiState.selectedTicketIds.contains(ticketWithItems.ticket.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .combinedClickable(
                                onClick = { 
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleTicketSelection(ticketWithItems.ticket.id)
                                    } else {
                                        showPreviewDialog = ticketWithItems
                                    }
                                },
                                onLongClick = { viewModel.toggleTicketSelection(ticketWithItems.ticket.id) }
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
                        colors = if (isSelected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isSelectionMode) {
                                Checkbox(checked = isSelected, onCheckedChange = { _ -> viewModel.toggleTicketSelection(ticketWithItems.ticket.id) })
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            Column {
                                val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
                                Text("Folio: ${ticketWithItems.ticket.id}", fontWeight = FontWeight.Bold)
                                Text("Fecha: ${dateFormat.format(Date(ticketWithItems.ticket.timestamp))}")
                                Text("Total: $" + "%.2f".format(ticketWithItems.ticket.totalAmount))
                            }
                        }
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
                viewModel.toggleTicketSelection(showPreviewDialog!!.ticket.id)
                viewModel.printSelectedTicketsForAudit()
                showPreviewDialog = null
            }
        )
    }
}

@Composable
fun TicketPreviewDialog(
    ticketWithItems: TicketWithItems,
    onDismiss: () -> Unit,
    onPrint: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vista Previa", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Representación visual de un ticket físico
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    color = Color(0xFFFFFDE7), // Color papel hueso
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "LA PALMA CARNICERIA",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "--------------------------------",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                            items(ticketWithItems.items) { item ->
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${item.quantity} x $${item.unitPrice}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        Text("$${item.totalPrice}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                        
                        Text(
                            "--------------------------------",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "TOTAL: $" + "%.2f".format(ticketWithItems.ticket.totalAmount),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Folio: ${ticketWithItems.ticket.id}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onPrint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reimprimir Ticket")
                }
            }
        }
    }
}
