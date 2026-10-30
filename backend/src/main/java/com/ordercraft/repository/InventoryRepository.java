package com.ordercraft.repository;

import com.ordercraft.entity.Inventory;
import com.ordercraft.entity.InventoryItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByRawMaterialId(Long rawMaterialId);
    Optional<Inventory> findByProductId(Long productId);
    Page<Inventory> findByItemType(InventoryItemType itemType, Pageable pageable);

    @Query("SELECT i FROM Inventory i WHERE " +
           "(i.itemType = 'RAW_MATERIAL' AND LOWER(i.rawMaterial.name) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           "(i.itemType = 'RAW_MATERIAL' AND LOWER(i.rawMaterial.materialCode) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           "(i.itemType = 'FINISHED_PRODUCT' AND LOWER(i.product.name) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           "(i.itemType = 'FINISHED_PRODUCT' AND LOWER(i.product.sku) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Inventory> searchInventory(@Param("query") String query, Pageable pageable);

    @Query("SELECT i FROM Inventory i WHERE i.itemType = 'RAW_MATERIAL' AND i.currentStock <= i.rawMaterial.minimumStockLevel")
    List<Inventory> findLowStockRawMaterials();

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.itemType = 'RAW_MATERIAL' AND i.currentStock <= i.rawMaterial.minimumStockLevel")
    long countLowStockRawMaterials();

    @Query("SELECT COALESCE(SUM(i.currentStock * i.rawMaterial.unitCost), 0) FROM Inventory i WHERE i.itemType = 'RAW_MATERIAL'")
    BigDecimal calculateRawMaterialValuation();

    @Query("SELECT COALESCE(SUM(i.currentStock * i.product.unitPrice), 0) FROM Inventory i WHERE i.itemType = 'FINISHED_PRODUCT'")
    BigDecimal calculateFinishedGoodsValuation();
}
