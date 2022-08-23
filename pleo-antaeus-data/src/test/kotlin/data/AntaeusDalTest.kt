package io.pleo.antaeus.data


import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection


class AntaeusDalTest {

    // Create tables for db
    private val tables = arrayOf(InvoiceTable, CustomerTable)

    // Connect to  database
    private val db =  Database.connect("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;", "org.h2.Driver", "root", "")
    private val dal = AntaeusDal(db = db)

    @BeforeEach
    fun before() {
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction(db) {
            addLogger(StdOutSqlLogger)
            // Drop all existing tables to ensure a clean slate on each run
            SchemaUtils.drop(*tables)
            // Create all tables
            SchemaUtils.create(*tables)
            setupInitialData(dal = dal)
        }
    }

    @Test
    internal fun `get invoices`() {
        val invoices = dal.fetchInvoices()
        Assertions.assertEquals(1000, invoices.size)
    }

    @Test
    internal fun `create invoice`() {
        val newInvoice = createInvoice()
        val customer = Customer(newInvoice.customerId, newInvoice.amount.currency)

        val invoice = dal.createInvoice(amount = newInvoice.amount, customer = customer)
        Assertions.assertEquals(1001, invoice?.id)
    }
}
