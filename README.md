# LearnMate AI

LearnMate AI is a full-stack learning platform where students learn with an AI tutor, ask a human for help, share notes, practise with quizzes and coding problems, and track their progress. Administrators manage users, content, moderation and rewards from a dedicated portal.

- **Frontend:** React 19, TypeScript, Vite, React Router, Monaco editor
- **Backend:** Spring Boot 4, Spring Security + JWT, Spring Data JPA, MySQL 8
- **Integrations:** Google Gemini (AI Tutor), Google Meet (peer-learning links), Judge0 (code execution)

## Features

- **Accounts and sessions:** registration, JWT sign-in, profile editing, password change, session expiry handling
- **AI Tutor:** persistent conversations, Markdown answers with code blocks, text-to-speech and voice input
- **Exam Centre:** quick-revision quizzes, timed mock exams with server-side scoring, and Code Studio with 33 seeded problems judged against test cases through Judge0
- **Peer learning:** help requests, accept/start/complete flows, ratings, disputes, teaching rewards, and Google Meet links created with the teacher's own Google account
- **Notes:** upload, private or shared library, search, download and delete
- **Community:** questions, answers, accepted answers, reports and moderation
- **Rewards and progress:** points balance and history, courses redemption, XP, streaks, heatmap, achievements
- **Admin portal:** dedicated `/admin/login`, user/role management, courses, quizzes and exams, community reports, peer-learning disputes, reward adjustments, platform analytics
- **UI:** responsive dark and light themes, accessible focus states, reduced-motion support

## Architecture

```
learnmate-backend/
├── frontend/                     React single-page app (Vite)
│   ├── src/
│   │   ├── auth/                 session context, route guards, role routing
│   │   ├── components/           shared UI kit, tables, modals, charts
│   │   ├── hooks/                API resource loading and session helpers
│   │   ├── layouts/              app shell, role-aware navigation
│   │   ├── pages/                public, student and admin pages
│   │   ├── services/             single typed Axios client (envelope + JWT)
│   │   ├── styles/               design tokens and feature styles
│   │   └── theme/                dark/light theme provider
│   └── public/                   static assets and SPA rewrites
├── backend/learnmate-backend/    Spring Boot application (Maven wrapper)
│   └── src/main/java/com/learnmate/backend/
│       ├── auth/                 login, registration, JWT, Google OAuth controller
│       ├── ai/                   AI Tutor + Gemini/Anthropic providers
│       ├── exam/                 quizzes, mock exams, Code Studio, Judge0
│       ├── google/               OAuth token storage/encryption and Meet spaces
│       ├── peer/                 peer sessions, ratings, disputes, meetings
│       ├── notes/ community/     notes library and community Q&A
│       ├── reward/ progress/     points, XP, streaks, achievements
│       ├── admin/ user/ course/  administration and accounts
│       └── common/               security, errors, shared responses
├── docs/                         API contract, security notes, integration reports
├── .env.example                  backend environment variables (placeholders only)
└── frontend/.env.example         frontend environment variables
```

The frontend talks to the backend only through `frontend/src/services/api.ts`. The backend is stateless: every protected request carries a JWT, and admin endpoints require `ROLE_ADMIN` server-side.

## Prerequisites

- **Java 21** — `java -version` must report 21, and `JAVA_HOME` must point to a JDK 21 installation (Maven fails with `release version 21 not supported` otherwise)
- **MySQL 8**
- **Node.js 20+** and npm
- Optional: a Gemini API key, a Google Cloud OAuth client, and a Judge0 instance

## 1. Create the database

```sql
CREATE DATABASE learnmate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'learnmate_user'@'localhost' IDENTIFIED BY 'choose-a-strong-password';
GRANT ALL PRIVILEGES ON learnmate.* TO 'learnmate_user'@'localhost';
```

Tables are created on first start (`JPA_DDL_AUTO=update`). Use `validate` once the schema is stable.

## 2. Configure environment variables

Copy `.env.example` values into the environment of the shell that starts the backend. Spring Boot reads environment variables, not `.env` files, so set them in the terminal (or an IDE run configuration).

Required:

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `JWT_SECRET` | HMAC signing key, at least 32 random characters |
| `CORS_ALLOWED_ORIGINS` | Allowed browser origin, normally `http://localhost:5173` |

Optional:

| Variable | Default | Purpose |
|---|---|---|
| `JWT_EXPIRY_MS` | `86400000` | Access-token lifetime |
| `JPA_DDL_AUTO` | `update` | Hibernate schema mode |
| `AI_PROVIDER` | `gemini` | `gemini`, `anthropic` or unconfigured |
| `AI_API_KEY` | `NOT_CONFIGURED` | Gemini (or Anthropic) API key |
| `AI_MODEL` | `gemini-2.5-flash` | Model name for the selected provider |
| `AI_MAX_TOKENS` | `1000` | Response token limit |
| `JUDGE_BASE_URL`, `JUDGE_API_KEY` | `NOT_CONFIGURED` | Judge0 instance |
| `UPLOAD_DIR` | `./uploads` | Local note storage |
| `MAX_FILE_SIZE`, `MAX_REQUEST_SIZE` | `10MB` | Upload limits |
| `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET` | `NOT_CONFIGURED` | Google Meet OAuth client |
| `GOOGLE_OAUTH_REDIRECT_URI` | `http://localhost:8080/api/v1/auth/google/callback` | Must match Google Cloud exactly |
| `GOOGLE_OAUTH_FRONTEND_URL` | `http://localhost:5173` | Where the OAuth callback returns |
| `GOOGLE_TOKEN_ENCRYPTION_KEY` | empty (ephemeral) | AES-GCM key for stored Google tokens, 32+ random characters |
| `GOOGLE_OAUTH_STATE_TTL_SECONDS` | `600` | OAuth state lifetime |
| `CODE_STUDIO_SEED` | `true` | Seed the problem catalogue on start |
| `CODE_STUDIO_RATE_LIMIT_PER_MINUTE` / `_PER_HOUR` | `10` / `60` | Submission limits |

The AI key and the Google OAuth client are separate credentials. Never reuse one for the other.

### Google Gemini (AI Tutor)

1. Create an API key in [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Set `AI_PROVIDER=gemini`, `AI_API_KEY=<your key>` and `AI_MODEL=gemini-2.5-flash`.
3. Restart the backend and open **AI Tutor**. Without a key the endpoint returns a clear `503` instead of a fabricated answer.

### Google Meet (OAuth)

1. Create or select a project in [Google Cloud Console](https://console.cloud.google.com/).
2. Enable the **Google Meet API** for the project.
3. Configure the **OAuth consent screen**. For an External app in testing mode, add the Google accounts that will create meetings as test users. Add the scope `https://www.googleapis.com/auth/meetings.space.created`.
4. Under **Credentials → Create credentials → OAuth client ID → Web application**, add the authorized redirect URI `http://localhost:8080/api/v1/auth/google/callback` (it must match `GOOGLE_OAUTH_REDIRECT_URI` exactly).
5. Copy the client ID and secret into `GOOGLE_OAUTH_CLIENT_ID` / `GOOGLE_OAUTH_CLIENT_SECRET`.
6. Generate a token-encryption key (`openssl rand -hex 32` or a random 32+ character string) and set `GOOGLE_TOKEN_ENCRYPTION_KEY`. Without it, tokens are encrypted with an ephemeral key and Google must be reconnected after every restart.
7. Restart the backend, sign in, open **Peer Learning** and choose **Connect Google**. Access and refresh tokens are stored encrypted server-side and are never sent to the browser.

## 3. Start the backend (Windows PowerShell)

```powershell
cd backend\learnmate-backend

$env:DB_URL       = 'jdbc:mysql://localhost:3306/learnmate?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:DB_USERNAME  = 'learnmate_user'
$env:DB_PASSWORD  = 'choose-a-strong-password'

# 256-bit signing secret (PowerShell 5.1 and 7)
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
$env:JWT_SECRET = -join ($b | ForEach-Object { $_.ToString('x2') })

.\mvnw.cmd spring-boot:run
```

The API is served at `http://localhost:8080/api/v1`. The first run downloads Maven and dependencies.

## 4. Start the frontend (Windows PowerShell)

```powershell
cd frontend
npm install
npm run dev
```

Vite prints the local URL, normally `http://localhost:5173`. `VITE_API_BASE_URL` (see `frontend/.env.example`) defaults to `http://localhost:8080/api/v1`. All `VITE_*` values are embedded in the browser bundle, so never put secrets there.

## 5. Create the first administrator

Anyone who registers is a student. Register in the app, then promote that account in MySQL:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';
```

Sign out and use the **Admin Portal** at `/admin/login`. Students who try the admin form receive an access-denied message and no session; admin routes are also protected server-side.

## Testing and builds

```powershell
# Backend (uses H2, no MySQL needed)
$env:JAVA_HOME = 'C:\path\to\jdk-21'
cd backend\learnmate-backend
.\mvnw.cmd test

# Frontend
cd frontend
npm run lint
npm run build
```

Current status: the backend suite passes (80 tests, including Google OAuth/Meet and admin authorization), `npm run lint` reports 0 errors (13 existing compiler-purity warnings), and `npm run build` succeeds. There is no frontend unit-test runner configured.

Browser checks performed during development: authentication and role redirects, invalid credentials, student denial on the admin form, admin route guarding, responsive layouts at desktop and phone widths, keyboard focus order, and both themes. A live administrator sign-in and a real Google Meet creation require an administrator account and Google consent, so repeat those two checks in your environment.

## Security precautions

- **Never commit credentials.** `.env` files, local OAuth credentials, encryption keys and tokens are ignored by `.gitignore`; only `.env.example` belongs in the repository.
- **Use a strong `JWT_SECRET`.** The default in `application.properties` is a public placeholder and must not be used outside local development.
- **Keep local secrets outside the repository.** For example, store a Google credential file under `%USERPROFILE%\.learnmate\` and load it into the shell before starting the backend:

```powershell
Get-Content "$env:USERPROFILE\.learnmate\google-oauth.env" | ForEach-Object {
  if ($_ -match '^\s*([^#][^=]+)=(.*)$') { Set-Item "Env:$($matches[1].Trim())" $matches[2].Trim() }
}
```

- **HTTPS in production.** Terminate TLS at a reverse proxy or load balancer and restrict `CORS_ALLOWED_ORIGINS` to the deployed frontend.
- **Protect Google and AI keys.** Revoke and rotate any key or client secret that has been shared, committed or pasted into an issue.
- **Restrict Judge0.** Do not expose the judge instance publicly; only the backend should call it.
- **Use least-privilege database accounts** and back up the database and `UPLOAD_DIR`.
- **Set `JPA_DDL_AUTO=validate`** in production and manage schema changes explicitly.
- Google access/refresh tokens are encrypted at rest with AES-GCM, and OAuth state values are random and single-use.

See `docs/SECURITY_NOTES.md` for the current risk register.

## Current limitations

- JWTs cannot be revoked before expiry (logout is client-side); there is no refresh-token endpoint.
- No rate limiting on authentication endpoints — add it at the proxy or with a library before public deployment.
- No password-reset flow or "remember me" option.
- Quizzes and mock exams only reveal right/wrong after submission; explanations are never returned by the API.
- Admin pages return JPA entities directly; `POST/PUT /admin/courses` and `/admin/problems` accept entities, which is a mass-assignment risk.
- There is no admin overview/analytics aggregate endpoint; figures are counted from list endpoints and some are capped to the first 100 rows.
- No endpoint to resolve/dismiss community reports, and no global reward-transaction listing.
- AI conversation messages are not paginated.
- Notes are stored on the local filesystem, which suits a single node only.
- The SPA keeps the JWT in `sessionStorage`; a production deployment should prefer httpOnly cookies.
- Judge0 and Gemini are external services; without configuration those features respond `503` rather than pretending to work.
- The build warns about the Monaco editor chunk size (~2.7 MB); it is loaded only by Code Studio.
- Automated tests cover the backend only; Google Meet space creation and live Gemini calls are not part of the test suite.

## Deployment

Build the frontend with `npm run build` and host `frontend/dist` with SPA rewrites (`public/_redirects` for Netlify, `vercel.json` for Vercel). Set `VITE_API_BASE_URL` at build time, add the site origin to `CORS_ALLOWED_ORIGINS`, and run the backend behind HTTPS with production database and secret values.
