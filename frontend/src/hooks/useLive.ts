import { useAuth } from '../auth/authContext'

/** True when a backend session exists. */
export function useLive(): boolean {
  const { session } = useAuth()
  return !!session
}
