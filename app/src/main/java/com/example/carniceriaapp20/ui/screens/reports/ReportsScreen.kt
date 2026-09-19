package com.example.carniceriaapp20.ui.screens.reports

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"))
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = localMidnightToUtcPickerMillis(uiState.selectedDate)
    )

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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onDateSelected(utcPickerMillisToLocalMidnight(it))
                    }
                    showDatePicker = false
                }) { Text("ACEPTAR") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCELAR") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Cambiar Fecha")
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
                        text = dateFormat.format(Date(uiState.selectedDate)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currencyFormat.format(uiState.totalDay),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1B5E20)
                    )
                    Text("VENTA TOTAL DEL DÍA", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.departmentSales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay ventas registradas", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
