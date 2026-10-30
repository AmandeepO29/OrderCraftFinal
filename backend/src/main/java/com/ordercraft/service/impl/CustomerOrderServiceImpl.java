package com.ordercraft.service.impl;

import com.ordercraft.dto.request.CustomerOrderRequest;
import com.ordercraft.dto.request.OrderItemRequest;
import com.ordercraft.dto.request.OrderStatusUpdateRequest;
import com.ordercraft.dto.response.CustomerOrderResponse;
import com.ordercraft.dto.response.OrderItemResponse;
import com.ordercraft.entity.Customer;
import com.ordercraft.entity.CustomerOrder;
import com.ordercraft.entity.Invoice;
import com.ordercraft.entity.OrderItem;
import com.ordercraft.entity.OrderStatus;
import com.ordercraft.entity.Product;
import com.ordercraft.exception.BusinessRuleException;
import com.ordercraft.exception.ResourceNotFoundException;
import com.ordercraft.repository.CustomerOrderRepository;
import com.ordercraft.repository.CustomerRepository;
import com.ordercraft.repository.InvoiceRepository;
import com.ordercraft.repository.ProductRepository;
import com.ordercraft.service.CustomerOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;

    public CustomerOrderServiceImpl(CustomerOrderRepository customerOrderRepository,
                                    CustomerRepository customerRepository,
                                    ProductRepository productRepository,
                                    InvoiceRepository invoiceRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerOrderResponse> getAllOrders(String query, OrderStatus status, Pageable pageable) {
        Page<CustomerOrder> orders;
        if (StringUtils.hasText(query)) {
            orders = customerOrderRepository.searchOrders(query.trim(), pageable);
        } else if (status != null) {
            orders = customerOrderRepository.findByStatus(status, pageable);
        } else {
            orders = customerOrderRepository.findAll(pageable);
        }
        return orders.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOrderResponse getOrderById(Long id) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer order not found with id: " + id));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOrderResponse getOrderByNumber(String orderNumber) {
        CustomerOrder order = customerOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer order not found with order number: " + orderNumber));
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public CustomerOrderResponse createOrder(CustomerOrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        String orderNum = request.getOrderNumber();
        if (!StringUtils.hasText(orderNum)) {
            orderNum = "ORD-" + System.currentTimeMillis() % 1000000;
        } else if (customerOrderRepository.existsByOrderNumber(orderNum)) {
            throw new BusinessRuleException("Order number '" + orderNum + "' already exists");
        }

        CustomerOrder order = new CustomerOrder(
                orderNum,
                customer,
                request.getOrderDate(),
                request.getExpectedDeliveryDate(),
                request.getNotes()
        );

        if (request.getDiscountAmount() != null) {
            order.setDiscountAmount(request.getDiscountAmount());
        }

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.getProductId()));

            BigDecimal unitPrice = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : product.getUnitPrice();
            OrderItem item = new OrderItem(product, itemReq.getQuantity(), unitPrice);
            order.addItem(item);
        }

        order.recalculateTotals();
        CustomerOrder savedOrder = customerOrderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional
    public CustomerOrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer order not found with id: " + id));

        order.setStatus(request.getStatus());
        if (StringUtils.hasText(request.getNotes())) {
            order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + request.getNotes());
        }

        CustomerOrder updated = customerOrderRepository.save(order);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer order not found with id: " + id));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel an order that has already been completed.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        customerOrderRepository.save(order);
    }

    private CustomerOrderResponse mapToResponse(CustomerOrder order) {
        CustomerOrderResponse res = new CustomerOrderResponse();
        res.setId(order.getId());
        res.setOrderNumber(order.getOrderNumber());
        res.setCustomerId(order.getCustomer().getId());
        res.setCustomerName(order.getCustomer().getName());
        res.setCustomerCompanyName(order.getCustomer().getCompanyName());
        res.setCustomerEmail(order.getCustomer().getEmail());
        res.setCustomerPhone(order.getCustomer().getPhone());
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

        Optional<Invoice> invoiceOpt = invoiceRepository.findByCustomerOrderId(order.getId());
        if (invoiceOpt.isPresent()) {
            res.setInvoiceId(invoiceOpt.get().getId());
            res.setInvoiceNumber(invoiceOpt.get().getInvoiceNumber());
        }

        return res;
    }
}
