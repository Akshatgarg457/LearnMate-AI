import { Award, Flame, Gift, Target } from 'lucide-react'
import { Heatmap, WeeklyBars } from '../../components/charts'
import ResourceView from '../../components/ResourceView'
import { Badge, PageHeader, ProgressRing, StatCard } from '../../components/ui'
import { useLive } from '../../hooks/useLive'
import { useResource } from '../../hooks/useResource'
import { lastDaysXp, prettyEnum, XP_PER_LEVEL } from '../../lib/dates'
import type { ActivityDay, LearningActivity, Page, UserAchievement, UserProgress } from '../../lib/types'

export default function ProgressPage() {
  const live = useLive()
  const summary = useResource<UserProgress>(live ? '/progress/summary' : null)
  const activity = useResource<ActivityDay[]>(live ? '/progress/activity' : null)
  const achievements = useResource<UserAchievement[]>(live ? '/progress/achievements' : null)
  const balance = useResource<{ balance: number }>(live ? '/rewards/balance' : null)
  const history = useResource<Page<LearningActivity>>(live ? '/progress/history' : null, { size: '10' })

  return (
    <>
      <PageHeader
        eyebrow="Progress Centre"
        title="Your streaks, XP and activity"
        description="Opening the site does not count. Only qualifying activity does, and the server decides official dates and streaks using your timezone."
      />

      <ResourceView resource={summary} endpoint="GET /progress/summary" emptyIcon={Target} emptyTitle="No progress yet">
        {(o) => (
          <>
            <div className="two-col">
              <section className="card pad enter" style={{ display: 'flex', alignItems: 'center', gap: 22 }}>
                <ProgressRing value={XP_PER_LEVEL - o.nextLevelXp} max={XP_PER_LEVEL} size={120} label="XP progress to next level">
                  <span><small className="muted">Level</small><br />{o.level}</span>
                </ProgressRing>
                <div>
                  <h2 className="h3">{o.totalXp.toLocaleString()} XP</h2>
                  <p className="muted">{o.nextLevelXp} XP to level {o.level + 1}. XP shows learning progression; it is not spendable.</p>
                </div>
              </section>
              <div className="stat-grid" style={{ gridTemplateColumns: '1fr' }}>
                <StatCard index={0} icon={Flame} tone="amber" label="Current streak" value={o.currentStreak} unit="days" hint={`Longest: ${o.longestStreak} days`} />
                <StatCard index={1} icon={Gift} tone="green" label="Redeemable points" value={balance.data ? balance.data.balance : '\u2014'} hint="Separate from XP; spend in the Rewards Store" />
              </div>
            </div>

            {activity.data && (
              <section className="card pad section">
                <h2 className="h3">XP this week</h2>
                <WeeklyBars values={lastDaysXp(activity.data, 7)} label="XP earned" />
              </section>
            )}

            <section className="section">
              <h2 className="h3">Achievements</h2>
              {achievements.data && achievements.data.length > 0 ? (
                <div className="badge-grid">
                  {achievements.data.map((a) => (
                    <div key={a.id} className="card pad achievement is-unlocked">
                      <Award size={20} aria-hidden="true" />
                      <strong>{a.achievement.title}</strong>
                      <p className="muted">{a.achievement.description}</p>
                      <Badge tone="green">Earned {new Date(a.earnedAt).toLocaleDateString()}</Badge>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="card pad muted">No achievements earned yet. Keep practising; they appear here the moment you earn them. (The backend lists earned achievements only, so locked ones aren&apos;t shown.)</p>
              )}
            </section>

            <section className="section">
              <h2 className="h3">Recent activity</h2>
              <div className="card pad">
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
                  <p className="muted">No recorded activity yet.</p>
                )}
              </div>
            </section>
          </>
        )}
      </ResourceView>

      <section className="section">
        <h2 className="h3">Activity heatmap (XP per day)</h2>
        <div className="card pad">
          <ResourceView resource={activity} endpoint="GET /progress/activity" isEmpty={(a) => a.length === 0} emptyIcon={Target} emptyTitle="No activity recorded" emptyText="Your calendar fills in as you complete qualifying activity.">
            {(a) => <Heatmap days={a} />}
          </ResourceView>
        </div>
      </section>
    </>
  )
}
