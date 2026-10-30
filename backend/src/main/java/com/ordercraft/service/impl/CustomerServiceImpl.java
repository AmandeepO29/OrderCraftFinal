package com.ordercraft.service.impl;

import com.ordercraft.dto.request.CustomerRequest;
import com.ordercraft.dto.response.CustomerOrderResponse;
import com.ordercraft.dto.response.CustomerResponse;
import com.ordercraft.dto.response.OrderItemResponse;
import com.ordercraft.entity.Customer;
import com.ordercraft.entity.CustomerOrder;
import com.ordercraft.exception.BusinessRuleException;
import com.ordercraft.exception.ResourceNotFoundException;
import com.ordercraft.repository.CustomerOrderRepository;
import com.ordercraft.repository.CustomerRepository;
import com.ordercraft.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderRepository customerOrderRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository, CustomerOrderRepository customerOrderRepository) {
        this.customerRepository = customerRepository;
        this.customerOrderRepository = customerOrderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(String query, Pageable pageable) {
        Page<Customer> customers;
        if (StringUtils.hasText(query)) {
            customers = customerRepository.searchCustomers(query.trim(), pageable);
        } else {
            customers = customerRepository.findByActiveTrue(pageable);
        }
        return customers.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return mapToResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        String code = request.getCustomerCode();
        if (!StringUtils.hasText(code)) {
            code = "CUST-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } else if (customerRepository.existsByCustomerCode(code)) {
            throw new BusinessRuleException("Customer with code '" + code + "' already exists");
        }

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Customer with email '" + request.getEmail() + "' already exists");
        }

        Customer customer = new Customer(
                code,
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                request.getCompanyName(),
                request.getBillingAddress(),
                request.getShippingAddress()
        );
        customer.setTaxId(request.getTaxId());

        Customer saved = customerRepository.save(customer);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setCompanyName(request.getCompanyName());
        customer.setTaxId(request.getTaxId());
        customer.setBillingAddress(request.getBillingAddress());
        customer.setShippingAddress(request.getShippingAddress());

        Customer updated = customerRepository.save(customer);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        customer.setActive(false);
        customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrderResponse> getCustomerOrders(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        List<CustomerOrder> orders = customerOrderRepository.findByCustomerId(customerId);
        return orders.stream().map(this::mapOrderToResponse).collect(Collectors.toList());
    }

    private CustomerResponse mapToResponse(Customer customer) {
        CustomerResponse res = new CustomerResponse();
        res.setId(customer.getId());
        res.setCustomerCode(customer.getCustomerCode());
        res.setName(customer.getName());
        res.setEmail(customer.getEmail());
        res.setPhone(customer.getPhone());
        res.setCompanyName(customer.getCompanyName());
        res.setTaxId(customer.getTaxId());
        res.setBillingAddress(customer.getBillingAddress());
        res.setShippingAddress(customer.getShippingAddress());
        res.setActive(customer.isActive());
        res.setCreatedAt(customer.getCreatedAt());
        res.setTotalOrdersCount(customerOrderRepository.findByCustomerId(customer.getId()).size());
        return res;
    }

    private CustomerOrderResponse mapOrderToResponse(CustomerOrder order) {
        CustomerOrderResponse res = new CustomerOrderResponse();
        res.setId(order.getId());
        res.setOrderNumber(order.getOrderNumber());
        res.setCustomerId(order.getCustomer().getId());
        res.setCustomerName(order.getCustomer().getName());
        res.setCustomerCompanyName(order.getCustomer().getCompanyName());
        res.setOrderDate(order.getOrderDate());
        res.setExpectedDeliveryDate(order.getExpectedDeliveryDate());
        res.setStatus(order.getStatus());
        res.setSubtotal(order.getSubtotal());
        res.setTaxAmount(order.getTaxAmount());
        res.setDiscountAmount(order.getDiscountAmount());
        res.setTotalAmount(order.getTotalAmount());
        res.setNotes(order.getNotes());
        res.setCreatedAt(order.getCreatedAt());

        if (order.getItems() != null) {
            res.setItems(order.getItems().stream().map(item -> {
                OrderItemResponse ir = new OrderItemResponse();
                ir.setId(item.getId());
                ir.setProductId(item.getProduct().getId());
                ir.setProductSku(item.getProduct().getSku());
                ir.setProductName(item.getProduct().getName());
                ir.setQuantity(item.getQuantity());
                ir.setUnitPrice(item.getUnitPrice());
                ir.setLineTotal(item.getLineTotal());
                return ir;
            }).collect(Collectors.toList()));
        }
        return res;
    }
}
