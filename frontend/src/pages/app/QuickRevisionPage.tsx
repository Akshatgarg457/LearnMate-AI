import { useState } from 'react'
import { Clock, Timer } from 'lucide-react'
import ExamRunner from '../../components/ExamRunner'
import Pager from '../../components/Pager'
import ResourceView from '../../components/ResourceView'
import { Badge, Btn, Field, PageHeader, Tabs } from '../../components/ui'
import { useDebounced } from '../../hooks/useDebounced'
import { useLive } from '../../hooks/useLive'
import { useResource } from '../../hooks/useResource'
import { prettyEnum } from '../../lib/dates'
import { resultFromAttempt, savedAnswers, toRunnerQuestions } from '../../lib/examAdapter'
import { sampleRunnerQuestions, scoreSample } from '../../lib/sampleQuiz'
import type { Page, Quiz, QuizAttempt } from '../../lib/types'
import api, { describeError, serverNow } from '../../services/api'
import { useToast } from '../../toast/toastContext'

type Session = { kind: 'live'; attempt: QuizAttempt } | { kind: 'sample'; timed: boolean }

export default function QuickRevisionPage() {
  const live = useLive()
  const toast = useToast()
  const [tab, setTab] = useState<'quizzes' | 'history'>('quizzes')
  const [subject, setSubject] = useState('')
  const [page, setPage] = useState(0)
  const [timed, setTimed] = useState(true)
  const [starting, setStarting] = useState<number | null>(null)
  const [session, setSession] = useState<Session | null>(null)
  const q = useDebounced(subject)
  const quizzes = useResource<Page<Quiz>>(live && tab === 'quizzes' ? '/exam/quizzes' : null, { subject: q, page: String(page), size: '12' })
  const history = useResource<Page<QuizAttempt>>(live && tab === 'history' ? '/exam/attempts' : null, { page: String(page), size: '15' })

  const start = async (quiz: Quiz) => {
    if (starting !== null) return
    setStarting(quiz.id)
    try {
      const { data } = await api.post<QuizAttempt>(`/exam/quizzes/${quiz.id}/attempts`, { untimed: !timed })
      setSession({ kind: 'live', attempt: data })
    } catch (err) {
      const p = describeError(err)
      if (p.kind === 'conflict') {
        // An attempt is already running: resume it instead of starting another.
        try {
          const { data } = await api.get<Page<QuizAttempt>>('/exam/attempts', { params: { size: 50 } })
          const active = data.content.find((a) => a.status === 'IN_PROGRESS' && a.quiz.id === quiz.id)
          if (active) {
            toast('Resuming your attempt in progress.', 'info')
            setSession({ kind: 'live', attempt: active })
            return
          }
        } catch {
          /* fall through to the original message */
        }
      }
      toast(p.message, 'error')
    } finally {
      setStarting(null)
    }
  }

  if (session?.kind === 'sample') {
    return (
      <>
        <PageHeader eyebrow="Quick Revision" title="Sample quiz" />
        <ExamRunner
          title="Quick Revision (sample)"
          sample
          questions={sampleRunnerQuestions}
          deadlineMs={session.timed ? serverNow() + 8 * 45 * 1000 : null}
          finish={async (a) => scoreSample(a)}
          onExit={() => setSession(null)}
          exitLabel="Back to setup"
        />
      </>
    )
  }

  if (session?.kind === 'live') {
    const { attempt } = session
    const questions = toRunnerQuestions(attempt.quiz.questions)
    return (
      <>
        <PageHeader eyebrow="Quick Revision" title={attempt.quiz.title} />
        <ExamRunner
          key={attempt.id}
          title={attempt.quiz.title}
          sample={false}
          questions={questions}
          deadlineMs={attempt.deadlineAt ? Date.parse(attempt.deadlineAt) : null}
          initialAnswers={savedAnswers(attempt.answers)}
          persistAnswer={async (qid, oid) => {
            try {
              await api.put(`/exam/attempts/${attempt.id}/answers`, { questionId: Number(qid), optionId: Number(oid) })
            } catch (e) {
              throw new Error(describeError(e).message)
            }
          }}
          finish={async () => {
            try {
              const { data } = await api.post<QuizAttempt>(`/exam/attempts/${attempt.id}/submit`)
              return resultFromAttempt(questions, data)
            } catch (e) {
              throw new Error(describeError(e).message)
            }
          }}
          onExit={() => { setSession(null); history.reload() }}
          exitLabel="Back to quizzes"
        />
      </>
    )
  }

  return (
    <>
      <PageHeader eyebrow="Exam Centre" title="Quick Revision" description="Pick a quiz and answer short multiple-choice questions. Your answers are saved as you go and the server scores the attempt." actions={<Btn variant="secondary" onClick={() => setSession({ kind: 'sample', timed })}>Try a sample quiz</Btn>} />
      <Tabs label="Quick Revision sections" value={tab} onChange={(t) => { setTab(t); setPage(0) }} tabs={[{ id: 'quizzes', label: 'Quizzes' }, { id: 'history', label: 'Attempt history' }]} />
      {tab === 'quizzes' && (
        <>
          <div className="filters">
            <Field label="Subject" value={subject} onChange={(e) => { setSubject(e.target.value); setPage(0) }} placeholder="e.g. Java" />
            <label className="check"><input type="checkbox" checked={timed} onChange={(e) => setTimed(e.target.checked)} /> Timed (recommended)</label>
          </div>
          <ResourceView resource={quizzes} endpoint="GET /exam/quizzes" idleHint="In the live app, published quizzes appear here as cards." isEmpty={(d) => d.content.length === 0} emptyIcon={Timer} emptyTitle="No quizzes available yet" emptyText="An admin has not published any quizzes, or none match this subject.">
            {(d) => (
              <>
                <div className="feature-grid three">
                  {d.content.map((qz) => (
                    <div className="card feature-card" key={qz.id}>
                      <div className="feature-card-top"><Badge tone="blue">{qz.subject ?? 'General'}</Badge>{qz.topic && <Badge tone="violet">{qz.topic}</Badge>}</div>
                      <h3>{qz.title}</h3>
                      {qz.description && <p className="muted">{qz.description}</p>}
                      <p className="muted"><Clock size={13} aria-hidden="true" /> {timed ? `${qz.durationMinutes} min` : 'Untimed'} &middot; {qz.questions.length} questions</p>
                      <Btn loading={starting === qz.id} disabled={starting !== null} onClick={() => void start(qz)}>Start quiz</Btn>
                    </div>
                  ))}
                </div>
                <Pager page={d.number} totalPages={d.totalPages} onChange={setPage} />
              </>
            )}
          </ResourceView>
        </>
      )}
      {tab === 'history' && (
        <ResourceView resource={history} endpoint="GET /exam/attempts" isEmpty={(d) => d.content.length === 0} emptyIcon={Timer} emptyTitle="No attempts yet" emptyText="Finished quizzes appear here.">
          {(d) => (
            <>
              <div className="table-wrap">
                <table>
                  <thead><tr><th scope="col">Quiz</th><th scope="col">Status</th><th scope="col">Score</th><th scope="col">Started</th></tr></thead>
                  <tbody>{d.content.map((a) => (<tr key={a.id}><td>{a.quiz.title}</td><td><Badge tone={a.status === 'SUBMITTED' ? 'green' : a.status === 'IN_PROGRESS' ? 'blue' : 'amber'}>{prettyEnum(a.status)}</Badge></td><td>{a.score != null ? `${a.score} / ${a.maxScore}` : '\u2014'}</td><td>{new Date(a.startedAt).toLocaleString()}</td></tr>))}</tbody>
                </table>
              </div>
              <Pager page={d.number} totalPages={d.totalPages} onChange={setPage} />
            </>
          )}
        </ResourceView>
      )}
    </>
  )
}
