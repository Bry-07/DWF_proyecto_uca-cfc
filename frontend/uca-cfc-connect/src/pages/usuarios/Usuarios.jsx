import { useCallback, useState } from 'react'
import { idOf, opt } from '../../api/lookups'
import { rolesApi, usuariosApi } from '../../api/services'
import DataTable from '../../components/DataTable'
import FormModal from '../../components/FormModal'
import StatusBadge from '../../components/StatusBadge'
import { useDebounce } from '../../hooks/useDebounce'
import { useFetch } from '../../hooks/useFetch'
import { useLayoutSearch } from '../../hooks/useLayoutSearch'
import { usePagedList } from '../../hooks/usePagedList'
import { alertError, alertSuccess, confirmAction } from '../../utils/alerts'
import { getErrorMessage } from '../../utils/errorHandler'
import { humanize } from '../../utils/labels'

const PASS = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{10,72}$/

export default function Usuarios() {
  const { search } = useLayoutSearch('Buscar usuario por nombre o correo...')
  const dq = useDebounce(search)
  const [estado, setEstado] = useState('')
  const list = usePagedList(usuariosApi.list, { texto: dq || undefined, estado: estado || undefined })
  const rolesFn = useCallback(() => rolesApi.list().then((r) => opt(r, (x) => humanize(x.nombre))), [])
  const roles = useFetch(rolesFn)
  const [form, setForm] = useState(null)

  const fields = (editing) => [
    { name: 'nombre', label: 'Nombre', required: true, maxLength: 150 },
    { name: 'email', label: 'Correo', type: 'email', required: true, maxLength: 150 },
    { name: 'rolId', label: 'Rol', type: 'select', options: roles.data ?? [], required: true, number: true },
    {
      name: 'password',
      label: editing ? 'Nueva contraseña (opcional)' : 'Contraseña',
      type: 'password',
      required: !editing,
      pattern: PASS,
      patternMessage: 'Mínimo 10 caracteres, con mayúscula, minúscula y número.',
      hint: 'Mínimo 10 caracteres, con mayúscula, minúscula y número.',
    },
  ]

  const submit = async (body) => {
    if (form.item) await usuariosApi.update(form.item.id, body)
    else await usuariosApi.create(body)
    alertSuccess('Usuario guardado')
    setForm(null)
    list.refetch()
  }

  const toggle = async (u) => {
    const next = u.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'
    if (!(await confirmAction({ title: `¿${next === 'INACTIVO' ? 'Desactivar' : 'Activar'} a ${u.nombre}?`, danger: next === 'INACTIVO' }))) return
    try {
      await usuariosApi.setEstado(u.id, { estado: next })
      alertSuccess('Estado actualizado')
      list.refetch()
    } catch (err) {
      alertError(getErrorMessage(err))
    }
  }

  return (
    <div className="content">
      <div className="page-head">
        <div>
          <h1>Usuarios y roles</h1>
          <p>Cuentas del sistema y su rol de acceso — {list.total} usuarios</p>
        </div>
        <button type="button" className="btn-primary" disabled={!roles.data} onClick={() => setForm({})}>Nuevo usuario</button>
      </div>

      <div className="filters">
        {[['', 'Todos'], ['ACTIVO', 'Activos'], ['INACTIVO', 'Inactivos']].map(([v, l]) => (
          <button key={v} type="button" className={`chip${estado === v ? ' on' : ''}`} onClick={() => setEstado(v)}>{l}</button>
        ))}
      </div>

      <DataTable
        {...list}
        rows={list.items}
        onRetry={list.refetch}
        columns={[
          { header: 'Nombre', className: 'client-cell', render: (r) => r.nombre },
          { header: 'Correo', render: (r) => r.email },
          { header: 'Rol', render: (r) => <span className="badge proceso">{r.rol?.nombre ?? '—'}</span> },
          { header: 'Estado', render: (r) => <StatusBadge value={r.estado} /> },
          {
            header: '',
            render: (r) => (
              <div className="row-actions">
                <button type="button" className="link-action" onClick={() => setForm({ item: r })}>Editar</button>
                <button type="button" className="link-action" onClick={() => toggle(r)}>{r.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'}</button>
              </div>
            ),
          },
        ]}
      />

      {form && (
        <FormModal
          title={form.item ? 'Editar usuario' : 'Nuevo usuario'}
          fields={fields(Boolean(form.item))}
          initial={form.item && { ...form.item, rolId: idOf(form.item.rol) }}
          onSubmit={submit}
          onClose={() => setForm(null)}
        />
      )}
    </div>
  )
}
