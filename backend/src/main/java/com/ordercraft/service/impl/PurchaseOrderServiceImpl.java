package com.ordercraft.service.impl;

import com.ordercraft.dto.request.PurchaseOrderItemRequest;
import com.ordercraft.dto.request.PurchaseOrderRequest;
import com.ordercraft.dto.request.ReceivePurchaseOrderRequest;
import com.ordercraft.dto.response.PurchaseOrderItemResponse;
import com.ordercraft.dto.response.PurchaseOrderResponse;
import com.ordercraft.entity.Inventory;
import com.ordercraft.entity.InventoryTransactionType;
import com.ordercraft.entity.PurchaseOrder;
import com.ordercraft.entity.PurchaseOrderItem;
import com.ordercraft.entity.PurchaseOrderStatus;
import com.ordercraft.entity.RawMaterial;
import com.ordercraft.entity.Supplier;
import com.ordercraft.exception.BusinessRuleException;
import com.ordercraft.exception.ResourceNotFoundException;
import com.ordercraft.repository.InventoryRepository;
import com.ordercraft.repository.PurchaseOrderRepository;
import com.ordercraft.repository.RawMaterialRepository;
import com.ordercraft.repository.SupplierRepository;
import com.ordercraft.service.InventoryService;
import com.ordercraft.service.PurchaseOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    public PurchaseOrderServiceImpl(PurchaseOrderRepository purchaseOrderRepository,
                                    SupplierRepository supplierRepository,
                                    RawMaterialRepository rawMaterialRepository,
                                    InventoryRepository inventoryRepository,
                                    InventoryService inventoryService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.rawMaterialRepository = rawMaterialRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> getAllPurchaseOrders(String query, PurchaseOrderStatus status, Pageable pageable) {
        Page<PurchaseOrder> orders;
        if (StringUtils.hasText(query)) {
            orders = purchaseOrderRepository.searchPurchaseOrders(query.trim(), pageable);
        } else if (status != null) {
            orders = purchaseOrderRepository.findByStatus(status, pageable);
        } else {
            orders = purchaseOrderRepository.findAll(pageable);
        }
        return orders.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderById(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));

        String poNum = request.getPoNumber();
        if (!StringUtils.hasText(poNum)) {
            poNum = "PO-" + System.currentTimeMillis() % 1000000;
        } else if (purchaseOrderRepository.existsByPoNumber(poNum)) {
            throw new BusinessRuleException("PO number '" + poNum + "' already exists");
        }

        PurchaseOrder po = new PurchaseOrder(
                poNum,
                supplier,
                request.getOrderDate(),
                request.getExpectedDate(),
                request.getNotes()
        );

        for (PurchaseOrderItemRequest itemReq : request.getItems()) {
            RawMaterial material = rawMaterialRepository.findById(itemReq.getRawMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Raw material not found with id: " + itemReq.getRawMaterialId()));

            BigDecimal unitPrice = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : material.getUnitCost();
            PurchaseOrderItem item = new PurchaseOrderItem(material, itemReq.getQuantityOrdered(), unitPrice);
            po.addItem(item);
        }

        po.recalculateTotal();
        PurchaseOrder savedPo = purchaseOrderRepository.save(po);
        return mapToResponse(savedPo);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse markAsOrdered(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));

        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessRuleException("Only draft purchase orders can be marked as ORDERED");
        }

        po.setStatus(PurchaseOrderStatus.ORDERED);
        PurchaseOrder updated = purchaseOrderRepository.save(po);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse receivePurchaseOrder(Long id, ReceivePurchaseOrderRequest request, String currentUsername) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));

        if (po.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new BusinessRuleException("Purchase order " + po.getPoNumber() + " has already been received");
        }
        if (po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot receive a cancelled purchase order");
        }

        Map<Long, BigDecimal> receivedMap = request != null ? request.getReceivedQuantities() : null;

        for (PurchaseOrderItem item : po.getItems()) {
            BigDecimal qtyToReceive;
            if (receivedMap != null && receivedMap.containsKey(item.getId())) {
                qtyToReceive = receivedMap.get(item.getId());
            } else {
                qtyToReceive = item.getQuantityOrdered().subtract(item.getQuantityReceived());
            }

            if (qtyToReceive.compareTo(BigDecimal.ZERO) > 0) {
                item.setQuantityReceived(item.getQuantityReceived().add(qtyToReceive));

                // Auto-update raw material inventory
                Inventory inventory = inventoryRepository.findByRawMaterialId(item.getRawMaterial().getId())
                        .orElseGet(() -> {
                            Inventory newInv = Inventory.ofRawMaterial(item.getRawMaterial(), BigDecimal.ZERO, "Raw Materials Warehouse");
                            return inventoryRepository.save(newInv);
                        });

                inventory.setCurrentStock(inventory.getCurrentStock().add(qtyToReceive));
                inventoryRepository.save(inventory);

                inventoryService.recordTransaction(
                        inventory.getId(),
                        InventoryTransactionType.PURCHASE_RECEIPT,
                        qtyToReceive,
                        po.getPoNumber(),
                        "PURCHASE_ORDER",
                        "Received items for PO " + po.getPoNumber(),
                        currentUsername
                );
            }
        }

        po.setStatus(PurchaseOrderStatus.RECEIVED);
        if (request != null && StringUtils.hasText(request.getNotes())) {
            po.setNotes((po.getNotes() != null ? po.getNotes() + " | " : "") + request.getNotes());
        }

        PurchaseOrder updated = purchaseOrderRepository.save(po);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void cancelPurchaseOrder(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));

        if (po.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new BusinessRuleException("Cannot cancel a purchase order that has already been received");
        }

        po.setStatus(PurchaseOrderStatus.CANCELLED);
        purchaseOrderRepository.save(po);
    }

    private PurchaseOrderResponse mapToResponse(PurchaseOrder po) {
        PurchaseOrderResponse res = new PurchaseOrderResponse();
        res.setId(po.getId());
        res.setPoNumber(po.getPoNumber());
        res.setSupplierId(po.getSupplier().getId());
        res.setSupplierCompanyName(po.getSupplier().getCompanyName());
        res.setSupplierContactPerson(po.getSupplier().getContactPerson());
        res.setSupplierEmail(po.getSupplier().getEmail());
        res.setOrderDate(po.getOrderDate());
        res.setExpectedDate(po.getExpectedDate());
        res.setStatus(po.getStatus());
        res.setTotalAmount(po.getTotalAmount());
        res.setNotes(po.getNotes());
        res.setCreatedAt(po.getCreatedAt());

        if (po.getItems() != null) {
            res.setItems(po.getItems().stream().map(item -> {
                PurchaseOrderItemResponse ir = new PurchaseOrderItemResponse();
                ir.setId(item.getId());
                ir.setRawMaterialId(item.getRawMaterial().getId());
                ir.setMaterialCode(item.getRawMaterial().getMaterialCode());
                ir.setMaterialName(item.getRawMaterial().getName());
                ir.setUnitOfMeasure(item.getRawMaterial().getUnitOfMeasure());
                ir.setQuantityOrdered(item.getQuantityOrdered());
                ir.setQuantityReceived(item.getQuantityReceived());
                ir.setUnitPrice(item.getUnitPrice());
                ir.setLineTotal(item.getLineTotal());
                return ir;
            }).collect(Collectors.toList()));
        }

        return res;
    }
}
