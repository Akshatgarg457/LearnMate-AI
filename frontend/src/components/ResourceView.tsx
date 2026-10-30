import type { ReactNode } from 'react'
import { CloudOff, Inbox, RefreshCw, ShieldAlert, type LucideIcon } from 'lucide-react'
import type { ResourceState } from '../hooks/useResource'
import { Btn, EmptyState, Skeleton } from './ui'

/** Honest banner: says which backend endpoint a screen is waiting for. */
export function ApiNotice({ endpoint, message, onRetry }: { endpoint: string; message: string; onRetry?: () => void }) {
  return (
    <div className="api-notice" role="status">
      <CloudOff size={18} aria-hidden="true" />
      <div>
        <strong>Not connected yet</strong>
        <p>{message}</p>
        <code>{endpoint}</code>
      </div>
      {onRetry && (
        <Btn variant="secondary" icon={RefreshCw} onClick={onRetry}>
          Try again
        </Btn>
      )}
    </div>
  )
}

/**
 * Renders a backend resource with loading, error, empty and ready states.
 * Nothing is invented: if the endpoint is missing the user sees that, not sample data.
 */
export default function ResourceView<T>({
  resource,
  endpoint,
  isEmpty,
  emptyIcon,
  emptyTitle,
  emptyText,
  idleHint,
  children,
}: {
  resource: ResourceState<T>
  endpoint: string
  isEmpty?: (data: T) => boolean
  emptyIcon: LucideIcon
  emptyTitle: string
  emptyText?: string
  /** Extra sentence describing what this area will show once a request is made. */
  idleHint?: string
  children: (data: T) => ReactNode
}) {
  if (resource.status === 'idle') {
    return (
      <EmptyState icon={Inbox} title="Nothing to load yet">
        {idleHint ?? 'This area has no data to request right now.'}
      </EmptyState>
    )
  }
  if (resource.status === 'loading' && !resource.data) {
    return (
      <div className="card pad" aria-busy="true">
        <Skeleton lines={4} />
      </div>
    )
  }
  if (resource.status === 'error') {
    const p = resource.problem
    if (p?.kind === 'forbidden') {
      return (
        <EmptyState icon={ShieldAlert} title="You don't have access to this">
          {p.message}
        </EmptyState>
      )
    }
    return <ApiNotice endpoint={endpoint} message={p?.message ?? 'Request failed.'} onRetry={resource.reload} />
  }
  if (resource.data === undefined) return null
  if (isEmpty?.(resource.data)) {
    return (
      <EmptyState icon={emptyIcon} title={emptyTitle}>
        {emptyText}
      </EmptyState>
    )
  }
  return <>{children(resource.data)}</>
}
