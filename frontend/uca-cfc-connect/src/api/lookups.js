import { cursosApi, diplomadosApi } from './services'

// Trae todos los registros de un recurso paginado (catálogos pequeños).
export const fetchAll = (res, params) => res.list({ page: 0, size: 200, ...params }).then((p) => p.items)

// [{id, nombre}] -> opciones de <select>
export const opt = (items, label = (x) => x.nombre) => items.map((x) => ({ value: String(x.id), label: label(x) }))

// Cursos + diplomados en una sola lista, marcados con `tipo`.
export async function loadOffers(onlyActive = false) {
  const params = onlyActive ? { estado: 'ACTIVO' } : {}
  const [cursos, diplomados] = await Promise.all([fetchAll(cursosApi, params), fetchAll(diplomadosApi, params)])
  return [
    ...cursos.map((c) => ({ ...c, tipo: 'CURSO' })),
    ...diplomados.map((d) => ({ ...d, tipo: 'DIPLOMADO' })),
  ]
}
export { idOf } from './adapters'
