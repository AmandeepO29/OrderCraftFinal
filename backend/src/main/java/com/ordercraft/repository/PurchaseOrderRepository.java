package com.ordercraft.repository;

import com.ordercraft.entity.PurchaseOrder;
import com.ordercraft.entity.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByPoNumber(String poNumber);
    Boolean existsByPoNumber(String poNumber);

    Page<PurchaseOrder> findByStatus(PurchaseOrderStatus status, Pageable pageable);
    Page<PurchaseOrder> findBySupplierId(Long supplierId, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE " +
           "LOWER(po.poNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(po.supplier.companyName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<PurchaseOrder> searchPurchaseOrders(@Param("query") String query, Pageable pageable);

    long countByStatus(PurchaseOrderStatus status);

    @Query("SELECT COALESCE(SUM(po.totalAmount), 0) FROM PurchaseOrder po WHERE po.status = 'RECEIVED'")
    BigDecimal calculateTotalPurchasesAmount();

    @Query("SELECT po FROM PurchaseOrder po WHERE po.orderDate BETWEEN :startDate AND :endDate ORDER BY po.orderDate DESC")
    List<PurchaseOrder> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
