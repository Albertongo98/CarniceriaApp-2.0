package com.example.carniceriaapp20.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.carniceriaapp20.data.preferences.UserPreferencesRepository
import com.example.carniceriaapp20.util.BluetoothPrinterHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context): BluetoothManager {
        return context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    @Provides
    @Singleton
    fun provideBluetoothAdapter(bluetoothManager: BluetoothManager): BluetoothAdapter? {
        return bluetoothManager.adapter
    }

    @Provides
    @Singleton
    fun provideBluetoothPrinterHelper(
        @ApplicationContext context: Context,
        userPreferencesRepository: UserPreferencesRepository
    ): BluetoothPrinterHelper {
        return BluetoothPrinterHelper(context, userPreferencesRepository)
    }
}
