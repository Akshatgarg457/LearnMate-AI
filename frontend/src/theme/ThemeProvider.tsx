import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { THEME_KEY, ThemeContext, type Theme, type ThemeState } from './themeContext'

const META_COLORS: Record<Theme, string> = { dark: '#101014', light: '#f3f2f8' }

function initialTheme(): Theme {
  // index.html already set data-theme before first paint; read it back so React agrees.
  const fromDom = typeof document !== 'undefined' ? document.documentElement.getAttribute('data-theme') : null
  return fromDom === 'light' ? 'light' : 'dark'
}

function hasSavedChoice(): boolean {
  try {
    const v = localStorage.getItem(THEME_KEY)
    return v === 'light' || v === 'dark'
  } catch {
    return false
  }
}

/** UI preference only: stored locally, never sent to the backend. */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(initialTheme)
  const first = useRef(true)

  useEffect(() => {
    const root = document.documentElement
    root.setAttribute('data-theme', theme)
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', META_COLORS[theme])
    if (first.current) {
      first.current = false
      return
    }
    // Briefly enable colour transitions so the switch cross-fades (skipped by reduced-motion CSS).
    root.classList.add('theme-switching')
    const id = window.setTimeout(() => root.classList.remove('theme-switching'), 260)
    return () => window.clearTimeout(id)
  }, [theme])

  // Follow the operating system until the user makes an explicit choice.
  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: light)')
    const onChange = (e: MediaQueryListEvent) => {
      if (!hasSavedChoice()) setThemeState(e.matches ? 'light' : 'dark')
    }
    media.addEventListener('change', onChange)
    return () => media.removeEventListener('change', onChange)
  }, [])

  const setTheme = useCallback((t: Theme) => {
    try {
      localStorage.setItem(THEME_KEY, t)
    } catch {
      /* storage unavailable: theme still applies for this visit */
    }
    setThemeState(t)
  }, [])

  const toggle = useCallback(() => setTheme(theme === 'dark' ? 'light' : 'dark'), [theme, setTheme])

  const value = useMemo<ThemeState>(() => ({ theme, toggle, setTheme }), [theme, toggle, setTheme])
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}
