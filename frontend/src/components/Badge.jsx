import React from 'react';

const BADGE_STYLES = {
  // Order & Production Statuses
  PENDING: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  CONFIRMED: 'bg-sky-500/10 text-sky-400 border-sky-500/20',
  IN_PRODUCTION: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
  PLANNED: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  IN_PROGRESS: 'bg-purple-500/10 text-purple-400 border-purple-500/20',
  COMPLETED: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  CANCELLED: 'bg-rose-500/10 text-rose-400 border-rose-500/20',

  // Purchase Order Statuses
  DRAFT: 'bg-slate-500/10 text-slate-400 border-slate-500/20',
  ORDERED: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  RECEIVED: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',

  // Invoice Statuses
  UNPAID: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
  PARTIALLY_PAID: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  PAID: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  OVERDUE: 'bg-red-500/10 text-red-400 border-red-500/20',

  // Product Statuses
  ACTIVE: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  DISCONTINUED: 'bg-slate-500/10 text-slate-400 border-slate-500/20',

  // Roles
  ROLE_ADMIN: 'bg-rose-500/10 text-rose-300 border-rose-500/30 font-semibold',
  ROLE_MANAGER: 'bg-brand-500/10 text-brand-300 border-brand-500/30 font-semibold',
  ROLE_EMPLOYEE: 'bg-slate-500/10 text-slate-300 border-slate-500/30 font-semibold',

  // Stock
  LOW_STOCK: 'bg-rose-500/20 text-rose-400 border-rose-500/30 animate-pulse',
  IN_STOCK: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
};

export const Badge = ({ status, label, className = '' }) => {
  const normalized = status ? status.toUpperCase().replace(/\s+/g, '_') : 'DEFAULT';
  const style = BADGE_STYLES[normalized] || 'bg-slate-700/50 text-slate-300 border-slate-600';
  const display = label || (status ? status.replace(/_/g, ' ') : '');

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${style} ${className}`}
    >
      <span className="w-1.5 h-1.5 mr-1.5 rounded-full bg-current opacity-70"></span>
      {display}
    </span>
  );
};

export default Badge;
