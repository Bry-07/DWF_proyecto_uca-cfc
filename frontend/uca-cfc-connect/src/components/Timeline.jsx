export default function Timeline({ estado }) {
  const rejected = estado === 'RECHAZADA'
  const idx = estado === 'PENDIENTE' ? 0 : estado === 'EN_PROCESO' ? 1 : 2
  const steps = ['Pendiente', 'En proceso', rejected ? 'Rechazada' : 'Aprobada']
  return (
    <div className="timeline">
      {steps.map((label, i) => {
        const dot = i < idx ? 'done' : i === idx ? (idx === 2 ? (rejected ? 'bad' : 'done') : 'current') : ''
        return (
          <div key={label} style={{ display: 'contents' }}>
            {i > 0 && <div className={`tl-bar${i <= idx ? ' done' : ''}`} />}
            <div className="tl-step"><div className={`tl-dot ${dot}`} /><span className={`tl-label${i <= idx ? ' active' : ''}`}>{label}</span></div>
          </div>
        )
      })}
    </div>
  )
}
