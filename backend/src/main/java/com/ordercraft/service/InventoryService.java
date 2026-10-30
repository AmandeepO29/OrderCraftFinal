package com.ordercraft.service;

import com.ordercraft.dto.request.StockAdjustmentRequest;
import com.ordercraft.dto.response.InventoryResponse;
import com.ordercraft.dto.response.InventoryTransactionResponse;
import com.ordercraft.entity.InventoryItemType;
import com.ordercraft.entity.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryService {
    Page<InventoryResponse> getAllInventory(String query, InventoryItemType itemType, Pageable pageable);
    InventoryResponse getInventoryById(Long id);
    InventoryResponse getInventoryByRawMaterialId(Long rawMaterialId);
    InventoryResponse getInventoryByProductId(Long productId);

    InventoryResponse adjustStock(StockAdjustmentRequest request, String currentUsername);

    void recordTransaction(Long inventoryId,
                           InventoryTransactionType transactionType,
                           BigDecimal quantityChange,
                           String referenceId,
                           String referenceType,
                           String notes,
                           String username);

    Page<InventoryTransactionResponse> getTransactions(Long inventoryId, Pageable pageable);
    List<InventoryResponse> getLowStockAlerts();
}
