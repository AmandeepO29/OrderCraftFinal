package com.ordercraft.service;

import com.ordercraft.dto.request.CustomerRequest;
import com.ordercraft.dto.response.CustomerOrderResponse;
import com.ordercraft.dto.response.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService {
    Page<CustomerResponse> getAllCustomers(String query, Pageable pageable);
    CustomerResponse getCustomerById(Long id);
    CustomerResponse createCustomer(CustomerRequest request);
    CustomerResponse updateCustomer(Long id, CustomerRequest request);
    void deleteCustomer(Long id);
    List<CustomerOrderResponse> getCustomerOrders(Long customerId);
}
