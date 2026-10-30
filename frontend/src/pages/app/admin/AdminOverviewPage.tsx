import { CalendarDays, ClipboardList, Coins, Flag, Library, Scale, ShieldCheck, UserCheck, Users } from 'lucide-react'
import { Link } from 'react-router-dom'
import Button from '../../../components/Button'
import { LinkCard, SectionHead } from '../../../components/cards'
import { Badge, StatCard } from '../../../components/ui'
import { useLive } from '../../../hooks/useLive'
import { useResource } from '../../../hooks/useResource'
import type { ContentReport, Course, MockExam, Page, PeerSession, Quiz, UserProfile } from '../../../lib/types'

const dash = '\u2014'

function Panel({ title, to, empty, children }: { title: string; to?: string; empty: string; children?: React.ReactNode }) {
  return (
    <section className="card pad admin-panel">
      <div className="admin-panel-head">
        <h3 className="h3">{title}</h3>
        {to && <Link to={to} className="text-link">View all</Link>}
      </div>
      {children ?? <p className="muted">{empty}</p>}
    </section>
  )
}

export default function AdminOverviewPage() {
  const live = useLive()
  // The backend has no single "overview" endpoint, so the figures come from each list's own total.
  const users = useResource<Page<UserProfile>>(live ? '/admin/users' : null, { size: '1' })
  const reports = useResource<Page<ContentReport>>(live ? '/admin/reports' : null, { status: 'PENDING', size: '5' })
  const disputes = useResource<Page<PeerSession>>(live ? '/admin/disputes' : null, { size: '5' })
  const courses = useResource<Page<Course>>(live ? '/admin/courses' : null, { size: '100' })
  const quizzes = useResource<Page<Quiz>>(live ? '/admin/quizzes' : null, { size: '1' })
  const mocks = useResource<Page<MockExam>>(live ? '/admin/mock-exams' : null, { size: '1' })
  const today = new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' })

  const published = courses.data ? courses.data.content.filter((c) => c.active).length : undefined
  const publishedLabel = courses.data && courses.data.totalPages > 1 ? `${published}+` : published
  const examItems = quizzes.data && mocks.data ? quizzes.data.totalElements + mocks.data.totalElements : undefined
  const anyError = [users, reports, disputes, courses, quizzes, mocks].some((r) => r.status === 'error')

  return (
    <>
      <header className="admin-head enter">
        <div>
          <p className="date-chip">
            <ShieldCheck size={14} aria-hidden="true" /> Admin &middot; <CalendarDays size={14} aria-hidden="true" /> {today}
          </p>
          <h1>Admin Dashboard</h1>
          <p>Manage users, content and moderation for LearnMate AI. Figures below come from the platform backend and are not estimated here.</p>
        </div>
        <div className="page-header-actions">
          <Button to="/admin/moderation"><Flag size={16} aria-hidden="true" /> Review reports</Button>
          <Button to="/admin/users" variant="secondary"><Users size={16} aria-hidden="true" /> Manage users</Button>
        </div>
      </header>

      <div className="stat-grid admin-stats">
        <StatCard index={0} icon={Users} tone="violet" label="Registered users" value={users.data?.totalElements ?? dash} />
        <StatCard index={1} icon={UserCheck} tone="green" label="Active users" value={dash} hint="Not reported by the backend" />
        <StatCard index={2} icon={Flag} tone="amber" label="Pending reports" value={reports.data?.totalElements ?? dash} />
        <StatCard index={3} icon={Library} tone="blue" label="Published courses" value={publishedLabel ?? dash} />
        <StatCard index={4} icon={ClipboardList} tone="green" label="Quiz and exam items" value={examItems ?? dash} />
        <StatCard index={5} icon={Scale} tone="amber" label="Disputed sessions" value={disputes.data?.totalElements ?? dash} />
      </div>
      {anyError && (
        <p className="muted fine" role="status">Some figures could not be loaded. Check that you are signed in as an admin and the backend is running.</p>
      )}

      <section className="section" aria-label="Quick actions">
        <SectionHead title="Quick actions" hint="Jump to the work that needs attention." />
        <div className="admin-actions">
          <LinkCard compact index={0} to="/admin/users" icon={Users} tone="violet" title="Manage users" text="Accounts, roles and suspensions." cta="Open" />
          <LinkCard compact index={1} to="/admin/courses" icon={Library} tone="blue" title="Manage courses" text="Publish courses to the Rewards Store." cta="Open" />
          <LinkCard compact index={2} to="/admin/exams" icon={ClipboardList} tone="green" title="Quizzes and exams" text="Switch quizzes and mock exams on or off." cta="Open" />
          <LinkCard compact index={3} to="/admin/moderation" icon={Flag} tone="amber" title="Community reports" text="Content flagged by students." cta="Review" />
          <LinkCard compact index={4} to="/admin/disputes" icon={Scale} tone="violet" title="Peer-learning disputes" text="Contested sessions." cta="Review" />
          <LinkCard compact index={5} to="/admin/rewards" icon={Coins} tone="blue" title="Reward adjustments" text="Add or remove points with a recorded reason." cta="Open" />
        </div>
      </section>

      <section className="section">
        <SectionHead title="Needs review" hint="Newest items from the backend." />
        <div className="two-col">
          <Panel title="Pending moderation reports" to="/admin/moderation" empty={reports.data ? 'No reports are waiting.' : 'Reports appear here once you are signed in as an admin.'}>
            {reports.data && reports.data.content.length > 0 && (
              <ul className="timeline">
                {reports.data.content.map((r) => (
                  <li key={r.id}><Badge tone="amber">{r.contentType.toLowerCase().replace('_', ' ')}</Badge><span>{r.reason}</span><time dateTime={r.createdAt}>{new Date(r.createdAt).toLocaleString()}</time></li>
                ))}
              </ul>
            )}
          </Panel>
          <Panel title="Disputes awaiting review" to="/admin/disputes" empty={disputes.data ? 'No disputes are waiting.' : 'Disputes appear here once you are signed in as an admin.'}>
            {disputes.data && disputes.data.content.length > 0 && (
              <ul className="timeline">
                {disputes.data.content.map((s) => (
                  <li key={s.id}><Badge tone="red">disputed</Badge><span>{s.topic}</span><time dateTime={s.createdAt}>{new Date(s.createdAt).toLocaleString()}</time></li>
                ))}
              </ul>
            )}
          </Panel>
          <section className="card pad admin-panel wide">
            <div className="admin-panel-head"><h3 className="h3">Recent administrative activity</h3></div>
            <p className="muted">The backend does not keep an audit trail yet, so there is nothing to show here.</p>
          </section>
        </div>
      </section>
    </>
  )
}
