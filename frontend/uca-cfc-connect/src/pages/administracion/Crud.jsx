import { useState } from 'react'
import DataTable from '../../components/DataTable'
import FormModal from '../../components/FormModal'
import { usePagedList } from '../../hooks/usePagedList'
import { alertError, alertSuccess, confirmDelete } from '../../utils/alerts'
import { getErrorMessage } from '../../utils/errorHandler'

// Mantenimiento genérico (lista + crear/editar/eliminar) para los catálogos base.
// `locked(row)` bloquea editar/eliminar (p. ej. roles del sistema). Los errores 409 (registro en uso) se muestran tal cual.
export default function Crud({ api, noun, nounPlural, columns, fields, locked = () => false, rowLabel = (r) => r.nombre }) {
  const list = usePagedList(api.listPage ?? api.list)
  const [form, setForm] = useState(null)

  const submit = async (body) => {
    if (form.item) await api.update(form.item.id, body)
    else await api.create(body)
    alertSuccess(`${noun} guardado`)
    setForm(null)
    list.refetch()
  }

  const remove = async (r) => {
    if (!(await confirmDelete(`${noun.toLowerCase()} «${rowLabel(r)}»`))) return
    try {
      await api.remove(r.id)
      alertSuccess(`${noun} eliminado`)
      list.refetch()
    } catch (err) {
      alertError(getErrorMessage(err))
    }
  }

  return (
    <>
      <div className="page-head" style={{ marginBottom: 14 }}>
        <p>{list.total} {nounPlural}</p>
        <button type="button" className="btn-primary" onClick={() => setForm({})}>Nuevo {noun.toLowerCase()}</button>
      </div>

      <DataTable
        {...list}
        rows={list.items}
        onRetry={list.refetch}
        empty={`Aún no hay ${nounPlural}.`}
        columns={[
          ...columns,
          {
            header: '',
            render: (r) => !locked(r) && (
              <div className="row-actions">
                <button type="button" className="link-action" onClick={() => setForm({ item: r })}>Editar</button>
                <button type="button" className="link-action" onClick={() => remove(r)}>Eliminar</button>
              </div>
            ),
          },
        ]}
      />

      {form && (
        <FormModal
          title={form.item ? `Editar ${noun.toLowerCase()}` : `Nuevo ${noun.toLowerCase()}`}
          fields={fields}
          initial={form.item}
          onSubmit={submit}
          onClose={() => setForm(null)}
        />
      )}
    </>
  )
}
