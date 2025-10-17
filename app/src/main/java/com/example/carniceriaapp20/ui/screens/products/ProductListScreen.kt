package com.example.carniceriaapp20.ui.screens.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.carniceriaapp20.data.local.Product

@Composable
fun ProductListScreen(
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit,
    viewModel: ProductListViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    var showDialog by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProduct) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Producto")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = "Gestión de Productos",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(products) { product ->
                    ProductItem(
                        product = product,
                        onEdit = { onEditProduct(product.code) },
                        onDelete = { showDialog = product }
                    )
                    Divider()
                }
            }
        }
    }

    showDialog?.let { productToDelete ->
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = { Text("Eliminar Producto") },
            text = { Text("¿Estás seguro de que quieres eliminar el producto \"${productToDelete.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(productToDelete)
                        showDialog = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ProductItem(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = "Código: ${product.code}", fontSize = 14.sp)
            Text(text = "Precio: $" + "%.2f".format(product.price), fontSize = 14.sp)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Editar")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
        }
    }
}
