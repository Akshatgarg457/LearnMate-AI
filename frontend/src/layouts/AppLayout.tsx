import { Suspense, useEffect, useLayoutEffect, useRef, useState, type RefObject } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { ChevronDown, ChevronRight, Menu, PanelLeft, ShieldCheck, Sparkles, X } from 'lucide-react'
import { useAuth } from '../auth/authContext'
import { homeFor } from '../auth/roles'
import { Skeleton } from '../components/ui'
import ThemeToggle from '../theme/ThemeToggle'
import { labelByPath, navFor, type NavItem } from './nav'
import ProfileMenu from './ProfileMenu'

const COLLAPSE_KEY = 'lm.sidebar'

function SideLink({ item, onNavigate }: { item: NavItem; onNavigate: () => void }) {
  const Icon = item.icon
  // `end` keeps parent items (Dashboard, Admin Overview, Exam Centre) from staying active on child routes.
  const end = !!item.children || item.to === '/admin'
  return (
    <NavLink to={item.to} end={end} onClick={onNavigate} title={item.label} className={({ isActive }) => `side-link ${isActive ? 'is-active' : ''}`}>
      <Icon size={18} strokeWidth={1.9} aria-hidden="true" />
      <span className="side-text">{item.label}</span>
    </NavLink>
  )
}

const prettify = (p: string) => p.replace(/-/g, ' ').replace(/^\w/, (c) => c.toUpperCase())

function Breadcrumbs({ admin }: { admin: boolean }) {
  const { pathname } = useLocation()
  const parts = pathname.split('/').filter(Boolean)
  // Admin pages read "Admin > User Management" rather than repeating "admin" as a segment.
  const trail = (admin && parts[0] === 'admin' ? parts.slice(1) : parts).map((seg, i, arr) => {
    const base = admin && parts[0] === 'admin' ? ['admin'] : []
    const path = '/' + [...base, ...arr.slice(0, i + 1)].join('/')
    return { path, label: labelByPath[path] ?? prettify(seg) }
  })
  const rootPath = admin ? '/admin' : '/dashboard'
  return (
    <nav className="crumbs" aria-label="Breadcrumb">
      {trail.length === 0 && admin ? (
        <span key="admin-root"><span aria-current="page">Admin Overview</span></span>
      ) : (
        <Link to={rootPath}>{admin ? 'Admin' : 'Home'}</Link>
      )}
      {trail.map((c, i) => (
        <span key={c.path}>
          <ChevronRight size={13} aria-hidden="true" />
          {i === trail.length - 1 ? <span aria-current="page">{c.label}</span> : <Link to={c.path}>{c.label}</Link>}
        </span>
      ))}
    </nav>
  )
}

/** One marker that slides to the active link, instead of each link showing its own. */
function useSlidingIndicator(nav: RefObject<HTMLElement | null>, routeKey: string) {
  const [box, setBox] = useState({ y: 0, h: 0, visible: false })
  const [animate, setAnimate] = useState(false)
  useLayoutEffect(() => {
    const el = nav.current
    if (!el) return
    const update = () => {
      const active = el.querySelector<HTMLElement>('.side-link.is-active')
      if (!active || active.offsetParent === null) return setBox((b) => (b.visible ? { ...b, visible: false } : b))
      const a = active.getBoundingClientRect()
      const n = el.getBoundingClientRect()
      const y = Math.round(a.top - n.top)
      const h = Math.round(a.height)
      setBox((b) => (b.visible && b.y === y && b.h === h ? b : { y, h, visible: true }))
    }
    update()
    const frame = requestAnimationFrame(() => setAnimate(true)) // no fly-in on first paint
    // Re-measure while groups expand/collapse and when the sidebar resizes.
    const ro = new ResizeObserver(update)
    ro.observe(el)
    window.addEventListener('resize', update)
    return () => {
      cancelAnimationFrame(frame)
      ro.disconnect()
      window.removeEventListener('resize', update)
    }
  }, [nav, routeKey])
  return { box, animate }
}

export default function AppLayout() {
  const { session } = useAuth()
  const { pathname } = useLocation()
  const [open, setOpen] = useState(false)
  const [manual, setManual] = useState<Record<string, boolean>>({})
  const [collapsed, setCollapsed] = useState(() => {
    try {
      return localStorage.getItem(COLLAPSE_KEY) === '1'
    } catch {
      return false
    }
  })
  const navRef = useRef<HTMLElement>(null)
  const role = session?.user.role ?? 'STUDENT'
  const { box, animate } = useSlidingIndicator(navRef, `${pathname}|${role}|${collapsed}`)

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open])

  // Opening a route from the drawer or keyboard should never leave the drawer covering the page.
  useEffect(() => setOpen(false), [pathname])

  if (!session) return null
  const admin = role === 'ADMIN'
  const groups = navFor(role)
  const close = () => setOpen(false)
  const toggleCollapse = () =>
    setCollapsed((c) => {
      try {
        localStorage.setItem(COLLAPSE_KEY, c ? '0' : '1')
      } catch {
        /* ignore */
      }
      return !c
    })

  return (
    <div className={`shell ${admin ? 'shell--admin' : ''}`} data-collapsed={collapsed} data-role={role}>
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <aside className={`sidebar ${open ? 'is-open' : ''}`} aria-label={admin ? 'Admin navigation' : 'Application navigation'}>
        <div className="sidebar-top">
          <Link to={homeFor(role)} className="brand" onClick={close} aria-label={admin ? 'LearnMate AI admin overview' : 'LearnMate AI dashboard'}>
            <span className="brand-mark" aria-hidden="true">
              <Sparkles size={19} strokeWidth={2.2} />
            </span>
            <span className="side-text">
              LearnMate <span className="brand-ai">AI</span>
            </span>
          </Link>
          <button type="button" className="icon-btn drawer-close" onClick={close} aria-label="Close navigation">
            <X size={18} aria-hidden="true" />
          </button>
        </div>
        {admin && (
          <p className="role-chip">
            <ShieldCheck size={14} aria-hidden="true" />
            <span className="side-text">Admin console</span>
          </p>
        )}
        <nav className="side-nav" ref={navRef}>
          <span className={`side-indicator ${animate ? 'is-animated' : ''}`} aria-hidden="true" style={{ transform: `translateY(${box.y + box.h * 0.2}px)`, height: box.h * 0.6, opacity: box.visible ? 1 : 0 }} />
          {groups.map((group) => (
            <div className="side-group" key={group.label}>
              <p className="side-group-label">{group.label}</p>
              {group.items.map((item) => {
                const expanded = manual[item.to] ?? pathname.startsWith(item.to)
                const subId = `sub-${item.to.replace(/\W/g, '')}`
                return (
                  <div key={item.to}>
                    {item.children ? (
                      <div className="side-row">
                        <SideLink item={item} onNavigate={close} />
                        <button
                          type="button"
                          className="side-chevron"
                          aria-expanded={expanded}
                          aria-controls={subId}
                          aria-label={`${expanded ? 'Collapse' : 'Expand'} ${item.label} menu`}
                          onClick={() => setManual((m) => ({ ...m, [item.to]: !expanded }))}
                        >
                          <ChevronDown size={15} aria-hidden="true" />
                        </button>
                      </div>
                    ) : (
                      <SideLink item={item} onNavigate={close} />
                    )}
                    {item.children && (
                      <div className={`side-sub-wrap ${expanded ? 'is-open' : ''}`} id={subId} inert={!expanded}>
                        <div className="side-sub">
                          {item.children.map((c) => (
                            <SideLink key={c.to} item={c} onNavigate={close} />
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          ))}
        </nav>
      </aside>
      <div className={`scrim ${open ? 'is-open' : ''}`} onClick={close} aria-hidden="true" />

      <div className="shell-main">
        <div className="topbar">
          <button type="button" className="icon-btn menu-btn" onClick={() => setOpen(true)} aria-label="Open navigation" aria-expanded={open}>
            <Menu size={19} aria-hidden="true" />
          </button>
          <button type="button" className="icon-btn collapse-btn" onClick={toggleCollapse} aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'} aria-pressed={collapsed}>
            <PanelLeft size={18} aria-hidden="true" />
          </button>
          <Breadcrumbs admin={admin} />
          <div className="topbar-end">
            <ThemeToggle />
            <ProfileMenu />
          </div>
        </div>
        <main id="main" className="content" key={pathname}>
          <Suspense fallback={<div className="card pad"><Skeleton lines={5} /></div>}>
            <Outlet />
          </Suspense>
        </main>
      </div>
    </div>
  )
}
