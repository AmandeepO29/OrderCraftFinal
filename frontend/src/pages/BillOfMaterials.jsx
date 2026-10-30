import React, { useEffect, useState } from 'react';
import { FileCode2, Plus, Search, Edit2, Trash2, CheckCircle2, AlertTriangle, Layers, Calculator, ShieldCheck } from 'lucide-react';
import api from '../api/client';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Pagination } from '../components/Pagination';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export const BillOfMaterials = () => {
  const [boms, setBoms] = useState([]);
  const [productsWithoutBom, setProductsWithoutBom] = useState([]);
  const [rawMaterials, setRawMaterials] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);

  // Form modal
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingBom, setEditingBom] = useState(null);
  const [formData, setFormData] = useState({
    productId: '',
    bomCode: '',
    version: '1.0',
    description: '',
    items: [],
  });

  // Availability check modal
  const [checkModalOpen, setCheckModalOpen] = useState(false);
  const [checkResult, setCheckResult] = useState(null);
  const [checkQuantity, setCheckQuantity] = useState(1);
  const [checkingBom, setCheckingBom] = useState(null);
  const [checkingLoading, setCheckingLoading] = useState(false);

  // Delete
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const { isManager, isAdmin } = useAuth();
  const toast = useToast();

  const fetchBoms = async (page = 0, query = searchQuery) => {
    setLoading(true);
    try {
      const res = await api.get('/boms', {
        params: { page, size: 10, query: query || undefined },
      });
      setBoms(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalItems(res.data.totalElements);
      setCurrentPage(page);
    } catch (err) {
      toast.error('Failed to load Bill of Materials.');
    } finally {
      setLoading(false);
    }
  };

  const fetchMeta = async () => {
    try {
      const [prodsRes, matsRes] = await Promise.all([
        api.get('/products/active'),
        api.get('/raw-materials/active'),
      ]);
      setProductsWithoutBom(prodsRes.data);
      setRawMaterials(matsRes.data);
    } catch (err) {}
  };

  useEffect(() => {
    fetchBoms(0, searchQuery);
    fetchMeta();
  }, [searchQuery]);

  const handleOpenForm = (bom = null) => {
    if (bom) {
      setEditingBom(bom);
      setFormData({
        productId: bom.productId,
        bomCode: bom.bomCode,
        version: bom.version,
        description: bom.description || '',
        items: bom.items.map((it) => ({
          rawMaterialId: it.rawMaterialId,
          quantityRequired: it.quantityRequired,
          unitOfMeasure: it.unitOfMeasure,
          scrapPercentage: it.scrapPercentage || 0,
        })),
      });
    } else {
      setEditingBom(null);
      setFormData({
        productId: productsWithoutBom[0]?.id || '',
        bomCode: '',
        version: '1.0',
        description: '',
        items: [{ rawMaterialId: rawMaterials[0]?.id || '', quantityRequired: 1, unitOfMeasure: rawMaterials[0]?.unitOfMeasure || 'kg', scrapPercentage: 0 }],
      });
    }
    setIsFormOpen(true);
  };

  const handleAddItemRow = () => {
    setFormData({
      ...formData,
      items: [
        ...formData.items,
        {
          rawMaterialId: rawMaterials[0]?.id || '',
          quantityRequired: 1,
          unitOfMeasure: rawMaterials[0]?.unitOfMeasure || 'kg',
          scrapPercentage: 0,
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
      const selected = rawMaterials.find((rm) => rm.id === Number(value));
      if (selected) {
        updated[index].unitOfMeasure = selected.unitOfMeasure;
      }
    }
    setFormData({ ...formData, items: updated });
  };

  // Live estimated formulation cost calculation
  const calculatedEstimatedCost = formData.items.reduce((sum, item) => {
    const mat = rawMaterials.find((m) => m.id === Number(item.rawMaterialId));
    if (!mat) return sum;
    const qty = Number(item.quantityRequired) || 0;
    const scrap = Number(item.scrapPercentage) || 0;
    const cost = Number(mat.unitCost) || 0;
    return sum + cost * qty * (1 + scrap / 100);
  }, 0);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (formData.items.length === 0) {
      toast.error('BOM recipe must contain at least one raw material.');
      return;
    }

    try {
      if (editingBom) {
        await api.put(`/boms/${editingBom.id}`, formData);
        toast.success('BOM formulation updated successfully!');
      } else {
        await api.post('/boms', formData);
        toast.success('BOM formulation created successfully!');
      }
      setIsFormOpen(false);
      fetchBoms(currentPage);
      fetchMeta();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save BOM recipe.');
    }
  };

  const handleCheckAvailability = async (bom, qty = 1) => {
    setCheckingBom(bom);
    setCheckQuantity(qty);
    setCheckModalOpen(true);
    setCheckingLoading(true);
    try {
      const res = await api.get('/boms/check-availability', {
        params: { productId: bom.productId, quantity: qty },
      });
      setCheckResult(res.data);
    } catch (err) {
      toast.error('Failed to verify inventory availability.');
    } finally {
      setCheckingLoading(false);
    }
  };

  const handleDelete = async () => {
    try {
      await api.delete(`/boms/${deletingId}`);
      toast.success('BOM deleted successfully.');
      setDeleteOpen(false);
      fetchBoms(currentPage);
      fetchMeta();
    } catch (err) {
      toast.error('Failed to delete BOM.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-100">Bill of Materials (BOM) Formulations</h1>
          <p className="text-xs text-slate-400 mt-0.5">Manufacturing recipes, raw material consumption ratios, and cost engineering</p>
        </div>
        {isManager && (
          <button
            onClick={() => handleOpenForm()}
            className="px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white text-xs font-semibold rounded-xl flex items-center gap-2 shadow-lg shadow-brand-600/20 transition"
          >
            <Plus className="w-4 h-4" /> Create BOM Recipe
          </button>
        )}
      </div>

      {/* Search */}
      <div className="flex items-center gap-4 bg-slate-900/60 p-3 rounded-2xl border border-slate-800">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search by BOM code, product name, or product SKU..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-brand-500 transition"
          />
        </div>
      </div>

      {/* BOM Cards/Table */}
      <div className="space-y-4">
        {loading && boms.length === 0 ? (
          <div className="text-center py-12 text-slate-400 rounded-2xl bg-slate-900/80 border border-slate-800">
            <div className="w-6 h-6 border-2 border-brand-500/20 border-t-brand-500 rounded-full animate-spin mx-auto mb-2" />
            Loading BOM formulations...
          </div>
        ) : boms.length === 0 ? (
          <div className="text-center py-12 text-slate-400 rounded-2xl bg-slate-900/80 border border-slate-800">
            No Bill of Materials found. Click "Create BOM Recipe" to configure one.
          </div>
        ) : (
          boms.map((bom) => (
            <div
              key={bom.id}
              className="rounded-2xl bg-slate-900/80 border border-slate-800 p-5 shadow-xl transition hover:border-slate-700 space-y-4"
            >
              {/* Card top */}
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-b border-slate-800/80 pb-3">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center text-brand-400">
                    <FileCode2 className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="text-base font-bold text-slate-100">{bom.productName}</h3>
                      <span className="font-mono text-xs text-brand-400 font-semibold">({bom.productSku})</span>
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                        v{bom.version}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 font-mono mt-0.5">BOM Code: {bom.bomCode}</p>
                  </div>
                </div>

                <div className="flex items-center gap-4">
                  <div className="text-right">
                    <p className="text-[10px] uppercase font-bold text-slate-400">Estimated Unit Cost</p>
                    <p className="text-base font-mono font-bold text-emerald-400">
                      ${Number(bom.totalEstimatedCost).toFixed(2)}
                    </p>
                  </div>

                  <button
                    onClick={() => handleCheckAvailability(bom, 1)}
                    className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-brand-300 border border-slate-700 text-xs font-semibold flex items-center gap-1.5 transition"
                  >
                    <ShieldCheck className="w-3.5 h-3.5 text-brand-400" />
                    Verify Stock
                  </button>

                  {isManager && (
                    <button
                      onClick={() => handleOpenForm(bom)}
                      className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                  )}
                  {isAdmin && (
                    <button
                      onClick={() => {
                        setDeletingId(bom.id);
                        setDeleteOpen(true);
                      }}
                      className="p-2 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </div>

              {/* Items Table */}
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="text-slate-400 uppercase font-semibold text-[10px] tracking-wider border-b border-slate-800/60 pb-2">
                      <th className="py-2">Raw Material</th>
                      <th className="py-2">Code</th>
                      <th className="py-2 text-right">Required Qty</th>
                      <th className="py-2 text-right">Unit Cost</th>
                      <th className="py-2 text-right">Scrap %</th>
                      <th className="py-2 text-right">Total Item Cost</th>
                      <th className="py-2 text-center">Stock Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/40 text-slate-300">
                    {bom.items?.map((item) => (
                      <tr key={item.id}>
                        <td className="py-2.5 font-medium text-slate-200">{item.materialName}</td>
                        <td className="py-2.5 font-mono text-slate-400">{item.materialCode}</td>
                        <td className="py-2.5 text-right font-mono font-semibold">
                          {item.quantityRequired} {item.unitOfMeasure}
                        </td>
                        <td className="py-2.5 text-right font-mono text-slate-400">
                          ${Number(item.unitCost).toFixed(2)}
                        </td>
                        <td className="py-2.5 text-right font-mono text-slate-400">
                          {item.scrapPercentage > 0 ? `${item.scrapPercentage}%` : '0%'}
                        </td>
                        <td className="py-2.5 text-right font-mono font-bold text-slate-100">
                          ${Number(item.itemCost).toFixed(2)}
                        </td>
                        <td className="py-2.5 text-center">
                          {item.inStock ? (
                            <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-400">
                              <CheckCircle2 className="w-3 h-3" /> In Stock ({item.currentStock})
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-rose-400">
                              <AlertTriangle className="w-3 h-3" /> Low ({item.currentStock})
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ))
        )}

        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          totalItems={totalItems}
          pageSize={10}
          onPageChange={(page) => fetchBoms(page)}
        />
      </div>

      {/* BOM Form Modal */}
      <Modal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        title={editingBom ? `Edit BOM: ${editingBom.bomCode}` : 'Create Bill of Materials (BOM)'}
        maxWidth="max-w-4xl"
      >
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Target Product *</label>
              {editingBom ? (
                <input
                  type="text"
                  disabled
                  value={`${editingBom.productName} (${editingBom.productSku})`}
                  className="w-full px-3 py-2 bg-slate-950/60 border border-slate-800 rounded-xl text-xs text-slate-400"
                />
              ) : (
                <select
                  required
                  value={formData.productId}
                  onChange={(e) => setFormData({ ...formData, productId: Number(e.target.value) })}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
                >
                  {productsWithoutBom.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.sku})
                    </option>
                  ))}
                </select>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">BOM Code</label>
              <input
                type="text"
                placeholder="Auto-generated if left empty"
                value={formData.bomCode}
                onChange={(e) => setFormData({ ...formData, bomCode: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 uppercase font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Recipe Version</label>
              <input
                type="text"
                value={formData.version}
                onChange={(e) => setFormData({ ...formData, version: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Assembly Notes & Guidelines</label>
            <input
              type="text"
              placeholder="e.g. Standard hydraulic tolerance specs, assembly torque 45Nm"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
            />
          </div>

          {/* Dynamic Item Rows */}
          <div className="space-y-3 pt-3 border-t border-slate-800">
            <div className="flex items-center justify-between">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300">
                Raw Material Composition Ratio
              </h4>
              <button
                type="button"
                onClick={handleAddItemRow}
                className="px-3 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-brand-400 text-xs font-semibold flex items-center gap-1 transition"
              >
                <Plus className="w-3.5 h-3.5" /> Add Material
              </button>
            </div>

            <div className="space-y-2.5 max-h-60 overflow-y-auto pr-1">
              {formData.items.map((row, idx) => {
                const selectedMat = rawMaterials.find((m) => m.id === Number(row.rawMaterialId));
                const itemLineCost = selectedMat
                  ? selectedMat.unitCost * row.quantityRequired * (1 + (row.scrapPercentage || 0) / 100)
                  : 0;

                return (
                  <div
                    key={idx}
                    className="p-3 rounded-xl bg-slate-950/80 border border-slate-800 flex items-center gap-3 text-xs"
                  >
                    <div className="flex-1">
                      <select
                        value={row.rawMaterialId}
                        onChange={(e) => handleItemChange(idx, 'rawMaterialId', e.target.value)}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-slate-100"
                      >
                        {rawMaterials.map((rm) => (
                          <option key={rm.id} value={rm.id}>
                            {rm.name} (${rm.unitCost}/{rm.unitOfMeasure})
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="w-24">
                      <input
                        type="number"
                        step="0.0001"
                        placeholder="Qty"
                        value={row.quantityRequired}
                        onChange={(e) => handleItemChange(idx, 'quantityRequired', parseFloat(e.target.value) || 0)}
                        className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-slate-100 font-mono text-right"
                      />
                    </div>

                    <div className="w-16 font-mono text-slate-400 text-center">{row.unitOfMeasure}</div>

                    <div className="w-20">
                      <input
                        type="number"
                        step="0.5"
                        placeholder="Scrap %"
                        value={row.scrapPercentage}
                        onChange={(e) => handleItemChange(idx, 'scrapPercentage', parseFloat(e.target.value) || 0)}
                        className="w-full px-2 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-slate-100 font-mono text-right"
                      />
                    </div>

                    <div className="w-24 text-right font-mono font-bold text-emerald-400">
                      ${Number(itemLineCost).toFixed(2)}
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

          {/* Cost Footer */}
          <div className="flex items-center justify-between p-3.5 rounded-xl bg-slate-950 border border-slate-800">
            <div className="flex items-center gap-2 text-xs text-slate-400">
              <Calculator className="w-4 h-4 text-brand-400" />
              <span>Calculated Estimated Cost per Manufactured Unit:</span>
            </div>
            <div className="text-lg font-black font-mono text-emerald-400">
              ${Number(calculatedEstimatedCost).toFixed(2)}
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
              {editingBom ? 'Update Recipe' : 'Save BOM Formulation'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Stock Readiness Verification Modal */}
      <Modal
        isOpen={checkModalOpen}
        onClose={() => setCheckModalOpen(false)}
        title={`Inventory Readiness Check: ${checkingBom?.productName || ''}`}
        maxWidth="max-w-2xl"
      >
        <div className="space-y-4">
          <div className="flex items-center gap-3 p-3 rounded-xl bg-slate-950 border border-slate-800">
            <label className="text-xs font-semibold text-slate-300">Simulate Production Batch Size:</label>
            <input
              type="number"
              min="1"
              value={checkQuantity}
              onChange={(e) => {
                const val = parseInt(e.target.value) || 1;
                handleCheckAvailability(checkingBom, val);
              }}
              className="w-24 px-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs font-mono font-bold text-white text-center"
            />
            <span className="text-xs text-slate-400">units</span>
          </div>

          {checkingLoading ? (
            <div className="py-8 text-center text-slate-400 text-xs">Simulating BOM material availability...</div>
          ) : checkResult ? (
            <div className="space-y-4">
              <div
                className={`p-4 rounded-xl border flex items-center gap-3 ${
                  checkResult.sufficient
                    ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-300'
                    : 'bg-rose-500/10 border-rose-500/20 text-rose-300'
                }`}
              >
                {checkResult.sufficient ? (
                  <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                ) : (
                  <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0" />
                )}
                <div>
                  <p className="text-xs font-bold">
                    {checkResult.sufficient
                      ? 'Stock Adequate for Production!'
                      : 'Insufficient Raw Materials to Fulfill Batch'}
                  </p>
                  <p className="text-[11px] opacity-80 mt-0.5">
                    {checkResult.sufficient
                      ? `Inventory has sufficient raw materials to immediately start manufacturing ${checkQuantity} units.`
                      : 'Generate purchase orders to procure the deficit quantities before initiating production.'}
                  </p>
                </div>
              </div>

              <div className="rounded-xl border border-slate-800 overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="bg-slate-950 text-slate-400 text-[10px] uppercase font-semibold border-b border-slate-800">
                      <th className="py-2.5 px-3">Material</th>
                      <th className="py-2.5 px-3 text-right">Required</th>
                      <th className="py-2.5 px-3 text-right">Available in Stock</th>
                      <th className="py-2.5 px-3 text-center">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {checkResult.requirements?.map((req) => (
                      <tr key={req.rawMaterialId} className="hover:bg-slate-850/40">
                        <td className="py-2.5 px-3 font-medium text-slate-200">{req.materialName}</td>
                        <td className="py-2.5 px-3 text-right font-mono font-semibold text-slate-100">
                          {req.requiredQuantity} {req.unitOfMeasure}
                        </td>
                        <td className="py-2.5 px-3 text-right font-mono text-slate-300">
                          {req.availableStock} {req.unitOfMeasure}
                        </td>
                        <td className="py-2.5 px-3 text-center">
                          {req.isAvailable ? (
                            <span className="text-emerald-400 font-semibold text-[11px] flex items-center justify-center gap-1">
                              <CheckCircle2 className="w-3 h-3" /> Ready
                            </span>
                          ) : (
                            <span className="text-rose-400 font-semibold text-[11px] flex items-center justify-center gap-1">
                              <AlertTriangle className="w-3 h-3" /> Deficit
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : null}
        </div>
      </Modal>

      {/* Delete Dialog */}
      <ConfirmDialog
        isOpen={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={handleDelete}
        title="Delete BOM Recipe"
        message="Are you sure you want to delete this Bill of Materials recipe?"
        confirmText="Yes, Delete"
        isDanger={true}
      />
    </div>
  );
};
