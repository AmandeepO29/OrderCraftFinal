package com.ordercraft.service;

import com.ordercraft.dto.request.ProductionOrderRequest;
import com.ordercraft.dto.request.ProductionStatusUpdateRequest;
import com.ordercraft.dto.response.ProductionOrderResponse;
import com.ordercraft.entity.ProductionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductionOrderService {
    Page<ProductionOrderResponse> getAllProductionOrders(String query, ProductionStatus status, Pageable pageable);
    ProductionOrderResponse getProductionOrderById(Long id);
    ProductionOrderResponse createProductionOrder(ProductionOrderRequest request);
    ProductionOrderResponse startProduction(Long id);
    ProductionOrderResponse completeProduction(Long id, ProductionStatusUpdateRequest request, String currentUsername);
    void cancelProductionOrder(Long id);
}
