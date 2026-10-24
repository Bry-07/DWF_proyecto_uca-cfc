import { useAuth } from '../hooks/useAuth'
import { initials } from '../utils/format'
import Icon from './Icon'

// `search` es opcional: solo las pantallas con buscador (catálogo, pagos...) lo activan.
export default function Topbar({ crumb, search }) {
  const { user } = useAuth()
  return (
    <header className="topbar">
      <div className="crumb">
        CFC Connect / <b>{crumb}</b>
      </div>
      {search && (
        <label className="search">
          <Icon name="search" />
          <input value={search.value} onChange={(e) => search.onChange(e.target.value)} placeholder={search.placeholder} />
        </label>
      )}
      <div className="topbar-right">
        <button type="button" className="icon-btn" aria-label="Notificaciones">
          <Icon name="bell" />
        </button>
        <div className="role-chip" title={user?.nombre}>
          <div className="avatar">{initials(user?.nombre)}</div>
        </div>
      </div>
    </header>
  )
}
