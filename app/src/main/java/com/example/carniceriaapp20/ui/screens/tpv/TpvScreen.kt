package com.example.carniceriaapp20.ui.screens.tpv

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.carniceriaapp20.R
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.ui.navigation.Routes
import com.example.carniceriaapp20.util.PrintResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TpvScreen(
    navController: NavController,
    viewModel: TpvViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }

    val bluetoothPrintPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberMultiplePermissionsState(
            permissions = listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        )
    } else {
        rememberMultiplePermissionsState(permissions = emptyList())
    }

    LaunchedEffect(key1 = uiState.printResult) {
        uiState.printResult?.let {
            val message = when (it) {
                is PrintResult.Success -> "Ticket impreso correctamente"
                is PrintResult.Error -> "Error de impresión: ${it.message}"
            }
            snackbarHostState.showSnackbar(message)
            viewModel.onPrintResultConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carniceriapp") },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Gestionar Productos") },
                                onClick = {
                                    navController.navigate(Routes.PRODUCT_LIST)
                                    menuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Historial de Tickets") },
                                onClick = { 
                                    navController.navigate(Routes.HISTORY)
                                    menuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Generador de Etiquetas") },
                                onClick = { 
                                    navController.navigate(Routes.LABEL_GENERATOR)
                                    menuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Actualizar Base de Datos") },
                                onClick = { 
                                    navController.navigate(Routes.UPDATE_FROM_CSV)
                                    menuExpanded = false 
                                },
                                leadingIcon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) }
                            )
                             DropdownMenuItem(
                                text = { Text("Configurar Impresora") },
                                onClick = { 
                                    viewModel.onSettingsClick()
                                    menuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).padding(4.dp)) {
                ProductCatalogPanel(
                    modifier = Modifier.weight(0.45f).fillMaxHeight(),
                    uiState = uiState,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onProductClick = viewModel::addProductToCart
                )

                Divider(modifier = Modifier.fillMaxHeight().width(1.dp).padding(vertical = 8.dp))

                TicketManagementPanel(
                    modifier = Modifier.weight(0.55f).fillMaxHeight(),
                    uiState = uiState,
                    onSelectItem = viewModel::onSelectItem,
                    onRemoveItem = viewModel::removeItemFromCart,
                    onIncrementItem = viewModel::incrementCartItemQuantity,
                    onDecrementItem = viewModel::decrementCartItemQuantity,
                    onAddTicket = viewModel::addTicket,
                    onCloseTicket = viewModel::closeTicket,
                    onSetActiveTicket = viewModel::setActiveTicket,
                    onFinalizeSale = {
                        if (!bluetoothPrintPermissions.allPermissionsGranted) {
                            bluetoothPrintPermissions.launchMultiplePermissionRequest()
                        } else {
                            viewModel.onFinalizeSaleClick()
                        }
                    },
                    onReprintLast = viewModel::reprintLastTicket
                )
            }

            Divider()
            
            KeypadWithActionsPanel(
                modifier = Modifier.fillMaxWidth(),
                selectedItem = uiState.selectedCartItem,
                keypadInput = uiState.keypadInput,
                onKeypadInput = viewModel::onKeypadInput,
                onClear = viewModel::onKeypadClear,
                onBackspace = viewModel::onKeypadBackspace,
                onApply = viewModel::onApplyKeypadInput
            )
        }
    }

    if (uiState.showConfirmSaleDialog) {
        ConfirmSaleDialog(
            ticket = uiState.activeTicket,
            onDismiss = viewModel::onDismissFinalizeSaleDialog,
            onConfirm = viewModel::confirmSale
        )
    }

    if (uiState.showNoPrinterDialog) {
        NoPrinterDialog(
            onDismiss = viewModel::onDismissNoPrinterDialog,
            onGoToSettings = { 
                viewModel.onDismissNoPrinterDialog()
                viewModel.onSettingsClick()
            }
        )
    }

    if (uiState.showSettingsDialog) {
        SettingsDialog(
            uiState = uiState,
            onDismiss = viewModel::onDismissSettingsDialog,
            onSelectPrinter = viewModel::selectPrinter,
            onRefreshDevices = viewModel::refreshPairedDevices
        )
    }
}

@Composable
fun ProductCatalogPanel(
    modifier: Modifier = Modifier,
    uiState: TpvUiState,
    onSearchQueryChange: (String) -> Unit,
    onProductClick: (Product) -> Unit
) {
    var expandedState by rememberSaveable { mutableStateOf(mapOf<String, Boolean>()) }

    val isSearchActive = uiState.searchQuery.isNotBlank()

    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        Image(
            painter = painterResource(id = R.drawable.logo_color),
            contentDescription = "Logo de La Palma",
            modifier = Modifier.fillMaxWidth().height(80.dp).padding(vertical = 8.dp),
            contentScale = ContentScale.Fit
        )
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Buscar producto...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            uiState.filteredProducts.forEach { (department, products) ->
                item {
                    val isExpanded = expandedState.getOrDefault(department, false)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            if (!isSearchActive) {
                                expandedState = expandedState + (department to !isExpanded) 
                            }
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSearchActive || isExpanded) Icons.Default.ArrowDropDown else Icons.Default.KeyboardArrowRight, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(text = department, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                if (isSearchActive || expandedState.getOrDefault(department, false)) {
                    items(products) { product ->
                        Card(
                            onClick = { onProductClick(product) }, 
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 16.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(product.name, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketManagementPanel(
    modifier: Modifier = Modifier,
    uiState: TpvUiState,
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
    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        ScrollableTabRow(selectedTabIndex = uiState.activeTicketIndex) {
            uiState.tickets.forEachIndexed { index, ticket ->
                Tab(
                    selected = uiState.activeTicketIndex == index,
                    onClick = { onSetActiveTicket(index) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Ticket ${ticket.id}")
                            if (uiState.tickets.size > 1) {
                                IconButton(onClick = { onCloseTicket(index) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar ticket", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                )
            }
            Tab(selected = false, onClick = onAddTicket, text = { Icon(Icons.Default.Add, contentDescription = "Añadir ticket") })
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            items(uiState.activeTicket.items, key = { it.product.code + it.quantity + it.customPrice }) { item ->
                CartItemRow(
                    item = item, 
                    isSelected = uiState.selectedCartItem == item,
                    onClick = { 
                        if (uiState.selectedCartItem == item) onSelectItem(null) else onSelectItem(item)
                    },
                    onRemove = { onRemoveItem(item) },
                    onIncrement = { onIncrementItem(item) },
                    onDecrement = { onDecrementItem(item) }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        Text(
            text = "Total: ${ String.format("%.2f", uiState.activeTicket.total)}",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.End).padding(bottom = 8.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onReprintLast, modifier = Modifier.weight(1f)) {
                Text("Reimprimir")
            }
            Button(onClick = onFinalizeSale, modifier = Modifier.weight(1f)) {
                Text("Finalizar Venta")
            }
        }
    }
}

@Composable
fun KeypadWithActionsPanel(
    modifier: Modifier = Modifier,
    selectedItem: CartItem?,
    keypadInput: String,
    onKeypadInput: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onApply: (isPrice: Boolean) -> Unit
) {
    Column(modifier = modifier.padding(8.dp)) {
        OutlinedTextField(
            value = keypadInput,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (selectedItem != null) "Editando: ${selectedItem.product.name}" else "Seleccione un producto para editar") },
            textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = onBackspace) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Borrar")
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(0.6f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                userScrollEnabled = false
            ) {
                items(listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", ".", "0")) {
                    Button(modifier = Modifier.aspectRatio(1.8f), onClick = { onKeypadInput(it) }) {
                        Text(it, fontSize = 18.sp)
                    }
                }
            }
            Column(
                modifier = Modifier.weight(0.4f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { onApply(false) }, // Apply Quantity
                    enabled = selectedItem != null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2.5f)
                ) {
                    Text("KG/PZ")
                }
                Button(
                    onClick = { onApply(true) }, // Apply Price
                    enabled = selectedItem?.product?.unit == ProductUnit.GRANEL,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2.5f)
                ) {
                    Text("$")
                }
                 OutlinedButton(
                    modifier = Modifier.fillMaxWidth().aspectRatio(2.5f), 
                    onClick = onClear
                 ) {
                    Text("Limpiar")
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem, 
    isSelected: Boolean, 
    onClick: () -> Unit, 
    onRemove: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, fontWeight = FontWeight.Bold)
            val desc = if (item.product.unit == ProductUnit.GRANEL) {
                if (item.customPrice != null) "Precio Manual" else "${item.quantity} kg"
            } else {
                "${item.quantity.toInt()} x $${item.product.price}"
            }
            Text(desc)
        }
        if (item.product.unit == ProductUnit.UNIDAD) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.RemoveCircle, contentDescription = "Quitar 1", tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onIncrement, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.AddCircle, contentDescription = "Añadir 1", tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text("$" + "%.2f".format(item.totalPrice), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar producto", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun ConfirmSaleDialog(ticket: TicketState, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Venta") },
        text = {
            Column {
                Text("Resumen del Ticket:", fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                    items(ticket.items) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${it.quantity}x ${it.product.name}")
                            Text("$" + "%.2f".format(it.totalPrice))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Total: $" + "%.2f".format(ticket.total), style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.End))
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Imprimir y Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun NoPrinterDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Impresora no configurada") },
        text = { Text("Para poder imprimir tickets, primero debe seleccionar una impresora en la pantalla de ajustes.") },
        confirmButton = { 
            Button(onClick = onGoToSettings) { 
                Text("Ir a Ajustes") 
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
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
        if (bluetoothPermissions.allPermissionsGranted) {
            onRefreshDevices()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Impresora") },
        text = {
            Column {
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
                        LazyColumn {
                            items(uiState.pairedDevices) { device ->
                                val isSelected = uiState.selectedPrinterMac == device.second
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectPrinter(device.second) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(device.first, modifier = Modifier.weight(1f))
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        dismissButton = {
            IconButton(onClick = {
                if (!bluetoothPermissions.allPermissionsGranted) {
                    bluetoothPermissions.launchMultiplePermissionRequest()
                } else {
                    onRefreshDevices()
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
            }
        }
    )
}
