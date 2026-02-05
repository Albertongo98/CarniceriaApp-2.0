package com.example.carniceriaapp20.core.datastore

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
class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val printerMacAddressKey = stringPreferencesKey("printer_mac_address")

    val printerMacAddress: Flow<String?> = context.dataStore.data.map {
        it[printerMacAddressKey]
    }

    suspend fun savePrinterMacAddress(macAddress: String) {
        context.dataStore.edit {
            it[printerMacAddressKey] = macAddress
        }
    }
}
