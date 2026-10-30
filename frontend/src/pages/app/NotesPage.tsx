import { useState, type FormEvent } from 'react'
import { Download, FileText, NotebookPen, Search, Trash2, Upload } from 'lucide-react'
import { useAuth } from '../../auth/authContext'
import Pager from '../../components/Pager'
import ResourceView from '../../components/ResourceView'
import { Badge, Btn, Field, Modal, PageHeader, ProgressBar, Tabs, TextAreaField } from '../../components/ui'
import { useDebounced } from '../../hooks/useDebounced'
import { useLive } from '../../hooks/useLive'
import { useResource } from '../../hooks/useResource'
import type { Note, Page } from '../../lib/types'
import api, { describeError } from '../../services/api'
import { useToast } from '../../toast/toastContext'

// Usability checks only. The backend enforces the real rules (these match its allow-list and 10 MB limit).
const MAX_MB = 10
const ALLOWED = ['pdf', 'doc', 'docx', 'txt', 'md', 'png', 'jpg', 'jpeg']
const kb = (n: number) => (n >= 1048576 ? `${(n / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.round(n / 1024))} KB`)

export default function NotesPage() {
  const live = useLive()
  const toast = useToast()
  const { session } = useAuth()
  const [tab, setTab] = useState<'library' | 'mine'>('library')
  const [search, setSearch] = useState('')
  const [subject, setSubject] = useState('')
  const [page, setPage] = useState(0)
  const q = useDebounced(search)
  const notes = useResource<Page<Note>>(live ? (tab === 'library' ? '/notes' : '/notes/my-notes') : null, tab === 'library' ? { q, subject, page: String(page), size: '12' } : { page: String(page), size: '12' })

  const [open, setOpen] = useState(false)
  const [meta, setMeta] = useState({ title: '', subject: '', topic: '', description: '', isPublic: false })
  const [file, setFile] = useState<File | null>(null)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [progress, setProgress] = useState<number | null>(null)
  const [deleting, setDeleting] = useState<Note | null>(null)

  const pick = (f: File | null) => {
    setFile(f)
    if (!f) return
    const ext = f.name.split('.').pop()?.toLowerCase() ?? ''
    setErrors((e) => ({ ...e, file: !ALLOWED.includes(ext) ? `Unsupported file type. Allowed: ${ALLOWED.join(', ')}.` : f.size > MAX_MB * 1024 * 1024 ? `File is larger than ${MAX_MB} MB.` : '' }))
  }

  const upload = async (e: FormEvent) => {
    e.preventDefault()
    if (progress !== null) return
    const next: Record<string, string> = { title: meta.title.trim() ? '' : 'Add a title.', file: !file ? 'Choose a file.' : errors.file ?? '' }
    setErrors(next)
    if (Object.values(next).some(Boolean) || !file) return
    const data = new FormData()
    data.append('file', file)
    data.append('title', meta.title.trim())
    if (meta.subject.trim()) data.append('subject', meta.subject.trim())
    if (meta.topic.trim()) data.append('topic', meta.topic.trim())
    if (meta.description.trim()) data.append('description', meta.description.trim())
    data.append('isPublic', String(meta.isPublic))
    setProgress(0)
    try {
      await api.post('/notes', data, { headers: { 'Content-Type': 'multipart/form-data' }, onUploadProgress: (p) => setProgress(Math.round((p.loaded / (p.total ?? file.size)) * 100)) })
      toast(meta.isPublic ? 'Note uploaded and shared.' : 'Note uploaded. It is private; only you can see it.', 'success')
      setOpen(false)
      setFile(null)
      setMeta({ title: '', subject: '', topic: '', description: '', isPublic: false })
      notes.reload()
    } catch (err) {
      toast(describeError(err).message, 'error')
    } finally {
      setProgress(null)
    }
  }

  const download = async (n: Note) => {
    try {
      const res = await api.get<Blob>(`/notes/${n.id}/download`, { responseType: 'blob' })
      const url = URL.createObjectURL(res.data)
      const a = document.createElement('a')
      a.href = url
      a.download = n.originalFilename
      a.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      toast(describeError(err).message, 'error')
    }
  }

  const remove = async () => {
    if (!deleting) return
    try {
      await api.delete(`/notes/${deleting.id}`)
      toast('Note deleted.', 'success')
      setDeleting(null)
      notes.reload()
    } catch (err) {
      toast(describeError(err).message, 'error')
    }
  }

  return (
    <>
      <PageHeader eyebrow="Notes Library" title="Discover and share study notes" actions={<Btn icon={Upload} onClick={() => setOpen(true)}>Upload a note</Btn>} />
      <Tabs label="Notes sections" value={tab} onChange={(t) => { setTab(t); setPage(0) }} tabs={[{ id: 'library', label: 'Shared library' }, { id: 'mine', label: 'My notes' }]} />
      {tab === 'library' && (
        <div className="filters">
          <div className="field search-field">
            <label htmlFor="n-search">Search notes</label>
            <div className="input-icon"><Search size={15} aria-hidden="true" /><input id="n-search" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0) }} placeholder="Title or keyword" /></div>
          </div>
          <Field label="Subject" value={subject} onChange={(e) => { setSubject(e.target.value); setPage(0) }} placeholder="e.g. DBMS" />
        </div>
      )}
      <ResourceView resource={notes} endpoint={tab === 'library' ? 'GET /notes' : 'GET /notes/my-notes'} isEmpty={(d) => d.content.length === 0} emptyIcon={NotebookPen} emptyTitle="No notes found" emptyText={tab === 'library' ? 'Nobody has shared a note yet, or nothing matches your search.' : 'You have not uploaded any notes yet.'}>
        {(d) => (
          <>
            <div className="feature-grid three">
              {d.content.map((n) => (
                <div className="card feature-card card--resource" key={n.id}>
                  <div className="feature-card-top"><span className="feature-icon tone-green" aria-hidden="true"><FileText size={19} /></span><Badge tone={n.public ? 'green' : 'neutral'}>{n.public ? 'Shared' : 'Private'}</Badge></div>
                  <h3>{n.title}</h3>
                  <p>{[n.subject, n.topic].filter(Boolean).join(' \u00b7 ') || 'No subject'}</p>
                  <p className="muted">by {n.owner.displayName} &middot; {kb(n.fileSizeBytes)} &middot; {n.downloadCount} downloads</p>
                  <div className="row">
                    <Btn variant="secondary" icon={Download} onClick={() => void download(n)}>Download</Btn>
                    {String(n.owner.id) === session?.user.id && <Btn variant="ghost" icon={Trash2} onClick={() => setDeleting(n)} aria-label={`Delete ${n.title}`}>Delete</Btn>}
                  </div>
                </div>
              ))}
            </div>
            <Pager page={d.number} totalPages={d.totalPages} onChange={setPage} />
          </>
        )}
      </ResourceView>

      <Modal open={open} onClose={() => progress === null && setOpen(false)} title="Upload a note" footer={<><Btn variant="secondary" disabled={progress !== null} onClick={() => setOpen(false)}>Cancel</Btn><Btn type="submit" form="upload-form" loading={progress !== null}>Upload</Btn></>}>
        <form id="upload-form" className="form-stack" onSubmit={upload} noValidate>
          <Field label="Title" value={meta.title} error={errors.title} onChange={(e) => setMeta({ ...meta, title: e.target.value })} />
          <Field label="Subject (optional)" value={meta.subject} onChange={(e) => setMeta({ ...meta, subject: e.target.value })} />
          <Field label="Topic (optional)" value={meta.topic} onChange={(e) => setMeta({ ...meta, topic: e.target.value })} />
          <TextAreaField label="Description (optional)" rows={2} value={meta.description} onChange={(e) => setMeta({ ...meta, description: e.target.value })} />
          <Field label={`File (max ${MAX_MB} MB)`} type="file" accept={ALLOWED.map((e) => `.${e}`).join(',')} error={errors.file} onChange={(e) => pick(e.target.files?.[0] ?? null)} hint="PDF, Word, text, Markdown or image files." />
          <label className="check"><input type="checkbox" checked={meta.isPublic} onChange={(e) => setMeta({ ...meta, isPublic: e.target.checked })} /> Share with all students (otherwise only you can see it)</label>
          {progress !== null && <ProgressBar label="Upload progress" value={progress} />}
        </form>
      </Modal>
      <Modal open={!!deleting} onClose={() => setDeleting(null)} title="Delete this note?" footer={<><Btn variant="secondary" onClick={() => setDeleting(null)}>Cancel</Btn><Btn variant="danger" onClick={remove}>Delete</Btn></>}>
        <p><strong>{deleting?.title}</strong> and its file will be permanently removed.</p>
      </Modal>
    </>
  )
}
