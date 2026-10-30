package com.ordercraft.repository;

import com.ordercraft.entity.ProductionOrder;
import com.ordercraft.entity.ProductionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {
    Optional<ProductionOrder> findByProductionNumber(String productionNumber);
    Boolean existsByProductionNumber(String productionNumber);

    Page<ProductionOrder> findByStatus(ProductionStatus status, Pageable pageable);
    List<ProductionOrder> findByCustomerOrderId(Long customerOrderId);

    @Query("SELECT p FROM ProductionOrder p WHERE " +
           "LOWER(p.productionNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.product.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.product.sku) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<ProductionOrder> searchProductionOrders(@Param("query") String query, Pageable pageable);

    long countByStatus(ProductionStatus status);

    @Query("SELECT p FROM ProductionOrder p WHERE p.startDate BETWEEN :startDate AND :endDate ORDER BY p.startDate DESC")
    List<ProductionOrder> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
