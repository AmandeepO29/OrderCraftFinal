package com.ordercraft.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class BomAvailabilityCheckResponse {
    private Long productId;
    private String productName;
    private Integer requestedQuantity;
    private boolean sufficient;
    private List<MaterialRequirement> requirements;

    public static class MaterialRequirement {
        private Long rawMaterialId;
        private String materialName;
        private String unitOfMeasure;
        private BigDecimal requiredQuantity;
        private BigDecimal availableStock;
        private boolean isAvailable;

        public MaterialRequirement() {}

        public MaterialRequirement(Long rawMaterialId, String materialName, String unitOfMeasure, BigDecimal requiredQuantity, BigDecimal availableStock, boolean isAvailable) {
            this.rawMaterialId = rawMaterialId;
            this.materialName = materialName;
            this.unitOfMeasure = unitOfMeasure;
            this.requiredQuantity = requiredQuantity;
            this.availableStock = availableStock;
            this.isAvailable = isAvailable;
        }

        public Long getRawMaterialId() {
            return rawMaterialId;
        }

        public void setRawMaterialId(Long rawMaterialId) {
            this.rawMaterialId = rawMaterialId;
        }

        public String getMaterialName() {
            return materialName;
        }

        public void setMaterialName(String materialName) {
            this.materialName = materialName;
        }

        public String getUnitOfMeasure() {
            return unitOfMeasure;
        }

        public void setUnitOfMeasure(String unitOfMeasure) {
            this.unitOfMeasure = unitOfMeasure;
        }

        public BigDecimal getRequiredQuantity() {
            return requiredQuantity;
        }

        public void setRequiredQuantity(BigDecimal requiredQuantity) {
            this.requiredQuantity = requiredQuantity;
        }

        public BigDecimal getAvailableStock() {
            return availableStock;
        }

        public void setAvailableStock(BigDecimal availableStock) {
            this.availableStock = availableStock;
        }

        public boolean isAvailable() {
            return isAvailable;
        }

        public void setAvailable(boolean available) {
            isAvailable = available;
        }
    }

    public BomAvailabilityCheckResponse() {}

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public boolean isSufficient() {
        return sufficient;
    }

    public void setSufficient(boolean sufficient) {
        this.sufficient = sufficient;
    }

    public List<MaterialRequirement> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<MaterialRequirement> requirements) {
        this.requirements = requirements;
    }
}
