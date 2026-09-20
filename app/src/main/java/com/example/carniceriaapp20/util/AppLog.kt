package com.example.carniceriaapp20.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registro de errores en un archivo local (filesDir/logs/app.log) que se puede exportar desde la
 * pantalla "Respaldo y diagnóstico". La app es offline: sin esto un fallo en el mostrador no deja rastro.
 * Antes de [init] (por ejemplo en tests unitarios) no hace nada.
 */
object AppLog {

    private const val MAX_BYTES = 256 * 1024L
    private const val MAX_TRACE_LINES = 25

    @Volatile
    private var logFile: File? = null

    fun init(context: Context) {
        logFile = File(File(context.filesDir, "logs").apply { mkdirs() }, "app.log")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) = write("E", tag, message, throwable)

    fun i(tag: String, message: String) = write("I", tag, message, null)

    /** Contenido actual del registro (el anterior rotado va primero), vacío si no hay nada. */
    fun readAll(): String {
        val file = logFile ?: return ""
        val old = File(file.parentFile, "app.old.log")
        return listOf(old, file).filter { it.exists() }.joinToString("\n") { it.readText() }
    }

    fun clear() {
        val file = logFile ?: return
        synchronized(this) {
            file.delete()
            File(file.parentFile, "app.old.log").delete()
        }
    }

    private fun write(level: String, tag: String, message: String, throwable: Throwable?) {
        val file = logFile ?: return
        if (level == "E") Log.e(tag, message, throwable) else Log.i(tag, message)
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val trace = throwable?.stackTraceToString()?.lines()?.take(MAX_TRACE_LINES)?.joinToString("\n") ?: ""
        val entry = "$time $level/$tag: $message" + if (trace.isNotEmpty()) "\n$trace" else ""
        synchronized(this) {
            try {
                if (file.exists() && file.length() > MAX_BYTES) {
                    val old = File(file.parentFile, "app.old.log")
                    old.delete()
                    file.renameTo(old)
                }
                file.appendText(entry + "\n")
            } catch (e: Exception) {
                Log.e(tag, "No se pudo escribir el registro de errores", e)
            }
        }
    }
}
