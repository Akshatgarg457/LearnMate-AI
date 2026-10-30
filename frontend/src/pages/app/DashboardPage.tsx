import { Activity, Bot, CalendarDays, CalendarCheck, ClipboardCheck, CodeXml, Flame, Gift, GraduationCap, Sparkles, Target, Timer, Trophy } from 'lucide-react'
import { useAuth } from '../../auth/authContext'
import Button from '../../components/Button'
import { ApiNotice } from '../../components/ResourceView'
import { LinkCard, SectionHead } from '../../components/cards'
import { WeeklyBars } from '../../components/charts'
import { Badge, EmptyState, ProgressBar, ProgressRing, StatCard } from '../../components/ui'
import { useLive } from '../../hooks/useLive'
import { useResource } from '../../hooks/useResource'
import { daysAgo, isoDate, lastDaysXp, prettyEnum, XP_PER_LEVEL } from '../../lib/dates'
import type { ActivityDay, DashboardSummary, LearningActivity, Page, UserProgress } from '../../lib/types'

const dash = '\u2014'

export default function DashboardPage() {
  const { session } = useAuth()
  const live = useLive()
  const res = useResource<DashboardSummary>(live ? '/dashboard' : null)
  const progress = useResource<UserProgress>(live ? '/progress/summary' : null)
  const activity = useResource<ActivityDay[]>(live ? '/progress/activity' : null, { from: isoDate(daysAgo(6)), to: isoDate(new Date()) })
  const history = useResource<Page<LearningActivity>>(live ? '/progress/history' : null, { size: '5' })
  const d = res.data
  const hour = new Date().getHours()
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening'
  const first = session?.user.displayName.split(' ')[0]
  const title = first ? `${greeting}, ${first}` : `${greeting}!`
  const today = new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' })
  const weekXp = activity.data ? lastDaysXp(activity.data, 7) : null
  const toNext = progress.data?.nextLevelXp

  return (
    <>
      <section className="card hero-panel enter" aria-labelledby="dash-title">
        <div>
          <p className="date-chip">
            <CalendarDays size={14} aria-hidden="true" /> {today}
          </p>
          <h1 id="dash-title">{title}</h1>
          <p>Pick up where you left off, or start something new. Ask the tutor, practise in the Exam Centre, or help a classmate.</p>
          <div className="hero-actions-row">
            <Button to="/ai-tutor">
              <Sparkles size={16} aria-hidden="true" /> Continue learning
            </Button>
            <Button to="/exam-centre" variant="secondary">
              <GraduationCap size={16} aria-hidden="true" /> Open Exam Centre
            </Button>
          </div>
        </div>
        <div className="goal-box">
          <ProgressRing value={d?.activeDaysLast30 ?? 0} max={30} label="Active days in the last 30" tone="teal" size={92}>
            <span>{d ? d.activeDaysLast30 : dash}</span>
          </ProgressRing>
          <div>
            <strong>Active days</strong>
            <span className="muted">{d ? `${d.activeDaysLast30} of the last 30 days` : 'Appears once activity is recorded'}</span>
          </div>
        </div>
      </section>

      <div className="stat-grid">
        <StatCard index={0} icon={Flame} tone="amber" label="Current streak" value={d ? d.currentStreak : dash} unit={d ? 'days' : undefined} hint={d ? `Longest: ${d.longestStreak} days` : undefined} />
        <StatCard index={1} icon={Sparkles} tone="violet" label="XP and level" value={d ? `Level ${d.level}` : dash} hint={d ? `${d.totalXp.toLocaleString()} XP` : undefined} />
        <StatCard index={2} icon={Gift} tone="green" label="Redeemable points" value={d ? d.rewardBalance : dash} hint="Spend them in the Rewards Store" />
        <StatCard index={3} icon={CodeXml} tone="blue" label="Code submissions" value={d ? d.totalCodeSubmissions : dash} hint="All attempts, accepted or not" />
        <StatCard index={4} icon={Trophy} tone="violet" label="Quizzes and mocks" value={d ? `${d.quizzesCompleted} / ${d.mockExamsCompleted}` : dash} hint="Quizzes / mock exams" />
        <StatCard index={5} icon={CalendarCheck} tone="green" label="Active days (30 days)" value={d ? d.activeDaysLast30 : dash} />
      </div>

      <section className="section" aria-labelledby="continue-title">
        <SectionHead title="Continue learning" hint="Jump straight back into practice." />
        <span id="continue-title" className="sr-only">Continue learning</span>
        <div className="exam-tiles">
          <LinkCard index={0} to="/exam-centre/code-studio" icon={CodeXml} tone="violet" title="Code Studio" text="Solve problems in Java, Python or C++." meta={['Java', 'Python', 'C++']} cta="Open Code Studio" />
          <LinkCard index={1} to="/exam-centre/quick-revision" icon={Timer} tone="blue" title="Quick Revision" text="Short timed quizzes by subject." meta={['Timed', 'Untimed option']} cta="Start a quiz" />
          <LinkCard index={2} to="/exam-centre/mock-exams" icon={ClipboardCheck} tone="green" title="Mock Exams" text="Full-length exams with a question navigator." cta="Browse mock exams" />
          <LinkCard index={3} to="/ai-tutor" icon={Bot} tone="amber" title="AI Tutor" text="Ask for an explanation, an example or practice questions." cta="Ask the tutor" />
        </div>
      </section>

      <section className="section">
        <SectionHead title="Your progress" hint="Everything here comes from your recorded activity." />
        {res.status === 'error' ? (
          <ApiNotice endpoint="GET /dashboard" message={res.problem?.message ?? 'Request failed.'} onRetry={res.reload} />
        ) : d ? (
          <div className="two-col">
            <section className="card pad">
              <h3 className="h3">XP this week</h3>
              {weekXp ? <WeeklyBars values={weekXp} label="XP earned" /> : <p className="muted">Loading…</p>}
            </section>
            <section className="card pad">
              <h3 className="h3">XP to next level</h3>
              {toNext !== undefined ? (
                <>
                  <ProgressBar label="XP progress to next level" value={XP_PER_LEVEL - toNext} max={XP_PER_LEVEL} />
                  <p className="muted">{toNext} XP to level {d.level + 1}</p>
                </>
              ) : (
                <p className="muted">Loading…</p>
              )}
            </section>
            <section className="card pad wide">
              <h3 className="h3">Recent activity</h3>
              {history.data && history.data.content.length > 0 ? (
                <ul className="timeline">
                  {history.data.content.map((r) => (
                    <li key={r.id}>
                      <Badge tone="violet">{prettyEnum(r.activityType)}</Badge>
                      <span>+{r.xpAwarded} XP</span>
                      <time dateTime={r.createdAt}>{new Date(r.createdAt).toLocaleString()}</time>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="muted">Nothing yet. Solve a problem or take a quiz to get started.</p>
              )}
            </section>
          </div>
        ) : (
          <div className="two-col">
            <EmptyState icon={Activity} title="Your week at a glance" action={<div className="empty-actions"><Button to="/exam-centre/quick-revision" variant="secondary">Take a quick quiz</Button></div>}>
              {res.status === 'idle' ? 'Your weekly XP will appear here once activity is available.' : 'Loading your activity…'}
            </EmptyState>
            <EmptyState icon={Target} title="Streaks and milestones" action={<div className="empty-actions"><Button to="/progress" variant="secondary">Open Progress Centre</Button></div>}>
              Solve a problem, finish a quiz or complete a study session to start a streak. Just opening the site doesn&apos;t count.
            </EmptyState>
          </div>
        )}
      </section>
    </>
  )
}
