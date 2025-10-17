package com.example.carniceriaapp20.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Genera el código de barras en formato EAN-13 para ser leído por el TPV.
 * Especificación: "200" + código de producto (4 dígitos) + precio total (5 dígitos) + "5"
 *
 * @param productoCodigo El código del producto (ej. "1090").
 * @param montoVenta El precio total del item (ej. 50.50).
 * @return El código de barras formateado como String (ej. "2001090050505").
 */
fun generarCodigoParaPOS(productoCodigo: String, montoVenta: Double): String {
    val codigoProductoStr = productoCodigo.padStart(4, '0')
    val montoVentaStr = (montoVenta * 100).roundToInt().toString().padStart(5, '0')
    return "200${codigoProductoStr}${montoVentaStr}5"
}

/**
 * Genera la cadena de datos para el código QR de control interno.
 * Especificación: HHMMSS-FFF-MMMM.CC
 *
 * @param timestamp La fecha y hora de la venta en milisegundos.
 * @param folio El folio de la venta (ej. "1").
 * @param montoTotal El monto total del ticket (ej. 125.50).
 * @return La cadena formateada para el QR (ej. "143025-001-0125.50").
 */
fun generarCodigoControlInterno(timestamp: Long, folio: String, montoTotal: Double): String {
    val dateFormat = SimpleDateFormat("HHmmss", Locale.US)
    val timeStr = dateFormat.format(Date(timestamp))
    val folioStr = folio.padStart(3, '0')
    val montoStr = String.format(Locale.US, "%07.2f", montoTotal)
    
    return "$timeStr-$folioStr-$montoStr"
}
