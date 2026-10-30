// SAMPLE CONTENT for demonstrating the exam interface only.
// It is not real exam data and results from it are never official.
import type { RunnerQuestion, RunnerResult } from '../components/ExamRunner'

type SampleQuestion = { id: string; topic: string; prompt: string; options: string[]; correct: number; explanation: string }

const questions: SampleQuestion[] = [
  { id: 's1', topic: 'Java', prompt: 'Which keyword stops a Java class from being subclassed?', options: ['abstract', 'final', 'static', 'private'], correct: 1, explanation: 'A class declared final cannot be extended.' },
  { id: 's2', topic: 'Python', prompt: 'What does len([1, 2, [3, 4]]) return in Python?', options: ['2', '3', '4', 'It raises an error'], correct: 1, explanation: 'The list has three elements; the nested list counts as one element.' },
  { id: 's3', topic: 'DSA', prompt: 'What is the time complexity of binary search on a sorted array of n items?', options: ['O(n)', 'O(n log n)', 'O(log n)', 'O(1)'], correct: 2, explanation: 'Each step halves the search range, so it takes about log2(n) steps.' },
  { id: 's4', topic: 'DSA', prompt: 'Which data structure follows last-in, first-out (LIFO) order?', options: ['Queue', 'Stack', 'Heap', 'Linked list'], correct: 1, explanation: 'A stack removes the most recently added item first.' },
  { id: 's5', topic: 'DBMS', prompt: 'Which SQL clause filters groups after GROUP BY has been applied?', options: ['WHERE', 'ORDER BY', 'HAVING', 'LIMIT'], correct: 2, explanation: 'WHERE filters rows before grouping; HAVING filters the resulting groups.' },
  { id: 's6', topic: 'C++', prompt: 'Which operator allocates memory dynamically in C++?', options: ['malloc_new', 'alloc', 'new', 'create'], correct: 2, explanation: 'The new operator allocates memory and constructs the object.' },
  { id: 's7', topic: 'DSA', prompt: 'What is the worst-case time complexity of quicksort?', options: ['O(n log n)', 'O(n)', 'O(log n)', 'O(n\u00b2)'], correct: 3, explanation: 'With consistently bad pivots (for example an already sorted array and a naive pivot) it degrades to O(n\u00b2).' },
  { id: 's8', topic: 'Web', prompt: 'Which HTTP status code means the requested resource was not found?', options: ['200', '301', '404', '500'], correct: 2, explanation: '404 Not Found means the server cannot find the resource.' },
]

export const sampleRunnerQuestions: RunnerQuestion[] = questions.map((q) => ({ id: q.id, topic: q.topic, prompt: q.prompt, options: q.options.map((text, i) => ({ id: String(i), text })) }))

/** Scores the sample in the browser. This is a demo only; real attempts are always scored by the server. */
export function scoreSample(answers: Record<string, string>): RunnerResult {
  const perQuestion: RunnerResult['perQuestion'] = {}
  const topics = new Map<string, { earned: number; max: number }>()
  let score = 0
  for (const q of questions) {
    const picked = answers[q.id]
    const correct = picked === undefined ? null : Number(picked) === q.correct
    if (correct) score += 1
    perQuestion[q.id] = { correct, correctText: q.options[q.correct], explanation: q.explanation }
    const t = topics.get(q.topic) ?? { earned: 0, max: 0 }
    t.max += 1
    if (correct) t.earned += 1
    topics.set(q.topic, t)
  }
  return { score, maxScore: questions.length, perQuestion, topics: [...topics.entries()].map(([name, v]) => ({ name, ...v })), note: 'Sample mode: scored in your browser for demonstration. This is not an official result and is not saved.' }
}
