package com.example.carniceriaapp20.ui.screens.tpv

import android.Manifest
import android.os.Build
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.carniceriaapp20.ui.composables.rememberPinGate
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
    // Le avisa al sistema cuándo el TPV ya muestra el catálogo ("Fully drawn +Xms" en logcat): sirve para medir el arranque real.
    ReportDrawnWhen { uiState.filteredProducts.isNotEmpty() }
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }
    var showSetPin by remember { mutableStateOf(false) }
    val pinGate = rememberPinGate()
    
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val bluetoothPrintPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberMultiplePermissionsState(
            permissions = listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        )
    } else {
        rememberMultiplePermissionsState(permissions = emptyList())
    }

    val backupReminder by viewModel.backupReminder.collectAsState()
    LaunchedEffect(backupReminder) {
        backupReminder?.let {
            viewModel.onBackupReminderShown()
            snackbarHostState.showSnackbar(it)
        }
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
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Gestionar Productos") }, 
                                onClick = { navController.navigate(Routes.PRODUCT_LIST); menuExpanded = false }, 
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Historial de Tickets") }, 
                                onClick = { navController.navigate(Routes.HISTORY); menuExpanded = false }, 
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Reportes de Venta") }, // NUEVO: Acceso a reportes
                                onClick = { menuExpanded = false; pinGate.require { navController.navigate(Routes.REPORTS) } }, 
                                leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Generador de Etiquetas") }, 
                                onClick = { navController.navigate(Routes.LABEL_GENERATOR); menuExpanded = false }, 
                                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Actualizar Base de Datos") }, 
                                onClick = { menuExpanded = false; pinGate.require { navController.navigate(Routes.UPDATE_FROM_CSV) } }, 
                                leadingIcon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) }
                            )
                             DropdownMenuItem(
                                 text = { Text("Configurar Impresora") }, 
                                 onClick = { viewModel.onSettingsClick(); menuExpanded = false }, 
                                 leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                             )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Respaldo y diagnóstico") },
                                onClick = { menuExpanded = false; pinGate.require { navController.navigate(Routes.BACKUP) } },
                                leadingIcon = { Icon(Icons.Default.Backup, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Seguridad (PIN)") },
                                onClick = { menuExpanded = false; pinGate.require { showSetPin = true } },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // Las dos disposiciones (tablet / teléfono) usan el mismo panel del ticket.
        val ticketPanel: @Composable (Modifier, Boolean) -> Unit = { panelModifier, tablet ->
            TicketManagementPanel(
                modifier = panelModifier,
                uiState = uiState,
                isTablet = tablet,
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

        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (isTablet) {
                Row(modifier = Modifier.weight(1f)) {
                    ProductCatalogPanel(
                        modifier = Modifier.weight(0.42f).fillMaxHeight(),
                        uiState = uiState,
                        isTablet = true,
                        onSearchQueryChange = viewModel::onSearchQueryChange,
                        onProductClick = viewModel::addProductToCart
                    )
                    VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    ticketPanel(Modifier.weight(0.58f).fillMaxHeight(), true)
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    ProductCatalogPanel(
                        modifier = Modifier.weight(0.45f).fillMaxWidth(),
                        uiState = uiState,
                        isTablet = false,
                        onSearchQueryChange = viewModel::onSearchQueryChange,
                        onProductClick = viewModel::addProductToCart
                    )
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    ticketPanel(Modifier.weight(0.55f).fillMaxWidth(), false)
                }
            }

            if (isTablet) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                DynamicKeyboardPanel(
                    modifier = Modifier.fillMaxWidth(),
                    height = 380.dp,
                    uiState = uiState,
                    onQwertyKeyPress = viewModel::onQwertyKeyPress,
                    onKeypadInput = viewModel::onKeypadInput,
                    onClear = viewModel::onKeypadClear,
                    onBackspace = viewModel::onKeypadBackspace,
                    onApply = viewModel::onApplyKeypadInput,
                    onApplyPieces = viewModel::applyEstimatedPieces,
                    onProductClick = viewModel::addProductToCart,
                    onAmountClick = viewModel::applyQuickAmount
                )
            } else if (uiState.selectedCartItem != null) {
                PhoneEditBar(
                    selectedItem = uiState.selectedCartItem!!,
                    keypadInput = uiState.keypadInput,
                    onKeypadInput = viewModel::onKeypadInput,
                    onBackspace = viewModel::onKeypadBackspace,
                    onApply = viewModel::onApplyKeypadInput,
                    onApplyPieces = viewModel::applyEstimatedPieces,
                    onCancel = { viewModel.onSelectItem(null) }
                )
            }
        }
    }

    if (uiState.showConfirmSaleDialog) ConfirmSaleDialog(ticket = uiState.activeTicket, isPrinting = uiState.isPrinting, onDismiss = viewModel::onDismissFinalizeSaleDialog, onConfirm = viewModel::confirmSale)
    if (uiState.showNoPrinterDialog) NoPrinterDialog(onDismiss = viewModel::onDismissNoPrinterDialog, onGoToSettings = { viewModel.onDismissNoPrinterDialog(); viewModel.onSettingsClick() })
    pinGate.Dialog()
    pinGate.SetPin(show = showSetPin, onDismiss = { showSetPin = false })
    if (uiState.showSettingsDialog) SettingsDialog(uiState = uiState, onDismiss = viewModel::onDismissSettingsDialog, onSelectPrinter = viewModel::selectPrinter, onRefreshDevices = viewModel::refreshPairedDevices)
}
