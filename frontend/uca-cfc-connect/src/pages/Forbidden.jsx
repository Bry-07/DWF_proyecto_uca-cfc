import { Link } from 'react-router-dom'
import Icon from '../components/Icon'

export default function Forbidden() {
  return (
    <div className="content">
      <div className="panel">
        <div className="state-box">
          <Icon name="lock" size={28} />
          <h3>No tienes permisos para ver esta sección</h3>
          <span>Tu rol no incluye acceso a esta pantalla. Si crees que es un error, contacta a Administración.</span>
          <Link to="/" className="btn-primary">Volver al inicio</Link>
        </div>
      </div>
    </div>
  )
}
