package com.example.carniceriaapp20.ui.screens.tpv

import androidx.compose.material.icons.automirrored.outlined.FactCheck
import com.example.carniceriaapp20.util.formatMoney2
import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carniceriaapp20.data.local.ProductUnit
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.util.Locale

@Composable
fun ConfirmSaleDialog(ticket: TicketState, isPrinting: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val scrollState = rememberLazyListState()
    val infiniteTransition = rememberInfiniteTransition(label = "scrollArrow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    AlertDialog(
        onDismissRequest = if (isPrinting) ({}) else onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Outlined.FactCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Confirmar Venta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Validar productos con el cliente:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(modifier = Modifier.heightIn(max = 240.dp).fillMaxWidth()) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(ticket.items) {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val qtyText = if(it.product.unit == ProductUnit.GRANEL) "%.3f kg".format(Locale.forLanguageTag("es-MX"), it.quantity) else "${it.quantity.toInt()} pz"
                                Text(it.product.name.uppercase(), fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                                
                                // MOSTRAR PIEZAS ESTIMADAS EN RESUMEN
                                val detailText = if (it.estimatedPieces != null) "$qtyText | ${it.estimatedPieces} pz | $" + formatMoney2(it.totalPrice) else "$qtyText | $" + formatMoney2(it.totalPrice)
                                Text(detailText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    if (ticket.items.size > 4) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardDoubleArrowDown,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.BottomCenter).offset(y = 10.dp).size(24.dp).alpha(alpha),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL DE LA VENTA (${ticket.items.size} PRODUCTOS)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = "$" + formatMoney2(ticket.total),
                            style = MaterialTheme.typography.displayMedium,
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isPrinting,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                if (isPrinting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("GENERAR TICKET", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isPrinting, modifier = Modifier.fillMaxWidth()) {
                Text("CORREGIR / REGRESAR", textAlign = TextAlign.Center)
            }
        }
    )
}

@Composable
fun NoPrinterDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Impresora no configurada") },
        text = { Text("Para poder imprimir tickets, primero debe seleccionar una impresora en la pantalla de ajustes.") },
        confirmButton = { Button(onClick = onGoToSettings) { Text("Ir a Ajustes") } },
        dismissButton = { TextButton(onClick = { onDismiss() }) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun SettingsDialog(
    uiState: TpvUiState,
    onDismiss: () -> Unit,
    onSelectPrinter: (String) -> Unit,
    onRefreshDevices: () -> Unit
) {
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberMultiplePermissionsState(
            permissions = listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        )
    } else {
        rememberMultiplePermissionsState(permissions = emptyList())
    }

    LaunchedEffect(key1 = bluetoothPermissions.allPermissionsGranted) {
        if (bluetoothPermissions.allPermissionsGranted) onRefreshDevices()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Impresora") },
        text = {
            Column {
                when {
                    !bluetoothPermissions.allPermissionsGranted -> {
                        Text("Se necesitan permisos de Bluetooth para buscar impresoras.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { bluetoothPermissions.launchMultiplePermissionRequest() }) { Text("Otorgar Permisos") }
                    }
                    uiState.pairedDevices.isEmpty() -> Text("No se encontraron impresoras vinculadas.")
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(uiState.pairedDevices) { device ->
                            PrinterRow(
                                name = device.first,
                                selected = uiState.selectedPrinterMac == device.second,
                                onClick = { onSelectPrinter(device.second) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDismiss() }) { Text("CERRAR") } },
        dismissButton = {
            IconButton(onClick = {
                if (!bluetoothPermissions.allPermissionsGranted) {
                    bluetoothPermissions.launchMultiplePermissionRequest()
                } else {
                    onRefreshDevices()
                }
            }) { Icon(Icons.Default.Refresh, contentDescription = "Refrescar") }
        }
    )
}

@Composable
private fun PrinterRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, modifier = Modifier.weight(1f), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
