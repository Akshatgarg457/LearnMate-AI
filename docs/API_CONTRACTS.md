# LearnMate AI — API Contracts

**Base URL:** `http://localhost:8080/api/v1`
**Auth header:** `Authorization: Bearer <accessToken>`
**Content-Type:** `application/json` (unless multipart)

## Response Envelope
All endpoints return:
```json
{ "success": true, "data": { ... }, "timestamp": "2026-10-24T..." }
{ "success": false, "error": "Human-readable message", "timestamp": "..." }
```

## Pagination
Page-based endpoints accept `?page=0&size=20` and return Spring `Page<T>`:
```json
{ "content": [...], "totalElements": 100, "totalPages": 5, "number": 0, "size": 20 }
```

---

## Auth  ✅ IMPLEMENTED

### POST /api/v1/auth/register
- Auth: None
- Request: `{ "displayName": "string(2-100)", "email": "string", "password": "string(8-128)" }`
- Response 201: AuthResponse
- Errors: 400 validation, 409 email taken

### POST /api/v1/auth/login
- Auth: None
- Request: `{ "email": "string", "password": "string" }`
- Response 200: AuthResponse
- Errors: 400, 401 bad credentials, 403 suspended

**AuthResponse:**
```json
{ "accessToken": "...", "tokenType": "Bearer", "expiresIn": 86400000,
  "userId": 1, "email": "...", "displayName": "...", "role": "STUDENT" }
```

---

## Users  ✅ IMPLEMENTED

### GET /api/v1/users/me
- Auth: Required
- Response 200: UserResponse

### PATCH /api/v1/users/me
- Auth: Required
- Request: `{ "displayName"?: "string", "bio"?: "string", "timezone"?: "string" }`
- Response 200: UserResponse

### POST /api/v1/users/me/password
- Auth: Required
- Request: `{ "currentPassword": "string", "newPassword": "string(8-128)" }`
- Response 204

**UserResponse:**
```json
{ "id": 1, "email": "...", "displayName": "...", "role": "STUDENT",
  "status": "ACTIVE", "bio": null, "avatarUrl": null, "timezone": "UTC", "createdAt": "..." }
```

---

## Dashboard  ✅ IMPLEMENTED

### GET /api/v1/dashboard
- Auth: Required
- Response 200:
```json
{ "currentStreak": 5, "longestStreak": 14, "totalXp": 320, "level": 4,
  "rewardBalance": 150, "totalCodeSubmissions": 12, "quizzesCompleted": 8,
  "mockExamsCompleted": 2, "activeDaysLast30": 11 }
```

---

## Exam Centre — Code Studio  ✅ IMPLEMENTED

### GET /api/v1/exam/problems
- Auth: Required
- Query: `?difficulty=EASY|MEDIUM|HARD&topic=string&page=0&size=20`
- Response 200: Page\<Problem\>

### GET /api/v1/exam/problems/{id}
- Auth: Required
- Response 200: Problem (without hidden test cases)
- Errors: 404

### GET /api/v1/exam/problems/{id}/test-cases
- Auth: Required
- Response 200: List\<TestCase\> (sample only — hidden never returned)

### POST /api/v1/exam/problems/{id}/submissions
- Auth: Required
- Request: `{ "language": "JAVA|PYTHON|CPP", "code": "string" }`
- Response 202: `{ "id": 1, "status": "PROCESSING", "judgeToken": "..." }`
- Errors: 400 invalid lang, 503 judge not configured

### GET /api/v1/exam/submissions/{submissionId}
- Auth: Required (owner only)
- Response 200: CodeSubmission (polls judge if still PROCESSING)
- Status values: PENDING, PROCESSING, ACCEPTED, WRONG_ANSWER, TIME_LIMIT_EXCEEDED,
  MEMORY_LIMIT_EXCEEDED, COMPILATION_ERROR, RUNTIME_ERROR, JUDGE_ERROR

### GET /api/v1/exam/submissions
- Auth: Required
- Query: `?problemId=1&page=0&size=20`
- Response 200: Page\<CodeSubmission\>

---

## Exam Centre — Quick Revision  ✅ IMPLEMENTED

### GET /api/v1/exam/quizzes
- Auth: Required
- Query: `?subject=string&page=0&size=20`
- Response 200: Page\<Quiz\>

### GET /api/v1/exam/quizzes/{id}
- Auth: Required
- Response 200: Quiz (with questions and options; correct flags hidden in production — expose only after submission)

### POST /api/v1/exam/quizzes/{id}/attempts
- Auth: Required
- Request: `{ "untimed": false }` (optional body)
- Response 201: QuizAttempt with deadlineAt set if timed
- Errors: 409 active attempt exists

### PUT /api/v1/exam/attempts/{attemptId}/answers
- Auth: Required (owner)
- Request: `{ "questionId": 1, "optionId": 2 }` (optionId null = clear)
- Response 200: QuizAnswer
- Errors: 409 attempt not in progress, 422 time expired

### POST /api/v1/exam/attempts/{attemptId}/submit
- Auth: Required (owner)
- Response 200: QuizAttempt with score (server-scored — client score never trusted)
- Errors: 409 already submitted

### GET /api/v1/exam/attempts/{attemptId}
- Auth: Required (owner)
- Response 200: QuizAttempt

### GET /api/v1/exam/attempts
- Auth: Required
- Query: `?page=0&size=20`
- Response 200: Page\<QuizAttempt\>

---

## Exam Centre — Mock Exams  ✅ IMPLEMENTED

### GET /api/v1/exam/mock-exams
- Auth: Required
- Query: `?subject=string&page=0&size=20`

### GET /api/v1/exam/mock-exams/{id}
- Auth: Required
- Response 200: MockExam

### POST /api/v1/exam/mock-exams/{id}/attempts
- Auth: Required
- Response 201: MockExamAttempt with deadlineAt
- Errors: 409 active attempt exists

### PUT /api/v1/exam/mock-attempts/{attemptId}/answers
- Auth: Required (owner)
- Request: `{ "questionId": 1, "optionId": 2 }`
- Response 200: MockExamAnswer
- Errors: 409, 422 time expired

### POST /api/v1/exam/mock-attempts/{attemptId}/submit
- Auth: Required (owner)
- Response 200: MockExamAttempt with score, maxScore, passed (server-scored)

### GET /api/v1/exam/mock-attempts/{attemptId}
- Auth: Required (owner)

### GET /api/v1/exam/mock-attempts/{attemptId}/performance
- Auth: Required (owner)
- Response 200: `{ "Arrays": [8, 10], "Trees": [4, 5] }` (earned, max per topic)

### GET /api/v1/exam/mock-attempts
- Auth: Required
- Query: `?page=0&size=20`

---

## Peer Learning  ✅ IMPLEMENTED

### GET /api/v1/peer/requests
- Auth: Required
- Response 200: Page\<PeerSession\> (REQUESTED status)

### POST /api/v1/peer/requests
- Auth: Required (learner)
- Request: `{ "topic": "string(max200)", "description": "string", "requestedMinutes": 5-240 }`
- Response 201: PeerSession

### POST /api/v1/peer/requests/{id}/accept
- Auth: Required (any student except learner)
- Response 200: PeerSession (status → ACCEPTED)
- Errors: 400 own request, 409 already accepted

### POST /api/v1/peer/sessions/{id}/start
- Auth: Required (teacher)
- Response 200: PeerSession (status → IN_PROGRESS, startedAt set)

### POST /api/v1/peer/sessions/{id}/complete
- Auth: Required (learner or teacher)
- Request (optional): `{ "rating": 1-5, "feedback": "string" }` (learner only)
- Response 200: PeerSession
- Note: status → COMPLETED only when both confirm. Reward issued server-side after both confirm,
  if actualMinutes ≥ 5 AND learner rating provided.

### POST /api/v1/peer/sessions/{id}/dispute
- Auth: Required (participant)
- Query: `?reason=string`
- Response 200: PeerSession (status → DISPUTED)

### GET /api/v1/peer/sessions
- Auth: Required
- Response 200: Page\<PeerSession\> (all sessions where user is learner or teacher)

---

## Rewards  ✅ IMPLEMENTED

### GET /api/v1/rewards/balance
- Auth: Required
- Response 200: `{ "balance": 150 }`

### GET /api/v1/rewards/transactions
- Auth: Required
- Query: `?page=0&size=20`
- Response 200: Page\<RewardTransaction\>
- Transaction types: TEACHING_REWARD, STREAK_REWARD, MILESTONE_REWARD, COURSE_REDEMPTION, ADMIN_ADJUSTMENT, ADMIN_REVERSAL

---

## Progress Centre  ✅ IMPLEMENTED

### GET /api/v1/progress/summary
- Auth: Required
- Response 200: UserProgress `{ currentStreak, longestStreak, lastActivityDate, totalXp, level, nextLevelXp }`

### GET /api/v1/progress/streak
- Auth: Required
- Response 200: `{ "currentStreak": 5, "longestStreak": 14, "lastActivityDate": "2026-10-24" }`

### GET /api/v1/progress/activity
- Auth: Required
- Query: `?from=2026-01-01&to=2026-10-24`
- Response 200: `[{ "date": "2026-10-20", "xp": 35 }, ...]`

### GET /api/v1/progress/achievements
- Auth: Required
- Response 200: List\<UserAchievement\>

### GET /api/v1/progress/history
- Auth: Required
- Query: `?page=0&size=20`
- Response 200: Page\<LearningActivity\>

---

## Courses  ✅ IMPLEMENTED

### GET /api/v1/courses
- Auth: Required
- Query: `?category=string&type=INTERNAL|PARTNER&page=0&size=20`
- Response 200: Page\<Course\>

### GET /api/v1/courses/{id}
- Auth: Required
- Response 200: Course

### POST /api/v1/courses/{id}/redeem
- Auth: Required
- Response 201: CourseRedemption
- Errors: 409 already redeemed, 409 insufficient balance, 404 course not found/inactive

### GET /api/v1/courses/my-courses
- Auth: Required
- Query: `?page=0&size=20`
- Response 200: Page\<CourseRedemption\>

---

## AI Tutor  ✅ IMPLEMENTED

### POST /api/v1/ai/conversations
- Auth: Required
- Request (optional): `{ "title": "string", "subject": "string" }`
- Response 201: AiConversation

### GET /api/v1/ai/conversations
- Auth: Required
- Query: `?page=0&size=20`
- Response 200: Page\<AiConversation\>

### POST /api/v1/ai/conversations/{id}/messages
- Auth: Required (owner)
- Request: `{ "content": "string(max 4000)" }`
- Response 201: AiMessage (assistant reply)
- Errors: 503 if AI_API_KEY not configured

### GET /api/v1/ai/conversations/{id}/messages
- Auth: Required (owner)
- Response 200: List\<AiMessage\>

---

## Notes Library  ✅ IMPLEMENTED

### GET /api/v1/notes
- Auth: Required
- Query: `?subject=string&q=searchterm&page=0&size=20`
- Response 200: Page\<Note\> (public notes only)

### GET /api/v1/notes/my-notes
- Auth: Required
- Query: `?page=0&size=20`
- Response 200: Page\<Note\>

### POST /api/v1/notes (multipart/form-data)
- Auth: Required
- Parts: `file` (required), `title` (required), `subject`, `topic`, `description`, `isPublic`
- Allowed MIME: application/pdf, image/png, image/jpeg, application/msword, .docx, text/plain, text/markdown
- Max size: 10MB
- Response 201: Note (storagePath not exposed)

### GET /api/v1/notes/{id}
- Auth: Required
- Response 200: Note metadata

### GET /api/v1/notes/{id}/download
- Auth: Required (owner or public note)
- Response 200: file binary with Content-Disposition header
- Errors: 403 private note not owned, 404 file missing

### DELETE /api/v1/notes/{id}
- Auth: Required (owner)
- Response 204

---

## Community Q&A  ✅ IMPLEMENTED

### GET /api/v1/community/questions
- Auth: None (public)
- Query: `?subject=string&q=keyword&page=0&size=20`
- Response 200: Page\<CommunityQuestion\>

### GET /api/v1/community/questions/{id}
- Auth: None (increments view count)
- Response 200: CommunityQuestion
- Errors: 404 (removed questions)

### POST /api/v1/community/questions
- Auth: Required
- Request: `{ "title": "string(max400)", "body": "string(max10000)", "subject"?, "topic"? }`
- Response 201: CommunityQuestion

### DELETE /api/v1/community/questions/{id}
- Auth: Required (owner only)
- Response 204 (soft-delete → status=REMOVED)

### GET /api/v1/community/questions/{questionId}/answers
- Auth: None
- Query: `?page=0&size=20`
- Response 200: Page\<CommunityAnswer\> (sorted by voteCount desc)

### POST /api/v1/community/questions/{questionId}/answers
- Auth: Required
- Request: `{ "body": "string(max10000)" }`
- Response 201: CommunityAnswer

### DELETE /api/v1/community/answers/{id}
- Auth: Required (owner only)
- Response 204

### POST /api/v1/community/questions/{questionId}/answers/{answerId}/accept
- Auth: Required (question author only)
- Response 200: CommunityAnswer (accepted=true)

### POST /api/v1/community/questions/{id}/report
### POST /api/v1/community/answers/{id}/report
- Auth: Required
- Request: `{ "reason": "string(max100)", "details"?: "string" }`
- Response 201: ContentReport
- Errors: 409 already reported

---

## Admin  ✅ IMPLEMENTED
**All endpoints require `ROLE_ADMIN` — enforced server-side.**

### GET /api/v1/admin/users?page=0&size=25
### PATCH /api/v1/admin/users/{id}/status  `{ "status": "ACTIVE|SUSPENDED|DELETED" }`
### PATCH /api/v1/admin/users/{id}/role    `{ "role": "STUDENT|ADMIN" }`

### GET /api/v1/admin/courses?page=0&size=25
### POST /api/v1/admin/courses              (Course entity body)
### PATCH /api/v1/admin/courses/{id}        (partial Course update)
### PATCH /api/v1/admin/courses/{id}/active `{ "active": true|false }`

### GET /api/v1/admin/reports?status=PENDING&page=0&size=25
### GET /api/v1/admin/disputes?page=0&size=25
### POST /api/v1/admin/disputes/{id}/resolve?resolution=COMPLETED|CANCELLED

### POST /api/v1/admin/rewards/adjustments?userId=1  `{ "amount": 50, "reason": "string" }`
  (positive=credit, negative=debit)

### GET /api/v1/admin/quizzes?page=0&size=25
### PATCH /api/v1/admin/quizzes/{id}/active         `{ "active": true|false }`
### GET /api/v1/admin/mock-exams?page=0&size=25
### PATCH /api/v1/admin/mock-exams/{id}/active      `{ "active": true|false }`
### POST /api/v1/admin/problems                     (Problem entity body)
### PATCH /api/v1/admin/problems/{id}/active        `{ "active": true|false }`

---

## Error Codes Reference
| HTTP | When |
|---|---|
| 400 | Validation failure, invalid input |
| 401 | Missing/invalid/expired JWT |
| 403 | Wrong role, not owner |
| 404 | Resource not found or removed |
| 409 | Conflict (duplicate, wrong state, insufficient balance) |
| 422 | Unprocessable (expired attempt) |
| 503 | External provider not configured |
| 500 | Unexpected server error (safe message only) |
