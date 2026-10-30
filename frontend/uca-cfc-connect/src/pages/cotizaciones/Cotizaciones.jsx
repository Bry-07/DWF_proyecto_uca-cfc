import { useState } from 'react'
import { Link, Route, Routes } from 'react-router-dom'
import { cotizacionesApi } from '../../api/services'
import DataTable from '../../components/DataTable'
import Timeline from '../../components/Timeline'
import Modal from '../../components/Modal'
import StatusBadge from '../../components/StatusBadge'
import { useAuth } from '../../hooks/useAuth'
import { usePagedList } from '../../hooks/usePagedList'
import { alertError, alertSuccess, confirmAction, promptText } from '../../utils/alerts'
import { getErrorMessage } from '../../utils/errorHandler'
import { money, shortDate } from '../../utils/format'
import { ENUMS, humanize } from '../../utils/labels'
import { canResolveQuotation } from '../../utils/permissions'
import CotizacionNueva from './CotizacionNueva'

function Detalle({ cot, canResolve, onClose, onDone }) {
  const open = cot.estado === 'PENDIENTE' || cot.estado === 'EN_PROCESO'

  const resolve = async (kind) => {
    const comentario = await promptText({ title: kind === 'aprobar' ? 'Aprobar cotización' : 'Rechazar cotización', label: 'Comentario (opcional)' })
    if (comentario === null) return
    if (!(await confirmAction({ title: kind === 'aprobar' ? '¿Aprobar?' : '¿Rechazar?', danger: kind === 'rechazar' }))) return
    try {
      await cotizacionesApi[kind](cot.id, comentario)
      alertSuccess(kind === 'aprobar' ? 'Cotización aprobada' : 'Cotización rechazada')
      onDone()
    } catch (err) {
      alertError(getErrorMessage(err))
    }
  }

  return (
    <Modal
      wide
      title={`${cot.codigo} · ${cot.asunto}`}
      onClose={onClose}
      footer={
        canResolve && open && (
          <>
            <button type="button" className="btn-danger" onClick={() => resolve('rechazar')}>Rechazar</button>
            <button type="button" className="btn-primary" onClick={() => resolve('aprobar')}>Aprobar</button>
          </>
        )
      }
    >
      <Timeline estado={cot.estado} />
      <dl className="kv" style={{ marginBottom: 20 }}>
        <dt>Cliente</dt><dd>{cot.cliente?.nombre ?? '—'}</dd>
        <dt>Tipo</dt><dd>{humanize(cot.tipo)}</dd>
        <dt>Solicitada</dt><dd>{shortDate(cot.fechaSolicitud)} · vence {shortDate(cot.fechaVencimiento)}</dd>
        <dt>Responsable</dt><dd>{cot.responsableRevision ?? '—'}</dd>
        <dt>Nota interna</dt><dd>{cot.notaInterna ?? '—'}</dd>
      </dl>
      <DataTable
        rows={cot.items ?? []}
        columns={[
          { header: 'Concepto', render: (r) => <>{r.concepto}<span className="sub-text">{humanize(r.tipo)}{r.detalle ? ` · ${r.detalle}` : ''}</span></> },
          { header: 'Cant.', className: 'num', render: (r) => r.cantidad },
          { header: 'Precio', className: 'num', render: (r) => money(r.precioUnitario) },
          { header: 'Subtotal', className: 'num', render: (r) => money(r.subtotal) },
        ]}
      />
      <p style={{ textAlign: 'right', marginTop: 14, fontFamily: 'IBM Plex Mono', fontWeight: 600 }}>Total: {money(cot.total)}</p>
    </Modal>
  )
}

function Lista() {
  const { role } = useAuth()
  const canResolve = canResolveQuotation(role)
  const [estado, setEstado] = useState('')
  const list = usePagedList(cotizacionesApi.list, { estado: estado || undefined })
  const [sel, setSel] = useState(null)

  return (
    <div className="content">
      <div className="page-head">
        <div>
          <h1>Cotizaciones</h1>
          <p>Combina cursos, espacios y catering en una sola propuesta — {list.total} registros</p>
        </div>
        <Link to="nueva" className="btn-primary">Solicitar cotización</Link>
      </div>

      <div className="filters">
        <button type="button" className={`chip${estado === '' ? ' on' : ''}`} onClick={() => setEstado('')}>Todas</button>
        {ENUMS.ESTADO_COTIZACION.map((e) => <button key={e} type="button" className={`chip${estado === e ? ' on' : ''}`} onClick={() => setEstado(e)}>{humanize(e)}</button>)}
      </div>

      <DataTable
        {...list}
        rows={list.items}
        onRetry={list.refetch}
        empty="No hay cotizaciones con ese estado."
        columns={[
          { header: 'Código', className: 'code', render: (r) => r.codigo },
          { header: 'Cliente', className: 'client-cell', render: (r) => r.cliente?.nombre ?? '—' },
          { header: 'Servicios', render: (r) => <>{r.asunto}<span className="sub-text">{humanize(r.tipo)}</span></> },
          { header: 'Total', className: 'num', render: (r) => money(r.total) },
          { header: 'Estado', render: (r) => <StatusBadge value={r.estado} /> },
          { header: 'Fecha', render: (r) => shortDate(r.fechaSolicitud) },
          { header: '', render: (r) => <button type="button" className="link-action" onClick={() => setSel(r)}>Ver detalle</button> },
        ]}
      />

      {sel && <Detalle cot={sel} canResolve={canResolve} onClose={() => setSel(null)} onDone={() => { setSel(null); list.refetch() }} />}
    </div>
  )
}

export default function Cotizaciones() {
  return (
    <Routes>
      <Route index element={<Lista />} />
      <Route path="nueva" element={<CotizacionNueva />} />
    </Routes>
  )
}
