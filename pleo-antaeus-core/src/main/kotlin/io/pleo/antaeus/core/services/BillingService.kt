package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val customerService: CustomerService,
        private val invoiceService: InvoiceService
) {

    private val logger = KotlinLogging.logger {}

    fun chargeInvoice(invoice: Invoice): Invoice {

        logger.info {
            "Starting to charge Invoice with Id: ${invoice.id} for ${invoice.amount.value} ${invoice.amount.currency}"
        }

        val customer = customerService.fetch(invoice.customerId)
        if (customer.currency != invoice.amount.currency) {
            throw CurrencyMismatchException(invoiceId = invoice.id, customerId = customer.id)
        }

        return invoiceService.charge(invoice.id) { existingInvoice ->
            retry(2, false) {
                paymentProvider.charge(existingInvoice)
            }
        }
    }
}

fun <T> retry(
        times: Int,
        failureValue: T,
        block: () -> T
): T {

    return (1..times).fold(failureValue) { _, retryNum ->
        try {
            return block()
        } catch (e: NetworkException) {
            logger.error(e) { "Trying again... $retryNum" }
            return@fold if (retryNum == times) throw e else failureValue
        }
    }
}
