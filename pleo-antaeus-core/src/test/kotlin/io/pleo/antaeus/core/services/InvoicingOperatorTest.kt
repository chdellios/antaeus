package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.data.setupInitialData
import io.pleo.antaeus.models.InvoiceStatus
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InvoicingOperatorTest {
    private val tables = arrayOf(InvoiceTable, CustomerTable)
    private val db = Database.connect("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;",
            "org.h2.Driver", "root", "")

    private val dal = AntaeusDal(db = db)
    private val invoiceService = InvoiceService(dal = dal)
    private val customerService = CustomerService(dal = dal)

    private fun createInvoicingOperator(paymentProvider: PaymentProvider): InvoicingOperator {
        val billingService = BillingService(paymentProvider, customerService, invoiceService)
        return InvoicingOperator(billingService, invoiceService)
    }

    private fun createPaymentProviderMock(block: () -> Boolean): PaymentProvider {
        return mockk {
            every { charge(any()) } returns block()
        }
    }

    @BeforeEach
    fun before() {
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
    fun `successful billing`() {
        val invoicingOperator = createInvoicingOperator(createPaymentProviderMock { true })
        invoicingOperator.chargeByStatus(InvoiceStatus.PENDING.toString())
        invoicingOperator.chargeByStatus(InvoiceStatus.FAILED.toString())
        Assertions.assertEquals(0, dal.fetchInvoiceByStatus(InvoiceStatus.PENDING.toString()).count()
                + dal.fetchInvoiceByStatus(InvoiceStatus.FAILED.toString()).count())
    }

    @Test
    fun `billing failure`() {
        val invoicingOperator = createInvoicingOperator(createPaymentProviderMock { false })
        invoicingOperator.chargeByStatus(InvoiceStatus.PENDING.toString())
        invoicingOperator.chargeByStatus(InvoiceStatus.FAILED.toString())
        Assertions.assertEquals(100, dal.fetchInvoiceByStatus(InvoiceStatus.PENDING.toString()).count()
                + dal.fetchInvoiceByStatus(InvoiceStatus.FAILED.toString()).count())
    }
}
