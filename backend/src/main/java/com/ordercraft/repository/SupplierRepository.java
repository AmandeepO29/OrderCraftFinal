package com.ordercraft.repository;

import com.ordercraft.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findBySupplierCode(String supplierCode);
    Boolean existsBySupplierCode(String supplierCode);
    Boolean existsByEmail(String email);

    @Query("SELECT s FROM Supplier s WHERE s.active = true AND (" +
           "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.supplierCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Supplier> searchSuppliers(@Param("query") String query, Pageable pageable);

    Page<Supplier> findByActiveTrue(Pageable pageable);
    long countByActiveTrue();
}
