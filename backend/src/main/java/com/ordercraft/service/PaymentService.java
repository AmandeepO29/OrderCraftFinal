package com.ordercraft.service;

import com.ordercraft.dto.request.PaymentRequest;
import com.ordercraft.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {
    Page<PaymentResponse> getAllPayments(String query, Pageable pageable);
    PaymentResponse getPaymentById(Long id);
    List<PaymentResponse> getPaymentsByInvoiceId(Long invoiceId);
    PaymentResponse recordPayment(PaymentRequest request, String currentUsername);
}
