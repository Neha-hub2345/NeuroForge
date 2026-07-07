import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { roleLabel } from '../utils/roles'

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: '◧' },
  { to: '/projects', label: 'Projects', icon: '◨' },
  { to: '/teams', label: 'Teams', icon: '◫' },
  { to: '/users', label: 'Users', icon: '◍' }
]

export default function AppLayout() {
  // 1. Correctly destructure from useAuth
  const { username, roles, logout } = useAuth()

  const handleLogout = () => {
    // 2. Just call logout(). Keycloak takes over and redirects the browser.
    logout()
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">NF</span>
          <div>
            <div className="brand-name">NeuroForge</div>
            <div className="brand-sub">Nexus · Milestone 1</div>
          </div>
        </div>

        <nav className="nav-list">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')}
            >
              <span className="nav-icon">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="user-chip">
            {/* 3. Updated all 'user?.username' to just 'username' */}
            <div className="avatar">{username?.[0]?.toUpperCase() || '?'}</div>
            <div>
              <div className="user-name">{username}</div>
              
              {/* 4. Pass the first role from the array into the formatter */}
              <div className="user-role">{roleLabel(roles?.[0]) || roles?.[0]}</div>
            </div>
          </div>
          <button className="btn-ghost" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </aside>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}