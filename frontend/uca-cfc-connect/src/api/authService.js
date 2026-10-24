import api from './axiosConfig'
import { tokenStorage } from '../utils/tokenStorage'

// Toda la comunicación de autenticación vive aquí (bitácora §7).
// Si OpenAPI usa otros nombres de campo, se ajusta SOLO en normalizeTokens.
const normalizeTokens = (data) => ({
  accessToken: data.accessToken,
  refreshToken: data.refreshToken,
})

export const authService = {
  async login({ email, password }, remember = false) {
    const { data } = await api.post('/auth/login', { email, password })
    tokenStorage.setTokens(normalizeTokens(data), remember)
    // La contraseña nunca se guarda; solo tokens + perfil.
    const profile = await authService.me()
    return profile
  },

  async me() {
    const { data } = await api.get('/auth/me')
    tokenStorage.setProfile(data)
    return data
  },

  // Aunque el servidor responda error (p. ej. token ya revocado), la sesión local se limpia.
  async logout() {
    const refreshToken = tokenStorage.getRefreshToken()
    try {
      await api.post('/auth/logout', { refreshToken })
    } finally {
      tokenStorage.clear()
    }
  },

  registro: (payload) => api.post('/auth/registro', payload).then((r) => r.data),
  recuperarPassword: (email) => api.post('/auth/recuperar-password', { email }).then((r) => r.data),
  restablecerPassword: (payload) => api.post('/auth/restablecer-password', payload).then((r) => r.data),
}
