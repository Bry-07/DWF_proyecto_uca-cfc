import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authService } from '../../api/authService'
import AuthField from '../../components/AuthField'
import AuthShell from '../../components/AuthShell'
import ErrorMessage from '../../components/ErrorMessage'
import { getErrorMessage, getFieldErrors } from '../../utils/errorHandler'
import { RE } from '../../utils/validators'

export default function Recuperar() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [serverError, setServerError] = useState('')
  const [result, setResult] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const onSubmit = async (e) => {
    e.preventDefault()
    setServerError('')
    if (!RE.EMAIL.test(email.trim())) {
      setError('Ingresa un correo válido.')
      return
    }
    setSubmitting(true)
    try {
      setResult(await authService.recuperarPassword(email.trim()))
    } catch (err) {
      setError(getFieldErrors(err).email ?? '')
      setServerError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthShell>
      <div className="top-mark">Recuperar acceso</div>
      <h2>Recuperar contraseña</h2>
      <p className="sub">Te enviaremos un enlace con un código para crear una nueva contraseña. Vence a los 30 minutos.</p>

      {result ? (
        <>
          <div className="notice">{result.mensaje ?? 'Si el correo está registrado, recibirás las instrucciones.'}</div>
          {/* tokenDePrueba solo lo devuelve el backend en entornos de prueba (sin correo real). */}
          {result.tokenDePrueba && (
            <button
              type="button" className="btn-outline block" style={{ marginBottom: 14 }}
              onClick={() => navigate('/restablecer', { state: { token: result.tokenDePrueba } })}
            >
              Usar el código de prueba
            </button>
          )}
          <Link className="btn-primary block" to="/restablecer" style={{ textAlign: 'center', textDecoration: 'none' }}>Ya tengo mi código</Link>
        </>
      ) : (
        <>
          <ErrorMessage message={serverError} />
          <form onSubmit={onSubmit} noValidate>
            <AuthField id="email" type="email" label="Correo electrónico" autoComplete="username" value={email} error={error} onChange={(e) => { setEmail(e.target.value); setError('') }} />
            <button className="btn-primary block" type="submit" disabled={submitting}>{submitting ? 'Enviando…' : 'Enviar instrucciones'}</button>
          </form>
        </>
      )}

      <p className="sub" style={{ marginTop: 22, marginBottom: 0, textAlign: 'center' }}>
        <Link className="link" to="/login">Volver a iniciar sesión</Link>
      </p>
    </AuthShell>
  )
}
