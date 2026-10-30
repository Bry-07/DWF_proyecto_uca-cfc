import { ROLES } from './constants'

const { ADMIN, RECEPCIONISTA, CONTABILIDAD, CLIENTE } = ROLES

// Fuente única de la navegación. `roles` solo controla lo que se MUESTRA y
// protege la ruta en el cliente; la seguridad real la aplica el backend (403).
// Ajustable sin tocar componentes: basta editar esta tabla.
export const NAV_GROUPS = [
  {
    label: 'Operación',
    items: [
      { path: '/agenda', label: 'Agenda', icon: 'agenda', roles: [ADMIN, RECEPCIONISTA] },
      { path: '/cursos', label: 'Cursos y diplomados', icon: 'cursos', roles: [ADMIN, RECEPCIONISTA, CLIENTE] },
      { path: '/clientes', label: 'Clientes', icon: 'clientes', roles: [ADMIN, RECEPCIONISTA, CONTABILIDAD] },
      { path: '/inscripciones', label: 'Inscripciones', icon: 'inscripciones', roles: [ADMIN, RECEPCIONISTA, CLIENTE] },
      { path: '/cotizaciones', label: 'Cotizaciones', icon: 'cotizaciones', roles: [ADMIN, RECEPCIONISTA, CLIENTE] },
    ],
  },
  {
    label: 'Recursos',
    items: [
      { path: '/espacios', label: 'Espacios', clientLabel: 'Mis alquileres', icon: 'espacios', roles: [ADMIN, RECEPCIONISTA, CLIENTE] },
      { path: '/catering', label: 'Catering', clientLabel: 'Mis solicitudes de catering', icon: 'catering', roles: [ADMIN, RECEPCIONISTA, CLIENTE] },
      { path: '/pagos', label: 'Pagos', icon: 'pagos', roles: [ADMIN, RECEPCIONISTA, CONTABILIDAD, CLIENTE] },
    ],
  },
  {
    label: 'Sistema',
    items: [
      { path: '/usuarios', label: 'Usuarios y roles', icon: 'usuarios', roles: [ADMIN] },
      { path: '/catalogos', label: 'Catálogos base', icon: 'tools', roles: [ADMIN] },
    ],
  },
]

export const ALL_ITEMS = NAV_GROUPS.flatMap((g) => g.items)

export const canAccess = (item, role) => item.roles.includes(role)

// Grupos visibles para un rol (los grupos vacíos se omiten).
export const navForRole = (role) =>
  NAV_GROUPS.map((g) => ({ ...g, items: g.items.filter((i) => canAccess(i, role)) })).filter((g) => g.items.length)

export const findItem = (pathname) =>
  ALL_ITEMS.find((i) => pathname === i.path || pathname.startsWith(`${i.path}/`))

// Texto del chip del pie del sidebar (como en los mockups).
export const ROLE_LABEL = {
  ADMIN: 'Administración',
  RECEPCIONISTA: 'Recepción',
  CONTABILIDAD: 'Contabilidad',
  CLIENTE: 'Cliente',
}

// Destino tras iniciar sesión: la ruta pedida solo si el rol nuevo puede verla;
// si no (otra cuenta, /403, /login...), se va al inicio.
export function landingFor(role, from = '/') {
  if (!from || from === '/' || from === '/login' || from === '/403') return '/'
  const item = findItem(from)
  return item && canAccess(item, role) ? from : '/'
}
