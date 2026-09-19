package com.example.carniceriaapp20.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.carniceriaapp20.data.local.DepartmentSalesReport
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductSalesReport
import com.example.carniceriaapp20.data.local.ProductUnit
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object ReportExporter {

    fun generateHtmlReport(
        date: Long,
        totalDay: Double,
        deptSales: List<DepartmentSalesReport>,
        prodSales: List<ProductSalesReport>
    ): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(Date(date))
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"))

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
                <div class="header">
                    <p><strong>Fecha:</strong> $dateStr</p>
                    <p><strong>Venta Total del Día:</strong> <span class="total">${currencyFormat.format(totalDay)}</span></p>
                </div>

                <h2>Resumen por Departamento</h2>
                <table>
                    <thead>
                        <tr><th>Departamento</th><th>Piezas</th><th>Kilos</th><th>Total</th></tr>
                    </thead>
                    <tbody>
                        ${deptSales.joinToString("") {
                            val piezas = if (it.totalPieces > 0) (if (it.totalPieces % 1 == 0.0) it.totalPieces.toInt().toString() else "%.3f".format(Locale.forLanguageTag("es-MX"), it.totalPieces)) else "-"
                            val kilos = if (it.totalKilos > 0) "%.3f".format(Locale.forLanguageTag("es-MX"), it.totalKilos) else "-"
                            "<tr><td>${it.department}</td><td>$piezas</td><td>$kilos</td><td>${currencyFormat.format(it.totalAmount)}</td></tr>"
                        }}
                    </tbody>
                </table>

                <h2>Desglose por Producto</h2>
                <table>
                    <thead>
                        <tr><th>Producto</th><th>Cantidad</th><th>Importe</th></tr>
                    </thead>
                    <tbody>
                        ${prodSales.groupBy { it.department }.entries.joinToString("") { (dept, products) ->
                            val deptRow = "<tr class='dept-row'><td colspan='3'>$dept</td></tr>"
                            val productRows = products.joinToString("") { p ->
                                val qtyStr = if (p.totalQuantity % 1 == 0.0) p.totalQuantity.toInt().toString() else "%.3f".format(Locale.forLanguageTag("es-MX"), p.totalQuantity)
                                val unitStr = if (p.effectiveUnit == ProductUnit.GRANEL) "kg" else "pz"
                                "<tr><td>${p.productName}</td><td>$qtyStr $unitStr</td><td>${currencyFormat.format(p.totalAmount)}</td></tr>"
                            }
                            deptRow + productRows
                        }}
                    </tbody>
                </table>

                <div class="footer">
                    Generado por CarniceriaApp 2.0 - ${SimpleDateFormat("HH:mm:ss").format(Date())}
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun productsToCsv(products: List<Product>): String {
        val sb = StringBuilder()
        sb.append("codigo,nombre,precio,departamento,unidad\n")
        products.forEach { p ->
            sb.append("${p.code},${p.name},${p.price},${p.department},${p.unit.name}\n")
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

    fun saveAndShareFile(context: Context, fileName: String, content: String) {
        try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { it.write(content.toByteArray()) }

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (fileName.endsWith(".html")) "text/html" else "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Compartir archivo..."))
        } catch (e: Exception) { e.printStackTrace() }
    }
}
