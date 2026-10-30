import { useEffect, useMemo, useRef, useState } from 'react'
import {
  Check,
  Clock,
  CodeXml,
  Copy,
  Gauge,
  History as HistoryIcon,
  LoaderCircle,
  Maximize2,
  Minimize2,
  Play,
  RotateCcw,
  Search,
  Send,
  Target,
} from 'lucide-react'
import MonacoCodeEditor from '../../components/MonacoCodeEditor'
import Pager from '../../components/Pager'
import ResourceView from '../../components/ResourceView'
import { Badge, Btn, EmptyState, Modal, PageHeader, ProgressBar, SelectField, Skeleton, Tabs } from '../../components/ui'
import { useAuth } from '../../auth/authContext'
import { useDebounced } from '../../hooks/useDebounced'
import { useLive } from '../../hooks/useLive'
import { useResource } from '../../hooks/useResource'
import { prettyEnum } from '../../lib/dates'
import type { CodeLanguage, CodeProgress, CodeSubmission, Page, Problem, RunResult, SubmissionCase, SubmissionStatus, SubmissionSummary, TestCase } from '../../lib/types'
import api, { describeError } from '../../services/api'
import { useTheme } from '../../theme/themeContext'
import { useToast } from '../../toast/toastContext'

const LANGUAGES: { id: CodeLanguage; label: string; monaco: string }[] = [
  { id: 'JAVA', label: 'Java', monaco: 'java' },
  { id: 'PYTHON', label: 'Python', monaco: 'python' },
  { id: 'CPP', label: 'C++', monaco: 'cpp' },
  { id: 'JAVASCRIPT', label: 'JavaScript', monaco: 'javascript' },
]
const LANGUAGE_LABEL = Object.fromEntries(LANGUAGES.map((l) => [l.id, l.label])) as Record<CodeLanguage, string>

const TOPICS = [
  'Arrays', 'Strings', 'Hashing', 'Searching', 'Sorting', 'Two Pointers', 'Sliding Window',
  'Recursion', 'Linked Lists', 'Stacks', 'Queues', 'Binary Trees', 'Binary Search Trees',
  'Graphs', 'Dynamic Programming', 'Greedy Algorithms',
]

const DIFFICULTY_TONE = { EASY: 'green', MEDIUM: 'amber', HARD: 'red' } as const

const FAILED_STATUSES: SubmissionStatus[] = [
  'WRONG_ANSWER', 'TIME_LIMIT_EXCEEDED', 'MEMORY_LIMIT_EXCEEDED', 'COMPILATION_ERROR', 'RUNTIME_ERROR', 'JUDGE_ERROR',
]

const verdictTone = (status: SubmissionStatus) =>
  status === 'ACCEPTED' ? 'green' : status === 'PENDING' || status === 'PROCESSING' ? 'blue' : 'red'

const savedKey = (userId: string, problemId: number, language: CodeLanguage) =>
  `lm.code.v1.${userId}.${problemId}.${language}`

function readSaved(userId: string, problemId: number, language: CodeLanguage): string | null {
  try {
    return localStorage.getItem(savedKey(userId, problemId, language))
  } catch {
    return null
  }
}

function writeSaved(userId: string, problemId: number, language: CodeLanguage, code: string) {
  try {
    localStorage.setItem(savedKey(userId, problemId, language), code)
  } catch {
    /* storage unavailable: code still lives in memory for this visit */
  }
}

function clearSaved(userId: string, problemId: number, language: CodeLanguage) {
  try {
    localStorage.removeItem(savedKey(userId, problemId, language))
  } catch {
    /* ignore */
  }
}

/* ---------- Results ---------- */

function CaseResults({ cases }: { cases: SubmissionCase[] }) {
  if (cases.length === 0) return <p className="muted">No test case results were returned.</p>
  return (
    <ul className="case-list">
      {cases.map((c) => (
        <li className={`case-card ${c.passed ? 'is-pass' : 'is-fail'}`} key={c.index}>
          <div className="case-head">
            <strong>Case {c.index + 1}</strong>
            <Badge tone="neutral">{c.sample ? 'Sample' : 'Hidden'}</Badge>
            <Badge tone={c.passed ? 'green' : 'red'}>{c.passed ? 'Passed' : 'Failed'}</Badge>
            <span className="muted">
              {prettyEnum(c.status)}
              {c.executionTimeMs != null ? ` · ${c.executionTimeMs} ms` : ''}
              {c.memoryUsedKb != null ? ` · ${(c.memoryUsedKb / 1024).toFixed(1)} MB` : ''}
            </span>
          </div>
          {c.sample ? (
            <div className="case-io">
              <div><span>Input</span><pre>{c.input || '(empty)'}</pre></div>
              <div><span>Expected</span><pre>{c.expectedOutput ?? ''}</pre></div>
              <div><span>Your output</span><pre>{c.actualOutput ?? '(no output)'}</pre></div>
              {c.stderr && <div className="case-error"><span>Error</span><pre>{c.stderr}</pre></div>}
            </div>
          ) : (
            <p className="muted case-hidden">Hidden test data and expected output stay on the server. Only the verdict is reported.</p>
          )}
        </li>
      ))}
    </ul>
  )
}

function outcomeView(kind: 'run' | 'submit', data: RunResult | CodeSubmission) {
  if (kind === 'run') {
    const run = data as RunResult
    return { status: run.status, passed: run.passed, total: run.total, timeMs: run.executionTimeMs, memoryKb: run.memoryUsedKb, error: run.compileOutput, cases: run.cases }
  }
  const sub = data as CodeSubmission
  return { status: sub.status, passed: sub.passedTests, total: sub.totalTests, timeMs: sub.executionTimeMs, memoryKb: sub.memoryUsedKb, error: sub.errorMessage, cases: sub.cases }
}

/* ---------- Submission comparison ---------- */

function lcsDiff(before: string[], after: string[]) {
  const a = before.slice(0, 500)
  const b = after.slice(0, 500)
  const dp = Array.from({ length: a.length + 1 }, () => new Array<number>(b.length + 1).fill(0))
  for (let i = a.length - 1; i >= 0; i--) {
    for (let j = b.length - 1; j >= 0; j--) {
      dp[i][j] = a[i] === b[j] ? dp[i + 1][j + 1] + 1 : Math.max(dp[i + 1][j], dp[i][j + 1])
    }
  }
  const out: { type: 'same' | 'add' | 'remove'; text: string }[] = []
  let i = 0
  let j = 0
  while (i < a.length && j < b.length) {
    if (a[i] === b[j]) {
      out.push({ type: 'same', text: a[i] })
      i++
      j++
    } else if (dp[i + 1][j] >= dp[i][j + 1]) {
      out.push({ type: 'remove', text: a[i++] })
    } else {
      out.push({ type: 'add', text: b[j++] })
    }
  }
  while (i < a.length) out.push({ type: 'remove', text: a[i++] })
  while (j < b.length) out.push({ type: 'add', text: b[j++] })
  return out
}

function CodeDiff({ before, after }: { before: string; after: string }) {
  const lines = useMemo(() => lcsDiff(before.split('\n'), after.split('\n')), [before, after])
  const added = lines.filter((l) => l.type === 'add').length
  const removed = lines.filter((l) => l.type === 'remove').length
  return (
    <div className="code-diff">
      <p className="muted">Submitted version → current editor: <strong className="diff-add">+{added}</strong> added, <strong className="diff-remove">−{removed}</strong> removed.</p>
      <pre>
        {lines.map((line, index) => (
          <span key={index} className={`diff-line diff-${line.type}`}>
            {line.type === 'add' ? '+ ' : line.type === 'remove' ? '− ' : '  '}
            {line.text || ' '}
            {'\n'}
          </span>
        ))}
      </pre>
      {(before.split('\n').length > 500 || after.split('\n').length > 500) && <p className="muted">Comparison is limited to the first 500 lines.</p>}
    </div>
  )
}

/* ---------- Workspace ---------- */

type RunState =
  | { phase: 'idle' }
  | { phase: 'running'; action: 'run' | 'submit' }
  | { phase: 'done'; outcome: { kind: 'run'; data: RunResult } | { kind: 'submit'; data: CodeSubmission } }
  | { phase: 'error'; message: string }

function Workspace({ problem, onBack }: { problem: Problem; onBack: () => void }) {
  const live = useLive()
  const { session } = useAuth()
  const { theme } = useTheme()
  const toast = useToast()
  const userId = session?.user.id ?? 'anon'

  const [language, setLanguage] = useState<CodeLanguage>('PYTHON')
  const [fontSize, setFontSize] = useState(14)
  const [fullscreen, setFullscreen] = useState(false)
  const [panel, setPanel] = useState<'problem' | 'history'>('problem')
  const [resultTab, setResultTab] = useState<'results' | 'output' | 'details'>('results')
  const [code, setCode] = useState<Partial<Record<CodeLanguage, string>>>({})
  const [runState, setRunState] = useState<RunState>({ phase: 'idle' })
  const [historyStatus, setHistoryStatus] = useState('')
  const [historyLanguage, setHistoryLanguage] = useState('')
  const [historyPage, setHistoryPage] = useState(0)
  const [openSubmission, setOpenSubmission] = useState<number | null>(null)
  const [compare, setCompare] = useState(false)
  const detailRef = useRef<HTMLDivElement>(null)

  const samples = useResource<TestCase[]>(live ? `/exam/problems/${problem.id}/test-cases` : null)
  const starters = useResource<Record<string, string>>(live ? `/exam/problems/${problem.id}/starter-code` : null)
  const history = useResource<Page<SubmissionSummary>>(live && panel === 'history' ? '/exam/submissions' : null, {
    problemId: String(problem.id),
    status: historyStatus,
    language: historyLanguage,
    page: String(historyPage),
    size: '10',
  })
  const detail = useResource<CodeSubmission>(live && openSubmission !== null ? `/exam/submissions/${openSubmission}` : null)

  const starterCodes = useMemo(() => {
    if (!starters.data) return null
    const next: Partial<Record<CodeLanguage, string>> = {}
    for (const lang of LANGUAGES) {
      next[lang.id] = readSaved(userId, problem.id, lang.id) ?? starters.data[lang.id] ?? ''
    }
    return next
  }, [starters.data, problem.id, userId])

  useEffect(() => {
    if (!fullscreen) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setFullscreen(false)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [fullscreen])

  const currentCode = code[language] ?? starterCodes?.[language] ?? ''
  const monacoLanguage = LANGUAGES.find((l) => l.id === language)?.monaco ?? 'plaintext'
  const examples = samples.data ?? []
  const busy = runState.phase === 'running'

  const updateCode = (value: string) => {
    setCode((prev) => ({ ...prev, [language]: value }))
    writeSaved(userId, problem.id, language, value)
  }

  const resetCode = () => {
    const starter = starters.data?.[language] ?? ''
    setCode((prev) => ({ ...prev, [language]: starter }))
    clearSaved(userId, problem.id, language)
    toast('Starter code restored.', 'info')
  }

  const copyCode = async () => {
    try {
      await navigator.clipboard.writeText(currentCode)
      toast('Code copied to the clipboard.', 'success')
    } catch {
      toast('Could not copy code. Select the editor text and copy it manually.', 'error')
    }
  }

  const execute = async (kind: 'run' | 'submit') => {
    if (busy) return
    if (!currentCode.trim()) {
      toast('Write some code first.', 'error')
      return
    }
    setRunState({ phase: 'running', action: kind })
    setResultTab('results')
    detailRef.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    try {
      if (kind === 'run') {
        const { data } = await api.post<RunResult>(`/exam/problems/${problem.id}/run`, { language, code: currentCode })
        setRunState({ phase: 'done', outcome: { kind: 'run', data } })
      } else {
        const { data } = await api.post<CodeSubmission>(`/exam/problems/${problem.id}/submissions`, { language, code: currentCode })
        setRunState({ phase: 'done', outcome: { kind: 'submit', data } })
        history.reload()
        if (data.status === 'ACCEPTED') toast(`Accepted — all ${data.totalTests} tests passed.`, 'success')
        else toast(`Verdict: ${prettyEnum(data.status)} — ${data.passedTests}/${data.totalTests} tests passed.`, 'error')
      }
    } catch (err) {
      const apiProblem = describeError(err)
      setRunState({ phase: 'error', message: apiProblem.message })
      toast(apiProblem.message, 'error')
    }
  }

  const loadSubmission = (submission: CodeSubmission) => {
    setLanguage(submission.language)
    setCode((prev) => ({ ...prev, [submission.language]: submission.code }))
    writeSaved(userId, problem.id, submission.language, submission.code)
    setOpenSubmission(null)
    setCompare(false)
    toast(`Submission #${submission.id} loaded into the editor.`, 'info')
  }

  let result = null as ReturnType<typeof outcomeView> | null
  if (runState.phase === 'done') result = outcomeView(runState.outcome.kind, runState.outcome.data)

  return (
    <>
      <PageHeader
        eyebrow="Code Studio"
        title={problem.title}
        actions={<Btn variant="secondary" onClick={onBack}>All problems</Btn>}
      />
      <div className="studio">
        <section className="card pad studio-problem">
          <Tabs
            label="Problem panels"
            value={panel}
            onChange={(next) => {
              setPanel(next)
              setHistoryPage(0)
            }}
            tabs={[{ id: 'problem', label: 'Problem' }, { id: 'history', label: 'Submissions' }]}
          />
          {panel === 'problem' ? (
            <div className="prose">
              <div className="row badges">
                <Badge tone={DIFFICULTY_TONE[problem.difficulty]}>{problem.difficulty.toLowerCase()}</Badge>
                {problem.topic && <Badge tone="violet">{problem.topic}</Badge>}
                {problem.tags && problem.tags.split(',').map((tag) => <Badge key={tag} tone="neutral">{tag.trim()}</Badge>)}
              </div>
              <p style={{ whiteSpace: 'pre-wrap' }}>{problem.description}</p>
              {problem.inputFormat && <><h3>Input format</h3><p style={{ whiteSpace: 'pre-wrap' }}>{problem.inputFormat}</p></>}
              {problem.outputFormat && <><h3>Output format</h3><p style={{ whiteSpace: 'pre-wrap' }}>{problem.outputFormat}</p></>}
              {problem.constraints && <><h3>Constraints</h3><p style={{ whiteSpace: 'pre-wrap' }}>{problem.constraints}</p></>}
              {samples.status === 'loading' && <Skeleton lines={3} />}
              {examples.length > 0 && <h3>Examples</h3>}
              {examples.map((example, index) => (
                <div className="example" key={example.id}>
                  <span>Input</span><pre>{example.input}</pre>
                  <span>Output</span><pre>{example.expectedOutput}</pre>
                  {example.explanation && <><span>Why</span><p className="example-why">{example.explanation}</p></>}
                  <span className="sr-only">Example {index + 1}</span>
                </div>
              ))}
              <h3>Limits</h3>
              <ul className="plain-list">
                <li>Time limit: {problem.timeLimitMs} ms</li>
                <li>Memory limit: {problem.memoryLimitMb} MB</li>
                <li>Languages: {LANGUAGES.map((l) => l.label).join(', ')}</li>
              </ul>
            </div>
          ) : (
            <>
              <div className="history-filters">
                <SelectField label="Verdict" value={historyStatus} onChange={(e) => { setHistoryStatus(e.target.value); setHistoryPage(0) }}>
                  <option value="">All verdicts</option>
                  <option value="ACCEPTED">Accepted</option>
                  {FAILED_STATUSES.map((status) => <option key={status} value={status}>{prettyEnum(status)}</option>)}
                </SelectField>
                <SelectField label="Language" value={historyLanguage} onChange={(e) => { setHistoryLanguage(e.target.value); setHistoryPage(0) }}>
                  <option value="">All languages</option>
                  {LANGUAGES.map((l) => <option key={l.id} value={l.id}>{l.label}</option>)}
                </SelectField>
              </div>
              <ResourceView
                resource={history}
                endpoint="GET /exam/submissions?problemId=…"
                isEmpty={(d) => d.content.length === 0}
                emptyIcon={HistoryIcon}
                emptyTitle="No submissions yet"
                emptyText="Submit a solution to start building your history for this problem."
              >
                {(d) => (
                  <>
                    <ul className="sub-list">
                      {d.content.map((submission) => (
                        <li key={submission.id}>
                          <button
                            type="button"
                            className="sub-row"
                            onClick={() => { setOpenSubmission(submission.id); setCompare(false) }}
                          >
                            <Badge tone={verdictTone(submission.status)}>{prettyEnum(submission.status)}</Badge>
                            <span>{LANGUAGE_LABEL[submission.language]}</span>
                            <span className="muted">{submission.passedTests}/{submission.totalTests} tests</span>
                            <span className="muted">{submission.executionTimeMs != null ? `${submission.executionTimeMs} ms` : '—'}</span>
                            <time dateTime={submission.submittedAt}>{new Date(submission.submittedAt).toLocaleString()}</time>
                            <span className="muted">#{submission.id}</span>
                          </button>
                        </li>
                      ))}
                    </ul>
                    <Pager page={d.number} totalPages={d.totalPages} onChange={setHistoryPage} />
                  </>
                )}
              </ResourceView>
            </>
          )}
        </section>

        <section className={`card studio-editor ${fullscreen ? 'is-fullscreen' : ''}`} aria-label="Editor and results">
          <div className="editor-bar">
            <div className="editor-controls">
              <SelectField label="Language" value={language} onChange={(e) => setLanguage(e.target.value as CodeLanguage)}>
                {LANGUAGES.map((l) => <option key={l.id} value={l.id}>{l.label}</option>)}
              </SelectField>
              <SelectField label="Font size" value={String(fontSize)} onChange={(e) => setFontSize(Number(e.target.value))}>
                {[12, 13, 14, 15, 16, 18, 20].map((size) => <option key={size} value={size}>{size}px</option>)}
              </SelectField>
            </div>
            <div className="editor-actions">
              <button type="button" className="icon-btn" title="Reset to starter code" aria-label="Reset to starter code" onClick={resetCode} disabled={!starters.data}>
                <RotateCcw size={16} aria-hidden="true" />
              </button>
              <button type="button" className="icon-btn" title="Copy code" aria-label="Copy code" onClick={() => void copyCode()} disabled={!currentCode}>
                <Copy size={16} aria-hidden="true" />
              </button>
              <button
                type="button"
                className="icon-btn"
                title={fullscreen ? 'Exit full screen' : 'Full screen editor'}
                aria-label={fullscreen ? 'Exit full screen' : 'Full screen editor'}
                onClick={() => setFullscreen((value) => !value)}
              >
                {fullscreen ? <Minimize2 size={16} aria-hidden="true" /> : <Maximize2 size={16} aria-hidden="true" />}
              </button>
              <Btn variant="secondary" icon={Play} loading={busy && runState.phase === 'running' && runState.action === 'run'} disabled={busy || !currentCode} onClick={() => void execute('run')}>
                Run
              </Btn>
              <Btn icon={Send} loading={busy && runState.phase === 'running' && runState.action === 'submit'} disabled={busy || !currentCode} onClick={() => void execute('submit')}>
                Submit
              </Btn>
            </div>
          </div>
          {starters.status === 'loading' || !currentCode ? (
            <div className="card pad"><Skeleton lines={6} /></div>
          ) : (
            <MonacoCodeEditor
              value={currentCode}
              language={monacoLanguage}
              fontSize={fontSize}
              theme={theme}
              onChange={updateCode}
              onRun={() => void execute('run')}
              onSubmit={() => void execute('submit')}
            />
          )}
          <p className="editor-hint">Ctrl/Cmd + Enter runs the samples · Ctrl/Cmd + S submits · your code is kept for this problem in this browser.</p>

          <div className="results" ref={detailRef} aria-live="polite">
            <div className="results-head">
              <h3>Results</h3>
              <Tabs
                label="Result views"
                value={resultTab}
                onChange={setResultTab}
                tabs={[
                  { id: 'results', label: 'Test Results' },
                  { id: 'output', label: 'Output' },
                  { id: 'details', label: 'Details' },
                ]}
              />
            </div>
            {runState.phase === 'idle' && <p className="muted">Run executes the sample cases. Submit executes every case, including hidden tests, and saves the submission to your history.</p>}
            {runState.phase === 'running' && (
              <p className="results-waiting"><LoaderCircle className="spin" size={16} aria-hidden="true" /> Running your code in the judge. This usually takes a few seconds.</p>
            )}
            {runState.phase === 'error' && <p className="form-alert form-alert--error">{runState.message}</p>}
            {runState.phase === 'done' && result && (
              <div>
                <div className="verdict-row">
                  <Badge tone={verdictTone(result.status)}>{prettyEnum(result.status)}</Badge>
                  <strong>{result.passed} / {result.total} tests</strong>
                  {result.timeMs != null && <span className="muted"><Clock size={13} aria-hidden="true" /> {result.timeMs} ms</span>}
                  {result.memoryKb != null && <span className="muted"><Gauge size={13} aria-hidden="true" /> {(result.memoryKb / 1024).toFixed(1)} MB</span>}
                  {runState.outcome.kind === 'submit' && (
                    <span className="muted">#{runState.outcome.data.id} · {new Date(runState.outcome.data.submittedAt).toLocaleString()}</span>
                  )}
                </div>
                {result.error && <pre className="output output--error">{result.error}</pre>}
                {resultTab === 'results' && <CaseResults cases={result.cases} />}
                {resultTab === 'output' && (
                  <div className="output-view">
                    {result.cases.filter((c) => c.sample).length === 0 && <p className="muted">No sample output was captured.</p>}
                    {result.cases.filter((c) => c.sample).map((c) => (
                      <div key={c.index}>
                        <h4>Sample {c.index + 1} output</h4>
                        <pre className="output">{c.actualOutput || '(no output)'}</pre>
                        {c.stderr && <pre className="output output--error">{c.stderr}</pre>}
                      </div>
                    ))}
                  </div>
                )}
                {resultTab === 'details' && (
                  <dl className="detail-grid">
                    <dt>Verdict</dt><dd>{prettyEnum(result.status)}</dd>
                    <dt>Tests passed</dt><dd>{result.passed} of {result.total}</dd>
                    <dt>Language</dt><dd>{LANGUAGE_LABEL[language]}</dd>
                    <dt>Execution time</dt><dd>{result.timeMs != null ? `${result.timeMs} ms` : 'Reported by the judge only when available'}</dd>
                    <dt>Memory</dt><dd>{result.memoryKb != null ? `${(result.memoryKb / 1024).toFixed(1)} MB` : 'Reported by the judge only when available'}</dd>
                    <dt>Limits</dt><dd>{problem.timeLimitMs} ms CPU · {problem.memoryLimitMb} MB</dd>
                    {runState.outcome.kind === 'submit' && <><dt>Submission</dt><dd>#{runState.outcome.data.id}</dd><dt>Submitted</dt><dd>{new Date(runState.outcome.data.submittedAt).toLocaleString()}</dd><dt>Judged</dt><dd>{runState.outcome.data.judgedAt ? new Date(runState.outcome.data.judgedAt).toLocaleString() : '—'}</dd></>}
                  </dl>
                )}
              </div>
            )}
          </div>
        </section>
      </div>

      <Modal open={openSubmission !== null} onClose={() => { setOpenSubmission(null); setCompare(false) }} title={`Submission #${openSubmission ?? ''}`}>
        <ResourceView
          resource={detail}
          endpoint={`GET /exam/submissions/${openSubmission ?? ''}`}
          emptyIcon={CodeXml}
          emptyTitle="Submission not found"
        >
          {(submission) => (
            <div className="submission-detail">
              <div className="verdict-row">
                <Badge tone={verdictTone(submission.status)}>{prettyEnum(submission.status)}</Badge>
                <strong>{submission.passedTests}/{submission.totalTests} tests</strong>
                <span>{LANGUAGE_LABEL[submission.language]}</span>
                {submission.executionTimeMs != null && <span className="muted">{submission.executionTimeMs} ms</span>}
                {submission.memoryUsedKb != null && <span className="muted">{(submission.memoryUsedKb / 1024).toFixed(1)} MB</span>}
                <span className="muted">{new Date(submission.submittedAt).toLocaleString()}</span>
              </div>
              <div className="row wrap">
                <Btn variant="secondary" icon={CodeXml} onClick={() => loadSubmission(submission)}>Load into editor</Btn>
                {submission.language === language && (
                  <Btn variant="secondary" onClick={() => setCompare((value) => !value)}>{compare ? 'Hide comparison' : 'Compare with editor'}</Btn>
                )}
              </div>
              {compare && submission.language === language && <CodeDiff before={submission.code} after={currentCode} />}
              {submission.errorMessage && <pre className="output output--error">{submission.errorMessage}</pre>}
              <h3>Submitted code</h3>
              <pre className="output code-view">{submission.code}</pre>
              <h3>Test results</h3>
              <CaseResults cases={submission.cases} />
            </div>
          )}
        </ResourceView>
      </Modal>
    </>
  )
}

/* ---------- Catalogue ---------- */

function Catalogue({ onOpen }: { onOpen: (problem: Problem) => void }) {
  const live = useLive()
  const [search, setSearch] = useState('')
  const [difficulty, setDifficulty] = useState('')
  const [topic, setTopic] = useState('')
  const [status, setStatus] = useState('')
  const [language, setLanguage] = useState('')
  const [page, setPage] = useState(0)
  const query = useDebounced(search)

  const progress = useResource<CodeProgress>(live ? '/exam/progress' : null)
  const list = useResource<Page<Problem>>(live ? '/exam/problems' : null, {
    q: query,
    difficulty,
    topic,
    status,
    language,
    page: String(page),
    size: '15',
  })
  const solved = useMemo(() => new Set(progress.data?.solvedProblemIds ?? []), [progress.data])
  const stats = progress.data

  return (
    <>
      <PageHeader
        eyebrow="Exam Centre"
        title="Code Studio"
        description="Practice original coding problems in Java, Python, C++ and JavaScript. Code runs only in the server's external judge, never in your browser."
      />
      {stats && (
        <>
          <div className="code-stats">
            <div className="card pad code-stat">
              <span className="stat-label">Problems solved</span>
              <strong>{stats.solved} <span className="muted">/ {stats.totalProblems}</span></strong>
              <span className="muted">{stats.attempted} attempted</span>
            </div>
            <div className="card pad code-stat">
              <span className="stat-label">Acceptance rate</span>
              <strong>{stats.acceptanceRate}%</strong>
              <span className="muted">{stats.acceptedSubmissions} of {stats.totalSubmissions} submissions</span>
            </div>
            <div className="card pad code-stat">
              <span className="stat-label">Current streak</span>
              <strong>{stats.currentStreak} <span className="muted">days</span></strong>
              <span className="muted">Longest {stats.longestStreak} days</span>
            </div>
            <div className="card pad code-stat">
              <span className="stat-label">By difficulty</span>
              <strong className="code-diff-counts">
                <span className="diff-easy">{stats.solvedByDifficulty.easy} easy</span>
                <span className="diff-medium">{stats.solvedByDifficulty.medium} medium</span>
                <span className="diff-hard">{stats.solvedByDifficulty.hard} hard</span>
              </strong>
              <span className="muted">Level {stats.level} · {stats.totalXp} XP</span>
            </div>
          </div>
          <div className="card pad topic-progress">
            <div className="row spread">
              <h3>Topic progress</h3>
              <span className="muted"><Target size={14} aria-hidden="true" /> {stats.solved} solved across {stats.topics.length} topics</span>
            </div>
            <ul className="topic-grid">
              {stats.topics.map((item) => (
                <li key={item.topic}>
                  <div className="row spread">
                    <span>{item.topic}</span>
                    <span className="muted">{item.solved}/{item.total}</span>
                  </div>
                  <ProgressBar value={item.solved} max={item.total} label={`${item.topic}: ${item.solved} of ${item.total} solved`} tone={item.solved === item.total && item.total > 0 ? 'green' : 'violet'} />
                </li>
              ))}
            </ul>
          </div>
          {stats.recentSubmissions.length > 0 && (
            <div className="card pad recent-submissions">
              <h3>Recent submissions</h3>
              <ul className="sub-list">
                {stats.recentSubmissions.map((submission) => (
                  <li key={submission.id}>
                    <div className="sub-row sub-row--static">
                      <Badge tone={verdictTone(submission.status)}>{prettyEnum(submission.status)}</Badge>
                      <span className="sub-problem">{submission.problemTitle}</span>
                      <span>{LANGUAGE_LABEL[submission.language]}</span>
                      <span className="muted">{submission.passedTests}/{submission.totalTests} tests</span>
                      <time dateTime={submission.submittedAt}>{new Date(submission.submittedAt).toLocaleString()}</time>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}

      <div className="filters">
        <div className="field search-field">
          <label htmlFor="code-search">Search problems</label>
          <div className="input-icon">
            <Search size={15} aria-hidden="true" />
            <input id="code-search" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0) }} placeholder="Title or tag" />
          </div>
        </div>
        <SelectField label="Difficulty" value={difficulty} onChange={(e) => { setDifficulty(e.target.value); setPage(0) }}>
          <option value="">All</option>
          <option value="EASY">Easy</option>
          <option value="MEDIUM">Medium</option>
          <option value="HARD">Hard</option>
        </SelectField>
        <SelectField label="Topic" value={topic} onChange={(e) => { setTopic(e.target.value); setPage(0) }}>
          <option value="">All topics</option>
          {TOPICS.map((item) => <option key={item} value={item}>{item}</option>)}
        </SelectField>
        <SelectField label="Status" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
          <option value="">Any status</option>
          <option value="SOLVED">Solved</option>
          <option value="ATTEMPTED">Attempted</option>
          <option value="UNSOLVED">Not attempted</option>
        </SelectField>
        <SelectField label="Attempted in" value={language} onChange={(e) => { setLanguage(e.target.value); setPage(0) }}>
          <option value="">Any language</option>
          {LANGUAGES.map((l) => <option key={l.id} value={l.id}>{l.label}</option>)}
        </SelectField>
      </div>

      <ResourceView
        resource={list}
        endpoint="GET /exam/problems"
        isEmpty={(d) => d.content.length === 0}
        emptyIcon={CodeXml}
        emptyTitle="No problems match"
        emptyText="No published problem matches these filters."
        idleHint="Sign in to load the problem catalogue."
      >
        {(d) => (
          <>
            {d.content.length === 0 ? (
              <p className="card pad muted">No problem on this page matches your filters.</p>
            ) : (
              <ul className="problem-list">
                {d.content.map((problem) => (
                  <li key={problem.id}>
                    <button type="button" className="card problem-row" onClick={() => onOpen(problem)}>
                      <span className="problem-row-main">
                        <span className="problem-title">
                          {solved.has(problem.id) && <Check size={15} className="solved-mark" aria-label="Solved" />}
                          {problem.title}
                        </span>
                        {problem.tags && <span className="muted problem-tags">{problem.tags.split(',').join(' · ')}</span>}
                      </span>
                      <span className="row">
                        {solved.has(problem.id) && <Badge tone="green">Solved</Badge>}
                        {problem.topic && <Badge tone="violet">{problem.topic}</Badge>}
                        <Badge tone={DIFFICULTY_TONE[problem.difficulty]}>{problem.difficulty.toLowerCase()}</Badge>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <Pager page={d.number} totalPages={d.totalPages} onChange={setPage} />
          </>
        )}
      </ResourceView>
      {!live && <EmptyState icon={CodeXml} title="Sign in to practise">Run and submit buttons execute code in the configured judge and save results to your account.</EmptyState>}
    </>
  )
}

export default function CodeStudioPage() {
  const [selected, setSelected] = useState<Problem | null>(null)
  if (selected) return <Workspace problem={selected} onBack={() => setSelected(null)} />
  return <Catalogue onOpen={setSelected} />
}
