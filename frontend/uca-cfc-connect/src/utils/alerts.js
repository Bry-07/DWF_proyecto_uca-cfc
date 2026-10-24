import Swal from 'sweetalert2'

// SweetAlert2 con los colores de los mockups.
const base = Swal.mixin({
  confirmButtonColor: '#14213b', // --ink
  cancelButtonColor: '#68728c', // --muted
  customClass: { popup: 'swal-cfc' },
})

const toast = Swal.mixin({
  toast: true,
  position: 'top-end',
  showConfirmButton: false,
  timer: 2600,
  timerProgressBar: true,
})

export const alertSuccess = (message) => toast.fire({ icon: 'success', title: message })
export const alertError = (message, title = 'Ocurrió un problema') =>
  base.fire({ icon: 'error', title, text: message })
export const alertInfo = (message, title = 'Información') =>
  base.fire({ icon: 'info', title, text: message })

// Devuelve true si el usuario confirma.
export async function confirmAction({
  title = '¿Continuar?',
  text = '',
  confirmText = 'Sí, continuar',
  danger = false,
} = {}) {
  const result = await base.fire({
    icon: 'question',
    title,
    text,
    showCancelButton: true,
    confirmButtonText: confirmText,
    cancelButtonText: 'Cancelar',
    confirmButtonColor: danger ? '#b23a3a' : '#14213b', // --red / --ink
    reverseButtons: true,
  })
  return result.isConfirmed
}

export const confirmDelete = (what = 'este registro') =>
  confirmAction({
    title: `¿Eliminar ${what}?`,
    text: 'Esta acción no se puede deshacer.',
    confirmText: 'Sí, eliminar',
    danger: true,
  })

// Lista desplegable. Devuelve el valor elegido o null si se cancela.
export async function promptSelect({ title, options, placeholder = 'Selecciona…', confirmText = 'Aplicar' }) {
  const r = await base.fire({
    title,
    input: 'select',
    inputOptions: options,
    inputPlaceholder: placeholder,
    showCancelButton: true,
    confirmButtonText: confirmText,
    cancelButtonText: 'Cancelar',
    reverseButtons: true,
    inputValidator: (v) => (v ? undefined : 'Selecciona una opción'),
  })
  return r.isConfirmed ? r.value : null
}

// Texto libre. Devuelve el texto (puede ser '') o null si se cancela.
export async function promptText({ title, label = '', confirmText = 'Continuar' }) {
  const r = await base.fire({
    title,
    input: 'textarea',
    inputLabel: label,
    showCancelButton: true,
    confirmButtonText: confirmText,
    cancelButtonText: 'Cancelar',
    reverseButtons: true,
  })
  return r.isConfirmed ? (r.value ?? '') : null
}
