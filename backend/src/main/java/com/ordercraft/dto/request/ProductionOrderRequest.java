package com.ordercraft.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class ProductionOrderRequest {
    private String productionNumber;
    private Long customerOrderId; // Optional: Can be null if building for general stock

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity to produce is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityToProduce;

    private LocalDate startDate = LocalDate.now();
    private LocalDate endDate;
    private String notes;

    public ProductionOrderRequest() {}

    public String getProductionNumber() {
        return productionNumber;
    }

    public void setProductionNumber(String productionNumber) {
        this.productionNumber = productionNumber;
    }

    public Long getCustomerOrderId() {
        return customerOrderId;
    }

    public void setCustomerOrderId(Long customerOrderId) {
        this.customerOrderId = customerOrderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantityToProduce() {
        return quantityToProduce;
    }

    public void setQuantityToProduce(Integer quantityToProduce) {
        this.quantityToProduce = quantityToProduce;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
