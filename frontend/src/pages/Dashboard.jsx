import React, { useEffect, useState } from 'react';
import {
  ShoppingCart,
  Clock,
  CheckCircle2,
  DollarSign,
  AlertTriangle,
  Factory,
  Boxes,
  Users,
  TrendingUp,
  CreditCard,
  ArrowRight,
  RefreshCw,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { MetricCard } from '../components/MetricCard';
import { Badge } from '../components/Badge';
import { useToast } from '../context/ToastContext';

export const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const toast = useToast();

  const fetchStats = async () => {
    setLoading(true);
    try {
      const res = await api.get('/dashboard/stats');
      setStats(res.data);
    } catch (err) {
      toast.error('Failed to load dashboard metrics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  if (loading && !stats) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="w-10 h-10 border-4 border-brand-500/20 border-t-brand-500 rounded-full animate-spin" />
      </div>
    );
  }

  // Calculate SVG polyline points for the revenue chart
  const chartData = stats?.revenueChart || [];
  const maxVal = Math.max(...chartData.map((d) => Math.max(Number(d.revenue || 0), Number(d.payments || 0))), 1000);

  const getPoints = (key) => {
    if (!chartData.length) return '';
    const width = 600;
    const height = 180;
    const step = width / (chartData.length - 1 || 1);
    return chartData
      .map((d, idx) => {
        const x = idx * step;
        const val = Number(d[key] || 0);
        const y = height - (val / maxVal) * (height - 30) - 15;
        return `${x},${y}`;
      })
      .join(' ');
  };

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Top action bar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-100 tracking-tight">Plant & Operations Pulse</h1>
          <p className="text-xs text-slate-400 mt-1">Real-time telemetry across orders, BOM assembly, shop floor & finance</p>
        </div>
        <button
          onClick={fetchStats}
          className="px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 hover:bg-slate-800 text-xs font-semibold text-slate-300 flex items-center gap-2 transition"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh Metrics
        </button>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          title="Total Orders"
          value={stats?.totalOrders ?? 0}
          subtitle={`${stats?.pendingOrders ?? 0} pending processing`}
          icon={ShoppingCart}
          color="brand"
        />
        <MetricCard
          title="Total Revenue"
          value={`$${Number(stats?.totalRevenue || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}`}
          subtitle={`$${Number(stats?.totalPaidRevenue || 0).toLocaleString('en-US')} collected`}
          icon={DollarSign}
          color="emerald"
        />
        <MetricCard
          title="Pending Payments"
          value={`$${Number(stats?.pendingPayments || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}`}
          subtitle="Awaiting customer remittance"
          icon={CreditCard}
          color="amber"
        />
        <MetricCard
          title="Low Stock Alerts"
          value={stats?.lowStockMaterialsCount ?? 0}
          subtitle={stats?.lowStockMaterialsCount > 0 ? 'Below safety thresholds' : 'All materials optimal'}
          icon={AlertTriangle}
          color={stats?.lowStockMaterialsCount > 0 ? 'rose' : 'emerald'}
        />
      </div>

      {/* Secondary KPI Row */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center gap-3">
          <Factory className="w-6 h-6 text-indigo-400" />
          <div>
            <p className="text-xs text-slate-400">Active Production</p>
            <p className="text-lg font-bold text-white">{stats?.activeProductionCount ?? 0} runs</p>
          </div>
        </div>
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center gap-3">
          <Boxes className="w-6 h-6 text-cyan-400" />
          <div>
            <p className="text-xs text-slate-400">Raw Mat Valuation</p>
            <p className="text-lg font-bold text-white">${Number(stats?.rawMaterialInventoryValuation || 0).toLocaleString()}</p>
          </div>
        </div>
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center gap-3">
          <TrendingUp className="w-6 h-6 text-emerald-400" />
          <div>
            <p className="text-xs text-slate-400">Finished Goods Val.</p>
            <p className="text-lg font-bold text-white">${Number(stats?.finishedGoodsInventoryValuation || 0).toLocaleString()}</p>
          </div>
        </div>
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center gap-3">
          <Users className="w-6 h-6 text-brand-400" />
          <div>
            <p className="text-xs text-slate-400">Active Clients</p>
            <p className="text-lg font-bold text-white">{stats?.totalCustomersCount ?? 0}</p>
          </div>
        </div>
      </div>

      {/* Charts Section: Revenue Trend & Order Status Distribution */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Revenue vs Collections Chart */}
        <div className="lg:col-span-2 rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-sm font-bold text-slate-100">Revenue & Cash Collections (Past 7 Days)</h3>
              <p className="text-xs text-slate-400 mt-0.5">Daily billed orders vs recorded payment receipts</p>
            </div>
            <div className="flex items-center gap-4 text-xs">
              <span className="flex items-center gap-1.5 text-brand-400 font-semibold">
                <span className="w-3 h-1 bg-brand-500 rounded"></span> Orders Revenue
              </span>
              <span className="flex items-center gap-1.5 text-emerald-400 font-semibold">
                <span className="w-3 h-1 bg-emerald-500 rounded"></span> Cash Collected
              </span>
            </div>
          </div>

          {/* SVG Chart Graphic */}
          <div className="w-full h-52 relative">
            <svg viewBox="0 0 600 200" className="w-full h-full overflow-visible">
              {/* Grid lines */}
              <line x1="0" y1="20" x2="600" y2="20" stroke="#334155" strokeDasharray="3 3" strokeOpacity="0.4" />
              <line x1="0" y1="75" x2="600" y2="75" stroke="#334155" strokeDasharray="3 3" strokeOpacity="0.4" />
              <line x1="0" y1="130" x2="600" y2="130" stroke="#334155" strokeDasharray="3 3" strokeOpacity="0.4" />
              <line x1="0" y1="180" x2="600" y2="180" stroke="#334155" strokeOpacity="0.8" />

              {/* Polylines */}
              <polyline
                fill="none"
                stroke="#0e8ce9"
                strokeWidth="3"
                strokeLinecap="round"
                strokeLinejoin="round"
                points={getPoints('revenue')}
              />
              <polyline
                fill="none"
                stroke="#10b981"
                strokeWidth="3"
                strokeLinecap="round"
                strokeLinejoin="round"
                points={getPoints('payments')}
              />

              {/* Data points */}
              {chartData.map((d, idx) => {
                const step = 600 / (chartData.length - 1 || 1);
                const x = idx * step;
                const revY = 180 - (Number(d.revenue || 0) / maxVal) * 150 - 15;
                const payY = 180 - (Number(d.payments || 0) / maxVal) * 150 - 15;
                return (
                  <g key={idx}>
                    <circle cx={x} cy={revY} r="4" fill="#0e8ce9" className="hover:r-6 transition-all" />
                    <circle cx={x} cy={payY} r="4" fill="#10b981" className="hover:r-6 transition-all" />
                  </g>
                );
              })}
            </svg>

            {/* X-axis labels */}
            <div className="flex justify-between text-[10px] text-slate-400 mt-2 px-1 font-mono">
              {chartData.map((d, i) => (
                <span key={i}>{d.date}</span>
              ))}
            </div>
          </div>
        </div>

        {/* Order Status Breakdown */}
        <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl flex flex-col justify-between">
          <div>
            <h3 className="text-sm font-bold text-slate-100">Order Status Lifecycle</h3>
            <p className="text-xs text-slate-400 mt-0.5">Distribution across manufacturing pipeline</p>

            <div className="space-y-3.5 mt-5">
              {stats?.orderStatusDistribution &&
                Object.entries(stats.orderStatusDistribution).map(([status, count]) => {
                  const total = stats.totalOrders || 1;
                  const pct = Math.round((count / total) * 100);
                  return (
                    <div key={status} className="space-y-1">
                      <div className="flex justify-between text-xs font-semibold">
                        <span className="text-slate-300">{status.replace('_', ' ')}</span>
                        <span className="text-slate-400 font-mono">
                          {count} ({pct}%)
                        </span>
                      </div>
                      <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
                        <div
                          className={`h-full rounded-full ${
                            status === 'COMPLETED'
                              ? 'bg-emerald-500'
                              : status === 'IN_PRODUCTION'
                              ? 'bg-indigo-500'
                              : status === 'CONFIRMED'
                              ? 'bg-sky-500'
                              : status === 'CANCELLED'
                              ? 'bg-rose-500'
                              : 'bg-amber-500'
                          }`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  );
                })}
            </div>
          </div>

          <div className="pt-4 border-t border-slate-800 mt-4">
            <Link
              to="/orders"
              className="text-xs font-semibold text-brand-400 hover:text-brand-300 flex items-center justify-between"
            >
              <span>View Full Order Pipeline</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        </div>
      </div>

      {/* Bottom Grid: Low Stock Warnings & Recent Feeds */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Low Stock Material Alerts */}
        <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-400" />
              Critical Stock Alerts
            </h3>
            <Link to="/inventory" className="text-xs text-brand-400 hover:text-brand-300 font-medium">
              Manage
            </Link>
          </div>

          {stats?.lowStockAlerts?.length === 0 ? (
            <div className="text-center py-8 text-xs text-slate-400">
              <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto mb-2 opacity-60" />
              All materials currently above minimum reorder points.
            </div>
          ) : (
            <div className="space-y-3">
              {stats?.lowStockAlerts?.map((item) => (
                <div
                  key={item.id}
                  className="p-3 rounded-xl bg-slate-950/60 border border-rose-500/20 flex items-center justify-between text-xs"
                >
                  <div>
                    <p className="font-semibold text-slate-200">{item.name}</p>
                    <p className="text-[11px] text-slate-400 font-mono">{item.materialCode}</p>
                  </div>
                  <div className="text-right">
                    <p className="font-bold text-rose-400 font-mono">
                      {item.currentStock} {item.unitOfMeasure}
                    </p>
                    <p className="text-[10px] text-slate-400">Min: {item.minimumStockLevel}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Recent Orders */}
        <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-bold text-slate-100">Recent Customer Orders</h3>
            <Link to="/orders" className="text-xs text-brand-400 hover:text-brand-300 font-medium">
              View All
            </Link>
          </div>
          <div className="space-y-3">
            {stats?.recentOrders?.map((ord) => (
              <div
                key={ord.id}
                className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/80 flex items-center justify-between text-xs"
              >
                <div>
                  <p className="font-bold text-slate-200 font-mono">{ord.orderNumber}</p>
                  <p className="text-[11px] text-slate-400">{ord.customerName}</p>
                </div>
                <div className="text-right space-y-1">
                  <p className="font-bold text-slate-100 font-mono">${Number(ord.totalAmount).toFixed(2)}</p>
                  <Badge status={ord.status} />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Recent Payment Receipts */}
        <div className="rounded-2xl bg-slate-900/80 border border-slate-800 p-6 shadow-xl">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-bold text-slate-100">Recent Payment Receipts</h3>
            <Link to="/payments" className="text-xs text-brand-400 hover:text-brand-300 font-medium">
              View All
            </Link>
          </div>
          <div className="space-y-3">
            {stats?.recentPayments?.map((p) => (
              <div
                key={p.id}
                className="p-3 rounded-xl bg-slate-950/60 border border-slate-800/80 flex items-center justify-between text-xs"
              >
                <div>
                  <p className="font-bold text-slate-200 font-mono">{p.paymentReference}</p>
                  <p className="text-[11px] text-slate-400">Inv #{p.invoiceNumber}</p>
                </div>
                <div className="text-right">
                  <p className="font-bold text-emerald-400 font-mono">+${Number(p.amount).toFixed(2)}</p>
                  <p className="text-[10px] text-slate-400">{p.paymentDate}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
