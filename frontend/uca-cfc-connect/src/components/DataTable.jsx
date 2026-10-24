import EmptyState from './EmptyState'
import ErrorMessage from './ErrorMessage'
import Loading from './Loading'
import Pagination from './Pagination'

// columns: [{ header, render(row), className, headerClassName }]
export default function DataTable({ columns, rows, loading, error, onRetry, pager, empty = 'No hay registros para mostrar.', rowKey = (r) => r.id }) {
  const hasRows = rows?.length > 0
  return (
    <>
      <ErrorMessage message={error} onRetry={onRetry} />
      <div className="table-wrap">
        {loading && !hasRows ? (
          <Loading />
        ) : hasRows ? (
          <table>
            <thead>
              <tr>{columns.map((c, i) => <th key={i} className={c.headerClassName ?? c.className}>{c.header}</th>)}</tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={rowKey(r)}>{columns.map((c, i) => <td key={i} className={c.className}>{c.render(r)}</td>)}</tr>
              ))}
            </tbody>
          </table>
        ) : (
          !error && <EmptyState text={empty} />
        )}
        {pager && hasRows && <Pagination {...pager} />}
      </div>
    </>
  )
}
