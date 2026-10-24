import { useCallback, useState } from 'react'
import { fetchAll, opt } from '../../api/lookups'
import { clientesApi, pagosApi } from '../../api/services'
import DataTable from '../../components/DataTable'
import ErrorMessage from '../../components/ErrorMessage'
import FormModal from '../../components/FormModal'
import Icon from '../../components/Icon'
import StatusBadge from '../../components/StatusBadge'
import { useAuth } from '../../hooks/useAuth'
import { useFetch } from '../../hooks/useFetch'
import { usePagedList } from '../../hooks/usePagedList'
import { alertError, alertSuccess, confirmAction } from '../../utils/alerts'
import { monthRange } from '../../utils/dates'
import { getErrorMessage } from '../../utils/errorHandler'
import { money, shortDate } from '../../utils/format'
import { ENUMS, enumOptions, humanize } from '../../utils/labels'
import { isFinance } from '../../utils/permissions'

const METHOD_COLORS = { TRANSFERENCIA: 'var(--teal)', TARJETA: 'var(--slate)', EFECTIVO: 'var(--amber)', DEPOSITO: 'var(--muted-2)' }

export default function Pagos() {
  const { user, role } = useAuth()
  const finance = isFinance(role)
  const [estado, setEstado] = useState('')
  const [month, setMonth] = useState('')
  const range = monthRange(month)
  const rangeKey = `${range.desde ?? ''}|${range.hasta ?? ''}`

  const clienteId = finance ? undefined : user?.clienteId
  const list = usePagedList(pagosApi.list, { estado: estado || undefined, clienteId, ...range })
  // El reporte de estados financieros es solo para ADMIN/CONTABILIDAD (el backend responde 403 al resto).
  // El cliente ve el mismo resumen, calculado a partir de sus propios pagos.
  const reportFn = useCallback(async () => {
    const [desde, hasta] = rangeKey.split('|')
    if (finance) return pagosApi.reporte(rangeKey === '|' ? {} : { desde, hasta })
    const { items } = await pagosApi.list({ clienteId, size: 200, desde, hasta })
    const sum = (arr, k) => arr.reduce((t, x) => t + Number(x[k] ?? 0), 0)
    const group = (key) => [...new Set(items.map((x) => x[key]))].map((v) => ({ v, rows: items.filter((x) => x[key] === v) }))
    return {
      totalCobrado: sum(items, 'montoPagado'),
      cantidadPagos: items.length,
      porEstado: group('estado').map(({ v, rows }) => ({ estado: v, cantidad: rows.length, saldo: sum(rows, 'saldo') })),
      porMetodo: group('metodo').map(({ v, rows }) => ({ metodo: v, montoPagado: sum(rows, 'montoPagado') })),
    }
  }, [rangeKey, finance, clienteId])
  const report = useFetch(reportFn)
  const clientsFn = useCallback(() => (finance ? fetchAll(clientesApi).then((c) => opt(c)) : Promise.resolve([])), [finance])
  const clients = useFetch(clientsFn)

  const [form, setForm] = useState(false)
  const [abono, setAbono] = useState(null)

  const refreshAll = () => { list.refetch(); report.refetch() }

  const byEstado = (e) => report.data?.porEstado?.find((x) => x.estado === e)
  const methods = report.data?.porMetodo ?? []
  const methodsTotal = methods.reduce((s, m) => s + Number(m.montoPagado ?? 0), 0)

  const act = async (kind, p) => {
    const verb = kind === 'validar' ? 'Validar' : 'Confirmar'
    if (!(await confirmAction({ title: `¿${verb} el pago ${p.codigo}?`, text: `Quedará registrado a nombre de ${user.nombre}.`, confirmText: `Sí, ${verb.toLowerCase()}` }))) return
    try {
      await pagosApi[kind](p.id, user.nombre)
      alertSuccess(kind === 'validar' ? 'Pago validado' : 'Pago confirmado')
      refreshAll()
    } catch (err) {
      alertError(getErrorMessage(err))
    }
  }

  const pagoFields = [
    { name: 'clienteId', label: 'Cliente', type: 'select', options: clients.data ?? [], required: true, number: true, full: true },
    { name: 'concepto', label: 'Concepto', type: 'select', options: enumOptions(ENUMS.CONCEPTO_PAGO), required: true },
    { name: 'referenciaId', label: 'ID del documento (inscripción, cotización…)', type: 'number', required: true, min: 1 },
    { name: 'montoEsperado', label: 'Monto esperado (USD)', type: 'number', step: '0.01', required: true, min: 0 },
    { name: 'montoPagado', label: 'Monto recibido (USD)', type: 'number', step: '0.01', min: 0 },
    { name: 'metodo', label: 'Método', type: 'select', options: enumOptions(ENUMS.METODO_PAGO), required: true },
    { name: 'fechaPago', label: 'Fecha de pago', type: 'date' },
    { name: 'referenciaBancaria', label: 'Referencia bancaria', maxLength: 100, full: true },
  ]

  return (
    <div className="content">
      <div className="page-head">
        <div>
          <h1>Control de pagos</h1>
          <p>Inscripciones, cotizaciones, alquileres y catering{month ? ` — ${month}` : ' — acumulado'}</p>
        </div>
        {finance && <button type="button" className="btn-primary" disabled={!clients.data} onClick={() => setForm(true)}>Registrar pago</button>}
      </div>

      <ErrorMessage message={report.error} onRetry={report.refetch} />
      <div className="summary-row">
        <div className="sum-card dark">
          <div className="lbl"><Icon name="pagos" />Cobrado</div>
          <div className="val">{money(report.data?.totalCobrado)}</div>
          <div className="sub">{report.data?.cantidadPagos ?? 0} pagos registrados</div>
        </div>
        <div className="sum-card">
          <div className="lbl"><Icon name="alert" style={{ color: 'var(--amber)' }} />Pendiente</div>
          <div className="val" style={{ color: 'var(--amber)' }}>{money(byEstado('PENDIENTE')?.saldo)}</div>
          <div className="sub">{byEstado('PENDIENTE')?.cantidad ?? 0} pagos por confirmar</div>
        </div>
        <div className="sum-card">
          <div className="lbl"><Icon name="alert" style={{ color: 'var(--slate)' }} />Parcial</div>
          <div className="val" style={{ color: 'var(--slate)' }}>{money(byEstado('PARCIAL')?.saldo)}</div>
          <div className="sub">{byEstado('PARCIAL')?.cantidad ?? 0} pagos en abono</div>
        </div>
        <div className="sum-card">
          <div className="lbl">Métodos de pago</div>
          <div className="methods">
            {methods.length === 0 && <span className="sub">Sin pagos en el periodo.</span>}
            {methods.map((m) => {
              const pct = methodsTotal > 0 ? Math.round((Number(m.montoPagado) / methodsTotal) * 100) : 0
              return (
                <div key={m.metodo} className="method-row">
                  <span className="name">{humanize(m.metodo)}</span>
                  <div className="method-bar"><div className="method-fill" style={{ width: `${pct}%`, background: METHOD_COLORS[m.metodo] }} /></div>
                  <span className="pct">{pct}%</span>
                </div>
              )
            })}
          </div>
        </div>
      </div>

      <div className="filters">
        <button type="button" className={`chip${estado === '' ? ' on' : ''}`} onClick={() => setEstado('')}>Todos</button>
        {ENUMS.ESTADO_PAGO.map((e) => <button key={e} type="button" className={`chip${estado === e ? ' on' : ''}`} onClick={() => setEstado(e)}>{humanize(e)}</button>)}
        <span className="spacer" />
        <input type="month" className="select-pill" value={month} onChange={(e) => setMonth(e.target.value)} aria-label="Filtrar por mes" />
        {month && <button type="button" className="link-action" onClick={() => setMonth('')}>Quitar mes</button>}
      </div>

      <DataTable
        {...list}
        rows={list.items}
        onRetry={list.refetch}
        empty="No hay pagos con esos filtros."
        columns={[
          { header: 'ID', className: 'code', render: (r) => r.codigo },
          { header: 'Cliente', className: 'client-cell', render: (r) => r.cliente?.nombre ?? '—' },
          { header: 'Concepto', className: 'concept', render: (r) => <>{humanize(r.concepto)} · #{r.referenciaId}</> },
          { header: 'Método', render: (r) => humanize(r.metodo) },
          { header: 'Monto', className: 'num', render: (r) => <>{money(r.montoPagado)}<span className="sub-text">de {money(r.montoEsperado)}</span></> },
          { header: 'Estado', render: (r) => <><StatusBadge value={r.estado} />{r.fechaConfirmacion ? <span className="sub-text">Confirmado</span> : r.fechaValidacion ? <span className="sub-text">Validado</span> : null}</> },
          { header: 'Fecha', render: (r) => shortDate(r.fechaPago) },
          {
            header: '',
            render: (r) =>
              finance && (
                <div className="row-actions">
                  {Number(r.saldo) > 0 && <button type="button" className="link-action" onClick={() => setAbono(r)}>Abono</button>}
                  {!r.fechaValidacion && Number(r.montoPagado) > 0 && <button type="button" className="link-action" onClick={() => act('validar', r)}>Validar</button>}
                  {r.fechaValidacion && !r.fechaConfirmacion && <button type="button" className="link-action" onClick={() => act('confirmar', r)}>Confirmar</button>}
                </div>
              ),
          },
        ]}
      />

      {form && (
        <FormModal wide title="Registrar pago" fields={pagoFields} onSubmit={async (b) => { await pagosApi.create(b); alertSuccess('Pago registrado'); setForm(false); refreshAll() }} onClose={() => setForm(false)} />
      )}
      {abono && (
        <FormModal
          title={`Abono · ${abono.codigo}`}
          fields={[
            { name: 'monto', label: `Monto (saldo ${money(abono.saldo)})`, type: 'number', step: '0.01', required: true, min: 0.01 },
            { name: 'referenciaBancaria', label: 'Referencia bancaria', maxLength: 100 },
          ]}
          onSubmit={async (b) => { await pagosApi.abono(abono.id, b); alertSuccess('Abono registrado'); setAbono(null); refreshAll() }}
          onClose={() => setAbono(null)}
        />
      )}
    </div>
  )
}
