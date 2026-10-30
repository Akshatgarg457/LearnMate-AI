import { useState, type FormEvent } from 'react'
import { useAuth } from '../../../auth/authContext'
import { Badge, Btn, Field, Modal, PageHeader, Tabs } from '../../../components/ui'
import { prettyEnum } from '../../../lib/dates'
import type { ContentReport, Course, MockExam, PeerSession, Quiz, UserProfile } from '../../../lib/types'
import api, { describeError } from '../../../services/api'
import { useToast } from '../../../toast/toastContext'
import AdminTable, { StatusBadge, type Column, type RowAction } from './AdminTable'
import { adminSections, type AdminSectionKey } from './config'

const when = (iso?: string | null) => (iso ? new Date(iso).toLocaleString() : '\u2014')

function UsersSection() {
  const { session } = useAuth()
  const me = session?.user.id
  const cfg = adminSections.users
  const columns: Column<UserProfile>[] = [
    { key: 'name', label: 'Name', render: (u) => u.displayName, text: (u) => u.displayName },
    { key: 'email', label: 'Email', render: (u) => u.email, text: (u) => u.email },
    { key: 'role', label: 'Role', render: (u) => <Badge tone={u.role === 'ADMIN' ? 'violet' : 'neutral'}>{u.role.toLowerCase()}</Badge>, text: (u) => u.role },
    { key: 'status', label: 'Status', render: (u) => <StatusBadge value={u.status} />, text: (u) => u.status },
    { key: 'joined', label: 'Joined', render: (u) => when(u.createdAt) },
  ]
  const notSelf = (u: UserProfile) => String(u.id) !== me // never offer to lock yourself out
  const actions: RowAction<UserProfile>[] = [
    { label: 'Suspend', danger: true, show: (u) => u.status === 'ACTIVE' && notSelf(u), confirm: (u) => `${u.displayName} will lose access immediately.`, run: (u) => api.patch(`/admin/users/${u.id}/status`, { status: 'SUSPENDED' }) },
    { label: 'Reactivate', show: (u) => u.status === 'SUSPENDED', confirm: (u) => `${u.displayName} will be able to sign in again.`, run: (u) => api.patch(`/admin/users/${u.id}/status`, { status: 'ACTIVE' }) },
    { label: 'Make admin', show: (u) => u.role === 'STUDENT' && u.status === 'ACTIVE', confirm: (u) => `${u.displayName} will get full administrator access.`, run: (u) => api.patch(`/admin/users/${u.id}/role`, { role: 'ADMIN' }) },
    { label: 'Make student', show: (u) => u.role === 'ADMIN' && notSelf(u), confirm: (u) => `${u.displayName} will lose administrator access.`, run: (u) => api.patch(`/admin/users/${u.id}/role`, { role: 'STUDENT' }) },
  ]
  return <AdminTable<UserProfile> endpoint="/admin/users" columns={columns} actions={actions} searchLabel="Search users" emptyTitle="No users to show" icon={cfg.icon} statusOf={(u) => u.status} />
}

function CoursesSection() {
  const cfg = adminSections.courses
  const columns: Column<Course>[] = [{ key: 'title', label: 'Title', render: (c) => c.title, text: (c) => `${c.title} ${c.providerName ?? ''} ${c.category ?? ''}` }]
  const actions: RowAction<Course>[] = [
    { label: 'Publish', show: (c) => !c.active, confirm: (c) => `"${c.title}" will appear in the Rewards Store and can be redeemed with points.`, run: (c) => api.patch(`/admin/courses/${c.id}/active`, { active: true }) },
    { label: 'Unpublish', danger: true, show: (c) => c.active, confirm: (c) => `"${c.title}" will be hidden from the Rewards Store. Students who already redeemed it keep access.`, run: (c) => api.patch(`/admin/courses/${c.id}/active`, { active: false }) },
  ]
  return (
    <AdminTable<Course>
      endpoint="/admin/courses"
      columns={columns}
      actions={actions}
      searchLabel="Search courses"
      emptyTitle="No courses yet"
      icon={cfg.icon}
      statusOf={(c) => (c.active ? 'PUBLISHED' : 'DRAFT')}
      note="Creating courses is done through the backend API (POST /admin/courses); there is no create form in this console yet."
      cards={(c, buttons) => (
        <div className="card feature-card card--course">
          <div className="feature-card-top"><StatusBadge value={c.active ? 'PUBLISHED' : 'DRAFT'} /><Badge tone={c.courseType === 'PARTNER' ? 'blue' : 'violet'}>{c.courseType.toLowerCase()}</Badge></div>
          <h3>{c.title}</h3>
          <p className="muted">{[c.providerName, c.category].filter(Boolean).join(' \u00b7 ')}</p>
          <p><strong>{c.pointsPrice.toLocaleString()} points</strong></p>
          {buttons}
        </div>
      )}
    />
  )
}

function ExamsSection() {
  const [tab, setTab] = useState<'quizzes' | 'mock'>('quizzes')
  const cfg = adminSections.exams
  const quizCols: Column<Quiz>[] = [
    { key: 'title', label: 'Title', render: (q) => q.title, text: (q) => `${q.title} ${q.subject ?? ''}` },
    { key: 'subject', label: 'Subject', render: (q) => q.subject ?? '\u2014' },
    { key: 'q', label: 'Questions', render: (q) => q.questions.length },
    { key: 'min', label: 'Minutes', render: (q) => q.durationMinutes },
    { key: 'status', label: 'Status', render: (q) => <StatusBadge value={q.active ? 'ACTIVE' : 'DRAFT'} /> },
  ]
  const mockCols: Column<MockExam>[] = [
    { key: 'title', label: 'Title', render: (q) => q.title, text: (q) => `${q.title} ${q.subject ?? ''}` },
    { key: 'subject', label: 'Subject', render: (q) => q.subject ?? '\u2014' },
    { key: 'q', label: 'Questions', render: (q) => q.questions.length },
    { key: 'min', label: 'Minutes', render: (q) => q.durationMinutes },
    { key: 'pass', label: 'Pass mark', render: (q) => `${q.passPercentage}%` },
    { key: 'status', label: 'Status', render: (q) => <StatusBadge value={q.active ? 'ACTIVE' : 'DRAFT'} /> },
  ]
  const toggle = <T extends { id: number; title: string; active?: boolean }>(base: string): RowAction<T>[] => [
    { label: 'Deactivate', danger: true, show: (r) => !!r.active, confirm: (r) => `"${r.title}" will no longer be offered to students.`, run: (r) => api.patch(`${base}/${r.id}/active`, { active: false }) },
    { label: 'Activate', show: (r) => !r.active, confirm: (r) => `"${r.title}" will be offered to students.`, run: (r) => api.patch(`${base}/${r.id}/active`, { active: true }) },
  ]
  return (
    <>
      <Tabs label="Quizzes and exams" value={tab} onChange={setTab} tabs={[{ id: 'quizzes', label: 'Quizzes' }, { id: 'mock', label: 'Mock exams' }]} />
      {tab === 'quizzes' ? (
        <AdminTable<Quiz> key="quizzes" endpoint="/admin/quizzes" columns={quizCols} actions={toggle<Quiz>('/admin/quizzes')} searchLabel="Search quizzes" emptyTitle="No quizzes yet" icon={cfg.icon} statusOf={(q) => (q.active ? 'ACTIVE' : 'DRAFT')} note="Question content is created through the backend API; this console only switches content on or off." />
      ) : (
        <AdminTable<MockExam> key="mock" endpoint="/admin/mock-exams" columns={mockCols} actions={toggle<MockExam>('/admin/mock-exams')} searchLabel="Search mock exams" emptyTitle="No mock exams yet" icon={cfg.icon} statusOf={(q) => (q.active ? 'ACTIVE' : 'DRAFT')} />
      )}
    </>
  )
}

function ModerationSection() {
  const [status, setStatus] = useState('PENDING')
  const cfg = adminSections.moderation
  const columns: Column<ContentReport>[] = [
    { key: 'what', label: 'Content', render: (r) => `${prettyEnum(r.contentType)} #${r.contentId}`, text: (r) => `${r.contentType} ${r.contentId}` },
    { key: 'reason', label: 'Reason', render: (r) => r.reason, text: (r) => `${r.reason} ${r.details ?? ''}` },
    { key: 'details', label: 'Details', render: (r) => r.details ?? '\u2014' },
    { key: 'by', label: 'Reported by', render: (r) => r.reporter.displayName, text: (r) => r.reporter.displayName },
    { key: 'status', label: 'Status', render: (r) => <StatusBadge value={r.status} /> },
    { key: 'at', label: 'Reported', render: (r) => when(r.createdAt) },
  ]
  return (
    <>
      <Tabs label="Report status" value={status} onChange={setStatus} tabs={[{ id: 'PENDING', label: 'Pending' }, { id: 'REVIEWED', label: 'Reviewed' }, { id: 'ACTIONED', label: 'Actioned' }, { id: 'DISMISSED', label: 'Dismissed' }]} />
      <AdminTable<ContentReport> key={status} endpoint="/admin/reports" params={{ status }} columns={columns} searchLabel="Search reports" emptyTitle="No reports here" icon={cfg.icon} note="The backend can list reports but has no endpoint to resolve or dismiss them yet, so this view is read-only." />
    </>
  )
}

function DisputesSection() {
  const cfg = adminSections.disputes
  const columns: Column<PeerSession>[] = [
    { key: 'topic', label: 'Topic', render: (s) => s.topic, text: (s) => s.topic },
    { key: 'learner', label: 'Learner', render: (s) => s.learner.displayName, text: (s) => s.learner.displayName },
    { key: 'teacher', label: 'Teacher', render: (s) => s.teacher?.displayName ?? '\u2014', text: (s) => s.teacher?.displayName ?? '' },
    { key: 'min', label: 'Verified min', render: (s) => s.actualMinutes ?? '\u2014' },
    { key: 'status', label: 'Status', render: (s) => <StatusBadge value={s.status} /> },
    { key: 'at', label: 'Updated', render: (s) => when(s.endedAt ?? s.createdAt) },
  ]
  const actions: RowAction<PeerSession>[] = [
    { label: 'Mark completed', confirm: (s) => `Session "${s.topic}" will be marked completed. No reward is issued automatically.`, run: (s) => api.post(`/admin/disputes/${s.id}/resolve`, null, { params: { resolution: 'COMPLETED' } }) },
    { label: 'Cancel session', danger: true, confirm: (s) => `Session "${s.topic}" will be cancelled.`, run: (s) => api.post(`/admin/disputes/${s.id}/resolve`, null, { params: { resolution: 'CANCELLED' } }) },
  ]
  return <AdminTable<PeerSession> endpoint="/admin/disputes" columns={columns} actions={actions} searchLabel="Search disputes" emptyTitle="No disputes waiting" icon={cfg.icon} />
}

function RewardsSection() {
  const toast = useToast()
  const [form, setForm] = useState({ userId: '', amount: '', reason: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)
  const [confirming, setConfirming] = useState(false)

  const validate = (e: FormEvent) => {
    e.preventDefault()
    const amount = Number(form.amount)
    const next: Record<string, string> = {
      userId: /^\d+$/.test(form.userId) ? '' : 'Enter the numeric user ID (see User Management).',
      amount: Number.isInteger(amount) && amount !== 0 ? '' : 'Enter a whole number, positive to add points or negative to remove them.',
      reason: form.reason.trim().length >= 3 ? '' : 'A reason is required and is recorded with the adjustment.',
    }
    setErrors(next)
    if (!Object.values(next).some(Boolean)) setConfirming(true)
  }
  const apply = async () => {
    if (busy) return
    setBusy(true)
    try {
      await api.post('/admin/rewards/adjustments', { amount: Number(form.amount), reason: form.reason.trim() }, { params: { userId: form.userId } })
      toast('Adjustment recorded by the backend.', 'success')
      setForm({ userId: '', amount: '', reason: '' })
      setConfirming(false)
    } catch (err) {
      toast(describeError(err).message, 'error')
    } finally {
      setBusy(false)
    }
  }
  return (
    <>
      <p className="muted fine">The backend has no endpoint that lists all reward transactions, so there is no transaction table here. Students see their own history in the Rewards Store.</p>
      <form className="card pad form-stack" style={{ maxWidth: 520 }} onSubmit={validate} noValidate>
        <h2 className="h3">Adjust a points balance</h2>
        <Field label="User ID" inputMode="numeric" value={form.userId} error={errors.userId} onChange={(e) => setForm({ ...form, userId: e.target.value })} />
        <Field label="Points (positive adds, negative removes)" inputMode="numeric" value={form.amount} error={errors.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
        <Field label="Reason" value={form.reason} error={errors.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} />
        <Btn type="submit">Review adjustment</Btn>
      </form>
      <ConfirmAdjust open={confirming} busy={busy} form={form} onCancel={() => setConfirming(false)} onApply={() => void apply()} />
    </>
  )
}

function ConfirmAdjust({ open, busy, form, onCancel, onApply }: { open: boolean; busy: boolean; form: { userId: string; amount: string; reason: string }; onCancel: () => void; onApply: () => void }) {
  return (
    <Modal open={open} onClose={onCancel} title="Apply this adjustment?" footer={<><Btn variant="secondary" onClick={onCancel}>Cancel</Btn><Btn variant="danger" loading={busy} onClick={onApply}>Apply adjustment</Btn></>}>
      <p>User <strong>#{form.userId}</strong>: <strong>{Number(form.amount) > 0 ? '+' : ''}{form.amount} points</strong></p>
      <p className="muted">Reason: {form.reason}. The backend records this adjustment and rejects it if it would make the balance negative.</p>
    </Modal>
  )
}

export default function AdminSectionPage({ section }: { section: AdminSectionKey }) {
  const cfg = adminSections[section]
  const body = { users: <UsersSection />, courses: <CoursesSection />, exams: <ExamsSection />, moderation: <ModerationSection />, disputes: <DisputesSection />, rewards: <RewardsSection /> }[section]
  return (
    <>
      <PageHeader eyebrow="Admin" title={cfg.title} description={cfg.description} />
      {body}
    </>
  )
}
