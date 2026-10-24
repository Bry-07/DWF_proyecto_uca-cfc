import { useAuth } from '../hooks/useAuth'
import { ROLES } from '../utils/constants'
import { ROLE_LABEL } from '../utils/navigation'
import ClientDashboard from './dashboard/ClientDashboard'
import FinanceDashboard from './dashboard/FinanceDashboard'
import StaffDashboard from './dashboard/StaffDashboard'

// Inicio: cada rol ve su propio tablero.
export default function Dashboard() {
  const { user, role } = useAuth()

  return (
    <div className="content">
      <div className="page-head">
        <div>
          <h1>Hola, {user?.nombre?.split(' ')[0] ?? 'bienvenido'}</h1>
          <p>Sesión como {ROLE_LABEL[role] ?? role} · {user?.email}</p>
        </div>
      </div>

      {role === ROLES.CLIENTE && <ClientDashboard />}
      {role === ROLES.CONTABILIDAD && <FinanceDashboard />}
      {role === ROLES.ADMIN && <StaffDashboard canSeeFinance />}
      {role === ROLES.RECEPCIONISTA && <StaffDashboard />}
    </div>
  )
}
