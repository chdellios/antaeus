package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.data.setupInitialData
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.javamoney.moneta.convert.ecb.ECBHistoricRateProvider
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Connection
import javax.money.convert.ConversionQueryBuilder
import javax.money.convert.ExchangeRateProvider

val customerNotFound = createInvoice(
        id = 1,
        customerId = 1,
        status = InvoiceStatus.PENDING
)
val networkException = createInvoice(
        id = 21,
        customerId = 3,
        status = InvoiceStatus.PENDING
)
val chargedSuccess = createInvoice(
        id = 31,
        customerId = 4,
        status = InvoiceStatus.PENDING
)

class BillingServiceTest {
    // Create tables for the test
    private val tables = arrayOf(InvoiceTable, CustomerTable)

    private val db = Database.connect("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;",
            "org.h2.Driver", "root", "")
    private val dal = AntaeusDal(db = db)

    private fun createBillingService(
            invoice: Invoice,
            chargeInvoiceMock: PaymentProvider.(invoice: Invoice) -> Unit = {
                every { charge(it) } returns false
            },
            fetchInvoiceMock: InvoiceService.(invoice: Invoice) -> Unit = {
                every { fetch(it.id) } returns it
            },
            fetchCustomerMock: CustomerService.(invoice: Invoice) -> Unit = {
                every { fetch(it.customerId) } returns Customer(it.customerId, it.amount.currency)
            }
    ): BillingService {
        val paymentProvider = mockk<PaymentProvider> { chargeInvoiceMock(invoice) }
        val invoiceService = spyk(InvoiceService(dal)) { fetchInvoiceMock(invoice) }
        val customerService = mockk<CustomerService> { fetchCustomerMock(invoice) }

        return BillingService(paymentProvider, customerService, invoiceService)
    }

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
    internal fun `non existing customer`() {
        val billingService = createBillingService(
                invoice = customerNotFound,
                chargeInvoiceMock = { invoice ->
                    every { charge(invoice) } throws CustomerNotFoundException(invoice.customerId)
                }
        )
        assertThrows<CustomerNotFoundException> {
            billingService.chargeInvoice(customerNotFound)
        }
        assertChargingInvoice(customerNotFound, invoiceStatus = "PENDING")
    }

    @Test
    internal fun `network exception error`() {
        val billingService = createBillingService(
                invoice = networkException,
                chargeInvoiceMock = {
                    every { charge(it) } throws NetworkException()
                }
        )
        assertThrows<NetworkException> {
            billingService.chargeInvoice(networkException)
        }
        assertChargingInvoice(networkException, invoiceStatus = "PENDING")
    }

    @Test
    internal fun `charging success`() {
        val billingService = createBillingService(
                invoice = chargedSuccess,
                chargeInvoiceMock = {
                    every { charge(it) } returns true
                }
        )
        billingService.chargeInvoice(chargedSuccess)
        assertChargingInvoice(chargedSuccess, invoiceStatus = "PAID")
    }

    @Test
    internal fun `currency conversion when currency mismatch`() {
        val ecb: ExchangeRateProvider = ECBHistoricRateProvider()
        val exr = ecb.getExchangeRate(ConversionQueryBuilder.of()
                .setBaseCurrency("DKK").setTermCurrency("EUR").build())
        val dkk = 20.0
        val euroToDkk = BigDecimal.valueOf(exr.factor.toDouble()).multiply(BigDecimal.valueOf(dkk))
        Assertions.assertEquals("EUR 2.68", "EUR ${euroToDkk.setScale(2, RoundingMode.FLOOR)}")
        Assertions.assertNotNull(euroToDkk)
    }

    private fun assertChargingInvoice(invoice: Invoice, invoiceStatus: String) {
        val dbInvoice = dal.fetchInvoice(invoice.id)
        Assertions.assertEquals(invoiceStatus, dbInvoice?.status.toString())
    }
}
