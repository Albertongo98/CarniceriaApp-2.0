package com.example.carniceriaapp20.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Único acceso a DataStore "settings" de toda la app (dos instancias sobre el mismo archivo pueden crashear).
@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferencesKeys {
        val PRINTER_MAC_ADDRESS = stringPreferencesKey("printer_mac_address")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
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

    /** Sal y hash del PIN, o null si no hay PIN configurado. */
    suspend fun getPin(): Pair<String, String>? {
        val prefs = context.dataStore.data.first()
        val salt = prefs[PreferencesKeys.PIN_SALT] ?: return null
        val hash = prefs[PreferencesKeys.PIN_HASH] ?: return null
        return salt to hash
    }

    suspend fun savePin(salt: String, hash: String) {
        context.dataStore.edit {
            it[PreferencesKeys.PIN_SALT] = salt
            it[PreferencesKeys.PIN_HASH] = hash
        }
    }

    suspend fun clearPin() {
        context.dataStore.edit {
            it.remove(PreferencesKeys.PIN_SALT)
            it.remove(PreferencesKeys.PIN_HASH)
        }
    }

    val lastBackupAt: Flow<Long?> = context.dataStore.data.map { it[PreferencesKeys.LAST_BACKUP_AT] }

    suspend fun saveLastBackupAt(timestamp: Long) {
        context.dataStore.edit { it[PreferencesKeys.LAST_BACKUP_AT] = timestamp }
    }
}
