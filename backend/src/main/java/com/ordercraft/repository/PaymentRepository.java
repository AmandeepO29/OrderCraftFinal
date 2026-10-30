package com.ordercraft.repository;

import com.ordercraft.entity.Payment;
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
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentReference(String paymentReference);
    List<Payment> findByInvoiceId(Long invoiceId);
    Page<Payment> findByInvoiceId(Long invoiceId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE " +
           "LOWER(p.paymentReference) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.invoice.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.transactionNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Payment> searchPayments(@Param("query") String query, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p")
    BigDecimal calculateTotalPaymentsReceived();

    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<Payment> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<Payment> findTop5ByOrderByCreatedAtDesc();
}
