package com.example.carniceriaapp20.util

import org.junit.Assert.assertEquals
import org.junit.Test

class BarcodeHelperTest {

    @Test
    fun `generarCodigoParaPOS formatos correctos`() {
        // Caso estándar
        assertEquals("2001090050505", generarCodigoParaPOS("1090", 50.50))

        // Caso con código de producto corto
        assertEquals("2000012123455", generarCodigoParaPOS("12", 123.45))

        // Caso con precio entero
        assertEquals("2009999100005", generarCodigoParaPOS("9999", 100.0))

        // Caso con decimales múltiples
        assertEquals("2001234000185", generarCodigoParaPOS("1234", 0.178))
    }
}
