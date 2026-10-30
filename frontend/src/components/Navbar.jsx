import React from 'react';
import { Menu, Bell, Shield, Sparkles } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Badge } from './Badge';

export const Navbar = ({ onMenuClick, title }) => {
  const { user } = useAuth();

  return (
    <header className="sticky top-0 z-30 h-16 bg-slate-950/70 backdrop-blur-md border-b border-slate-800/80 px-4 sm:px-8 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <button
          onClick={onMenuClick}
          className="p-2 -ml-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 lg:hidden transition"
        >
          <Menu className="w-5 h-5" />
        </button>
        <h2 className="text-lg font-bold text-slate-100 tracking-tight">{title}</h2>
      </div>

      <div className="flex items-center gap-3 sm:gap-4">
        {/* System environment pill */}
        <div className="hidden sm:flex items-center gap-2 px-3 py-1 rounded-full bg-slate-900 border border-slate-800 text-xs text-slate-400">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          <span>OrderCraft Core Engine: Online</span>
        </div>

        {/* User Role Badge */}
        {user?.roles?.[0] && (
          <Badge status={user.roles[0]} label={user.roles[0].replace('ROLE_', '')} />
        )}
      </div>
    </header>
  );
};

export default Navbar;
