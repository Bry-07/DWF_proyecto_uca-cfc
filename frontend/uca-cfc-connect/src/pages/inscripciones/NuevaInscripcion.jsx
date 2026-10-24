import { useCallback, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { loadOffers } from '../../api/lookups'
import { clientesApi, inscripcionesApi } from '../../api/services'
import ErrorMessage from '../../components/ErrorMessage'
import Icon from '../../components/Icon'
import Loading from '../../components/Loading'
import { useAuth } from '../../hooks/useAuth'
import { useDebounce } from '../../hooks/useDebounce'
import { useFetch } from '../../hooks/useFetch'
import { alertError, alertSuccess } from '../../utils/alerts'
import { ROLES } from '../../utils/constants'
import { getErrorMessage, getFieldErrors } from '../../utils/errorHandler'
import { initials, money, shortDate } from '../../utils/format'

const loadActive = () => loadOffers(true)
const TEL = /^[0-9]{4}-[0-9]{4}$/
const STEPS = ['Cliente', 'Curso y participante', 'Confirmación']

export default function NuevaInscripcion() {
  const { user, role } = useAuth()
  const navigate = useNavigate()
  const isClient = role === ROLES.CLIENTE

  const [step, setStep] = useState(isClient ? 2 : 1)
  const [cliente, setCliente] = useState(isClient && user?.clienteId ? { id: user.clienteId, nombre: user.nombre } : null)
  const [q, setQ] = useState('')
  const dq = useDebounce(q)
  const fetchClientes = useCallback(() => (isClient ? Promise.resolve({ items: [] }) : clientesApi.list({ nombre: dq || undefined, size: 6 })), [dq, isClient])
  const clientes = useFetch(fetchClientes)
  const offers = useFetch(loadActive)

  const [tipo, setTipo] = useState('CURSO')
  const [oq, setOq] = useState('')
  const [oferta, setOferta] = useState(null)
  const [p, setP] = useState({ nombre: '', email: '', telefono: '', observaciones: '' })
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const visibleOffers = useMemo(() => {
    const term = oq.trim().toLowerCase()
    return (offers.data ?? []).filter(
      (o) => o.tipo === tipo && (o.cuposDisponibles ?? 1) > 0 && (!term || `${o.nombre} ${o.codigo}`.toLowerCase().includes(term)),
    )
  }, [offers.data, tipo, oq])

  const setField = (k) => (e) => {
    setP((s) => ({ ...s, [k]: e.target.value }))
    setErrors((er) => ({ ...er, [k]: undefined }))
  }

  const next = () => {
    if (step === 1) {
      if (!cliente) return alertError('Selecciona un cliente para continuar.')
      return setStep(2)
    }
    const er = {}
    if (!oferta) return alertError('Selecciona un curso o diplomado.')
    if (!p.nombre.trim()) er.nombre = 'Ingresa el nombre del participante.'
    if (!/^\S+@\S+\.\S+$/.test(p.email)) er.email = 'Correo no válido.'
    if (p.telefono && !TEL.test(p.telefono)) er.telefono = 'Formato 0000-0000.'
    setErrors(er)
    if (Object.keys(er).length === 0) setStep(3)
  }

  const confirm = async () => {
    setSaving(true)
    try {
      const res = await inscripcionesApi.create({
        clienteId: cliente.id,
        [oferta.tipo === 'CURSO' ? 'cursoId' : 'diplomadoId']: oferta.id,
        participanteNombre: p.nombre.trim(),
        participanteEmail: p.email.trim(),
        participanteTelefono: p.telefono,
        observaciones: p.observaciones,
      })
      alertSuccess(`Inscripción ${res.codigo ?? ''} creada`)
      navigate('/inscripciones')
    } catch (err) {
      setErrors(getFieldErrors(err))
      alertError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const back = () => (step === (isClient ? 2 : 1) ? navigate('/inscripciones') : setStep(step - 1))

  return (
    <div className="content" style={{ maxWidth: 1080 }}>
      <div className="page-head">
        <div>
          <h1>Nueva inscripción</h1>
          <p>Vincula un cliente con un curso o diplomado del catálogo</p>
        </div>
        <Link to="/inscripciones" className="btn-outline">Cancelar</Link>
      </div>

      <div className="stepper">
        {STEPS.map((label, i) => {
          const n = i + 1
          const cls = n < step ? 'done' : n === step ? 'current' : ''
          return (
            <div key={label} style={{ display: 'flex', alignItems: 'center' }}>
              {i > 0 && <div className={`step-line${n <= step ? ' done' : ''}`} />}
              <div className={`step ${cls}`}><div className="num">{n < step ? '✓' : n}</div><span className="label">{label}</span></div>
            </div>
          )
        })}
      </div>

      <div className="layout-wiz">
        <div className="panel">
          {step === 1 && (
            <>
              <h2>Selecciona el cliente</h2>
              <p className="hint">Busca por nombre. Si no existe, créalo primero en Clientes.</p>
              <div className="search-field"><Icon name="search" /><input placeholder="Buscar cliente..." value={q} onChange={(e) => setQ(e.target.value)} /></div>
              <ErrorMessage message={clientes.error} onRetry={clientes.refetch} />
              {clientes.loading && !clientes.data && <Loading />}
              <div className="client-list">
                {(clientes.data?.items ?? []).map((c) => (
                  <button key={c.id} type="button" className={`client-row${cliente?.id === c.id ? ' selected' : ''}`} onClick={() => setCliente(c)}>
                    <div className="client-info">
                      <div className="avatar">{initials(c.nombre)}</div>
                      <div><div className="client-name">{c.nombre}</div><div className="client-sub">{c.email}</div></div>
                    </div>
                    <div className={`radio${cliente?.id === c.id ? ' on' : ''}`} />
                  </button>
                ))}
              </div>
            </>
          )}

          {step === 2 && (
            <>
              <h2>Curso y participante</h2>
              <p className="hint">Solo se muestran ofertas activas con cupo disponible. Cliente: <b>{cliente?.nombre}</b></p>
              <div className="filters">
                {[['CURSO', 'Cursos'], ['DIPLOMADO', 'Diplomados']].map(([v, l]) => (
                  <button key={v} type="button" className={`chip${tipo === v ? ' on' : ''}`} onClick={() => { setTipo(v); setOferta(null) }}>{l}</button>
                ))}
              </div>
              <div className="search-field"><Icon name="search" /><input placeholder="Buscar por nombre o código..." value={oq} onChange={(e) => setOq(e.target.value)} /></div>
              <ErrorMessage message={offers.error} onRetry={offers.refetch} />
              {offers.loading && !offers.data && <Loading />}
              <div className="client-list" style={{ maxHeight: 260, overflow: 'auto' }}>
                {visibleOffers.map((o) => (
                  <button key={`${o.tipo}-${o.id}`} type="button" className={`client-row${oferta?.id === o.id && oferta?.tipo === o.tipo ? ' selected' : ''}`} onClick={() => setOferta(o)}>
                    <div className="client-info">
                      <div className="avatar" style={{ background: 'var(--slate-tint)', color: 'var(--slate)' }}>{initials(o.nombre)}</div>
                      <div>
                        <div className="client-name">{o.nombre}</div>
                        <div className="client-sub">{o.codigo} · {o.horario ?? 'Horario por definir'} · {o.cuposDisponibles ?? '—'} cupos libres</div>
                      </div>
                    </div>
                    <div className={`radio${oferta?.id === o.id && oferta?.tipo === o.tipo ? ' on' : ''}`} />
                  </button>
                ))}
                {!offers.loading && visibleOffers.length === 0 && <div className="state-box">No hay ofertas disponibles con ese criterio.</div>}
              </div>

              <div className="field-grid" style={{ marginTop: 18 }}>
                <div className={`field${errors.nombre ? ' has-error' : ''}`}><label>Participante *</label><input value={p.nombre} onChange={setField('nombre')} maxLength={150} />{errors.nombre && <div className="error-text">{errors.nombre}</div>}</div>
                <div className={`field${errors.email ? ' has-error' : ''}`}><label>Correo del participante *</label><input type="email" value={p.email} onChange={setField('email')} />{errors.email && <div className="error-text">{errors.email}</div>}</div>
                <div className={`field${errors.telefono ? ' has-error' : ''}`}><label>Teléfono</label><input placeholder="0000-0000" value={p.telefono} onChange={setField('telefono')} />{errors.telefono && <div className="error-text">{errors.telefono}</div>}</div>
                <div className="field"><label>Observaciones</label><input value={p.observaciones} onChange={setField('observaciones')} maxLength={500} /></div>
              </div>
            </>
          )}

          {step === 3 && (
            <>
              <h2>Confirmación</h2>
              <p className="hint">Revisa la ficha de la derecha. Al confirmar, la inscripción queda <b>Pendiente</b> hasta registrar el pago.</p>
              <div className="alert-bar info"><Icon name="alert" /><span>El cupo se valida al confirmar; si el curso se llenó, verás el aviso del sistema.</span></div>
            </>
          )}

          <div className="btn-row">
            <button type="button" className="btn-outline" onClick={back} disabled={saving}>Atrás</button>
            {step < 3 ? (
              <button type="button" className="btn-primary" onClick={next}>Continuar</button>
            ) : (
              <button type="button" className="btn-primary" onClick={confirm} disabled={saving}>{saving ? 'Guardando…' : 'Confirmar inscripción'}</button>
            )}
          </div>
        </div>

        <div className="ficha">
          <div className="ficha-tag">Pendiente</div>
          <div className="ficha-code">FICHA DE INSCRIPCIÓN</div>
          <div className="ficha-line"><span>Cliente</span><span>{cliente?.nombre ?? '—'}</span></div>
          <div className="ficha-line"><span>Participante</span><span>{p.nombre || '—'}</span></div>
          <div className="ficha-line"><span>{oferta?.tipo === 'DIPLOMADO' ? 'Diplomado' : 'Curso'}</span><span>{oferta?.nombre ?? '—'}</span></div>
          <div className="ficha-line"><span>Horario</span><span>{oferta?.horario ?? '—'}</span></div>
          <div className="ficha-line"><span>Inicio</span><span>{oferta ? shortDate(oferta.fechaInicio) : '—'}</span></div>
          <div className="ficha-total"><span className="lbl">Total a pagar</span><span className="amt">{money(oferta?.precio ?? 0)}</span></div>
        </div>
      </div>
    </div>
  )
}
