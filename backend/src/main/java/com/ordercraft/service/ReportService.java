package com.ordercraft.service;

import java.time.LocalDate;
import java.util.Map;

public interface ReportService {
    Map<String, Object> getSalesReport(LocalDate startDate, LocalDate endDate);
    Map<String, Object> getProductionReport(LocalDate startDate, LocalDate endDate);
    Map<String, Object> getInventoryReport();
    Map<String, Object> getPurchaseReport(LocalDate startDate, LocalDate endDate);
    Map<String, Object> getRevenueReport(LocalDate startDate, LocalDate endDate);
    String exportReportToCsv(String reportType, LocalDate startDate, LocalDate endDate);
}
