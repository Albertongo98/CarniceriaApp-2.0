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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType as SystemKeyboardType
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
    
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

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
                is PrintResult.Success -> "Ticket enviado correctamente"
                is PrintResult.Error -> "Error de impresión: ${it.message}"
            }
            snackbarHostState.showSnackbar(message)
            viewModel.onPrintResultConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CarniceriaApp", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Gestionar Productos") }, onClick = { navController.navigate(Routes.PRODUCT_LIST); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) })
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Historial de Tickets") }, onClick = { navController.navigate(Routes.HISTORY); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.History, contentDescription = null) })
                            DropdownMenuItem(text = { Text("Generador de Etiquetas") }, onClick = { navController.navigate(Routes.LABEL_GENERATOR); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) })
                            DropdownMenuItem(text = { Text("Actualizar Base de Datos") }, onClick = { navController.navigate(Routes.UPDATE_FROM_CSV); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) })
                             DropdownMenuItem(text = { Text("Configurar Impresora") }, onClick = { viewModel.onSettingsClick(); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) })
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            
            if (isTablet) {
                Row(modifier = Modifier.weight(1f)) {
                    ProductCatalogPanel(modifier = Modifier.weight(0.42f).fillMaxHeight(), uiState = uiState, isTablet = true, onSearchQueryChange = viewModel::onSearchQueryChange, onProductClick = viewModel::addProductToCart)
                    VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    TicketManagementPanel(modifier = Modifier.weight(0.58f).fillMaxHeight(), uiState = uiState, isTablet = true, onSelectItem = viewModel::onSelectItem, onRemoveItem = viewModel::removeItemFromCart, onIncrementItem = viewModel::incrementCartItemQuantity, onDecrementItem = viewModel::decrementCartItemQuantity, onAddTicket = viewModel::addTicket, onCloseTicket = viewModel::closeTicket, onSetActiveTicket = viewModel::setActiveTicket, onFinalizeSale = { if (!bluetoothPrintPermissions.allPermissionsGranted) bluetoothPrintPermissions.launchMultiplePermissionRequest() else viewModel.onFinalizeSaleClick() }, onReprintLast = viewModel::reprintLastTicket)
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    ProductCatalogPanel(modifier = Modifier.weight(0.45f).fillMaxWidth(), uiState = uiState, isTablet = false, onSearchQueryChange = viewModel::onSearchQueryChange, onProductClick = viewModel::addProductToCart)
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    TicketManagementPanel(modifier = Modifier.weight(0.55f).fillMaxWidth(), uiState = uiState, isTablet = false, onSelectItem = viewModel::onSelectItem, onRemoveItem = viewModel::removeItemFromCart, onIncrementItem = viewModel::incrementCartItemQuantity, onDecrementItem = viewModel::decrementCartItemQuantity, onAddTicket = viewModel::addTicket, onCloseTicket = viewModel::closeTicket, onSetActiveTicket = viewModel::setActiveTicket, onFinalizeSale = { if (!bluetoothPrintPermissions.allPermissionsGranted) bluetoothPrintPermissions.launchMultiplePermissionRequest() else viewModel.onFinalizeSaleClick() }, onReprintLast = viewModel::reprintLastTicket)
                }
            }

            if (isTablet) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                DynamicKeyboardPanel(modifier = Modifier.fillMaxWidth(), uiState = uiState, onQwertyKeyPress = viewModel::onQwertyKeyPress, onKeypadInput = viewModel::onKeypadInput, onClear = viewModel::onKeypadClear, onBackspace = viewModel::onKeypadBackspace, onApply = viewModel::onApplyKeypadInput, onProductClick = viewModel::addProductToCart, onAmountClick = viewModel::applyQuickAmount)
            } else if (uiState.selectedCartItem != null) {
                PhoneEditBar(selectedItem = uiState.selectedCartItem!!, keypadInput = uiState.keypadInput, onKeypadInput = viewModel::onKeypadInput, onBackspace = viewModel::onKeypadBackspace, onApply = viewModel::onApplyKeypadInput, onCancel = { viewModel.onSelectItem(null) })
            }
        }
    }

    if (uiState.showConfirmSaleDialog) ConfirmSaleDialog(ticket = uiState.activeTicket, isPrinting = uiState.isPrinting, onDismiss = viewModel::onDismissFinalizeSaleDialog, onConfirm = viewModel::confirmSale)
    if (uiState.showNoPrinterDialog) NoPrinterDialog(onDismiss = viewModel::onDismissNoPrinterDialog, onGoToSettings = { viewModel.onDismissNoPrinterDialog(); viewModel.onSettingsClick() })
    if (uiState.showSettingsDialog) SettingsDialog(uiState = uiState, onDismiss = viewModel::onDismissSettingsDialog, onSelectPrinter = viewModel::selectPrinter, onRefreshDevices = viewModel::refreshPairedDevices)
}

@Composable
fun ProductCatalogPanel(modifier: Modifier = Modifier, uiState: TpvUiState, isTablet: Boolean, onSearchQueryChange: (String) -> Unit, onProductClick: (Product) -> Unit) {
    var expandedState by rememberSaveable { mutableStateOf(mapOf<String, Boolean>()) }
    val isSearchActive = uiState.searchQuery.isNotBlank()
    Column(modifier = modifier.padding(8.dp)) {
        if (isTablet) Box(modifier = Modifier.fillMaxWidth().height(90.dp).padding(bottom = 8.dp), contentAlignment = Alignment.Center) { Image(painter = painterResource(id = R.drawable.logo_color), contentDescription = null, modifier = Modifier.fillMaxHeight(), contentScale = ContentScale.Fit) }
        if (isTablet) {
            Surface(modifier = Modifier.fillMaxWidth().focusable(true).onFocusChanged { if (it.isFocused) onSearchQueryChange(uiState.searchQuery) }, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).clickable { onSearchQueryChange(uiState.searchQuery) }, verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) ; Spacer(modifier = Modifier.width(12.dp)) ; Text(text = if (uiState.searchQuery.isEmpty()) "Buscar producto..." else uiState.searchQuery, style = MaterialTheme.typography.bodyLarge, color = if (uiState.searchQuery.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
        } else {
            OutlinedTextField(value = uiState.searchQuery, onValueChange = onSearchQueryChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Buscar producto...", style = MaterialTheme.typography.bodySmall) }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }, trailingIcon = { if (uiState.searchQuery.isNotEmpty()) IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Close, contentDescription = null) } }, shape = RoundedCornerShape(12.dp), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search))
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            uiState.filteredProducts.forEach { (department, products) ->
                item {
                    val isExpanded = expandedState.getOrDefault(department, false)
                    Surface(onClick = { if (!isSearchActive) expandedState = expandedState + (department to !isExpanded) }, color = if (isSearchActive || isExpanded) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.padding(if(isTablet) 8.dp else 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = if (isSearchActive || isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(if(isTablet) 24.dp else 16.dp)) ; Spacer(modifier = Modifier.width(8.dp)) ; Text(text = department.uppercase(), style = if(isTablet) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) } }
                }
                if (isSearchActive || expandedState.getOrDefault(department, false)) {
                    items(products) { product ->
                        OutlinedCard(onClick = { onProductClick(product) }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp, horizontal = 8.dp), shape = RoundedCornerShape(8.dp)) { Row(modifier = Modifier.padding(if(isTablet) 12.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(product.name, style = if(isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis) ; Text("$${product.price}", style = if(isTablet) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold) } }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketManagementPanel(modifier: Modifier = Modifier, uiState: TpvUiState, isTablet: Boolean, onSelectItem: (CartItem?) -> Unit, onRemoveItem: (CartItem) -> Unit, onIncrementItem: (CartItem) -> Unit, onDecrementItem: (CartItem) -> Unit, onAddTicket: () -> Unit, onCloseTicket: (Int) -> Unit, onSetActiveTicket: (Int) -> Unit, onFinalizeSale: () -> Unit, onReprintLast: () -> Unit) {
    Column(modifier = modifier) {
        ScrollableTabRow(selectedTabIndex = uiState.activeTicketIndex, containerColor = Color.Transparent, divider = {}, edgePadding = 8.dp) {
            uiState.tickets.forEachIndexed { index, ticket -> Tab(selected = uiState.activeTicketIndex == index, onClick = { onSetActiveTicket(index) }, text = { Row(verticalAlignment = Alignment.CenterVertically) { Text("T${index + 1}", style = if(isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall, fontWeight = if (uiState.activeTicketIndex == index) FontWeight.Bold else FontWeight.Normal) ; if (uiState.tickets.size > 1) { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(if(isTablet) 12.dp else 10.dp).padding(start = 4.dp).clickable { onCloseTicket(index) }) } } }) }
            Tab(selected = false, onClick = onAddTicket, text = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(if(isTablet) 18.dp else 14.dp)) })
        }
        Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            if (uiState.activeTicket.items.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ticket Vacío", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall) } }
            else { LazyColumn(modifier = Modifier.fillMaxSize()) { items(uiState.activeTicket.items, key = { it.id }) { item -> CartItemRow(item = item, isSelected = uiState.selectedCartItem == item, isTablet = isTablet, onClick = { if (uiState.selectedCartItem == item) onSelectItem(null) else onSelectItem(item) }, onRemove = { onRemoveItem(item) }, onIncrement = { onIncrementItem(item) }, onDecrement = { onDecrementItem(item) }) ; HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) } } }
        }
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(if(isTablet) 16.dp else 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("TOTAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) ; Text(text = "$${ String.format("%.2f", uiState.activeTicket.total)}", style = if(isTablet) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) }
                Spacer(modifier = Modifier.height(if(isTablet) 8.dp else 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReprintLast, enabled = !uiState.isPrinting, modifier = Modifier.weight(0.35f).height(if(isTablet) 56.dp else 44.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    Button(onClick = onFinalizeSale, enabled = !uiState.isPrinting && uiState.activeTicket.items.isNotEmpty(), modifier = Modifier.weight(0.65f).height(if(isTablet) 56.dp else 44.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))) { if (uiState.isPrinting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White) else Text("FINALIZAR", fontWeight = FontWeight.Bold, fontSize = if(isTablet) 18.sp else 14.sp) }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, isSelected: Boolean, isTablet: Boolean, onClick: () -> Unit, onRemove: () -> Unit, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent).padding(vertical = if(isTablet) 8.dp else 4.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) { 
            Text(item.product.name, style = if(isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val unitLabel = if(item.product.unit == ProductUnit.GRANEL) "kg" else "pz"
                val quantityDisplay = if (item.product.unit == ProductUnit.GRANEL) (if (item.customPrice != null) "Manual" else "${String.format("%.3f", item.quantity)} kg") else "${item.quantity.toInt()} pz"
                Text(quantityDisplay, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text("|", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.width(6.dp))
                Text("$${item.product.price}/${unitLabel.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
        }
        if (item.product.unit == ProductUnit.UNIDAD) { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onDecrement, modifier = Modifier.size(if(isTablet) 28.dp else 24.dp)) { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp)) } ; Text(item.quantity.toInt().toString(), style = if(isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) ; IconButton(onClick = onIncrement, modifier = Modifier.size(if(isTablet) 28.dp else 24.dp)) { Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp)) } } }
        Text("$${"%.2f".format(item.totalPrice)}", style = if(isTablet) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold)
        IconButton(onClick = onRemove, modifier = Modifier.size(if(isTablet) 32.dp else 28.dp)) { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) }
    }
}

@Composable
fun PhoneEditBar(selectedItem: CartItem, keypadInput: String, onKeypadInput: (String) -> Unit, onBackspace: () -> Unit, onApply: (isPrice: Boolean) -> Unit, onCancel: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 8.dp) {
        Column(modifier = Modifier.padding(8.dp)) {
            val unitLabel = if(selectedItem.product.unit == ProductUnit.GRANEL) "KG" else "PZ"
            Text("EDITANDO: ${selectedItem.product.name} ($${selectedItem.product.price}/$unitLabel)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(modifier = Modifier.weight(1f).height(48.dp), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                        Text(text = if (keypadInput.isEmpty()) "0" else keypadInput, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onBackspace) { Icon(Icons.Default.Backspace, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                Button(onClick = { onApply(false) }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp)) { Text("KG/PZ", fontSize = 12.sp) }
                if (selectedItem.product.unit == ProductUnit.GRANEL) { Button(onClick = { onApply(true) }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp)) { Text("$", fontSize = 12.sp) } }
                IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = null) }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("1","2","3","4","5","6","7","8","9",".","0").forEach { num ->
                    TextButton(onClick = { onKeypadInput(num) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text(num, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                }
            }
        }
    }
}

@Composable
fun DynamicKeyboardPanel(modifier: Modifier = Modifier, uiState: TpvUiState, onQwertyKeyPress: (String) -> Unit, onKeypadInput: (String) -> Unit, onClear: () -> Unit, onBackspace: () -> Unit, onApply: (isPrice: Boolean) -> Unit, onProductClick: (Product) -> Unit, onAmountClick: (Double) -> Unit) {
    Box(modifier.height(440.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surface)) { 
        when (uiState.activeKeyboard) {
            KeyboardType.QWERTY -> { QwertyKeyboard(modifier = Modifier.fillMaxSize(), fastProducts = uiState.fastProducts, onKeyPress = onQwertyKeyPress, onProductClick = onProductClick, onAmountClick = onAmountClick) }
            KeyboardType.NUMERIC -> { KeypadWithActionsPanel(modifier = Modifier.fillMaxSize(), selectedItem = uiState.selectedCartItem, keypadInput = uiState.keypadInput, onKeypadInput = onKeypadInput, onClear = onClear, onBackspace = onBackspace, onApply = onApply) }
            else -> { /* No keyboard */ }
        }
    }
}

@Composable
fun KeypadWithActionsPanel(modifier: Modifier = Modifier, selectedItem: CartItem?, keypadInput: String, onKeypadInput: (String) -> Unit, onClear: () -> Unit, onBackspace: () -> Unit, onApply: (isPrice: Boolean) -> Unit) {
    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp)) { 
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { 
                Column(modifier = Modifier.weight(1f)) { 
                    if (selectedItem != null) {
                        val unitLabel = if(selectedItem.product.unit == ProductUnit.GRANEL) "KG" else "PZ"
                        Text(text = "EDITANDO: ${selectedItem.product.name} ($${selectedItem.product.price}/$unitLabel)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    } else {
                        Text(text = "SELECCIONE UN PRODUCTO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(text = if (keypadInput.isEmpty()) "0" else keypadInput, style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface) 
                } ; 
                Button(onClick = onBackspace, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Backspace, contentDescription = "Borrar", modifier = Modifier.size(24.dp)) } 
            } 
        }
        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(0.65f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), userScrollEnabled = false) { val keys = listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", ".", "0") ; items(keys) { key -> Button(modifier = Modifier.fillMaxWidth().height(75.dp), onClick = { onKeypadInput(key) }, shape = RoundedCornerShape(8.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))) { Text(key, fontSize = 32.sp, fontWeight = FontWeight.Bold) } } }
            Column(modifier = Modifier.weight(0.35f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) { Button(onClick = { onApply(false) }, enabled = selectedItem != null, modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))) { Text("PESAR\n(KG / PZ)", textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 18.sp) } ; Button(onClick = { onApply(true) }, enabled = selectedItem?.product?.unit == ProductUnit.GRANEL, modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))) { Text("IMPORTE\n($)", textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 18.sp) } ; OutlinedButton(modifier = Modifier.fillMaxWidth().weight(0.8f), onClick = onClear, shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1B5E20))) { Text("LIMPIAR", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20), fontSize = 14.sp) } } }
    }
}

@Composable
fun ConfirmSaleDialog(ticket: TicketState, isPrinting: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) { AlertDialog(onDismissRequest = if (isPrinting) ({}) else onDismiss, title = { Text("Finalizar Ticket", fontWeight = FontWeight.Bold) }, text = { Column { Text("Resumen para caja:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary) ; Spacer(modifier = Modifier.height(8.dp)) ; LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) { items(ticket.items) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { val qtyText = if(it.product.unit == ProductUnit.GRANEL) String.format("%.3f kg", it.quantity) else "${it.quantity.toInt()} pz" ; Text("$qtyText ${it.product.name}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis) ; Text("$" + "%.2f".format(it.totalPrice), fontWeight = FontWeight.Bold) } } } ; HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) ; Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("TOTAL ESTIMADO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) ; Text(text = "$" + "%.2f".format(ticket.total), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold) } } }, confirmButton = { Button(onClick = onConfirm, enabled = !isPrinting, modifier = Modifier.widthIn(min = 140.dp), shape = RoundedCornerShape(12.dp)) { if (isPrinting) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) } else { Text("GENERAR TICKET") } } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !isPrinting) { Text("CANCELAR") } } ) }
@Composable
fun NoPrinterDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Impresora no configurada") }, text = { Text("Para poder imprimir tickets, primero debe seleccionar una impresora en la pantalla de ajustes.") }, confirmButton = { Button(onClick = onGoToSettings) { Text("Ir a Ajustes") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } } ) }
@OptIn(ExperimentalPermissionsApi::class) @SuppressLint("MissingPermission") @Composable
fun SettingsDialog(uiState: TpvUiState, onDismiss: () -> Unit, onSelectPrinter: (String) -> Unit, onRefreshDevices: () -> Unit) { val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { rememberMultiplePermissionsState(permissions = listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)) } else { rememberMultiplePermissionsState(permissions = emptyList()) } ; LaunchedEffect(key1 = bluetoothPermissions.allPermissionsGranted) { if (bluetoothPermissions.allPermissionsGranted) { onRefreshDevices() } } ; AlertDialog(onDismissRequest = onDismiss, title = { Text("Configurar Impresora") }, text = { Column { if (!bluetoothPermissions.allPermissionsGranted) { Text("Se necesitan permisos de Bluetooth para buscar impresoras.") ; Spacer(modifier = Modifier.height(8.dp)) ; Button(onClick = { bluetoothPermissions.launchMultiplePermissionRequest() }) { Text("Otorgar Permisos") } } else { if (uiState.pairedDevices.isEmpty()) { Text("No se encontraron impresoras vinculadas.") } else { LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) { items(uiState.pairedDevices) { device -> val isSelected = uiState.selectedPrinterMac == device.second ; Surface(modifier = Modifier.fillMaxWidth().clickable { onSelectPrinter(device.second) }, color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, shape = RoundedCornerShape(8.dp)) { Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Bluetooth, contentDescription = null, tint = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) ; Spacer(modifier = Modifier.width(12.dp)) ; Text(device.first, modifier = Modifier.weight(1f), fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal) ; if (isSelected) { Icon(Icons.Default.CheckCircle, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary) } } } } } } } } }, confirmButton = { TextButton(onClick = { onDismiss() }) { Text("CERRAR") } }, dismissButton = { IconButton(onClick = { if (!bluetoothPermissions.allPermissionsGranted) { bluetoothPermissions.launchMultiplePermissionRequest() } else { onRefreshDevices() } }) { Icon(Icons.Default.Refresh, contentDescription = "Refrescar") } } ) }
