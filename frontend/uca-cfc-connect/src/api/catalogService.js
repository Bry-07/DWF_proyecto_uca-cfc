import api from './axiosConfig'
import { label, pick, unwrapList } from './adapters'

// El catálogo es pequeño (~40 registros): se trae en una sola página grande y
// se filtra/pagina en el cliente (evita adivinar nombres de filtros del backend).
const BIG = { params: { page: 0, size: 200, sort: 'nombre,asc' } }

function toItem(raw, tipo) {
  const prefix = tipo === 'DIPLOMADO' ? 'DIP' : 'CUR'
  const modalidad = String(label(pick(raw, ['modalidad', 'modalidadNombre'])) ?? '')
  const max = pick(raw, ['cupoMaximo', 'cupo', 'cupos', 'capacidad', 'cupoTotal'])
  const free = pick(raw, ['cuposDisponibles', 'cupoDisponible', 'disponibles'])
  let used = pick(raw, ['inscritos', 'cuposOcupados', 'cupoOcupado', 'totalInscritos', 'cupoActual'])
  if (used === null && max !== null && free !== null) used = max - free

  const inicio = pick(raw, ['horaInicio', 'horaInicial'])
  const fin = pick(raw, ['horaFin', 'horaFinal'])
  const horario = pick(raw, ['horario', 'dias', 'diasSemana']) ?? (inicio && fin ? `${inicio} – ${fin}` : null)

  return {
    id: raw.id,
    tipo,
    codigo: pick(raw, ['codigo', 'code']) ?? `${prefix}-${String(raw.id).padStart(3, '0')}`,
    nombre: pick(raw, ['nombre', 'titulo', 'name']) ?? 'Sin nombre',
    descripcion: pick(raw, ['descripcion', 'detalle']),
    modalidad,
    esVirtual: modalidad.toLowerCase().includes('virtual'),
    categoria: label(pick(raw, ['categoria', 'categoriaNombre'])),
    docente: label(pick(raw, ['docente', 'docenteNombre'])),
    horario: typeof horario === 'object' ? JSON.stringify(horario) : horario,
    lugar: label(pick(raw, ['aula', 'espacio', 'lugar', 'plataforma', 'ubicacion'])),
    cupoMax: max,
    cupoUsado: used,
    precio: pick(raw, ['precio', 'costo', 'precioParticipante', 'precioBase']),
    activo: pick(raw, ['activo', 'estado']),
  }
}

export const catalogService = {
  async listAll() {
    const [cursos, diplomados] = await Promise.all([
      api.get('/cursos', BIG).then((r) => unwrapList(r.data).map((c) => toItem(c, 'CURSO'))),
      api.get('/diplomados', BIG).then((r) => unwrapList(r.data).map((d) => toItem(d, 'DIPLOMADO'))),
    ])
    return [...cursos, ...diplomados]
  },
}
