package com.ordercraft.controller;

import com.ordercraft.dto.request.RawMaterialRequest;
import com.ordercraft.dto.response.RawMaterialResponse;
import com.ordercraft.service.RawMaterialService;
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

import java.util.List;

@RestController
@RequestMapping("/api/raw-materials")
@Tag(name = "Raw Materials", description = "Raw material master data and inventory thresholds")
public class RawMaterialController {

    private final RawMaterialService rawMaterialService;

    public RawMaterialController(RawMaterialService rawMaterialService) {
        this.rawMaterialService = rawMaterialService;
    }

    @GetMapping
    @Operation(summary = "Get paginated raw materials with search query")
    public ResponseEntity<Page<RawMaterialResponse>> getAllRawMaterials(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(rawMaterialService.getAllRawMaterials(query, pageRequest));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active raw materials for BOM building")
    public ResponseEntity<List<RawMaterialResponse>> getActiveRawMaterials() {
        return ResponseEntity.ok(rawMaterialService.getAllActiveRawMaterials());
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get raw materials breaching minimum stock levels")
    public ResponseEntity<List<RawMaterialResponse>> getLowStockMaterials() {
        return ResponseEntity.ok(rawMaterialService.getLowStockMaterials());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get raw material details by ID")
    public ResponseEntity<RawMaterialResponse> getRawMaterialById(@PathVariable Long id) {
        return ResponseEntity.ok(rawMaterialService.getRawMaterialById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create new raw material (Admin or Manager)")
    public ResponseEntity<RawMaterialResponse> createRawMaterial(@Valid @RequestBody RawMaterialRequest request) {
        RawMaterialResponse created = rawMaterialService.createRawMaterial(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update raw material details (Admin or Manager)")
    public ResponseEntity<RawMaterialResponse> updateRawMaterial(@PathVariable Long id, @Valid @RequestBody RawMaterialRequest request) {
        return ResponseEntity.ok(rawMaterialService.updateRawMaterial(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete raw material (Admin only)")
    public ResponseEntity<Void> deleteRawMaterial(@PathVariable Long id) {
        rawMaterialService.deleteRawMaterial(id);
        return ResponseEntity.noContent().build();
    }
}
