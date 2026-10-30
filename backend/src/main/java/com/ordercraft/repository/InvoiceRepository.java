package com.ordercraft.repository;

import com.ordercraft.entity.Invoice;
import com.ordercraft.entity.InvoiceStatus;
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
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Boolean existsByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByCustomerOrderId(Long customerOrderId);
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE " +
           "LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.customerOrder.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.customerOrder.customer.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Invoice> searchInvoices(@Param("query") String query, Pageable pageable);

    long countByStatus(InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM Invoice i WHERE i.status != 'PAID'")
    BigDecimal calculatePendingPayments();

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i")
    BigDecimal calculateTotalInvoiced();

    @Query("SELECT COALESCE(SUM(i.paidAmount), 0) FROM Invoice i")
    BigDecimal calculateTotalPaid();

    @Query("SELECT i FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate ORDER BY i.invoiceDate DESC")
    List<Invoice> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
