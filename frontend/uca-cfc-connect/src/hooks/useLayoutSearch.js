import { useEffect } from 'react'
import { useOutletContext } from 'react-router-dom'

// El buscador vive en la Topbar (como en los mockups) pero lo consumen las páginas.
// Una página que quiera buscador lo activa pasando un placeholder:
//   const { search } = useLayoutSearch('Buscar curso, diplomado o código...')
// Al salir de la página el buscador desaparece solo.
export function useLayoutSearch(placeholder) {
  const ctx = useOutletContext()
  const setPlaceholder = ctx?.setSearchPlaceholder
  useEffect(() => {
    if (!placeholder || !setPlaceholder) return undefined
    setPlaceholder(placeholder)
    return () => setPlaceholder('')
  }, [placeholder, setPlaceholder])
  return { search: ctx?.search ?? '' }
}
