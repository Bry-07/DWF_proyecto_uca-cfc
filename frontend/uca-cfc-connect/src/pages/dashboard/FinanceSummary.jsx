import { useState } from 'react'
import { money } from '../../utils/format'
import { humanize } from '../../utils/labels'
import ErrorMessage from '../../components/ErrorMessage'
import StatCard from '../../components/StatCard'
import { MONTH_NAMES } from './shared'

// Bloque de cifras del mes (estados financieros). Lo usan Administración y Contabilidad.
export default function FinanceSummary({ report }) {
  const { data, loading, error, refetch } = report
  const methods = data?.porMetodo ?? []
  const total = methods.reduce((s, m) => s + Number(m.montoPagado ?? 0), 0)
  const [mes] = useState(() => MONTH_NAMES[new Date().getMonth()])

  return (
    <>
      <ErrorMessage message={error} onRetry={refetch} />
      <div className="grid" style={{ marginBottom: 24 }}>
        <StatCard dark label={`Cobrado en ${mes}`} value={money(data?.totalCobrado)} sub={`${data?.cantidadPagos ?? 0} pagos registrados`} loading={loading} />
        <StatCard label="Por cobrar" value={money(data?.totalPorCobrar)} sub="Saldo de pagos pendientes y parciales" loading={loading} />
        <div className="sum-card">
            <div className="lbl">Cobrado por método</div>
            {methods.length === 0 ? (
              <div className="sub">{loading ? 'Cargando…' : 'Sin pagos en el período.'}</div>
            ) : (
              <div className="methods" style={{ display: 'grid', gap: 8 }}>
                {methods.map((m) => {
                  const pct = total ? Math.round((Number(m.montoPagado ?? 0) / total) * 100) : 0
                  return (
                    <div className="method-row" key={m.metodo}>
                      <span className="name">{humanize(m.metodo)}</span>
                      <div className="method-bar"><div className="method-fill" style={{ width: `${pct}%` }} /></div>
                      <span className="pct">{pct}%</span>
                    </div>
                  )
                })}
              </div>
            )}
        </div>
      </div>
    </>
  )
}
