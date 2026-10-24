export const humanize = (v) => {
  if (!v) return '—'
  const s = String(v).toLowerCase().replace(/_/g, ' ')
  return s.charAt(0).toUpperCase() + s.slice(1)
}

export const enumOptions = (values) => values.map((v) => ({ value: v, label: humanize(v) }))

// Enums del backend (OpenAPI).
export const ENUMS = {
  TIPO_CLIENTE: ['PERSONA_PARTICULAR', 'EMPRESA'],
  ESTADO_RESERVA: ['PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'FINALIZADA'],
  ESTADO_COTIZACION: ['PENDIENTE', 'EN_PROCESO', 'APROBADA', 'RECHAZADA'],
  ESTADO_PAGO: ['PENDIENTE', 'PARCIAL', 'PAGADO'],
  METODO_PAGO: ['EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'DEPOSITO'],
  CONCEPTO_PAGO: ['INSCRIPCION', 'COTIZACION', 'ALQUILER', 'CATERING'],
  TIPO_ESPACIO: ['AUDITORIO', 'SALA', 'LABORATORIO', 'AULA', 'SALA_MULTIMEDIA'],
  ESTADO_ESPACIO: ['DISPONIBLE', 'MANTENIMIENTO', 'INACTIVO'],
  TIPO_CATERING: ['COFFEE_BREAK', 'DESAYUNO', 'ALMUERZO', 'CENA', 'REFRIGERIO'],
}

// Estado -> clase .badge de App.css
export const ESTADO_CLASS = {
  PENDIENTE: 'pendiente', EN_PROCESO: 'proceso', APROBADA: 'aprobada', RECHAZADA: 'rechazada',
  CONFIRMADA: 'confirmada', CANCELADA: 'cancelada', FINALIZADA: 'finalizada',
  PARCIAL: 'parcial', PAGADO: 'pagado', ACTIVO: 'activo', INACTIVO: 'inactivo',
  DISPONIBLE: 'activo', MANTENIMIENTO: 'pendiente',
  PRESENCIAL: 'presencial', VIRTUAL: 'virtual', HIBRIDA: 'parcial',
}
