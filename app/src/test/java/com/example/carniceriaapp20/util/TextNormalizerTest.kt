package com.example.carniceriaapp20.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextNormalizerTest {

    private val known = listOf("Carnicería", "Verdura")

    @Test
    fun `reutiliza la escritura del departamento existente sin importar mayusculas ni acentos`() {
        assertEquals("Carnicería", normalizeDepartment("carniceria", known))
        assertEquals("Carnicería", normalizeDepartment("  CARNICERÍA  ", known))
        assertEquals("Verdura", normalizeDepartment("verdura", known))
    }

    @Test
    fun `limpia espacios de sobra en un departamento nuevo`() {
        assertEquals("Carnes Frias", normalizeDepartment("  Carnes    Frias ", known))
    }

    @Test
    fun `un departamento vacio pasa a Sin departamento`() {
        assertEquals(NO_DEPARTMENT, normalizeDepartment("   ", known))
        assertEquals(NO_DEPARTMENT, normalizeDepartment("", emptyList()))
    }

    @Test
    fun `parseDecimal acepta coma o punto`() {
        assertEquals(12.5, parseDecimal("12,5")!!, 0.0)
        assertEquals(12.5, parseDecimal(" 12.5 ")!!, 0.0)
        assertNull(parseDecimal("abc"))
        assertNull(parseDecimal(""))
    }

    @Test
    fun `formatPlain quita ceros de sobra`() {
        assertEquals("12", formatPlain(12.0))
        assertEquals("12.5", formatPlain(12.5))
        assertEquals("0", formatPlain(0.0))
        assertEquals("9.65", formatPlain(10.0 - 0.35 - 0.0000000000001)) // ruido de coma flotante al restar ventas
        assertEquals("1785.5", formatPlain(1785.5))
    }
}
