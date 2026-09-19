package com.example.carniceriaapp20.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    @Test
    fun `generarCodigoControlInterno formatos correctos`() {
        // Se necesita un timestamp y folio fijos para una prueba consistente.
        // El HHmmss se calcula con la misma zona horaria por defecto de la JVM que usa la función
        // bajo prueba, para que el test no dependa de correr específicamente en GMT.
        val testTimestamp = 1672531200000L // 01/01/2023 00:00:00 GMT
        val expectedTimeStr = SimpleDateFormat("HHmmss", Locale.US).format(Date(testTimestamp))

        // Caso estándar
        assertEquals("$expectedTimeStr-001-0125.50", generarCodigoControlInterno(testTimestamp, "1", 125.50))

        // Caso con folio largo
        assertEquals("$expectedTimeStr-123-0050.00", generarCodigoControlInterno(testTimestamp, "123", 50.0))

        // Caso con monto grande
        assertEquals("$expectedTimeStr-999-9999.99", generarCodigoControlInterno(testTimestamp, "999", 9999.99))
    }
}
