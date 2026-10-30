package com.ordercraft.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "bom_items")
public class BomItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id", nullable = false)
    @JsonIgnore
    private Bom bom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "raw_material_id", nullable = false)
    private RawMaterial rawMaterial;

    @Column(name = "quantity_required", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantityRequired;

    @Column(name = "unit_of_measure", nullable = false, length = 30)
    private String unitOfMeasure;

    @Column(name = "scrap_percentage", precision = 5, scale = 2)
    private BigDecimal scrapPercentage = BigDecimal.ZERO;

    @Column(name = "item_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal itemCost = BigDecimal.ZERO;

    public BomItem() {}

    public BomItem(RawMaterial rawMaterial, BigDecimal quantityRequired, String unitOfMeasure, BigDecimal scrapPercentage) {
        this.rawMaterial = rawMaterial;
        this.quantityRequired = quantityRequired;
        this.unitOfMeasure = unitOfMeasure;
        this.scrapPercentage = scrapPercentage != null ? scrapPercentage : BigDecimal.ZERO;
        calculateCost();
    }

    public void calculateCost() {
        if (rawMaterial != null && rawMaterial.getUnitCost() != null && quantityRequired != null) {
            BigDecimal factor = BigDecimal.ONE;
            if (scrapPercentage != null && scrapPercentage.compareTo(BigDecimal.ZERO) > 0) {
                factor = factor.add(scrapPercentage.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
            }
            this.itemCost = rawMaterial.getUnitCost().multiply(quantityRequired).multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bom getBom() {
        return bom;
    }

    public void setBom(Bom bom) {
        this.bom = bom;
    }

    public RawMaterial getRawMaterial() {
        return rawMaterial;
    }

    public void setRawMaterial(RawMaterial rawMaterial) {
        this.rawMaterial = rawMaterial;
        calculateCost();
    }

    public BigDecimal getQuantityRequired() {
        return quantityRequired;
    }

    public void setQuantityRequired(BigDecimal quantityRequired) {
        this.quantityRequired = quantityRequired;
        calculateCost();
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getScrapPercentage() {
        return scrapPercentage;
    }

    public void setScrapPercentage(BigDecimal scrapPercentage) {
        this.scrapPercentage = scrapPercentage;
        calculateCost();
    }

    public BigDecimal getItemCost() {
        return itemCost;
    }

    public void setItemCost(BigDecimal itemCost) {
        this.itemCost = itemCost;
    }
}
