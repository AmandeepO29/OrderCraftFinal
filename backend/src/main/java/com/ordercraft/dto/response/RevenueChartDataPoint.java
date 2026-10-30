package com.ordercraft.dto.response;

import java.math.BigDecimal;

public class RevenueChartDataPoint {
    private String date;
    private BigDecimal revenue;
    private BigDecimal payments;
    private long orderCount;

    public RevenueChartDataPoint() {}

    public RevenueChartDataPoint(String date, BigDecimal revenue, BigDecimal payments, long orderCount) {
        this.date = date;
        this.revenue = revenue;
        this.payments = payments;
        this.orderCount = orderCount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getPayments() {
        return payments;
    }

    public void setPayments(BigDecimal payments) {
        this.payments = payments;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }
}
