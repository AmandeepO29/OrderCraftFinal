package com.ordercraft.controller;

import com.ordercraft.dto.request.PurchaseOrderRequest;
import com.ordercraft.dto.request.ReceivePurchaseOrderRequest;
import com.ordercraft.dto.response.PurchaseOrderResponse;
import com.ordercraft.entity.PurchaseOrderStatus;
import com.ordercraft.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-orders")
@Tag(name = "Purchase Orders", description = "Procurement of raw materials from suppliers and automated inventory receiving")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping
    @Operation(summary = "Get paginated purchase orders with search and status filter")
    public ResponseEntity<Page<PurchaseOrderResponse>> getAllPurchaseOrders(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders(query, status, pageRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase order details by ID")
    public ResponseEntity<PurchaseOrderResponse> getPurchaseOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create purchase order for raw materials (Admin or Manager)")
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrderResponse created = purchaseOrderService.createPurchaseOrder(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/order")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Submit draft purchase order to supplier")
    public ResponseEntity<PurchaseOrderResponse> markAsOrdered(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.markAsOrdered(id));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Record receipt of raw materials and automatically update inventory and stock logs")
    public ResponseEntity<PurchaseOrderResponse> receivePurchaseOrder(@PathVariable Long id,
                                                                     @RequestBody(required = false) ReceivePurchaseOrderRequest request,
                                                                     Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "System";
        return ResponseEntity.ok(purchaseOrderService.receivePurchaseOrder(id, request, username));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel purchase order (Admin only)")
    public ResponseEntity<Void> cancelPurchaseOrder(@PathVariable Long id) {
        purchaseOrderService.cancelPurchaseOrder(id);
        return ResponseEntity.noContent().build();
    }
}
