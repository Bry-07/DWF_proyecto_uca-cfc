import { clean } from '../api/adapters'
import { alertError, alertSuccess, promptSelect, promptText } from './alerts'
import { getErrorMessage } from './errorHandler'
import { humanize } from './labels'

// Flujo común "cambiar estado" (inscripciones, alquileres, catering): elige estado,
// pide motivo si se cancela y llama a PUT /{id}/estado.
export async function changeEstado({ api, id, estados, current, onDone }) {
  const estado = await promptSelect({
    title: 'Cambiar estado',
    options: Object.fromEntries(estados.filter((e) => e !== current).map((e) => [e, humanize(e)])),
  })
  if (!estado) return
  let motivo
  if (estado === 'CANCELADA') {
    motivo = await promptText({ title: 'Motivo de la cancelación', label: 'Opcional' })
    if (motivo === null) return
  }
  try {
    await api.setEstado(id, clean({ estado, motivo }))
    alertSuccess('Estado actualizado')
    onDone?.()
  } catch (err) {
    alertError(getErrorMessage(err))
  }
}
