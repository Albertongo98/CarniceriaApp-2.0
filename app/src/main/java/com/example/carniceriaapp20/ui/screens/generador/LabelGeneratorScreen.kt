package com.example.carniceriaapp20.ui.screens.generador

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.carniceriaapp20.data.local.LabelHistory
import com.example.carniceriaapp20.util.PrintResult

@Composable
fun LabelGeneratorScreen(
    viewModel: LabelGeneratorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.printResult) {
        when (val result = uiState.printResult) {
            is PrintResult.Success -> {
                snackbarHostState.showSnackbar("Impresión enviada con éxito")
                viewModel.onPrintResultConsumed()
            }
            is PrintResult.Error -> {
                snackbarHostState.showSnackbar("Error de impresión: ${result.message}")
                viewModel.onPrintResultConsumed()
            }
            null -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.toggleFormVisibility() }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Etiqueta")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            AnimatedVisibility(visible = uiState.isFormVisible) {
                NewLabelForm(uiState = uiState, viewModel = viewModel)
            }

            Text("Historial de Etiquetas", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.history) { label ->
                    LabelHistoryItem(label = label, viewModel = viewModel)
                    Divider()
                }
            }
        }
    }
}

@Composable
fun NewLabelForm(
    uiState: LabelGeneratorUiState,
    viewModel: LabelGeneratorViewModel
) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Crear Nueva Etiqueta", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.formName,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nombre del Producto") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.formPrice,
                onValueChange = viewModel::onPriceChange,
                label = { Text("Precio (Ej: 12.50)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.formCode,
                onValueChange = viewModel::onCodeChange,
                label = { Text("Código de Barras") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = { viewModel.printNewLabel() },
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
            ) {
                Text("Imprimir")
            }
        }
    }
}

@Composable
fun LabelHistoryItem(
    label: LabelHistory,
    viewModel: LabelGeneratorViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.reprintLabel(label) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label.name, style = MaterialTheme.typography.bodyLarge)
            Text("Código: ${label.code} | Precio: ${label.price}", style = MaterialTheme.typography.bodySmall)
        }
        Row {
            IconButton(onClick = { viewModel.reprintLabel(label) }) {
                Icon(Icons.Default.Print, contentDescription = "Reimprimir")
            }
            IconButton(onClick = { viewModel.deleteLabelFromHistory(label) }) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}
