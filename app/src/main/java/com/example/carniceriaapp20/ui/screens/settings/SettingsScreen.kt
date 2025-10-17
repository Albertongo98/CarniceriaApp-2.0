package com.example.carniceriaapp20.ui.screens.settings

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberMultiplePermissionsState(
            permissions = listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        )
    } else {
        rememberMultiplePermissionsState(permissions = emptyList())
    }

    LaunchedEffect(key1 = bluetoothPermissions.allPermissionsGranted) {
        if (bluetoothPermissions.allPermissionsGranted) {
            viewModel.refreshPairedDevices()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                if (!bluetoothPermissions.allPermissionsGranted) {
                    bluetoothPermissions.launchMultiplePermissionRequest()
                } else {
                    viewModel.refreshPairedDevices() 
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refrescar dispositivos")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Text("Configuración de Impresora", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (!bluetoothPermissions.allPermissionsGranted) {
                Text("Se necesitan permisos de Bluetooth para buscar impresoras.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { bluetoothPermissions.launchMultiplePermissionRequest() }) {
                    Text("Otorgar Permisos")
                }
            } else {
                if (uiState.pairedDevices.isEmpty()) {
                    Text("No se encontraron impresoras vinculadas. Asegúrese de que la impresora esté encendida y vinculada al dispositivo.")
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(uiState.pairedDevices) { device ->
                            val isSelected = uiState.selectedPrinterMac == device.second
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectPrinter(device.second) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(device.first ?: "Dispositivo desconocido", modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }
}
