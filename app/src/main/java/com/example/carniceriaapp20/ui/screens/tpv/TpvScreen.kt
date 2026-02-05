package com.example.carniceriaapp20.ui.screens.tpv

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.carniceriaapp20.R
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.ui.composables.QwertyKeyboard
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
                title = { 
                    Text(
                        "Carniceriapp 2.0", 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
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
                            HorizontalDivider()
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
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(modifier = Modifier.weight(1f)) {
                ProductCatalogPanel(
                    modifier = Modifier.weight(0.42f).fillMaxHeight(),
                    uiState = uiState,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onProductClick = viewModel::addProductToCart
                )

                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                TicketManagementPanel(
                    modifier = Modifier.weight(0.58f).fillMaxHeight(),
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

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            
            DynamicKeyboardPanel(
                modifier = Modifier.fillMaxWidth(),
                uiState = uiState,
                onQwertyKeyPress = viewModel::onQwertyKeyPress,
                onKeypadInput = viewModel::onKeypadInput,
                onClear = viewModel::onKeypadClear,
                onBackspace = viewModel::onKeypadBackspace,
                onApply = viewModel::onApplyKeypadInput,
                onProductClick = viewModel::addProductToCart,
                onAmountClick = viewModel::applyQuickAmount
            )
        }
    }

    if (uiState.showConfirmSaleDialog) {
        ConfirmSaleDialog(
            ticket = uiState.activeTicket,
            isPrinting = uiState.isPrinting,
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

    Column(modifier = modifier.padding(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_color),
                contentDescription = "Logo de La Palma",
                modifier = Modifier.fillMaxHeight(),
                contentScale = ContentScale.Fit
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().focusable(true).onFocusChanged {
                if (it.isFocused) {
                    onSearchQueryChange(uiState.searchQuery) 
                }
            },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable { onSearchQueryChange(uiState.searchQuery) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (uiState.searchQuery.isEmpty()) "Buscar producto..." else uiState.searchQuery,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (uiState.searchQuery.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            uiState.filteredProducts.forEach { (department, products) ->
                item {
                    val isExpanded = expandedState.getOrDefault(department, false)
                    Surface(
                        onClick = { 
                            if (!isSearchActive) {
                                expandedState = expandedState + (department to !isExpanded) 
                            }
                        },
                        color = if (isSearchActive || isExpanded) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSearchActive || isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = department.uppercase(), 
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp), 
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (isSearchActive || expandedState.getOrDefault(department, false)) {
                    items(products) { product ->
                        OutlinedCard(
                            onClick = { onProductClick(product) }, 
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp, horizontal = 12.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    product.name, 
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "$${product.price}", 
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
    Column(modifier = modifier) {
        ScrollableTabRow(
            selectedTabIndex = uiState.activeTicketIndex,
            containerColor = Color.Transparent,
            divider = {},
            edgePadding = 8.dp
        ) {
            uiState.tickets.forEachIndexed { index, ticket ->
                Tab(
                    selected = uiState.activeTicketIndex == index,
                    onClick = { onSetActiveTicket(index) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Ticket ${index + 1}", fontWeight = if (uiState.activeTicketIndex == index) FontWeight.Bold else FontWeight.Normal)
                            if (uiState.tickets.size > 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Close, 
                                    contentDescription = "Cerrar", 
                                    modifier = Modifier.size(14.dp).clickable { onCloseTicket(index) }
                                )
                            }
                        }
                    }
                )
            }
            Tab(
                selected = false, 
                onClick = onAddTicket, 
                text = { Icon(Icons.Default.Add, contentDescription = "Añadir ticket") }
            )
        }

        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            if (uiState.activeTicket.items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("El ticket está vacío", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // CORREGIDO: Usamos el nuevo ID único como clave de la lista
                    items(uiState.activeTicket.items, key = { it.id }) { item ->
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL A PAGAR", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "$${ String.format("%.2f", uiState.activeTicket.total)}",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReprintLast, 
                        enabled = !uiState.isPrinting,
                        modifier = Modifier.weight(0.4f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("REIMPRIMIR")
                    }
                    Button(
                        onClick = onFinalizeSale, 
                        enabled = !uiState.isPrinting && uiState.activeTicket.items.isNotEmpty(),
                        modifier = Modifier.weight(0.6f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                    ) {
                        if (uiState.isPrinting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("FINALIZAR VENTA", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
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
    onClick: () -> Unit, 
    onRemove: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.product.name, 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            val desc = if (item.product.unit == ProductUnit.GRANEL) {
                if (item.customPrice != null) "Importe manual" else "${String.format("%.3f", item.quantity)} kg"
            } else {
                "${item.quantity.toInt()} piezas x $${item.product.price}"
            }
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (item.product.unit == ProductUnit.UNIDAD) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
                Text(
                    item.quantity.toInt().toString(), 
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            "$${"%.2f".format(item.totalPrice)}", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun DynamicKeyboardPanel(
    modifier: Modifier = Modifier,
    uiState: TpvUiState,
    onQwertyKeyPress: (String) -> Unit,
    onKeypadInput: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onApply: (isPrice: Boolean) -> Unit,
    onProductClick: (Product) -> Unit,
    onAmountClick: (Double) -> Unit
) {
    Box(modifier.height(440.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surface)) { 
        when (uiState.activeKeyboard) {
            KeyboardType.QWERTY -> {
                QwertyKeyboard(
                    modifier = Modifier.fillMaxSize(), 
                    fastProducts = uiState.fastProducts,
                    onKeyPress = onQwertyKeyPress,
                    onProductClick = onProductClick,
                    onAmountClick = onAmountClick
                )
            }
            KeyboardType.NUMERIC -> {
                KeypadWithActionsPanel(
                    modifier = Modifier.fillMaxSize(),
                    selectedItem = uiState.selectedCartItem,
                    keypadInput = uiState.keypadInput,
                    onKeypadInput = onKeypadInput,
                    onClear = onClear,
                    onBackspace = onBackspace,
                    onApply = onApply
                )
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
    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedItem != null) "EDITANDO: ${selectedItem.product.name}" else "SELECCIONE UN PRODUCTO",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (keypadInput.isEmpty()) "0" else keypadInput,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Button(
                    onClick = onBackspace,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Backspace, contentDescription = "Borrar", modifier = Modifier.size(24.dp))
                }
            }
        }

        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(0.65f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                userScrollEnabled = false
            ) {
                val keys = listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", ".", "0")
                items(keys) { key ->
                    Button(
                        modifier = Modifier.fillMaxWidth().height(75.dp),
                        onClick = { onKeypadInput(key) },
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                    ) {
                        Text(key, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Column(
                modifier = Modifier.weight(0.35f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onApply(false) }, 
                    enabled = selectedItem != null,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Text("PESAR\n(KG / PZ)", textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 18.sp)
                }
                Button(
                    onClick = { onApply(true) }, 
                    enabled = selectedItem?.product?.unit == ProductUnit.GRANEL,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Text("IMPORTE\n($)", textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 18.sp)
                }
                 OutlinedButton(
                    modifier = Modifier.fillMaxWidth().weight(0.8f), 
                    onClick = onClear,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1B5E20))
                 ) {
                    Text("LIMPIAR", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ConfirmSaleDialog(
    ticket: TicketState, 
    isPrinting: Boolean, 
    onDismiss: () -> Unit, 
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = if (isPrinting) ({}) else onDismiss,
        title = { Text("Confirmar Venta", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Resumen de compra:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(ticket.items) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            val qtyText = if(it.product.unit == ProductUnit.GRANEL) String.format("%.3f kg", it.quantity) else "${it.quantity.toInt()} pz"
                            Text("$qtyText ${it.product.name}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$" + "%.2f".format(it.totalPrice), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "$" + "%.2f".format(ticket.total), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = onConfirm, 
                enabled = !isPrinting,
                modifier = Modifier.widthIn(min = 140.dp),
                shape = RoundedCornerShape(12.dp)
            ) { 
                if (isPrinting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("IMPRIMIR Y GUARDAR") 
                }
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss, enabled = !isPrinting) { 
                Text("CANCELAR") 
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
                        Text("No se encontraron impresoras vinculadas.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(uiState.pairedDevices) { device ->
                                val isSelected = uiState.selectedPrinterMac == device.second
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { onSelectPrinter(device.second) },
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(device.first, modifier = Modifier.weight(1f), fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("CERRAR")
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
