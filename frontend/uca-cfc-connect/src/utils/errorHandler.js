import { MESSAGES } from './constants'

// Contrato real (OpenAPI ErrorResponse): { timestamp, status, error, message, path, errores:[{campo, mensaje}] }.
// Se aceptan además otros nombres habituales por tolerancia; si algo cambia, solo se toca este archivo.

const MESSAGE_KEYS = ['mensaje', 'message', 'detalle', 'detail', 'error']
const FIELD_KEYS = ['errores', 'errors', 'fieldErrors', 'detalles', 'campos']

const byStatus = {
  400: MESSAGES.VALIDATION,
  401: MESSAGES.UNAUTHORIZED,
  403: MESSAGES.FORBIDDEN,
  404: MESSAGES.NOT_FOUND,
  409: MESSAGES.CONFLICT,
  422: MESSAGES.VALIDATION,
}

export function getErrorMessage(error) {
  if (!error.response) return MESSAGES.NETWORK
  const { status, data } = error.response

  if (typeof data === 'string' && data.trim()) return data
  if (data && typeof data === 'object') {
    for (const key of MESSAGE_KEYS) {
      if (typeof data[key] === 'string' && data[key].trim()) return data[key]
    }
  }
  if (status >= 500) return MESSAGES.SERVER
  return byStatus[status] ?? MESSAGES.GENERIC
}

// Devuelve { campo: "mensaje" } para pintar errores bajo cada input.
// Acepta array [{campo|field, mensaje|message}] u objeto {campo: mensaje}.
export function getFieldErrors(error) {
  const data = error.response?.data
  if (!data || typeof data !== 'object') return {}

  for (const key of FIELD_KEYS) {
    const value = data[key]
    if (Array.isArray(value)) {
      return value.reduce((acc, item) => {
        const field = item.campo ?? item.field
        const msg = item.mensaje ?? item.message
        if (field && msg) acc[field] = msg
        return acc
      }, {})
    }
    if (value && typeof value === 'object') return { ...value }
  }
  return {}
}

export const isStatus = (error, status) => error.response?.status === status
