package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class InvoicingOperator (
        private val billingService: BillingService,
        private val invoiceService: InvoiceService
) {


    private val logger = KotlinLogging.logger {}

    fun chargeInvoices(invoiceStatus: String) {

        var invoices = listOf<Invoice>()

        if (invoiceStatus == InvoiceStatus.PENDING.toString()) {
            logger.info { "Fetching $invoiceStatus invoices to charge" }
            invoices = invoiceService.fetchInvoicesByStatus(InvoiceStatus.PENDING.toString())
        } else if (invoiceStatus == InvoiceStatus.FAILED.toString()) {
            logger.info { "Fetching $invoiceStatus invoices to charge" }
            invoices = invoiceService.fetchInvoicesByStatus(InvoiceStatus.FAILED.toString())
        }

        logger.info { "${invoices.count()} invoices to charge ${invoices.map { it.id }}" }
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
