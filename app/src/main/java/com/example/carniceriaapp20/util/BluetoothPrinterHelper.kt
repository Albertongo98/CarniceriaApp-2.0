package com.example.carniceriaapp20.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.carniceriaapp20.data.local.DaySalesReport
import com.example.carniceriaapp20.data.local.DepartmentSalesReport
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductSalesReport
import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.model.EtiquetaProducto
import com.example.carniceriaapp20.data.preferences.UserPreferencesRepository
import com.example.carniceriaapp20.ui.screens.tpv.CartItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class PrintResult {
    object Success : PrintResult()
    data class Error(val message: String) : PrintResult()
}

/**
 * Transporte Bluetooth (RFCOMM) hacia la impresora térmica: conexión, envío por chunks y reintentos.
 * El formato de lo que se imprime está en [TicketPrintFormatter].
 */
@Singleton
class BluetoothPrinterHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val printMutex = Mutex()
    private val bluetoothManager by lazy { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val bluetoothAdapter by lazy { bluetoothManager.adapter }

    companion object {
        private const val TAG = "BTPrinterHelper"
        private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return null
        return bluetoothAdapter?.bondedDevices
    }

    // Toda impresión pasa por aquí: una a la vez (mutex) y siempre a la impresora configurada.
    private suspend fun print(errorPrefix: String = "Error", build: () -> ByteArray): PrintResult = printMutex.withLock {
        val deviceAddress = userPreferencesRepository.printerMacAddress.first() ?: return PrintResult.Error("Impresora no configurada")
        return withContext(Dispatchers.IO) {
            try {
                sendDataToDeviceWithRetry(deviceAddress, build())
            } catch (e: Exception) {
                AppLog.e(TAG, "$errorPrefix al preparar/enviar la impresión", e)
                PrintResult.Error("$errorPrefix: ${e.message}")
            }
        }
    }

    suspend fun printTicket(ticket: Ticket, items: List<CartItem>, folio: String): PrintResult =
        print { TicketPrintFormatter.buildTicket(ticket, items, folio) }

    suspend fun printSalesReport(
        period: String,
        total: Double,
        ticketCount: Int,
        deptSales: List<DepartmentSalesReport>,
        prodSales: List<ProductSalesReport>,
        dailySales: List<DaySalesReport>
    ): PrintResult = print { TicketPrintFormatter.buildSalesReport(period, total, ticketCount, deptSales, prodSales, dailySales) }

    suspend fun printRestockList(products: List<Product>, printedAt: Long): PrintResult =
        print { TicketPrintFormatter.buildRestockList(products, printedAt) }

    suspend fun printEtiqueta(etiqueta: EtiquetaProducto, quantity: Int): PrintResult =
        print { TicketPrintFormatter.buildLabels(etiqueta, quantity) }

    suspend fun printTicketsBatch(ticketsWithItems: List<Pair<Ticket, List<CartItem>>>): PrintResult = printMutex.withLock {
        val deviceAddress = userPreferencesRepository.printerMacAddress.first() ?: return PrintResult.Error("Impresora no configurada")
        for ((ticket, items) in ticketsWithItems) {
            val folio = ticket.dailyFolio.toString().padStart(3, '0')
            val result = withContext(Dispatchers.IO) {
                try {
                    sendDataToDeviceWithRetry(deviceAddress, TicketPrintFormatter.buildTicket(ticket, items, folio))
                } catch (e: Exception) {
                    AppLog.e(TAG, "Error al imprimir el folio $folio", e)
                    PrintResult.Error("Error en folio $folio: ${e.message}")
                }
            }
            if (result is PrintResult.Error) return result
            delay(1000)
        }
        return PrintResult.Success
    }

    private suspend fun sendDataToDeviceWithRetry(deviceAddress: String, data: ByteArray): PrintResult {
        var lastError = ""
        repeat(3) { attempt ->
            var socket: BluetoothSocket? = null
            try {
                val device = bluetoothAdapter?.getRemoteDevice(deviceAddress) ?: return PrintResult.Error("Dispositivo no encontrado")
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter?.cancelDiscovery()
                }
                socket = device.createInsecureRfcommSocketToServiceRecord(PRINTER_UUID)
                withTimeout(10000) { socket.connect() }

                delay(500) // Regla de Oro

                val outputStream = socket.outputStream
                val chunkSize = 256 // Regla de Oro
                var offset = 0
                while (offset < data.size) {
                    val length = if (data.size - offset < chunkSize) data.size - offset else chunkSize
                    outputStream.write(data, offset, length)
                    outputStream.flush()
                    offset += length
                    delay(20) // Regla de Oro
                }

                outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A))
                outputStream.flush()
                delay(1000) // Regla de Oro

                socket.close()
                return PrintResult.Success
            } catch (e: Exception) {
                lastError = e.message ?: "Error de comunicación"
                AppLog.e(TAG, "Fallo de impresión, intento ${attempt + 1} de 3: $lastError", e)
                try { socket?.close() } catch (_: Exception) {} // limpieza: si el socket ya está roto, no hay nada más que hacer
                delay(1000)
            }
        }
        return PrintResult.Error(lastError)
    }
}
