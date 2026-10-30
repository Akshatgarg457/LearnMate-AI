import { Moon, Sun } from 'lucide-react'
import { useTheme } from './themeContext'

export default function ThemeToggle({ className = '' }: { className?: string }) {
  const { theme, toggle } = useTheme()
  const next = theme === 'dark' ? 'light' : 'dark'
  return (
    <button type="button" className={`icon-btn theme-toggle ${className}`.trim()} onClick={toggle} aria-label={`Switch to ${next} theme`} title={`Switch to ${next} theme`}>
      <Sun className="theme-sun" size={17} aria-hidden="true" />
      <Moon className="theme-moon" size={17} aria-hidden="true" />
    </button>
  )
}
