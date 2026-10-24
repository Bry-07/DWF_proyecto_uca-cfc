import { useCallback, useEffect, useMemo, useState } from 'react'
import { authService } from '../api/authService'
import { AUTH_LOGOUT_EVENT, SESSION_EXPIRED_KEY } from '../utils/constants'
import { tokenStorage } from '../utils/tokenStorage'
import { AuthContext } from './authContext'

// /auth/me devuelve { id, nombre, email, rol }. Se tolera "ROLE_" y `roles[]`
// por si el backend cambia, pero el contrato actual es `rol`.
function normalizeProfile(p) {
  if (!p) return null
  const raw = p.rol ?? p.roles?.[0] ?? null
  const rol = typeof raw === 'string' ? raw.replace(/^ROLE_/, '') : (raw?.nombre ?? null)
  return { ...p, rol }
}

function initialUser() {
  // Solo se confía en el perfil guardado si también hay access token.
  return tokenStorage.getAccessToken() ? normalizeProfile(tokenStorage.getProfile()) : null
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(initialUser)
  // Si hay token pero no perfil guardado, hay que consultarlo antes de decidir rutas.
  const [loading, setLoading] = useState(() => Boolean(tokenStorage.getAccessToken()) && !initialUser())

  // Revalida la sesión al arrancar (el interceptor hace el refresh si hace falta).
  useEffect(() => {
    if (!tokenStorage.getAccessToken()) return undefined
    let active = true
    authService
      .me()
      .then((p) => active && setUser(normalizeProfile(p)))
      .catch(() => {
        // 401 definitivo: el interceptor ya limpió y emitió el evento.
        // Errores de red/500: se conserva el perfil guardado.
      })
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [])

  // El cliente HTTP avisa cuando el refresh falla: se cierra la sesión local.
  useEffect(() => {
    const onLogout = () => {
      sessionStorage.setItem(SESSION_EXPIRED_KEY, '1')
      setUser(null)
    }
    window.addEventListener(AUTH_LOGOUT_EVENT, onLogout)
    return () => window.removeEventListener(AUTH_LOGOUT_EVENT, onLogout)
  }, [])

  const login = useCallback(async (credentials, remember) => {
    const profile = normalizeProfile(await authService.login(credentials, remember))
    setUser(profile)
    return profile
  }, [])

  const logout = useCallback(async () => {
    try {
      await authService.logout()
    } catch {
      // authService.logout ya limpia los tokens aunque el servidor falle.
    } finally {
      setUser(null)
    }
  }, [])

  const value = useMemo(
    () => ({ user, role: user?.rol ?? null, isAuthenticated: Boolean(user), loading, login, logout }),
    [user, loading, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
