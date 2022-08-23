package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging

class InvoicingOperator (
        private val billingService: BillingService,
        private val invoiceService: InvoiceService
) {


    private val logger = KotlinLogging.logger {}

    fun process() {
        val invoices = invoiceService.fetchPendingInvoices()
        invoices.forEach {
            invoicingOperator(it)
        }

    }

    private fun invoicingOperator(invoice: Invoice) {
        try {
            billingService.chargeInvoice(invoice)
        } catch (e: Exception) {
            logger.error(e) { "Unexpected error during invoice charge" }
        }

    }
}
