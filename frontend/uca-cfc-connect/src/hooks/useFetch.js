import { useCallback, useEffect, useState } from 'react'
import { getErrorMessage } from '../utils/errorHandler'

// `fn` debe ser estable (función de módulo o useCallback). Devuelve { data, loading, error, refetch }.
export function useFetch(fn) {
  const [state, setState] = useState({ loading: true, data: null, error: '' })
  const [nonce, setNonce] = useState(0)

  useEffect(() => {
    let active = true
    fn()
      .then((data) => active && setState({ loading: false, data, error: '' }))
      .catch((err) => active && setState({ loading: false, data: null, error: getErrorMessage(err) }))
    return () => {
      active = false
    }
  }, [fn, nonce])

  const refetch = useCallback(() => {
    setState((s) => ({ ...s, loading: true, error: '' }))
    setNonce((n) => n + 1)
  }, [])

  return { ...state, refetch }
}
