import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { useAuth } from '../../auth/authContext'
import ResourceView from '../../components/ResourceView'
import { Btn, Field, PageHeader, SelectField, TextAreaField } from '../../components/ui'
import PasswordField from '../auth/PasswordField'
import { useLive } from '../../hooks/useLive'
import { useResource } from '../../hooks/useResource'
import type { UserProfile } from '../../lib/types'
import api, { describeError } from '../../services/api'
import { useToast } from '../../toast/toastContext'

const zones: string[] = typeof Intl.supportedValuesOf === 'function' ? Intl.supportedValuesOf('timeZone') : ['UTC', 'Asia/Kolkata']

function ProfileForm({ initial }: { initial: { displayName: string; bio: string; timezone: string; email: string } }) {
  const { updateUser } = useAuth()
  const toast = useToast()
  const [form, setForm] = useState(initial)
  const [error, setError] = useState<string | undefined>()
  const [busy, setBusy] = useState(false)

  const save = async (e: FormEvent) => {
    e.preventDefault()
    if (busy) return
    if (form.displayName.trim().length < 2) return setError('Enter your name (2 to 100 characters).')
    setError(undefined)
    setBusy(true)
    try {
      const { data } = await api.patch<UserProfile>('/users/me', { displayName: form.displayName.trim(), bio: form.bio, timezone: form.timezone })
      updateUser({ displayName: data.displayName })
      toast('Settings saved.', 'success')
    } catch (err) {
      toast(describeError(err).message, 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card pad form-stack" onSubmit={save} noValidate>
      <Field label="Name" value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} error={error} />
      <Field label="Email" value={form.email} disabled readOnly hint="Email changes are not supported yet." />
      <TextAreaField label="Bio" rows={3} maxLength={512} value={form.bio} onChange={(e) => setForm({ ...form, bio: e.target.value })} hint="Up to 512 characters." />
      <SelectField label="Timezone" value={form.timezone} onChange={(e) => setForm({ ...form, timezone: e.target.value })}>
        {(zones.includes(form.timezone) ? zones : [form.timezone, ...zones]).map((z) => (<option key={z}>{z}</option>))}
      </SelectField>
      <p className="muted fine">Your timezone decides which calendar day your streak activity counts toward.</p>
      <Btn type="submit" loading={busy}>Save changes</Btn>
    </form>
  )
}

function PasswordForm() {
  const toast = useToast()
  const [form, setForm] = useState({ current: '', next: '', confirm: '' })
  const [errors, setErrors] = useState<{ current?: string; next?: string; confirm?: string }>({})
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (busy) return
    const next = {
      current: form.current ? undefined : 'Enter your current password.',
      next: form.next.length >= 8 && form.next.length <= 128 ? undefined : 'Use 8 to 128 characters.',
      confirm: form.confirm === form.next ? undefined : 'Passwords do not match.',
    }
    setErrors(next)
    if (next.current || next.next || next.confirm) return
    setBusy(true)
    try {
      await api.post('/users/me/password', { currentPassword: form.current, newPassword: form.next })
      toast('Password changed.', 'success')
      setForm({ current: '', next: '', confirm: '' })
    } catch (err) {
      toast(describeError(err).message, 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card pad form-stack" onSubmit={submit} noValidate>
      <h2 className="h3">Change password</h2>
      <PasswordField label="Current password" autoComplete="current-password" value={form.current} onChange={(e) => setForm({ ...form, current: e.target.value })} error={errors.current} />
      <PasswordField label="New password" autoComplete="new-password" value={form.next} onChange={(e) => setForm({ ...form, next: e.target.value })} error={errors.next} />
      <PasswordField label="Confirm new password" autoComplete="new-password" value={form.confirm} onChange={(e) => setForm({ ...form, confirm: e.target.value })} error={errors.confirm} />
      <Btn type="submit" variant="secondary" loading={busy}>Change password</Btn>
    </form>
  )
}

export default function SettingsPage() {
  const { session, logout } = useAuth()
  const live = useLive()
  const navigate = useNavigate()
  const me = useResource<UserProfile>(live ? '/users/me' : null)
  const fromSession = { displayName: session?.user.displayName ?? '', bio: '', timezone: Intl.DateTimeFormat().resolvedOptions().timeZone, email: session?.user.email ?? '' }

  return (
    <>
      <PageHeader eyebrow="Profile & Settings" title="Your profile" description="Update how you appear to other students and which timezone your streaks use." />
      <div className="two-col">
        <div className="stack">
          <ResourceView resource={me} endpoint="GET /users/me" emptyIcon={LogOut} emptyTitle="Profile unavailable">
            {(p) => <ProfileForm key={p.id} initial={{ displayName: p.displayName, bio: p.bio ?? '', timezone: p.timezone ?? fromSession.timezone, email: p.email }} />}
          </ResourceView>
          <PasswordForm />
        </div>
        <aside className="card pad">
          <h2 className="h3">Account</h2>
          <p className="muted">Notification and privacy preferences are not available yet because the backend has no endpoints for them.</p>
          <Btn variant="secondary" icon={LogOut} onClick={() => { logout(); navigate('/') }}>Log out</Btn>
        </aside>
      </div>
    </>
  )
}
