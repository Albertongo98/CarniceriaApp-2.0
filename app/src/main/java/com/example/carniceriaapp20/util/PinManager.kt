package com.example.carniceriaapp20.util

import com.example.carniceriaapp20.data.preferences.UserPreferencesRepository
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/** Hash SHA-256 con sal: el PIN nunca se guarda en claro. */
object PinHasher {
    fun newSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()

    fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray(Charsets.UTF_8)).toHex()

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}

/** Tras [maxAttempts] fallos seguidos bloquea los intentos durante [lockMillis] (contra adivinar el PIN a base de intentos). */
class PinAttemptLimiter(
    private val maxAttempts: Int = 5,
    private val lockMillis: Long = 30_000L,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private var failures = 0
    private var lockedUntil = 0L

    /** Milisegundos que faltan para poder volver a intentar (0 = se puede intentar). */
    fun remainingLockMillis(): Long = (lockedUntil - clock()).coerceAtLeast(0)

    fun registerFailure() {
        failures++
        if (failures >= maxAttempts) {
            lockedUntil = clock() + lockMillis
            failures = 0
        }
    }

    fun registerSuccess() {
        failures = 0
        lockedUntil = 0
    }
}

sealed class PinCheck {
    object Ok : PinCheck()
    object Wrong : PinCheck()
    data class Locked(val seconds: Int) : PinCheck()
}

/**
 * PIN del dueño para acciones sensibles (reportes, actualizar base de datos, respaldo, borrar productos,
 * anular tickets). Singleton: el límite de intentos no se reinicia al cambiar de pantalla. Sin PIN
 * configurado, todo está permitido.
 */
@Singleton
class PinManager @Inject constructor(private val prefs: UserPreferencesRepository) {

    private val limiter = PinAttemptLimiter()

    suspend fun isPinSet(): Boolean = prefs.getPin() != null

    suspend fun verify(pin: String): PinCheck {
        val stored = prefs.getPin() ?: return PinCheck.Ok
        val remaining = limiter.remainingLockMillis()
        if (remaining > 0) return PinCheck.Locked(((remaining + 999) / 1000).toInt())

        return if (PinHasher.hash(pin, stored.first) == stored.second) {
            limiter.registerSuccess()
            PinCheck.Ok
        } else {
            limiter.registerFailure()
            AppLog.i("PIN", "PIN incorrecto")
            PinCheck.Wrong
        }
    }

    suspend fun setPin(newPin: String) {
        val salt = PinHasher.newSalt()
        prefs.savePin(salt, PinHasher.hash(newPin, salt))
    }

    suspend fun clearPin() = prefs.clearPin()

    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 8
        fun isValidPin(pin: String) = pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it.isDigit() }
    }
}
