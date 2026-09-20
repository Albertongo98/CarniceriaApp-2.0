package com.example.carniceriaapp20.ui.screens.tpv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.carniceriaapp20.R
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.isLowStock

/** Catálogo del TPV: buscador + productos agrupados por departamento (colapsables). */
@Composable
fun ProductCatalogPanel(
    modifier: Modifier = Modifier,
    uiState: TpvUiState,
    isTablet: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onProductClick: (Product) -> Unit
) {
    var expandedState by rememberSaveable { mutableStateOf(mapOf<String, Boolean>()) }
    val isSearchActive = uiState.searchQuery.isNotBlank()

    Column(modifier = modifier.padding(8.dp)) {
        if (isTablet) CatalogLogo()
        if (isTablet) {
            TabletSearchBar(uiState.searchQuery, onSearchQueryChange)
        } else {
            PhoneSearchField(uiState.searchQuery, onSearchQueryChange)
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            uiState.filteredProducts.forEach { (department, products) ->
                val isExpanded = expandedState.getOrDefault(department, false)
                item {
                    DepartmentHeader(
                        department = department,
                        highlighted = isSearchActive || isExpanded,
                        isTablet = isTablet,
                        onClick = { if (!isSearchActive) expandedState = expandedState + (department to !isExpanded) }
                    )
                }
                if (isSearchActive || isExpanded) {
                    items(products) { product ->
                        ProductCard(product, isTablet) { onProductClick(product) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogLogo() {
    Box(
        modifier = Modifier.fillMaxWidth().height(90.dp).padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_color),
            contentDescription = null,
            modifier = Modifier.fillMaxHeight(),
            contentScale = ContentScale.Fit
        )
    }
}

// En tablet el teclado QWERTY propio escribe la búsqueda: el campo es solo una vista que le da el foco.
@Composable
private fun TabletSearchBar(query: String, onSearchQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().focusable(true).onFocusChanged { if (it.isFocused) onSearchQueryChange(query) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).clickable { onSearchQueryChange(query) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (query.isEmpty()) "Buscar producto..." else query,
                style = MaterialTheme.typography.bodyLarge,
                color = if (query.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PhoneSearchField(query: String, onSearchQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onSearchQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Buscar producto...", style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
}

@Composable
private fun DepartmentHeader(department: String, highlighted: Boolean, isTablet: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (highlighted) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(if (isTablet) 8.dp else 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (highlighted) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (isTablet) 24.dp else 16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = department.uppercase(),
                style = if (isTablet) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ProductCard(product: Product, isTablet: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(if (isTablet) 12.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                product.name,
                style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Solo si el producto controla su existencia. No bloquea la venta: es un aviso.
            if (product.isLowStock) {
                Text(
                    text = if ((product.stock ?: 0.0) <= 0.0) "SIN EXISTENCIA" else "BAJO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                "$${product.price}",
                style = if (isTablet) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
