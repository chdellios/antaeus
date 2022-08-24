/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.InvoicingOperator
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class AntaeusRest(
        private val invoiceService: InvoiceService,
        private val customerService: CustomerService,
        private val invoicingOperator: InvoicingOperator
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
            .create()
            .apply {
                // InvoiceNotFoundException: return 404 HTTP status code
                exception(EntityNotFoundException::class.java) { _, ctx ->
                    ctx.status(404)
                }
                // Unexpected exception: return HTTP 500
                exception(Exception::class.java) { e, _ ->
                    logger.error(e) { "Internal server error" }
                }
                // On 404: return message
                error(404) { ctx -> ctx.json("not found") }
            }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
            get("/") {
                it.result("Welcome to Antaeus! see AntaeusRest class for routes")
            }
            path("rest") {
                // Route to check whether the app is running
                // URL: /rest/health
                get("health") {
                    it.json("ok")
                }

                // V1
                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        get {
                            it.json(invoiceService.fetchAll())
                        }

                        // URL: /rest/v1/invoices/{:id}
                        get(":id") {
                            it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                        }
                    }

                    path("charging") {
                        path("pending") {
                            // URL: /rest/v1/charging/pending
                            get {

                                it.json(invoicingOperator.chargeInvoices(InvoiceStatus.PENDING.toString()))

                            }
                        }
                        path("failed") {
                            // URL: /rest/v1/charging/failed
                            get {

                                it.json(invoicingOperator.chargeInvoices(InvoiceStatus.FAILED.toString()))

                            }
                        }
                    }

                    path("customers") {
                        // URL: /rest/v1/customers
                        get {
                            it.json(customerService.fetchAll())
                        }

                        // URL: /rest/v1/customers/{:id}
                        get(":id") {
                            it.json(customerService.fetch(it.pathParam("id").toInt()))
                        }
                    }
                }
            }
        }
    }
}
