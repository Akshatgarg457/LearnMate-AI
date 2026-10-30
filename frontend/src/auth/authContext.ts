import { createContext, useContext } from 'react'
import type { User } from '../lib/types'

export type Session = { user: User; token: string | null }

/** Thrown by loginAsAdmin when the credentials are valid but belong to a student. */
export class AdminAccessError extends Error {
  constructor() {
    super('This account does not have administrator access.')
    this.name = 'AdminAccessError'
  }
}

export type AuthState = {
  session: Session | null
  login: (email: string, password: string) => Promise<void>
  /** Same backend login, but only creates a session when the account is an administrator. */
  loginAsAdmin: (email: string, password: string) => Promise<void>
  register: (displayName: string, email: string, password: string) => Promise<void>
  logout: () => void
  /** Keeps the signed-in name in sync after the profile is edited. */
  updateUser: (patch: Partial<Pick<User, 'displayName'>>) => void
}

export const AuthContext = createContext<AuthState | null>(null)

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
