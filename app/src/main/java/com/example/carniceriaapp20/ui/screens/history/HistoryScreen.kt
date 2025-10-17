package com.example.carniceriaapp20.ui.screens.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(key1 = uiState.printResult) {
        uiState.printResult?.let {
            val message = when (it) {
                is PrintResult.Success -> "Impresión de auditoría enviada"
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
    ) {
        Column(modifier = Modifier.padding(it).padding(horizontal = 8.dp)) {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(uiState.tickets, key = { it.ticket.id }) {
                    val isSelected = uiState.selectedTicketIds.contains(it.ticket.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .combinedClickable(
                                onClick = { 
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleTicketSelection(it.ticket.id)
                                    } 
                                },
                                onLongClick = { viewModel.toggleTicketSelection(it.ticket.id) }
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
                        colors = if (isSelected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isSelectionMode) {
                                Checkbox(checked = isSelected, onCheckedChange = { _ -> viewModel.toggleTicketSelection(it.ticket.id) })
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            Column {
                                val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
                                Text("Folio: ${it.ticket.id}", fontWeight = FontWeight.Bold)
                                Text("Fecha: ${dateFormat.format(Date(it.ticket.timestamp))}")
                                Text("Total: $" + "%.2f".format(it.ticket.totalAmount))
                            }
                        }
                    }
                }
            }
        }
    }
}
