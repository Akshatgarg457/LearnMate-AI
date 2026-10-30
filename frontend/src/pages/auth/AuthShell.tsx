import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Sparkles } from 'lucide-react'
import ThemeToggle from '../../theme/ThemeToggle'

export default function AuthShell({ title, subtitle, children, footer, className = '' }: { title: string; subtitle: string; children: ReactNode; footer: ReactNode; className?: string }) {
  return (
    <main className="auth-page">
      <div className="auth-glow" aria-hidden="true" />
      <ThemeToggle />
      <div className={`auth-card card enter ${className}`.trim()}>
        <Link to="/" className="brand" aria-label="LearnMate AI home">
          <span className="brand-mark" aria-hidden="true">
            <Sparkles size={19} strokeWidth={2.2} />
          </span>
          <span>
            LearnMate <span className="brand-ai">AI</span>
          </span>
        </Link>
        <h1>{title}</h1>
        <p className="auth-sub">{subtitle}</p>
        {children}
        <div className="auth-foot">{footer}</div>
      </div>
    </main>
  )
}
