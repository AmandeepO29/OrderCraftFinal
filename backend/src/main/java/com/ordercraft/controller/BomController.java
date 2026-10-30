package com.ordercraft.controller;

import com.ordercraft.dto.request.BomRequest;
import com.ordercraft.dto.response.BomAvailabilityCheckResponse;
import com.ordercraft.dto.response.BomResponse;
import com.ordercraft.service.BomService;
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
@RequestMapping("/api/boms")
@Tag(name = "Bill of Materials (BOM)", description = "Product recipes, material formulations, and production costing")
public class BomController {

    private final BomService bomService;

    public BomController(BomService bomService) {
        this.bomService = bomService;
    }

    @GetMapping
    @Operation(summary = "Get paginated Bill of Materials records")
    public ResponseEntity<Page<BomResponse>> getAllBoms(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(bomService.getAllBoms(query, pageRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get BOM details by ID")
    public ResponseEntity<BomResponse> getBomById(@PathVariable Long id) {
        return ResponseEntity.ok(bomService.getBomById(id));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get BOM for a specific product")
    public ResponseEntity<BomResponse> getBomByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(bomService.getBomByProductId(productId));
    }

    @GetMapping("/check-availability")
    @Operation(summary = "Check raw material availability for a given product and batch quantity")
    public ResponseEntity<BomAvailabilityCheckResponse> checkAvailability(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        return ResponseEntity.ok(bomService.checkMaterialAvailability(productId, quantity));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create Bill of Materials recipe for a manufactured product (Admin or Manager)")
    public ResponseEntity<BomResponse> createBom(@Valid @RequestBody BomRequest request) {
        BomResponse created = bomService.createBom(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update Bill of Materials recipe (Admin or Manager)")
    public ResponseEntity<BomResponse> updateBom(@PathVariable Long id, @Valid @RequestBody BomRequest request) {
        return ResponseEntity.ok(bomService.updateBom(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete BOM (Admin only)")
    public ResponseEntity<Void> deleteBom(@PathVariable Long id) {
        bomService.deleteBom(id);
        return ResponseEntity.noContent().build();
    }
}
