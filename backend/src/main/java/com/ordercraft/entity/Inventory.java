package com.ordercraft.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private InventoryItemType itemType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "raw_material_id")
    private RawMaterial rawMaterial;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "current_stock", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(name = "reserved_stock", nullable = false, precision = 12, scale = 2)
    private BigDecimal reservedStock = BigDecimal.ZERO;

    @Column(name = "available_stock", nullable = false, precision = 12, scale = 2)
    private BigDecimal availableStock = BigDecimal.ZERO;

    @Column(length = 100)
    private String location; // e.g., "Warehouse A - Rack 3"

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        this.lastUpdated = LocalDateTime.now();
        recalculateAvailableStock();
    }

    public void recalculateAvailableStock() {
        if (currentStock == null) currentStock = BigDecimal.ZERO;
        if (reservedStock == null) reservedStock = BigDecimal.ZERO;
        this.availableStock = currentStock.subtract(reservedStock);
    }

    public Inventory() {}

    public static Inventory ofRawMaterial(RawMaterial material, BigDecimal initialStock, String location) {
        Inventory inv = new Inventory();
        inv.setItemType(InventoryItemType.RAW_MATERIAL);
        inv.setRawMaterial(material);
        inv.setCurrentStock(initialStock != null ? initialStock : BigDecimal.ZERO);
        inv.setReservedStock(BigDecimal.ZERO);
        inv.setLocation(location);
        inv.recalculateAvailableStock();
        return inv;
    }

    public static Inventory ofProduct(Product product, BigDecimal initialStock, String location) {
        Inventory inv = new Inventory();
        inv.setItemType(InventoryItemType.FINISHED_PRODUCT);
        inv.setProduct(product);
        inv.setCurrentStock(initialStock != null ? initialStock : BigDecimal.ZERO);
        inv.setReservedStock(BigDecimal.ZERO);
        inv.setLocation(location);
        inv.recalculateAvailableStock();
        return inv;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InventoryItemType getItemType() {
        return itemType;
    }

    public void setItemType(InventoryItemType itemType) {
        this.itemType = itemType;
    }

    public RawMaterial getRawMaterial() {
        return rawMaterial;
    }

    public void setRawMaterial(RawMaterial rawMaterial) {
        this.rawMaterial = rawMaterial;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
        recalculateAvailableStock();
    }

    public BigDecimal getReservedStock() {
        return reservedStock;
    }

    public void setReservedStock(BigDecimal reservedStock) {
        this.reservedStock = reservedStock;
        recalculateAvailableStock();
    }

    public BigDecimal getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(BigDecimal availableStock) {
        this.availableStock = availableStock;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
