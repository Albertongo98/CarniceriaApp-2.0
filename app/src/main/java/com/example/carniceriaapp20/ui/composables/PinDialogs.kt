package com.example.carniceriaapp20.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.carniceriaapp20.util.PinCheck
import com.example.carniceriaapp20.util.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinViewModel @Inject constructor(val pinManager: PinManager) : ViewModel()

/**
 * Portero de acciones sensibles: `pinGate.require { ... }` ejecuta la acción de inmediato si no hay
 * PIN configurado, o pide el PIN antes. Hay que llamar `pinGate.Dialog()` una vez en la pantalla.
 */
@Stable
class PinGate internal constructor(
    private val manager: PinManager,
    private val scope: CoroutineScope
) {
    private var pendingAction by mutableStateOf<(() -> Unit)?>(null)

    fun require(action: () -> Unit) {
        scope.launch {
            if (manager.isPinSet()) pendingAction = action else action()
        }
    }

    @Composable
    fun SetPin(show: Boolean, onDismiss: () -> Unit) {
        if (show) SetPinDialog(manager, onDismiss)
    }

    @Composable
    fun Dialog() {
        val action = pendingAction ?: return
        PinPromptDialog(
            manager = manager,
            onSuccess = {
                pendingAction = null
                action()
            },
            onDismiss = { pendingAction = null }
        )
    }
}

@Composable
fun rememberPinGate(): PinGate {
    val viewModel: PinViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    return remember { PinGate(viewModel.pinManager, scope) }
}

@Composable
fun PinPromptDialog(
    manager: PinManager,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "PIN requerido"
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val submit: () -> Unit = {
        scope.launch {
            when (val result = manager.verify(pin)) {
                PinCheck.Ok -> onSuccess()
                PinCheck.Wrong -> {
                    error = "PIN incorrecto"
                    pin = ""
                }
                is PinCheck.Locked -> {
                    error = "Demasiados intentos. Espera ${result.seconds} s"
                    pin = ""
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= PinManager.MAX_LENGTH && it.all(Char::isDigit)) pin = it
                    error = null
                },
                label = { Text("PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(onClick = submit, enabled = pin.length >= PinManager.MIN_LENGTH) { Text("ACEPTAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}

/** Crear, cambiar o quitar el PIN (el llamador ya pidió el PIN actual con [PinGate] si existía). */
@Composable
fun SetPinDialog(manager: PinManager, onDismiss: () -> Unit) {
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var hasPin by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { hasPin = manager.isPinSet() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasPin) "Cambiar PIN" else "Crear PIN") },
        text = {
            Column {
                Text(
                    "Protege reportes, actualizar la base de datos, respaldo, borrar productos y anular tickets. " +
                        "Anótalo en un lugar seguro: si se olvida, la única salida es borrar los datos de la app.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= PinManager.MAX_LENGTH && it.all(Char::isDigit)) newPin = it; error = null },
                    label = { Text("Nuevo PIN (4 a 8 dígitos)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { if (it.length <= PinManager.MAX_LENGTH && it.all(Char::isDigit)) confirm = it; error = null },
                    label = { Text("Repite el PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    !PinManager.isValidPin(newPin) -> error = "El PIN debe tener de 4 a 8 dígitos"
                    newPin != confirm -> error = "Los PIN no coinciden"
                    else -> scope.launch {
                        manager.setPin(newPin)
                        onDismiss()
                    }
                }
            }) { Text("GUARDAR") }
        },
        dismissButton = {
            Column {
                if (hasPin) {
                    TextButton(onClick = { scope.launch { manager.clearPin(); onDismiss() } }) { Text("QUITAR PIN") }
                }
                TextButton(onClick = onDismiss) { Text("CANCELAR") }
            }
        }
    )
}
