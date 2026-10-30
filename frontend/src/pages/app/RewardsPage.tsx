import { useMemo, useState } from 'react'
import { Gift, Handshake, Sparkles } from 'lucide-react'
import Pager from '../../components/Pager'
import ResourceView from '../../components/ResourceView'
import { Badge, Btn, Field, Modal, Tabs } from '../../components/ui'
import { useLive } from '../../hooks/useLive'
import { useResource } from '../../hooks/useResource'
import { prettyEnum } from '../../lib/dates'
import type { Course, CourseRedemption, Page, RewardTransaction } from '../../lib/types'
import api, { describeError } from '../../services/api'
import { useToast } from '../../toast/toastContext'

type CourseState = 'AVAILABLE' | 'OWNED' | 'INSUFFICIENT_POINTS'
const stateInfo: Record<CourseState, { label: string; tone: 'green' | 'amber' | 'blue' }> = {
  AVAILABLE: { label: 'Available', tone: 'green' },
  OWNED: { label: 'Owned', tone: 'blue' },
  INSUFFICIENT_POINTS: { label: 'Not enough points', tone: 'amber' },
}

export default function RewardsPage() {
  const live = useLive()
  const toast = useToast()
  const [tab, setTab] = useState<'catalogue' | 'redemptions' | 'transactions'>('catalogue')
  const [filter, setFilter] = useState('')
  const [kind, setKind] = useState('')
  const [page, setPage] = useState(0)
  const [txPage, setTxPage] = useState(0)
  const balance = useResource<{ balance: number }>(live ? '/rewards/balance' : null)
  const courses = useResource<Page<Course>>(live && tab === 'catalogue' ? '/courses' : null, { type: kind, page: String(page), size: '12' })
  const mine = useResource<Page<CourseRedemption>>(live ? '/courses/my-courses' : null, { size: '100' })
  const transactions = useResource<Page<RewardTransaction>>(live && tab === 'transactions' ? '/rewards/transactions' : null, { page: String(txPage), size: '15' })
  const [target, setTarget] = useState<Course | null>(null)
  const [busy, setBusy] = useState(false)

  const ownedIds = useMemo(() => new Set((mine.data?.content ?? []).filter((r) => r.status === 'ACTIVE').map((r) => r.course.id)), [mine.data])
  const stateOf = (c: Course): CourseState => (ownedIds.has(c.id) ? 'OWNED' : balance.data && balance.data.balance < c.pointsPrice ? 'INSUFFICIENT_POINTS' : 'AVAILABLE')

  // Access is shown as granted only after the server confirms the redemption.
  const redeem = async () => {
    if (!target || busy) return
    setBusy(true)
    try {
      await api.post(`/courses/${target.id}/redeem`)
      toast(`"${target.title}" redeemed. The server confirmed your access.`, 'success')
      setTarget(null)
      balance.reload()
      mine.reload()
    } catch (err) {
      toast(`Redemption failed: ${describeError(err).message}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  const needle = filter.trim().toLowerCase()
  return (
    <>
      <section className="card hero-panel enter" aria-labelledby="rewards-title">
        <div>
          <p className="date-chip"><Sparkles size={14} aria-hidden="true" /> Rewards Store</p>
          <h1 id="rewards-title">Turn learning into new skills</h1>
          <p>Earn points through verified teaching and streak rewards, then redeem them for courses LearnMate is authorised to offer.</p>
        </div>
        <div className="balance-card" aria-label="Points balance">
          <p>Your points balance</p>
          <strong>{balance.data ? balance.data.balance.toLocaleString() : '\u2014'}</strong>
          <small>{balance.data ? 'Updated by the server after each reward or redemption.' : 'Your real balance appears here once you are signed in with a real account.'}</small>
        </div>
      </section>

      <Tabs label="Rewards sections" value={tab} onChange={setTab} tabs={[{ id: 'catalogue', label: 'Courses' }, { id: 'redemptions', label: 'My courses' }, { id: 'transactions', label: 'Point history' }]} />
      {tab === 'catalogue' && (
        <>
          <div className="chip-row" role="group" aria-label="Course type">
            {([['', 'All courses'], ['INTERNAL', 'LearnMate courses'], ['PARTNER', 'Approved partners']] as const).map(([v, l]) => (
              <button key={v} type="button" className={`chip ${kind === v ? 'is-active' : ''}`} aria-pressed={kind === v} onClick={() => { setKind(v); setPage(0) }}>{l}</button>
            ))}
          </div>
          <div className="filters">
            <Field label="Filter courses on this page" value={filter} onChange={(e) => setFilter(e.target.value)} placeholder="Title, provider or category" />
          </div>
          <ResourceView resource={courses} endpoint="GET /courses" idleHint="In the live app, approved internal and partner courses appear here as cards with a points price." isEmpty={(d) => d.content.length === 0} emptyIcon={Handshake} emptyTitle="No courses available yet" emptyText="Courses appear here once an admin publishes them.">
            {(d) => {
              const shown = d.content.filter((c) => !needle || [c.title, c.providerName, c.category, c.description].some((v) => v?.toLowerCase().includes(needle)))
              return (
                <>
                  {shown.length === 0 ? <p className="card pad muted">No course on this page matches your filter.</p> : (
                    <div className="feature-grid three">
                      {shown.map((c, i) => {
                        const st = stateOf(c)
                        const info = stateInfo[st]
                        return (
                          <div className="card feature-card card--course enter" style={{ '--i': i } as React.CSSProperties} key={c.id}>
                            <div className="feature-card-top"><Badge tone={c.courseType === 'PARTNER' ? 'blue' : 'violet'}>{c.courseType === 'PARTNER' ? 'Partner' : 'LearnMate'}</Badge><Badge tone={info.tone}>{info.label}</Badge></div>
                            <h3>{c.title}</h3>
                            <p className="muted">{[c.providerName, c.category].filter(Boolean).join(' \u00b7 ')}</p>
                            {c.description && <p>{c.description}</p>}
                            <p className="price"><Gift size={15} aria-hidden="true" /> <strong>{c.pointsPrice.toLocaleString()} points</strong></p>
                            <Btn variant={st === 'AVAILABLE' ? 'primary' : 'secondary'} disabled={st !== 'AVAILABLE'} onClick={() => setTarget(c)}>{st === 'OWNED' ? 'Owned' : 'Redeem'}</Btn>
                          </div>
                        )
                      })}
                    </div>
                  )}
                  <Pager page={d.number} totalPages={d.totalPages} onChange={setPage} />
                </>
              )
            }}
          </ResourceView>
        </>
      )}
      {tab === 'redemptions' && (
        <ResourceView resource={mine} endpoint="GET /courses/my-courses" isEmpty={(d) => d.content.length === 0} emptyIcon={Gift} emptyTitle="No redeemed courses yet" emptyText="Courses you redeem with points appear here.">
          {(d) => (
            <div className="table-wrap">
              <table>
                <thead><tr><th scope="col">Course</th><th scope="col">Points spent</th><th scope="col">Status</th><th scope="col">Redeemed</th></tr></thead>
                <tbody>{d.content.map((r) => (<tr key={r.id}><td>{r.course.title}</td><td>{r.pointsSpent.toLocaleString()}</td><td><Badge tone={r.status === 'ACTIVE' ? 'green' : 'neutral'}>{prettyEnum(r.status)}</Badge></td><td>{new Date(r.redeemedAt).toLocaleString()}</td></tr>))}</tbody>
              </table>
            </div>
          )}
        </ResourceView>
      )}
      {tab === 'transactions' && (
        <ResourceView resource={transactions} endpoint="GET /rewards/transactions" isEmpty={(d) => d.content.length === 0} emptyIcon={Gift} emptyTitle="No point activity yet">
          {(d) => (
            <>
              <div className="table-wrap">
                <table>
                  <thead><tr><th scope="col">Description</th><th scope="col">Type</th><th scope="col">Points</th><th scope="col">Date</th></tr></thead>
                  <tbody>{d.content.map((t) => (<tr key={t.id}><td>{t.description}</td><td>{prettyEnum(t.type)}</td><td><strong>{t.amount > 0 ? '+' : ''}{t.amount.toLocaleString()}</strong></td><td>{new Date(t.createdAt).toLocaleString()}</td></tr>))}</tbody>
                </table>
              </div>
              <Pager page={d.number} totalPages={d.totalPages} onChange={setTxPage} />
            </>
          )}
        </ResourceView>
      )}
      <Modal open={!!target} onClose={() => setTarget(null)} title="Redeem this course?" footer={<><Btn variant="secondary" onClick={() => setTarget(null)}>Cancel</Btn><Btn loading={busy} onClick={redeem}>Confirm redemption</Btn></>}>
        <p><strong>{target?.title}</strong> costs <strong>{target?.pointsPrice.toLocaleString()} points</strong>.</p>
        <p className="muted">Access is granted only after the server confirms your balance and records the redemption.</p>
      </Modal>
    </>
  )
}
