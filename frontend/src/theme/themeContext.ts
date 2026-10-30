import { createContext, useContext } from 'react'

export type Theme = 'light' | 'dark'

export type ThemeState = {
  theme: Theme
  toggle: () => void
  setTheme: (t: Theme) => void
}

export const THEME_KEY = 'lm.theme'

export const ThemeContext = createContext<ThemeState | null>(null)

export function useTheme(): ThemeState {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme must be used inside ThemeProvider')
  return ctx
}
