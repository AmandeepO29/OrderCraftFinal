package com.ordercraft.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class BomRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;

    private String bomCode;
    private String version = "1.0";
    private String description;

    @NotEmpty(message = "BOM must have at least one raw material item")
    @Valid
    private List<BomItemRequest> items;

    public BomRequest() {}

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getBomCode() {
        return bomCode;
    }

    public void setBomCode(String bomCode) {
        this.bomCode = bomCode;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<BomItemRequest> getItems() {
        return items;
    }

    public void setItems(List<BomItemRequest> items) {
        this.items = items;
    }
}
