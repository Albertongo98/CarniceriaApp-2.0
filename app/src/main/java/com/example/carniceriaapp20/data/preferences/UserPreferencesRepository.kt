package com.example.carniceriaapp20.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferencesKeys {
        val PRINTER_MAC_ADDRESS = stringPreferencesKey("printer_mac_address")
    }

    val printerMacAddress: Flow<String?> = context.dataStore.data
        .map {
            it[PreferencesKeys.PRINTER_MAC_ADDRESS]
        }

    suspend fun savePrinterMacAddress(macAddress: String) {
        context.dataStore.edit {
            it[PreferencesKeys.PRINTER_MAC_ADDRESS] = macAddress
        }
    }
}