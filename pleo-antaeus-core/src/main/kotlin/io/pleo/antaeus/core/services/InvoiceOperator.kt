package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging


class InvoicingOperator(
        private val billingService: BillingService,
        private val invoiceService: InvoiceService
) {

    private val logger = KotlinLogging.logger {}

    fun chargeByStatus(invoiceStatus: String) = runBlocking {
        val invoices = getInvoices(invoiceStatus)
        invoices.collect { invoice ->
            invoicingOperator(invoice)
        }
    }

    private fun getInvoices(invoiceStatus: String) = flow {

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
            logger.info { "Emitted invoices: $it " }
            emit(it)
        }
    }

    private fun invoicingOperator(invoice: Invoice) = runBlocking {
        try {
            billingService.chargeInvoice(invoice)
        } catch (e: Exception) {
            logger.error(e) { "Unexpected error during invoice charge" }
            logger.info { "Updating invoice status to FAILED" }
            invoiceService.updateInvoiceByStatus(invoice.id, InvoiceStatus.FAILED)
        }
    }
}
