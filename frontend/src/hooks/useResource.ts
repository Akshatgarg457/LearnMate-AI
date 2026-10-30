import { useCallback, useEffect, useRef, useState } from 'react'
import api, { describeError, type ApiProblem } from '../services/api'

export type ResourceState<T> = {
  status: 'idle' | 'loading' | 'ready' | 'error'
  data?: T
  problem?: ApiProblem
  reload: () => void
}

/**
 * GET a backend resource. Pass `path = null` to skip the request
 * (used before a request is ready or when a page has nothing to load).
 */
export function useResource<T>(path: string | null, params?: Record<string, string | undefined>): ResourceState<T> {
  const [state, setState] = useState<Omit<ResourceState<T>, 'reload'>>({ status: path ? 'loading' : 'idle' })
  const [tick, setTick] = useState(0)
  const paramKey = JSON.stringify(params ?? {})
  const paramsRef = useRef(params)
  paramsRef.current = params

  useEffect(() => {
    if (!path) {
      setState({ status: 'idle' })
      return
    }
    const controller = new AbortController()
    setState((prev) => ({ status: 'loading', data: prev.data }))
    const clean = Object.fromEntries(Object.entries(paramsRef.current ?? {}).filter(([, v]) => v))
    api
      .get<T>(path, { params: clean, signal: controller.signal })
      .then((res) => setState({ status: 'ready', data: res.data }))
      .catch((err: unknown) => {
        const problem = describeError(err)
        if (problem.kind !== 'cancelled') setState({ status: 'error', problem })
      })
    return () => controller.abort()
  }, [path, paramKey, tick])

  const reload = useCallback(() => setTick((n) => n + 1), [])
  return { ...state, reload }
}
