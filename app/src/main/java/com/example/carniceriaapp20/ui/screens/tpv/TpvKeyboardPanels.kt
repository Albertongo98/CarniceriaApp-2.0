package com.example.carniceriaapp20.ui.screens.tpv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.ui.composables.QwertyKeyboard

private val PalmaGreen = Color(0xFF1B5E20)

/** Barra de edición del teléfono: campo numérico + acciones (KG/PZ, $, +PZ) y teclado numérico compacto. */
@Composable
fun PhoneEditBar(
    selectedItem: CartItem,
    keypadInput: String,
    onKeypadInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onApply: (isPrice: Boolean) -> Unit,
    onApplyPieces: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 8.dp) {
        Column(modifier = Modifier.padding(8.dp)) {
            val unitLabel = if (selectedItem.product.unit == ProductUnit.GRANEL) "KG" else "PZ"
            Text(
                "EDITANDO: ${selectedItem.product.name} ($${selectedItem.product.price}/$unitLabel)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f).height(48.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                        Text(
                            text = keypadInput.ifEmpty { "0" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onBackspace) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
                Button(onClick = { onApply(false) }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("KG/PZ", fontSize = 12.sp)
                }
                if (selectedItem.product.unit == ProductUnit.GRANEL) {
                    Button(onClick = { onApply(true) }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("$", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onApplyPieces,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("+PZ", fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = null) }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0").forEach { num ->
                    TextButton(onClick = { onKeypadInput(num) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                        Text(num, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

/** Panel inferior de la tablet: QWERTY para buscar o teclado numérico para editar el renglón seleccionado. */
@Composable
fun DynamicKeyboardPanel(
    modifier: Modifier = Modifier,
    height: Dp,
    uiState: TpvUiState,
    onQwertyKeyPress: (String) -> Unit,
    onKeypadInput: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onApply: (isPrice: Boolean) -> Unit,
    onApplyPieces: () -> Unit,
    onProductClick: (Product) -> Unit,
    onAmountClick: (Double) -> Unit
) {
    Box(modifier.height(height).fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        when (uiState.activeKeyboard) {
            KeyboardType.QWERTY -> QwertyKeyboard(
                modifier = Modifier.fillMaxSize(),
                fastProducts = uiState.fastProducts,
                onKeyPress = onQwertyKeyPress,
                onProductClick = onProductClick,
                onAmountClick = onAmountClick
            )
            KeyboardType.NUMERIC -> KeypadWithActionsPanel(
                modifier = Modifier.fillMaxSize(),
                selectedItem = uiState.selectedCartItem,
                keypadInput = uiState.keypadInput,
                onKeypadInput = onKeypadInput,
                onClear = onClear,
                onBackspace = onBackspace,
                onApply = onApply,
                onApplyPieces = onApplyPieces
            )
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
    onApply: (isPrice: Boolean) -> Unit,
    onApplyPieces: () -> Unit
) {
    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        KeypadDisplay(selectedItem, keypadInput, onBackspace)

        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(0.65f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                userScrollEnabled = false
            ) {
                items(listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", ".", "0")) { key ->
                    Button(
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        onClick = { onKeypadInput(key) },
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PalmaGreen)
                    ) {
                        Text(key, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(modifier = Modifier.weight(0.35f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionKey("PESAR\n(KG/PZ)", enabled = selectedItem != null, modifier = Modifier.fillMaxWidth().weight(1f)) { onApply(false) }
                ActionKey(
                    "IMPORTE\n($)",
                    enabled = selectedItem?.product?.unit == ProductUnit.GRANEL,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) { onApply(true) }

                // +PIEZAS solo aplica a productos a granel.
                if (selectedItem?.product?.unit == ProductUnit.GRANEL) {
                    Button(
                        onClick = onApplyPieces,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("+ PIEZAS", textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().weight(0.7f),
                    onClick = onClear,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, PalmaGreen)
                ) {
                    Text("LIMPIAR", fontWeight = FontWeight.ExtraBold, color = PalmaGreen, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun KeypadDisplay(selectedItem: CartItem?, keypadInput: String, onBackspace: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (selectedItem != null) {
                    val unitLabel = if (selectedItem.product.unit == ProductUnit.GRANEL) "KG" else "PZ"
                    Text(
                        text = "EDITANDO: ${selectedItem.product.name} ($${selectedItem.product.price}/$unitLabel)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = keypadInput.ifEmpty { "0" },
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Button(
                onClick = onBackspace,
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Borrar", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ActionKey(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PalmaGreen)
    ) {
        Text(label, textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, lineHeight = 16.sp)
    }
}
