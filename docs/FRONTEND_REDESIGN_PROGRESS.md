# Frontend visual redesign: checkpoint

_Redesign completed in code on 24 Oct 2026; light-theme and motion refinement pass added the same day. Browser verification is still outstanding (see below)._

## Stage log
- [x] 1 Design tokens and theme system
- [x] 2 Legacy CSS converted to tokens (no hard-coded colours left in `App.css` or `styles/app.css`)
- [x] 3 App shell redesign
- [x] 4 Card system and motion
- [x] 5 Page redesigns: Landing, Dashboard, Exam Centre, Rewards, Progress. Remaining pages inherit the new tokens, cards, status accents and shell.
- [x] 6 Verification that was possible without a browser
- [ ] 7 Browser pass on Windows (lint, build, 375/430/768/1024/1440 widths, both themes). **Next step.**

## Theme implementation
- Tokens live in `src/index.css`: `:root` (dark, default) and `:root[data-theme='light']`. Surfaces, text, borders, accent, semantic hues, chart and heatmap colours, shadows, radii, spacing, motion durations and easing, focus ring.
- `index.html` has a tiny inline script that sets `data-theme` before first paint (saved choice, else system preference), so there is no wrong-theme flash.
- `src/theme/`: `ThemeProvider` (persists to `localStorage` key `lm.theme`, follows the OS until the user chooses, briefly adds `theme-switching` for a cross-fade, updates `theme-color`), `ThemeToggle` (sun/moon button with accessible label). The toggle is in the app top bar, the landing header and the sign-in/register pages.

## What changed
- **Shell:** grouped sidebar (Learn, Together, Growth, Account) with animated active indicator, desktop collapse-to-rail (remembered), mobile drawer, sticky blurred top bar, breadcrumbs, profile menu (settings, log out).
- **Cards:** `LinkCard` (whole card is one link; accent edge, glow, icon and arrow motion), metric cards (count-up numbers, unavailable shown as an em dash with screen-reader text), course/resource cards, session cards with status-coloured edge, achievement shine on unlocked badges. Hover motion only on hover-capable devices; reduced-motion disables it.
- **Dashboard:** hero with date, Continue learning / Open Exam Centre, daily-goal ring; metric cards; four Continue-learning link cards; progress area with balanced empty states.
- **Exam Centre:** three distinct link cards plus a note on server-side execution.
- **Rewards:** hero with points-balance card (shows an em dash until the server supplies a balance), type chips, richer course cards (description and outcomes when the API provides them), polished empty states. No invented courses or prices.
- **Progress:** XP ring plus streak and points cards; the heatmap renders only when the API returns activity.
- **Landing:** new narrative: hero with illustrative product window, bento features, Exam Centre / AI Tutor / Peer learning / Progress showcases, FAQ, closing banner. Illustrations are labelled and contain no statistics or testimonials.
- New files: `src/theme/*`, `src/components/cards.tsx`, `src/layouts/ProfileMenu.tsx`, `src/styles/system.css`. Main edits: `index.css`, `App.css`, `styles/app.css`, `layouts/*`, `components/ui.tsx`, `pages/*`.
- Backend untouched.

## Verified (actual results, Linux sandbox)
- `tsc -b --force`: 0 errors.
- All 4 stylesheets parse with PostCSS; braces balanced.
- CSS variable audit: every `var(--x)` is defined; every dark token has a light override.
- Contrast computed from the real token values: 46 checks (text, muted, accent, button, semantic tints, code, brand gradient, non-text) all meet WCAG AA targets in both themes (one dark-theme brand-gradient pair was fixed after the first run).
- Node server render of all 14 app pages (student + admin routes), landing, login, register, 403, 404, exam runner and heatmap: no exceptions; 15 content assertions pass (no invented names, balances, courses or statistics; theme toggle present).

## Not verified
- `npm run lint`, `npm run build` (Windows-only binaries in the supplied `node_modules`).
- Anything that needs a browser: real colours on screen, theme-toggle click and persistence across refresh, animations, sidebar collapse and drawer, sticky header, keyboard focus order, responsive widths, console errors.
- Light-theme appearance of every page was checked by contrast maths, not by eye.

## Known issues / cleanup
- `App.css` still contains the old landing-page rules (hero, workspace card, etc.), now unused. Safe to prune after the browser pass.
- No automated UI tests exist.

## Exact next steps
1. `npm install`, `npm run lint`, `npm run build`, `npm run dev` on Windows; fix anything reported.
2. In the browser, toggle the theme on every route, refresh to confirm persistence, then resize through 375 / 430 / 768 / 1024 / 1440 px.
3. Report visual problems (with screenshots) and I will fix them.


---

# Pass 2: light-theme redesign and motion upgrade

## Diagnosis (why the light theme felt flat)
- Page, sidebar and cards were near-identical lightness (`#f3f2f8` vs `#fff` with almost no shadow), so there was no visible surface ladder.
- Buttons, tabs, inputs and chips had no light-specific treatment (flat violet, low-contrast input borders).
- No atmosphere: the dark theme had glows and washes, the light theme had none.
- Entrance animations pinned `transform: none` as their final keyframe, which silently blocked hover lift on cards (**bug in both themes**, fixed).

## Light palette (tokens in `src/index.css`)
- Surface ladder: page `#eeecf9` (lavender-tinted) < sidebar `#f8f7fe` < card `#ffffff` < nested `#f9f8ff` < popover/modal/toast `#ffffff` with the strongest shadow.
- Text `#14112a`, muted `#48445f`, subtle/placeholder `#5d5975`. Violet `#6a45e0`, deeper violet for text/focus `#5733c9`, indigo `#3558d6`, teal `#0f8f86` (learning progress), amber `#d97706` (streaks), green/red semantic.
- Violet-tinted elevation (`--shadow-card`, `--shadow-card-hover`, `--shadow-2/3`), never black.
- Selective gradients: primary button, hero text, brand surfaces (balance card, closing banner), card washes. Not one gradient everywhere.
- Atmosphere tokens (`--page-a/b`, `--wash`, `--hero-a/b/teal`, `--band`, `--edge-glow`) are **0% in dark**, so the dark look is unchanged.
- Form controls use `--surface-input` and `--border-input` (3:1+ boundary in light), tabs use a white pill with a soft shadow, secondary buttons are white with a visible border.

## Dark theme
- Diffed against the version you reviewed: **no existing dark token value changed**; only new tokens were added, with dark values that reproduce the old look.
- Small intentional differences: focus ring slightly stronger (35% vs 22%), faint inset outline on empty heatmap cells, table header tint and row hover, quiz options sit on the card colour instead of transparent.

## Motion system
- Shared tokens: `--dur-fast/med/slow`, `--ease`, `--ease-in-out`, `--ease-spring`, `--stagger`.
- Page enter on every route; CSS-only staggered entrance for stat, exam, feature, achievement, problem, peer-list and quick-link grids (`:nth-child`, capped at 8 steps); landing scroll reveals with a no-JS fallback.
- Cards: lift + soft shadow + accent edge + icon/arrow motion, hover only on hover-capable devices, matching `:focus-visible`.
- Buttons: hover lift, press scale, focus ring. Progress bars fill in; rings animate; metric numbers count up; heatmap cells scale on hover; unlocked achievements shine once.
- Sidebar: animated active marker, collapsible Exam Centre group (grid-rows height animation, chevron toggle, `inert` when closed), collapse-to-rail, drawer scrim now fades in and out.
- Profile menu and toasts animate in and out; native `<dialog>` dialogs fade/slide using `@starting-style` (older browsers simply show them without animation).
- Theme switch cross-fades colour, border and shadow for 220 ms only while switching.
- Exam screens stay motion-free (`.exam-focus`), and `prefers-reduced-motion` disables transitions and entrance animation.

## Verified (actual results, Linux sandbox)
- `tsc -b --force`: 0 errors.
- PostCSS parses all four stylesheets; no undefined CSS variables; every dark token has a light value; no hard-coded colours outside `index.css`.
- **Contrast computed from the real token values: 88 checks across both themes, 0 below target** (text 4.5:1, non-text 3:1, including input borders, heatmap, focus ring, charts, tinted status badges, gradient button, brand surfaces). Lowest light result 3.10 (input border), lowest dark 4.72.
- Node server render: 14 app pages (student and admin routes), landing, login, register, 403, 404, exam runner and heatmap, 42 checks including the new sub-nav, scrim and menu structure. No fabricated names, balances, courses or statistics.

## Not verified (needs you, in a browser)
- `npm run lint`, `npm run build` (the supplied `node_modules` is Windows-only).
- How anything actually looks or moves: both themes on every page, hover/press/focus feel, stagger timing, theme-switch smoothness, dialog and menu transitions, drawer and sidebar behaviour, reduced-motion with the OS setting on.
- Responsive checks at 375, 430, 768, 1024, 1440 px; horizontal overflow; Code Studio layout on a phone.
- Theme persistence after refresh and no wrong-theme flash (logic unchanged this pass, but untested in a browser).
- Reference screenshots did not reach me, so the palette follows your written description, not the images.

## Next steps
1. `npm install`, `npm run lint`, `npm run build`, `npm run dev`.
2. Walk every route in light and dark, then 375 to 1440 px. Screenshot anything that looks off.
3. Prune the unused legacy landing rules in `App.css` once the new landing is confirmed.


---

# Pass 3: admin experience and animation refinement

## Root cause of admins landing on the student dashboard
Three things combined; hiding or adding a link would not have fixed any of them:
1. **Redirect race.** The sign-in page redirected any signed-in user to `from ?? '/dashboard'` regardless of role.
2. **Role-blind dashboard.** `/dashboard` rendered the student dashboard for everyone.
3. **One navigation for all roles.** The sidebar was a single student list with an extra "Admin" link.

## Fix
- `auth/roles.ts` (`homeFor`): one place that says where each role lands. Login, registration and the admin portal redirect through it.
- `auth/AdminToHome.tsx`: admins opening `/dashboard` are sent to `/admin`.
- `layouts/nav.ts`: separate `studentNavGroups` and `adminNavGroups` (`navFor(role)`). The admin list has Admin Overview, Platform Analytics, User / Course / Quiz & Exam management, Community Moderation, Peer-Learning Disputes, Rewards & Transactions, Account Settings. Student links are not shown to admins.
- `layouts/AppLayout.tsx` is role-aware: labelled "Admin navigation", an "Admin console" chip and admin breadcrumbs (`Admin > User Management`). One shared shell, not a duplicate.
- Admin routes (all behind `ProtectedRoute role="ADMIN"`): `/admin`, `/admin/users`, `/admin/courses`, `/admin/exams`, `/admin/moderation`, `/admin/disputes`, `/admin/rewards`, `/admin/analytics`, `/admin/settings`. Roles survive a refresh because the session role is restored from `sessionStorage` before the first render.
- `/admin/login` is the dedicated administrator sign-in. The backend accepts only accounts with the `ADMIN` role there; a student receives an access-denied message and no session is created.
- Frontend guards are navigation only. The backend also enforces roles on every `/api/v1/admin/**` endpoint.

## Admin pages (`pages/app/admin/`)
- `AdminOverviewPage`: "Admin Dashboard", compact metric cards (users, active users, pending reports, published courses, quiz/exam items, pending disputes), six quick-action cards, needs-review panels (reports, disputes) and an activity list. Without data every figure is an em dash (screen readers hear "Not available"); nothing is estimated.
- `AdminSectionPage`: one reusable management view (search, collapsible status filter, "Showing X of Y", accessible table or course cards, confirmation dialog for every action, errors surfaced, no success toast until the backend answers).
- `AdminAnalyticsPage`: metric cards plus a chart when the backend returns data.
- `config.ts` (section definitions and proposed endpoints), `format.ts` (status badge tones, cell formatting).
- New proposed endpoints (also in `API_INTEGRATION_GAPS.md`): `GET /admin/overview`, `GET /admin/analytics` shape. Existing admin endpoints unchanged.

## Animation changes
- Single **sliding nav indicator** that glides to the active link (and re-measures while groups expand), replacing per-link markers.
- Sidebar collapse: labels fade and shrink with the rail instead of disappearing; nested Exam Centre group animates; breadcrumb segments ease in on route change.
- Dialogs keep their content mounted while fading out (previously the body went blank mid-fade) and keep their last title.
- Mobile drawer closes on route change; scrim fades both ways.
- Admin filter panel opens with a height animation; table rows and status badges reuse the shared hover/transition tokens.
- All hover-only transforms are now inside `@media (hover: hover)` (7 rules fixed automatically), so touch devices get no stuck hover states.
- Weekly bars now label each bar with its real weekday (they were a fixed Mon-Sun sequence before).
- Reduced-motion rules cover the new indicator, labels, filter panel and breadcrumbs.

## Files changed
New: `auth/roles.ts`, `auth/AdminToHome.tsx`, `pages/app/admin/{AdminOverviewPage,AdminSectionPage,AdminAnalyticsPage,config,format}.ts(x)`.
Edited: `App.tsx`, `layouts/{AppLayout.tsx,nav.ts,ProfileMenu.tsx}`, `pages/auth/{LoginPage,RegisterPage,AdminLoginPage}.tsx`, `components/{ui.tsx,cards.tsx,charts.tsx}`, `lib/types.ts`, `styles/{system.css,app.css}`, `App.css`, `docs/*`.
Removed: `pages/app/AdminPage.tsx` (replaced by the pages above).
Backend: untouched.

## Verified (actual results, Linux sandbox)
- `tsc -b --force`: 0 errors.
- PostCSS parses all four stylesheets; no undefined CSS variables; no hover transforms outside `(hover: hover)`; no hard-coded colours outside `index.css`.
- Contrast on the new admin elements (role chip, links, status badges, table header) meets AA in both themes (lowest 4.69).
- **Role behaviour test, 59 checks, all passing** (server render with a route table that mirrors `App.tsx`'s guards): student sees the student dashboard and nav and no admin content; admin sees "Admin Dashboard", admin nav and breadcrumbs, no student greeting or learning CTAs and no student links in the sidebar; exactly one correct active nav item on each of the 9 admin routes; student and signed-out users do not receive admin page content; admin on `/dashboard` and signed-in users on `/login` do not get the page rendered (redirect); all 13 student pages render under both roles with the right chrome; six admin metric cards show "Not available"; no invented numbers.

## Not verified (needs you, in a browser)
- `npm run lint`, `npm run build` (the supplied `node_modules` is Windows-only).
- The redirects themselves: the test environment cannot follow `<Navigate>`, so "an administrator lands on /admin after signing in at /admin/login" is confirmed by code path and rendering of each side, not by clicking.
- Real feel of every animation: sliding indicator position (it measures link positions in the browser), sidebar collapse, dialog fade-out (uses `@starting-style`; older browsers just skip the animation), drawer, menus, filter panel, hover on touch devices, reduced-motion with the OS setting on.
- Both themes on the admin pages by eye, and 375 / 430 / 768 / 1024 / 1440 px layouts.

## Next steps
1. `npm install`, `npm run lint`, `npm run build`, `npm run dev`.
2. Sign in at `/admin/login` with an administrator account: confirm you land on the Admin Dashboard with the admin sidebar; refresh on `/admin/users`.
3. Try collapse, drawer, dialogs, theme toggle on admin pages; report anything off.


---

# Pass 4: backend integration

Done: the frontend was rewired from a guessed API to the real Spring Boot contract and the supplied backend was adopted and patched. Full detail, fixed backend defects, and unverified items are in `docs/INTEGRATION_REPORT.md`; endpoint status per screen is in `docs/API_INTEGRATION_GAPS.md`.

Frontend files changed: `services/api.ts` (envelope, clock skew, error shape), `lib/types.ts` (real field names), `lib/dates.ts`, `lib/examAdapter.ts`, `lib/sampleQuiz.ts`, `auth/*` (login/register mapping, `updateUser`), `components/ExamRunner.tsx` (server-driven answers, deadline and scoring), `components/charts.tsx` (XP heatmap, real weekday labels), every page under `pages/app/` plus the admin pages, `pages/auth/*` redirects.

Behaviour changes worth knowing: no hard-coded reward amount in the UI; no Idempotency-Key header (it would fail the backend's CORS preflight); Code Studio is submit-and-poll with an explicit judge-limitation notice; settings now cover name, bio, timezone and password (daily goal and subject preferences were removed because the backend has no such fields); admin overview counts come from the list endpoints.

Still to verify by you: `npm run lint`, `npm run build`, `.\mvnw.cmd test`, then a full click-through with the backend running.
