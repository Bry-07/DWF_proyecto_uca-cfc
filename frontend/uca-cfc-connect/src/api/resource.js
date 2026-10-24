import api from './axiosConfig'
import { clean, unwrapPage } from './adapters'

// CRUD genérico: GET lista paginada, GET/POST/PUT/DELETE y PUT /{id}/estado.
export const resource = (path) => ({
  list: (params) => api.get(path, { params: { sort: 'id,desc', ...clean(params) } }).then((r) => unwrapPage(r.data)),
  get: (id) => api.get(`${path}/${id}`).then((r) => r.data),
  create: (body) => api.post(path, clean(body)).then((r) => r.data),
  update: (id, body) => api.put(`${path}/${id}`, clean(body)).then((r) => r.data),
  remove: (id) => api.delete(`${path}/${id}`).then((r) => r.data),
  setEstado: (id, body) => api.put(`${path}/${id}/estado`, clean(body)).then((r) => r.data),
})
