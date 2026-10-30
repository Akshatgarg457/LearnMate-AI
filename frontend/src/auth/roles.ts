import type { Role } from '../lib/types'

/** Where each role lands after signing in. */
export function homeFor(role: Role): string {
  return role === 'ADMIN' ? '/admin' : '/dashboard'
}
