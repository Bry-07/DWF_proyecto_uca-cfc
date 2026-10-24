import { ESTADO_CLASS, humanize } from '../utils/labels'

export default function StatusBadge({ value }) {
  if (!value) return <span className="code">—</span>
  return <span className={`badge ${ESTADO_CLASS[value] ?? ''}`}>{humanize(value)}</span>
}
