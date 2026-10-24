// Contrato real (OpenAPI): PageResponse = { contenido, pagina, tamanio, totalElementos, totalPaginas, ultima }.
export const unwrapPage = (data) => ({
  items: data?.contenido ?? [],
  page: data?.pagina ?? 0,
  size: data?.tamanio ?? 0,
  total: data?.totalElementos ?? 0,
  pages: data?.totalPaginas ?? 1,
})

// Quita undefined / null / '' (recursivo) antes de enviar parámetros o cuerpos.
export function clean(v) {
  if (Array.isArray(v)) return v.map(clean)
  if (v && typeof v === 'object') {
    return Object.fromEntries(
      Object.entries(v)
        .filter(([, x]) => x !== undefined && x !== null && x !== '')
        .map(([k, x]) => [k, clean(x)]),
    )
  }
  return v
}

export const idOf = (r) => (r?.id !== undefined && r?.id !== null ? String(r.id) : '')
