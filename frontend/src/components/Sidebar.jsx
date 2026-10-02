import { NavLink } from 'react-router-dom';
import clsx from 'clsx';
import {
  LayoutDashboard, FolderLock, FileStack, Link2, ScrollText, Users, Bell, FileBarChart, ShieldCheck, X,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import { CAN_MANAGE_USERS, CAN_VIEW_AUDIT_LOGS } from '../utils/constants';

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/cases', label: 'Cases', icon: FolderLock },
  { to: '/evidence', label: 'Evidence', icon: FileStack },
  { to: '/chain-of-custody', label: 'Chain of Custody', icon: Link2 },
  { to: '/reports', label: 'Reports', icon: FileBarChart },
  { to: '/notifications', label: 'Notifications', icon: Bell, badgeKey: 'unread' },
  { to: '/audit-logs', label: 'Audit Logs', icon: ScrollText, roles: CAN_VIEW_AUDIT_LOGS },
  { to: '/users', label: 'User Management', icon: Users, roles: CAN_MANAGE_USERS },
];

export default function Sidebar({ mobileOpen, onCloseMobile }) {
  const { user, hasRole } = useAuth();
  const { unreadCount } = useNotifications();

  const visibleItems = NAV_ITEMS.filter((item) => !item.roles || (user && item.roles.includes(user.role)));

  return (
    <>
      {mobileOpen && (
        <div className="fixed inset-0 z-40 bg-navy-950/50 lg:hidden" onClick={onCloseMobile} />
      )}
      <aside
        className={clsx(
          'fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-navy-950 text-navy-100 transition-transform lg:static lg:translate-x-0',
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="flex items-center justify-between px-5 py-5">
          <div className="flex items-center gap-2">
            <ShieldCheck className="text-accent-400" size={24} />
            <span className="text-lg font-bold text-white">CaseLock</span>
          </div>
          <button className="text-navy-300 lg:hidden" onClick={onCloseMobile} aria-label="Close menu">
            <X size={20} />
          </button>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto px-3">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onCloseMobile}
              className={({ isActive }) =>
                clsx(
                  'flex items-center justify-between gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                  isActive ? 'bg-accent-600 text-white' : 'text-navy-200 hover:bg-navy-900 hover:text-white'
                )
              }
            >
              <span className="flex items-center gap-3">
                <item.icon size={17} />
                {item.label}
              </span>
              {item.badgeKey === 'unread' && unreadCount > 0 && (
                <span className="rounded-full bg-red-500 px-1.5 py-0.5 text-[10px] font-semibold text-white">
                  {unreadCount > 99 ? '99+' : unreadCount}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-navy-800 px-4 py-4 text-xs text-navy-400">
          CaseLock v1.0 &middot; Secure Evidence Platform
        </div>
      </aside>
    </>
  );
}
