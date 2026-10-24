// Iconos copiados de los mockups (viewBox 24, trazo heredado de index.css).
const PATHS = {
  agenda: <><rect x="3" y="4" width="18" height="17" rx="2" /><path d="M3 9h18M8 2v4M16 2v4" /></>,
  cursos: <><path d="M4 4h11a3 3 0 013 3v13H7a3 3 0 01-3-3V4z" /><path d="M4 4v13a3 3 0 003 3h14" /></>,
  clientes: <><circle cx="9" cy="8" r="3.2" /><path d="M2.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6" /><circle cx="18" cy="8.5" r="2.4" /><path d="M15.5 14.1c2.7.2 4.9 2.4 4.9 5.9" /></>,
  inscripciones: <><path d="M5 3h11l3 3v15H5z" /><path d="M9 12l2 2 4-4" /></>,
  cotizaciones: <><path d="M6 3h9l4 4v14H6z" /><path d="M9 11h6M9 15h6M9 7h3" /></>,
  espacios: <><path d="M4 21V9l8-6 8 6v12" /><path d="M9 21v-7h6v7" /></>,
  catering: <><path d="M5 10h14l-1.5 9h-11z" /><path d="M8 10a4 4 0 018 0" /></>,
  pagos: <><rect x="2.5" y="5.5" width="19" height="13" rx="2" /><path d="M2.5 10h19" /></>,
  usuarios: <path d="M12 3l7 3v6c0 4.6-3 7.6-7 9-4-1.4-7-4.4-7-9V6z" />,
  inicio: <><path d="M3 11l9-8 9 8" /><path d="M5 10v10h14V10" /></>,
  bell: <><path d="M6 8a6 6 0 1112 0c0 5 2 6 2 6H4s2-1 2-6" /><path d="M9.5 21a2.5 2.5 0 005 0" /></>,
  search: <><circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3" /></>,
  logout: <><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" /><path d="M16 17l5-5-5-5M21 12H9" /></>,
  alert: <><path d="M12 9v4M12 17h.01" /><path d="M10.3 3.9L2.7 18a1.8 1.8 0 001.6 2.7h15.4a1.8 1.8 0 001.6-2.7L13.7 3.9a1.8 1.8 0 00-3.4 0z" /></>,
  lock: <><rect x="4" y="11" width="16" height="10" rx="2" /><path d="M8 11V7a4 4 0 018 0v4" /></>,
  tools: <path d="M14.7 6.3a4 4 0 00-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 005.4-5.4l-2.5 2.5-2.4-.6-.6-2.4z" />,
}

export default function Icon({ name, size = 16, ...rest }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true" {...rest}>
      {PATHS[name] ?? null}
    </svg>
  )
}
