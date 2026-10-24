import Icon from './Icon'

export default function ErrorMessage({ message, onRetry }) {
  if (!message) return null
  return (
    <div className="alert-bar" role="alert">
      <Icon name="alert" />
      <span style={{ flex: 1 }}>{message}</span>
      {onRetry && (
        <button type="button" className="link-action" onClick={onRetry}>
          Reintentar
        </button>
      )}
    </div>
  )
}
