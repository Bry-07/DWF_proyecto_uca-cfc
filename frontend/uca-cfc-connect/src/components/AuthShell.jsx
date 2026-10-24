import '../pages/Login.css'

// Marco común de las pantallas públicas (login, registro, recuperar): panel de marca + tarjeta.
export default function AuthShell({ children, wide = false }) {
  return (
    <div className="screen">
      <section className="brand-panel">
        <div className="brand-top">
          <div className="mark">CFC</div>
          <div className="brand-name">
            UCA · CFC Connect<small>Centro de Formación Continua</small>
          </div>
        </div>

        <div className="pitch">
          <div className="eyebrow">Acceso institucional</div>
          <h1>Cursos, espacios y catering — coordinados en un solo horario.</h1>
          <p>Gestiona inscripciones, cotizaciones y pagos con la certeza de que ninguna actividad se cruza en la agenda del centro.</p>
        </div>

        <div className="ruler-wrap" aria-hidden="true">
          <div className="ruler-track">
            <div className="slot busy-teal" style={{ top: '8%' }} />
            <div className="slot busy-amber" style={{ top: '34%' }} />
            <div className="slot conflict" style={{ top: '52%' }} />
            <div className="slot busy-teal" style={{ top: '78%' }} />
          </div>
          <div className="ruler-labels">
            <span>07:00</span><span>10:30</span><span>14:00</span><span>17:30</span><span>20:00</span>
          </div>
          <div className="ruler-caption">Franja horaria del auditorio principal — hoy</div>
        </div>

        <div className="foot-note"><span className="dot" /> Agenda unificada · cursos, espacios y catering</div>
      </section>

      <section className="form-panel">
        <div className={`form-card${wide ? ' wide' : ''}`}>{children}</div>
      </section>
    </div>
  )
}
