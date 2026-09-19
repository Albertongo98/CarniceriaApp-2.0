package com.example.carniceriaapp20.ui.screens.products

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.carniceriaapp20.data.local.ProductUnit
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    onSave: () -> Unit,
    viewModel: AddEditProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditProductViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message
                    )
                }
                is AddEditProductViewModel.UiEvent.SaveSuccess -> {
                    onSave()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
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
                enabled = !uiState.isEditing,
                isError = uiState.code.isBlank()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.name.isBlank()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.price,
                onValueChange = viewModel::onPriceChange,
                label = { Text("Precio") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.price.toDoubleOrNull() == null || uiState.price.toDouble() <= 0.0
            )
            Spacer(modifier = Modifier.height(8.dp))

            var departmentExpanded by remember { mutableStateOf(false) }
            val filteredDepartments = remember(uiState.department, departments) {
                if (uiState.department.isBlank()) departments
                else departments.filter { it.contains(uiState.department, ignoreCase = true) }
            }
            ExposedDropdownMenuBox(
                expanded = departmentExpanded && filteredDepartments.isNotEmpty(),
                onExpandedChange = { departmentExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.department,
                    onValueChange = {
                        viewModel.onDepartmentChange(it)
                        departmentExpanded = true
                    },
                    label = { Text("Departamento") },
                    placeholder = { Text("Selecciona uno existente o escribe uno nuevo") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                    isError = uiState.department.isBlank()
                )
                ExposedDropdownMenu(
                    expanded = departmentExpanded && filteredDepartments.isNotEmpty(),
                    onDismissRequest = { departmentExpanded = false }
                ) {
                    filteredDepartments.forEach { dept ->
                        DropdownMenuItem(
                            text = { Text(dept) },
                            onClick = {
                                viewModel.onDepartmentChange(dept)
                                departmentExpanded = false
                            }
                        )
                    }
                }
            }
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
                onClick = viewModel::saveProduct,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar")
            }
        }
    }
}
