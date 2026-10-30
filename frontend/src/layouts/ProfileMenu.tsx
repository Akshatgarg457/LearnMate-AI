import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ChevronDown, LogOut, Settings } from 'lucide-react'
import { useAuth } from '../auth/authContext'

function initials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  return ((parts[0]?.[0] ?? '?') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase()
}

export default function ProfileMenu() {
  const { session, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)
  const trigger = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!open) return
    const onDown = (e: MouseEvent) => {
      if (root.current && !root.current.contains(e.target as Node)) setOpen(false)
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false)
        trigger.current?.focus()
      }
    }
    document.addEventListener('mousedown', onDown)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDown)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  if (!session) return null
  const { user } = session

  return (
    <div className="profile" ref={root}>
      <button ref={trigger} type="button" className="profile-trigger" aria-expanded={open} aria-haspopup="true" onClick={() => setOpen((o) => !o)}>
        <span className="avatar" aria-hidden="true">
          {initials(user.displayName)}
        </span>
        <span className="profile-name">{user.displayName}</span>
        <ChevronDown size={15} aria-hidden="true" className="profile-chevron" />
      </button>
      {(
        <div className="menu-pop" data-open={open} inert={!open}>
          <div className="menu-who">
            <strong>{user.displayName}</strong>
            <span>{user.email}</span>
          </div>
          <Link to={user.role === 'ADMIN' ? '/admin/settings' : '/settings'} className="menu-item" onClick={() => setOpen(false)}>
            <Settings size={16} aria-hidden="true" /> Profile &amp; settings
          </Link>
          <button
            type="button"
            className="menu-item"
            onClick={() => {
              setOpen(false)
              logout()
              navigate('/')
            }}
          >
            <LogOut size={16} aria-hidden="true" /> Log out
          </button>
        </div>
      )}
    </div>
  )
}
