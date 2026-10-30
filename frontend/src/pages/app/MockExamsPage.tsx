import { useState } from 'react'
import { ClipboardCheck, Clock } from 'lucide-react'
import ExamRunner, { type RunnerResult } from '../../components/ExamRunner'
import Pager from '../../components/Pager'
import ResourceView from '../../components/ResourceView'
import { Badge, Btn, Modal, PageHeader, Tabs } from '../../components/ui'
import { useLive } from '../../hooks/useLive'
import { useResource } from '../../hooks/useResource'
import { prettyEnum } from '../../lib/dates'
import { resultFromAttempt, savedAnswers, toRunnerQuestions } from '../../lib/examAdapter'
import { sampleRunnerQuestions, scoreSample } from '../../lib/sampleQuiz'
import type { MockExam, MockExamAttempt, Page } from '../../lib/types'
import api, { describeError, serverNow } from '../../services/api'
import { useToast } from '../../toast/toastContext'

type Session = { kind: 'live'; attempt: MockExamAttempt } | { kind: 'sample' }

export default function MockExamsPage() {
  const live = useLive()
  const toast = useToast()
  const [tab, setTab] = useState<'exams' | 'history'>('exams')
  const [page, setPage] = useState(0)
  const exams = useResource<Page<MockExam>>(live && tab === 'exams' ? '/exam/mock-exams' : null, { page: String(page), size: '12' })
  const history = useResource<Page<MockExamAttempt>>(live && tab === 'history' ? '/exam/mock-attempts' : null, { page: String(page), size: '15' })
  const [details, setDetails] = useState<MockExam | null>(null)
  const [sampleOpen, setSampleOpen] = useState(false)
  const [starting, setStarting] = useState(false)
  const [session, setSession] = useState<Session | null>(null)

  const start = async (exam: MockExam) => {
    if (starting) return
    setStarting(true)
    try {
      const { data } = await api.post<MockExamAttempt>(`/exam/mock-exams/${exam.id}/attempts`)
      setDetails(null)
      setSession({ kind: 'live', attempt: data })
    } catch (err) {
      const p = describeError(err)
      if (p.kind === 'conflict') {
        try {
          const { data } = await api.get<Page<MockExamAttempt>>('/exam/mock-attempts', { params: { size: 50 } })
          const active = data.content.find((a) => a.status === 'IN_PROGRESS' && a.exam.id === exam.id)
          if (active) {
            toast('Resuming your exam in progress.', 'info')
            setDetails(null)
            setSession({ kind: 'live', attempt: active })
            return
          }
        } catch {
          /* fall through */
        }
      }
      toast(p.message, 'error')
    } finally {
      setStarting(false)
    }
  }

  if (session?.kind === 'sample') {
    return (
      <>
        <PageHeader eyebrow="Mock Exams" title="Sample mock exam" />
        <ExamRunner title="Mock exam (sample)" sample questions={sampleRunnerQuestions} deadlineMs={serverNow() + 8 * 60 * 1000} finish={async (a) => scoreSample(a)} onExit={() => setSession(null)} exitLabel="Back to mock exams" />
      </>
    )
  }

  if (session?.kind === 'live') {
    const { attempt } = session
    const questions = toRunnerQuestions(attempt.exam.questions)
    return (
      <>
        <PageHeader eyebrow="Mock Exams" title={attempt.exam.title} />
        <ExamRunner
          key={attempt.id}
          title={attempt.exam.title}
          sample={false}
          questions={questions}
          deadlineMs={Date.parse(attempt.deadlineAt)}
          initialAnswers={savedAnswers(attempt.answers)}
          persistAnswer={async (qid, oid) => {
            try {
              await api.put(`/exam/mock-attempts/${attempt.id}/answers`, { questionId: Number(qid), optionId: Number(oid) })
            } catch (e) {
              throw new Error(describeError(e).message)
            }
          }}
          finish={async (): Promise<RunnerResult> => {
            try {
              const { data } = await api.post<MockExamAttempt>(`/exam/mock-attempts/${attempt.id}/submit`)
              let topics: RunnerResult['topics']
              try {
                const perf = await api.get<Record<string, [number, number]>>(`/exam/mock-attempts/${attempt.id}/performance`)
                topics = Object.entries(perf.data).map(([name, [earned, max]]) => ({ name, earned, max }))
              } catch {
                /* topic breakdown is optional */
              }
              return resultFromAttempt(questions, data, topics)
            } catch (e) {
              throw new Error(describeError(e).message)
            }
          }}
          onExit={() => { setSession(null); history.reload() }}
          exitLabel="Back to mock exams"
        />
      </>
    )
  }

  return (
    <>
      <PageHeader eyebrow="Exam Centre" title="Mock Exams" description="Full-length timed exams. The server controls the start time, deadline and scoring." actions={<Btn variant="secondary" onClick={() => setSampleOpen(true)}>Try sample exam</Btn>} />
      <Tabs label="Mock exam sections" value={tab} onChange={(t) => { setTab(t); setPage(0) }} tabs={[{ id: 'exams', label: 'Exams' }, { id: 'history', label: 'Attempt history' }]} />
      {tab === 'exams' && (
        <ResourceView resource={exams} endpoint="GET /exam/mock-exams" idleHint="In the live app, published mock exams appear here." isEmpty={(d) => d.content.length === 0} emptyIcon={ClipboardCheck} emptyTitle="No mock exams published yet" emptyText="Check back soon.">
          {(d) => (
            <>
              <div className="feature-grid three">
                {d.content.map((m) => (
                  <div className="card feature-card" key={m.id}>
                    <div className="feature-card-top">{m.subject && <Badge tone="blue">{m.subject}</Badge>}<Badge tone="neutral">Pass {m.passPercentage}%</Badge></div>
                    <h3>{m.title}</h3>
                    <p className="muted"><Clock size={13} aria-hidden="true" /> {m.durationMinutes} min &middot; {m.questions.length} questions</p>
                    <Btn variant="secondary" onClick={() => setDetails(m)}>View details</Btn>
                  </div>
                ))}
              </div>
              <Pager page={d.number} totalPages={d.totalPages} onChange={setPage} />
            </>
          )}
        </ResourceView>
      )}
      {tab === 'history' && (
        <ResourceView resource={history} endpoint="GET /exam/mock-attempts" isEmpty={(d) => d.content.length === 0} emptyIcon={ClipboardCheck} emptyTitle="No attempts yet">
          {(d) => (
            <>
              <div className="table-wrap">
                <table>
                  <thead><tr><th scope="col">Exam</th><th scope="col">Status</th><th scope="col">Score</th><th scope="col">Result</th><th scope="col">Started</th></tr></thead>
                  <tbody>{d.content.map((a) => (<tr key={a.id}><td>{a.exam.title}</td><td>{prettyEnum(a.status)}</td><td>{a.score != null ? `${a.score} / ${a.maxScore}` : '\u2014'}</td><td>{a.passed == null ? '\u2014' : <Badge tone={a.passed ? 'green' : 'amber'}>{a.passed ? 'Passed' : 'Not passed'}</Badge>}</td><td>{new Date(a.startedAt).toLocaleString()}</td></tr>))}</tbody>
                </table>
              </div>
              <Pager page={d.number} totalPages={d.totalPages} onChange={setPage} />
            </>
          )}
        </ResourceView>
      )}

      <Modal open={!!details} onClose={() => setDetails(null)} title={details?.title ?? ''} footer={<><Btn variant="secondary" onClick={() => setDetails(null)}>Cancel</Btn><Btn loading={starting} onClick={() => details && void start(details)}>Start exam</Btn></>}>
        {details && (
          <>
            <ul className="plain-list">
              <li>{details.questions.length} questions &middot; {details.durationMinutes} minutes</li>
              <li>Pass mark: {details.passPercentage}%</li>
              <li>The exam submits automatically when time ends.</li>
            </ul>
            {details.instructions && <p>{details.instructions}</p>}
            <p className="muted">Starting begins the timer on the server.</p>
          </>
        )}
      </Modal>
      <Modal open={sampleOpen} onClose={() => setSampleOpen(false)} title="Before you begin" footer={<><Btn variant="secondary" onClick={() => setSampleOpen(false)}>Cancel</Btn><Btn onClick={() => { setSampleOpen(false); setSession({ kind: 'sample' }) }}>Start sample exam</Btn></>}>
        <Badge tone="amber">Sample mode</Badge>
        <ul className="plain-list">
          <li>8 questions &middot; 8 minutes</li>
          <li>Results are computed in your browser for demonstration and are not official.</li>
        </ul>
      </Modal>
    </>
  )
}
