package com.ordercraft.service.impl;

import com.ordercraft.entity.CustomerOrder;
import com.ordercraft.entity.Inventory;
import com.ordercraft.entity.InventoryItemType;
import com.ordercraft.entity.Invoice;
import com.ordercraft.entity.Payment;
import com.ordercraft.entity.ProductionOrder;
import com.ordercraft.entity.PurchaseOrder;
import com.ordercraft.repository.CustomerOrderRepository;
import com.ordercraft.repository.InventoryRepository;
import com.ordercraft.repository.InvoiceRepository;
import com.ordercraft.repository.PaymentRepository;
import com.ordercraft.repository.ProductionOrderRepository;
import com.ordercraft.repository.PurchaseOrderRepository;
import com.ordercraft.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public ReportServiceImpl(CustomerOrderRepository customerOrderRepository,
                             ProductionOrderRepository productionOrderRepository,
                             InventoryRepository inventoryRepository,
                             PurchaseOrderRepository purchaseOrderRepository,
                             InvoiceRepository invoiceRepository,
                             PaymentRepository paymentRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.inventoryRepository = inventoryRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSalesReport(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<CustomerOrder> orders = customerOrderRepository.findOrdersBetweenDates(start, end);

        BigDecimal totalSales = orders.stream()
                .filter(o -> !o.getStatus().name().equals("CANCELLED"))
                .map(CustomerOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", start);
        report.put("endDate", end);
        report.put("orderCount", orders.size());
        report.put("totalSales", totalSales);
        report.put("orders", orders);
        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getProductionReport(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<ProductionOrder> productionList = productionOrderRepository.findByDateRange(start, end);
        long completed = productionList.stream().filter(p -> p.getStatus().name().equals("COMPLETED")).count();
        long inProgress = productionList.stream().filter(p -> p.getStatus().name().equals("IN_PROGRESS")).count();

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", start);
        report.put("endDate", end);
        report.put("totalProductionOrders", productionList.size());
        report.put("completedCount", completed);
        report.put("inProgressCount", inProgress);
        report.put("productionOrders", productionList);
        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getInventoryReport() {
        List<Inventory> inventoryList = inventoryRepository.findAll();
        BigDecimal rawValuation = inventoryRepository.calculateRawMaterialValuation();
        BigDecimal finishedValuation = inventoryRepository.calculateFinishedGoodsValuation();
        long lowStockCount = inventoryRepository.countLowStockRawMaterials();

        Map<String, Object> report = new HashMap<>();
        report.put("totalItems", inventoryList.size());
        report.put("rawMaterialValuation", rawValuation);
        report.put("finishedGoodsValuation", finishedValuation);
        report.put("totalValuation", rawValuation.add(finishedValuation));
        report.put("lowStockCount", lowStockCount);
        report.put("inventory", inventoryList);
        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPurchaseReport(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<PurchaseOrder> poList = purchaseOrderRepository.findByDateRange(start, end);
        BigDecimal totalSpend = poList.stream()
                .filter(po -> po.getStatus().name().equals("RECEIVED"))
                .map(PurchaseOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", start);
        report.put("endDate", end);
        report.put("totalPurchaseOrders", poList.size());
        report.put("totalSpend", totalSpend);
        report.put("purchaseOrders", poList);
        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getRevenueReport(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<Invoice> invoices = invoiceRepository.findByDateRange(start, end);
        List<Payment> payments = paymentRepository.findByDateRange(start, end);

        BigDecimal invoicedTotal = invoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal collectedTotal = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", start);
        report.put("endDate", end);
        report.put("invoicesCount", invoices.size());
        report.put("totalInvoiced", invoicedTotal);
        report.put("paymentsCount", payments.size());
        report.put("totalCollected", collectedTotal);
        report.put("outstanding", invoicedTotal.subtract(collectedTotal));
        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportReportToCsv(String reportType, LocalDate startDate, LocalDate endDate) {
        StringBuilder csv = new StringBuilder();
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        switch (reportType.toUpperCase()) {
            case "SALES":
                csv.append("Order Number,Customer,Order Date,Status,Subtotal,Tax,Discount,Total\n");
                List<CustomerOrder> orders = customerOrderRepository.findOrdersBetweenDates(start, end);
                for (CustomerOrder o : orders) {
                    csv.append(o.getOrderNumber()).append(",")
                            .append("\"").append(o.getCustomer().getName()).append("\",")
                            .append(o.getOrderDate()).append(",")
                            .append(o.getStatus()).append(",")
                            .append(o.getSubtotal()).append(",")
                            .append(o.getTaxAmount()).append(",")
                            .append(o.getDiscountAmount()).append(",")
                            .append(o.getTotalAmount()).append("\n");
                }
                break;

            case "PURCHASES":
                csv.append("PO Number,Supplier,Order Date,Expected Date,Status,Total Amount\n");
                List<PurchaseOrder> pos = purchaseOrderRepository.findByDateRange(start, end);
                for (PurchaseOrder po : pos) {
                    csv.append(po.getPoNumber()).append(",")
                            .append("\"").append(po.getSupplier().getCompanyName()).append("\",")
                            .append(po.getOrderDate()).append(",")
                            .append(po.getExpectedDate()).append(",")
                            .append(po.getStatus()).append(",")
                            .append(po.getTotalAmount()).append("\n");
                }
                break;

            case "PRODUCTION":
                csv.append("Production Number,Product,Quantity To Produce,Quantity Produced,Start Date,End Date,Status\n");
                List<ProductionOrder> prds = productionOrderRepository.findByDateRange(start, end);
                for (ProductionOrder p : prds) {
                    csv.append(p.getProductionNumber()).append(",")
                            .append("\"").append(p.getProduct().getName()).append("\",")
                            .append(p.getQuantityToProduce()).append(",")
                            .append(p.getQuantityProduced()).append(",")
                            .append(p.getStartDate()).append(",")
                            .append(p.getEndDate()).append(",")
                            .append(p.getStatus()).append("\n");
                }
                break;

            case "INVENTORY":
            default:
                csv.append("Type,Item Code,Item Name,Current Stock,Reserved Stock,Available Stock,Location\n");
                List<Inventory> invs = inventoryRepository.findAll();
                for (Inventory i : invs) {
                    String code = i.getItemType() == InventoryItemType.RAW_MATERIAL ? i.getRawMaterial().getMaterialCode() : i.getProduct().getSku();
                    String name = i.getItemType() == InventoryItemType.RAW_MATERIAL ? i.getRawMaterial().getName() : i.getProduct().getName();
                    csv.append(i.getItemType()).append(",")
                            .append(code).append(",")
                            .append("\"").append(name).append("\",")
                            .append(i.getCurrentStock()).append(",")
                            .append(i.getReservedStock()).append(",")
                            .append(i.getAvailableStock()).append(",")
                            .append("\"").append(i.getLocation() != null ? i.getLocation() : "").append("\"\n");
                }
                break;
        }

        return csv.toString();
    }
}
