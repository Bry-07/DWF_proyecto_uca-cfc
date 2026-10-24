import { useCallback } from 'react'
import { Link } from 'react-router-dom'
import { pagosApi } from '../../api/services'
import DataTable from '../../components/DataTable'
import ErrorMessage from '../../components/ErrorMessage'
import StatusBadge from '../../components/StatusBadge'
import { useFetch } from '../../hooks/useFetch'
import { money, shortDate } from '../../utils/format'
import { humanize } from '../../utils/labels'
import FinanceSummary from './FinanceSummary'
import { monthRangeNow } from './shared'

// Pagos que requieren seguimiento: pendientes y parciales, los más recientes primero.
async function loadPorGestionar() {
  const [pend, parc] = await Promise.all([
    pagosApi.list({ estado: 'PENDIENTE', size: 6 }),
    pagosApi.list({ estado: 'PARCIAL', size: 6 }),
  ])
  return { items: [...pend.items, ...parc.items].sort((a, b) => b.id - a.id).slice(0, 8), pend: pend.total, parc: parc.total }
}

export default function FinanceDashboard() {
  const reportFn = useCallback(() => pagosApi.reporte(monthRangeNow()), [])
  const report = useFetch(reportFn)
  const gest = useFetch(loadPorGestionar)

  return (
    <>
      <FinanceSummary report={report} />

      <div className="page-head" style={{ marginBottom: 12 }}>
        <div>
          <div className="section-title" style={{ marginBottom: 0 }}>Pagos por gestionar</div>
          <p>{gest.data ? `${gest.data.pend} pendientes · ${gest.data.parc} parciales` : ' '}</p>
        </div>
        <Link className="link-action" to="/pagos">Ir a pagos</Link>
      </div>
      <ErrorMessage message={gest.error} onRetry={gest.refetch} />
      <DataTable
        loading={gest.loading}
        rows={gest.data?.items}
        empty="No hay pagos pendientes de gestión."
        columns={[
          { header: 'Código', className: 'code', render: (r) => r.codigo },
          { header: 'Cliente', className: 'client-cell', render: (r) => <>{r.cliente?.nombre ?? '—'}<span className="sub-text">{humanize(r.concepto)}</span></> },
          { header: 'Esperado', className: 'num', render: (r) => money(r.montoEsperado) },
          { header: 'Saldo', className: 'num', render: (r) => money(r.saldo) },
          { header: 'Último pago', className: 'code', render: (r) => shortDate(r.fechaPago) },
          { header: 'Estado', render: (r) => <StatusBadge value={r.estado} /> },
        ]}
      />
    </>
  )
}
