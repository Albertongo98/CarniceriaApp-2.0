package com.example.carniceriaapp20.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer

const val NO_DEPARTMENT = "Sin departamento"

private val MULTIPLE_SPACES = Regex("\\s+")
private val DIACRITICS = Regex("\\p{Mn}+")

// Clave de comparación: sin acentos, sin mayúsculas ni espacios de sobra ("Carnicería " == "carniceria").
private fun comparisonKey(text: String): String =
    Normalizer.normalize(text.trim().replace(MULTIPLE_SPACES, " "), Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase()

/**
 * Deja el nombre de un departamento limpio (sin espacios de sobra) y, si ya existe uno equivalente
 * (sin importar mayúsculas ni acentos), reutiliza SU escritura. Así "carniceria" no crea un
 * departamento aparte de "Carnicería" en los reportes. Vacío => [NO_DEPARTMENT].
 */
fun normalizeDepartment(raw: String, known: Collection<String>): String {
    val cleaned = raw.trim().replace(MULTIPLE_SPACES, " ")
    if (cleaned.isEmpty()) return NO_DEPARTMENT
    val key = comparisonKey(cleaned)
    return known.firstOrNull { comparisonKey(it) == key } ?: cleaned
}

/** Acepta coma o punto decimal: el teclado de la tablet escribe "12,5". */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

/** 12.0 -> "12", 12.5 -> "12.5", 9.649999999999999 -> "9.65" (3 decimales máximo, sin ceros de sobra). */
fun formatPlain(value: Double): String =
    BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
