export default function Loading({ text = 'Cargando…', fullscreen = false }) {
  return (
    <div className="state-box" role="status" style={fullscreen ? { minHeight: '100vh' } : undefined}>
      <div className="spinner" />
      <span>{text}</span>
    </div>
  )
}
