package com.ordercraft.dto.request;

import java.math.BigDecimal;
import java.util.Map;

public class ReceivePurchaseOrderRequest {
    // Map of PurchaseOrderItem ID -> Quantity Received (if null, receives remaining ordered quantity)
    private Map<Long, BigDecimal> receivedQuantities;
    private String notes;

    public ReceivePurchaseOrderRequest() {}

    public Map<Long, BigDecimal> getReceivedQuantities() {
        return receivedQuantities;
    }

    public void setReceivedQuantities(Map<Long, BigDecimal> receivedQuantities) {
        this.receivedQuantities = receivedQuantities;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
