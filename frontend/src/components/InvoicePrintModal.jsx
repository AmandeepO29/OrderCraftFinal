import React from 'react';
import { Printer, Download, X } from 'lucide-react';
import { Modal } from './Modal';
import { Badge } from './Badge';

export const InvoicePrintModal = ({ isOpen, onClose, invoice }) => {
  if (!invoice) return null;

  const handlePrint = () => {
    window.print();
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Invoice #${invoice.invoiceNumber}`} maxWidth="max-w-4xl">
      <div className="flex justify-end gap-3 mb-4 -mt-2">
        <button
          onClick={handlePrint}
          className="px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white text-xs font-semibold rounded-xl flex items-center gap-2 shadow-lg shadow-brand-600/20 transition"
        >
          <Printer className="w-4 h-4" /> Print / Save as PDF
        </button>
      </div>

      <div id="printable-invoice" className="bg-white text-slate-900 p-8 rounded-2xl shadow-xl font-sans">
        {/* Header */}
        <div className="flex justify-between items-start border-b border-slate-200 pb-6 mb-6">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-2xl font-black tracking-tight text-slate-900">OrderCraft</span>
              <span className="text-xs font-bold uppercase tracking-wider text-brand-600 bg-brand-50 px-2 py-0.5 rounded">
                Manufacturing
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-1">Industrial Automation & Precision Engineering</p>
            <p className="text-xs text-slate-500">1000 Manufacturing Row, Suite 400, Chicago, IL</p>
            <p className="text-xs text-slate-500">billing@ordercraft.com | +1 (800) 555-CRAFT</p>
          </div>
          <div className="text-right">
            <h2 className="text-2xl font-black text-slate-900 tracking-tight">TAX INVOICE</h2>
            <p className="text-sm font-bold text-slate-700 mt-1">#{invoice.invoiceNumber}</p>
            <div className="mt-2">
              <span className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                invoice.status === 'PAID' ? 'bg-emerald-100 text-emerald-800' :
                invoice.status === 'PARTIALLY_PAID' ? 'bg-amber-100 text-amber-800' :
                'bg-rose-100 text-rose-800'
              }`}>
                {invoice.status}
              </span>
            </div>
          </div>
        </div>

        {/* Invoice meta info */}
        <div className="grid grid-cols-2 gap-8 mb-8 text-xs">
          <div>
            <h5 className="font-bold text-slate-400 uppercase tracking-wider mb-2">Billed To</h5>
            <p className="font-bold text-sm text-slate-900">{invoice.customerName}</p>
            {invoice.customerCompanyName && <p className="font-semibold text-slate-700">{invoice.customerCompanyName}</p>}
            <p className="text-slate-600 mt-1">{invoice.customerEmail}</p>
            <p className="text-slate-600 whitespace-pre-line mt-1">{invoice.billingAddress || 'No billing address provided'}</p>
          </div>
          <div className="text-right space-y-1">
            <div className="flex justify-between sm:justify-end gap-6">
              <span className="text-slate-500 font-medium">Order Number:</span>
              <span className="font-bold text-slate-900">{invoice.customerOrderNumber}</span>
            </div>
            <div className="flex justify-between sm:justify-end gap-6">
              <span className="text-slate-500 font-medium">Invoice Date:</span>
              <span className="font-bold text-slate-900">{invoice.invoiceDate}</span>
            </div>
            <div className="flex justify-between sm:justify-end gap-6">
              <span className="text-slate-500 font-medium">Payment Due Date:</span>
              <span className="font-bold text-slate-900">{invoice.dueDate}</span>
            </div>
          </div>
        </div>

        {/* Line Items Table */}
        <div className="overflow-x-auto mb-8">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b-2 border-slate-200 text-slate-500 uppercase tracking-wider font-semibold">
                <th className="py-2.5">Item & Description</th>
                <th className="py-2.5">SKU</th>
                <th className="py-2.5 text-right">Qty</th>
                <th className="py-2.5 text-right">Unit Price</th>
                <th className="py-2.5 text-right">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {invoice.items?.map((item, idx) => (
                <tr key={idx}>
                  <td className="py-3 font-semibold text-slate-800">{item.productName}</td>
                  <td className="py-3 text-slate-500 font-mono text-[11px]">{item.productSku}</td>
                  <td className="py-3 text-right font-medium text-slate-700">{item.quantity}</td>
                  <td className="py-3 text-right font-medium text-slate-700">${Number(item.unitPrice).toFixed(2)}</td>
                  <td className="py-3 text-right font-bold text-slate-900">${Number(item.lineTotal).toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Financial Calculation Summary */}
        <div className="flex justify-end border-t border-slate-200 pt-4">
          <div className="w-64 space-y-2 text-xs">
            <div className="flex justify-between text-slate-600">
              <span>Subtotal:</span>
              <span className="font-semibold text-slate-900">${Number(invoice.subtotal).toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-slate-600">
              <span>Tax ({Number(invoice.taxRate)}%):</span>
              <span className="font-semibold text-slate-900">${Number(invoice.taxAmount).toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-base font-black text-slate-900 border-t-2 border-slate-900 pt-2">
              <span>Total Amount:</span>
              <span>${Number(invoice.totalAmount).toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-slate-600">
              <span>Amount Paid:</span>
              <span className="font-semibold text-emerald-600">-${Number(invoice.paidAmount).toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-sm font-bold border-t border-slate-200 pt-2 text-rose-600">
              <span>Balance Due:</span>
              <span>${Number(invoice.balanceDue).toFixed(2)}</span>
            </div>
          </div>
        </div>

        {/* Footer Notes */}
        <div className="mt-8 pt-6 border-t border-slate-100 text-[11px] text-slate-400">
          <p className="font-bold text-slate-600 mb-1">Payment Instructions:</p>
          <p>Please send wire/ACH transfers to Silicon Federal Bank, Routing: 071000288, Account: 9884210081.</p>
          <p className="mt-1">Thank you for your business. For billing inquiries, contact accounting@ordercraft.com.</p>
        </div>
      </div>
    </Modal>
  );
};

export default InvoicePrintModal;
