import axios from 'axios'
import { AUTH_LOGOUT_EVENT } from '../utils/constants'
import { tokenStorage } from '../utils/tokenStorage'

// Único lugar que conoce la URL del backend (bitácora §4).
export const API_URL = import.meta.env.VITE_API_URL

const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Cliente "limpio" para el refresh: sin interceptores, evita bucles.
const bare = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
})

// Rutas públicas de auth: un 401 aquí significa "credenciales/token inválido",
// nunca "access token expirado", así que no se intenta refresh.
const PUBLIC_AUTH = ['/auth/login', '/auth/refresh', '/auth/registro', '/auth/recuperar-password', '/auth/restablecer-password']
const isPublicAuth = (url = '') => PUBLIC_AUTH.some((p) => url.startsWith(p))

function endSession() {
  tokenStorage.clear()
  window.dispatchEvent(new Event(AUTH_LOGOUT_EVENT))
}

// 1) Bearer automático (los componentes nunca lo agregan a mano)
api.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken()
  if (token && !isPublicAuth(config.url)) {
    config.headers.Authorization = `Bearer ${token}`
    config._sentToken = token // para detectar tokens ya renovados por otra petición
  }
  return config
})

// 2) Refresh con rotación, una sola vez por solicitud y una sola llamada
//    simultánea aunque fallen varias peticiones a la vez.
let refreshPromise = null

function refreshTokens() {
  if (!refreshPromise) {
    const refreshToken = tokenStorage.getRefreshToken()
    refreshPromise = bare
      .post('/auth/refresh', { refreshToken })
      .then(({ data }) => {
        // Se reemplazan AMBOS. Nunca se reutiliza el refresh anterior.
        tokenStorage.setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken })
        return data.accessToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    const status = error.response?.status

    // 403 = autenticado pero sin permisos: NO se refresca, se propaga.
    if (status !== 401 || !original || isPublicAuth(original.url)) {
      return Promise.reject(error)
    }

    // Sin refresh token, o ya se reintentó una vez: sesión terminada.
    if (!tokenStorage.getRefreshToken() || original._retry) {
      if (tokenStorage.getAccessToken()) endSession()
      return Promise.reject(error)
    }

    original._retry = true

    // Otra petición ya renovó el token mientras esta estaba en vuelo:
    // basta con reintentar con el token vigente (sin rotar de nuevo).
    const current = tokenStorage.getAccessToken()
    if (current && original._sentToken && current !== original._sentToken) {
      original.headers.Authorization = `Bearer ${current}`
      return api(original)
    }

    try {
      const newAccess = await refreshTokens()
      original.headers.Authorization = `Bearer ${newAccess}`
      return api(original)
    } catch (refreshError) {
      endSession()
      return Promise.reject(refreshError)
    }
  },
)

export default api
