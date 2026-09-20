package com.example.carniceriaapp20.util

import java.util.Locale

private val MX: Locale = Locale.forLanguageTag("es-MX")

/** 12.5 -> "12.50" siempre con punto decimal, aunque el dispositivo use coma (ver regla de formatos). */
fun formatMoney2(value: Double): String = "%.2f".format(MX, value)
