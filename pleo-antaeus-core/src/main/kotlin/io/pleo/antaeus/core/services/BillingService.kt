package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import java.util.*

class BillingService(
        private val dal: AntaeusDal,
        private val paymentProvider: PaymentProvider,
        private val customerService: CustomerService
) {

    private val logger = KotlinLogging.logger {}

    fun chargeInvoice(invoice: Invoice): List<String> {

        val chargedInvoices: ArrayList<String> = arrayListOf()


        try {
            logger.info {
                "Starting to charge Invoice with Id: ${invoice.id} for ${invoice.amount.value} ${invoice.amount.currency}"
            }

            val customer = customerService.fetch(invoice.customerId)
            if (customer.currency != invoice.amount.currency) {
                throw CurrencyMismatchException(invoiceId = invoice.id, customerId = customer.id)
            }

            logger.info { "Updating invoice status" }
            dal.updateInvoiceStatus(
                    invoice.id,
                    status = InvoiceStatus.PAID
            )
            logger.info { "Payment successfully for $invoice at ${Date()}" }

            paymentProvider.charge(invoice)

        } catch (e: Exception) {
            logger.error(e) { "Unexpected Error: Invoice Charge" }
        }
        return chargedInvoices
    }
}
