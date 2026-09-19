package com.example.carniceriaapp20.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.carniceriaapp20.data.local.DepartmentSalesReport
import com.example.carniceriaapp20.data.local.ProductSalesReport
import com.example.carniceriaapp20.data.local.ProductUnit
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
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class PrintResult {
    object Success : PrintResult()
    data class Error(val message: String) : PrintResult()
}

@Singleton
class BluetoothPrinterHelper @Inject constructor(
    @field:ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val printMutex = Mutex()
    private val bluetoothManager by lazy { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val bluetoothAdapter by lazy { bluetoothManager.adapter }

    companion object {
        private const val TAG = "BTPrinterHelper"
        private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // ESC/POS Commands
        private val CMD_INIT: ByteArray = byteArrayOf(0x1B, 0x40)
        private val CMD_ALIGN_LEFT: ByteArray = byteArrayOf(0x1B, 0x61, 0)
        private val CMD_ALIGN_CENTER: ByteArray = byteArrayOf(0x1B, 0x61, 1)
        private val CMD_ALIGN_RIGHT: ByteArray = byteArrayOf(0x1B, 0x61, 2)
        private val CMD_BOLD_ON: ByteArray = byteArrayOf(0x1B, 0x45, 1)
        private val CMD_BOLD_OFF: ByteArray = byteArrayOf(0x1B, 0x45, 0)
        private val CMD_DOUBLE_SIZE_ON: ByteArray = byteArrayOf(0x1D, 0x21, 0x11)
        private val CMD_NORMAL_SIZE: ByteArray = byteArrayOf(0x1D, 0x21, 0x00)
        private val CMD_FEED_PAPER: ByteArray = byteArrayOf(0x1B, 0x64, 5) 
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return null
        return bluetoothAdapter?.bondedDevices
    }

    suspend fun printTicket(ticket: Ticket, items: List<CartItem>, folio: String): PrintResult = printMutex.withLock {
        val deviceAddress = userPreferencesRepository.printerMacAddress.first() ?: return PrintResult.Error("Impresora no configurada")
        return withContext(Dispatchers.IO) {
            try {
                val data = buildTicketData(ticket, items, folio)
                sendDataToDeviceWithRetry(deviceAddress, data)
            } catch (e: Exception) { PrintResult.Error("Error: ${e.message}") }
        }
    }

    suspend fun printSalesReport(
        date: String,
        totalDay: Double,
        deptSales: List<DepartmentSalesReport>,
        prodSales: List<ProductSalesReport>
    ): PrintResult = printMutex.withLock {
        val deviceAddress = userPreferencesRepository.printerMacAddress.first() ?: return PrintResult.Error("Impresora no configurada")
        return withContext(Dispatchers.IO) {
            try {
                val data = buildSalesReportData(date, totalDay, deptSales, prodSales)
                sendDataToDeviceWithRetry(deviceAddress, data)
            } catch (e: Exception) { PrintResult.Error("Error: ${e.message}") }
        }
    }

    private fun buildSalesReportData(
        date: String,
        totalDay: Double,
        deptSales: List<DepartmentSalesReport>,
        prodSales: List<ProductSalesReport>
    ): ByteArray {
        val charset = Charsets.ISO_8859_1
        val localeMexico = Locale.forLanguageTag("es-MX")
        val currencyFormat = NumberFormat.getCurrencyInstance(localeMexico)

        return ByteArrayOutputStream().apply {
            write(CMD_INIT)
            write(CMD_ALIGN_CENTER)
            write(CMD_BOLD_ON)
            write("CORTE DE CAJA DETALLADO\n".toByteArray(charset))
            write(CMD_NORMAL_SIZE)
            write("LA PALMA CARNICERIA\n".toByteArray(charset))
            write(CMD_BOLD_OFF)
            write("Fecha: $date\n".toByteArray(charset))
            write("--------------------------------\n".toByteArray(charset))

            write(CMD_ALIGN_LEFT)
            deptSales.forEach { dept ->
                write(CMD_BOLD_ON)
                write("DEP: ${dept.department.uppercase()}\n".toByteArray(charset))
                write(CMD_BOLD_OFF)
                
                prodSales.filter { it.department == dept.department }.forEach { prod ->
                    val qtyStr = if (prod.totalQuantity % 1 == 0.0) prod.totalQuantity.toInt().toString() else "%.3f".format(localeMexico, prod.totalQuantity)
                    val unitStr = if (prod.effectiveUnit == ProductUnit.GRANEL) "kg" else "pz"
                    val name = if (prod.productName.length > 15) prod.productName.take(15) else prod.productName
                    val amountStr = currencyFormat.format(prod.totalAmount)
                    val line = " $name $qtyStr$unitStr $amountStr"
                    write("${line.take(32)}\n".toByteArray(charset))
                }
                write(" Subtotal: ${currencyFormat.format(dept.totalAmount)}\n".toByteArray(charset))
                write("--------------------------------\n".toByteArray(charset))
            }

            write(CMD_ALIGN_RIGHT)
            write(CMD_BOLD_ON)
            write("TOTAL VENTA: ".toByteArray(charset))
            write(CMD_DOUBLE_SIZE_ON)
            write("${currencyFormat.format(totalDay)}\n".toByteArray(charset))
            write(CMD_NORMAL_SIZE)
            write(CMD_BOLD_OFF)
            
            write(CMD_ALIGN_CENTER)
            write("\n¡CONTROL DE VENTAS EXITOSO!\n".toByteArray(charset))
            write(CMD_FEED_PAPER)
        }.toByteArray()
    }

    suspend fun printTicketsBatch(ticketsWithItems: List<Pair<Ticket, List<CartItem>>>): PrintResult = printMutex.withLock {
        val deviceAddress = userPreferencesRepository.printerMacAddress.first() ?: return PrintResult.Error("Impresora no configurada")
        for ((ticket, items) in ticketsWithItems) {
            val folio = ticket.dailyFolio.toString().padStart(3, '0')
            val result = withContext(Dispatchers.IO) {
                try {
                    val data = buildTicketData(ticket, items, folio)
                    sendDataToDeviceWithRetry(deviceAddress, data)
                } catch (e: Exception) { PrintResult.Error("Error en folio $folio: ${e.message}") }
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
                try { socket?.close() } catch (_: Exception) {}
                delay(1000) 
            }
        }
        return PrintResult.Error(lastError)
    }

    private fun buildTicketData(ticket: Ticket, items: List<CartItem>, folio: String): ByteArray {
        val charset = Charsets.ISO_8859_1
        val localeMexico = Locale.forLanguageTag("es-MX")
        val currencyFormat = NumberFormat.getCurrencyInstance(localeMexico)
        val fullDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", localeMexico)

        return ByteArrayOutputStream().apply {
            write(CMD_INIT)
            write(CMD_ALIGN_CENTER)
            write("LA PALMA CARNICERIA\n".toByteArray(charset))
            write("--------------------------------\n".toByteArray(charset))
            write("${fullDateFormat.format(Date(ticket.timestamp))}\n".toByteArray(charset))
            write("--------------------------------\n".toByteArray(charset))
            
            write(CMD_ALIGN_LEFT)
            items.forEach { item ->
                write(CMD_BOLD_ON)
                val cleanName = item.product.name.uppercase()
                write("${if (cleanName.length > 20) cleanName.take(20) else cleanName} (${item.product.code.padStart(4, '0')})\n".toByteArray(charset))
                write(CMD_BOLD_OFF)

                if (item.product.unit == ProductUnit.UNIDAD && item.quantity >= 2) {
                    write(CMD_ALIGN_CENTER)
                    write(CMD_BOLD_ON)
                    write(CMD_DOUBLE_SIZE_ON)
                    write(">> ${item.quantity.toInt()} PIEZAS <<\n".toByteArray(charset))
                    write(CMD_NORMAL_SIZE)
                    write(CMD_BOLD_OFF)
                    write(CMD_ALIGN_LEFT)
                }

                val cantidadStr = if (item.product.unit == ProductUnit.GRANEL) "%.3f kg".format(item.quantity) else "${item.quantity.toInt()} un."
                val montoItemStr = currencyFormat.format(item.totalPrice)
                val spaces = 32 - cantidadStr.length - montoItemStr.length
                write("${if (spaces > 0) cantidadStr + " ".repeat(spaces) + montoItemStr else "$cantidadStr $montoItemStr".take(32)}\n".toByteArray(charset))

                try {
                    write(CMD_ALIGN_CENTER)
                    val codeToPrint = if (item.product.unit == ProductUnit.GRANEL) generarCodigoParaPOS(item.product.code, item.totalPrice) else item.product.code
                    printBarcode(this, codeToPrint, type = 73, height = 45, hriPosition = 2)
                    write(CMD_ALIGN_LEFT)
                    write("\n".toByteArray())
                } catch (_: Exception) { }
            }

            write("--------------------------------\n".toByteArray(charset))
            write("TOTAL: ".toByteArray(charset))
            write(CMD_DOUBLE_SIZE_ON)
            write("${currencyFormat.format(ticket.totalAmount)}\n\n".toByteArray(charset))
            write(CMD_NORMAL_SIZE)
            write(CMD_ALIGN_CENTER)
            write("Folio: $folio\n\n".toByteArray(charset))

            val sdfTime = SimpleDateFormat("HHmmss", Locale.getDefault())
            val controlData = "${sdfTime.format(Date(ticket.timestamp))}-$folio"
            try {
                printBarcode(this, controlData, type = 73, height = 70, hriPosition = 0)
            } catch (_: Exception) { }

            write("\n¡GRACIAS POR SU COMPRA!\n".toByteArray(charset))
            write(CMD_FEED_PAPER)
        }.toByteArray()
    }

    private fun printBarcode(outputStream: OutputStream, data: String, type: Int, height: Int = 60, hriPosition: Int = 2) {
        val formattedData = if (type == 73 && !data.startsWith("{")) "{B$data" else data
        val dataBytes = formattedData.toByteArray(Charsets.US_ASCII)
        outputStream.write(byteArrayOf(0x1D, 0x68, height.toByte())) 
        outputStream.write(byteArrayOf(0x1D, 0x77, 2.toByte()))      
        outputStream.write(byteArrayOf(0x1D, 0x48, hriPosition.toByte())) 
        outputStream.write(byteArrayOf(0x1D, 0x6B, type.toByte(), dataBytes.size.toByte()))
        outputStream.write(dataBytes)
    }

    suspend fun printEtiqueta(etiqueta: EtiquetaProducto, quantity: Int): PrintResult = printMutex.withLock {
        val deviceAddress = userPreferencesRepository.printerMacAddress.first() ?: return PrintResult.Error("Impresora no configurada")
        return withContext(Dispatchers.IO) {
            try {
                val baos = ByteArrayOutputStream()
                val charset = Charsets.ISO_8859_1
                repeat(quantity) {
                    baos.apply {
                        write(CMD_INIT)
                        write(CMD_ALIGN_CENTER)
                        write("${etiqueta.nombre}\n".toByteArray(charset))
                        write("${etiqueta.precio}\n".toByteArray(charset))
                        write(CMD_FEED_PAPER)
                    }
                }
                sendDataToDeviceWithRetry(deviceAddress, baos.toByteArray())
            } catch (e: Exception) { PrintResult.Error("Error: ${e.message}") }
        }
    }
}
