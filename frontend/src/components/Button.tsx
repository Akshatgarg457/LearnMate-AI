import type { AnchorHTMLAttributes } from 'react'
import { Link } from 'react-router-dom'

type ButtonProps = AnchorHTMLAttributes<HTMLAnchorElement> & {
  variant?: 'primary' | 'secondary'
  /** Internal route. When set, renders a client-side router link instead of an anchor. */
  to?: string
}

function Button({
  children,
  className = '',
  variant = 'primary',
  to,
  ...props
}: ButtonProps) {
  const classes = `button button--${variant} ${className}`.trim()
  if (to) {
    return (
      <Link className={classes} to={to} {...props}>
        {children}
      </Link>
    )
  }
  return (
    <a className={classes} {...props}>
      {children}
    </a>
  )
}

export default Button
