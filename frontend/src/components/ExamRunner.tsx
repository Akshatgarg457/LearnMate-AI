import { useCallback, useEffect, useRef, useState } from 'react'
import { ArrowLeft, ArrowRight, Check, Clock, Flag, TriangleAlert } from 'lucide-react'
import { serverNow } from '../services/api'
import { Badge, Btn, Modal } from './ui'

export type RunnerQuestion = { id: string; topic?: string | null; prompt: string; options: { id: string; text: string }[] }

export type RunnerResult = {
  score: number
  maxScore: number
  passed?: boolean | null
  /** correct === null means unanswered. correctText / explanation are only present when the source provides them. */
  perQuestion: Record<string, { correct: boolean | null; correctText?: string; explanation?: string }>
  topics?: { name: string; earned: number; max: number }[]
  note?: string
}

type Props = {
  title: string
  questions: RunnerQuestion[]
  /** Absolute deadline in server-clock milliseconds (see serverNow), or null for untimed practice. */
  deadlineMs: number | null
  initialAnswers?: Record<string, string>
  sample: boolean
  /** Saves one answer on the server. Rejecting reverts the choice. */
  persistAnswer?: (questionId: string, optionId: string) => Promise<void>
  /** Submits and returns the server's result. The browser never scores a real attempt. */
  finish: (answers: Record<string, string>) => Promise<RunnerResult>
  onExit: () => void
  exitLabel?: string
}

const fmt = (s: number) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`

/**
 * Focused exam interface: timer, navigator, mark-for-review, confirm-submit, results.
 * The countdown is a presentation aid; the backend owns the deadline, the answers and the score.
 */
export default function ExamRunner({ title, questions, deadlineMs, initialAnswers, sample, persistAnswer, finish, onExit, exitLabel = 'Back' }: Props) {
  const [current, setCurrent] = useState(0)
  const [answers, setAnswers] = useState<Record<string, string>>(initialAnswers ?? {})
  const [marked, setMarked] = useState<Set<number>>(new Set())
  const [confirming, setConfirming] = useState(false)
  const [phase, setPhase] = useState<'running' | 'submitting' | 'done'>('running')
  const [result, setResult] = useState<RunnerResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [remaining, setRemaining] = useState<number | null>(deadlineMs === null ? null : Math.max(0, Math.round((deadlineMs - serverNow()) / 1000)))
  const [timedOut, setTimedOut] = useState(false)
  const submitting = useRef(false) // guards against double submission (button + timer)
  const answersRef = useRef(answers)
  answersRef.current = answers

  const submit = useCallback(async () => {
    if (submitting.current) return
    submitting.current = true
    setConfirming(false)
    setError(null)
    setPhase('submitting')
    try {
      const r = await finish(answersRef.current)
      setResult(r)
      setPhase('done')
    } catch (e) {
      submitting.current = false // allow a retry after a failed request
      setError(e instanceof Error ? e.message : 'Could not submit. Please try again.')
      setPhase('running')
    }
  }, [finish])

  useEffect(() => {
    if (phase !== 'running' || deadlineMs === null) return
    const id = window.setInterval(() => {
      const left = Math.max(0, Math.round((deadlineMs - serverNow()) / 1000))
      setRemaining(left)
      if (left === 0) {
        setTimedOut(true)
        void submit()
      }
    }, 500)
    return () => window.clearInterval(id)
  }, [phase, deadlineMs, submit])

  const choose = async (q: RunnerQuestion, optionId: string) => {
    if (phase !== 'running') return
    const previous = answers[q.id]
    setAnswers((a) => ({ ...a, [q.id]: optionId }))
    if (!persistAnswer) return
    try {
      await persistAnswer(q.id, optionId)
    } catch (e) {
      setAnswers((a) => {
        const n = { ...a }
        if (previous === undefined) delete n[q.id]
        else n[q.id] = previous
        return n
      })
      setError(e instanceof Error ? e.message : 'Could not save that answer.')
    }
  }

  const answered = Object.keys(answers).length
  const low = remaining !== null && remaining <= 60 && remaining > 0

  if (phase === 'done' && result) {
    const pct = result.maxScore > 0 ? Math.round((result.score / result.maxScore) * 100) : 0
    return (
      <div className="exam-focus">
        <div className="card pad result-head">
          <div>
            <p className="eyebrow">{timedOut ? 'Time is up: submitted automatically' : 'Submitted'}</p>
            <h2>{result.score} / {result.maxScore} <span className="muted">({pct}%)</span></h2>
            {result.passed != null && <Badge tone={result.passed ? 'green' : 'amber'}>{result.passed ? 'Passed' : 'Not passed'}</Badge>}
            {result.note && <p className="muted">{result.note}</p>}
          </div>
          <Btn onClick={onExit}>{exitLabel}</Btn>
        </div>
        {result.topics && result.topics.length > 0 && (
          <div className="card pad">
            <h3>Topic-wise performance</h3>
            <ul className="topic-list">
              {result.topics.map((t) => (
                <li key={t.name}>
                  <span>{t.name}</span>
                  <span className="progress progress--green" role="progressbar" aria-label={`${t.name} score`} aria-valuenow={t.max ? Math.round((t.earned / t.max) * 100) : 0} aria-valuemin={0} aria-valuemax={100}>
                    <span style={{ width: `${t.max ? (t.earned / t.max) * 100 : 0}%` }} />
                  </span>
                  <span>{t.earned}/{t.max}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
        <div className="review-list">
          {questions.map((q, i) => {
            const r = result.perQuestion[q.id]
            const mine = q.options.find((o) => o.id === answers[q.id])
            const state = r?.correct === true ? 'correct' : r?.correct === false ? 'incorrect' : mine ? 'ungraded' : 'unanswered'
            return (
              <div className="card pad" key={q.id}>
                <div className="row between">
                  <strong>Q{i + 1}. {q.prompt}</strong>
                  <Badge tone={state === 'correct' ? 'green' : state === 'incorrect' ? 'red' : 'amber'}>{state === 'ungraded' ? 'Not graded' : state[0].toUpperCase() + state.slice(1)}</Badge>
                </div>
                <p className="muted">Your answer: {mine ? mine.text : '\u2014'}{r?.correctText && <> &middot; Correct answer: {r.correctText}</>}</p>
                {r?.explanation && <p>{r.explanation}</p>}
              </div>
            )
          })}
        </div>
      </div>
    )
  }

  const q = questions[current]
  const busy = phase === 'submitting'
  return (
    <div className="exam-focus">
      <div className="exam-bar card">
        <div>
          <strong>{title}</strong>
          {sample && <Badge tone="amber">Sample mode</Badge>}
        </div>
        <div className={`timer ${low ? 'timer--low' : ''}`} role="timer" aria-live="off">
          <Clock size={16} aria-hidden="true" />
          {remaining === null ? 'Untimed practice' : fmt(remaining)}
          {low && <span> &middot; under a minute left</span>}
          {low && <span className="sr-only" role="alert">Less than one minute remaining</span>}
        </div>
        <Btn loading={busy} onClick={() => setConfirming(true)}>Submit</Btn>
      </div>
      {error && <p className="form-alert" role="alert"><TriangleAlert size={15} aria-hidden="true" /> {error}</p>}

      <div className="exam-grid">
        <section className="card pad exam-question" aria-labelledby="q-title">
          <p className="muted">Question {current + 1} of {questions.length}{q.topic ? ` \u00b7 ${q.topic}` : ''}</p>
          <h2 id="q-title">{q.prompt}</h2>
          <div className="options" role="radiogroup" aria-labelledby="q-title">
            {q.options.map((opt, i) => (
              <label key={opt.id} className={`option ${answers[q.id] === opt.id ? 'is-selected' : ''}`}>
                <input type="radio" name={`q-${q.id}`} disabled={busy} checked={answers[q.id] === opt.id} onChange={() => void choose(q, opt.id)} />
                <span className="option-key">{String.fromCharCode(65 + i)}</span>
                <span>{opt.text}</span>
              </label>
            ))}
          </div>
          <div className="row between exam-actions">
            <Btn variant="secondary" icon={ArrowLeft} disabled={current === 0} onClick={() => setCurrent((c) => c - 1)}>Previous</Btn>
            <Btn
              variant={marked.has(current) ? 'primary' : 'secondary'}
              icon={Flag}
              aria-pressed={marked.has(current)}
              onClick={() => setMarked((m) => { const n = new Set(m); if (n.has(current)) n.delete(current); else n.add(current); return n })}
            >
              {marked.has(current) ? 'Marked for review' : 'Mark for review'}
            </Btn>
            <Btn variant="secondary" disabled={current === questions.length - 1} onClick={() => setCurrent((c) => c + 1)}>Next <ArrowRight size={16} aria-hidden="true" /></Btn>
          </div>
        </section>

        <aside className="card pad exam-nav" aria-label="Question navigator">
          <h3>Questions</h3>
          <div className="nav-grid">
            {questions.map((qq, i) => (
              <button
                key={qq.id}
                type="button"
                className={`nav-dot ${i === current ? 'is-current' : ''} ${answers[qq.id] !== undefined ? 'is-answered' : ''} ${marked.has(i) ? 'is-marked' : ''}`}
                aria-label={`Question ${i + 1}: ${answers[qq.id] !== undefined ? 'answered' : 'unanswered'}${marked.has(i) ? ', marked for review' : ''}`}
                aria-current={i === current}
                onClick={() => setCurrent(i)}
              >
                {i + 1}
              </button>
            ))}
          </div>
          <ul className="nav-legend">
            <li><span className="nav-dot is-answered" /> Answered</li>
            <li><span className="nav-dot" /> Unanswered</li>
            <li><span className="nav-dot is-marked" /> Marked</li>
          </ul>
        </aside>
      </div>

      <Modal
        open={confirming}
        onClose={() => setConfirming(false)}
        title="Submit your attempt?"
        footer={<><Btn variant="secondary" onClick={() => setConfirming(false)}>Keep working</Btn><Btn icon={Check} onClick={() => void submit()}>Submit now</Btn></>}
      >
        <p>You answered <strong>{answered}</strong> of {questions.length} questions.</p>
        {answered < questions.length && (
          <p className="form-alert form-alert--info"><TriangleAlert size={15} aria-hidden="true" /> {questions.length - answered} unanswered and {marked.size} marked for review.</p>
        )}
        <p className="muted">You can&apos;t change answers after submitting.</p>
      </Modal>
    </div>
  )
}
