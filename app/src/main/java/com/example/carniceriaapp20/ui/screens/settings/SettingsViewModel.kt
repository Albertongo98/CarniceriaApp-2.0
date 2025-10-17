package com.example.carniceriaapp20.ui.screens.settings

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.preferences.UserPreferencesRepository
import com.example.carniceriaapp20.util.BluetoothPrinterHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val pairedDevices: List<Pair<String, String>> = emptyList(),
    val selectedPrinterMac: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val printerHelper: BluetoothPrinterHelper,
    private val userPreferencesRepository: UserPreferencesRepository // Kept for observing the Flow
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observePrinterSelection()
        refreshPairedDevices()
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val devices = printerHelper.getPairedDevices()?.map { 
            // Check for permission before accessing name
            it.name to it.address 
        } ?: emptyList()
        _uiState.value = _uiState.value.copy(pairedDevices = devices)
    }

    fun selectPrinter(macAddress: String) {
        viewModelScope.launch {
            printerHelper.savePrinterAddress(macAddress)
        }
    }

    private fun observePrinterSelection() {
        viewModelScope.launch {
            userPreferencesRepository.printerMacAddress.collect {
                _uiState.value = _uiState.value.copy(selectedPrinterMac = it)
            }
        }
    }
}
