import { useState } from 'react'
import { History } from 'lucide-react'
import Pager from '../../components/Pager'
import ResourceView from '../../components/ResourceView'
import { Badge, PageHeader, SelectField } from '../../components/ui'
import { useLive } from '../../hooks/useLive'
import { useResource } from '../../hooks/useResource'
import { prettyEnum } from '../../lib/dates'
import type { Course, CourseRedemption, LearningActivity, MockExamAttempt, Page, PeerSession, QuizAttempt, RewardTransaction, SubmissionSummary } from '../../lib/types'

type Row = { id: number; title: string; status: string; at: string }
type Source = { label: string; path: string; map: (x: never) => Row }
const m = <T,>(f: (x: T) => Row) => f as unknown as (x: never) => Row
const score = (s?: number | null, max?: number | null) => (s != null && max != null ? ` \u00b7 ${s}/${max}` : '')

// Each kind of history comes from its own backend endpoint; this page normalises them into one list.
const sources: Record<string, Source> = {
  activity: { label: 'Learning activity (XP)', path: '/progress/history', map: m((x: LearningActivity) => ({ id: x.id, title: prettyEnum(x.activityType), status: `+${x.xpAwarded} XP`, at: x.createdAt })) },
  code: { label: 'Coding submissions', path: '/exam/submissions', map: m((x: SubmissionSummary) => ({ id: x.id, title: `${x.problemTitle ?? 'Problem'} (${x.language})`, status: prettyEnum(x.status), at: x.submittedAt })) },
  quiz: { label: 'Quick Revision attempts', path: '/exam/attempts', map: m((x: QuizAttempt) => ({ id: x.id, title: x.quiz.title, status: prettyEnum(x.status) + score(x.score, x.maxScore), at: x.startedAt })) },
  mock: { label: 'Mock exams', path: '/exam/mock-attempts', map: m((x: MockExamAttempt) => ({ id: x.id, title: x.exam.title, status: prettyEnum(x.status) + score(x.score, x.maxScore) + (x.passed != null ? (x.passed ? ' \u00b7 passed' : ' \u00b7 not passed') : ''), at: x.startedAt })) },
  peer: { label: 'Peer sessions', path: '/peer/sessions', map: m((x: PeerSession) => ({ id: x.id, title: x.topic, status: prettyEnum(x.status), at: x.createdAt })) },
  rewards: { label: 'Reward transactions', path: '/rewards/transactions', map: m((x: RewardTransaction) => ({ id: x.id, title: x.description, status: `${x.amount > 0 ? '+' : ''}${x.amount} points`, at: x.createdAt })) },
  courses: { label: 'Course redemptions', path: '/courses/my-courses', map: m((x: CourseRedemption & { course: Course }) => ({ id: x.id, title: x.course.title, status: `${prettyEnum(x.status)} \u00b7 ${x.pointsSpent} points`, at: x.redeemedAt })) },
}

export default function HistoryPage() {
  const live = useLive()
  const [kind, setKind] = useState('activity')
  const [page, setPage] = useState(0)
  const src = sources[kind]
  const res = useResource<Page<unknown>>(live ? src.path : null, { page: String(page), size: '15' })
  return (
    <>
      <PageHeader eyebrow="Learning History" title="Everything you've done" description="Pick a kind of activity to see its history." />
      <div className="filters">
        <SelectField label="Activity type" value={kind} onChange={(e) => { setKind(e.target.value); setPage(0) }}>
          {Object.entries(sources).map(([k, s]) => (<option key={k} value={k}>{s.label}</option>))}
        </SelectField>
      </div>
      <ResourceView resource={res} endpoint={`GET ${src.path}`} isEmpty={(d) => d.content.length === 0} emptyIcon={History} emptyTitle="Nothing here yet" emptyText="Your activity will show up here.">
        {(d) => (
          <>
            <ul className="activity-list card pad">
              {d.content.map((raw) => {
                const h = src.map(raw as never)
                return (
                  <li key={h.id}><span>{h.title}</span><Badge tone="neutral">{h.status}</Badge><time dateTime={h.at}>{new Date(h.at).toLocaleString()}</time></li>
                )
              })}
            </ul>
            <Pager page={d.number} totalPages={d.totalPages} onChange={setPage} />
          </>
        )}
      </ResourceView>
    </>
  )
}
