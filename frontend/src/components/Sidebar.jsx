import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  Truck,
  Package,
  Layers,
  FileCode2,
  Boxes,
  ShoppingCart,
  ShoppingBag,
  Factory,
  Receipt,
  CreditCard,
  BarChart3,
  ShieldAlert,
  LogOut,
  ChevronRight,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const Sidebar = ({ isOpen, setIsOpen }) => {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  const navGroups = [
    {
      title: 'Operations & Materials',
      items: [
        { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
        { path: '/customers', label: 'Customers', icon: Users },
        { path: '/suppliers', label: 'Suppliers', icon: Truck },
        { path: '/products', label: 'Products', icon: Package },
        { path: '/raw-materials', label: 'Raw Materials', icon: Layers },
        { path: '/boms', label: 'Bill of Materials', icon: FileCode2 },
        { path: '/inventory', label: 'Inventory & Stock', icon: Boxes },
      ],
    },
    {
      title: 'Order Fulfillment',
      items: [
        { path: '/orders', label: 'Customer Orders', icon: ShoppingCart },
        { path: '/purchase-orders', label: 'Purchase Orders', icon: ShoppingBag },
        { path: '/production', label: 'Production Run', icon: Factory },
      ],
    },
    {
      title: 'Financial & Analytics',
      items: [
        { path: '/invoices', label: 'Invoices', icon: Receipt },
        { path: '/payments', label: 'Payments', icon: CreditCard },
        { path: '/reports', label: 'Reports & Analytics', icon: BarChart3 },
      ],
    },
  ];

  if (isAdmin) {
    navGroups.push({
      title: 'System Security',
      items: [{ path: '/users', label: 'User Management', icon: ShieldAlert }],
    });
  }

  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-950/80 backdrop-blur-sm lg:hidden"
          onClick={() => setIsOpen(false)}
        />
      )}

      {/* Sidebar shell */}
      <aside
        className={`fixed top-0 bottom-0 left-0 z-40 w-64 bg-slate-950 border-r border-slate-800/80 flex flex-col transition-transform duration-300 ease-in-out lg:translate-x-0 ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Brand header */}
        <div className="h-16 flex items-center gap-3 px-6 border-b border-slate-800/80 bg-slate-950/50">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-500 flex items-center justify-center text-white shadow-lg shadow-brand-500/25">
            <Factory className="w-5 h-5" />
          </div>
          <div>
            <div className="font-extrabold text-slate-100 tracking-tight flex items-center gap-1.5">
              OrderCraft
              <span className="text-[10px] font-bold px-1.5 py-0.5 rounded-full bg-brand-500/20 text-brand-300 border border-brand-500/30">
                OMS
              </span>
            </div>
            <p className="text-[11px] text-slate-400 font-medium tracking-wide">Manufacturing Suite</p>
          </div>
        </div>

        {/* Scrollable nav list */}
        <div className="flex-1 overflow-y-auto px-3 py-4 space-y-6">
          {navGroups.map((group, gIdx) => (
            <div key={gIdx} className="space-y-1">
              <h5 className="px-3 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                {group.title}
              </h5>
              <div className="space-y-0.5 mt-1">
                {group.items.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.path}
                      to={item.path}
                      onClick={() => setIsOpen(false)}
                      className={({ isActive }) =>
                        `flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-semibold transition-all duration-200 group ${
                          isActive
                            ? 'bg-brand-600/15 text-brand-400 border border-brand-500/30 shadow-sm'
                            : 'text-slate-400 hover:text-slate-100 hover:bg-slate-900 border border-transparent'
                        }`
                      }
                    >
                      <Icon className="w-4 h-4 shrink-0 transition-transform group-hover:scale-110" />
                      <span className="flex-1 truncate">{item.label}</span>
                      <ChevronRight className="w-3.5 h-3.5 opacity-0 group-hover:opacity-100 transition-opacity text-slate-400" />
                    </NavLink>
                  );
                })}
              </div>
            </div>
          ))}
        </div>

        {/* User profile card & logout */}
        <div className="p-3 border-t border-slate-800/80 bg-slate-900/40">
          <div className="flex items-center justify-between p-2 rounded-xl bg-slate-900/80 border border-slate-800">
            <div className="flex items-center gap-2.5 overflow-hidden">
              <div className="w-8 h-8 rounded-lg bg-slate-800 border border-slate-700 flex items-center justify-center font-bold text-xs text-brand-400 shrink-0">
                {user?.fullName?.charAt(0) || 'U'}
              </div>
              <div className="overflow-hidden">
                <p className="text-xs font-semibold text-slate-200 truncate">{user?.fullName}</p>
                <p className="text-[10px] text-slate-400 truncate">
                  {user?.roles?.[0]?.replace('ROLE_', '') || 'EMPLOYEE'}
                </p>
              </div>
            </div>
            <button
              onClick={logout}
              title="Sign Out"
              className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </aside>
    </>
  );
};

export default Sidebar;
