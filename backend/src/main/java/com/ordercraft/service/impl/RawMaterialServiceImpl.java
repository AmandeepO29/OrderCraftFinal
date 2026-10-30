package com.ordercraft.service.impl;

import com.ordercraft.dto.request.RawMaterialRequest;
import com.ordercraft.dto.response.RawMaterialResponse;
import com.ordercraft.entity.Inventory;
import com.ordercraft.entity.RawMaterial;
import com.ordercraft.entity.Supplier;
import com.ordercraft.exception.BusinessRuleException;
import com.ordercraft.exception.ResourceNotFoundException;
import com.ordercraft.repository.InventoryRepository;
import com.ordercraft.repository.RawMaterialRepository;
import com.ordercraft.repository.SupplierRepository;
import com.ordercraft.service.RawMaterialService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RawMaterialServiceImpl implements RawMaterialService {

    private final RawMaterialRepository rawMaterialRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;

    public RawMaterialServiceImpl(RawMaterialRepository rawMaterialRepository,
                                  SupplierRepository supplierRepository,
                                  InventoryRepository inventoryRepository) {
        this.rawMaterialRepository = rawMaterialRepository;
        this.supplierRepository = supplierRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RawMaterialResponse> getAllRawMaterials(String query, Pageable pageable) {
        Page<RawMaterial> materials;
        if (StringUtils.hasText(query)) {
            materials = rawMaterialRepository.searchMaterials(query.trim(), pageable);
        } else {
            materials = rawMaterialRepository.findByActiveTrue(pageable);
        }
        return materials.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RawMaterialResponse> getAllActiveRawMaterials() {
        return rawMaterialRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RawMaterialResponse getRawMaterialById(Long id) {
        RawMaterial material = rawMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Raw material not found with id: " + id));
        return mapToResponse(material);
    }

    @Override
    @Transactional
    public RawMaterialResponse createRawMaterial(RawMaterialRequest request) {
        String code = request.getMaterialCode();
        if (!StringUtils.hasText(code)) {
            code = "RM-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } else if (rawMaterialRepository.existsByMaterialCode(code)) {
            throw new BusinessRuleException("Raw material with code '" + code + "' already exists");
        }

        RawMaterial material = new RawMaterial(
                code,
                request.getName().trim(),
                request.getDescription(),
                request.getUnitOfMeasure().trim(),
                request.getUnitCost(),
                request.getMinimumStockLevel(),
                request.getReorderQuantity()
        );

        if (request.getPreferredSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getPreferredSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Preferred supplier not found with id: " + request.getPreferredSupplierId()));
            material.setPreferredSupplier(supplier);
        }

        RawMaterial savedMaterial = rawMaterialRepository.save(material);

        // Auto-create raw material inventory record
        BigDecimal initialStock = request.getInitialStock() != null ? request.getInitialStock() : BigDecimal.ZERO;
        Inventory inventory = Inventory.ofRawMaterial(savedMaterial, initialStock, "Raw Materials Warehouse");
        inventoryRepository.save(inventory);

        return mapToResponse(savedMaterial);
    }

    @Override
    @Transactional
    public RawMaterialResponse updateRawMaterial(Long id, RawMaterialRequest request) {
        RawMaterial material = rawMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Raw material not found with id: " + id));

        material.setName(request.getName().trim());
        material.setDescription(request.getDescription());
        material.setUnitOfMeasure(request.getUnitOfMeasure().trim());
        material.setUnitCost(request.getUnitCost());
        material.setMinimumStockLevel(request.getMinimumStockLevel());
        material.setReorderQuantity(request.getReorderQuantity());

        if (request.getPreferredSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getPreferredSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Preferred supplier not found with id: " + request.getPreferredSupplierId()));
            material.setPreferredSupplier(supplier);
        } else {
            material.setPreferredSupplier(null);
        }

        RawMaterial updated = rawMaterialRepository.save(material);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteRawMaterial(Long id) {
        RawMaterial material = rawMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Raw material not found with id: " + id));
        material.setActive(false);
        rawMaterialRepository.save(material);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RawMaterialResponse> getLowStockMaterials() {
        return inventoryRepository.findLowStockRawMaterials().stream()
                .filter(inv -> inv.getRawMaterial() != null)
                .map(inv -> mapToResponse(inv.getRawMaterial()))
                .collect(Collectors.toList());
    }

    private RawMaterialResponse mapToResponse(RawMaterial material) {
        RawMaterialResponse res = new RawMaterialResponse();
        res.setId(material.getId());
        res.setMaterialCode(material.getMaterialCode());
        res.setName(material.getName());
        res.setDescription(material.getDescription());
        res.setUnitOfMeasure(material.getUnitOfMeasure());
        res.setUnitCost(material.getUnitCost());
        res.setMinimumStockLevel(material.getMinimumStockLevel());
        res.setReorderQuantity(material.getReorderQuantity());
        res.setActive(material.isActive());
        res.setCreatedAt(material.getCreatedAt());

        if (material.getPreferredSupplier() != null) {
            res.setPreferredSupplierId(material.getPreferredSupplier().getId());
            res.setPreferredSupplierName(material.getPreferredSupplier().getCompanyName());
        }

        Optional<Inventory> inv = inventoryRepository.findByRawMaterialId(material.getId());
        if (inv.isPresent()) {
            res.setCurrentStock(inv.get().getCurrentStock());
            res.setAvailableStock(inv.get().getAvailableStock());
            res.setLowStock(inv.get().getCurrentStock().compareTo(material.getMinimumStockLevel()) <= 0);
        } else {
            res.setCurrentStock(BigDecimal.ZERO);
            res.setAvailableStock(BigDecimal.ZERO);
            res.setLowStock(true);
        }

        return res;
    }
}
