import { useState } from 'react'
import { categoriasApi, docentesApi, modalidadesApi, rolesApi } from '../../api/services'
import { enumOptions } from '../../utils/labels'
import Crud from './Crud'

const TEL = /^[0-9]{4}-[0-9]{4}$/

const TABS = {
  categorias: {
    label: 'Categorías',
    props: {
      api: categoriasApi, noun: 'Categoría', nounPlural: 'categorías',
      columns: [
        { header: 'Nombre', className: 'client-cell', render: (r) => r.nombre },
        { header: 'Descripción', className: 'concept', render: (r) => r.descripcion ?? '—' },
      ],
      fields: [
        { name: 'nombre', label: 'Nombre', required: true, maxLength: 100, full: true },
        { name: 'descripcion', label: 'Descripción', type: 'textarea', maxLength: 255 },
      ],
    },
  },
  docentes: {
    label: 'Docentes',
    props: {
      api: docentesApi, noun: 'Docente', nounPlural: 'docentes', rowLabel: (r) => r.nombreCompleto,
      columns: [
        { header: 'Nombre', className: 'client-cell', render: (r) => r.nombreCompleto ?? `${r.nombres} ${r.apellidos}` },
        { header: 'Especialidad', render: (r) => r.especialidad },
        { header: 'Correo', render: (r) => r.email },
        { header: 'Teléfono', className: 'code', render: (r) => r.telefono ?? '—' },
      ],
      fields: [
        { name: 'nombres', label: 'Nombres', required: true, maxLength: 100 },
        { name: 'apellidos', label: 'Apellidos', required: true, maxLength: 100 },
        { name: 'email', label: 'Correo', type: 'email', required: true, maxLength: 150 },
        { name: 'telefono', label: 'Teléfono', placeholder: '0000-0000', pattern: TEL, patternMessage: 'Formato 0000-0000.' },
        { name: 'especialidad', label: 'Especialidad', required: true, maxLength: 120, full: true },
      ],
    },
  },
  modalidades: {
    label: 'Modalidades',
    props: {
      api: modalidadesApi, noun: 'Modalidad', nounPlural: 'modalidades',
      columns: [
        { header: 'Nombre', className: 'client-cell', render: (r) => r.nombre },
        { header: 'Tipo', render: (r) => <span className="badge proceso">{r.tipo}</span> },
        { header: 'Descripción', className: 'concept', render: (r) => r.descripcion ?? '—' },
      ],
      fields: [
        { name: 'nombre', label: 'Nombre', required: true, maxLength: 50 },
        { name: 'tipo', label: 'Tipo', type: 'select', required: true, options: enumOptions(['PRESENCIAL', 'VIRTUAL', 'HIBRIDA']) },
        { name: 'descripcion', label: 'Descripción', type: 'textarea', maxLength: 255 },
      ],
    },
  },
  roles: {
    label: 'Roles',
    props: {
      api: rolesApi, noun: 'Rol', nounPlural: 'roles', locked: (r) => r.rolDelSistema,
      columns: [
        { header: 'Rol', className: 'client-cell', render: (r) => <>{r.nombre}{r.rolDelSistema && <span className="sub-text">Rol del sistema (no editable)</span>}</> },
        { header: 'Descripción', className: 'concept', render: (r) => r.descripcion ?? '—' },
      ],
      fields: [
        { name: 'nombre', label: 'Nombre (MAYÚSCULAS_Y_GUION_BAJO)', required: true, maxLength: 50, pattern: /^[A-Z][A-Z_]*$/, patternMessage: 'Solo mayúsculas y guion bajo, empezando con una letra.', full: true },
        { name: 'descripcion', label: 'Descripción', type: 'textarea', maxLength: 255 },
      ],
    },
  },
}

export default function Administracion() {
  const [tab, setTab] = useState('categorias')
  const current = TABS[tab]

  return (
    <div className="content">
      <div className="page-head">
        <div>
          <h1>Catálogos base</h1>
          <p>Categorías, docentes, modalidades y roles que alimentan el resto del sistema</p>
        </div>
      </div>

      <div className="tabs">
        {Object.entries(TABS).map(([k, t]) => (
          <button key={k} type="button" className={`chip${tab === k ? ' on' : ''}`} onClick={() => setTab(k)}>{t.label}</button>
        ))}
      </div>

      <Crud key={tab} {...current.props} />
    </div>
  )
}
