package com.ordercraft.service;

import com.ordercraft.dto.request.PurchaseOrderRequest;
import com.ordercraft.dto.request.ReceivePurchaseOrderRequest;
import com.ordercraft.dto.response.PurchaseOrderResponse;
import com.ordercraft.entity.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseOrderService {
    Page<PurchaseOrderResponse> getAllPurchaseOrders(String query, PurchaseOrderStatus status, Pageable pageable);
    PurchaseOrderResponse getPurchaseOrderById(Long id);
    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request);
    PurchaseOrderResponse markAsOrdered(Long id);
    PurchaseOrderResponse receivePurchaseOrder(Long id, ReceivePurchaseOrderRequest request, String currentUsername);
    void cancelPurchaseOrder(Long id);
}
