export type Tone = 'neutral' | 'violet' | 'blue' | 'green' | 'amber' | 'red'

/** Maps a backend status word to a badge colour. The word itself is always shown too (never colour alone). */
export function statusTone(status: string): Tone {
  const s = status.toUpperCase()
  if (['ACTIVE', 'APPROVED', 'PUBLISHED', 'RESOLVED', 'VERIFIED', 'COMPLETED', 'OK'].includes(s)) return 'green'
  if (['PENDING', 'OPEN', 'IN_REVIEW', 'PENDING_REVIEW', 'DRAFT'].includes(s)) return 'amber'
  if (['SUSPENDED', 'REJECTED', 'DISPUTED', 'FAILED', 'REMOVED'].includes(s)) return 'red'
  return 'neutral'
}

export const prettyKey = (k: string) => k.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/_/g, ' ').toLowerCase()

export function formatCell(key: string, value: string | number | boolean): string {
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'
  if (typeof value === 'string' && /(At|Date|Time)$/.test(key)) {
    const d = new Date(value)
    if (!Number.isNaN(d.getTime())) return d.toLocaleString()
  }
  return String(value)
}
