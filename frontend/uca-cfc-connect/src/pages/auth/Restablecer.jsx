import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { authService } from '../../api/authService'
import AuthField from '../../components/AuthField'
import AuthShell from '../../components/AuthShell'
import ErrorMessage from '../../components/ErrorMessage'
import { alertSuccess } from '../../utils/alerts'
import { getErrorMessage, getFieldErrors } from '../../utils/errorHandler'
import { passwordError, PASS_HINT } from '../../utils/validators'

export default function Restablecer() {
  const navigate = useNavigate()
  const location = useLocation()
  // El código puede venir del enlace del correo (?token=) o de la pantalla anterior; no se deja en la URL.
  const [incoming] = useState(() => location.state?.token ?? new URLSearchParams(location.search).get('token') ?? '')
  const [f, setF] = useState({ token: incoming, nuevaPassword: '', password2: '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (location.search || location.state) navigate('/restablecer', { replace: true })
  }, [location.search, location.state, navigate])

  const set = (k) => (e) => {
    setF((s) => ({ ...s, [k]: e.target.value }))
    setErrors((er) => ({ ...er, [k]: undefined }))
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setServerError('')
    const er = {}
    if (!f.token.trim()) er.token = 'Pega el código que recibiste por correo.'
    er.nuevaPassword = passwordError(f.nuevaPassword)
    if (!er.nuevaPassword && f.nuevaPassword !== f.password2) er.password2 = 'Las contraseñas no coinciden.'
    const clean = Object.fromEntries(Object.entries(er).filter(([, v]) => v))
    setErrors(clean)
    if (Object.keys(clean).length) return

    setSubmitting(true)
    try {
      await authService.restablecerPassword({ token: f.token.trim(), nuevaPassword: f.nuevaPassword })
      alertSuccess('Contraseña actualizada. Inicia sesión con la nueva.')
      navigate('/login', { replace: true })
    } catch (err) {
      setErrors(getFieldErrors(err))
      setServerError(getErrorMessage(err))
      setF((s) => ({ ...s, nuevaPassword: '', password2: '' }))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthShell>
      <div className="top-mark">Recuperar acceso</div>
      <h2>Nueva contraseña</h2>
      <p className="sub">El código sirve una sola vez. Al cambiarla se cierran tus sesiones abiertas.</p>

      <ErrorMessage message={serverError} />

      <form onSubmit={onSubmit} noValidate>
        <AuthField id="token" label="Código de recuperación" value={f.token} onChange={set('token')} error={errors.token} autoComplete="off" />
        <AuthField id="nueva" type="password" label="Nueva contraseña" maxLength={72} value={f.nuevaPassword} onChange={set('nuevaPassword')} error={errors.nuevaPassword} hint={PASS_HINT} autoComplete="new-password" />
        <AuthField id="nueva2" type="password" label="Confirmar contraseña" maxLength={72} value={f.password2} onChange={set('password2')} error={errors.password2} autoComplete="new-password" />
        <button className="btn-primary block" type="submit" disabled={submitting}>{submitting ? 'Guardando…' : 'Cambiar contraseña'}</button>
      </form>

      <p className="sub" style={{ marginTop: 22, marginBottom: 0, textAlign: 'center' }}>
        <Link className="link" to="/login">Volver a iniciar sesión</Link>
      </p>
    </AuthShell>
  )
}
