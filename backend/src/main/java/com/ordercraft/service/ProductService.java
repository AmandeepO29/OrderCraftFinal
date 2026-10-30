package com.ordercraft.service;

import com.ordercraft.dto.request.ProductRequest;
import com.ordercraft.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    Page<ProductResponse> getAllProducts(String query, Pageable pageable);
    List<ProductResponse> getAllActiveProducts();
    ProductResponse getProductById(Long id);
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(Long id, ProductRequest request);
    void deleteProduct(Long id);
    List<String> getCategories();
}
