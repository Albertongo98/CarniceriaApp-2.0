package com.example.carniceriaapp20.util

import android.content.Context
import android.net.Uri
import com.example.carniceriaapp20.data.local.DaySalesReport
import com.example.carniceriaapp20.data.local.DepartmentSalesReport
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductSalesReport
import com.example.carniceriaapp20.data.local.ProductUnit
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object ReportExporter {

    private val mx: Locale = Locale.forLanguageTag("es-MX")

    private fun qty(value: Double): String =
        if (value % 1 == 0.0) value.toInt().toString() else "%.3f".format(mx, value)

    private fun String.esc(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    fun formatPeriod(startDate: Long, endDate: Long): String {
        val fmt = SimpleDateFormat("dd/MM/yyyy", mx)
        return if (startDate == endDate) fmt.format(Date(startDate))
        else "${fmt.format(Date(startDate))} - ${fmt.format(Date(endDate))}"
    }

    fun generateHtmlReport(
        startDate: Long,
        endDate: Long,
        total: Double,
        ticketCount: Int,
        deptSales: List<DepartmentSalesReport>,
        prodSales: List<ProductSalesReport>,
        dailySales: List<DaySalesReport>
    ): String {
        val money = NumberFormat.getCurrencyInstance(mx)
        val dayFmt = SimpleDateFormat("EEEE dd/MM/yyyy", mx)
        val isSingleDay = startDate == endDate
        val avgTicket = if (ticketCount > 0) total / ticketCount else 0.0
        val bestDay = dailySales.maxByOrNull { it.total }

        val kpis = buildString {
            append("<p><strong>${if (isSingleDay) "Fecha" else "Periodo"}:</strong> ${formatPeriod(startDate, endDate)}</p>")
            append("<p><strong>Venta total${if (isSingleDay) " del día" else " del periodo"}:</strong> <span class=\"total\">${money.format(total)}</span></p>")
            append("<p><strong>Tickets:</strong> $ticketCount &nbsp;&nbsp; <strong>Ticket promedio:</strong> ${money.format(avgTicket)}</p>")
            if (!isSingleDay && dailySales.isNotEmpty()) {
                append("<p><strong>Días con venta:</strong> ${dailySales.size} &nbsp;&nbsp; <strong>Promedio por día con venta:</strong> ${money.format(total / dailySales.size)}</p>")
                bestDay?.let { append("<p><strong>Mejor día:</strong> ${dayFmt.format(Date(it.dayStart))} (${money.format(it.total)})</p>") }
            }
        }

        val dailyTable = if (isSingleDay || dailySales.isEmpty()) "" else buildString {
            append("<h2>Ventas por Día</h2><table><thead><tr><th>Día</th><th>Tickets</th><th>Total</th><th>Ticket promedio</th></tr></thead><tbody>")
            dailySales.forEach {
                append("<tr><td>${dayFmt.format(Date(it.dayStart))}</td><td>${it.tickets}</td><td>${money.format(it.total)}</td><td>${money.format(it.total / it.tickets)}</td></tr>")
            }
            append("<tr class='dept-row'><td>Total</td><td>$ticketCount</td><td>${money.format(total)}</td><td>${money.format(avgTicket)}</td></tr>")
            append("</tbody></table>")
        }

        val deptRows = deptSales.joinToString("") {
            val piezas = if (it.totalPieces > 0) qty(it.totalPieces) else "-"
            val kilos = if (it.totalKilos > 0) "%.3f".format(mx, it.totalKilos) else "-"
            "<tr><td>${it.department.esc()}</td><td>$piezas</td><td>$kilos</td><td>${money.format(it.totalAmount)}</td></tr>"
        }

        val productRows = prodSales.groupBy { it.department }.entries.joinToString("") { (dept, products) ->
            "<tr class='dept-row'><td colspan='3'>${dept.esc()}</td></tr>" + products.joinToString("") { p ->
                val unitStr = if (p.effectiveUnit == ProductUnit.GRANEL) "kg" else "pz"
                "<tr><td>${p.productName.esc()}</td><td>${qty(p.totalQuantity)} $unitStr</td><td>${money.format(p.totalAmount)}</td></tr>"
            }
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: sans-serif; margin: 20px; color: #333; }
                    h1 { color: #1B5E20; border-bottom: 2px solid #1B5E20; padding-bottom: 10px; }
                    .header { background: #e8f5e9; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
                    .total { font-size: 24px; font-weight: bold; color: #1B5E20; }
                    table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                    th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
                    th { background-color: #f5f5f5; font-weight: bold; }
                    .dept-row { background-color: #f9f9f9; font-weight: bold; }
                    .footer { margin-top: 30px; font-size: 12px; color: #777; text-align: center; }
                </style>
            </head>
            <body>
                <h1>Reporte de Ventas - La Palma</h1>
                <div class="header">$kpis</div>

                $dailyTable

                <h2>Resumen por Departamento</h2>
                <table>
                    <thead>
                        <tr><th>Departamento</th><th>Piezas</th><th>Kilos</th><th>Total</th></tr>
                    </thead>
                    <tbody>$deptRows</tbody>
                </table>

                <h2>Desglose por Producto</h2>
                <table>
                    <thead>
                        <tr><th>Producto</th><th>Cantidad</th><th>Importe</th></tr>
                    </thead>
                    <tbody>$productRows</tbody>
                </table>

                <div class="footer">
                    Generado por CarniceriaApp 2.0 - ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", mx).format(Date())}
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    // Campos con coma, comillas o salto de línea van entre comillas (RFC 4180) para que un nombre
    // como "Bistec, corte fino" no se parta en dos columnas al volver a importar.
    private fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    fun productsToCsv(products: List<Product>): String {
        val sb = StringBuilder()
        sb.append("codigo,nombre,precio,departamento,unidad\n")
        products.forEach { p ->
            sb.append("${csvField(p.code)},${csvField(p.name)},${p.price},${csvField(p.department)},${p.unit.name}\n")
        }
        return sb.toString()
    }

    fun writeFileToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { 
                it.write(content.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
