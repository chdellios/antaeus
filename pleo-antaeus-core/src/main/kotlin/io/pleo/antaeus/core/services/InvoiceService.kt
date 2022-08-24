package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class InvoiceService(private val dal: AntaeusDal) {

    private val logger = KotlinLogging.logger {}

    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchInvoicesByStatus(invoiceStatus: String): List<Invoice> {
        return dal.fetchInvoiceByStatus(invoiceStatus)
    }

    fun charge(invoiceId: Int, successfulCharge: (invoice: Invoice) -> Boolean): Invoice {

        val invoice = fetch(invoiceId)
        logger.info { "Fetched invoice with Id: ${invoice.id}" }
        val successful = successfulCharge(invoice)
        if (successful) {

            logger.info { "Payment provider charged successfully for invoice : $invoiceId" }
            logger.info { "Updating invoice status to PAID" }
            dal.updateInvoiceStatus(
                    invoice.id,
                    status = InvoiceStatus.PAID
            )
        } else {
            logger.error { "Payment provider could not charge invoice : $invoiceId" }
            logger.info { "Updating invoice status to FAILED" }
            dal.updateInvoiceStatus(
                    invoice.id,
                    status = InvoiceStatus.FAILED
            )
        }
        return fetch(invoiceId)
    }
}
