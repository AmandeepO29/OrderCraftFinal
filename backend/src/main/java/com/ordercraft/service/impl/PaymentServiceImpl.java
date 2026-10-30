package com.ordercraft.service.impl;

import com.ordercraft.dto.request.PaymentRequest;
import com.ordercraft.dto.response.PaymentResponse;
import com.ordercraft.entity.Invoice;
import com.ordercraft.entity.Payment;
import com.ordercraft.entity.User;
import com.ordercraft.exception.BusinessRuleException;
import com.ordercraft.exception.ResourceNotFoundException;
import com.ordercraft.repository.InvoiceRepository;
import com.ordercraft.repository.PaymentRepository;
import com.ordercraft.repository.UserRepository;
import com.ordercraft.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              InvoiceRepository invoiceRepository,
                              UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(String query, Pageable pageable) {
        Page<Payment> payments;
        if (StringUtils.hasText(query)) {
            payments = paymentRepository.searchPayments(query.trim(), pageable);
        } else {
            payments = paymentRepository.findAll(pageable);
        }
        return payments.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByInvoiceId(Long invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("Invoice not found with id: " + invoiceId);
        }
        return paymentRepository.findByInvoiceId(invoiceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentResponse recordPayment(PaymentRequest request, String currentUsername) {
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + request.getInvoiceId()));

        if (invoice.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Invoice " + invoice.getInvoiceNumber() + " is already fully paid.");
        }

        if (request.getAmount().compareTo(invoice.getBalanceDue()) > 0) {
            throw new BusinessRuleException("Payment amount (" + request.getAmount() + ") cannot exceed outstanding balance due (" + invoice.getBalanceDue() + ")");
        }

        String ref = request.getPaymentReference();
        if (!StringUtils.hasText(ref)) {
            ref = "PAY-" + System.currentTimeMillis() % 1000000;
        }

        User user = null;
        if (StringUtils.hasText(currentUsername)) {
            user = userRepository.findByUsername(currentUsername).orElse(null);
        }

        LocalDate pDate = request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now();

        Payment payment = new Payment(
                ref,
                invoice,
                pDate,
                request.getAmount(),
                request.getPaymentMethod(),
                request.getTransactionNumber(),
                request.getNotes(),
                user
        );

        invoice.addPayment(payment);
        Payment saved = paymentRepository.save(payment);
        invoiceRepository.save(invoice);

        return mapToResponse(saved);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse res = new PaymentResponse();
        res.setId(payment.getId());
        res.setPaymentReference(payment.getPaymentReference());
        res.setInvoiceId(payment.getInvoice().getId());
        res.setInvoiceNumber(payment.getInvoice().getInvoiceNumber());
        if (payment.getInvoice().getCustomerOrder() != null && payment.getInvoice().getCustomerOrder().getCustomer() != null) {
            res.setCustomerName(payment.getInvoice().getCustomerOrder().getCustomer().getName());
        }
        res.setPaymentDate(payment.getPaymentDate());
        res.setAmount(payment.getAmount());
        res.setPaymentMethod(payment.getPaymentMethod());
        res.setTransactionNumber(payment.getTransactionNumber());
        res.setNotes(payment.getNotes());
        res.setRecordedByUsername(payment.getRecordedBy() != null ? payment.getRecordedBy().getUsername() : "System");
        res.setCreatedAt(payment.getCreatedAt());
        return res;
    }
}
