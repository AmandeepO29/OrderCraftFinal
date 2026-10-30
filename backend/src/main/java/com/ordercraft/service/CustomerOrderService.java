package com.ordercraft.service;

import com.ordercraft.dto.request.CustomerOrderRequest;
import com.ordercraft.dto.request.OrderStatusUpdateRequest;
import com.ordercraft.dto.response.CustomerOrderResponse;
import com.ordercraft.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerOrderService {
    Page<CustomerOrderResponse> getAllOrders(String query, OrderStatus status, Pageable pageable);
    CustomerOrderResponse getOrderById(Long id);
    CustomerOrderResponse getOrderByNumber(String orderNumber);
    CustomerOrderResponse createOrder(CustomerOrderRequest request);
    CustomerOrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request);
    void cancelOrder(Long id);
}
