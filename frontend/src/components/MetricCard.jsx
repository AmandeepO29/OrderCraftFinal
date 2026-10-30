import React from 'react';

export const MetricCard = ({ title, value, subtitle, icon: Icon, trend, color = 'brand' }) => {
  const colorMap = {
    brand: 'text-brand-400 bg-brand-500/10 border-brand-500/20 group-hover:border-brand-500/40',
    emerald: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20 group-hover:border-emerald-500/40',
    amber: 'text-amber-400 bg-amber-500/10 border-amber-500/20 group-hover:border-amber-500/40',
    purple: 'text-purple-400 bg-purple-500/10 border-purple-500/20 group-hover:border-purple-500/40',
    rose: 'text-rose-400 bg-rose-500/10 border-rose-500/20 group-hover:border-rose-500/40',
    sky: 'text-sky-400 bg-sky-500/10 border-sky-500/20 group-hover:border-sky-500/40',
  };

  const activeColor = colorMap[color] || colorMap.brand;

  return (
    <div className="group relative overflow-hidden rounded-2xl bg-slate-900/80 border border-slate-800 p-5 shadow-xl transition-all duration-300 hover:-translate-y-0.5 hover:shadow-2xl">
      <div className="flex items-start justify-between">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">{title}</p>
          <h4 className="text-2xl font-bold tracking-tight text-white">{value}</h4>
        </div>
        <div className={`p-3 rounded-xl border transition-colors ${activeColor}`}>
          <Icon className="w-5 h-5" />
        </div>
      </div>

      {(subtitle || trend) && (
        <div className="mt-4 flex items-center justify-between text-xs">
          {subtitle && <span className="text-slate-400">{subtitle}</span>}
          {trend && (
            <span
              className={`font-semibold flex items-center gap-1 ${
                trend.isPositive ? 'text-emerald-400' : 'text-rose-400'
              }`}
            >
              {trend.isPositive ? '↑' : '↓'} {trend.text}
            </span>
          )}
        </div>
      )}

      {/* Ambient gradient */}
      <div className="absolute -bottom-8 -right-8 w-24 h-24 bg-brand-500/5 rounded-full blur-2xl group-hover:bg-brand-500/10 transition-colors pointer-events-none" />
    </div>
  );
};

export default MetricCard;
