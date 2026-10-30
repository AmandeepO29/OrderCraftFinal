package com.ordercraft.repository;

import com.ordercraft.entity.InventoryTransaction;
import com.ordercraft.entity.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    Page<InventoryTransaction> findByInventoryId(Long inventoryId, Pageable pageable);
    Page<InventoryTransaction> findByTransactionType(InventoryTransactionType transactionType, Pageable pageable);

    @Query("SELECT t FROM InventoryTransaction t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<InventoryTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    Page<InventoryTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
