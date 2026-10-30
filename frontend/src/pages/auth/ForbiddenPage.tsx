import { ArrowLeft, ShieldAlert } from 'lucide-react'
import { Link } from 'react-router-dom'

export default function ForbiddenPage() {
  return (
    <main className="not-found">
      <div className="not-found-card">
        <span className="not-found-icon" aria-hidden="true">
          <ShieldAlert size={24} />
        </span>
        <p className="eyebrow">403 · access denied</p>
        <h1>You don&apos;t have access to this page.</h1>
        <p className="not-found-description">This area is limited to administrators. If you think this is a mistake, contact your platform admin.</p>
        <Link to="/dashboard" className="button button--primary">
          <ArrowLeft size={16} aria-hidden="true" />
          Back to dashboard
        </Link>
      </div>
    </main>
  )
}
