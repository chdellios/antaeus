package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val customerService: CustomerService,
        private val invoiceService: InvoiceService
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

            invoiceService.charge(invoice.id)
            paymentProvider.charge(invoice)

        } catch (e: Exception) {
            logger.error(e) { "Unexpected Error: Invoice Charge" }
        }
        return chargedInvoices
    }
}
