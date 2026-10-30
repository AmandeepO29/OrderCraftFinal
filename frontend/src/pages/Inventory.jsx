import React, { useEffect, useState } from 'react';
import { Boxes, Plus, Minus, Search, AlertTriangle, History, ArrowDownRight, ArrowUpRight, RotateCcw, Filter, CheckCircle2 } from 'lucide-react';
import api from '../api/client';
import { Modal } from '../components/Modal';
import { Pagination } from '../components/Pagination';
import { Badge } from '../components/Badge';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export const Inventory = () => {
  const [activeTab, setActiveTab] = useState('ALL'); // 'ALL', 'RAW_MATERIAL', 'FINISHED_PRODUCT', 'TRANSACTIONS'
  const [inventoryList, setInventoryList] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);

  // Manual adjustment modal
  const [adjustOpen, setAdjustOpen] = useState(false);
  const [adjustingItem, setAdjustingItem] = useState(null);
  const [adjustType, setAdjustType] = useState('ADD');
  const [adjustQty, setAdjustQty] = useState('');
  const [adjustReason, setAdjustReason] = useState('');

  const { isManager } = useAuth();
  const toast = useToast();

  const fetchInventory = async (page = 0, query = searchQuery, tab = activeTab) => {
    setLoading(true);
    try {
      if (tab === 'TRANSACTIONS') {
        const res = await api.get('/inventory/transactions', {
          params: { page, size: 15 },
        });
        setTransactions(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalItems(res.data.totalElements);
      } else {
        const itemType = tab === 'ALL' ? undefined : tab;
        const res = await api.get('/inventory', {
          params: { page, size: 10, query: query || undefined, itemType },
        });
        setInventoryList(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalItems(res.data.totalElements);
      }
      setCurrentPage(page);
    } catch (err) {
      toast.error('Failed to load inventory data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInventory(0, searchQuery, activeTab);
  }, [searchQuery, activeTab]);

  const handleOpenAdjust = (item, type = 'ADD') => {
    setAdjustingItem(item);
    setAdjustType(type);
    setAdjustQty('');
    setAdjustReason('');
    setAdjustOpen(true);
  };

  const handleAdjustSubmit = async (e) => {
    e.preventDefault();
    if (!adjustQty || Number(adjustQty) <= 0) {
      toast.error('Please enter a valid positive quantity.');
      return;
    }

    try {
      await api.post('/inventory/adjust', {
        inventoryId: adjustingItem.id,
        quantity: parseFloat(adjustQty),
        type: adjustType,
        notes: adjustReason,
      });
      toast.success(`Inventory stock successfully ${adjustType === 'ADD' ? 'increased' : 'reduced'}!`);
      setAdjustOpen(false);
      fetchInventory(currentPage, searchQuery, activeTab);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Inventory adjustment failed.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-100">Inventory & Physical Stock Control</h1>
          <p className="text-xs text-slate-400 mt-0.5">Real-time balances across raw materials, finished units, and audit transaction log</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-2">
        <button
          onClick={() => setActiveTab('ALL')}
          className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
            activeTab === 'ALL'
              ? 'bg-brand-600 text-white shadow-md'
              : 'text-slate-400 hover:text-white hover:bg-slate-900'
          }`}
        >
          All Stock
        </button>
        <button
          onClick={() => setActiveTab('RAW_MATERIAL')}
          className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
            activeTab === 'RAW_MATERIAL'
              ? 'bg-brand-600 text-white shadow-md'
              : 'text-slate-400 hover:text-white hover:bg-slate-900'
          }`}
        >
          Raw Materials
        </button>
        <button
          onClick={() => setActiveTab('FINISHED_PRODUCT')}
          className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
            activeTab === 'FINISHED_PRODUCT'
              ? 'bg-brand-600 text-white shadow-md'
              : 'text-slate-400 hover:text-white hover:bg-slate-900'
          }`}
        >
          Finished Goods
        </button>
        <button
          onClick={() => setActiveTab('TRANSACTIONS')}
          className={`px-3.5 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 transition ${
            activeTab === 'TRANSACTIONS'
              ? 'bg-indigo-600 text-white shadow-md'
              : 'text-slate-400 hover:text-white hover:bg-slate-900'
          }`}
        >
          <History className="w-3.5 h-3.5" />
          Audit Trail Log
        </button>
      </div>

      {/* Search (when not in transactions view) */}
      {activeTab !== 'TRANSACTIONS' && (
        <div className="flex items-center gap-4 bg-slate-900/60 p-3 rounded-2xl border border-slate-800">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />
            <input
              type="text"
              placeholder="Search inventory by material name, product SKU, or material code..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-brand-500 transition"
            />
          </div>
        </div>
      )}

      {/* Main Table Content */}
      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          {activeTab === 'TRANSACTIONS' ? (
            /* Audit Trail Table */
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-950/50 text-slate-400 font-semibold uppercase tracking-wider">
                  <th className="py-3 px-4">Timestamp</th>
                  <th className="py-3 px-4">Item Name</th>
                  <th className="py-3 px-4">Category</th>
                  <th className="py-3 px-4">Transaction Type</th>
                  <th className="py-3 px-4 text-right">Qty Change</th>
                  <th className="py-3 px-4 text-right">Balance After</th>
                  <th className="py-3 px-4">Reference</th>
                  <th className="py-3 px-4">Operator</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-slate-300">
                {loading && transactions.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="text-center py-12 text-slate-400">
                      <div className="w-6 h-6 border-2 border-brand-500/20 border-t-brand-500 rounded-full animate-spin mx-auto mb-2" />
                      Loading audit logs...
                    </td>
                  </tr>
                ) : transactions.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="text-center py-12 text-slate-400">
                      No inventory transactions recorded yet.
                    </td>
                  </tr>
                ) : (
                  transactions.map((tx) => (
                    <tr key={tx.id} className="hover:bg-slate-850/50 transition">
                      <td className="py-3.5 px-4 font-mono text-slate-400 text-[11px] whitespace-nowrap">
                        {tx.createdAt ? new Date(tx.createdAt).toLocaleString() : '—'}
                      </td>
                      <td className="py-3.5 px-4 font-semibold text-slate-100">{tx.itemName}</td>
                      <td className="py-3.5 px-4 text-[11px] text-slate-400">
                        {tx.itemType === 'RAW_MATERIAL' ? 'Raw Material' : 'Finished Good'}
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-800 border border-slate-700 text-slate-300">
                          {tx.transactionType}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono font-bold">
                        <span className={tx.quantity > 0 ? 'text-emerald-400' : 'text-rose-400'}>
                          {tx.quantity > 0 ? `+${tx.quantity}` : tx.quantity}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono font-semibold text-slate-200">
                        {tx.balanceAfter}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-[11px] text-slate-400">
                        {tx.referenceId || 'N/A'}
                      </td>
                      <td className="py-3.5 px-4 text-slate-400">{tx.createdByUsername}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          ) : (
            /* Inventory Balance Table */
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-950/50 text-slate-400 font-semibold uppercase tracking-wider">
                  <th className="py-3 px-4">Type</th>
                  <th className="py-3 px-4">Item Code</th>
                  <th className="py-3 px-4">Item Name</th>
                  <th className="py-3 px-4">Warehouse Location</th>
                  <th className="py-3 px-4 text-center">Current Stock</th>
                  <th className="py-3 px-4 text-center">Available Stock</th>
                  <th className="py-3 px-4 text-center">Status</th>
                  <th className="py-3 px-4 text-right">Adjust Stock</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-slate-300">
                {loading && inventoryList.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="text-center py-12 text-slate-400">
                      <div className="w-6 h-6 border-2 border-brand-500/20 border-t-brand-500 rounded-full animate-spin mx-auto mb-2" />
                      Loading inventory records...
                    </td>
                  </tr>
                ) : inventoryList.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="text-center py-12 text-slate-400">
                      No inventory records found.
                    </td>
                  </tr>
                ) : (
                  inventoryList.map((item) => {
                    const isRaw = item.itemType === 'RAW_MATERIAL';
                    const code = isRaw ? item.materialCode : item.productSku;
                    const name = isRaw ? item.materialName : item.productName;
                    const uom = isRaw ? item.unitOfMeasure : 'units';

                    return (
                      <tr key={item.id} className="hover:bg-slate-850/50 transition">
                        <td className="py-3.5 px-4">
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                              isRaw
                                ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                                : 'bg-brand-500/10 text-brand-400 border-brand-500/20'
                            }`}
                          >
                            {isRaw ? 'RAW MATERIAL' : 'FINISHED GOOD'}
                          </span>
                        </td>
                        <td className="py-3.5 px-4 font-mono font-bold text-slate-200">{code}</td>
                        <td className="py-3.5 px-4 font-semibold text-slate-100">{name}</td>
                        <td className="py-3.5 px-4 text-slate-400">{item.location || 'Warehouse Main'}</td>
                        <td className="py-3.5 px-4 text-center font-mono font-bold text-slate-100">
                          {item.currentStock} {uom}
                        </td>
                        <td className="py-3.5 px-4 text-center font-mono font-bold text-emerald-400">
                          {item.availableStock} {uom}
                        </td>
                        <td className="py-3.5 px-4 text-center">
                          {item.lowStockAlert ? (
                            <span className="inline-flex items-center gap-1 text-[11px] font-bold text-rose-400 bg-rose-500/10 border border-rose-500/20 px-2 py-0.5 rounded-full animate-pulse">
                              <AlertTriangle className="w-3 h-3" /> Low Stock
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-full">
                              <CheckCircle2 className="w-3 h-3" /> Optimal
                            </span>
                          )}
                        </td>
                        <td className="py-3.5 px-4 text-right space-x-1.5">
                          {isManager && (
                            <>
                              <button
                                onClick={() => handleOpenAdjust(item, 'ADD')}
                                title="Add Stock"
                                className="px-2 py-1 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 text-xs font-semibold inline-flex items-center gap-0.5 transition"
                              >
                                <Plus className="w-3.5 h-3.5" /> In
                              </button>
                              <button
                                onClick={() => handleOpenAdjust(item, 'DEDUCT')}
                                title="Deduct Stock"
                                className="px-2 py-1 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-300 border border-rose-500/30 text-xs font-semibold inline-flex items-center gap-0.5 transition"
                              >
                                <Minus className="w-3.5 h-3.5" /> Out
                              </button>
                            </>
                          )}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          )}
        </div>

        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          totalItems={totalItems}
          pageSize={activeTab === 'TRANSACTIONS' ? 15 : 10}
          onPageChange={(page) => fetchInventory(page, searchQuery, activeTab)}
        />
      </div>

      {/* Stock Adjustment Modal */}
      <Modal
        isOpen={adjustOpen}
        onClose={() => setAdjustOpen(false)}
        title={`Adjust Stock: ${adjustingItem?.rawMaterialId ? adjustingItem?.materialName : adjustingItem?.productName || ''}`}
        maxWidth="max-w-md"
      >
        <form onSubmit={handleAdjustSubmit} className="space-y-4">
          <div className="flex rounded-xl bg-slate-950 p-1 border border-slate-800">
            <button
              type="button"
              onClick={() => setAdjustType('ADD')}
              className={`flex-1 py-1.5 text-xs font-bold rounded-lg flex items-center justify-center gap-1 transition ${
                adjustType === 'ADD'
                  ? 'bg-emerald-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Plus className="w-3.5 h-3.5" /> Stock Addition (IN)
            </button>
            <button
              type="button"
              onClick={() => setAdjustType('DEDUCT')}
              className={`flex-1 py-1.5 text-xs font-bold rounded-lg flex items-center justify-center gap-1 transition ${
                adjustType === 'DEDUCT'
                  ? 'bg-rose-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Minus className="w-3.5 h-3.5" /> Stock Deduction (OUT)
            </button>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">
              Quantity to {adjustType === 'ADD' ? 'Add' : 'Deduct'} *
            </label>
            <input
              type="number"
              step="0.01"
              required
              min="0.01"
              placeholder="0.00"
              value={adjustQty}
              onChange={(e) => setAdjustQty(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono text-lg font-bold focus:outline-none focus:border-brand-500 transition"
            />
            <p className="text-[11px] text-slate-400 mt-1">
              Current Available Stock: {adjustingItem?.availableStock}
            </p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">
              Adjustment Audit Justification / Reason *
            </label>
            <textarea
              required
              rows="3"
              placeholder="e.g. Physical cycle count variance, damaged in transit, QA testing sample"
              value={adjustReason}
              onChange={(e) => setAdjustReason(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={() => setAdjustOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-300 hover:text-white bg-slate-800 rounded-xl transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              className={`px-4 py-2 text-xs font-semibold text-white rounded-xl shadow-lg transition ${
                adjustType === 'ADD'
                  ? 'bg-emerald-600 hover:bg-emerald-500 shadow-emerald-600/20'
                  : 'bg-rose-600 hover:bg-rose-500 shadow-rose-600/20'
              }`}
            >
              Confirm Stock Adjustment
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
