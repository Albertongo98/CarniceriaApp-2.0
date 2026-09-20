package com.example.carniceriaapp20.data.local

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Prueba la migración 4→5 contra un SQLite real (sqlite-jdbc) usando los esquemas que exporta Room
 * (app/schemas). Room se niega a abrir la base si la estructura migrada no coincide EXACTAMENTE con
 * la que espera, así que aquí se compara contra la base v5 creada desde cero.
 */
class MigrationV5Test {

    private lateinit var db: Connection

    @Before
    fun setUp() {
        db = DriverManager.getConnection("jdbc:sqlite::memory:")
        db.createStatement().use { it.execute("PRAGMA foreign_keys = ON") } // Room lo activa al abrir
    }

    @After
    fun tearDown() = db.close()

    // ---------- utilidades ----------

    private fun schemaJson(version: Int): String =
        File("schemas/com.example.carniceriaapp20.data.local.CarniceriaDatabase/$version.json").readText()

    /** Sentencias CREATE TABLE / CREATE INDEX del esquema exportado por Room. */
    private fun schemaSql(version: Int): List<String> {
        val statements = mutableListOf<String>()
        schemaJson(version).split("\"tableName\": \"").drop(1).forEach { block ->
            val table = block.substringBefore('"')
            Regex("\"createSql\":\\s*\"((?:CREATE TABLE|CREATE INDEX)[^\"]*)\"").findAll(block).forEach {
                statements += it.groupValues[1].replace("\${TABLE_NAME}", table)
            }
        }
        return statements
    }

    private fun createSchema(version: Int) = db.createStatement().use { st -> schemaSql(version).forEach { st.execute(it) } }

    private fun exec(sql: String) = db.createStatement().use { it.execute(sql) }

    private fun rows(sql: String): List<List<Any?>> = db.createStatement().use { st ->
        st.executeQuery(sql).use { rs ->
            val out = mutableListOf<List<Any?>>()
            while (rs.next()) out += (1..rs.metaData.columnCount).map { rs.getObject(it) }
            out
        }
    }

    /** Estructura observable por Room: columnas, índices y claves foráneas de cada tabla. */
    private fun structure(): Map<String, List<Any?>> {
        val tables = rows("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'")
            .map { it[0] as String }.sorted()
        return tables.associateWith { t ->
            // (name, type, notnull, pk) — el valor DEFAULT no cuenta: Room lo ignora si la entidad no declara uno.
            val columns = rows("SELECT name, type, \"notnull\", pk FROM pragma_table_info('$t')").toSet()
            val indexes = rows("SELECT name, \"unique\" FROM pragma_index_list('$t') WHERE name NOT LIKE 'sqlite_autoindex%'").toSet()
            val fks = rows("SELECT \"table\", \"from\", \"to\", on_delete FROM pragma_foreign_key_list('$t')").toSet()
            listOf(columns, indexes, fks)
        }
    }

    private fun migrate() = MIGRATION_4_5_STATEMENTS.forEach { exec(it) }

    // ---------- pruebas ----------

    @Test
    fun `la base migrada de 4 a 5 tiene la misma estructura que una base v5 nueva`() {
        createSchema(4)
        migrate()
        val migrated = structure()

        tearDown(); setUp()
        createSchema(5)

        assertEquals(structure(), migrated)
    }

    @Test
    fun `rescata el codigo, departamento y unidad de las ventas viejas`() {
        createSchema(4)
        exec("INSERT INTO products VALUES ('A', 'Bistec', 180.0, 'Carniceria', 'GRANEL')")
        exec("INSERT INTO products VALUES ('B', 'Chorizo', 10.0, 'Embutidos', 'UNIDAD')")
        exec("INSERT INTO tickets (id, timestamp, total_amount, daily_folio) VALUES (1, 1000, 100.0, 1)")
        fun item(id: Int, code: String?, name: String, dept: String, qty: Double) = exec(
            "INSERT INTO ticket_items (id, ticket_id, product_code, product_name, product_department, quantity, unit_price, total_price) " +
                "VALUES ($id, 1, ${code?.let { "'$it'" } ?: "NULL"}, '$name', '$dept', $qty, 10.0, 10.0)"
        )
        item(1, null, "Bistec", "", 1.5)            // código perdido: se recupera por nombre
        item(2, null, "Chorizo", "", 3.0)
        item(3, null, "Producto viejo", "X", 0.75)  // sin match y con decimales: kg
        item(4, null, "Pieza vieja", "Y", 2.0)      // sin match y entera: piezas
        item(5, "A", "Bistec", "Carniceria", 2.0)   // a granel aunque la cantidad sea entera

        migrate()

        val result = rows("SELECT id, product_code, product_department, unit FROM ticket_items ORDER BY id")
        assertEquals(listOf<Any?>(1, "A", "Carniceria", "GRANEL"), result[0])
        assertEquals(listOf<Any?>(2, "B", "Embutidos", "UNIDAD"), result[1])
        assertEquals(listOf<Any?>(3, null, "X", "GRANEL"), result[2])
        assertEquals(listOf<Any?>(4, null, "Y", "UNIDAD"), result[3])
        assertEquals(listOf<Any?>(5, "A", "Carniceria", "GRANEL"), result[4])

        // Valores por defecto de lo nuevo: existencia sin controlar, mínimo 0 y tickets no anulados.
        assertEquals(listOf<Any?>(null, 0.0), rows("SELECT stock, min_stock FROM products WHERE code = 'A'")[0])
        assertNull(rows("SELECT voided_at FROM tickets")[0][0])
    }

    private fun seedSale() {
        createSchema(5)
        exec("INSERT INTO products (code, name, price, department, unit, min_stock) VALUES ('A', 'Bistec', 180.0, 'Carniceria', 'GRANEL', 0)")
        exec("INSERT INTO tickets (id, timestamp, total_amount, daily_folio) VALUES (1, 1000, 90.0, 1)")
        exec("INSERT INTO ticket_items (ticket_id, product_code, product_name, product_department, quantity, unit_price, total_price, unit) VALUES (1, 'A', 'Bistec', 'Carniceria', 0.5, 180.0, 90.0, 'GRANEL')")
    }

    @Test
    fun `INSERT OR REPLACE sobre un producto deja en NULL el codigo de sus ventas`() {
        seedSale()

        exec("INSERT OR REPLACE INTO products (code, name, price, department, unit, min_stock) VALUES ('A', 'Bistec', 200.0, 'Carniceria', 'GRANEL', 0)")

        assertNull(rows("SELECT product_code FROM ticket_items")[0][0])
    }

    @Test
    fun `DELETE de un producto deja en NULL el codigo de sus ventas`() {
        seedSale()

        exec("DELETE FROM products")

        assertNull(rows("SELECT product_code FROM ticket_items")[0][0])
    }

    @Test
    fun `el upsert actualiza el producto sin desvincular sus ventas`() {
        seedSale()

        exec(
            "INSERT INTO products (code, name, price, department, unit, min_stock) VALUES ('A', 'Bistec', 200.0, 'Carniceria', 'GRANEL', 0) " +
                "ON CONFLICT(code) DO UPDATE SET price = excluded.price"
        )

        assertEquals("A", rows("SELECT product_code FROM ticket_items")[0][0])
        assertEquals(200.0, rows("SELECT price FROM products WHERE code = 'A'")[0][0] as Double, 0.0)
    }
}
