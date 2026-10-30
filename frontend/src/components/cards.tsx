import type { CSSProperties, ReactNode } from 'react'
import { ArrowRight, type LucideIcon } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Badge } from './ui'

export type Tone = 'violet' | 'blue' | 'green' | 'amber'

/** A card that navigates. The whole card is one link, so keyboard and screen-reader users get one clear target. */
export function LinkCard({ to, icon: Icon, title, text, tone = 'violet', cta = 'Open', meta, index = 0, compact, children }: { to: string; icon: LucideIcon; title: string; text: string; tone?: Tone; cta?: string; meta?: string[]; index?: number; compact?: boolean; children?: ReactNode }) {
  return (
    <Link to={to} className={`card card--link edge-${tone} enter ${compact ? 'card--compact' : ''}`} style={{ '--i': index } as CSSProperties}>
      <span className={`feature-icon lc-icon tone-${tone}`} aria-hidden="true">
        <Icon size={21} strokeWidth={1.8} />
      </span>
      <h3>{title}</h3>
      <p>{text}</p>
      {children}
      {meta && (
        <span className="lc-meta">
          {meta.map((m) => (
            <Badge key={m} tone="neutral">
              {m}
            </Badge>
          ))}
        </span>
      )}
      <span className="lc-cta">
        {cta} <ArrowRight size={15} aria-hidden="true" />
      </span>
    </Link>
  )
}

/** Compact heading used above card groups. */
export function SectionHead({ title, hint, action }: { title: string; hint?: string; action?: ReactNode }) {
  return (
    <div className="section-head">
      <div>
        <h2 className="h3">{title}</h2>
        {hint && <p className="muted">{hint}</p>}
      </div>
      {action}
    </div>
  )
}
