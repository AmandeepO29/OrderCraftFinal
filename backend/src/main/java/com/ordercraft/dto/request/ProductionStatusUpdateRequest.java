package com.ordercraft.dto.request;

import com.ordercraft.entity.ProductionStatus;
import jakarta.validation.constraints.NotNull;

public class ProductionStatusUpdateRequest {
    @NotNull(message = "Production status is required")
    private ProductionStatus status;

    private Integer quantityProduced;
    private String notes;

    public ProductionStatusUpdateRequest() {}

    public ProductionStatus getStatus() {
        return status;
    }

    public void setStatus(ProductionStatus status) {
        this.status = status;
    }

    public Integer getQuantityProduced() {
        return quantityProduced;
    }

    public void setQuantityProduced(Integer quantityProduced) {
        this.quantityProduced = quantityProduced;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
