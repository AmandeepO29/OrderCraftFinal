package com.ordercraft.controller;

import com.ordercraft.dto.request.CustomerOrderRequest;
import com.ordercraft.dto.request.OrderStatusUpdateRequest;
import com.ordercraft.dto.response.CustomerOrderResponse;
import com.ordercraft.entity.OrderStatus;
import com.ordercraft.service.CustomerOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Customer Orders", description = "Customer order processing, line items, and lifecycle workflow")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;

    public CustomerOrderController(CustomerOrderService customerOrderService) {
        this.customerOrderService = customerOrderService;
    }

    @GetMapping
    @Operation(summary = "Get paginated customer orders with search and status filter")
    public ResponseEntity<Page<CustomerOrderResponse>> getAllOrders(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(customerOrderService.getAllOrders(query, status, pageRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details by ID")
    public ResponseEntity<CustomerOrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(customerOrderService.getOrderById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create customer order with products (Admin or Manager)")
    public ResponseEntity<CustomerOrderResponse> createOrder(@Valid @RequestBody CustomerOrderRequest request) {
        CustomerOrderResponse created = customerOrderService.createOrder(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update customer order status (PENDING, CONFIRMED, IN_PRODUCTION, COMPLETED, CANCELLED)")
    public ResponseEntity<CustomerOrderResponse> updateStatus(@PathVariable Long id,
                                                             @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(customerOrderService.updateOrderStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel customer order (Admin only)")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        customerOrderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}
