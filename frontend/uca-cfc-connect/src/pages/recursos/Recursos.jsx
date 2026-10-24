import { useCallback, useState } from 'react'
import { fetchAll, opt } from '../../api/lookups'
import { alquileresApi, clientesApi, espaciosApi } from '../../api/services'
import DataTable from '../../components/DataTable'
import FormModal from '../../components/FormModal'
import StatusBadge from '../../components/StatusBadge'
import { useAuth } from '../../hooks/useAuth'
import { useFetch } from '../../hooks/useFetch'
import { usePagedList } from '../../hooks/usePagedList'
import { alertSuccess } from '../../utils/alerts'
import { ROLES } from '../../utils/constants'
import { dateTimeLabel } from '../../utils/dates'
import { changeEstado } from '../../utils/estadoFlow'
import { money } from '../../utils/format'
import { ENUMS, enumOptions, humanize } from '../../utils/labels'
import { isStaff } from '../../utils/permissions'

const espacioFields = [
  { name: 'nombre', label: 'Nombre', required: true, maxLength: 100 },
  { name: 'tipo', label: 'Tipo', type: 'select', options: enumOptions(ENUMS.TIPO_ESPACIO), required: true },
  { name: 'capacidad', label: 'Capacidad (personas)', type: 'number', required: true, min: 1 },
  { name: 'precioPorHora', label: 'Precio por hora (USD)', type: 'number', step: '0.01', required: true, min: 0 },
  { name: 'ubicacion', label: 'Ubicación', maxLength: 150 },
  { name: 'estado', label: 'Estado', type: 'select', options: enumOptions(ENUMS.ESTADO_ESPACIO), default: 'DISPONIBLE' },
  { name: 'equipamiento', label: 'Equipamiento', type: 'textarea', maxLength: 500 },
]

function Espacios({ staff }) {
  const list = usePagedList(espaciosApi.list, {})
  const [form, setForm] = useState(null)
  const submit = async (body) => {
    if (form.item) await espaciosApi.update(form.item.id, body)
    else await espaciosApi.create(body)
    alertSuccess('Espacio guardado')
    setForm(null)
    list.refetch()
  }
  return (
    <>
      {staff && <div className="toolbar"><span className="spacer" /><button type="button" className="btn-primary" onClick={() => setForm({})}>Nuevo espacio</button></div>}
      <DataTable
        {...list}
        rows={list.items}
        onRetry={list.refetch}
        columns={[
          { header: 'Espacio', className: 'client-cell', render: (r) => <>{r.nombre}<span className="sub-text">{r.ubicacion ?? ''}</span></> },
          { header: 'Tipo', render: (r) => humanize(r.tipo) },
          { header: 'Capacidad', className: 'num', render: (r) => r.capacidad },
          { header: 'Precio/hora', className: 'num', render: (r) => money(r.precioPorHora) },
          { header: 'Estado', render: (r) => <StatusBadge value={r.estado} /> },
          { header: '', render: (r) => staff && <button type="button" className="link-action" onClick={() => setForm({ item: r })}>Editar</button> },
        ]}
      />
      {form && <FormModal title={form.item ? 'Editar espacio' : 'Nuevo espacio'} fields={espacioFields} initial={form.item} onSubmit={submit} onClose={() => setForm(null)} />}
    </>
  )
}

function Alquileres({ staff }) {
  const { user, role } = useAuth()
  const [estado, setEstado] = useState('')
  const list = usePagedList(alquileresApi.list, { estado: estado || undefined })
  const lkFn = useCallback(async () => {
    const [espacios, clientes] = await Promise.all([fetchAll(espaciosApi, { estado: 'DISPONIBLE' }), role === ROLES.CLIENTE ? Promise.resolve([]) : fetchAll(clientesApi)])
    return { espacios: opt(espacios, (e) => `${e.nombre} (cap. ${e.capacidad})`), clientes: opt(clientes) }
  }, [role])
  const lk = useFetch(lkFn)
  const [form, setForm] = useState(false)

  const fields = [
    ...(role === ROLES.CLIENTE ? [] : [{ name: 'clienteId', label: 'Cliente', type: 'select', options: lk.data?.clientes ?? [], required: true, number: true, full: true }]),
    { name: 'espacioId', label: 'Espacio', type: 'select', options: lk.data?.espacios ?? [], required: true, number: true, full: true },
    { name: 'fechaHoraInicio', label: 'Inicio', type: 'datetime-local', required: true },
    { name: 'fechaHoraFin', label: 'Fin', type: 'datetime-local', required: true },
    { name: 'proposito', label: 'Propósito', required: true, maxLength: 255 },
    { name: 'cantidadAsistentes', label: 'Asistentes', type: 'number', required: true, min: 1 },
    { name: 'observaciones', label: 'Observaciones', type: 'textarea', maxLength: 500 },
  ]
  const submit = async (body) => {
    await alquileresApi.create(role === ROLES.CLIENTE ? { ...body, clienteId: user.clienteId } : body)
    alertSuccess('Alquiler registrado')
    setForm(false)
    list.refetch()
  }

  return (
    <>
      <div className="filters">
        <button type="button" className={`chip${estado === '' ? ' on' : ''}`} onClick={() => setEstado('')}>Todos</button>
        {ENUMS.ESTADO_RESERVA.map((e) => <button key={e} type="button" className={`chip${estado === e ? ' on' : ''}`} onClick={() => setEstado(e)}>{humanize(e)}</button>)}
        <span className="spacer" />
        <button type="button" className="btn-primary" disabled={!lk.data} onClick={() => setForm(true)}>Nuevo alquiler</button>
      </div>
      <DataTable
        {...list}
        rows={list.items}
        onRetry={list.refetch}
        columns={[
          { header: 'Código', className: 'code', render: (r) => r.codigo },
          { header: 'Cliente', className: 'client-cell', render: (r) => r.cliente?.nombre ?? '—' },
          { header: 'Espacio', render: (r) => <>{r.espacio?.nombre}<span className="sub-text">{r.proposito}</span></> },
          { header: 'Horario', render: (r) => <>{dateTimeLabel(r.fechaHoraInicio)}<span className="sub-text">→ {dateTimeLabel(r.fechaHoraFin)}</span></> },
          { header: 'Monto', className: 'num', render: (r) => money(r.montoTotal) },
          { header: 'Estado', render: (r) => <StatusBadge value={r.estado} /> },
          { header: '', render: (r) => staff && <button type="button" className="link-action" onClick={() => changeEstado({ api: alquileresApi, id: r.id, estados: ENUMS.ESTADO_RESERVA, current: r.estado, onDone: list.refetch })}>Cambiar estado</button> },
        ]}
      />
      {form && <FormModal wide title="Nuevo alquiler" fields={fields} onSubmit={submit} onClose={() => setForm(false)} />}
    </>
  )
}

export default function Recursos() {
  const { role } = useAuth()
  const staff = isStaff(role)
  const [tab, setTab] = useState(role === ROLES.CLIENTE ? 'alquileres' : 'espacios')
  return (
    <div className="content">
      <div className="page-head">
        <div>
          <h1>Espacios y alquileres</h1>
          <p>Auditorios, aulas y laboratorios del centro, y sus reservas. Los conflictos de horario los valida el sistema.</p>
        </div>
      </div>
      <div className="tabs">
        <button type="button" className={`chip${tab === 'espacios' ? ' on' : ''}`} onClick={() => setTab('espacios')}>Espacios</button>
        <button type="button" className={`chip${tab === 'alquileres' ? ' on' : ''}`} onClick={() => setTab('alquileres')}>Alquileres</button>
      </div>
      {tab === 'espacios' ? <Espacios staff={staff} /> : <Alquileres staff={staff} />}
    </div>
  )
}
