import { useState, type FormEvent } from 'react'
import { Check, Flag, MessageSquarePlus, MessagesSquare, Trash2 } from 'lucide-react'
import { useAuth } from '../../auth/authContext'
import Pager from '../../components/Pager'
import ResourceView from '../../components/ResourceView'
import { Badge, Btn, Field, Modal, PageHeader, SelectField, TextAreaField } from '../../components/ui'
import { useDebounced } from '../../hooks/useDebounced'
import { useLive } from '../../hooks/useLive'
import { useResource } from '../../hooks/useResource'
import type { CommunityAnswer, CommunityQuestion, Page } from '../../lib/types'
import api, { describeError } from '../../services/api'
import { useToast } from '../../toast/toastContext'

const reasons = ['Spam', 'Inappropriate or abusive', 'Incorrect or misleading', 'Off topic', 'Other']
type ReportTarget = { kind: 'questions' | 'answers'; id: number }

function QuestionDetail({ id, onBack }: { id: number; onBack: () => void }) {
  const toast = useToast()
  const { session } = useAuth()
  const me = session?.user.id
  const question = useResource<CommunityQuestion>(`/community/questions/${id}`)
  const answers = useResource<Page<CommunityAnswer>>(`/community/questions/${id}/answers`, { size: '50' })
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [report, setReport] = useState<ReportTarget | null>(null)
  const [reason, setReason] = useState(reasons[0])
  const [details, setDetails] = useState('')
  const [confirmDelete, setConfirmDelete] = useState<{ kind: 'questions' | 'answers'; id: number } | null>(null)

  const act = async (fn: () => Promise<unknown>, ok: string, after?: () => void) => {
    try {
      await fn()
      toast(ok, 'success')
      after?.()
    } catch (err) {
      toast(describeError(err).message, 'error')
    }
  }

  const post = async (e: FormEvent) => {
    e.preventDefault()
    if (busy || !text.trim()) return
    setBusy(true)
    await act(() => api.post(`/community/questions/${id}/answers`, { body: text.trim() }), 'Answer posted.', () => { setText(''); answers.reload(); question.reload() })
    setBusy(false)
  }

  return (
    <>
      <PageHeader eyebrow="Community" title="Question" actions={<Btn variant="secondary" onClick={onBack}>Back to feed</Btn>} />
      <ResourceView resource={question} endpoint={`GET /community/questions/${id}`} emptyIcon={MessagesSquare} emptyTitle="Question not found">
        {(q) => {
          const mine = String(q.author.id) === me
          return (
            <>
              <article className="card pad">
                <div className="row">{q.subject && <Badge tone="blue">{q.subject}</Badge>}{q.topic && <Badge tone="violet">{q.topic}</Badge>}{q.status !== 'OPEN' && <Badge tone="amber">{q.status.toLowerCase()}</Badge>}</div>
                <h2>{q.title}</h2>
                <p className="muted">by {q.author.displayName} &middot; {new Date(q.createdAt).toLocaleString()} &middot; {q.viewCount} views</p>
                <p style={{ whiteSpace: 'pre-wrap' }}>{q.body}</p>
                <div className="row">
                  {!mine && <Btn variant="ghost" icon={Flag} onClick={() => setReport({ kind: 'questions', id: q.id })}>Report</Btn>}
                  {mine && <Btn variant="ghost" icon={Trash2} onClick={() => setConfirmDelete({ kind: 'questions', id: q.id })}>Delete question</Btn>}
                </div>
              </article>
              <ResourceView resource={answers} endpoint={`GET /community/questions/${id}/answers`} isEmpty={(d) => d.content.length === 0} emptyIcon={MessagesSquare} emptyTitle="No answers yet" emptyText="Be the first to help.">
                {(a) => (
                  <>
                    <h2 className="h3 section">{a.totalElements} {a.totalElements === 1 ? 'answer' : 'answers'}</h2>
                    <ul className="stack">
                      {a.content.map((r) => (
                        <li className="card pad" key={r.id}>
                          {r.accepted && <Badge tone="green">Accepted answer</Badge>}
                          <p style={{ whiteSpace: 'pre-wrap' }}>{r.body}</p>
                          <p className="muted">{r.author.displayName} &middot; {new Date(r.createdAt).toLocaleString()}</p>
                          <div className="row">
                            {mine && !r.accepted && <Btn variant="secondary" icon={Check} onClick={() => void act(() => api.post(`/community/questions/${id}/answers/${r.id}/accept`), 'Answer accepted.', answers.reload)}>Accept answer</Btn>}
                            {String(r.author.id) === me ? <Btn variant="ghost" icon={Trash2} onClick={() => setConfirmDelete({ kind: 'answers', id: r.id })}>Delete</Btn> : <Btn variant="ghost" icon={Flag} onClick={() => setReport({ kind: 'answers', id: r.id })}>Report</Btn>}
                          </div>
                        </li>
                      ))}
                    </ul>
                  </>
                )}
              </ResourceView>
              <form className="card pad form-stack section" onSubmit={post}>
                <TextAreaField label="Your answer" rows={4} maxLength={10000} value={text} onChange={(e) => setText(e.target.value)} />
                <Btn type="submit" loading={busy} disabled={!text.trim()}>Post answer</Btn>
              </form>
            </>
          )
        }}
      </ResourceView>

      <Modal open={!!report} onClose={() => setReport(null)} title="Report this content" footer={<><Btn variant="secondary" onClick={() => setReport(null)}>Cancel</Btn><Btn onClick={() => report && void act(() => api.post(`/community/${report.kind}/${report.id}/report`, { reason, details: details.trim() || undefined }), 'Thanks. A moderator will review this.', () => { setReport(null); setDetails('') })}>Send report</Btn></>}>
        <SelectField label="Reason" value={reason} onChange={(e) => setReason(e.target.value)}>{reasons.map((r) => (<option key={r}>{r}</option>))}</SelectField>
        <TextAreaField label="Details (optional)" rows={3} maxLength={1000} value={details} onChange={(e) => setDetails(e.target.value)} />
      </Modal>
      <Modal open={!!confirmDelete} onClose={() => setConfirmDelete(null)} title="Delete this?" footer={<><Btn variant="secondary" onClick={() => setConfirmDelete(null)}>Cancel</Btn><Btn variant="danger" onClick={() => confirmDelete && void act(() => api.delete(`/community/${confirmDelete.kind}/${confirmDelete.id}`), 'Deleted.', () => { const wasQuestion = confirmDelete.kind === 'questions'; setConfirmDelete(null); if (wasQuestion) onBack(); else { answers.reload(); question.reload() } })}>Delete</Btn></>}>
        <p>This cannot be undone.</p>
      </Modal>
    </>
  )
}

export default function CommunityPage() {
  const live = useLive()
  const toast = useToast()
  const [search, setSearch] = useState('')
  const [subject, setSubject] = useState('')
  const [page, setPage] = useState(0)
  const [openId, setOpenId] = useState<number | null>(null)
  const q = useDebounced(search)
  const feed = useResource<Page<CommunityQuestion>>(live ? '/community/questions' : null, { q, subject, page: String(page), size: '15' })
  const [asking, setAsking] = useState(false)
  const [form, setForm] = useState({ title: '', body: '', subject: '', topic: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)

  if (openId !== null) return <QuestionDetail id={openId} onBack={() => { setOpenId(null); feed.reload() }} />

  const ask = async (e: FormEvent) => {
    e.preventDefault()
    if (busy) return
    const next: Record<string, string> = {
      title: form.title.trim().length >= 10 && form.title.length <= 400 ? '' : 'Write a clear title (10 to 400 characters).',
      body: form.body.trim().length >= 20 ? '' : 'Add some detail (20+ characters).',
    }
    setErrors(next)
    if (Object.values(next).some(Boolean)) return
    setBusy(true)
    try {
      await api.post('/community/questions', { title: form.title.trim(), body: form.body.trim(), subject: form.subject.trim() || undefined, topic: form.topic.trim() || undefined })
      toast('Question posted.', 'success')
      setAsking(false)
      setForm({ title: '', body: '', subject: '', topic: '' })
      feed.reload()
    } catch (err) {
      toast(describeError(err).message, 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <PageHeader eyebrow="Community" title="Ask the community" actions={<Btn icon={MessageSquarePlus} onClick={() => setAsking(true)}>Ask a question</Btn>} />
      <div className="filters">
        <Field label="Search questions" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0) }} placeholder="Keyword" />
        <Field label="Subject" value={subject} onChange={(e) => { setSubject(e.target.value); setPage(0) }} placeholder="e.g. Java" />
      </div>
      <ResourceView resource={feed} endpoint="GET /community/questions" isEmpty={(d) => d.content.length === 0} emptyIcon={MessagesSquare} emptyTitle="No questions yet" emptyText="Ask the first one.">
        {(d) => (
          <>
            <ul className="stack">
              {d.content.map((qn) => (
                <li key={qn.id}>
                  <button type="button" className="card pad problem-row" onClick={() => setOpenId(qn.id)}>
                    <span><strong>{qn.title}</strong><span className="muted block">by {qn.author.displayName} &middot; {qn.answerCount} {qn.answerCount === 1 ? 'answer' : 'answers'} &middot; {qn.viewCount} views</span></span>
                    <span className="row">{qn.subject && <Badge tone="blue">{qn.subject}</Badge>}{qn.topic && <Badge tone="violet">{qn.topic}</Badge>}</span>
                  </button>
                </li>
              ))}
            </ul>
            <Pager page={d.number} totalPages={d.totalPages} onChange={setPage} />
          </>
        )}
      </ResourceView>
      <Modal open={asking} onClose={() => setAsking(false)} title="Ask a question" footer={<><Btn variant="secondary" onClick={() => setAsking(false)}>Cancel</Btn><Btn type="submit" form="ask-form" loading={busy}>Post question</Btn></>}>
        <form id="ask-form" className="form-stack" onSubmit={ask} noValidate>
          <Field label="Title" maxLength={400} value={form.title} error={errors.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
          <TextAreaField label="Details" rows={5} maxLength={10000} value={form.body} error={errors.body} onChange={(e) => setForm({ ...form, body: e.target.value })} />
          <Field label="Subject (optional)" value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })} />
          <Field label="Topic (optional)" value={form.topic} onChange={(e) => setForm({ ...form, topic: e.target.value })} />
        </form>
      </Modal>
    </>
  )
}
