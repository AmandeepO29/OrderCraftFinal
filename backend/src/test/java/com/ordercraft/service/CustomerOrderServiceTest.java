package com.ordercraft.service;

import com.ordercraft.dto.request.CustomerOrderRequest;
import com.ordercraft.dto.request.OrderItemRequest;
import com.ordercraft.dto.response.CustomerOrderResponse;
import com.ordercraft.entity.Customer;
import com.ordercraft.entity.CustomerOrder;
import com.ordercraft.entity.Product;
import com.ordercraft.repository.CustomerOrderRepository;
import com.ordercraft.repository.CustomerRepository;
import com.ordercraft.repository.InvoiceRepository;
import com.ordercraft.repository.ProductRepository;
import com.ordercraft.service.impl.CustomerOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerOrderServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private CustomerOrderServiceImpl customerOrderService;

    private Customer customer;
    private Product product;

    @BeforeEach
    void setUp() {
        customer = new Customer("C-1", "SpaceX", "procurement@spacex.com", "+1-555", "SpaceX Inc", "Hawthorne, CA", "Starbase, TX");
        customer.setId(10L);

        product = new Product("PROD-01", "Rocket Valve", "Aerospace valve", "Propulsion", new BigDecimal("1000.00"), 10);
        product.setId(20L);
    }

    @Test
    @DisplayName("Should create customer order with correct subtotal, tax calculation, and line totals")
    void testCreateOrder() {
        CustomerOrderRequest request = new CustomerOrderRequest();
        request.setCustomerId(10L);
        request.setOrderDate(LocalDate.now());

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(20L);
        itemReq.setQuantity(2);
        request.setItems(List.of(itemReq));

        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerOrderResponse response = customerOrderService.createOrder(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("2000.00"), response.getSubtotal());
        assertEquals(new BigDecimal("200.00"), response.getTaxAmount()); // 10% tax
        assertEquals(new BigDecimal("2200.00"), response.getTotalAmount());
        verify(customerOrderRepository, times(1)).save(any(CustomerOrder.class));
    }
}
