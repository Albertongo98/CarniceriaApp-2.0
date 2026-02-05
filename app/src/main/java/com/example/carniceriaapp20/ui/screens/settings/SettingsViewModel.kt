package com.example.carniceriaapp20.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.core.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val printerMacAddress = settingsDataStore.printerMacAddress
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun savePrinterMacAddress(macAddress: String) {
        viewModelScope.launch {
            settingsDataStore.savePrinterMacAddress(macAddress)
        }
    }
}
