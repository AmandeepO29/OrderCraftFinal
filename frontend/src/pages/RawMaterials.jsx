import React, { useEffect, useState } from 'react';
import { Layers, Plus, Search, Edit2, Trash2, AlertTriangle, CheckCircle2, Truck } from 'lucide-react';
import api from '../api/client';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Pagination } from '../components/Pagination';
import { Badge } from '../components/Badge';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export const RawMaterials = () => {
  const [materials, setMaterials] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterLowStock, setFilterLowStock] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);

  // Form modal
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingMaterial, setEditingMaterial] = useState(null);
  const [formData, setFormData] = useState({
    materialCode: '',
    name: '',
    description: '',
    unitOfMeasure: 'kg',
    unitCost: '',
    minimumStockLevel: '',
    reorderQuantity: '',
    preferredSupplierId: '',
    initialStock: 0,
  });

  // Delete
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const { isManager, isAdmin } = useAuth();
  const toast = useToast();

  const fetchMaterials = async (page = 0, query = searchQuery) => {
    setLoading(true);
    try {
      const res = await api.get('/raw-materials', {
        params: { page, size: 10, query: query || undefined },
      });
      setMaterials(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalItems(res.data.totalElements);
      setCurrentPage(page);
    } catch (err) {
      toast.error('Failed to load raw materials.');
    } finally {
      setLoading(false);
    }
  };

  const fetchSuppliers = async () => {
    try {
      const res = await api.get('/suppliers', { params: { page: 0, size: 100 } });
      setSuppliers(res.data.content);
    } catch (err) {}
  };

  useEffect(() => {
    fetchMaterials(0, searchQuery);
    fetchSuppliers();
  }, [searchQuery]);

  const handleOpenForm = (mat = null) => {
    if (mat) {
      setEditingMaterial(mat);
      setFormData({
        materialCode: mat.materialCode,
        name: mat.name,
        description: mat.description || '',
        unitOfMeasure: mat.unitOfMeasure,
        unitCost: mat.unitCost,
        minimumStockLevel: mat.minimumStockLevel,
        reorderQuantity: mat.reorderQuantity,
        preferredSupplierId: mat.preferredSupplierId || '',
        initialStock: 0,
      });
    } else {
      setEditingMaterial(null);
      setFormData({
        materialCode: '',
        name: '',
        description: '',
        unitOfMeasure: 'kg',
        unitCost: '',
        minimumStockLevel: '',
        reorderQuantity: '',
        preferredSupplierId: suppliers[0]?.id || '',
        initialStock: 0,
      });
    }
    setIsFormOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingMaterial) {
        await api.put(`/raw-materials/${editingMaterial.id}`, formData);
        toast.success('Raw material updated successfully!');
      } else {
        await api.post('/raw-materials', formData);
        toast.success('Raw material registered with active stock ledger!');
      }
      setIsFormOpen(false);
      fetchMaterials(currentPage);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save raw material.');
    }
  };

  const handleDelete = async () => {
    try {
      await api.delete(`/raw-materials/${deletingId}`);
      toast.success('Raw material deleted successfully.');
      setDeleteOpen(false);
      fetchMaterials(currentPage);
    } catch (err) {
      toast.error('Failed to delete raw material.');
    }
  };

  const displayedMaterials = filterLowStock
    ? materials.filter((m) => m.lowStock)
    : materials;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-100">Raw Material Registry</h1>
          <p className="text-xs text-slate-400 mt-0.5">Track procurement specs, reorder thresholds, and unit costs</p>
        </div>
        {isManager && (
          <button
            onClick={() => handleOpenForm()}
            className="px-4 py-2 bg-brand-600 hover:bg-brand-500 text-white text-xs font-semibold rounded-xl flex items-center gap-2 shadow-lg shadow-brand-600/20 transition"
          >
            <Plus className="w-4 h-4" /> Add Raw Material
          </button>
        )}
      </div>

      {/* Filter Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 bg-slate-900/60 p-3 rounded-2xl border border-slate-800">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search by material code, name, description..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-brand-500 transition"
          />
        </div>

        <button
          onClick={() => setFilterLowStock(!filterLowStock)}
          className={`px-3 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 border transition ${
            filterLowStock
              ? 'bg-rose-500/20 text-rose-300 border-rose-500/40'
              : 'bg-slate-950/80 text-slate-400 border-slate-800 hover:text-white'
          }`}
        >
          <AlertTriangle className="w-3.5 h-3.5 text-rose-400" />
          Low Stock Only
        </button>
      </div>

      {/* Materials Table */}
      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-950/50 text-slate-400 font-semibold uppercase tracking-wider">
                <th className="py-3 px-4">Material Code</th>
                <th className="py-3 px-4">Name & Specs</th>
                <th className="py-3 px-4">Unit of Measure</th>
                <th className="py-3 px-4 text-right">Unit Cost</th>
                <th className="py-3 px-4 text-center">Current Stock</th>
                <th className="py-3 px-4 text-center">Safety Level</th>
                <th className="py-3 px-4">Preferred Supplier</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300">
              {loading && materials.length === 0 ? (
                <tr>
                  <td colSpan="8" className="text-center py-12 text-slate-400">
                    <div className="w-6 h-6 border-2 border-brand-500/20 border-t-brand-500 rounded-full animate-spin mx-auto mb-2" />
                    Loading materials...
                  </td>
                </tr>
              ) : displayedMaterials.length === 0 ? (
                <tr>
                  <td colSpan="8" className="text-center py-12 text-slate-400">
                    No raw materials found matching filters.
                  </td>
                </tr>
              ) : (
                displayedMaterials.map((m) => (
                  <tr key={m.id} className="hover:bg-slate-850/50 transition">
                    <td className="py-3.5 px-4 font-mono font-bold text-brand-400">{m.materialCode}</td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-slate-100">{m.name}</div>
                      <div className="text-[11px] text-slate-400 line-clamp-1">{m.description}</div>
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="px-2 py-0.5 rounded-md bg-slate-800 text-slate-300 font-mono text-[11px]">
                        {m.unitOfMeasure}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono font-semibold text-emerald-400">
                      ${Number(m.unitCost).toFixed(2)}
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <div className="flex items-center justify-center gap-1.5 font-mono font-bold">
                        <span className={m.lowStock ? 'text-rose-400' : 'text-slate-200'}>
                          {m.currentStock} {m.unitOfMeasure}
                        </span>
                        {m.lowStock && <AlertTriangle className="w-3.5 h-3.5 text-rose-400 animate-pulse" />}
                      </div>
                    </td>
                    <td className="py-3.5 px-4 text-center font-mono text-slate-400">
                      {m.minimumStockLevel} {m.unitOfMeasure}
                    </td>
                    <td className="py-3.5 px-4">
                      {m.preferredSupplierName ? (
                        <div className="flex items-center gap-1.5 text-slate-300">
                          <Truck className="w-3.5 h-3.5 text-slate-500" />
                          <span>{m.preferredSupplierName}</span>
                        </div>
                      ) : (
                        <span className="text-slate-500">—</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-right space-x-2">
                      {isManager && (
                        <button
                          onClick={() => handleOpenForm(m)}
                          title="Edit Material"
                          className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                      )}
                      {isAdmin && (
                        <button
                          onClick={() => {
                            setDeletingId(m.id);
                            setDeleteOpen(true);
                          }}
                          title="Delete Material"
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
          onPageChange={(page) => fetchMaterials(page)}
        />
      </div>

      {/* Material Modal Form */}
      <Modal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        title={editingMaterial ? `Edit Material: ${editingMaterial.materialCode}` : 'Register New Raw Material'}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Material Code *</label>
              <input
                type="text"
                required
                placeholder="e.g. RM-ALUM-6061"
                value={formData.materialCode}
                onChange={(e) => setFormData({ ...formData, materialCode: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 uppercase font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Material Name *</label>
              <input
                type="text"
                required
                placeholder="e.g. Stainless Steel 304 Sheet"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Unit of Measure *</label>
              <input
                type="text"
                required
                placeholder="kg, meter, pcs, liters"
                value={formData.unitOfMeasure}
                onChange={(e) => setFormData({ ...formData, unitOfMeasure: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Unit Cost ($) *</label>
              <input
                type="number"
                step="0.01"
                required
                placeholder="0.00"
                value={formData.unitCost}
                onChange={(e) => setFormData({ ...formData, unitCost: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Minimum Safety Stock *</label>
              <input
                type="number"
                step="0.01"
                required
                placeholder="50.00"
                value={formData.minimumStockLevel}
                onChange={(e) => setFormData({ ...formData, minimumStockLevel: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Reorder Quantity *</label>
              <input
                type="number"
                step="0.01"
                required
                placeholder="200.00"
                value={formData.reorderQuantity}
                onChange={(e) => setFormData({ ...formData, reorderQuantity: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Preferred Supplier</label>
              <select
                value={formData.preferredSupplierId}
                onChange={(e) => setFormData({ ...formData, preferredSupplierId: e.target.value })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
              >
                <option value="">None Selected</option>
                {suppliers.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.companyName} ({s.supplierCode})
                  </option>
                ))}
              </select>
            </div>
          </div>

          {!editingMaterial && (
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Opening Stock Quantity</label>
              <input
                type="number"
                step="0.01"
                value={formData.initialStock}
                onChange={(e) => setFormData({ ...formData, initialStock: parseFloat(e.target.value) || 0 })}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 font-mono focus:outline-none focus:border-brand-500 transition"
              />
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">Material Description & Specification</label>
            <textarea
              rows="3"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-brand-500 transition"
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
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
              {editingMaterial ? 'Update Material' : 'Save Material'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Delete Dialog */}
      <ConfirmDialog
        isOpen={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={handleDelete}
        title="Delete Raw Material"
        message="Are you sure you want to deactivate this raw material? BOM recipes depending on it should be reviewed."
        confirmText="Yes, Deactivate"
        isDanger={true}
      />
    </div>
  );
};
