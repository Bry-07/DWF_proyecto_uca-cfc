import { useCallback, useState } from 'react'
import { clientesApi } from '../../api/services'
import DataTable from '../../components/DataTable'
import ErrorMessage from '../../components/ErrorMessage'
import FormModal from '../../components/FormModal'
import Loading from '../../components/Loading'
import Modal from '../../components/Modal'
import StatusBadge from '../../components/StatusBadge'
import { useAuth } from '../../hooks/useAuth'
import { useDebounce } from '../../hooks/useDebounce'
import { useFetch } from '../../hooks/useFetch'
import { useLayoutSearch } from '../../hooks/useLayoutSearch'
import { usePagedList } from '../../hooks/usePagedList'
import { alertSuccess } from '../../utils/alerts'
import { money } from '../../utils/format'
import { ENUMS, enumOptions, humanize } from '../../utils/labels'
import { isStaff } from '../../utils/permissions'

const DUI = /^[0-9]{8}-[0-9]$/
const NIT = /^[0-9]{4}-[0-9]{6}-[0-9]{3}-[0-9]$/
const TEL = /^[0-9]{4}-[0-9]{4}$/

const fields = [
  { name: 'tipo', label: 'Tipo de cliente', type: 'select', options: enumOptions(ENUMS.TIPO_CLIENTE), required: true },
  { name: 'nombre', label: 'Nombre completo', required: true, maxLength: 150 },
  { name: 'email', label: 'Correo', type: 'email', required: true, maxLength: 150 },
  { name: 'telefono', label: 'Teléfono', placeholder: '0000-0000', pattern: TEL, patternMessage: 'Formato 0000-0000.' },
  { name: 'dui', label: 'DUI', placeholder: '00000000-0', pattern: DUI, patternMessage: 'Formato 00000000-0.' },
  { name: 'nit', label: 'NIT', placeholder: '0000-000000-000-0', pattern: NIT, patternMessage: 'Formato 0000-000000-000-0.' },
  { name: 'direccion', label: 'Dirección', maxLength: 255, full: true },
  { name: 'empresa.razonSocial', label: 'Razón social', required: true, show: (v) => v.tipo === 'EMPRESA' },
  { name: 'empresa.giro', label: 'Giro', show: (v) => v.tipo === 'EMPRESA' },
  { name: 'empresa.contactoNombre', label: 'Contacto', show: (v) => v.tipo === 'EMPRESA' },
  { name: 'empresa.contactoCargo', label: 'Cargo del contacto', show: (v) => v.tipo === 'EMPRESA' },
]

function Historial({ cliente, onClose }) {
  const fn = useCallback(() => clientesApi.historial(cliente.id), [cliente.id])
  const { data, loading, error } = useFetch(fn)
  return (
    <Modal title={`Historial · ${cliente.nombre}`} onClose={onClose} wide>
      <ErrorMessage message={error} />
      {loading && <Loading />}
      {data && (
        <>
          <dl className="kv" style={{ marginBottom: 18 }}>
            <dt>Inscripciones</dt><dd>{data.resumen?.totalInscripciones ?? 0}</dd>
            <dt>Cotizaciones</dt><dd>{data.resumen?.totalCotizaciones ?? 0}</dd>
            <dt>Pagos</dt><dd>{data.resumen?.totalPagos ?? 0} · {money(data.resumen?.montoPagado)}</dd>
          </dl>
          <div className="section-title">Inscripciones</div>
          <DataTable
            rows={data.inscripciones ?? []}
            empty="Sin inscripciones."
            columns={[
              { header: 'Código', className: 'code', render: (r) => r.codigo },
              { header: 'Oferta', render: (r) => r.oferta?.nombre ?? '—' },
              { header: 'Participante', render: (r) => r.participanteNombre },
              { header: 'Estado', render: (r) => <StatusBadge value={r.estado} /> },
            ]}
          />
        </>
      )}
    </Modal>
  )
}

export default function Clientes() {
  const { role } = useAuth()
  const staff = isStaff(role)
  const { search } = useLayoutSearch('Buscar cliente por nombre...')
  const dq = useDebounce(search)
  const [tipo, setTipo] = useState('')
  const list = usePagedList(clientesApi.list, { nombre: dq || undefined, tipo: tipo || undefined })
  const [form, setForm] = useState(null) // { item? }
  const [hist, setHist] = useState(null)

  const submit = async (body) => {
    if (form.item) await clientesApi.update(form.item.id, body)
    else await clientesApi.create(body)
    alertSuccess(form.item ? 'Cliente actualizado' : 'Cliente creado')
    setForm(null)
    list.refetch()
  }

  return (
    <div className="content">
      <div className="page-head">
        <div>
          <h1>Clientes</h1>
          <p>Personas particulares y empresas — {list.total} registrados</p>
        </div>
        {staff && <button type="button" className="btn-primary" onClick={() => setForm({})}>Nuevo cliente</button>}
      </div>

      <div className="filters">
        <button type="button" className={`chip${tipo === '' ? ' on' : ''}`} onClick={() => setTipo('')}>Todos</button>
        {ENUMS.TIPO_CLIENTE.map((t) => <button key={t} type="button" className={`chip${tipo === t ? ' on' : ''}`} onClick={() => setTipo(t)}>{humanize(t)}</button>)}
      </div>

      <DataTable
        {...list}
        rows={list.items}
        onRetry={list.refetch}
        empty="No hay clientes con esos filtros."
        columns={[
          { header: 'Cliente', className: 'client-cell', render: (r) => <>{r.nombre}{r.empresa?.razonSocial && <span className="sub-text">{r.empresa.razonSocial}</span>}</> },
          { header: 'Tipo', render: (r) => <span className="badge proceso">{humanize(r.tipo)}</span> },
          { header: 'Correo', render: (r) => r.email },
          { header: 'Teléfono', className: 'code', render: (r) => r.telefono ?? '—' },
          { header: 'DUI / NIT', className: 'code', render: (r) => r.dui ?? r.nit ?? '—' },
          {
            header: '',
            render: (r) => (
              <div className="row-actions">
                <button type="button" className="link-action" onClick={() => setHist(r)}>Historial</button>
                {staff && <button type="button" className="link-action" onClick={() => setForm({ item: r })}>Editar</button>}
              </div>
            ),
          },
        ]}
      />

      {form && (
        <FormModal wide title={form.item ? 'Editar cliente' : 'Nuevo cliente'} fields={fields} initial={form.item} onSubmit={submit} onClose={() => setForm(null)} />
      )}
      {hist && <Historial cliente={hist} onClose={() => setHist(null)} />}
    </div>
  )
}
