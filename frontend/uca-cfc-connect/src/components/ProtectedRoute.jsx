import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import Loading from './Loading'

// Sin `roles`: basta con estar autenticado. Con `roles`: además el rol debe estar permitido.
// Es solo UX: el backend sigue siendo la autoridad (403).
export default function ProtectedRoute({ roles }) {
  const { isAuthenticated, role, loading } = useAuth()
  const location = useLocation()

  if (loading) return <Loading fullscreen text="Verificando sesión…" />
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location }} />
  if (roles && !roles.includes(role)) return <Navigate to="/403" replace />
  return <Outlet />
}
