import type { ActivityDay } from './types'

export const isoDate = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

export function daysAgo(n: number): Date {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return d
}

/** XP per day for the last `n` days ending today (oldest first). A day with no recorded activity is genuinely 0. */
export function lastDaysXp(days: ActivityDay[], n: number): number[] {
  const byDate = new Map(days.map((d) => [d.date, d.xp]))
  return Array.from({ length: n }, (_, i) => byDate.get(isoDate(daysAgo(n - 1 - i))) ?? 0)
}

export const prettyEnum = (s: string) => s.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase())

/** Backend XP_PER_LEVEL: the level is 1 + totalXp / 100, so each level spans 100 XP. */
export const XP_PER_LEVEL = 100
