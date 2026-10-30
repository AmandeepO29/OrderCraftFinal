package com.ordercraft.service.impl;

import com.ordercraft.dto.request.ProductionOrderRequest;
import com.ordercraft.dto.request.ProductionStatusUpdateRequest;
import com.ordercraft.dto.response.ProductionOrderResponse;
import com.ordercraft.entity.Bom;
import com.ordercraft.entity.BomItem;
import com.ordercraft.entity.CustomerOrder;
import com.ordercraft.entity.Inventory;
import com.ordercraft.entity.InventoryTransactionType;
import com.ordercraft.entity.OrderStatus;
import com.ordercraft.entity.Product;
import com.ordercraft.entity.ProductionOrder;
import com.ordercraft.entity.ProductionStatus;
import com.ordercraft.entity.RawMaterial;
import com.ordercraft.exception.BusinessRuleException;
import com.ordercraft.exception.InsufficientInventoryException;
import com.ordercraft.exception.ResourceNotFoundException;
import com.ordercraft.repository.BomRepository;
import com.ordercraft.repository.CustomerOrderRepository;
import com.ordercraft.repository.InventoryRepository;
import com.ordercraft.repository.ProductRepository;
import com.ordercraft.repository.ProductionOrderRepository;
import com.ordercraft.service.InventoryService;
import com.ordercraft.service.ProductionOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductionOrderServiceImpl implements ProductionOrderService {

    private final ProductionOrderRepository productionOrderRepository;
    private final ProductRepository productRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final BomRepository bomRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    public ProductionOrderServiceImpl(ProductionOrderRepository productionOrderRepository,
                                     ProductRepository productRepository,
                                     CustomerOrderRepository customerOrderRepository,
                                     BomRepository bomRepository,
                                     InventoryRepository inventoryRepository,
                                     InventoryService inventoryService) {
        this.productionOrderRepository = productionOrderRepository;
        this.productRepository = productRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.bomRepository = bomRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductionOrderResponse> getAllProductionOrders(String query, ProductionStatus status, Pageable pageable) {
        Page<ProductionOrder> orders;
        if (StringUtils.hasText(query)) {
            orders = productionOrderRepository.searchProductionOrders(query.trim(), pageable);
        } else if (status != null) {
            orders = productionOrderRepository.findByStatus(status, pageable);
        } else {
            orders = productionOrderRepository.findAll(pageable);
        }
        return orders.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductionOrderResponse getProductionOrderById(Long id) {
        ProductionOrder order = productionOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Production order not found with id: " + id));
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public ProductionOrderResponse createProductionOrder(ProductionOrderRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (!bomRepository.existsByProductId(product.getId())) {
            throw new BusinessRuleException("Cannot create production order: Product '" + product.getName() + "' does not have a Bill of Materials (BOM) configured.");
        }

        CustomerOrder customerOrder = null;
        if (request.getCustomerOrderId() != null) {
            customerOrder = customerOrderRepository.findById(request.getCustomerOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer order not found with id: " + request.getCustomerOrderId()));
            // Update customer order status to IN_PRODUCTION if currently PENDING/CONFIRMED
            if (customerOrder.getStatus() == OrderStatus.PENDING || customerOrder.getStatus() == OrderStatus.CONFIRMED) {
                customerOrder.setStatus(OrderStatus.IN_PRODUCTION);
                customerOrderRepository.save(customerOrder);
            }
        }

        String prodNum = request.getProductionNumber();
        if (!StringUtils.hasText(prodNum)) {
            prodNum = "PRD-" + System.currentTimeMillis() % 1000000;
        } else if (productionOrderRepository.existsByProductionNumber(prodNum)) {
            throw new BusinessRuleException("Production order number '" + prodNum + "' already exists");
        }

        ProductionOrder order = new ProductionOrder(
                prodNum,
                customerOrder,
                product,
                request.getQuantityToProduce(),
                request.getStartDate(),
                request.getEndDate(),
                request.getNotes()
        );

        ProductionOrder saved = productionOrderRepository.save(order);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ProductionOrderResponse startProduction(Long id) {
        ProductionOrder order = productionOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Production order not found with id: " + id));

        if (order.getStatus() != ProductionStatus.PLANNED) {
            throw new BusinessRuleException("Only PLANNED production orders can be started");
        }

        order.setStatus(ProductionStatus.IN_PROGRESS);
        ProductionOrder updated = productionOrderRepository.save(order);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ProductionOrderResponse completeProduction(Long id, ProductionStatusUpdateRequest request, String currentUsername) {
        ProductionOrder order = productionOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Production order not found with id: " + id));

        if (order.getStatus() == ProductionStatus.COMPLETED) {
            throw new BusinessRuleException("Production order " + order.getProductionNumber() + " is already completed");
        }
        if (order.getStatus() == ProductionStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot complete a cancelled production order");
        }

        int produced = (request != null && request.getQuantityProduced() != null && request.getQuantityProduced() > 0)
                ? request.getQuantityProduced()
                : order.getQuantityToProduce();

        Bom bom = bomRepository.findByProductId(order.getProduct().getId())
                .orElseThrow(() -> new BusinessRuleException("No BOM found for product: " + order.getProduct().getName()));

        BigDecimal qtyFactor = BigDecimal.valueOf(produced);

        // 1. Verify and Deduct Raw Materials based on BOM
        for (BomItem item : bom.getItems()) {
            RawMaterial material = item.getRawMaterial();
            BigDecimal factor = BigDecimal.ONE;
            if (item.getScrapPercentage() != null && item.getScrapPercentage().compareTo(BigDecimal.ZERO) > 0) {
                factor = factor.add(item.getScrapPercentage().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
            }
            BigDecimal totalRequired = item.getQuantityRequired().multiply(qtyFactor).multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);

            Inventory rawMatInv = inventoryRepository.findByRawMaterialId(material.getId())
                    .orElseThrow(() -> new InsufficientInventoryException("No inventory record for raw material: " + material.getName()));

            if (rawMatInv.getCurrentStock().compareTo(totalRequired) < 0) {
                throw new InsufficientInventoryException("Insufficient stock for raw material '" + material.getName() + "'. Required: " + totalRequired + ", Available: " + rawMatInv.getCurrentStock());
            }

            rawMatInv.setCurrentStock(rawMatInv.getCurrentStock().subtract(totalRequired));
            inventoryRepository.save(rawMatInv);

            inventoryService.recordTransaction(
                    rawMatInv.getId(),
                    InventoryTransactionType.PRODUCTION_CONSUMPTION,
                    totalRequired.negate(),
                    order.getProductionNumber(),
                    "PRODUCTION_ORDER",
                    "Consumed for producing " + produced + " units of " + order.getProduct().getName(),
                    currentUsername
            );
        }

        // 2. Increment Finished Goods Inventory
        Inventory productInv = inventoryRepository.findByProductId(order.getProduct().getId())
                .orElseGet(() -> {
                    Inventory newInv = Inventory.ofProduct(order.getProduct(), BigDecimal.ZERO, "Finished Goods Warehouse");
                    return inventoryRepository.save(newInv);
                });

        productInv.setCurrentStock(productInv.getCurrentStock().add(BigDecimal.valueOf(produced)));
        inventoryRepository.save(productInv);

        inventoryService.recordTransaction(
                productInv.getId(),
                InventoryTransactionType.PRODUCTION_OUTPUT,
                BigDecimal.valueOf(produced),
                order.getProductionNumber(),
                "PRODUCTION_ORDER",
                "Produced " + produced + " finished units from " + order.getProductionNumber(),
                currentUsername
        );

        // 3. Update Production Order Status
        order.setQuantityProduced(produced);
        order.setStatus(ProductionStatus.COMPLETED);
        if (request != null && StringUtils.hasText(request.getNotes())) {
            order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + request.getNotes());
        }

        // 4. If linked to Customer Order, check if Customer Order is complete
        if (order.getCustomerOrder() != null) {
            CustomerOrder co = order.getCustomerOrder();
            if (co.getStatus() == OrderStatus.IN_PRODUCTION) {
                co.setStatus(OrderStatus.COMPLETED);
                customerOrderRepository.save(co);
            }
        }

        ProductionOrder completed = productionOrderRepository.save(order);
        return mapToResponse(completed);
    }

    @Override
    @Transactional
    public void cancelProductionOrder(Long id) {
        ProductionOrder order = productionOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Production order not found with id: " + id));

        if (order.getStatus() == ProductionStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel an already completed production order");
        }

        order.setStatus(ProductionStatus.CANCELLED);
        productionOrderRepository.save(order);
    }

    private ProductionOrderResponse mapToResponse(ProductionOrder order) {
        ProductionOrderResponse res = new ProductionOrderResponse();
        res.setId(order.getId());
        res.setProductionNumber(order.getProductionNumber());
        res.setProductId(order.getProduct().getId());
        res.setProductSku(order.getProduct().getSku());
        res.setProductName(order.getProduct().getName());
        res.setQuantityToProduce(order.getQuantityToProduce());
        res.setQuantityProduced(order.getQuantityProduced());
        res.setStartDate(order.getStartDate());
        res.setEndDate(order.getEndDate());
        res.setStatus(order.getStatus());
        res.setNotes(order.getNotes());
        res.setCreatedAt(order.getCreatedAt());

        if (order.getCustomerOrder() != null) {
            res.setCustomerOrderId(order.getCustomerOrder().getId());
            res.setCustomerOrderNumber(order.getCustomerOrder().getOrderNumber());
        }

        // Check if raw materials are sufficient to complete
        Optional<Bom> bomOpt = bomRepository.findByProductId(order.getProduct().getId());
        if (bomOpt.isPresent()) {
            boolean sufficient = true;
            BigDecimal factor = BigDecimal.valueOf(order.getQuantityToProduce());
            for (BomItem item : bomOpt.get().getItems()) {
                Optional<Inventory> inv = inventoryRepository.findByRawMaterialId(item.getRawMaterial().getId());
                BigDecimal available = inv.map(Inventory::getCurrentStock).orElse(BigDecimal.ZERO);
                BigDecimal req = item.getQuantityRequired().multiply(factor);
                if (available.compareTo(req) < 0) {
                    sufficient = false;
                    break;
                }
            }
            res.setCanComplete(sufficient);
        } else {
            res.setCanComplete(false);
        }

        return res;
    }
}
