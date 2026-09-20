package com.example.carniceriaapp20.util

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.carniceriaapp20.data.local.CARNICERIA_DB_NAME
import com.example.carniceriaapp20.data.local.CARNICERIA_DB_VERSION
import com.example.carniceriaapp20.data.local.CarniceriaDatabase
import com.example.carniceriaapp20.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

/** Todo archivo SQLite empieza con "SQLite format 3\0". */
internal fun looksLikeSqlite(header: ByteArray): Boolean =
    header.size >= SQLITE_MAGIC.size && SQLITE_MAGIC.indices.all { header[it] == SQLITE_MAGIC[it] }

internal fun backupFileName(now: Date = Date()): String =
    "respaldo_carniceria_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(now)}.db"

internal fun logFileName(now: Date = Date()): String =
    "registro_errores_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(now)}.txt"

/**
 * Respaldo y restauración de TODA la base de datos (productos, ventas, etiquetas) como un archivo .db
 * que el usuario guarda donde quiera (Descargas, Drive, USB). Al restaurar, primero se guarda una copia
 * de lo actual en filesDir/backups (las últimas 3), y la app se reinicia para volver a abrir la base.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: CarniceriaDatabase,
    private val prefs: UserPreferencesRepository
) {
    private val dbFile: File get() = context.getDatabasePath(CARNICERIA_DB_NAME)

    suspend fun exportTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            checkpoint()
            val out = context.contentResolver.openOutputStream(uri) ?: error("No se pudo abrir el destino")
            out.use { stream -> dbFile.inputStream().use { it.copyTo(stream) } }
            prefs.saveLastBackupAt(System.currentTimeMillis())
            AppLog.i("Backup", "Respaldo exportado (${dbFile.length()} bytes)")
        }.onFailure { AppLog.e("Backup", "Fallo al exportar el respaldo", it) }
    }

    /** Devuelve éxito solo si la base quedó reemplazada; el llamador debe reiniciar la app. */
    suspend fun restoreFrom(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "restaurar.db")
        runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: error("No se pudo leer el archivo")
            input.use { src -> temp.outputStream().use { src.copyTo(it) } }
            validate(temp)?.let { error(it) }

            saveSafetyCopy()
            database.close()
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            temp.copyTo(dbFile, overwrite = true)
            AppLog.i("Backup", "Base restaurada desde un respaldo")
        }.also { temp.delete() }
            .onFailure { AppLog.e("Backup", "Fallo al restaurar el respaldo", it) }
    }

    suspend fun exportLogTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val text = AppLog.readAll().ifBlank { "(sin errores registrados)" }
            val out = context.contentResolver.openOutputStream(uri) ?: error("No se pudo abrir el destino")
            out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
    }

    /** Mensaje de error si el archivo no sirve como respaldo de esta app; null si es válido. */
    private fun validate(file: File): String? {
        val header = file.inputStream().use { stream -> ByteArray(16).also { stream.read(it) } }
        if (!looksLikeSqlite(header)) return "El archivo no es un respaldo de la base de datos."
        return try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val tables = db.rawQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name IN ('products', 'tickets', 'ticket_items')", null
                ).use { it.moveToFirst(); it.getInt(0) }
                when {
                    tables != 3 -> "El archivo no tiene las tablas de esta app."
                    db.version > CARNICERIA_DB_VERSION -> "El respaldo es de una versión más nueva de la app (${db.version}). Actualiza la app primero."
                    db.version < 1 -> "El archivo no tiene versión de base de datos."
                    else -> null
                }
            }
        } catch (e: Exception) {
            "El archivo está dañado o no es una base de datos válida."
        }
    }

    private fun checkpoint() {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    }

    private fun saveSafetyCopy() {
        checkpoint()
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        dbFile.copyTo(File(dir, "antes_de_restaurar_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.db"), overwrite = true)
        dir.listFiles { f -> f.name.startsWith("antes_de_restaurar_") }
            ?.sortedByDescending { it.name }?.drop(3)?.forEach { it.delete() }
    }

    /** Reinicia la app desde cero (necesario tras restaurar: la base anterior ya está cerrada). */
    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) ?: return
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
