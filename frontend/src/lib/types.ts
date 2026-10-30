// Shapes returned by the LearnMate backend (Spring Boot). Field names match the real JSON.
// The backend wraps every response in { success, data, timestamp }; services/api.ts unwraps it.

export type Role = 'STUDENT' | 'ADMIN'

/** What the app keeps about the signed-in person. */
export type User = { id: string; email: string; displayName: string; role: Role }

export type AuthResponse = { accessToken: string; tokenType: string; expiresIn: number; userId: number; email: string; displayName: string; role: Role }

export type UserProfile = { id: number; email: string; displayName: string; role: Role; status: 'ACTIVE' | 'SUSPENDED' | 'DELETED'; bio?: string | null; avatarUrl?: string | null; timezone?: string | null; createdAt?: string }

/** Spring Data page. `number` is the zero-based page index. */
export type Page<T> = { content: T[]; number: number; totalPages: number; totalElements: number; size: number }

/** Other people appear only as id + display name. */
export type Person = { id: number; displayName: string }

export type DashboardSummary = {
  currentStreak: number
  longestStreak: number
  totalXp: number
  level: number
  rewardBalance: number
  totalCodeSubmissions: number
  quizzesCompleted: number
  mockExamsCompleted: number
  activeDaysLast30: number
}

export type UserProgress = { userId: number; currentStreak: number; longestStreak: number; lastActivityDate?: string | null; totalXp: number; level: number; nextLevelXp: number }

export type LearningActivity = { id: number; activityType: string; activityDate: string; xpAwarded: number; referenceId?: number | null; createdAt: string }

export type ActivityDay = { date: string; xp: number }

export type Achievement = { id: number; code: string; title: string; description: string; iconUrl?: string | null; xpReward: number; pointsReward: number }
export type UserAchievement = { id: number; achievement: Achievement; earnedAt: string }

export type RewardTransaction = { id: number; amount: number; type: string; description: string; referenceId?: number | null; createdAt: string }

export type Course = { id: number; title: string; description?: string | null; category?: string | null; courseType: 'INTERNAL' | 'PARTNER'; pointsPrice: number; thumbnailUrl?: string | null; providerName?: string | null; active: boolean; createdAt?: string }
export type CourseRedemption = { id: number; course: Course; pointsSpent: number; status: 'ACTIVE' | 'REVOKED'; redeemedAt: string }

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type Problem = { id: number; slug?: string | null; title: string; description: string; difficulty: Difficulty; topic?: string | null; tags?: string | null; inputFormat?: string | null; outputFormat?: string | null; constraints?: string | null; timeLimitMs: number; memoryLimitMb: number; active?: boolean }
export type TestCase = { id: number; input: string; expectedOutput: string; sample: boolean; explanation?: string | null; orderIndex: number }
export type CodeLanguage = 'JAVA' | 'PYTHON' | 'CPP' | 'JAVASCRIPT'
export type SubmissionStatus = 'PENDING' | 'PROCESSING' | 'ACCEPTED' | 'WRONG_ANSWER' | 'TIME_LIMIT_EXCEEDED' | 'MEMORY_LIMIT_EXCEEDED' | 'COMPILATION_ERROR' | 'RUNTIME_ERROR' | 'JUDGE_ERROR'
export type SubmissionCase = { index: number; sample: boolean; passed: boolean; status: SubmissionStatus; executionTimeMs?: number | null; memoryUsedKb?: number | null; input?: string | null; expectedOutput?: string | null; actualOutput?: string | null; stderr?: string | null }
export type CodeSubmission = { id: number; problemId: number; problemTitle: string; language: CodeLanguage; code: string; status: SubmissionStatus; passedTests: number; totalTests: number; executionTimeMs?: number | null; memoryUsedKb?: number | null; errorMessage?: string | null; submittedAt: string; judgedAt?: string | null; cases: SubmissionCase[] }
export type SubmissionSummary = Omit<CodeSubmission, 'code' | 'cases' | 'errorMessage'>
export type RunResult = { status: SubmissionStatus; passed: number; total: number; executionTimeMs?: number | null; memoryUsedKb?: number | null; compileOutput?: string | null; cases: SubmissionCase[] }
export type CodeProgress = {
  totalProblems: number
  attempted: number
  solved: number
  solvedByDifficulty: { easy: number; medium: number; hard: number }
  acceptanceRate: number
  totalSubmissions: number
  acceptedSubmissions: number
  currentStreak: number
  longestStreak: number
  totalXp: number
  level: number
  topics: { topic: string; total: number; solved: number }[]
  solvedProblemIds: number[]
  recentSubmissions: SubmissionSummary[]
}

// Quiz and mock-exam answer keys and explanations are never sent by the backend.
export type ExamOption = { id: number; text: string; optionIndex: number }
export type ExamQuestion = { id: number; stem: string; orderIndex: number; marks: number; topic?: string | null; options: ExamOption[] }
export type Quiz = { id: number; title: string; description?: string | null; subject?: string | null; topic?: string | null; durationMinutes: number; active?: boolean; questions: ExamQuestion[] }
export type MockExam = { id: number; title: string; instructions?: string | null; subject?: string | null; durationMinutes: number; passPercentage: number; active?: boolean; questions: ExamQuestion[] }
export type AttemptAnswer = { id: number; question: { id: number }; selectedOption?: { id: number } | null; correct?: boolean | null; marksAwarded?: number | null }
export type AttemptStatus = 'IN_PROGRESS' | 'SUBMITTED' | 'EXPIRED' | 'TIMED_OUT'
export type QuizAttempt = { id: number; quiz: Quiz; status: AttemptStatus; deadlineAt?: string | null; startedAt: string; submittedAt?: string | null; score?: number | null; maxScore?: number | null; answers: AttemptAnswer[] }
export type MockExamAttempt = { id: number; exam: MockExam; status: AttemptStatus; deadlineAt: string; startedAt: string; submittedAt?: string | null; score?: number | null; maxScore?: number | null; passPercentage?: number | null; passed?: boolean | null; answers: AttemptAnswer[] }

export type PeerStatus = 'REQUESTED' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'DISPUTED' | 'CANCELLED'
export type MeetingStatus = 'NOT_CREATED' | 'CREATED' | 'FAILED'
export type PeerSession = {
  id: number
  learner: Person
  teacher?: Person | null
  topic: string
  description?: string | null
  requestedMinutes: number
  status: PeerStatus
  startedAt?: string | null
  endedAt?: string | null
  actualMinutes?: number | null
  learnerConfirmed: boolean
  teacherConfirmed: boolean
  learnerRating?: number | null
  rewardIssued: boolean
  meetingStatus: MeetingStatus
  meetingUri?: string | null
  meetingSpaceName?: string | null
  meetingCreatedAt?: string | null
  meetingError?: string | null
  createdAt: string
}

/** Whether the server has Google OAuth configured and this user connected it. */
export type GoogleStatus = { configured: boolean; connected: boolean }

export type Note = { id: number; owner: Person; title: string; description?: string | null; subject?: string | null; topic?: string | null; originalFilename: string; mimeType: string; fileSizeBytes: number; public: boolean; downloadCount: number; createdAt: string }

export type CommunityQuestion = { id: number; author: Person; title: string; body: string; subject?: string | null; topic?: string | null; status: 'OPEN' | 'CLOSED' | 'REMOVED'; voteCount: number; viewCount: number; answerCount: number; createdAt: string }
export type CommunityAnswer = { id: number; author: Person; body: string; voteCount: number; accepted: boolean; status: 'VISIBLE' | 'REMOVED'; createdAt: string }
export type ContentReport = { id: number; reporter: Person; contentType: 'QUESTION' | 'ANSWER' | 'PEER_SESSION'; contentId: number; reason: string; details?: string | null; status: 'PENDING' | 'REVIEWED' | 'DISMISSED' | 'ACTIONED'; createdAt: string }

export type AiConversation = { id: number; title: string; subject?: string | null; createdAt: string; updatedAt: string; lastMessage?: string | null; lastMessageRole?: 'USER' | 'ASSISTANT' | null }
export type AiMessage = { id: number; role: 'USER' | 'ASSISTANT'; content: string; createdAt: string }
