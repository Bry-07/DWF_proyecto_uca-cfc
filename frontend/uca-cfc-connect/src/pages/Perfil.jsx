import { useAuth } from '../hooks/useAuth'
import { ROLE_LABEL } from '../utils/navigation'

// Perfil de la sesión (/auth/me). El backend no permite al CLIENTE leer /clientes/{id}, por eso solo se muestran los datos de la cuenta.
export default function Perfil() {
  const { user, role, logout } = useAuth()

  return (
    <div className="content">
      <div className="page-head">
        <div>
          <h1>Mi perfil</h1>
          <p>Datos de tu cuenta en UCA · CFC Connect</p>
        </div>
        <button type="button" className="btn-outline" onClick={logout}>Cerrar sesión</button>
      </div>

      <div className="panel" style={{ maxWidth: 640, marginBottom: 20 }}>
        <h2>Cuenta</h2>
        <p className="hint">Los permisos los define el rol asignado por la administración.</p>
        <dl className="kv">
          <dt>Nombre</dt><dd>{user?.nombre ?? '—'}</dd>
          <dt>Correo</dt><dd>{user?.email ?? '—'}</dd>
          <dt>Rol</dt><dd>{ROLE_LABEL[role] ?? role}</dd>
        </dl>
      </div>

    </div>
  )
}
