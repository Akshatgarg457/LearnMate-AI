import type { RunnerQuestion, RunnerResult } from '../components/ExamRunner'
import type { AttemptAnswer, ExamQuestion } from './types'

/** Backend questions to the runner's shape, in the order the backend defines. */
export function toRunnerQuestions(qs: ExamQuestion[]): RunnerQuestion[] {
  return [...qs]
    .sort((a, b) => a.orderIndex - b.orderIndex)
    .map((q) => ({ id: String(q.id), topic: q.topic, prompt: q.stem, options: [...q.options].sort((a, b) => a.optionIndex - b.optionIndex).map((o) => ({ id: String(o.id), text: o.text })) }))
}

/** Answers the server has already saved (used when resuming an attempt). */
export function savedAnswers(answers: AttemptAnswer[]): Record<string, string> {
  const out: Record<string, string> = {}
  for (const a of answers) if (a.selectedOption) out[String(a.question.id)] = String(a.selectedOption.id)
  return out
}

/** The server's grading, shaped for the results screen. The backend does not send answer keys or explanations. */
export function resultFromAttempt(questions: RunnerQuestion[], attempt: { answers: AttemptAnswer[]; score?: number | null; maxScore?: number | null; passed?: boolean | null; status: string }, topics?: RunnerResult['topics']): RunnerResult {
  const perQuestion: RunnerResult['perQuestion'] = {}
  for (const q of questions) {
    const ans = attempt.answers.find((a) => String(a.question.id) === q.id)
    perQuestion[q.id] = { correct: ans ? ans.correct ?? null : null }
  }
  const late = attempt.status === 'TIMED_OUT'
  return {
    score: attempt.score ?? 0,
    maxScore: attempt.maxScore ?? 0,
    passed: attempt.passed,
    perQuestion,
    topics,
    note: `Scored by the server${late ? ' (submitted after the deadline)' : ''}. The backend does not send correct answers or explanations yet, so the review shows right or wrong only.`,
  }
}

export const errorMessage = (e: unknown, describe: (e: unknown) => { message: string }) => new Error(describe(e).message)
