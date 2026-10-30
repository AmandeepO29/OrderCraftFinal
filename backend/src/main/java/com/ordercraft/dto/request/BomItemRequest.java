package com.ordercraft.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BomItemRequest {
    @NotNull(message = "Raw material ID is required")
    private Long rawMaterialId;

    @NotNull(message = "Quantity required must be specified")
    @DecimalMin(value = "0.0001", message = "Quantity required must be greater than 0")
    private BigDecimal quantityRequired;

    private String unitOfMeasure;
    private BigDecimal scrapPercentage = BigDecimal.ZERO;

    public BomItemRequest() {}

    public Long getRawMaterialId() {
        return rawMaterialId;
    }

    public void setRawMaterialId(Long rawMaterialId) {
        this.rawMaterialId = rawMaterialId;
    }

    public BigDecimal getQuantityRequired() {
        return quantityRequired;
    }

    public void setQuantityRequired(BigDecimal quantityRequired) {
        this.quantityRequired = quantityRequired;
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
    }
}
