package com.ordercraft.service.impl;

import com.ordercraft.dto.request.ProductRequest;
import com.ordercraft.dto.response.ProductResponse;
import com.ordercraft.entity.Inventory;
import com.ordercraft.entity.InventoryItemType;
import com.ordercraft.entity.Product;
import com.ordercraft.entity.ProductStatus;
import com.ordercraft.exception.BusinessRuleException;
import com.ordercraft.exception.ResourceNotFoundException;
import com.ordercraft.repository.BomRepository;
import com.ordercraft.repository.InventoryRepository;
import com.ordercraft.repository.ProductRepository;
import com.ordercraft.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final BomRepository bomRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              InventoryRepository inventoryRepository,
                              BomRepository bomRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.bomRepository = bomRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(String query, Pageable pageable) {
        Page<Product> products;
        if (StringUtils.hasText(query)) {
            products = productRepository.searchProducts(query.trim(), pageable);
        } else {
            products = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
        }
        return products.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllActiveProducts() {
        return productRepository.findByStatus(ProductStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new BusinessRuleException("Product with SKU '" + request.getSku() + "' already exists");
        }

        Product product = new Product(
                request.getSku().toUpperCase().trim(),
                request.getName().trim(),
                request.getDescription(),
                request.getCategory().trim(),
                request.getUnitPrice(),
                request.getLeadTimeDays() != null ? request.getLeadTimeDays() : 7
        );
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        Product savedProduct = productRepository.save(product);

        // Auto-create finished product inventory record
        BigDecimal initialStock = request.getInitialStock() != null ? request.getInitialStock() : BigDecimal.ZERO;
        Inventory inventory = Inventory.ofProduct(savedProduct, initialStock, "Finished Goods Warehouse");
        inventoryRepository.save(inventory);

        return mapToResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (!product.getSku().equalsIgnoreCase(request.getSku().trim()) &&
                productRepository.existsBySku(request.getSku().trim())) {
            throw new BusinessRuleException("Product with SKU '" + request.getSku() + "' already exists");
        }

        product.setSku(request.getSku().toUpperCase().trim());
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory().trim());
        product.setUnitPrice(request.getUnitPrice());
        if (request.getLeadTimeDays() != null) {
            product.setLeadTimeDays(request.getLeadTimeDays());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return productRepository.findAllCategories();
    }

    private ProductResponse mapToResponse(Product product) {
        ProductResponse res = new ProductResponse();
        res.setId(product.getId());
        res.setSku(product.getSku());
        res.setName(product.getName());
        res.setDescription(product.getDescription());
        res.setCategory(product.getCategory());
        res.setUnitPrice(product.getUnitPrice());
        res.setStatus(product.getStatus());
        res.setLeadTimeDays(product.getLeadTimeDays());
        res.setCreatedAt(product.getCreatedAt());

        Optional<Inventory> inv = inventoryRepository.findByProductId(product.getId());
        if (inv.isPresent()) {
            res.setCurrentStock(inv.get().getCurrentStock());
            res.setAvailableStock(inv.get().getAvailableStock());
        } else {
            res.setCurrentStock(BigDecimal.ZERO);
            res.setAvailableStock(BigDecimal.ZERO);
        }

        res.setHasBom(bomRepository.existsByProductId(product.getId()));
        return res;
    }
}
