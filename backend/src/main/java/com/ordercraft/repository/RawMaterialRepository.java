package com.ordercraft.repository;

import com.ordercraft.entity.RawMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RawMaterialRepository extends JpaRepository<RawMaterial, Long> {
    Optional<RawMaterial> findByMaterialCode(String materialCode);
    Boolean existsByMaterialCode(String materialCode);

    @Query("SELECT r FROM RawMaterial r WHERE r.active = true AND (" +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.materialCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<RawMaterial> searchMaterials(@Param("query") String query, Pageable pageable);

    List<RawMaterial> findByActiveTrue();
    Page<RawMaterial> findByActiveTrue(Pageable pageable);
    long countByActiveTrue();
}
