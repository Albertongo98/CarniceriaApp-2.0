package com.example.carniceriaapp20.ui.screens.update

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun UpdateFromCsvScreen(
    viewModel: UpdateFromCsvViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            readCsvContent(context, it)?.let { content ->
                viewModel.importProductsFromCsv(content)
            }
        }
    }

    // Reset state when the screen is left, for a better UX
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Actualizar Base de Datos", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        when (val result = uiState.result) {
            is UpdateResult.Idle -> {
                Button(onClick = { filePickerLauncher.launch("*/*") }) { // Changed to allow all file types
                    Text("Seleccionar archivo .csv para importar")
                }
            }
            is UpdateResult.InProgress -> {
                CircularProgressIndicator()
                Text("Importando...", modifier = Modifier.padding(top = 8.dp))
            }
            is UpdateResult.Success -> {
                Text("¡Éxito! Se importaron ${result.count} productos.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.resetState() }) {
                    Text("Importar otro archivo")
                }
            }
            is UpdateResult.Error -> {
                Text("Error: ${result.message}")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.resetState() }) {
                    Text("Intentar de nuevo")
                }
            }
        }
    }
}

private fun readCsvContent(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            reader.readText()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
