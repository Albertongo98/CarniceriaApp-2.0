package com.example.carniceriaapp20.util

import com.example.carniceriaapp20.data.local.DaySalesReport
import com.example.carniceriaapp20.data.local.DepartmentSalesReport
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductSalesReport
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.ui.screens.tpv.CartItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketPrintFormatterTest {

    private val defaultLocale = Locale.getDefault()

    @Before
    fun setUp() {
        // La tablet real usa coma decimal: el ticket no debe depender de eso.
        Locale.setDefault(Locale("es", "ES"))
    }

    @After
    fun tearDown() = Locale.setDefault(defaultLocale)

    private val timestamp = 1_700_000_000_000L
    private val ticket = Ticket(id = 1, timestamp = timestamp, totalAmount = 100.0, dailyFolio = 42)

    private fun unidad(code: String = "1090", qty: Double, price: Double = 10.0) =
        CartItem(product = Product(code, "Chorizo", price, "Embutidos", ProductUnit.UNIDAD), quantity = qty)

    private fun granel(code: String = "1090", qty: Double, total: Double? = null) =
        CartItem(product = Product(code, "Bistec", 180.0, "Carniceria", ProductUnit.GRANEL), quantity = qty, customPrice = total)

    private fun build(vararg items: CartItem, t: Ticket = ticket): ByteArray =
        TicketPrintFormatter.buildTicket(t, items.toList(), "042")

    private fun ByteArray.text() = String(this, Charsets.ISO_8859_1)

    private fun ByteArray.count(pattern: ByteArray): Int {
        var found = 0
        var i = 0
        while (i <= size - pattern.size) {
            if (pattern.indices.all { this[i + it] == pattern[it] }) { found++; i += pattern.size } else i++
        }
        return found
    }

    private fun ByteArray.has(pattern: ByteArray) = count(pattern) > 0

    // GS h (alto) + GS w 2 + GS H (HRI) + GS k 73 (CODE128) + longitud, seguido de "{B" + datos
    private fun barcode(data: String, height: Int, hri: Int): ByteArray {
        val payload = "{B$data".toByteArray(Charsets.US_ASCII)
        return byteArrayOf(0x1D, 0x68, height.toByte(), 0x1D, 0x77, 2, 0x1D, 0x48, hri.toByte(), 0x1D, 0x6B, 73, payload.size.toByte()) + payload
    }

    private val gsK73 = byteArrayOf(0x1D, 0x6B, 73)

    @Test
    fun `el ticket termina con el codigo de control HHmmss-folio sin texto HRI`() {
        val time = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date(timestamp))

        val bytes = build(unidad(qty = 1.0))

        assertTrue(bytes.has(barcode("$time-042", height = 70, hri = 0)))
        assertTrue(bytes.text().contains("Folio: 042"))
    }

    @Test
    fun `cada producto lleva su codigo de barras, el granel con el precio embebido`() {
        val bytes = build(unidad(code = "1090", qty = 1.0), granel(code = "2001", qty = 0.5, total = 90.0))

        assertTrue(bytes.has(barcode("1090", height = 45, hri = 2)))
        assertTrue(bytes.has(barcode(generarCodigoParaPOS("2001", 90.0), height = 45, hri = 2)))
        assertEquals(3, bytes.count(gsK73)) // 2 productos + el de control
    }

    @Test
    fun `tras cada codigo de barras se vuelve a alinear a la izquierda`() {
        val bytes = build(unidad(code = "1090", qty = 1.0))

        assertTrue(bytes.has(barcode("1090", height = 45, hri = 2) + TicketPrintFormatter.CMD_ALIGN_LEFT))
    }

    @Test
    fun `banner de piezas solo para unidades con cantidad mayor o igual a 2`() {
        assertTrue(build(unidad(qty = 3.0)).text().contains(">> 3 PIEZAS <<"))
        assertFalse(build(unidad(qty = 1.0)).text().contains("PIEZAS"))
        assertFalse(build(granel(qty = 3.5)).text().contains("PIEZAS"))
    }

    @Test
    fun `el banner de piezas va en tamano doble y negritas`() {
        val bytes = build(unidad(qty = 5.0))

        val banner = TicketPrintFormatter.CMD_ALIGN_CENTER + TicketPrintFormatter.CMD_BOLD_ON +
            TicketPrintFormatter.CMD_DOUBLE_SIZE_ON + ">> 5 PIEZAS <<\n".toByteArray()
        assertTrue(bytes.has(banner))
    }

    @Test
    fun `un renglon sin codigo no imprime codigo de barras pero el ticket conserva el de control`() {
        val bytes = build(unidad(code = "", qty = 1.0))

        assertEquals(1, bytes.count(gsK73))
    }

    @Test
    fun `los kilos usan punto decimal aunque el dispositivo use coma`() {
        val text = build(granel(qty = 0.75, total = 135.0)).text()

        assertTrue(text, text.contains("0.750 kg"))
        assertFalse(text.contains("0,750"))
    }

    @Test
    fun `un ticket anulado lo indica con su motivo y uno normal no`() {
        val voided = ticket.copy(voidedAt = timestamp + 1, voidReason = "Error de captura")

        val voidedText = build(unidad(qty = 1.0), t = voided).text()

        assertTrue(voidedText.contains("*** ANULADO ***"))
        assertTrue(voidedText.contains("Motivo: Error de captura"))
        assertFalse(build(unidad(qty = 1.0)).text().contains("ANULADO"))
    }

    @Test
    fun `el corte de caja lleva periodo, tickets y ventas por dia solo si hay varios dias`() {
        val dept = listOf(DepartmentSalesReport("Carniceria", 500.0, 0.0, 2.5))
        val prod = listOf(ProductSalesReport("Bistec", "1", 500.0, 2.5, "Carniceria", ProductUnit.GRANEL))
        val day1 = DaySalesReport(timestamp, 3, 200.0)
        val day2 = DaySalesReport(timestamp + 86_400_000L, 4, 300.0)

        val multi = TicketPrintFormatter.buildSalesReport("01/09/2026 - 30/09/2026", 500.0, 7, dept, prod, listOf(day1, day2)).text()
        val single = TicketPrintFormatter.buildSalesReport("18/09/2026", 500.0, 7, dept, prod, listOf(day1)).text()

        assertTrue(multi.contains("01/09/2026 - 30/09/2026"))
        assertTrue(multi.contains("Tickets: 7"))
        assertTrue(multi.contains("VENTAS POR DIA"))
        assertTrue(multi.contains("2.500kg"))
        assertFalse(single.contains("VENTAS POR DIA"))
    }

    @Test
    fun `la lista de resurtido agrupa por departamento e indica cuanto falta`() {
        val bistec = Product("1", "Bistec de Res", 189.0, "Carniceria", ProductUnit.GRANEL, stock = 2.5, minStock = 10.0)
        val sal = Product("2", "Sal 1 kg", 10.0, "Abarrotes", ProductUnit.UNIDAD, stock = 3.0, minStock = 5.0)
        val pollo = Product("3", "Pechuga", 119.0, "Carniceria", ProductUnit.GRANEL, stock = 0.0, minStock = 10.0)

        val text = TicketPrintFormatter.buildRestockList(listOf(bistec, sal, pollo), timestamp).text()

        assertTrue(text.contains("LISTA DE RESURTIDO"))
        assertTrue(text.indexOf("-- ABARROTES --") < text.indexOf("-- CARNICERIA --")) // orden por departamento
        assertTrue(text.contains("[ ] BISTEC DE RES"))
        assertTrue(text.contains("Hay: 2.5 kg  Min: 10 kg"))
        assertTrue(text.contains("Faltan: 7.5 kg"))
        assertTrue(text.contains("Faltan: 2 pz"))
        assertTrue(text.contains("** SIN EXISTENCIA **")) // la pechuga
        assertTrue(text.contains("Productos por resurtir: 3"))
        assertFalse(text.contains("2,5"))
    }

    @Test
    fun `una lista de resurtido vacia lo dice en vez de salir en blanco`() {
        assertTrue(TicketPrintFormatter.buildRestockList(emptyList(), timestamp).text().contains("No hay productos por resurtir"))
    }

    @Test
    fun `las etiquetas se repiten la cantidad pedida`() {
        val etiqueta = com.example.carniceriaapp20.data.model.EtiquetaProducto("Bistec", "$180.00", "1090")

        val bytes = TicketPrintFormatter.buildLabels(etiqueta, 3)

        assertEquals(3, bytes.count(TicketPrintFormatter.CMD_INIT))
    }
}
