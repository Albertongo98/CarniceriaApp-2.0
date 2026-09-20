package com.example.carniceriaapp20.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.carniceriaapp20.data.local.CarniceriaDatabase
import com.example.carniceriaapp20.data.local.Product
import com.example.carniceriaapp20.data.local.ProductUnit
import com.example.carniceriaapp20.data.local.Ticket
import com.example.carniceriaapp20.data.local.TicketItem
import com.example.carniceriaapp20.data.repository.ProductRepositoryImpl
import com.example.carniceriaapp20.data.repository.TicketRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Reglas de datos con Room real (claves foráneas activas): catálogo, ventas, existencias y anulaciones. */
@RunWith(AndroidJUnit4::class)
class RepositoryInstrumentedTest {

    private lateinit var db: CarniceriaDatabase
    private lateinit var products: ProductRepositoryImpl
    private lateinit var tickets: TicketRepositoryImpl

    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CarniceriaDatabase::class.java).allowMainThreadQueries().build()
        products = ProductRepositoryImpl(db, db.productDao(), db.ticketDao())
        tickets = TicketRepositoryImpl(db, db.ticketDao(), db.productDao())
    }

    @After
    fun tearDown() = db.close()

    private fun bistec(price: Double = 180.0, stock: Double? = null) =
        Product("A", "Bistec", price, "Carniceria", ProductUnit.GRANEL, stock = stock)

    private fun sale(code: String?, qty: Double, total: Double) = TicketItem(
        ticketId = 0, productCode = code, productName = "Bistec", productDepartment = "Carniceria",
        quantity = qty, unitPrice = 180.0, totalPrice = total, unit = ProductUnit.GRANEL
    )

    @Test
    fun replacingTheCatalogKeepsSalesLinkedToTheirProducts() = runBlocking {
        db.productDao().insertProduct(bistec())
        val ticketId = tickets.saveTicket(Ticket(timestamp = now, totalAmount = 90.0, dailyFolio = 1), listOf(sale("A", 0.5, 90.0)))

        // Importar el catálogo: A cambia de precio y aparece B. Las ventas de A no se deben desvincular.
        products.replaceAllProducts(listOf(bistec(price = 200.0), Product("B", "Chorizo", 10.0, "Embutidos", ProductUnit.UNIDAD)))

        assertEquals("A", db.ticketDao().getItemsOfTicket(ticketId).single().productCode)
        val catalog = products.getAllProducts().first().associateBy { it.code }
        assertEquals(200.0, catalog.getValue("A").price, 0.0)
        assertEquals(setOf("A", "B"), catalog.keys)
    }

    @Test
    fun replacingTheCatalogDeletesProductsThatNoLongerCome() = runBlocking {
        db.productDao().insertProduct(bistec())

        products.replaceAllProducts(listOf(Product("B", "Chorizo", 10.0, "Embutidos", ProductUnit.UNIDAD)))

        assertEquals(setOf("B"), products.getAllProducts().first().map { it.code }.toSet())
    }

    @Test
    fun aSaleDecrementsStockAndVoidingRestoresIt() = runBlocking {
        db.productDao().insertProduct(bistec(stock = 10.0))
        val ticketId = tickets.saveTicket(Ticket(timestamp = now, totalAmount = 450.0, dailyFolio = 1), listOf(sale("A", 2.5, 450.0)))
        assertEquals(7.5, products.getProductByCode("A")!!.stock!!, 0.0)

        assertTrue(tickets.voidTicket(ticketId, "Error de captura"))

        assertEquals(10.0, products.getProductByCode("A")!!.stock!!, 0.0)
        assertFalse("un ticket ya anulado no se puede anular otra vez", tickets.voidTicket(ticketId, "otra vez"))
        assertEquals(10.0, products.getProductByCode("A")!!.stock!!, 0.0) // sin devolver dos veces
    }

    @Test
    fun voidedTicketsAreExcludedFromReportsButKeepTheirFolio() = runBlocking {
        db.productDao().insertProduct(bistec())
        val keep = tickets.saveTicket(Ticket(timestamp = now, totalAmount = 90.0, dailyFolio = 1), listOf(sale("A", 0.5, 90.0)))
        val voided = tickets.saveTicket(Ticket(timestamp = now + 1000, totalAmount = 180.0, dailyFolio = 2), listOf(sale("A", 1.0, 180.0)))
        tickets.voidTicket(voided, "Cliente canceló")

        val report = tickets.getProductSalesReport(now - 1000, now + 10_000)
        val inRange = tickets.getTicketsBetween(now - 1000, now + 10_000)

        assertEquals(90.0, report.sumOf { it.totalAmount }, 0.0)
        assertEquals(listOf(keep), inRange.map { it.id })
        assertEquals("el folio diario cuenta también los anulados", 2, tickets.countTicketsOfDay(now - 1000))
    }

    @Test
    fun aFailedSaleLeavesNoTicketBehind() = runBlocking {
        // El renglón apunta a un producto que no existe: viola la clave foránea y debe deshacer TODO.
        try {
            tickets.saveTicket(Ticket(timestamp = now, totalAmount = 90.0, dailyFolio = 1), listOf(sale("NOEXISTE", 0.5, 90.0)))
            fail("debía fallar por la clave foránea")
        } catch (expected: SQLiteConstraintException) {
            // esperado
        }

        assertNull("la transacción debe deshacer también el ticket", db.ticketDao().getLastTicket())
    }
}
