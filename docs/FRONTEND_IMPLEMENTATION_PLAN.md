# Frontend implementation plan

> **Note:** the endpoints this plan proposed were replaced by the real backend contract; see `API_INTEGRATION_GAPS.md`.


Scope: complete the React frontend against a backend that does not have endpoints yet. Build every screen, connect it to a **proposed** API through one typed client, and show honest states (loading, not connected, empty, error). No fake user data.

## Stages and acceptance criteria
| Stage | Scope | Acceptance |
|---|---|---|
| 0 Audit | Inspect ZIP, create these docs | Findings recorded in `AI_PROGRESS.md` |
| 1 Foundation | Router, layouts, UI kit, tokens, motion, toasts, modals | Every nav item opens a page; refresh on any route works (host rewrite) |
| 2 Auth + dashboard | Login, register, logout, guards, 401 handling, dashboard | Validation, password toggle, loading/disabled states, expired-session message |
| 3 Exam Centre | Code Studio, Quick Revision, Mock Exams | Split layout, timer, navigator, mark-for-review, confirm-submit, auto-submit, results; sample mode clearly labelled |
| 4 Peer Learning | Requests, sessions, confirm/feedback/report, reward policy note | Lifecycle badges; no client-side awarding |
| 5 Progress Centre | Streak/XP/points, weekly bars, heatmap, achievements | Heatmap keyboard accessible; never invents history |
| 6 Rewards Store | Catalogue, redeem dialog, history | Redeemed only after server confirms; idempotency key sent |
| 7 AI Tutor, Notes, Community | Chat, upload/download, Q&A | Errors and retry; upload validation + progress |
| 8 History, Settings, Admin | Timeline, profile, admin tabs | Admin hidden from students; confirm dialogs |
| 9 QA | Type-check, lint, build, browser tests | See `AI_PROGRESS.md` for what has actually run |

## Design decisions
- Keep the existing dark violet system; new styles are in `src/styles/app.css` and reuse the tokens in `index.css`.
- No new dependencies. Charts, heatmap, modals (native `<dialog>`) and animation are hand-built CSS/React.
- Light theme not built (a complete dark theme was prioritised over a partial switcher).
