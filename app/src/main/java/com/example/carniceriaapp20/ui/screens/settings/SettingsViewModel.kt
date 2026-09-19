package com.example.carniceriaapp20.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carniceriaapp20.data.preferences.UserPreferencesRepository
import com.example.carniceriaapp20.data.repository.ProductRepository
import com.example.carniceriaapp20.util.ReportExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    val printerMacAddress = userPreferencesRepository.printerMacAddress
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun savePrinterMacAddress(macAddress: String) {
        viewModelScope.launch {
            userPreferencesRepository.savePrinterMacAddress(macAddress)
        }
    }

    suspend fun getProductsCsvContent(): String {
        val products = productRepository.getAllProducts().first()
        return ReportExporter.productsToCsv(products)
    }
}
