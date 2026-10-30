import { useMemo, useState, type KeyboardEvent } from 'react'
import type { ActivityDay } from '../lib/types'

/** Short weekday name for the day `daysAgo` days before today. */
function dayLabel(daysAgo: number) {
  const d = new Date()
  d.setDate(d.getDate() - daysAgo)
  return d.toLocaleDateString(undefined, { weekday: 'short' })
}

/** Bars for the last N days ending today (oldest first). Values come from the backend. */
export function WeeklyBars({ values, label = 'XP earned' }: { values: number[]; label?: string }) {
  const max = Math.max(1, ...values)
  return (
    <div className="bars" role="img" aria-label={`${label} for the last ${values.length} days: ${values.join(', ')}`}>
      {values.map((m, i) => (
        <div className="bar-col" key={i}>
          <span className="bar-value">{m}</span>
          <span className="bar" style={{ height: `${(m / max) * 100}%`, '--i': i } as React.CSSProperties} />
          <span className="bar-label">{dayLabel(values.length - 1 - i)}</span>
        </div>
      ))}
    </div>
  )
}

function level(xp: number) {
  if (xp <= 0) return 0
  if (xp < 10) return 1
  if (xp < 25) return 2
  if (xp < 50) return 3
  return 4
}

const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

/**
 * Calendar heatmap. Only dates present in `days` get a level; nothing is invented.
 * Keyboard: arrow keys move between days (left/right = week, up/down = day).
 */
export function Heatmap({ days, weeks = 26 }: { days: ActivityDay[]; weeks?: number }) {
  const byDate = useMemo(() => new Map(days.map((d) => [d.date, d])), [days])
  const cells = useMemo(() => {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const start = new Date(today)
    start.setDate(start.getDate() - ((start.getDay() + 6) % 7) - (weeks - 1) * 7) // Monday of first week
    return Array.from({ length: weeks * 7 }, (_, i) => {
      const d = new Date(start)
      d.setDate(start.getDate() + i)
      return { date: iso(d), future: d > today }
    })
  }, [weeks])
  const [active, setActive] = useState(() => cells.filter((c) => !c.future).length - 1)
  const selected = cells[active]
  const detail = byDate.get(selected.date)

  const onKey = (e: KeyboardEvent<HTMLDivElement>) => {
    const step = { ArrowLeft: -7, ArrowRight: 7, ArrowUp: -1, ArrowDown: 1 }[e.key]
    if (!step) return
    e.preventDefault()
    const next = active + step
    if (next < 0 || next >= cells.length || cells[next].future) return
    setActive(next)
    e.currentTarget.querySelector<HTMLElement>(`[data-idx="${next}"]`)?.focus()
  }

  return (
    <div>
      <div className="heatmap-scroll">
        <div className="heatmap" role="group" aria-label="Activity heatmap. Use arrow keys to move between days." onKeyDown={onKey}>
          {cells.map((c, i) =>
            c.future ? (
              <span key={c.date} className="heat-cell heat-future" aria-hidden="true" />
            ) : (
              <button
                key={c.date}
                type="button"
                data-idx={i}
                tabIndex={i === active ? 0 : -1}
                className={`heat-cell heat-${level(byDate.get(c.date)?.xp ?? 0)} ${i === active ? 'is-selected' : ''}`}
                aria-label={`${c.date}: ${byDate.get(c.date) ? `${byDate.get(c.date)!.xp} XP` : 'no activity recorded'}`}
                title={`${c.date}: ${byDate.get(c.date) ? `${byDate.get(c.date)!.xp} XP` : 'no activity recorded'}`}
                onClick={() => setActive(i)}
                onFocus={() => setActive(i)}
              />
            ),
          )}
        </div>
      </div>
      <div className="heat-legend" aria-hidden="true">
        Less {[0, 1, 2, 3, 4].map((l) => <span key={l} className={`heat-cell heat-${l}`} />)} More
      </div>
      <p className="heat-detail" aria-live="polite">
        <strong>{selected.date}</strong>{' '}
        {detail
          ? `${detail.xp} XP earned.`
          : 'No activity recorded for this day.'}
      </p>
    </div>
  )
}
