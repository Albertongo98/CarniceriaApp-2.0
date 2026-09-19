package com.example.carniceriaapp20.ui.screens.reports

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.carniceriaapp20.data.local.DaySalesReport
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.util.PrintResult
import com.example.carniceriaapp20.util.ReportExporter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val mx = remember { Locale.forLanguageTag("es-MX") }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(mx) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", mx) }
    val today = remember { startOfDay(System.currentTimeMillis()) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    // Launcher para GUARDAR el reporte HTML localmente
    val saveHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        uri?.let {
            val (_, content) = viewModel.getHtmlReportContent()
            val success = ReportExporter.writeFileToUri(context, it, content)
            if (success) {
                Toast.makeText(context, "Reporte guardado con éxito", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(uiState.printResult) {
        uiState.printResult?.let { result ->
            when (result) {
                is PrintResult.Success -> Toast.makeText(context, "Reporte impreso", Toast.LENGTH_SHORT).show()
                is PrintResult.Error -> Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_LONG).show()
            }
            viewModel.onPrintResultConsumed()
        }
    }

    if (showStartPicker) {
        ReportDatePickerDialog(
            initialLocal = uiState.startDate,
            minLocal = addDays(today, -3650),
            maxLocal = today,
            onConfirm = viewModel::onStartDateSelected,
            onDismiss = { showStartPicker = false }
        )
    }

    if (showEndPicker) {
        ReportDatePickerDialog(
            initialLocal = uiState.endDate,
            minLocal = uiState.startDate,
            maxLocal = minOf(addDays(uiState.startDate, MAX_RANGE_DAYS - 1), today), // rango máximo: 31 días
            onConfirm = viewModel::onEndDateSelected,
            onDismiss = { showEndPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes de Venta", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (uiState.departmentSales.isNotEmpty()) {
                        IconButton(onClick = { viewModel.printReport() }, enabled = !uiState.isPrinting) {
                            if (uiState.isPrinting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Default.Print, contentDescription = "Imprimir Corte")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (uiState.departmentSales.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val (fileName, _) = viewModel.getHtmlReportContent()
                        saveHtmlLauncher.launch(fileName)
                    },
                    containerColor = Color(0xFF1B5E20),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Save, contentDescription = null) },
                    text = { Text("DESCARGAR REPORTE") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = ReportExporter.formatPeriod(uiState.startDate, uiState.endDate),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currencyFormat.format(uiState.total),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = if (uiState.isSingleDay) "VENTA TOTAL DEL DÍA" else "VENTA TOTAL DEL PERIODO (${uiState.dayCount} DÍAS)",
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (uiState.ticketCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            SummaryStat("TICKETS", uiState.ticketCount.toString())
                            SummaryStat("TICKET PROM.", currencyFormat.format(uiState.total / uiState.ticketCount))
                            if (!uiState.isSingleDay && uiState.dailySales.isNotEmpty()) {
                                SummaryStat("PROM. POR DÍA", currencyFormat.format(uiState.total / uiState.dailySales.size))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val thisMonthStart = firstOfMonth(today)
                val lastMonthStart = firstOfMonth(today, -1)
                val lastMonthEnd = addDays(thisMonthStart, -1)
                FilterChip(
                    selected = uiState.endDate == today && uiState.dayCount == 1,
                    onClick = { viewModel.selectLastDays(1) },
                    label = { Text("Hoy") }
                )
                FilterChip(
                    selected = uiState.endDate == today && uiState.dayCount == 7,
                    onClick = { viewModel.selectLastDays(7) },
                    label = { Text("7 días") }
                )
                FilterChip(
                    selected = uiState.startDate == thisMonthStart && uiState.endDate == today,
                    onClick = viewModel::selectThisMonth,
                    label = { Text("Este mes") }
                )
                FilterChip(
                    selected = uiState.startDate == lastMonthStart && uiState.endDate == lastMonthEnd,
                    onClick = viewModel::selectPreviousMonth,
                    label = { Text("Mes anterior") }
                )
                OutlinedButton(onClick = { showStartPicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Desde: ${dateFormat.format(Date(uiState.startDate))}")
                }
                OutlinedButton(onClick = { showEndPicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hasta: ${dateFormat.format(Date(uiState.endDate))}")
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.departmentSales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay ventas registradas en este periodo", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!uiState.isSingleDay && uiState.dailySales.isNotEmpty()) {
                        item {
                            Text("Ventas por Día", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        items(uiState.dailySales, key = { it.dayStart }) { day ->
                            DaySalesRow(day, currencyFormat)
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    item {
                        Text("Resumen por Departamento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    items(uiState.departmentSales) { report ->
                        DepartmentReportItem(report.department, report.totalAmount, report.totalPieces, report.totalKilos, currencyFormat)
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Desglose Detallado", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    val grouped = uiState.productSales.groupBy { it.department }
                    grouped.forEach { (dept, products) ->
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    text = dept.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(products) { prod ->
                            ProductReportItem(
                                productName = prod.productName,
                                quantity = prod.totalQuantity,
                                amount = prod.totalAmount,
                                unit = prod.effectiveUnit,
                                currencyFormat = currencyFormat
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun DaySalesRow(day: DaySalesReport, format: NumberFormat) {
    val dayFormat = remember { SimpleDateFormat("EEE dd/MM", Locale.forLanguageTag("es-MX")) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(dayFormat.format(Date(day.dayStart)).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${day.tickets} tickets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Text(format.format(day.total), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportDatePickerDialog(
    initialLocal: Long,
    minLocal: Long,
    maxLocal: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val minUtc = localMidnightToUtcPickerMillis(minLocal)
    val maxUtc = localMidnightToUtcPickerMillis(maxLocal)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = localMidnightToUtcPickerMillis(initialLocal),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis in minUtc..maxUtc
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onConfirm(utcPickerMillisToLocalMidnight(it)) }
                onDismiss()
            }) { Text("ACEPTAR") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    ) {
        DatePicker(state = state)
    }
}

@Composable
fun DepartmentReportItem(dept: String, amount: Double, pieces: Double, kilos: Double, format: NumberFormat) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(dept, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                val reportLocale = Locale.forLanguageTag("es-MX")
                val parts = buildList {
                    if (pieces > 0) add("${if (pieces % 1 == 0.0) pieces.toInt() else "%.3f".format(reportLocale, pieces)} pz")
                    if (kilos > 0) add("${"%.3f".format(reportLocale, kilos)} kg")
                }
                Text(
                    text = if (parts.isEmpty()) "0 movs." else parts.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(format.format(amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ProductReportItem(productName: String, quantity: Double, amount: Double, unit: ProductUnit, currencyFormat: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                val unitLabel = if (unit == ProductUnit.GRANEL) "kg" else "pz"
                val qtyStr = if (quantity % 1 == 0.0) quantity.toInt().toString() else "%.3f".format(Locale.forLanguageTag("es-MX"), quantity)
                Text("$qtyStr $unitLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Text(currencyFormat.format(amount), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// El DatePicker de Material3 siempre reporta/espera milisegundos anclados a medianoche UTC del día
// elegido, sin importar la zona horaria del dispositivo. El resto de la pantalla (Calendar,
// SimpleDateFormat) usa la zona horaria local, así que hay que convertir explícitamente entre
// ambas representaciones o el día seleccionado queda desfasado.
private fun localMidnightToUtcPickerMillis(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcPickerMillisToLocalMidnight(utcPickerMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcPickerMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
