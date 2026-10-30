import { Navigate, Outlet, useLocation } from 'react-router-dom'
import type { Role } from '../lib/types'
import { useAuth } from './authContext'

/**
 * Client-side route guard for usability only.
 * The backend must enforce real authorization on every request.
 */
export default function ProtectedRoute({ role, loginPath = '/login' }: { role?: Role; loginPath?: string }) {
  const { session } = useAuth()
  const location = useLocation()
  if (!session) return <Navigate to={loginPath} replace state={{ from: location.pathname }} />
  if (role && session.user.role !== role) return <Navigate to="/forbidden" replace />
  return <Outlet />
}
