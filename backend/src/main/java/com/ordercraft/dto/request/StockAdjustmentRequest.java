package com.ordercraft.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class StockAdjustmentRequest {
    @NotNull(message = "Inventory ID is required")
    private Long inventoryId;

    @NotNull(message = "Adjustment quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotBlank(message = "Type must be ADD or DEDUCT")
    private String type; // "ADD" or "DEDUCT"

    @NotBlank(message = "Reason/notes are required for inventory adjustment audit")
    private String notes;

    public StockAdjustmentRequest() {}

    public Long getInventoryId() {
        return id();
    }

    public Long id() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
