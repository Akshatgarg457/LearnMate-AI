import { useCallback, useRef, useState, type ReactNode } from 'react'
import { CircleAlert, CircleCheck, Info, X } from 'lucide-react'
import { ToastContext, type ToastApi, type ToastTone } from './toastContext'

type Toast = { id: number; message: string; tone: ToastTone; leaving?: boolean }
const icons = { info: Info, success: CircleCheck, error: CircleAlert }

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const nextId = useRef(1)

  const dismiss = useCallback((id: number) => {
    setToasts((all) => all.map((t) => (t.id === id ? { ...t, leaving: true } : t)))
    window.setTimeout(() => setToasts((all) => all.filter((t) => t.id !== id)), 260)
  }, [])

  const push = useCallback<ToastApi>(
    (message, tone = 'info') => {
      const id = nextId.current++
      setToasts((all) => [...all.slice(-3), { id, message, tone }])
      window.setTimeout(() => dismiss(id), 5000)
    },
    [dismiss],
  )

  return (
    <ToastContext.Provider value={push}>
      {children}
      <div className="toast-stack" role="status" aria-live="polite">
        {toasts.map((t) => {
          const Icon = icons[t.tone]
          return (
            <div className={`toast toast--${t.tone} ${t.leaving ? 'is-leaving' : ''}`} key={t.id}>
              <Icon size={17} aria-hidden="true" />
              <span>{t.message}</span>
              <button type="button" onClick={() => dismiss(t.id)} aria-label="Dismiss notification">
                <X size={14} aria-hidden="true" />
              </button>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}
