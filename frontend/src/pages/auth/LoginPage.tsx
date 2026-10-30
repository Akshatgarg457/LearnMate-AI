import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { CircleAlert } from 'lucide-react'
import { useAuth } from '../../auth/authContext'
import { homeFor } from '../../auth/roles'
import { EXPIRED_KEY } from '../../auth/AuthProvider'
import { Btn, Field } from '../../components/ui'
import { describeError } from '../../services/api'
import AuthShell from './AuthShell'
import PasswordField from './PasswordField'

const emailOk = (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)

export default function LoginPage() {
  const { session, login } = useAuth()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [expired] = useState(() => {
    try {
      const flag = sessionStorage.getItem(EXPIRED_KEY) === '1'
      sessionStorage.removeItem(EXPIRED_KEY)
      return flag
    } catch {
      return false
    }
  })

  // Signed in: go where the requested page or the role's home says.
  if (session) return <Navigate to={from ?? homeFor(session.user.role)} replace />

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (busy) return
    const next = {
      email: emailOk(email) ? undefined : 'Enter a valid email address.',
      password: password ? undefined : 'Enter your password.',
    }
    setErrors(next)
    setFormError(null)
    if (next.email || next.password) return
    setBusy(true)
    try {
      await login(email.trim(), password) // the redirect above runs once the session exists
    } catch (err) {
      const p = describeError(err)
      setFormError(p.kind === 'unauthorized' || p.kind === 'validation' ? 'Incorrect email or password.' : p.message)
      setBusy(false)
    }
  }

  return (
    <AuthShell
      title="Welcome back"
      subtitle="Sign in to continue learning."
      footer={
        <div className="auth-foot-links">
          <span>
            New to LearnMate? <Link to="/register">Create an account</Link>
          </span>
          <Link to="/admin/login" className="auth-admin-link">
            Administrator sign-in
          </Link>
        </div>
      }
    >
      {expired && (
        <p className="form-alert form-alert--info" role="status">
          Your session expired. Please sign in again.
        </p>
      )}
      <form onSubmit={submit} noValidate>
        <Field label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} />
        <PasswordField label="Password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password} />
        {formError && (
          <p className="form-alert" role="alert">
            <CircleAlert size={15} aria-hidden="true" /> {formError}
          </p>
        )}
        <Btn type="submit" loading={busy} className="block">
          {busy ? 'Signing in…' : 'Sign in'}
        </Btn>
      </form>
    </AuthShell>
  )
}
