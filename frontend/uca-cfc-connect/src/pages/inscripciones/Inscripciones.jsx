import { useState } from 'react'
import { Link, Route, Routes } from 'react-router-dom'
import { inscripcionesApi } from '../../api/services'
import DataTable from '../../components/DataTable'
import StatusBadge from '../../components/StatusBadge'
import { useAuth } from '../../hooks/useAuth'
import { usePagedList } from '../../hooks/usePagedList'
import { money, shortDate } from '../../utils/format'
import { ENUMS, humanize } from '../../utils/labels'
import { changeEstado } from '../../utils/estadoFlow'
import { isStaff } from '../../utils/permissions'
import NuevaInscripcion from './NuevaInscripcion'

function Lista() {
  const { role } = useAuth()
  const staff = isStaff(role)
  const [estado, setEstado] = useState('')
  const list = usePagedList(inscripcionesApi.list, { estado: estado || undefined })

  return (
    <div className="content">
      <div className="page-head">
        <div>
          <h1>Inscripciones</h1>
          <p>Vincula clientes con cursos y diplomados — {list.total} registros</p>
        </div>
        <Link to="nueva" className="btn-primary">Nueva inscripción</Link>
      </div>

      <div className="filters">
        <button type="button" className={`chip${estado === '' ? ' on' : ''}`} onClick={() => setEstado('')}>Todas</button>
        {ENUMS.ESTADO_RESERVA.map((e) => <button key={e} type="button" className={`chip${estado === e ? ' on' : ''}`} onClick={() => setEstado(e)}>{humanize(e)}</button>)}
      </div>

      <DataTable
        {...list}
        rows={list.items}
        onRetry={list.refetch}
        empty="No hay inscripciones con ese estado."
        columns={[
          { header: 'Código', className: 'code', render: (r) => r.codigo },
          { header: 'Cliente', className: 'client-cell', render: (r) => r.cliente?.nombre ?? '—' },
          { header: 'Oferta', render: (r) => <>{r.oferta?.nombre ?? '—'}<span className="sub-text">{humanize(r.tipoOferta)}</span></> },
          { header: 'Participante', render: (r) => <>{r.participanteNombre}<span className="sub-text">{r.participanteEmail}</span></> },
          { header: 'Fecha', render: (r) => shortDate(r.fechaInscripcion) },
          { header: 'Monto', className: 'num', render: (r) => money(r.montoTotal) },
          { header: 'Estado', render: (r) => <StatusBadge value={r.estado} /> },
          {
            header: '',
            render: (r) =>
              staff && (
                <button type="button" className="link-action" onClick={() => changeEstado({ api: inscripcionesApi, id: r.id, estados: ENUMS.ESTADO_RESERVA, current: r.estado, onDone: list.refetch })}>
                  Cambiar estado
                </button>
              ),
          },
        ]}
      />
    </div>
  )
}

export default function Inscripciones() {
  return (
    <Routes>
      <Route index element={<Lista />} />
      <Route path="nueva" element={<NuevaInscripcion />} />
    </Routes>
  )
}
