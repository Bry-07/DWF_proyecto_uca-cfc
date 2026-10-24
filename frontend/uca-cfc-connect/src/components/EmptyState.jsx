export default function EmptyState({ title = 'Sin resultados', text = 'No hay registros para mostrar.', action }) {
  return (
    <div className="state-box">
      <h3>{title}</h3>
      <span>{text}</span>
      {action}
    </div>
  )
}
