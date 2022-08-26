/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.InvoicingOperator
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.runBlocking
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
            .create() {
                it.accessManager(Auth::accessManager)
            }
            .apply {
                // InvoiceNotFoundException: return 404 HTTP status code
                exception(EntityNotFoundException::class.java) { _, ctx ->
                    ctx.status(404)
                }
                // CustomerNotFoundException: return 404 HTTP status code
                exception(CustomerNotFoundException::class.java) { _, ctx ->
                    ctx.status(404)
                }
                // CurrencyMismatchException: return 400 HTTP status code
                exception(CurrencyMismatchException::class.java) { _, ctx ->
                    ctx.status(400)
                }
                // Unexpected exception: return HTTP 500
                exception(Exception::class.java) { e, _ ->
                    logger.error(e) { "Internal server error" }
                }
                // On 404: return message
                error(404) { ctx -> ctx.json("not found") }
                // On 400: return message
                error(400) { ctx -> ctx.json("Bad Request: possible currency mismatch") }
                // On 500: return message
                error(500) { ctx -> ctx.json("Internal Server Error") }
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
                get("/health", {
                    it.json("ok")
                }, Role.ANYONE)

                // V1
                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        get({
                            it.json(invoiceService.fetchAll())
                        }, Role.USER_READ
                        )
                        get("{id}", {
                            it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                        }, Role.USER_READ)
                    }

                    path("charging") {
                        path("pending") {
                            // URL: /rest/v1/charging/pending
                            runBlocking {
                                get({
                                    it.json(invoicingOperator.chargeByStatus(InvoiceStatus.PENDING.toString()))
                                }, Role.USER_WRITE)
                            }
                        }
                        path("failed") {
                            // URL: /rest/v1/charging/failed
                            runBlocking {
                                get({
                                    it.json(invoicingOperator.chargeByStatus(InvoiceStatus.FAILED.toString()))
                                }, Role.USER_WRITE)
                            }
                        }
                    }

                    path("customers") {
                        // URL: /rest/v1/customers
                        get({
                            it.json(customerService.fetchAll())
                        }, Role.USER_READ)

                        // URL: /rest/v1/customers/{:id}
                        get("{id}", {
                            it.json(customerService.fetch(it.pathParam("id").toInt()))
                        }, Role.USER_READ)
                    }
                }
            }
        }
    }
}
