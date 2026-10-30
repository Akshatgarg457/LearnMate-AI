import { useEffect, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Plus, RefreshCw, UsersRound, Video, VideoOff } from 'lucide-react'
import { useAuth } from '../../auth/authContext'
import Pager from '../../components/Pager'
import ResourceView from '../../components/ResourceView'
import { Badge, Btn, Field, Modal, PageHeader, SelectField, Tabs, TextAreaField } from '../../components/ui'
import { useLive } from '../../hooks/useLive'
import { useResource } from '../../hooks/useResource'
import type { GoogleStatus, Page, PeerSession, PeerStatus } from '../../lib/types'
import api, { describeError } from '../../services/api'
import { useToast } from '../../toast/toastContext'

type Action = { kind: 'accept' | 'start' | 'complete' | 'dispute'; session: PeerSession }

const statusLabel: Record<PeerStatus, string> = { REQUESTED: 'Open', ACCEPTED: 'Accepted', IN_PROGRESS: 'In progress', COMPLETED: 'Completed', DISPUTED: 'Disputed', CANCELLED: 'Cancelled' }
const statusTone: Record<PeerStatus, 'neutral' | 'violet' | 'blue' | 'green' | 'amber' | 'red'> = { REQUESTED: 'blue', ACCEPTED: 'violet', IN_PROGRESS: 'violet', COMPLETED: 'green', DISPUTED: 'red', CANCELLED: 'neutral' }

export default function PeerLearningPage() {
  const live = useLive()
  const toast = useToast()
  const { session } = useAuth()
  const me = session?.user.id
  const [tab, setTab] = useState<'open' | 'mine' | 'taught'>('open')
  const [filter, setFilter] = useState('')
  const [page, setPage] = useState(0)
  const [params, setParams] = useSearchParams()
  const requests = useResource<Page<PeerSession>>(live && tab === 'open' ? '/peer/requests' : null, { page: String(page), size: '15' })
  const sessions = useResource<Page<PeerSession>>(live && tab !== 'open' ? '/peer/sessions' : null, { size: '50' })
  const google = useResource<GoogleStatus>(live ? '/auth/google/status' : null)
  const reloadGoogle = google.reload
  const googleReady = !google.data?.configured || google.data.connected

  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState({ topic: '', description: '', requestedMinutes: '30' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)
  const [action, setAction] = useState<Action | null>(null)
  const [rating, setRating] = useState('5')
  const [feedback, setFeedback] = useState('')
  const [googleBusy, setGoogleBusy] = useState(false)
  const [meetingBusy, setMeetingBusy] = useState<number | null>(null)

  const reloadAll = () => { requests.reload(); sessions.reload() }

  useEffect(() => {
    const result = params.get('google')
    if (!result) return
    const reason = params.get('reason')
    if (result === 'connected') {
      toast('Google Meet connected. Meeting links are created when a request is accepted.', 'success')
      reloadGoogle()
    } else if (reason === 'access_denied') {
      toast('Google authorization was cancelled.', 'info')
    } else {
      toast('Google could not be connected. Please try again.', 'error')
    }
    const next = new URLSearchParams(params)
    next.delete('google')
    next.delete('reason')
    setParams(next, { replace: true })
  }, [params, setParams, toast, reloadGoogle])

  const connectGoogle = async () => {
    if (googleBusy) return
    setGoogleBusy(true)
    try {
      const { data } = await api.get<{ authorizationUrl: string }>('/auth/google')
      window.location.assign(data.authorizationUrl)
    } catch (err) {
      toast(describeError(err).message, 'error')
      setGoogleBusy(false)
    }
  }

  const createMeeting = async (s: PeerSession) => {
    if (meetingBusy !== null) return
    setMeetingBusy(s.id)
    try {
      await api.post(`/peer/sessions/${s.id}/meeting`)
      toast('Google Meet link created.', 'success')
    } catch (err) {
      toast(describeError(err).message, 'error')
    } finally {
      setMeetingBusy(null)
      reloadAll()
      reloadGoogle()
    }
  }

  const createRequest = async (e: FormEvent) => {
    e.preventDefault()
    if (busy) return
    const minutes = Number(form.requestedMinutes)
    const next: Record<string, string> = {
      topic: form.topic.trim() && form.topic.length <= 200 ? '' : 'Add a topic (up to 200 characters).',
      requestedMinutes: Number.isInteger(minutes) && minutes >= 5 && minutes <= 240 ? '' : 'Choose 5 to 240 minutes.',
    }
    setErrors(next)
    if (Object.values(next).some(Boolean)) return
    setBusy(true)
    try {
      await api.post('/peer/requests', { topic: form.topic.trim(), description: form.description.trim() || undefined, requestedMinutes: minutes })
      toast('Help request posted.', 'success')
      setCreating(false)
      setForm({ topic: '', description: '', requestedMinutes: '30' })
      setTab('open')
      requests.reload()
    } catch (err) {
      toast(describeError(err).message, 'error')
    } finally {
      setBusy(false)
    }
  }

  const run = async () => {
    if (!action || busy) return
    const { kind, session: s } = action
    setBusy(true)
    try {
      if (kind === 'accept') {
        const { data } = await api.post<PeerSession>(`/peer/requests/${s.id}/accept`)
        if (data.meetingUri) toast('Request accepted and the Google Meet link is ready.', 'success')
        else if (data.meetingStatus === 'FAILED') toast(data.meetingError ?? 'Request accepted, but the Google Meet link could not be created. Retry from the session card.', 'error')
        else toast('Request accepted.', 'success')
      } else {
        if (kind === 'start') await api.post(`/peer/sessions/${s.id}/start`)
        else if (kind === 'complete') await api.post(`/peer/sessions/${s.id}/complete`, String(s.learner.id) === me ? { rating: Number(rating), feedback: feedback.trim() || undefined } : {})
        else await api.post(`/peer/sessions/${s.id}/dispute`, null, { params: { reason: 'Disputed by a participant' } })
        toast('Done. The server decides the final status and any reward.', 'success')
      }
      setAction(null)
      setFeedback('')
      reloadAll()
    } catch (err) {
      toast(describeError(err).message, 'error')
    } finally {
      setBusy(false)
    }
  }

  const needle = filter.trim().toLowerCase()
  const matches = (s: PeerSession) => !needle || [s.topic, s.description, s.learner.displayName].some((v) => v?.toLowerCase().includes(needle))

  const sessionRow = (s: PeerSession) => {
    const isTeacher = String(s.teacher?.id) === me
    const iConfirmed = isTeacher ? s.teacherConfirmed : s.learnerConfirmed
    const other = isTeacher ? s.learner.displayName : s.teacher?.displayName
    return (
      <li key={s.id} className="card pad session" data-status={s.status}>
        <div>
          <strong>{s.topic}</strong>
          <p className="muted">
            With {other ?? 'a peer'} &middot; you are the {isTeacher ? 'teacher' : 'learner'} &middot; {s.requestedMinutes} min requested
            {s.actualMinutes != null && <> &middot; {s.actualMinutes} min verified</>}
          </p>
          {s.status === 'COMPLETED' && isTeacher && <p className="muted">{s.rewardIssued ? 'Teaching reward issued by the server.' : 'No teaching reward was issued for this session.'}</p>}
          {(s.status === 'ACCEPTED' || s.status === 'IN_PROGRESS') && (
            s.meetingUri ? (
              <div className="meeting-line">
                <Video size={16} aria-hidden="true" />
                <span>Google Meet link ready</span>
                <a className="button button--secondary" href={s.meetingUri} target="_blank" rel="noreferrer noopener">Join meeting</a>
              </div>
            ) : s.meetingStatus === 'FAILED' ? (
              <div className="meeting-line">
                <VideoOff size={16} aria-hidden="true" />
                <span>{s.meetingError ?? 'The Google Meet link could not be created.'}</span>
                {isTeacher && (google.data?.connected
                  ? <Btn variant="secondary" icon={RefreshCw} loading={meetingBusy === s.id} onClick={() => createMeeting(s)}>Retry link</Btn>
                  : <Btn variant="secondary" icon={Video} loading={googleBusy} onClick={connectGoogle}>Connect Google</Btn>)}
              </div>
            ) : google.data?.configured ? (
              <div className="meeting-line">
                <VideoOff size={16} aria-hidden="true" />
                <span>No Google Meet link yet.</span>
                {isTeacher && (google.data.connected
                  ? <Btn variant="secondary" icon={RefreshCw} loading={meetingBusy === s.id} onClick={() => createMeeting(s)}>Create link</Btn>
                  : <Btn variant="secondary" icon={Video} loading={googleBusy} onClick={connectGoogle}>Connect Google</Btn>)}
              </div>
            ) : null
          )}
        </div>
        <Badge tone={statusTone[s.status]}>{statusLabel[s.status]}</Badge>
        <div className="row">
          {isTeacher && s.status === 'ACCEPTED' && <Btn variant="secondary" onClick={() => setAction({ kind: 'start', session: s })}>Start session</Btn>}
          {s.status === 'IN_PROGRESS' && !iConfirmed && <Btn variant="secondary" onClick={() => setAction({ kind: 'complete', session: s })}>Confirm completion</Btn>}
          {s.status === 'IN_PROGRESS' && iConfirmed && <Badge tone="amber">Waiting for the other student</Badge>}
          {['ACCEPTED', 'IN_PROGRESS', 'COMPLETED'].includes(s.status) && <Btn variant="ghost" onClick={() => setAction({ kind: 'dispute', session: s })}>Report a problem</Btn>}
        </div>
      </li>
    )
  }

  const sessionList = (kindFilter: (s: PeerSession) => boolean, endpoint: string, emptyText: string) => (
    <ResourceView resource={sessions} endpoint={endpoint} isEmpty={(d) => d.content.filter(kindFilter).length === 0} emptyIcon={UsersRound} emptyTitle="Nothing here yet" emptyText={emptyText}>
      {(d) => <ul className="stack">{d.content.filter(kindFilter).map(sessionRow)}</ul>}
    </ResourceView>
  )

  return (
    <>
      <PageHeader eyebrow="Peer Learning" title="Learn from each other" description="Ask for help or teach a fellow student." actions={<Btn icon={Plus} onClick={() => setCreating(true)}>New help request</Btn>} />
      <div className="card pad policy">
        <strong>How teaching rewards work</strong>
        <p>The server rewards teaching only after the session is verified: both students confirm it, the learner rates it, and it meets the minimum length. The server works out the points; this screen never awards them.</p>
      </div>

      {google.status === 'ready' && google.data?.configured && (
        <div className="card pad policy">
          <strong>Google Meet</strong>
          {google.data.connected ? (
            <p>Connected. Accepting a help request creates a Google Meet link for the session.</p>
          ) : (
            <>
              <p>Connect your Google account to create a Meet link when you accept a request. LearnMate only asks for the permission to create Meet spaces.</p>
              <Btn variant="secondary" icon={Video} loading={googleBusy} onClick={connectGoogle}>Connect Google</Btn>
            </>
          )}
        </div>
      )}

      {google.status === 'ready' && google.data && !google.data.configured && (
        <div className="card pad policy">
          <strong>Google Meet is not configured</strong>
          <p>The server administrator needs to set the Google OAuth client ID and secret. Peer sessions still work without a meeting link.</p>
        </div>
      )}

      <Tabs label="Peer learning sections" value={tab} onChange={(t) => { setTab(t); setPage(0) }} tabs={[{ id: 'open', label: 'Help requests' }, { id: 'mine', label: 'My sessions' }, { id: 'taught', label: 'Teaching history' }]} />

      {tab === 'open' && (
        <>
          <div className="filters"><Field label="Filter requests on this page" value={filter} onChange={(e) => setFilter(e.target.value)} placeholder="Topic or keyword" /></div>
          <ResourceView resource={requests} endpoint="GET /peer/requests" isEmpty={(d) => d.content.length === 0} emptyIcon={UsersRound} emptyTitle="No open requests" emptyText="Be the first to ask for help.">
            {(d) => (
              <>
                <ul className="stack">
                  {d.content.filter(matches).map((r) => (
                    <li key={r.id} className="card pad session" data-status={r.status}>
                      <div>
                        <strong>{r.topic}</strong>
                        <p className="muted">{r.description ? `${r.description} \u00b7 ` : ''}asked by {r.learner.displayName} &middot; {r.requestedMinutes} min &middot; {new Date(r.createdAt).toLocaleDateString()}</p>
                      </div>
                      <Badge tone={statusTone[r.status]}>{statusLabel[r.status]}</Badge>
                      {String(r.learner.id) !== me ? <Btn variant="secondary" onClick={() => setAction({ kind: 'accept', session: r })}>Teach this</Btn> : <Badge tone="neutral">Your request</Badge>}
                    </li>
                  ))}
                </ul>
                <Pager page={d.number} totalPages={d.totalPages} onChange={setPage} />
              </>
            )}
          </ResourceView>
        </>
      )}
      {tab === 'mine' && sessionList(() => true, 'GET /peer/sessions', 'Accept a request or ask for help to start a session.')}
      {tab === 'taught' && sessionList((s) => String(s.teacher?.id) === me && s.status === 'COMPLETED', 'GET /peer/sessions', 'Sessions you taught and completed appear here.')}

      <Modal open={creating} onClose={() => setCreating(false)} title="New help request" footer={<><Btn variant="secondary" onClick={() => setCreating(false)}>Cancel</Btn><Btn type="submit" form="help-form" loading={busy}>Post request</Btn></>}>
        <form id="help-form" className="form-stack" onSubmit={createRequest} noValidate>
          <Field label="Topic" maxLength={200} value={form.topic} error={errors.topic} onChange={(e) => setForm({ ...form, topic: e.target.value })} />
          <TextAreaField label="What do you need help with? (optional)" rows={4} maxLength={2000} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <Field label="Session length (minutes)" type="number" min={5} max={240} value={form.requestedMinutes} error={errors.requestedMinutes} onChange={(e) => setForm({ ...form, requestedMinutes: e.target.value })} />
        </form>
      </Modal>

      <Modal
        open={!!action}
        onClose={() => setAction(null)}
        title={action ? { accept: 'Teach this request?', start: 'Start the session?', complete: 'Confirm the session happened?', dispute: 'Report a problem?' }[action.kind] : ''}
        footer={<><Btn variant="secondary" onClick={() => setAction(null)}>Cancel</Btn><Btn variant={action?.kind === 'dispute' ? 'danger' : 'primary'} loading={busy} disabled={action?.kind === 'accept' && !googleReady} onClick={run}>Confirm</Btn></>}
      >
        {action?.kind === 'accept' && (
          <>
            <p>You will become the teacher for <strong>{action.session.topic}</strong>.</p>
            {google.data?.configured && !google.data.connected && (
              <p className="muted">Connect your Google account first so the server can create the meeting link. <Btn variant="secondary" icon={Video} loading={googleBusy} onClick={connectGoogle}>Connect Google</Btn></p>
            )}
          </>
        )}
        {action?.kind === 'start' && <p>Start only when you and the learner are ready. The server records the start time.</p>}
        {action?.kind === 'complete' && (
          String(action.session.learner.id) === me ? (
            <>
              <p>Confirm the session took place and rate it. A rating is required for the teacher to be rewarded.</p>
              <SelectField label="How helpful was it?" value={rating} onChange={(e) => setRating(e.target.value)}>{['5', '4', '3', '2', '1'].map((n) => (<option key={n} value={n}>{n} / 5</option>))}</SelectField>
              <TextAreaField label="Feedback (optional)" rows={3} maxLength={1000} value={feedback} onChange={(e) => setFeedback(e.target.value)} />
            </>
          ) : <p>Confirm the session took place. Rewards are issued only after both students confirm.</p>
        )}
        {action?.kind === 'dispute' && <p>An admin will review <strong>{action.session.topic}</strong>. Use this if the session did not happen as recorded or conduct was a problem.</p>}
      </Modal>
    </>
  )
}
