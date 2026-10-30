import React, { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Navbar } from './Navbar';

const TITLE_MAP = {
  '/dashboard': 'Executive Manufacturing Dashboard',
  '/customers': 'Customer Directory & Accounts',
  '/suppliers': 'Supplier & Vendor Directory',
  '/products': 'Manufactured Products Catalog',
  '/raw-materials': 'Raw Material Inventory & Sourcing',
  '/boms': 'Bill of Materials (BOM) Recipes',
  '/inventory': 'Real-Time Inventory & Material Audit',
  '/orders': 'Customer Sales Orders',
  '/purchase-orders': 'Purchase Orders (Procurement)',
  '/production': 'Shop Floor Production Scheduling',
  '/invoices': 'Invoices & Accounts Receivable',
  '/payments': 'Payments & Receipts',
  '/reports': 'Manufacturing Intelligence & Reports',
  '/users': 'Security & User Management',
};

export const Layout = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();

  const title = TITLE_MAP[location.pathname] || 'OrderCraft OMS';

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col">
      <Sidebar isOpen={sidebarOpen} setIsOpen={setSidebarOpen} />

      <div className="lg:pl-64 flex flex-col flex-1">
        <Navbar onMenuClick={() => setSidebarOpen(true)} title={title} />
        <main className="flex-1 p-4 sm:p-8 max-w-7xl w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
