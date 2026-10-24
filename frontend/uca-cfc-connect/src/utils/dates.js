const pad = (n) => String(n).padStart(2, '0')

export const toISODate = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
export const toISODateTime = (d) => `${toISODate(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`
export const addDays = (d, n) => {
  const x = new Date(d)
  x.setDate(x.getDate() + n)
  return x
}
export function mondayOf(d) {
  const x = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  x.setDate(x.getDate() - ((x.getDay() + 6) % 7))
  return x
}
// 'YYYY-MM-DDTHH:mm[:ss]' -> Date local (sin desfases de zona horaria)
export function parseLocal(iso) {
  const m = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2}))?/.exec(iso || '')
  return m ? new Date(+m[1], +m[2] - 1, +m[3], +(m[4] || 0), +(m[5] || 0)) : null
}
export const inputDT = (iso) => (iso ? iso.slice(0, 16) : '')
export const timeLabel = (d) => `${d.getHours() % 12 || 12}:${pad(d.getMinutes())}${d.getHours() >= 12 ? 'pm' : 'am'}`
export const dateTimeLabel = (iso) => {
  const d = parseLocal(iso)
  return d ? `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${timeLabel(d)}` : '—'
}
export const monthRange = (ym) => {
  if (!ym) return {}
  const [y, m] = ym.split('-').map(Number)
  return { desde: `${ym}-01`, hasta: `${ym}-${pad(new Date(y, m, 0).getDate())}` }
}
