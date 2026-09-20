package com.example.carniceriaapp20.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BackupManagerTest {

    @Test
    fun `reconoce el encabezado de un archivo SQLite`() {
        val header = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII) + ByteArray(100)

        assertTrue(looksLikeSqlite(header))
    }

    @Test
    fun `rechaza archivos que no son SQLite o que estan truncados`() {
        assertFalse(looksLikeSqlite("codigo,nombre,precio".toByteArray()))
        assertFalse(looksLikeSqlite(ByteArray(16)))
        assertFalse(looksLikeSqlite(ByteArray(0)))
        assertFalse(looksLikeSqlite("SQLite format".toByteArray()))
    }

    @Test
    fun `los nombres de archivo llevan fecha y hora y extension correcta`() {
        val date = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 18, 14, 5, 0) }.time

        assertEquals("respaldo_carniceria_20260918_1405.db", backupFileName(date))
        assertEquals("registro_errores_20260918_1405.txt", logFileName(date))
    }
}
