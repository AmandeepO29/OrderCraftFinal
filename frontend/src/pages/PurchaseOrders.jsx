import React, { useEffect, useState } from 'react';
import { ShoppingBag, Plus, Search, CheckCircle2, Send, PackageCheck, Eye, Trash2, Truck } from 'lucide-react';
import api from '../api/client';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Pagination } from '../components/Pagination';
import { Badge } from '../components/Badge';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export const PurchaseOrders = () => {
  const [purchaseOrders, setPurchaseOrders] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [rawMaterials, setRawMaterials] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);

  // Form modal
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    supplierId: '',
    orderDate: new Date().toISOString().split('T')[0],
    expectedDate: '',
    notes: '',
    items: [],
  });

  // Receive materials modal
  const [receiveModalOpen, setReceiveModalOpen] = useState(false);
  const [receivingPo, setReceivingPo] = useState(null);
  const [receiveNotes, setReceiveNotes] = useState('');

  // View details modal
  const [viewModalOpen, setViewModalOpen] = useState(false);
  const [viewingPo, setViewingPo] = useState(null);

  // Cancel dialog
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelingId, setCancelingId] = useState(null);

  const { isManager, isAdmin } = useAuth();
  const toast = useToast();

  const fetchPurchaseOrders = async (page = 0, query = searchQuery, status = selectedStatus) => {
    setLoading(true);
    try {
      const res = await api.get('/purchase-orders', {
        params: {
          page,
          size: 10,
          query: query || undefined,
          status: status || undefined,
        },
      });
      setPurchaseOrders(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalItems(res.data.totalElements);
      setCurrentPage(page);
    } catch (err) {
      toast.error('Failed to load purchase orders.');
    } finally {
      setLoading(false);
    }
  };

  const fetchMeta = async () => {
    try {
      const [supsRes, matsRes] = await Promise.all([
        api.get('/suppliers', { params: { page: 0, size: 100 } }),
        api.get('/raw-materials/active'),
      ]);
      setSuppliers(supsRes.data.content);
      setRawMaterials(matsRes.data);
    } catch (err) {}
  };

  useEffect(() => {
    fetchPurchaseOrders(0, searchQuery, selectedStatus);
    fetchMeta();
  }, [searchQuery, selectedStatus]);

  const handleOpenCreate = () => {
    setFormData({
      supplierId: suppliers[0]?.id || '',
      orderDate: new Date().toISOString().split('T')[0],
      expectedDate: '',
      notes: '',
      items: [
        {
          rawMaterialId: rawMaterials[0]?.id || '',
          quantityOrdered: 100,
          unitPrice: rawMaterials[0]?.unitCost || 0,
        },
      ],
    });
    setIsFormOpen(true);
  };

  const handleAddItemRow = () => {
    setFormData({
      ...formData,
      items: [
        ...formData.items,
        {
          rawMaterialId: rawMaterials[0]?.id || '',
          quantityOrdered: 100,
          unitPrice: rawMaterials[0]?.unitCost || 0,
        },
      ],
    });
  };

  const handleRemoveItemRow = (index) => {
    const updated = [...formData.items];
    updated.splice(index, 1);
    setFormData({ ...formData, items: updated });
  };

  const handleItemChange = (index, field, value) => {
    const updated = [...formData.items];
    updated[index][field] = value;
    if (field === 'rawMaterialId') {
      const mat = rawMaterials.find((m) => m.id === Number(value));
      if (mat) {
        updated[index].unitPrice = mat.unitCost;
      }
    }
    setFormData({ ...formData, items: updated });
  };

  const poTotal = formData.items.reduce((sum, item) => {
    return sum + (Number(item.quantityOrdered) || 0) * (Number(item.unitPrice) || 0);
  }, 0);

  const handleSubmitPo = async (e) => {
    e.preventDefault();
    if (formData.items.length === 0) {
      toast.error('PO must contain at least one raw material line item.');
      return;
    }

    try {
      await api.post('/purchase-orders', formData);
      toast.success('Purchase order created in DRAFT status!');
      setIsFormOpen(false);
      fetchPurchaseOrders(currentPage);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create PO.');
    }
  };

  const handleMarkOrdered = async (id) => {
    try {
      await api.patch(`/purchase-orders/${id}/order`);
      toast.success('PO marked as ORDERED with vendor!');
      fetchPurchaseOrders(currentPage);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update PO status.');
    }
  };

  const handleReceivePoSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.post(`/purchase-orders/${receivingPo.id}/receive`, {
        notes: receiveNotes,
      });
      toast.success(`Materials from PO #${receivingPo.poNumber} received and added to physical inventory!`);
      setReceiveModalOpen(false);
      fetchPurchaseOrders(currentPage);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to receive PO.');
    }
  };

  const handleCancelPo = async () => {
    try {
      await api.delete(`/purchase-orders/${cancelingId}`);
      toast.success('Purchase order cancelled.');
      setCancelOpen(false);
      fetchPurchaseOrders(currentPage);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to cancel PO.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-100">Purchase Orders (Procurement)</h1>
          <p className="text-xs text-slate-400 mt-0.5">Procure raw materials from suppliers and automatically update warehouse stock on receipt</p>
        </div>
        {isManager && (
          <button
            onClick={handleOpenCreate}
            className="px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white text-xs font-semibold rounded-xl flex items-center gap-2 shadow-lg shadow-brand-600/20 transition"
          >
            <Plus className="w-4 h-4" /> Create Purchase Order
          </button>
        )}
      </div>

      {/* Filter Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 bg-slate-900/60 p-3 rounded-2xl border border-slate-800">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search by PO number, supplier company name..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-brand-500 transition"
          />
        </div>

        <select
          value={selectedStatus}
          onChange={(e) => setSelectedStatus(e.target.value)}
          className="px-3 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-brand-500 transition"
        >
          <option value="">All Statuses</option>
          <option value="DRAFT">DRAFT</option>
          <option value="ORDERED">ORDERED</option>
          <option value="RECEIVED">RECEIVED</option>
          <option value="CANCELLED">CANCELLED</option>
        </select>
      </div>

      {/* PO Table */}
      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-950/50 text-slate-400 font-semibold uppercase tracking-wider">
                <th className="py-3 px-4">PO #</th>
                <th className="py-3 px-4">Supplier Vendor</th>
                <th className="py-3 px-4">Order Date</th>
                <th className="py-3 px-4">Expected Date</th>
                <th className="py-3 px-4 text-center">Status</th>
                <th className="py-3 px-4 text-right">Total Amount</th>
                <th className="py-3 px-4 text-right">Workflow Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300">
              {loading && purchaseOrders.length === 0 ? (
                <tr>
                  <td colSpan="7" className="text-center py-12 text-slate-400">
                    <div className="w-6 h-6 border-2 border-brand-500/20 border-t-brand-500 rounded-full animate-spin mx-auto mb-2" />
                    Loading purchase orders...
                  </td>
                </tr>
              ) : purchaseOrders.length === 0 ? (
                <tr>
                  <td colSpan="7" className="text-center py-12 text-slate-400">
                    No purchase orders found.
                  </td>
                </tr>
              ) : (
                purchaseOrders.map((po) => (
                  <tr key={po.id} className="hover:bg-slate-850/50 transition">
                    <td className="py-3.5 px-4 font-mono font-bold text-brand-400">{po.poNumber}</td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-slate-100">{po.supplierCompanyName}</div>
                      <div className="text-[11px] text-slate-400">{po.supplierContactPerson}</div>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-300">{po.orderDate}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-400">{po.expectedDate || '—'}</td>
                    <td className="py-3.5 px-4 text-center">
                      <Badge status={po.status} />
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-slate-100">
                      ${Number(po.totalAmount).toFixed(2)}
                    </td>
                    <td className="py-3.5 px-4 text-right space-x-1.5">
                      <button
                        onClick={() => {
                          setViewingPo(po);
                          setViewModalOpen(true);
                        }}
                        title="View Details"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
                      >
                        <Eye className="w-4 h-4" />
                      </button>

                      {isManager && po.status === 'DRAFT' && (
                        <button
                          onClick={() => handleMarkOrdered(po.id)}
                          className="px-2 py-1 rounded-lg bg-blue-500/10 hover:bg-blue-500/20 text-blue-300 border border-blue-500/30 text-[11px] font-semibold inline-flex items-center gap-1 transition"
                        >
                          <Send className="w-3 h-3" /> Submit Order
                        </button>
                      )}

                      {isManager && po.status === 'ORDERED' && (
                        <button
                          onClick={() => {
                            setReceivingPo(po);
                            setReceiveNotes('');
                            setReceiveModalOpen(true);
                          }}
                          className="px-2 py-1 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 text-[11px] font-semibold inline-flex items-center gap-1 transition"
                        >
                          <PackageCheck className="w-3 h-3" /> Receive Stock
                        </button>
                      )}

                      {isAdmin && po.status !== 'RECEIVED' && po.status !== 'CANCELLED' && (
                        <button
                          onClick={() => {
                            setCancelingId(po.id);
                            setCancelOpen(true);
                          }}
                          title="Cancel PO"
                          className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          totalItems={totalItems}
          pageSize={10}
          onPageChange={(page) => fetchPurchaseOrders(page)}
        />
      </div>

      {/* Create PO Modal */}
      <Modal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        title="Create Raw Material Purchase Order"
        maxWidth="max-w-3xl"
      >
        <form onSubmit={handleSubmitPo} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Supplier Vendor *</label>
              <select
                required
                value={formData.supplierId}
                onChange={(e) => setFormData({ ...formData, supplierId: Number(e.target.value) })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
              >
                {suppliers.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.companyName} ({s.paymentTerms})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Order Date *</label>
              <input
                type="date"
                required
                value={formData.orderDate}
                onChange={(e) => setFormData({ ...formData, orderDate: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Expected Delivery Date</label>
              <input
                type="date"
                value={formData.expectedDate}
                onChange={(e) => setFormData({ ...formData, expectedDate: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>
          </div>

          {/* Dynamic PO Line Items */}
          <div className="space-y-3 pt-3 border-t border-slate-800">
            <div className="flex items-center justify-between">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300">
                Raw Material Line Items
              </h4>
              <button
                type="button"
                onClick={handleAddItemRow}
                className="px-3 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-brand-400 text-xs font-semibold flex items-center gap-1 transition"
              >
                <Plus className="w-3.5 h-3.5" /> Add Material
              </button>
            </div>

            <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
              {formData.items.map((row, idx) => {
                const lineTotal = (Number(row.quantityOrdered) || 0) * (Number(row.unitPrice) || 0);
                const mat = rawMaterials.find((m) => m.id === Number(row.rawMaterialId));
                return (
                  <div
                    key={idx}
                    className="p-3 rounded-xl bg-slate-950 border border-slate-800 flex items-center gap-3 text-xs"
                  >
                    <div className="flex-1">
                      <select
                        value={row.rawMaterialId}
                        onChange={(e) => handleItemChange(idx, 'rawMaterialId', e.target.value)}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-slate-100"
                      >
                        {rawMaterials.map((rm) => (
                          <option key={rm.id} value={rm.id}>
                            {rm.name} ({rm.materialCode})
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="w-24">
                      <input
                        type="number"
                        step="0.01"
                        min="0.01"
                        placeholder="Qty"
                        value={row.quantityOrdered}
                        onChange={(e) => handleItemChange(idx, 'quantityOrdered', parseFloat(e.target.value) || 0)}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-slate-100 font-mono text-right"
                      />
                    </div>

                    <div className="w-14 text-center font-mono text-slate-400">
                      {mat?.unitOfMeasure || 'units'}
                    </div>

                    <div className="w-24 text-right font-mono font-semibold text-slate-300">
                      ${Number(row.unitPrice).toFixed(2)}
                    </div>

                    <div className="w-28 text-right font-mono font-bold text-emerald-400">
                      ${lineTotal.toFixed(2)}
                    </div>

                    <button
                      type="button"
                      onClick={() => handleRemoveItemRow(idx)}
                      className="p-1 rounded-md text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                );
              })}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Procurement Instructions / Notes</label>
            <input
              type="text"
              placeholder="e.g. Include Certificate of Analysis (COA) with shipment"
              value={formData.notes}
              onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
            />
          </div>

          <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 flex justify-between items-center text-xs">
            <span className="text-slate-400 font-medium">Estimated Purchase Total:</span>
            <span className="text-base font-black text-emerald-400 font-mono">${poTotal.toFixed(2)}</span>
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={() => setIsFormOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-300 hover:text-white bg-slate-800 rounded-xl transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-xs font-semibold text-white bg-brand-600 hover:bg-brand-500 rounded-xl shadow-lg shadow-brand-600/20 transition"
            >
              Save Draft PO
            </button>
          </div>
        </form>
      </Modal>

      {/* Receive Stock Modal */}
      <Modal
        isOpen={receiveModalOpen}
        onClose={() => setReceiveModalOpen(false)}
        title={`Receive Materials for PO #${receivingPo?.poNumber || ''}`}
        maxWidth="max-w-md"
      >
        <form onSubmit={handleReceivePoSubmit} className="space-y-4">
          <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-xs text-emerald-300">
            <p className="font-bold flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Confirm Warehouse Receipt
            </p>
            <p className="mt-1 opacity-80">
              Confirming this will mark all items as RECEIVED and immediately increment stock quantities in your warehouse inventory.
            </p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Receiving Inspection Notes</label>
            <textarea
              rows="3"
              placeholder="e.g. Inspected shipment dock #2, lot numbers verified, quality approved"
              value={receiveNotes}
              onChange={(e) => setReceiveNotes(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={() => setReceiveModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-300 hover:text-white bg-slate-800 rounded-xl transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-500 rounded-xl shadow-lg shadow-emerald-600/20 transition flex items-center gap-1.5"
            >
              <PackageCheck className="w-4 h-4" /> Receive & Restock Inventory
            </button>
          </div>
        </form>
      </Modal>

      {/* View Details Modal */}
      <Modal
        isOpen={viewModalOpen}
        onClose={() => setViewModalOpen(false)}
        title={`PO Details: ${viewingPo?.poNumber || ''}`}
        maxWidth="max-w-2xl"
      >
        {viewingPo && (
          <div className="space-y-4 text-xs">
            <div className="grid grid-cols-2 gap-4 p-4 rounded-xl bg-slate-950 border border-slate-800">
              <div>
                <p className="text-slate-400 font-semibold">Vendor Supplier</p>
                <p className="text-slate-100 font-bold mt-0.5">{viewingPo.supplierCompanyName}</p>
                <p className="text-slate-400 text-[11px]">{viewingPo.supplierEmail}</p>
              </div>
              <div className="text-right">
                <p className="text-slate-400 font-semibold">Status & Delivery</p>
                <div className="mt-1">
                  <Badge status={viewingPo.status} />
                </div>
                <p className="text-slate-400 text-[11px] mt-1 font-mono">Order Date: {viewingPo.orderDate}</p>
              </div>
            </div>

            <div className="rounded-xl border border-slate-800 overflow-hidden">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="bg-slate-950 text-slate-400 uppercase text-[10px] font-semibold border-b border-slate-800">
                    <th className="py-2.5 px-3">Material</th>
                    <th className="py-2.5 px-3">Code</th>
                    <th className="py-2.5 px-3 text-right">Ordered</th>
                    <th className="py-2.5 px-3 text-right">Received</th>
                    <th className="py-2.5 px-3 text-right">Unit Price</th>
                    <th className="py-2.5 px-3 text-right">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {viewingPo.items?.map((it) => (
                    <tr key={it.id}>
                      <td className="py-2.5 px-3 font-semibold text-slate-200">{it.materialName}</td>
                      <td className="py-2.5 px-3 font-mono text-slate-400">{it.materialCode}</td>
                      <td className="py-2.5 px-3 text-right font-mono text-slate-300">
                        {it.quantityOrdered} {it.unitOfMeasure}
                      </td>
                      <td className="py-2.5 px-3 text-right font-mono font-bold text-emerald-400">
                        {it.quantityReceived} {it.unitOfMeasure}
                      </td>
                      <td className="py-2.5 px-3 text-right font-mono text-slate-300">
                        ${Number(it.unitPrice).toFixed(2)}
                      </td>
                      <td className="py-2.5 px-3 text-right font-mono font-bold text-slate-100">
                        ${Number(it.lineTotal).toFixed(2)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex justify-between items-center p-3 rounded-xl bg-slate-950 border border-slate-800 font-mono">
              <span className="text-slate-400 font-sans">Total Spend:</span>
              <span className="text-base font-black text-emerald-400">${Number(viewingPo.totalAmount).toFixed(2)}</span>
            </div>
          </div>
        )}
      </Modal>

      {/* Cancel Confirm */}
      <ConfirmDialog
        isOpen={cancelOpen}
        onClose={() => setCancelOpen(false)}
        onConfirm={handleCancelPo}
        title="Cancel Purchase Order"
        message="Are you sure you want to cancel this purchase order?"
        confirmText="Yes, Cancel PO"
        isDanger={true}
      />
    </div>
  );
};
