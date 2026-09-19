package com.example.carniceriaapp20.util

import kotlin.math.roundToInt

/**
 * Genera el código de barras de un producto a granel para ser leído por el TPV.
 * Especificación: "200" + código de producto (4 dígitos) + precio total en centavos (5 dígitos) + "5"
 * Los centavos se redondean (no se truncan).
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
