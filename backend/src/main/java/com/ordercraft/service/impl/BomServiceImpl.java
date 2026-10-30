package com.ordercraft.service.impl;

import com.ordercraft.dto.request.BomItemRequest;
import com.ordercraft.dto.request.BomRequest;
import com.ordercraft.dto.response.BomAvailabilityCheckResponse;
import com.ordercraft.dto.response.BomItemResponse;
import com.ordercraft.dto.response.BomResponse;
import com.ordercraft.entity.Bom;
import com.ordercraft.entity.BomItem;
import com.ordercraft.entity.Inventory;
import com.ordercraft.entity.Product;
import com.ordercraft.entity.RawMaterial;
import com.ordercraft.exception.BusinessRuleException;
import com.ordercraft.exception.ResourceNotFoundException;
import com.ordercraft.repository.BomRepository;
import com.ordercraft.repository.InventoryRepository;
import com.ordercraft.repository.ProductRepository;
import com.ordercraft.repository.RawMaterialRepository;
import com.ordercraft.service.BomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BomServiceImpl implements BomService {

    private final BomRepository bomRepository;
    private final ProductRepository productRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final InventoryRepository inventoryRepository;

    public BomServiceImpl(BomRepository bomRepository,
                          ProductRepository productRepository,
                          RawMaterialRepository rawMaterialRepository,
                          InventoryRepository inventoryRepository) {
        this.bomRepository = bomRepository;
        this.productRepository = productRepository;
        this.rawMaterialRepository = rawMaterialRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BomResponse> getAllBoms(String query, Pageable pageable) {
        Page<Bom> boms;
        if (StringUtils.hasText(query)) {
            boms = bomRepository.searchBoms(query.trim(), pageable);
        } else {
            boms = bomRepository.findByActiveTrue(pageable);
        }
        return boms.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BomResponse getBomById(Long id) {
        Bom bom = bomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BOM not found with id: " + id));
        return mapToResponse(bom);
    }

    @Override
    @Transactional(readOnly = true)
    public BomResponse getBomByProductId(Long productId) {
        Bom bom = bomRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("No BOM found for product id: " + productId));
        return mapToResponse(bom);
    }

    @Override
    @Transactional
    public BomResponse createBom(BomRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (bomRepository.existsByProductId(product.getId())) {
            throw new BusinessRuleException("A BOM already exists for product: " + product.getName() + " (" + product.getSku() + ")");
        }

        String bomCode = request.getBomCode();
        if (!StringUtils.hasText(bomCode)) {
            bomCode = "BOM-" + product.getSku() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        } else if (bomRepository.existsByBomCode(bomCode)) {
            throw new BusinessRuleException("BOM code '" + bomCode + "' already exists");
        }

        Bom bom = new Bom(
                product,
                bomCode,
                StringUtils.hasText(request.getVersion()) ? request.getVersion() : "1.0",
                request.getDescription()
        );

        for (BomItemRequest itemReq : request.getItems()) {
            RawMaterial material = rawMaterialRepository.findById(itemReq.getRawMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Raw material not found with id: " + itemReq.getRawMaterialId()));

            String uom = StringUtils.hasText(itemReq.getUnitOfMeasure()) ? itemReq.getUnitOfMeasure() : material.getUnitOfMeasure();
            BomItem item = new BomItem(material, itemReq.getQuantityRequired(), uom, itemReq.getScrapPercentage());
            bom.addItem(item);
        }

        Bom savedBom = bomRepository.save(bom);
        return mapToResponse(savedBom);
    }

    @Override
    @Transactional
    public BomResponse updateBom(Long id, BomRequest request) {
        Bom bom = bomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BOM not found with id: " + id));

        if (StringUtils.hasText(request.getVersion())) {
            bom.setVersion(request.getVersion());
        }
        bom.setDescription(request.getDescription());

        // Clear existing items and repopulate
        bom.getItems().clear();
        for (BomItemRequest itemReq : request.getItems()) {
            RawMaterial material = rawMaterialRepository.findById(itemReq.getRawMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Raw material not found with id: " + itemReq.getRawMaterialId()));

            String uom = StringUtils.hasText(itemReq.getUnitOfMeasure()) ? itemReq.getUnitOfMeasure() : material.getUnitOfMeasure();
            BomItem item = new BomItem(material, itemReq.getQuantityRequired(), uom, itemReq.getScrapPercentage());
            bom.addItem(item);
        }

        bom.recalculateTotalCost();
        Bom updatedBom = bomRepository.save(bom);
        return mapToResponse(updatedBom);
    }

    @Override
    @Transactional
    public void deleteBom(Long id) {
        Bom bom = bomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BOM not found with id: " + id));
        bom.setActive(false);
        bomRepository.save(bom);
    }

    @Override
    @Transactional(readOnly = true)
    public BomAvailabilityCheckResponse checkMaterialAvailability(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Bom bom = bomRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessRuleException("Cannot check availability: No BOM defined for product " + product.getName()));

        int qty = (quantity != null && quantity > 0) ? quantity : 1;
        BigDecimal mult = BigDecimal.valueOf(qty);

        boolean allSufficient = true;
        List<BomAvailabilityCheckResponse.MaterialRequirement> requirements = new ArrayList<>();

        for (BomItem item : bom.getItems()) {
            RawMaterial material = item.getRawMaterial();
            BigDecimal factor = BigDecimal.ONE;
            if (item.getScrapPercentage() != null && item.getScrapPercentage().compareTo(BigDecimal.ZERO) > 0) {
                factor = factor.add(item.getScrapPercentage().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
            }
            BigDecimal totalReq = item.getQuantityRequired().multiply(mult).multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);

            Optional<Inventory> inv = inventoryRepository.findByRawMaterialId(material.getId());
            BigDecimal available = inv.map(Inventory::getAvailableStock).orElse(BigDecimal.ZERO);

            boolean isAvailable = available.compareTo(totalReq) >= 0;
            if (!isAvailable) {
                allSufficient = false;
            }

            requirements.add(new BomAvailabilityCheckResponse.MaterialRequirement(
                    material.getId(),
                    material.getName(),
                    item.getUnitOfMeasure(),
                    totalReq,
                    available,
                    isAvailable
            ));
        }

        BomAvailabilityCheckResponse res = new BomAvailabilityCheckResponse();
        res.setProductId(product.getId());
        res.setProductName(product.getName());
        res.setRequestedQuantity(qty);
        res.setSufficient(allSufficient);
        res.setRequirements(requirements);

        return res;
    }

    private BomResponse mapToResponse(Bom bom) {
        BomResponse res = new BomResponse();
        res.setId(bom.getId());
        res.setProductId(bom.getProduct().getId());
        res.setProductSku(bom.getProduct().getSku());
        res.setProductName(bom.getProduct().getName());
        res.setProductUnitPrice(bom.getProduct().getUnitPrice());
        res.setBomCode(bom.getBomCode());
        res.setVersion(bom.getVersion());
        res.setDescription(bom.getDescription());
        res.setTotalEstimatedCost(bom.getTotalEstimatedCost());
        res.setActive(bom.isActive());
        res.setCreatedAt(bom.getCreatedAt());

        if (bom.getItems() != null) {
            res.setItems(bom.getItems().stream().map(item -> {
                BomItemResponse ir = new BomItemResponse();
                ir.setId(item.getId());
                ir.setRawMaterialId(item.getRawMaterial().getId());
                ir.setMaterialCode(item.getRawMaterial().getMaterialCode());
                ir.setMaterialName(item.getRawMaterial().getName());
                ir.setUnitCost(item.getRawMaterial().getUnitCost());
                ir.setQuantityRequired(item.getQuantityRequired());
                ir.setUnitOfMeasure(item.getUnitOfMeasure());
                ir.setScrapPercentage(item.getScrapPercentage());
                ir.setItemCost(item.getItemCost());

                Optional<Inventory> inv = inventoryRepository.findByRawMaterialId(item.getRawMaterial().getId());
                BigDecimal stock = inv.map(Inventory::getCurrentStock).orElse(BigDecimal.ZERO);
                ir.setCurrentStock(stock);
                ir.setInStock(stock.compareTo(item.getQuantityRequired()) >= 0);

                return ir;
            }).collect(Collectors.toList()));
        }

        return res;
    }
}
