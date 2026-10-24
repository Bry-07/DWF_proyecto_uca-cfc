import { useEffect, useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import AuthShell from '../components/AuthShell'
import ErrorMessage from '../components/ErrorMessage'
import { useAuth } from '../hooks/useAuth'
import { SESSION_EXPIRED_KEY } from '../utils/constants'
import { getErrorMessage, getFieldErrors } from '../utils/errorHandler'
import { landingFor } from '../utils/navigation'

const ROLE_PILLS = ['ADMIN', 'RECEPCIONISTA', 'CONTABILIDAD', 'CLIENTE']

export default function Login() {
  const { login, isAuthenticated, loading, role } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = location.state?.from?.pathname ?? '/'

  const [form, setForm] = useState({ email: '', password: '' })
  const [remember, setRemember] = useState(false)
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [expired] = useState(() => sessionStorage.getItem(SESSION_EXPIRED_KEY) === '1')
  useEffect(() => { sessionStorage.removeItem(SESSION_EXPIRED_KEY) }, [])

  if (!loading && isAuthenticated) return <Navigate to={landingFor(role, from)} replace />

  const set = (key) => (e) => {
    setForm((f) => ({ ...f, [key]: e.target.value }))
    setErrors((er) => ({ ...er, [key]: undefined }))
  }

  const validate = () => {
    const er = {}
    if (!form.email.trim()) er.email = 'Ingresa tu correo institucional.'
    else if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) er.email = 'El correo no tiene un formato válido.'
    if (!form.password) er.password = 'Ingresa tu contraseña.'
    return er
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setServerError('')
    const er = validate()
    setErrors(er)
    if (Object.keys(er).length) return

    setSubmitting(true)
    try {
      const profile = await login({ email: form.email.trim(), password: form.password }, remember)
      navigate(landingFor(profile.rol, from), { replace: true })
    } catch (err) {
      setErrors(getFieldErrors(err))
      setServerError(getErrorMessage(err))
      setForm((f) => ({ ...f, password: '' })) // la contraseña no se conserva tras un fallo
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthShell>
          <div className="top-mark">EIC · UDB — Proyecto de cátedra</div>
          <h2>Iniciar sesión</h2>
          <p className="sub">Ingresa con tu cuenta institucional del centro.</p>

          {expired && !serverError && <div className="notice">Tu sesión expiró. Inicia sesión nuevamente para continuar.</div>}
          <ErrorMessage message={serverError} />

          <form onSubmit={onSubmit} noValidate>
            <div className={`field${errors.email ? ' has-error' : ''}`}>
              <label htmlFor="email">Correo institucional</label>
              <input id="email" type="email" autoComplete="username" placeholder="usuario@cfc.uca.edu.sv" value={form.email} onChange={set('email')} />
              {errors.email && <div className="error-text">{errors.email}</div>}
            </div>
            <div className={`field${errors.password ? ' has-error' : ''}`}>
              <label htmlFor="pass">Contraseña</label>
              <input id="pass" type="password" autoComplete="current-password" placeholder="••••••••••" value={form.password} onChange={set('password')} />
              {errors.password && <div className="error-text">{errors.password}</div>}
            </div>

            <div className="row-between">
              <label className="check">
                <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} /> Mantener sesión iniciada
              </label>
              <Link className="link" to="/recuperar">Recuperar contraseña</Link>
            </div>

            <button className="btn-primary block" type="submit" disabled={submitting}>
              {submitting ? 'Ingresando…' : 'Entrar'}
            </button>
          </form>

          <div className="divider">acceso por rol</div>
          <div className="roles">
            {ROLE_PILLS.map((r) => <span key={r} className="role-pill">{r}</span>)}
          </div>

          <p className="sub" style={{ marginTop: 22, marginBottom: 0, textAlign: 'center' }}>
            ¿Aún no tienes cuenta? <Link className="link" to="/registro">Regístrate como cliente</Link>
          </p>
    </AuthShell>
  )
}
