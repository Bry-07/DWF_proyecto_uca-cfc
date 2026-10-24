import Icon from './Icon'

// Tarjeta de indicador (clase .sum-card de los mockups).
export default function StatCard({ label, value, sub, icon, dark = false, loading = false }) {
  return (
    <div className={`sum-card${dark ? ' dark' : ''}`}>
      <div className="lbl">{icon && <Icon name={icon} size={14} />}{label}</div>
      <div className="val">{loading ? '…' : value}</div>
      {sub && <div className="sub">{sub}</div>}
    </div>
  )
}
