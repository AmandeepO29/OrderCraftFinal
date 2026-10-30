import React, { useEffect, useState } from 'react';
import { ShoppingCart, Plus, Search, Edit2, Trash2, Eye, Receipt, ArrowRight, CheckCircle2, Clock, XCircle, FileText } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api/client';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Pagination } from '../components/Pagination';
import { Badge } from '../components/Badge';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export const CustomerOrders = () => {
  const [orders, setOrders] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);

  // Create Order Modal
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    customerId: '',
    orderDate: new Date().toISOString().split('T')[0],
    expectedDeliveryDate: '',
    discountAmount: 0,
    notes: '',
    items: [],
  });

  // Status Change Modal
  const [statusModalOpen, setStatusModalOpen] = useState(false);
  const [updatingOrder, setUpdatingOrder] = useState(null);
  const [newStatus, setNewStatus] = useState('CONFIRMED');
  const [statusNotes, setStatusNotes] = useState('');

  // View details modal
  const [viewModalOpen, setViewModalOpen] = useState(false);
  const [viewingOrder, setViewingOrder] = useState(null);

  // Cancel dialog
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelingId, setCancelingId] = useState(null);

  const { isManager, isAdmin } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const fetchOrders = async (page = 0, query = searchQuery, status = selectedStatus) => {
    setLoading(true);
    try {
      const res = await api.get('/orders', {
        params: {
          page,
          size: 10,
          query: query || undefined,
          status: status || undefined,
        },
      });
      setOrders(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalItems(res.data.totalElements);
      setCurrentPage(page);
    } catch (err) {
      toast.error('Failed to load customer orders.');
    } finally {
      setLoading(false);
    }
  };

  const fetchMeta = async () => {
    try {
      const [custsRes, prodsRes] = await Promise.all([
        api.get('/customers', { params: { page: 0, size: 100 } }),
        api.get('/products/active'),
      ]);
      setCustomers(custsRes.data.content);
      setProducts(prodsRes.data);
    } catch (err) {}
  };

  useEffect(() => {
    fetchOrders(0, searchQuery, selectedStatus);
    fetchMeta();
  }, [searchQuery, selectedStatus]);

  const handleOpenCreate = () => {
    setFormData({
      customerId: customers[0]?.id || '',
      orderDate: new Date().toISOString().split('T')[0],
      expectedDeliveryDate: '',
      discountAmount: 0,
      notes: '',
      items: [{ productId: products[0]?.id || '', quantity: 1, unitPrice: products[0]?.unitPrice || 0 }],
    });
    setIsFormOpen(true);
  };

  const handleAddItemRow = () => {
    setFormData({
      ...formData,
      items: [
        ...formData.items,
        { productId: products[0]?.id || '', quantity: 1, unitPrice: products[0]?.unitPrice || 0 },
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
    if (field === 'productId') {
      const prod = products.find((p) => p.id === Number(value));
      if (prod) {
        updated[index].unitPrice = prod.unitPrice;
      }
    }
    setFormData({ ...formData, items: updated });
  };

  // Live order calculations
  const subtotal = formData.items.reduce((sum, item) => {
    const qty = Number(item.quantity) || 0;
    const price = Number(item.unitPrice) || 0;
    return sum + qty * price;
  }, 0);
  const taxAmount = subtotal * 0.1; // 10%
  const total = subtotal + taxAmount - (Number(formData.discountAmount) || 0);

  const handleSubmitOrder = async (e) => {
    e.preventDefault();
    if (formData.items.length === 0) {
      toast.error('Order must contain at least one product.');
      return;
    }

    try {
      await api.post('/orders', formData);
      toast.success('Customer order created successfully!');
      setIsFormOpen(false);
      fetchOrders(currentPage);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create customer order.');
    }
  };

  const handleUpdateStatusSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.patch(`/orders/${updatingOrder.id}/status`, {
        status: newStatus,
        notes: statusNotes,
      });
      toast.success(`Order status updated to ${newStatus}!`);
      setStatusModalOpen(false);
      fetchOrders(currentPage);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update order status.');
    }
  };

  const handleGenerateInvoice = async (orderId) => {
    try {
      const res = await api.post('/invoices', { customerOrderId: orderId });
      toast.success(`Invoice ${res.data.invoiceNumber} created!`);
      fetchOrders(currentPage);
      navigate('/invoices');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to generate invoice.');
    }
  };

  const handleCancelOrder = async () => {
    try {
      await api.delete(`/orders/${cancelingId}`);
      toast.success('Order cancelled successfully.');
      setCancelOpen(false);
      fetchOrders(currentPage);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to cancel order.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-100">Customer Sales Orders</h1>
          <p className="text-xs text-slate-400 mt-0.5">Order pipeline: PENDING &rarr; CONFIRMED &rarr; IN_PRODUCTION &rarr; COMPLETED</p>
        </div>
        {isManager && (
          <button
            onClick={handleOpenCreate}
            className="px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white text-xs font-semibold rounded-xl flex items-center gap-2 shadow-lg shadow-brand-600/20 transition"
          >
            <Plus className="w-4 h-4" /> New Customer Order
          </button>
        )}
      </div>

      {/* Filter Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 bg-slate-900/60 p-3 rounded-2xl border border-slate-800">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search by order number, customer name, or company..."
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
          <option value="PENDING">PENDING</option>
          <option value="CONFIRMED">CONFIRMED</option>
          <option value="IN_PRODUCTION">IN_PRODUCTION</option>
          <option value="COMPLETED">COMPLETED</option>
          <option value="CANCELLED">CANCELLED</option>
        </select>
      </div>

      {/* Orders Table */}
      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-950/50 text-slate-400 font-semibold uppercase tracking-wider">
                <th className="py-3 px-4">Order #</th>
                <th className="py-3 px-4">Customer</th>
                <th className="py-3 px-4">Order Date</th>
                <th className="py-3 px-4 text-center">Lifecycle Status</th>
                <th className="py-3 px-4 text-right">Subtotal</th>
                <th className="py-3 px-4 text-right">Total Amount</th>
                <th className="py-3 px-4 text-center">Invoice</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300">
              {loading && orders.length === 0 ? (
                <tr>
                  <td colSpan="8" className="text-center py-12 text-slate-400">
                    <div className="w-6 h-6 border-2 border-brand-500/20 border-t-brand-500 rounded-full animate-spin mx-auto mb-2" />
                    Loading orders...
                  </td>
                </tr>
              ) : orders.length === 0 ? (
                <tr>
                  <td colSpan="8" className="text-center py-12 text-slate-400">
                    No customer orders found.
                  </td>
                </tr>
              ) : (
                orders.map((ord) => (
                  <tr key={ord.id} className="hover:bg-slate-850/50 transition">
                    <td className="py-3.5 px-4 font-mono font-bold text-brand-400">{ord.orderNumber}</td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-slate-100">{ord.customerName}</div>
                      {ord.customerCompanyName && (
                        <div className="text-[11px] text-slate-400">{ord.customerCompanyName}</div>
                      )}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-300">{ord.orderDate}</td>
                    <td className="py-3.5 px-4 text-center">
                      <Badge status={ord.status} />
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono text-slate-400">
                      ${Number(ord.subtotal).toFixed(2)}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-emerald-400">
                      ${Number(ord.totalAmount).toFixed(2)}
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      {ord.invoiceNumber ? (
                        <Link
                          to="/invoices"
                          className="inline-flex items-center gap-1 font-mono text-[11px] text-brand-400 font-semibold hover:underline"
                        >
                          <Receipt className="w-3 h-3" /> {ord.invoiceNumber}
                        </Link>
                      ) : isManager && ord.status !== 'CANCELLED' ? (
                        <button
                          onClick={() => handleGenerateInvoice(ord.id)}
                          className="px-2 py-0.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-brand-300 text-[10px] font-bold border border-slate-700 transition"
                        >
                          Generate Inv
                        </button>
                      ) : (
                        <span className="text-slate-500">—</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-right space-x-1.5">
                      <button
                        onClick={() => {
                          setViewingOrder(ord);
                          setViewModalOpen(true);
                        }}
                        title="View Details"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
                      >
                        <Eye className="w-4 h-4" />
                      </button>

                      {isManager && (
                        <button
                          onClick={() => {
                            setUpdatingOrder(ord);
                            setNewStatus(ord.status);
                            setStatusNotes('');
                            setStatusModalOpen(true);
                          }}
                          title="Update Status"
                          className="p-1.5 rounded-lg text-slate-400 hover:text-brand-400 hover:bg-slate-800 transition"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                      )}

                      {isAdmin && ord.status !== 'CANCELLED' && ord.status !== 'COMPLETED' && (
                        <button
                          onClick={() => {
                            setCancelingId(ord.id);
                            setCancelOpen(true);
                          }}
                          title="Cancel Order"
                          className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition"
                        >
                          <XCircle className="w-4 h-4" />
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
          onPageChange={(page) => fetchOrders(page)}
        />
      </div>

      {/* Create Order Modal */}
      <Modal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        title="Create New Customer Order"
        maxWidth="max-w-3xl"
      >
        <form onSubmit={handleSubmitOrder} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Customer Client *</label>
              <select
                required
                value={formData.customerId}
                onChange={(e) => setFormData({ ...formData, customerId: Number(e.target.value) })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
              >
                {customers.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} {c.companyName ? `(${c.companyName})` : ''}
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
              <label className="block text-xs font-semibold text-slate-300 mb-1">Expected Delivery</label>
              <input
                type="date"
                value={formData.expectedDeliveryDate}
                onChange={(e) => setFormData({ ...formData, expectedDeliveryDate: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>
          </div>

          {/* Dynamic Order Line Items */}
          <div className="space-y-3 pt-3 border-t border-slate-800">
            <div className="flex items-center justify-between">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300">
                Order Manufactured Line Items
              </h4>
              <button
                type="button"
                onClick={handleAddItemRow}
                className="px-3 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-brand-400 text-xs font-semibold flex items-center gap-1 transition"
              >
                <Plus className="w-3.5 h-3.5" /> Add Product
              </button>
            </div>

            <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
              {formData.items.map((row, idx) => {
                const lineTotal = (Number(row.quantity) || 0) * (Number(row.unitPrice) || 0);
                return (
                  <div
                    key={idx}
                    className="p-3 rounded-xl bg-slate-950 border border-slate-800 flex items-center gap-3 text-xs"
                  >
                    <div className="flex-1">
                      <select
                        value={row.productId}
                        onChange={(e) => handleItemChange(idx, 'productId', e.target.value)}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-slate-100"
                      >
                        {products.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.name} ({p.sku}) - ${p.unitPrice}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="w-24">
                      <input
                        type="number"
                        min="1"
                        placeholder="Qty"
                        value={row.quantity}
                        onChange={(e) => handleItemChange(idx, 'quantity', parseInt(e.target.value) || 1)}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-slate-100 font-mono text-right"
                      />
                    </div>

                    <div className="w-28 text-right font-mono font-semibold text-slate-300">
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

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Commercial Discount ($)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                value={formData.discountAmount}
                onChange={(e) => setFormData({ ...formData, discountAmount: parseFloat(e.target.value) || 0 })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Production / Delivery Notes</label>
              <input
                type="text"
                value={formData.notes}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
              />
            </div>
          </div>

          {/* Totals Summary */}
          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 flex justify-end">
            <div className="w-56 space-y-1.5 text-xs text-slate-400">
              <div className="flex justify-between">
                <span>Subtotal:</span>
                <span className="font-mono text-slate-200 font-bold">${subtotal.toFixed(2)}</span>
              </div>
              <div className="flex justify-between">
                <span>Tax (10%):</span>
                <span className="font-mono text-slate-200 font-bold">${taxAmount.toFixed(2)}</span>
              </div>
              <div className="flex justify-between">
                <span>Discount:</span>
                <span className="font-mono text-rose-400 font-bold">-${Number(formData.discountAmount).toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-base font-black text-emerald-400 border-t border-slate-800 pt-2">
                <span>Grand Total:</span>
                <span className="font-mono">${total.toFixed(2)}</span>
              </div>
            </div>
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
              Create Customer Order
            </button>
          </div>
        </form>
      </Modal>

      {/* Update Status Modal */}
      <Modal
        isOpen={statusModalOpen}
        onClose={() => setStatusModalOpen(false)}
        title={`Advance Order Status: ${updatingOrder?.orderNumber || ''}`}
        maxWidth="max-w-md"
      >
        <form onSubmit={handleUpdateStatusSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">New Workflow Stage</label>
            <select
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-semibold focus:outline-none focus:border-brand-500 transition"
            >
              <option value="PENDING">PENDING</option>
              <option value="CONFIRMED">CONFIRMED</option>
              <option value="IN_PRODUCTION">IN_PRODUCTION</option>
              <option value="COMPLETED">COMPLETED</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Status Transition Notes</label>
            <textarea
              rows="3"
              placeholder="e.g. Cleared credit check, authorized production run"
              value={statusNotes}
              onChange={(e) => setStatusNotes(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={() => setStatusModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-300 hover:text-white bg-slate-800 rounded-xl transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-xs font-semibold text-white bg-brand-600 hover:bg-brand-500 rounded-xl shadow-lg shadow-brand-600/20 transition"
            >
              Apply Status Change
            </button>
          </div>
        </form>
      </Modal>

      {/* View Order Details Modal */}
      <Modal
        isOpen={viewModalOpen}
        onClose={() => setViewModalOpen(false)}
        title={`Order Details: ${viewingOrder?.orderNumber || ''}`}
        maxWidth="max-w-2xl"
      >
        {viewingOrder && (
          <div className="space-y-4 text-xs">
            <div className="grid grid-cols-2 gap-4 p-4 rounded-xl bg-slate-950 border border-slate-800">
              <div>
                <p className="text-slate-400 font-semibold">Client Name</p>
                <p className="text-slate-100 font-bold mt-0.5">{viewingOrder.customerName}</p>
                <p className="text-slate-400 text-[11px]">{viewingOrder.customerEmail}</p>
              </div>
              <div className="text-right">
                <p className="text-slate-400 font-semibold">Status & Dates</p>
                <div className="mt-1">
                  <Badge status={viewingOrder.status} />
                </div>
                <p className="text-slate-400 text-[11px] mt-1 font-mono">Date: {viewingOrder.orderDate}</p>
              </div>
            </div>

            <div className="rounded-xl border border-slate-800 overflow-hidden">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="bg-slate-950 text-slate-400 uppercase text-[10px] font-semibold border-b border-slate-800">
                    <th className="py-2.5 px-3">Product</th>
                    <th className="py-2.5 px-3">SKU</th>
                    <th className="py-2.5 px-3 text-right">Qty</th>
                    <th className="py-2.5 px-3 text-right">Unit Price</th>
                    <th className="py-2.5 px-3 text-right">Line Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {viewingOrder.items?.map((it) => (
                    <tr key={it.id}>
                      <td className="py-2.5 px-3 font-semibold text-slate-200">{it.productName}</td>
                      <td className="py-2.5 px-3 font-mono text-slate-400">{it.productSku}</td>
                      <td className="py-2.5 px-3 text-right font-mono font-medium text-slate-300">{it.quantity}</td>
                      <td className="py-2.5 px-3 text-right font-mono text-slate-300">${Number(it.unitPrice).toFixed(2)}</td>
                      <td className="py-2.5 px-3 text-right font-mono font-bold text-slate-100">
                        ${Number(it.lineTotal).toFixed(2)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex justify-between items-center p-3 rounded-xl bg-slate-950 border border-slate-800 font-mono">
              <span className="text-slate-400 font-sans">Final Order Total:</span>
              <span className="text-base font-black text-emerald-400">${Number(viewingOrder.totalAmount).toFixed(2)}</span>
            </div>

            {viewingOrder.notes && (
              <p className="text-slate-400 text-[11px] bg-slate-950/60 p-2.5 rounded-lg border border-slate-800">
                <span className="font-semibold text-slate-300">Notes:</span> {viewingOrder.notes}
              </p>
            )}
          </div>
        )}
      </Modal>

      {/* Cancel Confirm */}
      <ConfirmDialog
        isOpen={cancelOpen}
        onClose={() => setCancelOpen(false)}
        onConfirm={handleCancelOrder}
        title="Cancel Customer Order"
        message="Are you sure you want to cancel this order? This cannot be undone."
        confirmText="Yes, Cancel Order"
        isDanger={true}
      />
    </div>
  );
};
