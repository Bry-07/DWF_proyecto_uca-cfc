import { NavLink } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { initials } from '../utils/format'
import { navForRole, ROLE_LABEL } from '../utils/navigation'
import Icon from './Icon'

export default function Sidebar() {
  const { user, role, logout } = useAuth()
  const groups = navForRole(role)

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="mark">CFC</div>
        <div className="brand-name">
          UCA · CFC Connect<small>Centro de Formación</small>
        </div>
      </div>

      <NavLink to="/" end className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
        <Icon name="inicio" />
        Inicio
      </NavLink>

      {groups.map((g) => (
        <div key={g.label}>
          <div className="nav-group-label">{g.label}</div>
          {g.items.map((item) => (
            <NavLink key={item.path} to={item.path} className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
              <Icon name={item.icon} />
              {role === 'CLIENTE' && item.clientLabel ? item.clientLabel : item.label}
            </NavLink>
          ))}
        </div>
      ))}

      <div className="sidebar-foot">
        <NavLink to="/perfil" className="role-chip" title={`${user?.nombre ?? ''} · ${user?.email ?? ''}`} style={{ textDecoration: 'none', color: 'inherit' }}>
          <div className="avatar">{initials(user?.nombre)}</div>
          {ROLE_LABEL[role] ?? role}
        </NavLink>
        <button type="button" className="nav-item" style={{ width: '100%', background: 'none', border: 'none', borderLeft: '2px solid transparent', marginTop: 10, marginBottom: 0 }} onClick={logout}>
          <Icon name="logout" />
          Cerrar sesión
        </button>
      </div>
    </aside>
  )
}
