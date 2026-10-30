package com.ordercraft.service.impl;

import com.ordercraft.dto.request.StockAdjustmentRequest;
import com.ordercraft.dto.response.InventoryResponse;
import com.ordercraft.dto.response.InventoryTransactionResponse;
import com.ordercraft.entity.Inventory;
import com.ordercraft.entity.InventoryItemType;
import com.ordercraft.entity.InventoryTransaction;
import com.ordercraft.entity.InventoryTransactionType;
import com.ordercraft.entity.User;
import com.ordercraft.exception.BusinessRuleException;
import com.ordercraft.exception.InsufficientInventoryException;
import com.ordercraft.exception.ResourceNotFoundException;
import com.ordercraft.repository.InventoryRepository;
import com.ordercraft.repository.InventoryTransactionRepository;
import com.ordercraft.repository.UserRepository;
import com.ordercraft.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                InventoryTransactionRepository transactionRepository,
                                UserRepository userRepository) {
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getAllInventory(String query, InventoryItemType itemType, Pageable pageable) {
        Page<Inventory> page;
        if (StringUtils.hasText(query)) {
            page = inventoryRepository.searchInventory(query.trim(), pageable);
        } else if (itemType != null) {
            page = inventoryRepository.findByItemType(itemType, pageable);
        } else {
            page = inventoryRepository.findAll(pageable);
        }
        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(Long id) {
        Inventory inv = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found with id: " + id));
        return mapToResponse(inv);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByRawMaterialId(Long rawMaterialId) {
        Inventory inv = inventoryRepository.findByRawMaterialId(rawMaterialId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for raw material id: " + rawMaterialId));
        return mapToResponse(inv);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(Long productId) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product id: " + productId));
        return mapToResponse(inv);
    }

    @Override
    @Transactional
    public InventoryResponse adjustStock(StockAdjustmentRequest request, String currentUsername) {
        Inventory inventory = inventoryRepository.findById(request.getInventoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + request.getInventoryId()));

        String type = request.getType().toUpperCase().trim();
        BigDecimal qty = request.getQuantity();

        if ("ADD".equals(type)) {
            BigDecimal newStock = inventory.getCurrentStock().add(qty);
            inventory.setCurrentStock(newStock);
            inventoryRepository.save(inventory);

            recordTransaction(inventory.getId(),
                    InventoryTransactionType.MANUAL_ADJUSTMENT,
                    qty,
                    "MANUAL-ADD",
                    "ADJUSTMENT",
                    request.getNotes(),
                    currentUsername);

        } else if ("DEDUCT".equals(type)) {
            if (inventory.getCurrentStock().compareTo(qty) < 0) {
                throw new InsufficientInventoryException("Cannot deduct " + qty + ". Current stock is " + inventory.getCurrentStock());
            }
            BigDecimal newStock = inventory.getCurrentStock().subtract(qty);
            inventory.setCurrentStock(newStock);
            inventoryRepository.save(inventory);

            recordTransaction(inventory.getId(),
                    InventoryTransactionType.MANUAL_ADJUSTMENT,
                    qty.negate(),
                    "MANUAL-DEDUCT",
                    "ADJUSTMENT",
                    request.getNotes(),
                    currentUsername);
        } else {
            throw new BusinessRuleException("Adjustment type must be either 'ADD' or 'DEDUCT'");
        }

        return mapToResponse(inventory);
    }

    @Override
    @Transactional
    public void recordTransaction(Long inventoryId,
                                  InventoryTransactionType transactionType,
                                  BigDecimal quantityChange,
                                  String referenceId,
                                  String referenceType,
                                  String notes,
                                  String username) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + inventoryId));

        User user = null;
        if (StringUtils.hasText(username)) {
            user = userRepository.findByUsername(username).orElse(null);
        }

        InventoryTransaction tx = new InventoryTransaction(
                inventory,
                transactionType,
                quantityChange,
                inventory.getCurrentStock(),
                referenceId,
                referenceType,
                notes,
                user
        );
        transactionRepository.save(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponse> getTransactions(Long inventoryId, Pageable pageable) {
        Page<InventoryTransaction> transactions;
        if (inventoryId != null) {
            transactions = transactionRepository.findByInventoryId(inventoryId, pageable);
        } else {
            transactions = transactionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return transactions.map(this::mapTransactionToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockAlerts() {
        return inventoryRepository.findLowStockRawMaterials().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        InventoryResponse res = new InventoryResponse();
        res.setId(inventory.getId());
        res.setItemType(inventory.getItemType());
        res.setCurrentStock(inventory.getCurrentStock());
        res.setReservedStock(inventory.getReservedStock());
        res.setAvailableStock(inventory.getAvailableStock());
        res.setLocation(inventory.getLocation());
        res.setLastUpdated(inventory.getLastUpdated());

        if (inventory.getItemType() == InventoryItemType.RAW_MATERIAL && inventory.getRawMaterial() != null) {
            res.setRawMaterialId(inventory.getRawMaterial().getId());
            res.setMaterialName(inventory.getRawMaterial().getName());
            res.setMaterialCode(inventory.getRawMaterial().getMaterialCode());
            res.setUnitOfMeasure(inventory.getRawMaterial().getUnitOfMeasure());
            res.setUnitCost(inventory.getRawMaterial().getUnitCost());
            res.setMinimumStockLevel(inventory.getRawMaterial().getMinimumStockLevel());
            res.setLowStockAlert(inventory.getCurrentStock().compareTo(inventory.getRawMaterial().getMinimumStockLevel()) <= 0);
        } else if (inventory.getItemType() == InventoryItemType.FINISHED_PRODUCT && inventory.getProduct() != null) {
            res.setProductId(inventory.getProduct().getId());
            res.setProductName(inventory.getProduct().getName());
            res.setProductSku(inventory.getProduct().getSku());
            res.setUnitPrice(inventory.getProduct().getUnitPrice());
            res.setLowStockAlert(inventory.getCurrentStock().compareTo(BigDecimal.valueOf(5)) <= 0);
        }

        return res;
    }

    private InventoryTransactionResponse mapTransactionToResponse(InventoryTransaction tx) {
        InventoryTransactionResponse res = new InventoryTransactionResponse();
        res.setId(tx.getId());
        res.setInventoryId(tx.getInventory().getId());
        res.setItemType(tx.getInventory().getItemType().name());

        if (tx.getInventory().getItemType() == InventoryItemType.RAW_MATERIAL && tx.getInventory().getRawMaterial() != null) {
            res.setItemName(tx.getInventory().getRawMaterial().getName());
        } else if (tx.getInventory().getProduct() != null) {
            res.setItemName(tx.getInventory().getProduct().getName());
        } else {
            res.setItemName("N/A");
        }

        res.setTransactionType(tx.getTransactionType());
        res.setQuantity(tx.getQuantity());
        res.setBalanceAfter(tx.getBalanceAfter());
        res.setReferenceId(tx.getReferenceId());
        res.setReferenceType(tx.getReferenceType());
        res.setNotes(tx.getNotes());
        res.setCreatedByUsername(tx.getCreatedBy() != null ? tx.getCreatedBy().getUsername() : "System");
        res.setCreatedAt(tx.getCreatedAt());

        return res;
    }
}
