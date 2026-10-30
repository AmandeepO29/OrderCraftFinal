# OrderCraft – Manufacturing Order Management System (OMS)

[![Java Version](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x%20JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![React](https://img.shields.io/badge/React-18.x-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://reactjs.org/)
[![TailwindCSS](https://img.shields.io/badge/Tailwind%20CSS-3.4-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)

**OrderCraft** is an industry-grade Full-Stack Manufacturing Order Management System (OMS) architected to streamline end-to-end discrete manufacturing operations. It automates customer order processing, Bill of Materials (BOM) explosion, inventory ledger auditing, procurement purchase orders, shop-floor production planning, automated finished goods replenishment, invoice billing, and payment balance reconciliation.

---

## 🏛️ System Architecture

OrderCraft follows a robust multi-tier layered architecture designed for high scalability, separation of concerns, and maintainability:

```mermaid
graph TD
    subgraph Client Tier ["React 18 Single Page Application"]
        UI[Tailwind CSS & Lucide Icons]
        Router[React Router v6]
        Context[Auth & Toast Contexts]
        Axios[Axios HTTP Client with JWT Interceptors]
    end

    subgraph Security & Gateways ["Spring Security 6 Tier"]
        CORS[CORS Filter]
        JWT[JwtAuthenticationFilter & JJWT 0.12.6]
        RBAC[Method Security @PreAuthorize]
    end

    subgraph REST Controllers ["Spring Boot REST APIs"]
        C_Auth[AuthController]
        C_Dash[DashboardController]
        C_Cust[CustomerController]
        C_Prod[ProductController]
        C_Raw[RawMaterialController]
        C_Bom[BomController]
        C_Inv[InventoryController]
        C_Ord[CustomerOrderController]
        C_Po[PurchaseOrderController]
        C_Prd[ProductionOrderController]
        C_Inv2[InvoiceController]
        C_Pay[PaymentController]
        C_Rep[ReportController]
        C_User[UserController]
    end

    subgraph Domain Services ["Service Layer (Transactions & Business Rules)"]
        S_Ord[CustomerOrderService]
        S_Bom[BomService & Recipe Engine]
        S_Prd[ProductionOrderService]
        S_Inv[InventoryService & Audit Ledger]
        S_Po[PurchaseOrderService]
        S_Bill[Invoice & Payment Reconciliation]
    end

    subgraph Persistence ["Spring Data JPA & Hibernate"]
        Repos[17 Spring Data JPA Repositories]
        Entities[17 Relational JPA Entities]
    end

    subgraph Database ["Relational Storage"]
        H2[(In-Memory H2 Dev Profile)]
        MySQL[(MySQL 8.0 Production)]
    end

    Client Tier -->|Bearer Token HTTP/JSON| Security & Gateways
    Security & Gateways --> REST Controllers
    REST Controllers --> Domain Services
    Domain Services --> Persistence
    Persistence --> Database
```

---

## 🚀 Key Features & Functional Modules

### 1. Authentication & Role-Based Access Control (RBAC)
- Stateless authentication powered by JSON Web Tokens (**JJWT 0.12.6**).
- Password hashing utilizing industry-standard **BCrypt**.
- Three granular permission roles:
  - `ROLE_ADMIN`: Complete access to all operational modules + exclusive access to User Account Management and Role assignment.
  - `ROLE_MANAGER`: Full operational authority over Customer Orders, Procurement, Production Runs, BOMs, Invoices, and Payments.
  - `ROLE_EMPLOYEE`: Operational visibility to view orders, browse products/inventory, and report production floor updates.
- 1-click demo login buttons directly on the authentication view for rapid testing.

### 2. Executive Manufacturing Dashboard
- **8 Core Real-time KPIs**:
  1. Total Customer Orders
  2. Orders In Production / Pending
  3. Total Revenue Booked
  4. Outstanding Receivables
  5. Critical Low-Stock Raw Material Warnings
  6. Active Production Runs
  7. Finished Products Ready-to-Ship
  8. Total Inventory Valuation
- Interactive SVG Revenue and Cash Collections 30-day time-series graph.
- Real-time customer order status breakdown donut visual.
- Critical raw material inventory warnings with 1-click "Reorder" shortcuts.
- Recent order entries and live cash receipt feeds.

### 3. Customer & Supplier Directory
- Full CRUD management with server-side pagination, searching, and sorting.
- Customer order history modal detailing all historical bookings and payment statuses.
- Supplier catalog with contact details, lead times, tax registration IDs, and payment terms (Net 30, Net 60, COD).

### 4. Product Catalog & Raw Materials
- Manufactured products directory featuring SKU codes, price points, lead times, and active stock levels.
- Raw material sourcing ledger tracking standard unit cost, unit of measurement (kg, meters, pcs, liters, units), safety stock thresholds, and recommended reorder quantities.

### 5. Bill of Materials (BOM) & Costing Engine
- Product recipe creation pairing raw materials with required quantities per finished unit.
- Integrated **Scrap Percentage** factor calculation.
- Live BOM unit cost calculation computed dynamically from component material costs.
- **Batch Material Readiness Simulator**: Enter target batch quantity (e.g. 50 units) to simulate required materials against current available warehouse stock with instant sufficiency flags.

### 6. Real-Time Inventory & Material Audit Ledger
- Real-time stock tracking separating **Raw Materials** and **Finished Goods**.
- Physical stock vs. **Reserved Stock** vs. **Available Stock** accounting.
- Manual Stock Adjustment modal requiring audit justification reason codes (`CYCLE_COUNT`, `SCRAP_DAMAGED`, `MANUAL_CORRECTION`, `RESTOCK`).
- Immutable inventory transaction history ledger recording all movements (`PURCHASE_RECEIPT`, `PRODUCTION_CONSUMPTION`, `PRODUCTION_OUTPUT`, `ORDER_FULFILLMENT`, `MANUAL_ADD`, `MANUAL_DEDUCT`).
- Database constraints preventing negative inventory balances.

### 7. Customer Sales Order Processing
- Multi-product line item order builder with dynamic product lookups, live unit price resolution, and quantity selection.
- Automatic server-side subtotal, customizable sales tax (default 8%), discount deduction, and total calculation.
- Order lifecycle states: `PENDING` $\rightarrow$ `CONFIRMED` $\rightarrow$ `IN_PRODUCTION` $\rightarrow$ `COMPLETED` $\rightarrow$ `CANCELLED`.
- Direct 1-click **Generate Invoice** trigger from confirmed orders.

### 8. Procurement & Purchase Orders
- Sourcing workflow for raw materials with multi-line item purchase orders.
- Order states: `DRAFT` $\rightarrow$ `ORDERED` $\rightarrow$ `RECEIVED` $\rightarrow$ `CANCELLED`.
- **Automated Restocking**: Confirming receipt of a Purchase Order automatically increments physical raw material stock and logs audit receipts.

### 9. Shop-Floor Production Planning
- Create production runs linked to confirmed customer sales orders or build-to-stock replenishment.
- Pre-production material validation verifying that all required BOM components are physically available.
- Start and complete production runs with automated warehouse synchronization:
  - Automatically deducts required BOM raw materials from warehouse stock.
  - Automatically deposits finished product units into warehouse finished goods inventory.
  - Updates linked customer order status to `COMPLETED`.

### 10. Invoicing & Accounts Receivable (AR)
- Automatic invoice generation with standard payment terms and calculated due dates.
- Payment status transitions: `UNPAID` $\rightarrow$ `PARTIALLY_PAID` $\rightarrow$ `PAID` $\rightarrow$ `OVERDUE`.
- Interactive **Printable Invoice Modal** with browser print & save-as-PDF layout matching commercial invoices.

### 11. Payments & Remittance Reconciliation
- Record remittances against invoices supporting multiple payment channels (`BANK_TRANSFER`, `CREDIT_CARD`, `CHECK`, `CASH`, `ACH`).
- Automatically updates invoice `paidAmount`, calculates outstanding balance due, and adjusts payment status.

### 12. Executive Business Intelligence & Reports
- Specialized reporting tabs:
  - **Sales Report**: Volume, average order value, tax, and discount breakdown.
  - **Manufacturing Report**: Production run throughput, completion rate, and timeline logs.
  - **Inventory Valuation**: Capital invested in raw components vs finished goods.
  - **Procurement Report**: Total spend per supplier and purchase order history.
  - **Revenue & Cashflow**: Invoiced billed vs cash collected vs pending receivables.
- Date range filtering with quick buttons (7 days, 30 days, Quarter).
- **Export to CSV**: Instant server-generated CSV downloads for spreadsheet analysis.

### 13. System Security & User Management (Admin Only)
- Secure administrative portal to manage users and access privileges.
- Multi-role assignment (assign one or more roles per user).
- Instant account activation/deactivation switch.
- Password reset capability.

---

## 🛠️ Technology Stack

| Domain | Technology / Library | Version | Description |
| :--- | :--- | :--- | :--- |
| **Backend Core** | Java | 21+ | Modern LTS Java runtime |
| **Framework** | Spring Boot | 3.3.5 | Production-ready micro-framework |
| **Security** | Spring Security | 6.x | Stateless JWT authentication & authorization |
| **JWT** | JJWT | 0.12.6 | JWT creation, signing, and verification |
| **ORM / Data** | Spring Data JPA / Hibernate | 6.5+ | Declarative data persistence & JPQL |
| **Validation** | Jakarta Validation | 3.0+ | Robust request bean constraints |
| **API Docs** | SpringDoc OpenAPI | 2.6.0 | Swagger UI and OpenAPI 3.0 specs |
| **Testing** | JUnit 5 & Mockito | Latest | Unit and integration test suite |
| **Frontend Core** | React | 18.3.1 | Component-based UI framework |
| **Build Tool** | Vite | 5.4.21 | Ultra-fast frontend bundler |
| **Routing** | React Router | 6.28.0 | Declarative client-side routing |
| **Styling** | Tailwind CSS | 3.4.14 | Modern utility-first CSS styling |
| **Icons** | Lucide React | 0.460.0 | Polished industrial icon set |
| **HTTP Client** | Axios | 1.7.7 | Promise-based HTTP client with interceptors |
| **Databases** | H2 (Dev) & MySQL 8.0 (Prod) | 8.0 | Flexible dual-database strategy |

---

## 🗄️ Relational Database Schema

The database consists of **17 normalized tables** with strict primary keys, foreign key constraints, indexes, and precision numeric storage (`DECIMAL(12, 2)` / `DECIMAL(12, 4)`) for all financial and stock arithmetic:

1. `users` & `roles` (connected via `user_roles` Many-to-Many join table)
2. `customers` (account info, tax id, payment terms)
3. `suppliers` (procurement vendors)
4. `raw_materials` (sourcing materials, safety thresholds, reorder points)
5. `products` (finished sellable catalog items)
6. `boms` & `bom_items` (engineering recipes with quantity & scrap factors)
7. `inventory` (warehouse physical, reserved, and available stock)
8. `inventory_transactions` (immutable audit ledger for all movements)
9. `customer_orders` & `order_items` (sales orders and lines)
10. `purchase_orders` & `purchase_order_items` (procurement orders and lines)
11. `production_orders` (shop-floor manufacturing runs)
12. `invoices` (billing records linked to customer orders)
13. `payments` (cash remittances credited against invoices)

---

## ⚡ Quick Start Guide

### Prerequisites
- **Java**: OpenJDK 21 or newer installed (`java -version`)
- **Maven**: Version 3.8+ installed (`mvn -version`)
- **Node.js**: Version 18+ and npm installed (`node -v` & `npm -v`)
- **MySQL (Optional)**: MySQL 8.0+ if running with the production profile.

---

### Step 1: Run the Backend

By default, the backend runs in the **`dev`** profile using an in-memory H2 database in MySQL compatibility mode. **No database configuration or local server installation is required to start running immediately!**

```bash
cd backend
mvn clean spring-boot:run
```

Once started, the backend will be available at:
- **REST API Base URL**: `http://localhost:8080/api`
- **Swagger UI Interactive Documentation**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON Spec**: `http://localhost:8080/v3/api-docs`
- **H2 In-Memory Web Console**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:ordercraft_db`, User: `sa`, Password: *(leave blank)*)

#### (Optional) Running with MySQL 8.0 in Production
If you wish to run against a real MySQL instance:
1. Create a MySQL database:
   ```sql
   CREATE DATABASE ordercraft_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Open `backend/src/main/resources/application-mysql.properties` and verify your username and password:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/ordercraft_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   ```
3. Run with the `mysql` profile:
   ```bash
   mvn clean spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

---

### Step 2: Run the Frontend

In a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

The frontend application will boot up at:
- **Application URL**: `http://localhost:5173`

The Vite development server is configured with a built-in reverse proxy forwarding `/api` requests directly to `http://localhost:8080`.

---

## 🔑 Pre-Seeded User Accounts

The database comes pre-populated with realistic manufacturing data and default user credentials (password is identical for all demo accounts):

| Username | Password | Role | Description |
| :--- | :--- | :--- | :--- |
| **`admin`** | `password123` | `ROLE_ADMIN` | Full system administrator, user management & RBAC |
| **`manager`** | `password123` | `ROLE_MANAGER` | Production manager, order fulfillment & procurement |
| **`employee`** | `password123` | `ROLE_EMPLOYEE` | Floor operator, production reporting & stock views |

> **Pro Tip**: The Login page features **1-Click Quick Login** buttons for instant demo access as Admin, Manager, or Employee!

---

## 🧪 Running Automated Tests

### Backend Unit & Integration Tests
The backend test suite verifies BOM calculations, stock sufficiency checks, order totals, and negative inventory prevention:

```bash
cd backend
mvn test
```

### Packaging Backend Executable JAR
```bash
cd backend
mvn package -DskipTests
# The fat jar is created at: target/ordercraft-backend-1.0.0.jar
```

### Frontend Production Build
To test and bundle the React production assets:

```bash
cd frontend
npm run build
```

---

## 📋 Comprehensive API Route Overview

| Module | Method | Endpoint | Description | Min Role |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/api/auth/login` | Authenticate user & issue JWT | Public |
| **Auth** | `POST` | `/api/auth/register` | Register new user account | Public |
| **Auth** | `GET` | `/api/auth/me` | Fetch authenticated user profile | Authenticated |
| **Dashboard** | `GET` | `/api/dashboard/stats` | Retrieve 8 KPIs, revenue trends, feeds | Authenticated |
| **Customers** | `GET` | `/api/customers` | Search & paginated customer list | Authenticated |
| **Customers** | `POST` | `/api/customers` | Register customer | MANAGER |
| **Customers** | `GET` | `/api/customers/{id}/orders` | Customer historical order ledger | Authenticated |
| **Suppliers** | `GET` | `/api/suppliers` | Paginated vendor directory | Authenticated |
| **Suppliers** | `POST` | `/api/suppliers` | Register new supplier | MANAGER |
| **Products** | `GET` | `/api/products` | Manufactured products catalog | Authenticated |
| **Products** | `POST` | `/api/products` | Create manufactured product | MANAGER |
| **Raw Materials**| `GET` | `/api/raw-materials` | Raw materials sourcing catalog | Authenticated |
| **Raw Materials**| `POST` | `/api/raw-materials` | Add raw material with unit cost | MANAGER |
| **BOM** | `GET` | `/api/boms` | List Bill of Materials recipes | Authenticated |
| **BOM** | `POST` | `/api/boms` | Create BOM recipe with item ratios | MANAGER |
| **BOM** | `GET` | `/api/boms/product/{id}/check-availability` | Check component availability | Authenticated |
| **Inventory** | `GET` | `/api/inventory` | Real-time stock levels | Authenticated |
| **Inventory** | `POST` | `/api/inventory/adjust` | Manual stock addition/deduction | MANAGER |
| **Inventory** | `GET` | `/api/inventory/transactions` | Transaction audit log | Authenticated |
| **Customer Orders** | `GET` | `/api/orders` | Customer sales orders list | Authenticated |
| **Customer Orders** | `POST` | `/api/orders` | Book new multi-item customer order | MANAGER |
| **Customer Orders** | `PATCH` | `/api/orders/{id}/status` | Transition order status | MANAGER |
| **Purchase Orders** | `GET` | `/api/purchase-orders` | Supplier procurement orders | Authenticated |
| **Purchase Orders** | `POST` | `/api/purchase-orders` | Issue purchase order | MANAGER |
| **Purchase Orders** | `PATCH` | `/api/purchase-orders/{id}/receive` | Confirm receipt & restock | MANAGER |
| **Production** | `GET` | `/api/production` | Shop floor production runs | Authenticated |
| **Production** | `POST` | `/api/production` | Schedule new production run | MANAGER |
| **Production** | `PATCH` | `/api/production/{id}/start` | Start manufacturing assembly | EMPLOYEE |
| **Production** | `PATCH` | `/api/production/{id}/complete` | Deduct materials & restock goods | EMPLOYEE |
| **Invoices** | `GET` | `/api/invoices` | Accounts receivable ledger | Authenticated |
| **Invoices** | `POST` | `/api/invoices/generate/{orderId}`| Generate invoice from order | MANAGER |
| **Payments** | `GET` | `/api/payments` | Cash collection records | Authenticated |
| **Payments** | `POST` | `/api/payments` | Record payment against invoice | MANAGER |
| **Reports** | `GET` | `/api/reports/sales` | Sales analytics & metrics | MANAGER |
| **Reports** | `GET` | `/api/reports/inventory` | Capital valuation report | MANAGER |
| **Reports** | `GET` | `/api/reports/export-csv` | Stream downloadable CSV report | MANAGER |
| **Users** | `GET` | `/api/users` | List accounts & security roles | ADMIN |
| **Users** | `PUT` | `/api/users/{id}` | Edit user roles & credentials | ADMIN |
| **Users** | `PATCH` | `/api/users/{id}/toggle-status` | Enable/disable user access | ADMIN |

---

## 🏭 Real Manufacturing Workflow Walkthrough

Here is how OrderCraft models the full manufacturing business lifecycle:

1. **Procurement**:
   - Order raw materials from suppliers using **Purchase Orders**.
   - Upon delivery, click **Receive Items**: the system automatically increases physical raw material inventory and logs a `PURCHASE_RECEIPT` in the audit ledger.
2. **Product & BOM Definition**:
   - Register the finished product in the catalog (e.g. *Industrial Quadcopter Pro*).
   - Define its **Bill of Materials (BOM)** recipe (e.g. 1 Carbon Fiber Frame, 4 Brushless Motors, 1 LiPo Battery Pack, 1 Flight Controller).
   - The BOM engine calculates the exact estimated cost and scrap factor.
3. **Customer Order**:
   - Register customer (e.g. *Apex Logistics Corp*) and place an order for 10 units.
   - The system automatically calculates line subtotals, applies discounts and tax, and places the order in `PENDING` status.
4. **Production Planning & Execution**:
   - Run the **Material Availability Check**: system verifies whether the warehouse has sufficient components for 10 units.
   - Create a **Production Run** linked to the customer order.
   - Click **Start Run**: order moves to `IN_PROGRESS`.
   - Click **Complete Run**: system automatically deducts the raw components from inventory (`PRODUCTION_CONSUMPTION`), stocks 10 finished quadcopters in finished goods inventory (`PRODUCTION_OUTPUT`), and completes the run.
5. **Billing & Collections**:
   - Click **Generate Invoice**: creates an official invoice in `UNPAID` status.
   - Click **Print / PDF**: view and print the commercial invoice.
   - Record payment when the customer remits funds: invoice status automatically transitions to `PAID`.
6. **Executive Analytics**:
   - View updated gross revenue, manufacturing throughput, inventory valuation, and cash collection KPIs across the interactive Dashboard and Executive Reports.

---

---

## 📂 Repository Structure

```text
OrderCraftFinal/
├── backend/                       # Spring Boot 3.3.5 & Java 21+ REST API
│   ├── src/main/java/com/ordercraft/
│   │   ├── config/                # Security, OpenAPI, DataInitializer
│   │   ├── controller/            # 15 REST API Controllers
│   │   ├── dto/                   # 25+ Request/Response DTOs
│   │   ├── entity/                # 17 Relational JPA Entities
│   │   ├── exception/             # Global exception handlers
│   │   ├── repository/            # 17 Spring Data JPA Repositories
│   │   ├── security/              # JWT filter, UserDetails, Token provider
│   │   └── service/               # 15 Business service implementations
│   └── src/main/resources/        # application properties & SQL scripts
├── frontend/                      # React 18, Vite & Tailwind CSS SPA
│   ├── src/api/                   # Axios client with JWT interceptor
│   ├── src/components/            # UI components (Navbar, Sidebar, Modals, etc.)
│   ├── src/context/               # AuthContext, ToastContext
│   └── src/pages/                 # 15 Complete functional application pages
├── database/                      # Standalone MySQL DDL and DML scripts
│   ├── schema.sql                 # 18 relational tables, constraints, foreign keys
│   └── data.sql                   # Realistic sample enterprise records
├── docs/                          # Project Documentation & Academic Artifacts
│   ├── OrderCraft_Project_Synopsis.docx   # Academic Project Synopsis
│   ├── OrderCraft_SRS.docx               # Full Software Requirements Specification
│   └── practice_worksheets/              # 10 Academic Practice Worksheets (PDF)
│       ├── 001_Version_Control_and_Release_Workflow.pdf
│       ├── 002_SDLC_Model_and_Agile_Sprint_Planning.pdf
│       ├── Software_Testing_Debugging_Concepts_using_AI.pdf
│       ├── OOPS_Concepts_using_Java_with_AI.pdf
│       ├── UI_Development_with_Angular_using_AI.pdf
│       ├── Working_with_Spring_Framework_with_AI.pdf
│       ├── Practice_Set_1_Creative_Prompt_Crafting.pdf
│       ├── Practice_Set_2_Prompt_Clarity_and_Rewriting.pdf
│       ├── Practice_Set_3_Bias_Mitigation_and_Ethical_Framing.pdf
│       └── Practice_Set_4_Chatbot_Dialogue_Design.pdf
├── .gitignore
└── README.md
```

## 📜 License
This project is licensed under the MIT License. Developed as a production-grade portfolio application showcasing full-stack enterprise Java Spring Boot and React development.
