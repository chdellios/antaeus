package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.InvoiceStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
        every { fetchInvoices() } returns (0..10).map { createInvoice() }
        every { fetchInvoice(12) } returns createSingleInvoice()
        every { fetchInvoiceByStatus(InvoiceStatus.PENDING.toString()) } returns (13..15).map { createPendingInvoice() }
        every { fetchInvoiceByStatus(InvoiceStatus.FAILED.toString()) } returns (16..20).map { createFailedInvoice() }

    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will return all invoices`() {
        Assertions.assertEquals(invoiceService.fetchAll().size, 11)
    }

    @Test
    fun `will return single invoice`() {
        val invoice = invoiceService.fetch(12)
        Assertions.assertEquals(invoice.id, 12)
    }

    @Test
    fun `will return pending invoices`() {
        val invoices = invoiceService.fetchInvoicesByStatus(InvoiceStatus.PENDING.toString())
        Assertions.assertEquals(invoices.size, 3)
    }

    @Test
    fun `will return failed invoices`() {
        val invoices = invoiceService.fetchInvoicesByStatus(InvoiceStatus.FAILED.toString())
        Assertions.assertEquals(invoices.size, 5)
    }
}
