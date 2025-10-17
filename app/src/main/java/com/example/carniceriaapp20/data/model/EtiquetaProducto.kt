package com.example.carniceriaapp20.data.model

/**
 * Data class simple para pasar la información de la etiqueta a BluetoothPrinterHelper.
 */
data class EtiquetaProducto(
    val nombre: String,
    val precio: String, // Se maneja como String para flexibilidad en el formato
    val codigo: String
)
