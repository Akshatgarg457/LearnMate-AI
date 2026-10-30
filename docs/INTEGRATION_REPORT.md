# Frontend and backend integration report

_24 Oct 2026._ The backend came from `learnmate-backend-complete.zip`; its bundled `frontend/` was an early version and was **not** used. The current (redesigned) frontend was kept and rewired to the real backend.

## What was done
1. Read the backend source (controllers, services, entities, security) and its docs.
2. Rewrote the frontend data layer to the real contract: response envelope, Spring page shape, auth fields, field names, paths, HTTP methods, request bodies and query parameters, upload format, error format.
3. Reworked every screen that had been built against a guessed API (details in `API_INTEGRATION_GAPS.md`).
4. Fixed backend defects that would have broken or endangered the integration (below). **These are the only backend changes**; 33 files, all annotation, configuration or small additions.

## Backend defects found and fixed
| # | Problem found | Why it mattered | Fix |
|---|---|---|---|
| 1 | Controllers return JPA entities directly, with **no Jackson annotations anywhere** | Parent/child links point at each other (`Quiz.questions` and `QuizQuestion.quiz`), which recurses forever; lazy `User` links either crash with a 500 or serialize the whole user, **including `passwordHash` and email of other users** | `@JsonIgnore` on the user's own `user` links and every child-to-parent back-reference; other people (author, owner, learner, teacher, reporter) expose only `id` and `displayName`; `User.passwordHash` never serialized; `hibernateLazyInitializer` hidden on proxied classes |
| 2 | Quiz and mock-exam JSON contained each option's `correct` flag and each question's `explanation` | Any student could read the answers before answering | Both are write-only (accepted on input, never sent) |
| 3 | `Problem.testCases` serialized | Would reveal hidden test cases | Write-only; sample cases still come from `/test-cases` |
| 4 | `spring.jpa.open-in-view=false` while entities are serialized lazily | Lazy relations fail after the service transaction ends | Set to `true` (documented trade-off: a database connection is held for the request) |
| 5 | Unauthenticated requests got an empty **403**, not the documented 401 | The SPA could not tell "session expired" from "not allowed" | JSON 401 entry point and 403 handler in `SecurityConfig` |
| 6 | A valid token for a suspended or deleted user threw inside the JWT filter | A 500 on every request instead of a clean 401 | `JwtAuthFilter` now leaves the request unauthenticated |
| 7 | `GET /notes/**` was public, and `GET /notes/{id}` had no owner check | Anyone could read private note metadata | Notes require sign-in; `getVisibleNote` allows only public notes or the owner |
| 8 | `Note.storagePath`, `CodeSubmission.judgeToken`, `RewardTransaction.idempotencyKey` serialized | Internal values exposed | `@JsonIgnore` |
| 9 | Community feed had no answer count (answers list is hidden) | The feed could not show it | `CommunityQuestion.getAnswerCount()` |

Changed files: `ai/entity` (AiConversation, AiMessage), `common/security` (JwtAuthFilter, SecurityConfig), `community/entity` (CommunityAnswer, CommunityQuestion, ContentReport), `course/entity` (Course, CourseRedemption), `exam/entity` (CodeSubmission, MockExam, MockExamAnswer, MockExamAttempt, MockExamOption, MockExamQuestion, Problem, Quiz, QuizAnswer, QuizAttempt, QuizOption, QuizQuestion, TestCase), `notes` (NotesController, NotesService, Note), `peer/entity/PeerSession`, `progress/entity` (Achievement, LearningActivity, UserAchievement, UserProgress), `reward/entity/RewardTransaction`, `user/entity/User`, `resources/application.properties`.

## Backend problems found and NOT fixed (decide before relying on them)
- **The judge is a stub.** `CodeStudioService` submits each program once with empty input and no expected output, so any program that runs without crashing is "Accepted", earns XP, and counts toward streaks. Fix: run each test case with its `input` and `expectedOutput` (Judge0 supports both) and aggregate. The UI warns about this on every result.
- **Credit race on redemption.** The balance is checked and then debited without locking the user's ledger, so two simultaneous redemptions could overspend. Add row locking or a conditional update.
- **Suspended users logging in** get "Invalid email or password" (401), not the documented 403, because the user lookup hides them.
- **Peer dispute `reason` is accepted but never stored**; resolving a dispute does not issue the teaching reward.
- **No audit trail**, no way to resolve reports, no list of all reward transactions, no admin overview or analytics endpoints.
- **Default JWT secret** is a public placeholder long enough to pass the length check. Always set `JWT_SECRET`. Consider failing startup when it is unset.
- `POST /admin/courses` and `/admin/problems` bind request bodies directly to entities (mass assignment). Admin-only, but worth a DTO.
- `docs/SECURITY_NOTES.md` says login returns 403 for suspended accounts and that hidden test cases are never sent; the first was inaccurate (see above) and the second only became true with fix #3.

## Verification
**Done (in a Linux sandbox without Maven, MySQL or a browser):**
- Frontend `tsc -b --force`: 0 errors.
- All 67 distinct frontend HTTP calls match a backend controller route and method.
- All 27 frontend response types match the JSON fields the backend entities will emit (after fixes 1 to 3), including write-only and ignored fields.
- All 112 backend Java files parse with the JDK compiler (syntax only; types and symbols were **not** resolved).
- API client tested against a server speaking the backend's exact envelope: unwrapping, token header, server-clock skew, 401 expiry vs 403, 409, 503 and network errors, 204, and file downloads.
- Role, route-guard and render tests: 59 checks (student and admin navigation, dashboards, breadcrumbs, redirects, every student and admin page renders).
- CSS: parses, no undefined variables, contrast meets AA in both themes.

**Not done:**
- `.\mvnw.cmd test` and `spring-boot:run`: the backend has never been compiled or started by me. The Jackson edits are annotation-only, but Spring Boot 4 uses Jackson 3; the annotations live in the shared `com.fasterxml.jackson.annotation` package, which the existing code already relied on. If compilation fails, the likely cause is an import in one of the 33 files.
- Any real database, upload, judge, AI or admin flow end to end.
- `npm run lint`, `npm run build`, and any browser check (layout, animation, redirects).
- A load or security test.

## If something fails
- **Maven compile error** in an edited entity: open that file, check the `import com.fasterxml.jackson.annotation...` lines at the top.
- **401 on every request after sign-in:** the token or `JWT_SECRET` changed between runs; sign in again.
- **Browser says "Cannot reach the LearnMate server":** backend not running, or `CORS_ALLOWED_ORIGINS` does not include the page's origin.
- **403 on admin pages:** the account is still a student; run the `UPDATE users SET role='ADMIN'` statement and sign in again.
