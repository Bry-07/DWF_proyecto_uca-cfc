// Constantes compartidas. Los valores de enums deben coincidir con el backend
// (confirmar en /v3/api-docs si algún formulario los rechaza).

export const ROLES = {
  ADMIN: 'ADMIN',
  RECEPCIONISTA: 'RECEPCIONISTA',
  CONTABILIDAD: 'CONTABILIDAD',
  CLIENTE: 'CLIENTE',
}

// Estados válidos de cotización (bitácora §10). GENERADA ya NO existe.
export const ESTADOS_COTIZACION = {
  PENDIENTE: 'PENDIENTE',
  EN_PROCESO: 'EN_PROCESO',
  APROBADA: 'APROBADA',
  RECHAZADA: 'RECHAZADA',
}

export const PAGE_SIZE = 10

export const STORAGE_KEYS = {
  ACCESS: 'cfc.accessToken',
  REFRESH: 'cfc.refreshToken',
  PROFILE: 'cfc.profile',
}

// Evento global que dispara el cliente HTTP cuando la sesión ya no es válida.
export const AUTH_LOGOUT_EVENT = 'cfc:auth-logout'
// Marca (solo en esta pestaña) para avisar en el login que la sesión expiró.
export const SESSION_EXPIRED_KEY = 'cfc.sessionExpired'

export const MESSAGES = {
  NETWORK: 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.',
  UNAUTHORIZED: 'Tu sesión expiró. Inicia sesión nuevamente.',
  FORBIDDEN: 'No tienes permisos para realizar esta acción.',
  NOT_FOUND: 'El recurso solicitado no existe.',
  CONFLICT: 'La operación entra en conflicto con el estado actual de los datos.',
  VALIDATION: 'Revisa los datos ingresados.',
  SERVER: 'Ocurrió un error inesperado. Intenta de nuevo.',
  GENERIC: 'No se pudo completar la operación.',
}
