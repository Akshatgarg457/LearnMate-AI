# LearnMate AI — Security Notes

Last updated: 2026-10-30

This register reflects the current source. Items are only marked resolved when the code was inspected and the behaviour verified.

---

## Implemented safeguards

### Authentication
- Passwords hashed with BCrypt (strength 10)
- JWT signed with HMAC-SHA256; secret from `JWT_SECRET` (the built-in default is a public placeholder and must not be used outside local development)
- Token expiry enforced server-side (default 24h)
- Suspended and deleted accounts cannot sign in
- Email normalised to lowercase; password hashes are never returned in responses
- The admin portal (`/admin/login`) reuses the backend login and refuses to create a session unless the returned role is `ADMIN`

### Authorization
- All endpoints except `/api/v1/auth/**` and public community/notes reads require a JWT
- `/api/v1/admin/**` requires `ROLE_ADMIN` (path rule in `SecurityConfig` plus `@PreAuthorize` on the controller)
- `JwtAuthFilter` populates the security context from the Bearer token
- Ownership is checked in the service layer (`findByIdAndUserId`) for owned resources
- Frontend guards are navigation only; the server enforces every admin rule
- Admin authorization is covered by `AdminAuthorizationTest` (admin 200, student 403, anonymous 401)

### Exam integrity
- User code is never executed inside the Spring Boot JVM; Judge0 is called over HTTP
- Hidden test cases are excluded from API responses (only sample cases are returned)
- Quiz/mock-exam scoring happens on the server; client scores are ignored
- Option correctness and explanations are write-only: `QuizOption.correct`, `MockExamOption.correct`, `QuizQuestion.explanation` and `MockExamQuestion.explanation` use `@JsonProperty(access = WRITE_ONLY)`, so they are not serialised to students before or after submission

### Google OAuth / Meet
- OAuth state values are 32 random bytes, stored server-side, single-use and time-limited
- Access and refresh tokens are encrypted at rest with AES-GCM and never sent to the browser
- The callback only redirects to the configured frontend with a short result code
- Meet space creation validates that Google returns an `https://meet.google.com/` URI
- With no token-encryption key configured, an ephemeral key is generated and a warning is logged; users must reconnect after restart

### Rewards
- All credits/debits go through `RewardService` with an idempotency key
- Balance is checked before a debit (409 when insufficient)
- Teaching rewards are calculated server-side from session duration and rating

### File uploads (notes)
- MIME type validated against an allow-list; size capped at 10 MB
- Files stored under random UUID names; `storagePath` is never returned
- Download path validated to stay inside the upload directory
- Download allowed for the owner or when `isPublic=true`

### Error handling and CORS
- Stack traces and internal messages are not included in responses
- CORS is restricted to `CORS_ALLOWED_ORIGINS`
- CSRF is disabled because the API is stateless and uses Bearer tokens

### Community rendering
- The frontend renders question/answer bodies as text (React escaping, no `dangerouslySetInnerHTML`), so stored content is not interpreted as HTML today. This must be revisited if rich-text or HTML rendering is added.

---

## Remaining risks

### High
1. **No JWT revocation.** Tokens remain valid until expiry after logout, suspension or a password change. Fix: a server-side token blocklist or short-lived tokens plus rotating refresh tokens.
2. **No rate limiting on authentication endpoints.** Login and registration can be brute-forced. Fix: rate limiting at the reverse proxy or with a library such as Bucket4j.
3. **No refresh-token endpoint.** Clients must sign in again when the access token expires. Fix: implement rotating refresh tokens and revoke on logout.

### Medium
4. **Mass assignment on admin writes.** `POST/PUT /api/v1/admin/courses` and `POST /api/v1/admin/problems` accept JPA entities directly, allowing fields the UI never sets to be changed. Fix: dedicated request DTOs per operation.
5. **Entities exposed over the API.** Controllers return entities, so response safety depends on Jackson annotations. Any new field must be reviewed, or controllers should move to response DTOs.
6. **SPA stores the JWT in `sessionStorage`.** Tokens are readable by any injected script. Fix: httpOnly, Secure, SameSite cookies with CSRF protection, or keep tokens in memory and add refresh handling.
7. **No pagination for AI messages.** `GET /ai/conversations/{id}/messages` returns the whole conversation.
8. **Community bodies have no DB-level length limit.** DTO validation (10,000 characters) is the only guard.

### Low
9. **No audit log.** Role changes, suspensions and reward adjustments are not recorded in a dedicated audit table.
10. **HTTPS is not enforced by the application.** Terminate TLS at the proxy or load balancer.
11. **Local filesystem uploads.** Notes do not work across multiple nodes and depend on local backups.
12. **No automated test for live Gemini or live Google Meet space creation.** Both require external credentials and, for Meet, user consent; unit tests cover the adapters and OAuth flow but not the external calls.

---

## Verification status

- Backend suite: 80 tests passing (H2), including Google OAuth controller/service, Google Meet service and admin authorization.
- Frontend: `npm run lint` 0 errors (13 existing purity warnings); `npm run build` succeeds.
- Live checks performed: JWT login, student rejected by admin endpoints (403), admin token accepted by admin endpoints (200 in tests), Google OAuth consent URL generation and invalid-state callback handling.
- Not verified live: a real administrator sign-in in the browser, Google consent and Meet space creation, and Gemini responses (the running backend had no `AI_API_KEY` configured).
