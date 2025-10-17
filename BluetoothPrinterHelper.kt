package com.example.carniceriapp.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.carniceriapp.model.EtiquetaProducto
import com.example.carniceriapp.model.TicketItemSnapshot
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class BluetoothPrinterHelper(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "BluetoothPrinterHelper"
        private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val PREFS_NAME = "CarniceriappPrefs"
        private const val PRINTER_ADDRESS_KEY = "printer_mac_address"

        // Comandos ESC/POS
        private val CMD_INIT: ByteArray = byteArrayOf(0x1B, 0x40)
        private val CMD_ALIGN_LEFT: ByteArray = byteArrayOf(0x1B, 0x61, 0)
        private val CMD_ALIGN_CENTER: ByteArray = byteArrayOf(0x1B, 0x61, 1)
        private val CMD_ALIGN_RIGHT: ByteArray = byteArrayOf(0x1B, 0x61, 2)
        private val CMD_BOLD_ON: ByteArray = byteArrayOf(0x1B, 0x45, 1)
        private val CMD_BOLD_OFF: ByteArray = byteArrayOf(0x1B, 0x45, 0)
        private val CMD_FONT_A: ByteArray = byteArrayOf(0x1B, 0x4D, 0)
        private val CMD_FONT_B: ByteArray = byteArrayOf(0x1B, 0x4D, 1)
        private val CMD_DOUBLE_SIZE_ON: ByteArray = byteArrayOf(0x1D, 0x21, 0x11)
        private val CMD_NORMAL_SIZE: ByteArray = byteArrayOf(0x1D, 0x21, 0x00)
    }

    fun savePrinterAddress(address: String) {
        prefs.edit().putString(PRINTER_ADDRESS_KEY, address).apply()
    }

    fun getSavedPrinterAddress(): String? {
        return prefs.getString(PRINTER_ADDRESS_KEY, null)
    }

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return bluetoothAdapter?.bondedDevices
    }

    fun printEtiquetas(deviceAddress: String, etiqueta: EtiquetaProducto, cantidad: Int): Boolean {
        val outputStream = ByteArrayOutputStream()
        val charset = Charsets.US_ASCII

        try {
            repeat(cantidad) {
                outputStream.write(CMD_INIT)
                outputStream.write(CMD_ALIGN_CENTER)
                
                outputStream.write(CMD_BOLD_ON)
                outputStream.write(CMD_DOUBLE_SIZE_ON)
                outputStream.write((etiqueta.nombre + "\n").toByteArray(charset))
                outputStream.write(CMD_NORMAL_SIZE)
                outputStream.write(CMD_BOLD_OFF)

                outputStream.write((etiqueta.precio + "\n\n").toByteArray(charset))
                
                printBarcode(outputStream, etiqueta.codigo, type = 73, width = 2)
                
                outputStream.write("\n\n\n".toByteArray(charset))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creando el stream de bytes para las etiquetas", e)
            return false
        }

        return printRawDataToDevice(deviceAddress, outputStream.toByteArray())
    }

    fun printTicket(
        deviceAddress: String,
        items: List<TicketItemSnapshot>,
        total: Double,
        folio: String,
        fecha: String,
        internalControlBarcodeData: String,
        humanReadableInternalCode: String
    ): Boolean {
        val outputStream = ByteArrayOutputStream()
        val localeMexico = Locale.forLanguageTag("es-MX")
        val currencyFormat = NumberFormat.getCurrencyInstance(localeMexico)
        val charset = Charsets.ISO_8859_1

        try {
            outputStream.write(CMD_INIT)
            outputStream.write(CMD_ALIGN_CENTER)
            outputStream.write("CARNICERIA APP\n".toByteArray(charset))
            outputStream.write("$fecha\n".toByteArray(charset))
            outputStream.write("--------------------------------\n".toByteArray(charset))
            
            outputStream.write(CMD_ALIGN_LEFT)

            items.forEach { item ->
                outputStream.write(CMD_BOLD_ON)
                outputStream.write((item.nombreProducto + "\n").toByteArray(charset))
                outputStream.write(CMD_BOLD_OFF)

                val cantidadStr = item.gramajeFormateado
                val montoItemStr = currencyFormat.format(item.montoVentaItem)
                val spacesNeeded = 32 - cantidadStr.length - montoItemStr.length
                val itemLine = if (spacesNeeded > 0) {
                    cantidadStr + " ".repeat(spacesNeeded) + montoItemStr
                } else {
                    (cantidadStr + " " + montoItemStr).take(32)
                }
                outputStream.write((itemLine + "\n").toByteArray(charset))

                val barcodeData = item.codigoParaPOS?.takeIf { it.isNotBlank() } ?: item.codigoProducto
                if (barcodeData.isNotBlank()) {
                    outputStream.write(CMD_ALIGN_CENTER)
                    printBarcode(outputStream, barcodeData, type = 73, width = 2)
                    outputStream.write(CMD_ALIGN_LEFT)
                }
                outputStream.write("\n".toByteArray(charset))
            }

            outputStream.write("--------------------------------\n".toByteArray(charset))
            outputStream.write(CMD_ALIGN_RIGHT)
            outputStream.write("TOTAL: ".toByteArray(charset))
            outputStream.write(CMD_DOUBLE_SIZE_ON)
            outputStream.write((currencyFormat.format(total) + "\n").toByteArray(charset))
            outputStream.write(CMD_NORMAL_SIZE)
            
            outputStream.write(CMD_ALIGN_CENTER)
            outputStream.write("\n".toByteArray(charset))
            outputStream.write("Folio: $folio\n".toByteArray(charset))
            outputStream.write("\n".toByteArray(charset))
            
            // Imprimir código de control interno
            outputStream.write(CMD_FONT_B)
            outputStream.write("Codigo para control interno\n".toByteArray(charset))
            outputStream.write(CMD_FONT_A)

            // Imprimir el código de control como QR
            printQrCode(outputStream, internalControlBarcodeData)
            
            outputStream.write(CMD_ALIGN_CENTER)
            outputStream.write((humanReadableInternalCode + "\n\n").toByteArray(charset))

            outputStream.write("¡GRACIAS POR SU COMPRA!\n".toByteArray(charset))
            outputStream.write("\n\n\n".toByteArray(charset))

        } catch (e: Exception) {
            Log.e(TAG, "Error creando el stream de bytes para el ticket", e)
            return false
        }

        return printRawDataToDevice(deviceAddress, outputStream.toByteArray())
    }

    private fun printQrCode(outputStream: OutputStream, data: String) {
        val charset = Charsets.ISO_8859_1
        val dataBytes = data.toByteArray(charset)

        // 1. Set QR Code Model to Model 2 (the most common)
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))

        // 2. Set QR Code Module Size (dot size). 4 is a good balance.
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x04))

        // 3. Set QR Code Error Correction Level to M (Medium)
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31))

        // 4. Store QR Code Data in the symbol storage area
        val pL = (dataBytes.size + 3) % 256
        val pH = (dataBytes.size + 3) / 256
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, pL.toByte(), pH.toByte(), 0x31, 0x50, 0x30))
        outputStream.write(dataBytes)

        // 5. Print the QR Code from the symbol storage area
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))
    }

    private fun printBarcode(outputStream: OutputStream, data: String, type: Int, width: Int = 2, height: Int = 60, printHumanReadable: Boolean = true) {
        val charset = Charsets.US_ASCII
        
        outputStream.write(byteArrayOf(0x1D, 0x68, height.toByte())) // Height
        outputStream.write(byteArrayOf(0x1D, 0x77, width.toByte())) // Width
        outputStream.write(byteArrayOf(0x1D, 0x48, if(printHumanReadable) 2 else 0)) // HRI
        outputStream.write(byteArrayOf(0x1D, 0x6B, type.toByte(), data.length.toByte()))
        outputStream.write(data.toByteArray(charset))
    }
    
    private fun printRawDataToDevice(deviceAddress: String, data: ByteArray): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        if (!isBluetoothEnabled()) {
            return false
        }

        val device: BluetoothDevice? = try {
            bluetoothAdapter?.getRemoteDevice(deviceAddress)
        } catch (e: IllegalArgumentException) {
            return false
        }

        if (device == null) {
            return false
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
            socket.connect()
            outputStream = socket.outputStream
            outputStream.write(data)
            outputStream.flush()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error al imprimir: ${e.message}")
            return false
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error al cerrar recursos: ${e.message}")
            }
        }
    }
}
