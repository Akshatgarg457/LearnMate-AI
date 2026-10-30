import { useMemo, useState, type ReactNode } from 'react'
import { Search, SlidersHorizontal, type LucideIcon } from 'lucide-react'
import Pager from '../../../components/Pager'
import ResourceView from '../../../components/ResourceView'
import { Badge, Btn, Modal, SelectField } from '../../../components/ui'
import { useLive } from '../../../hooks/useLive'
import { useResource } from '../../../hooks/useResource'
import type { Page } from '../../../lib/types'
import { describeError } from '../../../services/api'
import { useToast } from '../../../toast/toastContext'
import { statusTone } from './format'

export type Column<T> = { key: string; label: string; render: (row: T) => ReactNode; text?: (row: T) => string }
export type RowAction<T> = { label: string; danger?: boolean; show?: (row: T) => boolean; confirm: (row: T) => string; run: (row: T) => Promise<unknown> }

type Props<T extends { id: number }> = {
  endpoint: string
  params?: Record<string, string | undefined>
  columns: Column<T>[]
  actions?: RowAction<T>[]
  searchLabel: string
  emptyTitle: string
  icon: LucideIcon
  statusOf?: (row: T) => string | undefined
  cards?: (row: T, actions: ReactNode) => ReactNode
  note?: string
}

export const StatusBadge = ({ value }: { value: string }) => <Badge tone={statusTone(value)}>{value.replace(/_/g, ' ').toLowerCase()}</Badge>

/** One server-paged admin list: search and status filter (applied to the loaded page), plus confirmed row actions. */
export default function AdminTable<T extends { id: number }>({ endpoint, params, columns, actions = [], searchLabel, emptyTitle, icon, statusOf, cards, note }: Props<T>) {
  const live = useLive()
  const toast = useToast()
  const [page, setPage] = useState(0)
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('')
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [pending, setPending] = useState<{ action: RowAction<T>; row: T } | null>(null)
  const [busy, setBusy] = useState(false)
  const res = useResource<Page<T>>(live ? endpoint : null, { ...params, page: String(page), size: '25' })
  const id = `admin-${endpoint.replace(/\W/g, '')}`

  const statuses = useMemo(() => (statusOf ? [...new Set((res.data?.content ?? []).map(statusOf).filter((s): s is string => !!s))] : []), [res.data, statusOf])
  const visible = useMemo(() => {
    const q = query.trim().toLowerCase()
    return (res.data?.content ?? []).filter((r) => (!status || statusOf?.(r) === status) && (!q || columns.some((c) => (c.text?.(r) ?? '').toLowerCase().includes(q))))
  }, [res.data, query, status, columns, statusOf])

  const confirm = async () => {
    if (!pending || busy) return
    setBusy(true)
    try {
      await pending.action.run(pending.row)
      toast(`${pending.action.label}: the backend accepted the request.`, 'success')
      setPending(null)
      res.reload()
    } catch (err) {
      toast(describeError(err).message, 'error')
    } finally {
      setBusy(false)
    }
  }

  const rowButtons = (row: T) => actions.filter((a) => !a.show || a.show(row)).map((a) => (
    <Btn key={a.label} variant={a.danger ? 'danger' : 'secondary'} onClick={() => setPending({ action: a, row })}>{a.label}</Btn>
  ))

  return (
    <>
      {note && <p className="muted fine">{note}</p>}
      <ResourceView resource={res} endpoint={`GET ${endpoint}`} idleHint="With a real admin account, records from the backend are listed here." isEmpty={(d) => d.content.length === 0} emptyIcon={icon} emptyTitle={emptyTitle}>
        {(d) => (
          <>
            <div className="admin-toolbar">
              <div className="field search-field">
                <label htmlFor={`${id}-search`}>{searchLabel}</label>
                <div className="input-icon"><Search size={15} aria-hidden="true" /><input id={`${id}-search`} value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Filter this page" /></div>
              </div>
              {statuses.length > 0 && (
                <Btn variant="secondary" icon={SlidersHorizontal} aria-expanded={filtersOpen} aria-controls={`${id}-filters`} onClick={() => setFiltersOpen((o) => !o)}>Filters{status ? ' (1)' : ''}</Btn>
              )}
            </div>
            <div id={`${id}-filters`} className={`filter-panel ${filtersOpen ? 'is-open' : ''}`} inert={!filtersOpen}>
              <div>
                <SelectField label="Status" value={status} onChange={(e) => setStatus(e.target.value)}>
                  <option value="">All statuses</option>
                  {statuses.map((s) => (<option key={s} value={s}>{s.replace(/_/g, ' ').toLowerCase()}</option>))}
                </SelectField>
              </div>
            </div>
            <p className="muted fine" role="status" aria-live="polite">Showing {visible.length} of {d.content.length} on this page ({d.totalElements} in total)</p>

            {visible.length === 0 ? (
              <p className="card pad muted">Nothing on this page matches your search or filters.</p>
            ) : cards ? (
              <div className="feature-grid three">{visible.map((r) => (<div key={r.id}>{cards(r, <div className="row">{rowButtons(r)}</div>)}</div>))}</div>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead><tr>{columns.map((c) => (<th key={c.key} scope="col">{c.label}</th>))}{actions.length > 0 && <th scope="col">Actions</th>}</tr></thead>
                  <tbody>
                    {visible.map((r) => (
                      <tr key={r.id}>
                        {columns.map((c) => (<td key={c.key}>{c.render(r)}</td>))}
                        {actions.length > 0 && <td><div className="row">{rowButtons(r)}</div></td>}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <Pager page={d.number} totalPages={d.totalPages} onChange={setPage} />
          </>
        )}
      </ResourceView>

      <Modal
        open={!!pending}
        onClose={() => setPending(null)}
        title={pending ? `${pending.action.label}?` : ''}
        footer={<><Btn variant="secondary" onClick={() => setPending(null)}>Cancel</Btn><Btn variant={pending?.action.danger ? 'danger' : 'primary'} loading={busy} onClick={() => void confirm()}>{pending?.action.label}</Btn></>}
      >
        <p>{pending?.action.confirm(pending.row)}</p>
        <p className="muted">The backend performs and records this action. Nothing changes here until it confirms.</p>
      </Modal>
    </>
  )
}
