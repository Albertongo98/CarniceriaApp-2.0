package com.example.carniceriaapp20.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carniceriaapp20.data.local.Product

@Composable
fun QwertyKeyboard(
    modifier: Modifier = Modifier,
    fastProducts: List<Product> = emptyList(),
    onKeyPress: (String) -> Unit,
    onProductClick: (Product) -> Unit = {},
    onAmountClick: (Double) -> Unit = {}
) {
    val row1 = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
    val row2 = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", "Ñ")
    val row3 = listOf("Z", "X", "C", "V", "B", "N", "M")
    
    val quickAmounts10 = listOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0)
    val quickAmounts5 = listOf(5.0, 15.0, 25.0, 35.0, 45.0, 55.0, 65.0, 75.0, 85.0, 95.0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. PRODUCTOS SUGERIDOS
        if (fastProducts.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(fastProducts) { product ->
                    SuggestionChip(product = product, onClick = { onProductClick(product) })
                }
            }
        }

        // 2. DINERO RÁPIDO
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(quickAmounts10) { amount ->
                    AmountButton(amount = amount, onClick = { onAmountClick(amount) })
                }
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(quickAmounts5) { amount ->
                    AmountButton(amount = amount, onClick = { onAmountClick(amount) })
                }
            }
        }

        // 3. TECLADO QWERTY
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row1.forEach { key ->
                    KeyButton(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Spacer(modifier = Modifier.weight(0.2f))
                row2.forEach { key ->
                    KeyButton(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
                Spacer(modifier = Modifier.weight(0.2f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(6.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(0.4f))
                row3.forEach { key ->
                    KeyButton(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
                
                Button(
                    onClick = { onKeyPress("BACKSPACE") },
                    modifier = Modifier.weight(1.8f).height(80.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer, 
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.Backspace, contentDescription = "Borrar", modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.weight(0.1f))
            }
        }
    }
}

@Composable
private fun SuggestionChip(product: Product, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFF1B5E20),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(48.dp).widthIn(min = 120.dp, max = 200.dp),
        shadowElevation = 2.dp // CORREGIDO: shadowElevation en lugar de elevation
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = product.name.uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AmountButton(amount: Double, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(48.dp).widthIn(min = 80.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1B5E20)),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Text(
            text = "$${amount.toInt()}",
            color = Color(0xFF1B5E20),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun KeyButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20), contentColor = Color.White),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text(text = text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
    }
}
