package com.example.carniceriaapp20.util

import com.example.carniceriaapp20.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PinManagerTest {

    private val prefs: UserPreferencesRepository = mock()

    private suspend fun managerWithPin(pin: String?): PinManager {
        if (pin == null) {
            whenever(prefs.getPin()).thenReturn(null)
        } else {
            val salt = PinHasher.newSalt()
            whenever(prefs.getPin()).thenReturn(salt to PinHasher.hash(pin, salt))
        }
        return PinManager(prefs)
    }

    @Test
    fun `el hash depende de la sal y no contiene el PIN`() {
        val hashA = PinHasher.hash("1234", "salA")
        val hashB = PinHasher.hash("1234", "salB")

        assertNotEquals(hashA, hashB)
        assertEquals(hashA, PinHasher.hash("1234", "salA"))
        assertFalse(hashA.contains("1234"))
    }

    @Test
    fun `sin PIN configurado todo esta permitido`() = runTest {
        val manager = managerWithPin(null)

        assertFalse(manager.isPinSet())
        assertEquals(PinCheck.Ok, manager.verify("cualquier cosa"))
    }

    @Test
    fun `acepta el PIN correcto y rechaza el incorrecto`() = runTest {
        val manager = managerWithPin("4321")

        assertTrue(manager.isPinSet())
        assertEquals(PinCheck.Wrong, manager.verify("0000"))
        assertEquals(PinCheck.Ok, manager.verify("4321"))
    }

    @Test
    fun `tras cinco fallos se bloquea aunque el PIN sea correcto`() = runTest {
        val manager = managerWithPin("4321")

        repeat(5) { assertEquals(PinCheck.Wrong, manager.verify("0000")) }

        val result = manager.verify("4321")
        assertTrue(result is PinCheck.Locked)
        assertTrue((result as PinCheck.Locked).seconds in 1..30)
    }

    @Test
    fun `el bloqueo termina con el tiempo y un acierto reinicia los fallos`() {
        var now = 0L
        val limiter = PinAttemptLimiter(maxAttempts = 3, lockMillis = 30_000, clock = { now })

        repeat(3) { limiter.registerFailure() }
        assertEquals(30_000, limiter.remainingLockMillis())

        now = 30_000
        assertEquals(0, limiter.remainingLockMillis())

        limiter.registerFailure(); limiter.registerFailure()
        limiter.registerSuccess()
        limiter.registerFailure(); limiter.registerFailure() // solo 2 desde el acierto: no bloquea
        assertEquals(0, limiter.remainingLockMillis())
    }

    @Test
    fun `un PIN valido tiene de 4 a 8 digitos`() {
        assertTrue(PinManager.isValidPin("1234"))
        assertTrue(PinManager.isValidPin("12345678"))
        assertFalse(PinManager.isValidPin("123"))
        assertFalse(PinManager.isValidPin("123456789"))
        assertFalse(PinManager.isValidPin("12a4"))
    }
}
