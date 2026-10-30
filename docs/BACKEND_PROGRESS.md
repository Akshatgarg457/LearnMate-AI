# LearnMate AI — Backend Progress

Last updated: 2026-10-24

---

## Stage 0 — Audit ✅
## Stage 1 — Foundation ✅
## Stage 2 — Authentication and User Management ✅

## Stage 3 — Exam Centre ✅
- Problem catalogue, difficulty/topic filters, pagination
- TestCase entity (sample vs hidden — hidden never sent to client)
- CodeSubmission entity with full status enum
- JudgeProvider interface + Judge0Provider adapter + UnconfiguredJudgeProvider fallback
- Server never executes code locally
- QuizService: start attempt, save answer, server-side scoring, expiry enforcement
- MockExamService: timed attempt, answer save, server-side scoring, topic performance

## Stage 4 — Peer Learning and Reward Ledger ✅
- PeerSession state machine: REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED/DISPUTED/CANCELLED
- Dual confirmation required from both learner and teacher
- Teaching reward: 10 pts/min × min(actual, maxRewardable) — only if ≥5 min AND learner rating provided
- Reward idempotency key prevents duplicate awards
- RewardService: credit/debit with idempotencyKey, balance enforcement (no negative)

## Stage 5 — Progress Centre ✅
- LearningActivity: one record per (userId, date, activityType) — server-created only
- UserProgress: cached streak, longest streak, XP, level — updated transactionally
- Streak milestone rewards at 7, 14, 30, 60, 100, 365 days with dedup
- XP = totalXp, Level = 1 + totalXp/100
- Heatmap endpoint: daily XP aggregation by date range
- Achievements and UserAchievements entities ready

## Stage 6 — Course Rewards Store ✅
- Course catalogue (INTERNAL/PARTNER), category/type filters
- Atomic redemption: eligibility check → balance check → debit → entitlement in one transaction
- Duplicate redemption prevented by unique constraint + check

## Stage 7 — AI Tutor, Notes, Community ✅
- AiProvider interface + AnthropicAiProvider + UnconfiguredAiProvider fallback
- AiConversation/AiMessage persistence; full conversation history sent to provider
- NotesService: MIME validation, size limit, random storage path (path traversal prevention)
- Download authorization (owner or public)
- CommunityService: questions, answers, voting, accept answer, content reports

## Stage 8 — Admin ✅
- AdminController: all endpoints guarded by @PreAuthorize("hasRole('ADMIN')")
- User management: list, update status, update role
- Course management: create, update, toggle active
- Quiz/MockExam/Problem: toggle active
- Reports/disputes: list, review, resolve
- Reward adjustments with admin idempotency key

## Stage 9 — Tests ✅ (written; pending local Maven run)
- AuthServiceTest: register, login, duplicate, wrong password, email normalization
- RewardServiceTest: credit, idempotency, debit, insufficient balance
- QuizServiceTest: start, timed/untimed, server-side scoring, duplicate/double-submit prevention
- PeerSessionServiceTest: full lifecycle, teacher-cannot-accept-own, accept twice, dispute
- ProgressServiceTest: XP tracking, idempotency, different types same day, level calculation

---

## Files Created (112 total)

### Entities (26)
User, Problem, TestCase, CodeSubmission, Quiz, QuizQuestion, QuizOption, QuizAttempt, QuizAnswer,
MockExam, MockExamQuestion, MockExamOption, MockExamAttempt, MockExamAnswer, PeerSession,
RewardTransaction, LearningActivity, UserProgress, Achievement, UserAchievement,
Course, CourseRedemption, AiConversation, AiMessage, Note, CommunityQuestion, CommunityAnswer, ContentReport

### Repositories (21)
UserRepository, ProblemRepository, CodeSubmissionRepository, QuizRepository, QuizAttemptRepository,
QuizAnswerRepository, MockExamRepository, MockExamAttemptRepository, MockExamAnswerRepository,
PeerSessionRepository, RewardTransactionRepository, LearningActivityRepository,
UserProgressRepository, AchievementRepository, UserAchievementRepository,
CourseRepository, CourseRedemptionRepository, AiConversationRepository, AiMessageRepository,
NoteRepository, CommunityQuestionRepository, CommunityAnswerRepository, ContentReportRepository

### Services (12)
AuthService, UserService, UserDetailsServiceImpl, CodeStudioService, QuizService, MockExamService,
PeerSessionService, RewardService, ProgressService, CourseService, AiTutorService,
NotesService, CommunityService, AdminService, DashboardService

### Controllers (11)
AuthController, UserController, CodeStudioController, QuizController, MockExamController,
PeerSessionController, RewardController, ProgressController, CourseController,
AiTutorController, NotesController, CommunityController, AdminController, DashboardController

### Security / Infrastructure (5)
JwtUtil, JwtAuthFilter, SecurityConfig, GlobalExceptionHandler, ApiResponse, ApiException

### Providers (4)
JudgeProvider, Judge0Provider, UnconfiguredJudgeProvider (fallback), AiProvider, AnthropicAiProvider, UnconfiguredAiProvider (fallback)

---

## Build Status
| Check | Status | Notes |
|---|---|---|
| Maven dependency resolution | ⚠️ Not run | Network unavailable in this env — run locally |
| Compilation | ⚠️ Not run | All source written; run ./mvnw clean package |
| Unit tests (H2) | ⚠️ Not run | ./mvnw test -Dspring.profiles.active=test |
| MySQL integration | ⚠️ Not run | Requires local MySQL instance |

---

## External Services Still Required
| Service | Status |
|---|---|
| MySQL | Env vars: DB_URL, DB_USERNAME, DB_PASSWORD |
| Code judge (Judge0) | Env vars: JUDGE_BASE_URL, JUDGE_API_KEY |
| AI provider (Anthropic) | Env vars: AI_API_KEY, AI_MODEL |
| File storage | Env var: UPLOAD_DIR (local path or mount) |

---

## Known Limitations / Next Steps
1. JWT revocation not implemented (stateless — tokens valid until expiry)
2. Rate limiting on auth endpoints not implemented
3. Refresh token rotation endpoint not implemented
4. Admin course creation accepts entity directly — should use request DTO in production
5. File storage is local filesystem only — needs S3 adapter for production scale
6. No pagination on AiMessages (all messages returned — may be large for long conversations)
7. `@ConditionalOnProperty` for Judge0Provider activates on ANY non-missing value of base-url including "NOT_CONFIGURED" — replace with `@ConditionalOnExpression` if needed
