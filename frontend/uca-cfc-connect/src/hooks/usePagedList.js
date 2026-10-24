import { useCallback, useState } from 'react'
import { PAGE_SIZE } from '../utils/constants'
import { useFetch } from './useFetch'

// Paginación en servidor (page desde 0). `list` debe ser estable (p. ej. cursosApi.list).
// Al cambiar los filtros, la página vuelve a 0 (derivado de la clave, sin efectos).
export function usePagedList(list, filters = {}, size = PAGE_SIZE) {
  const key = JSON.stringify(filters)
  const [pk, setPk] = useState({ key, page: 0 })
  const page = pk.key === key ? pk.page : 0
  const fetcher = useCallback(() => list({ ...JSON.parse(key), page, size }), [list, key, page, size])
  const { data, loading, error, refetch } = useFetch(fetcher)
  return {
    items: data?.items ?? [],
    total: data?.total ?? 0,
    page,
    size,
    loading,
    error,
    refetch,
    pager: { page, size, total: data?.total ?? 0, onPage: (p) => setPk({ key, page: p }) },
  }
}
