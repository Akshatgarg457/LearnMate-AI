import axios, { AxiosError } from 'axios'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

// The auth layer plugs in here so this file has no dependency on React.
let getToken: () => string | null = () => null
let handleUnauthorized: () => void = () => {}

export function configureApi(options: { getToken: () => string | null; onUnauthorized: () => void }) {
  getToken = options.getToken
  handleUnauthorized = options.onUnauthorized
}

/**
 * Difference between the server clock and this device's clock, learned from the `timestamp`
 * the backend puts on every response. Countdowns use it so a wrong device clock cannot
 * make an exam look longer or shorter than the server's deadline.
 */
let serverSkewMs = 0
export const serverNow = () => Date.now() + serverSkewMs

type Envelope = { success: boolean; data?: unknown; error?: string; timestamp?: string }
const isEnvelope = (v: unknown): v is Envelope => typeof v === 'object' && v !== null && 'success' in v

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.set('Authorization', `Bearer ${token}`)
  return config
})

api.interceptors.response.use(
  (response) => {
    // Every JSON response is { success, data, timestamp }; callers only ever see `data`.
    if (response.config.responseType !== 'blob' && isEnvelope(response.data)) {
      if (response.data.timestamp) {
        const t = Date.parse(response.data.timestamp)
        if (!Number.isNaN(t)) serverSkewMs = t - Date.now()
      }
      response.data = response.data.data
    }
    return response
  },
  (error: unknown) => {
    // 401 with a token means it expired or the account was disabled: sign the user out.
    if (axios.isAxiosError(error) && error.response?.status === 401 && getToken()) handleUnauthorized()
    return Promise.reject(error)
  },
)

export type ProblemKind = 'network' | 'not-found' | 'unauthorized' | 'forbidden' | 'validation' | 'conflict' | 'rate-limit' | 'unavailable' | 'server' | 'cancelled' | 'unknown'

export type ApiProblem = { kind: ProblemKind; message: string; status?: number }

/** Turns any thrown value into a friendly, typed problem. The backend sends { success:false, error:"..." }. */
export function describeError(error: unknown): ApiProblem {
  if (axios.isCancel(error)) return { kind: 'cancelled', message: 'Request cancelled.' }
  if (!axios.isAxiosError(error)) return { kind: 'unknown', message: 'Something unexpected went wrong.' }
  const err = error as AxiosError<{ error?: string; message?: string }>
  if (!err.response) {
    return { kind: 'network', message: `Cannot reach the LearnMate server at ${API_BASE_URL}. Make sure the backend is running and CORS allows this site.` }
  }
  const status = err.response.status
  const body = err.response.data
  const serverMessage = typeof body === 'object' && body ? (body.error ?? body.message) : undefined
  const kinds: Record<number, ProblemKind> = { 400: 'validation', 401: 'unauthorized', 403: 'forbidden', 404: 'not-found', 409: 'conflict', 422: 'validation', 429: 'rate-limit', 503: 'unavailable' }
  const fallback: Record<number, string> = {
    401: 'Your session is not valid. Please sign in again.',
    403: 'You do not have permission to do that.',
    404: 'That item was not found.',
    429: 'Too many requests. Please wait a moment and try again.',
    503: 'This service is not available right now.',
  }
  return {
    kind: kinds[status] ?? (status >= 500 ? 'server' : 'unknown'),
    message: serverMessage ?? fallback[status] ?? 'The server could not complete the request.',
    status,
  }
}

export default api
