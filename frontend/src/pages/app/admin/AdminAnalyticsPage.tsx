import { BookOpen, ChartColumn, ClipboardList, Flag, Scale, ShieldCheck, Users } from 'lucide-react'
import { PageHeader, StatCard } from '../../../components/ui'
import { useLive } from '../../../hooks/useLive'
import { useResource } from '../../../hooks/useResource'
import type { ContentReport, Course, MockExam, Page, PeerSession, Quiz, UserProfile } from '../../../lib/types'

const dash = '\u2014'

/** Platform figures the backend can report today, counted from its admin lists. */
export default function AdminAnalyticsPage() {
  const live = useLive()
  const users = useResource<Page<UserProfile>>(live ? '/admin/users' : null, { size: '100' })
  const courses = useResource<Page<Course>>(live ? '/admin/courses' : null, { size: '100' })
  const quizzes = useResource<Page<Quiz>>(live ? '/admin/quizzes' : null, { size: '100' })
  const mocks = useResource<Page<MockExam>>(live ? '/admin/mock-exams' : null, { size: '100' })
  const pending = useResource<Page<ContentReport>>(live ? '/admin/reports' : null, { status: 'PENDING', size: '1' })
  const disputes = useResource<Page<PeerSession>>(live ? '/admin/disputes' : null, { size: '1' })

  const u = users.data
  const capped = !!u && u.totalPages > 1
  const count = (rows: { active?: boolean }[] | undefined, active: boolean) => (rows ? rows.filter((r) => !!r.active === active).length : undefined)
  const note = (d?: Page<unknown>) => (d && d.totalPages > 1 ? ' (first 100)' : '')

  return (
    <>
      <PageHeader eyebrow="Admin" title="Platform Analytics" description="Counts the backend can report today. It does not provide usage trends or time series yet." />
      <div className="stat-grid">
        <StatCard index={0} icon={Users} tone="violet" label="Registered users" value={u?.totalElements ?? dash} />
        <StatCard index={1} icon={ShieldCheck} tone="blue" label={`Administrators${capped ? ' (first 100 users)' : ''}`} value={u ? u.content.filter((x) => x.role === 'ADMIN').length : dash} />
        <StatCard index={2} icon={Users} tone="amber" label={`Suspended accounts${capped ? ' (first 100 users)' : ''}`} value={u ? u.content.filter((x) => x.status === 'SUSPENDED').length : dash} />
        <StatCard index={3} icon={BookOpen} tone="green" label={`Published courses${note(courses.data)}`} value={count(courses.data?.content, true) ?? dash} hint={courses.data ? `${count(courses.data.content, false)} unpublished` : undefined} />
        <StatCard index={4} icon={ClipboardList} tone="green" label={`Active quizzes${note(quizzes.data)}`} value={count(quizzes.data?.content, true) ?? dash} hint={quizzes.data ? `${count(quizzes.data.content, false)} inactive` : undefined} />
        <StatCard index={5} icon={ChartColumn} tone="blue" label={`Active mock exams${note(mocks.data)}`} value={count(mocks.data?.content, true) ?? dash} hint={mocks.data ? `${count(mocks.data.content, false)} inactive` : undefined} />
        <StatCard index={6} icon={Flag} tone="amber" label="Pending reports" value={pending.data?.totalElements ?? dash} />
        <StatCard index={7} icon={Scale} tone="amber" label="Disputed sessions" value={disputes.data?.totalElements ?? dash} />
      </div>
    </>
  )
}
