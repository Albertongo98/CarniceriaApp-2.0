package com.example.carniceriaapp20.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class NumberFormattingTest {

    @Test
    fun `formatMoney2 usa punto decimal aunque el dispositivo use coma`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("es", "ES")) // coma decimal, como la tablet real
            assertEquals("12.50", formatMoney2(12.5))
            assertEquals("0.00", formatMoney2(0.0))
            assertEquals("1234.57", formatMoney2(1234.567)) // sin separador de miles: es para totales en pantalla
        } finally {
            Locale.setDefault(original)
        }
    }
}
