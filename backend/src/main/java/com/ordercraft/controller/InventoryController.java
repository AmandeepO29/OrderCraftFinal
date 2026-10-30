package com.ordercraft.controller;

import com.ordercraft.dto.request.StockAdjustmentRequest;
import com.ordercraft.dto.response.InventoryResponse;
import com.ordercraft.dto.response.InventoryTransactionResponse;
import com.ordercraft.entity.InventoryItemType;
import com.ordercraft.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Stock tracking for raw materials and finished products, manual stock adjustments, and audit trail")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "Get paginated inventory with optional filtering by item type and search query")
    public ResponseEntity<Page<InventoryResponse>> getAllInventory(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) InventoryItemType itemType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastUpdated") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(inventoryService.getAllInventory(query, itemType, pageRequest));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock inventory warnings")
    public ResponseEntity<List<InventoryResponse>> getLowStockAlerts() {
        return ResponseEntity.ok(inventoryService.getLowStockAlerts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inventory record by ID")
    public ResponseEntity<InventoryResponse> getInventoryById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Manually adjust inventory stock (ADD or DEDUCT) with audit logging")
    public ResponseEntity<InventoryResponse> adjustStock(@Valid @RequestBody StockAdjustmentRequest request,
                                                         Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "System";
        return ResponseEntity.ok(inventoryService.adjustStock(request, username));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get paginated inventory transaction audit history")
    public ResponseEntity<Page<InventoryTransactionResponse>> getTransactions(
            @RequestParam(required = false) Long inventoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return ResponseEntity.ok(inventoryService.getTransactions(inventoryId, pageRequest));
    }
}
