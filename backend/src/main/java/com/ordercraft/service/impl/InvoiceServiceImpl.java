package com.ordercraft.service.impl;

import com.ordercraft.dto.request.InvoiceRequest;
import com.ordercraft.dto.response.InvoiceResponse;
import com.ordercraft.dto.response.OrderItemResponse;
import com.ordercraft.dto.response.PaymentResponse;
import com.ordercraft.entity.CustomerOrder;
import com.ordercraft.entity.Invoice;
import com.ordercraft.entity.InvoiceStatus;
import com.ordercraft.exception.BusinessRuleException;
import com.ordercraft.exception.ResourceNotFoundException;
import com.ordercraft.repository.CustomerOrderRepository;
import com.ordercraft.repository.InvoiceRepository;
import com.ordercraft.repository.PaymentRepository;
import com.ordercraft.service.InvoiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              CustomerOrderRepository customerOrderRepository,
                              PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAllInvoices(String query, InvoiceStatus status, Pageable pageable) {
        Page<Invoice> invoices;
        if (StringUtils.hasText(query)) {
            invoices = invoiceRepository.searchInvoices(query.trim(), pageable);
        } else if (status != null) {
            invoices = invoiceRepository.findByStatus(status, pageable);
        } else {
            invoices = invoiceRepository.findAll(pageable);
        }
        return invoices.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        return mapToResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByOrderId(Long orderId) {
        Invoice invoice = invoiceRepository.findByCustomerOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No invoice found for customer order id: " + orderId));
        return mapToResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse generateInvoice(InvoiceRequest request) {
        CustomerOrder order = customerOrderRepository.findById(request.getCustomerOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer order not found with id: " + request.getCustomerOrderId()));

        if (invoiceRepository.findByCustomerOrderId(order.getId()).isPresent()) {
            throw new BusinessRuleException("An invoice already exists for customer order " + order.getOrderNumber());
        }

        String invNum = request.getInvoiceNumber();
        if (!StringUtils.hasText(invNum)) {
            invNum = "INV-" + order.getOrderNumber().replace("ORD-", "") + "-" + System.currentTimeMillis() % 10000;
        } else if (invoiceRepository.existsByInvoiceNumber(invNum)) {
            throw new BusinessRuleException("Invoice number '" + invNum + "' already exists");
        }

        LocalDate invDate = request.getInvoiceDate() != null ? request.getInvoiceDate() : LocalDate.now();
        LocalDate dueDate = request.getDueDate() != null ? request.getDueDate() : invDate.plusDays(30);

        BigDecimal subtotal = order.getSubtotal().subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            subtotal = BigDecimal.ZERO;
        }

        BigDecimal taxRate = request.getTaxRate() != null ? request.getTaxRate() : new BigDecimal("10.00");

        Invoice invoice = new Invoice(
                invNum,
                order,
                invDate,
                dueDate,
                subtotal,
                taxRate,
                request.getNotes()
        );

        Invoice saved = invoiceRepository.save(invoice);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void cancelInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        if (invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Cannot cancel an invoice with recorded payments");
        }

        invoiceRepository.delete(invoice);
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        InvoiceResponse res = new InvoiceResponse();
        res.setId(invoice.getId());
        res.setInvoiceNumber(invoice.getInvoiceNumber());
        res.setCustomerOrderId(invoice.getCustomerOrder().getId());
        res.setCustomerOrderNumber(invoice.getCustomerOrder().getOrderNumber());
        res.setCustomerName(invoice.getCustomerOrder().getCustomer().getName());
        res.setCustomerCompanyName(invoice.getCustomerOrder().getCustomer().getCompanyName());
        res.setCustomerEmail(invoice.getCustomerOrder().getCustomer().getEmail());
        res.setBillingAddress(invoice.getCustomerOrder().getCustomer().getBillingAddress());
        res.setInvoiceDate(invoice.getInvoiceDate());
        res.setDueDate(invoice.getDueDate());
        res.setStatus(invoice.getStatus());
        res.setSubtotal(invoice.getSubtotal());
        res.setTaxRate(invoice.getTaxRate());
        res.setTaxAmount(invoice.getTaxAmount());
        res.setTotalAmount(invoice.getTotalAmount());
        res.setPaidAmount(invoice.getPaidAmount());
        res.setBalanceDue(invoice.getBalanceDue());
        res.setNotes(invoice.getNotes());
        res.setCreatedAt(invoice.getCreatedAt());

        if (invoice.getCustomerOrder().getItems() != null) {
            res.setItems(invoice.getCustomerOrder().getItems().stream().map(item -> {
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

        res.setPayments(paymentRepository.findByInvoiceId(invoice.getId()).stream().map(p -> {
            PaymentResponse pr = new PaymentResponse();
            pr.setId(p.getId());
            pr.setPaymentReference(p.getPaymentReference());
            pr.setInvoiceId(invoice.getId());
            pr.setInvoiceNumber(invoice.getInvoiceNumber());
            pr.setCustomerName(invoice.getCustomerOrder().getCustomer().getName());
            pr.setPaymentDate(p.getPaymentDate());
            pr.setAmount(p.getAmount());
            pr.setPaymentMethod(p.getPaymentMethod());
            pr.setTransactionNumber(p.getTransactionNumber());
            pr.setNotes(p.getNotes());
            pr.setRecordedByUsername(p.getRecordedBy() != null ? p.getRecordedBy().getUsername() : "System");
            pr.setCreatedAt(p.getCreatedAt());
            return pr;
        }).collect(Collectors.toList()));

        return res;
    }
}
