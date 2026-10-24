import { useCallback, useMemo, useState } from 'react'
import { categoriasApi, cursosApi, diplomadosApi, docentesApi, espaciosApi, modalidadesApi } from '../../api/services'
import { fetchAll, idOf, loadOffers, opt } from '../../api/lookups'
import EmptyState from '../../components/EmptyState'
import ErrorMessage from '../../components/ErrorMessage'
import FormModal from '../../components/FormModal'
import Icon from '../../components/Icon'
import Loading from '../../components/Loading'
import Modal from '../../components/Modal'
import Pagination from '../../components/Pagination'
import StatusBadge from '../../components/StatusBadge'
import { useAuth } from '../../hooks/useAuth'
import { useFetch } from '../../hooks/useFetch'
import { useLayoutSearch } from '../../hooks/useLayoutSearch'
import { alertError, alertSuccess } from '../../utils/alerts'
import { ROLES } from '../../utils/constants'
import { getErrorMessage } from '../../utils/errorHandler'
import { money, shortDate } from '../../utils/format'
import { isStaff } from '../../utils/permissions'

const PER_PAGE = 9
const TIPOS = [['TODOS', 'Todos'], ['CURSO', 'Cursos'], ['DIPLOMADO', 'Diplomados']]
const MODS = [['TODAS', 'Cualquier modalidad'], ['PRESENCIAL', 'Presencial'], ['VIRTUAL', 'Virtual'], ['HIBRIDA', 'Híbrida']]

async function loadLookups() {
  const [categorias, modalidades, docentes, espacios] = await Promise.all([
    fetchAll(categoriasApi), modalidadesApi.list(), fetchAll(docentesApi), fetchAll(espaciosApi),
  ])
  return {
    categorias: opt(categorias),
    modalidades: opt(modalidades, (m) => `${m.nombre} (${m.tipo})`),
    docentes: opt(docentes, (d) => d.nombreCompleto ?? `${d.nombres} ${d.apellidos}`),
    espacios: opt(espacios),
  }
}

const offerFields = (lk) => [
  { name: 'codigo', label: 'Código', required: true, placeholder: 'ABC-123', pattern: /^[A-Z]{3}-[0-9]{3}$/, patternMessage: 'Formato: 3 letras mayúsculas, guion y 3 dígitos (CUR-014).' },
  { name: 'nombre', label: 'Nombre', required: true, maxLength: 150 },
  { name: 'categoriaId', label: 'Categoría', type: 'select', options: lk.categorias, required: true, number: true },
  { name: 'modalidadId', label: 'Modalidad', type: 'select', options: lk.modalidades, required: true, number: true },
  { name: 'docenteId', label: 'Docente', type: 'select', options: lk.docentes, number: true },
  { name: 'espacioId', label: 'Espacio / aula', type: 'select', options: lk.espacios, number: true },
  { name: 'fechaInicio', label: 'Fecha de inicio', type: 'date', required: true },
  { name: 'fechaFin', label: 'Fecha de fin', type: 'date', required: true },
  { name: 'horario', label: 'Horario', placeholder: 'Lun–Mié · 6:00 – 8:00 pm', maxLength: 100, full: true },
  { name: 'duracionHoras', label: 'Duración (horas)', type: 'number', required: true, min: 1 },
  { name: 'cupoMaximo', label: 'Cupo máximo', type: 'number', required: true, min: 1 },
  { name: 'precio', label: 'Precio por participante (USD)', type: 'number', step: '0.01', required: true, min: 0 },
  { name: 'descripcion', label: 'Descripción', type: 'textarea', maxLength: 1000 },
]

const toInitial = (it) =>
  it && {
    ...it,
    categoriaId: idOf(it.categoria),
    modalidadId: idOf(it.modalidad),
    docenteId: idOf(it.docente),
    espacioId: idOf(it.espacio),
  }

function CatalogCard({ item, onOpen }) {
  const max = item.cupoMaximo ?? 0
  const used = item.cuposOcupados ?? 0
  const pct = max > 0 ? Math.min(100, Math.round((used / max) * 100)) : 0
  const lleno = max > 0 && used >= max
  const barColor = lleno ? 'var(--red)' : pct >= 85 ? 'var(--amber)' : undefined
  const top = lleno || pct >= 85 ? 'amber' : item.tipo === 'DIPLOMADO' ? 'plum' : item.tipoModalidad === 'VIRTUAL' ? 'slate' : 'teal'

  return (
    <article className="card" style={item.estado === 'INACTIVO' ? { opacity: 0.65 } : undefined}>
      <div className={`card-top ${top}`} />
      <div className="card-body">
        <div className="card-code">
          {item.codigo}
          <StatusBadge value={item.tipoModalidad} />
        </div>
        <h3>{item.nombre}</h3>
        <div className="meta">
          {item.horario && <div className="meta-row"><Icon name="agenda" />{item.horario}</div>}
          {item.espacio?.nombre && <div className="meta-row"><Icon name="espacios" />{item.espacio.nombre}</div>}
          {item.docente?.nombre && <div className="meta-row"><Icon name="clientes" />{item.docente.nombre}</div>}
        </div>
        <div className="cupo-bar"><div className="cupo-fill" style={{ width: `${pct}%`, background: barColor }} /></div>
        <div className="status-row">{used} / {max} cupos{lleno ? ' · lleno' : ''}{item.estado === 'INACTIVO' ? ' · inactivo' : ''}</div>
        <div className="card-foot">
          <div className="price">{money(item.precio)}<small>/participante</small></div>
          <button type="button" className="btn-ghost" onClick={() => onOpen(item)}>Ver detalle</button>
        </div>
      </div>
    </article>
  )
}

export default function Catalogo() {
  const { role } = useAuth()
  const staff = isStaff(role)
  const load = useCallback(() => loadOffers(role === ROLES.CLIENTE), [role])
  const { data, loading, error, refetch } = useFetch(load)
  const lookupsFn = useCallback(() => (staff ? loadLookups() : Promise.resolve(null)), [staff])
  const lookups = useFetch(lookupsFn)
  const { search } = useLayoutSearch('Buscar curso, diplomado o código...')

  const [tipo, setTipo] = useState('TODOS')
  const [mod, setMod] = useState('TODAS')
  const [pageState, setPageState] = useState({ key: '', page: 0 })
  const [detail, setDetail] = useState(null)
  const [form, setForm] = useState(null) // { tipo, item? }

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    return (data ?? []).filter(
      (i) =>
        (tipo === 'TODOS' || i.tipo === tipo) &&
        (mod === 'TODAS' || i.tipoModalidad === mod) &&
        (!q || `${i.nombre} ${i.codigo} ${i.docente?.nombre ?? ''}`.toLowerCase().includes(q)),
    )
  }, [data, tipo, mod, search])

  const key = `${tipo}|${mod}|${search}`
  const page = pageState.key === key ? Math.min(pageState.page, Math.max(0, Math.ceil(filtered.length / PER_PAGE) - 1)) : 0
  const visible = filtered.slice(page * PER_PAGE, (page + 1) * PER_PAGE)
  const apiFor = (t) => (t === 'DIPLOMADO' ? diplomadosApi : cursosApi)

  const submit = async (body) => {
    const api = apiFor(form.tipo)
    if (form.item) await api.update(form.item.id, body)
    else await api.create(body)
    alertSuccess(form.item ? 'Cambios guardados' : 'Registro creado')
    setForm(null)
    setDetail(null)
    refetch()
  }

  const toggleEstado = async (item) => {
    try {
      await apiFor(item.tipo).setEstado(item.id, { estado: item.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO' })
      alertSuccess('Estado actualizado')
      setDetail(null)
      refetch()
    } catch (err) {
      alertError(getErrorMessage(err))
    }
  }

  const formReady = Boolean(lookups.data)

  return (
    <div className="content">
      <div className="page-head">
        <div>
          <h1>Catálogo académico</h1>
          <p>Cursos empresariales, diplomados y talleres del centro{data ? ` — ${data.length} registrados` : ''}</p>
        </div>
        {staff && (
          <div style={{ display: 'flex', gap: 10 }}>
            <button type="button" className="btn-outline" disabled={!formReady} onClick={() => setForm({ tipo: 'DIPLOMADO' })}>Nuevo diplomado</button>
            <button type="button" className="btn-primary" disabled={!formReady} onClick={() => setForm({ tipo: 'CURSO' })}>Nuevo curso</button>
          </div>
        )}
      </div>

      <div className="filters">
        {TIPOS.map(([v, l]) => <button key={v} type="button" className={`chip${tipo === v ? ' on' : ''}`} onClick={() => setTipo(v)}>{l}</button>)}
        <span className="spacer" />
        {MODS.map(([v, l]) => <button key={v} type="button" className={`chip${mod === v ? ' on' : ''}`} onClick={() => setMod(v)}>{l}</button>)}
      </div>

      <ErrorMessage message={error} onRetry={refetch} />
      {staff && lookups.error && <ErrorMessage message={`No se cargaron las listas del formulario: ${lookups.error}`} onRetry={lookups.refetch} />}
      {loading && !data && <Loading />}
      {!loading && !error && filtered.length === 0 && <div className="panel"><EmptyState text="Ajusta los filtros o la búsqueda." /></div>}
      {visible.length > 0 && (
        <>
          <div className="grid">{visible.map((i) => <CatalogCard key={`${i.tipo}-${i.id}`} item={i} onOpen={setDetail} />)}</div>
          <div className="table-wrap" style={{ marginTop: 16 }}>
            <Pagination page={page} size={PER_PAGE} total={filtered.length} onPage={(p) => setPageState({ key, page: p })} />
          </div>
        </>
      )}

      {detail && (
        <Modal
          title={`${detail.codigo} · ${detail.nombre}`}
          onClose={() => setDetail(null)}
          footer={
            staff && (
              <>
                <button type="button" className="btn-outline" onClick={() => toggleEstado(detail)}>{detail.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'}</button>
                <button type="button" className="btn-primary" disabled={!formReady} onClick={() => setForm({ tipo: detail.tipo, item: detail })}>Editar</button>
              </>
            )
          }
        >
          <dl className="kv">
            <dt>Tipo</dt><dd>{detail.tipo === 'DIPLOMADO' ? 'Diplomado' : 'Curso'} · <StatusBadge value={detail.estado} /></dd>
            <dt>Modalidad</dt><dd>{detail.modalidad?.nombre ?? '—'} ({detail.tipoModalidad ?? '—'})</dd>
            <dt>Categoría</dt><dd>{detail.categoria?.nombre ?? '—'}</dd>
            <dt>Docente</dt><dd>{detail.docente?.nombre ?? '—'}</dd>
            <dt>Espacio</dt><dd>{detail.espacio?.nombre ?? '—'}</dd>
            <dt>Fechas</dt><dd>{shortDate(detail.fechaInicio)} → {shortDate(detail.fechaFin)}</dd>
            <dt>Horario</dt><dd>{detail.horario ?? '—'} · {detail.duracionHoras ?? '—'} h</dd>
            <dt>Cupos</dt><dd>{detail.cuposOcupados ?? 0} ocupados · {detail.cuposDisponibles ?? '—'} disponibles de {detail.cupoMaximo}</dd>
            <dt>Precio</dt><dd>{money(detail.precio)}</dd>
            {detail.descripcion && <><dt>Descripción</dt><dd>{detail.descripcion}</dd></>}
          </dl>
        </Modal>
      )}

      {form && lookups.data && (
        <FormModal
          wide
          title={`${form.item ? 'Editar' : 'Nuevo'} ${form.tipo === 'DIPLOMADO' ? 'diplomado' : 'curso'}`}
          fields={offerFields(lookups.data)}
          initial={toInitial(form.item)}
          onSubmit={submit}
          onClose={() => setForm(null)}
        />
      )}
    </div>
  )
}
