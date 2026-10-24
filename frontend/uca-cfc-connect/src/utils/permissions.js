import { ROLES } from './constants'

// Solo UX: el backend es la autoridad y responde 403 si el rol no puede.
export const STAFF = [ROLES.ADMIN, ROLES.RECEPCIONISTA]
export const FINANCE = [ROLES.ADMIN, ROLES.CONTABILIDAD]
export const isStaff = (role) => STAFF.includes(role)
export const isFinance = (role) => FINANCE.includes(role)
