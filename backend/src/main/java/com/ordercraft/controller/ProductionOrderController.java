package com.ordercraft.controller;

import com.ordercraft.dto.request.ProductionOrderRequest;
import com.ordercraft.dto.request.ProductionStatusUpdateRequest;
import com.ordercraft.dto.response.ProductionOrderResponse;
import com.ordercraft.entity.ProductionStatus;
import com.ordercraft.service.ProductionOrderService;
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
@RequestMapping("/api/production")
@Tag(name = "Production Management", description = "Shop floor production scheduling, BOM raw material consumption, and finished goods inventory replenishment")
public class ProductionOrderController {

    private final ProductionOrderService productionOrderService;

    public ProductionOrderController(ProductionOrderService productionOrderService) {
        this.productionOrderService = productionOrderService;
    }

    @GetMapping
    @Operation(summary = "Get paginated production orders with search and status filter")
    public ResponseEntity<Page<ProductionOrderResponse>> getAllProductionOrders(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ProductionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(productionOrderService.getAllProductionOrders(query, status, pageRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get production order details by ID")
    public ResponseEntity<ProductionOrderResponse> getProductionOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(productionOrderService.getProductionOrderById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create production order plan (Admin or Manager)")
    public ResponseEntity<ProductionOrderResponse> createProductionOrder(@Valid @RequestBody ProductionOrderRequest request) {
        ProductionOrderResponse created = productionOrderService.createProductionOrder(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Start production run (transitions PLANNED to IN_PROGRESS)")
    public ResponseEntity<ProductionOrderResponse> startProduction(@PathVariable Long id) {
        return ResponseEntity.ok(productionOrderService.startProduction(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Complete production order: deducts raw materials using BOM, adds finished goods to inventory, and logs audit transactions")
    public ResponseEntity<ProductionOrderResponse> completeProduction(@PathVariable Long id,
                                                                      @RequestBody(required = false) ProductionStatusUpdateRequest request,
                                                                      Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "System";
        return ResponseEntity.ok(productionOrderService.completeProduction(id, request, username));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel production order (Admin only)")
    public ResponseEntity<Void> cancelProductionOrder(@PathVariable Long id) {
        productionOrderService.cancelProductionOrder(id);
        return ResponseEntity.noContent().build();
    }
}
