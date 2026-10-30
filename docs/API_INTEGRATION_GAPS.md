# API integration status

The frontend now uses the **real backend contract** (`docs/API_CONTRACTS.md`). Every response is wrapped as `{ success, data, timestamp }` (unwrapped by `frontend/src/services/api.ts`); errors are `{ success:false, error }`. Lists use Spring's page shape `{ content, number, totalPages, totalElements }`.

Verified by script (no running backend needed): all 67 distinct frontend calls match a controller route and HTTP method, and all 27 frontend response types match the JSON fields the backend entities emit (after the serialization fixes in `INTEGRATION_REPORT.md`).

## Connected
| Screen | Endpoints |
|---|---|
| Sign in / register | `POST /auth/login`, `/auth/register` |
| Dashboard | `GET /dashboard`, `/progress/summary`, `/progress/activity`, `/progress/history` |
| Progress Centre | `GET /progress/summary`, `/activity`, `/achievements`, `/history`, `/rewards/balance` |
| Rewards Store | `GET /courses`, `/courses/my-courses`, `/rewards/balance`, `/rewards/transactions`; `POST /courses/{id}/redeem` |
| Code Studio | `GET /exam/problems`, `/exam/problems/{id}`, `/exam/problems/{id}/test-cases`, `/exam/submissions`, `/exam/submissions/{id}`; `POST /exam/problems/{id}/submissions` |
| Quick Revision | `GET /exam/quizzes`, `/exam/attempts`; `POST /exam/quizzes/{id}/attempts`, `/exam/attempts/{id}/submit`; `PUT /exam/attempts/{id}/answers` |
| Mock Exams | `GET /exam/mock-exams`, `/exam/mock-attempts`, `/exam/mock-attempts/{id}/performance`; `POST /exam/mock-exams/{id}/attempts`, `/exam/mock-attempts/{id}/submit`; `PUT /exam/mock-attempts/{id}/answers` |
| Peer Learning | `GET /peer/requests`, `/peer/sessions`; `POST /peer/requests`, `/peer/requests/{id}/accept`, `/peer/sessions/{id}/start`, `/complete`, `/dispute` |
| Notes | `GET /notes`, `/notes/my-notes`, `/notes/{id}/download`; `POST /notes` (multipart); `DELETE /notes/{id}` |
| Community | `GET /community/questions`, `/questions/{id}`, `/questions/{id}/answers`; `POST` ask, answer, accept, report; `DELETE` question/answer |
| AI Tutor | `GET /ai/conversations`, `/ai/conversations/{id}/messages`; `POST /ai/conversations`, `/ai/conversations/{id}/messages` |
| Settings | `GET /users/me`; `PATCH /users/me`; `POST /users/me/password` |
| Learning History | `/progress/history`, `/exam/submissions`, `/exam/attempts`, `/exam/mock-attempts`, `/peer/sessions`, `/rewards/transactions`, `/courses/my-courses` |
| Admin | `GET /admin/users`, `/courses`, `/quizzes`, `/mock-exams`, `/reports`, `/disputes`; `PATCH /admin/users/{id}/status`, `/role`, `/courses/{id}/active`, `/quizzes/{id}/active`, `/mock-exams/{id}/active`; `POST /admin/disputes/{id}/resolve`, `/admin/rewards/adjustments` |

## Not provided by the backend (the UI says so instead of faking it)
| Missing | Effect in the UI |
|---|---|
| Resolve / dismiss a report | Moderation is read-only |
| List of all reward transactions | Admin rewards page only offers balance adjustments |
| `GET /admin/overview`, `/admin/analytics` | Overview and analytics counts are derived from the list endpoints; "active users" is not available; no audit trail |
| Sample-test "run" without submitting | Code Studio has Submit only |
| Test-case comparison in the judge | "Accepted" shown with an explicit warning |
| Answer keys / explanations after submission | Review shows right or wrong only |
| Locked achievements, daily goal, subject preferences, notification and privacy settings | Not shown |
| Search parameters on problems, courses and peer requests | Filtering is applied to the loaded page and labelled that way |
| Reason stored on a peer dispute | Dispute sent with a fixed reason |
| Course creation form, quiz/exam content authoring | Done through the API (`POST /admin/courses`, `/admin/problems`); the console switches content on or off |
