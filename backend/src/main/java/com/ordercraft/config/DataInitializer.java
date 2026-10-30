package com.ordercraft.config;

import com.ordercraft.entity.*;
import com.ordercraft.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final ProductRepository productRepository;
    private final BomRepository bomRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           CustomerRepository customerRepository,
                           SupplierRepository supplierRepository,
                           RawMaterialRepository rawMaterialRepository,
                           ProductRepository productRepository,
                           BomRepository bomRepository,
                           InventoryRepository inventoryRepository,
                           CustomerOrderRepository customerOrderRepository,
                           PurchaseOrderRepository purchaseOrderRepository,
                           ProductionOrderRepository productionOrderRepository,
                           InvoiceRepository invoiceRepository,
                           PaymentRepository paymentRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.rawMaterialRepository = rawMaterialRepository;
        this.productRepository = productRepository;
        this.bomRepository = bomRepository;
        this.inventoryRepository = inventoryRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            logger.info("Database already seeded. Skipping initialization.");
            return;
        }

        logger.info("Initializing OrderCraft Enterprise Sample Data...");

        // 1. Roles
        Role adminRole = roleRepository.save(new Role(RoleType.ROLE_ADMIN, "System Administrator"));
        Role managerRole = roleRepository.save(new Role(RoleType.ROLE_MANAGER, "Manufacturing & Operations Manager"));
        Role employeeRole = roleRepository.save(new Role(RoleType.ROLE_EMPLOYEE, "Production & Shop Floor Employee"));

        // 2. Users (Default password: password123)
        User admin = new User("admin", "admin@ordercraft.com", passwordEncoder.encode("password123"), "Alexander Vance", "+1-555-0101");
        admin.setRoles(Set.of(adminRole, managerRole));
        userRepository.save(admin);

        User manager = new User("manager", "manager@ordercraft.com", passwordEncoder.encode("password123"), "Elena Rostova", "+1-555-0102");
        manager.setRoles(Set.of(managerRole));
        userRepository.save(manager);

        User employee = new User("employee", "employee@ordercraft.com", passwordEncoder.encode("password123"), "Marcus Chen", "+1-555-0103");
        employee.setRoles(Set.of(employeeRole));
        userRepository.save(employee);

        // 3. Suppliers
        Supplier s1 = new Supplier("SUP-101", "Apex Steel & Alloys Corp", "David Miller", "dmiller@apexsteel.com", "+1-555-301", "1200 Industrial Pkwy, Cleveland, OH", "Net 30");
        s1.setTaxId("US-EIN-992143");
        Supplier s2 = new Supplier("SUP-102", "PolymerTech Industries", "Sarah Jenkins", "sjenkins@polymertech.com", "+1-555-302", "450 Polymer Way, Wilmington, DE", "Net 15");
        s2.setTaxId("US-EIN-881234");
        Supplier s3 = new Supplier("SUP-103", "Precision Fasteners Ltd", "Kenji Sato", "ksato@precisionfast.com", "+1-555-303", "88 Assembly Rd, Detroit, MI", "Net 30");
        s3.setTaxId("US-EIN-774411");
        supplierRepository.saveAll(java.util.List.of(s1, s2, s3));

        // 4. Raw Materials
        RawMaterial rm1 = new RawMaterial("RM-ALUM-6061", "Aerospace Grade Aluminum 6061", "High-strength extruded bar stock", "kg", new BigDecimal("12.50"), new BigDecimal("100.00"), new BigDecimal("500.00"));
        rm1.setPreferredSupplier(s1);
        RawMaterial rm2 = new RawMaterial("RM-ST-304", "Stainless Steel 304 Sheet", "1.5mm cold rolled sheet", "sq_m", new BigDecimal("45.00"), new BigDecimal("50.00"), new BigDecimal("200.00"));
        rm2.setPreferredSupplier(s1);
        RawMaterial rm3 = new RawMaterial("RM-POLY-HDPE", "High-Density Polyethylene Granules", "Injection grade virgin polymer", "kg", new BigDecimal("4.20"), new BigDecimal("250.00"), new BigDecimal("1000.00"));
        rm3.setPreferredSupplier(s2);
        RawMaterial rm4 = new RawMaterial("RM-SCREW-M4", "Titanium Hex Screws M4x12", "ISO 4762 zinc-nickel plated", "pcs", new BigDecimal("0.35"), new BigDecimal("1000.00"), new BigDecimal("5000.00"));
        rm4.setPreferredSupplier(s3);
        RawMaterial rm5 = new RawMaterial("RM-SEAL-O-RING", "Viton O-Ring Seals 25mm", "High-temperature chemical resistant", "pcs", new BigDecimal("1.10"), new BigDecimal("200.00"), new BigDecimal("800.00"));
        rm5.setPreferredSupplier(s2);
        rawMaterialRepository.saveAll(java.util.List.of(rm1, rm2, rm3, rm4, rm5));

        // 5. Raw Material Initial Inventory
        Inventory invRm1 = Inventory.ofRawMaterial(rm1, new BigDecimal("450.00"), "Rack A1 - Metals Bay");
        Inventory invRm2 = Inventory.ofRawMaterial(rm2, new BigDecimal("120.00"), "Rack A2 - Sheet Metal");
        Inventory invRm3 = Inventory.ofRawMaterial(rm3, new BigDecimal("850.00"), "Silo B - Polymers");
        Inventory invRm4 = Inventory.ofRawMaterial(rm4, new BigDecimal("3200.00"), "Bin C3 - Fasteners");
        Inventory invRm5 = Inventory.ofRawMaterial(rm5, new BigDecimal("45.00"), "Bin C4 - Seals (LOW STOCK)"); // Low stock trigger
        inventoryRepository.saveAll(java.util.List.of(invRm1, invRm2, invRm3, invRm4, invRm5));

        // 6. Products
        Product p1 = new Product("PRD-ACTUATOR-X1", "Hydraulic Precision Actuator X1", "High-torque industrial actuator for robotic automation", "Hydraulics", new BigDecimal("850.00"), 5);
        Product p2 = new Product("PRD-ENCLOSURE-M2", "IP67 Weatherproof Server Enclosure M2", "Modular CNC-machined protective chassis", "Chassis", new BigDecimal("420.00"), 4);
        Product p3 = new Product("PRD-VALVE-ASSY-V3", "Flow Control High-Pressure Valve V3", "Dual-stage pressure regulating valve assembly", "Fluidics", new BigDecimal("290.00"), 3);
        productRepository.saveAll(java.util.List.of(p1, p2, p3));

        // 7. Product Initial Inventory
        Inventory invP1 = Inventory.ofProduct(p1, new BigDecimal("18.00"), "Finished Goods Bay - F1");
        Inventory invP2 = Inventory.ofProduct(p2, new BigDecimal("25.00"), "Finished Goods Bay - F2");
        Inventory invP3 = Inventory.ofProduct(p3, new BigDecimal("40.00"), "Finished Goods Bay - F3");
        inventoryRepository.saveAll(java.util.List.of(invP1, invP2, invP3));

        // 8. Bill of Materials (BOM)
        // BOM for Actuator X1
        Bom bom1 = new Bom(p1, "BOM-ACT-X1", "2.1", "Standard assembly BOM for hydraulic actuator X1");
        bom1.addItem(new BomItem(rm1, new BigDecimal("8.50"), "kg", new BigDecimal("2.0")));
        bom1.addItem(new BomItem(rm4, new BigDecimal("16.00"), "pcs", BigDecimal.ZERO));
        bom1.addItem(new BomItem(rm5, new BigDecimal("4.00"), "pcs", BigDecimal.ZERO));
        bom1.recalculateTotalCost();
        bomRepository.save(bom1);

        // BOM for Enclosure M2
        Bom bom2 = new Bom(p2, "BOM-ENC-M2", "1.0", "Precision folded and welded steel enclosure");
        bom2.addItem(new BomItem(rm2, new BigDecimal("2.20"), "sq_m", new BigDecimal("3.0")));
        bom2.addItem(new BomItem(rm4, new BigDecimal("24.00"), "pcs", BigDecimal.ZERO));
        bom2.recalculateTotalCost();
        bomRepository.save(bom2);

        // BOM for Valve Assy V3
        Bom bom3 = new Bom(p3, "BOM-VAL-V3", "1.5", "High pressure dual-chamber valve assembly");
        bom3.addItem(new BomItem(rm1, new BigDecimal("3.20"), "kg", new BigDecimal("1.5")));
        bom3.addItem(new BomItem(rm3, new BigDecimal("1.80"), "kg", new BigDecimal("2.0")));
        bom3.addItem(new BomItem(rm5, new BigDecimal("6.00"), "pcs", BigDecimal.ZERO));
        bom3.recalculateTotalCost();
        bomRepository.save(bom3);

        // 9. Customers
        Customer c1 = new Customer("CUST-1001", "Tesla Energy Systems", "procurement@teslaenergy.corp", "+1-555-881", "Tesla Inc.", "3500 Deer Creek Rd, Palo Alto, CA", "Fremont Factory Gigabay 4");
        c1.setTaxId("US-CA-994821");
        Customer c2 = new Customer("CUST-1002", "Siemens Advanced Mobility", "orders@siemens-mobility.de", "+1-555-882", "Siemens AG", "Otto-Hahn-Ring 6, Munich", "Logistics Hub B - Port Newark, NJ");
        c2.setTaxId("DE-129274202");
        Customer c3 = new Customer("CUST-1003", "Boston Dynamics Automation", "supply@bostondynamics.com", "+1-555-883", "Boston Dynamics", "78 4th Ave, Waltham, MA", "Waltham Labs Dock 2");
        c3.setTaxId("US-MA-331002");
        customerRepository.saveAll(java.util.List.of(c1, c2, c3));

        // 10. Sample Customer Orders
        CustomerOrder o1 = new CustomerOrder("ORD-2024-001", c1, LocalDate.now().minusDays(15), LocalDate.now().minusDays(5), "Priority order for pilot factory expansion");
        o1.addItem(new OrderItem(p1, 5, p1.getUnitPrice()));
        o1.addItem(new OrderItem(p2, 10, p2.getUnitPrice()));
        o1.setStatus(OrderStatus.COMPLETED);
        o1.recalculateTotals();
        customerOrderRepository.save(o1);

        CustomerOrder o2 = new CustomerOrder("ORD-2024-002", c2, LocalDate.now().minusDays(8), LocalDate.now().plusDays(10), "Regular quarterly actuator procurement");
        o2.addItem(new OrderItem(p1, 12, p1.getUnitPrice()));
        o2.setStatus(OrderStatus.IN_PRODUCTION);
        o2.recalculateTotals();
        customerOrderRepository.save(o2);

        CustomerOrder o3 = new CustomerOrder("ORD-2024-003", c3, LocalDate.now().minusDays(2), LocalDate.now().plusDays(14), "High precision valve batches");
        o3.addItem(new OrderItem(p3, 20, p3.getUnitPrice()));
        o3.setStatus(OrderStatus.CONFIRMED);
        o3.recalculateTotals();
        customerOrderRepository.save(o3);

        // 11. Sample Invoices & Payments
        Invoice inv1 = new Invoice("INV-2024-001", o1, LocalDate.now().minusDays(14), LocalDate.now().plusDays(16), o1.getSubtotal(), new BigDecimal("10.00"), "Net 30 invoice");
        inv1.recalculateAmounts();
        invoiceRepository.save(inv1);

        Payment pay1 = new Payment("PAY-2024-001", inv1, LocalDate.now().minusDays(10), inv1.getTotalAmount(), PaymentMethod.BANK_TRANSFER, "WIRE-FED-8842109", "Full payment received via wire transfer", admin);
        inv1.addPayment(pay1);
        paymentRepository.save(pay1);
        invoiceRepository.save(inv1);

        Invoice inv2 = new Invoice("INV-2024-002", o2, LocalDate.now().minusDays(7), LocalDate.now().plusDays(23), o2.getSubtotal(), new BigDecimal("10.00"), "Deposit requested");
        inv2.recalculateAmounts();
        invoiceRepository.save(inv2);

        Payment pay2 = new Payment("PAY-2024-002", inv2, LocalDate.now().minusDays(5), new BigDecimal("5000.00"), PaymentMethod.BANK_TRANSFER, "WIRE-CITI-442199", "50% advance deposit", manager);
        inv2.addPayment(pay2);
        paymentRepository.save(pay2);
        invoiceRepository.save(inv2);

        // 12. Sample Purchase Orders
        PurchaseOrder po1 = new PurchaseOrder("PO-2024-001", s1, LocalDate.now().minusDays(20), LocalDate.now().minusDays(12), "Raw material replenishment");
        po1.addItem(new PurchaseOrderItem(rm1, new BigDecimal("300.00"), rm1.getUnitCost()));
        po1.addItem(new PurchaseOrderItem(rm2, new BigDecimal("80.00"), rm2.getUnitCost()));
        po1.setStatus(PurchaseOrderStatus.RECEIVED);
        po1.recalculateTotal();
        purchaseOrderRepository.save(po1);

        PurchaseOrder po2 = new PurchaseOrder("PO-2024-002", s3, LocalDate.now().minusDays(4), LocalDate.now().plusDays(5), "Fasteners bulk order");
        po2.addItem(new PurchaseOrderItem(rm4, new BigDecimal("5000.00"), rm4.getUnitCost()));
        po2.setStatus(PurchaseOrderStatus.ORDERED);
        po2.recalculateTotal();
        purchaseOrderRepository.save(po2);

        // 13. Sample Production Orders
        ProductionOrder prd1 = new ProductionOrder("PRD-2024-001", o1, p1, 5, LocalDate.now().minusDays(14), LocalDate.now().minusDays(7), "Production completed for Tesla order");
        prd1.setQuantityProduced(5);
        prd1.setStatus(ProductionStatus.COMPLETED);
        productionOrderRepository.save(prd1);

        ProductionOrder prd2 = new ProductionOrder("PRD-2024-002", o2, p1, 12, LocalDate.now().minusDays(6), LocalDate.now().plusDays(3), "Actuator manufacturing run currently in progress");
        prd2.setStatus(ProductionStatus.IN_PROGRESS);
        productionOrderRepository.save(prd2);

        ProductionOrder prd3 = new ProductionOrder("PRD-2024-003", o3, p3, 20, LocalDate.now().minusDays(1), LocalDate.now().plusDays(8), "Valve assembly run queued");
        prd3.setStatus(ProductionStatus.PLANNED);
        productionOrderRepository.save(prd3);

        logger.info("OrderCraft sample data initialized successfully!");
    }
}
