import { useCallback, useState } from 'react'
import { fetchAll, opt } from '../../api/lookups'
import { clientesApi, espaciosApi, serviciosCateringApi, solicitudesCateringApi } from '../../api/services'
import DataTable from '../../components/DataTable'
import FormModal from '../../components/FormModal'
import StatusBadge from '../../components/StatusBadge'
import { useAuth } from '../../hooks/useAuth'
import { useFetch } from '../../hooks/useFetch'
import { usePagedList } from '../../hooks/usePagedList'
import { alertSuccess } from '../../utils/alerts'
import { changeEstado } from '../../utils/estadoFlow'
import { money, shortDate } from '../../utils/format'
import { ENUMS, enumOptions, humanize } from '../../utils/labels'
import { canManageCatalog, isStaff } from '../../utils/permissions'

const servicioFields = [
  { name: 'nombre', label: 'Nombre', required: true, maxLength: 120 },
  { name: 'tipo', label: 'Tipo', type: 'select', options: enumOptions(ENUMS.TIPO_CATERING), required: true },
  { name: 'precioPorPersona', label: 'Precio por persona (USD)', type: 'number', step: '0.01', required: true, min: 0 },
  { name: 'minimoPersonas', label: 'Mínimo de personas', type: 'number', required: true, min: 1 },
  { name: 'activo', label: 'Servicio activo', type: 'checkbox', default: true },
  { name: 'descripcion', label: 'Descripción', type: 'textarea', maxLength: 500 },
]

function Servicios({ canEdit }) {
  const list = usePagedList(serviciosCateringApi.list, {})
  const [form, setForm] = useState(null)
  const submit = async (body) => {
    if (form.item) await serviciosCateringApi.update(form.item.id, body)
    else await serviciosCateringApi.create(body)
    alertSuccess('Servicio guardado')
    setForm(null)
    list.refetch()
  }
  return (
    <>
      {canEdit && <div className="toolbar"><span className="spacer" /><button type="button" className="btn-primary" onClick={() => setForm({})}>Nuevo servicio</button></div>}
      <DataTable
        {...list}
        rows={list.items}
        onRetry={list.refetch}
        columns={[
          { header: 'Código', className: 'code', render: (r) => r.codigo },
          { header: 'Servicio', className: 'client-cell', render: (r) => <>{r.nombre}<span className="sub-text">{r.descripcion ?? ''}</span></> },
          { header: 'Tipo', render: (r) => humanize(r.tipo) },
          { header: 'Por persona', className: 'num', render: (r) => money(r.precioPorPersona) },
          { header: 'Mínimo', className: 'num', render: (r) => r.minimoPersonas },
          { header: 'Estado', render: (r) => <StatusBadge value={r.activo ? 'ACTIVO' : 'INACTIVO'} /> },
          { header: '', render: (r) => canEdit && <button type="button" className="link-action" onClick={() => setForm({ item: r })}>Editar</button> },
        ]}
      />
      {form && <FormModal title={form.item ? 'Editar servicio' : 'Nuevo servicio'} fields={servicioFields} initial={form.item} onSubmit={submit} onClose={() => setForm(null)} />}
    </>
  )
}

function Solicitudes({ staff }) {
  const { user } = useAuth()
  const [estado, setEstado] = useState('')
  const list = usePagedList(solicitudesCateringApi.list, { estado: estado || undefined })
  const lkFn = useCallback(async () => {
    const [servicios, espacios, clientes] = await Promise.all([fetchAll(serviciosCateringApi, { activo: true }), fetchAll(espaciosApi), staff ? fetchAll(clientesApi) : Promise.resolve([])])
    return { servicios: opt(servicios, (s) => `${s.nombre} · ${money(s.precioPorPersona)}/persona`), espacios: opt(espacios), clientes: opt(clientes) }
  }, [staff])
  const lk = useFetch(lkFn)
  const [form, setForm] = useState(false)

  const fields = [
    ...(staff ? [{ name: 'clienteId', label: 'Cliente', type: 'select', options: lk.data?.clientes ?? [], required: true, number: true, full: true }] : []),
    { name: 'servicioId', label: 'Servicio', type: 'select', options: lk.data?.servicios ?? [], required: true, number: true, full: true },
    { name: 'fecha', label: 'Fecha', type: 'date', required: true },
    { name: 'cantidadAsistentes', label: 'Asistentes', type: 'number', required: true, min: 1 },
    { name: 'horaInicio', label: 'Hora de inicio', type: 'time', required: true },
    { name: 'horaFin', label: 'Hora de fin', type: 'time', required: true },
    { name: 'espacioId', label: 'Espacio', type: 'select', options: lk.data?.espacios ?? [], number: true },
    { name: 'lugar', label: 'Otro lugar', maxLength: 255 },
    { name: 'menu', label: 'Menú', type: 'textarea', maxLength: 1000 },
    { name: 'observaciones', label: 'Observaciones', type: 'textarea', maxLength: 500 },
  ]
  const submit = async (body) => {
    await solicitudesCateringApi.create(staff ? body : { ...body, clienteId: user.clienteId })
    alertSuccess('Solicitud registrada')
    setForm(false)
    list.refetch()
  }

  return (
    <>
      <div className="filters">
        <button type="button" className={`chip${estado === '' ? ' on' : ''}`} onClick={() => setEstado('')}>Todas</button>
        {ENUMS.ESTADO_RESERVA.map((e) => <button key={e} type="button" className={`chip${estado === e ? ' on' : ''}`} onClick={() => setEstado(e)}>{humanize(e)}</button>)}
        <span className="spacer" />
        <button type="button" className="btn-primary" disabled={!lk.data} onClick={() => setForm(true)}>Nueva solicitud</button>
      </div>
      <DataTable
        {...list}
        rows={list.items}
        onRetry={list.refetch}
        columns={[
          { header: 'Código', className: 'code', render: (r) => r.codigo },
          { header: 'Cliente', className: 'client-cell', render: (r) => r.cliente?.nombre ?? '—' },
          { header: 'Servicio', render: (r) => <>{r.servicio?.nombre}<span className="sub-text">{r.espacio?.nombre ?? r.lugar ?? ''}</span></> },
          { header: 'Fecha', render: (r) => <>{shortDate(r.fecha)}<span className="sub-text">{r.horaInicio?.slice(0, 5)}–{r.horaFin?.slice(0, 5)}</span></> },
          { header: 'Personas', className: 'num', render: (r) => r.cantidadAsistentes },
          { header: 'Monto', className: 'num', render: (r) => money(r.montoTotal) },
          { header: 'Estado', render: (r) => <StatusBadge value={r.estado} /> },
          { header: '', render: (r) => staff && <button type="button" className="link-action" onClick={() => changeEstado({ api: solicitudesCateringApi, id: r.id, estados: ENUMS.ESTADO_RESERVA, current: r.estado, onDone: list.refetch })}>Cambiar estado</button> },
        ]}
      />
      {form && <FormModal wide title="Nueva solicitud de catering" fields={fields} onSubmit={submit} onClose={() => setForm(false)} />}
    </>
  )
}

export default function Catering() {
  const { role } = useAuth()
  const staff = isStaff(role)
  const [tab, setTab] = useState('solicitudes')
  return (
    <div className="content">
      <div className="page-head">
        <div>
          <h1>Catering</h1>
          <p>Catálogo de servicios y solicitudes de coffee break, almuerzos y más</p>
        </div>
      </div>
      <div className="tabs">
        <button type="button" className={`chip${tab === 'solicitudes' ? ' on' : ''}`} onClick={() => setTab('solicitudes')}>Solicitudes</button>
        <button type="button" className={`chip${tab === 'servicios' ? ' on' : ''}`} onClick={() => setTab('servicios')}>Servicios</button>
      </div>
      {tab === 'solicitudes' ? <Solicitudes staff={staff} /> : <Servicios canEdit={canManageCatalog(role)} />}
    </div>
  )
}
