package com.ordercraft.service;

import com.ordercraft.dto.request.BomRequest;
import com.ordercraft.dto.response.BomAvailabilityCheckResponse;
import com.ordercraft.dto.response.BomResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BomService {
    Page<BomResponse> getAllBoms(String query, Pageable pageable);
    BomResponse getBomById(Long id);
    BomResponse getBomByProductId(Long productId);
    BomResponse createBom(BomRequest request);
    BomResponse updateBom(Long id, BomRequest request);
    void deleteBom(Long id);
    BomAvailabilityCheckResponse checkMaterialAvailability(Long productId, Integer quantity);
}
