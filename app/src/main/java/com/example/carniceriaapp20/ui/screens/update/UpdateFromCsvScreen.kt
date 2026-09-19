package com.example.carniceriaapp20.ui.screens.update

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.carniceriaapp20.util.ReportExporter
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun UpdateFromCsvScreen(
    viewModel: UpdateFromCsvViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Launcher para GUARDAR el catálogo localmente (SAF)
    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                val content = viewModel.getProductsCsvContent()
                val success = ReportExporter.writeFileToUri(context, it, content)
                if (success) {
                    Toast.makeText(context, "Catálogo exportado con éxito", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Launcher para IMPORTAR un archivo
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            readCsvContent(context, it)?.let { content ->
                viewModel.importProductsFromCsv(content)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetState() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Gestión de Inventario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Exporta tu lista para corregir precios en Excel o importa la lista nueva.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        when (val result = uiState.result) {
            is UpdateResult.Idle -> {
                // BOTÓN EXPORTAR
                Button(
                    onClick = { saveCsvLauncher.launch("catalogo_precios_actual.csv") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Exportar DB")
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(24.dp))

                // BOTÓN IMPORTAR
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Actualizar DB")
                }
            }
            is UpdateResult.InProgress -> {
                CircularProgressIndicator()
                Text("Procesando...", modifier = Modifier.padding(top = 16.dp))
            }
            is UpdateResult.Success -> {
                Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Text("¡Éxito! ${result.count} productos actualizados.", style = MaterialTheme.typography.titleMedium)
                if (result.skipped > 0) {
                    Text(
                        "${result.skipped} líneas se omitieron (datos inválidos o código repetido).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.resetState() }) { Text("ENTENDIDO") }
            }
            is UpdateResult.Error -> {
                Text("Error: ${result.message}", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.resetState() }) { Text("REINTENTAR") }
            }
        }
    }
}

private fun readCsvContent(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
    } catch (e: Exception) { null }
}
