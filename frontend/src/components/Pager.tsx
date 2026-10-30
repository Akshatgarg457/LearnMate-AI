import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Btn } from './ui'

export default function Pager({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null
  return (
    <nav className="pager" aria-label="Pagination">
      <Btn variant="secondary" icon={ChevronLeft} disabled={page <= 0} onClick={() => onChange(page - 1)}>
        Previous
      </Btn>
      <span>
        Page {page + 1} of {totalPages}
      </span>
      <Btn variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        Next <ChevronRight size={16} aria-hidden="true" />
      </Btn>
    </nav>
  )
}
