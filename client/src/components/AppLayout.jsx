import { useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import { useTheme } from '../context/ThemeContext';
import { useIsMobile } from '../hooks/useMediaQuery';
import { initials } from '../utils/format';

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', icon: '◈' },
  { to: '/accounts', label: 'Accounts', icon: '◎' },
  { to: '/posts', label: 'Posts', icon: '▤' },
  { to: '/reports', label: 'Reports', icon: '▦' },
  { to: '/goals', label: 'Goals', icon: '◎' },
  { to: '/notifications', label: 'Notifications', icon: '◔', badge: true },
  { to: '/profile', label: 'Profile', icon: '◍' },
];

const TITLES = {
  '/dashboard': 'Dashboard',
  '/accounts': 'Connected Accounts',
  '/posts': 'Posts',
  '/reports': 'Analytics Reports',
  '/goals': 'Goals',
  '/notifications': 'Notifications',
  '/profile': 'Profile Settings',
};

export default function AppLayout() {
  const { user, logout } = useAuth();
  const { count } = useNotifications();
  const { isDark, toggle } = useTheme();
  const isMobile = useIsMobile();
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const title = TITLES[location.pathname] || 'Social Dashboard';

  async function handleLogout() {
    await logout();
    navigate('/login', { replace: true });
  }

  function closeMenu() {
    if (isMobile) setMenuOpen(false);
  }

  return (
    <div className="app-shell">
      <aside className={`sidebar${menuOpen ? ' open' : ''}`}>
        <div className="sidebar-brand">
          <div className="logo">◈</div>
          <span>Social Dashboard</span>
        </div>

        <nav className="sidebar-nav">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
              onClick={closeMenu}
            >
              <span className="nav-icon" aria-hidden="true">
                {item.icon}
              </span>
              <span>{item.label}</span>
              {item.badge && count > 0 && <span className="nav-badge">{count > 99 ? '99+' : count}</span>}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="user-chip">
            <div className="avatar" style={{ background: user?.avatarColor || 'var(--brand)' }}>
              {initials(user?.name)}
            </div>
            <div className="meta">
              <div className="name">{user?.name}</div>
              <div className="email">{user?.email}</div>
            </div>
            <button
              type="button"
              className="icon-button"
              onClick={handleLogout}
              title="Log out"
              aria-label="Log out"
            >
              ⎋
            </button>
          </div>
        </div>
      </aside>

      {menuOpen && isMobile && <button type="button" className="sidebar-scrim" onClick={() => setMenuOpen(false)} aria-label="Close menu" />}

      <div className="main-area">
        <header className="topbar">
          <button
            type="button"
            className="menu-toggle"
            onClick={() => setMenuOpen((open) => !open)}
            aria-label="Toggle navigation"
            aria-expanded={menuOpen}
          >
            ☰
          </button>
          <h1>{title}</h1>
          <div className="spacer" />
          <button
            type="button"
            className="icon-button"
            onClick={toggle}
            title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
            aria-label="Toggle colour theme"
            style={{ color: 'var(--text-muted)' }}
          >
            {isDark ? '☀' : '☾'}
          </button>
        </header>

        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
