import { useCallback, useMemo, useState } from 'react'
import { fetchAll, opt } from '../../api/lookups'
import { agendaApi, espaciosApi } from '../../api/services'
import ErrorMessage from '../../components/ErrorMessage'
import FormModal from '../../components/FormModal'
import Icon from '../../components/Icon'
import Loading from '../../components/Loading'
import Modal from '../../components/Modal'
import { useAuth } from '../../hooks/useAuth'
import { useFetch } from '../../hooks/useFetch'
import { alertError, alertSuccess, confirmDelete } from '../../utils/alerts'
import { addDays, dateTimeLabel, mondayOf, parseLocal, timeLabel, toISODate, toISODateTime } from '../../utils/dates'
import { getErrorMessage } from '../../utils/errorHandler'
import { humanize } from '../../utils/labels'
import { isStaff } from '../../utils/permissions'

const H0 = 7
const H1 = 21
const PX = 32
const DAYS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado']
const MONTHS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']
const LEGEND = [['curso', 'Cursos'], ['diplomado', 'Diplomados'], ['evento', 'Eventos'], ['alquiler', 'Alquileres'], ['catering', 'Catering'], ['conflicto', 'Conflicto']]

// Reparte eventos que se solapan en carriles lado a lado.
function layoutDay(events) {
  const sorted = [...events].sort((a, b) => a.start - b.start)
  const out = []
  let cluster = []
  let clusterEnd = 0
  const flush = () => {
    const lanes = []
    cluster.forEach((e) => {
      let lane = lanes.findIndex((end) => end <= e.start)
      if (lane === -1) { lane = lanes.length; lanes.push(0) }
      lanes[lane] = e.end
      e.lane = lane
    })
    cluster.forEach((e) => out.push({ ...e, lanes: lanes.length }))
    cluster = []
  }
  sorted.forEach((e) => {
    if (cluster.length && e.start >= clusterEnd) flush()
    cluster.push(e)
    clusterEnd = Math.max(clusterEnd, e.end)
  })
  if (cluster.length) flush()
  return out
}

export default function Agenda() {
  const { role } = useAuth()
  const staff = isStaff(role)
  const [monday, setMonday] = useState(() => mondayOf(new Date()))
  const [espacioId, setEspacioId] = useState('')
  const [sel, setSel] = useState(null)
  const [form, setForm] = useState(false)
  const [todayIso] = useState(() => toISODate(new Date()))

  const desde = toISODateTime(monday)
  const hasta = toISODateTime(new Date(addDays(monday, 6).setHours(23, 59, 0, 0)))

  const load = useCallback(async () => {
    const [page, conflictos] = await Promise.all([
      agendaApi.list({ fechaDesde: desde, fechaHasta: hasta, espacioId: espacioId || undefined, size: 200 }),
      agendaApi.conflictos({ fechaDesde: desde, fechaHasta: hasta }),
    ])
    return { items: page.items, conflictos }
  }, [desde, hasta, espacioId])
  const { data, loading, error, refetch } = useFetch(load)
  const espaciosFn = useCallback(() => fetchAll(espaciosApi).then((l) => opt(l)), [])
  const espacios = useFetch(espaciosFn)

  const conflictIds = useMemo(() => {
    const s = new Set()
    ;(data?.conflictos ?? []).forEach((c) => { s.add(c.actividadA?.id); s.add(c.actividadB?.id) })
    return s
  }, [data])

  const columns = useMemo(() => {
    // Cada actividad se recorta al tramo visible (H0–H1) de cada día que cubre,
    // así una actividad de varios días no se sale de la rejilla.
    const byDay = DAYS.map(() => [])
    ;(data?.items ?? []).forEach((a) => {
      const os = parseLocal(a.fechaHoraInicio)
      const oe = parseLocal(a.fechaHoraFin)
      if (!os || !oe || oe <= os) return
      DAYS.forEach((_, i) => {
        const d = addDays(monday, i)
        const visStart = new Date(d.getFullYear(), d.getMonth(), d.getDate(), H0, 0)
        const visEnd = new Date(d.getFullYear(), d.getMonth(), d.getDate(), H1, 0)
        const start = os > visStart ? os : visStart
        const end = oe < visEnd ? oe : visEnd
        if (end > start) byDay[i].push({ a, start, end, os, oe })
      })
    })
    return byDay.map(layoutDay)
  }, [data, monday])

  const range = `${monday.getDate()} ${MONTHS[monday.getMonth()]} – ${addDays(monday, 5).getDate()} ${MONTHS[addDays(monday, 5).getMonth()]} ${addDays(monday, 5).getFullYear()}`
  const hours = Array.from({ length: H1 - H0 }, (_, i) => H0 + i)

  const submit = async (body) => {
    await agendaApi.create({ ...body, tipo: 'EVENTO' })
    alertSuccess('Actividad creada')
    setForm(false)
    refetch()
  }

  const remove = async (a) => {
    if (!(await confirmDelete('esta actividad'))) return
    try {
      await agendaApi.remove(a.id)
      alertSuccess('Actividad eliminada')
      setSel(null)
      refetch()
    } catch (err) {
      alertError(getErrorMessage(err))
    }
  }

  const fields = [
    { name: 'titulo', label: 'Título', required: true, maxLength: 200, full: true },
    { name: 'espacioId', label: 'Espacio', type: 'select', options: espacios.data ?? [], number: true, full: true },
    { name: 'fechaHoraInicio', label: 'Inicio', type: 'datetime-local', required: true },
    { name: 'fechaHoraFin', label: 'Fin', type: 'datetime-local', required: true },
    { name: 'responsable', label: 'Responsable', maxLength: 150, full: true },
    { name: 'descripcion', label: 'Descripción', type: 'textarea', maxLength: 500 },
  ]

  return (
    <div className="content wide">
      <div className="page-head">
        <div>
          <h1>Agenda institucional</h1>
          <p>Cursos, diplomados, eventos, alquileres y catering en una sola línea de tiempo</p>
        </div>
        <div className="week-nav">
          <button type="button" className="icon-btn" aria-label="Semana anterior" onClick={() => setMonday(addDays(monday, -7))}>‹</button>
          <span><b>{range}</b></span>
          <button type="button" className="icon-btn" aria-label="Semana siguiente" onClick={() => setMonday(addDays(monday, 7))}>›</button>
          <button type="button" className="btn-outline" onClick={() => setMonday(mondayOf(new Date()))}>Hoy</button>
          {staff && <button type="button" className="btn-primary" onClick={() => setForm(true)}>Nueva actividad</button>}
        </div>
      </div>

      <div className="toolbar">
        <div className="legend-inline">{LEGEND.map(([c, l]) => <span key={c}><i className={`dot ${c}`} />{l}</span>)}</div>
        <div className="spacer" />
        <select className="select-pill" value={espacioId} onChange={(e) => setEspacioId(e.target.value)} aria-label="Filtrar por espacio">
          <option value="">Espacio: Todos</option>
          {(espacios.data ?? []).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      </div>

      <ErrorMessage message={error} onRetry={refetch} />
      {loading && !data && <Loading />}

      {data && (
        <div className="agenda-wrap">
          <div className="agenda-grid">
            <div className="corner" />
            {DAYS.map((d, i) => {
              const date = addDays(monday, i)
              return (
                <div key={d} className={`day-head${toISODate(date) === todayIso ? ' today' : ''}`}>
                  <div className="dname">{d}</div>
                  <div className="ddate">{date.getDate()} {MONTHS[date.getMonth()]}</div>
                </div>
              )
            })}

            <div>{hours.map((h) => <div key={h} className="hour-label">{h % 12 || 12}:00</div>)}</div>
            {columns.map((evs, i) => (
              <div key={i} className="day-col" style={{ height: (H1 - H0) * PX }}>
                {evs.map(({ a, start, end, os, oe, lane, lanes }) => {
                  const top = (start.getHours() + start.getMinutes() / 60 - H0) * PX
                  const colH = (H1 - H0) * PX
                  const height = Math.min(Math.max(22, ((end - start) / 3600000) * PX - 2), colH - top)
                  const bad = conflictIds.has(a.id)
                  return (
                    <button
                      key={`${a.id}-${i}`}
                      type="button"
                      className={`event ${bad ? 'conflict' : String(a.tipo).toLowerCase()}`}
                      style={{ top, height, left: `calc(${(lane / lanes) * 100}% + 3px)`, width: `calc(${100 / lanes}% - 6px)` }}
                      onClick={() => setSel(a)}
                    >
                      <span className="t">{timeLabel(os)}–{timeLabel(oe)}{toISODate(os) !== toISODate(oe) ? ' (varios días)' : ''}</span>
                      <span className="n">{a.titulo}{a.espacio?.nombre ? ` · ${a.espacio.nombre}` : ''}</span>
                    </button>
                  )
                })}
              </div>
            ))}
          </div>
        </div>
      )}

      {data?.conflictos?.length > 0 && (
        <div className="alert-bar" style={{ marginTop: 18, alignItems: 'flex-start' }}>
          <Icon name="alert" />
          <span>
            <b>{data.conflictos.length} conflicto(s) de horario en la semana.</b>
            {data.conflictos.slice(0, 3).map((c, i) => (
              <span key={i} style={{ display: 'block' }}>
                {c.espacio?.nombre ?? 'Espacio'}: «{c.actividadA?.titulo}» y «{c.actividadB?.titulo}» se solapan {c.minutosTraslape} min ({dateTimeLabel(c.desde)}).
              </span>
            ))}
          </span>
        </div>
      )}

      {sel && (
        <Modal
          title={sel.titulo}
          onClose={() => setSel(null)}
          footer={staff && sel.editable && <button type="button" className="btn-danger" onClick={() => remove(sel)}>Eliminar</button>}
        >
          <dl className="kv">
            <dt>Tipo</dt><dd>{humanize(sel.tipo)}</dd>
            <dt>Espacio</dt><dd>{sel.espacio?.nombre ?? '—'}</dd>
            <dt>Inicio</dt><dd>{dateTimeLabel(sel.fechaHoraInicio)}</dd>
            <dt>Fin</dt><dd>{dateTimeLabel(sel.fechaHoraFin)}</dd>
            <dt>Responsable</dt><dd>{sel.responsable ?? '—'}</dd>
            {sel.descripcion && <><dt>Descripción</dt><dd>{sel.descripcion}</dd></>}
            {!sel.editable && <><dt>Origen</dt><dd>Generada por otro módulo (curso, alquiler o catering); se gestiona desde allí.</dd></>}
          </dl>
        </Modal>
      )}

      {form && <FormModal title="Nueva actividad (evento)" fields={fields} onSubmit={submit} onClose={() => setForm(false)} />}
    </div>
  )
}
