package com.ordercraft.repository;

import com.ordercraft.entity.Bom;
import com.ordercraft.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BomRepository extends JpaRepository<Bom, Long> {
    Optional<Bom> findByProduct(Product product);
    Optional<Bom> findByProductId(Long productId);
    Optional<Bom> findByBomCode(String bomCode);
    Boolean existsByBomCode(String bomCode);
    Boolean existsByProductId(Long productId);

    @Query("SELECT b FROM Bom b WHERE b.active = true AND (" +
           "LOWER(b.bomCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.product.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.product.sku) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Bom> searchBoms(@Param("query") String query, Pageable pageable);

    Page<Bom> findByActiveTrue(Pageable pageable);
}
