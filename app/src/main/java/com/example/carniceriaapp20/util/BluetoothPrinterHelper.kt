package com.example.carniceriaapp20.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.carniceriaapp20.R
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.model.EtiquetaProducto
import com.example.carniceriaapp20.data.preferences.UserPreferencesRepository
import com.example.carniceriaapp20.ui.screens.tpv.CartItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import java.io.IOException
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
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter by lazy { bluetoothManager.adapter }

    companion object {
        private const val TAG = "BluetoothPrinterHelper"
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
        private val CMD_CUT: ByteArray = byteArrayOf(0x1D, 0x56, 1)
        private val CMD_FEED_AND_CUT: ByteArray = byteArrayOf(0x0A, 0x0A, 0x0A, 0x1D, 0x56, 1)
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return bluetoothAdapter?.bondedDevices
    }

    suspend fun printTicket(ticket: Ticket, items: List<CartItem>, folio: String, withLogo: Boolean = false): PrintResult {
        val deviceAddress = userPreferencesRepository.printerMacAddress.first() ?: return PrintResult.Error("No hay impresora configurada")
        val charset = Charsets.ISO_8859_1
        val localeMexico = Locale.forLanguageTag("es-MX")
        val currencyFormat = NumberFormat.getCurrencyInstance(localeMexico)
        val fullDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", localeMexico)

        try {
            val outputStream = ByteArrayOutputStream().apply {
                write(CMD_INIT)
                
                write(CMD_ALIGN_CENTER)
                write("--------------------------------\n".toByteArray(charset))
                
                // Fecha y Hora en la parte superior
                write(("${fullDateFormat.format(Date(ticket.timestamp))}\n").toByteArray(charset))
                write("--------------------------------\n".toByteArray(charset))
                
                write(CMD_ALIGN_LEFT)
                items.forEach { item ->
                    write(CMD_BOLD_ON)
                    val productCode = item.product.code.padStart(4, '0')
                    write(("${item.product.name} ($productCode)\n").toByteArray(charset))
                    write(CMD_BOLD_OFF)

                    if (item.product.unit == ProductUnit.UNIDAD && item.quantity > 1) {
                        write(CMD_ALIGN_CENTER)
                        write(CMD_BOLD_ON)
                        write(CMD_DOUBLE_SIZE_ON)
                        write((">> ${item.quantity.toInt()} PIEZAS <<\n").toByteArray(charset))
                        write(CMD_NORMAL_SIZE)
                        write(CMD_BOLD_OFF)
                        write(CMD_ALIGN_LEFT)
                    }

                    val cantidadStr = if (item.product.unit == ProductUnit.GRANEL) "%.3f kg".format(item.quantity) else "${item.quantity.toInt()} un."
                    val montoItemStr = currencyFormat.format(item.totalPrice)
                    val spaces = 32 - cantidadStr.length - montoItemStr.length
                    val itemLine = if (spaces > 0) cantidadStr + " ".repeat(spaces) + montoItemStr else (cantidadStr + " " + montoItemStr).take(32)
                    write((itemLine + "\n").toByteArray(charset))

                    try {
                        write(CMD_ALIGN_CENTER)
                        val codeToPrint = if (item.product.unit == ProductUnit.GRANEL) generarCodigoParaPOS(item.product.code, item.totalPrice) else item.product.code
                        printBarcode(this, codeToPrint, type = 73, height = 50)
                        write(CMD_ALIGN_LEFT)
                        write("\n".toByteArray())
                    } catch (e: Exception) { Log.e(TAG, "Error barcode", e) }
                }

                write("--------------------------------\n".toByteArray(charset))
                write(CMD_ALIGN_RIGHT)
                write("TOTAL: ".toByteArray(charset))
                write(CMD_DOUBLE_SIZE_ON)
                write((currencyFormat.format(ticket.totalAmount) + "\n\n").toByteArray(charset))
                write(CMD_NORMAL_SIZE)
                
                write(CMD_ALIGN_CENTER)
                write("Folio: $folio\n\n".toByteArray(charset))
                
                val internalCodeData = generarCodigoControlInterno(ticket.timestamp, folio, ticket.totalAmount)
                printQrCode(this, internalCodeData)
                
                // Imprimimos la cadena del QR en texto para que sea "legible" al ojo humano
                write(("\n$internalCodeData\n").toByteArray(charset))
                
                write("\n¡GRACIAS POR SU COMPRA!\n".toByteArray(charset))
                
                write(CMD_FEED_AND_CUT)
            }
            return printRawDataToDevice(deviceAddress, outputStream.toByteArray())
        } catch (e: Exception) {
            return PrintResult.Error("Error creando ticket: ${e.message}")
        }
    }
    
    suspend fun flushPrinter(): PrintResult {
        val deviceAddress = userPreferencesRepository.printerMacAddress.first() ?: return PrintResult.Error("No hay impresora configurada")
        return printRawDataToDevice(deviceAddress, "\n\n".toByteArray())
    }

    suspend fun printEtiqueta(etiqueta: EtiquetaProducto, quantity: Int): PrintResult {
        val deviceAddress = userPreferencesRepository.printerMacAddress.first() ?: return PrintResult.Error("No hay impresora configurada")
        val outputStream = ByteArrayOutputStream()
        try {
            repeat(quantity) {
                outputStream.apply {
                    write(CMD_INIT)
                    write(CMD_ALIGN_CENTER)
                    write(CMD_BOLD_ON)
                    write(CMD_DOUBLE_SIZE_ON)
                    write((etiqueta.nombre + "\n").toByteArray(Charsets.US_ASCII))
                    write(CMD_NORMAL_SIZE)
                    write(CMD_BOLD_OFF)
                    write((etiqueta.precio + "\n\n").toByteArray(Charsets.US_ASCII))
                    printBarcode(this, etiqueta.codigo, type = 73, height = 50)
                    write("\n\n".toByteArray())
                    write(CMD_FEED_AND_CUT)
                }
            }
            return printRawDataToDevice(deviceAddress, outputStream.toByteArray())
        } catch (e: Exception) { return PrintResult.Error("Error etiquetas") }
    }

    private fun printRawDataToDevice(deviceAddress: String, data: ByteArray): PrintResult {
        var socket: BluetoothSocket? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return PrintResult.Error("Sin permiso")
            }
            val device: BluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress) ?: return PrintResult.Error("No device")
            socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
            socket.connect() 
            socket.outputStream.apply {
                write(data)
                flush()
            }
            Thread.sleep(800)
            return PrintResult.Success
        } catch (e: Exception) {
            return PrintResult.Error("Error BT: ${e.message}")
        } finally {
            try { socket?.close() } catch (e: IOException) {}
        }
    }

    private fun printQrCode(outputStream: OutputStream, data: String) {
        val dataBytes = data.toByteArray(Charsets.ISO_8859_1)
        val pL = (dataBytes.size + 3) % 256
        val pH = (dataBytes.size + 3) / 256
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00)) 
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x04)) 
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31)) 
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, pL.toByte(), pH.toByte(), 0x31, 0x50, 0x30)) 
        outputStream.write(dataBytes)
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30)) 
    }

    private fun printBarcode(outputStream: OutputStream, data: String, type: Int, height: Int = 60) {
        val dataBytes = data.toByteArray(Charsets.US_ASCII)
        outputStream.write(byteArrayOf(0x1D, 0x68, height.toByte())) 
        outputStream.write(byteArrayOf(0x1D, 0x77, 2.toByte())) 
        outputStream.write(byteArrayOf(0x1D, 0x48, 2.toByte())) 
        outputStream.write(byteArrayOf(0x1D, 0x6B, type.toByte(), dataBytes.size.toByte()))
        outputStream.write(dataBytes)
    }

    private fun getMonoChromeData(bitmap: Bitmap): ByteArray { return byteArrayOf() }
}
