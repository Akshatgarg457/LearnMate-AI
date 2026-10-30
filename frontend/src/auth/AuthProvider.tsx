import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import api, { configureApi } from '../services/api'
import type { AuthResponse } from '../lib/types'
import { AuthContext, AdminAccessError, type AuthState, type Session } from './authContext'

const STORAGE_KEY = 'lm.session'
export const EXPIRED_KEY = 'lm.expired'

function readSession(): Session | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as Session) : null
  } catch {
    return null
  }
}

function writeSession(session: Session | null) {
  try {
    if (session) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    else sessionStorage.removeItem(STORAGE_KEY)
  } catch {
    /* storage can be unavailable (private mode); the session just won't survive a refresh */
  }
}

function toSession(r: AuthResponse): Session {
  return { user: { id: String(r.userId), email: r.email, displayName: r.displayName, role: r.role }, token: r.accessToken }
}

// The token lives in sessionStorage (cleared when the tab closes) and is never
// put in a VITE_* variable. A production setup should prefer an httpOnly cookie.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(readSession)
  const sessionRef = useRef(session)

  const update = useCallback((next: Session | null) => {
    sessionRef.current = next
    writeSession(next)
    setSession(next)
  }, [])

  useEffect(() => {
    configureApi({
      getToken: () => sessionRef.current?.token ?? null,
      onUnauthorized: () => {
        try {
          sessionStorage.setItem(EXPIRED_KEY, '1')
        } catch {
          /* ignore */
        }
        update(null)
      },
    })
  }, [update])

  const authenticate = useCallback(async (email: string, password: string) => {
    const { data } = await api.post<AuthResponse>('/auth/login', { email: email.trim().toLowerCase(), password })
    return data
  }, [])

  const login = useCallback<AuthState['login']>(
    async (email, password) => {
      update(toSession(await authenticate(email, password)))
    },
    [authenticate, update],
  )

  const loginAsAdmin = useCallback<AuthState['loginAsAdmin']>(
    async (email, password) => {
      const data = await authenticate(email, password)
      if (data.role !== 'ADMIN') throw new AdminAccessError()
      update(toSession(data))
    },
    [authenticate, update],
  )

  const register = useCallback<AuthState['register']>(
    async (displayName, email, password) => {
      const { data } = await api.post<AuthResponse>('/auth/register', { displayName, email: email.trim().toLowerCase(), password })
      update(toSession(data))
    },
    [update],
  )

  const logout = useCallback(() => update(null), [update])

  const updateUser = useCallback<AuthState['updateUser']>(
    (patch) => {
      const cur = sessionRef.current
      if (cur) update({ ...cur, user: { ...cur.user, ...patch } })
    },
    [update],
  )

  const value = useMemo<AuthState>(
    () => ({ session, login, loginAsAdmin, register, logout, updateUser }),
    [session, login, loginAsAdmin, register, logout, updateUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
