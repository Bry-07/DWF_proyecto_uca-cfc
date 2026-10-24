import { useCallback, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { fetchAll, opt } from '../../api/lookups'
import { clientesApi, cotizacionesApi, cursosApi, diplomadosApi, espaciosApi, serviciosCateringApi } from '../../api/services'
import ErrorMessage from '../../components/ErrorMessage'
import Icon from '../../components/Icon'
import Loading from '../../components/Loading'
import Timeline from '../../components/Timeline'
import { useAuth } from '../../hooks/useAuth'
import { useFetch } from '../../hooks/useFetch'
import { alertError, alertSuccess } from '../../utils/alerts'
import { ROLES } from '../../utils/constants'
import { getErrorMessage } from '../../utils/errorHandler'
import { money } from '../../utils/format'
import { humanize } from '../../utils/labels'
import { isStaff } from '../../utils/permissions'

const TIPOS = [
  ['CURSO', 'Curso empresarial', 'cursos'],
  ['DIPLOMADO', 'Diplomado', 'cursos'],
  ['ESPACIO', 'Alquiler de espacio', 'espacios'],
  ['CATERING', 'Catering', 'catering'],
]
// precio sugerido según el tipo de referencia
const PRICE = { CURSO: (r) => r.precio, DIPLOMADO: (r) => r.precio, ESPACIO: (r) => r.precioPorHora, CATERING: (r) => r.precioPorPersona }

async function loadAll(staff) {
  const [cursos, diplomados, espacios, catering, clientes] = await Promise.all([
    fetchAll(cursosApi, { estado: 'ACTIVO' }), fetchAll(diplomadosApi, { estado: 'ACTIVO' }),
    fetchAll(espaciosApi), fetchAll(serviciosCateringApi, { activo: true }),
    staff ? fetchAll(clientesApi) : Promise.resolve([]),
  ])
  return { CURSO: cursos, DIPLOMADO: diplomados, ESPACIO: espacios, CATERING: catering, clientes }
}

let rowSeq = 0
const newRow = (tipo) => ({ key: ++rowSeq, tipo, referenciaId: '', concepto: '', detalle: '', cantidad: 1, precioUnitario: '' })

export default function CotizacionNueva() {
  const { user, role } = useAuth()
  const staff = isStaff(role)
  const navigate = useNavigate()
  const fn = useCallback(() => loadAll(staff), [staff])
  const { data: lk, loading, error, refetch } = useFetch(fn)

  const [clienteId, setClienteId] = useState(role === ROLES.CLIENTE && user?.clienteId ? String(user.clienteId) : '')
  const [head, setHead] = useState({ asunto: '', fechaVencimiento: '', responsableRevision: '', notaInterna: '' })
  const [rows, setRows] = useState([newRow('CURSO')])
  const [saving, setSaving] = useState(false)

  const total = rows.reduce((s, r) => s + (Number(r.cantidad) || 0) * (Number(r.precioUnitario) || 0), 0)
  const patch = (key, p) => setRows((rs) => rs.map((r) => (r.key === key ? { ...r, ...p } : r)))

  const pickRef = (row, id) => {
    const ref = lk?.[row.tipo]?.find((x) => String(x.id) === id)
    patch(row.key, ref ? { referenciaId: id, concepto: ref.nombre, precioUnitario: PRICE[row.tipo](ref) ?? '' } : { referenciaId: '' })
  }

  const submit = async () => {
    if (!clienteId) return alertError('Selecciona el cliente.')
    if (!head.asunto.trim()) return alertError('Escribe el asunto de la cotización.')
    if (rows.length === 0) return alertError('Agrega al menos un servicio.')
    if (rows.some((r) => !r.concepto.trim() || !(Number(r.cantidad) > 0) || r.precioUnitario === '')) return alertError('Completa concepto, cantidad y precio de cada línea.')
    setSaving(true)
    try {
      const res = await cotizacionesApi.create({
        clienteId: Number(clienteId),
        asunto: head.asunto.trim(),
        fechaVencimiento: head.fechaVencimiento,
        responsableRevision: head.responsableRevision,
        notaInterna: head.notaInterna,
        items: rows.map((r) => ({
          tipo: r.tipo,
          concepto: r.concepto.trim(),
          detalle: r.detalle,
          referenciaId: r.referenciaId ? Number(r.referenciaId) : undefined,
          cantidad: Number(r.cantidad),
          precioUnitario: Number(r.precioUnitario),
        })),
      })
      alertSuccess(`Cotización ${res.codigo ?? ''} enviada`)
      navigate('/cotizaciones')
    } catch (err) {
      alertError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="content" style={{ maxWidth: 1180 }}>
      <div className="page-head">
        <div>
          <h1>Solicitar cotización</h1>
          <p>Combina cursos, espacios y catering en una sola propuesta para el cliente</p>
        </div>
        <Link to="/cotizaciones" className="btn-outline">Cancelar</Link>
      </div>

      <ErrorMessage message={error} onRetry={refetch} />
      {loading && !lk && <Loading />}
      {lk && (
        <div className="layout-2">
          <div className="panel">
            <h2>Datos de la solicitud</h2>
            <p className="hint">Agrega los servicios y ajusta cantidades. Los precios se sugieren desde el catálogo.</p>

            <div className="field-grid" style={{ marginBottom: 20 }}>
              {staff && (
                <div className="field full">
                  <label>Cliente *</label>
                  <select value={clienteId} onChange={(e) => setClienteId(e.target.value)}>
                    <option value="">Selecciona…</option>
                    {opt(lk.clientes).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                  </select>
                </div>
              )}
              <div className="field full"><label>Asunto *</label><input value={head.asunto} maxLength={255} placeholder="Evento institucional de cierre de año" onChange={(e) => setHead({ ...head, asunto: e.target.value })} /></div>
              <div className="field"><label>Vencimiento</label><input type="date" value={head.fechaVencimiento} onChange={(e) => setHead({ ...head, fechaVencimiento: e.target.value })} /></div>
              {staff && <div className="field"><label>Responsable de revisión</label><input value={head.responsableRevision} maxLength={150} onChange={(e) => setHead({ ...head, responsableRevision: e.target.value })} /></div>}
              {staff && <div className="field full"><label>Nota interna</label><input value={head.notaInterna} maxLength={500} onChange={(e) => setHead({ ...head, notaInterna: e.target.value })} /></div>}
            </div>

            <div className="filters">
              {TIPOS.map(([tipo, label]) => (
                <button key={tipo} type="button" className="chip" onClick={() => setRows((rs) => [...rs, newRow(tipo)])}>+ {label}</button>
              ))}
            </div>

            <div className="table-wrap">
              <table className="items-table">
                <thead><tr><th>Servicio</th><th>Referencia / concepto</th><th>Cant.</th><th>Precio</th><th className="num">Subtotal</th><th /></tr></thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.key}>
                      <td className="concept">{humanize(r.tipo)}</td>
                      <td>
                        <select value={r.referenciaId} onChange={(e) => pickRef(r, e.target.value)} style={{ marginBottom: 6 }}>
                          <option value="">Del catálogo…</option>
                          {opt(lk[r.tipo] ?? []).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                        </select>
                        <input value={r.concepto} placeholder="Concepto" maxLength={255} onChange={(e) => patch(r.key, { concepto: e.target.value })} />
                      </td>
                      <td style={{ width: 70 }}><input type="number" min="1" value={r.cantidad} onChange={(e) => patch(r.key, { cantidad: e.target.value })} /></td>
                      <td style={{ width: 100 }}><input type="number" min="0" step="0.01" value={r.precioUnitario} onChange={(e) => patch(r.key, { precioUnitario: e.target.value })} /></td>
                      <td className="num">{money((Number(r.cantidad) || 0) * (Number(r.precioUnitario) || 0))}</td>
                      <td><button type="button" className="link-action" aria-label="Quitar" onClick={() => setRows((rs) => rs.filter((x) => x.key !== r.key))}>✕</button></td>
                    </tr>
                  ))}
                  <tr className="total-row"><td colSpan={4}>Total estimado</td><td className="num">{money(total)}</td><td /></tr>
                </tbody>
              </table>
            </div>

            <button type="button" className="btn-primary block" style={{ marginTop: 18 }} onClick={submit} disabled={saving}>{saving ? 'Enviando…' : 'Enviar cotización'}</button>
          </div>

          <div className="panel">
            <h2>Estado de la solicitud</h2>
            <p className="hint">Al enviarla, nace como Pendiente y Administración la revisa.</p>
            <Timeline estado="PENDIENTE" />
            <div className="alert-bar info"><Icon name="alert" /><span>Si incluye un espacio, se verificará la disponibilidad antes de aprobar.</span></div>
          </div>
        </div>
      )}
    </div>
  )
}
