package com.ordercraft.repository;

import com.ordercraft.entity.Product;
import com.ordercraft.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    Boolean existsBySku(String sku);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    List<Product> findByStatus(ProductStatus status);

    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.status = 'ACTIVE'")
    List<String> findAllCategories();

    long countByStatus(ProductStatus status);
}
