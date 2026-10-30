import { ArrowLeft, CircleHelp } from 'lucide-react'
import { Link } from 'react-router-dom'

function NotFoundPage() {
  return (
    <main className="not-found">
      <div className="not-found-card">
        <span className="not-found-icon" aria-hidden="true">
          <CircleHelp size={24} />
        </span>
        <p className="eyebrow">404 · page not found</p>
        <h1>This page is off the study path.</h1>
        <p className="not-found-description">
          The link may be outdated, or the page may not exist yet.
        </p>
        <Link to="/" className="button button--primary">
          <ArrowLeft size={16} aria-hidden="true" />
          Back to LearnMate
        </Link>
      </div>
    </main>
  )
}

export default NotFoundPage
