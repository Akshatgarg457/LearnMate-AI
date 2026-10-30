import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from './authContext'
import { homeFor } from './roles'

/**
 * The student dashboard is for students. Administrators are sent to their own console.
 * This is navigation behaviour only: the backend still decides what each role may do.
 */
export default function AdminToHome({ children }: { children: ReactNode }) {
  const { session } = useAuth()
  if (session?.user.role === 'ADMIN') return <Navigate to={homeFor('ADMIN')} replace />
  return <>{children}</>
}
