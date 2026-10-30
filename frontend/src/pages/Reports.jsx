import React, { useState, useEffect, useCallback } from 'react';
import { 
  BarChart3, 
  Calendar, 
  Download, 
  TrendingUp, 
  Factory, 
  Boxes, 
  ShoppingBag, 
  DollarSign, 
  RefreshCw,
  AlertTriangle,
  CheckCircle2,
  FileSpreadsheet
} from 'lucide-react';
import api from '../api/client';
import MetricCard from '../components/MetricCard';
import Badge from '../components/Badge';
import { useToast } from '../context/ToastContext';

export default function Reports() {
  const [activeTab, setActiveTab] = useState('SALES');
  const [loading, setLoading] = useState(false);
  const [reportData, setReportData] = useState(null);
  const [exporting, setExporting] = useState(false);

  // Default dates: last 30 days
  const today = new Date().toISOString().split('T')[0];
  const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
  const [startDate, setStartDate] = useState(thirtyDaysAgo);
  const [endDate, setEndDate] = useState(today);

  const { showSuccess, showError } = useToast();

  const fetchReport = useCallback(async () => {
    setLoading(true);
    try {
      let endpoint = '';
      const params = {};
      
      if (activeTab !== 'INVENTORY') {
        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;
      }

      switch (activeTab) {
        case 'SALES':
          endpoint = '/reports/sales';
          break;
        case 'PRODUCTION':
          endpoint = '/reports/production';
          break;
        case 'INVENTORY':
          endpoint = '/reports/inventory';
          break;
        case 'PURCHASES':
          endpoint = '/reports/purchases';
          break;
        case 'REVENUE':
          endpoint = '/reports/revenue';
          break;
        default:
          endpoint = '/reports/sales';
      }

      const res = await api.get(endpoint, { params });
      setReportData(res.data);
    } catch (err) {
      console.error('Failed to load report:', err);
      showError('Failed to generate report data.');
    } finally {
      setLoading(false);
    }
  }, [activeTab, startDate, endDate, showError]);

  useEffect(() => {
    fetchReport();
  }, [fetchReport]);

  const handleExportCsv = async () => {
    setExporting(true);
    try {
      const params = { type: activeTab };
      if (activeTab !== 'INVENTORY') {
        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;
      }

      const res = await api.get('/reports/export-csv', {
        params,
        responseType: 'blob'
      });

      const blob = new Blob([res.data], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `ordercraft_${activeTab.toLowerCase()}_report_${startDate || 'all'}_to_${endDate || 'now'}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      showSuccess(`CSV export for ${activeTab} downloaded.`);
    } catch (err) {
      console.error('Export failed:', err);
      showError('Failed to export CSV report.');
    } finally {
      setExporting(false);
    }
  };

  const handleQuickDate = (days) => {
    const end = new Date().toISOString().split('T')[0];
    const start = new Date(Date.now() - days * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    setStartDate(start);
    setEndDate(end);
  };

  const tabs = [
    { id: 'SALES', label: 'Sales Orders', icon: TrendingUp },
    { id: 'PRODUCTION', label: 'Manufacturing & Output', icon: Factory },
    { id: 'INVENTORY', label: 'Inventory Valuation', icon: Boxes },
    { id: 'PURCHASES', label: 'Supplier Purchases', icon: ShoppingBag },
    { id: 'REVENUE', label: 'Revenue & Collections', icon: DollarSign },
  ];

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <BarChart3 className="w-7 h-7 text-blue-600" />
            Executive Reports & Analytics
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Real-time business intelligence for sales, manufacturing, inventory valuation, and cashflow
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={fetchReport}
            disabled={loading}
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 shadow-sm"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-blue-600' : ''}`} />
            Refresh
          </button>
          <button
            onClick={handleExportCsv}
            disabled={exporting || loading}
            className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-sm transition-colors"
          >
            <Download className="w-4 h-4" />
            {exporting ? 'Exporting...' : 'Export CSV'}
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-200 gap-2 overflow-x-auto pb-1">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 whitespace-nowrap transition-colors ${
                isActive
                  ? 'border-blue-600 text-blue-600 bg-blue-50/50 rounded-t-lg'
                  : 'border-transparent text-slate-500 hover:text-slate-800 hover:border-slate-300'
              }`}
            >
              <Icon className="w-4 h-4" />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Filter Bar (Only if not INVENTORY) */}
      {activeTab !== 'INVENTORY' && (
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2 text-sm text-slate-600 font-medium">
              <Calendar className="w-4 h-4 text-slate-400" />
              <span>Date Filter:</span>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="px-3 py-1.5 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
              <span className="text-slate-400 text-sm">to</span>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="px-3 py-1.5 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-500">Quick Range:</span>
            <button
              onClick={() => handleQuickDate(7)}
              className="px-2.5 py-1 text-xs font-medium bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-md"
            >
              7 Days
            </button>
            <button
              onClick={() => handleQuickDate(30)}
              className="px-2.5 py-1 text-xs font-medium bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-md"
            >
              30 Days
            </button>
            <button
              onClick={() => handleQuickDate(90)}
              className="px-2.5 py-1 text-xs font-medium bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-md"
            >
              Quarter
            </button>
          </div>
        </div>
      )}

      {/* Loading state */}
      {loading ? (
        <div className="flex items-center justify-center p-16">
          <RefreshCw className="w-8 h-8 text-blue-600 animate-spin" />
        </div>
      ) : reportData ? (
        <div className="space-y-6">
          {/* TAB 1: SALES REPORT */}
          {activeTab === 'SALES' && (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <MetricCard
                  title="Total Sales Revenue"
                  value={`$${Number(reportData.totalSales || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
                  subtitle="Excluding cancelled orders"
                  icon={DollarSign}
                  color="green"
                />
                <MetricCard
                  title="Customer Orders"
                  value={reportData.orderCount || 0}
                  subtitle="Placed in selected period"
                  icon={ShoppingBag}
                  color="blue"
                />
                <MetricCard
                  title="Average Order Value"
                  value={
                    reportData.orderCount > 0
                      ? `$${(Number(reportData.totalSales || 0) / reportData.orderCount).toFixed(2)}`
                      : '$0.00'
                  }
                  subtitle="Per order booked"
                  icon={TrendingUp}
                  color="indigo"
                />
                <MetricCard
                  title="Reporting Window"
                  value={`${reportData.startDate} ~ ${reportData.endDate}`}
                  subtitle="Active filter bounds"
                  icon={Calendar}
                  color="slate"
                />
              </div>

              <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
                  <h3 className="font-semibold text-slate-900">Sales Transactions</h3>
                  <span className="text-xs text-slate-500 font-medium">
                    Showing {reportData.orders?.length || 0} records
                  </span>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm text-slate-600">
                    <thead className="bg-slate-50 text-slate-700 text-xs uppercase font-semibold border-b border-slate-200">
                      <tr>
                        <th className="px-6 py-3">Order Number</th>
                        <th className="px-6 py-3">Customer</th>
                        <th className="px-6 py-3">Date</th>
                        <th className="px-6 py-3">Status</th>
                        <th className="px-6 py-3 text-right">Subtotal</th>
                        <th className="px-6 py-3 text-right">Tax</th>
                        <th className="px-6 py-3 text-right">Discount</th>
                        <th className="px-6 py-3 text-right font-bold text-slate-900">Total</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {reportData.orders?.map((ord) => (
                        <tr key={ord.id} className="hover:bg-slate-50/75">
                          <td className="px-6 py-3.5 font-medium text-blue-600">{ord.orderNumber}</td>
                          <td className="px-6 py-3.5 font-medium text-slate-900">{ord.customer?.name}</td>
                          <td className="px-6 py-3.5 text-slate-500">{ord.orderDate}</td>
                          <td className="px-6 py-3.5">
                            <Badge status={ord.status} />
                          </td>
                          <td className="px-6 py-3.5 text-right font-mono">${Number(ord.subtotal).toFixed(2)}</td>
                          <td className="px-6 py-3.5 text-right font-mono text-slate-500">${Number(ord.taxAmount).toFixed(2)}</td>
                          <td className="px-6 py-3.5 text-right font-mono text-amber-600">-${Number(ord.discountAmount).toFixed(2)}</td>
                          <td className="px-6 py-3.5 text-right font-mono font-bold text-slate-900">${Number(ord.totalAmount).toFixed(2)}</td>
                        </tr>
                      ))}
                      {(!reportData.orders || reportData.orders.length === 0) && (
                        <tr>
                          <td colSpan="8" className="px-6 py-8 text-center text-slate-400">
                            No sales orders recorded within this date range.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}

          {/* TAB 2: PRODUCTION REPORT */}
          {activeTab === 'PRODUCTION' && (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <MetricCard
                  title="Total Production Runs"
                  value={reportData.totalProductionOrders || 0}
                  subtitle="Scheduled manufacturing runs"
                  icon={Factory}
                  color="blue"
                />
                <MetricCard
                  title="Completed Runs"
                  value={reportData.completedCount || 0}
                  subtitle="Successfully produced and stocked"
                  icon={CheckCircle2}
                  color="green"
                />
                <MetricCard
                  title="In Progress"
                  value={reportData.inProgressCount || 0}
                  subtitle="Currently on assembly floor"
                  icon={RefreshCw}
                  color="amber"
                />
                <MetricCard
                  title="Throughput Rate"
                  value={
                    reportData.totalProductionOrders > 0
                      ? `${Math.round(((reportData.completedCount || 0) / reportData.totalProductionOrders) * 100)}%`
                      : '0%'
                  }
                  subtitle="Completion efficiency"
                  icon={TrendingUp}
                  color="indigo"
                />
              </div>

              <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
                  <h3 className="font-semibold text-slate-900">Manufacturing Runs Breakdown</h3>
                  <span className="text-xs text-slate-500 font-medium">
                    Showing {reportData.productionOrders?.length || 0} orders
                  </span>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm text-slate-600">
                    <thead className="bg-slate-50 text-slate-700 text-xs uppercase font-semibold border-b border-slate-200">
                      <tr>
                        <th className="px-6 py-3">Production #</th>
                        <th className="px-6 py-3">Product</th>
                        <th className="px-6 py-3 text-right">Target Qty</th>
                        <th className="px-6 py-3 text-right">Produced Qty</th>
                        <th className="px-6 py-3">Start Date</th>
                        <th className="px-6 py-3">Completion Date</th>
                        <th className="px-6 py-3">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {reportData.productionOrders?.map((po) => (
                        <tr key={po.id} className="hover:bg-slate-50/75">
                          <td className="px-6 py-3.5 font-medium text-blue-600">{po.productionNumber}</td>
                          <td className="px-6 py-3.5 font-medium text-slate-900">{po.product?.name}</td>
                          <td className="px-6 py-3.5 text-right font-mono font-bold text-slate-800">{po.quantityToProduce}</td>
                          <td className="px-6 py-3.5 text-right font-mono text-emerald-600 font-bold">{po.quantityProduced}</td>
                          <td className="px-6 py-3.5 text-slate-500">{po.startDate || '—'}</td>
                          <td className="px-6 py-3.5 text-slate-500">{po.endDate || '—'}</td>
                          <td className="px-6 py-3.5">
                            <Badge status={po.status} />
                          </td>
                        </tr>
                      ))}
                      {(!reportData.productionOrders || reportData.productionOrders.length === 0) && (
                        <tr>
                          <td colSpan="7" className="px-6 py-8 text-center text-slate-400">
                            No production orders found for this time period.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}

          {/* TAB 3: INVENTORY VALUATION */}
          {activeTab === 'INVENTORY' && (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <MetricCard
                  title="Total Warehouse Valuation"
                  value={`$${Number(reportData.totalValuation || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
                  subtitle="Raw materials + finished products"
                  icon={DollarSign}
                  color="green"
                />
                <MetricCard
                  title="Raw Materials Capital"
                  value={`$${Number(reportData.rawMaterialValuation || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
                  subtitle="Purchased component value"
                  icon={Boxes}
                  color="blue"
                />
                <MetricCard
                  title="Finished Goods Capital"
                  value={`$${Number(reportData.finishedGoodsValuation || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
                  subtitle="Ready-to-ship product assets"
                  icon={Factory}
                  color="indigo"
                />
                <MetricCard
                  title="Low Stock SKUs"
                  value={reportData.lowStockCount || 0}
                  subtitle="Under safety stock threshold"
                  icon={AlertTriangle}
                  color={reportData.lowStockCount > 0 ? 'red' : 'green'}
                />
              </div>

              <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
                  <h3 className="font-semibold text-slate-900">Current Warehouse Stock Ledger</h3>
                  <span className="text-xs text-slate-500 font-medium">
                    {reportData.inventory?.length || 0} inventory items
                  </span>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm text-slate-600">
                    <thead className="bg-slate-50 text-slate-700 text-xs uppercase font-semibold border-b border-slate-200">
                      <tr>
                        <th className="px-6 py-3">Category</th>
                        <th className="px-6 py-3">Code / SKU</th>
                        <th className="px-6 py-3">Item Name</th>
                        <th className="px-6 py-3 text-right">Physical Stock</th>
                        <th className="px-6 py-3 text-right">Reserved</th>
                        <th className="px-6 py-3 text-right">Available</th>
                        <th className="px-6 py-3 text-right">Unit Value</th>
                        <th className="px-6 py-3 text-right font-bold text-slate-900">Total Asset Value</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {reportData.inventory?.map((inv) => {
                        const isRaw = inv.itemType === 'RAW_MATERIAL';
                        const code = isRaw ? inv.rawMaterial?.materialCode : inv.product?.sku;
                        const name = isRaw ? inv.rawMaterial?.name : inv.product?.name;
                        const unitCost = isRaw ? (inv.rawMaterial?.unitCost || 0) : (inv.product?.price || 0);
                        const assetVal = Number(inv.currentStock || 0) * Number(unitCost);

                        return (
                          <tr key={inv.id} className="hover:bg-slate-50/75">
                            <td className="px-6 py-3.5">
                              <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold ${
                                isRaw ? 'bg-amber-50 text-amber-700 border border-amber-200' : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              }`}>
                                {isRaw ? 'Raw Material' : 'Finished Good'}
                              </span>
                            </td>
                            <td className="px-6 py-3.5 font-mono text-xs font-medium text-slate-800">{code}</td>
                            <td className="px-6 py-3.5 font-medium text-slate-900">{name}</td>
                            <td className="px-6 py-3.5 text-right font-mono font-bold text-slate-800">{inv.currentStock}</td>
                            <td className="px-6 py-3.5 text-right font-mono text-amber-600">{inv.reservedStock}</td>
                            <td className="px-6 py-3.5 text-right font-mono text-emerald-600 font-bold">{inv.availableStock}</td>
                            <td className="px-6 py-3.5 text-right font-mono text-slate-500">${Number(unitCost).toFixed(2)}</td>
                            <td className="px-6 py-3.5 text-right font-mono font-bold text-slate-900">${assetVal.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                          </tr>
                        );
                      })}
                      {(!reportData.inventory || reportData.inventory.length === 0) && (
                        <tr>
                          <td colSpan="8" className="px-6 py-8 text-center text-slate-400">
                            No inventory items found.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}

          {/* TAB 4: PURCHASES REPORT */}
          {activeTab === 'PURCHASES' && (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                <MetricCard
                  title="Total Procurement Spend"
                  value={`$${Number(reportData.totalSpend || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
                  subtitle="Received purchase orders"
                  icon={DollarSign}
                  color="green"
                />
                <MetricCard
                  title="Purchase Orders Issued"
                  value={reportData.totalPurchaseOrders || 0}
                  subtitle="In selected time window"
                  icon={ShoppingBag}
                  color="blue"
                />
                <MetricCard
                  title="Average PO Amount"
                  value={
                    reportData.totalPurchaseOrders > 0
                      ? `$${(Number(reportData.totalSpend || 0) / reportData.totalPurchaseOrders).toFixed(2)}`
                      : '$0.00'
                  }
                  subtitle="Per procurement order"
                  icon={TrendingUp}
                  color="indigo"
                />
              </div>

              <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
                  <h3 className="font-semibold text-slate-900">Procurement Orders Log</h3>
                  <span className="text-xs text-slate-500 font-medium">
                    Showing {reportData.purchaseOrders?.length || 0} orders
                  </span>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm text-slate-600">
                    <thead className="bg-slate-50 text-slate-700 text-xs uppercase font-semibold border-b border-slate-200">
                      <tr>
                        <th className="px-6 py-3">PO Number</th>
                        <th className="px-6 py-3">Supplier</th>
                        <th className="px-6 py-3">Order Date</th>
                        <th className="px-6 py-3">Expected Date</th>
                        <th className="px-6 py-3">Status</th>
                        <th className="px-6 py-3 text-right font-bold text-slate-900">Total Amount</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {reportData.purchaseOrders?.map((po) => (
                        <tr key={po.id} className="hover:bg-slate-50/75">
                          <td className="px-6 py-3.5 font-medium text-blue-600">{po.poNumber}</td>
                          <td className="px-6 py-3.5 font-medium text-slate-900">{po.supplier?.companyName}</td>
                          <td className="px-6 py-3.5 text-slate-500">{po.orderDate}</td>
                          <td className="px-6 py-3.5 text-slate-500">{po.expectedDate || '—'}</td>
                          <td className="px-6 py-3.5">
                            <Badge status={po.status} />
                          </td>
                          <td className="px-6 py-3.5 text-right font-mono font-bold text-slate-900">${Number(po.totalAmount).toFixed(2)}</td>
                        </tr>
                      ))}
                      {(!reportData.purchaseOrders || reportData.purchaseOrders.length === 0) && (
                        <tr>
                          <td colSpan="6" className="px-6 py-8 text-center text-slate-400">
                            No purchase orders found for this time period.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}

          {/* TAB 5: REVENUE & COLLECTIONS */}
          {activeTab === 'REVENUE' && (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <MetricCard
                  title="Total Invoiced"
                  value={`$${Number(reportData.totalInvoiced || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
                  subtitle={`${reportData.invoicesCount || 0} invoices generated`}
                  icon={FileSpreadsheet}
                  color="blue"
                />
                <MetricCard
                  title="Total Cash Collected"
                  value={`$${Number(reportData.totalCollected || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
                  subtitle={`${reportData.paymentsCount || 0} customer payments`}
                  icon={DollarSign}
                  color="green"
                />
                <MetricCard
                  title="Outstanding Receivables"
                  value={`$${Number(reportData.outstanding || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
                  subtitle="Unpaid balances due"
                  icon={AlertTriangle}
                  color={Number(reportData.outstanding || 0) > 0 ? 'amber' : 'green'}
                />
                <MetricCard
                  title="Cash Collection Ratio"
                  value={
                    Number(reportData.totalInvoiced || 0) > 0
                      ? `${Math.min(100, Math.round((Number(reportData.totalCollected || 0) / Number(reportData.totalInvoiced || 1)) * 100))}%`
                      : '100%'
                  }
                  subtitle="Collected vs invoiced"
                  icon={TrendingUp}
                  color="indigo"
                />
              </div>

              <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
                <h3 className="font-semibold text-slate-900 mb-4">Financial Flow Summary</h3>
                <div className="space-y-4 max-w-2xl">
                  <div>
                    <div className="flex justify-between text-sm mb-1">
                      <span className="font-medium text-slate-700">Cash Realization Progress</span>
                      <span className="font-bold text-slate-900">
                        ${Number(reportData.totalCollected || 0).toFixed(2)} / ${Number(reportData.totalInvoiced || 0).toFixed(2)}
                      </span>
                    </div>
                    <div className="w-full bg-slate-100 rounded-full h-3 overflow-hidden">
                      <div
                        className="bg-emerald-500 h-3 rounded-full transition-all duration-500"
                        style={{
                          width: `${Math.min(100, (Number(reportData.totalCollected || 0) / (Number(reportData.totalInvoiced || 1) || 1)) * 100)}%`
                        }}
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4 pt-4 border-t border-slate-100 text-sm">
                    <div className="bg-slate-50 p-4 rounded-lg">
                      <span className="text-slate-500 block text-xs">Gross Invoiced Billed</span>
                      <span className="text-lg font-bold text-slate-800 font-mono mt-1 block">
                        ${Number(reportData.totalInvoiced || 0).toFixed(2)}
                      </span>
                    </div>
                    <div className="bg-emerald-50 p-4 rounded-lg">
                      <span className="text-emerald-700 block text-xs">Net Cash Remitted</span>
                      <span className="text-lg font-bold text-emerald-700 font-mono mt-1 block">
                        ${Number(reportData.totalCollected || 0).toFixed(2)}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      ) : null}
    </div>
  );
}
