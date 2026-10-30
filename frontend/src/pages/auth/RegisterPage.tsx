import { useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { CircleAlert } from 'lucide-react'
import { useAuth } from '../../auth/authContext'
import { homeFor } from '../../auth/roles'
import { Btn, Field } from '../../components/ui'
import { describeError } from '../../services/api'
import AuthShell from './AuthShell'
import PasswordField from './PasswordField'

type Errors = Partial<Record<'displayName' | 'email' | 'password' | 'confirm', string>>

export default function RegisterPage() {
  const { session, register } = useAuth()
  const [form, setForm] = useState({ displayName: '', email: '', password: '', confirm: '' })
  const [errors, setErrors] = useState<Errors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (session) return <Navigate to={homeFor(session.user.role)} replace />

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm((f) => ({ ...f, [key]: e.target.value }))

  // These rules are for usability only; the backend is the authority on validity.
  const validate = (): Errors => ({
    displayName: form.displayName.trim().length >= 2 ? undefined : 'Enter your name (at least 2 characters).',
    email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email) ? undefined : 'Enter a valid email address.',
    password: form.password.length >= 8 && /[A-Za-z]/.test(form.password) && /\d/.test(form.password) ? undefined : 'Use at least 8 characters with a letter and a number.',
    confirm: form.confirm === form.password ? undefined : 'Passwords do not match.',
  })

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (busy) return
    const next = validate()
    setErrors(next)
    setFormError(null)
    if (Object.values(next).some(Boolean)) return
    setBusy(true)
    try {
      await register(form.displayName.trim(), form.email.trim(), form.password) // redirect above handles navigation
    } catch (err) {
      const p = describeError(err)
      setFormError(p.kind === 'conflict' ? 'An account with this email already exists.' : p.message)
      setBusy(false)
    }
  }

  return (
    <AuthShell
      title="Create your account"
      subtitle="Start learning with AI and your peers."
      footer={
        <>
          Already registered? <Link to="/login">Sign in</Link>
        </>
      }
    >
      <form onSubmit={submit} noValidate>
        <Field label="Name" autoComplete="name" value={form.displayName} onChange={set('displayName')} error={errors.displayName} />
        <Field label="Email" type="email" autoComplete="email" value={form.email} onChange={set('email')} error={errors.email} />
        <PasswordField label="Password" autoComplete="new-password" value={form.password} onChange={set('password')} error={errors.password} hint="At least 8 characters, with a letter and a number." />
        <PasswordField label="Confirm password" autoComplete="new-password" value={form.confirm} onChange={set('confirm')} error={errors.confirm} />
        {formError && (
          <p className="form-alert" role="alert">
            <CircleAlert size={15} aria-hidden="true" /> {formError}
          </p>
        )}
        <Btn type="submit" loading={busy} className="block">
          {busy ? 'Creating account…' : 'Create account'}
        </Btn>
      </form>
    </AuthShell>
  )
}
