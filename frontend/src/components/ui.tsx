import {
  useEffect,
  useId,
  useState,
  useRef,
  type ButtonHTMLAttributes,
  type InputHTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react'
import { LoaderCircle, X, type LucideIcon } from 'lucide-react'

/* ---------- Buttons ---------- */
type BtnProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
  loading?: boolean
  icon?: LucideIcon
}

export function Btn({ variant = 'primary', loading, icon: Icon, children, className = '', disabled, type = 'button', ...rest }: BtnProps) {
  return (
    <button
      type={type}
      className={`button button--${variant} ${className}`.trim()}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading ? <LoaderCircle className="spin" size={16} aria-hidden="true" /> : Icon ? <Icon size={16} aria-hidden="true" /> : null}
      {children}
    </button>
  )
}

/* ---------- Layout bits ---------- */
export function PageHeader({ title, description, actions, eyebrow }: { title: string; description?: string; actions?: ReactNode; eyebrow?: string }) {
  return (
    <header className="page-header">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {actions && <div className="page-header-actions">{actions}</div>}
    </header>
  )
}

export function EmptyState({ icon: Icon, title, children, action }: { icon: LucideIcon; title: string; children?: ReactNode; action?: ReactNode }) {
  return (
    <div className="empty-state">
      <span className="empty-icon" aria-hidden="true">
        <Icon size={22} />
      </span>
      <h3>{title}</h3>
      {children && <p>{children}</p>}
      {action}
    </div>
  )
}

export function Badge({ tone = 'neutral', children }: { tone?: 'neutral' | 'violet' | 'blue' | 'green' | 'amber' | 'red'; children: ReactNode }) {
  return <span className={`badge badge--${tone}`}>{children}</span>
}

export function ProgressBar({ value, max = 100, label, tone = 'violet' }: { value: number; max?: number; label: string; tone?: 'violet' | 'green' | 'amber' | 'teal' }) {
  const pct = max > 0 ? Math.min(100, Math.max(0, (value / max) * 100)) : 0
  return (
    <div
      className={`progress progress--${tone}`}
      role="progressbar"
      aria-label={label}
      aria-valuenow={Math.round(pct)}
      aria-valuemin={0}
      aria-valuemax={100}
    >
      <span style={{ width: `${pct}%` }} />
    </div>
  )
}

export function Skeleton({ lines = 3 }: { lines?: number }) {
  return (
    <div className="skeleton-block" aria-hidden="true">
      {Array.from({ length: lines }, (_, i) => (
        <span className="skeleton" key={i} style={{ width: `${100 - i * 14}%` }} />
      ))}
    </div>
  )
}

/** Counts up to `value` when it first appears. Jumps straight to it when reduced motion is requested. */
export function AnimatedNumber({ value, durationMs = 800 }: { value: number; durationMs?: number }) {
  const [shown, setShown] = useState(0)
  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || value === 0) {
      setShown(value)
      return
    }
    let frame = 0
    const start = performance.now()
    const tick = (now: number) => {
      const t = Math.min(1, (now - start) / durationMs)
      setShown(Math.round(value * (1 - Math.pow(1 - t, 3))))
      if (t < 1) frame = requestAnimationFrame(tick)
    }
    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [value, durationMs])
  return <span className="num">{shown.toLocaleString()}</span>
}

export function StatCard({ icon: Icon, label, value, unit, hint, tone = 'violet', index = 0 }: { icon: LucideIcon; label: string; value: ReactNode; unit?: string; hint?: string; tone?: 'violet' | 'blue' | 'green' | 'amber'; index?: number }) {
  const unavailable = value === '\u2014'
  return (
    <div className={`card card--metric edge-${tone} enter ${unavailable ? 'is-unavailable' : ''}`} style={{ '--i': index } as React.CSSProperties}>
      <span className={`feature-icon tone-${tone}`} aria-hidden="true">
        <Icon size={18} strokeWidth={1.8} />
      </span>
      <div>
        <p className="stat-label">{label}</p>
        <p className="stat-value">
          {unavailable ? (
            <>
              <span aria-hidden="true">{'\u2014'}</span>
              <span className="sr-only">Not available</span>
            </>
          ) : typeof value === 'number' ? (
            <AnimatedNumber value={value} />
          ) : (
            value
          )}
          {unit && !unavailable && <span className="stat-unit"> {unit}</span>}
        </p>
        {hint && <p className="stat-hint">{hint}</p>}
      </div>
    </div>
  )
}

/** Circular progress. Animates from empty when it mounts. */
export function ProgressRing({ value, max, size = 104, stroke = 9, label, tone = 'violet', children }: { value: number; max: number; size?: number; stroke?: number; label: string; tone?: 'violet' | 'green' | 'amber' | 'teal'; children?: ReactNode }) {
  const [ready, setReady] = useState(false)
  useEffect(() => {
    const id = requestAnimationFrame(() => setReady(true))
    return () => cancelAnimationFrame(id)
  }, [])
  const r = (size - stroke) / 2
  const c = 2 * Math.PI * r
  const pct = max > 0 ? Math.min(1, Math.max(0, value / max)) : 0
  return (
    <div className={`ring ring--${tone}`} style={{ width: size, height: size }} role="progressbar" aria-label={label} aria-valuenow={Math.round(pct * 100)} aria-valuemin={0} aria-valuemax={100}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} aria-hidden="true">
        <circle className="ring-track" cx={size / 2} cy={size / 2} r={r} strokeWidth={stroke} fill="none" />
        <circle className="ring-bar" cx={size / 2} cy={size / 2} r={r} strokeWidth={stroke} fill="none" strokeLinecap="round" strokeDasharray={c} strokeDashoffset={ready ? c * (1 - pct) : c} transform={`rotate(-90 ${size / 2} ${size / 2})`} />
      </svg>
      <div className="ring-center">{children}</div>
    </div>
  )
}

/* ---------- Forms ---------- */
type FieldShell = { label: string; error?: string; hint?: string }

export function Field({ label, error, hint, ...input }: FieldShell & InputHTMLAttributes<HTMLInputElement>) {
  const id = useId()
  return (
    <div className={`field ${error ? 'field--error' : ''}`}>
      <label htmlFor={id}>{label}</label>
      <input id={id} aria-invalid={!!error} aria-describedby={error ? `${id}-e` : hint ? `${id}-h` : undefined} {...input} />
      {hint && !error && <small id={`${id}-h`}>{hint}</small>}
      {error && (
        <small id={`${id}-e`} role="alert">
          {error}
        </small>
      )}
    </div>
  )
}

export function SelectField({ label, error, children, ...select }: FieldShell & SelectHTMLAttributes<HTMLSelectElement>) {
  const id = useId()
  return (
    <div className={`field ${error ? 'field--error' : ''}`}>
      <label htmlFor={id}>{label}</label>
      <select id={id} aria-invalid={!!error} {...select}>
        {children}
      </select>
      {error && <small role="alert">{error}</small>}
    </div>
  )
}

export function TextAreaField({ label, error, hint, ...area }: FieldShell & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const id = useId()
  return (
    <div className={`field ${error ? 'field--error' : ''}`}>
      <label htmlFor={id}>{label}</label>
      <textarea id={id} aria-invalid={!!error} {...area} />
      {hint && !error && <small>{hint}</small>}
      {error && <small role="alert">{error}</small>}
    </div>
  )
}

/* ---------- Tabs (arrow-key navigable) ---------- */
export function Tabs<T extends string>({ tabs, value, onChange, label }: { tabs: { id: T; label: string }[]; value: T; onChange: (id: T) => void; label: string }) {
  const onKey = (e: KeyboardEvent<HTMLDivElement>) => {
    if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') return
    const idx = tabs.findIndex((t) => t.id === value)
    const next = tabs[(idx + (e.key === 'ArrowRight' ? 1 : -1) + tabs.length) % tabs.length]
    onChange(next.id)
    e.currentTarget.querySelector<HTMLButtonElement>(`[data-tab="${next.id}"]`)?.focus()
  }
  return (
    <div className="tabs" role="tablist" aria-label={label} onKeyDown={onKey}>
      {tabs.map((t) => (
        <button
          key={t.id}
          type="button"
          role="tab"
          data-tab={t.id}
          aria-selected={t.id === value}
          tabIndex={t.id === value ? 0 : -1}
          className={t.id === value ? 'is-active' : ''}
          onClick={() => onChange(t.id)}
        >
          {t.label}
        </button>
      ))}
    </div>
  )
}

/* ---------- Modal (native <dialog>: focus trap, Esc to close) ---------- */
export function Modal({ open, onClose, title, children, footer }: { open: boolean; onClose: () => void; title: string; children: ReactNode; footer?: ReactNode }) {
  const ref = useRef<HTMLDialogElement>(null)
  const lastTitle = useRef(title)
  if (title) lastTitle.current = title
  // Keep the body mounted while the dialog fades out so it doesn't go blank mid-animation.
  const [mounted, setMounted] = useState(open)
  useEffect(() => {
    if (open) {
      setMounted(true)
      return
    }
    const id = window.setTimeout(() => setMounted(false), 280)
    return () => window.clearTimeout(id)
  }, [open])
  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])
  return (
    <dialog ref={ref} className="modal" onClose={onClose} onClick={(e) => e.target === ref.current && onClose()}>
      <div className="modal-body">
        <div className="modal-head">
          <h2>{lastTitle.current}</h2>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close dialog">
            <X size={17} aria-hidden="true" />
          </button>
        </div>
        {(open || mounted) && children}
        {footer && <div className="modal-foot">{footer}</div>}
      </div>
    </dialog>
  )
}
