import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { CircleAlert, LogOut, ShieldAlert, ShieldCheck } from 'lucide-react'
import { AdminAccessError, useAuth } from '../../auth/authContext'
import { EXPIRED_KEY } from '../../auth/AuthProvider'
import { Btn, Field } from '../../components/ui'
import Button from '../../components/Button'
import { describeError } from '../../services/api'
import AuthShell from './AuthShell'
import PasswordField from './PasswordField'

const emailOk = (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)

const studentFooter = (
  <span>
    Not an administrator? <Link to="/login">Use the student sign-in</Link>
  </span>
)

export default function AdminLoginPage() {
  const { session, loginAsAdmin, logout } = useAuth()
  const location = useLocation()
  const requested = (location.state as { from?: string } | null)?.from
  const from = requested && requested.startsWith('/admin') && requested !== '/admin/login' ? requested : '/admin'
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [accessDenied, setAccessDenied] = useState(false)
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

  if (session?.user.role === 'ADMIN') return <Navigate to={from} replace />

  if (session) {
    return (
      <AuthShell
        className="auth-card--admin"
        title="Admin Portal"
        subtitle="Sign in to manage LearnMate AI."
        footer={studentFooter}
      >
        <p className="admin-chip">
          <ShieldCheck size={14} aria-hidden="true" /> Restricted area
        </p>
        <p className="form-alert" role="alert">
          <ShieldAlert size={15} aria-hidden="true" /> You are signed in as a student. Administrator access requires an administrator account.
        </p>
        <div className="admin-actions">
          <Btn variant="secondary" icon={LogOut} onClick={logout}>
            Sign out
          </Btn>
          <Button to="/dashboard" variant="secondary">
            Student dashboard
          </Button>
        </div>
      </AuthShell>
    )
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (busy) return
    const next = {
      email: emailOk(email) ? undefined : 'Enter a valid email address.',
      password: password ? undefined : 'Enter your password.',
    }
    setErrors(next)
    setFormError(null)
    setAccessDenied(false)
    if (next.email || next.password) return
    setBusy(true)
    try {
      await loginAsAdmin(email.trim(), password) // the redirect above runs once an ADMIN session exists
    } catch (err) {
      if (err instanceof AdminAccessError) {
        setAccessDenied(true)
      } else {
        const p = describeError(err)
        setFormError(p.kind === 'unauthorized' || p.kind === 'validation' ? 'Incorrect email or password.' : p.message)
      }
      setBusy(false)
    }
  }

  return (
    <AuthShell
      className="auth-card--admin"
      title="Admin Portal"
      subtitle="Sign in to manage LearnMate AI."
      footer={studentFooter}
    >
      <p className="admin-chip">
        <ShieldCheck size={14} aria-hidden="true" /> Restricted area
      </p>
      {expired && (
        <p className="form-alert form-alert--info" role="status">
          Your session expired. Please sign in again.
        </p>
      )}
      <form onSubmit={submit} noValidate>
        <Field
          label="Email"
          type="email"
          autoComplete="username"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={errors.email}
        />
        <PasswordField
          label="Password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={errors.password}
        />
        {accessDenied && (
          <p className="form-alert" role="alert">
            <ShieldAlert size={15} aria-hidden="true" /> This account does not have administrator access.
          </p>
        )}
        {formError && (
          <p className="form-alert" role="alert">
            <CircleAlert size={15} aria-hidden="true" /> {formError}
          </p>
        )}
        <Btn type="submit" loading={busy} className="block">
          {busy ? 'Verifying access…' : 'Sign in as Admin'}
        </Btn>
      </form>
      <p className="admin-note">Only administrator accounts can access the management console.</p>
    </AuthShell>
  )
}
