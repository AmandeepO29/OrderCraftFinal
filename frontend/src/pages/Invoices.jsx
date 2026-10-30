import React, { useEffect, useState } from 'react';
import { Receipt, Search, Printer, DollarSign, Eye, AlertCircle, CheckCircle2, Clock } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api/client';
import { Modal } from '../components/Modal';
import { Pagination } from '../components/Pagination';
import { Badge } from '../components/Badge';
import { InvoicePrintModal } from '../components/InvoicePrintModal';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export const Invoices = () => {
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);

  // Print Modal
  const [printModalOpen, setPrintModalOpen] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState(null);

  const { isManager } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const fetchInvoices = async (page = 0, query = searchQuery, status = selectedStatus) => {
    setLoading(true);
    try {
      const res = await api.get('/invoices', {
        params: {
          page,
          size: 10,
          query: query || undefined,
          status: status || undefined,
        },
      });
      setInvoices(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalItems(res.data.totalElements);
      setCurrentPage(page);
    } catch (err) {
      toast.error('Failed to load invoices.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvoices(0, searchQuery, selectedStatus);
  }, [searchQuery, selectedStatus]);

  const handleOpenPrint = (inv) => {
    setSelectedInvoice(inv);
    setPrintModalOpen(true);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-100">Invoices & Accounts Receivable</h1>
          <p className="text-xs text-slate-400 mt-0.5">Commercial customer tax invoices, remittance tracking & printable statements</p>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 bg-slate-900/60 p-3 rounded-2xl border border-slate-800">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search by invoice number, order number, or client name..."
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
          <option value="">All Payment Statuses</option>
          <option value="UNPAID">UNPAID</option>
          <option value="PARTIALLY_PAID">PARTIALLY_PAID</option>
          <option value="PAID">PAID</option>
          <option value="OVERDUE">OVERDUE</option>
        </select>
      </div>

      {/* Invoices Table */}
      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-950/50 text-slate-400 font-semibold uppercase tracking-wider">
                <th className="py-3 px-4">Invoice #</th>
                <th className="py-3 px-4">Order #</th>
                <th className="py-3 px-4">Client</th>
                <th className="py-3 px-4">Invoice Date</th>
                <th className="py-3 px-4">Due Date</th>
                <th className="py-3 px-4 text-center">Status</th>
                <th className="py-3 px-4 text-right">Total Amount</th>
                <th className="py-3 px-4 text-right">Balance Due</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300">
              {loading && invoices.length === 0 ? (
                <tr>
                  <td colSpan="9" className="text-center py-12 text-slate-400">
                    <div className="w-6 h-6 border-2 border-brand-500/20 border-t-brand-500 rounded-full animate-spin mx-auto mb-2" />
                    Loading invoices...
                  </td>
                </tr>
              ) : invoices.length === 0 ? (
                <tr>
                  <td colSpan="9" className="text-center py-12 text-slate-400">
                    No invoices found. Generate one from the Customer Orders page.
                  </td>
                </tr>
              ) : (
                invoices.map((inv) => (
                  <tr key={inv.id} className="hover:bg-slate-850/50 transition">
                    <td className="py-3.5 px-4 font-mono font-bold text-brand-400">{inv.invoiceNumber}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-300">{inv.customerOrderNumber}</td>
                    <td className="py-3.5 px-4 font-semibold text-slate-100">{inv.customerName}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-400">{inv.invoiceDate}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-400">{inv.dueDate}</td>
                    <td className="py-3.5 px-4 text-center">
                      <Badge status={inv.status} />
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold text-slate-100">
                      ${Number(inv.totalAmount).toFixed(2)}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-bold">
                      <span className={inv.balanceDue > 0 ? 'text-rose-400' : 'text-emerald-400'}>
                        ${Number(inv.balanceDue).toFixed(2)}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-right space-x-1.5">
                      <button
                        onClick={() => handleOpenPrint(inv)}
                        title="Print / View Invoice"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-brand-400 hover:bg-slate-800 transition"
                      >
                        <Printer className="w-4 h-4" />
                      </button>

                      {isManager && inv.status !== 'PAID' && (
                        <Link
                          to={`/payments?invoiceId=${inv.id}`}
                          className="px-2.5 py-1 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 text-[11px] font-semibold inline-flex items-center gap-1 transition"
                        >
                          <DollarSign className="w-3.5 h-3.5" /> Pay
                        </Link>
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
          onPageChange={(page) => fetchInvoices(page)}
        />
      </div>

      {/* Invoice Printable View Modal */}
      <InvoicePrintModal
        isOpen={printModalOpen}
        onClose={() => setPrintModalOpen(false)}
        invoice={selectedInvoice}
      />
    </div>
  );
};
