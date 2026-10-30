import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { authService } from '../api/authService'
import { cotizacionesApi, pagosApi } from '../api/services'
import { navForRole } from '../utils/navigation'
import Pagos from '../pages/pagos/Pagos'
import Catalogo from '../pages/catalogo/Catalogo'
import Recursos from '../pages/recursos/Recursos'
import Catering from '../pages/catering/Catering'
import Cotizaciones from '../pages/cotizaciones/Cotizaciones'
import StaffDashboard from '../pages/dashboard/StaffDashboard'
import Registro from '../pages/auth/Registro'

vi.mock('../hooks/useAuth', () => ({ useAuth: vi.fn() }))
vi.mock('../api/authService', () => ({ authService: { registro: vi.fn() } }))
vi.mock('../api/services', () => {
  const resource = () => ({ list: vi.fn().mockResolvedValue({ items: [], total: 0 }), reporte: vi.fn().mockResolvedValue({}), conflictos: vi.fn().mockResolvedValue([]) })
  return Object.fromEntries(['pagosApi', 'clientesApi', 'cotizacionesApi', 'inscripcionesApi', 'agendaApi', 'espaciosApi', 'serviciosCateringApi', 'solicitudesCateringApi', 'cursosApi', 'diplomadosApi', 'categoriasApi', 'docentesApi', 'modalidadesApi'].map((k) => [k, resource()]))
})

const page = (items) => ({ items, total: items.length })
const show = (component) => render(<MemoryRouter>{component}</MemoryRouter>)
const role = (rol) => useAuth.mockReturnValue({ role: rol, user: { nombre: 'Usuario', clienteId: 1 } })
const pago = { id: 1, codigo: 'PAG-1', cliente: { nombre: 'Cliente' }, metodo: 'EFECTIVO', montoEsperado: 100, montoPagado: 100, saldo: 0, fechaValidacion: '2026-10-30T09:00:00', estado: 'PAGADO' }

beforeEach(() => {
  role('RECEPCIONISTA')
  pagosApi.list.mockResolvedValue(page([pago]))
  cotizacionesApi.list.mockResolvedValue(page([]))
})

describe('Permisos de pagos', () => {
  it('recepción puede entrar, confirmar y no consulta reportes financieros', async () => {
    expect(navForRole('RECEPCIONISTA').flatMap((g) => g.items).some((i) => i.path === '/pagos')).toBe(true)
    show(<Pagos />)
    expect(await screen.findByRole('button', { name: 'Confirmar' })).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Validar' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Registrar pago' })).toBeNull()
    expect(pagosApi.reporte).not.toHaveBeenCalled()
  })
  it('contabilidad valida pero no confirma', async () => {
    role('CONTABILIDAD')
    pagosApi.list.mockResolvedValue(page([{ ...pago, fechaValidacion: null }]))
    show(<Pagos />)
    expect(await screen.findByRole('button', { name: 'Validar' })).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Confirmar' })).toBeNull()
  })
  it('contabilidad tampoco confirma un pago ya validado', async () => {
    role('CONTABILIDAD')
    show(<Pagos />)
    await screen.findByText('PAG-1')
    expect(screen.queryByRole('button', { name: 'Confirmar' })).toBeNull()
  })
  it('el cliente no ve acciones de gestión', async () => {
    role('CLIENTE')
    show(<Pagos />)
    await screen.findByText('PAG-1')
    expect(screen.queryByRole('button', { name: /Confirmar|Validar|Registrar pago/ })).toBeNull()
  })
})

describe('Acciones exclusivas de administración', () => {
  it('recepción no ve crear ni editar cursos', async () => {
    show(<Catalogo />)
    await screen.findByText('Ajusta los filtros o la búsqueda.')
    expect(screen.queryByRole('button', { name: /Nuevo curso|Nuevo diplomado/ })).toBeNull()
  })
  it('recepción no ve crear espacios', async () => {
    show(<Recursos />)
    await screen.findByText('No hay registros para mostrar.')
    expect(screen.queryByRole('button', { name: 'Nuevo espacio' })).toBeNull()
  })
  it('recepción no ve crear servicios de catering', async () => {
    show(<Catering />)
    fireEvent.click(screen.getByRole('button', { name: 'Servicios' }))
    await screen.findByText('No hay registros para mostrar.')
    expect(screen.queryByRole('button', { name: 'Nuevo servicio' })).toBeNull()
  })
  it('recepción no puede aprobar una cotización', async () => {
    cotizacionesApi.list.mockResolvedValue(page([{ id: 1, codigo: 'COT-1', estado: 'EN_PROCESO', items: [] }]))
    show(<Cotizaciones />)
    fireEvent.click(await screen.findByRole('button', { name: 'Ver detalle' }))
    expect(screen.queryByRole('button', { name: 'Aprobar' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Rechazar' })).toBeNull()
  })
  it('ADMIN conserva la creación de cursos', async () => {
    role('ADMIN')
    show(<Catalogo />)
    expect(await screen.findByRole('button', { name: 'Nuevo curso' })).toBeTruthy()
  })
})

it('el tablero incluye cotizaciones recién creadas EN_PROCESO', async () => {
  cotizacionesApi.list.mockImplementation(({ estado }) => Promise.resolve(page(estado === 'EN_PROCESO'
    ? [{ id: 2, codigo: 'COT-NUEVA', asunto: 'Nueva propuesta' }] : [])))
  show(<StaffDashboard />)
  expect(await screen.findByText('COT-NUEVA')).toBeTruthy()
  expect(cotizacionesApi.list).toHaveBeenCalledWith({ estado: 'PENDIENTE', size: 5 })
  expect(cotizacionesApi.list).toHaveBeenCalledWith({ estado: 'EN_PROCESO', size: 5 })
})

describe('Identificación obligatoria en registro', () => {
  it('bloquea una persona sin DUI antes de llamar la API', async () => {
    show(<Registro />)
    fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }))
    expect(await screen.findByText('El DUI es obligatorio para una persona particular.')).toBeTruthy()
    expect(authService.registro).not.toHaveBeenCalled()
  })
  it('bloquea una empresa sin NIT', async () => {
    show(<Registro />)
    fireEvent.change(screen.getByLabelText('Tipo de cliente'), { target: { value: 'EMPRESA' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }))
    expect(await screen.findByText('El NIT es obligatorio para una empresa.')).toBeTruthy()
    expect(screen.queryByLabelText('DUI *')).toBeNull()
    expect(authService.registro).not.toHaveBeenCalled()
  })
  it('envía una persona válida a la API', async () => {
    authService.registro.mockResolvedValue({ id: 1 })
    show(<Registro />)
    for (const [label, value] of [['Nombre completo', 'Ana Pérez'], ['Correo electrónico', 'ana@example.com'], ['Contraseña', 'Password1234'], ['Confirmar contraseña', 'Password1234'], ['DUI *', '12345678-9']]) {
      fireEvent.change(screen.getByLabelText(label), { target: { value } })
    }
    fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }))
    await waitFor(() => expect(authService.registro).toHaveBeenCalledWith(expect.objectContaining({ dui: '12345678-9' })))
  })
  it('al cambiar a empresa ignora el DUI oculto y envía los datos corporativos', async () => {
    authService.registro.mockResolvedValue({ id: 2 })
    show(<Registro />)
    fireEvent.change(screen.getByLabelText('DUI *'), { target: { value: 'incompleto' } })
    fireEvent.change(screen.getByLabelText('Tipo de cliente'), { target: { value: 'EMPRESA' } })
    for (const [label, value] of [['Nombre del representante', 'Ana Pérez'], ['Correo electrónico', 'empresa@example.com'], ['Contraseña', 'Password1234'], ['Confirmar contraseña', 'Password1234'], ['NIT *', '0614-120589-102-3'], ['Razón social', 'Empresa de pruebas']]) {
      fireEvent.change(screen.getByLabelText(label), { target: { value } })
    }
    fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }))
    await waitFor(() => expect(authService.registro).toHaveBeenCalledWith(expect.objectContaining({
      tipo: 'EMPRESA', dui: undefined, nit: '0614-120589-102-3', empresa: { razonSocial: 'Empresa de pruebas' },
    })))
  })
})
