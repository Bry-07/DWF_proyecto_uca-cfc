import { useCallback } from 'react'
import { Link } from 'react-router-dom'
import { agendaApi, clientesApi, cotizacionesApi, inscripcionesApi, pagosApi } from '../../api/services'
import DataTable from '../../components/DataTable'
import ErrorMessage from '../../components/ErrorMessage'
import StatCard from '../../components/StatCard'
import { useFetch } from '../../hooks/useFetch'
import { dateTimeLabel, parseLocal, timeLabel } from '../../utils/dates'
import { money } from '../../utils/format'
import { humanize } from '../../utils/labels'
import FinanceSummary from './FinanceSummary'
import { monthRangeNow, todayRange } from './shared'

async function loadStaff() {
  const hoy = todayRange()
  const semana = todayRange(7)
  const [insc, cot, clientes, agenda, conflictos] = await Promise.all([
    inscripcionesApi.list({ estado: 'PENDIENTE', size: 1 }),
    loadCotizacionesAbiertas(),
    clientesApi.list({ size: 1 }),
    agendaApi.list({ fechaDesde: hoy.desde, fechaHasta: hoy.hasta, size: 50 }),
    agendaApi.conflictos({ fechaDesde: semana.desde, fechaHasta: semana.hasta }),
  ])
  return { insc, cot, clientes, agenda, conflictos }
}

async function loadCotizacionesAbiertas() {
  const paginas = await Promise.all(['PENDIENTE', 'EN_PROCESO'].map((estado) => cotizacionesApi.list({ estado, size: 5 })))
  return {
    total: paginas.reduce((sum, pagina) => sum + pagina.total, 0),
    items: paginas.flatMap((pagina) => pagina.items).sort((a, b) => b.id - a.id).slice(0, 5),
  }
}

// Administración (con cifras financieras) y Recepción (solo operación).
export default function StaffDashboard({ canSeeFinance }) {
  const { data, loading, error, refetch } = useFetch(loadStaff)
  const reportFn = useCallback(() => (canSeeFinance ? pagosApi.reporte(monthRangeNow()) : Promise.resolve(null)), [canSeeFinance])
  const report = useFetch(reportFn)

  const hoy = [...(data?.agenda.items ?? [])].sort((a, b) => String(a.fechaHoraInicio).localeCompare(String(b.fechaHoraInicio)))

  return (
    <>
      <ErrorMessage message={error} onRetry={refetch} />

      <div className="grid cols-4" style={{ marginBottom: 24 }}>
        <StatCard label="Cotizaciones por revisar" icon="cotizaciones" value={data?.cot.total} sub="Pendientes y en proceso" loading={loading} />
        <StatCard label="Inscripciones pendientes" icon="inscripciones" value={data?.insc.total} sub="Por confirmar" loading={loading} />
        <StatCard label="Clientes" icon="clientes" value={data?.clientes.total} sub="Registrados" loading={loading} />
        <StatCard label="Conflictos de agenda" icon="alert" value={data?.conflictos.length} sub="Próximos 7 días" loading={loading} />
      </div>

      {canSeeFinance && <FinanceSummary report={report} />}

      <div className="layout-2">
        <div>
          <div className="page-head" style={{ marginBottom: 12 }}>
            <div className="section-title" style={{ marginBottom: 0 }}>Agenda de hoy</div>
            <Link className="link-action" to="/agenda">Ver agenda</Link>
          </div>
          <DataTable
            loading={loading}
            rows={hoy}
            empty="No hay actividades programadas para hoy."
            columns={[
              { header: 'Horario', className: 'code', render: (r) => `${timeLabel(parseLocal(r.fechaHoraInicio))} – ${timeLabel(parseLocal(r.fechaHoraFin))}` },
              { header: 'Actividad', className: 'client-cell', render: (r) => <>{r.titulo}{r.espacio?.nombre && <span className="sub-text">{r.espacio.nombre}</span>}</> },
              { header: 'Tipo', render: (r) => humanize(r.tipo) },
            ]}
          />
        </div>

        <div>
          <div className="page-head" style={{ marginBottom: 12 }}>
            <div className="section-title" style={{ marginBottom: 0 }}>Cotizaciones por revisar</div>
            <Link className="link-action" to="/cotizaciones">Ver todas</Link>
          </div>
          <DataTable
            loading={loading}
            rows={data?.cot.items}
            empty="No hay cotizaciones pendientes."
            columns={[
              { header: 'Código', className: 'code', render: (r) => r.codigo },
              { header: 'Cliente', className: 'client-cell', render: (r) => <>{r.cliente?.nombre ?? '—'}<span className="sub-text">{r.asunto}</span></> },
              { header: 'Total', className: 'num', render: (r) => money(r.total) },
            ]}
          />
        </div>
      </div>

      {data?.conflictos.length > 0 && (
        <div className="alert-bar" style={{ marginTop: 18, alignItems: 'flex-start' }}>
          <span>
            <b>{data.conflictos.length} conflicto(s) de horario en los próximos 7 días.</b>{' '}
            {data.conflictos[0].espacio?.nombre ?? 'Espacio'}: «{data.conflictos[0].actividadA?.titulo}» y «{data.conflictos[0].actividadB?.titulo}» se solapan ({dateTimeLabel(data.conflictos[0].desde)}). <Link className="link-action" to="/agenda">Resolver en la agenda</Link>
          </span>
        </div>
      )}
    </>
  )
}
