package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.Money
import mu.KotlinLogging
import org.javamoney.moneta.convert.ecb.ECBHistoricRateProvider
import java.math.BigDecimal
import javax.money.convert.ConversionQueryBuilder
import javax.money.convert.ExchangeRateProvider


private val logger = KotlinLogging.logger {}

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val customerService: CustomerService,
        private val invoiceService: InvoiceService
) {

    private val logger = KotlinLogging.logger {}

    fun chargeInvoice(invoice: Invoice): Invoice {

        logger.info { "Starting to charge Invoice with Id: ${invoice.id} for ${invoice.amount.value} ${invoice.amount.currency}" }

        val customer = customerService.fetch(invoice.customerId)
        //As a poc I am testing moneta API in order
        //to get the currency exchange rate based on Europe Central Bank
        //and create a converter
        if (customer.currency != invoice.amount.currency) {

            val ecb: ExchangeRateProvider = ECBHistoricRateProvider()
            val exr = ecb.getExchangeRate(ConversionQueryBuilder.of().setBaseCurrency(invoice.amount.currency.toString())
                    .setTermCurrency(customer.currency.toString()).build())
            val amountToCharge = BigDecimal.valueOf(exr.factor.toDouble()).multiply(invoice.amount.value)

            customerService.convertCurrency(customer.id, Money(amountToCharge.setScale(2), customer.currency))
        }

        return invoiceService.charge(invoice.id) { existingInvoice ->
            retry(3, false) {
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
