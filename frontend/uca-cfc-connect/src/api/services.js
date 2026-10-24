import api from './axiosConfig'
import { clean } from './adapters'
import { resource } from './resource'

// Un servicio por recurso del backend (rutas bajo /api/v1, ver OpenAPI).
export const cursosApi = resource('/cursos')
export const diplomadosApi = resource('/diplomados')
export const categoriasApi = resource('/categorias')
export const docentesApi = resource('/docentes')
export const clientesApi = { ...resource('/clientes'), historial: (id) => api.get(`/clientes/${id}/historial`).then((r) => r.data) }
export const inscripcionesApi = resource('/inscripciones')
export const espaciosApi = {
  ...resource('/espacios'),
  disponibilidad: (id, params) => api.get(`/espacios/${id}/disponibilidad`, { params }).then((r) => r.data),
}
export const alquileresApi = resource('/alquileres')
export const serviciosCateringApi = resource('/servicios-catering')
export const solicitudesCateringApi = resource('/solicitudes-catering')
export const usuariosApi = resource('/usuarios')
export const agendaApi = {
  ...resource('/agenda'),
  conflictos: (params) => api.get('/agenda/conflictos', { params: clean(params) }).then((r) => r.data),
}
export const cotizacionesApi = {
  ...resource('/cotizaciones'),
  aprobar: (id, comentario) => api.put(`/cotizaciones/${id}/aprobar`, clean({ comentario })).then((r) => r.data),
  rechazar: (id, comentario) => api.put(`/cotizaciones/${id}/rechazar`, clean({ comentario })).then((r) => r.data),
}
export const pagosApi = {
  ...resource('/pagos'),
  abono: (id, body) => api.put(`/pagos/${id}/abonos`, clean(body)).then((r) => r.data),
  validar: (id, responsable) => api.put(`/pagos/${id}/validar`, { responsable }).then((r) => r.data),
  confirmar: (id, responsable) => api.put(`/pagos/${id}/confirmar`, { responsable }).then((r) => r.data),
  reporte: (params) => api.get('/pagos/reportes/estados-financieros', { params: clean(params) }).then((r) => r.data),
}

// Estos dos devuelven lista directa (sin paginar).
// Estos dos devuelven lista directa (sin paginar). `listPage` adapta esa lista al formato de las tablas.
const directList = (path) => ({
  list: () => api.get(path).then((r) => r.data),
  listPage: () => api.get(path).then((r) => ({ items: r.data, total: r.data.length })),
  create: (body) => api.post(path, clean(body)).then((r) => r.data),
  update: (id, body) => api.put(`${path}/${id}`, clean(body)).then((r) => r.data),
  remove: (id) => api.delete(`${path}/${id}`).then((r) => r.data),
})
export const modalidadesApi = directList('/modalidades')
export const rolesApi = directList('/roles')
