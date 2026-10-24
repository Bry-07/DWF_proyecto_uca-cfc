import Icon from '../components/Icon'

// Marcador de posición para módulos de fases siguientes.
export default function ComingSoon({ title, phase }) {
  return (
    <div className="content">
      <div className="page-head">
        <div>
          <h1>{title}</h1>
          <p>Módulo en construcción{phase ? ` — Fase ${phase}` : ''}</p>
        </div>
      </div>
      <div className="panel">
        <div className="state-box">
          <Icon name="tools" size={28} />
          <h3>Próximamente</h3>
          <span>Esta pantalla se implementa en una fase posterior del frontend.</span>
        </div>
      </div>
    </div>
  )
}
