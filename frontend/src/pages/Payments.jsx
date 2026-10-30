import React, { useEffect, useState } from 'react';
import { CreditCard, Plus, Search, DollarSign, CheckCircle2, Calendar, FileText, ArrowUpRight } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import api from '../api/client';
import { Modal } from '../components/Modal';
import { Pagination } from '../components/Pagination';
import { Badge } from '../components/Badge';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export const Payments = () => {
  const [payments, setPayments] = useState([]);
  const [unpaidInvoices, setUnpaidInvoices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);

  // Form modal
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    invoiceId: '',
    paymentReference: '',
    paymentDate: new Date().toISOString().split('T')[0],
    amount: '',
    paymentMethod: 'BANK_TRANSFER',
    transactionNumber: '',
    notes: '',
  });

  const [searchParams] = useSearchParams();
  const preselectedInvoiceId = searchParams.get('invoiceId');

  const { isManager } = useAuth();
  const toast = useToast();

  const fetchPayments = async (page = 0, query = searchQuery) => {
    setLoading(true);
    try {
      const res = await api.get('/payments', {
        params: { page, size: 10, query: query || undefined },
      });
      setPayments(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalItems(res.data.totalElements);
      setCurrentPage(page);
    } catch (err) {
      toast.error('Failed to load payment receipts.');
    } finally {
      setLoading(false);
    }
  };

  const fetchUnpaidInvoices = async () => {
    try {
      const res = await api.get('/invoices', { params: { page: 0, size: 50 } });
      const unpaid = res.data.content.filter((inv) => inv.status !== 'PAID');
      setUnpaidInvoices(unpaid);
      if (preselectedInvoiceId && !formData.invoiceId) {
        const found = res.data.content.find((i) => i.id === Number(preselectedInvoiceId));
        if (found) {
          setFormData((prev) => ({
            ...prev,
            invoiceId: found.id,
            amount: found.balanceDue,
          }));
          setIsFormOpen(true);
        }
      }
    } catch (err) {}
  };

  useEffect(() => {
    fetchPayments(0, searchQuery);
    fetchUnpaidInvoices();
  }, [searchQuery]);

  const handleOpenCreate = () => {
    const defaultInv = unpaidInvoices[0];
    setFormData({
      invoiceId: defaultInv?.id || '',
      paymentReference: '',
      paymentDate: new Date().toISOString().split('T')[0],
      amount: defaultInv ? defaultInv.balanceDue : '',
      paymentMethod: 'BANK_TRANSFER',
      transactionNumber: '',
      notes: '',
    });
    setIsFormOpen(true);
  };

  const handleInvoiceChange = (invId) => {
    const inv = unpaidInvoices.find((i) => i.id === Number(invId));
    setFormData({
      ...formData,
      invoiceId: invId,
      amount: inv ? inv.balanceDue : '',
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.amount || Number(formData.amount) <= 0) {
      toast.error('Please enter a valid payment amount.');
      return;
    }

    try {
      await api.post('/payments', {
        ...formData,
        invoiceId: Number(formData.invoiceId),
        amount: parseFloat(formData.amount),
      });
      toast.success('Payment recorded and customer balance updated!');
      setIsFormOpen(false);
      fetchPayments(currentPage);
      fetchUnpaidInvoices();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to record payment.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-100">Customer Payment Receipts</h1>
          <p className="text-xs text-slate-400 mt-0.5">Record customer remittances, wire transfers, and reconcile invoice balances</p>
        </div>
        {isManager && (
          <button
            onClick={handleOpenCreate}
            className="px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white text-xs font-semibold rounded-xl flex items-center gap-2 shadow-lg shadow-brand-600/20 transition"
          >
            <Plus className="w-4 h-4" /> Record New Payment
          </button>
        )}
      </div>

      {/* Filter Bar */}
      <div className="flex items-center gap-4 bg-slate-900/60 p-3 rounded-2xl border border-slate-800">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search by payment reference, invoice number, or transaction number..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-brand-500 transition"
          />
        </div>
      </div>

      {/* Table */}
      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-950/50 text-slate-400 font-semibold uppercase tracking-wider">
                <th className="py-3 px-4">Receipt Ref</th>
                <th className="py-3 px-4">Invoice #</th>
                <th className="py-3 px-4">Customer</th>
                <th className="py-3 px-4">Payment Date</th>
                <th className="py-3 px-4">Method</th>
                <th className="py-3 px-4 text-right">Amount Received</th>
                <th className="py-3 px-4">Recorded By</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300">
              {loading && payments.length === 0 ? (
                <tr>
                  <td colSpan="7" className="text-center py-12 text-slate-400">
                    <div className="w-6 h-6 border-2 border-brand-500/20 border-t-brand-500 rounded-full animate-spin mx-auto mb-2" />
                    Loading payment records...
                  </td>
                </tr>
              ) : payments.length === 0 ? (
                <tr>
                  <td colSpan="7" className="text-center py-12 text-slate-400">
                    No payment receipts found. Click "Record New Payment" to log one.
                  </td>
                </tr>
              ) : (
                payments.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-850/50 transition">
                    <td className="py-3.5 px-4 font-mono font-bold text-brand-400">{p.paymentReference}</td>
                    <td className="py-3.5 px-4 font-mono font-semibold text-slate-300">#{p.invoiceNumber}</td>
                    <td className="py-3.5 px-4 font-medium text-slate-100">{p.customerName || 'N/A'}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-400">{p.paymentDate}</td>
                    <td className="py-3.5 px-4">
                      <span className="px-2 py-0.5 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 text-[11px] font-medium">
                        {p.paymentMethod.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-emerald-400">
                      +${Number(p.amount).toFixed(2)}
                    </td>
                    <td className="py-3.5 px-4 text-slate-400">{p.recordedByUsername}</td>
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
          onPageChange={(page) => fetchPayments(page)}
        />
      </div>

      {/* Record Payment Modal */}
      <Modal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        title="Record Customer Payment Receipt"
        maxWidth="max-w-md"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Target Invoice *</label>
            <select
              required
              value={formData.invoiceId}
              onChange={(e) => handleInvoiceChange(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
            >
              <option value="">Select Invoice to Reconcile</option>
              {unpaidInvoices.map((inv) => (
                <option key={inv.id} value={inv.id}>
                  {inv.invoiceNumber} - {inv.customerName} (Due: ${Number(inv.balanceDue).toFixed(2)})
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Payment Amount ($) *</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                placeholder="0.00"
                value={formData.amount}
                onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono font-bold text-emerald-400 text-lg focus:outline-none focus:border-brand-500 transition"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Payment Date *</label>
              <input
                type="date"
                required
                value={formData.paymentDate}
                onChange={(e) => setFormData({ ...formData, paymentDate: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Payment Method *</label>
              <select
                value={formData.paymentMethod}
                onChange={(e) => setFormData({ ...formData, paymentMethod: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
              >
                <option value="BANK_TRANSFER">Bank Transfer (Wire / ACH)</option>
                <option value="CREDIT_CARD">Credit Card</option>
                <option value="UPI">UPI / Digital Rail</option>
                <option value="CHECK">Commercial Check</option>
                <option value="CASH">Cash</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Transaction / Reference #</label>
              <input
                type="text"
                placeholder="e.g. WIRE-FED-99182"
                value={formData.transactionNumber}
                onChange={(e) => setFormData({ ...formData, transactionNumber: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Accounting Notes / Remarks</label>
            <textarea
              rows="2"
              placeholder="e.g. Cleared via Silicon Federal Bank confirmation"
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
              className="px-4 py-2 text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-500 rounded-xl shadow-lg shadow-emerald-600/20 transition flex items-center gap-1.5"
            >
              <DollarSign className="w-4 h-4" /> Confirm & Post Payment
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
