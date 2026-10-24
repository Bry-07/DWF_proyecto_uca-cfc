import { STORAGE_KEYS } from './constants'

// "Mantener sesión iniciada" => localStorage; si no => sessionStorage.
// Solo se guardan tokens y el perfil necesario. NUNCA contraseñas.

const stores = () => [window.localStorage, window.sessionStorage]

function safe(fn, fallback = null) {
  try {
    return fn()
  } catch {
    return fallback
  }
}

function read(key) {
  for (const store of stores()) {
    const value = safe(() => store.getItem(key))
    if (value) return value
  }
  return null
}

export const tokenStorage = {
  getAccessToken: () => read(STORAGE_KEYS.ACCESS),
  getRefreshToken: () => read(STORAGE_KEYS.REFRESH),

  getProfile() {
    const raw = read(STORAGE_KEYS.PROFILE)
    return raw ? safe(() => JSON.parse(raw)) : null
  },

  // Reemplaza SIEMPRE ambos tokens (el refresh rota en cada uso).
  // Mantiene el almacenamiento donde ya estaba la sesión salvo que se indique `remember`.
  setTokens({ accessToken, refreshToken }, remember) {
    const useLocal =
      remember !== undefined
        ? remember
        : safe(() => window.localStorage.getItem(STORAGE_KEYS.REFRESH)) !== null
    const target = useLocal ? window.localStorage : window.sessionStorage
    const other = useLocal ? window.sessionStorage : window.localStorage
    safe(() => {
      target.setItem(STORAGE_KEYS.ACCESS, accessToken)
      target.setItem(STORAGE_KEYS.REFRESH, refreshToken)
      other.removeItem(STORAGE_KEYS.ACCESS)
      other.removeItem(STORAGE_KEYS.REFRESH)
    })
  },

  setProfile(profile) {
    const raw = JSON.stringify(profile)
    const inLocal = safe(() => window.localStorage.getItem(STORAGE_KEYS.REFRESH)) !== null
    safe(() => (inLocal ? window.localStorage : window.sessionStorage).setItem(STORAGE_KEYS.PROFILE, raw))
  },

  clear() {
    for (const store of stores()) {
      Object.values(STORAGE_KEYS).forEach((key) => safe(() => store.removeItem(key)))
    }
  },
}
