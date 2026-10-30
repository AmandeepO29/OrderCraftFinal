import React, { useEffect, useState } from 'react';
import { Factory, Plus, Search, Play, CheckCircle2, AlertTriangle, ShieldCheck, Clock, FileCode2, Boxes, ArrowRight, Eye } from 'lucide-react';
import api from '../api/client';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Pagination } from '../components/Pagination';
import { Badge } from '../components/Badge';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export const ProductionManagement = () => {
  const [productionOrders, setProductionOrders] = useState([]);
  const [confirmedCustomerOrders, setConfirmedCustomerOrders] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);

  // Create Production Plan Modal
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    productionNumber: '',
    customerOrderId: '',
    productId: '',
    quantityToProduce: 1,
    startDate: new Date().toISOString().split('T')[0],
    endDate: '',
    notes: '',
  });

  // Complete Production Run Modal
  const [completeModalOpen, setCompleteModalOpen] = useState(false);
  const [completingOrder, setCompletingOrder] = useState(null);
  const [producedQuantity, setProducedQuantity] = useState(1);
  const [completionNotes, setCompletionNotes] = useState('');

  // Availability check modal
  const [checkModalOpen, setCheckModalOpen] = useState(false);
  const [checkResult, setCheckResult] = useState(null);
  const [checkLoading, setCheckLoading] = useState(false);

  // Cancel Dialog
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelingId, setCancelingId] = useState(null);

  const { isManager, isAdmin } = useAuth();
  const toast = useToast();

  const fetchProductionOrders = async (page = 0, query = searchQuery, status = selectedStatus) => {
    setLoading(true);
    try {
      const res = await api.get('/production', {
        params: {
          page,
          size: 10,
          query: query || undefined,
          status: status || undefined,
        },
      });
      setProductionOrders(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalItems(res.data.totalElements);
      setCurrentPage(page);
    } catch (err) {
      toast.error('Failed to load production orders.');
    } finally {
      setLoading(false);
    }
  };

  const fetchMeta = async () => {
    try {
      const [ordersRes, prodsRes] = await Promise.all([
        api.get('/orders', { params: { page: 0, size: 50, status: 'CONFIRMED' } }),
        api.get('/products/active'),
      ]);
      setConfirmedCustomerOrders(ordersRes.data.content);
      setProducts(prodsRes.data);
    } catch (err) {}
  };

  useEffect(() => {
    fetchProductionOrders(0, searchQuery, selectedStatus);
    fetchMeta();
  }, [searchQuery, selectedStatus]);

  const handleOpenCreate = () => {
    setFormData({
      productionNumber: '',
      customerOrderId: confirmedCustomerOrders[0]?.id || '',
      productId: products[0]?.id || '',
      quantityToProduce: 5,
      startDate: new Date().toISOString().split('T')[0],
      endDate: '',
      notes: '',
    });
    setIsFormOpen(true);
  };

  const handleSubmitPlan = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...formData,
        customerOrderId: formData.customerOrderId ? Number(formData.customerOrderId) : null,
        productId: Number(formData.productId),
        quantityToProduce: parseInt(formData.quantityToProduce) || 1,
      };
      await api.post('/production', payload);
      toast.success('Production order plan created in PLANNED status!');
      setIsFormOpen(false);
      fetchProductionOrders(currentPage);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create production plan.');
    }
  };

  const handleStartProduction = async (id) => {
    try {
      await api.patch(`/production/${id}/start`);
      toast.success('Production run started (status IN_PROGRESS)!');
      fetchProductionOrders(currentPage);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to start production.');
    }
  };

  const handleOpenCompleteModal = (order) => {
    setCompletingOrder(order);
    setProducedQuantity(order.quantityToProduce);
    setCompletionNotes('');
    setCompleteModalOpen(true);
  };

  const handleCompleteSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.post(`/production/${completingOrder.id}/complete`, {
        quantityProduced: parseInt(producedQuantity) || completingOrder.quantityToProduce,
        notes: completionNotes,
      });
      toast.success(`Production completed! Raw materials deducted according to BOM and finished goods added to warehouse!`);
      setCompleteModalOpen(false);
      fetchProductionOrders(currentPage);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to complete production run.');
    }
  };

  const handleVerifyStock = async (productId, qty) => {
    setCheckModalOpen(true);
    setCheckLoading(true);
    try {
      const res = await api.get('/boms/check-availability', {
        params: { productId, quantity: qty },
      });
      setCheckResult(res.data);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to verify BOM readiness.');
    } finally {
      setCheckLoading(false);
    }
  };

  const handleCancelProduction = async () => {
    try {
      await api.delete(`/production/${cancelingId}`);
      toast.success('Production order cancelled.');
      setCancelOpen(false);
      fetchProductionOrders(currentPage);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to cancel production order.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-100">Shop Floor Production Scheduling</h1>
          <p className="text-xs text-slate-400 mt-0.5">Automated material consumption via BOM, batch scheduling & finished goods restocking</p>
        </div>
        {isManager && (
          <button
            onClick={handleOpenCreate}
            className="px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white text-xs font-semibold rounded-xl flex items-center gap-2 shadow-lg shadow-brand-600/20 transition"
          >
            <Plus className="w-4 h-4" /> Schedule Production Run
          </button>
        )}
      </div>

      {/* Filter Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 bg-slate-900/60 p-3 rounded-2xl border border-slate-800">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search by production number, product name, or SKU..."
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
          <option value="PLANNED">PLANNED</option>
          <option value="IN_PROGRESS">IN_PROGRESS</option>
          <option value="COMPLETED">COMPLETED</option>
          <option value="CANCELLED">CANCELLED</option>
        </select>
      </div>

      {/* Production Runs Table */}
      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-950/50 text-slate-400 font-semibold uppercase tracking-wider">
                <th className="py-3 px-4">Production Run #</th>
                <th className="py-3 px-4">Manufactured Product</th>
                <th className="py-3 px-4">Linked Sales Order</th>
                <th className="py-3 px-4 text-center">Batch Target</th>
                <th className="py-3 px-4">Timeline</th>
                <th className="py-3 px-4 text-center">Production Status</th>
                <th className="py-3 px-4 text-right">Floor Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300">
              {loading && productionOrders.length === 0 ? (
                <tr>
                  <td colSpan="7" className="text-center py-12 text-slate-400">
                    <div className="w-6 h-6 border-2 border-brand-500/20 border-t-brand-500 rounded-full animate-spin mx-auto mb-2" />
                    Loading production floor orders...
                  </td>
                </tr>
              ) : productionOrders.length === 0 ? (
                <tr>
                  <td colSpan="7" className="text-center py-12 text-slate-400">
                    No production orders found. Click "Schedule Production Run" to plan one.
                  </td>
                </tr>
              ) : (
                productionOrders.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-850/50 transition">
                    <td className="py-3.5 px-4 font-mono font-bold text-brand-400">{p.productionNumber}</td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-slate-100">{p.productName}</div>
                      <div className="font-mono text-[11px] text-slate-400">{p.productSku}</div>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-300">
                      {p.customerOrderNumber ? (
                        <span className="font-semibold text-brand-400">{p.customerOrderNumber}</span>
                      ) : (
                        <span className="text-slate-500">General Stock Run</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-center font-mono font-bold text-slate-100">
                      {p.status === 'COMPLETED' ? (
                        <span className="text-emerald-400 font-bold">{p.quantityProduced} units</span>
                      ) : (
                        <span>{p.quantityToProduce} units</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-[11px] text-slate-400 space-y-0.5">
                      <div>Start: {p.startDate || '—'}</div>
                      <div>End: {p.endDate || '—'}</div>
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <Badge status={p.status} />
                    </td>
                    <td className="py-3.5 px-4 text-right space-x-1.5">
                      <button
                        onClick={() => handleVerifyStock(p.productId, p.quantityToProduce)}
                        title="Check BOM Material Stock"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-cyan-400 hover:bg-slate-800 transition"
                      >
                        <ShieldCheck className="w-4 h-4" />
                      </button>

                      {p.status === 'PLANNED' && (
                        <button
                          onClick={() => handleStartProduction(p.id)}
                          className="px-2.5 py-1 rounded-lg bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 text-[11px] font-semibold inline-flex items-center gap-1 transition"
                        >
                          <Play className="w-3 h-3" /> Start Run
                        </button>
                      )}

                      {p.status === 'IN_PROGRESS' && (
                        <button
                          onClick={() => handleOpenCompleteModal(p)}
                          className="px-2.5 py-1 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 text-[11px] font-semibold inline-flex items-center gap-1 transition"
                        >
                          <CheckCircle2 className="w-3 h-3" /> Complete Run
                        </button>
                      )}

                      {isAdmin && p.status !== 'COMPLETED' && p.status !== 'CANCELLED' && (
                        <button
                          onClick={() => {
                            setCancelingId(p.id);
                            setCancelOpen(true);
                          }}
                          title="Cancel Production Order"
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
          onPageChange={(page) => fetchProductionOrders(page)}
        />
      </div>

      {/* Schedule Production Modal */}
      <Modal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        title="Schedule New Production Run"
        maxWidth="max-w-2xl"
      >
        <form onSubmit={handleSubmitPlan} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Product to Manufacture *</label>
              <select
                required
                value={formData.productId}
                onChange={(e) => setFormData({ ...formData, productId: Number(e.target.value) })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
              >
                {products.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} ({p.sku})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Link Customer Sales Order</label>
              <select
                value={formData.customerOrderId}
                onChange={(e) => setFormData({ ...formData, customerOrderId: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
              >
                <option value="">None (Build for Finished Goods Stock)</option>
                {confirmedCustomerOrders.map((o) => (
                  <option key={o.id} value={o.id}>
                    {o.orderNumber} - {o.customerName} (${o.totalAmount})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Batch Target Units *</label>
              <input
                type="number"
                min="1"
                required
                value={formData.quantityToProduce}
                onChange={(e) => setFormData({ ...formData, quantityToProduce: parseInt(e.target.value) || 1 })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono font-bold focus:outline-none focus:border-brand-500 transition"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Scheduled Start Date *</label>
              <input
                type="date"
                required
                value={formData.startDate}
                onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Target Completion Date</label>
              <input
                type="date"
                value={formData.endDate}
                onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Work Order Instructions / Shop Notes</label>
            <textarea
              rows="3"
              placeholder="e.g. Ensure robotic welding calibration step 4 is performed prior to sealing"
              value={formData.notes}
              onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
            />
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
              Create Production Plan
            </button>
          </div>
        </form>
      </Modal>

      {/* Complete Production Modal */}
      <Modal
        isOpen={completeModalOpen}
        onClose={() => setCompleteModalOpen(false)}
        title={`Complete Production Run: ${completingOrder?.productionNumber || ''}`}
        maxWidth="max-w-md"
      >
        <form onSubmit={handleCompleteSubmit} className="space-y-4">
          <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-xs text-emerald-300">
            <p className="font-bold flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Automated Inventory Reconciliation
            </p>
            <p className="mt-1 opacity-80">
              Completing this run will:
            </p>
            <ul className="list-disc list-inside mt-1 space-y-0.5 text-[11px] opacity-90">
              <li>Deduct all raw materials consumed using the product's BOM recipe.</li>
              <li>Record PRODUCTION_CONSUMPTION audit logs.</li>
              <li>Increment finished goods warehouse inventory.</li>
              <li>Record PRODUCTION_OUTPUT audit log.</li>
            </ul>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Total Finished Units Produced *</label>
            <input
              type="number"
              min="1"
              required
              value={producedQuantity}
              onChange={(e) => setProducedQuantity(parseInt(e.target.value) || 1)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono font-bold text-lg focus:outline-none focus:border-brand-500 transition"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Quality Inspection & Shift Log Notes</label>
            <textarea
              rows="3"
              placeholder="e.g. Batch completed on CNC Line 3. 100% QA inspected, zero defects."
              value={completionNotes}
              onChange={(e) => setCompletionNotes(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={() => setCompleteModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-300 hover:text-white bg-slate-800 rounded-xl transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-500 rounded-xl shadow-lg shadow-emerald-600/20 transition flex items-center gap-1.5"
            >
              <CheckCircle2 className="w-4 h-4" /> Reconcile & Complete Run
            </button>
          </div>
        </form>
      </Modal>

      {/* Verify BOM Modal */}
      <Modal
        isOpen={checkModalOpen}
        onClose={() => setCheckModalOpen(false)}
        title="BOM Raw Material Readiness Verification"
        maxWidth="max-w-2xl"
      >
        {checkLoading ? (
          <div className="py-8 text-center text-slate-400 text-xs">Evaluating raw material balances...</div>
        ) : checkResult ? (
          <div className="space-y-4">
            <div
              className={`p-3.5 rounded-xl border flex items-center gap-2.5 text-xs ${
                checkResult.sufficient
                  ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-300 font-bold'
                  : 'bg-rose-500/10 border-rose-500/20 text-rose-300 font-bold'
              }`}
            >
              {checkResult.sufficient ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              ) : (
                <AlertTriangle className="w-4 h-4 text-rose-400" />
              )}
              <span>
                {checkResult.sufficient
                  ? `Sufficient inventory to manufacture batch of ${checkResult.requestedQuantity} units!`
                  : `Deficit detected: Procure raw materials before starting production.`}
              </span>
            </div>

            <div className="rounded-xl border border-slate-800 overflow-hidden">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="bg-slate-950 text-slate-400 uppercase text-[10px] font-semibold border-b border-slate-800">
                    <th className="py-2.5 px-3">Raw Material</th>
                    <th className="py-2.5 px-3 text-right">Required</th>
                    <th className="py-2.5 px-3 text-right">Current Stock</th>
                    <th className="py-2.5 px-3 text-center">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {checkResult.requirements?.map((req) => (
                    <tr key={req.rawMaterialId}>
                      <td className="py-2.5 px-3 font-semibold text-slate-200">{req.materialName}</td>
                      <td className="py-2.5 px-3 text-right font-mono text-slate-100 font-bold">
                        {req.requiredQuantity} {req.unitOfMeasure}
                      </td>
                      <td className="py-2.5 px-3 text-right font-mono text-slate-300">
                        {req.availableStock} {req.unitOfMeasure}
                      </td>
                      <td className="py-2.5 px-3 text-center">
                        {req.isAvailable ? (
                          <span className="text-emerald-400 font-bold text-[11px]">Ready</span>
                        ) : (
                          <span className="text-rose-400 font-bold text-[11px]">Shortage</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : null}
      </Modal>

      {/* Cancel Confirm */}
      <ConfirmDialog
        isOpen={cancelOpen}
        onClose={() => setCancelOpen(false)}
        onConfirm={handleCancelProduction}
        title="Cancel Production Order"
        message="Are you sure you want to cancel this scheduled production run?"
        confirmText="Yes, Cancel Run"
        isDanger={true}
      />
    </div>
  );
};
