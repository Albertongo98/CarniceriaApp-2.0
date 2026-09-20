package com.example.carniceriaapp20.util

import com.example.carniceriaapp20.data.local.DaySalesReport
import com.example.carniceriaapp20.data.local.DepartmentSalesReport
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductSalesReport
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.local.isVoided
import com.example.carniceriaapp20.data.model.EtiquetaProducto
import com.example.carniceriaapp20.ui.screens.tpv.CartItem
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppConfig {
    const val BUSINESS_NAME = "LA PALMA CARNICERIA"
}

/**
 * Arma los bytes ESC/POS (impresora térmica de 58 mm, 32 columnas, ISO-8859-1) de tickets, cortes
 * de caja y etiquetas. Es una clase pura (sin Android ni Bluetooth) para poder probar el formato en
 * la JVM; el envío por Bluetooth vive en [BluetoothPrinterHelper].
 */
object TicketPrintFormatter {

    private const val LINE = "--------------------------------"
    private const val WIDTH = 32
    private val charset = Charsets.ISO_8859_1
    private val mx: Locale = Locale.forLanguageTag("es-MX")

    // ESC/POS
    internal val CMD_INIT = byteArrayOf(0x1B, 0x40)
    internal val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0)
    internal val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 1)
    internal val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 2)
    internal val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 1)
    internal val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0)
    internal val CMD_DOUBLE_SIZE_ON = byteArrayOf(0x1D, 0x21, 0x11)
    internal val CMD_NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00)
    internal val CMD_FEED_PAPER = byteArrayOf(0x1B, 0x64, 5)

    private fun OutputStream.text(value: String) = write(value.toByteArray(charset))

    /** Texto a la izquierda y monto a la derecha en una línea de 32 columnas. */
    private fun leftRight(left: String, right: String): String {
        val spaces = WIDTH - left.length - right.length
        return if (spaces > 0) left + " ".repeat(spaces) + right else "$left $right".take(WIDTH)
    }

    /** GS k 73 (CODE128) con el subconjunto B ("{B"). [hriPosition]: 0 = sin texto, 2 = texto abajo. */
    internal fun barcode128(out: OutputStream, data: String, height: Int, hriPosition: Int) {
        val dataBytes = (if (data.startsWith("{")) data else "{B$data").toByteArray(Charsets.US_ASCII)
        out.write(byteArrayOf(0x1D, 0x68, height.toByte()))
        out.write(byteArrayOf(0x1D, 0x77, 2))
        out.write(byteArrayOf(0x1D, 0x48, hriPosition.toByte()))
        out.write(byteArrayOf(0x1D, 0x6B, 73, dataBytes.size.toByte()))
        out.write(dataBytes)
    }

    fun buildTicket(ticket: Ticket, items: List<CartItem>, folio: String): ByteArray {
        val money = NumberFormat.getCurrencyInstance(mx)
        val fullDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", mx)

        return ByteArrayOutputStream().apply {
            write(CMD_INIT)
            write(CMD_ALIGN_CENTER)
            text("${AppConfig.BUSINESS_NAME}\n")
            if (ticket.isVoided) {
                write(CMD_BOLD_ON)
                write(CMD_DOUBLE_SIZE_ON)
                text("*** ANULADO ***\n")
                write(CMD_NORMAL_SIZE)
                write(CMD_BOLD_OFF)
                ticket.voidReason?.takeIf { it.isNotBlank() }?.let { text("Motivo: ${it.take(WIDTH - 8)}\n") }
            }
            text("$LINE\n")
            text("${fullDate.format(Date(ticket.timestamp))}\n")
            text("$LINE\n")

            write(CMD_ALIGN_LEFT)
            items.forEach { item ->
                write(CMD_BOLD_ON)
                val name = item.product.name.uppercase().take(20)
                text("$name (${item.product.code.padStart(4, '0')})\n")
                write(CMD_BOLD_OFF)

                // Aviso grande para que el cajero que escanea sepa de un vistazo cuántas piezas son.
                if (item.product.unit == ProductUnit.UNIDAD && item.quantity >= 2) {
                    write(CMD_ALIGN_CENTER)
                    write(CMD_BOLD_ON)
                    write(CMD_DOUBLE_SIZE_ON)
                    text(">> ${item.quantity.toInt()} PIEZAS <<\n")
                    write(CMD_NORMAL_SIZE)
                    write(CMD_BOLD_OFF)
                    write(CMD_ALIGN_LEFT)
                }

                val quantity = if (item.product.unit == ProductUnit.GRANEL) "%.3f kg".format(mx, item.quantity) else "${item.quantity.toInt()} un."
                text("${leftRight(quantity, money.format(item.totalPrice))}\n")

                // Sin código (venta vieja cuyo producto ya no existe) no hay nada que codificar. Cualquier otro
                // fallo al armar el código de barras debe abortar la impresión (PrintResult.Error), no imprimirse sin él.
                if (item.product.code.isNotBlank()) {
                    write(CMD_ALIGN_CENTER)
                    val code = if (item.product.unit == ProductUnit.GRANEL) generarCodigoParaPOS(item.product.code, item.totalPrice) else item.product.code
                    barcode128(this, code, height = 45, hriPosition = 2)
                    write(CMD_ALIGN_LEFT)
                    text("\n")
                }
            }

            text("$LINE\n")
            text("TOTAL: ")
            write(CMD_DOUBLE_SIZE_ON)
            text("${money.format(ticket.totalAmount)}\n\n")
            write(CMD_NORMAL_SIZE)
            write(CMD_ALIGN_CENTER)
            text("Folio: $folio\n\n")

            // Código de control que lee el sistema de control de tickets: HHmmss-folio.
            val time = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date(ticket.timestamp))
            barcode128(this, "$time-$folio", height = 70, hriPosition = 0)

            text("\n¡GRACIAS POR SU COMPRA!\n")
            write(CMD_FEED_PAPER)
        }.toByteArray()
    }

    fun buildSalesReport(
        period: String,
        total: Double,
        ticketCount: Int,
        deptSales: List<DepartmentSalesReport>,
        prodSales: List<ProductSalesReport>,
        dailySales: List<DaySalesReport>
    ): ByteArray {
        val money = NumberFormat.getCurrencyInstance(mx)

        return ByteArrayOutputStream().apply {
            write(CMD_INIT)
            write(CMD_ALIGN_CENTER)
            write(CMD_BOLD_ON)
            text("CORTE DE CAJA DETALLADO\n")
            write(CMD_NORMAL_SIZE)
            text("${AppConfig.BUSINESS_NAME}\n")
            write(CMD_BOLD_OFF)
            text("$period\n")
            text("Tickets: $ticketCount\n")
            if (ticketCount > 0) text("Ticket prom.: ${money.format(total / ticketCount)}\n")
            text("$LINE\n")

            write(CMD_ALIGN_LEFT)
            deptSales.forEach { dept ->
                write(CMD_BOLD_ON)
                text("DEP: ${dept.department.uppercase()}\n")
                write(CMD_BOLD_OFF)

                prodSales.filter { it.department == dept.department }.forEach { prod ->
                    val qty = if (prod.totalQuantity % 1 == 0.0) prod.totalQuantity.toInt().toString() else "%.3f".format(mx, prod.totalQuantity)
                    val unit = if (prod.unit == ProductUnit.GRANEL) "kg" else "pz"
                    text(" ${prod.productName.take(15)} $qty$unit ${money.format(prod.totalAmount)}".take(WIDTH) + "\n")
                }
                text(" Subtotal: ${money.format(dept.totalAmount)}\n")
                text("$LINE\n")
            }

            if (dailySales.size > 1) {
                val dayFormat = SimpleDateFormat("dd/MM", mx)
                write(CMD_BOLD_ON)
                text("VENTAS POR DIA\n")
                write(CMD_BOLD_OFF)
                dailySales.forEach { day ->
                    text("${leftRight("${dayFormat.format(Date(day.dayStart))} ${day.tickets}tk", money.format(day.total))}\n")
                }
                text("$LINE\n")
            }

            write(CMD_ALIGN_RIGHT)
            write(CMD_BOLD_ON)
            text("TOTAL VENTA: ")
            write(CMD_DOUBLE_SIZE_ON)
            text("${money.format(total)}\n")
            write(CMD_NORMAL_SIZE)
            write(CMD_BOLD_OFF)

            write(CMD_ALIGN_CENTER)
            text("\n¡CONTROL DE VENTAS EXITOSO!\n")
            write(CMD_FEED_PAPER)
        }.toByteArray()
    }

    /**
     * Lista para salir a comprar: productos en o bajo su inventario mínimo, por departamento, con casilla
     * para palomear, lo que hay, el mínimo y cuánto falta para llegar al mínimo.
     */
    fun buildRestockList(products: List<Product>, printedAt: Long): ByteArray {
        val dateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", mx).format(Date(printedAt))

        return ByteArrayOutputStream().apply {
            write(CMD_INIT)
            write(CMD_ALIGN_CENTER)
            write(CMD_BOLD_ON)
            text("LISTA DE RESURTIDO\n")
            write(CMD_BOLD_OFF)
            text("${AppConfig.BUSINESS_NAME}\n")
            text("$dateTime\n")
            text("$LINE\n")

            write(CMD_ALIGN_LEFT)
            if (products.isEmpty()) {
                text("No hay productos por resurtir\n")
            }
            products.sortedWith(compareBy({ it.department.lowercase() }, { it.name.lowercase() }))
                .groupBy { it.department }
                .forEach { (department, items) ->
                    write(CMD_BOLD_ON)
                    text("-- ${department.uppercase()} --\n")
                    write(CMD_BOLD_OFF)
                    items.forEach { product ->
                        val unit = if (product.unit == ProductUnit.GRANEL) "kg" else "pz"
                        val stock = product.stock ?: 0.0
                        text("[ ] ${product.name.uppercase()}".take(WIDTH) + "\n")
                        if (stock <= 0.0) {
                            write(CMD_BOLD_ON)
                            text("    ** SIN EXISTENCIA **\n")
                            write(CMD_BOLD_OFF)
                        } else {
                            text("    Hay: ${formatPlain(stock)} $unit  Min: ${formatPlain(product.minStock)} $unit".take(WIDTH) + "\n")
                        }
                        if (product.minStock > stock) {
                            text("    Faltan: ${formatPlain(product.minStock - stock)} $unit\n")
                        }
                    }
                    text("$LINE\n")
                }

            write(CMD_ALIGN_CENTER)
            text("Productos por resurtir: ${products.size}\n")
            write(CMD_FEED_PAPER)
        }.toByteArray()
    }

    fun buildLabels(etiqueta: EtiquetaProducto, quantity: Int): ByteArray = ByteArrayOutputStream().apply {
        repeat(quantity) {
            write(CMD_INIT)
            write(CMD_ALIGN_CENTER)
            text("${etiqueta.nombre}\n")
            text("${etiqueta.precio}\n")
            write(CMD_FEED_PAPER)
        }
    }.toByteArray()
}
