import { Link } from 'react-router-dom'
import { useCallback } from 'react'
import { cotizacionesApi, inscripcionesApi, pagosApi } from '../../api/services'
import DataTable from '../../components/DataTable'
import ErrorMessage from '../../components/ErrorMessage'
import StatCard from '../../components/StatCard'
import StatusBadge from '../../components/StatusBadge'
import { useAuth } from '../../hooks/useAuth'
import { useFetch } from '../../hooks/useFetch'
import { money, shortDate } from '../../utils/format'
import { humanize } from '../../utils/labels'

// Portal del cliente: solo lo suyo (se filtra por clienteId del perfil; el backend también lo restringe).
export default function ClientDashboard() {
  const { user } = useAuth()
  const clienteId = user?.clienteId
  const load = useCallback(async () => {
    const p = { clienteId, size: 50 }
    const [insc, cot, pag] = await Promise.all([inscripcionesApi.list(p), cotizacionesApi.list(p), pagosApi.list(p)])
    return { insc, cot, pag }
  }, [clienteId])
  const { data, loading, error, refetch } = useFetch(load)

  const activas = data?.insc.items.filter((i) => ['PENDIENTE', 'CONFIRMADA'].includes(i.estado)).length
  const abiertas = data?.cot.items.filter((c) => ['PENDIENTE', 'EN_PROCESO'].includes(c.estado)).length
  const saldo = data?.pag.items.filter((p) => p.estado !== 'PAGADO').reduce((s, p) => s + Number(p.saldo ?? 0), 0)

  return (
    <>
      <ErrorMessage message={error} onRetry={refetch} />

      <div className="grid" style={{ marginBottom: 24 }}>
        <StatCard label="Inscripciones activas" icon="inscripciones" value={activas} sub="Pendientes o confirmadas" loading={loading} />
        <StatCard label="Cotizaciones abiertas" icon="cotizaciones" value={abiertas} sub="En espera de respuesta" loading={loading} />
        <StatCard dark label="Saldo pendiente" icon="pagos" value={money(saldo)} sub="Pagos sin completar" loading={loading} />
      </div>

      <div className="page-head" style={{ marginBottom: 12 }}>
        <div className="section-title" style={{ marginBottom: 0 }}>Mis inscripciones</div>
        <span>
          <Link className="link-action" to="/cursos">Ver cursos y diplomados</Link>{' · '}
          <Link className="link-action" to="/inscripciones">Ver todas</Link>
        </span>
      </div>
      <DataTable
        loading={loading}
        rows={data?.insc.items.slice(0, 5)}
        empty="Aún no tienes inscripciones. Explora los cursos y diplomados disponibles."
        columns={[
          { header: 'Código', className: 'code', render: (r) => r.codigo },
          { header: 'Programa', className: 'client-cell', render: (r) => <>{r.oferta?.nombre ?? '—'}<span className="sub-text">{humanize(r.tipoOferta)}</span></> },
          { header: 'Fecha', className: 'code', render: (r) => shortDate(r.fechaInscripcion) },
          { header: 'Monto', className: 'num', render: (r) => money(r.montoTotal) },
          { header: 'Estado', render: (r) => <StatusBadge value={r.estado} /> },
        ]}
      />

      <div className="layout-2" style={{ marginTop: 24 }}>
        <div>
          <div className="page-head" style={{ marginBottom: 12 }}>
            <div className="section-title" style={{ marginBottom: 0 }}>Mis cotizaciones</div>
            <Link className="link-action" to="/cotizaciones">Ver todas</Link>
          </div>
          <DataTable
            loading={loading}
            rows={data?.cot.items.slice(0, 5)}
            empty="No tienes cotizaciones."
            columns={[
              { header: 'Código', className: 'code', render: (r) => r.codigo },
              { header: 'Asunto', className: 'client-cell', render: (r) => r.asunto },
              { header: 'Total', className: 'num', render: (r) => money(r.total) },
              { header: 'Estado', render: (r) => <StatusBadge value={r.estado} /> },
            ]}
          />
        </div>
        <div>
          <div className="page-head" style={{ marginBottom: 12 }}>
            <div className="section-title" style={{ marginBottom: 0 }}>Mis pagos</div>
            <Link className="link-action" to="/pagos">Ver todos</Link>
          </div>
          <DataTable
            loading={loading}
            rows={data?.pag.items.slice(0, 5)}
            empty="No tienes pagos registrados."
            columns={[
              { header: 'Código', className: 'code', render: (r) => r.codigo },
              { header: 'Saldo', className: 'num', render: (r) => money(r.saldo) },
              { header: 'Estado', render: (r) => <StatusBadge value={r.estado} /> },
            ]}
          />
        </div>
      </div>
    </>
  )
}
