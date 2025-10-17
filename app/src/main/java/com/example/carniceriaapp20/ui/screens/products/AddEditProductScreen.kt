package com.example.carniceriaapp20.ui.screens.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.carniceriaapp20.data.local.ProductUnit

@Composable
fun AddEditProductScreen(
    onSave: () -> Unit,
    viewModel: AddEditProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = if (uiState.isEditing) "Editar Producto" else "Añadir Producto",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text("Código") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isEditing
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.price,
                onValueChange = viewModel::onPriceChange,
                label = { Text("Precio") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.department,
                onValueChange = viewModel::onDepartmentChange,
                label = { Text("Departamento") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Unidad de Venta", style = MaterialTheme.typography.bodyLarge)
            Row {
                ProductUnit.values().forEach { unit ->
                    Row(
                        Modifier
                            .selectable(
                                selected = (unit == uiState.unit),
                                onClick = { viewModel.onUnitChange(unit) }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (unit == uiState.unit),
                            onClick = { viewModel.onUnitChange(unit) }
                        )
                        Text(
                            text = unit.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.saveProduct()
                    onSave()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar")
            }
        }
    }
}
