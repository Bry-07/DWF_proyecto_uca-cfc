import { useState } from 'react'
import { getErrorMessage, getFieldErrors } from '../utils/errorHandler'
import ErrorMessage from './ErrorMessage'
import Modal from './Modal'

const getIn = (o, path) => path.split('.').reduce((a, k) => a?.[k], o)
function setIn(o, path, val) {
  const ks = path.split('.')
  const last = ks.pop()
  let cur = o
  for (const k of ks) {
    cur[k] = cur[k] ?? {}
    cur = cur[k]
  }
  cur[last] = val
}

// Formulario genérico en modal.
// field: { name (admite 'a.b'), label, type: text|number|email|password|date|time|datetime-local|select|textarea|checkbox,
//          options:[{value,label}], required, number, full, show(values), pattern, patternMessage, maxLength, step, min, hint, disabled, placeholder }
// onSubmit(body) debe lanzar si falla; el modal muestra el mensaje y los errores por campo (ErrorResponse.errores).
export default function FormModal({ title, fields, initial = {}, onSubmit, onClose, submitLabel = 'Guardar', wide = false }) {
  const [values, setValues] = useState(() =>
    Object.fromEntries(
      fields.map((f) => {
        const raw = getIn(initial, f.name)
        if (f.type === 'checkbox') return [f.name, raw ?? f.default ?? false]
        if (f.type === 'datetime-local') return [f.name, typeof raw === 'string' ? raw.slice(0, 16) : (f.default ?? '')]
        return [f.name, raw ?? f.default ?? '']
      }),
    ),
  )
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const visible = fields.filter((f) => !f.show || f.show(values))
  const set = (name, v) => {
    setValues((s) => ({ ...s, [name]: v }))
    setErrors((e) => ({ ...e, [name]: undefined }))
  }

  const submit = async (e) => {
    e.preventDefault()
    setServerError('')
    const er = {}
    const body = {}
    for (const f of visible) {
      let v = values[f.name]
      if (f.type === 'checkbox') {
        setIn(body, f.name, Boolean(v))
        continue
      }
      if (v === '' || v === null || v === undefined) {
        if (f.required) er[f.name] = 'Este campo es obligatorio.'
        continue
      }
      if (f.pattern && !f.pattern.test(String(v))) er[f.name] = f.patternMessage ?? 'Formato inválido.'
      if (f.type === 'number' || f.number) v = Number(v)
      setIn(body, f.name, v)
    }
    setErrors(er)
    if (Object.keys(er).length) return
    setSubmitting(true)
    try {
      await onSubmit(body)
    } catch (err) {
      setErrors(getFieldErrors(err))
      setServerError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      title={title}
      onClose={onClose}
      wide={wide}
      footer={
        <>
          <button type="button" className="btn-outline" onClick={onClose}>Cancelar</button>
          <button type="submit" form="form-modal" className="btn-primary" disabled={submitting}>
            {submitting ? 'Guardando…' : submitLabel}
          </button>
        </>
      }
    >
      <ErrorMessage message={serverError} />
      <form id="form-modal" onSubmit={submit} noValidate>
        <div className="field-grid">
          {visible.map((f) => {
            const err = errors[f.name]
            const id = `f-${f.name}`
            const full = f.full || f.type === 'textarea'
            return (
              <div key={f.name} className={`field${err ? ' has-error' : ''}${full ? ' full' : ''}`}>
                {f.type === 'checkbox' ? (
                  <label className="check-inline">
                    <input type="checkbox" checked={Boolean(values[f.name])} onChange={(e) => set(f.name, e.target.checked)} />
                    {f.label}
                  </label>
                ) : (
                  <>
                    <label htmlFor={id}>{f.label}{f.required && ' *'}</label>
                    {f.type === 'select' ? (
                      <select id={id} value={values[f.name]} disabled={f.disabled} onChange={(e) => set(f.name, e.target.value)}>
                        <option value="">{f.placeholder ?? 'Selecciona…'}</option>
                        {(f.options ?? []).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                      </select>
                    ) : f.type === 'textarea' ? (
                      <textarea id={id} rows={3} maxLength={f.maxLength} value={values[f.name]} placeholder={f.placeholder} onChange={(e) => set(f.name, e.target.value)} />
                    ) : (
                      <input
                        id={id}
                        type={f.type ?? 'text'}
                        value={values[f.name]}
                        placeholder={f.placeholder}
                        maxLength={f.maxLength}
                        step={f.step}
                        min={f.min}
                        disabled={f.disabled}
                        autoComplete={f.type === 'password' ? 'new-password' : 'off'}
                        onChange={(e) => set(f.name, e.target.value)}
                      />
                    )}
                  </>
                )}
                {f.hint && !err && <div className="hint-text">{f.hint}</div>}
                {err && <div className="error-text">{err}</div>}
              </div>
            )
          })}
        </div>
      </form>
    </Modal>
  )
}
