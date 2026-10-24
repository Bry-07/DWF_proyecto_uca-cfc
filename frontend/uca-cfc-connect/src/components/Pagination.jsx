// Paginación del lado del cliente (page desde 0, como el backend).
export default function Pagination({ page, size, total, onPage }) {
  const pages = Math.max(1, Math.ceil(total / size))
  const from = total === 0 ? 0 : page * size + 1
  const to = Math.min(total, (page + 1) * size)
  return (
    <div className="pagination">
      <span>{from}–{to} de {total}</span>
      <div className="pages">
        <button type="button" className="btn-outline" disabled={page <= 0} onClick={() => onPage(page - 1)}>Anterior</button>
        <span style={{ alignSelf: 'center' }}>{page + 1} / {pages}</span>
        <button type="button" className="btn-outline" disabled={page >= pages - 1} onClick={() => onPage(page + 1)}>Siguiente</button>
      </div>
    </div>
  )
}
