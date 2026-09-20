package com.example.carniceriaapp20.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tickets ADD COLUMN daily_folio INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ticket_items ADD COLUMN estimated_pieces INTEGER")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ticket_items ADD COLUMN product_department TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * v5: unidad guardada en cada renglón vendido, anulación de tickets, existencias de productos e
 * índice por fecha para los reportes por rango. Es una lista de sentencias (y no código dentro del
 * Migration) para poder ejecutarla contra un SQLite real en los tests unitarios.
 */
internal val MIGRATION_4_5_STATEMENTS = listOf(
    "ALTER TABLE ticket_items ADD COLUMN unit TEXT NOT NULL DEFAULT 'UNIDAD'",
    "ALTER TABLE tickets ADD COLUMN voided_at INTEGER",
    "ALTER TABLE tickets ADD COLUMN void_reason TEXT",
    "ALTER TABLE products ADD COLUMN stock REAL",
    "ALTER TABLE products ADD COLUMN min_stock REAL NOT NULL DEFAULT 0",
    "CREATE INDEX IF NOT EXISTS `index_tickets_timestamp` ON `tickets` (`timestamp`)",

    // Rescate: importar el catálogo con "borrar todo" dejó product_code en NULL en las ventas viejas.
    // Se vuelve a enlazar por nombre exacto (si hay varios con el mismo nombre, el de menor código).
    """UPDATE ticket_items SET product_code =
        (SELECT p.code FROM products p WHERE p.name = ticket_items.product_name ORDER BY p.code LIMIT 1)
        WHERE product_code IS NULL""",
    // Departamento vacío en ventas de la versión 3 (columna agregada después).
    """UPDATE ticket_items SET product_department =
        (SELECT p.department FROM products p WHERE p.code = ticket_items.product_code)
        WHERE product_department = '' AND product_code IS NOT NULL""",
    // Unidad: la del catálogo cuando el producto existe...
    "UPDATE ticket_items SET unit = 'GRANEL' WHERE product_code IN (SELECT code FROM products WHERE unit = 'GRANEL')",
    // ...y si no se pudo enlazar, cantidad con decimales = venta por peso.
    "UPDATE ticket_items SET unit = 'GRANEL' WHERE product_code IS NULL AND quantity <> CAST(quantity AS INTEGER)"
)

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_4_5_STATEMENTS.forEach { db.execSQL(it) }
    }
}
