package com.ordercraft.repository;

import com.ordercraft.entity.CustomerOrder;
import com.ordercraft.entity.OrderStatus;
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
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);
    Boolean existsByOrderNumber(String orderNumber);

    Page<CustomerOrder> findByStatus(OrderStatus status, Pageable pageable);
    Page<CustomerOrder> findByCustomerId(Long customerId, Pageable pageable);
    List<CustomerOrder> findByCustomerId(Long customerId);

    @Query("SELECT o FROM CustomerOrder o WHERE " +
           "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(o.customer.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(o.customer.companyName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<CustomerOrder> searchOrders(@Param("query") String query, Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o WHERE o.status != 'CANCELLED'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT o FROM CustomerOrder o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<CustomerOrder> findOrdersBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<CustomerOrder> findTop5ByOrderByCreatedAtDesc();
}
