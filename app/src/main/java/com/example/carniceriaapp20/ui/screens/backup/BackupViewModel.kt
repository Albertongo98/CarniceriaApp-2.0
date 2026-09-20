package com.example.carniceriaapp20.ui.screens.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.preferences.UserPreferencesRepository
import com.example.carniceriaapp20.util.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val working: Boolean = false,
    val message: String? = null,
    val restored: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    prefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState = _uiState.asStateFlow()

    val lastBackupAt: StateFlow<Long?> = prefs.lastBackupAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun run(success: String, block: suspend () -> Result<Unit>, onOk: () -> Unit = {}) {
        if (_uiState.value.working) return
        viewModelScope.launch {
            _uiState.update { it.copy(working = true) }
            val result = block()
            result.onSuccess { onOk() }
            _uiState.update {
                it.copy(
                    working = false,
                    message = result.fold({ success }, { e -> "Error: ${e.message ?: "no se pudo completar"}" })
                )
            }
        }
    }

    fun export(uri: Uri) = run("Respaldo guardado correctamente", { backupManager.exportTo(uri) })

    fun restore(uri: Uri) = run(
        "Respaldo restaurado",
        { backupManager.restoreFrom(uri) },
        onOk = { _uiState.update { it.copy(restored = true) } }
    )

    fun exportLog(uri: Uri) = run("Registro de errores guardado", { backupManager.exportLogTo(uri) })

    fun restartApp() = backupManager.restartApp()

    fun onMessageConsumed() = _uiState.update { it.copy(message = null) }
}
