// Campo de formulario para pantallas públicas. options -> <select>.
export default function AuthField({ id, label, error, hint, options, ...props }) {
  return (
    <div className={`field${error ? ' has-error' : ''}`}>
      <label htmlFor={id}>{label}</label>
      {options ? (
        <select id={id} {...props}>
          {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      ) : (
        <input id={id} {...props} />
      )}
      {error ? <div className="error-text">{error}</div> : hint && <div className="hint">{hint}</div>}
    </div>
  )
}
