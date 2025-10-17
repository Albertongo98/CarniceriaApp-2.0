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
        private val CMD_FONT_A: ByteArray = byteArrayOf(0x1B, 0x4D, 0)
        private val CMD_FONT_B: ByteArray = byteArrayOf(0x1B, 0x4D, 1)
        private val CMD_DOUBLE_SIZE_ON: ByteArray = byteArrayOf(0x1D, 0x21, 0x11) // Double height and width
        private val CMD_NORMAL_SIZE: ByteArray = byteArrayOf(0x1D, 0x21, 0x00)
        private val CMD_CUT: ByteArray = byteArrayOf(0x1D, 0x56, 1) // Partial cut
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return bluetoothAdapter?.bondedDevices
    }

    suspend fun savePrinterAddress(address: String) {
        userPreferencesRepository.savePrinterMacAddress(address)
    }

    private suspend fun getSavedPrinterAddress(): String? = userPreferencesRepository.printerMacAddress.first()

    suspend fun printTicket(ticket: Ticket, items: List<CartItem>, folio: String, withLogo: Boolean = true): PrintResult {
        val deviceAddress = getSavedPrinterAddress() ?: return PrintResult.Error("No hay impresora configurada")
        val charset = Charsets.ISO_8859_1
        val localeMexico = Locale.forLanguageTag("es-MX")
        val currencyFormat = NumberFormat.getCurrencyInstance(localeMexico)

        try {
            val outputStream = ByteArrayOutputStream().apply {
                write(CMD_INIT)
                
                if (withLogo) {
                    try {
                        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo_ticket)
                        if(logoBitmap != null) {
                            val logoData = getMonoChromeData(logoBitmap)
                            write(CMD_ALIGN_CENTER)
                            write(logoData)
                            write("\n".toByteArray())
                        } else {
                             Log.e(TAG, "logo_ticket.png no encontrado o no se pudo decodificar.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "No se pudo cargar o imprimir el logo", e)
                    }
                }

                write(CMD_ALIGN_CENTER)
                write("--------------------------------\n".toByteArray(charset))
                
                write(CMD_ALIGN_LEFT)
                items.forEach { item ->
                    val nameAndCode = "${item.product.name} (${item.product.code})"
                    write(CMD_BOLD_ON)
                    write((nameAndCode + "\n").toByteArray(charset))
                    write(CMD_BOLD_OFF)

                    if (item.product.unit == ProductUnit.UNIDAD && item.quantity.toInt() > 1) {
                        write(CMD_BOLD_ON)
                        write(CMD_DOUBLE_SIZE_ON)
                        write(CMD_ALIGN_CENTER)
                        write((">> ${item.quantity.toInt()} PIEZAS <<\n").toByteArray(charset))
                        write(CMD_NORMAL_SIZE)
                        write(CMD_BOLD_OFF)
                        write(CMD_ALIGN_LEFT) // Reset alignment
                    }

                    val cantidadStr = if (item.product.unit == ProductUnit.GRANEL) "%.3f kg".format(item.quantity) else "${item.quantity.toInt()} un."
                    val montoItemStr = currencyFormat.format(item.totalPrice)
                    val spaces = 32 - cantidadStr.length - montoItemStr.length
                    val itemLine = if (spaces > 0) cantidadStr + " ".repeat(spaces) + montoItemStr else (cantidadStr + " " + montoItemStr).take(32)
                    write((itemLine + "\n").toByteArray(charset))
                    
                    if (item.product.unit == ProductUnit.GRANEL && item.totalPrice > 0) {
                        try {
                            val barcodeData = generarCodigoParaPOS(item.product.code, item.totalPrice)
                            write(CMD_ALIGN_CENTER)
                            printBarcode(this, barcodeData, type = 73, height=50)
                            write(CMD_ALIGN_LEFT)
                            write("\n".toByteArray())
                        } catch (e: Exception) {
                             Log.e(TAG, "Error generando o imprimiendo código de barras para ${item.product.code}", e)
                        }
                    } else if (item.product.unit == ProductUnit.UNIDAD) {
                        try {
                            write(CMD_ALIGN_CENTER)
                            printBarcode(this, item.product.code, type = 73, height=50)
                            write(CMD_ALIGN_LEFT)
                            write("\n".toByteArray())
                        } catch (e: Exception) {
                            Log.e(TAG, "Error imprimiendo código de barras para ${item.product.code}", e)
                        }
                    }
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
                write("Control Interno\n".toByteArray(charset))
                printQrCode(this, internalCodeData)
                write((internalCodeData + "\n\n").toByteArray(charset))

                write("¡GRACIAS POR SU COMPRA!\n\n\n".toByteArray(charset))
                write(CMD_CUT)
            }
            return printRawDataToDevice(deviceAddress, outputStream.toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "Error creando el stream de bytes para el ticket", e)
            return PrintResult.Error("Error generando datos del ticket: ${e.message}")
        }
    }
    
    suspend fun flushPrinter(): PrintResult {
        val deviceAddress = getSavedPrinterAddress() ?: return PrintResult.Error("No hay impresora configurada")
        val flushData = "\n".toByteArray(Charsets.US_ASCII)
        return printRawDataToDevice(deviceAddress, flushData)
    }

    suspend fun printEtiqueta(etiqueta: EtiquetaProducto, quantity: Int): PrintResult {
        val deviceAddress = getSavedPrinterAddress() ?: return PrintResult.Error("No hay impresora configurada")
        val charset = Charsets.US_ASCII
        val outputStream = ByteArrayOutputStream()
        try {
            repeat(quantity) {
                outputStream.apply {
                    write(CMD_INIT)
                    write(CMD_ALIGN_CENTER)
                    
                    write(CMD_BOLD_ON)
                    write(CMD_DOUBLE_SIZE_ON)
                    write((etiqueta.nombre + "\n").toByteArray(charset))
                    write(CMD_NORMAL_SIZE)
                    write(CMD_BOLD_OFF)

                    write((etiqueta.precio + "\n\n").toByteArray(charset))
                    
                    val barcodeData = etiqueta.codigo
                    printBarcode(this, barcodeData, type = 73, height = 50)
                    
                    write("\n\n\n".toByteArray(charset))
                    write(CMD_CUT)
                }
            }
            return printRawDataToDevice(deviceAddress, outputStream.toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "Error creando el stream de bytes para las etiquetas", e)
            return PrintResult.Error("Error generando datos de etiqueta: ${e.message}")
        }
    }

    private fun printRawDataToDevice(deviceAddress: String, data: ByteArray): PrintResult {
        var socket: BluetoothSocket? = null
        try {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return PrintResult.Error("Sin permiso BLUETOOTH_CONNECT")
            }
            val device: BluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress) ?: return PrintResult.Error("Dispositivo no encontrado")
            socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
            socket.connect() 
            val outputStream: OutputStream = socket.outputStream
            outputStream.write(data)
            outputStream.flush()
            Thread.sleep(200) // Crucial pause to allow data to be sent before closing the socket.
            return PrintResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error al imprimir", e)
            return PrintResult.Error("Fallo al conectar o escribir en impresora: ${e.message}")
        } finally {
             try {
                socket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error al cerrar el socket de impresión", e)
            }
        }
    }

    private fun printQrCode(outputStream: OutputStream, data: String) {
        val dataBytes = data.toByteArray(Charsets.ISO_8859_1)
        val pL = (dataBytes.size + 3) % 256
        val pH = (dataBytes.size + 3) / 256

        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00)) // Model
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x03)) // Size
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31)) // Error Correction
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, pL.toByte(), pH.toByte(), 0x31, 0x50, 0x30)) // Store data
        outputStream.write(dataBytes)
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30)) // Print
    }

    private fun printBarcode(outputStream: OutputStream, data: String, type: Int, height: Int = 60) {
        val dataBytes = data.toByteArray(Charsets.US_ASCII)
        outputStream.write(byteArrayOf(0x1D, 0x68, height.toByte())) // Height
        outputStream.write(byteArrayOf(0x1D, 0x77, 2.toByte())) // Width
        outputStream.write(byteArrayOf(0x1D, 0x48, 2.toByte())) // HRI (Human Readable) position below barcode
        
        outputStream.write(byteArrayOf(0x1D, 0x6B, type.toByte(), dataBytes.size.toByte()))
        outputStream.write(dataBytes)
    }

    private fun getMonoChromeData(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        val command = byteArrayOf(0x1D, 0x76, 0x30, 0)
        stream.write(command)

        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        stream.write(widthBytes % 256) // xL
        stream.write(widthBytes / 256) // xH
        stream.write(height % 256) // yL
        stream.write(height / 256) // yH

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val data = ByteArray(widthBytes * height)
        var dataIndex = 0
        for (y in 0 until height) {
            for (x in 0 until widthBytes) {
                var slice: Byte = 0
                for (b in 0..7) {
                    val i = y * width + (x * 8 + b)
                    if (i < pixels.size) {
                        val pixel = pixels[i]
                        val luminance = (0.299 * ((pixel shr 16) and 0xFF) + 
                                       0.587 * ((pixel shr 8) and 0xFF) + 
                                       0.114 * (pixel and 0xFF))
                        if (luminance < 128) {
                            slice = (slice.toInt() or (1 shl (7 - b))).toByte()
                        }
                    }
                }
                data[dataIndex++] = slice
            }
        }
        stream.write(data)
        return stream.toByteArray()
    }
}