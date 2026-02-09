package com.example.carniceriaapp20.ui.composables

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
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
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp) // Mayor separación para claridad
    ) {
        // 1. PRODUCTOS HOY
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(" PRODUCTOS HOY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Box(modifier = Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.CenterEnd) {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(fastProducts) { product ->
                        SuggestionChip(product = product, onClick = { onProductClick(product) })
                    }
                }
                if (fastProducts.size > 3) ScrollIndicator(42.dp)
            }
        }

        // 2. IMPORTES RÁPIDOS
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(contentAlignment = Alignment.CenterEnd) {
                LazyRow(modifier = Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickAmounts10) { AmountButton(it, onClick = { onAmountClick(it) }) }
                }
                ScrollIndicator(40.dp)
            }
            Box(contentAlignment = Alignment.CenterEnd) {
                LazyRow(modifier = Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickAmounts5) { AmountButton(it, true, onClick = { onAmountClick(it) }) }
                }
                ScrollIndicator(40.dp)
            }
        }

        // 3. TECLADO QWERTY - BOTONES MÁS GRANDES PARA APROVECHAR ESPACIO
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row1.forEach { KeyButton(it, Modifier.weight(1f), onKeyPress) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Spacer(modifier = Modifier.weight(0.2f))
                row2.forEach { KeyButton(it, Modifier.weight(1f), onKeyPress) }
                Spacer(modifier = Modifier.weight(0.2f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(0.5f))
                row3.forEach { KeyButton(it, Modifier.weight(1f), onKeyPress) }
                Button(
                    onClick = { onKeyPress("BACKSPACE") },
                    modifier = Modifier.weight(1.5f).height(64.dp), // Altura optimizada
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) { Icon(Icons.Default.Backspace, contentDescription = null, modifier = Modifier.size(28.dp)) }
                Spacer(modifier = Modifier.weight(0.1f))
            }
        }
    }
}

@Composable
private fun ScrollIndicator(height: androidx.compose.ui.unit.Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "scroll")
    val alpha by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 0.8f, animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse), label = "alpha")
    Box(
        modifier = Modifier
            .height(height)
            .width(30.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f)))),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp).alpha(alpha), tint = Color.Gray)
    }
}

@Composable
private fun SuggestionChip(product: Product, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color(0xFF1B5E20), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(40.dp).widthIn(min = 120.dp), shadowElevation = 2.dp) {
        Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(product.name.uppercase(), color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AmountButton(amount: Double, isAlt: Boolean = false, onClick: () -> Unit) {
    val color = if(isAlt) Color(0xFF2E7D32) else Color(0xFF1B5E20)
    OutlinedButton(onClick = onClick, modifier = Modifier.height(40.dp).widthIn(min = 70.dp), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(2.dp, color), contentPadding = PaddingValues(horizontal = 8.dp)) {
        Text("$${amount.toInt()}", color = color, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

@Composable
private fun KeyButton(text: String, modifier: Modifier = Modifier, onClick: (String) -> Unit) {
    Button(onClick = { onClick(text) }, modifier = modifier.height(64.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)), contentPadding = PaddingValues(0.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)) {
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}
