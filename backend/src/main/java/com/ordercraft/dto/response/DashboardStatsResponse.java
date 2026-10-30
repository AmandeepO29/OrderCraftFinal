package com.ordercraft.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardStatsResponse {
    private long totalOrders;
    private long pendingOrders;
    private long inProductionOrders;
    private long completedOrders;
    private long cancelledOrders;

    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal pendingPayments = BigDecimal.ZERO;
    private BigDecimal totalPaidRevenue = BigDecimal.ZERO;

    private long lowStockMaterialsCount;
    private long activeProductsCount;
    private long totalCustomersCount;
    private long activeProductionCount;

    private BigDecimal rawMaterialInventoryValuation = BigDecimal.ZERO;
    private BigDecimal finishedGoodsInventoryValuation = BigDecimal.ZERO;

    private List<RevenueChartDataPoint> revenueChart;
    private Map<String, Long> orderStatusDistribution;
    private List<CustomerOrderResponse> recentOrders;
    private List<PaymentResponse> recentPayments;
    private List<RawMaterialResponse> lowStockAlerts;

    public DashboardStatsResponse() {}

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getInProductionOrders() {
        return inProductionOrders;
    }

    public void setInProductionOrders(long inProductionOrders) {
        this.inProductionOrders = inProductionOrders;
    }

    public long getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(long completedOrders) {
        this.completedOrders = completedOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(long cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getPendingPayments() {
        return pendingPayments;
    }

    public void setPendingPayments(BigDecimal pendingPayments) {
        this.pendingPayments = pendingPayments;
    }

    public BigDecimal getTotalPaidRevenue() {
        return totalPaidRevenue;
    }

    public void setTotalPaidRevenue(BigDecimal totalPaidRevenue) {
        this.totalPaidRevenue = totalPaidRevenue;
    }

    public long getLowStockMaterialsCount() {
        return lowStockMaterialsCount;
    }

    public void setLowStockMaterialsCount(long lowStockMaterialsCount) {
        this.lowStockMaterialsCount = lowStockMaterialsCount;
    }

    public long getActiveProductsCount() {
        return activeProductsCount;
    }

    public void setActiveProductsCount(long activeProductsCount) {
        this.activeProductsCount = activeProductsCount;
    }

    public long getTotalCustomersCount() {
        return totalCustomersCount;
    }

    public void setTotalCustomersCount(long totalCustomersCount) {
        this.totalCustomersCount = totalCustomersCount;
    }

    public long getActiveProductionCount() {
        return activeProductionCount;
    }

    public void setActiveProductionCount(long activeProductionCount) {
        this.activeProductionCount = activeProductionCount;
    }

    public BigDecimal getRawMaterialInventoryValuation() {
        return rawMaterialInventoryValuation;
    }

    public void setRawMaterialInventoryValuation(BigDecimal rawMaterialInventoryValuation) {
        this.rawMaterialInventoryValuation = rawMaterialInventoryValuation;
    }

    public BigDecimal getFinishedGoodsInventoryValuation() {
        return finishedGoodsInventoryValuation;
    }

    public void setFinishedGoodsInventoryValuation(BigDecimal finishedGoodsInventoryValuation) {
        this.finishedGoodsInventoryValuation = finishedGoodsInventoryValuation;
    }

    public List<RevenueChartDataPoint> getRevenueChart() {
        return revenueChart;
    }

    public void setRevenueChart(List<RevenueChartDataPoint> revenueChart) {
        this.revenueChart = revenueChart;
    }

    public Map<String, Long> getOrderStatusDistribution() {
        return orderStatusDistribution;
    }

    public void setOrderStatusDistribution(Map<String, Long> orderStatusDistribution) {
        this.orderStatusDistribution = orderStatusDistribution;
    }

    public List<CustomerOrderResponse> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<CustomerOrderResponse> recentOrders) {
        this.recentOrders = recentOrders;
    }

    public List<PaymentResponse> getRecentPayments() {
        return recentPayments;
    }

    public void setRecentPayments(List<PaymentResponse> recentPayments) {
        this.recentPayments = recentPayments;
    }

    public List<RawMaterialResponse> getLowStockAlerts() {
        return lowStockAlerts;
    }

    public void setLowStockAlerts(List<RawMaterialResponse> lowStockAlerts) {
        this.lowStockAlerts = lowStockAlerts;
    }
}
