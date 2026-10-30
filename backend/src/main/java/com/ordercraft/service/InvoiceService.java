package com.ordercraft.service;

import com.ordercraft.dto.request.InvoiceRequest;
import com.ordercraft.dto.response.InvoiceResponse;
import com.ordercraft.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvoiceService {
    Page<InvoiceResponse> getAllInvoices(String query, InvoiceStatus status, Pageable pageable);
    InvoiceResponse getInvoiceById(Long id);
    InvoiceResponse getInvoiceByOrderId(Long orderId);
    InvoiceResponse generateInvoice(InvoiceRequest request);
    void cancelInvoice(Long id);
}
