package com.ordercraft.service;

import com.ordercraft.dto.request.RawMaterialRequest;
import com.ordercraft.dto.response.RawMaterialResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RawMaterialService {
    Page<RawMaterialResponse> getAllRawMaterials(String query, Pageable pageable);
    List<RawMaterialResponse> getAllActiveRawMaterials();
    RawMaterialResponse getRawMaterialById(Long id);
    RawMaterialResponse createRawMaterial(RawMaterialRequest request);
    RawMaterialResponse updateRawMaterial(Long id, RawMaterialRequest request);
    void deleteRawMaterial(Long id);
    List<RawMaterialResponse> getLowStockMaterials();
}
