package com.ordercraft.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class RawMaterialRequest {
    private String materialCode;

    @NotBlank(message = "Material name is required")
    private String name;

    private String description;

    @NotBlank(message = "Unit of measure is required (e.g., kg, meter, pcs)")
    private String unitOfMeasure;

    @NotNull(message = "Unit cost is required")
    @DecimalMin(value = "0.01", message = "Unit cost must be greater than 0")
    private BigDecimal unitCost;

    @NotNull(message = "Minimum stock level is required")
    @DecimalMin(value = "0.00", message = "Minimum stock cannot be negative")
    private BigDecimal minimumStockLevel;

    @NotNull(message = "Reorder quantity is required")
    @DecimalMin(value = "0.01", message = "Reorder quantity must be greater than 0")
    private BigDecimal reorderQuantity;

    private Long preferredSupplierId;
    private BigDecimal initialStock = BigDecimal.ZERO;

    public RawMaterialRequest() {}

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getMinimumStockLevel() {
        return minimumStockLevel;
    }

    public void setMinimumStockLevel(BigDecimal minimumStockLevel) {
        this.minimumStockLevel = minimumStockLevel;
    }

    public BigDecimal getReorderQuantity() {
        return reorderQuantity;
    }

    public void setReorderQuantity(BigDecimal reorderQuantity) {
        this.reorderQuantity = reorderQuantity;
    }

    public Long getPreferredSupplierId() {
        return preferredSupplierId;
    }

    public void setPreferredSupplierId(Long preferredSupplierId) {
        this.preferredSupplierId = preferredSupplierId;
    }

    public BigDecimal getInitialStock() {
        return initialStock;
    }

    public void setInitialStock(BigDecimal initialStock) {
        this.initialStock = initialStock;
    }
}
