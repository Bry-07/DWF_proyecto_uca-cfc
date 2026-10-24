import { toISODate } from '../../utils/dates'

const pad = (n) => String(n).padStart(2, '0')

// Se llaman desde efectos/callbacks (no durante el render).
export const todayRange = (extraDays = 0) => {
  const d = new Date()
  const end = new Date(d.getFullYear(), d.getMonth(), d.getDate() + extraDays)
  return { desde: `${toISODate(d)}T00:00:00`, hasta: `${toISODate(end)}T23:59:00` }
}

export const monthRangeNow = () => {
  const d = new Date()
  const ym = `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
  return { desde: `${ym}-01`, hasta: `${ym}-${pad(new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate())}` }
}

export const MONTH_NAMES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre']
