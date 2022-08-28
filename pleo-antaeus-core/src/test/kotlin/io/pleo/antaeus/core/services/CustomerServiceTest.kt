package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CustomerServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchCustomer(404) } returns null
        every { fetchCustomers() } returns (0..10).map { createCustomer() }
        every { fetchCustomer(12) } returns  createSingleCustomer()
    }

    private val customerService = CustomerService(dal = dal)

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<CustomerNotFoundException> {
            customerService.fetch(404)
        }
    }

    @Test
    fun `will return all customers`() {
        Assertions.assertEquals(customerService.fetchAll().size, 11)
    }

    @Test
    fun `will return a single customer`() {
        val customer = customerService.fetch(12)
        Assertions.assertEquals(customer.id, 12)
    }
}
