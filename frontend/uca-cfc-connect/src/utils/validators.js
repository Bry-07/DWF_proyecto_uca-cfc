// Mismas reglas que el OpenAPI (RegistroClienteRequest / RestablecerPasswordRequest).
export const RE = {
  EMAIL: /^\S+@\S+\.\S+$/,
  DUI: /^[0-9]{8}-[0-9]$/,
  NIT: /^[0-9]{4}-[0-9]{6}-[0-9]{3}-[0-9]$/,
  TEL: /^[0-9]{4}-[0-9]{4}$/,
  PASS: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/,
}

export const PASS_HINT = 'Entre 10 y 72 caracteres, con mayúscula, minúscula y número.'

export function passwordError(p) {
  if (!p) return 'Ingresa una contraseña.'
  if (p.length < 10 || p.length > 72 || !RE.PASS.test(p)) return PASS_HINT
  return undefined
}
