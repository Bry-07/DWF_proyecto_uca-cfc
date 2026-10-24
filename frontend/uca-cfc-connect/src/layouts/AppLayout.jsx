import { useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import Sidebar from '../components/Sidebar'
import Topbar from '../components/Topbar'
import { findItem } from '../utils/navigation'

const TITLES = { '/': 'Inicio', '/403': 'Acceso denegado', '/perfil': 'Mi perfil' }

export default function AppLayout() {
  const { pathname } = useLocation()
  const [searchState, setSearchState] = useState({ path: pathname, value: '' })
  const [placeholder, setSearchPlaceholder] = useState('')

  // La búsqueda se reinicia al cambiar de pantalla (derivado del path, sin efectos).
  const search = searchState.path === pathname ? searchState.value : ''
  const setSearch = (value) => setSearchState({ path: pathname, value })

  const crumb = TITLES[pathname] ?? findItem(pathname)?.label ?? 'Página'
  const ctx = { search, setSearch, setSearchPlaceholder }

  return (
    <div className="app">
      <Sidebar />
      <div className="main">
        <Topbar
          crumb={crumb}
          search={placeholder ? { value: search, onChange: setSearch, placeholder } : null}
        />
        <Outlet context={ctx} />
      </div>
    </div>
  )
}
