# Progress checkpoint

> **Superseded for backend status.** The backend now exists and the frontend is wired to it; see `INTEGRATION_REPORT.md` and `API_INTEGRATION_GAPS.md`. The notes below describe the earlier, backend-less state.


_Last updated: 24 Oct 2026_

> Visual redesign (themes, shell, cards, landing) is tracked in `FRONTEND_REDESIGN_PROGRESS.md`.

## Current stage
Stages 0-8 implemented in code. Stage 9 (QA) only partly done, see "Verified" and "Not verified".

## Done
- Router with lazy-loaded pages, protected and admin routes, 403/404 pages
- Auth (login/register/logout, session expiry), session restored from `sessionStorage`
- App shell: responsive sidebar/drawer, breadcrumbs, skip link, toasts, modal, tabs
- Pages: Dashboard, AI Tutor, Exam Centre (Code Studio, Quick Revision, Mock Exams), Peer Learning, Notes, Community, Rewards, Progress (heatmap), History, Settings, Admin
- Landing page: real CTAs, in-app tour section, FAQ, scroll reveal
- Deployment rewrites: `public/_redirects` (Netlify), `vercel.json`

## Verified (actual results, sandbox)
- `tsc -b --force`: 0 errors (45 source files; an injected canary error was detected)
- Node server-side render of all 14 app pages (student and admin routes), 5 public pages, Heatmap, WeeklyBars and ExamRunner: no exceptions

## Not verified
- `npm run lint` and `npm run build`: the supplied `node_modules` holds Windows-only binaries; could not run in the Linux sandbox
- Nothing was tested in a real browser: layout, animation, keyboard behaviour, mobile widths, console errors
- No automated test framework exists in the project; none added
- No real backend integration: the backend has no endpoints

## Known issues / limitations
- Quick Revision and Mock Exams run only in labelled sample mode; the live attempt player needs the backend contract
- Code Studio cannot execute code; Run/Submit show "unavailable" unless a real service answers
- No light theme, no notifications panel
- Fonts load from Google Fonts (existing behaviour)

## Next actions
1. On Windows: `npm install`, `npm run lint`, `npm run build`, `npm run dev`, then click through every page at desktop and phone widths and fix what you find
2. Backend Phase 2-3: datasource config, `/auth/register`, `/auth/login` (JWT), CORS for `http://localhost:5173`, then `/dashboard/summary`
3. Work through `API_INTEGRATION_GAPS.md` one module at a time
