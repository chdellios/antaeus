/*
    Implements endpoints related to customers.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Money

class CustomerService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Customer> {
        return dal.fetchCustomers()
    }

    fun fetch(id: Int): Customer {
        return dal.fetchCustomer(id) ?: throw CustomerNotFoundException(id)
    }

    fun convertCurrency(id: Int, amount: Money): Int {
        return dal.convertCustomerCurrency(id, amount)
    }
}
