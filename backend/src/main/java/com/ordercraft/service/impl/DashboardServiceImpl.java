package com.ordercraft.service.impl;

import com.ordercraft.dto.response.CustomerOrderResponse;
import com.ordercraft.dto.response.DashboardStatsResponse;
import com.ordercraft.dto.response.OrderItemResponse;
import com.ordercraft.dto.response.PaymentResponse;
import com.ordercraft.dto.response.RawMaterialResponse;
import com.ordercraft.dto.response.RevenueChartDataPoint;
import com.ordercraft.entity.CustomerOrder;
import com.ordercraft.entity.Inventory;
import com.ordercraft.entity.OrderStatus;
import com.ordercraft.entity.Payment;
import com.ordercraft.entity.ProductStatus;
import com.ordercraft.entity.ProductionStatus;
import com.ordercraft.repository.CustomerOrderRepository;
import com.ordercraft.repository.CustomerRepository;
import com.ordercraft.repository.InventoryRepository;
import com.ordercraft.repository.InvoiceRepository;
import com.ordercraft.repository.PaymentRepository;
import com.ordercraft.repository.ProductRepository;
import com.ordercraft.repository.ProductionOrderRepository;
import com.ordercraft.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ProductionOrderRepository productionOrderRepository;

    public DashboardServiceImpl(CustomerOrderRepository customerOrderRepository,
                                CustomerRepository customerRepository,
                                ProductRepository productRepository,
                                InventoryRepository inventoryRepository,
                                InvoiceRepository invoiceRepository,
                                PaymentRepository paymentRepository,
                                ProductionOrderRepository productionOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.productionOrderRepository = productionOrderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        // 1. Order Counts
        stats.setTotalOrders(customerOrderRepository.count());
        stats.setPendingOrders(customerOrderRepository.countByStatus(OrderStatus.PENDING));
        stats.setInProductionOrders(customerOrderRepository.countByStatus(OrderStatus.IN_PRODUCTION));
        stats.setCompletedOrders(customerOrderRepository.countByStatus(OrderStatus.COMPLETED));
        stats.setCancelledOrders(customerOrderRepository.countByStatus(OrderStatus.CANCELLED));

        // 2. Financial Metrics
        stats.setTotalRevenue(customerOrderRepository.calculateTotalRevenue());
        stats.setPendingPayments(invoiceRepository.calculatePendingPayments());
        stats.setTotalPaidRevenue(invoiceRepository.calculateTotalPaid());

        // 3. Operational Counts
        stats.setLowStockMaterialsCount(inventoryRepository.countLowStockRawMaterials());
        stats.setActiveProductsCount(productRepository.countByStatus(ProductStatus.ACTIVE));
        stats.setTotalCustomersCount(customerRepository.countByActiveTrue());
        stats.setActiveProductionCount(productionOrderRepository.countByStatus(ProductionStatus.IN_PROGRESS) +
                productionOrderRepository.countByStatus(ProductionStatus.PLANNED));

        // 4. Inventory Valuation
        stats.setRawMaterialInventoryValuation(inventoryRepository.calculateRawMaterialValuation());
        stats.setFinishedGoodsInventoryValuation(inventoryRepository.calculateFinishedGoodsValuation());

        // 5. Order Status Distribution Map
        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (OrderStatus st : OrderStatus.values()) {
            statusMap.put(st.name(), customerOrderRepository.countByStatus(st));
        }
        stats.setOrderStatusDistribution(statusMap);

        // 6. Time-Series Revenue Chart (Past 7 Days)
        List<RevenueChartDataPoint> chartPoints = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String label = date.format(formatter);

            List<CustomerOrder> dailyOrders = customerOrderRepository.findOrdersBetweenDates(date, date);
            BigDecimal dailyRevenue = dailyOrders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .map(CustomerOrder::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<Payment> dailyPayments = paymentRepository.findByDateRange(date, date);
            BigDecimal dailyPaid = dailyPayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            chartPoints.add(new RevenueChartDataPoint(label, dailyRevenue, dailyPaid, dailyOrders.size()));
        }
        stats.setRevenueChart(chartPoints);

        // 7. Recent Orders
        List<CustomerOrder> recentOrders = customerOrderRepository.findTop5ByOrderByCreatedAtDesc();
        stats.setRecentOrders(recentOrders.stream().map(this::mapOrderToResponse).collect(Collectors.toList()));

        // 8. Recent Payments
        List<Payment> recentPayments = paymentRepository.findTop5ByOrderByCreatedAtDesc();
        stats.setRecentPayments(recentPayments.stream().map(this::mapPaymentToResponse).collect(Collectors.toList()));

        // 9. Low Stock Alerts
        List<Inventory> lowStockInvs = inventoryRepository.findLowStockRawMaterials();
        stats.setLowStockAlerts(lowStockInvs.stream()
                .filter(inv -> inv.getRawMaterial() != null)
                .limit(5)
                .map(inv -> {
                    RawMaterialResponse rm = new RawMaterialResponse();
                    rm.setId(inv.getRawMaterial().getId());
                    rm.setMaterialCode(inv.getRawMaterial().getMaterialCode());
                    rm.setName(inv.getRawMaterial().getName());
                    rm.setUnitOfMeasure(inv.getRawMaterial().getUnitOfMeasure());
                    rm.setUnitCost(inv.getRawMaterial().getUnitCost());
                    rm.setMinimumStockLevel(inv.getRawMaterial().getMinimumStockLevel());
                    rm.setReorderQuantity(inv.getRawMaterial().getReorderQuantity());
                    rm.setCurrentStock(inv.getCurrentStock());
                    rm.setAvailableStock(inv.getAvailableStock());
                    rm.setLowStock(true);
                    return rm;
                }).collect(Collectors.toList()));

        return stats;
    }

    private CustomerOrderResponse mapOrderToResponse(CustomerOrder order) {
        CustomerOrderResponse res = new CustomerOrderResponse();
        res.setId(order.getId());
        res.setOrderNumber(order.getOrderNumber());
        res.setCustomerId(order.getCustomer().getId());
        res.setCustomerName(order.getCustomer().getName());
        res.setCustomerCompanyName(order.getCustomer().getCompanyName());
        res.setOrderDate(order.getOrderDate());
        res.setStatus(order.getStatus());
        res.setTotalAmount(order.getTotalAmount());
        res.setCreatedAt(order.getCreatedAt());

        if (order.getItems() != null) {
            res.setItems(order.getItems().stream().map(item -> {
                OrderItemResponse ir = new OrderItemResponse();
                ir.setId(item.getId());
                ir.setProductId(item.getProduct().getId());
                ir.setProductName(item.getProduct().getName());
                ir.setQuantity(item.getQuantity());
                ir.setUnitPrice(item.getUnitPrice());
                ir.setLineTotal(item.getLineTotal());
                return ir;
            }).collect(Collectors.toList()));
        }
        return res;
    }

    private PaymentResponse mapPaymentToResponse(Payment p) {
        PaymentResponse res = new PaymentResponse();
        res.setId(p.getId());
        res.setPaymentReference(p.getPaymentReference());
        res.setInvoiceId(p.getInvoice().getId());
        res.setInvoiceNumber(p.getInvoice().getInvoiceNumber());
        if (p.getInvoice().getCustomerOrder() != null && p.getInvoice().getCustomerOrder().getCustomer() != null) {
            res.setCustomerName(p.getInvoice().getCustomerOrder().getCustomer().getName());
        }
        res.setPaymentDate(p.getPaymentDate());
        res.setAmount(p.getAmount());
        res.setPaymentMethod(p.getPaymentMethod());
        res.setTransactionNumber(p.getTransactionNumber());
        res.setCreatedAt(p.getCreatedAt());
        return res;
    }
}
