import { useEffect } from 'react'

/** Adds `.is-visible` to every `[data-reveal]` element when it scrolls into view. */
export function useReveal() {
  useEffect(() => {
    const nodes = document.querySelectorAll<HTMLElement>('[data-reveal]')
    if (!('IntersectionObserver' in window)) {
      nodes.forEach((n) => n.classList.add('is-visible'))
      return
    }
    const observer = new IntersectionObserver(
      (entries) =>
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible')
            observer.unobserve(entry.target)
          }
        }),
      { threshold: 0.12 },
    )
    nodes.forEach((n) => observer.observe(n))
    return () => observer.disconnect()
  }, [])
}
