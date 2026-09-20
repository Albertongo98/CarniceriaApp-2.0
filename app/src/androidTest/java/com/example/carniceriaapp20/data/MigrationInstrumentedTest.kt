package com.example.carniceriaapp20.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.carniceriaapp20.data.local.CarniceriaDatabase
import com.example.carniceriaapp20.data.local.MIGRATION_4_5
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migración 4→5 con el motor real de Room en el dispositivo: crea una base v4 con los esquemas
 * exportados, la migra y deja que Room VALIDE la estructura contra 5.json (si no coincide, falla).
 */
@RunWith(AndroidJUnit4::class)
class MigrationInstrumentedTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), CarniceriaDatabase::class.java)

    @Test
    fun migrate4To5_validatesSchemaAndRescuesLegacySales() {
        helper.createDatabase(DB_NAME, 4).apply {
            execSQL("INSERT INTO products VALUES ('A', 'Bistec', 180.0, 'Carniceria', 'GRANEL')")
            execSQL("INSERT INTO products VALUES ('B', 'Chorizo', 10.0, 'Embutidos', 'UNIDAD')")
            execSQL("INSERT INTO tickets (id, timestamp, total_amount, daily_folio) VALUES (1, 1000, 100.0, 1)")
            // Ventas antiguas: el código del producto se perdió (NULL) y el departamento quedó vacío.
            execSQL(
                "INSERT INTO ticket_items (id, ticket_id, product_code, product_name, product_department, quantity, unit_price, total_price) " +
                    "VALUES (1, 1, NULL, 'Bistec', '', 1.5, 180.0, 270.0)"
            )
            execSQL(
                "INSERT INTO ticket_items (id, ticket_id, product_code, product_name, product_department, quantity, unit_price, total_price) " +
                    "VALUES (2, 1, NULL, 'Chorizo', '', 3.0, 10.0, 30.0)"
            )
            close()
        }

        // validateDroppedTables = true: Room compara la base migrada con el esquema 5.json.
        val db = helper.runMigrationsAndValidate(DB_NAME, 5, true, MIGRATION_4_5)

        db.query("SELECT product_code, product_department, unit FROM ticket_items ORDER BY id").use { c ->
            c.moveToFirst()
            assertEquals("A", c.getString(0))
            assertEquals("Carniceria", c.getString(1))
            assertEquals("GRANEL", c.getString(2))
            c.moveToNext()
            assertEquals("B", c.getString(0))
            assertEquals("Embutidos", c.getString(1))
            assertEquals("UNIDAD", c.getString(2))
        }
        db.query("SELECT stock, min_stock FROM products WHERE code = 'A'").use { c ->
            c.moveToFirst()
            assertNull(if (c.isNull(0)) null else c.getDouble(0))
            assertEquals(0.0, c.getDouble(1), 0.0)
        }
        db.query("SELECT voided_at FROM tickets").use { c ->
            c.moveToFirst()
            assertEquals(true, c.isNull(0))
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
