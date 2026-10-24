import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authService } from '../../api/authService'
import AuthField from '../../components/AuthField'
import AuthShell from '../../components/AuthShell'
import ErrorMessage from '../../components/ErrorMessage'
import { alertSuccess } from '../../utils/alerts'
import { getErrorMessage, getFieldErrors } from '../../utils/errorHandler'
import { passwordError, PASS_HINT, RE } from '../../utils/validators'

const EMPTY = {
  tipo: 'PERSONA_PARTICULAR', nombre: '', email: '', password: '', password2: '',
  dui: '', nit: '', telefono: '', direccion: '',
  razonSocial: '', giro: '', contactoNombre: '', contactoCargo: '',
}

export default function Registro() {
  const navigate = useNavigate()
  const [f, setF] = useState(EMPTY)
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const empresa = f.tipo === 'EMPRESA'

  const set = (k) => (e) => {
    setF((s) => ({ ...s, [k]: e.target.value }))
    setErrors((er) => ({ ...er, [k]: undefined }))
  }

  const validate = () => {
    const er = {}
    if (!f.nombre.trim()) er.nombre = 'Ingresa tu nombre.'
    if (!f.email.trim()) er.email = 'Ingresa tu correo.'
    else if (!RE.EMAIL.test(f.email.trim())) er.email = 'El correo no tiene un formato válido.'
    er.password = passwordError(f.password)
    if (!er.password && f.password !== f.password2) er.password2 = 'Las contraseñas no coinciden.'
    if (f.dui && !RE.DUI.test(f.dui)) er.dui = 'Formato 00000000-0.'
    if (f.nit && !RE.NIT.test(f.nit)) er.nit = 'Formato 0000-000000-000-0.'
    if (f.telefono && !RE.TEL.test(f.telefono)) er.telefono = 'Formato 0000-0000.'
    if (empresa && !f.razonSocial.trim()) er.razonSocial = 'Ingresa la razón social.'
    return Object.fromEntries(Object.entries(er).filter(([, v]) => v))
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setServerError('')
    const er = validate()
    setErrors(er)
    if (Object.keys(er).length) return

    const body = {
      tipo: f.tipo, nombre: f.nombre.trim(), email: f.email.trim(), password: f.password,
      dui: f.dui, nit: f.nit, telefono: f.telefono, direccion: f.direccion.trim(),
      ...(empresa && {
        empresa: { razonSocial: f.razonSocial.trim(), giro: f.giro, contactoNombre: f.contactoNombre, contactoCargo: f.contactoCargo },
      }),
    }
    // El servicio no limpia vacíos: se quitan aquí para no enviar '' a campos con patrón.
    Object.keys(body).forEach((k) => body[k] === '' && delete body[k])
    if (body.empresa) Object.keys(body.empresa).forEach((k) => body.empresa[k] === '' && delete body.empresa[k])

    setSubmitting(true)
    try {
      await authService.registro(body)
      alertSuccess('Cuenta creada. Ya puedes iniciar sesión.')
      navigate('/login', { replace: true })
    } catch (err) {
      const fe = getFieldErrors(err)
      // El backend nombra los campos de empresa como "empresa.razonSocial".
      setErrors(Object.fromEntries(Object.entries(fe).map(([k, v]) => [k.replace('empresa.', ''), v])))
      setServerError(getErrorMessage(err))
      setF((s) => ({ ...s, password: '', password2: '' }))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthShell wide>
      <div className="top-mark">Registro de clientes</div>
      <h2>Crear cuenta</h2>
      <p className="sub">Regístrate para inscribirte, cotizar y revisar tus pagos.</p>

      <ErrorMessage message={serverError} />

      <form onSubmit={onSubmit} noValidate>
        <AuthField
          id="tipo" label="Tipo de cliente" value={f.tipo} onChange={set('tipo')}
          options={[{ value: 'PERSONA_PARTICULAR', label: 'Persona particular' }, { value: 'EMPRESA', label: 'Empresa' }]}
        />
        <AuthField id="nombre" label={empresa ? 'Nombre del representante' : 'Nombre completo'} maxLength={150} value={f.nombre} onChange={set('nombre')} error={errors.nombre} autoComplete="name" />
        <AuthField id="email" type="email" label="Correo electrónico" maxLength={150} value={f.email} onChange={set('email')} error={errors.email} autoComplete="username" />

        <div className="field-grid">
          <AuthField id="password" type="password" label="Contraseña" maxLength={72} value={f.password} onChange={set('password')} error={errors.password} autoComplete="new-password" />
          <AuthField id="password2" type="password" label="Confirmar contraseña" maxLength={72} value={f.password2} onChange={set('password2')} error={errors.password2} autoComplete="new-password" />
        </div>
        <p className="sub" style={{ fontSize: 11.5, margin: '6px 0 16px' }}>{PASS_HINT}</p>

        <div className="field-grid">
          <AuthField id="telefono" label="Teléfono (opcional)" placeholder="0000-0000" value={f.telefono} onChange={set('telefono')} error={errors.telefono} />
          <AuthField id="dui" label="DUI (opcional)" placeholder="00000000-0" value={f.dui} onChange={set('dui')} error={errors.dui} />
        </div>
        <AuthField id="nit" label="NIT (opcional)" placeholder="0000-000000-000-0" value={f.nit} onChange={set('nit')} error={errors.nit} />
        <AuthField id="direccion" label="Dirección (opcional)" maxLength={255} value={f.direccion} onChange={set('direccion')} error={errors.direccion} />

        {empresa && (
          <>
            <AuthField id="razonSocial" label="Razón social" value={f.razonSocial} onChange={set('razonSocial')} error={errors.razonSocial} />
            <div className="field-grid">
              <AuthField id="giro" label="Giro (opcional)" value={f.giro} onChange={set('giro')} error={errors.giro} />
              <AuthField id="contactoNombre" label="Contacto (opcional)" value={f.contactoNombre} onChange={set('contactoNombre')} error={errors.contactoNombre} />
            </div>
            <AuthField id="contactoCargo" label="Cargo del contacto (opcional)" value={f.contactoCargo} onChange={set('contactoCargo')} error={errors.contactoCargo} />
          </>
        )}

        <button className="btn-primary block" type="submit" disabled={submitting} style={{ marginTop: 8 }}>
          {submitting ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>
      </form>

      <p className="sub" style={{ marginTop: 22, marginBottom: 0, textAlign: 'center' }}>
        ¿Ya tienes cuenta? <Link className="link" to="/login">Iniciar sesión</Link>
      </p>
    </AuthShell>
  )
}
