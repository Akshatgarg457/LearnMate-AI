import { useId, useState, type InputHTMLAttributes } from 'react'
import { Eye, EyeOff } from 'lucide-react'

export default function PasswordField({ label, error, hint, ...input }: { label: string; error?: string; hint?: string } & InputHTMLAttributes<HTMLInputElement>) {
  const id = useId()
  const [visible, setVisible] = useState(false)
  return (
    <div className={`field ${error ? 'field--error' : ''}`}>
      <label htmlFor={id}>{label}</label>
      <div className="password-wrap">
        <input id={id} type={visible ? 'text' : 'password'} aria-invalid={!!error} {...input} />
        <button type="button" className="icon-btn" onClick={() => setVisible((v) => !v)} aria-label={visible ? 'Hide password' : 'Show password'} aria-pressed={visible}>
          {visible ? <EyeOff size={16} aria-hidden="true" /> : <Eye size={16} aria-hidden="true" />}
        </button>
      </div>
      {hint && !error && <small>{hint}</small>}
      {error && <small role="alert">{error}</small>}
    </div>
  )
}
