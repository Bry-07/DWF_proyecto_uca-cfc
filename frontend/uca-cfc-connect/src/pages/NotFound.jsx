import { Link } from 'react-router-dom'
import Icon from '../components/Icon'

export default function NotFound() {
  return (
    <div className="content">
      <div className="panel">
        <div className="state-box">
          <Icon name="alert" size={28} />
          <h3>Página no encontrada</h3>
          <span>La dirección no existe o fue movida.</span>
          <Link to="/" className="btn-primary">Volver al inicio</Link>
        </div>
      </div>
    </div>
  )
}
