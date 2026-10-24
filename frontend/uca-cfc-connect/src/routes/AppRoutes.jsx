import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from '../components/ProtectedRoute'
import AppLayout from '../layouts/AppLayout'
import Recuperar from '../pages/auth/Recuperar'
import Registro from '../pages/auth/Registro'
import Restablecer from '../pages/auth/Restablecer'
import Administracion from '../pages/administracion/Administracion'
import Perfil from '../pages/Perfil'
import Agenda from '../pages/agenda/Agenda'
import Catalogo from '../pages/catalogo/Catalogo'
import Catering from '../pages/catering/Catering'
import Clientes from '../pages/clientes/Clientes'
import Cotizaciones from '../pages/cotizaciones/Cotizaciones'
import Inscripciones from '../pages/inscripciones/Inscripciones'
import Pagos from '../pages/pagos/Pagos'
import Recursos from '../pages/recursos/Recursos'
import Usuarios from '../pages/usuarios/Usuarios'
import ComingSoon from '../pages/ComingSoon'
import Dashboard from '../pages/Dashboard'
import Forbidden from '../pages/Forbidden'
import Login from '../pages/Login'
import NotFound from '../pages/NotFound'
import { ALL_ITEMS } from '../utils/navigation'

// Fase en la que se implementa cada módulo (solo para el marcador).
const PHASE = {
  '/agenda': 8, '/cursos': 5, '/clientes': 6, '/inscripciones': 6, '/cotizaciones': 7,
  '/espacios': 8, '/catering': 8, '/pagos': 9, '/usuarios': 10,
}

// Módulos ya implementados (el resto cae en <ComingSoon/>).
const MODULES = {
  '/agenda': <Agenda />,
  '/cursos': <Catalogo />,
  '/clientes': <Clientes />,
  '/inscripciones': <Inscripciones />,
  '/cotizaciones': <Cotizaciones />,
  '/espacios': <Recursos />,
  '/catering': <Catering />,
  '/pagos': <Pagos />,
  '/usuarios': <Usuarios />,
  '/catalogos': <Administracion />,
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/registro" element={<Registro />} />
      <Route path="/recuperar" element={<Recuperar />} />
      <Route path="/restablecer" element={<Restablecer />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="/403" element={<Forbidden />} />
          <Route path="/perfil" element={<Perfil />} />

          {/* Cada módulo se protege por rol según utils/navigation.js.
              Al implementar una fase, se reemplaza <ComingSoon/> por la página real. */}
          {ALL_ITEMS.map((item) => (
            <Route key={item.path} element={<ProtectedRoute roles={item.roles} />}>
              <Route path={`${item.path}/*`} element={MODULES[item.path] ?? <ComingSoon title={item.label} phase={PHASE[item.path]} />} />
            </Route>
          ))}

          <Route path="*" element={<NotFound />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
